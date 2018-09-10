package zemberek.normalization;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.Token;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

/**
 * Tries to normalize a sentence using lookup tables and heuristics.
 */
public class TurkishSentenceNormalizer {

  TurkishMorphology morphology;
  Map<String, String> commonSplits = new HashMap<>();
  SmoothLm lm;
  TurkishSpellChecker spellChecker;
  HashSet<String> commonConnectedSuffixes = new HashSet<>();

  public TurkishSentenceNormalizer(
      TurkishMorphology morphology,
      Path commonSplit,
      SmoothLm languageModel) throws IOException {
    this.morphology = morphology;

    List<String> splitLines = Files.readAllLines(commonSplit, Charsets.UTF_8);
    for (String splitLine : splitLines) {
      String[] tokens = splitLine.split("=");
      commonSplits.put(tokens[0].trim(), tokens[1].trim());
    }
    this.lm = languageModel;

    StemEndingGraph graph = new StemEndingGraph(morphology);
    CharacterGraphDecoder decoder = new CharacterGraphDecoder(graph.stemGraph);
    this.spellChecker = new TurkishSpellChecker(
        morphology,
        decoder,
        CharacterGraphDecoder.ASCII_TOLERANT_MATCHER);
    this.commonConnectedSuffixes.addAll(TextIO.loadLinesFromResource("question-suffixes"));
    this.commonConnectedSuffixes.addAll(Arrays.asList("de", "da", "ki"));
  }

  public String normalize(List<Token> tokens) {
    String s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = splitNecessaryWords(tokens, false);
    if (probablyRequiresDeasciifier(s)) {
      Deasciifier deasciifier = new Deasciifier(s);
      s = deasciifier.convertToTurkish();
    }
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = splitNecessaryWords(tokens, true);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);

    s = useInformalAnalysis(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = useSpellChecker(tokens);
    return s;
  }

  public String normalize(String input) {
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(input);
    return normalize(tokens);
  }

  String splitNecessaryWords(List<Token> tokens, boolean useLookup) {
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      String text = token.getText();
      if (isWord(token)) {
        result.add(separateCommon(text, useLookup));
      } else {
        result.add(text);
      }
    }
    return String.join(" ", result);
  }

  private boolean isWord(Token token) {
    int type = token.getType();
    return type == TurkishLexer.Word
        || type == TurkishLexer.UnknownWord
        || type == TurkishLexer.WordAlphanumerical
        || type == TurkishLexer.WordWithSymbol;
  }

  String combineNecessaryWords(List<Token> tokens) {
    List<String> result = new ArrayList<>();
    boolean combined = false;
    for (int i = 0; i < tokens.size() - 1; i++) {
      Token first = tokens.get(i);
      Token second = tokens.get(i + 1);
      String firstS = first.getText();
      String secondS = second.getText();
      if (!isWord(first) || !isWord(second)) {
        combined = false;
        result.add(firstS);
        continue;
      }
      if (combined) {
        combined = false;
        continue;
      }
      String c = combineCommon(firstS, secondS);
      if (c.length() > 0) {
        result.add(c);
        combined = true;
      } else {
        result.add(first.getText());
        combined = false;
      }
    }
    if (!combined) {
      result.add(tokens.get(tokens.size() - 1).getText());
    }
    return String.join(" ", result);
  }

  String useInformalAnalysis(List<Token> tokens) {
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      String text = token.getText();
      if (isWord(token)) {
        WordAnalysis a = morphology.analyze(text);

        if (a.analysisCount() > 0) {

          if (a.stream().anyMatch(s -> !s.containsInformalMorpheme())) {
            result.add(text);
          } else {
            SingleAnalysis s = a.getAnalysisResults().get(0);
            WordGenerator gen = morphology.getWordGenerator();
            List<Morpheme> generated = toFormalMorphemeNames(s);
            List<WordGenerator.Result> results = gen.generate(s.getDictionaryItem(), generated);
            if (results.size() > 0) {
              result.add(results.get(0).surface);
            } else {
              result.add(text);
            }
          }
        } else {
          result.add(text);
        }
      } else {
        result.add(text);
      }
    }
    return String.join(" ", result);
  }

  List<Morpheme> toFormalMorphemeNames(SingleAnalysis a) {
    List<Morpheme> transform = new ArrayList<>();
    for (Morpheme m : a.getMorphemes()) {
      if (m.informal && m.mappedMorpheme != null) {
        transform.add(m.mappedMorpheme);
      } else {
        transform.add(m);
      }
    }
    return transform;
  }

  String useSpellChecker(List<Token> tokens) {

    List<String> result = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      Token currentToken = tokens.get(i);
      String current = currentToken.getText();
      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).getText();
      String previous = i == 0 ? null : tokens.get(i - 1).getText();
      if (isWord(currentToken) && (!hasAnalysis(current))) {
        List<String> candidates = spellChecker.suggestForWord(current, previous, next, lm);
        if (candidates.size() > 0) {
          result.add(candidates.get(0));
        } else {
          result.add(current);
        }
      } else {
        result.add(current);
      }
    }
    return String.join(" ", result);
  }


  /**
   * Tries to combine words that are written separately using heuristics. If it cannot combine,
   * returns empty string.
   *
   * Such as:
   * <pre>
   * göndere bilirler -> göndere bilirler
   * elma lar -> elmalar
   * ankara 'ya -> ankara'ya
   * </pre>
   */
  String combineCommon(String i1, String i2) {
    String combined = i1 + i2;
    if (i2.startsWith("'") || i2.startsWith("bil")) {
      if (hasAnalysis(combined)) {
        return combined;
      }
    }
    if (!hasRegularAnalysis(i2)) {
      if (hasAnalysis(combined)) {
        return combined;
      }
    }
    return "";
  }

  boolean hasAnalysis(String s) {
    WordAnalysis a = morphology.analyze(s);
    return a.analysisCount() > 0;
  }

  /**
   * Returns true if only word is analysed with internal dictionary and analysis dictionary item is
   * not proper noun.
   */
  boolean hasRegularAnalysis(String s) {
    WordAnalysis a = morphology.analyze(s);
    return a.stream().anyMatch(k -> !k.isUnknown() && !k.isRuntime() &&
        k.getDictionaryItem().secondaryPos != SecondaryPos.ProperNoun &&
        k.getDictionaryItem().secondaryPos != SecondaryPos.Abbreviation
    );
  }

  /**
   * Tries to separate question words, conjunctions and common mistakes by looking from a lookup or
   * using heuristics. Such as:
   * <pre>
   * gelecekmisin -> gelecek misin
   * tutupda -> tutup da
   * öyleki -> öyle ki
   * olurya -> olur ya
   * </pre>
   */
  String separateCommon(String input, boolean useLookup) {
    if (useLookup && commonSplits.containsKey(input)) {
      return commonSplits.get(input);
    }
    if (!hasRegularAnalysis(input)) {
      for (int i = 1; i < input.length() - 1; i++) {
        String tail = input.substring(i);
        if (commonConnectedSuffixes.contains(tail)) {
          String head = input.substring(0, i);
          if (hasRegularAnalysis(head)) {
            return head + " " + tail;
          } else {
            return input;
          }
        }
      }
    }
    return input;
  }

  /**
   * Makes a guess if input sentence requires deasciifier.
   */
  public boolean probablyRequiresDeasciifier(String sentence) {
    int turkishSpecCount = 0;
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (TurkishAlphabet.INSTANCE.isTurkishSpecific(c)) {
        turkishSpecCount++;
      }
    }
    double ratio = turkishSpecCount * 1d / sentence.length();
    return ratio < 0.02;
  }

}
