package zemberek.tokenization;

import com.google.common.collect.Sets;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.io.IOUtil;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.TurkishAlphabet;

abstract class PerceptronSegmenter {

  private static final Set<String> webWords =
      Sets.newHashSet("http:", ".html", "www", ".tr", ".edu", ".com", ".net", ".gov", ".org", "@");
  static Set<String> TurkishAbbreviationSet = new HashSet<>();
  private static Locale localeTr = new Locale("tr");
  private static String lowerCaseVowels = "aeıioöuüâîû";
  private static String upperCaseVowels = "AEIİOÖUÜÂÎÛ";

  static {
    try {
      List<String> lines = TextIO.loadLinesFromResource("tokenization/abbreviations.txt");
      for (String line : lines) {
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }
        String abbr = line.replaceAll("\\s+", "").replaceAll("\\.$", "");
        if (line.endsWith(".") || Character.isUpperCase(abbr.charAt(0))) {
          TurkishAbbreviationSet.add(abbr);
        }
        if (!TurkishAlphabet.INSTANCE.containsVowel(abbr)) {
          TurkishAbbreviationSet.add(abbr.toLowerCase(localeTr));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  FloatValueMap<String> weights = new FloatValueMap<>();

  protected static FloatValueMap<String> load(DataInputStream dis) throws IOException {
    int size = dis.readInt();
    FloatValueMap<String> features = new FloatValueMap<>((int) (size * 1.5));
    for (int i = 0; i < size; i++) {
      features.set(dis.readUTF(), dis.readFloat());
    }
    return features;
  }

  static boolean potentialWebSite(String s) {
    for (String urlWord : webWords) {
      if (s.contains(urlWord)) {
        return true;
      }
    }
    return false;
  }

  private static char getMetaChar(char letter) {
    char c;
    if (Character.isUpperCase(letter)) {
      c = upperCaseVowels.indexOf(letter) >= 0 ? 'V' : 'C';
    } else if (Character.isLowerCase(letter)) {
      c = lowerCaseVowels.indexOf(letter) >= 0 ? 'v' : 'c';
    } else if (Character.isDigit(letter)) {
      c = 'd';
    } else if (Character.isWhitespace(letter)) {
      c = ' ';
    } else if (letter == '.' || letter == '!' || letter == '?' || letter == '…') {
      return letter;
    } else {
      c = '-';
    }
    return c;
  }

  static String getMetaChars(String str) {
    StringBuilder sb = new StringBuilder(str.length());
    for (int i = 0; i < str.length(); i++) {
      sb.append(getMetaChar(str.charAt(i)));
    }
    return sb.toString();
  }

  public void saveBinary(Path path) throws IOException {
    try (DataOutputStream dos = IOUtil.getDataOutputStream(path)) {
      dos.writeInt(weights.size());
      for (String feature : weights) {
        dos.writeUTF(feature);
        dos.writeFloat(weights.get(feature));
      }
    }
  }

}
