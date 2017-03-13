package zemberek.normalization;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.lm.NgramLanguageModel;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
    @Ignore("Slow test. Uses actual data.")
    public void suggestWordTest() throws IOException, URISyntaxException {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

        Log.info("Node count = %d", spellChecker.decoder.getGraph().getAllNodes().size());
        Log.info("Node count with single connection= %d",
                spellChecker.decoder.getGraph().getAllNodes(a->a.getAllChildNodes().size()==1).size());

        Path lmPath = Paths.get(ClassLoader.getSystemResource("lm-unigram.slm").toURI());
        NgramLanguageModel lm = SmoothLm.builder(lmPath.toFile()).build();

        Path r = Paths.get(ClassLoader.getSystemResource("10000_frequent_turkish_word").toURI());

        List<String> words = Files.readAllLines(r, StandardCharsets.UTF_8);
        int c = 0;
        Stopwatch sw = Stopwatch.createStarted();
        for (String word : words) {
            List<String> suggestions = spellChecker.suggestForWord(word, lm);
            c += suggestions.size();
        }
        Log.info("Elapsed = %d count = %d ", sw.elapsed(TimeUnit.MILLISECONDS), c);

    }
}
