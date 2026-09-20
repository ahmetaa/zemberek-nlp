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
import java.util.ArrayList;
import java.util.List;
import zemberek.apps.ConsoleApp;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.CandidateRecord;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.SentenceRecord;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.TokenRecord;
import zemberek.morphology.ambiguity.prior.NGramPriorPerceptronResolver;
import zemberek.morphology.ambiguity.prior.NGramPriorStore;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * Evaluates morphological disambiguation models against LLM-annotated test datasets.
 * Compares one or two trained models side-by-side with Zemberek's default baseline.
 */
public class EvaluateAmbiguityModel extends ConsoleApp {

  @Parameter(
      names = {"--annotated", "-a"},
      required = true,
      description = "Path to ground truth annotated JSONL file")
  public Path annotatedPath;

  @Parameter(
      names = {"--model", "-m"},
      required = true,
      description = "Path to primary trained binary model (.bin)")
  public Path modelPath;

  @Parameter(
      names = {"--model2", "-m2"},
      required = false,
      description = "Optional path to second trained binary model (.bin)")
  public Path model2Path;

  @Parameter(
      names = {"--priorBigrams"},
      description = "Path to unambiguous bigrams text file for prior-enhanced model")
  public Path priorBigramsPath;

  @Parameter(
      names = {"--priorTrigrams"},
      description = "Path to unambiguous trigrams text file for prior-enhanced model")
  public Path priorTrigramsPath;

  @Parameter(
      names = {"--priorCollocations"},
      description = "Path to significant collocations text file for prior-enhanced model")
  public Path priorCollocationsPath;

  @Parameter(
      names = {"--priorMinCount"},
      description = "Minimum count threshold for including an N-gram. Default is 3.")
  public int priorMinCount = 3;

  @Parameter(
      names = {"--model1HasPriors"},
      description = "Explicitly specify whether model 1 uses N-gram prior feature extractor")
  public Boolean model1HasPriors = null;

  @Parameter(
      names = {"--model2HasPriors"},
      description = "Explicitly specify whether model 2 uses N-gram prior feature extractor")
  public Boolean model2HasPriors = null;

  @Parameter(
      names = {"--output", "-o"},
      required = true,
      description = "Path to save detailed JSON evaluation report")
  public Path outputPath;

  @Parameter(
      names = {"--beamSize", "-b"},
      description = "Beam size for NGramPriorPerceptronResolver (default -1 for exact Viterbi).")
  public int beamSize = -1;

  @Parameter(
      names = {"--greedy", "-g"},
      description = "Enable greedy decoding mode for NGramPriorPerceptronResolver")
  public boolean greedy = false;

  public static void main(String[] args) {
    new EvaluateAmbiguityModel().execute(args);
  }

  @Override
  public String description() {
    return "Evaluates trained morphological disambiguation model(s) and baseline against annotated ground truth.";
  }

