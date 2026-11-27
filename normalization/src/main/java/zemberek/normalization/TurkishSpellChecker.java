package zemberek.normalization;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import zemberek.core.ScoredItem;
import zemberek.core.logging.Log;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.DummyLanguageModel;
import zemberek.lm.LmVocabulary;
import zemberek.lm.NgramLanguageModel;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalysisSurfaceFormatter;
import zemberek.normalization.CharacterGraphDecoder.CharMatcher;
import zemberek.tokenization.Token;
import zemberek.tokenization.TurkishTokenizer;

public class TurkishSpellChecker {

  private static final float EDIT_DISTANCE_WEIGHT = 2.4f;
  private static final float DIACRITIC_DIFF_WEIGHT = 0.65f;
  private static final float DIACRITIC_DIFF_WEIGHT_ASCII = 0.20f;

  private static final TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
  TurkishMorphology morphology;
  WordAnalysisSurfaceFormatter formatter = new WordAnalysisSurfaceFormatter();
  CharacterGraphDecoder decoder;
  NgramLanguageModel unigramModel;

  // Null means exact matcher will be used.
  CharMatcher charMatcher = null;

  // can be used for filtering analysis results.
  Predicate<SingleAnalysis> analysisPredicate;

  public NgramLanguageModel getUnigramLanguageModel() {
    return unigramModel;
  }

  public TurkishSpellChecker(TurkishMorphology morphology) throws IOException {
    this.morphology = morphology;
    StemEndingGraph graph = new StemEndingGraph(morphology);
    this.decoder = new CharacterGraphDecoder(graph.stemGraph);
    try (InputStream is = Resources.getResource("lm-unigram.slm").openStream()) {
      unigramModel = SmoothLm.builder(is).build();
    }
  }

  public TurkishSpellChecker(TurkishMorphology morphology, CharacterGraph graph) {
    this.morphology = morphology;
    this.decoder = new CharacterGraphDecoder(graph);
  }

  public TurkishSpellChecker(
      TurkishMorphology morphology,
      CharacterGraphDecoder decoder,
      CharMatcher matcher) {
    this.morphology = morphology;
    this.decoder = decoder;
    this.charMatcher = matcher;
  }

  public void setAnalysisPredicate(Predicate<SingleAnalysis> analysisPredicate) {
    this.analysisPredicate = analysisPredicate;
  }

