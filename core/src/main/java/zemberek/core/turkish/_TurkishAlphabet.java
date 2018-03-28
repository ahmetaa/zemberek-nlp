package zemberek.core.turkish;

import java.util.Locale;
import zemberek.core.collections.FixedBitVector;

public class _TurkishAlphabet {

  public static final Locale TR = new Locale("tr");

  private String lowercase = "abcçdefgğhıijklmnoöprsştuüvyzxwqâîû";
  private String uppercase = lowercase.toUpperCase(TR);
  FixedBitVector dictionaryLettersLookup =
      generateBitLookup(lowercase + uppercase);

  private String vowelsLowercase = "aeıioöuüâîû";
  private String vowelsUppercase = vowelsLowercase.toUpperCase(TR);
  FixedBitVector vowelLookup =
      generateBitLookup(vowelsLowercase + vowelsUppercase);

  private String stopConsonants = "çkpt";
  FixedBitVector stopConsonantLookup =
      generateBitLookup(stopConsonants + stopConsonants.toUpperCase(TR));

  public static _TurkishAlphabet INSTANCE = Singleton.Instance.alphabet;

  private enum Singleton {
    Instance;
    _TurkishAlphabet alphabet = new _TurkishAlphabet();
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


  public boolean containsVowel(String s) {
    if (s.isEmpty()) {
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

  private static FixedBitVector generateBitLookup(String characters) {
    int max = 0;
    for (char c : characters.toCharArray()) {
      if (c > max) {
        max = c;
      }
    }

    FixedBitVector result = new FixedBitVector(max + 1);
    for (char c : characters.toCharArray()) {
      result.set(c);
    }
    return result;
  }

}