  @Override
  public void run() throws Exception {
    boolean hasPriorFiles = priorBigramsPath != null || priorTrigramsPath != null || priorCollocationsPath != null;
    NGramPriorStore priorStore = null;
    if (hasPriorFiles) {
      Log.info("Loading N-gram priors for evaluation...");
      priorStore = NGramPriorStore.builder()
          .bigramsPath(priorBigramsPath)
          .trigramsPath(priorTrigramsPath)
          .collocationsPath(priorCollocationsPath)
          .minCount(priorMinCount)
          .build();
      Log.info("Loaded priors: %d bigrams, %d trigrams, %d collocations.",
          priorStore.bigramSize(), priorStore.trigramSize(), priorStore.collocationSize());
    }

    boolean m1Prior = (model1HasPriors != null) ? model1HasPriors :
        (priorStore != null || modelPath.getFileName().toString().contains("disambiguation") || modelPath.getFileName().toString().contains("contrastive") || modelPath.getFileName().toString().contains("prior"));

    Log.info("Loading primary trained model from: %s (with priors: %s)", modelPath, m1Prior);
    AmbiguityResolver trainedResolver = m1Prior
        ? NGramPriorPerceptronResolver.fromModelFile(modelPath, priorStore)
        : PerceptronAmbiguityResolver.fromModelFile(modelPath);

    if (trainedResolver instanceof NGramPriorPerceptronResolver) {
      NGramPriorPerceptronResolver pr = (NGramPriorPerceptronResolver) trainedResolver;
      pr.setBeamSize(beamSize);
      pr.setGreedy(greedy);
    }

    TurkishMorphology trainedMorphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setAmbiguityResolver(trainedResolver)
        .build();

    TurkishMorphology model2Morphology = null;
    if (model2Path != null) {
      boolean m2Prior = (model2HasPriors != null) ? model2HasPriors :
          (priorStore != null && (model2Path.getFileName().toString().contains("disambiguation") || model2Path.getFileName().toString().contains("contrastive") || model2Path.getFileName().toString().contains("prior")));

      Log.info("Loading second trained model from: %s (with priors: %s)", model2Path, m2Prior);
      AmbiguityResolver model2Resolver = m2Prior
          ? NGramPriorPerceptronResolver.fromModelFile(model2Path, priorStore)
          : PerceptronAmbiguityResolver.fromModelFile(model2Path);

      if (model2Resolver instanceof NGramPriorPerceptronResolver) {
        NGramPriorPerceptronResolver pr2 = (NGramPriorPerceptronResolver) model2Resolver;
        pr2.setBeamSize(beamSize);
        pr2.setGreedy(greedy);
      }

      model2Morphology = TurkishMorphology.builder()
          .setLexicon(RootLexicon.getDefault())
          .setAmbiguityResolver(model2Resolver)
          .build();
    }

    Log.info("Loading default baseline morphology...");
    TurkishMorphology defaultMorphology = TurkishMorphology.createWithDefaults();

    Log.info("Evaluating test dataset from: %s", annotatedPath);
    EvaluationReport report = evaluate(annotatedPath, modelPath, trainedMorphology, model2Path, model2Morphology, defaultMorphology);

    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
      gson.toJson(report, writer);
    }

