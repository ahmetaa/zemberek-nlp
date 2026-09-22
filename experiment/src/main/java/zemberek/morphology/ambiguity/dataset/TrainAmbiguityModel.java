package zemberek.morphology.ambiguity.dataset;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import zemberek.apps.ConsoleApp;
import zemberek.core.data.CompressedWeights;
import zemberek.core.data.WeightLookup;
import zemberek.core.data.Weights;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.CandidateRecord;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.SentenceRecord;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.TokenRecord;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

/**
 * Trains an Averaged Perceptron Morphological Disambiguator model from annotated datasets
 * (either in JSONL format from llm_disambiguator.py or Zemberek's native text format).
 */
public class TrainAmbiguityModel extends ConsoleApp {

  @Parameter(
      names = {"--train", "-t"},
      required = true,
      description = "Path to annotated training dataset (.jsonl or .txt)")
  public Path trainPath;

  @Parameter(
      names = {"--dev", "-d"},
      description = "Path to development dataset (.jsonl or .txt). If omitted, evaluates on training set.")
  public Path devPath;

  @Parameter(
      names = {"--output", "-o"},
      required = true,
      description = "Path to output binary compressed model (.bin)")
  public Path outputPath;

  @Parameter(
      names = {"--exportText"},
      description = "Also export uncompressed human-readable weights file (.txt)")
  public boolean exportText = false;

  @Parameter(
      names = {"--iterations", "-it"},
      description = "Number of perceptron training iterations (epochs). Default is 7.")
  public int iterationCount = 7;

  @Parameter(
      names = {"--pruneWeight", "-pw"},
      description = "Prune threshold for removing near-zero weights. Default is 0.0")
  public double pruneWeight = 0.0;

  @Parameter(
      names = {"--filterUnreachable", "-fu"},
      description = "Filter out sentences containing unreachable gold analyses. Default is false.")
  public boolean filterUnreachable = false;

  public static void main(String[] args) {
    new TrainAmbiguityModel().execute(args);
  }

  @Override
  public String description() {
    return "Trains an Averaged Perceptron morphological ambiguity resolver from annotated datasets.";
  }

  @Override
  public void run() throws Exception {
    Config config = new Config.Builder()
        .trainPath(trainPath)
        .devPath(devPath)
        .outputPath(outputPath)
        .exportText(exportText)
        .iterationCount(iterationCount)
        .pruneWeight(pruneWeight)
        .filterUnreachable(filterUnreachable)
        .build();

    TrainingResult result = train(config);
    Log.info("Training completed successfully.");
    Log.info("Output model size: %d features", result.featureCount);
    Log.info("Compressed model written to: %s", config.outputPath);
    if (config.exportText) {
      Log.info("Text weights written to: %s", config.getTextWeightsPath());
    }
  }

  public static TrainingResult train(Config config) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    Log.info("Loading training dataset from: %s", config.trainPath);
    DataSet trainingSet = loadDataSet(config.trainPath, morphology, config.filterUnreachable);
    trainingSet.info();

    Path dev = config.devPath != null ? config.devPath : config.trainPath;
    Log.info("Loading development dataset from: %s", dev);
    DataSet devSet = loadDataSet(dev, morphology, config.filterUnreachable);
    devSet.info();

    PerceptronAmbiguityResolverTrainer trainer =
        new PerceptronAmbiguityResolverTrainer(morphology, config.pruneWeight);

    Log.info("Starting perceptron training with %d iterations...", config.iterationCount);
    PerceptronAmbiguityResolver resolver = trainer.train(trainingSet, devSet, config.iterationCount);

    if (config.outputPath.getParent() != null) {
      Files.createDirectories(config.outputPath.getParent());
    }

    WeightLookup model = resolver.getModel();
    int featureCount = model.size();

    if (model instanceof Weights) {
      Weights weights = (Weights) model;
      CompressedWeights compressed = weights.compress();
      compressed.serialize(config.outputPath);

      if (config.exportText) {
        weights.saveAsText(config.getTextWeightsPath());
      }
    } else {
      throw new IllegalStateException("Model is not of type Weights: " + model.getClass().getName());
    }

    // Run final evaluation on dev set
    Log.info("Final evaluation on development set:");
    PerceptronAmbiguityResolverTrainer.test(devSet, resolver);

