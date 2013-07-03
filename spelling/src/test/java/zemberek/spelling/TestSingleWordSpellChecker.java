package zemberek.spelling;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.DoubleValueSet;
import zemberek.core.logging.Log;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TestSingleWordSpellChecker {

    @Test
    public void simpleDecodeTest() {
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        String vocabulary = "elma";
        spellChecker.addWord(vocabulary);
        Assert.assertTrue(spellChecker.decode(vocabulary).contains(vocabulary));
        check1Distance(spellChecker, "elma");

        spellChecker.addWord("armut");
        spellChecker.addWord("ayva");
        check1Distance(spellChecker, "armut");
    }

    @Test
    public void multiWordDecodeTest() {
        Log.setDebug();
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        spellChecker.addWords("çak", "sak", "saka", "bak", "çaka", "çakal", "sakal");

        DoubleValueSet<String> result = spellChecker.decode("çak");

        Assert.assertEquals(4, result.size());
        assertContainsAll(result, "çak", "sak", "bak", "çaka");

        double delta = 0.0001;
        Assert.assertEquals(0, result.get("çak"), delta);
        Assert.assertEquals(1, result.get("sak"), delta);
        Assert.assertEquals(1, result.get("bak"), delta);
        Assert.assertEquals(1, result.get("çaka"), delta);

        result = spellChecker.decode("çaka");

        Assert.assertEquals(4, result.size());
        assertContainsAll(result, "çaka", "saka", "çakal", "çak");

        Assert.assertEquals(0, result.get("çaka"), delta);
        Assert.assertEquals(1, result.get("saka"), delta);
        Assert.assertEquals(1, result.get("çakal"), delta);
        Assert.assertEquals(1, result.get("çak"), delta);

        Log.setInfo();
    }

    @Test
    public void performanceTest() throws IOException {
        List<String> words = Resources.readLines(Resources.getResource("10000"), Charsets.UTF_8);
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker();
        spellChecker.buildDictionary(words);
        Stopwatch sw = new Stopwatch().start();
        int solutionCount = 0;
        for (String word : words) {
            DoubleValueSet<String> result = spellChecker.decode(word);
            solutionCount += result.size();
        }
        Log.info("Elapsed: " + sw.elapsed(TimeUnit.MILLISECONDS));
        Log.info("Solution count:" + solutionCount);
    }

    void assertContainsAll(DoubleValueSet<String> set, String... words) {
        for (String word : words) {
            Assert.assertTrue(set.contains(word));
        }
    }

    private void check1Distance(SingleWordSpellChecker spellChecker, String expected) {
        Set<String> randomDeleted = randomDelete(expected, 1);
        for (String s : randomDeleted) {
            DoubleValueSet<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> randomInserted = randomInsert(expected, 1);
        for (String s : randomInserted) {
            DoubleValueSet<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> randomSubstitute = randomSubstitute(expected, 1);
        for (String s : randomSubstitute) {
            DoubleValueSet<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> transpositions = transpositions(expected);
        for (String s : transpositions) {
            DoubleValueSet<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }
    }

    @Test
    public void nearKeyCheck() {
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(
                1,
                SingleWordSpellChecker.TURKISH_Q_NEAR_KEY_MAP);
        String vocabulary = "elma";
        spellChecker.addWord(vocabulary);
        Assert.assertTrue(spellChecker.decode(vocabulary).contains(vocabulary));

        // "r" is near key "e" therefore it give a smaller penalty.
        DoubleValueSet<String> res1 = spellChecker.decode("rlma");
        Assert.assertTrue(res1.contains("elma"));
        DoubleValueSet<String> res2 = spellChecker.decode("ylma");
        Assert.assertTrue(res2.contains("elma"));
        Assert.assertTrue(res1.get("elma") < res2.get("elma"));

    }

    Set<String> randomDelete(String input, int d) {
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

    Set<String> randomInsert(String input, int d) {
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

    Set<String> randomSubstitute(String input, int d) {
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

    Set<String> transpositions(String input) {
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
