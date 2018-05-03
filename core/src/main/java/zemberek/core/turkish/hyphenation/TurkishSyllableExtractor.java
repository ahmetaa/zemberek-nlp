package zemberek.core.turkish.hyphenation;


import java.util.ArrayList;
import java.util.List;
import zemberek.core.turkish.TurkishAlphabet;

/**
 * This syllable service is designed for extracting syllable information from Turkish words. This
 * class uses a strict syllable extraction algorithm, meaning that it cannot parse words like
 * "tren", "spor", "sfinks", "angstrom", "mavimtrak", "stetoskop" etc.
 */
public class TurkishSyllableExtractor implements SyllableExtractor {

  private final TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
  boolean strict = false;

  public void setStrict(boolean strict) {
    this.strict = strict;
  }

  public List<String> getSyllables(String str) {
    int[] boudaries = syllableBoundaries(str);
    List<String> result = new ArrayList<>();
    for (int i = 0; i < boudaries.length - 1; i++) {
      int boudary = boudaries[i];
      result.add(str.substring(boudary, boudaries[i + 1]));
    }
    if (boudaries.length > 0) {
      result.add(str.substring(boudaries[boudaries.length - 1], str.length()));
    }
    return result;
  }

  public int[] syllableBoundaries(String str) {
    final int size = str.length();
    char[] chr = str.toCharArray();
    int[] boundarIndexes = new int[size];
    int lastIndex = size;
    int index = 0;
    while (lastIndex > 0) {
      int letterCount = letterCountForLastSyllable(chr, lastIndex);
      if (letterCount == -1) {
        return new int[0];
      }
      boundarIndexes[index++] = lastIndex - letterCount;
      lastIndex -= letterCount;
    }
    int[] result = new int[index];
    for (int i = 0; i < index; i++) {
      result[i] = boundarIndexes[index - i - 1];
    }
    return result;
  }


  boolean isVowel(char c) {
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
        //word dort harfli ise yukaridaki kurallari gecmesi nedeniyle hecelenemez sayiyoruz.
        // tren, strateji, krank, angstrom gibi kelimeler henuz hecelenmiyor.
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