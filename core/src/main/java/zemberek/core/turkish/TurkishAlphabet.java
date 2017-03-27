package zemberek.core.turkish;

import com.google.common.collect.ImmutableMap;
import zemberek.core.text.TextUtil;

import java.util.Arrays;
import java.util.Locale;

import static zemberek.core.turkish.TurkicLetter.builder;

/**
 * Contains Turkish Letters, Turkish Letter equivalent chars, several helper methods.
 * TurkishAlphabet only contains small case letters.
 */
public class TurkishAlphabet {

    static final Locale TR = new Locale("tr");

    private enum Singleton {
        Instance;
        TurkishAlphabet alphabet = new TurkishAlphabet();
    }

    private TurkishAlphabet() {
    }

    public static TurkishAlphabet INSTANCE = Singleton.Instance.alphabet;

    // Turkish specific characters.
    public static final char C_CC = '\u00c7'; // Ç
    public static final char C_cc = '\u00e7'; // ç
    public static final char C_GG = '\u011e'; // Ğ
    public static final char C_gg = '\u011f'; // ğ
    public static final char C_ii = '\u0131'; // ı
    public static final char C_II = '\u0130'; // İ
    public static final char C_OO = '\u00d6'; // Ö
    public static final char C_oo = '\u00f6'; // ö
    public static final char C_SS = '\u015e'; // Ş
    public static final char C_ss = '\u015f'; // ş
    public static final char C_UU = '\u00dc'; // Ü
    public static final char C_uu = '\u00fc'; // ü

    // letters used in turkish text having circumflex.
    public static final char A_CIRC = '\u00c2'; // Â
    public static final char a_CIRC = '\u00e2'; // â
    public static final char I_CIRC = '\u00ce'; // Î
    public static final char i_CIRC = '\u00ee'; // î
    public static final char U_CIRC = '\u00db'; // Û
    public static final char u_CIRC = '\u00fb'; // û

    /**
     * Turkish Letters. q,x,w is also added for foreign proper nouns. They are marked as 'foreign'
     */
    public static final TurkicLetter L_a = builder('a', 1).vowel().build();
    public static final TurkicLetter L_b = builder('b', 2).build();
    public static final TurkicLetter L_c = builder('c', 3).build();
    public static final TurkicLetter L_cc = builder(C_cc, 4).notInAscii().voiceless().similarAscii('c').build();
    public static final TurkicLetter L_d = builder('d', 5).build();
    public static final TurkicLetter L_e = builder('e', 6).vowel().frontalVowel().build();
    public static final TurkicLetter L_f = builder('f', 7).continuant().voiceless().build();
    public static final TurkicLetter L_g = builder('g', 8).build();
    public static final TurkicLetter L_gg = builder(C_gg, 9).continuant().notInAscii().similarAscii('g').build();
    public static final TurkicLetter L_h = builder('h', 10).continuant().voiceless().build();
    public static final TurkicLetter L_ii = builder(C_ii, 11).vowel().notInAscii().similarAscii('i').build();
    public static final TurkicLetter L_i = builder('i', 12).vowel().frontalVowel().build();
    public static final TurkicLetter L_j = builder('j', 13).continuant().build();
    public static final TurkicLetter L_k = builder('k', 14).voiceless().build();
    public static final TurkicLetter L_l = builder('l', 15).continuant().build();
    public static final TurkicLetter L_m = builder('m', 16).continuant().build();
    public static final TurkicLetter L_n = builder('n', 17).continuant().build();
    public static final TurkicLetter L_o = builder('o', 18).vowel().roundedVowel().build();
    public static final TurkicLetter L_oo = builder(C_oo, 19).vowel().frontalVowel().roundedVowel().notInAscii().similarAscii('o').build();
    public static final TurkicLetter L_p = builder('p', 20).voiceless().build();
    public static final TurkicLetter L_r = builder('r', 21).continuant().build();
    public static final TurkicLetter L_s = builder('s', 22).continuant().voiceless().build();
    public static final TurkicLetter L_ss = builder(C_ss, 23).continuant().notInAscii().voiceless().similarAscii('s').build();
    public static final TurkicLetter L_t = builder('t', 24).voiceless().build();
    public static final TurkicLetter L_u = builder('u', 25).vowel().roundedVowel().build();
    public static final TurkicLetter L_uu = builder(C_uu, 26).vowel().roundedVowel().frontalVowel().similarAscii('u').notInAscii().build();
    public static final TurkicLetter L_v = builder('v', 27).continuant().build();
    public static final TurkicLetter L_y = builder('y', 28).continuant().build();
    public static final TurkicLetter L_z = builder('z', 29).continuant().build();
    // Not Turkish but sometimes appears in geographical names etc.
    public static final TurkicLetter L_q = builder('q', 30).foreign().build();
    public static final TurkicLetter L_w = builder('w', 31).foreign().build();
    public static final TurkicLetter L_x = builder('x', 32).foreign().build();
    // Circumflexed letters
    public static final TurkicLetter L_ac = builder(a_CIRC, 33).vowel().similarAscii('a').notInAscii().build();
    public static final TurkicLetter L_ic = builder(i_CIRC, 34).vowel().frontalVowel().similarAscii('i').notInAscii().build();
    public static final TurkicLetter L_uc = builder(u_CIRC, 35).vowel().frontalVowel().similarAscii('u').roundedVowel().notInAscii().build();

