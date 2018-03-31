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

public class _TurkishAlphabet {

  public static final Locale TR = new Locale("tr");

  private final String lowercase = "abcçdefgğhıijklmnoöprsştuüvyzxwqâîû";
  private final String uppercase = lowercase.toUpperCase(TR);
  private final FixedBitVector dictionaryLettersLookup =
      TextUtil.generateBitLookup(lowercase + uppercase);

  private final String vowelsLowercase = "aeıioöuüâîû";
  private String vowelsUppercase = vowelsLowercase.toUpperCase(TR);
  private FixedBitVector vowelLookup =
      TextUtil.generateBitLookup(vowelsLowercase + vowelsUppercase);

  private final String stopConsonants = "çkpt";
  private FixedBitVector stopConsonantLookup =
      TextUtil.generateBitLookup(stopConsonants + stopConsonants.toUpperCase(TR));

  private String voicelessConsonants = "çfhkpsşt";
  private FixedBitVector voicelessConsonantsLookup =
      TextUtil.generateBitLookup(voicelessConsonants + voicelessConsonants.toUpperCase(TR));

  public static _TurkishAlphabet INSTANCE = Singleton.Instance.alphabet;

  private enum Singleton {
    Instance;
    _TurkishAlphabet alphabet = new _TurkishAlphabet();
  }

  private IntMap<TurkicLetter> letterMap = new IntMap<>();
  private IntIntMap voicingMap = new IntIntMap();
  private IntIntMap devoicingMap = new IntIntMap();

  private _TurkishAlphabet() {
    List<TurkicLetter> letters = generateLetters();
    for (TurkicLetter letter : letters) {
      letterMap.put(letter.charValue, letter);
    }

    generateVoicingDevoicingLookups();
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
  }

  private void populateCharMap(IntIntMap map, String inStr, String outStr) {
    for (int i = 0; i < inStr.length(); i++) {
      char in = inStr.charAt(i);
      char out = outStr.charAt(i);
      map.put(in, out);
    }
  }

  List<TurkicLetter> generateLetters() {
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

  public char devoice(char c) {
    int res = devoicingMap.get(c);
    return res == IntIntMap.NO_RESULT ? c : (char) res;
  }

  public char voice(char c) {
    int res = voicingMap.get(c);
    return res == IntIntMap.NO_RESULT ? c : (char) res;
  }

  public TurkicLetter getLetter(char c) {
    TurkicLetter letter = letterMap.get(c);
    return letter == null ? TurkicLetter.UNDEFINED : letter;
  }

  public TurkicLetter getLastLetter(CharSequence s) {
    if (s.length() == 0) {
      return TurkicLetter.UNDEFINED;
    }
    return letterMap.get(s.charAt(s.length() - 1));
  }

  public char getLastChar(CharSequence s) {
    return s.charAt(s.length() - 1);
  }

  public TurkicLetter getFirstLetter(CharSequence s) {
    if (s.length() == 0) {
      return TurkicLetter.UNDEFINED;
    }
    return letterMap.get(s.charAt(0));
  }

  public boolean isVowel(char c) {
    return lookup(vowelLookup, c);
  }

  public boolean isDictionaryLetter(char c) {
    return lookup(dictionaryLettersLookup, c);
  }

  public boolean isStopConsonant(char c) {
    return lookup(stopConsonantLookup, c);
  }

  public boolean isVoicelessConsonant(char c) {
    return lookup(voicelessConsonantsLookup, c);
  }

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

  public int vowelCount(String s) {
    int result = 0;
    for (int i = 0; i < s.length(); i++) {
      if (isVowel(s.charAt(i))) {
        result++;
      }
    }
    return result;
  }

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

  private boolean lookup(FixedBitVector vector, char c) {
    return c < vector.length && vector.get(c);
  }

}
