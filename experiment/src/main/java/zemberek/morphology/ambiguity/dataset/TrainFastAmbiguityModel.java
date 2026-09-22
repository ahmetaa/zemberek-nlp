package zemberek.morphology.ambiguity.dataset;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import zemberek.apps.ConsoleApp;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.CompressedWeights;
import zemberek.core.data.Weights;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastDecodeResult;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastDecoder;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastFeatureExtractor;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;

/**
 * Trains a Fast Averaged Perceptron morphological disambiguation model.
 */
public class TrainFastAmbiguityModel extends ConsoleApp {

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
      names = {"--iterations", "-it"},
      description = "Number of perceptron training iterations (epochs). Default is 7.")
  public int iterationCount = 7;

  @Parameter(
      names = {"--pruneWeight", "-pw"},
      description = "Prune threshold for removing near-zero weights. Default is 0.0")
  public double pruneWeight = 0.0;

  @Parameter(
      names = {"--exportText"},
      description = "Also export uncompressed human-readable weights file (.txt)")
  public boolean exportText = false;

  @Parameter(
      names = {"--filterUnreachable", "-fu"},
      description = "Filter out sentences containing unreachable gold analyses. Default is true.")
  public boolean filterUnreachable = true;

  public static void main(String[] args) {
    new TrainFastAmbiguityModel().execute(args);
  }

  @Override
  public String description() {
    return "Trains a Fast Averaged Perceptron morphological ambiguity resolver.";
  }

  @Override
  public void run() throws Exception {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    Log.info("Loading training dataset from: %s", trainPath);
    DataSet trainingSet = TrainAmbiguityModel.loadDataSet(trainPath, morphology, filterUnreachable);
    trainingSet.info();

    Path dev = devPath != null ? devPath : trainPath;
    Log.info("Loading development dataset from: %s", dev);
    DataSet devSet = TrainAmbiguityModel.loadDataSet(dev, morphology, filterUnreachable);
    devSet.info();

    FastPerceptronTrainer trainer = new FastPerceptronTrainer(pruneWeight);
    Log.info("Starting perceptron training (%d iterations)...", iterationCount);
    FastPerceptronAmbiguityResolver resolver = trainer.train(trainingSet, devSet, iterationCount);

    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }

    Weights finalWeights = trainer.getAveragedWeights();
    Log.info("Pruning exact and near-zero weights (|w| <= 0.0001)...");
    finalWeights.pruneNearZeroWeights();

    if (pruneWeight > 0) {
      Log.info("Pruning weights with absolute value <= %.4f...", pruneWeight);
      FloatValueMap<String> data = finalWeights.getData();
      for (String feat : data.getKeyList()) {
        if (Math.abs(data.get(feat)) <= pruneWeight) {
          data.remove(feat);
        }
      }
    }

    Log.info("Saving compressed model to: %s", outputPath);
    CompressedWeights compressedWeights = finalWeights.compress();
    compressedWeights.serialize(outputPath);

    if (exportText) {
      Path textPath = outputPath.resolveSibling(outputPath.getFileName().toString().replace(".bin", ".txt"));
      Log.info("Exporting readable weights to: %s", textPath);
      finalWeights.saveAsText(textPath);
    }

    Log.info("Training finished successfully. Feature count: %d", finalWeights.size());
  }

  // --- Internal Trainer Implementation ---

  public static class FastPerceptronTrainer {
    private final double pruneThreshold;
    private final Weights weights = new Weights();
    private final Weights averagedWeights = new Weights();
    private Weights bestAveragedWeights = null;
    private final IntValueMap<String> counts = new IntValueMap<>();

    public FastPerceptronTrainer(double pruneThreshold) {
      this.pruneThreshold = pruneThreshold;
    }

    public Weights getAveragedWeights() {
      return bestAveragedWeights != null ? bestAveragedWeights : averagedWeights;
    }

    public FastPerceptronAmbiguityResolver train(
        DataSet trainingSet,
        DataSet devSet,
        int iterationCount) {

      FastFeatureExtractor extractor = new FastFeatureExtractor(true);
      FastDecoder decoder = new FastDecoder(weights, extractor);

      Weights bestWeights = null;
      double bestDevAccuracy = -1.0;
      int bestIteration = -1;

      int numExamples = 0;
      for (int it = 0; it < iterationCount; it++) {
        Log.info("Iteration: %d", it);
        trainingSet.shuffle();
        decoder = new FastDecoder(weights, extractor);
        int sentenceIndex = 0;

        for (SentenceAnalysis sentence : trainingSet.sentences) {
          if (sentence.size() == 0) {
            continue;
          }
          numExamples++;
          sentenceIndex++;

          FastDecodeResult result = decoder.bestPath(sentence.ambiguousAnalysis());
          if (sentence.bestAnalysis().equals(result.bestParse)) {
            continue;
          }

          IntValueMap<String> correctFeatures =
              extractor.extractFeatureCounts(sentence.bestAnalysis());
          IntValueMap<String> bestFeatures =
              extractor.extractFeatureCounts(result.bestParse);

          updateModel(correctFeatures, bestFeatures, numExamples);
        }

        for (String feat : counts) {
          updateAveragedWeights(feat, numExamples);
          counts.put(feat, numExamples);
        }

        Log.info("Testing on development set after iteration %d...", it);
        double devAcc = test(devSet, new FastPerceptronAmbiguityResolver(averagedWeights, extractor));
        if (devAcc > bestDevAccuracy) {
          bestDevAccuracy = devAcc;
          bestIteration = it;
          bestWeights = averagedWeights.copy();
          Log.info("-> Checkpointed new best dev model (accuracy: %.2f%% at iteration %d)", devAcc * 100.0, it);
        }
      }

      this.bestAveragedWeights = (bestWeights != null) ? bestWeights : averagedWeights;
      Log.info("Selected optimal model from iteration %d with dev token accuracy %.2f%%",
          bestIteration, bestDevAccuracy * 100.0);

      FastFeatureExtractor testExtractor = new FastFeatureExtractor(false);
      return new FastPerceptronAmbiguityResolver(bestAveragedWeights, testExtractor);
    }

    private void updateModel(
        IntValueMap<String> correctFeatures,
        IntValueMap<String> bestFeatures,
        int numExamples) {
      Set<String> keySet = Sets.newHashSet();
      keySet.addAll(correctFeatures.getKeyList());
      keySet.addAll(bestFeatures.getKeyList());

      for (String feat : keySet) {
        updateAveragedWeights(feat, numExamples);
        weights.increment(feat, (correctFeatures.get(feat) - bestFeatures.get(feat)));
        counts.put(feat, numExamples);
      }
    }

    private void updateAveragedWeights(String feat, int numExamples) {
      int featureCount = counts.get(feat);
      float updatedWeight = (averagedWeights.get(feat) * featureCount
          + (numExamples - featureCount) * weights.get(feat))
          / numExamples;
      averagedWeights.put(feat, updatedWeight);
    }

    public static double test(DataSet set, FastPerceptronAmbiguityResolver disambiguator) {
      int hit = 0;
      int total = 0;
      for (SentenceAnalysis sentence : set.sentences) {
        if (sentence.size() == 0) {
          continue;
        }
        FastDecodeResult result = disambiguator.getDecoder().bestPath(sentence.ambiguousAnalysis());
        List<SingleAnalysis> bestExpected = sentence.bestAnalysis();
        for (int i = 0; i < result.bestParse.size(); i++) {
          if (bestExpected.get(i).equals(result.bestParse.get(i))) {
            hit++;
          }
          total++;
        }
      }
      double acc = total > 0 ? (double) hit / total : 0.0;
      Log.info("Dev Token Accuracy: %.2f%% (%d / %d)", (100.0 * acc), hit, total);
      return acc;
    }
  }
}
