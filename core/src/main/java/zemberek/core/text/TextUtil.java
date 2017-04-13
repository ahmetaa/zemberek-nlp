package zemberek.core.text;


import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import zemberek.core.collections.FixedBitVector;
import zemberek.core.io.IOs;
import zemberek.core.io.KeyValueReader;
import zemberek.core.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextUtil {

    public static List<String> getElementChunks(String allContent, String elementName) {
        elementName = elementName.trim().replaceAll("<>", "");
        Pattern p = Pattern.compile("(<" + elementName + ")" + "(.+?)" + "(</" + elementName + ">)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return Regexps.allMatches(p, allContent);
    }

    public static List<String> getSingleLineElementData(String allContent, String elementName) {
        elementName = elementName.trim().replaceAll("<>", "");
        Pattern p = Pattern.compile("(<" + elementName + ")" + "(.+?)" + "(>)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return Regexps.allMatches(p, allContent);
    }

    private static Pattern attributePattern =
            Pattern.compile("([\\w\\-]+)([ ]*=[ ]*\")(.+?)(\")"); // catches all xml attributes in a line.
    /**
     * returns a map with attributes of an xml line. For example if [content] is `<Foo a="one" b="two">` and [element]
     * is `Foo` it returns [a:one b:two] Map. It only check the first match in the content.
     */
    public static Map<String, String> getAttributes(String content, String elementName) {
        elementName = elementName.trim();
        Pattern p = Pattern.compile("(<" + elementName + ")" + "(.+?)" + "(>)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        String elementLine = Regexps.firstMatch(p, content);

        Map<String, String> attributes = new HashMap<>();
        if (elementLine == null) {
            return attributes;
        }

        Matcher m = attributePattern.matcher(elementLine);
        while (m.find()) {
            attributes.put(m.group(1), m.group(3));
        }
        return attributes;
    }

    /**
     * returns a map with attributes of an xml line. For example if [content] is `<Foo a="one" b="two">`
     * it returns [a:one b:two] Map. It only checks the first match in the content.
     */
    public static Map<String, String> getAttributes(String content) {
        return getAttributes(content, "");
    }


    public static FixedBitVector generateBitLookup(String characters) {
        int max = 0;
        for (char c : characters.toCharArray()) {
            if (c > max) {
                max = c;
            }
        }

        FixedBitVector result = new FixedBitVector(max + 1);
        for (char c : characters.toCharArray()) {
            result.set(c);
        }
        return result;
    }

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


    /**
     * This method converts different apostrophe symbols to a unified form.
     *
     * @param input input string.
     * @return cleaned input string.
     */
    public static String normalizeApostrophes(String input) {
        // rdquo, ldquo, laquo, raquo, Prime sybols in unicode.
        return input.replaceAll("[\u0091\u0092\u2032´`’‘]", "'");
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

    static Pattern DIGIT = Pattern.compile("\\d+", Pattern.DOTALL);

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

    public static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

    public static String loadUtf8AsString(Path filePath) throws IOException {
        return String.join("\n", Files.readAllLines(filePath, StandardCharsets.UTF_8));
    }

    public static List<String> loadLinesWithText(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8)
                .stream()
                .filter(s -> s.trim().length() > 0)
                .collect(Collectors.toList());
    }

    public static String escapeQuotesApostrpohes(String input) {
        return input.replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
    }

    public static Path createTempFile(String content) throws IOException {
        return createTempFile(Collections.singletonList(content));
    }

    public static Path createTempFile(String... content) throws IOException {
        return createTempFile(Arrays.asList(content));
    }

    public static Path createTempFile(List<String> content) throws IOException {
        Path temp = Files.createTempFile("tmp", ".tmp");
        temp.toFile().deleteOnExit();
        Files.write(temp, content, StandardCharsets.UTF_8);
        return temp;
    }

    private static Map<String, String> HTML_STRING_TO_CHAR_MAP_FULL = new HashMap<>();
    private static Map<String, String> HTML_STRING_TO_CHAR_MAP_COMMON = new HashMap<>();
    private static Map<Character, Character> SPECIAL_CHAR_TO_SIMPLE = new HashMap<>();

    static {
        initializeHtmlCharMap(HTML_STRING_TO_CHAR_MAP_FULL, "zemberek/core/text/html-char-map-full.txt");
        initializeHtmlCharMap(HTML_STRING_TO_CHAR_MAP_COMMON, "zemberek/core/text/html-char-map-common.txt");
        initializeToSimplifiedChars();
    }

    private static void initializeHtmlCharMap(Map<String, String> map, String resource) {
        try {
            InputStream stream = IOs.getClassPathResourceAsStream(resource);
            Map<String, String> fullMap = new KeyValueReader(":", "!")
                    .loadFromStream(stream, "utf-8");
            for (String key : fullMap.keySet()) {
                String value = fullMap.get(key);
                if (value.length() != 0) {
                    if (value.length() > 1)
                        throw new IllegalArgumentException("I was expecting a single or no character but:" + value);
                    map.put("&" + key + ";", value);
                }
            }
            // add nbrsp manually.
            map.put("&nbsp;", " ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initializeToSimplifiedChars() {
        try {
            Map<String, String> fullMap = new KeyValueReader(":", "#")
                    .loadFromStream(IOs.getClassPathResourceAsStream("zemberek/core/text/special-char-to-simple-char.txt"), "utf-8");
            for (String key : fullMap.keySet()) {
                SPECIAL_CHAR_TO_SIMPLE.put(key.charAt(0), fullMap.get(key).charAt(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Pattern AMPERSAND_PATTERN = Pattern.compile("&[^ ]{2,6};");

    /**
     * replaces all special html Strings such as(&....; or &#dddd;) with their original characters.
     *
     * @param input input which may contain html specific strings.
     * @return cleaned input.
     */
    public static String convertAmpresandStrings(String input) {
        return Regexps.replaceMap(AMPERSAND_PATTERN.matcher(input), HTML_STRING_TO_CHAR_MAP_FULL);
    }

    /**
     * This method removes all &....; type strings form html.
     *
     * @param input input String
     * @return cleaned input.
     */
    public static String removeAmpresandStrings(String input) {
        // remove rest.
        Matcher m = AMPERSAND_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (m.find()) {
            String match = m.group();
            if (match.length() < 8) {
                m.appendReplacement(buffer, "");
            }
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

    public static Pattern HTML_TAG_CONTENT_PATTERN = Regexps.defaultPattern("<[^>]+>");
    public static Pattern HTML_COMMENT_CONTENT_PATTERN = Regexps.defaultPattern("<!--.+?-->");

    /**
     * @param input input String
     * @return input, all html comment and tags are cleaned.
     */
    public static String cleanHtmlTagsAndComments(String input) {
        return HTML_TAG_CONTENT_PATTERN.matcher(HTML_COMMENT_CONTENT_PATTERN.matcher(input).replaceAll("")).replaceAll("");
    }

    public static String cleanAllHtmlRelated(String input) {
        return cleanHtmlTagsAndComments(removeAmpresandStrings(convertAmpresandStrings(input)));
    }

    public static Pattern HTML_NEWLINE_PATTERN =
            Regexps.defaultPattern("•|\u8286|<strong>|</strong>|</p>|<p>|</br>|<br />|<br>|</span>|<span>|<li>|</li>|<b>|</b>");

    /**
     * it replaces several paragraph html tags with desired String.
     *
     * @param input       input String.
     * @param replacement replacement.
     * @return content, new line html tags are replaced with a given string.
     */
    public static String generateLineBreaksFromHtmlTags(String input, String replacement) {
        return HTML_NEWLINE_PATTERN.matcher(input).replaceAll(replacement);
    }

    static Pattern HTML_BODY = Regexps.defaultPattern("<body.+?</body>");
    static Pattern SCRIPT = Regexps.defaultPattern("<script.+?</script>");

    public static String getHtmlBody(String html) {
        Preconditions.checkNotNull(html, "input cannot be null.");
        return Regexps.firstMatch(HTML_BODY, html);
    }

    public static String cleanScripts(String html) {
        Preconditions.checkNotNull(html, "input cannot be null.");
        return SCRIPT.matcher(html).replaceAll(" ");
    }


    public static final String HTML_START = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
    public static final String META_CHARSET_UTF8 = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>";
    static Pattern HTML_META_CONTENT_TAG = Regexps.defaultPattern("<meta http-equiv=\"content-type\".+?>");

    /**
     * it generates an HTML only containing bare head and meta tags with utf-8 charset. and body content.
     * it also eliminates all script tags.
     *
     * @param htmlToReduce html file to reduce.
     * @return reduced html file. charset is set to utf-8.
     */
    public static String reduceHtmlFixedUTF8Charset(String htmlToReduce) {
        return HTML_START + "<html><head>" + META_CHARSET_UTF8 + "</head>\n" +
                cleanScripts(getHtmlBody(htmlToReduce)) + "</html>";
    }

    public static String reduceHtml(String htmlToReduce) {
        String htmlBody = getHtmlBody(htmlToReduce);
        if (htmlBody == null) {
            Log.warn("Cannot get html body. ");
            return htmlToReduce;
        }
        List<String> parts = Regexps.allMatches(HTML_META_CONTENT_TAG, htmlToReduce);
        return HTML_START + "<html><head>" + Joiner.on(" ").join(parts) +
                "</head>\n" + cleanScripts(htmlBody) + "</html>";
    }
}