    return new TrainingResult(resolver, featureCount, config.outputPath);
  }

  /**
   * Loads a dataset either directly from JSONL in-memory or from Zemberek's native text format.
   */
  public static DataSet loadDataSet(Path path, TurkishMorphology morphology) throws IOException {
    return loadDataSet(path, morphology, false);
  }

  public static DataSet loadDataSet(Path path, TurkishMorphology morphology, boolean filterUnreachable) throws IOException {
    if (path.toString().endsWith(".jsonl")) {
      return loadDataSetFromJsonl(path, morphology, filterUnreachable);
    }
    return DataSet.load(path, morphology);
  }

  public static DataSet loadDataSetFromJsonl(Path jsonlPath, TurkishMorphology morphology) throws IOException {
    return loadDataSetFromJsonl(jsonlPath, morphology, false);
  }

  /**
   * Reads an annotated JSONL file directly into a DataSet without writing temporary text files to disk.
   */
  public static DataSet loadDataSetFromJsonl(Path jsonlPath, TurkishMorphology morphology, boolean filterUnreachable) throws IOException {
    Gson gson = new Gson();
    List<SentenceAnalysis> sentences = new ArrayList<>();
    int totalTokens = 0;
    int totalUnreachableTokens = 0;
    int totalUnreachableSentences = 0;

    try (BufferedReader reader = Files.newBufferedReader(jsonlPath, StandardCharsets.UTF_8)) {
      String line;
      int lineNum = 0;
      while ((line = reader.readLine()) != null) {
        lineNum++;
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }

        SentenceRecord record;
        try {
          record = gson.fromJson(line, SentenceRecord.class);
        } catch (Exception e) {
          Log.warn("Failed to parse JSON on line %d of %s: %s", lineNum, jsonlPath, e.getMessage());
          continue;
        }

        if (record == null || record.text == null || record.tokens == null || record.tokens.isEmpty()) {
          continue;
        }

        List<WordAnalysis> wordAnalyses = morphology.analyzeSentence(record.text);
        if (wordAnalyses.size() != record.tokens.size()) {
          Log.warn("Sentence [%s] at line %d token size mismatch (analyzer=%d, json=%d). Skipping.",
              record.text, lineNum, wordAnalyses.size(), record.tokens.size());
          continue;
        }

        List<SentenceWordAnalysis> unambigiousAnalyses = new ArrayList<>(wordAnalyses.size());
        boolean sentenceValid = true;
        int unreachableInSentence = 0;

        for (int i = 0; i < wordAnalyses.size(); i++) {
          totalTokens++;
          WordAnalysis wa = wordAnalyses.get(i);
          TokenRecord tr = record.tokens.get(i);

          if (tr.candidates == null || tr.candidates.isEmpty()) {
            sentenceValid = false;
            break;
          }

          int selectedId = 0;
          if (tr.is_ambiguous) {
            if (tr.selected_candidate_id != null) {
              selectedId = tr.selected_candidate_id;
            } else {
              selectedId = extractSelectedCandidateId(line, tr.index);
            }
          }

          CandidateRecord chosenCandidate = null;
          for (CandidateRecord cr : tr.candidates) {
            if (cr.id == selectedId) {
              chosenCandidate = cr;
              break;
            }
          }
          if (chosenCandidate == null) {
            chosenCandidate = tr.candidates.get(0);
          }

          SingleAnalysis matchedAnalysis = null;
          if (chosenCandidate.zemberek_key != null) {
            for (SingleAnalysis sa : wa) {
              if (sa.formatLong().equals(chosenCandidate.zemberek_key)) {
                matchedAnalysis = sa;
                break;
              }
            }
          }

          if (matchedAnalysis == null) {
            unreachableInSentence++;
            totalUnreachableTokens++;
            if (filterUnreachable) {
              sentenceValid = false;
              break;
            }
            if (selectedId >= 0 && selectedId < wa.analysisCount()) {
              matchedAnalysis = wa.getAnalysisResults().get(selectedId);
            } else if (wa.analysisCount() > 0) {
              matchedAnalysis = wa.getAnalysisResults().get(0);
            }
          }

          if (matchedAnalysis != null) {
            unambigiousAnalyses.add(new SentenceWordAnalysis(matchedAnalysis, wa));
          } else {
            sentenceValid = false;
            break;
          }
        }

        if (unreachableInSentence > 0) {
          totalUnreachableSentences++;
        }

        if (sentenceValid && unambigiousAnalyses.size() == record.tokens.size()) {
          sentences.add(new SentenceAnalysis(record.text, unambigiousAnalyses));
        }
      }
    }

    double unreachableRate = totalTokens > 0 ? (100.0 * totalUnreachableTokens / totalTokens) : 0.0;
    Log.info("Loaded %d sentences directly from JSONL: %s (Unreachable gold parses: %d/%d tokens [%.2f%%] across %d sentences%s)",
        sentences.size(), jsonlPath, totalUnreachableTokens, totalTokens, unreachableRate, totalUnreachableSentences,
        filterUnreachable ? " - filtered out" : "");
    return new DataSet(sentences);
  }

  /**
   * Converts a JSONL file to Zemberek training text format if needed.
   * If the input is already a .txt file, returns it as-is.
   * @deprecated Use {@link #loadDataSet(Path, TurkishMorphology)} for direct in-memory loading.
   */
  @Deprecated
  public static Path ensureTextFormat(Path path) throws IOException {
    if (path.toString().endsWith(".jsonl")) {
      Path txtPath = Paths.get(path.toString().substring(0, path.toString().length() - 6) + ".zemberek.txt");
      convertJsonlToText(path, txtPath);
      return txtPath;
    }
    return path;
  }

  public static void convertJsonlToText(Path jsonlPath, Path textPath) throws IOException {
    Gson gson = new Gson();
    try (BufferedReader reader = Files.newBufferedReader(jsonlPath, StandardCharsets.UTF_8);
         BufferedWriter writer = Files.newBufferedWriter(textPath, StandardCharsets.UTF_8)) {

      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }

        SentenceRecord record = gson.fromJson(line, SentenceRecord.class);
        if (record == null || record.text == null || record.tokens == null || record.tokens.isEmpty()) {
          continue;
        }

        writer.write("S:" + record.text);
        writer.newLine();

        for (TokenRecord token : record.tokens) {
          writer.write(token.surface);
          writer.newLine();

          if (token.candidates == null || token.candidates.isEmpty()) {
            continue;
          }

          int selectedId = 0;
          if (token.is_ambiguous) {
            // Find selected_candidate_id if present via JsonObject or default to 0
            selectedId = extractSelectedCandidateId(line, token.index);
          }

          for (CandidateRecord c : token.candidates) {
            if (c.zemberek_key == null) {
              continue;
            }
            String star = (token.is_ambiguous && c.id == selectedId) ? "*" : "";
            writer.write(c.zemberek_key + star);
            writer.newLine();
          }
        }
      }
    }
  }

  private static int extractSelectedCandidateId(String jsonLine, int tokenIndex) {
    try {
      com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(jsonLine).getAsJsonObject();
      com.google.gson.JsonArray tokens = obj.getAsJsonArray("tokens");
      if (tokens != null && tokenIndex < tokens.size()) {
        com.google.gson.JsonObject t = tokens.get(tokenIndex).getAsJsonObject();
        if (t.has("selected_candidate_id")) {
          return t.get("selected_candidate_id").getAsInt();
        }
      }
    } catch (Exception ignored) {
    }
    return 0;
  }

  // --- Configuration and Result Models ---

  public static class Config {
    public final Path trainPath;
    public final Path devPath;
    public final Path outputPath;
    public final boolean exportText;
    public final int iterationCount;
    public final double pruneWeight;
    public final boolean filterUnreachable;

    private Config(Builder b) {
      this.trainPath = b.trainPath;
      this.devPath = b.devPath;
      this.outputPath = b.outputPath;
      this.exportText = b.exportText;
      this.iterationCount = b.iterationCount;
      this.pruneWeight = b.pruneWeight;
      this.filterUnreachable = b.filterUnreachable;
    }

    public Path getTextWeightsPath() {
      String outStr = outputPath.toString();
      if (outStr.endsWith(".bin")) {
        return Paths.get(outStr.substring(0, outStr.length() - 4) + ".txt");
      }
      return Paths.get(outStr + ".txt");
    }

    public static class Builder {
      Path trainPath;
      Path devPath;
      Path outputPath;
      boolean exportText = false;
      int iterationCount = 7;
      double pruneWeight = 0.0;
      boolean filterUnreachable = false;

      public Builder trainPath(Path trainPath) {
        this.trainPath = trainPath;
        return this;
      }

      public Builder devPath(Path devPath) {
        this.devPath = devPath;
        return this;
      }

      public Builder outputPath(Path outputPath) {
        this.outputPath = outputPath;
        return this;
      }

      public Builder exportText(boolean exportText) {
        this.exportText = exportText;
        return this;
      }

      public Builder iterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
        return this;
      }

      public Builder pruneWeight(double pruneWeight) {
        this.pruneWeight = pruneWeight;
        return this;
      }

      public Builder filterUnreachable(boolean filterUnreachable) {
        this.filterUnreachable = filterUnreachable;
        return this;
      }

      public Config build() {
        if (trainPath == null) {
          throw new IllegalArgumentException("trainPath must be set");
        }
        if (outputPath == null) {
          throw new IllegalArgumentException("outputPath must be set");
        }
        return new Config(this);
      }
    }
  }

  public static class TrainingResult {
    public final PerceptronAmbiguityResolver resolver;
    public final int featureCount;
    public final Path modelPath;

    public TrainingResult(PerceptronAmbiguityResolver resolver, int featureCount, Path modelPath) {
      this.resolver = resolver;
      this.featureCount = featureCount;
      this.modelPath = modelPath;
    }
  }
}
