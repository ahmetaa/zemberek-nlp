package zemberek.morphology.structure;

import zemberek.core.io.KeyValueReader;
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
    public static final TurkishAlphabet Alphabet = new TurkishAlphabet();
    public static final Collator COLLATOR = Collator.getInstance(LOCALE);

    static Map<String, String> turkishLetterProns;

    static {
        try {
            turkishLetterProns = new KeyValueReader("=", "##").loadFromStream(
                    Turkish.class.getResourceAsStream("/tr/phonetics/turkish-letter-pronunciation.txt"), "utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String inferPronunciation(String w) {
        StringBuilder sb = new StringBuilder();
        for (char c : w.toCharArray()) {
            String str = String.valueOf(c);
            if (turkishLetterProns.containsKey(str))
                sb.append(turkishLetterProns.get(str));
            else {
                System.out.println("Cannot identify character " + str + " in pronunciation of :[" + w + "]");
            }
        }
        return sb.toString();
    }

    public static String capitalize(String lowerCase) {
        if (lowerCase.length() == 0)
            return lowerCase;
        return lowerCase.substring(0, 1).toUpperCase(LOCALE) + lowerCase.substring(1);
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

    /**
     * This method converts different single and double quote symbols to a unified form.
     * also it reduces two connected single quotes to a one double quote.
     *
     * @param input input string.
     * @return cleaned input string.
     */
    public static String normalizeQuotesHyphens(String input) {
        // rdquo, ldquo, laquo, raquo, Prime sybols in unicode.
        return input
                .replaceAll("[\u201C\u201D\u00BB\u00AB\u2033\u0093\u0094]|''", "\"")
                .replaceAll("[\u0091\u0092\u2032´`’‘]", "'")
                .replaceAll("[\u0096\u0097–]", "-");
    }

}
