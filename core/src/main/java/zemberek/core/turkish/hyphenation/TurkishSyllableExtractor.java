package zemberek.core.turkish.hyphenation;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.TurkishAlphabet;

/**
 * This syllable service is designed for extracting syllable information from Turkish words.
 * Algorithm cannot parse words like "tren", "spor", "sfinks", "angstrom", "mavimtrak", "stetoskop"
 * etc.
 */
public class TurkishSyllableExtractor implements SyllableExtractor {

  private final TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

  // STRICT extractor cannot parse words ending two consonants. Such as `kart`, `yoğurt`
  public static TurkishSyllableExtractor STRICT = new TurkishSyllableExtractor(true);

  // DEFAULT extractor allows parsing words like "kitapt -> ki-tapt"
  public static TurkishSyllableExtractor DEFAULT = new TurkishSyllableExtractor(false);

  public final boolean strict;

  private static HashSet<String> acceptedSyllablePrefixes;

  static {
    acceptedSyllablePrefixes = new HashSet<>();
    try {
      acceptedSyllablePrefixes.addAll(TextIO.loadLinesFromResource(
          "zemberek/core/syllable/accepted-syllable-prefixes"));
    } catch (IOException e) {
      Log.warn("Cannot find accepted syllable prefixes.");
    }
  }

  private TurkishSyllableExtractor(boolean strict) {
    this.strict = strict;
  }

  public List<String> getSyllables(String str) {
    int[] boundaries = syllableBoundaries(str);
    List<String> result = new ArrayList<>();
    for (int i = 0; i < boundaries.length - 1; i++) {
      int boundary = boundaries[i];
      result.add(str.substring(boundary, boundaries[i + 1]));
    }
    if (boundaries.length > 0) {
      result.add(str.substring(boundaries[boundaries.length - 1]));
    }
    return result;
  }

  public int[] syllableBoundaries(String str) {
    final int size = str.length();
    char[] chr = str.toCharArray();
    int[] boundaryIndexes = new int[size];
    int lastIndex = size;
    int index = 0;
    while (lastIndex > 0) {
      int letterCount = letterCountForLastSyllable(chr, lastIndex);
      if (letterCount == -1) {
        return new int[0];
      }
      boundaryIndexes[index++] = lastIndex - letterCount;
      lastIndex -= letterCount;
    }
    int[] result = new int[index];
    for (int i = 0; i < index; i++) {
      result[i] = boundaryIndexes[index - i - 1];
    }
    return result;
  }


  private boolean isVowel(char c) {
    return alphabet.isVowel(c);
  }

  private int letterCountForLastSyllable(char[] chr, int endIndex) {

    if (endIndex == 0) {
      return -1;
    }

    if (isVowel(chr[endIndex - 1])) {
      if (endIndex == 1) {
        return 1;
      }
      if (isVowel(chr[endIndex - 2])) {
        return 1;
      }
      if (endIndex == 2) {
        return 2;
      }
      if (!isVowel(chr[endIndex - 3]) && endIndex == 3) {
        return 3;
      }
      return 2;
    } else {
      if (endIndex == 1) {
        return -1;
      }
      if (isVowel(chr[endIndex - 2])) {
        if (endIndex == 2 || isVowel(chr[endIndex - 3])) {
          return 2;
        }
        if (endIndex == 3 || isVowel(chr[endIndex - 4])) {
          return 3;
        }
        // If the word is 4 letters and rules above passed, we assume this cannot be parsed.
        // That is why words like tren, strateji, krank, angstrom cannot be parsed.
        if (endIndex == 4) {
          return -1;
        }
        if (!isVowel(chr[endIndex - 5])) {
          return 3;
        }
        return 3;
      } else {
        if (strict && !isVowel(chr[endIndex - 2])) {
          return -1;
        }
        if (endIndex == 2 || !isVowel(chr[endIndex - 3])) {
          return -1;
        }
        if (endIndex > 3 && !isVowel(chr[endIndex - 4])) {
          return 4;
        }
        return 3;
      }
    }
  }
}