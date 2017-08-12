package zemberek.normalization;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.collections.FloatValueMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SingleWordSpellCheckerTest {

    @Test
    public void simpleDecodeTest() {
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        String vocabulary = "elma";
        spellChecker.addWord(vocabulary);
        Assert.assertTrue(spellChecker.decode(vocabulary).contains(vocabulary));
        NormalizationTestUtils.check1Distance(spellChecker, "elma");

        spellChecker.addWord("armut");
        spellChecker.addWord("ayva");
        NormalizationTestUtils.check1Distance(spellChecker, "armut");
    }

    @Test
    public void simpleDecodeTest2() {
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        spellChecker.addWords("apple", "apples");
        FloatValueMap<String> res = spellChecker.decode("apple");
        for (String re : res) {
            System.out.println(re);
        }
    }

    @Test
    public void simpleDecodeTest3() {
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        spellChecker.addWords("apple", "apples");
        List<SingleWordSpellChecker.ScoredString> res = spellChecker.getSuggestionsWithScores("apple");
        for (SingleWordSpellChecker.ScoredString re : res) {
            System.out.println(re.s);
        }
    }

    @Test
    public void multiWordDecodeTest() {

        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        spellChecker.addWords("çak", "sak", "saka", "bak", "çaka", "çakal", "sakal");

        FloatValueMap<String> result = spellChecker.decode("çak");

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

    }

    @Test
    public void performanceTest() throws Exception {
        Path r = Paths.get(ClassLoader.getSystemResource("10000_frequent_turkish_word").toURI());
        List<String> words = Files.readAllLines(r, StandardCharsets.UTF_8);
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker();
        spellChecker.buildDictionary(words);
        long start = System.currentTimeMillis();
        int solutionCount = 0;
        for (String word : words) {
            FloatValueMap<String> result = spellChecker.decode(word);
            solutionCount += result.size();
        }
        System.out.println("Elapsed: " + (System.currentTimeMillis() - start));
        System.out.println("Solution count:" + solutionCount);
    }

    void assertContainsAll(FloatValueMap<String> set, String... words) {
        for (String word : words) {
            Assert.assertTrue(set.contains(word));
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
        FloatValueMap<String> res1 = spellChecker.decode("rlma");
        Assert.assertTrue(res1.contains("elma"));
        FloatValueMap<String> res2 = spellChecker.decode("ylma");
        Assert.assertTrue(res2.contains("elma"));
        Assert.assertTrue(res1.get("elma") < res2.get("elma"));

    }


}
