package zemberek.normalization.deasciifier.latticedeasciifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import zemberek.core.turkish.TurkishAlphabet;

/**
 * Orthographic and tokenization utilities for Turkish deasciification.
 *
 * <p>Preserves exact character offsets, punctuation, pinned Turkish diacritics, URLs/emails/handles,
 * alphanumeric identifiers, Roman numerals, CamelCase brands, numeral+apostrophe suffixes
 * ({@code 3'üncü}, {@code 6'şar}, {@code 1923'ün}), and expressive elongations
 * ({@code coooook -> çooooook}).
 */
public final class TurkishTextCodec {

  public enum Casing {
    LOWER,
    TITLE,
    UPPER,
    MIXED
  }

  /** Character-accurate token span in an input text. */
  public record Token(
      int start,
      int end,
      String original,
      String asciiLower,
      Casing casing,
      int apostropheIndex,
      boolean numeralApostrophe,
      boolean immutable,
      boolean sentenceStart,
      char rightPunct,
      boolean hasPinnedTurkish) {

    public boolean hasApostrophe() {
      return apostropheIndex > 0 && apostropheIndex < original.length() - 1;
    }

    public boolean isPlainCacheableWord() {
      return !immutable
          && !hasPinnedTurkish
          && !numeralApostrophe
          && apostropheIndex < 0
          && (casing == Casing.LOWER || casing == Casing.TITLE);
    }

    /**
     * Returns a copy treating dotless {@code 'ı'} as unpinned ASCII {@code 'i'}, used only when a
     * sentence lacks non-{@code 'ı'} Turkish diacritics (e.g. after {@code
     * toLowerCase(Turkish.LOCALE)}) and {@code original} has no valid analysis with {@code 'ı'}.
     */
    Token withoutDotlessIPin() {
      return new Token(
          start,
          end,
          original,
          asciiLower,
          casing,
          apostropheIndex,
          numeralApostrophe,
          immutable,
          sentenceStart,
          rightPunct,
          containsNonDotlessTurkishDiacritic(original));
    }
  }

  /** Mapping from a squeezed elongation form back to original character run offsets. */
  public record SqueezedWord(String squeezedAscii, int[] runStarts, int[] runEnds) {
    public String projectToOriginalLength(String restoredSqueezedLower) {
      if (restoredSqueezedLower.length() != runStarts.length) {
        return restoredSqueezedLower;
      }
      int origLen = runEnds[runEnds.length - 1];
      char[] out = new char[origLen];
      for (int i = 0; i < runStarts.length; i++) {
        char ch = restoredSqueezedLower.charAt(i);
        for (int p = runStarts[i]; p < runEnds[i]; p++) {
          out[p] = ch;
        }
      }
      return new String(out);
    }
  }

  private static final Pattern ROMAN_NUMERAL =
      Pattern.compile("^(?:X{1,3}(?:IX|IV|V?I{0,3})|IX|IV|V?I{1,3}|V)$");

  private static final Pattern DOMAIN_NAME =
      Pattern.compile(
          "^[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+\\.(?:com|org|net|edu|gov|tr|io|dev|ai|co|info|biz)(?:/.*)?$",
          Pattern.CASE_INSENSITIVE);

  @FunctionalInterface
  private interface CharMapper {
    char map(char c);
  }

  private record ScannedSpan(int end, int apostropheIndex) {}

  private TurkishTextCodec() {}

