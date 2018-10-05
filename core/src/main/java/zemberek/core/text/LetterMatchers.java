package zemberek.core.text;

import zemberek.core.collections.IntMap;

public class LetterMatchers {

  static class AsciiMatcher implements LetterMatcher {

    static IntMap<char[]> map = new IntMap<>();

    static {
      String turkishSpecific = "çÇğĞıİöÖşŞüÜâîûÂÎÛ";
      String turkishAscii = "cCgGiIoOsSuUaiuAIU";
      for (int i = 0; i < turkishSpecific.length(); i++) {
        char[] c = new char[2];
        c[0] = turkishSpecific.charAt(i);
        c[1] = turkishAscii.charAt(i);
        map.put(c[0], c);
        map.put(c[1], c);
      }
    }

    char[] empty = new char[0];

    @Override
    public char[] matches(char c1) {
      char[] res = map.get(c1);
      return res == null ? empty : res;
    }

  }

}
