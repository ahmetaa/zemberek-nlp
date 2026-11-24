package zemberek.morphology.analysis.tr;

import static java.lang.String.valueOf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import zemberek.core.io.IOs;
import zemberek.core.io.KeyValueReader;
import zemberek.core.io.Strings;

public class TurkishNumbers {

  public static final long MAX_NUMBER = 999999999999999999L;
  public static final long MIN_NUMBER = -999999999999999999L;
  private static final Map<String, Long> stringToNumber = new HashMap<>();
  private static final Map<Long, String> NUMBER_TABLE = new HashMap<>();
  private static final Map<String, String> ordinalMap;

  // fill the NUMBER_TABLE and stringToNumber map.
  static {
    add(0, "sıfır");
    add(1, "bir");
    add(2, "iki");
    add(3, "üç");
    add(4, "dört");
    add(5, "beş");
    add(6, "altı");
    add(7, "yedi");
    add(8, "sekiz");
    add(9, "dokuz");
    add(10, "on");
    add(20, "yirmi");
    add(30, "otuz");
    add(40, "kırk");
    add(50, "elli");
    add(60, "altmış");
    add(70, "yetmiş");
    add(80, "seksen");
    add(90, "doksan");
    add(100, "yüz");
    add(1000, "bin");
    add(1000000, "milyon");
    add(1000000000L, "milyar");
    add(1000000000000L, "trilyon");
    add(1000000000000000L, "katrilyon");

    for (Map.Entry<Long, String> entry : NUMBER_TABLE.entrySet()) {
      stringToNumber.put(entry.getValue(), entry.getKey());
    }
    // TODO: we should not assume "atmış" -> "altmış"
    stringToNumber.put("atmış", 60L);

    // read ordinal readings.
    try {
      KeyValueReader reader = new KeyValueReader(":", "#");
      ordinalMap = reader.loadFromStream(
          IOs.getClassPathResourceAsStream("/tr/turkish-ordinal-numbers.txt"), "utf-8");
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load /tr/turkish-ordinal-numbers.txt", e);
    }
  }

