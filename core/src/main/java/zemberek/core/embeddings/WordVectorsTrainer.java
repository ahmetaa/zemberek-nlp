package zemberek.core.embeddings;

import java.nio.file.Path;
import zemberek.core.concurrency.ConcurrencyUtil;

/**
 * With this class word vectors can be trained from a text corpus. It uses Java port of fastText
 * library.
 * <p>
 *
 * @see <a href="https://fasttext.cc/">fastText</a>
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

    Builder modelType(ModelType type) {
      this.type  = type;
      return this;
    }

    Builder wordNgramOrder(int order) {
      this.wordNgramOrder = order;
      return this;
    }

    Builder learningRate(float lr) {
      this.learningRate = lr;
      return this;
    }

    Builder threadCount(int threadCount) {
      ConcurrencyUtil.validateCpuThreadCount(threadCount);
      return this;
    }

    Builder epochCount(int epochCount) {
      this.epochCount = epochCount;
      return this;
    }
  }

  public WordVectorsModel train(Path corpusc) {
    return null;
  }


}
