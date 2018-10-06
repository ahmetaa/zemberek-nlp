package zemberek.core.text;

import zemberek.core.turkish.TurkishAlphabet;

public class StringMatchers {

  private static TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

  public static final StringMatcher ASCII_MATCHER = new AsciiMatcher();
  public static final StringMatcher EXACT_MATCHER = (s1, s2) -> {
    if (s1 == null || s2 == null) {
      return false;
    }
    return s1.equals(s2);
  };

  static class AsciiMatcher implements StringMatcher {

    @Override
    public boolean matches(String s1, String s2) {
      if (s1 == null || s2 == null) {
        return false;
      }
      if (s1.length() != s2.length()) {
        return false;
      }
      for (int i = 0; i < s1.length(); i++) {
        char c1 = s1.charAt(i);
        char c2 = s2.charAt(i);
        if (!alphabet.isAsciiEqual(c1, c2)) {
          return false;
        }
      }
      return true;
    }
  }

}
