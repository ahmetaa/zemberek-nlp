package zemberek.normalization;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.LmVocabulary;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.normalization.NgramSequenceDecoder.Candidate;
import zemberek.normalization.NgramSequenceDecoder.Candidates;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier;
import zemberek.tokenization.Token;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Tries to normalize a sentence by collecting candidate words from
 * <pre>
 * - lookup tables (manual and collected from a large context graph)
 * - split-combine heuristics
 * - ascii tolerant analysis
 * - informal morphological analysis
 * - spell checker
 * </pre>
 * It then finds the most likely sequence using Viterbi search algorithm over candidate lists, using
 * a compressed language model.
 */
public class TurkishSentenceNormalizer {

  private static final float CLEAN_IDENTITY_BONUS = 0.35f;
  private static final float CLEAN_OVERRIDE_PENALTY = -1.35f;
  private static final float DIACRITIC_PENALTY = -0.10f;
  private static final float INFORMAL_PENALTY = -0.15f;
  private static final float GRAPH_LOOKUP_PENALTY = -0.35f;
  private static final float SPELL_BASE_PENALTY = -1.55f;
  private static final float OOV_IDENTITY_PENALTY = -2.50f;
  private static final Set<String> SENTENCE_BOUNDARIES = Set.of(".", "!", "?", "...");

  TurkishMorphology morphology;
  private final SmoothLm lm;
  private final TurkishSpellChecker spellChecker;
  private final ImmutableListMultimap<String, String> lookupFromGraph;
  private final ImmutableListMultimap<String, String> lookupManual;
  private final TurkishMorphology informalAsciiTolerantMorphology;
  private final FormalSurfaceRealizer surfaceRealizer;
  private final NormalizerPreprocessor preprocessor;
  private final NgramSequenceDecoder sequenceDecoder;

  boolean alwaysApplyDeasciifier = false;
  private Boolean spellCheckCapitalizedWords = null;
  private LatticeDeasciifier latticeDeasciifier;

  public TurkishSentenceNormalizer(
      TurkishMorphology morphology,
      Path dataRoot,
      Path languageModelPath) throws IOException {
    this.morphology = morphology;
    InformalAnalysisConverter analysisConverter =
        new InformalAnalysisConverter(morphology.getWordGenerator());
    this.lm = SmoothLm.builder(languageModelPath).logBase(Math.E).build();
    Log.info("Language model = %s", lm.info());
    this.sequenceDecoder = new NgramSequenceDecoder(lm);

    StemEndingGraph graph = new StemEndingGraph(morphology);
    CharacterGraphDecoder decoder = new CharacterGraphDecoder(graph.stemGraph);
    this.spellChecker = new TurkishSpellChecker(
        morphology,
        decoder,
        CharacterGraphDecoder.DIACRITICS_IGNORING_MATCHER);

    this.lookupManual =
        loadMultiMap(TextIO.loadLinesFromResource("normalization/candidates-manual"), Set.of());
    this.lookupFromGraph =
        loadMultiMap(TextIO.loadLines(dataRoot.resolve("lookup-from-graph")), lookupManual.keySet());

    this.informalAsciiTolerantMorphology = TurkishMorphology.builder()
        .setLexicon(morphology.getLexicon())
        .useInformalAnalysis()
        .ignoreDiacriticsInAnalysis()
        .setCache(AnalysisCache.builder().dynamicCacheSize(256, 1_024).build())
        .setAmbiguityResolver((s, a) -> new SentenceAnalysis(s, Collections.emptyList()))
        .build();
    this.latticeDeasciifier =
        LatticeDeasciifier.createWithDefaults(morphology, informalAsciiTolerantMorphology);
    this.surfaceRealizer =
        new FormalSurfaceRealizer(morphology, informalAsciiTolerantMorphology, analysisConverter);
    this.preprocessor =
        new NormalizerPreprocessor(morphology, surfaceRealizer, lm, lookupManual, dataRoot);
  }

