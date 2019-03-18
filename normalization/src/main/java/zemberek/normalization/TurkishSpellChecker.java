package zemberek.normalization;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

  public List<String> suggestForWord(String word, NgramLanguageModel lm) {
    List<String> unRanked = getUnrankedSuggestions(word);
    return rankWithUnigramProbability(unRanked, lm);
  }

  private List<String> getUnrankedSuggestions(String word) {
    String normalized = TurkishAlphabet.INSTANCE.normalize(word.replaceAll("['’]", ""));
    List<String> strings = decoder.getSuggestions(normalized, charMatcher);

    WordAnalysisSurfaceFormatter.CaseType caseType = formatter.guessCase(word);
    if (caseType == WordAnalysisSurfaceFormatter.CaseType.MIXED_CASE ||
        caseType == WordAnalysisSurfaceFormatter.CaseType.LOWER_CASE) {
      caseType = WordAnalysisSurfaceFormatter.CaseType.DEFAULT_CASE;
    }
    Set<String> results = new LinkedHashSet<>(strings.size());
    for (String string : strings) {
      WordAnalysis analyses = morphology.analyze(string);
      for (SingleAnalysis analysis : analyses) {
        if (analysis.isUnknown()) {
          continue;
        }
        if (analysisPredicate != null && !analysisPredicate.test(analysis)) {
          continue;
        }
        String formatted = formatter.formatToCase(analysis, caseType, getApostrophe(word));
        results.add(formatted);
      }
    }
    return new ArrayList<>(results);
  }

  public List<String> suggestForWord(
      String word,
      String leftContext,
      String rightContext,
      NgramLanguageModel lm) {
    List<String> unRanked = getUnrankedSuggestions(word);
    if (lm == null) {
      Log.warn("No language model provided. Returning unraked results.");
      return unRanked;
    }
    if (lm.getOrder() < 2) {
      Log.warn("Language model order is 1. For context ranking it should be at least 2. " +
          "Unigram ranking will be applied.");
      return suggestForWord(word, lm);
    }
    LmVocabulary vocabulary = lm.getVocabulary();
    List<ScoredItem<String>> results = new ArrayList<>(unRanked.size());
    for (String str : unRanked) {
      if (leftContext == null) {
        leftContext = vocabulary.getSentenceStart();
      } else {
        leftContext = normalizeForLm(leftContext);
      }
      if (rightContext == null) {
        rightContext = vocabulary.getSentenceEnd();
      } else {
        rightContext = normalizeForLm(rightContext);
      }
      String w = normalizeForLm(str);
      int wordIndex = vocabulary.indexOf(w);
      int leftIndex = vocabulary.indexOf(leftContext);
      int rightIndex = vocabulary.indexOf(rightContext);
      float score;
      if (lm.getOrder() == 2) {
        score = lm.getProbability(leftIndex, wordIndex) + lm.getProbability(wordIndex, rightIndex);
      } else {
        score = lm.getProbability(leftIndex, wordIndex, rightIndex);
      }
      results.add(new ScoredItem<>(str, score));
    }
    results.sort(ScoredItem.STRING_COMP_DESCENDING);
    return results.stream().map(s -> s.item).collect(Collectors.toList());

  }

  private String normalizeForLm(String s) {
    if (s.indexOf('\'') > 0) {
      return Turkish.capitalize(s);
    } else {
      return s.toLowerCase(Turkish.LOCALE);
    }
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
    List<ScoredItem<String>> results = new ArrayList<>(strings.size());
    for (String string : strings) {
      String w = normalizeForLm(string);
      int wordIndex = lm.getVocabulary().indexOf(w);
      results.add(new ScoredItem<>(string, lm.getUnigramProbability(wordIndex)));
    }
    results.sort(ScoredItem.STRING_COMP_DESCENDING);
    return results.stream().map(s -> s.item).collect(Collectors.toList());
  }
}
