package zemberek.morphology.ambiguity.fast;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.CompressedWeights;
import zemberek.core.data.Weights;
import zemberek.core.logging.Log;
import zemberek.core.text.TextConsumer;
import zemberek.core.text.TextIO;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastDecodeResult;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastDecoder;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastFeatureExtractor;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

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
        Log.info("Evaluating on development set after iteration %d...", it);
        evaluateAccuracy(devSet, new FastPerceptronAmbiguityResolver(averagedWeights, extractor));
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
  public static double evaluateAccuracy(DataSet set, FastPerceptronAmbiguityResolver disambiguator) {
    int hit = 0;
    int total = 0;
    for (SentenceAnalysis sentence : set.sentences) {
      if (sentence.size() == 0) {
        continue;
      }
      FastDecodeResult result = disambiguator.getDecoder().bestPath(sentence.ambiguousAnalysis());
      List<SingleAnalysis> bestExpected = sentence.bestAnalysis();
      int tokenCount = Math.min(bestExpected.size(), result.bestParse.size());
      for (int i = 0; i < tokenCount; i++) {
        if (bestExpected.get(i).equals(result.bestParse.get(i))) {
          hit++;
        }
      }
      total += Math.max(bestExpected.size(), result.bestParse.size());
    }
    double acc = total > 0 ? (double) hit / total : 0.0;
    Log.info("Token Accuracy: %.2f%% (%d / %d)", (100.0 * acc), hit, total);
    return acc;
  }

  public static double evaluateAccuracy(
      Path testFilePath,
      TurkishMorphology morphology,
      FastPerceptronAmbiguityResolver resolver) throws IOException {
    DataSet testSet = DataSet.load(testFilePath, morphology);
    return evaluateAccuracy(testSet, resolver);
  }

  /**
   * @deprecated Use {@link #evaluateAccuracy(DataSet, FastPerceptronAmbiguityResolver)} instead.
   */
  @Deprecated
  public static double test(DataSet set, FastPerceptronAmbiguityResolver disambiguator) {
    return evaluateAccuracy(set, disambiguator);
  }

  /**
   * @deprecated Use {@link #evaluateAccuracy(Path, TurkishMorphology, FastPerceptronAmbiguityResolver)} instead.
   */
  @Deprecated
  public static double test(
      Path testFilePath,
      TurkishMorphology morphology,
      FastPerceptronAmbiguityResolver resolver) throws IOException {
    return evaluateAccuracy(testFilePath, morphology, resolver);
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

  public static class DataSet {

    public List<SentenceAnalysis> sentences;
    Random rnd = new Random(0xbeef);

    public void shuffle() {
      Collections.shuffle(sentences, rnd);
    }

    public DataSet() {
      sentences = new ArrayList<>();
    }

    public DataSet(List<SentenceAnalysis> sentences) {
      this.sentences = sentences;
    }

    public void add(DataSet other) {
      this.sentences.addAll(other.sentences);
    }

    public static DataSet load(Path path, TurkishMorphology analyzer) throws IOException {
      List<SentenceDataStr> sentencesFromTextFile = DataSet.loadTrainingDataText(path);
      return new DataSet(DataSet.convert(sentencesFromTextFile, analyzer));
    }

    static List<SentenceAnalysis> convert(
        List<SentenceDataStr> set,
        TurkishMorphology analyzer) {

      List<SentenceAnalysis> sentences = new ArrayList<>();
      for (SentenceDataStr sentenceFromTrain : set) {
        String sentence = sentenceFromTrain.sentence;
        List<WordAnalysis> sentenceAnalysis = analyzer.analyzeSentence(sentence);

        if (sentenceAnalysis.size() != sentenceFromTrain.wordList.size()) {
          Log.warn("Actual analysis token size [%d] and sentence from file token size [%d] "
                  + "does not match for sentence [%s]",
              sentenceAnalysis.size(), sentenceFromTrain.wordList.size(), sentence);
        }

        List<SentenceWordAnalysis> unambigiousAnalyses = new ArrayList<>();

        BAD_SENTENCE:
        for (int i = 0; i < sentenceAnalysis.size(); i++) {
          WordAnalysis w = sentenceAnalysis.get(i);

          Map<String, SingleAnalysis> analysisMap = new HashMap<>();
          for (SingleAnalysis single : w) {
            analysisMap.put(single.formatLong(), single);
          }

          WordDataStr s = sentenceFromTrain.wordList.get(i);

          if (!w.getInput().equals(s.word)) {
            Log.warn(
                "Actual analysis token [%s] at index [%d] is different than word from training [%s] "
                    + " for sentence [%s]", w.getInput(), i, s.word, sentence);
          }

          if (w.analysisCount() != s.wordAnalysis.size()) {
            Log.warn(
                "Actual analysis token [%s] has [%d] analyses but word from training file has [%d] "
                    + " analyses for sentence [%s]",
                w.getInput(), w.analysisCount(), s.wordAnalysis.size(), sentence);
            break;
          }

          for (String analysis : s.wordAnalysis) {
            if (!analysisMap.containsKey(analysis)) {
              Log.warn(
                  "Anaysis [%s] from training set cannot be generated by Analyzer for sentence "
                      + " [%s]. Skipping sentence.", analysis, sentence);
              break BAD_SENTENCE;
            }
          }

          if (analysisMap.containsKey(s.correctAnalysis)) {
            SingleAnalysis correct = analysisMap.get(s.correctAnalysis);
            unambigiousAnalyses.add(new SentenceWordAnalysis(correct, w));
          } else {
            break;
          }
        }

        if (unambigiousAnalyses.size() == sentenceFromTrain.wordList.size()) {
          sentences.add(new SentenceAnalysis(sentence, unambigiousAnalyses));
        }
      }
      return sentences;
    }

    public void info() {
      Log.info("There are %d sentences and %d tokens.",
          sentences.size(),
          sentences.stream().mapToInt(SentenceAnalysis::size).sum());
    }

    static List<SentenceDataStr> loadTrainingDataText(Path input) throws IOException {
      List<String> allLines = TextIO.loadLines(input);
      List<SentenceDataStr> set = new ArrayList<>();

      TextConsumer tc = new TextConsumer(allLines);
      while (!tc.finished()) {
        List<String> sentenceData = new ArrayList<>();
        sentenceData.add(tc.current());
        tc.advance();
        sentenceData.addAll(tc.moveUntil(s -> s.startsWith("S:")));

        List<WordDataStr> wordDataStrList = new ArrayList<>();
        TextConsumer tw = new TextConsumer(sentenceData);
        String sentence = tw.getAndAdvance().substring(2);

        boolean ignoreSentence = false;
        while (!tw.finished()) {
          String word = tw.getAndAdvance();
          List<String> analysesFromLines = tw.moveUntil(s -> !s.startsWith("["));
          analysesFromLines = analysesFromLines
              .stream()
              .map(s -> s.endsWith("-") ? s.substring(0, s.length() - 1) : s)
              .collect(Collectors.toList());

          String selected = null;
          int i = 0;
          int index = -1;
          for (String s : analysesFromLines) {
            if (s.endsWith("*")) {
              selected = s.substring(0, s.length() - 1);
              index = i;
              break;
            }
            i++;
          }
          if (index >= 0) {
            analysesFromLines.set(index, selected);
          } else if (analysesFromLines.size() == 1) {
            selected = analysesFromLines.get(0);
          }

          WordDataStr w = new WordDataStr(word, selected, analysesFromLines);
          if (w.correctAnalysis == null) {
            Log.warn("Sentence [%s] contains ambiguous analysis for word %s. It will be ignored.",
                sentence, word);
            ignoreSentence = true;
            break;
          } else {
            wordDataStrList.add(w);
          }
        }

        if (!ignoreSentence) {
          set.add(new SentenceDataStr(sentence, wordDataStrList));
        }
      }
      Log.info("There are %d sentences and %d tokens in %s.",
          set.size(),
          set.stream().mapToInt(s -> s.wordList.size()).sum(),
          input);
      return set;
    }
  }

  static class SentenceDataStr {

    String sentence;
    List<WordDataStr> wordList;

    SentenceDataStr(String sentence, List<WordDataStr> wordList) {
      this.sentence = sentence;
      this.wordList = wordList;
    }
  }

  static class WordDataStr {

    String word;
    String correctAnalysis;
    List<String> wordAnalysis;

    WordDataStr(String word, String correctAnalysis, List<String> wordAnalysis) {
      this.word = word;
      this.correctAnalysis = correctAnalysis;
      this.wordAnalysis = wordAnalysis;
    }
  }
}