  public void setAlwaysApplyDeasciifier(boolean alwaysApplyDeasciifier) {
    this.alwaysApplyDeasciifier = alwaysApplyDeasciifier;
  }

  /**
   * Enables {@link LatticeDeasciifier} (enabled by default) using its bundled default model while
   * sharing this normalizer's formal and informal/diacritic-tolerant {@link TurkishMorphology}
   * instances.
   */
  public TurkishSentenceNormalizer useLatticeDeasciifier() {
    this.latticeDeasciifier =
        LatticeDeasciifier.createWithDefaults(morphology, informalAsciiTolerantMorphology);
    return this;
  }

  /**
   * Switches to the legacy pattern-table {@link Deasciifier} instead of the default {@link
   * LatticeDeasciifier}.
   */
  public TurkishSentenceNormalizer useLegacyDeasciifier() {
    this.latticeDeasciifier = null;
    return this;
  }

  /**
   * Configures a custom {@link LatticeDeasciifier} instance (or {@code null} to use the legacy
   * pattern-table {@link Deasciifier}).
   */
  public TurkishSentenceNormalizer setLatticeDeasciifier(LatticeDeasciifier latticeDeasciifier) {
    this.latticeDeasciifier = latticeDeasciifier;
    return this;
  }

  /**
   * Controls whether out-of-vocabulary capitalized words are passed to the edit-distance spell
   * checker.
   */
  public void setSpellCheckCapitalizedWords(boolean spellCheckCapitalizedWords) {
    this.spellCheckCapitalizedWords = spellCheckCapitalizedWords;
  }

  private static ImmutableListMultimap<String, String> loadMultiMap(
      List<String> lines, Set<String> excludedKeys) {
    ImmutableListMultimap.Builder<String, String> result =
        ImmutableListMultimap.builderWithExpectedKeys(lines.size());
    result.expectedValuesPerKey(1);
    Splitter commaSplitter = Splitter.on(',').trimResults();
    for (String line : lines) {
      int index = line.indexOf('=');
      if (index < 0) {
        throw new IllegalStateException("Line needs to have `=` symbol. But it is:" + line);
      }
      String key = line.substring(0, index).trim();
      if (excludedKeys.contains(key)) {
        continue;
      }
      String value = line.substring(index + 1).trim();
      if (value.indexOf(',') >= 0) {
        for (String token : commaSplitter.split(value)) {
          result.put(key, token);
        }
      } else {
        result.put(key, value);
      }
    }
    return result.build();
  }

