package zemberek.core.text;


import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    /**
     * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
     *
     * @param input input string
     * @return input stream with CDATA illegal characters replaces by a space character.
     */
    public static String cleanCdataIllegalChars(String input, String replacement) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if ((c >= 0x20 && c <= 0xD7ff) || c == 0x9 || c == 0xa || c == 0xd || (c >= 0x10000 && c <= 0x10FFFF))
                sb.append(c);
            else
                sb.append(replacement);
        }
        return sb.toString();
    }

    /**
     * This method converts different single and double quote symbols to a unified form.
     * also it reduces two connected single quotes to a one double quote.
     *
     * @param input input string.
     * @return clened input string.
     */
    public static String normalizeQuotesHyphens(String input) {
        // rdquo, ldquo, laquo, raquo, Prime sybols in unicode.
        return input
                .replaceAll("[\u201C\u201D\u00BB\u00AB\u2033\u0093\u0094]|''", "\"")
                .replaceAll("[\u0091\u0092\u2032´`’‘]", "'")
                .replaceAll("[\u0096\u0097–]", "-");
    }


    public static int countChars(String s, char c) {
        int cnt = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                cnt++;
            }
        }
        return cnt;
    }

    public static int countChars(String s, char... chars) {
        int cnt = 0;
        for (int i = 0; i < s.length(); i++) {
            char cs = s.charAt(i);
            for (char c : chars) {
                if (cs == c) {
                    cnt++;
                    break;
                }
            }
        }
        return cnt;
    }


    static Pattern separationPattern = Pattern.compile("(^[^ .,!?;:0-9]+)([.,!?;:]+)([^ .,!?;:0-9]+$)");
    static Pattern punctPattern = Pattern.compile("[.,!?;:]");

    /**
     * separates from punctuations. only for Strings without spaces.
     * abc.adf -> abc. adf
     * `minWordLength` is the minimum word length to separate. For the above example, it should be >2
     */
    public static String separatePunctuationConnectedWords(String input, int minWordLength) {
        List<String> k = new ArrayList<>();
        for (String s : Splitter.on(" ").omitEmptyStrings().trimResults().split(input)) {
            k.add(separateWords(s, minWordLength));
        }
        return Joiner.on(" ").join(k);
    }

    private static String separateWords(String s, int wordLength) {
        if (!punctPattern.matcher(s).find())
            return s;
        Matcher m = separationPattern.matcher(s);
        if (m.matches()) {
            if (m.group(1).length() >= wordLength && m.group(3).length() >= wordLength) {
                return m.group(1) + m.group(2) + " " + m.group(3);

            }
        }
        return s;
    }

    public static double digitRatio(String s) {
        if (s.trim().length() == 0)
            return 0;
        int d = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                d++;
            }
        }
        return (d * 1d) / s.length();
    }

    static Pattern DIGIT = Pattern.compile("\\d+",Pattern.DOTALL);

    public static boolean containsDigit(String s) {
        return DIGIT.matcher(s).find();
    }

    public static boolean containsOnlyDigit(String s) {
        return DIGIT.matcher(s).matches();
    }

    public static double uppercaseRatio(String s) {
        if (s.trim().length() == 0)
            return 0;
        int d = 0;
        for (char c : s.toCharArray()) {
            if (Character.isUpperCase(c)) {
                d++;
            }
        }
        return (d * 1d) / s.length();
    }

}
