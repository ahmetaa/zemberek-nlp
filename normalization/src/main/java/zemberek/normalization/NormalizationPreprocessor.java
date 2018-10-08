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
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
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

public class NormalizationPreprocessor {

  TurkishMorphology morphology;
  Map<String, String> commonSplits = new HashMap<>();
  Map<String, String> replacements = new HashMap<>();
  SmoothLm lm;
  HashSet<String> commonConnectedSuffixes = new HashSet<>();
  HashSet<String> commonLetterRepeatitionWords = new HashSet<>();

  public NormalizationPreprocessor(
      TurkishMorphology morphology,
      Path modelRoot,
      SmoothLm languageModel) throws IOException {
    Log.info("Language model = %s", languageModel.info());
    this.morphology = morphology;

    List<String> splitLines = Files.readAllLines(modelRoot.resolve("split"), Charsets.UTF_8);
    for (String splitLine : splitLines) {
      String[] tokens = splitLine.split("=");
      commonSplits.put(tokens[0].trim(), tokens[1].trim());
    }
    this.lm = languageModel;

    this.commonConnectedSuffixes.addAll(TextIO.loadLinesFromResource(
        "normalization/question-suffixes"));
    this.commonConnectedSuffixes.addAll(Arrays.asList("de", "da", "ki"));

    List<String> replaceLines = TextIO.loadLinesFromResource(
        "normalization/replacements");
    for (String replaceLine : replaceLines) {
      String[] tokens = replaceLine.split("=");
      replacements.put(tokens[0].trim(), tokens[1].trim());
    }
  }

  String preProcess(String sentence) {
    sentence = sentence.toLowerCase(Turkish.LOCALE);
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    String s = replaceCommon(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = splitNecessaryWords(tokens, false);
    if (probablyRequiresDeasciifier(s)) {
      Deasciifier deasciifier = new Deasciifier(s);
      s = deasciifier.convertToTurkish();
    }
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    return splitNecessaryWords(tokens, true);
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
  static boolean probablyRequiresDeasciifier(String sentence) {
    int turkishSpecCount = 0;
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (TurkishAlphabet.INSTANCE.isTurkishSpecific(c)) {
        turkishSpecCount++;
      }
    }
    double ratio = turkishSpecCount * 1d / sentence.length();
    return ratio < 0.05;
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

  static boolean isWord(Token token) {
    int type = token.getType();
    return type == TurkishLexer.Word
        || type == TurkishLexer.UnknownWord
        || type == TurkishLexer.WordAlphanumerical
        || type == TurkishLexer.WordWithSymbol;
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

  String replaceCommon(List<Token> tokens) {
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      String text = token.getText();
      result.add(replacements.getOrDefault(text, text));
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

}
