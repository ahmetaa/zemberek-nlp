package zemberek.morphology.ambiguity.dataset;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.apps.ConsoleApp;
import zemberek.core.logging.Log;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.tokenization.Token;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Extracts candidate morphological analyses for sentences from a raw Turkish text file and
 * exports them into a structured JSON Lines (.jsonl) file and companion metadata (.meta.json) file
 * for LLM-based disambiguation and classifier dataset generation.
 */
public class DisambiguationCandidateExtractor extends ConsoleApp {

  @Parameter(
      names = {"--input", "-i"},
      required = true,
      description = "Input text file containing Turkish sentences or documents.")
  public Path inputPath;

  @Parameter(
      names = {"--output", "-o"},
      required = true,
      description = "Output .jsonl file for candidate analyses.")
  public Path outputPath;

  @Parameter(
      names = {"--metaOutput", "-m"},
      description = "Output .meta.json sidecar file. If omitted, defaults to <output>.meta.json")
  public Path metaOutputPath;

  @Parameter(
      names = {"--maxSentences", "-max"},
      description = "Maximum number of sentences to extract. Default is -1 (unlimited).")
  public int maxSentences = -1;

  @Parameter(
      names = {"--allSentences", "-all"},
      description =
          "If set, includes sentences even if they have no ambiguous tokens. Default only exports "
              + "sentences containing at least one ambiguous token.")
  public boolean includeAllSentences = false;

  @Parameter(
      names = {"--enableInformal", "-informal"},
      description = "Enable informal word analysis.")
  public boolean enableInformal = false;

  @Parameter(
      names = {"--ignoreDiacritics", "-no_diacritics"},
      description = "Ignore Turkish diacritics in analysis.")
  public boolean ignoreDiacritics = false;

  public static void main(String[] args) {
    new DisambiguationCandidateExtractor().execute(args);
  }

  @Override
  public String description() {
    return "Extracts sentence tokens and morphological candidate analyses into JSONL format for LLM annotation.";
  }

  @Override
  public void run() throws Exception {
    Config config = new Config.Builder()
        .inputPath(inputPath)
        .outputPath(outputPath)
        .metaOutputPath(metaOutputPath)
        .maxSentences(maxSentences)
        .includeAllSentences(includeAllSentences)
        .enableInformal(enableInformal)
        .ignoreDiacritics(ignoreDiacritics)
        .build();

    ExtractionResult result = extract(config);
    Log.info("Extraction finished successfully.");
    Log.info("Sentences written: %d", result.metadata.statistics.sentence_count);
    Log.info("Total tokens: %d", result.metadata.statistics.token_count);
    Log.info("Ambiguous tokens: %d (%.2f%%)",
        result.metadata.statistics.ambiguous_token_count,
        result.metadata.statistics.ambiguity_rate * 100);
    Log.info("JSONL output: %s", config.outputPath);
    Log.info("Metadata output: %s", config.getEffectiveMetaPath());
  }

