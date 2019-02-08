package zemberek.morphology.analysis.tr;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import zemberek.core.io.KeyValueReader;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish.hyphenation.TurkishSyllableExtractor;

public class PronunciationGuesser {

  public static final TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
  private static Map<String, String> turkishLetterProns;
  private static Map<String, String> englishLetterProns;
  private static Map<String, String> englishPhonesToTurkish;

  static {
    turkishLetterProns = loadMap("/tr/phonetics/turkish-letter-names.txt");
    englishLetterProns = loadMap("/tr/phonetics/english-letter-names.txt");
    englishPhonesToTurkish = loadMap("/tr/phonetics/english-phones-to-turkish.txt");
  }

  private static Map<String, String> loadMap(String resource) {
    try {
      return new KeyValueReader("=", "##").loadFromStream(
          PronunciationGuesser.class.getResourceAsStream(resource),
          "utf-8");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public String toTurkishLetterPronunciations(String w) {
    if (alphabet.containsDigit(w)) {
      return toTurkishLetterPronunciationWithDigit(w);
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < w.length(); i++) {
      char c = w.charAt(i);
      if (c == '-') {
        continue;
      }
      String key = String.valueOf(c);
      if (turkishLetterProns.containsKey(key)) {
        // most abbreviations ends with k uses `ka` sounds.
        if (i == w.length() - 1 && key.equals("k")) {
          sb.append("ka");
        } else {
          sb.append(turkishLetterProns.get(key));
        }
      } else {
        if(Log.isDebug()) {
          Log.debug("Cannot guess pronunciation of letter [" + key + "] in :[" + w + "]");
        }
      }
    }
    return sb.toString();
  }

  private String toTurkishLetterPronunciationWithDigit(String in) {
    List<String> pieces = TurkishNumbers.separateNumbers(in);
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (String piece : pieces) {
      if (alphabet.containsDigit(piece)) {
        sb.append(TurkishNumbers.convertNumberToString(piece));
        i++;
        continue;
      }
      if (i < pieces.size() - 1) {
        sb.append(toTurkishLetterPronunciations(piece));
      } else {
        sb.append(replaceEnglishSpecificChars(piece));
      }
      i++;
    }
    return sb.toString().replaceAll("[ ]+", "");
  }

  public String replaceEnglishSpecificChars(String w) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < w.length(); i++) {
      char c = w.charAt(i);
      switch (c) {
        case 'w':
          sb.append("v");
          break;
        case 'q':
          sb.append("k");
          break;
        case 'x':
          sb.append("ks");
        case '-':
        case '\'':
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }

  private static TurkishSyllableExtractor extractorForAbbrv = TurkishSyllableExtractor.STRICT;

  /**
   * Tries to guess turkish abbreviation pronunciation.
   */
  public String guessForAbbreviation(String input) {
    List<String> syllables = extractorForAbbrv.getSyllables(input);

    boolean firstTwoCons = false;
    if (input.length() > 2) {
      if (!alphabet.containsVowel(input.substring(0, 2))) {
        firstTwoCons = true;
      }
    }

    if (syllables.size() == 0 || input.length() < 3 || firstTwoCons) {
      return toTurkishLetterPronunciations(input);
    } else {
      return replaceEnglishSpecificChars(input);
    }

  }

}