  public static List<String> tokenizeForSpelling(String sentence) {
    List<Token> tokens = tokenizer.tokenize(sentence);
    List<String> result = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      switch (token.getType()) {
        case Unknown, UnknownWord, Punctuation -> {}
        case Word -> result.add(token.getText().toLowerCase(Turkish.LOCALE));
        case WordWithSymbol -> result.add(Turkish.capitalize(token.getText()));
        default -> result.add(token.getText());
      }
    }
    return result;
  }

  public boolean check(String input) {
    WordAnalysis analyses = morphology.analyze(input);
    WordAnalysisSurfaceFormatter.CaseType caseType = formatter.guessCase(input);
    String apostrophe = getApostrophe(input);
    for (SingleAnalysis analysis : analyses) {
      if (!isAllowedAnalysis(analysis)) {
        continue;
      }
      if (formatter.canBeFormatted(analysis, caseType)
          && input.equals(formatter.formatToCase(analysis, caseType, apostrophe))) {
        return true;
      }
    }
    return false;
  }

  private boolean isAllowedAnalysis(SingleAnalysis analysis) {
    return !analysis.isUnknown()
        && (analysisPredicate == null || analysisPredicate.test(analysis));
  }

  private static String getApostrophe(String input) {
    if (input.indexOf('’') > 0) {
      return "’";
    }
    if (input.indexOf('\'') > 0) {
      return "'";
    }
    return null;
  }

  private record ScoredSuggestion(String surface, float editCost, int diacriticDiffs) {
    float channelPenalty(float diacriticWeight) {
      return EDIT_DISTANCE_WEIGHT * editCost + diacriticWeight * diacriticDiffs;
    }
  }

  public List<String> suggestForWord(String word, NgramLanguageModel lm) {
    List<ScoredSuggestion> scored = getScoredSuggestions(word);
    if (lm == null) {
      Log.warn("No language model provided. Returning unraked results.");
      return toSurfaceList(scored);
    }
    LmVocabulary vocabulary = lm.getVocabulary();
    float logScale = lmLogScale(lm);
    List<ScoredItem<String>> results = new ArrayList<>(scored.size());
    for (ScoredSuggestion suggestion : scored) {
      int wordIndex = vocabulary.indexOfTurkishWord(suggestion.surface);
      float score =
          lm.getUnigramProbability(wordIndex)
              - logScale * suggestion.channelPenalty(DIACRITIC_DIFF_WEIGHT);
      results.add(new ScoredItem<>(suggestion.surface, score));
    }
    return sortAndExtractItems(results);
  }

  private static List<String> toSurfaceList(List<ScoredSuggestion> scored) {
    return scored.stream().map(ScoredSuggestion::surface).toList();
  }

  private static List<String> sortAndExtractItems(List<ScoredItem<String>> results) {
    results.sort(ScoredItem.STRING_COMP_DESCENDING);
    return results.stream().map(s -> s.item).toList();
  }

  private List<ScoredSuggestion> getScoredSuggestions(String word) {
    String stripped = TurkishAlphabet.INSTANCE.stripApostrophes(word);
    String normalized = TurkishAlphabet.INSTANCE.normalize(stripped);
    List<ScoredItem<String>> rawScored = decoder.getSuggestionsWithScores(normalized, charMatcher);

    WordAnalysisSurfaceFormatter.CaseType caseType = formatter.guessCase(word);
    if (caseType == WordAnalysisSurfaceFormatter.CaseType.MIXED_CASE
        || caseType == WordAnalysisSurfaceFormatter.CaseType.LOWER_CASE) {
      caseType = WordAnalysisSurfaceFormatter.CaseType.DEFAULT_CASE;
    }
    String apostrophe = getApostrophe(word);
    Map<String, ScoredSuggestion> results = new LinkedHashMap<>(rawScored.size());
    for (ScoredItem<String> raw : rawScored) {
      for (SingleAnalysis analysis : morphology.analyze(raw.item)) {
        if (!isAllowedAnalysis(analysis)
            || analysis.getDictionaryItem().hasAttribute(RootAttribute.Vulgar)) {
          continue;
        }
        String formatted = formatter.formatToCase(analysis, caseType, apostrophe);
        int diacDiffs = countDiacriticDifferences(stripped, formatted);
        float nonDiacriticEdits =
            charMatcher == null ? Math.max(0f, raw.score - diacDiffs) : raw.score;
        ScoredSuggestion candidate =
            new ScoredSuggestion(formatted, nonDiacriticEdits, diacDiffs);
        results.merge(
            formatted,
            candidate,
            (existing, next) ->
                next.channelPenalty(DIACRITIC_DIFF_WEIGHT)
                        < existing.channelPenalty(DIACRITIC_DIFF_WEIGHT)
                    ? next
                    : existing);
      }
    }
    return new ArrayList<>(results.values());
  }

  public List<String> suggestForWord(
      String word,
      String leftContext,
      String rightContext,
      NgramLanguageModel lm) {
    return suggestForWord(word, leftContext, rightContext, lm, false);
  }

  List<String> suggestForWord(
      String word,
      String leftContext,
      String rightContext,
      NgramLanguageModel lm,
      boolean asciiContext) {
    List<ScoredSuggestion> scored = getScoredSuggestions(word);
    if (lm == null) {
      Log.warn("No language model provided. Returning unraked results.");
      return toSurfaceList(scored);
    }
    if (lm.getOrder() < 2) {
      Log.warn("Language model order is 1. For context ranking it should be at least 2. "
          + "Unigram ranking will be applied.");
      return suggestForWord(word, lm);
    }
    LmVocabulary vocabulary = lm.getVocabulary();
    int leftIndex = leftContext == null
        ? vocabulary.indexOf(vocabulary.getSentenceStart())
        : vocabulary.indexOfTurkishWord(leftContext);
    int rightIndex = rightContext == null
        ? vocabulary.indexOf(vocabulary.getSentenceEnd())
        : vocabulary.indexOfTurkishWord(rightContext);
    float diacriticWeight = asciiContext ? DIACRITIC_DIFF_WEIGHT_ASCII : DIACRITIC_DIFF_WEIGHT;
    float logScale = lmLogScale(lm);

    List<ScoredItem<String>> results = new ArrayList<>(scored.size());
    for (ScoredSuggestion suggestion : scored) {
      int wordIndex = vocabulary.indexOfTurkishWord(suggestion.surface);
      float lmScore = lm.getOrder() == 2
          ? lm.getProbability(leftIndex, wordIndex) + lm.getProbability(wordIndex, rightIndex)
          : lm.getProbability(leftIndex, wordIndex, rightIndex);
      float score = lmScore - logScale * suggestion.channelPenalty(diacriticWeight);
      results.add(new ScoredItem<>(suggestion.surface, score));
    }
    return sortAndExtractItems(results);
  }

  private static float lmLogScale(NgramLanguageModel lm) {
    return switch (lm) {
      case SmoothLm smooth when smooth.getLogBase() > 1.0001 ->
          (float) (1.0 / Math.log(smooth.getLogBase()));
      case DummyLanguageModel ignored -> 1.0f;
      default -> (float) (1.0 / Math.log(10.0));
    };
  }

  static int countDiacriticDifferences(String source, String target) {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String a = alphabet.stripApostrophes(source);
    String b = alphabet.stripApostrophes(target);
    int n = a.length();
    int m = b.length();
    if (n == m) {
      int diffs = 0;
      for (int i = 0; i < n; i++) {
        char la = Turkish.toLower(a.charAt(i));
        char lb = Turkish.toLower(b.charAt(i));
        if (la != lb && alphabet.getAsciiEqual(la) == alphabet.getAsciiEqual(lb)) {
          diffs++;
        }
      }
      return diffs;
    }
    int diffs = 0;
    int left = 0;
    int limit = Math.min(n, m);
    while (left < limit) {
      char la = Turkish.toLower(a.charAt(left));
      char lb = Turkish.toLower(b.charAt(left));
      if (alphabet.getAsciiEqual(la) != alphabet.getAsciiEqual(lb)) {
        break;
      }
      if (la != lb) {
        diffs++;
      }
      left++;
    }
    int ra = n;
    int rb = m;
    while (ra > left && rb > left) {
      char la = Turkish.toLower(a.charAt(ra - 1));
      char lb = Turkish.toLower(b.charAt(rb - 1));
      if (alphabet.getAsciiEqual(la) != alphabet.getAsciiEqual(lb)) {
        break;
      }
      if (la != lb) {
        diffs++;
      }
      ra--;
      rb--;
    }
    return diffs;
  }

  static int lmVocabularyIndex(String s, LmVocabulary vocabulary) {
    return vocabulary.indexOfTurkishWord(s);
  }

  public List<String> suggestForWord(String word) {
    return suggestForWord(word, unigramModel);
  }

  public CharacterGraphDecoder getDecoder() {
    return decoder;
  }

  public List<String> rankWithUnigramProbability(List<String> strings, NgramLanguageModel lm) {
    if (lm == null) {
      Log.warn("No language model provided. Returning unraked results.");
      return strings;
    }
    LmVocabulary vocabulary = lm.getVocabulary();
    List<ScoredItem<String>> results = new ArrayList<>(strings.size());
    for (String string : strings) {
      int wordIndex = vocabulary.indexOfTurkishWord(string);
      results.add(new ScoredItem<>(string, lm.getUnigramProbability(wordIndex)));
    }
    return sortAndExtractItems(results);
  }
}
