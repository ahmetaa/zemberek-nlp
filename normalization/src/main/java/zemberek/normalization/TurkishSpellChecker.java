package zemberek.normalization;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import zemberek.core.ScoredItem;
import zemberek.core.logging.Log;
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
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class TurkishSpellChecker {

  private static final float EDIT_DISTANCE_WEIGHT = 2.4f;
  private static final float DIACRITIC_DIFF_WEIGHT = 0.65f;
  private static final float DIACRITIC_DIFF_WEIGHT_ASCII = 0.20f;

  private static final NgramLanguageModel DUMMY_LM = new DummyLanguageModel();
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

  // TODO: this is a temporary hack.
  public void setAnalysisPredicate(Predicate<SingleAnalysis> analysisPredicate) {
    this.analysisPredicate = analysisPredicate;
  }

  //TODO: this does not cover all token types.
  public static List<String> tokenizeForSpelling(String sentence) {
    List<Token> tokens = tokenizer.tokenize(sentence);
    List<String> result = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      if (token.getType() == Token.Type.Unknown ||
          token.getType() == Token.Type.UnknownWord ||
          token.getType() == Token.Type.Punctuation) {
        continue;
      }
      String w = token.getText();
      if (token.getType() == Token.Type.Word) {
        w = w.toLowerCase(Turkish.LOCALE);
      } else if (token.getType() == Token.Type.WordWithSymbol) {
        w = Turkish.capitalize(w);
      }
      result.add(w);
    }
    return result;
  }

  public boolean check(String input) {
    WordAnalysis analyses = morphology.analyze(input);
    WordAnalysisSurfaceFormatter.CaseType caseType = formatter.guessCase(input);
    for (SingleAnalysis analysis : analyses) {
      if (analysis.isUnknown()) {
        continue;
      }
      if (analysisPredicate != null && !analysisPredicate.test(analysis)) {
        continue;
      }
      String apostrophe = getApostrophe(input);

      if (formatter.canBeFormatted(analysis, caseType)) {
        String formatted = formatter.formatToCase(analysis, caseType, apostrophe);
        if (input.equals(formatted)) {
          return true;
        }
      }
    }
    return false;
  }

  private String getApostrophe(String input) {
    if (input.indexOf('’') > 0) {
      return "’";
    } else if (input.indexOf('\'') > 0) {
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
      int wordIndex = lmVocabularyIndex(suggestion.surface, vocabulary);
      float score =
          lm.getUnigramProbability(wordIndex)
              - logScale * suggestion.channelPenalty(DIACRITIC_DIFF_WEIGHT);
      results.add(new ScoredItem<>(suggestion.surface, score));
    }
    results.sort(ScoredItem.STRING_COMP_DESCENDING);
    return results.stream().map(s -> s.item).collect(Collectors.toList());
  }

  private static List<String> toSurfaceList(List<ScoredSuggestion> scored) {
    List<String> out = new ArrayList<>(scored.size());
    for (ScoredSuggestion s : scored) {
      out.add(s.surface);
    }
    return out;
  }

  private List<ScoredSuggestion> getScoredSuggestions(String word) {
    String stripped = stripApostrophes(word);
    String normalized = TurkishAlphabet.INSTANCE.normalize(stripped);
    List<ScoredItem<String>> rawScored = decoder.getSuggestionsWithScores(normalized, charMatcher);

    WordAnalysisSurfaceFormatter.CaseType caseType = formatter.guessCase(word);
    if (caseType == WordAnalysisSurfaceFormatter.CaseType.MIXED_CASE ||
        caseType == WordAnalysisSurfaceFormatter.CaseType.LOWER_CASE) {
      caseType = WordAnalysisSurfaceFormatter.CaseType.DEFAULT_CASE;
    }
    Map<String, ScoredSuggestion> results = new LinkedHashMap<>(rawScored.size());
    for (ScoredItem<String> raw : rawScored) {
      WordAnalysis analyses = morphology.analyze(raw.item);
      for (SingleAnalysis analysis : analyses) {
        if (analysis.isUnknown()) {
          continue;
        }
        if (analysisPredicate != null && !analysisPredicate.test(analysis)) {
          continue;
        }
        String formatted = formatter.formatToCase(analysis, caseType, getApostrophe(word));
        int diacDiffs = countDiacriticDifferences(stripped, formatted);
        float nonDiacriticEdits =
            charMatcher == null ? Math.max(0f, raw.score - diacDiffs) : raw.score;
        ScoredSuggestion candidate =
            new ScoredSuggestion(formatted, nonDiacriticEdits, diacDiffs);
        ScoredSuggestion existing = results.get(formatted);
        if (existing == null
            || candidate.channelPenalty(DIACRITIC_DIFF_WEIGHT)
                < existing.channelPenalty(DIACRITIC_DIFF_WEIGHT)) {
          results.put(formatted, candidate);
        }
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
      Log.warn("Language model order is 1. For context ranking it should be at least 2. " +
          "Unigram ranking will be applied.");
      return suggestForWord(word, lm);
    }
    LmVocabulary vocabulary = lm.getVocabulary();
    int leftIndex = leftContext == null
        ? vocabulary.indexOf(vocabulary.getSentenceStart())
        : lmVocabularyIndex(leftContext, vocabulary);
    int rightIndex = rightContext == null
        ? vocabulary.indexOf(vocabulary.getSentenceEnd())
        : lmVocabularyIndex(rightContext, vocabulary);
    float diacriticWeight = asciiContext ? DIACRITIC_DIFF_WEIGHT_ASCII : DIACRITIC_DIFF_WEIGHT;
    float logScale = lmLogScale(lm);

    List<ScoredItem<String>> results = new ArrayList<>(scored.size());
    for (ScoredSuggestion suggestion : scored) {
      int wordIndex = lmVocabularyIndex(suggestion.surface, vocabulary);
      float lmScore = lm.getOrder() == 2
          ? lm.getProbability(leftIndex, wordIndex) + lm.getProbability(wordIndex, rightIndex)
          : lm.getProbability(leftIndex, wordIndex, rightIndex);
      float score = lmScore - logScale * suggestion.channelPenalty(diacriticWeight);
      results.add(new ScoredItem<>(suggestion.surface, score));
    }
    results.sort(ScoredItem.STRING_COMP_DESCENDING);
    return results.stream().map(s -> s.item).collect(Collectors.toList());
  }

  private static float lmLogScale(NgramLanguageModel lm) {
    if (lm instanceof SmoothLm) {
      double base = ((SmoothLm) lm).getLogBase();
      if (base > 1.0001) {
        return (float) (1.0 / Math.log(base));
      }
    }
    if (lm instanceof DummyLanguageModel) {
      return 1.0f;
    }
    return (float) (1.0 / Math.log(10.0));
  }

  private static String stripApostrophes(String s) {
    if (s.indexOf('\'') < 0 && s.indexOf('’') < 0) {
      return s;
    }
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c != '\'' && c != '’') {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static char toLowerTurkish(char c) {
    if (c == 'I') {
      return 'ı';
    }
    if (c == 'İ') {
      return 'i';
    }
    return Character.toLowerCase(c);
  }

  static int countDiacriticDifferences(String source, String target) {
    String a = stripApostrophes(source);
    String b = stripApostrophes(target);
    int n = a.length();
    int m = b.length();
    if (n == m) {
      int diffs = 0;
      for (int i = 0; i < n; i++) {
        char la = toLowerTurkish(a.charAt(i));
        char lb = toLowerTurkish(b.charAt(i));
        if (la != lb
            && TurkishAlphabet.INSTANCE.getAsciiEqual(la)
                == TurkishAlphabet.INSTANCE.getAsciiEqual(lb)) {
          diffs++;
        }
      }
      return diffs;
    }
    int diffs = 0;
    int left = 0;
    int limit = Math.min(n, m);
    while (left < limit) {
      char la = toLowerTurkish(a.charAt(left));
      char lb = toLowerTurkish(b.charAt(left));
      if (TurkishAlphabet.INSTANCE.getAsciiEqual(la)
          != TurkishAlphabet.INSTANCE.getAsciiEqual(lb)) {
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
      char la = toLowerTurkish(a.charAt(ra - 1));
      char lb = toLowerTurkish(b.charAt(rb - 1));
      if (TurkishAlphabet.INSTANCE.getAsciiEqual(la)
          != TurkishAlphabet.INSTANCE.getAsciiEqual(lb)) {
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
    String normalized = s.indexOf('’') >= 0 ? s.replace('’', '\'') : s;
    String lower = normalized.toLowerCase(Turkish.LOCALE);
    if (normalized.indexOf('\'') > 0) {
      String cap = Turkish.capitalize(normalized);
      if (vocabulary.contains(cap)) {
        return vocabulary.indexOf(cap);
      }
    }
    return vocabulary.indexOf(lower);
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
      int wordIndex = lmVocabularyIndex(string, vocabulary);
      results.add(new ScoredItem<>(string, lm.getUnigramProbability(wordIndex)));
    }
    results.sort(ScoredItem.STRING_COMP_DESCENDING);
    return results.stream().map(s -> s.item).collect(Collectors.toList());
  }
}
