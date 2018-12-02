package zemberek.morphology.analysis.tr;

import static java.lang.String.valueOf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zemberek.core.io.IOs;
import zemberek.core.io.KeyValueReader;
import zemberek.core.io.Strings;
import zemberek.core.text.Regexps;

public class TurkishNumbers {

  public static final long MAX_NUMBER = 999999999999999999L;
  public static final long MIN_NUMBER = -999999999999999999L;
  private static Map<String, Long> stringToNumber = new HashMap<>();
  private static Map<Long, String> NUMBER_TABLE = new HashMap<>();
  private static Map<String, String> ordinalMap = new HashMap<>();

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

    for (Long s : NUMBER_TABLE.keySet()) {
      stringToNumber.put(NUMBER_TABLE.get(s), s);
      // TODO: we should not assume "atmış" -> "altmış"
      stringToNumber.put("atmış", 60L);
    }

    // read ordinal readings.
    try {
      KeyValueReader reader = new KeyValueReader(":", "#");
      ordinalMap = reader
          .loadFromStream(
              IOs.getClassPathResourceAsStream("/tr/turkish-ordinal-numbers.txt"), "utf-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String singleDigitNumbers[] = {"", "bir", "iki", "üç", "dört", "beş", "altı",
      "yedi", "sekiz", "dokuz"};
  private static String tenToNinety[] = {"", "on", "yirmi", "otuz", "kırk", "elli", "altmış",
      "yetmiş", "seksen", "doksan"};
  private static String thousands[] = {"", "bin", "milyon", "milyar", "trilyon", "katrilyon"};
  private static Pattern NUMBER_SEPARATION = Pattern.compile("[0-9]+|[^0-9 ]+");
  private static Pattern NOT_NUMBER = Pattern.compile("[^0-9]");
  private static Pattern NUMBER = Pattern.compile("[0-9]");

  private static void add(long number, String string) {
    NUMBER_TABLE.put(number, string);
  }

  /**
   * converts a given three digit number.
   *
   * @param threeDigitNumber: a three digit number.
   * @return turkish string representation of the input number.
   */
  private static String convertThreeDigit(int threeDigitNumber) {
    String sonuc = "";
    int hundreds = threeDigitNumber / 100;
    int tens = threeDigitNumber / 10 % 10;
    int singleDigit = threeDigitNumber % 10;

    if (hundreds != 0) {
      sonuc = "yüz";
    }
    if (hundreds > 1) {
      sonuc = singleDigitNumbers[hundreds] + " " + sonuc;
    }
    sonuc = sonuc + " " + tenToNinety[tens] + " " + singleDigitNumbers[singleDigit];
    return sonuc.trim();
  }

  public static Map<String, String> getOrdinalMap() {
    return ordinalMap;
  }

  /**
   * returns the Turkish representation of the input. if negative "eksi" string is prepended.
   *
   * @param input: input. must be between (including both) -999999999999999999L to
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
        if (uclu == 1 && sayac == 1) {
          result = thousands[sayac] + " " + result;
        } else {
          result = convertThreeDigit(uclu) + " " + thousands[sayac] + " " + result;
        }
      }
      sayac++;
      girisPos /= 1000;
    }
    if (input < 0) {
      return "eksi " + result.trim();
    } else {
      return result.trim();
    }

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
    if (rest.length() > 0) {
      sb.add(convertToString(Long.parseLong(rest)));
    }

    return String.join(" ", sb);
  }

  /**
   * Returns the value of a single word number value. those values are limited. Word should not
   * contain any spaces and must be in lowercase.
   *
   * @param word the Turkish representation of a single key number string.
   * @return the
   * @throws IllegalArgumentException if word is not a number.
   */
  public static long singleWordNumberValue(String word) {
    if (!stringToNumber.containsKey(word)) {
      throw new IllegalArgumentException(
          "this is not a valid number string (check case and spaces.): " + word);
    }
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
      if (stringToNumber.containsKey(s)) {
        output.add(valueOf(stringToNumber.get(s)));
      } else {
        output.add(s);
      }
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
      if (stringToNumber.containsKey(s)) {
        output.add(valueOf(stringToNumber.get(s)));
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
    for (int i = 0; i < input.toCharArray().length; i++) {
      str.append(input.toCharArray()[i]);
      if (stringToNumber.containsKey(str.toString())) {
        words.add(str.toString());
        str.delete(0, str.length());
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

  private static TurkishTextToNumberConverter textToNumber = new TurkishTextToNumberConverter();

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
    return textToNumber.convert(text.split("[ ]+"));
  }

  public static String convertOrdinalNumberString(String input) {
    String numberPart = input;
    if (input.endsWith(".")) {
      numberPart = Strings.subStringUntilFirst(input, ".");
    }

    long number = Long.parseLong(numberPart);
    String text = convertToString(number);
    String[] words = text.trim().split("[ ]+");
    String lastNumber = words[words.length - 1];

    if (ordinalMap.containsKey(lastNumber)) {
      lastNumber = ordinalMap.get(lastNumber);
    } else {
      throw new RuntimeException("Cannot find ordinal reading for:" + lastNumber);
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < words.length - 1; i++) {
      sb.append(words[i]).append(" ");
    }
    sb.append(lastNumber);
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
    return Regexps.allMatches(NUMBER_SEPARATION, s);
  }

  public static String getOrdinal(String input) {
    return ordinalMap.get(input);
  }

  public static boolean hasNumber(String s) {
    return NUMBER.matcher(s).find();
  }

  public static boolean hasOnlyNumber(String s) {
    return !NOT_NUMBER.matcher(s).find();
  }


  static final Pattern romanNumeralPattern =
      Pattern.compile("^(M{0,3})(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$",
          Pattern.CASE_INSENSITIVE);

  /**
   * Convert a roman numeral to decimal numbers. Copied from public domain source
   * (https://stackoverflow.com/a/19392801).
   *
   * @param s roman numeral
   * @return decimal equivalent. if it cannot be converted, -1.
   */
  public static int romanToDecimal(String s) {
    if (s == null ||
        s.isEmpty() ||
        !romanNumeralPattern.matcher(s).matches()) {
      return -1;
    }

    final Matcher matcher = Pattern.compile("M|CM|D|CD|C|XC|L|XL|X|IX|V|IV|I").matcher(s);
    final int[] decimalValues = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    final String[] romanNumerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V",
        "IV", "I"};
    int result = 0;

    while (matcher.find()) {
      for (int i = 0; i < romanNumerals.length; i++) {
        if (romanNumerals[i].equals(matcher.group(0))) {
          result += decimalValues[i];
        }
      }
    }

    return result;
  }

}
