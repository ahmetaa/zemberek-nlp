package zemberek.embedding.fasttext;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.embeddings.Args;
import zemberek.core.embeddings.Dictionary;
import zemberek.core.embeddings.FastText;
import zemberek.core.logging.Log;

public class FastTextTest {

  /**
   * Runs the dbpedia classification task. run with -Xms8G or more.
   */
  @Test
  @Ignore("Not an actual Test.")
  public void dbpediaClassificationTest() throws Exception {

    Path inputRoot = Paths.get("/media/aaa/3t/aaa/fasttext");
    Path trainFile = inputRoot.resolve("dbpedia.train");
    Path modelPath = Paths.get("/media/aaa/3t/aaa/fasttext/dbpedia.model.bin");

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
      argz.dim = 32;
      argz.bucket = 5_000_000;

      fastText = FastText.train(trainFile, argz);
      fastText.saveModel(modelPath);
    }

    Path testFile = inputRoot.resolve("dbpedia.test");
    Log.info("Testing started.");
    fastText.test(testFile, 1);
  }

  /**
   * Runs the dbpedia classification task. run with -Xms8G or more.
   */
  @Test
  @Ignore("Not an actual Test.")
  public void quantizationTest() throws Exception {

    Path inputRoot = Paths.get("/home/ahmetaa/projects/fastText/data");
    Path trainFile = inputRoot.resolve("dbpedia.train");
    //Path trainFile = inputRoot.resolve("train.10k");
    Path modelPath = inputRoot.resolve("10k.model.bin");
    Path quantizedModelPath = inputRoot.resolve("10k.model.qbin");
    Path testFile = inputRoot.resolve("dbpedia.test");

    Args argz = Args.forSupervised();
    argz.thread = 4;
    argz.epoch = 15;
    argz.wordNgrams = 2;
    argz.minCount = 5;
    argz.lr = 0.1;
    argz.dim = 30;
    argz.bucket = 1000_000;

    FastText fastText = FastText.load(modelPath);
    //FastText fastText = FastText.train(trainFile, argz);


    fastText.saveModel(modelPath);
    Log.info("Testing started.");
    fastText.test(testFile, 1);
    fastText = FastText.load(modelPath);
    fastText.test(testFile, 1);

    argz.qnorm = false;
    argz.cutoff = 15000;
    fastText = fastText.quantize(modelPath, argz);
    fastText.saveModel(quantizedModelPath);
    Log.info("Testing quantization result.");
    fastText.test(testFile,1);
    fastText = FastText.load(quantizedModelPath);
    Log.info("Testing after loading quantized model.");
    fastText.test(testFile,1);
  }

  /**
   * Generates word vectors using skip-gram model. run with -Xms8G or more.
   */
  @Test
  @Ignore("Not an actual Test.")
  public void skipgram() throws Exception {
    Args argz = Args.forWordVectors(Args.model_name.sg);
    argz.thread = 8;
    argz.epoch = 10;
    argz.dim = 100;
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
