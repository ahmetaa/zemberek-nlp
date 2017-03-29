package zemberek.tokenization;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TurkishSentenceExtractorTest {

    static List<String> getSentences(String pipeDelimited) {
        return Splitter.on('|').trimResults().splitToList(pipeDelimited);
    }

    @Test
    public void singletonAccessShouldNotThrowException() throws IOException {
        TurkishSentenceExtractor.DEFAULT.fromParagraph("hello");
    }

    @Test
    public void shouldExtractSentences1() throws IOException {
        String test = "Merhaba Dünya.| Nasılsın?";
        List<String> expected = getSentences(test);

        Assert.assertEquals(expected, TurkishSentenceExtractor.DEFAULT.fromParagraph(test.replace("|", "")));
    }

    @Test
    public void shouldExtractSingleSentences() throws IOException {
        String test = "Merhaba Dünya.";
        List<String> expected = getSentences(test);

        Assert.assertEquals(expected, TurkishSentenceExtractor.DEFAULT.fromParagraph(test.replace("|", "")));
    }

    @Test
    public void shouldExtractSentencesSecondDoesNotEndWithDot() throws IOException {
        String test = "Merhaba Dünya.| Nasılsın";
        List<String> expected = getSentences(test);

        Assert.assertEquals(expected, TurkishSentenceExtractor.DEFAULT.fromParagraph(test.replace("|", "")));
    }

    @Test
    public void shouldReturnDotForDot() throws IOException {
        List<String> expected = getSentences(".");
        Assert.assertEquals(expected, TurkishSentenceExtractor.DEFAULT.fromParagraph("."));
    }

    @Test
    public void shouldReturn0ForEmpty() throws IOException {
        Assert.assertEquals(0, TurkishSentenceExtractor.DEFAULT.fromParagraph("").size());
    }

    @Test
    public void extractFromDocument() throws IOException {
        Assert.assertEquals("Merhaba!|Bugün 2. köprü Fsm.'de trafik vardı.|değil mi?",
                markBoundariesDocument("Merhaba!\n Bugün 2. köprü Fsm.'de trafik vardı.değil mi?\n"));
        Assert.assertEquals("Ali|gel.",
                markBoundariesDocument("Ali\n\n\rgel.\n"));
        Assert.assertEquals("Ali gel.|Merhaba|Ne haber?",
                markBoundariesDocument("\n\nAli gel. Merhaba\n\rNe haber?"));

    }

    private String markBoundariesDocument(String input) throws IOException {
        List<String> list = TurkishSentenceExtractor.DEFAULT.fromDocument(input);
        return Joiner.on("|").join(list);
    }


    private String markBoundariesParagraph(String input) throws IOException {
        List<String> list = TurkishSentenceExtractor.DEFAULT.fromParagraph(input);
        return Joiner.on("|").join(list);
    }

    @Test
    public void testSimpleSentence() throws IOException {
        Assert.assertEquals("Merhaba!|Bugün 2. köprü Fsm.'de trafik vardı.|değil mi?",
                markBoundariesParagraph("Merhaba! Bugün 2. köprü Fsm.'de trafik vardı.değil mi?"));
        Assert.assertEquals("Prof. Dr. Veli Zambur %2.5 lik enflasyon oranini begenmemis!",
                markBoundariesParagraph("Prof. Dr. Veli Zambur %2.5 lik enflasyon oranini begenmemis!"));
        Assert.assertEquals("Ali gel.",
                markBoundariesParagraph("Ali gel."));
        Assert.assertEquals("Ali gel.|Okul acildi!",
                markBoundariesParagraph("Ali gel. Okul acildi!"));
        Assert.assertEquals("Ali gel.|Okul acildi!",
                markBoundariesParagraph("Ali gel. Okul acildi!"));
        Assert.assertEquals("Ali gel...|Okul acildi.",
                markBoundariesParagraph("Ali gel... Okul acildi."));
        Assert.assertEquals("Tam 1.000.000 papeli cebe atmislar...",
                markBoundariesParagraph("Tam 1.000.000 papeli cebe atmislar..."));
        Assert.assertEquals("16. yüzyılda?|Dr. Av. Blah'a gitmiş.",
                markBoundariesParagraph("16. yüzyılda? Dr. Av. Blah'a gitmiş."));
        Assert.assertEquals("Ali gel.|Okul açıldı...|sınavda 2. oldum.",
                markBoundariesParagraph("Ali gel. Okul açıldı... sınavda 2. oldum."));
    }
}
