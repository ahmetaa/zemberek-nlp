package zemberek.core.text;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regexps {
    public static List<String> firstGroupMatches(Pattern p, String s) {
        List<String> matches = new ArrayList<>();
        Matcher m = p.matcher(s);
        while (m.find()) {
            matches.add(m.group(1).trim());
        }
        return matches;
    }

    public static List<String> allMatches(Pattern p, String s) {
        List<String> matches = new ArrayList<>();
        Matcher m = p.matcher(s);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches;
    }

    public static String firstMatchFirstGroup(Pattern p, String s) {

        Matcher m = p.matcher(s);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static String firstMatch(Pattern p, String s, int group) {

        Matcher m = p.matcher(s);
        if (m.find()) {
            return m.group(group);
        }
        return null;
    }

    public static String firstMatch(Pattern p, String s) {

        Matcher m = p.matcher(s);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    public static boolean matchesAny(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find();
    }

    public static boolean matchesAny(String regexp, String s) {
        Matcher m = Pattern.compile(regexp).matcher(s);
        return m.find();
    }

    public static Pattern defaultPattern(String regexp) {
        return Pattern.compile(regexp, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    /**
     * checks the matches if they exist as a key in the map.
     * if it exists, replaces the match with the "value" in the map.
     *
     * @param m   matcher.
     * @param map map to replace matches with values.
     * @return String after the replacement.
     */
    public static String replaceMap(Matcher m, Map<String, String> map) {
        StringBuffer buffer = new StringBuffer();
        while (m.find()) {
            String match = m.group();
            if (map.containsKey(match)) {
                m.appendReplacement(buffer, map.get(match));
            }
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

    public static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+");

    public static List<String> getMatchesForGroup(String str, Pattern pattern, int groupIndex) {
        List<String> result = new ArrayList<>();
        Matcher m = pattern.matcher(str);
        while (m.find()) {
            result.add(m.group(groupIndex));
        }
        return result;
    }

}
