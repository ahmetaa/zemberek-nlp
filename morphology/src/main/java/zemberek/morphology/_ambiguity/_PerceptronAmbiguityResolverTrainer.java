package zemberek.morphology._ambiguity;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import zemberek.core.collections.IntValueMap;
import zemberek.core.logging.Log;
import zemberek.core.text.TextConsumer;
import zemberek.core.text.TextIO;
import zemberek.morphology._ambiguity._PerceptronAmbiguityResolver.Decoder;
import zemberek.morphology._ambiguity._PerceptronAmbiguityResolver.FeatureExtractor;
import zemberek.morphology._ambiguity._PerceptronAmbiguityResolver.Model;
import zemberek.morphology._ambiguity._PerceptronAmbiguityResolver.ParseResult;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._SentenceWordAnalysis;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology._analyzer._WordAnalysis;

public class _PerceptronAmbiguityResolverTrainer {


  Model weights = new Model();
  Model averagedWeights = new Model();
  IntValueMap<String> counts = new IntValueMap<>();
  _TurkishMorphologicalAnalyzer analyzer;

  _PerceptronAmbiguityResolverTrainer(_TurkishMorphologicalAnalyzer analyzer) {
    this.analyzer = analyzer;
  }

  public _PerceptronAmbiguityResolver train(Path trainFile, Path devFile)
      throws IOException {

    FeatureExtractor extractor = new FeatureExtractor(true);
    Decoder decoder = new Decoder(weights, extractor);

    DataSet trainingSet = DataSet.load(trainFile, analyzer);
    DataSet devSet = DataSet.load(devFile, analyzer);
    int numExamples = 0;
    for (int i = 0; i < 4; i++) {
      Log.info("Iteration:" + i);
      for (_SentenceAnalysis sentence : trainingSet.sentences) {
        if (sentence.size() == 0) {
          continue;
        }
        numExamples++;
        ParseResult result = decoder.bestPath(sentence.allAnalyses());
        if (sentence.bestAnalysis().equals(result.bestParse)) {
          continue;
        }
        if (sentence.bestAnalysis().size() != result.bestParse.size()) {
          throw new IllegalStateException(
              "Best parse result must have same amount of tokens with Correct parse." +
                  " \nCorrect = " + sentence.bestAnalysis() + " \nBest = " + result.bestParse);
        }

        IntValueMap<String> correctFeatures =
            extractor.extractFromSentence(sentence.bestAnalysis());
        IntValueMap<String> bestFeatures =
            extractor.extractFromSentence(result.bestParse);
        updateModel(correctFeatures, bestFeatures, numExamples);
      }

      for (String feat : averagedWeights) {
        updateAveragedWeights(feat, numExamples);
        counts.put(feat, numExamples);
      }

      Log.info("Testing development set.");
      _PerceptronAmbiguityResolver disambiguator =
          new _PerceptronAmbiguityResolver(averagedWeights, extractor, analyzer);
      disambiguator.test(devSet);

    }
    return new _PerceptronAmbiguityResolver(averagedWeights,
        new FeatureExtractor(false), analyzer);
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
      if (Math.abs(wa) <= Model.epsilon) {
        averagedWeights.data.remove(feat);
      }
      float w = weights.get(feat);
      if (Math.abs(w) <= Model.epsilon) {
        weights.data.remove(feat);
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

    List<_SentenceAnalysis> sentences;

    DataSet(List<_SentenceAnalysis> sentences) {
      this.sentences = sentences;
    }

    static DataSet load(Path path, _TurkishMorphologicalAnalyzer analyzer) throws IOException {
      List<SentenceDataStr> sentencesFromTextFile = DataSet.loadTrainingDataText(path);
      return new DataSet(DataSet.convert(sentencesFromTextFile, analyzer));
    }

    static List<_SentenceAnalysis> convert(
        List<SentenceDataStr> set,
        _TurkishMorphologicalAnalyzer analyzer) {

      List<_SentenceAnalysis> trainingSentences = new ArrayList<>();
      // Find actual analysis equivalents.
      for (SentenceDataStr sentenceStr : set) {

        String sentence = sentenceStr.sentence;

        List<_WordAnalysis> sentenceAnalysis = analyzer.analyzeSentence(sentence);

        if (sentenceAnalysis.size() != sentenceStr.wordList.size()) {
          Log.warn("Actual analysis token size [%d] and sentence from file token size [%d] "
                  + "does not match for sentence [%s]",
              sentenceAnalysis.size(), sentenceStr.wordList.size(), sentence);
        }

        List<_SentenceWordAnalysis> unambigiousAnalyses = new ArrayList<>();

        BAD_SENTENCE:
        for (int i = 0; i < sentenceAnalysis.size(); i++) {

          _WordAnalysis w = sentenceAnalysis.get(i);
          WordDataStr s = sentenceStr.wordList.get(i);

          if (!w.getInput().equals(s.word)) {
            Log.warn(
                "Actual analysis token [%s] at index [%d] is different than word from training [%s] "
                    + " for sentence [%s]", w.getInput(), i, s.word, sentence);
          }

          if (w.analysisCount() != s.wordAnalysis.size()) {
            Log.warn(
                "Actual analysis token [%s] has [%d] analyses but word from training file has [%d] "
                    + " analyses for sentence [%s]",
                w.getInput(), i, s.wordAnalysis.size(), sentence);
            break;
          }

          Map<String, _SingleAnalysis> analysisMap = new HashMap<>();
          for (_SingleAnalysis single : w) {
            analysisMap.put(single.format(), single);
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
            _SingleAnalysis correct = analysisMap.get(s.correctAnalysis);
            unambigiousAnalyses.add(new _SentenceWordAnalysis(correct, w));
          } else {
            break;
          }
        }

        if (unambigiousAnalyses.size() == sentenceStr.wordList.size()) {
          trainingSentences.add(new _SentenceAnalysis(unambigiousAnalyses));
        }
      }
      Log.info("There are %d sentences in trainig set.", trainingSentences.size());
      Log.info("Total token count is %d.",
          trainingSentences.stream().mapToInt(_SentenceAnalysis::size).sum());

      return trainingSentences;

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
      Log.info("%d sentences are loaded. ", set.size());
      Log.info("Total token count is %d.", set.stream().mapToInt(s -> s.wordList.size()).sum());
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


}