    Log.info("Evaluation finished successfully.");
    Log.info("Report written to: %s", outputPath);
    printSummary(report);
  }

  public static EvaluationReport evaluate(
      Path annotatedPath,
      Path model1Path,
      TurkishMorphology trainedMorphology,
      Path model2Path,
      TurkishMorphology model2Morphology,
      TurkishMorphology defaultMorphology) throws IOException {

    Gson gson = new Gson();
    EvaluationReport report = new EvaluationReport();
    report.source_file = annotatedPath.toString();
    report.model1_name = model1Path != null ? model1Path.getFileName().toString() : "Model 1";
    report.has_model2 = (model2Morphology != null);
    if (report.has_model2 && model2Path != null) {
      report.model2_name = model2Path.getFileName().toString();
    }

    try (BufferedReader reader = Files.newBufferedReader(annotatedPath, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }

        SentenceRecord groundTruth = gson.fromJson(line, SentenceRecord.class);
        if (groundTruth == null || groundTruth.text == null || groundTruth.tokens == null) {
          continue;
        }

        report.total_sentences++;
        String sentenceText = groundTruth.text;

        SentenceAnalysis trainedAnalysis = trainedMorphology.analyzeAndDisambiguate(sentenceText);
        SentenceAnalysis model2Analysis = (model2Morphology != null)
            ? model2Morphology.analyzeAndDisambiguate(sentenceText)
            : null;
        SentenceAnalysis defaultAnalysis = defaultMorphology.analyzeAndDisambiguate(sentenceText);

        SentenceEvaluationRecord sentRec = new SentenceEvaluationRecord();
        sentRec.sentence_id = groundTruth.sentence_id;
        sentRec.text = sentenceText;

        boolean trainedExactMatch = true;
        boolean model2ExactMatch = true;
        boolean defaultExactMatch = true;

        for (int i = 0; i < groundTruth.tokens.size(); i++) {
          TokenRecord t = groundTruth.tokens.get(i);
          report.total_tokens++;

          TokenEvaluationRecord tokenEval = new TokenEvaluationRecord();
          tokenEval.index = t.index;
          tokenEval.surface = t.surface;
          tokenEval.is_ambiguous = t.is_ambiguous;
          tokenEval.candidate_count = t.candidates != null ? t.candidates.size() : 0;

          if (t.is_ambiguous) {
            report.ambiguous_tokens++;
          }

          int geminiSelectedId = extractSelectedId(t);
          tokenEval.gemini_selected_id = geminiSelectedId;
          tokenEval.gemini_selected_key = getCandidateKey(t, geminiSelectedId);
          tokenEval.gemini_pos = getCandidatePos(t, geminiSelectedId);

          // Get trained model choice
          // Get primary trained model choice
          SingleAnalysis trainedSa = (i < trainedAnalysis.size())
              ? trainedAnalysis.bestAnalysis().get(i)
              : null;
          int trainedMatchedId = matchAnalysisToCandidateId(t, trainedSa);
          tokenEval.trained_selected_id = trainedMatchedId;
          tokenEval.trained_selected_key = trainedSa != null ? trainedSa.formatLong() : "";

          // Get second model choice if present
          if (model2Analysis != null) {
            SingleAnalysis model2Sa = (i < model2Analysis.size())
                ? model2Analysis.bestAnalysis().get(i)
                : null;
            int model2MatchedId = matchAnalysisToCandidateId(t, model2Sa);
            tokenEval.model2_selected_id = model2MatchedId;
            tokenEval.model2_selected_key = model2Sa != null ? model2Sa.formatLong() : "";
            boolean model2Matches = (model2MatchedId == geminiSelectedId);
            tokenEval.model2_matches_gemini = model2Matches;

            if (model2Matches) {
              report.model2_overall_matches++;
              if (t.is_ambiguous) {
                report.model2_ambiguous_matches++;
              }
            } else {
              model2ExactMatch = false;
            }
          }

          // Get default baseline model choice
          SingleAnalysis defaultSa = (i < defaultAnalysis.size())
              ? defaultAnalysis.bestAnalysis().get(i)
              : null;
          int defaultMatchedId = matchAnalysisToCandidateId(t, defaultSa);
          tokenEval.default_selected_id = defaultMatchedId;
          tokenEval.default_selected_key = defaultSa != null ? defaultSa.formatLong() : "";

          // Check matches
          boolean trainedMatches = (trainedMatchedId == geminiSelectedId);
          boolean defaultMatches = (defaultMatchedId == geminiSelectedId);

          tokenEval.trained_matches_gemini = trainedMatches;
          tokenEval.default_matches_gemini = defaultMatches;

          if (trainedMatches) {
            report.trained_overall_matches++;
            if (t.is_ambiguous) {
              report.trained_ambiguous_matches++;
            }
          } else {
            trainedExactMatch = false;
          }

          if (defaultMatches) {
            report.default_overall_matches++;
            if (t.is_ambiguous) {
              report.default_ambiguous_matches++;
            }
          } else {
            defaultExactMatch = false;
          }

          sentRec.tokens.add(tokenEval);
        }

        if (trainedExactMatch) {
          report.trained_sentence_exact_matches++;
        }
        if (report.has_model2 && model2ExactMatch) {
          report.model2_sentence_exact_matches++;
        }
        if (defaultExactMatch) {
          report.default_sentence_exact_matches++;
        }

        report.sentences.add(sentRec);
      }
    }

    // Compute aggregate percentages
    if (report.total_tokens > 0) {
      report.trained_overall_accuracy =
          (double) report.trained_overall_matches / report.total_tokens;
      report.default_overall_accuracy =
          (double) report.default_overall_matches / report.total_tokens;
      if (report.has_model2) {
        report.model2_overall_accuracy =
            (double) report.model2_overall_matches / report.total_tokens;
      }
    }
    if (report.ambiguous_tokens > 0) {
      report.trained_ambiguity_accuracy =
          (double) report.trained_ambiguous_matches / report.ambiguous_tokens;
      report.default_ambiguity_accuracy =
          (double) report.default_ambiguous_matches / report.ambiguous_tokens;
      if (report.has_model2) {
        report.model2_ambiguity_accuracy =
            (double) report.model2_ambiguous_matches / report.ambiguous_tokens;
      }
    }
    if (report.total_sentences > 0) {
      report.trained_sentence_exact_match_rate =
          (double) report.trained_sentence_exact_matches / report.total_sentences;
      report.default_sentence_exact_match_rate =
          (double) report.default_sentence_exact_matches / report.total_sentences;
      if (report.has_model2) {
        report.model2_sentence_exact_match_rate =
            (double) report.model2_sentence_exact_matches / report.total_sentences;
      }
    }

    return report;
  }

  private static int extractSelectedId(TokenRecord t) {
    if (t.selected_candidate_id != null) {
      return t.selected_candidate_id;
    }
    return 0;
  }

  private static String getCandidateKey(TokenRecord t, int id) {
    if (t.candidates != null && id >= 0 && id < t.candidates.size()) {
      return t.candidates.get(id).zemberek_key;
    }
    return "";
  }

  private static String getCandidatePos(TokenRecord t, int id) {
    if (t.candidates != null && id >= 0 && id < t.candidates.size()) {
      return t.candidates.get(id).pos;
    }
    return "Unknown";
  }

  private static int matchAnalysisToCandidateId(TokenRecord t, SingleAnalysis sa) {
    if (t.candidates == null || sa == null) {
      return 0;
    }
    String saKey = sa.formatLong();
    for (CandidateRecord c : t.candidates) {
      if (saKey.equals(c.zemberek_key)) {
        return c.id;
      }
    }
    return 0;
  }

  private static void printSummary(EvaluationReport r) {
    System.out.println("\n==========================================================================================");
    System.out.printf("                             DISAMBIGUATION EVALUATION REPORT                             \n");
    System.out.println("==========================================================================================");
    System.out.printf("Total Sentences:   %d\n", r.total_sentences);
    System.out.printf("Total Tokens:      %d\n", r.total_tokens);
    System.out.printf("Ambiguous Tokens:  %d (%.2f%%)\n", r.ambiguous_tokens,
        (100.0 * r.ambiguous_tokens / Math.max(1, r.total_tokens)));
    System.out.println("------------------------------------------------------------------------------------------");

    if (r.has_model2) {
      System.out.printf("%-30s %-20s %-20s %-20s\n", "Metric", r.model1_name, r.model2_name, "Default Baseline");
      System.out.println("------------------------------------------------------------------------------------------");
      System.out.printf("%-30s %6.2f%% (%4d/%4d)  %6.2f%% (%4d/%4d)  %6.2f%% (%4d/%4d)\n",
          "Ambiguity Accuracy (Hard):",
          r.trained_ambiguity_accuracy * 100.0, r.trained_ambiguous_matches, r.ambiguous_tokens,
          r.model2_ambiguity_accuracy * 100.0, r.model2_ambiguous_matches, r.ambiguous_tokens,
          r.default_ambiguity_accuracy * 100.0, r.default_ambiguous_matches, r.ambiguous_tokens);
      System.out.printf("%-30s %6.2f%% (%4d/%4d)  %6.2f%% (%4d/%4d)  %6.2f%% (%4d/%4d)\n",
          "Overall Token Accuracy:",
          r.trained_overall_accuracy * 100.0, r.trained_overall_matches, r.total_tokens,
          r.model2_overall_accuracy * 100.0, r.model2_overall_matches, r.total_tokens,
          r.default_overall_accuracy * 100.0, r.default_overall_matches, r.total_tokens);
      System.out.printf("%-30s %6.2f%% (%4d/%4d)  %6.2f%% (%4d/%4d)  %6.2f%% (%4d/%4d)\n",
          "Sentence Exact Match:",
          r.trained_sentence_exact_match_rate * 100.0, r.trained_sentence_exact_matches, r.total_sentences,
          r.model2_sentence_exact_match_rate * 100.0, r.model2_sentence_exact_matches, r.total_sentences,
          r.default_sentence_exact_match_rate * 100.0, r.default_sentence_exact_matches, r.total_sentences);
    } else {
      System.out.printf("%-32s %-22s %-20s\n", "Metric", r.model1_name, "Default Baseline");
      System.out.println("------------------------------------------------------------------------------------------");
      System.out.printf("%-32s %6.2f%% (%4d/%4d)      %6.2f%% (%4d/%4d)\n",
          "Ambiguity Accuracy (Hard):",
          r.trained_ambiguity_accuracy * 100.0, r.trained_ambiguous_matches, r.ambiguous_tokens,
          r.default_ambiguity_accuracy * 100.0, r.default_ambiguous_matches, r.ambiguous_tokens);
      System.out.printf("%-32s %6.2f%% (%4d/%4d)      %6.2f%% (%4d/%4d)\n",
          "Overall Token Accuracy:",
          r.trained_overall_accuracy * 100.0, r.trained_overall_matches, r.total_tokens,
          r.default_overall_accuracy * 100.0, r.default_overall_matches, r.total_tokens);
      System.out.printf("%-32s %6.2f%% (%4d/%4d)      %6.2f%% (%4d/%4d)\n",
          "Sentence Exact Match:",
          r.trained_sentence_exact_match_rate * 100.0, r.trained_sentence_exact_matches, r.total_sentences,
          r.default_sentence_exact_match_rate * 100.0, r.default_sentence_exact_matches, r.total_sentences);
    }
    System.out.println("==========================================================================================\n");
  }

  // --- Data Models ---

  public static class EvaluationReport {
    public String source_file;
    public String model1_name;
    public String model2_name;
    public boolean has_model2;
    public int total_sentences;
    public int total_tokens;
    public int ambiguous_tokens;

    // Primary trained model
    public int trained_overall_matches;
    public int trained_ambiguous_matches;
    public int trained_sentence_exact_matches;
    public double trained_overall_accuracy;
    public double trained_ambiguity_accuracy;
    public double trained_sentence_exact_match_rate;

    // Optional second trained model
    public int model2_overall_matches;
    public int model2_ambiguous_matches;
    public int model2_sentence_exact_matches;
    public double model2_overall_accuracy;
    public double model2_ambiguity_accuracy;
    public double model2_sentence_exact_match_rate;

    // Baseline model
    public int default_overall_matches;
    public int default_ambiguous_matches;
    public int default_sentence_exact_matches;
    public double default_overall_accuracy;
    public double default_ambiguity_accuracy;
    public double default_sentence_exact_match_rate;

    public List<SentenceEvaluationRecord> sentences = new ArrayList<>();
  }

  public static class SentenceEvaluationRecord {
    public String sentence_id;
    public String text;
    public List<TokenEvaluationRecord> tokens = new ArrayList<>();
  }

  public static class TokenEvaluationRecord {
    public int index;
    public String surface;
    public boolean is_ambiguous;
    public int candidate_count;
    public int gemini_selected_id;
    public String gemini_selected_key;
    public String gemini_pos;

    // Primary model
    public int trained_selected_id;
    public String trained_selected_key;
    public boolean trained_matches_gemini;

    // Optional second model
    public int model2_selected_id;
    public String model2_selected_key;
    public boolean model2_matches_gemini;

    // Baseline model
    public int default_selected_id;
    public String default_selected_key;
    public boolean default_matches_gemini;
  }
}