  public static ExtractionResult extract(Config config) throws IOException {
    if (!Files.exists(config.inputPath)) {
      throw new IllegalArgumentException("Input file does not exist: " + config.inputPath);
    }

    if (config.outputPath.getParent() != null) {
      Files.createDirectories(config.outputPath.getParent());
    }

    TurkishSentenceExtractor sentenceExtractor = TurkishSentenceExtractor.DEFAULT;
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

    TurkishMorphology.Builder morphBuilder = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setTokenizer(tokenizer);

    if (config.enableInformal) {
      morphBuilder.useInformalAnalysis();
    }
    if (config.ignoreDiacritics) {
      morphBuilder.ignoreDiacriticsInAnalysis();
    }

    TurkishMorphology morphology = morphBuilder.build();
    Gson jsonlGson = new Gson();

    Statistics stats = new Statistics();
    int sentenceIndex = 0;

    Path effectiveMetaPath = config.getEffectiveMetaPath();
    if (effectiveMetaPath.getParent() != null) {
      Files.createDirectories(effectiveMetaPath.getParent());
    }

    try (BufferedReader reader = Files.newBufferedReader(config.inputPath, StandardCharsets.UTF_8);
         BufferedWriter writer = Files.newBufferedWriter(config.outputPath, StandardCharsets.UTF_8)) {

      String line;
      MAIN_LOOP:
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }

        List<String> sentences = sentenceExtractor.fromParagraph(line);
        for (String sentenceStr : sentences) {
          sentenceStr = sentenceStr.trim();
          if (sentenceStr.isEmpty()) {
            continue;
          }

          SentenceRecord record = processSentence(
              "s_" + String.format("%06d", sentenceIndex + 1),
              sentenceStr,
              tokenizer,
              morphology);

          boolean hasAmbiguity = record.tokens.stream().anyMatch(t -> t.is_ambiguous);
          if (!hasAmbiguity && !config.includeAllSentences) {
            continue;
          }

          writer.write(jsonlGson.toJson(record));
          writer.newLine();

          sentenceIndex++;
          stats.sentence_count++;
          stats.token_count += record.tokens.size();
          stats.ambiguous_token_count += (int) record.tokens.stream().filter(t -> t.is_ambiguous).count();

          if (config.maxSentences > 0 && stats.sentence_count >= config.maxSentences) {
            break MAIN_LOOP;
          }
        }
      }
    }

    if (stats.token_count > 0) {
      double rate = (double) stats.ambiguous_token_count / stats.token_count;
      stats.ambiguity_rate = Math.round(rate * 10000.0) / 10000.0;
    }

    ExtractionMetadata metadata = new ExtractionMetadata();
    metadata.provenance = new Provenance();
    metadata.provenance.source_file = config.inputPath.getFileName().toString();
    metadata.provenance.java_version = System.getProperty("java.version");
    metadata.provenance.created_at = Instant.now().toString();

    metadata.zemberek_settings = new ZemberekSettings();
    metadata.zemberek_settings.informal_analysis = config.enableInformal;
    metadata.zemberek_settings.ignore_diacritics = config.ignoreDiacritics;
    metadata.zemberek_settings.only_ambiguous_sentences = !config.includeAllSentences;

    metadata.statistics = stats;
    metadata.annotation = new AnnotationStatus();

    Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    try (BufferedWriter metaWriter = Files.newBufferedWriter(effectiveMetaPath, StandardCharsets.UTF_8)) {
      prettyGson.toJson(metadata, metaWriter);
    }

    return new ExtractionResult(metadata, config.outputPath, effectiveMetaPath);
  }

  private static SentenceRecord processSentence(
      String sentenceId,
      String sentence,
      TurkishTokenizer tokenizer,
      TurkishMorphology morphology) {

    SentenceRecord record = new SentenceRecord();
    record.sentence_id = sentenceId;
    record.text = sentence;

    String normalized = TextUtil.normalizeQuotesHyphens(sentence);
    List<Token> tokens = tokenizer.tokenize(normalized);

    int tokenIndex = 0;
    for (Token token : tokens) {
      TokenRecord tr = new TokenRecord();
      tr.index = tokenIndex++;
      tr.surface = token.getText();
      tr.char_start = token.getStart();
      tr.char_end = token.getEnd();

      WordAnalysis wa = morphology.analyze(token);
      int candidateId = 0;
      for (SingleAnalysis sa : wa.getAnalysisResults()) {
        CandidateRecord cr = new CandidateRecord();
        cr.id = candidateId++;
        cr.zemberek_key = sa.formatLong();

        DictionaryItem item = sa.getDictionaryItem();
        cr.lemma = item.lemma;
        cr.pos = item.primaryPos != null ? item.primaryPos.shortForm : PrimaryPos.Unknown.shortForm;
        cr.secondary_pos = (item.secondaryPos != null && item.secondaryPos != SecondaryPos.None)
            ? item.secondaryPos.shortForm
            : "None";
        cr.oflazer_style = AnalysisFormatters.OFLAZER_STYLE.format(sa);
        cr.morphemes = sa.getMorphemes().stream()
            .map(m -> m.id)
            .collect(Collectors.toList());
        cr.is_informal = sa.containsInformalMorpheme();

        tr.candidates.add(cr);
      }

      tr.is_ambiguous = tr.candidates.size() > 1;
      record.tokens.add(tr);
    }

    return record;
  }

  // --- Data Models ---

  public static class Config {
    public final Path inputPath;
    public final Path outputPath;
    public final Path metaOutputPath;
    public final int maxSentences;
    public final boolean includeAllSentences;
    public final boolean enableInformal;
    public final boolean ignoreDiacritics;

    private Config(Builder b) {
      this.inputPath = b.inputPath;
      this.outputPath = b.outputPath;
      this.metaOutputPath = b.metaOutputPath;
      this.maxSentences = b.maxSentences;
      this.includeAllSentences = b.includeAllSentences;
      this.enableInformal = b.enableInformal;
      this.ignoreDiacritics = b.ignoreDiacritics;
    }

    public Path getEffectiveMetaPath() {
      if (metaOutputPath != null) {
        return metaOutputPath;
      }
      String outStr = outputPath.toString();
      if (outStr.endsWith(".jsonl")) {
        return Paths.get(outStr.substring(0, outStr.length() - 6) + ".meta.json");
      }
      return Paths.get(outStr + ".meta.json");
    }

    public static class Builder {
      Path inputPath;
      Path outputPath;
      Path metaOutputPath;
      int maxSentences = -1;
      boolean includeAllSentences = false;
      boolean enableInformal = false;
      boolean ignoreDiacritics = false;

      public Builder inputPath(Path inputPath) {
        this.inputPath = inputPath;
        return this;
      }

      public Builder outputPath(Path outputPath) {
        this.outputPath = outputPath;
        return this;
      }

      public Builder metaOutputPath(Path metaOutputPath) {
        this.metaOutputPath = metaOutputPath;
        return this;
      }

      public Builder maxSentences(int maxSentences) {
        this.maxSentences = maxSentences;
        return this;
      }

      public Builder includeAllSentences(boolean includeAllSentences) {
        this.includeAllSentences = includeAllSentences;
        return this;
      }

      public Builder enableInformal(boolean enableInformal) {
        this.enableInformal = enableInformal;
        return this;
      }

      public Builder ignoreDiacritics(boolean ignoreDiacritics) {
        this.ignoreDiacritics = ignoreDiacritics;
        return this;
      }

      public Config build() {
        if (inputPath == null) {
          throw new IllegalArgumentException("inputPath must be set");
        }
        if (outputPath == null) {
          throw new IllegalArgumentException("outputPath must be set");
        }
        return new Config(this);
      }
    }
  }

  public static class ExtractionResult {
    public final ExtractionMetadata metadata;
    public final Path jsonlPath;
    public final Path metaPath;

    public ExtractionResult(ExtractionMetadata metadata, Path jsonlPath, Path metaPath) {
      this.metadata = metadata;
      this.jsonlPath = jsonlPath;
      this.metaPath = metaPath;
    }
  }

  public static class ExtractionMetadata {
    public String schema_version = "1.0";
    public String dataset_name = "turkish_morphology_disambiguation_candidates";
    public Provenance provenance;
    public ZemberekSettings zemberek_settings;
    public Statistics statistics;
    public AnnotationStatus annotation;
  }

  public static class Provenance {
    public String source_file;
    public String parser = "zemberek-nlp";
    public String parser_version = "0.17.2";
    public String java_version;
    public String created_at;
  }

  public static class ZemberekSettings {
    public boolean informal_analysis;
    public boolean ignore_diacritics;
    public boolean only_ambiguous_sentences;
  }

  public static class Statistics {
    public int sentence_count = 0;
    public int token_count = 0;
    public int ambiguous_token_count = 0;
    public double ambiguity_rate = 0.0;
  }

  public static class AnnotationStatus {
    public String status = "pending_llm_selection";
    public String annotator_model = null;
    public String annotated_at = null;
  }

  public static class SentenceRecord {
    public String sentence_id;
    public String text;
    public List<TokenRecord> tokens = new ArrayList<>();
  }

  public static class TokenRecord {
    public int index;
    public String surface;
    public int char_start;
    public int char_end;
    public boolean is_ambiguous;
    public Integer selected_candidate_id;
    public List<CandidateRecord> candidates = new ArrayList<>();
  }

  public static class CandidateRecord {
    public int id;
    public String zemberek_key;
    public String lemma;
    public String pos;
    public String secondary_pos;
    public String oflazer_style;
    public List<String> morphemes = new ArrayList<>();
    public boolean is_informal;
  }
}

