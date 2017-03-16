package zemberek.normalization;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.ScoredItem;
import zemberek.core.collections.FloatValueMap;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class CharacterGraphDecoderTest {

    @Test
    public void transpositionTest() {
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(1);
        String vocabulary = "elma";
        spellChecker.addWord(vocabulary);
        List<String> suggestions = spellChecker.getSuggestions("emla");
        Assert.assertEquals(1, suggestions.size());
        Assert.assertEquals("elma", suggestions.get(0));
    }

    @Test
    public void simpleDecodeTest() {
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(1);
        String vocabulary = "elma";
        spellChecker.addWord(vocabulary);
        Assert.assertTrue(spellChecker.decode(vocabulary).contains(vocabulary));
        check1Distance(spellChecker, "elma");
        spellChecker.addWord("armut");
        spellChecker.addWord("ayva");
        check1Distance(spellChecker, "armut");
    }

    @Test
    public void simpleDecodeTest2() {
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(1);
        spellChecker.addWords("apple", "apples");
        FloatValueMap<String> res = spellChecker.decode("apple");
        for (String re : res) {
            System.out.println(re);
        }
    }

    @Test
    public void simpleDecodeTest3() {
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(1);
        spellChecker.addWords("apple", "apples");
        List<ScoredItem<String>> res = spellChecker.getSuggestionsWithScores("apple");
        for (ScoredItem<String> re : res) {
            System.out.println(re.item);
        }
    }

    @Test
    public void sortedResultSet() {
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(1);
        spellChecker.addWords("apple", "apples", "app", "foo");
        List<String> res = spellChecker.getSuggestionsSorted("apple");
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("apple", res.get(0));
        Assert.assertEquals("apples", res.get(1));
    }

    @Test
    public void asciiTolerantTest() {
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(1);
        spellChecker.addWords("şıra", "sıra", "kömür");
        CharacterGraphDecoder.CharMatcher matcher = CharacterGraphDecoder.ASCII_TOLERANT_MATCHER;
        List<ScoredItem<String>> res = spellChecker.getSuggestionsWithScores("komur", matcher);
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("kömür", res.get(0).item);

        res = spellChecker.getSuggestionsWithScores("sıra", matcher);
        Assert.assertEquals(2, res.size());
        assertContainsAll(res, "sıra", "şıra");

    }


    @Test
    public void multiWordDecodeTest() {

        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(1);
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
    @Ignore("Not a unit test.")
    public void performanceTest() throws Exception {
        Path r = Paths.get(ClassLoader.getSystemResource("zemberek-parsed-words-min30.txt").toURI());
        List<String> words = Files.readAllLines(r, StandardCharsets.UTF_8);
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(1);
        spellChecker.buildDictionary(words);
        long start = System.currentTimeMillis();
        int solutionCount = 0;
        int c = 0;
        for (String word : words) {
            List<String> result = spellChecker.getSuggestionsSorted(word);
            solutionCount += result.size();
            if (c++ > 20000) {
                break;
            }
        }
        System.out.println("Elapsed: " + (System.currentTimeMillis() - start));
        System.out.println("Solution count:" + solutionCount);
    }

    private void assertContainsAll(FloatValueMap<String> set, String... words) {
        for (String word : words) {
            Assert.assertTrue(set.contains(word));
        }
    }

    private void assertContainsAll(List<ScoredItem<String>> list, String... words) {
        Set<String> set = new HashSet<>();
        set.addAll(list.stream().map(s1 -> s1.item).collect(Collectors.toList()));
        for (String word : words) {
            Assert.assertTrue(set.contains(word));
        }
    }

    private void check1Distance(CharacterGraphDecoder spellChecker, String expected) {
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

    @Test
    public void nearKeyCheck() {
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(
                1,
                CharacterGraphDecoder.TURKISH_Q_NEAR_KEY_MAP);
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

    @Test
    public void stemEndingTest1() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.builder().addDictionaryLines("bakmak", "gelmek").build();
        List<String> endings = Lists.newArrayList("acak", "ecek");
        StemEndingGraph graph = new StemEndingGraph(morphology, endings);
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(graph.stemGraph);
        List<String> res = spellChecker.getSuggestions("bakcaak");
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("bakacak", res.get(0));
    }

    @Test
    public void stemEndingTest2() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.builder().addDictionaryLines("üzmek", "yüz", "güz").build();
        List<String> endings = Lists.newArrayList("düm");
        StemEndingGraph graph = new StemEndingGraph(morphology, endings);
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(graph.stemGraph);
        List<ScoredItem<String>> res = spellChecker.getSuggestionsWithScores("yüzdüm");
        Assert.assertEquals(3, res.size());
        assertContainsAll(res, "yüzdüm", "üzdüm", "güzdüm");
    }

    @Test
    public void stemEndingTest3() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.builder().addDictionaryLines("o", "ol", "ola").build();
        List<String> endings = Lists.newArrayList("arak", "acak");
        StemEndingGraph graph = new StemEndingGraph(morphology, endings);
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(graph.stemGraph);
        List<ScoredItem<String>> res = spellChecker.getSuggestionsWithScores("olarak");
        assertContainsAll(res, "olarak", "olacak", "olaarak");
    }

    @Test
    public void stemEndingTest() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.builder().addDictionaryLines("Türkiye", "Bayram").build();
        List<String> endings = Lists.newArrayList("ında", "de");
        StemEndingGraph graph = new StemEndingGraph(morphology, endings);
        CharacterGraphDecoder spellChecker = new CharacterGraphDecoder(graph.stemGraph);
        List<ScoredItem<String>> res = spellChecker.getSuggestionsWithScores("türkiyede");
        assertContainsAll(res, "türkiyede");
    }
}
