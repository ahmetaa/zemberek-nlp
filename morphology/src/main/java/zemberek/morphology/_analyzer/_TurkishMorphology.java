package zemberek.morphology._analyzer;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish._TurkishAlphabet;
import zemberek.morphology._morphotactics.TurkishMorphotactics;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.Serializer;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.structure.StemAndEnding;
import zemberek.tokenization.TurkishTokenizer;

// TODO: mothods require some re-thinking.
// analysis method should probably not apply unidentified token analysis.
// this should be left to the user.
public class _TurkishMorphology {

  RootLexicon lexicon;
  InterpretingAnalyzer analyzer;
  private _Generator generator;
  private _UnidentifiedTokenAnalyzer unidentifiedTokenAnalyzer;
  private TurkishTokenizer tokenizer;
  private AnalysisCache cache;
  private TurkishMorphotactics morphotactics;

  private boolean useUnidentifiedTokenAnalyzer;
  private boolean useCache;

  private _TurkishMorphology(Builder builder) {
    this.lexicon = builder.lexicon;
    this.morphotactics = new TurkishMorphotactics(builder.lexicon);
    this.analyzer = new InterpretingAnalyzer(morphotactics);
    this.generator = new _Generator(morphotactics);
    this.unidentifiedTokenAnalyzer = new _UnidentifiedTokenAnalyzer(analyzer);
    this.tokenizer = builder.tokenizer;

    if (builder.useDynamicCache) {
      if (builder.cache == null) {
        cache = AnalysisCache.DEFAULT_INSTANCE;
      } else {
        cache = builder.cache;
      }
      cache.initializeStaticCache(this::analyzeWithoutCache);
    }
    this.useCache = builder.useDynamicCache;
    this.useUnidentifiedTokenAnalyzer = builder.useUnidentifiedTokenAnalyzer;
  }

  public static _TurkishMorphology createWithDefaults() throws IOException {
    Stopwatch sw = Stopwatch.createStarted();
    _TurkishMorphology instance = new Builder().addDefaultBinaryDictionary().build();
    Log.info("Initialized in %d ms.", sw.elapsed(TimeUnit.MILLISECONDS));
    return instance;
  }

  public static _TurkishMorphology createWithTextDictionaries() throws IOException {
    Stopwatch sw = Stopwatch.createStarted();
    _TurkishMorphology instance = new Builder().addDefaultDictionaries().build();
    Log.info("Initialized in %d ms.", sw.elapsed(TimeUnit.MILLISECONDS));
    return instance;
  }

  public TurkishMorphotactics getMorphotactics() {
    return morphotactics;
  }

  public _WordAnalysis analyze(String word) {
    return useCache ? analyzeWithCache(word) : analyzeWithoutCache(word);
  }

  //TODO: cannot use cache with tokens.
  public _WordAnalysis analyze(Token token) {
    return analyzeWithoutCache(token);
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

    if (res.size() == 0 && useUnidentifiedTokenAnalyzer) {
      res = unidentifiedTokenAnalyzer.analyze(s);
    }

    if (res.size() == 1 && res.get(0).getDictionaryItem().isUnknown()) {
      res = Collections.emptyList();
    }

    return new _WordAnalysis(word, s, res);
  }

  private _WordAnalysis analyzeWithoutCache(Token token) {

    String word = token.getText();
    String s = TextUtil.normalizeApostrophes(word.toLowerCase(_TurkishAlphabet.TR));

    if (s.length() == 0) {
      return _WordAnalysis.EMPTY_INPUT_RESULT;
    }
    List<_SingleAnalysis> result = analyzer.analyze(s);

    if (result.size() == 0) {
      result = analyzeWordsWithApostrophe(s);
    }

    if (result.size() == 0 && useUnidentifiedTokenAnalyzer) {
      result = unidentifiedTokenAnalyzer.analyze(token);
    }

    if (result.size() == 1 && result.get(0).getDictionaryItem().isUnknown()) {
      result = Collections.emptyList();
    }

    return new _WordAnalysis(word, s, result);
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
    String normalized = TextUtil.normalizeQuotesHyphens(sentence);
    List<_WordAnalysis> result = new ArrayList<>();
    for (Token token : tokenizer.tokenize(normalized)) {
      result.add(analyze(token));
    }
    return result;
  }

  public AnalysisCache getCache() {
    return cache;
  }

  public _Generator getGenerator() {
    return generator;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    RootLexicon lexicon = new RootLexicon();
    boolean useDynamicCache = true;
    boolean useUnidentifiedTokenAnalyzer = true;
    AnalysisCache cache;
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

    public Builder addBinaryDictionary(Path dictionaryPath) throws IOException {
      lexicon.addAll(Serializer.load(dictionaryPath).getAllItems());
      return this;
    }

    public Builder addDefaultBinaryDictionary() throws IOException {
      Stopwatch stopwatch = Stopwatch.createStarted();
      lexicon = Serializer.loadFromResources("/tr/lexicon.bin");
      Log.info("Binary dictionary loaded in %d ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
      return this;
    }

    public Builder addDefaultDictionaries() throws IOException {
      return addTextDictionaryResources(
          TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.toArray(
              new String[0]));
    }

    public Builder addTextDictionaries(File... dictionaryFiles) throws IOException {
      List<String> lines = new ArrayList<>();
      for (File file : dictionaryFiles) {
        lines.addAll(Files.readAllLines(file.toPath()));
      }
      lexicon.addAll(new TurkishDictionaryLoader().load(lines));
      return this;
    }

    public Builder addTextDictionaries(Path... dictionaryPaths) throws IOException {
      for (Path dictionaryPath : dictionaryPaths) {
        addTextDictionaries(dictionaryPath.toFile());
      }
      return this;
    }

    public Builder addDictionaryLines(String... lines) throws IOException {
      lexicon.addAll(new TurkishDictionaryLoader().load(lines));
      return this;
    }

    public Builder addDictionaryLines(Collection<String> lines) throws IOException {
      lexicon.addAll(new TurkishDictionaryLoader().load(lines));
      return this;
    }

    public Builder removeDictionaryFiles(File... dictionaryFiles) throws IOException {
      for (File file : dictionaryFiles) {
        lexicon.removeAll(new TurkishDictionaryLoader().load(file));
      }
      return this;
    }

    public Builder setCache(AnalysisCache cache) {
      this.cache = cache;
      return this;
    }

    public Builder setTokenizer(TurkishTokenizer tokenizer) {
      this.tokenizer = tokenizer;
      return this;
    }

    public Builder disableCache() {
      useDynamicCache = false;
      return this;
    }

    public Builder disableUnidentifiedTokenAnalyzer() {
      useUnidentifiedTokenAnalyzer = false;
      return this;
    }

    public Builder addTextDictionaryResources(String... resources) throws IOException {
      Log.info("Dictionaries :%s", String.join(", ", Arrays.asList(resources)));
      List<String> lines = new ArrayList<>();
      for (String resource : resources) {
        lines.addAll(TextIO.loadLinesFromResource(resource));
      }
      lexicon.addAll(new TurkishDictionaryLoader().load(lines));
      Log.info("Lexicon Generated.");
      return this;
    }

    public Builder removeItems(Iterable<String> dictionaryString) throws IOException {
      lexicon.removeAll(new TurkishDictionaryLoader().load(dictionaryString));
      return this;
    }

    public Builder removeAllLemmas(Iterable<String> lemmas) throws IOException {
      lexicon.removeAllLemmas(lemmas);
      return this;
    }

    public _TurkishMorphology build() throws IOException {
      return new _TurkishMorphology(this);
    }
  }
}