    // Punctuations
    public static final TurkicLetter P_Dot = builder('.', 33).build();
    public static final TurkicLetter P_Comma = builder(',', 34).build();
    public static final TurkicLetter P_Hyphen = builder('-', 35).build();
    public static final TurkicLetter P_Colon = builder(':', 36).build();
    public static final TurkicLetter P_Semicolon = builder(';', 37).build();
    public static final TurkicLetter P_Plus = builder('+', 38).build();
    public static final TurkicLetter P_Popen = builder('(', 39).build();
    public static final TurkicLetter P_Pclose = builder(')', 40).build();
    public static final TurkicLetter P_Bopen = builder('[', 41).build();
    public static final TurkicLetter P_Bclose = builder(']', 42).build();
    public static final TurkicLetter P_CBopen = builder('{', 43).build();
    public static final TurkicLetter P_CBclose = builder('}', 44).build();
    public static final TurkicLetter P_QuestionMark = builder('?', 45).build();
    public static final TurkicLetter P_ExcMark = builder('!', 46).build();
    public static final TurkicLetter P_SQuote = builder('\'', 47).build();
    public static final TurkicLetter P_DQuote = builder('\"', 48).build();
    public static final TurkicLetter P_Slash = builder('/', 49).build();
    public static final TurkicLetter P_Percent = builder('%', 50).build();
    public static final TurkicLetter P_Number = builder('#', 51).build();
    public static final TurkicLetter P_Dollar = builder('$', 52).build();
    public static final TurkicLetter P_Yen = builder('¥', 53).build();
    public static final TurkicLetter P_Pound = builder('£', 54).build();
    public static final TurkicLetter P_Euro = builder('€', 55).build();

    // numbers
    public static final TurkicLetter N_0 = builder('0', 100).build();
    public static final TurkicLetter N_1 = builder('1', 101).build();
    public static final TurkicLetter N_2 = builder('2', 102).build();
    public static final TurkicLetter N_3 = builder('3', 103).build();
    public static final TurkicLetter N_4 = builder('4', 104).build();
    public static final TurkicLetter N_5 = builder('5', 105).build();
    public static final TurkicLetter N_6 = builder('6', 106).build();
    public static final TurkicLetter N_7 = builder('7', 107).build();
    public static final TurkicLetter N_8 = builder('8', 108).build();
    public static final TurkicLetter N_9 = builder('9', 109).build();


