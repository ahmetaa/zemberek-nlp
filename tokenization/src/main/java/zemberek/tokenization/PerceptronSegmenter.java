package zemberek.tokenization;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.io.IOUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

abstract class PerceptronSegmenter {

    FloatValueMap<String> weights = new FloatValueMap<>();

    static Set<String> TurkishAbbreviationSet = new HashSet<>();
    private static Locale localeTr = new Locale("tr");

    static {
        try {
            for (String line : Resources.readLines(Resources.getResource("tokenization/abbreviations.txt"), Charsets.UTF_8)) {
                if (line.trim().length() > 0) {
                    final String abbr = line.trim().replaceAll("\\s+",""); // erase spaces
                    TurkishAbbreviationSet.add(abbr.replaceAll("\\.$", "")); // erase last dot amd add.
                    TurkishAbbreviationSet.add(abbr.toLowerCase(localeTr).replaceAll("\\.$", "")); // lowercase and add.
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static FloatValueMap<String> load(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        FloatValueMap<String> features = new FloatValueMap<>((int) (size * 1.5));
        for (int i = 0; i < size; i++) {
            features.set(dis.readUTF(), dis.readFloat());
        }
        return features;
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

    private static final Set<String> webWords =
            Sets.newHashSet("http:", ".html", "www", ".tr", ".edu", ".com", ".net", ".gov", ".org", "@");

    static boolean potentialWebSite(String s) {
        for (String urlWord : webWords) {
            if (s.contains(urlWord))
                return true;
        }
        return false;
    }

    private static String lowerCaseVowels = "aeıioöuüâîû";
    private static String upperCaseVowels = "AEIİOÖUÜÂÎÛ";

    private static char getMetaChar(char letter) {
        char c;
        if (Character.isUpperCase(letter)) {
            c = upperCaseVowels.indexOf(letter) > 0 ? 'V' : 'C';
        } else if (Character.isLowerCase(letter)) {
            c = lowerCaseVowels.indexOf(letter) > 0 ? 'v' : 'c';
        } else if (Character.isDigit(letter))
            c = 'd';
        else if (Character.isWhitespace(letter))
            c = ' ';
        else if (letter == '.' || letter == '!' || letter == '?')
            return letter;
        else c = '-';
        return c;
    }

    static boolean containsVowel(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (lowerCaseVowels.indexOf(c) > 0 || upperCaseVowels.indexOf(c) > 0) {
                return true;
            }
        }
        return false;
    }

    static String getMetaChars(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            sb.append(getMetaChar(str.charAt(i)));
        }
        return sb.toString();
    }

}
