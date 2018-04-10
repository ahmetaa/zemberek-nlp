package zemberek.morphology._analyzer;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish._TurkishAlphabet;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.structure.StemAndEnding;
import zemberek.tokenization.TurkishTokenizer;

// TODO: mothods require some re-thinking.
// analysis method should probably not apply unidentified token analysis.
// this should be left to the user.
public class _TurkishMorphologicalAnalyzer {

  RootLexicon lexicon;
  InterpretingAnalyzer analyzer;
  private _UnidentifiedTokenAnalyzer unidentifiedTokenAnalyzer;
  private TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
  private AnalysisCache cache = AnalysisCache.INSTANCE;

  public _TurkishMorphologicalAnalyzer(RootLexicon lexicon) {
    this.lexicon = lexicon;
    analyzer = new InterpretingAnalyzer(lexicon);
    unidentifiedTokenAnalyzer = new _UnidentifiedTokenAnalyzer(analyzer);
    cache.initializeStaticCache(this::analyzeWithoutCache);
  }

  public static _TurkishMorphologicalAnalyzer createDefault() throws IOException {
    RootLexicon lexicon = TurkishDictionaryLoader.loadDefaultDictionaries();
    return new _TurkishMorphologicalAnalyzer(lexicon);
  }

  public _WordAnalysis analyze(String word) {
    return analyzeWithCache(word);
  }

  private _WordAnalysis analyzeWithCache(String word) {
    return cache.getAnalysis(word, this::analyzeWithoutCache);
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
  private _WordAnalysis analyzeWithoutCache(String word) {

    String s = TextUtil.normalizeApostrophes(word.toLowerCase(_TurkishAlphabet.TR));

    if (s.length() == 0) {
      return _WordAnalysis.EMPTY_INPUT_RESULT;
    }
    List<_SingleAnalysis> res = analyzer.analyze(s);

    if (res.size() == 0) {
      res = analyzeWordsWithApostrophe(s);
    }

    if (res.size() == 0) {
      res = unidentifiedTokenAnalyzer.analyze(s);
    }

    if (res.size() == 1 && res.get(0).getItem().isUnknown()) {
      res = Collections.emptyList();
    }

    return new _WordAnalysis(word, s, res);
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

  public List<_WordAnalysis> analyzeSentence(String sentence) {
    String preprocessed = preProcessSentence(sentence);
    List<_WordAnalysis> result = new ArrayList<>();
    for (String s : Splitter.on(" ").omitEmptyStrings().trimResults().split(preprocessed)) {
      result.add(analyze(s));
    }
    return result;
  }

  private String preProcessSentence(String str) {
    String quotesHyphensNormalized = TextUtil.normalizeQuotesHyphens(str);
    return Joiner.on(" ").join(tokenizer.tokenizeToStrings(quotesHyphensNormalized));
  }

  public AnalysisCache getCache() {
    return cache;
  }
}
