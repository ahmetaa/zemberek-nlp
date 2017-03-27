package zemberek.morphology.structure;

import zemberek.core.collections.UIntMap;
import zemberek.core.io.KeyValueReader;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;

import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains some Turkish language specific helper methods and properties.
 */
public class Turkish {
    public static final Locale LOCALE = new Locale("tr");
    public static final TurkishAlphabet Alphabet = TurkishAlphabet.INSTANCE;
    public static final Collator COLLATOR = Collator.getInstance(LOCALE);

    static UIntMap<String> turkishLetterProns = new UIntMap<>();

    static {
        try {
            Map<String, String> map = new KeyValueReader("=", "##")
                    .loadFromStream(
                            Turkish.class.getResourceAsStream("/tr/phonetics/turkish-letter-pronunciation.txt"), "utf-8");
            for (String s : map.keySet()) {
                if (s.length() != 1) {
                    Log.warn("1 Character keys are expected. But it is : %s", s);
                }
                turkishLetterProns.put(s.charAt(0), map.get(s));
            }

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String inferPronunciation(String w) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (turkishLetterProns.containsKey(c)) {
                sb.append(turkishLetterProns.get(c));
            } else {
                Log.warn("Cannot identify character " + String.valueOf(c) + " in pronunciation of :[" + w + "]");
            }
        }
        return sb.toString();
    }

    public static String capitalize(String word) {
        if (word.length() == 0)
            return word;
        return word.substring(0, 1).toUpperCase(LOCALE) + word.substring(1).toLowerCase(LOCALE);
    }

    private static class TurkishStringComparator implements Comparator<String> {
        public int compare(String o1, String o2) {
            return COLLATOR.compare(o1, o2);
        }
    }

    public static final Comparator<String> STRING_COMPARATOR_ASC = new TurkishStringComparator();

    static Pattern WORD_BEGIN_END_SEPARATOR =
            Pattern.compile("^([.,'\"()\\[\\]{}:;*$]+|)(.+?)([:;)(\\[\\]{}'?!,.\"\\-*$]+|)$");

    /**
     * TODO: should not separate dot symbols from numbers.
     * <p>separates begin-end symbols from words. there are some exceptions tough, it does not separate +- from beginning
     * <p>examples:
     * <p>'123 -> ' 123
     * <p>+123 ->  +123
     * <p>"123" -> " 123 "
     * <p>,23,2 -> , 23,2
     * <p>merhaba? -> merhaba ?
     * <p>merhaba. -> merhaba.
     *
     * @param input input string.
     * @return output.
     */
    public static String separateBeginEndSymbolsFromWord(String input) {
        Matcher matcher = WORD_BEGIN_END_SEPARATOR.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 3);
        sb.append(matcher.group(1))
                .append(" ")
                .append(matcher.group(2))
                .append(" ")
                .append(matcher.group(3));
        return sb.toString().trim();
    }
}
