package zemberek.lm;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.Locale;

public class LmVocabularyTest {

    @Test
    public void emptyVocabularyTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary();
        Assert.assertTrue(vocabulary.size() == 3);
        Assert.assertEquals(
                vocabulary.getUnknownWord() + " " + vocabulary.getUnknownWord(),
                vocabulary.getWordsString(-1, -1));
    }

    @Test
    public void arrayConstructorTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("Hello", "World");
        simpleCheck(vocabulary);
    }

    @Test
    public void specialWordsTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("<S>", "Hello", "</S>");
        vocabulary.containsAll("<S>", "Hello", "</S>", "<unk>");
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
            LmVocabulary vocabulary = LmVocabulary.loadFromRandomAccessFile(raf);
            simpleCheck(vocabulary);
        }
    }

    private void simpleCheck(LmVocabulary vocabulary) {
        Assert.assertTrue(vocabulary.size() == 5);
        Assert.assertEquals("Hello World",
                vocabulary.getWordsString(vocabulary.toIndexes("Hello", "World")));
        Assert.assertEquals("Hello " + vocabulary.getUnknownWord(),
                vocabulary.getWordsString(vocabulary.toIndexes("Hello", vocabulary.getUnknownWord())));
        Assert.assertTrue(vocabulary.contains("Hello"));
        Assert.assertEquals(vocabulary.getUnknownWordIndex(), vocabulary.indexOf("Foo"));
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

        int helloIndex = vocabulary.indexOf("Hello");
        int worldIndex = vocabulary.indexOf("World");
        Assert.assertTrue(vocabulary.contains(helloIndex));
        Assert.assertTrue(vocabulary.contains(worldIndex));

        int unkIndex = vocabulary.indexOf("Foo");
        Assert.assertEquals(vocabulary.getUnknownWordIndex(), unkIndex);

        Assert.assertTrue(vocabulary.containsAll(helloIndex, worldIndex));
        Assert.assertFalse(vocabulary.containsAll(-1, 2));

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
        int[] indexes = vocabulary.toIndexes("a", "e", "b");
        Assert.assertEquals("a e b", Joiner.on(" ").join(vocabulary.toWords(indexes)));
        indexes = vocabulary.toIndexes("a", "e", "foo");
        Assert.assertEquals("a e <unk>", Joiner.on(" ").join(vocabulary.toWords(indexes)));
    }

    @Test
    public void toIndexTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("a", "b", "c", "d", "e");

        int[] indexes = {vocabulary.indexOf("a"), vocabulary.indexOf("e"), vocabulary.indexOf("b")};
        Assert.assertArrayEquals(indexes, vocabulary.toIndexes("a", "e", "b"));

        int[] indexes2 = {vocabulary.indexOf("a"), vocabulary.indexOf("<unk>"), vocabulary.indexOf("b")};
        Assert.assertArrayEquals(indexes2, vocabulary.toIndexes("a", "foo", "b"));
    }

    @Test
    public void builderTest() throws IOException {
        LmVocabulary.Builder builder = LmVocabulary.builder();
        String[] words = {"elma", "çilek", "karpuz", "armut", "elma", "armut"};
        for (String word : words) {
            builder.add(word);
        }
        Assert.assertEquals(4, builder.size());
        Assert.assertEquals(0, builder.indexOf("elma"));
        Assert.assertEquals(1, builder.indexOf("çilek"));
        Assert.assertEquals(2, builder.indexOf("karpuz"));
        Assert.assertEquals(-1, builder.indexOf("mango"));

        List<Integer> list = Lists.newArrayList(builder.alphabeticallySortedWordsIds());
        Assert.assertEquals(Lists.newArrayList(3, 0, 2, 1), list);

        list = Lists.newArrayList(builder.alphabeticallySortedWordsIds(new Locale("tr")));
        Assert.assertEquals(Lists.newArrayList(3, 1, 0, 2), list);

        LmVocabulary vocab = builder.generate();
        Assert.assertEquals(7, vocab.size());
    }

    @Test
    public void containsAffixesTest() throws IOException {
        LmVocabulary vocabulary = new LmVocabulary("a", "_b", "c", "d", "e");
        Assert.assertTrue(vocabulary.containsSuffix());
        vocabulary = new LmVocabulary("a", "-b", "c", "d", "e");
        Assert.assertTrue(vocabulary.containsSuffix());
        vocabulary = new LmVocabulary("a-", "-b", "c", "d", "e");
        Assert.assertTrue(vocabulary.containsPrefix());
        vocabulary = new LmVocabulary("a_", "b", "c", "d", "e");
        Assert.assertTrue(vocabulary.containsPrefix());
        vocabulary = new LmVocabulary("a", "b", "c", "d", "e");
        Assert.assertFalse(vocabulary.containsSuffix());
        Assert.assertFalse(vocabulary.containsPrefix());
    }


}
