package zemberek.normalization.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.tokenization.Token;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Core data types, candidate source bitflags, Turkish casing/character helpers, and
 * whitespace-preserving detokenizer for {@link SpanLatticePerceptronNormalizer}.
 */
public final class NormV2Types {

  private NormV2Types() {}

  public enum CasingShape {
    LOWER,
    TITLE,
    UPPER,
    MIXED,
    NON_ALPHA
  }

  // Candidate Generator Source Bitflags (combined into SpanArc.sourceMask)
  public static final int SRC_IDENTITY = 1;
  public static final int SRC_PARALLEL_1TO1 = 1 << 1;
  public static final int SRC_PARALLEL_SPLIT = 1 << 2;
  public static final int SRC_PARALLEL_MERGE = 1 << 3;
  public static final int SRC_FORMAL_MORPH = 1 << 4;
  public static final int SRC_INFORMAL_MORPH = 1 << 5;
  public static final int SRC_DEASCIIFIER = 1 << 6;
  public static final int SRC_SUFFIX_RULE = 1 << 7;
  public static final int SRC_OOV_APOSTROPHE = 1 << 8;
  public static final int SRC_ELONGATION = 1 << 9;
  public static final int SRC_SPELLCHECK = 1 << 10;
  public static final int SRC_CLITIC_SPLIT = 1 << 11;
  public static final int SRC_COMPOUND_MERGE = 1 << 12;
  public static final int SRC_NUMERAL_JOIN = 1 << 13;
  public static final int SRC_INFORMAL_EXACT = 1 << 14;
  public static final int SRC_APOSTROPHE_DROP = 1 << 15;
  public static final int SRC_SHORTHAND = 1 << 16;

  // Coarse Syntactic Categories for Head Words in Clitic Splits & Merges (SpanArc.headSyntaxClass)
  public static final byte HEAD_SYNTAX_OTHER = 0;
  public static final byte HEAD_SYNTAX_FINITE_OR_CONVERB = 1;
  public static final byte HEAD_SYNTAX_PRON_ADV_CONJ = 2;
  public static final byte HEAD_SYNTAX_NOMINAL = 3;

  private static final Set<Character> TRAILING_PUNCT =
      Set.of('.', ',', ';', ':', '!', '?', '…', ')', ']', '}', '»', '”', '’', '%');
  private static final Set<Character> OPENING_PUNCT = Set.of('(', '[', '{', '«', '“', '‘');
  private static final String EMOTICON_MOUTHS = "()dDpP/\\|oO03";
  private static final String EMOTICON_EXTRAS = "()dDpP/\\|oO03><*";

  private static final Pattern RANDOM_LAUGH_RE =
      Pattern.compile(
          "^(?:[asjkdfghwqe]{5,}|(?:ha){2,}h?|(?:he){2,}h?|(?:hi){2,}h?|(?:ho){2,}h?|(?:sj){2,}s?|(?:js){2,}j?|(?:ah){2,}a?)$");

  public record SpanToken(
      int index,
      String surface,
      String lower,
      String ascii,
      Token.Type type,
      CasingShape shape,
      int start,
      int end,
      boolean adjacentToPrev) {

    public boolean isImmutable() {
      return switch (type) {
        case Punctuation,
            Emoji,
            Emoticon,
            URL,
            Email,
            HashTag,
            Mention,
            Time,
            Date,
            PercentNumeral,
            RomanNumeral ->
            true;
        default -> false;
      };
    }

    public boolean containsDigit() {
      return NormV2Types.containsDigit(surface);
    }

    public boolean isRandomLaugh() {
      return surface.length() >= 4 && RANDOM_LAUGH_RE.matcher(ascii).matches();
    }

    /**
     * Returns {@code true} when an ALL-CAPS token contains ASCII {@code 'I'} ({@code U+0049}) and
     * no explicit Turkish uppercase diacritics ({@code İ, Ç, Ğ, Ö, Ş, Ü}).
     */
    public boolean hasUpperAsciiAmbiguousI() {
      return shape == CasingShape.UPPER
          && surface.indexOf('I') >= 0
          && !hasUpperTurkishDiacritics(surface);
    }
  }

