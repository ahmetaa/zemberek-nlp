package zemberek.normalization.deasciifier;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class provides functionality to deasciify a given ASCII based Turkish text. <p> <p> Note:
 * Adapted from Emre Sevinc's Turkish deasciifier for Python which was influenced from Deniz Yuret's
 * Emacs Turkish Mode implementation which is was inspired by Gokhan Tur's Turkish Text Deasciifier.
 * </p> <p> <p> See: <a href="http://denizyuret.blogspot.com/2006/11/emacs-turkish-mode.html">Deniz
 * Yuret's Emacs Turkish Mode</a><br /> <a href="http://ileriseviye.org/blog/?p=3274">Turkish
 * Deasciifier on Emre Sevinc's Blog</a><br /> <a href="http://github.com/emres/turkish-deasciifier/">Turkish
 * Deasciifier for Python on Emre Sevinc's Github Repo</a><br /> </p> <p> <p> <h3>Usage</h3> <p>
 * <pre>
 * String deasciified = Deasciifier.deasciify(&quot;Hadi bir masal uyduralim, icinde mutlu, doygun, 
 * telassiz durdugumuz.&quot;);
 * System.out.println(deasciified);
 * </pre>
 * <p> </p>
 *
 * @author Ahmet Alp Balkan <ahmet at ahmetalpbalkan.com>
 */
public final class Deasciifier {

  private static HashMap<Character, HashMap<String, Integer>> turkishPatternTable = getPatternTableFromResource();

  private static HashMap<Character, Character> turkishAsciifyTable = new HashMap<>();
  private static HashMap<Character, Character> turkishDowncaseAsciifyTable = new HashMap<>();
  private static HashMap<Character, Character> turkishUpcaseAccentsTable = new HashMap<>();
  private static HashMap<Character, Character> turkishToggleAccentTable = new HashMap<>();

  static {
    turkishAsciifyTable.put('ç', 'c');
    turkishAsciifyTable.put('Ç', 'C');
    turkishAsciifyTable.put('ğ', 'g');
    turkishAsciifyTable.put('Ğ', 'G');
    turkishAsciifyTable.put('ö', 'o');
    turkishAsciifyTable.put('Ö', 'O');
    turkishAsciifyTable.put('ı', 'i');
    turkishAsciifyTable.put('İ', 'I');
    turkishAsciifyTable.put('ş', 's');
    turkishAsciifyTable.put('Ş', 'S');
  }

  static {
    for (Character c = 'A'; c <= 'Z'; c++) {
      Character lowerCaseCharacter = Character.toLowerCase(c);
      turkishDowncaseAsciifyTable.put(c, lowerCaseCharacter);
      turkishDowncaseAsciifyTable.put(lowerCaseCharacter, lowerCaseCharacter);
    }

    turkishDowncaseAsciifyTable.put('ç', 'c');
    turkishDowncaseAsciifyTable.put('Ç', 'c');
    turkishDowncaseAsciifyTable.put('ğ', 'g');
    turkishDowncaseAsciifyTable.put('Ğ', 'g');
    turkishDowncaseAsciifyTable.put('ö', 'o');
    turkishDowncaseAsciifyTable.put('Ö', 'o');
    turkishDowncaseAsciifyTable.put('ı', 'i');
    turkishDowncaseAsciifyTable.put('İ', 'i');
    turkishDowncaseAsciifyTable.put('ş', 's');
    turkishDowncaseAsciifyTable.put('Ş', 's');
    turkishDowncaseAsciifyTable.put('ü', 'u');
    turkishDowncaseAsciifyTable.put('Ü', 'u');
  }

  static {
    for (Character c = 'A'; c <= 'Z'; c++) {
      Character lowerCaseCharacter = Character.toLowerCase(c);
      turkishUpcaseAccentsTable.put(c, lowerCaseCharacter);
      turkishUpcaseAccentsTable.put(lowerCaseCharacter, lowerCaseCharacter);
    }

    turkishUpcaseAccentsTable.put('ç', 'C');
    turkishUpcaseAccentsTable.put('Ç', 'C');
    turkishUpcaseAccentsTable.put('ğ', 'G');
    turkishUpcaseAccentsTable.put('Ğ', 'G');
    turkishUpcaseAccentsTable.put('ö', 'O');
    turkishUpcaseAccentsTable.put('Ö', 'O');
    turkishUpcaseAccentsTable.put('ı', 'I');
    turkishUpcaseAccentsTable.put('İ', 'i');
    turkishUpcaseAccentsTable.put('ş', 'S');
    turkishUpcaseAccentsTable.put('Ş', 'S');
    turkishUpcaseAccentsTable.put('ü', 'U');
    turkishUpcaseAccentsTable.put('Ü', 'U');
  }

  static {
    turkishToggleAccentTable.put('c', 'ç'); // initial direction
    turkishToggleAccentTable.put('C', 'Ç');
    turkishToggleAccentTable.put('g', 'ğ');
    turkishToggleAccentTable.put('G', 'Ğ');
    turkishToggleAccentTable.put('o', 'ö');
    turkishToggleAccentTable.put('O', 'Ö');
    turkishToggleAccentTable.put('u', 'ü');
    turkishToggleAccentTable.put('U', 'Ü');
    turkishToggleAccentTable.put('i', 'ı');
    turkishToggleAccentTable.put('I', 'İ');
    turkishToggleAccentTable.put('s', 'ş');
    turkishToggleAccentTable.put('S', 'Ş');
    turkishToggleAccentTable.put('ç', 'c'); // other direction
    turkishToggleAccentTable.put('Ç', 'C');
    turkishToggleAccentTable.put('ğ', 'g');
    turkishToggleAccentTable.put('Ğ', 'G');
    turkishToggleAccentTable.put('ö', 'o');
    turkishToggleAccentTable.put('Ö', 'O');
    turkishToggleAccentTable.put('ü', 'u');
    turkishToggleAccentTable.put('Ü', 'U');
    turkishToggleAccentTable.put('ı', 'i');
    turkishToggleAccentTable.put('İ', 'I');
    turkishToggleAccentTable.put('ş', 's');
    turkishToggleAccentTable.put('Ş', 'S');
  }