    static final TurkicLetter[] TURKISH_LETTERS = {
            L_a, L_b, L_c, L_cc, L_d, L_e, L_f, L_g,
            L_gg, L_h, L_ii, L_i, L_j, L_k, L_l, L_m,
            L_n, L_o, L_oo, L_p, L_r, L_s, L_ss, L_t,
            L_u, L_uu, L_v, L_y, L_z, L_q, L_w, L_x,
            L_ac, L_ic, L_uc,
            P_Dot, P_Comma, P_Hyphen, P_Colon, P_Semicolon,
            P_Plus, P_Popen, P_Pclose, P_Bopen, P_Bclose, P_CBopen, P_CBclose,
            P_QuestionMark, P_ExcMark, P_SQuote, P_DQuote, P_Slash, P_Percent, P_Number,
            P_Dollar, P_Yen, P_Pound, P_Euro,
            N_0, N_1, N_2, N_3, N_4, N_5, N_6, N_7, N_8, N_9
    };

    public static final int ALPHABET_LETTER_COUNT = TURKISH_LETTERS.length;

    // 0x15f is the maximum char value in turkish specific characters. It is the size
    // of our lookup tables. This could be done better, but for now it works.
    private static final int MAX_CHAR_VALUE = 0x20ac + 1;
    private static final TurkicLetter[] CHAR_TO_LETTER_LOOKUP = new TurkicLetter[MAX_CHAR_VALUE];
    private static final char[] TURKISH_ALPHABET_CHARS = new char[MAX_CHAR_VALUE];
    private static final int[] TURKISH_ALPHABET_INDEXES = new int[MAX_CHAR_VALUE];
    private static final boolean[] VOWEL_TABLE = new boolean[MAX_CHAR_VALUE];
    private static final boolean[] VALID_CHAR_TABLE = new boolean[MAX_CHAR_VALUE];

    static {
        Arrays.fill(CHAR_TO_LETTER_LOOKUP, TurkicLetter.UNDEFINED);
        Arrays.fill(TURKISH_ALPHABET_INDEXES, -1);
        Arrays.fill(VALID_CHAR_TABLE, false);
        for (TurkicLetter turkicLetter : TURKISH_LETTERS) {
            CHAR_TO_LETTER_LOOKUP[turkicLetter.charValue()] = turkicLetter;
            TURKISH_ALPHABET_CHARS[turkicLetter.alphabeticIndex() - 1] = turkicLetter.charValue();
            TURKISH_ALPHABET_INDEXES[turkicLetter.charValue()] = turkicLetter.alphabeticIndex();
            VALID_CHAR_TABLE[turkicLetter.charValue()] = true;
            if (turkicLetter.isVowel()) {
                VOWEL_TABLE[turkicLetter.charValue()] = true;
            }
        }
    }

    public boolean isVowel(char c) {
        return !(c >= MAX_CHAR_VALUE || !VALID_CHAR_TABLE[c]) && VOWEL_TABLE[c];
    }

