package zemberek.normalization;

import org.junit.Assert;
import zemberek.core.collections.FloatValueMap;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by mayata on 13/08/17.
 * <p>
 * Utils class for testing, handles some common code used among testing classes.
 */
public class NormalizationTestUtils {

    // keep the util class constructor private to prevent instantiations. 
    private NormalizationTestUtils() {
    }

    public static void check1Distance(NormalizationDecoder<String> spellChecker, String expected) {
        Set<String> randomDeleted = randomDelete(expected, 1);
        for (String s : randomDeleted) {
            FloatValueMap<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> randomInserted = randomInsert(expected, 1);
        for (String s : randomInserted) {
            FloatValueMap<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> randomSubstitute = randomSubstitute(expected, 1);
        for (String s : randomSubstitute) {
            FloatValueMap<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> transpositions = transpositions(expected);
        for (String s : transpositions) {
            FloatValueMap<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }
    }


    public static Set<String> randomDelete(String input, int d) {
        Set<String> result = new HashSet<>();
        Random r = new Random(0xbeef);
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(input);
            for (int j = 0; j < d; j++)
                sb.deleteCharAt(r.nextInt(sb.length()));
            result.add(sb.toString());
        }
        return result;
    }

    public static Set<String> randomInsert(String input, int d) {
        Set<String> result = new HashSet<>();
        Random r = new Random(0xbeef);
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(input);
            for (int j = 0; j < d; j++)
                sb.insert(r.nextInt(sb.length() + 1), "x");
            result.add(sb.toString());
        }
        return result;
    }

    public static Set<String> randomSubstitute(String input, int d) {
        Set<String> result = new HashSet<>();
        Random r = new Random(0xbeef);
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(input);
            for (int j = 0; j < d; j++) {
                int start = r.nextInt(sb.length());
                sb.replace(start, start + 1, "x");
            }
            result.add(sb.toString());
        }
        return result;
    }

    public static Set<String> transpositions(String input) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < input.length() - 1; i++) {
            StringBuilder sb = new StringBuilder(input);
            char tmp = sb.charAt(i);
            sb.setCharAt(i, sb.charAt(i + 1));
            sb.setCharAt(i + 1, tmp);
            result.add(sb.toString());
        }
        return result;
    }


}
