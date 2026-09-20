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
import zemberek.morphology.ambiguity.prior.NGramPriorPerceptronResolver;
import zemberek.morphology.ambiguity.prior.NGramPriorPerceptronResolver.PriorDecodeResult;
import zemberek.morphology.ambiguity.prior.NGramPriorPerceptronResolver.PriorDecoder;
import zemberek.morphology.ambiguity.prior.NGramPriorPerceptronResolver.PriorFeatureExtractor;
import zemberek.morphology.ambiguity.prior.NGramPriorStore;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;

/**
 * Trains an Averaged Perceptron morphological disambiguation model with N-gram statistical priors.
 */
public class TrainNGramPriorAmbiguityModel extends ConsoleApp {

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
      names = {"--bigrams"},
      description = "Path to unambiguous bigrams text file (e.g. unambiguous_bigrams.txt)")
  public Path bigramsPath;

  @Parameter(
      names = {"--trigrams"},
      description = "Path to unambiguous trigrams text file (e.g. unambiguous_trigrams.txt)")
  public Path trigramsPath;

  @Parameter(
      names = {"--collocations"},
      description = "Path to significant collocations text file (e.g. significant-bigrams.txt)")
  public Path collocationsPath;

  @Parameter(
      names = {"--minCount"},
      description = "Minimum count threshold for including an N-gram. Default is 3.")
  public int minCount = 3;

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

  public static void main(String[] args) {
    new TrainNGramPriorAmbiguityModel().execute(args);
  }

  @Override
  public String description() {
    return "Trains an Averaged Perceptron morphological ambiguity resolver equipped with N-gram statistical priors.";
  }

  @Override
  public void run() throws Exception {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    // Load N-gram priors
    Log.info("Loading N-gram priors...");
    NGramPriorStore priorStore = NGramPriorStore.builder()
        .bigramsPath(bigramsPath)
        .trigramsPath(trigramsPath)
        .collocationsPath(collocationsPath)
        .minCount(minCount)
        .build();

    Log.info("Loaded priors: %d bigrams, %d trigrams, %d collocations.",
        priorStore.bigramSize(), priorStore.trigramSize(), priorStore.collocationSize());

    Log.info("Loading training dataset from: %s", trainPath);
    DataSet trainingSet = TrainAmbiguityModel.loadDataSet(trainPath, morphology);
    trainingSet.info();

    Path dev = devPath != null ? devPath : trainPath;
    Log.info("Loading development dataset from: %s", dev);
    DataSet devSet = TrainAmbiguityModel.loadDataSet(dev, morphology);
    devSet.info();

    NGramPriorTrainer trainer = new NGramPriorTrainer(priorStore, pruneWeight);
    Log.info("Starting N-gram prior-enhanced perceptron training (%d iterations)...", iterationCount);
    NGramPriorPerceptronResolver resolver = trainer.train(trainingSet, devSet, iterationCount);

    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }

    Weights finalWeights = trainer.getAveragedWeights();
    if (pruneWeight > 0) {
      Log.info("Pruning weights with absolute value <= %.4f...", pruneWeight);
      FloatValueMap<String> data = finalWeights.getData();
      for (String feat : data.getKeyList()) {
        if (Math.abs(data.get(feat)) <= pruneWeight) {
          data.remove(feat);
        }
      }
    }

    Log.info("Compressing and saving model to: %s", outputPath);
    CompressedWeights compressed = finalWeights.compress();
    compressed.serialize(outputPath);

    if (exportText) {
      Path textPath = outputPath.resolveSibling(outputPath.getFileName().toString().replaceAll("\\.bin$", ".txt"));
      Log.info("Exporting readable weights to: %s", textPath);
      finalWeights.saveAsText(textPath);
    }

    Log.info("Prior-enhanced training finished successfully. Feature count: %d", finalWeights.size());
  }

  // --- Internal Trainer Implementation ---

  public static class NGramPriorTrainer {
    private final NGramPriorStore priorStore;
    private final double pruneThreshold;
    private final Weights weights = new Weights();
    private final Weights averagedWeights = new Weights();
    private Weights bestAveragedWeights = null;
    private final IntValueMap<String> counts = new IntValueMap<>();

    public NGramPriorTrainer(NGramPriorStore priorStore, double pruneThreshold) {
      this.priorStore = priorStore;
      this.pruneThreshold = pruneThreshold;
    }

    public Weights getAveragedWeights() {
      return bestAveragedWeights != null ? bestAveragedWeights : averagedWeights;
    }

    public NGramPriorPerceptronResolver train(
        DataSet trainingSet,
        DataSet devSet,
        int iterationCount) {

      PriorFeatureExtractor extractor = new PriorFeatureExtractor(false, priorStore);
      PriorDecoder decoder = new PriorDecoder(weights, extractor);

      Weights bestWeights = null;
      double bestDevAccuracy = -1.0;
      int bestIteration = -1;

      int numExamples = 0;
      for (int it = 0; it < iterationCount; it++) {
        Log.info("Iteration: %d", it);
        trainingSet.shuffle();
        int sentenceIndex = 0;

        for (SentenceAnalysis sentence : trainingSet.sentences) {
          if (sentence.size() == 0) {
            continue;
          }
          numExamples++;
          sentenceIndex++;

          PriorDecodeResult result = decoder.bestPath(sentence.ambiguousAnalysis());
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
        double devAcc = test(devSet, new NGramPriorPerceptronResolver(averagedWeights, extractor, priorStore));
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

      PriorFeatureExtractor testExtractor = new PriorFeatureExtractor(true, priorStore);
      return new NGramPriorPerceptronResolver(bestAveragedWeights, testExtractor, priorStore);
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

    public static double test(DataSet set, NGramPriorPerceptronResolver disambiguator) {
      int hit = 0;
      int total = 0;
      for (SentenceAnalysis sentence : set.sentences) {
        if (sentence.size() == 0) {
          continue;
        }
        PriorDecodeResult result = disambiguator.getDecoder().bestPath(sentence.ambiguousAnalysis());
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
