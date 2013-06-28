package zemberek.lm;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

public class LmVocabularyTest {

    @Test
    public void emptyVocabularyTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary();
        Assert.assertTrue(vocabulary.size() == 0);
        Assert.assertEquals(
                LmVocabulary.UNKNOWN_WORD + " " + LmVocabulary.UNKNOWN_WORD,
                vocabulary.getWordsString(0, 0));
    }

    @Test
    public void arrayConstructorTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("Hello", "World");
        simpleCheck(vocabulary);
    }

    @Test
    public void specialWordsTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("<S>", "Hello", "</S>");
        Assert.assertEquals(0, vocabulary.getSentenceStartIndex());
        Assert.assertEquals(2, vocabulary.getSentenceEndIndex());
        Assert.assertEquals(-1, vocabulary.getUnknownWordIndex());

        vocabulary = new LmVocabulary("<s>", "Hello", "</s>");
        Assert.assertEquals(0, vocabulary.getSentenceStartIndex());
        Assert.assertEquals(2, vocabulary.getSentenceEndIndex());
        Assert.assertEquals(-1, vocabulary.getUnknownWordIndex());


        vocabulary = new LmVocabulary("<s>", "<S>", "Hello", "</s>");
        Assert.assertEquals(0, vocabulary.getSentenceStartIndex());
        Assert.assertEquals(3, vocabulary.getSentenceEndIndex());
        Assert.assertEquals(-1, vocabulary.getUnknownWordIndex());
    }


    @Test
    public void binaryFileGenerationTest() throws IOException {
        File tmp = getBinaryVocFile();
        LmVocabulary vocabulary = LmVocabulary.loadFromBinary(tmp);
        simpleCheck(vocabulary);
    }

    @Test
    public void utf8FileGenerationTest() throws IOException {
        File tmp = getUtf8VocFile();
        LmVocabulary vocabulary = LmVocabulary.loadFromUtf8File(tmp);
        simpleCheck(vocabulary);
    }

    @Test
    public void streamGenerationTest() throws IOException {
        File tmp = getBinaryVocFile();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(tmp))) {
            LmVocabulary vocabulary = LmVocabulary.loadFromDataInputStream(dis);
            simpleCheck(vocabulary);
        }
    }

    @Test
    public void randomAccessGenerationTest() throws IOException {
        File tmp = getBinaryVocFile();
        try (RandomAccessFile raf = new RandomAccessFile(tmp, "r")) {
            LmVocabulary vocabulary = LmVocabulary.loadFromRandomAcessFile(raf);
            simpleCheck(vocabulary);
        }
    }

    private void simpleCheck(LmVocabulary vocabulary) {
        Assert.assertTrue(vocabulary.size() == 2);
        Assert.assertEquals("Hello World", vocabulary.getWordsString(0, 1));
        Assert.assertEquals("Hello " + LmVocabulary.UNKNOWN_WORD, vocabulary.getWordsString(0, 2));
        Assert.assertEquals(0, vocabulary.indexOf("Hello"));
        Assert.assertEquals(-1, vocabulary.indexOf("Foo"));
    }

    private File getBinaryVocFile() throws IOException {
        File tmp = File.createTempFile("voc_test", "foo");
        tmp.deleteOnExit();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(tmp))) {
            dos.writeInt(2);
            dos.writeUTF("Hello");
            dos.writeUTF("World");
        }
        return tmp;
    }

    private File getUtf8VocFile() throws IOException {
        File tmp = File.createTempFile("utf8_voc_test", "foo");
        tmp.deleteOnExit();
        Files.write(String.format("Hello%n%n      %n\t%nWorld"), tmp, Charsets.UTF_8);
        return tmp;
    }

    @Test
    public void collectionConstructorTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary(Lists.newArrayList("Hello", "World"));
        simpleCheck(vocabulary);
    }

    @Test
    public void contains() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("Hello", "World");
        Assert.assertTrue(vocabulary.contains(0));
        Assert.assertTrue(vocabulary.contains(1));
        Assert.assertFalse(vocabulary.contains(2));
        Assert.assertTrue(vocabulary.containsAll(0, 1));
        Assert.assertFalse(vocabulary.containsAll(0, 2));

        Assert.assertTrue(vocabulary.contains("Hello"));
        Assert.assertTrue(vocabulary.contains("World"));
        Assert.assertFalse(vocabulary.contains("Foo"));
        Assert.assertFalse(vocabulary.containsAll("Hello", "Foo"));
        Assert.assertTrue(vocabulary.containsAll("Hello", "World"));
    }

    @Test
    public void encodedTrigramTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("a", "b", "c", "d", "e");
        long k = ((1l << 21 | 2l) << 21) | 3l;
        Assert.assertEquals(k, vocabulary.encodeTrigram(3, 2, 1));
        Assert.assertEquals(k, vocabulary.encodeTrigram(3, 2, 1));
    }

    @Test
    public void toWordsTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("a", "b", "c", "d", "e");
        Assert.assertArrayEquals(new String[]{"a", "e", "b"}, vocabulary.toWords(0, 4, 1));
        Assert.assertArrayEquals(new String[]{"a", LmVocabulary.UNKNOWN_WORD, "b"}, vocabulary.toWords(0, 5, 1));
    }

    @Test
    public void toIndexTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("a", "b", "c", "d", "e");
        Assert.assertArrayEquals(new int[]{0, 4, 1}, vocabulary.toIndexes("a", "e", "b"));
        Assert.assertArrayEquals(new int[]{0, -1, 1}, vocabulary.toIndexes("a", "foo", "b"));
    }

}
