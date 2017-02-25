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

        Args argz = new Args();
        argz.thread = 8;
        argz.model = Args.model_name.sup;
        argz.epoch = 5;
        argz.wordNgrams = 2;
        argz.minCount = 1;
        argz.lr = 0.1;
        argz.dim = 10;
        argz.bucket = 5_000_000;
        argz.minn = 3;
        argz.maxn = 6;

        Path inputRoot = Paths.get("/home/ahmetaa/projects/fastText/data");
        Path trainFile = inputRoot.resolve("dbpedia.train");
        Path modelPath = Paths.get("/home/ahmetaa/data/vector/fasttext/dbpedia.bin");

        Dictionary dictionary = Dictionary.readFromFile(trainFile, argz);
        FastText fastText = new FastText(argz, dictionary);

        if (modelPath.toFile().exists()) {
            fastText.loadModel(modelPath);
        } else {
            fastText.train(trainFile);
            fastText.saveModel(modelPath);
        }

        Path testFile = inputRoot.resolve("dbpedia.test");
        fastText.test(testFile, 1);
    }

    /**
     * Generates word vectors using skip-gram model.
     * run with -Xms8G or more.
     */
    @Test
    @Ignore("Not an actual Test.")
    public void skipgram() throws Exception {
        Args argz = new Args();
        argz.thread = 8;
        argz.model = Args.model_name.sg;
        argz.epoch = 5;
        argz.wordNgrams = 1;
        argz.dim = 100;
        argz.bucket = 1_000_000;
        argz.minn = 3;
        argz.maxn = 6;

        Path input = Paths.get("/home/ahmetaa/data/nlp/corpora/corpus-100k.txt");

        Dictionary dictionary = Dictionary.readFromFile(input, argz);
        FastText fastText = new FastText(argz, dictionary);

        Path outRoot = Paths.get("/home/ahmetaa/data/vector/fasttext");

        fastText.train(input);
        Path vectorFile = outRoot.resolve("100k-skipgram.vec");
        Log.info("Saving vectors to %s", vectorFile);
        fastText.saveVectors(vectorFile);
    }
}
