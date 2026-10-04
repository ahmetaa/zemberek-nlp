package zemberek.morphology.ambiguity.fast;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.CompressedWeights;
import zemberek.core.data.Weights;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastDecodeResult;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastDecoder;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastFeatureExtractor;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;

/**
 * Fast Averaged Perceptron trainer for {@link FastPerceptronAmbiguityResolver}.
 * Implements Collins (2002) averaged perceptron with lazy-averaging feature updates.
 */
public class FastPerceptronAmbiguityResolverTrainer {

  private final double pruneThreshold;
  private final Weights weights = new Weights();
  private final Weights averagedWeights = new Weights();
  private final IntValueMap<String> counts = new IntValueMap<>();

  public FastPerceptronAmbiguityResolverTrainer() {
    this(0.0);
  }

  public FastPerceptronAmbiguityResolverTrainer(double pruneThreshold) {
    this.pruneThreshold = pruneThreshold;
  }

  public Weights getAveragedWeights() {
    return averagedWeights;
  }

  public Weights getWeights() {
    return weights;
  }

  /**
   * Trains a FastPerceptronAmbiguityResolver without a development set.
   */
  public FastPerceptronAmbiguityResolver train(DataSet trainingSet, int iterationCount) {
    return train(trainingSet, null, iterationCount);
  }

  /**
   * Trains a FastPerceptronAmbiguityResolver with a development set evaluated after each iteration.
   */
  public FastPerceptronAmbiguityResolver train(
      DataSet trainingSet,
      DataSet devSet,
      int iterationCount) {

    FastFeatureExtractor extractor = new FastFeatureExtractor(true);
    FastDecoder decoder = new FastDecoder(weights, extractor);

    int numExamples = 0;
    for (int it = 0; it < iterationCount; it++) {
      Log.info("Iteration: %d", it);
      trainingSet.shuffle();

      for (SentenceAnalysis sentence : trainingSet.sentences) {
        if (sentence.size() == 0) {
          continue;
        }
        numExamples++;

        FastDecodeResult result = decoder.bestPath(sentence.ambiguousAnalysis());
        if (sentence.bestAnalysis().equals(result.bestParse)) {
          continue;
        }
        if (sentence.bestAnalysis().size() != result.bestParse.size()) {
          throw new IllegalStateException(
              "Best parse result must have same amount of tokens as correct parse." +
                  " \nCorrect = " + sentence.bestAnalysis() + " \nBest = " + result.bestParse);
        }

        IntValueMap<String> correctFeatures =
            extractor.extractFeatureCounts(sentence.bestAnalysis());
        IntValueMap<String> predictedFeatures =
            extractor.extractFeatureCounts(result.bestParse);

        updateModel(correctFeatures, predictedFeatures, numExamples);
      }

      // Finalize running averaged weights for all observed features at epoch boundary
      for (String feat : counts) {
        updateAveragedWeights(feat, numExamples);
        counts.put(feat, numExamples);
      }

      if (devSet != null) {
        Log.info("Testing on development set after iteration %d...", it);
        test(devSet, new FastPerceptronAmbiguityResolver(averagedWeights, extractor));
      }
    }

    // Post-training weight pruning
    averagedWeights.pruneNearZeroWeights();
    if (pruneThreshold > 0) {
      FloatValueMap<String> data = averagedWeights.getData();
      for (String feat : data.getKeyList()) {
        if (Math.abs(data.get(feat)) <= pruneThreshold) {
          data.remove(feat);
        }
      }
    }

    FastFeatureExtractor testExtractor = new FastFeatureExtractor(false);
    return new FastPerceptronAmbiguityResolver(averagedWeights, testExtractor);
  }

  /**
   * Trains from text training and development files.
   */
  public FastPerceptronAmbiguityResolver train(
      Path trainFile,
      Path devFile,
      TurkishMorphology morphology,
      int iterationCount) throws IOException {
    DataSet trainingSet = DataSet.load(trainFile, morphology);
    DataSet devSet = devFile != null ? DataSet.load(devFile, morphology) : null;
    return train(trainingSet, devSet, iterationCount);
  }

  private void updateModel(
      IntValueMap<String> correctFeatures,
      IntValueMap<String> predictedFeatures,
      int numExamples) {
    Set<String> keySet = Sets.newHashSet();
    keySet.addAll(correctFeatures.getKeyList());
    keySet.addAll(predictedFeatures.getKeyList());

    for (String feat : keySet) {
      updateAveragedWeights(feat, numExamples);
      weights.increment(feat, (correctFeatures.get(feat) - predictedFeatures.get(feat)));
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

  /**
   * Evaluates a model on a given dataset and returns token accuracy.
   */
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

  public static double test(
      Path testFilePath,
      TurkishMorphology morphology,
      FastPerceptronAmbiguityResolver resolver) throws IOException {
    DataSet testSet = DataSet.load(testFilePath, morphology);
    return test(testSet, resolver);
  }

  /**
   * Serializes the trained averaged weights to disk as compressed binary, and optionally as text.
   */
  public void saveModel(Path outputPath, boolean exportText) throws IOException {
    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }
    Log.info("Saving compressed model to: %s", outputPath);
    CompressedWeights compressedWeights = averagedWeights.compress();
    compressedWeights.serialize(outputPath);

    if (exportText) {
      Path textPath = outputPath.resolveSibling(
          outputPath.getFileName().toString().replace(".bin", ".txt"));
      Log.info("Exporting readable weights to: %s", textPath);
      averagedWeights.saveAsText(textPath);
    }
    Log.info("Model saved successfully. Feature count: %d", averagedWeights.size());
  }
}

