package zemberek.morphology.analysis.tr;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import zemberek.core.io.KeyValueReader;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish.hyphenation.TurkishSyllableExtractor;

public class PronunciationGuesser {

  public static final TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
  private static final int MAX_DECIMAL_DIGITS = 18;
  private static final Map<String, String> turkishLetterProns =
      loadMap("/tr/phonetics/turkish-letter-names.txt");

  private static Map<String, String> loadMap(String resource) {
    try {
      return new KeyValueReader("=", "##").loadFromStream(
          PronunciationGuesser.class.getResourceAsStream(resource),
          "utf-8");
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load " + resource, e);
    }
  }

  public String toTurkishLetterPronunciations(String w) {
    if (alphabet.containsDigit(w)) {
      return toTurkishLetterPronunciationWithDigit(w);
    }
    StringBuilder sb = new StringBuilder(w.length() * 2);
    int lastIdx = w.length() - 1;
    for (int i = 0; i <= lastIdx; i++) {
      char c = w.charAt(i);
      if (c == '-') {
        continue;
      }
      if (i == lastIdx && c == 'k') {
        // most abbreviations ending with k use `ka` sound.
        sb.append("ka");
        continue;
      }
      String pron = turkishLetterProns.get(String.valueOf(c));
      if (pron != null) {
        sb.append(pron);
      } else if (Log.isDebug()) {
        Log.debug("Cannot guess pronunciation of letter [" + c + "] in :[" + w + "]");
      }
    }
    return sb.toString();
  }

  private String toTurkishLetterPronunciationWithDigit(String in) {
    List<String> pieces = TurkishNumbers.separateNumbers(in);
    StringBuilder sb = new StringBuilder(in.length() * 2);
    int lastIdx = pieces.size() - 1;
    for (int i = 0; i <= lastIdx; i++) {
      String piece = pieces.get(i);
      if (alphabet.containsDigit(piece)) {
        // Guard against >18-digit sequences overflowing Long.parseLong; trailing digits govern
        // Turkish vowel harmony and consonant voicing.
        String numPiece = piece.length() > MAX_DECIMAL_DIGITS
            ? piece.substring(piece.length() - MAX_DECIMAL_DIGITS)
            : piece;
        sb.append(TurkishNumbers.convertNumberToString(numPiece));
      } else if (i < lastIdx) {
        sb.append(guessForAbbreviation(piece));
      } else if (!alphabet.containsVowel(piece)) {
        sb.append(toTurkishLetterPronunciations(piece));
      } else {
        sb.append(replaceEnglishSpecificChars(piece));
      }
    }
    return sb.toString().replace(" ", "");
  }

  public String replaceEnglishSpecificChars(String w) {
    StringBuilder sb = new StringBuilder(w.length());
    for (int i = 0, n = w.length(); i < n; i++) {
      char c = w.charAt(i);
      switch (c) {
        case 'w' -> sb.append('v');
        case 'q' -> sb.append('k');
        case 'x' -> sb.append("ks");
        case '-', '\'' -> {
        }
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }

  private static final TurkishSyllableExtractor extractorForAbbrv = TurkishSyllableExtractor.STRICT;

  /**
   * Tries to guess turkish abbreviation pronunciation.
   */
  public String guessForAbbreviation(String input) {
    List<String> syllables = extractorForAbbrv.getSyllables(input);

    boolean firstTwoCons = input.length() > 2
        && !alphabet.isVowel(input.charAt(0))
        && !alphabet.isVowel(input.charAt(1));

    if (syllables.isEmpty() || input.length() < 3 || firstTwoCons) {
      return toTurkishLetterPronunciations(input);
    } else {
      return replaceEnglishSpecificChars(input);
    }
  }

}
