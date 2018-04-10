package zemberek.morphology.ambiguity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.text.TextConsumer;
import zemberek.core.text.TextIO;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology._analyzer._WordAnalysis;

public class _AmbiguityResolver implements _Disambiguator {


  public static void main(String[] args) throws IOException {

    Path path = Paths.get("data/ambiguity/www.aljazeera.com.tr-rule-result.txt");

    _TurkishMorphologicalAnalyzer analyzer = _TurkishMorphologicalAnalyzer.createDefault();

    List<SentenceDataStr> set = DataSet.t(path);
    // Find actual analysis equivalents.
    for (SentenceDataStr sentenceStr : set) {
      String sentence = sentenceStr.sentence;
      List<_WordAnalysis> sentenceAnalysis = analyzer.analyzeSentence(sentence);
      if (sentenceAnalysis.size() != sentenceStr.wordList.size()) {
        Log.warn("Actual anaysis token size [%d] and sentence from file token size [%d] "
                + "does not match for sentence [%s]",
            sentenceAnalysis.size(), sentenceStr.wordList.size(), sentence);
      }
      for (int i = 0; i < sentenceAnalysis.size(); i++) {
        _WordAnalysis w = sentenceAnalysis.get(i);
        WordDataStr s = sentenceStr.wordList.get(i);
        if (!w.getInput().equals(s.word)) {
          Log.warn("Actual anaysis token [%s] at index [%d] and sentence from file token [%s] "
              + "does not match for sentence [%s]", w.getInput(), i, sentence);
        }

        Map<String, _WordAnalysis> lexAnalysisMap = new HashMap<>();


      }


    }

  }

  @Override
  public _SentenceAnalysis disambiguate(String sentence) {
    return null;
  }

  static class DataSet {

    static List<SentenceDataStr> t(Path input)
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
          } else {
            for (String s : analysesFromLines) {
              if (s.endsWith("*")) {
                selected = s.substring(0, s.length() - 1);
                break;
              }
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
      return set;
    }
  }


  static class SentenceDataStr {

    String sentence;
    List<WordDataStr> wordList;

    public SentenceDataStr(String sentence,
        List<WordDataStr> wordList) {
      this.sentence = sentence;
      this.wordList = wordList;
    }
  }

  static class WordDataStr {

    String word;
    String correctAnalysis;
    List<String> wordAnalysis;

    public WordDataStr(String word, String correctAnalysis,
        List<String> wordAnalysis) {
      this.word = word;
      this.correctAnalysis = correctAnalysis;
      this.wordAnalysis = wordAnalysis;
    }
  }

}
