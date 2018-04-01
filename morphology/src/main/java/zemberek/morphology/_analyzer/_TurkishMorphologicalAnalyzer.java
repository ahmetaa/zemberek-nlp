package zemberek.morphology._analyzer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish._TurkishAlphabet;
import zemberek.morphology._morphotactics.TurkishMorphotactics;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.structure.StemAndEnding;

public class _TurkishMorphologicalAnalyzer {

  RootLexicon lexicon;
  TurkishMorphotactics morphotactics;
  InterpretingAnalyzer analyzer;
  _UnidentifiedTokenAnalyzer unidentifiedTokenAnalyzer;

  public _TurkishMorphologicalAnalyzer(RootLexicon lexicon) {
    this.lexicon = lexicon;
    morphotactics = new TurkishMorphotactics(lexicon);
    analyzer = new InterpretingAnalyzer(lexicon);
    unidentifiedTokenAnalyzer = new _UnidentifiedTokenAnalyzer(analyzer);
  }

  public List<_SingleAnalysis> analyze(String word) {
    return analyzeWithoutCache(word);
  }

  /**
   * Normalizes the input word and analyses it. If word cannot be parsed following occurs: - if
   * input is a number, system tries to parse it by creating a number DictionaryEntry. - if input
   * starts with a capital letter, or contains ['] adds a Dictionary entry as a proper noun. - if
   * above options does not generate a result, it generates an UNKNOWN dictionary entry and returns
   * a parse with it.
   *
   * @param word input word.
   * @return WordAnalysis list.
   */
  private List<_SingleAnalysis> analyzeWithoutCache(String word) {

    String s = TextUtil.normalizeApostrophes(word.toLowerCase(_TurkishAlphabet.TR));

    if (s.length() == 0) {
      return Collections.emptyList();
    }
    List<_SingleAnalysis> res = analyzer.analyze(s);

    if (res.size() == 0) {
      res = analyzeWordsWithApostrophe(s);

    }

    if (res.size() == 0) {
      res = unidentifiedTokenAnalyzer.analyze(s);
    }

    return res;
  }

  private List<_SingleAnalysis> analyzeWordsWithApostrophe(String word) {

    int index = word.indexOf('\'');

    if (index >= 0) {

      if (index == 0 || index == word.length() - 1) {
        return Collections.emptyList();
      }

      StemAndEnding se = new StemAndEnding(word.substring(0, index), word.substring(index + 1));
      String stem = TurkishAlphabet.INSTANCE.normalize(se.stem);

      String withoutQuote = word.replaceAll("'", "");

      List<_SingleAnalysis> noQuotesParses = analyzer.analyze(withoutQuote);
      if (noQuotesParses.size() == 0) {
        return Collections.emptyList();
      }

      return noQuotesParses.stream()
          .filter(noQuotesParse -> noQuotesParse.getStems().contains(stem))
          .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }

  }


}
