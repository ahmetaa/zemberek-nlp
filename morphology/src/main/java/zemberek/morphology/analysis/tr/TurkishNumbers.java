package zemberek.morphology.analysis.tr;

import zemberek.core.io.IOs;
import zemberek.core.io.KeyValueReader;
import zemberek.core.io.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.String.valueOf;

public class TurkishNumbers {

    private static Map<String, Long> stringToNumber = new HashMap<>();
    private static Map<Long, String> NUMBER_TABLE = new HashMap<>();
    private static Map<String, String> ORDINAL_READING_TABLE = new HashMap<>();

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
            stringToNumber.put("atmış", 60L);
        }

        // read ordinal readings.
        try {
            KeyValueReader reader = new KeyValueReader(":", "#");
            ORDINAL_READING_TABLE = reader
                    .loadFromStream(IOs.getClassPathResourceAsStream("/resources/turkish-ordinal-numbers.txt"), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void add(long number, String string) {
        NUMBER_TABLE.put(number, string);
    }

    public static final String BUCUK = "buçuk";

    public static final String SIFIR = NUMBER_TABLE.get(0L);

    public static final String BIR = NUMBER_TABLE.get(1L);
    public static final String IKI = NUMBER_TABLE.get(2L);
    public static final String UC = NUMBER_TABLE.get(3L);
    public static final String DORT = NUMBER_TABLE.get(4L);
    public static final String BES = NUMBER_TABLE.get(5L);
    public static final String ALTI = NUMBER_TABLE.get(6L);
    public static final String YEDI = NUMBER_TABLE.get(7L);
    public static final String SEKIZ = NUMBER_TABLE.get(8L);
    public static final String DOKUZ = NUMBER_TABLE.get(9L);

    public static final String ON = NUMBER_TABLE.get(10L);
    public static final String YIRMI = NUMBER_TABLE.get(20L);
    public static final String OTUZ = NUMBER_TABLE.get(30L);
    public static final String KIRK = NUMBER_TABLE.get(40L);
    public static final String ELLI = NUMBER_TABLE.get(50L);
    public static final String ATMIS = NUMBER_TABLE.get(60L);
    public static final String YETMIS = NUMBER_TABLE.get(70L);
    public static final String SEKSEN = NUMBER_TABLE.get(80L);
    public static final String DOKSAN = NUMBER_TABLE.get(90L);

    public static final String YUZ = NUMBER_TABLE.get(100L);
    public static final String BIN = NUMBER_TABLE.get(1000L);
    public static final String MILYON = NUMBER_TABLE.get(1000000L);
    public static final String MILYAR = NUMBER_TABLE.get(1000000000L);
    public static final String TRILYON = NUMBER_TABLE.get(1000000000000L);
    public static final String KATRILYON = NUMBER_TABLE.get(1000000000000000L);

    public static final long MAX_NUMBER = 999999999999999999L;
    public static final long MIN_NUMBER = -999999999999999999L;


    private static String singleDigitNumbers[] = {"", BIR, IKI, UC, DORT, BES, ALTI, YEDI, SEKIZ, DOKUZ};
    private static String tenToNinety[] = {"", ON, YIRMI, OTUZ, KIRK, ELLI, ATMIS, YETMIS, SEKSEN, DOKSAN};
    private static String thousands[] = {"", BIN, MILYON, MILYAR, TRILYON, KATRILYON};

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

        if (hundreds != 0)
            sonuc = YUZ;
        if (hundreds > 1)
            sonuc = singleDigitNumbers[hundreds] + " " + sonuc;
        sonuc = sonuc + " " + tenToNinety[tens] + " " + singleDigitNumbers[singleDigit];
        return sonuc.trim();
    }

    /**
     * returns the Turkish representation of the input. if negative "eksi" stirng is prepended.
     *
     * @param input: input. must be between (including both) -999999999999999999L to 999999999999999999L
     * @return Turkish representation of the input. if negative "eksi" stirng is prepended.
     * @throws IllegalArgumentException if input value is too low or high.
     */
    public static String convertToString(long input) {
        if (input == 0)
            return SIFIR;
        if (input < MIN_NUMBER || input > MAX_NUMBER)
            throw new IllegalArgumentException("number is out of bounds:" + input);
        String result = "";
        long girisPos = Math.abs(input);
        int sayac = 0;
        while (girisPos > 0) {
            int uclu = (int) (girisPos % 1000);
            if (uclu != 0) {
                if (uclu == 1 && sayac == 1)
                    result = thousands[sayac] + " " + result;
                else result = convertThreeDigit(uclu) + " " + thousands[sayac] + " " + result;
            }
            sayac++;
            girisPos /= 1000;
        }
        if (input < 0)
            return "eksi " + result.trim();
        else
            return result.trim();

    }

    /**
     * Methods converts a String containing an integer to a Strings.
     *
     * @param input
     * @return
     */
    public static String convertNumberToString(String input) {
        if (input.startsWith("+"))
            input = input.substring(1);
        StringBuilder sb = new StringBuilder();
        int i;
        for (i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '0')
                sb.append(" ").append(SIFIR).append(" ");
            else {
                break;
            }
        }
        String rest = input.substring(i);
        if (rest.length() == 0) {
            return sb.toString().trim();
        }

        return (Strings.whiteSpacesToSingleSpace(sb.toString() + " " + convertToString(Long.parseLong(rest)))).trim();
    }


    /**
     * return the value of a single key number value. those values are limited.
     * key should not contain any spaces and must be in smallcase.
     *
     * @param word the Turkish representation of a single key number string.
     * @return the
     * @throws IllegalArgumentException if key is not a number.
     */
    public static long singleWordNumberValue(String word) {
        if (!stringToNumber.containsKey(word)) {
            throw new IllegalArgumentException("this is not a valid number string (check case and spaces.): " + word);
        }
        return stringToNumber.get(word);
    }


    /**
     * replaces all number strings with actual numbers.
     * Such as: ["hello bir on iki nokta otuz bes hello"] -> ["hello 1 10 2 nokta 30 5 hello"]
     *
     * @param inputSequence a sequence of words.
     * @return same as input but string representations of numbers are replaced with numbers.
     */
    public static List<String> replaceNumberStrings(List<String> inputSequence) {
        List<String> output = new ArrayList<>(inputSequence.size());
        for (String s : inputSequence) {
            if (stringToNumber.containsKey(s))
                output.add(valueOf(stringToNumber.get(s)));
            else output.add(s);
        }
        return output;
    }

    /**
     * seperates connected number texts. such as
     * ["oniki","otuzbes","ikiiii"] -> ["on","iki","otuz","bes","ikiiii"]
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
     * ["oniki","otuzbes","ikiiii"] -> ["on","iki","otuz","bes","ikiiii"]
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

    public static long convertToNumber(String... words) {
        return new TurkishTextToNumberConverter().convert(words);
    }

    public static String convertOrdinalNumberString(String input) {
        String numberPart = input;
        if (input.endsWith(""))
            numberPart = Strings.subStringUntilFirst(input, "");

        long number = Long.parseLong(numberPart);
        String text = convertToString(number);
        String[] words = text.trim().split("[ ]+");
        String lastNumber = words[words.length - 1];

        if (ORDINAL_READING_TABLE.containsKey(lastNumber))
            lastNumber = ORDINAL_READING_TABLE.get(lastNumber);
        else
            throw new RuntimeException("Cannot find ordinal reading for:" + lastNumber);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length - 1; i++) {
            sb.append(words[i]).append(" ");
        }
        sb.append(lastNumber);
        return sb.toString();
    }

    public static String getOrdinal(String input) {
        return ORDINAL_READING_TABLE.get(input);
    }

    private static Pattern NUMBER_SEPARATION = Pattern.compile("[0-9]+|[^0-9 ]+");

    private static Pattern NOT_NUMBER = Pattern.compile("[^0-9]");
    private static Pattern NUMBER = Pattern.compile("[0-9]");

    public static boolean hasNumber(String s) {
        return NUMBER.matcher(s).find();
    }

    public static boolean hasOnlyNumber(String s) {
        return !NOT_NUMBER.matcher(s).find();
    }
}
