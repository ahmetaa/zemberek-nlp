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

    Path effectiveTrainTxt = ensureTextFormat(config.trainPath);
    Path effectiveDevTxt = config.devPath != null
        ? ensureTextFormat(config.devPath)
        : effectiveTrainTxt;

    Log.info("Loading training dataset from: %s", effectiveTrainTxt);
    DataSet trainingSet = DataSet.load(effectiveTrainTxt, morphology);
    trainingSet.info();

    Log.info("Loading development dataset from: %s", effectiveDevTxt);
    DataSet devSet = DataSet.load(effectiveDevTxt, morphology);
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
   * Converts a JSONL file to Zemberek training text format if needed.
   * If the input is already a .txt file, returns it as-is.
   */
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

    private Config(Builder b) {
      this.trainPath = b.trainPath;
      this.devPath = b.devPath;
      this.outputPath = b.outputPath;
      this.exportText = b.exportText;
      this.iterationCount = b.iterationCount;
      this.pruneWeight = b.pruneWeight;
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
