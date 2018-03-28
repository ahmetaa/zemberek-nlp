package zemberek.core.turkish;

import static zemberek.core.turkish.TurkicLetter.builder;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Locale;
import zemberek.core.text.TextUtil;

/**
 * Contains Turkish Letters, Turkish Letter equivalent chars, several helper methods.
 * TurkishAlphabet only contains small case letters.
 */
// TODO: document or fix. many methods only work for lowercase input.
public class TurkishAlphabet {

  // letters used in turkish text having circumflex.
  public static final char A_CIRC = 'Â'; // Â
  public static final char a_CIRC = 'â'; // â
  public static final char I_CIRC = 'Î'; // Î
  public static final char i_CIRC = 'î'; // î
  public static final char U_CIRC = 'Û'; // Û
  public static final char u_CIRC = 'û'; // û
  /**
   * Turkish Letters. q,x,w is also added for foreign proper nouns. They are marked as 'foreign'
   */
  public static final TurkicLetter L_a = builder('a').vowel().build();
  public static final TurkicLetter L_b = builder('b').build();
  public static final TurkicLetter L_c = builder('c').build();
  public static final TurkicLetter L_cc = builder('ç').notInAscii().voiceless()
      .similarAscii('c').build();
  public static final TurkicLetter L_d = builder('d').build();
  public static final TurkicLetter L_e = builder('e').vowel().frontalVowel().build();
  public static final TurkicLetter L_f = builder('f').continuant().voiceless().build();
  public static final TurkicLetter L_g = builder('g').build();
  public static final TurkicLetter L_gg = builder('ğ').continuant().notInAscii()
      .similarAscii('g').build();
  public static final TurkicLetter L_h = builder('h').continuant().voiceless().build();
  public static final TurkicLetter L_ii = builder('ı').vowel().notInAscii().similarAscii('i')
      .build();
  public static final TurkicLetter L_i = builder('i').vowel().frontalVowel().build();
  public static final TurkicLetter L_j = builder('j').continuant().build();
  public static final TurkicLetter L_k = builder('k').voiceless().build();
  public static final TurkicLetter L_l = builder('l').continuant().build();
  public static final TurkicLetter L_m = builder('m').continuant().build();
  public static final TurkicLetter L_n = builder('n').continuant().build();
  public static final TurkicLetter L_o = builder('o').vowel().roundedVowel().build();
  public static final TurkicLetter L_oo = builder('ö').vowel().frontalVowel().roundedVowel()
      .notInAscii().similarAscii('o').build();
  public static final TurkicLetter L_p = builder('p').voiceless().build();
  public static final TurkicLetter L_r = builder('r').continuant().build();
  public static final TurkicLetter L_s = builder('s').continuant().voiceless().build();
  public static final TurkicLetter L_ss = builder('ş').continuant().notInAscii().voiceless()
      .similarAscii('s').build();
  public static final TurkicLetter L_t = builder('t').voiceless().build();
  public static final TurkicLetter L_u = builder('u').vowel().roundedVowel().build();
  public static final TurkicLetter L_uu = builder('ü').vowel().roundedVowel().frontalVowel()
      .similarAscii('u').notInAscii().build();
  public static final TurkicLetter L_v = builder('v').continuant().build();
  public static final TurkicLetter L_y = builder('y').continuant().build();
  public static final TurkicLetter L_z = builder('z').continuant().build();
  // Not Turkish but sometimes appears in geographical names etc.
  public static final TurkicLetter L_q = builder('q').foreign().build();
  public static final TurkicLetter L_w = builder('w').foreign().build();
  public static final TurkicLetter L_x = builder('x').foreign().build();
  // Circumflexed letters
  public static final TurkicLetter L_ac = builder(a_CIRC).vowel().similarAscii('a').notInAscii()
      .build();
  public static final TurkicLetter L_ic = builder(i_CIRC).vowel().frontalVowel()
      .similarAscii('i').notInAscii().build();
  public static final TurkicLetter L_uc = builder(u_CIRC).vowel().frontalVowel()
      .similarAscii('u').roundedVowel().notInAscii().build();