  /**
   * Directed candidate edge in a {@link SentenceLattice} spanning raw tokens {@code [startIdx,
   * endIdx)} and emitting one or more normalized Turkish words ({@code normSurface}).
   */
  public static final class SpanArc {
    public final int startIdx;
    public final int endIdx;
    public final String normSurface;
    public final String normLower;
    public final String firstWordLower;
    public final String lastWordLower;
    public final int wordCount;
    public int sourceMask;
    public final short firstWordMorphTag;
    public final short coarseMorphTag;
    public final byte headSyntaxClass;
    public final byte verbPerson;
    public final boolean isRegularMorph;
    public final boolean isProperOrAbbr;
    public float parallelProb;
    public float parallelLogCount;
    public int rawParallelTotal;
    public final float suffixPrecision;
    public final float editCost;
    public final int diacriticDiffs;
    public final boolean caseChanged;
    public final boolean apostropheAdded;
    public final boolean apostropheRemoved;
    public final boolean stripsExplicitDiacritic;
    public final float charPhonotacticScore;
    public final float internalLmBonus;
    public byte verbPolarity;
    public boolean rawNegVerbCue;
    public int rawCleanCount;
    public float rawCharPhono;
    public float unaryCleanGain;
    public byte nextLexState;

    public static SpanArc plain(
        int startIdx,
        int endIdx,
        String normSurface,
        String normLower,
        int sourceMask,
        short coarseMorphTag) {
      return new SpanArc(
          startIdx,
          endIdx,
          normSurface,
          normLower,
          sourceMask,
          coarseMorphTag,
          coarseMorphTag,
          HEAD_SYNTAX_OTHER,
          (byte) 0,
          true,
          false,
          1.0f,
          0.0f,
          0.0f,
          0.0f,
          0,
          false,
          false,
          false,
          false,
          0.0f,
          0.0f);
    }

    public SpanArc(
        int startIdx,
        int endIdx,
        String normSurface,
        String normLower,
        int sourceMask,
        short firstWordMorphTag,
        short coarseMorphTag,
        byte headSyntaxClass,
        byte verbPerson,
        boolean isRegularMorph,
        boolean isProperOrAbbr,
        float parallelProb,
        float parallelLogCount,
        float suffixPrecision,
        float editCost,
        int diacriticDiffs,
        boolean caseChanged,
        boolean apostropheAdded,
        boolean apostropheRemoved,
        boolean stripsExplicitDiacritic,
        float charPhonotacticScore,
        float internalLmBonus) {
      this.startIdx = startIdx;
      this.endIdx = endIdx;
      this.normSurface = normSurface;
      this.normLower = normLower;
      int firstSpace = normLower.indexOf(' ');
      if (firstSpace < 0) {
        this.firstWordLower = normLower;
        this.lastWordLower = normLower;
        this.wordCount = 1;
      } else {
        this.firstWordLower = normLower.substring(0, firstSpace);
        this.lastWordLower = normLower.substring(normLower.lastIndexOf(' ') + 1);
        this.wordCount = countSpaceSeparatedWords(normLower);
      }
      this.sourceMask = sourceMask;
      this.firstWordMorphTag = firstWordMorphTag;
      this.coarseMorphTag = coarseMorphTag;
      this.headSyntaxClass = headSyntaxClass;
      this.verbPerson = verbPerson;
      this.isRegularMorph = isRegularMorph;
      this.isProperOrAbbr = isProperOrAbbr;
      this.parallelProb = parallelProb;
      this.parallelLogCount = parallelLogCount;
      this.suffixPrecision = suffixPrecision;
      this.editCost = editCost;
      this.diacriticDiffs = diacriticDiffs;
      this.caseChanged = caseChanged;
      this.apostropheAdded = apostropheAdded;
      this.apostropheRemoved = apostropheRemoved;
      this.stripsExplicitDiacritic = stripsExplicitDiacritic;
      this.charPhonotacticScore = charPhonotacticScore;
      this.internalLmBonus = internalLmBonus;
    }

    private static int countSpaceSeparatedWords(String text) {
      int words = 1;
      for (int i = 0; i < text.length(); i++) {
        if (text.charAt(i) == ' ') {
          words++;
        }
      }
      return words;
    }
  }

  public record SentenceLattice(
      List<SpanToken> tokens,
      List<SpanArc>[] outgoing,
      float sentenceAsciiRatio,
      byte overtSubjectPerson,
      boolean sentenceAllCaps,
      boolean hasQuestionMark) {}

  public static String trLower(String s) {
    if (s == null || s.isEmpty()) {
      return "";
    }
    return s.replace('’', '\'').toLowerCase(Turkish.LOCALE);
  }

  public static String trUpper(String s) {
    if (s == null || s.isEmpty()) {
      return "";
    }
    return s.replace('’', '\'').toUpperCase(Turkish.LOCALE);
  }

  public static String trCapitalize(String s) {
    if (s == null || s.isEmpty()) {
      return "";
    }
    String head = String.valueOf(s.charAt(0)).toUpperCase(Turkish.LOCALE);
    return s.length() == 1 ? head : head + s.substring(1);
  }

