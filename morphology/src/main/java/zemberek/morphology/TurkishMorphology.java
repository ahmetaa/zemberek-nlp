package zemberek.morphology;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.StemAndEnding;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.UnidentifiedTokenAnalyzer;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

// TODO: mothods require some re-thinking.
// analysis method should probably not apply unidentified token analysis.
// this should be left to the user.
public class TurkishMorphology {

  private RootLexicon lexicon;
  private RuleBasedAnalyzer analyzer;
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
    if (lexicon.isEmpty()) {
      Log.warn("TurkishMorphology class is being instantiated with empty root lexicon.");
    }

    this.morphotactics = builder.informalAnalysis ?
        new InformalTurkishMorphotactics(this.lexicon) : new TurkishMorphotactics(this.lexicon);

    this.analyzer = builder.ignoreDiacriticsInAnalysis ?
        RuleBasedAnalyzer.ignoreDiacriticsInstance(morphotactics) :
        RuleBasedAnalyzer.instance(morphotactics);

    this.wordGenerator = new WordGenerator(morphotactics);
    this.unidentifiedTokenAnalyzer = new UnidentifiedTokenAnalyzer(analyzer);
    this.tokenizer = builder.tokenizer;

    if (builder.useDynamicCache) {
      if (builder.cache == null) {
        cache = new AnalysisCache.Builder().build();
      } else {
        cache = builder.cache;
      }
      cache.initializeStaticCache(this::analyzeWithoutCache);
    }
    this.useCache = builder.useDynamicCache;
    this.useUnidentifiedTokenAnalyzer = builder.useUnidentifiedTokenAnalyzer;

    if (builder.ambiguityResolver == null) {
      String resourcePath = "/tr/ambiguity/model-compressed";
      try {
        this.ambiguityResolver =
            PerceptronAmbiguityResolver.fromResource(resourcePath);
      } catch (IOException e) {
        throw new RuntimeException(
            "Cannot initialize PerceptronAmbiguityResolver from resource " + resourcePath, e);
      }
    } else {
      this.ambiguityResolver = builder.ambiguityResolver;
    }
  }

  public RuleBasedAnalyzer getAnalyzer() {
    return analyzer;
  }

  public UnidentifiedTokenAnalyzer getUnidentifiedTokenAnalyzer() {
    return unidentifiedTokenAnalyzer;
  }

  public static TurkishMorphology createWithDefaults() {
    Stopwatch sw = Stopwatch.createStarted();
    TurkishMorphology instance = new Builder().setLexicon(RootLexicon.getDefault()).build();
    Log.info("Initialized in %d ms.", sw.elapsed(TimeUnit.MILLISECONDS));
    return instance;
  }

  public static TurkishMorphology create(RootLexicon lexicon) {
    return new Builder().setLexicon(lexicon).build();
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
    // TODO: This may cause problems for some foreign words with letter I.
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

    // TODO: this is somewhat a hack.Correct here once we decide what to do about
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

  public static Builder builder(RootLexicon lexicon) {
    return new Builder().setLexicon(lexicon);
  }

  public static class Builder {

    RootLexicon lexicon = new RootLexicon();
    boolean useDynamicCache = true;
    boolean useUnidentifiedTokenAnalyzer = true;
    AnalysisCache cache;
    AmbiguityResolver ambiguityResolver;
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    boolean informalAnalysis = false;
    boolean ignoreDiacriticsInAnalysis = false;

    public Builder setLexicon(RootLexicon lexicon) {
      this.lexicon = lexicon;
      return this;
    }

    public Builder setLexicon(String... dictionaryLines) {
      this.lexicon = RootLexicon.fromLines(dictionaryLines);
      return this;
    }

    public Builder useInformalAnalysis() {
      this.informalAnalysis = true;
      return this;
    }

    public Builder ignoreDiacriticsInAnalysis() {
      this.ignoreDiacriticsInAnalysis = true;
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

    public TurkishMorphology build() {
      return new TurkishMorphology(this);
    }
  }
}
