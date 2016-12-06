package zemberek.lm.compression;

import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.SpaceTabTokenizer;
import zemberek.core.io.LineIterator;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.logging.Log;
import zemberek.lm.BaseLanguageModel;
import zemberek.lm.FakeLm;
import zemberek.lm.LmVocabulary;
import zemberek.lm.backoff.SimpleBackoffNgramModel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class SmoothLmTest {

    @Test
    public void testGeneration() throws IOException {
        SmoothLm lm = getTinyLm();
        Assert.assertEquals(3, lm.getOrder());
    }

    private SmoothLm getTinyLm() throws IOException {
        return SmoothLm.builder(getTinyLmFile()).build();
    }

    URL TINY_ARPA_URL = Resources.getResource("tiny.arpa");

    File getTinyArpaFile() throws IOException {
        File tmp = File.createTempFile("tiny", ".arpa");
        Log.info("Temporary test Arpa model file %s", tmp);
        Files.copy(new File(TINY_ARPA_URL.getFile()), tmp);
        return tmp;
    }

    private File getTinyLmFile() throws IOException {
        File tmp = Files.createTempDir();
        tmp.deleteOnExit();
        final File lmFile = new File(tmp, "tiny.slm");
        Log.info("Temporary test compressed model file %s", lmFile);
        if (!lmFile.exists()) {
            UncompressedToSmoothLmConverter converter = new UncompressedToSmoothLmConverter(lmFile, tmp);
            converter.convertSmall(
                    MultiFileUncompressedLm.generate(getTinyArpaFile(), tmp, "utf-8",4).getLmDir(),
                    new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));
        }
        return lmFile;
    }

    @Test
    public void testNgramKeyExactMatch() throws IOException {
        File lmDir = Files.createTempDir();
        lmDir.deleteOnExit();
        MultiFileUncompressedLm.generate(getTinyArpaFile(), lmDir, "utf-8",4).getLmDir();
        final File lmFile = new File(lmDir, "tiny.slm");
        UncompressedToSmoothLmConverter converter = new UncompressedToSmoothLmConverter(lmFile, lmDir);
        converter.convertSmall(
                lmDir,
                new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));
        SmoothLm slm = SmoothLm.builder(lmFile).ngramKeyFilesDirectory(lmDir).build();
        SimpleBackoffNgramModel model = SimpleBackoffNgramModel.fromArpa(getTinyArpaFile());
        Iterator<SimpleBackoffNgramModel.NgramData> it = model.getAllIndexes();
        while (it.hasNext()) {
            Assert.assertTrue(slm.ngramExists(it.next().getIndexes()));
        }
    }

    @Test
    @Ignore("Requires external data")
    public void testBigFakeLm() throws IOException {
        int order = 4;
        final File lmFile = new File("/media/ahmetaa/depo/data/lm/fake/fake.slm");
        if (!lmFile.exists()) {
            final File arpaFile = new File("/media/ahmetaa/depo/data/lm/fake/fake.arpa");
            File tmp = new File("/tmp");
            if (!arpaFile.exists()) {
                FakeLm fakeLm = new FakeLm(order);
                fakeLm.generateArpa(arpaFile);
            }
            UncompressedToSmoothLmConverter converter = new UncompressedToSmoothLmConverter(lmFile, tmp);
            converter.convertSmall(
                    MultiFileUncompressedLm.generate(arpaFile, tmp, "utf-8",4).dir,
                    new UncompressedToSmoothLmConverter.NgramDataBlock(24, 24, 24));
        }
        SmoothLm lm = SmoothLm.builder(lmFile).build();

        FakeLm fakeLm = new FakeLm(order);
        for (int i = 1; i <= fakeLm.order; i++) {
            FakeLm.FakeGram[] probs = fakeLm.getNgramProbs(i);
            for (FakeLm.FakeGram prob : probs) {
                Assert.assertEquals("ouch:" + Arrays.toString(prob.vals), prob.prob, lm.getProbability(prob.indexes), 0.001);
            }
        }
    }

    @Test
    public void testVocabulary() throws IOException {
        SmoothLm lm = getTinyLm();
        LmVocabulary vocab = lm.getVocabulary();
        Assert.assertTrue(vocab.contains("Ahmet"));
        int i1 = vocab.indexOf("Ahmet");
        Assert.assertTrue(vocab.contains("elma"));
        int i2 = vocab.indexOf("elma");
        Assert.assertTrue(i1 != i2);
        Assert.assertEquals("Ahmet", vocab.getWord(i1));
        Assert.assertEquals("elma", vocab.getWord(i2));
    }

    @Test
    public void testProbabilities() throws IOException {
        SmoothLm lm = getTinyLm();
        System.out.println(lm.info());
        LmVocabulary vocabulary = lm.getVocabulary();
        int[] is = {vocabulary.indexOf("<s>")};
        Assert.assertEquals(-1.716003, lm.getProbabilityValue(is), 0.0001);
        Assert.assertEquals(-1.716003, lm.getProbability(is), 0.0001);
        //<s> kedi
        int[] is2 = {vocabulary.indexOf("<s>"), vocabulary.indexOf("kedi")};
        Assert.assertEquals(-0.796249, lm.getProbabilityValue(is2), 0.0001);
        Assert.assertEquals(-0.796249, lm.getProbability(is2), 0.0001);
        //Ahmet dondurma yedi
        int[] is3 = {vocabulary.indexOf("Ahmet"), vocabulary.indexOf("dondurma"), vocabulary.indexOf("yedi")};
        Assert.assertEquals(-0.602060, lm.getProbabilityValue(is3), 0.0001);
        Assert.assertEquals(-0.602060, lm.getProbability(is3), 0.0001);
    }

    @Test
    public void testBackoffcount() throws IOException {
        SmoothLm lm = getTinyLm();
        LmVocabulary vocabulary = lm.getVocabulary();
        int[] is = {vocabulary.indexOf("<s>")};
        Assert.assertEquals(0, lm.getBackoffCount(is));
        int[] is2 = vocabulary.toIndexes("<s>", "kedi");
        Assert.assertEquals(0, lm.getBackoffCount(is2));
        int[] is3 = vocabulary.toIndexes("Ahmet", "dondurma", "yedi");
        Assert.assertEquals(0, lm.getBackoffCount(is3));
        int[] is4 = vocabulary.toIndexes("Ahmet", "yemez");
        Assert.assertEquals(1, lm.getBackoffCount(is4));
        int[] is5 = vocabulary.toIndexes("Ahmet", "yemez", "kırmızı");
        Assert.assertEquals(2, lm.getBackoffCount(is5));
    }

    @Test
    public void testExplain() throws IOException {
        SmoothLm lm = getTinyLm();
        LmVocabulary vocabulary = lm.getVocabulary();
        int[] is = {vocabulary.indexOf("<s>")};
        System.out.println(lm.explain(is));
        //<s> kedi
        int[] is2 = vocabulary.toIndexes("<s>", "kedi");
        System.out.println(lm.explain(is2));
        //Ahmet dondurma yedi
        int[] is3 = vocabulary.toIndexes("Ahmet", "dondurma", "yedi");
        System.out.println(lm.explain(is3));
        int[] is4 = vocabulary.toIndexes("Ahmet", "yemez");
        System.out.println(lm.explain(is4));
        int[] is5 = vocabulary.toIndexes("Ahmet", "yemez", "kırmızı");
        System.out.println(lm.explain(is5));
    }

    @Test
    public void testLogBaseChange() throws IOException {
        SmoothLm lm10 = getTinyLm();
        System.out.println(lm10.info());
        SmoothLm lm = SmoothLm.builder(getTinyLmFile()).logBase(Math.E).build();
        System.out.println(lm.info());
        Assert.assertEquals(lm.getLogBase(), Math.E, 0.00001);
        LmVocabulary vocabulary = lm.getVocabulary();
        int[] is = {vocabulary.indexOf("<s>")};
        Assert.assertEquals(l(-1.716003), lm.getProbabilityValue(is), 0.0001);
        Assert.assertEquals(l(-1.716003), lm.getProbability(is), 0.0001);
        //<s> kedi
        int[] is2 = {vocabulary.indexOf("<s>"), vocabulary.indexOf("kedi")};
        Assert.assertEquals(l(-0.796249), lm.getProbabilityValue(is2), 0.0001);
        Assert.assertEquals(l(-0.796249), lm.getProbability(is2), 0.0001);
        //Ahmet dondurma yedi
        int[] is3 = {vocabulary.indexOf("Ahmet"), vocabulary.indexOf("dondurma"), vocabulary.indexOf("yedi")};
        Assert.assertEquals(l(-0.602060), lm.getProbabilityValue(is3), 0.0001);
        Assert.assertEquals(l(-0.602060), lm.getProbability(is3), 0.0001);
    }

    private double l(double i) {
        return Math.log(Math.pow(10, i));
    }

    @Test
    @Ignore
    public void testActualData() throws IOException {
        Stopwatch sw = Stopwatch.createStarted();
        File lmFile = new File("/home/ahmetaa/data/lm/smoothnlp-test/lm1.slm");
        File tmp = new File("/tmp");
        if (!lmFile.exists()) {
            final File arpaFile = new File("/home/ahmetaa/data/lm/smoothnlp-test/lm1.arpa");
            UncompressedToSmoothLmConverter converter = new UncompressedToSmoothLmConverter(lmFile, tmp);
            converter.convertLarge(
                    MultiFileUncompressedLm.generate(arpaFile, tmp, "utf-8",4).dir,
                    new UncompressedToSmoothLmConverter.NgramDataBlock(2, 1, 1), 20);
        }
        SmoothLm lm = SmoothLm.builder(lmFile).build();
        System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));
        sw.reset();
        final int order = 3;
        final int gramCount = 1000000;
        int[][] ids = new int[gramCount][order];
        long[] trigrams = new long[gramCount];
        LineIterator li = SimpleTextReader.trimmingUTF8LineIterator(
                new File("/home/ahmetaa/data/lm/smoothnlp-test/corpus-lowercase_1000000_2000000"));
        SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();
        int i = 0;
        while (i < gramCount) {
            String line = li.next();
            String[] tokens = tokenizer.split(line);
            if (tokens.length < order)
                continue;
            for (int j = 0; j < tokens.length - order - 1; j++) {
                String[] words = new String[order];
                System.arraycopy(tokens, j, words, 0, order);
                int[] indexes = lm.getVocabulary().toIndexes(words);
                if (!lm.getVocabulary().containsAll(indexes))
                    continue;
                ids[i] = indexes;
                if (order == 3)
                    trigrams[i] = lm.getVocabulary().encodeTrigram(indexes);
                i++;
                if (i == gramCount)
                    break;
            }
        }
        sw.start();
        double tr = 0;
        for (int[] id : ids) {
            tr += lm.getProbability(id);
        }
        System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));
        System.out.println("tr = " + tr);
    }

    @Test
    public void testProbWithBackoff() throws IOException {
        SmoothLm lm = getTinyLm();
        LmVocabulary vocabulary = lm.getVocabulary();
        int ahmet = vocabulary.indexOf("Ahmet");
        int yemez = vocabulary.indexOf("yemez");
        // p(yemez|ahmet) = p(yemez) + b(ahmet) if p(yemez|ahmet) does not exist.
        double expected = -1.414973 + -0.316824;
        Assert.assertEquals(expected, lm.getProbability(ahmet, yemez), 0.0001);
    }


    @Test
    public void testTrigramBackoff() throws IOException {
        SmoothLm lm = getTinyLm();
        LmVocabulary vocabulary = lm.getVocabulary();
        int ahmet = vocabulary.indexOf("Ahmet");
        int armut = vocabulary.indexOf("armut");
        int kirmizi = vocabulary.indexOf("kırmızı");
        // p(kirmizi | Ahmet,armut) = b(ahmet, armut) + p(kırmızı|armut) if initial trigram prob does not exist.
        // if p(kırmızı|armut) also do not exist, we back off to b(ahmet, armut) + b(armut) + p(kırmızı)
        double backoffAhmetArmut = -0.124939;
        double backoffArmut = -0.492916;
        double probKirmizi = -1.539912;

        double expected = backoffAhmetArmut + backoffArmut + probKirmizi;
        System.out.println("expected = " + expected);
        System.out.println(lm.explain(ahmet, armut, kirmizi));
        Assert.assertEquals(expected, lm.getProbability(ahmet, armut, kirmizi), 0.0001);
        Assert.assertEquals(expected, lm.getTriGramProbability(ahmet, armut, kirmizi), 0.0001);
    }

    @Test
    public void testStupifBackoff() throws IOException {
        SmoothLm lm = SmoothLm.builder(getTinyLmFile()).useStupidBackoff().build();
        LmVocabulary vocabulary = lm.getVocabulary();
        int ahmet = vocabulary.indexOf("Ahmet");
        int armut = vocabulary.indexOf("armut");
        int kirmizi = vocabulary.indexOf("kırmızı");
        // p(kirmizi | Ahmet,armut) = b(ahmet, armut) + p(kırmızı|armut) if initial trigram prob does not exist.
        // if p(kırmızı|armut) also do not exist, we back off to b(ahmet, armut) + b(armut) + p(kırmızı)
        double probKirmizi = -1.539912;
        double expected = lm.getStupidBackoffLogAlpha() + lm.getStupidBackoffLogAlpha() + probKirmizi;
        System.out.println("expected = " + expected);
        System.out.println(lm.explain(ahmet, armut, kirmizi));
        Assert.assertEquals(expected, lm.getProbability(ahmet, armut, kirmizi), 0.0001);
    }

    @Test
    public void cacheTest() throws IOException {
        SmoothLm lm = getTinyLm();
        BaseLanguageModel.LookupCache cache = new BaseLanguageModel.LookupCache(lm);
        int[] is3 = lm.getVocabulary().toIndexes("Ahmet", "dondurma", "yedi");
        Assert.assertEquals(lm.getProbability(is3), cache.get(is3), 0.0001);
        Assert.assertEquals(lm.getProbability(is3), cache.get(is3), 0.0001);

        BaseLanguageModel.LookupCache cache2 = new BaseLanguageModel.LookupCache(lm);
        Assert.assertEquals(lm.getProbability(is3), cache2.get(is3), 0.0001);
        Assert.assertEquals(lm.getProbability(is3), cache2.get(is3), 0.0001);
    }


    @Test
    public void ngramExistTest() throws IOException {
        SmoothLm lm = getTinyLm();
        Assert.assertTrue(lm.ngramExists(lm.getVocabulary().toIndexes("Ahmet","elma")));
        Assert.assertTrue(lm.ngramExists(lm.getVocabulary().toIndexes("elma")));
        Assert.assertFalse(lm.ngramExists(lm.getVocabulary().toIndexes("elma","Ahmet")));
    }

    @Test
    @Ignore("Not an actual test.")
    public void loadLargeLmAndPrintInfo() throws IOException {
        SmoothLm lm = SmoothLm.builder(new File("/media/depo/data/asr/model/language/tr/makine-sf/lm.slm")).build();
        System.out.println(lm.info());
        System.out.println(lm.getVocabulary().size());
        System.out.println(Arrays.toString(lm.counts));
        System.out.println(lm.getVocabulary().indexOf("<UNK>"));
        System.out.println(lm.getVocabulary().indexOf("<unk>"));
    }
}
