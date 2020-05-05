package zemberek.morphology.ambiguity;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.Weights;
import zemberek.core.logging.Log;
import zemberek.core.text.TextConsumer;
import zemberek.core.text.TextIO;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver.Decoder;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver.FeatureExtractor;

import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver.DecodeResult;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

/**
 * This is the trainer class for the Turkish morphological ambiguity resolution mechanism. This
 * class generates the model for actual ambiguity resolution class {@link
 * PerceptronAmbiguityResolver}
 * <p>
 * Trainer uses text files for training. It parses them and then converts them to Morphological
 * analysis result using the Morphological analyzer.
 */
public class PerceptronAmbiguityResolverTrainer {

  private Weights weights = new Weights();
  private Weights averagedWeights = new Weights();
  private IntValueMap<String> counts = new IntValueMap<>();
  private TurkishMorphology analyzer;

  // during model updates, keys with lower than this value will be removed from the model.
  private double minPruneWeight = 0;

  PerceptronAmbiguityResolverTrainer(TurkishMorphology analyzer) {
    this.analyzer = analyzer;
  }

  PerceptronAmbiguityResolverTrainer(TurkishMorphology analyzer, double weightThreshold) {
    this.analyzer = analyzer;
    this.minPruneWeight = weightThreshold;
  }

  public PerceptronAmbiguityResolver train(
      DataSet trainingSet,
      DataSet devSet,
      int iterationCount) {

    FeatureExtractor extractor = new FeatureExtractor(false);
    Decoder decoder = new Decoder(weights, extractor);

    int numExamples = 0;
    for (int i = 0; i < iterationCount; i++) {
      Log.info("Iteration:" + i);
      trainingSet.shuffle();
      for (SentenceAnalysis sentence : trainingSet.sentences) {
        if (sentence.size() == 0) {
          continue;
        }
        numExamples++;
        DecodeResult result = decoder.bestPath(sentence.ambiguousAnalysis());
        if (sentence.bestAnalysis().equals(result.bestParse)) {
          continue;
        }
        if (sentence.bestAnalysis().size() != result.bestParse.size()) {
          throw new IllegalStateException(
              "Best parse result must have same amount of tokens with Correct parse." +
                  " \nCorrect = " + sentence.bestAnalysis() + " \nBest = " + result.bestParse);
        }

        IntValueMap<String> correctFeatures =
            extractor.extractFeatureCounts(sentence.bestAnalysis());
        IntValueMap<String> bestFeatures =
            extractor.extractFeatureCounts(result.bestParse);
        updateModel(correctFeatures, bestFeatures, numExamples);
      }

      for (String feat : averagedWeights) {
        updateAveragedWeights(feat, numExamples);
        counts.put(feat, numExamples);
      }

      Log.info("Testing development set.");
      PerceptronAmbiguityResolver disambiguator =
          new PerceptronAmbiguityResolver(averagedWeights, extractor);
      test(devSet, disambiguator);

    }
    return new PerceptronAmbiguityResolver(averagedWeights, new FeatureExtractor(false));
  }

  public PerceptronAmbiguityResolver train(Path trainFile, Path devFile, int iterationCount)
      throws IOException {
    DataSet trainingSet = DataSet.load(trainFile, analyzer);
    DataSet devSet = DataSet.load(devFile, analyzer);
    return train(trainingSet, devSet, iterationCount);
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

      weights.increment(
          feat,
          (correctFeatures.get(feat) - bestFeatures.get(feat)));

      counts.put(feat, numExamples);

      // reduce model by eliminating near zero weights.
      float wa = averagedWeights.get(feat);
      if (Math.abs(wa) <= minPruneWeight) {
        averagedWeights.getData().remove(feat);
      }
      float w = weights.get(feat);
      if (Math.abs(w) <= minPruneWeight) {
        weights.getData().remove(feat);
      }
    }
  }

  private void updateAveragedWeights(String feat, int numExamples) {
    int featureCount = counts.get(feat);
    float updatedWeight = (averagedWeights.get(feat) * featureCount
        + (numExamples - featureCount) * weights.get(feat))
        / numExamples;

    averagedWeights.put(
        feat,
        updatedWeight);
  }


  static class DataSet {

    List<SentenceAnalysis> sentences;
    Random rnd = new Random(0xbeef);

    public void shuffle() {
      Collections.shuffle(sentences, rnd);
    }

    public DataSet() {
      sentences = new ArrayList<>();
    }

    DataSet(List<SentenceAnalysis> sentences) {
      this.sentences = sentences;
    }

    void add(DataSet other) {
      this.sentences.addAll(other.sentences);
    }

    static DataSet load(Path path, TurkishMorphology analyzer) throws IOException {
      List<SentenceDataStr> sentencesFromTextFile = DataSet.loadTrainingDataText(path);
      return new DataSet(DataSet.convert(sentencesFromTextFile, analyzer));
    }

    static List<SentenceAnalysis> convert(
        List<SentenceDataStr> set,
        TurkishMorphology analyzer) {

      List<SentenceAnalysis> sentences = new ArrayList<>();
      // Find actual analysis equivalents.
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

    void info() {
      Log.info("There are %d sentences and %d tokens.",
          sentences.size(),
          sentences.stream().mapToInt(SentenceAnalysis::size).sum());

    }

    static List<SentenceDataStr> loadTrainingDataText(Path input)
        throws IOException {

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
          if (analysesFromLines.size() == 1) {
            selected = analysesFromLines.get(0);
            analysesFromLines.set(0, selected);
          } else {
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
            }
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

    SentenceDataStr(String sentence,
        List<WordDataStr> wordList) {
      this.sentence = sentence;
      this.wordList = wordList;
    }
  }

  static class WordDataStr {

    String word;
    String correctAnalysis;
    List<String> wordAnalysis;

    WordDataStr(String word, String correctAnalysis,
        List<String> wordAnalysis) {
      this.word = word;
      this.correctAnalysis = correctAnalysis;
      this.wordAnalysis = wordAnalysis;
    }
  }

  /**
   * For evaluating a test file.
   */
  public static void test(
      Path testFilePath,
      TurkishMorphology morphology,
      PerceptronAmbiguityResolver resolver) throws IOException {
    DataSet testSet = DataSet.load(testFilePath, morphology);
    test(testSet, resolver);
  }

  public static void test(DataSet set, PerceptronAmbiguityResolver resolver) {
    int hit = 0, total = 0;
    Stopwatch sw = Stopwatch.createStarted();
    for (SentenceAnalysis sentence : set.sentences) {
      DecodeResult result = resolver.getDecoder().bestPath(sentence.ambiguousAnalysis());
      int i = 0;
      List<SingleAnalysis> bestExpected = sentence.bestAnalysis();
      for (SingleAnalysis bestActual : result.bestParse) {
        if (bestExpected.get(i).equals(bestActual)) {
          hit++;
        }
        total++;
        i++;
      }
    }
    Log.info("Elapsed: " + sw.elapsed(TimeUnit.MILLISECONDS));
    Log.info(
        "Word count:" + total + " hit=" + hit + String.format(
            Locale.ENGLISH," Accuracy:%f", hit * 1.0 / total));
  }


}