  protected static final ImmutableMap<TurkicLetter, TurkicLetter> devoicingMap = new ImmutableMap.Builder<TurkicLetter, TurkicLetter>()
      .put(L_b, L_p)
      .put(L_c, L_cc)
      .put(L_d, L_t)
      .put(L_g, L_k)
      .put(L_gg, L_k)
      .build();
  protected static final ImmutableMap<TurkicLetter, TurkicLetter> voicingMap = new ImmutableMap.Builder<TurkicLetter, TurkicLetter>()
      .
          put(L_p, L_b).
          put(L_k, L_gg).
          put(L_cc, L_c).
          put(L_t, L_d).
          put(L_g, L_gg).
          build();
  static final Locale TR = new Locale("tr");
  static final TurkicLetter[] TURKISH_LETTERS = {
      L_a, L_b, L_c, L_cc, L_d, L_e, L_f, L_g,
      L_gg, L_h, L_ii, L_i, L_j, L_k, L_l, L_m,
      L_n, L_o, L_oo, L_p, L_r, L_s, L_ss, L_t,
      L_u, L_uu, L_v, L_y, L_z, L_q, L_w, L_x,
      L_ac, L_ic, L_uc
  };
  public static final int ALPHABET_LETTER_COUNT = TURKISH_LETTERS.length;
  // 0x15f is the maximum char value in turkish specific characters. It is the size
  // of our lookup tables. This could be done better, but for now it works.
  private static final int MAX_CHAR_VALUE = 0x20ac + 1;
  private static final TurkicLetter[] CHAR_TO_LETTER_LOOKUP = new TurkicLetter[MAX_CHAR_VALUE];

  private static final int[] TURKISH_ALPHABET_INDEXES = new int[MAX_CHAR_VALUE];
  private static final boolean[] VOWEL_TABLE = new boolean[MAX_CHAR_VALUE];
  private static final boolean[] VALID_CHAR_TABLE = new boolean[MAX_CHAR_VALUE];
  // ------------------------ ASCII equivalency ----------------------------------
  // This lookup table maps each Turkish letter to its ASCII counterpart.
  private static final TurkicLetter[] ASCII_EQUIVALENT_LETTER_LOOKUP = {
      L_a, L_b, L_c, L_c, L_d, L_e, L_f, L_g,
      L_g, L_h, L_i, L_i, L_j, L_k, L_l, L_m,
      L_n, L_o, L_o, L_p, L_r, L_s, L_s, L_t,
      L_u, L_u, L_v, L_y, L_z, L_q, L_w, L_x,
      L_a, L_i, L_u};
  public static TurkishAlphabet INSTANCE = Singleton.Instance.alphabet;
  private static char[] ASCII_EQUIVALENT_CHARS_LOOKUP = new char[MAX_CHAR_VALUE];

  static {
    Arrays.fill(CHAR_TO_LETTER_LOOKUP, TurkicLetter.UNDEFINED);
    Arrays.fill(TURKISH_ALPHABET_INDEXES, -1);
    Arrays.fill(VALID_CHAR_TABLE, false);
    for (TurkicLetter turkicLetter : TURKISH_LETTERS) {
      CHAR_TO_LETTER_LOOKUP[turkicLetter.charValue()] = turkicLetter;
      VALID_CHAR_TABLE[turkicLetter.charValue()] = true;
      if (turkicLetter.isVowel()) {
        VOWEL_TABLE[turkicLetter.charValue()] = true;
      }
    }
  }

  static {
    Arrays.fill(ASCII_EQUIVALENT_CHARS_LOOKUP, (char) 0);
    for (TurkicLetter turkicLetter : TURKISH_LETTERS) {
      ASCII_EQUIVALENT_CHARS_LOOKUP[turkicLetter.charValue] = turkicLetter.englishEquivalentChar();
    }
  }

  private TurkishAlphabet() {
  }

