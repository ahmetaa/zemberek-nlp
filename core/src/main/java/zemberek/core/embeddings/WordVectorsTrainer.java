package zemberek.core.embeddings;

import zemberek.core.concurrency.ConcurrencyUtil;

/**
 * With this class word vectors can be trained from a corpus. It uses Java port of Fasttext
 * library.
 * <p>
 *
 * @see <a href="https://fasttext.cc/">Fasttext</a>
 */
public class WordVectorsTrainer {

  public enum ModelType {
    SKIP_GRAM, CBOW
  }

  static class Builder {

    ModelType type = ModelType.SKIP_GRAM;
    int wordNgramOrder = 1;
    SubWordHashProvider subWordHashProvider = new EmbeddingHashProviders.EmptySubwordHashProvider();
    float learningRate = 0.05f;
    int threadCount = ConcurrencyUtil
        .validateCpuThreadCount(Runtime.getRuntime().availableProcessors() / 2);
    int epochCount = 5;


  }


}
