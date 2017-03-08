package zemberek.normalization;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.IOException;

public class TurkishSpellCheckerTest {

    @Test
    public void checkProperNouns() throws IOException {
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

    //TODO: check for ordinals, percentages etc.
    @Test
    public void formatNumbers() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.builder().build();

        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

        String[] inputs = {"1'e", "4'ten", "123'ü", "12,5'ten"};

        for (String input : inputs) {
            Assert.assertTrue("Fail at " + input, spellChecker.check(input));
        }
    }


}
