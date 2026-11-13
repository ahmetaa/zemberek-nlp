package zemberek.normalization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalysisSurfaceFormatter;
import zemberek.morphology.analysis.WordAnalysisSurfaceFormatter.CaseType;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Projects surface casing and punctuation spacing from an original input sentence onto the
 * lowercase normalized output produced by {@link TurkishSentenceNormalizer}.
 */
public class NormalizationFormatProjector {

  private static final int BACKTRACK_1_TO_1 = 1;
  private static final int BACKTRACK_1_TO_0 = 2;
  private static final int BACKTRACK_0_TO_1 = 3;
  private static final int BACKTRACK_1_TO_2 = 4;
  private static final int BACKTRACK_2_TO_1 = 5;

  private static final String OPENING_PUNCT = "([{“‘«";
  private static final String CLOSING_PUNCT = ".,;:!?)]}”’»…";

  private final TurkishMorphology morphology;
  private final WordAnalysisSurfaceFormatter surfaceFormatter = new WordAnalysisSurfaceFormatter();

  public NormalizationFormatProjector(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  /**
   * Projects casing, unambiguous proper noun capitalization, and punctuation attachment from {@code
   * rawSentence} onto {@code normalizedLower}.
   */
  public String project(String rawSentence, String normalizedLower) {
    if (rawSentence == null || rawSentence.trim().isEmpty()) {
      return rawSentence;
    }
    if (normalizedLower == null || normalizedLower.isEmpty()) {
      return normalizedLower;
    }

    List<Token> rawTokens = TurkishTokenizer.DEFAULT.tokenize(rawSentence);
    List<Token> normTokens = TurkishTokenizer.DEFAULT.tokenize(normalizedLower);
    if (normTokens.isEmpty()) {
      return "";
    }
    if (rawTokens.isEmpty()) {
      return normalizedLower;
    }

    CaseRegime regime = detectCaseRegime(rawTokens);
    Alignment alignment = align(rawTokens, normTokens);

    int nLen = normTokens.size();
    String[] outTokens = new String[nLen];
    boolean atSentenceStart = true;

    for (int j = 0; j < nLen; j++) {
      Token normTok = normTokens.get(j);
      String normText = normTok.getText();
      int rStart = alignment.rawStart[j];
      int rEnd = alignment.rawEnd[j];
      boolean isSecondarySplit = j > 0 && alignment.splitGroup[j] && alignment.splitGroup[j - 1]
          && rStart >= 0 && rStart == alignment.rawStart[j - 1];

      if (normTok.getType() == Type.Punctuation || isNonWordFormattingToken(normTok.getType())) {
        outTokens[j] =
            (rStart >= 0 && rStart == rEnd && rawTokens.get(rStart).getText().equalsIgnoreCase(normText))
                ? rawTokens.get(rStart).getText()
                : normText;
        if (isSentenceEndingPunct(normText)) {
          atSentenceStart = true;
        }
        continue;
      }

      String projected =
          projectWordToken(
              normText,
              rawTokens,
              rStart,
              rEnd,
              isSecondarySplit,
              regime,
              atSentenceStart);
      outTokens[j] = projected;
      atSentenceStart = false;
    }

    return reconstructSpacing(rawTokens, normTokens, alignment, outTokens);
  }

  private String projectWordToken(
      String normText,
      List<Token> rawTokens,
      int rStart,
      int rEnd,
      boolean isSecondarySplit,
      CaseRegime regime,
      boolean atSentenceStart) {

    if (rStart >= 0 && !isSecondarySplit) {
      Token rawPrimary = rawTokens.get(rStart);
      String rawText = rawPrimary.getText();
      String rawLower = rawText.toLowerCase(Turkish.LOCALE);

      // Rule 1: 1:1 exact lexical match in normal/mixed-case sentences preserves raw token verbatim
      // unless the raw token was all-lowercase and is an unambiguous ProperNoun.
      if (rStart == rEnd && rawLower.equals(normText)) {
        if (!regime.allCaps && !regime.titleSpam) {
          if (containsUppercase(rawText)) {
            return rawText;
          }
          String proper = restoreUnambiguousProperNoun(normText);
          return proper != null ? proper : rawText;
        }
        if (atSentenceStart && startsWithUpper(rawText)) {
          return capitalizeWithApostropheIfProper(normText);
        }
        String proper = restoreUnambiguousProperNoun(normText);
        return proper != null ? proper : normText;
      }

      // Rule 2: Modified token (or 1:2 primary / 2:1 merge) inherits raw casing signal.
      if (!regime.allCaps && !regime.titleSpam) {
        String acronymFormatted = formatUpperAcronymIfApplicable(rawText, normText);
        if (acronymFormatted != null) {
          return acronymFormatted;
        }
        if (startsWithUpper(rawText)) {
          return capitalizeWithApostropheIfProper(normText);
        }
      } else {
        if (atSentenceStart && startsWithUpper(rawText)) {
          return capitalizeWithApostropheIfProper(normText);
        }
      }
    }

    // Rule 3: Unambiguous proper noun restoration on lowercase/shouting/title-spam tokens.
    String proper = restoreUnambiguousProperNoun(normText);
    return proper != null ? proper : normText;
  }

  private String capitalizeWithApostropheIfProper(String normText) {
    String proper = restoreUnambiguousProperNoun(normText);
    if (proper != null) {
      return proper;
    }
    int apos = normText.indexOf('\'');
    if (apos > 0) {
      return Turkish.capitalize(normText.substring(0, apos)) + normText.substring(apos);
    }
    return Turkish.capitalize(normText);
  }

  /**
   * Restores title-case (and apostrophe when already present or required by ProperNoun orthography)
   * ONLY if every valid morphological analysis of {@code normText} is a {@link
   * SecondaryPos#ProperNoun}. Homographs with any common-word reading are left untouched.
   */
  private String restoreUnambiguousProperNoun(String normText) {
    if (normText.length() < 2) {
      return null;
    }
    WordAnalysis analysis = morphology.analyze(normText);
    SingleAnalysis bestProper = null;
    int bestRank = Integer.MAX_VALUE;
    for (SingleAnalysis sa : analysis) {
      if (sa.isUnknown() || sa.isRuntime()) {
        continue;
      }
      if (sa.getDictionaryItem().secondaryPos != SecondaryPos.ProperNoun) {
        return null;
      }
      int rank = rankProperAnalysis(sa, normText);
      if (bestProper == null || rank < bestRank) {
        bestProper = sa;
        bestRank = rank;
      }
    }
    if (bestProper == null) {
      return null;
    }
    boolean hasApos = normText.indexOf('\'') >= 0;
    if (!hasApos && hasDerivationOrPlural(bestProper)) {
      return Turkish.capitalize(normText);
    }
    String apostrophe = hasApos ? "'" : null;
    String formatted = surfaceFormatter.formatToCase(bestProper, CaseType.TITLE_CASE, apostrophe);
    if (formatted.isEmpty()) {
      return null;
    }
    String strippedNorm = normText.replace("'", "");
    String strippedFormatted = formatted.replace("'", "").toLowerCase(Turkish.LOCALE);
    if (strippedFormatted.equals(strippedNorm)) {
      return formatted;
    }
    return Turkish.capitalize(normText);
  }

  private static int rankProperAnalysis(SingleAnalysis sa, String normText) {
    int rank = 0;
    if (normText.equals(sa.getDictionaryItem().normalizedLemma())) {
      rank -= 100;
    }
    if (sa.getEnding().isEmpty()) {
      rank -= 50;
    }
    if (hasDerivationOrPlural(sa)) {
      rank += 20;
    }
    return rank + sa.getEnding().length();
  }

  private static boolean hasDerivationOrPlural(SingleAnalysis sa) {
    for (SingleAnalysis.MorphemeData md : sa.getMorphemeDataList()) {
      if (md.morpheme.derivational || "A3pl".equals(md.morpheme.id)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsUppercase(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isUpperCase(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static String formatUpperAcronymIfApplicable(String rawText, String normText) {
    String rawStem = stripAfterApostrophe(rawText);
    if (rawStem.endsWith(".")) {
      rawStem = rawStem.substring(0, rawStem.length() - 1);
    }
    int letterCount = 0;
    boolean allUpper = true;
    for (int i = 0; i < rawStem.length(); i++) {
      char c = rawStem.charAt(i);
      if (Character.isLetter(c)) {
        letterCount++;
        if (!Character.isUpperCase(c)) {
          allUpper = false;
          break;
        }
      } else if (!Character.isDigit(c)) {
        allUpper = false;
        break;
      }
    }
    if (!allUpper || letterCount < 2 || letterCount > 6) {
      return null;
    }
    int apos = normText.indexOf('\'');
    if (apos > 0) {
      return normText.substring(0, apos).toUpperCase(Turkish.LOCALE) + normText.substring(apos);
    }
    return normText.toUpperCase(Turkish.LOCALE);
  }

  private static String stripAfterApostrophe(String s) {
    int apos = s.indexOf('\'');
    return apos > 0 ? s.substring(0, apos) : s;
  }

  private static String reconstructSpacing(
      List<Token> rawTokens,
      List<Token> normTokens,
      Alignment alignment,
      String[] outTokens) {
    StringBuilder sb = new StringBuilder(outTokens.length * 8);
    for (int j = 0; j < outTokens.length; j++) {
      if (j > 0) {
        boolean omitSpace = shouldOmitSpace(rawTokens, normTokens, alignment, j);
        if (!omitSpace) {
          sb.append(' ');
        }
      }
      sb.append(outTokens[j]);
    }
    return sb.toString();
  }

  private static boolean shouldOmitSpace(
      List<Token> rawTokens, List<Token> normTokens, Alignment alignment, int j) {
    Token prevNorm = normTokens.get(j - 1);
    Token currNorm = normTokens.get(j);
    boolean prevPunct = prevNorm.getType() == Type.Punctuation;
    boolean currPunct = currNorm.getType() == Type.Punctuation;

    int prevRawEnd = alignment.rawEnd[j - 1];
    int currRawStart = alignment.rawStart[j];
    if (prevRawEnd >= 0
        && currRawStart >= 0
        && prevRawEnd + 1 == currRawStart
        && (prevPunct || currPunct)) {
      int gapStart = rawTokens.get(prevRawEnd).getEnd() + 1;
      int gapEnd = rawTokens.get(currRawStart).getStart();
      if (gapStart == gapEnd) {
        return true;
      }
    }

    String prevText = prevNorm.getText();
    String currText = currNorm.getText();
    if (currPunct && currText.length() == 1 && CLOSING_PUNCT.indexOf(currText.charAt(0)) >= 0) {
      if (prevPunct && currText.equals(prevText)) {
        // Adjacent dots like ". ." when expanded or repeated punctuation
        return false;
      }
      return !prevPunct;
    }
    if (prevPunct && prevText.length() == 1 && OPENING_PUNCT.indexOf(prevText.charAt(0)) >= 0) {
      return true;
    }
    return false;
  }

  private static boolean isNonWordFormattingToken(Type type) {
    return type == Type.Emoticon
        || type == Type.URL
        || type == Type.Email
        || type == Type.HashTag
        || type == Type.Mention;
  }

  private static boolean isSentenceEndingPunct(String text) {
    return ".".equals(text) || "!".equals(text) || "?".equals(text) || "...".equals(text);
  }

  private static boolean startsWithUpper(String s) {
    return !s.isEmpty() && Character.isUpperCase(s.charAt(0));
  }

  private static CaseRegime detectCaseRegime(List<Token> rawTokens) {
    int wordCount = 0;
    int casedWordCount = 0;
    int allCapsCount = 0;
    int titleOrCapsCount = 0;

    for (Token token : rawTokens) {
      if (!isWordLike(token.getType())) {
        continue;
      }
      String text = token.getText();
      if (text.isEmpty()) {
        continue;
      }
      wordCount++;
      boolean hasLetter = false;
      boolean hasLower = false;
      boolean hasUpper = false;
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (Character.isLetter(c)) {
          hasLetter = true;
          if (Character.isLowerCase(c)) {
            hasLower = true;
          } else if (Character.isUpperCase(c)) {
            hasUpper = true;
          }
        }
      }
      if (!hasLetter) {
        continue;
      }
      if (hasUpper) {
        casedWordCount++;
      }
      if (hasUpper && !hasLower) {
        allCapsCount++;
      }
      if (Character.isUpperCase(text.charAt(0))) {
        titleOrCapsCount++;
      }
    }

    boolean allCaps = casedWordCount >= 2 && allCapsCount * 5 >= casedWordCount * 4;
    boolean titleSpam = !allCaps && wordCount >= 5 && titleOrCapsCount * 5 >= wordCount * 4;
    return new CaseRegime(allCaps, titleSpam);
  }

  private static boolean isWordLike(Type type) {
    return type == Type.Word
        || type == Type.WordAlphanumerical
        || type == Type.WordWithSymbol
        || type == Type.Abbreviation
        || type == Type.AbbreviationWithDots
        || type == Type.UnknownWord;
  }

  private static Alignment align(List<Token> rawTokens, List<Token> normTokens) {
    int rLen = rawTokens.size();
    int nLen = normTokens.size();

    String[] rawLower = new String[rLen];
    boolean[] rawPunct = new boolean[rLen];
    for (int i = 0; i < rLen; i++) {
      rawLower[i] = rawTokens.get(i).getText().toLowerCase(Turkish.LOCALE);
      rawPunct[i] = rawTokens.get(i).getType() == Type.Punctuation;
    }

    String[] normText = new String[nLen];
    boolean[] normPunct = new boolean[nLen];
    for (int j = 0; j < nLen; j++) {
      normText[j] = normTokens.get(j).getText();
      normPunct[j] = normTokens.get(j).getType() == Type.Punctuation;
    }

    double[][] dp = new double[rLen + 1][nLen + 1];
    byte[][] bt = new byte[rLen + 1][nLen + 1];
    for (double[] row : dp) {
      Arrays.fill(row, 1e9);
    }
    dp[0][0] = 0.0;

    for (int i = 0; i <= rLen; i++) {
      for (int j = 0; j <= nLen; j++) {
        double base = dp[i][j];
        if (base >= 1e8) {
          continue;
        }
        if (i < rLen && j < nLen) {
          double cost = pairCost(rawLower[i], rawPunct[i], normText[j], normPunct[j]);
          if (base + cost < dp[i + 1][j + 1]) {
            dp[i + 1][j + 1] = base + cost;
            bt[i + 1][j + 1] = BACKTRACK_1_TO_1;
          }
        }
        if (i < rLen) {
          double cost = 1.1;
          if (base + cost < dp[i + 1][j]) {
            dp[i + 1][j] = base + cost;
            bt[i + 1][j] = BACKTRACK_1_TO_0;
          }
        }
        if (j < nLen) {
          double cost = 1.1;
          if (base + cost < dp[i][j + 1]) {
            dp[i][j + 1] = base + cost;
            bt[i][j + 1] = BACKTRACK_0_TO_1;
          }
        }
        if (i < rLen && j + 1 < nLen && !rawPunct[i] && !normPunct[j] && !normPunct[j + 1]) {
          double cost = splitCost(rawLower[i], normText[j], normText[j + 1]);
          if (base + cost < dp[i + 1][j + 2]) {
            dp[i + 1][j + 2] = base + cost;
            bt[i + 1][j + 2] = BACKTRACK_1_TO_2;
          }
        }
        if (i + 1 < rLen && j < nLen && !rawPunct[i] && !rawPunct[i + 1] && !normPunct[j]) {
          double cost = mergeCost(rawLower[i], rawLower[i + 1], normText[j]);
          if (base + cost < dp[i + 2][j + 1]) {
            dp[i + 2][j + 1] = base + cost;
            bt[i + 2][j + 1] = BACKTRACK_2_TO_1;
          }
        }
      }
    }

    int[] rawStart = new int[nLen];
    int[] rawEnd = new int[nLen];
    boolean[] splitGroup = new boolean[nLen];
    Arrays.fill(rawStart, -1);
    Arrays.fill(rawEnd, -1);

    int i = rLen;
    int j = nLen;
    while (i > 0 || j > 0) {
      byte move = bt[i][j];
      if (move == BACKTRACK_1_TO_1) {
        rawStart[j - 1] = i - 1;
        rawEnd[j - 1] = i - 1;
        i--;
        j--;
      } else if (move == BACKTRACK_1_TO_0) {
        i--;
      } else if (move == BACKTRACK_0_TO_1) {
        j--;
      } else if (move == BACKTRACK_1_TO_2) {
        rawStart[j - 2] = i - 1;
        rawEnd[j - 2] = i - 1;
        rawStart[j - 1] = i - 1;
        rawEnd[j - 1] = i - 1;
        splitGroup[j - 2] = true;
        splitGroup[j - 1] = true;
        i--;
        j -= 2;
      } else if (move == BACKTRACK_2_TO_1) {
        rawStart[j - 1] = i - 2;
        rawEnd[j - 1] = i - 1;
        i -= 2;
        j--;
      } else {
        if (i > 0 && j > 0) {
          i--;
          j--;
        } else if (i > 0) {
          i--;
        } else {
          j--;
        }
      }
    }

    return new Alignment(rawStart, rawEnd, splitGroup);
  }

  private static double pairCost(
      String rawLower, boolean rawPunct, String normText, boolean normPunct) {
    if (rawLower.equals(normText)) {
      return 0.0;
    }
    if (rawPunct != normPunct) {
      return 2.5;
    }
    if (rawPunct) {
      return 0.6;
    }
    if (TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(
        rawLower.replace("'", ""), normText.replace("'", ""))) {
      return 0.15;
    }
    return 0.25 + 0.6 * normalizedLevenshtein(rawLower, normText);
  }

  private static double splitCost(String rawLower, String norm1, String norm2) {
    String concat = norm1 + norm2;
    if (TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(
        rawLower.replace("'", ""), concat.replace("'", ""))) {
      return 0.2;
    }
    double sim = 1.0 - normalizedLevenshtein(rawLower, concat);
    return sim >= 0.55 ? 0.35 + (1.0 - sim) : 2.2;
  }

  private static double mergeCost(String raw1, String raw2, String norm) {
    String concat = raw1 + raw2;
    if (TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(
        concat.replace("'", ""), norm.replace("'", ""))) {
      return 0.2;
    }
    double sim = 1.0 - normalizedLevenshtein(concat, norm);
    return sim >= 0.65 ? 0.35 + (1.0 - sim) : 2.2;
  }

  private static double normalizedLevenshtein(String a, String b) {
    int n = a.length();
    int m = b.length();
    int max = Math.max(n, m);
    if (max == 0) {
      return 0.0;
    }
    int[] prev = new int[m + 1];
    int[] curr = new int[m + 1];
    for (int j = 0; j <= m; j++) {
      prev[j] = j;
    }
    for (int i = 1; i <= n; i++) {
      curr[0] = i;
      char ca = a.charAt(i - 1);
      for (int j = 1; j <= m; j++) {
        int cost = ca == b.charAt(j - 1) ? 0 : 1;
        curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
      }
      int[] tmp = prev;
      prev = curr;
      curr = tmp;
    }
    return prev[m] * 1.0 / max;
  }

  private static final class CaseRegime {
    final boolean allCaps;
    final boolean titleSpam;

    CaseRegime(boolean allCaps, boolean titleSpam) {
      this.allCaps = allCaps;
      this.titleSpam = titleSpam;
    }
  }

  private static final class Alignment {
    final int[] rawStart;
    final int[] rawEnd;
    final boolean[] splitGroup;

    Alignment(int[] rawStart, int[] rawEnd, boolean[] splitGroup) {
      this.rawStart = rawStart;
      this.rawEnd = rawEnd;
      this.splitGroup = splitGroup;
    }
  }
}
