package zemberek.core.turkish;

import static zemberek.core.turkish.TurkicLetter.builder;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import zemberek.core.collections.FixedBitVector;
import zemberek.core.collections.IntIntMap;
import zemberek.core.collections.IntMap;
import zemberek.core.text.TextUtil;

public class TurkishAlphabet {

  public static final Locale TR = new Locale("tr");

  private final String lowercase = "abcçdefgğhıijklmnoöprsştuüvyzxwqâîû";
  private final String uppercase = lowercase.toUpperCase(TR);
  private final String allLetters = lowercase + uppercase;
  private final FixedBitVector dictionaryLettersLookup =
      TextUtil.generateBitLookup(allLetters);

  private final String vowelsLowercase = "aeıioöuüâîû";
  private String vowelsUppercase = vowelsLowercase.toUpperCase(TR);
  private FixedBitVector vowelLookup =
      TextUtil.generateBitLookup(vowelsLowercase + vowelsUppercase);

  private final String circumflex = "âîû";
  private final String circumflexUpper = "ÂÎÛ";
  private FixedBitVector circumflexLookup =
      TextUtil.generateBitLookup(circumflex + circumflexUpper);

  private final String apostrophe = "\u2032´`’‘'";
  private FixedBitVector apostropheLookup = TextUtil.generateBitLookup(apostrophe);

  private final String stopConsonants = "çkpt";
  private FixedBitVector stopConsonantLookup =
      TextUtil.generateBitLookup(stopConsonants + stopConsonants.toUpperCase(TR));

  private String voicelessConsonants = "çfhkpsşt";
  private FixedBitVector voicelessConsonantsLookup =
      TextUtil.generateBitLookup(voicelessConsonants + voicelessConsonants.toUpperCase(TR));

  private String turkishSpecific = "çÇğĞıİöÖşŞüÜâîûÂÎÛ";
  private String turkishAscii = "cCgGiIoOsSuUaiuAIU";
  private IntIntMap turkishToAsciiMap = new IntIntMap();
  private FixedBitVector turkishSpecificLookup = TextUtil.generateBitLookup(turkishSpecific);

  private String asciiEqTr = "cCgGiIoOsSuUçÇğĞıİöÖşŞüÜ";
  private String asciiEq = "çÇğĞıİöÖşŞüÜcCgGiIoOsSuU";
  private IntIntMap asciiEqualMap = new IntIntMap();
  private FixedBitVector asciiTrLookup = TextUtil.generateBitLookup(asciiEqTr);


  private String foreignDiacritics = "ÀÁÂÃÄÅÈÉÊËÌÍÎÏÑÒÓÔÕÙÚÛàáâãäåèéêëìíîïñòóôõùúû";
  private String diacriticsToTurkish = "AAAAAAEEEEIIIINOOOOUUUaaaaaaeeeeiiiinoooouuu";
  private IntIntMap foreignDiacriticsMap = new IntIntMap();
  private FixedBitVector foreignDiacriticsLookup = TextUtil.generateBitLookup(foreignDiacritics);

  public static TurkishAlphabet INSTANCE = Singleton.Instance.alphabet;

  private enum Singleton {
    Instance;
    TurkishAlphabet alphabet = new TurkishAlphabet();
  }

  private IntMap<TurkicLetter> letterMap = new IntMap<>();
  private IntIntMap voicingMap = new IntIntMap();
  private IntIntMap devoicingMap = new IntIntMap();
  private IntIntMap circumflexMap = new IntIntMap();

  private TurkishAlphabet() {
    List<TurkicLetter> letters = generateLetters();
    for (TurkicLetter letter : letters) {
      letterMap.put(letter.charValue, letter);
    }
    generateVoicingDevoicingLookups();

    populateCharMap(turkishToAsciiMap, turkishSpecific, turkishAscii);
    populateCharMap(foreignDiacriticsMap, foreignDiacritics, diacriticsToTurkish);

    for (int i = 0; i < asciiEqTr.length(); i++) {
      char in = asciiEqTr.charAt(i);
      char out = asciiEq.charAt(i);
      asciiEqualMap.put(in, out);
    }
  }

  public String toAscii(String in) {
    StringBuilder sb = new StringBuilder(in.length());
    for (int i = 0; i < in.length(); i++) {
      char c = in.charAt(i);
      int res = turkishToAsciiMap.get(c);
      char map = res == IntIntMap.NO_RESULT ? c : (char) res;
      sb.append(map);
    }
    return sb.toString();
  }

  public String foreignDiacriticsToTurkish(String in) {
    StringBuilder sb = new StringBuilder(in.length());
    for (int i = 0; i < in.length(); i++) {
      char c = in.charAt(i);
      int res = foreignDiacriticsMap.get(c);
      char map = res == IntIntMap.NO_RESULT ? c : (char) res;
      sb.append(map);
    }
    return sb.toString();
  }

