package zemberek.embedding.fasttext;

import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FastTextTest {

    /**
     * Runs the dbpedia classification task.
     * run with -Xms8G or more.
     */
    @Test
    @Ignore("Not an actual Test.")
    public void dbpediaClassificationTest() throws Exception {

        Path inputRoot = Paths.get("/media/data/aaa/fasttext");
        Path trainFile = inputRoot.resolve("dbpedia.train");
        Path modelPath = Paths.get("/media/data/aaa/fasttext/dbpedia.model.bin");

        FastText fastText;

        if (modelPath.toFile().exists()) {
            fastText = FastText.load(modelPath);
        } else {
            Args argz = Args.forSupervised();
            argz.thread = 4;
            argz.epoch = 5;
            argz.wordNgrams = 2;
            argz.minCount = 1;
            argz.lr = 0.1;
            argz.dim = 10;
            argz.bucket = 5_000_000;

            fastText = FastText.train(trainFile, argz);
            fastText.saveModel(modelPath);
        }

        Path testFile = inputRoot.resolve("dbpedia.test");
        Log.info("Testing started.");
        fastText.test(testFile, 1);
    }

    /**
     * Runs the dbpedia classification task.
     * run with -Xms8G or more.
     */
    @Test
    @Ignore("Not an actual Test.")
    public void quantizationTest() throws Exception {

        Path inputRoot = Paths.get("/home/ahmetaa/projects/fastText/data");
        Path trainFile = inputRoot.resolve("train.10k");
        Path modelPath = inputRoot.resolve("10k.model.bin");
        Path quantizedModelPath = inputRoot.resolve("10k.model.qbin");
        Path testFile = inputRoot.resolve("dbpedia.test");

        Args argz = Args.forSupervised();
        argz.thread = 4;
        argz.epoch = 5;
        argz.wordNgrams = 2;
        argz.minCount = 1;
        argz.lr = 0.1;
        argz.dim = 32;
        argz.bucket = 5_000_000;

        FastText fastText = FastText.train(trainFile, argz);
        fastText.saveModel(modelPath);
        Log.info("Testing started.");
        fastText.test(testFile, 1);
        FastText loaded = FastText.load(modelPath);
        loaded.test(testFile, 1);
        argz.qnorm = true;
        FastText quantized = fastText.quantize(modelPath, argz);
        quantized.saveModel(quantizedModelPath);
        Log.info("Testing started.");
        quantized.test(testFile, 1);
    }

    /**
     * Generates word vectors using skip-gram model.
     * run with -Xms8G or more.
     */
    @Test
    @Ignore("Not an actual Test.")
    public void skipgram() throws Exception {
        Args argz = Args.forWordVectors(Args.model_name.sg);
        argz.thread = 8;
        argz.epoch = 10;
        argz.dim = 150;
        argz.bucket = 2_000_000;
        argz.minn = 3;
        argz.maxn = 6;
        argz.subWordHashProvider = new Dictionary.CharacterNgramHashProvider(argz.minn, argz.maxn);

        Path input = Paths.get("/home/ahmetaa/data/nlp/corpora/corpus-1M.txt");

        Path outRoot = Paths.get("/home/ahmetaa/data/vector/fasttext");

        FastText fastText = FastText.train(input, argz);
        Path vectorFile = outRoot.resolve("1M-skipgram.vec");
        Log.info("Saving vectors to %s", vectorFile);
        fastText.saveVectors(vectorFile);
    }
}