  public String normalize(String sentence) {
    if (sentence.trim().isEmpty()) {
      return sentence;
    }

    boolean asciiSentence = alwaysApplyDeasciifier || probablyRequiresDeasciifier(sentence);
    String processed = preProcess(sentence);

    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(processed);
    if (tokens.isEmpty()) {
      return "";
    }

    Set<String> protectedCapitalizedWords = collectProtectedCapitalizedWords(sentence);
    List<Candidates> candidatesList = new ArrayList<>(tokens.size());
    for (int i = 0; i < tokens.size(); i++) {
      String current = tokens.get(i).getText();
      String previous = i == 0 ? null : tokens.get(i - 1).getText();
      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).getText();
      candidatesList.add(collectTokenCandidates(
          current, previous, next, asciiSentence, protectedCapitalizedWords));
    }
    return String.join(" ", sequenceDecoder.decode(candidatesList));
  }

  private Candidates collectTokenCandidates(
      String current,
      String previous,
      String next,
      boolean asciiSentence,
      Set<String> protectedCapitalizedWords) {
    boolean isFormallyValid = isFormallyValidWord(current);
    boolean manualForcedOverride =
        lookupManual.containsKey(current) && !lookupManual.get(current).contains(current);
    boolean protectCleanForm = isFormallyValid && !asciiSentence && !manualForcedOverride;
    Map<String, Float> candidateScores = new LinkedHashMap<>(4);

    addLookupCandidates(current, protectCleanForm, asciiSentence, candidateScores);

    boolean currentHasRegularAnalysis = surfaceRealizer.hasRegularAnalysis(current);
    if (currentHasRegularAnalysis && !manualForcedOverride) {
      // Place valid input ahead of ASCII/diacritic distractors so LM tie-breaking preserves it.
      putScore(candidateScores, current, protectCleanForm ? CLEAN_IDENTITY_BONUS : 0f);
    }

    boolean currentHasApostrophe = hasApostrophe(current);
    boolean hasUsableAnalysis = addMorphologicalCandidates(
        current,
        isFormallyValid,
        currentHasRegularAnalysis,
        currentHasApostrophe,
        protectCleanForm,
        candidateScores);

    if (!hasUsableAnalysis
        && current.length() > 3
        && !TurkishAlphabet.INSTANCE.containsDigit(current)
        && !protectedCapitalizedWords.contains(current)) {
      addSpellCandidates(
          current, previous, next, asciiSentence, currentHasApostrophe, candidateScores);
    }

    addIdentityCandidate(current, isFormallyValid, protectCleanForm, candidateScores);

    List<Candidate> scoredCandidates = new ArrayList<>(candidateScores.size());
    for (Map.Entry<String, Float> entry : candidateScores.entrySet()) {
      scoredCandidates.add(new Candidate(entry.getKey(), entry.getValue()));
    }
    return new Candidates(current, scoredCandidates);
  }

  private void addLookupCandidates(
      String current,
      boolean protectCleanForm,
      boolean asciiSentence,
      Map<String, Float> candidateScores) {
    for (String cand : lookupManual.get(current)) {
      float score = 0f;
      if (!cand.equals(current)) {
        int diffs = TurkishSpellChecker.countDiacriticDifferences(current, cand);
        score = protectCleanForm
            ? CLEAN_OVERRIDE_PENALTY * Math.max(1, diffs)
            : (asciiSentence ? DIACRITIC_PENALTY * diffs : 0f);
      }
      putScore(candidateScores, cand, score);
    }

    for (String cand : lookupFromGraph.get(current)) {
      float score = cand.equals(current)
          ? 0f
          : (protectCleanForm ? CLEAN_OVERRIDE_PENALTY : GRAPH_LOOKUP_PENALTY);
      putScore(candidateScores, cand, score);
    }
  }

  private boolean addMorphologicalCandidates(
      String current,
      boolean isFormallyValid,
      boolean currentHasRegularAnalysis,
      boolean currentHasApostrophe,
      boolean protectCleanForm,
      Map<String, Float> primaryScores) {
    String normalizedCurrent = null;
    boolean hasUsableAnalysis = false;
    Map<String, Float> nonStrippingScores = new LinkedHashMap<>(4);
    Map<String, Float> strippedFallbackScores = new LinkedHashMap<>(2);

    for (SingleAnalysis analysis : informalAsciiTolerantMorphology.analyze(current)) {
      if (isUnapostrophizedInflectedAbbreviation(analysis, currentHasApostrophe)) {
        continue;
      }
      hasUsableAnalysis = true;
      if (normalizedCurrent == null) {
        normalizedCurrent = TurkishMorphology.normalizeForAnalysis(current).replace("'", "");
      }
      if (analysis.containsInformalMorpheme()) {
        collectInformalCandidate(
            current,
            normalizedCurrent,
            analysis,
            currentHasRegularAnalysis,
            nonStrippingScores,
            strippedFallbackScores);
      } else {
        collectFormalCandidates(
            current,
            normalizedCurrent,
            analysis,
            isFormallyValid,
            currentHasRegularAnalysis,
            currentHasApostrophe,
            protectCleanForm,
            nonStrippingScores,
            strippedFallbackScores);
      }
    }

    Map<String, Float> chosen =
        nonStrippingScores.isEmpty() ? strippedFallbackScores : nonStrippingScores;
    chosen.forEach((surface, score) -> putScore(primaryScores, surface, score));
    return hasUsableAnalysis;
  }

  private void collectInformalCandidate(
      String current,
      String normalizedCurrent,
      SingleAnalysis analysis,
      boolean currentHasRegularAnalysis,
      Map<String, Float> nonStrippingScores,
      Map<String, Float> strippedFallbackScores) {
    Result result = surfaceRealizer.analysisConverter().convert(current, analysis);
    if (result == null) {
      return;
    }
    int stemLen = Math.min(
        normalizedCurrent.length(),
        Math.min(analysis.getStem().length(), result.surface.length()));
    boolean stripsStemDiacritic = FormalSurfaceRealizer.stripsExplicitTurkishDiacritic(
        normalizedCurrent.substring(0, stemLen), result.surface.substring(0, stemLen));
    if (!stripsStemDiacritic) {
      putScore(nonStrippingScores, result.surface, INFORMAL_PENALTY);
    } else if (!currentHasRegularAnalysis) {
      putScore(strippedFallbackScores, result.surface, INFORMAL_PENALTY);
    }
  }

  private void collectFormalCandidates(
      String current,
      String normalizedCurrent,
      SingleAnalysis analysis,
      boolean isFormallyValid,
      boolean currentHasRegularAnalysis,
      boolean currentHasApostrophe,
      boolean protectCleanForm,
      Map<String, Float> nonStrippingScores,
      Map<String, Float> strippedFallbackScores) {
    for (Result result : surfaceRealizer.generateSurfaces(analysis)) {
      if (currentHasApostrophe && isFormallyValid && !hasApostrophe(result.surface)) {
        continue;
      }
      if (!TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(normalizedCurrent, result.surface)) {
        continue;
      }
      boolean stripsExplicit =
          FormalSurfaceRealizer.stripsExplicitTurkishDiacritic(normalizedCurrent, result.surface);
      if (currentHasRegularAnalysis && stripsExplicit) {
        continue;
      }
      float score = 0f;
      if (!result.surface.equals(current)) {
        int diffs = Math.max(
            1, TurkishSpellChecker.countDiacriticDifferences(current, result.surface));
        score = (protectCleanForm ? CLEAN_OVERRIDE_PENALTY : DIACRITIC_PENALTY) * diffs;
      }
      putScore(stripsExplicit ? strippedFallbackScores : nonStrippingScores, result.surface, score);
    }
  }

  private void addSpellCandidates(
      String current,
      String previous,
      String next,
      boolean asciiSentence,
      boolean currentHasApostrophe,
      Map<String, Float> candidateScores) {
    List<String> spellCandidates =
        spellChecker.suggestForWord(current, previous, next, lm, asciiSentence);
    int added = 0;
    for (String suggestion : spellCandidates) {
      if (!isAcceptableSpellSuggestion(suggestion, currentHasApostrophe)) {
        continue;
      }
      putScore(
          candidateScores,
          suggestion.toLowerCase(Turkish.LOCALE),
          SPELL_BASE_PENALTY + GRAPH_LOOKUP_PENALTY * added);
      if (++added == 3) {
        break;
      }
    }
  }

  private static void addIdentityCandidate(
      String current,
      boolean isFormallyValid,
      boolean protectCleanForm,
      Map<String, Float> candidateScores) {
    if (isFormallyValid) {
      putScore(candidateScores, current, protectCleanForm ? CLEAN_IDENTITY_BONUS : 0f);
    } else {
      putScore(candidateScores, current, candidateScores.isEmpty() ? 0f : OOV_IDENTITY_PENALTY);
    }
  }

  private static void putScore(Map<String, Float> scores, String surface, float score) {
    scores.merge(surface, score, Float::max);
  }

  private static boolean hasApostrophe(String s) {
    return s.indexOf('\'') > 0 || s.indexOf('’') > 0;
  }

  private static boolean isUnapostrophizedInflectedAbbreviation(
      SingleAnalysis analysis, boolean hasApostrophe) {
    return !hasApostrophe
        && analysis.getDictionaryItem().secondaryPos == SecondaryPos.Abbreviation
        && !analysis.getEnding().isEmpty();
  }

  private boolean isAcceptableSpellSuggestion(String suggestion, boolean currentHasApostrophe) {
    if (suggestion.isEmpty() || !Character.isUpperCase(suggestion.charAt(0))) {
      return true;
    }
    if (!currentHasApostrophe && hasApostrophe(suggestion)) {
      return false;
    }
    LmVocabulary vocab = lm.getVocabulary();
    return TurkishSpellChecker.lmVocabularyIndex(suggestion, vocab) != vocab.getUnknownWordIndex();
  }

  private boolean isFormallyValidWord(String word) {
    boolean apostrophe = hasApostrophe(word);
    for (SingleAnalysis a : morphology.analyze(word)) {
      if (FormalSurfaceRealizer.isKnownLexical(a)
          && !isUnapostrophizedInflectedAbbreviation(a, apostrophe)) {
        return true;
      }
    }
    return apostrophe && morphology.analyze(Turkish.capitalize(word)).isCorrect();
  }

  String preProcess(String sentence) {
    return preprocessor.preProcess(sentence, alwaysApplyDeasciifier, latticeDeasciifier);
  }

  String deasciifyValidWords(String s) {
    return preprocessor.deasciifyValidWords(s, false);
  }

  String deasciifyValidWords(String s, boolean preserveValidWords) {
    return preprocessor.deasciifyValidWords(s, preserveValidWords);
  }

  static boolean hasTurkishSpecificChars(String sentence) {
    return NormalizerPreprocessor.hasTurkishSpecificChars(sentence);
  }

  static boolean probablyRequiresDeasciifier(String sentence) {
    return NormalizerPreprocessor.probablyRequiresDeasciifier(sentence);
  }

  static boolean isWord(Token token) {
    return NormalizerPreprocessor.isWord(token);
  }

  boolean hasRegularAnalysis(String s) {
    return surfaceRealizer.hasRegularAnalysis(s);
  }

  String combineCommon(String i1, String i2) {
    return preprocessor.combineCommon(i1, i2);
  }

  String separateCommon(String input, boolean useLookup) {
    return preprocessor.separateCommon(input, useLookup);
  }

  String combineNumeralSuffix(String prevWord, String number, String suffix) {
    return preprocessor.combineNumeralSuffix(prevWord, number, suffix);
  }

  String combineNecessaryWords(List<Token> tokens) {
    return preprocessor.combineNecessaryWords(tokens);
  }

  String splitNecessaryWords(List<Token> tokens, boolean useLookup) {
    return preprocessor.splitNecessaryWords(tokens, useLookup);
  }

  private Set<String> collectProtectedCapitalizedWords(String sentence) {
    if (Boolean.TRUE.equals(spellCheckCapitalizedWords) || !containsLowercaseLetter(sentence)) {
      return Set.of();
    }
    List<Token> rawTokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    if (Boolean.FALSE.equals(spellCheckCapitalizedWords)) {
      Set<String> result = new HashSet<>();
      for (Token token : rawTokens) {
        String text = token.getText();
        if (isWord(token) && !text.isEmpty() && Character.isUpperCase(text.charAt(0))) {
          result.add(text.toLowerCase(Turkish.LOCALE));
        }
      }
      return result;
    }
    boolean hasLowercaseInitialWord = rawTokens.stream()
        .filter(TurkishSentenceNormalizer::isWord)
        .map(Token::getText)
        .anyMatch(t -> !t.isEmpty() && Character.isLowerCase(t.charAt(0)));
    if (!hasLowercaseInitialWord) {
      return Set.of();
    }
    Set<String> result = new HashSet<>();
    boolean atSentenceStart = true;
    for (Token token : rawTokens) {
      String text = token.getText();
      if (isWord(token) && !text.isEmpty()) {
        if (!atSentenceStart && Character.isUpperCase(text.charAt(0))) {
          result.add(text.toLowerCase(Turkish.LOCALE));
        }
        atSentenceStart = false;
      } else if (SENTENCE_BOUNDARIES.contains(text)) {
        atSentenceStart = true;
      }
    }
    return result;
  }

  private static boolean containsLowercaseLetter(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLowerCase(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }
}
