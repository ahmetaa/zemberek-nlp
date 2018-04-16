package zemberek.morphology;

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
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.InterpretingAnalyzer;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.UnidentifiedTokenAnalyzer;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.morphology.generator.Generator;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.Serializer;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.core.turkish.StemAndEnding;
import zemberek.tokenization.TurkishTokenizer;

// TODO: mothods require some re-thinking.
// analysis method should probably not apply unidentified token analysis.
// this should be left to the user.
public class TurkishMorphology {

  private RootLexicon lexicon;
  private InterpretingAnalyzer analyzer;
  private Generator generator;
  private UnidentifiedTokenAnalyzer unidentifiedTokenAnalyzer;
  private TurkishTokenizer tokenizer;
  private AnalysisCache cache;
  private TurkishMorphotactics morphotactics;
  private AmbiguityResolver ambiguityResolver;

  private boolean useUnidentifiedTokenAnalyzer;
  private boolean useCache;

  private TurkishMorphology(Builder builder) {
    this.lexicon = builder.lexicon;
    this.morphotactics = new TurkishMorphotactics(builder.lexicon);
    this.analyzer = new InterpretingAnalyzer(morphotactics);
    this.generator = new Generator(morphotactics);
    this.unidentifiedTokenAnalyzer = new UnidentifiedTokenAnalyzer(analyzer);
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

    if (ambiguityResolver == null) {
      String resourcePath = "/tr/ambiguity/model-compressed";
      try {
        this.ambiguityResolver =
            PerceptronAmbiguityResolver.fromResource(resourcePath);
      } catch (IOException e) {
        throw new RuntimeException(
            "Cannot initialize PerceptronAmbiguityResolver from resource " + resourcePath, e);
      }
    }
  }

  public static TurkishMorphology createWithDefaults() throws IOException {
    Stopwatch sw = Stopwatch.createStarted();
    TurkishMorphology instance = new Builder().addDefaultBinaryDictionary().build();
    Log.info("Initialized in %d ms.", sw.elapsed(TimeUnit.MILLISECONDS));
    return instance;
  }

  public static TurkishMorphology createWithTextDictionaries() throws IOException {
    Stopwatch sw = Stopwatch.createStarted();
    TurkishMorphology instance = new Builder().addDefaultDictionaries().build();
    Log.info("Initialized in %d ms.", sw.elapsed(TimeUnit.MILLISECONDS));
    return instance;
  }

  public TurkishMorphotactics getMorphotactics() {
    return morphotactics;
  }

  public WordAnalysis analyze(String word) {
    return useCache ? analyzeWithCache(word) : analyzeWithoutCache(word);
  }

  //TODO: cannot use cache with tokens.
  public WordAnalysis analyze(Token token) {
    return analyzeWithoutCache(token);
  }

  private WordAnalysis analyzeWithCache(String word) {
    return cache.getAnalysis(word, this::analyzeWithoutCache);
  }

  public void invalidateCache() {
    if (useCache) {
      cache.invalidateDynamicCache();
    }
  }

  public RootLexicon getLexicon() {
    return lexicon;
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
  private WordAnalysis analyzeWithoutCache(String word) {

    String s = TextUtil.normalizeApostrophes(word.toLowerCase(TurkishAlphabet.TR));

    if (s.length() == 0) {
      return WordAnalysis.EMPTY_INPUT_RESULT;
    }
    List<SingleAnalysis> res = analyzer.analyze(s);

    if (res.size() == 0) {
      res = analyzeWordsWithApostrophe(s);
    }

    if (res.size() == 0 && useUnidentifiedTokenAnalyzer) {
      res = unidentifiedTokenAnalyzer.analyze(s);
    }

    if (res.size() == 1 && res.get(0).getDictionaryItem().isUnknown()) {
      res = Collections.emptyList();
    }

    return new WordAnalysis(word, s, res);
  }

  private WordAnalysis analyzeWithoutCache(Token token) {

    String word = token.getText();
    String s = TextUtil.normalizeApostrophes(word.toLowerCase(TurkishAlphabet.TR));

    if (s.length() == 0) {
      return WordAnalysis.EMPTY_INPUT_RESULT;
    }
    List<SingleAnalysis> result = analyzer.analyze(s);

    if (result.size() == 0) {
      result = analyzeWordsWithApostrophe(s);
    }

    if (result.size() == 0 && useUnidentifiedTokenAnalyzer) {
      result = unidentifiedTokenAnalyzer.analyze(token);
    }

    if (result.size() == 1 && result.get(0).getDictionaryItem().isUnknown()) {
      result = Collections.emptyList();
    }

    return new WordAnalysis(word, s, result);
  }

  private List<SingleAnalysis> analyzeWordsWithApostrophe(String word) {

    int index = word.indexOf('\'');

    if (index >= 0) {

      if (index == 0 || index == word.length() - 1) {
        return Collections.emptyList();
      }

      StemAndEnding se = new StemAndEnding(word.substring(0, index), word.substring(index + 1));
      String stem = TurkishAlphabet.INSTANCE.normalize(se.stem);

      String withoutQuote = word.replaceAll("'", "");

      List<SingleAnalysis> noQuotesParses = analyzer.analyze(withoutQuote);
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

  public List<WordAnalysis> analyzeSentence(String sentence) {
    String normalized = TextUtil.normalizeQuotesHyphens(sentence);
    List<WordAnalysis> result = new ArrayList<>();
    for (Token token : tokenizer.tokenize(normalized)) {
      result.add(analyze(token));
    }
    return result;
  }

  public SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> sentenceAnalysis) {
    return ambiguityResolver.disambiguate(sentence, sentenceAnalysis);
  }

  public SentenceAnalysis analyzeAndResolveAmbiguity(String sentence) {
    return disambiguate(sentence, analyzeSentence(sentence));
  }

  public AnalysisCache getCache() {
    return cache;
  }

  public Generator getGenerator() {
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
    AmbiguityResolver ambiguityResolver;
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

    public Builder addDictionaryLines(String... lines) {
      lexicon.addAll(new TurkishDictionaryLoader().load(lines));
      return this;
    }

    public Builder addDictionaryLines(Collection<String> lines) {
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

    public Builder setAmbiguityResolver(AmbiguityResolver ambiguityResolver) {
      this.ambiguityResolver = ambiguityResolver;
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

    public Builder removeItems(Iterable<String> dictionaryString) {
      lexicon.removeAll(new TurkishDictionaryLoader().load(dictionaryString));
      return this;
    }

    public Builder removeAllLemmas(Iterable<String> lemmas) {
      lexicon.removeAllLemmas(lemmas);
      return this;
    }

    public TurkishMorphology build() {
      return new TurkishMorphology(this);
    }
  }
}
