package zemberek.embedding.fasttext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.embeddings.Args;
import zemberek.core.embeddings.EmbeddingHashProviders;
import zemberek.core.embeddings.FastText;
import zemberek.core.embeddings.FastText.EvaluationResult;
import zemberek.core.embeddings.FastTextTrainer;
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

      fastText = new FastTextTrainer(argz).train(trainFile);
      fastText.saveModel(modelPath);
    }

    Path testFile = inputRoot.resolve("dbpedia.test");
    Log.info("Testing started.");
    EvaluationResult result = fastText.test(testFile, 1);
    Log.info(result.toString());
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
    test(fastText,testFile, 1);
    fastText = FastText.load(modelPath);
    test(fastText,testFile, 1);

    argz.qnorm = false;
    argz.cutoff = 15000;
    fastText = fastText.quantize(modelPath, argz);
    fastText.saveModel(quantizedModelPath);
    Log.info("Testing quantization result.");
    test(fastText, testFile, 1);
    fastText = FastText.load(quantizedModelPath);
    Log.info("Testing after loading quantized model.");
    test(fastText,testFile, 1);
  }

  private void test(FastText f, Path testPath, int k) throws IOException {
    EvaluationResult result = f.test(testPath, k);
    Log.info(result.toString());
  }

  /**
   * Runs the cooking label guessing task. run with -Xms8G or more.
   */
  @Test
  @Ignore("Not an actual Test.")
  public void classificationTest() throws Exception {

    Path inputRoot = Paths.get("/home/ahmetaa/data/fasttext");
    Path trainFile = inputRoot.resolve("cooking.train");
    //Path trainFile = inputRoot.resolve("train.10k");
    Path modelPath = inputRoot.resolve("cooking.model.bin");
    Path quantizedModelPath = inputRoot.resolve("cooking.model.qbin");
    Path testFile = inputRoot.resolve("cooking.valid");

    Args argz = Args.forSupervised();
    argz.thread = 4;
    argz.epoch = 25;
    argz.wordNgrams = 2;
    argz.minCount = 1;
    argz.lr = 1.0;
    argz.dim = 100;
    argz.bucket = 1000_000;

    FastText fastText;
/*    if(modelPath.toFile().exists())
      fastText = FastText.load(modelPath);
    else*/
    fastText = new FastTextTrainer(argz).train(trainFile);

    fastText.saveModel(modelPath);
    Log.info("Testing started.");
    test(fastText,testFile, 1);
    fastText = FastText.load(modelPath);
    test(fastText,testFile, 1);

    argz.qnorm = false;
    argz.cutoff = 3000;
    fastText = fastText.quantize(modelPath, argz);
    fastText.saveModel(quantizedModelPath);
    Log.info("Testing quantization result.");
    test(fastText,testFile, 1);
    fastText = FastText.load(quantizedModelPath);
    Log.info("Testing after loading quantized model.");
    test(fastText,testFile, 1);
  }

  /**
   * Generates word vectors using skip-gram model. run with -Xms8G or more.
   */
  @Test
  @Ignore("Not an actual Test.")
  public void skipgram() throws Exception {
    Args argz = Args.forWordVectors(Args.model_name.skipGram);
    argz.thread = 4;
    argz.epoch = 10;
    argz.dim = 100;
    argz.bucket = 2_000_000;
    argz.minn = 3;
    argz.maxn = 6;
    argz.subWordHashProvider = new EmbeddingHashProviders.CharacterNgramHashProvider(argz.minn,
        argz.maxn);

    Path input = Paths.get("/home/ahmetaa/data/nlp/corpora/sentences.50k");

    Path outRoot = Paths.get("/home/ahmetaa/data/fasttext");

    FastText fastText = new FastTextTrainer(argz).train(input);
    Path vectorFile = outRoot.resolve("sentences-50k-skipgram.vec");
    Log.info("Saving vectors to %s", vectorFile);
    fastText.saveVectors(vectorFile);
  }
}
