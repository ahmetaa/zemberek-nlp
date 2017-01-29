package zemberek.tokenizer;

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
    public void initializationShouldNotThrowException() throws IOException {
        SentenceExtractor extractor = TurkishSentenceExtractor.fromInternalModel();
    }

    @Test
    public void shouldExtractSentences1() throws IOException {
        String test = "Merhaba Dünya.| Nasılsın?";
        List<String> expected = getSentences(test);

        SentenceExtractor extractor = TurkishSentenceExtractor.fromInternalModel();
        Assert.assertEquals(expected, extractor.extract(test.replace("|", "")));
    }

    @Test
    public void shouldExtractSingleSentences() throws IOException {
        String test = "Merhaba Dünya.";
        List<String> expected = getSentences(test);

        SentenceExtractor extractor = TurkishSentenceExtractor.fromInternalModel();
        Assert.assertEquals(expected, extractor.extract(test.replace("|", "")));
    }

    @Test
    public void shouldExtractSentencesSecondDoesNotendWithDot() throws IOException {
        String test = "Merhaba Dünya.| Nasılsın";
        List<String> expected = getSentences(test);

        SentenceExtractor extractor = TurkishSentenceExtractor.fromInternalModel();
        Assert.assertEquals(expected, extractor.extract(test.replace("|", "")));
    }

    @Test
    public void shouldReturnDotForDot() throws IOException {
        List<String> expected = getSentences(".");
        SentenceExtractor extractor = TurkishSentenceExtractor.fromInternalModel();
        Assert.assertEquals(expected, extractor.extract("."));
    }

    @Test
    public void shouldReturn0ForEmpty() throws IOException {
        SentenceExtractor extractor = TurkishSentenceExtractor.fromInternalModel();
        Assert.assertEquals(0, extractor.extract("").size());
    }

    private String markBoundaries(String input) throws IOException {
        SentenceExtractor extractor = TurkishSentenceExtractor.fromInternalModel();
        List<String> list = extractor.extract(input);
        return Joiner.on("|").join(list);
    }

    @Test
    public void testSimpleSentence() throws IOException {
        Assert.assertEquals("Merhaba!|Bugün 2. köprü Fsm.'de trafik vardı.|değil mi?",
                markBoundaries("Merhaba! Bugün 2. köprü Fsm.'de trafik vardı.değil mi?"));
        Assert.assertEquals("Prof. Dr. Veli Zambur %2.5 lik enflasyon oranini begenmemis!",
                markBoundaries("Prof. Dr. Veli Zambur %2.5 lik enflasyon oranini begenmemis!"));
        Assert.assertEquals("Ali gel.",
                markBoundaries("Ali gel."));
        Assert.assertEquals("Ali gel.|Okul acildi!",
                markBoundaries("Ali gel. Okul acildi!"));
        Assert.assertEquals("Ali gel.|Okul acildi!",
                markBoundaries("Ali gel. Okul acildi!"));
        Assert.assertEquals("Ali gel...|Okul acildi.",
                markBoundaries("Ali gel... Okul acildi."));
        Assert.assertEquals("Tam 1.000.000 papeli cebe atmislar...",
                markBoundaries("Tam 1.000.000 papeli cebe atmislar..."));
        Assert.assertEquals("16. yüzyılda?|Dr. Av. Blah'a gitmiş.",
                markBoundaries("16. yüzyılda? Dr. Av. Blah'a gitmiş."));
        Assert.assertEquals("Ali gel.|Okul açıldı...|sınavda 2. oldum.",
                markBoundaries("Ali gel. Okul açıldı... sınavda 2. oldum."));
    }
}