  private void generateVoicingDevoicingLookups() {
    String voicingIn = "çgkpt";
    String voicingOut = "cğğbd";
    String devoicingIn = "bcdgğ";
    String devoicingOut = "pçtkk";

    populateCharMap(voicingMap,
        voicingIn + voicingIn.toUpperCase(TR),
        voicingOut + voicingOut.toUpperCase(TR));
    populateCharMap(devoicingMap,
        devoicingIn + devoicingIn.toUpperCase(TR),
        devoicingOut + devoicingOut.toUpperCase(TR));

    String circumflexNormalized = "aiu";
    populateCharMap(circumflexMap,
        circumflex + circumflex.toUpperCase(TR),
        circumflexNormalized + circumflexNormalized.toUpperCase(TR));
  }

  private boolean lookup(FixedBitVector vector, char c) {
    return c < vector.length && vector.get(c);
  }

  public boolean containsAsciiRelated(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < asciiTrLookup.length && asciiTrLookup.get(c)) {
        return true;
      }
    }
    return false;
  }

  public IntIntMap getTurkishToAsciiMap() {
    return turkishToAsciiMap;
  }

  public char getAsciiEqual(char c) {
    int res = turkishToAsciiMap.get(c);
    return res == IntIntMap.NO_RESULT ? c : (char) res;
  }

  public boolean isAsciiEqual(char c1, char c2) {
    if (c1 == c2) {
      return true;
    }
    int a1 = asciiEqualMap.get(c1);
    if (a1 == IntIntMap.NO_RESULT) {
      return false;
    }
    return a1 == c2;
  }


  private void populateCharMap(IntIntMap map, String inStr, String outStr) {
    for (int i = 0; i < inStr.length(); i++) {
      char in = inStr.charAt(i);
      char out = outStr.charAt(i);
      map.put(in, out);
    }
  }

  private List<TurkicLetter> generateLetters() {
    List<TurkicLetter> letters = Lists.newArrayList(
        builder('a').vowel().build(),
        builder('e').vowel().frontalVowel().build(),
        builder('ı').vowel().build(),
        builder('i').vowel().frontalVowel().build(),
        builder('o').vowel().roundedVowel().build(),
        builder('ö').vowel().frontalVowel().roundedVowel().build(),
        builder('u').vowel().roundedVowel().build(),
        builder('ü').vowel().roundedVowel().frontalVowel().build(),
        // Circumflexed letters
        builder('â').vowel().build(),
        builder('î').vowel().frontalVowel().build(),
        builder('û').vowel().frontalVowel().roundedVowel().build(),
        // Consonants
        builder('b').build(),
        builder('c').build(),
        builder('ç').voiceless().build(),
        builder('d').build(),
        builder('f').continuant().voiceless().build(),
        builder('g').build(),
        builder('ğ').continuant().build(),
        builder('h').continuant().voiceless().build(),
        builder('j').continuant().build(),
        builder('k').voiceless().build(),
        builder('l').continuant().build(),
        builder('m').continuant().build(),
        builder('n').continuant().build(),
        builder('p').voiceless().build(),
        builder('r').continuant().build(),
        builder('s').continuant().voiceless().build(),
        builder('ş').continuant().voiceless().build(),
        builder('t').voiceless().build(),
        builder('v').continuant().build(),
        builder('y').continuant().build(),
        builder('z').continuant().build(),
        builder('q').build(),
        builder('w').build(),
        builder('x').build()
    );
    List<TurkicLetter> capitals = new ArrayList<>();
    for (TurkicLetter letter : letters) {
      char upper = String.valueOf(letter.charValue).toUpperCase(TR).charAt(0);
      capitals.add(letter.copyFor(upper));
    }
    letters.addAll(capitals);
    return letters;
  }

  public boolean allCapital(String input) {
    for (int i = 0; i < input.length(); i++) {
      if (!Character.isUpperCase(input.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  // TODO: this should not be here,
  public String normalize(String input) {
    StringBuilder sb = new StringBuilder(input.length());
    input = TextUtil.normalizeApostrophes(input.toLowerCase(TR));
    for (char c : input.toCharArray()) {
      if (letterMap.containsKey(c) || c == '.' || c == '-') {
        sb.append(c);
      } else {
        sb.append("?");
      }
    }
    return sb.toString();
  }

  public char normalizeCircumflex(char c) {
    int res = circumflexMap.get(c);
    return res == IntIntMap.NO_RESULT ? c : (char) res;
  }

  public boolean containsCircumflex(String s) {
    return checkLookup(circumflexLookup, s);
  }

  public boolean isTurkishSpecific(char c) {
    return lookup(turkishSpecificLookup, c);
  }

  public boolean containsApostrophe(String s) {
    return checkLookup(apostropheLookup, s);
  }

  public boolean containsForeignDiacritics(String s) {
    return checkLookup(foreignDiacriticsLookup, s);
  }

  private boolean checkLookup(FixedBitVector lookup, String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (lookup(lookup, c)) {
        return true;
      }
    }
    return false;
  }

  public String getAllLetters() {
    return allLetters;
  }

  public String getLowercaseLetters() {
    return lowercase;
  }

  public String getUppercaseLetters() {
    return uppercase;
  }

  /**
   * Converts Turkish letters with circumflex symbols to letters without circumflexes. â->a î->i
   * û->u
   */
  public String normalizeCircumflex(String s) {
    if (!containsCircumflex(s)) {
      return s;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (lookup(circumflexLookup, c)) {
        sb.append((char) circumflexMap.get(c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public String normalizeApostrophe(String s) {
    if (!containsApostrophe(s)) {
      return s;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (lookup(apostropheLookup, c)) {
        sb.append('\'');
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * If there is a voiced char for `c`, returns it. Otherwise Returns the original input. ç->c g->ğ
   * k->ğ p->b t->d
   */
  public char voice(char c) {
    int res = voicingMap.get(c);
    return res == IntIntMap.NO_RESULT ? c : (char) res;
  }

  /**
   * If there is a devoiced char for `c`, returns it. Otherwise Returns the original input. b->p
   * c->ç d->t g->k ğ->k
   */
  public char devoice(char c) {
    int res = devoicingMap.get(c);
    return res == IntIntMap.NO_RESULT ? c : (char) res;
  }


  /**
   * Returns the TurkicLetter object for a character. If it does not exist, returns
   * TurkicLetter.UNDEFINED
   */
  public TurkicLetter getLetter(char c) {
    TurkicLetter letter = letterMap.get(c);
    return letter == null ? TurkicLetter.UNDEFINED : letter;
  }

  /**
   * Returns the last letter of the input as "TurkicLetter". If input is empty or the last character
   * does not belong to alphabet, returns TurkicLetter.UNDEFINED.
   */
  public TurkicLetter getLastLetter(CharSequence s) {
    if (s.length() == 0) {
      return TurkicLetter.UNDEFINED;
    }
    return getLetter(s.charAt(s.length() - 1));
  }

  public char lastChar(CharSequence s) {
    return s.charAt(s.length() - 1);
  }

  /**
   * Returns the first letter of the input as "TurkicLetter". If input is empty or the first
   * character does not belong to alphabet, returns TurkicLetter.UNDEFINED.
   */
  public TurkicLetter getFirstLetter(CharSequence s) {
    if (s.length() == 0) {
      return TurkicLetter.UNDEFINED;
    }
    TurkicLetter letter = letterMap.get(s.charAt(0));
    return getLetter(s.charAt(0));
  }

  /**
   * Returns is `c` is a Turkish vowel. Input can be lower or upper case. Turkish letters with
   * circumflex are included.
   */
  public boolean isVowel(char c) {
    return lookup(vowelLookup, c);
  }

  /**
   * Returns true if `c` is a member of set Turkish alphabet and three english letters w,x and q.
   * Turkish letters with circumflex are included. Input can be lower or upper case.
   */
  public boolean isDictionaryLetter(char c) {
    return lookup(dictionaryLettersLookup, c);
  }

  /**
   * Returns true if `c` is a stop consonant. Stop consonants for Turkish are: `ç,k,p,t`. Input can
   * be lower or upper case.
   */
  public boolean isStopConsonant(char c) {
    return lookup(stopConsonantLookup, c);
  }

  /**
   * Returns true if `c` is a stop consonant. Voiceless consonants for Turkish are:
   * `ç,f,h,k,p,s,ş,t`. Input can be lower or upper case.
   */
  public boolean isVoicelessConsonant(char c) {
    return lookup(voicelessConsonantsLookup, c);
  }

  /**
   * Returns the last vowel of the input as "TurkicLetter". If input is empty or there is no vowel,
   * returns TurkicLetter.UNDEFINED.
   */
  public TurkicLetter getLastVowel(CharSequence s) {
    if (s.length() == 0) {
      return TurkicLetter.UNDEFINED;
    }
    for (int i = s.length() - 1; i >= 0; i--) {
      char c = s.charAt(i);
      if (isVowel(c)) {
        return getLetter(c);
      }
    }
    return TurkicLetter.UNDEFINED;
  }

  /**
   * Returns the first vowel of the input as "TurkicLetter". If input is empty or there is no vowel,
   * returns TurkicLetter.UNDEFINED.
   */
  public TurkicLetter getFirstVowel(CharSequence s) {
    if (s.length() == 0) {
      return TurkicLetter.UNDEFINED;
    }
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (isVowel(c)) {
        return getLetter(c);
      }
    }
    return TurkicLetter.UNDEFINED;
  }

  /**
   * Returns true if target string matches source string with A-Type harmony
   * <pre>
   *   elma, ya -> true
   *   kedi, ye -> true
   *   kalem, a -> false
   * </pre>
   */
  public boolean checkVowelHarmonyA(CharSequence source, CharSequence target) {
    TurkicLetter sourceLastVowel = getLastVowel(source);
    TurkicLetter targetFirstVowel = getLastVowel(target);
    return checkVowelHarmonyA(sourceLastVowel, targetFirstVowel);
  }

  /**
   * Returns true if target string matches source string with I-Type harmony
   * <pre>
   *   elma, yı  -> true
   *   kedi, yi  -> true
   *   kalem, ü  -> false
   *   yogurt, u -> true
   * </pre>
   */
  public boolean checkVowelHarmonyI(CharSequence source, CharSequence target) {
    TurkicLetter sourceLastVowel = getLastVowel(source);
    TurkicLetter targetFirstVowel = getLastVowel(target);
    return checkVowelHarmonyI(sourceLastVowel, targetFirstVowel);
  }

  /**
   * Returns true if target letter matches source letter with A-Type harmony
   * <pre>
   *   i, e -> true
   *   i, a -> false
   *   u, a -> true
   *   c, b -> false
   * </pre>
   */
  public boolean checkVowelHarmonyA(TurkicLetter source, TurkicLetter target) {
    if (source == TurkicLetter.UNDEFINED || target == TurkicLetter.UNDEFINED) {
      return false;
    }
    if (!source.isVowel() || !target.isVowel()) {
      return false;
    }
    return (source.frontal && target.frontal) ||
        (!source.frontal && !target.frontal);
  }

  /**
   * Returns true if target letter matches source letter with I-Type harmony
   * <pre>
   *   e, i -> true
   *   a, i -> false
   *   o, u -> true
   *   c, b -> false
   * </pre>
   */
  public boolean checkVowelHarmonyI(TurkicLetter source, TurkicLetter target) {
    if (source == TurkicLetter.UNDEFINED || target == TurkicLetter.UNDEFINED) {
      return false;
    }
    if (!source.isVowel() || !target.isVowel()) {
      return false;
    }
    return ((source.frontal && target.frontal) ||
        (!source.frontal && !target.frontal)) &&
        ((source.rounded && target.rounded) ||
            (!source.rounded && !target.rounded));
  }

  /**
   * Returns tru if input contains a vowel.
   */
  public boolean containsVowel(CharSequence s) {
    if (s.length() == 0) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (isVowel(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the vowel count in a word. It only checks Turkish vowels.
   */
  public int vowelCount(String s) {
    int result = 0;
    for (int i = 0; i < s.length(); i++) {
      if (isVowel(s.charAt(i))) {
        result++;
      }
    }
    return result;
  }

  /**
   * Returns true if `s` contains a digit. If s is empty or has no digit, returns false.
   */
  public boolean containsDigit(String s) {
    if (s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c >= '0' && c <= '9') {
        return true;
      }
    }
    return false;
  }

  /**
   * Compares two strings ignoring diacritics symbols.
   * <pre>
   *   i, ı -> true
   *   i, î -> true
   *   s, ş -> true
   *   g, ğ -> true
   *   kişi, kışı -> true
   * </pre>
   */
  public boolean equalsIgnoreDiacritics(String s1, String s2) {
    if (s1 == null || s2 == null) {
      return false;
    }
    if (s1.length() != s2.length()) {
      return false;
    }
    for (int i = 0; i < s1.length(); i++) {
      char c1 = s1.charAt(i);
      char c2 = s2.charAt(i);
      if (!isAsciiEqual(c1, c2)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if s1 starts with s2 ignoring diacritics symbols.
   * <pre>
   *   kışı, kis -> true
   *   pîr, pi   -> true
   * </pre>
   */
  public boolean startsWithIgnoreDiacritics(String s1, String s2) {
    if (s1 == null || s2 == null) {
      return false;
    }
    if (s1.length() < s2.length()) {
      return false;
    }
    for (int i = 0; i < s2.length(); i++) {
      char c1 = s1.charAt(i);
      char c2 = s2.charAt(i);
      if (!isAsciiEqual(c1, c2)) {
        return false;
      }
    }
    return true;
  }

}
