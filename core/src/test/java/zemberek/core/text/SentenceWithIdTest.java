package zemberek.core.text;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SentenceWithIdTest {

    @Test
    public void testLoadFromFile() throws IOException {
        Path temp = TextUtil.createTempFile("123 foo bar", "456 baz zip");
        checkSentences(temp);
    }

    @Test
    public void testLoadFromFileSphinxStyle() throws IOException {
        Path temp = TextUtil.createTempFile("foo bar (123)", "baz zip (456)");
        checkSentences(temp);
    }

    private void checkSentences(Path twoSentenceFile) throws IOException {
        List<SentenceWithId>  sentences = SentenceWithId.fromPath(twoSentenceFile);
        Assert.assertEquals(2, sentences.size());
        SentenceWithId first = sentences.get(0);
        Assert.assertEquals("123", first.id);
        Assert.assertEquals(2, first.getSentence().size());
        Assert.assertArrayEquals(new String[] {"foo","bar"}, first.wordArray());
        Assert.assertEquals("foo bar", first.sentenceAsString());

        SentenceWithId second = sentences.get(1);
        Assert.assertEquals("456", second.id);
        Assert.assertEquals(2, second.getSentence().size());
        Assert.assertArrayEquals(new String[] {"baz","zip"}, second.wordArray());
        Assert.assertEquals("baz zip", second.sentenceAsString());
    }
}