    public boolean hasVowel(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (isVowel(str.charAt(i)))
                return true;
        }
        return false;
    }

    public int vowelCount(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (isVowel(str.charAt(i)))
                count++;
        }
        return count;
    }

    protected static final ImmutableMap<TurkicLetter, TurkicLetter> devoicingMap = new ImmutableMap.Builder<TurkicLetter, TurkicLetter>()
            .put(L_b, L_p)
            .put(L_c, L_cc)
            .put(L_d, L_t)
            .put(L_g, L_k)
            .put(L_gg, L_k)
            .build();

    public TurkicLetter devoice(TurkicLetter l) {
        return devoicingMap.get(l);
    }

    protected static final ImmutableMap<TurkicLetter, TurkicLetter> voicingMap = new ImmutableMap.Builder<TurkicLetter, TurkicLetter>().
            put(L_p, L_b).
            put(L_k, L_gg).
            put(L_cc, L_c).
            put(L_t, L_d).
            put(L_g, L_gg).
            build();


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
        if (c >= MAX_CHAR_VALUE || !VALID_CHAR_TABLE[c])
            throw new IllegalArgumentException("Unexpected char:" + c);
        else return CHAR_TO_LETTER_LOOKUP[c];
    }

    /**
     * returns the TurkicLetter equivalent with given alphabetic index. index starts from 1.
     *
     * @param alphabeticIndex alphabetical index. starts from 1
     * @return TurkicLetter for given alphabetical index.
     * @throws IllegalArgumentException if index is [< 1] or [> alphabetsize]
     */
    public TurkicLetter getLetter(int alphabeticIndex) {
        if (alphabeticIndex < 1 || alphabeticIndex > ALPHABET_LETTER_COUNT)
            throw new IllegalArgumentException("Unexpected alphabetic index:" + alphabeticIndex);
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
        if (!isValid(c))
            throw new IllegalArgumentException("unexpected char:" + c + " code:" + (int) c);
        return TURKISH_ALPHABET_INDEXES[c];
    }

    /**
     * retrieves a character from alphabetical index.
     *
     * @param alphabeticIndex index
     * @return char.
     * @throws IllegalArgumentException if alphabeticIndex is [< 1] or [> alphabetsize]
     */
    public char getCharByAlphabeticIndex(int alphabeticIndex) {
        if (alphabeticIndex < 1 || alphabeticIndex > ALPHABET_LETTER_COUNT)
            throw new IllegalArgumentException("Unexpected alphabetic index:" + alphabeticIndex);
        return TURKISH_ALPHABET_CHARS[alphabeticIndex - 1];
    }

    // ------------------------ ASCII equivalency ----------------------------------
    // This lookup table maps each Turkish letter to its ASCII counterpart.
    private static final TurkicLetter[] ASCII_EQUIVALENT_LETTER_LOOKUP = {
            L_a, L_b, L_c, L_c, L_d, L_e, L_f, L_g,
            L_g, L_h, L_i, L_i, L_j, L_k, L_l, L_m,
            L_n, L_o, L_o, L_p, L_r, L_s, L_s, L_t,
            L_u, L_u, L_v, L_y, L_z, L_q, L_w, L_x,
            L_a, L_i, L_u};

    private static char[] ASCII_EQUIVALENT_CHARS_LOOKUP = new char[MAX_CHAR_VALUE];

    static {
        Arrays.fill(ASCII_EQUIVALENT_CHARS_LOOKUP, (char) 0);
        for (TurkicLetter turkicLetter : TURKISH_LETTERS) {
            ASCII_EQUIVALENT_CHARS_LOOKUP[turkicLetter.charValue] = turkicLetter.englishEquivalentChar();
        }
    }

    /**
     * returns the English equivalnet letter. such as [a->a] and [c with cedil -> c]
     *
     * @param letter turkicletter
     * @return english equivalent letter.
     */
    public TurkicLetter getAsciiEquivalentLetter(TurkicLetter letter) {
        return ASCII_EQUIVALENT_LETTER_LOOKUP[letter.alphabeticIndex() - 1];
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
        if (!isValid(c))
            throw new IllegalArgumentException("unexpected char:" + c);
        return CHAR_TO_LETTER_LOOKUP[c].englishEquivalentChar();
    }

    public TurkicLetter getAsciEquivalentLetter(char c) {
        if (!isValid(c))
            throw new IllegalArgumentException("unexpected char:" + c);
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

    public byte[] toByteIndexes(String s) {
        byte[] indexes = new byte[s.length()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = (byte) getAlphabeticIndex(s.charAt(i));
        }
        return indexes;
    }

    public String normalize(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        input = TextUtil.normalizeApostrophes(input.toLowerCase(TR));
        for (char c : input.toCharArray()) {
            if (isValid(c))
                sb.append(c);
            else
                sb.append("?");
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

}