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
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.StemAndEnding;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.InterpretingAnalyzer;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.UnidentifiedTokenAnalyzer;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.tokenization.TurkishTokenizer;

// TODO: mothods require some re-thinking.
// analysis method should probably not apply unidentified token analysis.
// this should be left to the user.
public class TurkishMorphology {

  private RootLexicon lexicon;
  private InterpretingAnalyzer analyzer;
  private WordGenerator wordGenerator;
  private UnidentifiedTokenAnalyzer unidentifiedTokenAnalyzer;
  private TurkishTokenizer tokenizer;
  private AnalysisCache cache;
  private TurkishMorphotactics morphotactics;
  private AmbiguityResolver ambiguityResolver;

  private boolean useUnidentifiedTokenAnalyzer;
  private boolean useCache;

  private TurkishMorphology(Builder builder) {

    this.lexicon = builder.lexicon;
    if (builder.morphotactics == null) {
      this.morphotactics = new TurkishMorphotactics(this.lexicon);
    } else {
      this.morphotactics = builder.morphotactics;
    }
    if (builder.analyzer == null) {
      this.analyzer = InterpretingAnalyzer.instance(morphotactics);
    } else {
      analyzer = builder.analyzer;
    }
    this.wordGenerator = new WordGenerator(morphotactics);
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

  public InterpretingAnalyzer getAnalyzer() {
    return analyzer;
  }

  public InterpretingAnalyzer getAnalyzerInstance(TurkishMorphotactics morphotactics) {
    return InterpretingAnalyzer.instance(morphotactics);
  }

  public UnidentifiedTokenAnalyzer getUnidentifiedTokenAnalyzer() {
    return unidentifiedTokenAnalyzer;
  }

  public static TurkishMorphology createWithDefaults() {
    Stopwatch sw = Stopwatch.createStarted();
    TurkishMorphology instance = new Builder().addDefaultBinaryDictionary().build();
    Log.info("Initialized in %d ms.", sw.elapsed(TimeUnit.MILLISECONDS));
    return instance;
  }

  /**
   * Adds internal text dictionaries. This is used only for debugging and will throw exception in
   * jar distributions as they only contain binary dictionaries.
   *
   * @return same builder instance
   * @throws IOException I/O Error
   * @deprecated this method will be removed in 0.16.0 because it causes confusion, use builder
   * mechanism, and {@link Builder#addDefaultBinaryDictionary()} or {@link
   * Builder#addTextDictionaryResources(String...)} instead.
   */
  public static TurkishMorphology createWithTextDictionaries() throws IOException {
    Stopwatch sw = Stopwatch.createStarted();
    TurkishMorphology instance = new Builder()
        .addTextDictionaryResources(TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES).build();
    Log.info("Initialized in %d ms.", sw.elapsed(TimeUnit.MILLISECONDS));
    return instance;
  }

  public TurkishMorphotactics getMorphotactics() {
    return morphotactics;
  }

  public WordAnalysis analyze(String word) {
    return useCache ? analyzeWithCache(word) : analyzeWithoutCache(word);
  }

  public WordAnalysis analyze(Token token) {
    return cache.getAnalysis(token, this::analyzeWithoutCache);
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

    List<Token> tokens = tokenizer.tokenize(word);
    if (tokens.size() != 1) {
      return new WordAnalysis(word, word, new ArrayList<>(0));
    }
    return analyzeWithoutCache(tokens.get(0));
  }

  public static String normalizeForAnalysis(String word) {
    String s = word.toLowerCase(Turkish.LOCALE);
    s = TurkishAlphabet.INSTANCE.normalizeCircumflex(s);
    String noDot = s.replace(".", "");
    if (noDot.length() == 0) {
      noDot = s;
    }
    return TextUtil.normalizeApostrophes(noDot);
  }

  private WordAnalysis analyzeWithoutCache(Token token) {

    String word = token.getText();
    String s = normalizeForAnalysis(word);

    if (s.length() == 0) {
      return WordAnalysis.EMPTY_INPUT_RESULT;
    }

    List<SingleAnalysis> result;

    if (TurkishAlphabet.INSTANCE.containsApostrophe(s)) {
      s = TurkishAlphabet.INSTANCE.normalizeApostrophe(s);
      result = analyzeWordsWithApostrophe(s);
    } else {
      result = analyzer.analyze(s);
    }

    if (result.size() == 0 && useUnidentifiedTokenAnalyzer) {
      result = unidentifiedTokenAnalyzer.analyze(token);
    }

    if (result.size() == 1 && result.get(0).getDictionaryItem().isUnknown()) {
      result = Collections.emptyList();
    }

    return new WordAnalysis(word, s, result);
  }

  public List<SingleAnalysis> analyzeWordsWithApostrophe(String word) {

    int index = word.indexOf('\'');

    if (index <= 0 || index == word.length() - 1) {
      return Collections.emptyList();
    }

    StemAndEnding se = new StemAndEnding(
        word.substring(0, index),
        word.substring(index + 1));

    String stem = TurkishAlphabet.INSTANCE.normalize(se.stem);

    String withoutQuote = word.replace("'", "");

    List<SingleAnalysis> noQuotesParses = analyzer.analyze(withoutQuote);
    if (noQuotesParses.size() == 0) {
      return Collections.emptyList();
    }

    // TODO: this is somewhat a hack.Correcty here once we decide what to do about
    // words like "Hastanesi'ne". Should we accept Hastanesi or Hastane?
    return noQuotesParses.stream()
        .filter(
            a -> a.getDictionaryItem().primaryPos == PrimaryPos.Noun &&
                (a.containsMorpheme(TurkishMorphotactics.p3sg) || a.getStem().equals(stem)))
        .collect(Collectors.toList());
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

  /**
   * Applies morphological analysis and disambiguation to a sentence.
   *
   * @param sentence Sentence.
   * @return SentenceAnalysis instance.
   */
  public SentenceAnalysis analyzeAndDisambiguate(String sentence) {
    return disambiguate(sentence, analyzeSentence(sentence));
  }

  public AnalysisCache getCache() {
    return cache;
  }

  public WordGenerator getWordGenerator() {
    return wordGenerator;
  }

  public WordGenerator getWordGenerator(TurkishMorphotactics morphotactics) {
    return new WordGenerator(morphotactics);
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
    TurkishMorphotactics morphotactics;
    InterpretingAnalyzer analyzer;
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

    public Builder addBinaryDictionary(Path dictionaryPath) throws IOException {
      lexicon.addAll(DictionarySerializer.load(dictionaryPath).getAllItems());
      return this;
    }

    public Builder useLexicon(RootLexicon lexicon) {
      this.lexicon = lexicon;
      return this;
    }

    public Builder addDefaultBinaryDictionary() {
      try {
        Stopwatch stopwatch = Stopwatch.createStarted();
        lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");
        Log.info("Dictionary generated in %d ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
      } catch (IOException e) {
        throw new RuntimeException(
            "Cannot load default binary dictionary. Reason:" + e.getMessage(), e);
      }
      return this;
    }

    public Builder addTextDictionaries(File... dictionaryFiles) throws IOException {
      List<String> lines = new ArrayList<>();
      for (File file : dictionaryFiles) {
        lines.addAll(Files.readAllLines(file.toPath()));
      }
      lexicon.addAll(TurkishDictionaryLoader.load(lines));
      return this;
    }

    public Builder addTextDictionaries(Path... dictionaryPaths) throws IOException {
      for (Path dictionaryPath : dictionaryPaths) {
        addTextDictionaries(dictionaryPath.toFile());
      }
      return this;
    }

    public Builder addDictionaryLines(String... lines) {
      lexicon.addAll(TurkishDictionaryLoader.load(lines));
      return this;
    }

    public Builder useAnaylzer(InterpretingAnalyzer analyzer) {
      this.morphotactics = analyzer.getMorphotactics();
      this.analyzer = analyzer;
      return this;
    }

    public Builder addDictionaryLines(Collection<String> lines) {
      lexicon.addAll(TurkishDictionaryLoader.load(lines));
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

    public Builder addTextDictionaryResources(Collection<String> resources) throws IOException {
      Log.info("Dictionaries :%s", String.join(", ", resources));
      List<String> lines = new ArrayList<>();
      for (String resource : resources) {
        lines.addAll(TextIO.loadLinesFromResource(resource));
      }
      lexicon.addAll(TurkishDictionaryLoader.load(lines));
      return this;
    }

    public Builder addTextDictionaryResources(String... resources) throws IOException {
      return addTextDictionaryResources(Arrays.asList(resources));
    }

    public Builder removeItems(Iterable<String> dictionaryString) {
      lexicon.removeAll(TurkishDictionaryLoader.load(dictionaryString));
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