  private static final String[] singleDigitNumbers = {
      "", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz"};
  private static final String[] tenToNinety = {
      "", "on", "yirmi", "otuz", "kırk", "elli", "altmış", "yetmiş", "seksen", "doksan"};
  private static final String[] thousands = {
      "", "bin", "milyon", "milyar", "trilyon", "katrilyon"};

  private static void add(long number, String string) {
    NUMBER_TABLE.put(number, string);
  }

  /**
   * converts a given three digit number.
   *
   * @param threeDigitNumber a three digit number.
   * @return turkish string representation of the input number.
   */
  private static String convertThreeDigit(int threeDigitNumber) {
    int hundreds = threeDigitNumber / 100;
    int tens = threeDigitNumber / 10 % 10;
    int singleDigit = threeDigitNumber % 10;

    StringBuilder sb = new StringBuilder(24);
    if (hundreds > 1) {
      sb.append(singleDigitNumbers[hundreds]).append(' ');
    }
    if (hundreds != 0) {
      sb.append("yüz");
    }
    if (tens != 0) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(tenToNinety[tens]);
    }
    if (singleDigit != 0) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(singleDigitNumbers[singleDigit]);
    }
    return sb.toString();
  }

  public static Map<String, String> getOrdinalMap() {
    return ordinalMap;
  }

  /**
   * returns the Turkish representation of the input. if negative "eksi" string is prepended.
   *
   * @param input input. must be between (including both) -999999999999999999L to
   * 999999999999999999L
   * @return Turkish representation of the input. if negative "eksi" string is prepended.
   * @throws IllegalArgumentException if input value is too low or high.
   */
  public static String convertToString(long input) {
    if (input == 0) {
      return "sıfır";
    }
    if (input < MIN_NUMBER || input > MAX_NUMBER) {
      throw new IllegalArgumentException("number is out of bounds:" + input);
    }
    String result = "";
    long girisPos = Math.abs(input);
    int sayac = 0;
    while (girisPos > 0) {
      int uclu = (int) (girisPos % 1000);
      if (uclu != 0) {
        String part;
        if (uclu == 1 && sayac == 1) {
          part = thousands[sayac];
        } else {
          String digits = convertThreeDigit(uclu);
          part = thousands[sayac].isEmpty() ? digits : digits + " " + thousands[sayac];
        }
        result = result.isEmpty() ? part : part + " " + result;
      }
      sayac++;
      girisPos /= 1000;
    }
    return input < 0 ? "eksi " + result : result;
  }

  /**
   * Methods converts a String containing an integer to a Strings.
   */
  public static String convertNumberToString(String input) {
    if (input.startsWith("+")) {
      input = input.substring(1);
    }
    List<String> sb = new ArrayList<>();
    int i;
    for (i = 0; i < input.length(); i++) {
      if (input.charAt(i) == '0') {
        sb.add("sıfır");
      } else {
        break;
      }
    }
    String rest = input.substring(i);
    if (!rest.isEmpty()) {
      sb.add(convertToString(Long.parseLong(rest)));
    }
    return String.join(" ", sb);
  }

  /**
   * Returns the value of a single word number value. those values are limited. Word should not
   * contain any spaces and must be in lowercase.
   *
   * @param word the Turkish representation of a single key number string.
   * @return the numeric value.
   * @throws IllegalArgumentException if word is not a number.
   */
  public static long singleWordNumberValue(String word) {
    Long val = stringToNumber.get(word);
    if (val == null) {
      throw new IllegalArgumentException(
          "this is not a valid number string (check case and spaces.): " + word);
    }
    return val;
  }

  static Long singleWordNumberValueOrNull(String word) {
    return stringToNumber.get(word);
  }

  /**
   * replaces all number strings with actual numbers. Such as:
   * <pre>
   * ["hello bir on iki nokta otuz beş hello"] -> ["hello 1 10 2 nokta 30 5 hello"]
   * </pre>
   *
   * @param inputSequence a sequence of words.
   * @return same as input but string representations of numbers are replaced with numbers.
   */
  public static List<String> replaceNumberStrings(List<String> inputSequence) {
    List<String> output = new ArrayList<>(inputSequence.size());
    for (String s : inputSequence) {
      Long val = stringToNumber.get(s);
      output.add(val != null ? valueOf(val) : s);
    }
    return output;
  }

  /**
   * seperates connected number texts. such as
   * <pre>
   * ["oniki","otuzbeş","ikiiii"] -> ["on","iki","otuz","beş","ikiiii"]
   * </pre>
   *
   * @param inputSequence a sequence of words.
   * @return same list with strings where connected number strings are separated.
   */
  public static List<String> seperateConnectedNumbers(List<String> inputSequence) {
    List<String> output = new ArrayList<>(inputSequence.size());
    for (String s : inputSequence) {
      Long val = stringToNumber.get(s);
      if (val != null) {
        output.add(valueOf(val));
        continue;
      }
      output.addAll(seperateConnectedNumbers(s));
    }
    return output;
  }

  /**
   * seperates connected number texts. such as
   * <pre>
   * ["oniki","otuzbes","ikiiii"] -> ["on","iki","otuz","bes","ikiiii"]
   * </pre>
   *
   * @param input a single key.
   * @return same list with strings where connected number strings are separated.
   */
  public static List<String> seperateConnectedNumbers(String input) {
    StringBuilder str = new StringBuilder();
    List<String> words = new ArrayList<>(2);
    boolean numberFound = false;
    for (int i = 0, n = input.length(); i < n; i++) {
      str.append(input.charAt(i));
      String current = str.toString();
      if (stringToNumber.containsKey(current)) {
        words.add(current);
        str.setLength(0);
        numberFound = true;
      } else {
        numberFound = false;
      }
    }
    if (!numberFound) {
      words.clear();
      words.add(input);
    }
    return words;
  }

  private static final TurkishTextToNumberConverter textToNumber =
      new TurkishTextToNumberConverter();

  /**
   * Converts an array of number strings to number, if possible. Returns -1 if conversion is not possible.
   * <pre>
   *   "bir" -> 1
   *   "on", "bir" -> 11
   *   "bir", "bin" -> -1
   *   "bir", "armut" -> -1
   * </pre>
   * @param words Array of number strings.
   * @return number representation, or -1 if not possible to convert.
   */
  public static long convertToNumber(String... words) {
    return textToNumber.convert(words);
  }

  /**
   * Converts a text to number, if possible. Returns -1 if conversion is not possible.
   * <pre>
   *   "bir" -> 1
   *   "on bir" -> 11
   *   "bir bin" -> -1
   *   "bir armut" -> -1
   * </pre>
   * @param text a string.
   * @return number representation of input string, or -1 if not possible to convert.
   */
  public static long convertToNumber(String text) {
    if (text == null) {
      return -1;
    }
    return textToNumber.convert(splitBySpaces(text));
  }

  public static String convertOrdinalNumberString(String input) {
    String numberPart = input;
    if (input.endsWith(".")) {
      numberPart = Strings.subStringUntilFirst(input, ".");
    }

    long number = Long.parseLong(numberPart);
    String text = convertToString(number);
    List<String> words = splitBySpaces(text);
    String lastNumber = words.get(words.size() - 1);

    String ordinal = ordinalMap.get(lastNumber);
    if (ordinal == null) {
      throw new RuntimeException("Cannot find ordinal reading for:" + lastNumber);
    }

    StringBuilder sb = new StringBuilder(text.length() + 4);
    for (int i = 0; i < words.size() - 1; i++) {
      sb.append(words.get(i)).append(' ');
    }
    sb.append(ordinal);
    return sb.toString();
  }

  /**
   * Separate digits and non digits as Strings. Such as:
   * <pre>
   * A12 -> "A" "12"
   * 1A12'ye -> "1" "A" "12" "'ye"
   * </pre>
   *
   * @param s input.
   * @return separated list of numerical and non numerical tokens.
   */
  public static List<String> separateNumbers(String s) {
    List<String> result = new ArrayList<>(4);
    int len = s.length();
    int i = 0;
    while (i < len) {
      char c = s.charAt(i);
      if (c == ' ') {
        i++;
        continue;
      }
      int start = i;
      if (isAsciiDigit(c)) {
        i++;
        while (i < len && isAsciiDigit(s.charAt(i))) {
          i++;
        }
      } else {
        i++;
        while (i < len) {
          char ch = s.charAt(i);
          if (ch == ' ' || isAsciiDigit(ch)) {
            break;
          }
          i++;
        }
      }
      result.add(s.substring(start, i));
    }
    return result;
  }

  public static String getOrdinal(String input) {
    return ordinalMap.get(input);
  }

  public static boolean hasNumber(String s) {
    for (int i = 0, n = s.length(); i < n; i++) {
      if (isAsciiDigit(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasOnlyNumber(String s) {
    int n = s.length();
    if (n == 0) {
      return false;
    }
    for (int i = 0; i < n; i++) {
      if (!isAsciiDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static final Pattern ROMAN_NUMERAL_PATTERN =
      Pattern.compile("^(M{0,3})(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$",
          Pattern.CASE_INSENSITIVE);

  /**
   * Converts a canonical Roman numeral (1..3999, case-insensitive) to a decimal integer.
   *
   * @param s roman numeral
   * @return decimal equivalent, or -1 if invalid.
   */
  public static int romanToDecimal(String s) {
    if (s == null || s.isEmpty() || !ROMAN_NUMERAL_PATTERN.matcher(s).matches()) {
      return -1;
    }
    int result = 0;
    int prev = 0;
    for (int i = s.length() - 1; i >= 0; i--) {
      int val = switch (Character.toUpperCase(s.charAt(i))) {
        case 'I' -> 1;
        case 'V' -> 5;
        case 'X' -> 10;
        case 'L' -> 50;
        case 'C' -> 100;
        case 'D' -> 500;
        case 'M' -> 1000;
        default -> 0;
      };
      result += (val < prev) ? -val : val;
      prev = val;
    }
    return result;
  }

  private static List<String> splitBySpaces(String text) {
    List<String> parts = new ArrayList<>(4);
    int len = text.length();
    int i = 0;
    while (i < len) {
      while (i < len && text.charAt(i) == ' ') {
        i++;
      }
      int start = i;
      while (i < len && text.charAt(i) != ' ') {
        i++;
      }
      if (i > start) {
        parts.add(text.substring(start, i));
      }
    }
    return parts;
  }

  private static boolean isAsciiDigit(char c) {
    return c >= '0' && c <= '9';
  }

}