  public static String toAscii(String lower) {
    return lower == null || lower.isEmpty() ? "" : TurkishAlphabet.INSTANCE.toAscii(lower);
  }

  public static boolean containsDigit(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isDigit(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public static boolean isAllDigits(String s) {
    if (s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isDigitsOrPunctOnly(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLetter(s.charAt(i))) {
        return false;
      }
    }
    return !s.isEmpty();
  }

  public static boolean isPersonalInitial(String s) {
    return s.length() == 2 && s.charAt(1) == '.' && Character.isLetter(s.charAt(0));
  }

  public static boolean hasUpperTurkishDiacritics(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == 'İ' || c == 'Ç' || c == 'Ğ' || c == 'Ö' || c == 'Ş' || c == 'Ü') {
        return true;
      }
    }
    return false;
  }

  /** Clamps language-model log-probabilities into {@code [-20.0f, 0.0f]} with NaN/Inf safety. */
  public static float clampLm(float v) {
    return Float.isNaN(v) || Float.isInfinite(v) ? -20.0f : Math.clamp(v, -20.0f, 0.0f);
  }

  public static CasingShape detectShape(String s) {
    int upper = 0;
    int lower = 0;
    boolean firstUpper = false;
    boolean seenLetter = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!Character.isLetter(c)) {
        continue;
      }
      boolean isUpper = Character.isUpperCase(c);
      if (!seenLetter) {
        firstUpper = isUpper;
        seenLetter = true;
      }
      if (isUpper) {
        upper++;
      } else {
        lower++;
      }
    }
    if (!seenLetter) {
      return CasingShape.NON_ALPHA;
    }
    if (upper == 0) {
      return CasingShape.LOWER;
    }
    if (lower == 0) {
      return upper >= 2 ? CasingShape.UPPER : CasingShape.TITLE;
    }
    if (firstUpper && upper == 1) {
      return CasingShape.TITLE;
    }
    int ap = Math.max(s.indexOf('\''), s.indexOf('’'));
    if (ap > 0 && firstUpper) {
      return classifyApostropheStemShape(s, ap);
    }
    return CasingShape.MIXED;
  }

  private static CasingShape classifyApostropheStemShape(String s, int apostropheIndex) {
    int stemUpperAfterFirst = 0;
    int stemLowerAfterFirst = 0;
    for (int i = 1; i < apostropheIndex; i++) {
      char c = s.charAt(i);
      if (!Character.isLetter(c)) {
        continue;
      }
      if (Character.isUpperCase(c)) {
        stemUpperAfterFirst++;
      } else {
        stemLowerAfterFirst++;
      }
    }
    if (stemUpperAfterFirst == 0) {
      return CasingShape.TITLE;
    }
    if (stemLowerAfterFirst == 0) {
      return CasingShape.UPPER;
    }
    return CasingShape.MIXED;
  }

  public static List<SpanToken> spanTokenize(String sentence) {
    List<Token> raw = TurkishTokenizer.DEFAULT.tokenize(sentence);
    List<SpanToken> out = new ArrayList<>(raw.size());
    int prevEnd = -2;
    for (int i = 0; i < raw.size(); i++) {
      Token t = raw.get(i);
      String surf = t.getText();
      String low = trLower(surf);
      int start = t.getStart();
      int end = t.getEnd();
      out.add(
          new SpanToken(
              i,
              surf,
              low,
              toAscii(low),
              t.getType(),
              detectShape(surf),
              start,
              end,
              i > 0 && start == prevEnd + 1));
      prevEnd = end;
    }
    return out;
  }

  /**
   * Reconstructs natural Turkish surface text from a decoded non-overlapping {@link SpanArc} path,
   * preserving punctuation attachment, quotes, numeric decimals/ranges, emoticons, and emojis.
   */
  public static String reconstructText(List<SpanToken> tokens, List<SpanArc> path) {
    if (path.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(tokens.size() * 6);
    boolean prevWasOpening = false;
    boolean prevAttachNext = false;
    boolean inDoubleQuotes = false;
    boolean inSingleQuotes = false;
    String prevVal = "";
    Token.Type prevType = null;

    for (SpanArc arc : path) {
      String val = arc.normSurface;
      if (val == null || val.isEmpty()) {
        continue;
      }
      SpanToken firstTok = tokens.get(arc.startIdx);
      SpanToken lastTok = tokens.get(Math.min(tokens.size() - 1, arc.endIdx - 1));
      Token.Type tokType = arc.wordCount == 1 ? firstTok.type() : Token.Type.Word;
      boolean rawAdj = firstTok.adjacentToPrev();

      boolean isSinglePunct = tokType == Token.Type.Punctuation && val.length() == 1;
      boolean attachToPrev = false;
      boolean nextOpening = false;
      boolean nextAttach = false;

      if (val.startsWith("'") && rawAdj) {
        attachToPrev = true;
      } else if (rawAdj && tokType == Token.Type.Emoji && prevType == Token.Type.Emoji) {
        attachToPrev = true;
      } else if (rawAdj && isEmoticonSuffix(prevType, prevVal, tokType, val)) {
        attachToPrev = true;
      } else if (!rawAdj && isEmoticonEyesBeforeMouth(tokens, arc, val)) {
        attachToPrev = false;
      } else if (rawAdj && isNumericRangeDash(prevVal, val)) {
        attachToPrev = true;
      } else if (isSinglePunct && "\"".equals(val)) {
        attachToPrev = inDoubleQuotes;
        nextOpening = !inDoubleQuotes;
        inDoubleQuotes = !inDoubleQuotes;
      } else if (isSinglePunct && ("'".equals(val) || "’".equals(val))) {
        attachToPrev = inSingleQuotes;
        nextOpening = !inSingleQuotes;
        inSingleQuotes = !inSingleQuotes;
      } else if (isSinglePunct && "/".equals(val)) {
        if (rawAdj) {
          attachToPrev = true;
          nextAttach = true;
        }
      } else if (tokType == Token.Type.Punctuation && allTrailingPunct(val)) {
        attachToPrev = !"%".equals(val) || rawAdj;
        if (startsDecimalSeparator(isSinglePunct, rawAdj, prevVal, val)
            || ("%".equals(val) && !attachToPrev)) {
          nextAttach = true;
        }
      } else if (isSinglePunct && OPENING_PUNCT.contains(val.charAt(0))) {
        nextOpening = true;
      }

      if (prevAttachNext && rawAdj && continuesSlashDecimalOrPercent(prevVal, val)) {
        attachToPrev = true;
      }

      if (sb.isEmpty() || prevWasOpening || attachToPrev) {
        sb.append(val);
      } else {
        sb.append(' ').append(val);
      }

      prevWasOpening = nextOpening;
      prevAttachNext = nextAttach;
      prevVal = val;
      prevType =
          (prevType == Token.Type.Emoticon && attachToPrev)
              ? Token.Type.Emoticon
              : lastTok.type();
    }
    return sb.toString();
  }

  private static boolean isEmoticonSuffix(
      Token.Type prevType, String prevVal, Token.Type tokType, String val) {
    if (prevType == Token.Type.Emoticon && tokType == Token.Type.Punctuation) {
      return allCharsIn(val, EMOTICON_EXTRAS);
    }
    return (":".equals(prevVal) || ";".equals(prevVal)) && allCharsIn(val, EMOTICON_MOUTHS);
  }

  private static boolean isEmoticonEyesBeforeMouth(
      List<SpanToken> tokens, SpanArc arc, String val) {
    if (!":".equals(val) && !";".equals(val)) {
      return false;
    }
    if (arc.endIdx >= tokens.size()) {
      return false;
    }
    SpanToken next = tokens.get(arc.endIdx);
    return next.adjacentToPrev() && allCharsIn(next.surface(), EMOTICON_MOUTHS);
  }

  private static boolean isNumericRangeDash(String prevVal, String val) {
    return val.length() > 1
        && val.charAt(0) == '-'
        && Character.isDigit(val.charAt(1))
        && endsWithDigit(prevVal);
  }

  private static boolean startsDecimalSeparator(
      boolean isSinglePunct, boolean rawAdj, String prevVal, String val) {
    return isSinglePunct
        && rawAdj
        && (".".equals(val) || ",".equals(val))
        && endsWithDigit(prevVal);
  }

  private static boolean continuesSlashDecimalOrPercent(String prevVal, String val) {
    if ("/".equals(prevVal)) {
      return true;
    }
    return (".".equals(prevVal) || ",".equals(prevVal) || "%".equals(prevVal))
        && Character.isDigit(val.charAt(0));
  }

  private static boolean endsWithDigit(String s) {
    return !s.isEmpty() && Character.isDigit(s.charAt(s.length() - 1));
  }

  private static boolean allCharsIn(String s, String allowed) {
    for (int i = 0; i < s.length(); i++) {
      if (allowed.indexOf(s.charAt(i)) < 0) {
        return false;
      }
    }
    return !s.isEmpty();
  }

  private static boolean allTrailingPunct(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (!TRAILING_PUNCT.contains(s.charAt(i))) {
        return false;
      }
    }
    return !s.isEmpty();
  }
}
