package zemberek.lm;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

public class LmVocabularyTest {

    @Test
    public void emptyVocabularyTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary();
        Assert.assertTrue(vocabulary.size() == 0);
        Assert.assertEquals(
                LmVocabulary.OUT_OF_VOCABULARY + " " + LmVocabulary.OUT_OF_VOCABULARY,
                vocabulary.getWordsString(0, 0));
    }

    @Test
    public void arrayConstructorTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("Hello", "World");
        simpleCheck(vocabulary);
    }

    @Test
    public void fileConstructorTest() throws IOException {
        File tmp = getVocFile();
        LmVocabulary vocabulary = new LmVocabulary(tmp);
        simpleCheck(vocabulary);
    }

    @Test
    public void streamConstructorTest() throws IOException {
        File tmp = getVocFile();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(tmp))) {
            LmVocabulary vocabulary = new LmVocabulary(dis);
            simpleCheck(vocabulary);
        }
    }

    @Test
    public void randomAccessConstructorTest() throws IOException {
        File tmp = getVocFile();
        try (RandomAccessFile raf = new RandomAccessFile(tmp, "r")) {
            LmVocabulary vocabulary = new LmVocabulary(raf);
            simpleCheck(vocabulary);
        }
    }

    private void simpleCheck(LmVocabulary vocabulary) {
        Assert.assertTrue(vocabulary.size() == 2);
        Assert.assertEquals("Hello World", vocabulary.getWordsString(0, 1));
        Assert.assertEquals("Hello " + LmVocabulary.OUT_OF_VOCABULARY, vocabulary.getWordsString(0, 2));
        Assert.assertEquals(0, vocabulary.indexOf("Hello"));
        Assert.assertEquals(-1, vocabulary.indexOf("Foo"));
    }

    private File getVocFile() throws IOException {
        File tmp = File.createTempFile("voc_test", "foo");
        tmp.deleteOnExit();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(tmp))) {
            dos.writeInt(2);
            dos.writeUTF("Hello");
            dos.writeUTF("World");
        }
        return tmp;
    }

    @Test
    public void collectionConstructorTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary(Lists.newArrayList("Hello", "World"));
        simpleCheck(vocabulary);
    }

    @Test(expected = IllegalStateException.class)
    public void doubleConstructorTest() throws IOException {
        new LmVocabulary("Hello", "World", "Hello");
    }

    @Test
    public void contains() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("Hello", "World");
        Assert.assertTrue(vocabulary.contains(0));
        Assert.assertTrue(vocabulary.contains(1));
        Assert.assertFalse(vocabulary.contains(2));
        Assert.assertTrue(vocabulary.containsAll(0, 1));
        Assert.assertFalse(vocabulary.containsAll(0, 2));
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
        Assert.assertArrayEquals(new String[]{"a", "e", "b"}, vocabulary.getWords(0, 4, 2));
        Assert.assertArrayEquals(new String[]{"a", LmVocabulary.OUT_OF_VOCABULARY, "b"}, vocabulary.getWords(0, 4, 5));
    }

    @Test
    public void toIndexTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("a", "b", "c", "d", "e");
        Assert.assertArrayEquals(new int[]{0,4,1}, vocabulary.indexOf("a", "e", "b"));
        Assert.assertArrayEquals(new int[]{0,-1,1}, vocabulary.indexOf("a", "foo", "b"));
    }

}