  private static String mapChars(String s, CharMapper mapper) {
    int n = s.length();
    char[] out = null;
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      char mapped = mapper.map(c);
      if (mapped != c && out == null) {
        out = s.toCharArray();
      }
      if (out != null) {
        out[i] = mapped;
      }
    }
    return out == null ? s : new String(out);
  }

  public static boolean isApostrophe(char c) {
    return c == '\'' || c == '’' || c == '‘' || c == '`';
  }

  public static boolean isLetter(char c) {
    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
      return true;
    }
    return isPinnedTurkishDiacritic(c);
  }

  public static boolean isPinnedTurkishDiacritic(char c) {
    return TurkishAlphabet.INSTANCE.isTurkishSpecific(c);
  }

  public static boolean isNonDotlessTurkishDiacritic(char c) {
    return isPinnedTurkishDiacritic(c) && c != 'ı';
  }

  public static boolean containsPinnedTurkish(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (isPinnedTurkishDiacritic(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsNonDotlessTurkishDiacritic(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (isNonDotlessTurkishDiacritic(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsDotlessI(String s) {
    return s.indexOf('ı') >= 0;
  }

  public static char toTurkishLower(char c) {
    if (c == 'I') {
      return 'ı';
    }
    if (c == 'İ') {
      return 'i';
    }
    return (c >= 'A' && c <= 'Z') ? (char) (c + 32) : Character.toLowerCase(c);
  }

  public static String toTurkishLower(String s) {
    return mapChars(s, TurkishTextCodec::toTurkishLower);
  }

  public static char toTurkishUpper(char c) {
    if (c == 'i') {
      return 'İ';
    }
    if (c == 'ı') {
      return 'I';
    }
    return (c >= 'a' && c <= 'z') ? (char) (c - 32) : Character.toUpperCase(c);
  }

  public static char toAsciiLower(char c) {
    if (c >= 'a' && c <= 'z') {
      return c;
    }
    char ascii = TurkishAlphabet.INSTANCE.getAsciiEqual(c);
    if (ascii >= 'a' && ascii <= 'z') {
      return ascii;
    }
    if (ascii >= 'A' && ascii <= 'Z') {
      return (char) (ascii + 32);
    }
    return isApostrophe(ascii) ? '\'' : Character.toLowerCase(ascii);
  }

  public static String toAsciiLower(String s) {
    return mapChars(s, TurkishTextCodec::toAsciiLower);
  }

  public static char toAsciiPreserveCase(char c) {
    return TurkishAlphabet.INSTANCE.getAsciiEqual(c);
  }

  public static String asciify(String text) {
    return mapChars(text, TurkishTextCodec::toAsciiPreserveCase);
  }

  public static char stripCircumflex(char c) {
    return switch (c) {
      case 'â' -> 'a';
      case 'Â' -> 'A';
      case 'î' -> 'i';
      case 'Î' -> 'I';
      case 'û' -> 'u';
      case 'Û' -> 'U';
      default -> c;
    };
  }

  public static String stripCircumflex(String s) {
    return mapChars(s, TurkishTextCodec::stripCircumflex);
  }

  public static boolean hasAmbiguousAscii(String asciiLower) {
    for (int i = 0; i < asciiLower.length(); i++) {
      switch (asciiLower.charAt(i)) {
        case 'a', 'c', 'g', 'i', 'o', 's', 'u' -> {
          return true;
        }
        default -> {}
      }
    }
    return false;
  }

  public static boolean hasCoreDiacriticAscii(String asciiLower) {
    for (int i = 0; i < asciiLower.length(); i++) {
      switch (asciiLower.charAt(i)) {
        case 'c', 'g', 'i', 'o', 's', 'u' -> {
          return true;
        }
        default -> {}
      }
    }
    return false;
  }

  public static boolean hasWordFinalDoubleLetter(String asciiLower) {
    int n = asciiLower.length();
    if (n < 5) {
      return false;
    }
    char last = asciiLower.charAt(n - 1);
    return last >= 'a' && last <= 'z' && last == asciiLower.charAt(n - 2);
  }

  public static int vowelIndex(char c) {
    return switch (c) {
      case 'a', 'â' -> 0;
      case 'e' -> 1;
      case 'ı' -> 2;
      case 'i', 'î' -> 3;
      case 'o' -> 4;
      case 'ö' -> 5;
      case 'u' -> 6;
      case 'ü', 'û' -> 7;
      default -> -1;
    };
  }

  public static boolean isTurkishVowel(char c) {
    return vowelIndex(c) >= 0;
  }

  public static boolean isFrontVowel(char c) {
    return c == 'e' || c == 'i' || c == 'î' || c == 'ö' || c == 'ü' || c == 'û';
  }

  public static boolean isRoundedVowel(char c) {
    return c == 'o' || c == 'ö' || c == 'u' || c == 'ü' || c == 'û';
  }

  public static char highHarmonicVowel(boolean front, boolean rounded) {
    return front ? (rounded ? 'ü' : 'i') : (rounded ? 'u' : 'ı');
  }

  /**
   * Checks that {@code candidateLower} folds to the same ASCII letters as {@code inputOriginal} and
   * never alters any Turkish diacritic already present in {@code inputOriginal}.
   */
  public static boolean isCompatibleWithInput(String inputOriginal, String candidateLower) {
    return isCompatibleWithInput(inputOriginal, candidateLower, true);
  }

  public static boolean isCompatibleWithInput(
      String inputOriginal, String candidateLower, boolean pinDotlessI) {
    if (inputOriginal.length() != candidateLower.length()) {
      return false;
    }
    for (int i = 0; i < inputOriginal.length(); i++) {
      char orig = inputOriginal.charAt(i);
      char cand = candidateLower.charAt(i);
      if (isApostrophe(orig) && isApostrophe(cand)) {
        continue;
      }
      boolean isPin = pinDotlessI ? isPinnedTurkishDiacritic(orig) : isNonDotlessTurkishDiacritic(orig);
      if (isPin) {
        if (toTurkishLower(orig) != cand) {
          return false;
        }
      } else if (toAsciiLower(orig) != toAsciiLower(cand)) {
        return false;
      }
    }
    return true;
  }

  /** Projects original uppercase/lowercase shape and pinned diacritics onto {@code chosen}. */
  public static String applyCasingAndPins(Token token, Candidate chosen) {
    return applyCasingAndPins(token, chosen.surface, chosen.foreignStemLength);
  }

  /** Projects original uppercase/lowercase shape and pinned diacritics onto {@code candLower}. */
  public static String applyCasingAndPins(Token token, String candLower, int foreignStemLength) {
    String orig = token.original;
    if (token.casing == Casing.LOWER && !token.hasPinnedTurkish && foreignStemLength == 0) {
      return candLower;
    }
    if (orig.length() != candLower.length()) {
      return orig;
    }
    char[] out = new char[orig.length()];
    for (int i = 0; i < orig.length(); i++) {
      char o = orig.charAt(i);
      boolean pinned = token.hasPinnedTurkish && isPinnedTurkishDiacritic(o);
      if (isApostrophe(o) || !isLetter(o) || pinned || i < foreignStemLength) {
        out[i] = o;
      } else {
        char cl = candLower.charAt(i);
        if (toAsciiLower(o) != toAsciiLower(cl)) {
          out[i] = o;
        } else {
          out[i] = Character.isUpperCase(o) ? toTurkishUpper(cl) : cl;
        }
      }
    }
    return new String(out);
  }

  public static Casing detectCasing(String text, int limitLength) {
    int len = limitLength > 0 ? Math.min(text.length(), limitLength) : text.length();
    int upper = 0;
    int lower = 0;
    boolean firstUpper = false;
    boolean seenLetter = false;
    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
      if (!isLetter(c)) {
        continue;
      }
      boolean isUp = Character.isUpperCase(c);
      if (!seenLetter) {
        firstUpper = isUp;
        seenLetter = true;
      }
      if (isUp) {
        upper++;
      } else {
        lower++;
      }
    }
    if (upper == 0) {
      return Casing.LOWER;
    }
    if (lower == 0) {
      return Casing.UPPER;
    }
    return (firstUpper && upper == 1) ? Casing.TITLE : Casing.MIXED;
  }

  public static boolean isCamelCaseIdentifier(String orig) {
    if (orig.length() < 3 || containsPinnedTurkish(orig)) {
      return false;
    }
    boolean seenLower = false;
    for (int i = 0; i < orig.length(); i++) {
      char c = orig.charAt(i);
      if (isApostrophe(c)) {
        break;
      }
      if (Character.isLowerCase(c)) {
        seenLower = true;
      } else if (Character.isUpperCase(c) && seenLower) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsDigit(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isDigit(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /** Tokenizes {@code text} into character-accurate {@link Token} spans. */
  public static List<Token> tokenize(String text) {
    int length = text.length();
    List<Token> tokens = new ArrayList<>(Math.max(4, length / 6));
    boolean nextIsSentenceStart = true;

    int pos = 0;
    while (pos < length) {
      while (pos < length && Character.isWhitespace(text.charAt(pos))) {
        char ws = text.charAt(pos);
        if (ws == '\n' || ws == '\r') {
          nextIsSentenceStart = true;
        }
        pos++;
      }
      if (pos >= length) {
        break;
      }
      int chunkStart = pos;
      while (pos < length && !Character.isWhitespace(text.charAt(pos))) {
        pos++;
      }
      int chunkEnd = pos;

      if (isImmutableChunk(text, chunkStart, chunkEnd)) {
        String orig = text.substring(chunkStart, chunkEnd);
        tokens.add(
            new Token(
                chunkStart,
                chunkEnd,
                orig,
                toAsciiLower(orig),
                Casing.MIXED,
                -1,
                false,
                true,
                nextIsSentenceStart,
                ' ',
                containsPinnedTurkish(orig)));
        nextIsSentenceStart = false;
        continue;
      }

      int i = chunkStart;
      while (i < chunkEnd) {
        char c = text.charAt(i);
        if (c == '.' || c == '!' || c == '?' || c == '…') {
          nextIsSentenceStart = true;
          i++;
          continue;
        }
        if (!isLetter(c) && !Character.isDigit(c)) {
          i++;
          continue;
        }

        int tokStart = i;
        boolean startsWithDigit = Character.isDigit(c);
        ScannedSpan span =
            startsWithDigit
                ? scanNumberSpan(text, tokStart, chunkEnd)
                : scanWordSpan(text, tokStart, chunkEnd);
        i = span.end();

        char rightPunct = findRightPunct(text, span.end(), chunkEnd, length);
        Token tok =
            buildSpanToken(
                text,
                tokStart,
                span,
                startsWithDigit,
                nextIsSentenceStart,
                rightPunct);
        tokens.add(tok);

        nextIsSentenceStart =
            (rightPunct == '.' || rightPunct == '!' || rightPunct == '?')
                && !(casingLooksLikeInitialOrRoman(tok.original()) && rightPunct == '.');
      }
    }
    return tokens;
  }

  private static ScannedSpan scanNumberSpan(String text, int start, int chunkEnd) {
    int i = start;
    int aposRel = -1;
    while (i < chunkEnd) {
      char ch = text.charAt(i);
      if (Character.isDigit(ch)) {
        i++;
      } else if ((ch == '.' || ch == ',') && i + 1 < chunkEnd && Character.isDigit(text.charAt(i + 1))) {
        i++;
      } else if (aposRel < 0 && isApostrophe(ch) && i + 1 < chunkEnd && isLetter(text.charAt(i + 1))) {
        aposRel = i - start;
        i++;
      } else if (aposRel >= 0 && isLetter(ch)) {
        i++;
      } else {
        break;
      }
    }
    return new ScannedSpan(i, aposRel);
  }

  private static ScannedSpan scanWordSpan(String text, int start, int chunkEnd) {
    int i = start;
    int aposRel = -1;
    while (i < chunkEnd) {
      char ch = text.charAt(i);
      if (isLetter(ch) || Character.isDigit(ch)) {
        i++;
      } else if (isApostrophe(ch) && i + 1 < chunkEnd && isLetter(text.charAt(i + 1))) {
        if (aposRel < 0) {
          aposRel = i - start;
        }
        i++;
      } else {
        break;
      }
    }
    return new ScannedSpan(i, aposRel);
  }

  private static Token buildSpanToken(
      String text,
      int tokStart,
      ScannedSpan span,
      boolean startsWithDigit,
      boolean sentenceStart,
      char rightPunct) {
    int tokEnd = span.end();
    int aposRel = span.apostropheIndex();
    String orig = text.substring(tokStart, tokEnd);
    boolean numeralApos = startsWithDigit && aposRel > 0;
    if (startsWithDigit && !numeralApos) {
      return new Token(
          tokStart, tokEnd, orig, orig, Casing.LOWER, -1, false, true, sentenceStart, rightPunct, false);
    }
    Casing casing = detectCasing(orig, aposRel > 0 ? aposRel : orig.length());
    // Un-apostrophized alphanumeric tokens ("cimcime50", "covid19", "mp3") are usernames/codes.
    boolean immutable =
        isRomanNumeral(orig)
            || (aposRel < 0 && (isCamelCaseIdentifier(orig) || containsDigit(orig)));
    return new Token(
        tokStart,
        tokEnd,
        orig,
        toAsciiLower(orig),
        casing,
        aposRel,
        numeralApos,
        immutable,
        sentenceStart,
        rightPunct,
        containsPinnedTurkish(orig));
  }

  private static boolean casingLooksLikeInitialOrRoman(String orig) {
    return orig.length() == 1 || isRomanNumeral(orig);
  }

  private static char findRightPunct(String text, int tokEnd, int chunkEnd, int totalLen) {
    if (tokEnd < chunkEnd) {
      return text.charAt(tokEnd);
    }
    int look = tokEnd;
    while (look < totalLen && text.charAt(look) == ' ') {
      look++;
    }
    if (look < totalLen) {
      char p = text.charAt(look);
      if (p == '?' || p == '!' || p == '.' || p == ',' || p == ';' || p == ':') {
        return p;
      }
    }
    return ' ';
  }

  private static boolean isRomanNumeral(String orig) {
    if (orig.length() < 2 || orig.length() > 8) {
      return false;
    }
    for (int i = 0; i < orig.length(); i++) {
      char c = orig.charAt(i);
      if (c != 'I' && c != 'V' && c != 'X') {
        return false;
      }
    }
    return ROMAN_NUMERAL.matcher(orig).matches();
  }

  private static boolean isImmutableChunk(String text, int start, int end) {
    int s = start;
    int e = end;
    while (s < e && "([{\"<'“‘".indexOf(text.charAt(s)) >= 0) {
      s++;
    }
    while (e > s && ")]}\">'”’.,;:!?".indexOf(text.charAt(e - 1)) >= 0) {
      e--;
    }
    if (e - s <= 1) {
      return false;
    }
    if (text.charAt(s) == '@') {
      return true;
    }

    boolean hasMarker = false;
    for (int i = s; i < e; i++) {
      char c = text.charAt(i);
      if (c == '_' || c == '@' || c == ':' || c == '/' || c == '.') {
        hasMarker = true;
        break;
      }
    }
    if (!hasMarker) {
      return false;
    }

    String chunk = text.substring(s, e);
    if (chunk.indexOf('_') >= 0 || chunk.contains("://")) {
      return true;
    }
    String lower = chunk.toLowerCase(Locale.ROOT);
    if (lower.startsWith("http://")
        || lower.startsWith("https://")
        || lower.startsWith("ftp://")
        || lower.startsWith("www.")
        || lower.startsWith("mailto:")) {
      return true;
    }
    int atIdx = chunk.indexOf('@');
    if (atIdx > 0 && atIdx < chunk.length() - 1 && chunk.indexOf('.', atIdx) > atIdx) {
      return true;
    }
    if (chunk.startsWith("/") || chunk.startsWith("./") || chunk.startsWith("../")) {
      return true;
    }
    return chunk.indexOf('.') > 0 && !chunk.endsWith(".") && DOMAIN_NAME.matcher(chunk).matches();
  }

  public static boolean hasLetterElongation(String asciiLower) {
    int run = 1;
    for (int i = 1; i < asciiLower.length(); i++) {
      if (asciiLower.charAt(i) == asciiLower.charAt(i - 1)) {
        if (++run >= 3) {
          return true;
        }
      } else {
        run = 1;
      }
    }
    return false;
  }

  public static SqueezedWord squeezeElongations(String asciiLower, int maxRunLength) {
    int n = asciiLower.length();
    StringBuilder sb = new StringBuilder(n);
    int[] starts = new int[n];
    int[] ends = new int[n];
    int count = 0;

    int i = 0;
    while (i < n) {
      char c = asciiLower.charAt(i);
      int j = i + 1;
      if (c >= 'a' && c <= 'z') {
        while (j < n && asciiLower.charAt(j) == c) {
          j++;
        }
      }
      if (j - i >= 3) {
        if (maxRunLength == 2) {
          sb.append(c);
          starts[count] = i;
          ends[count++] = i + 1;
          sb.append(c);
          starts[count] = i + 1;
          ends[count++] = j;
        } else {
          sb.append(c);
          starts[count] = i;
          ends[count++] = j;
        }
      } else {
        for (int k = i; k < j; k++) {
          sb.append(asciiLower.charAt(k));
          starts[count] = k;
          ends[count++] = k + 1;
        }
      }
      i = j;
    }
    int[] finalStarts = new int[count];
    int[] finalEnds = new int[count];
    System.arraycopy(starts, 0, finalStarts, 0, count);
    System.arraycopy(ends, 0, finalEnds, 0, count);
    return new SqueezedWord(sb.toString(), finalStarts, finalEnds);
  }

  /**
   * Restores Turkish vowel harmony and distributive consonants on suffixes attached to numerals via
   * an apostrophe ({@code 3'uncu -> 3'üncü}, {@code 6'sar -> 6'şar}, {@code 1923'un -> 1923'ün},
   * {@code 00.00'dan -> 00.00'dan}).
   */
  public static String restoreNumeralApostrophe(Token token) {
    String numPart = token.original.substring(0, token.apostropheIndex);
    char apos = token.original.charAt(token.apostropheIndex);
    String suffixAscii = token.asciiLower.substring(token.apostropheIndex + 1);
    String spokenWord = spokenTurkishNumberStem(numPart);

    boolean front = true;
    boolean rounded = false;
    for (int i = spokenWord.length() - 1; i >= 0; i--) {
      char v = spokenWord.charAt(i);
      if (isTurkishVowel(v)) {
        front = isFrontVowel(v);
        rounded = isRoundedVowel(v);
        break;
      }
    }

    boolean distributive =
        suffixAscii.startsWith("sar") || suffixAscii.startsWith("ser");

    char[] suffix = new char[suffixAscii.length()];
    for (int i = 0; i < suffixAscii.length(); i++) {
      char raw = token.original.charAt(token.apostropheIndex + 1 + i);
      if (token.hasPinnedTurkish && isPinnedTurkishDiacritic(raw)) {
        char tl = toTurkishLower(raw);
        suffix[i] = tl;
        if (isTurkishVowel(tl)) {
          front = isFrontVowel(tl);
          rounded = isRoundedVowel(tl);
        }
        continue;
      }
      char c = suffixAscii.charAt(i);
      if (i == 0 && distributive && c == 's') {
        suffix[i] = 'ş';
      } else if (c == 'e') {
        front = true;
        rounded = false;
        suffix[i] = 'e';
      } else if (c == 'a') {
        front = false;
        rounded = false;
        suffix[i] = 'a';
      } else if (c == 'i' || c == 'u') {
        suffix[i] = highHarmonicVowel(front, rounded);
      } else {
        suffix[i] = c;
      }
    }
    return toAsciiLower(numPart) + apos + new String(suffix);
  }

  public static String spokenTurkishNumberStem(String rawNumber) {
    if (rawNumber == null || rawNumber.isEmpty()) {
      return "sıfır";
    }
    int lastSep = Math.max(rawNumber.lastIndexOf('.'), rawNumber.lastIndexOf(','));
    String activeDigits;
    if (lastSep >= 0) {
      String tail = rawNumber.substring(lastSep + 1);
      if (tail.equals("00")) {
        String headDigits = digitsOnly(rawNumber.substring(0, lastSep));
        if (allZeros(headDigits)) {
          return "sıfır";
        }
        activeDigits = headDigits;
      } else if (tail.length() == 3 && rawNumber.charAt(lastSep) == '.') {
        activeDigits = digitsOnly(rawNumber);
      } else {
        activeDigits = digitsOnly(tail);
      }
    } else {
      activeDigits = digitsOnly(rawNumber);
    }

    if (activeDigits.isEmpty() || allZeros(activeDigits)) {
      return "sıfır";
    }
    int len = activeDigits.length();
    char last = activeDigits.charAt(len - 1);
    if (last != '0') {
      return switch (last) {
        case '1' -> "bir";
        case '2' -> "iki";
        case '3' -> "üç";
        case '4' -> "dört";
        case '5' -> "beş";
        case '6' -> "altı";
        case '7' -> "yedi";
        case '8' -> "sekiz";
        case '9' -> "dokuz";
        default -> "sıfır";
      };
    }

    int trailingZeros = 0;
    for (int i = len - 1; i >= 0 && activeDigits.charAt(i) == '0'; i--) {
      trailingZeros++;
    }
    if (trailingZeros == 1 && len >= 2) {
      return switch (activeDigits.charAt(len - 2)) {
        case '1' -> "on";
        case '2' -> "yirmi";
        case '3' -> "otuz";
        case '4' -> "kırk";
        case '5' -> "elli";
        case '6' -> "altmış";
        case '7' -> "yetmiş";
        case '8' -> "seksen";
        case '9' -> "doksan";
        default -> "sıfır";
      };
    }
    if (trailingZeros == 2) {
      return "yüz";
    }
    if (trailingZeros >= 3 && trailingZeros <= 5) {
      return "bin";
    }
    if (trailingZeros >= 6 && trailingZeros <= 8) {
      return "milyon";
    }
    if (trailingZeros >= 9 && trailingZeros <= 11) {
      return "milyar";
    }
    return "trilyon";
  }

  private static String digitsOnly(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isDigit(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static boolean allZeros(String digits) {
    if (digits.isEmpty()) {
      return true;
    }
    for (int i = 0; i < digits.length(); i++) {
      if (digits.charAt(i) != '0') {
        return false;
      }
    }
    return true;
  }
}