  public static String toAscii(String input) {
    StringBuilder sb = new StringBuilder(input.length());
    for (char c : input.toCharArray()) {
      switch (c) {
        case 'ç':
          sb.append('c');
          break;
        case 'ğ':
          sb.append('g');
          break;
        case 'ı':
          sb.append('i');
          break;
        case 'ö':
          sb.append('o');
          break;
        case 'ş':
          sb.append('s');
          break;
        case 'ü':
          sb.append('u');
          break;
        case 'Ç':
          sb.append('C');
          break;
        case 'Ğ':
          sb.append('G');
          break;
        case 'İ':
          sb.append('I');
          break;
        case 'Ö':
          sb.append('O');
          break;
        case 'Ş':
          sb.append('S');
          break;
        case 'Ü':
          sb.append('U');
          break;
        case TurkishAlphabet.a_CIRC:
          sb.append('a');
          break;
        case TurkishAlphabet.A_CIRC:
          sb.append('A');
          break;
        case TurkishAlphabet.i_CIRC:
          sb.append('i');
          break;
        case TurkishAlphabet.I_CIRC:
          sb.append('İ');
          break;
        case TurkishAlphabet.u_CIRC:
          sb.append('u');
          break;
        case TurkishAlphabet.U_CIRC:
          sb.append('U');
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }

  public boolean isVowel(char c) {
    return !(c >= MAX_CHAR_VALUE || !VALID_CHAR_TABLE[c]) && VOWEL_TABLE[c];
  }

  public boolean hasVowel(String str) {
    for (int i = 0; i < str.length(); i++) {
      if (isVowel(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public int vowelCount(String str) {
    int count = 0;
    for (int i = 0; i < str.length(); i++) {
      if (isVowel(str.charAt(i))) {
        count++;
      }
    }
    return count;
  }

  public TurkicLetter devoice(TurkicLetter l) {
    return devoicingMap.get(l);
  }

  public TurkicLetter voice(TurkicLetter l) {
    return voicingMap.get(l);
  }

  /**
   * returns the TurkicLetter equivalent of character c.
   *
   * @param c input character
   * @return TurkishLetter equivalent.
   * @throws IllegalArgumentException if input character is out of alphabet.
   */
  public TurkicLetter getLetter(char c) {
    if (c >= MAX_CHAR_VALUE || !VALID_CHAR_TABLE[c]) {
      throw new IllegalArgumentException("Unexpected char:" + c);
    } else {
      return CHAR_TO_LETTER_LOOKUP[c];
    }
  }

  /**
   * returns the TurkicLetter equivalent with given alphabetic index. index starts from 1.
   *
   * @param alphabeticIndex alphabetical index. starts from 1
   * @return TurkicLetter for given alphabetical index.
   * @throws IllegalArgumentException if index is [< 1] or [> alphabetsize]
   */
  public TurkicLetter getLetter(int alphabeticIndex) {
    if (alphabeticIndex < 1 || alphabeticIndex > ALPHABET_LETTER_COUNT) {
      throw new IllegalArgumentException("Unexpected alphabetic index:" + alphabeticIndex);
    }
    return TURKISH_LETTERS[alphabeticIndex - 1];
  }

  /**
   * returns the alphabetic index of a char.
   *
   * @param c char
   * @return alphabetic index.
   * @throws IllegalArgumentException if char is out of alphabet.
   */
  public int getAlphabeticIndex(char c) {
    if (!isValid(c)) {
      throw new IllegalArgumentException("unexpected char:" + c + " code:" + (int) c);
    }
    return TURKISH_ALPHABET_INDEXES[c];
  }

  /**
   * checks if two characters are enlish character equal.
   *
   * @param c1 first char
   * @param c2 second char.
   * @return true if equals or enlish equivalents are same.
   */
  public boolean asciiEqual(char c1, char c2) {
    return (isValid(c1) && isValid(c2)) &&
        (c1 == c2 || ASCII_EQUIVALENT_CHARS_LOOKUP[c1] == ASCII_EQUIVALENT_CHARS_LOOKUP[c2]);
  }

  public char getAsciiEquivalentChar(char c) {
    if (!isValid(c)) {
      throw new IllegalArgumentException("unexpected char:" + c);
    }
    return CHAR_TO_LETTER_LOOKUP[c].englishEquivalentChar();
  }

  public TurkicLetter getAsciEquivalentLetter(char c) {
    if (!isValid(c)) {
      throw new IllegalArgumentException("unexpected char:" + c);
    }
    return ASCII_EQUIVALENT_LETTER_LOOKUP[getAlphabeticIndex(c) - 1];
  }

  public TurkicLetter aHarmony(TurkicLetter vowel) {
    if (vowel.isConsonant()) {
      throw new IllegalArgumentException("letter is not a vowel");
    }
    return vowel.isFrontal() ? L_e : L_a;
  }

  public TurkicLetter iHarmony(TurkicLetter vowel) {
    if (vowel.isConsonant()) {
      throw new IllegalArgumentException("letter is not a vowel");
    }
    if (!vowel.isFrontal()) {
      return vowel.isRounded() ? L_u : L_ii;
    } else {
      return vowel.isRounded() ? L_uu : L_i;
    }
  }

  public boolean compatibleForAHarmony(TurkicLetter source, TurkicLetter target) {
    return aHarmony(source) == target;
  }

  public boolean compatibleForIHarmony(TurkicLetter source, TurkicLetter target) {
    return iHarmony(source) == target;
  }

  /**
   * checks if a character is part of TurkishAlphabet.
   *
   * @param c character to check
   * @return true if it is part of the Turkish alphabet. false otherwise
   */
  public final boolean isValid(char c) {
    return c < MAX_CHAR_VALUE && VALID_CHAR_TABLE[c];
  }

  public String normalize(String input) {
    StringBuilder sb = new StringBuilder(input.length());
    input = TextUtil.normalizeApostrophes(input.toLowerCase(TR));
    for (char c : input.toCharArray()) {
      if (isValid(c)) {
        sb.append(c);
      } else {
        sb.append("?");
      }
    }
    return sb.toString();
  }

  public String normalizeCircumflex(String input) {
    StringBuilder sb = new StringBuilder(input.length());
    input = input.toLowerCase(TR);
    for (char c : input.toCharArray()) {
      switch (c) {
        case 'Â':
          sb.append("A");
          break;
        case 'â':
          sb.append("a");
          break;
        case 'Î':
          sb.append("İ");
          break;
        case 'î':
          sb.append("i");
          break;
        case 'Û':
          sb.append("U");
          break;
        case 'û':
          sb.append("u");
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }

  private enum Singleton {
    Instance;
    TurkishAlphabet alphabet = new TurkishAlphabet();
  }

}