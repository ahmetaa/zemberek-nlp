package zemberek.normalization;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.lm.NgramLanguageModel;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TurkishSpellCheckerTest {

    @Test
    public void checkProperNounsTest() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.builder()
                .addDictionaryLines("Ankara", "Iphone [Pr:ayfon]", "Google [Pr:gugıl]").build();
        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

        String[] correct = {"Ankara", "ANKARA", "Ankara'da", "ANKARA'DA", "ANKARA'da",
                "Iphone'umun", "Google'dan", "Iphone", "Google", "Google'sa"};

        for (String input : correct) {
            Assert.assertTrue("Fail at " + input, spellChecker.check(input));
        }

        String[] fail = {"Ankara'", "ankara", "AnKARA", "Ankarada", "ankara'DA", "-Ankara"};
        for (String input : fail) {
            Assert.assertFalse("Fail at " + input, spellChecker.check(input));
        }
    }

    //TODO: check for ordinals.
    @Test
    public void formatNumbersTest() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.builder()
                .addDictionaryLines("bir [P:Num]", "dört [P:Num;A:Voicing]", "üç [P:Num]", "beş [P:Num]").build();

        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

        String[] inputs = {
                "1'e", "4'ten", "123'ü", "12,5'ten",
                "1'E", "4'TEN", "123'Ü", "12,5'TEN",
                "%1", "%1'i", "%1,3'ü",
        };

        for (String input : inputs) {
            Assert.assertTrue("Fail at " + input, spellChecker.check(input));
        }
    }

    @Test
    public void apostropheTest() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.builder()
                .addDictionaryLines("zaman [P:Noun, Time]").build();

        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

        String[] inputs = {
                "zamanda"
        };

        for (String input : inputs) {
            Assert.assertTrue("Fail at " + input, spellChecker.check(input));
        }
    }

    @Test
    @Ignore("Slow. Uses actual data.")
    public void suggestWordPerformanceStemEnding() throws Exception {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);
        NgramLanguageModel lm = getLm("lm-unigram.slm");
        run(spellChecker, lm);
    }

    @Test
    @Ignore("Slow. Uses actual data.")
    public void suggestWord1() throws Exception {
        TurkishMorphology morphology = TurkishMorphology.builder().addDictionaryLines("Türkiye", "Bayram").build();
        List<String> endings = Lists.newArrayList("ında", "de");
        StemEndingGraph graph = new StemEndingGraph(morphology, endings);
        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology, graph.stemGraph);
        NgramLanguageModel lm = getLm("lm-unigram.slm");
        check(spellChecker, lm,"Türkiye'de", "Türkiye'de");
        // TODO: "Bayramı'nda" fails.
    }

    private void check(TurkishSpellChecker spellChecker, NgramLanguageModel lm, String input, String expected) throws Exception {
        List<String> res = spellChecker.suggestForWord(input, lm);
        Assert.assertTrue(res.contains(expected));
    }

    @Test
    @Ignore("Slow. Uses actual data.")
    public void suggestWordPerformanceWord() throws Exception {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        CharacterGraph graph = new CharacterGraph();
        Path r = Paths.get(ClassLoader.getSystemResource("zemberek-parsed-words-min10.txt").toURI());
        List<String> words = Files.readAllLines(r, StandardCharsets.UTF_8);
        words.forEach(s -> graph.addWord(s, Node.TYPE_WORD));
        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology, graph);
        NgramLanguageModel lm = getLm("lm-unigram.slm");
        run(spellChecker, lm);
    }

    private void run(TurkishSpellChecker spellChecker, NgramLanguageModel lm) throws Exception {
        Log.info("Node count = %d", spellChecker.decoder.getGraph().getAllNodes().size());
        Log.info("Node count with single connection= %d",
                spellChecker.decoder.getGraph().getAllNodes(a -> a.getAllChildNodes().size() == 1).size());

        URI uri = ClassLoader.getSystemResource("10000_frequent_turkish_word").toURI();
        Path r = Paths.get(uri);

        List<String> words = Files.readAllLines(r, StandardCharsets.UTF_8);
        int c = 0;
        Stopwatch sw = Stopwatch.createStarted();
        for (String word : words) {
            List<String> suggestions = spellChecker.suggestForWord(word, lm);
            c += suggestions.size();
        }
        Log.info("Elapsed = %d count = %d ", sw.elapsed(TimeUnit.MILLISECONDS), c);
    }

    private NgramLanguageModel getLm(String resource) throws Exception {
        Path lmPath = Paths.get(ClassLoader.getSystemResource(resource).toURI());
        return SmoothLm.builder(lmPath.toFile()).build();
    }

    @Test
    @Ignore("Slow. Uses actual data.")
    public void runSentence() throws Exception {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);
        NgramLanguageModel lm = getLm("lm-bigram.slm");
        Path testInput = Paths.get(ClassLoader.getSystemResource("spell-checker-test-small.txt").toURI());
        List<String> sentences = Files.readAllLines(testInput, StandardCharsets.UTF_8);
        try (PrintWriter pw = new PrintWriter("bigram-test-result.txt")) {
            for (String sentence : sentences) {
                pw.println(sentence);
                List<String> input = TurkishSpellChecker.tokenizeForSpelling(sentence);
                for (int i = 0; i < input.size(); i++) {
                    String left = i == 0 ? null : input.get(i - 1);
                    String right = i == input.size() - 1 ? null : input.get(i + 1);
                    String word = input.get(i);
                    String deformed = applyDeformation(word);
                    List<String> res = spellChecker.suggestForWord(deformed, left, right, lm);
                    pw.println(String.format("%s %s[%s] %s -> %s", left, deformed, word, right, res.toString()));
                }
                pw.println();
            }
        }
    }

    private static Random random = new Random(1);

    private static String applyDeformation(String word) {
        if (word.length() < 3) {
            return word;
        }
        int deformation = random.nextInt(4);
        StringBuilder sb = new StringBuilder(word);
        switch (deformation) {
            case 0: // substitution
                int start = random.nextInt(sb.length());
                sb.replace(start, start + 1, "x");
                return sb.toString();
            case 1: // insertion
                sb.insert(random.nextInt(sb.length() + 1), "x");
                return sb.toString();
            case 2: // deletion
                sb.deleteCharAt(random.nextInt(sb.length()));
                return sb.toString();
            case 3: // transposition
                int i = random.nextInt(sb.length() - 2);
                char tmp = sb.charAt(i);
                sb.setCharAt(i, sb.charAt(i + 1));
                sb.setCharAt(i + 1, tmp);
                return sb.toString();
        }
        return word;
    }
}
