package zemberek.tokenization;

import com.google.common.base.Splitter;
import org.junit.Assert;
import org.junit.Test;
import zemberek.tokenizer.PerceptronSentenceExtractor;
import zemberek.tokenizer.SentenceExtractor;

import java.io.IOException;
import java.util.List;

public class PerceptronSentenceExtractorTest {

    static List<String> getSentences(String pipeDelimited) {
        return Splitter.on('|').trimResults().splitToList(pipeDelimited);
    }

    @Test
    public void initializationShouldNotThrowException() throws IOException {
        SentenceExtractor extractor = PerceptronSentenceExtractor.loadFromResources();
    }

    @Test
    public void shouldExtractSentences1() throws IOException {
        String test = "Merhaba Dünya.| Nasılsın?";
        List<String> expected = getSentences(test);

        SentenceExtractor extractor = PerceptronSentenceExtractor.loadFromResources();
        Assert.assertEquals(expected, extractor.extract(test.replace("|", "")));
    }

    @Test
    public void shouldExtractSingleSentences() throws IOException {
        String test = "Merhaba Dünya.";
        List<String> expected = getSentences(test);

        SentenceExtractor extractor = PerceptronSentenceExtractor.loadFromResources();
        Assert.assertEquals(expected, extractor.extract(test.replace("|", "")));
    }

    @Test
    public void shouldExtractSentencesSecondDoesNotendWithDot() throws IOException {
        String test = "Merhaba Dünya.| Nasılsın";
        List<String> expected = getSentences(test);

        SentenceExtractor extractor = PerceptronSentenceExtractor.loadFromResources();
        Assert.assertEquals(expected, extractor.extract(test.replace("|", "")));
    }

    @Test
    public void shouldReturnDotForDot() throws IOException {
        List<String> expected = getSentences(".");
        SentenceExtractor extractor = PerceptronSentenceExtractor.loadFromResources();
        Assert.assertEquals(expected, extractor.extract("."));
    }


}