  private Deasciifier() {
    // prevent instances
  }

  private static char turkishToggleAccent(final char c) {
    return turkishToggleAccentTable.containsKey(c) ? turkishToggleAccentTable.get(c) : c;
  }

  private static boolean turkishMatchPattern(final char[] buffer, final HashMap<String, Integer> dlist, final int point,
      final int turkishContextSize) {
    Integer rank = dlist.size() * 2;
    final char[] context = turkishGetContext(buffer, turkishContextSize, point);
    int start = 0;
    final int contextLength = context.length;
    while (start <= turkishContextSize) {
      int end = turkishContextSize + 1;
      while (end <= contextLength) {
        final String s = new String(context, start, end - start);
        final Integer r = dlist.get(s);
        if (r != null && Math.abs(r) < Math.abs(rank)) {
          rank = r;
        }
        end++;
      }
      start++;
    }
    return rank > 0;
  }

  private static char[] turkishGetContext(final char[] buffer, final int size, final int point) {
    char[] s = new char[1 + (2 * size)];
    Arrays.fill(s, ' ');
    s[size] = 'X';

    int i = size + 1;
    boolean space = false;
    int index = point + 1;

    while (i < s.length && !space && index < buffer.length) {
      char currentChar = buffer[index];
      if (turkishDowncaseAsciifyTable.containsKey(currentChar)) {
        final char x = turkishDowncaseAsciifyTable.get(currentChar);
        s[i] = x;
      } else {
        space = true;
      }
      i++;
      index++;
    }

    s = Arrays.copyOf(s, i);

    index = point - 1;
    i = size - 1;
    space = false;

    while (i >= 0 && index >= 0) {
      final char currentChar = buffer[index];
      if (turkishUpcaseAccentsTable.containsKey(currentChar)) {
        final char x = turkishUpcaseAccentsTable.get(currentChar);
        s[i] = x;
        i--;
        space = false;
      } else {
        if (!space) {
          i--;
          space = true;
        }
      }
      index--;
    }
    return s;
  }

  private static boolean turkishNeedCorrection(final char[] buffer, final char c, final int point,
      final int turkishContextSize) {

    final char tr = turkishAsciifyTable.containsKey(c) ? turkishAsciifyTable.get(c) : c;

    final HashMap<String, Integer> pl = turkishPatternTable.get(Character.toLowerCase(tr));

    boolean m = false;
    if (pl != null) {
      m = turkishMatchPattern(buffer, pl, point, turkishContextSize);
    }

    if (tr == 'I') {
      return c == tr ? !m : m;
    }
    return c == tr ? m : !m;
  }

  /**
   * Convert a string with ASCII-only letters into one with Turkish letters.
   *
   * @return Deasciified text.
   */
  public static String deasciify(final String asciiString, final int turkishContextSize) {
    final char[] buffer = asciiString.toCharArray();
    deasciify(buffer, buffer.length, turkishContextSize);
    return new String(buffer);
  }

  /**
   * Convert a string with ASCII-only letters into one with Turkish letters.
   *
   * @return Deasciified text.
   */
  public static String deasciify(final String asciiString) {
    return deasciify(asciiString, 10);
  }

  /**
   * Convert a char buffer with ASCII-only letters into one with Turkish letters
   * (in-place).
   *
   * @return true if any modification has been made.
   */
  public static boolean deasciify(final char[] buffer, final int length, final int turkishContextSize) {
    boolean altered = false;
    for (int i = 0; i < length; i++) {
      final char c = buffer[i];
      if (turkishNeedCorrection(buffer, c, i, turkishContextSize)) {
        buffer[i] = turkishToggleAccent(c);
        altered = true;
      } else {
        buffer[i] = c;
      }
    }
    return altered;
  }

  /**
   * Convert a char buffer with ASCII-only letters into one with Turkish letters
   * (in-place).
   *
   * @return true if any modification has been made.
   */
  public static boolean deasciify(final char[] buffer, final int length) {
    return deasciify(buffer, length, 10);
  }

  private static HashMap<Character, HashMap<String, Integer>> getPatternTableFromResource() {
    final InputStream is = Deasciifier.class.getResourceAsStream("/patterns/turkishPatternTable");
    return getPatternTable(is);
  }

  private static HashMap<Character, HashMap<String, Integer>> getPatternTable(final InputStream is) {
    try (final ObjectInputStream ois = new ObjectInputStream(is)) {
      return (HashMap<Character, HashMap<String, Integer>>) ois.readObject();
    } catch (final Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static synchronized void loadPatternTable(final String filename) throws IOException {
    FileInputStream f = null;
    try {
      f = new FileInputStream(filename);
      turkishPatternTable = getPatternTable(f);
    }
    finally {
      if (f != null) {
        f.close();
      }
    }
  }

  public static void savePatternTable(final String filename) throws IOException {
    FileOutputStream f = null;
    try {
      f = new FileOutputStream(filename);
      final ObjectOutputStream out = new ObjectOutputStream(f);
      out.writeObject(turkishPatternTable);
      out.close();
    }
    finally {
      if (f != null) {
        f.close();
      }
    }
  }
}
