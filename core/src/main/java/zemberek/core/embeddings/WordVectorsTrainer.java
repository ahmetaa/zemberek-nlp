package zemberek.core.embeddings;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.nio.file.Path;
import zemberek.core.concurrency.ConcurrencyUtil;
import zemberek.core.embeddings.Args.model_name;

/**
 * With this class word vectors can be trained from a text corpus. It uses Java port of fastText
 * library.
 * <p>
 *
 * @see <a href="https://fasttext.cc/">fastText</a>
 */
public class WordVectorsTrainer {

  public static final int DEFAULT_DIMENSION = 50;
  public static final float DEFAULT_LR = 0.05f;
  public static final int DEFAULT_EPOCH = 5;
  public static final int DEFAULT_MIN_WORD_COUNT = 1;
  public static final int DEFAULT_WORD_NGRAM = 1;
  public static final int DEFAULT_CONTEXT_WINDOW_SIZE = 5;
  public static final int DEFAULT_TC = Runtime.getRuntime().availableProcessors() / 2;

  private EventBus eventBus = new EventBus();

  private Builder builder;

  WordVectorsTrainer(Builder builder) {
    this.builder = builder;
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  public static Builder builder() {
    return new Builder();
  }

  public enum ModelType {
    SKIP_GRAM, CBOW
  }

  public static class Builder {

    ModelType type = ModelType.SKIP_GRAM;
    int wordNgramOrder = DEFAULT_WORD_NGRAM;
    SubWordHashProvider subWordHashProvider =
        new EmbeddingHashProviders.EmptySubwordHashProvider();
    float learningRate = DEFAULT_LR;
    int threadCount = DEFAULT_TC;
    int contextWindowSize = DEFAULT_CONTEXT_WINDOW_SIZE;
    int epochCount = DEFAULT_EPOCH;
    int dimension = DEFAULT_DIMENSION;
    int minWordCount = DEFAULT_MIN_WORD_COUNT;

    public Builder modelType(ModelType type) {
      this.type = type;
      return this;
    }

    public Builder wordNgramOrder(int order) {
      this.wordNgramOrder = order;
      return this;
    }

    public Builder learningRate(float lr) {
      this.learningRate = lr;
      return this;
    }

    public Builder minWordCount(int minWordCount) {
      this.minWordCount = minWordCount;
      return this;
    }

    public Builder threadCount(int threadCount) {
      ConcurrencyUtil.validateCpuThreadCount(threadCount);
      return this;
    }

    public Builder epochCount(int epochCount) {
      this.epochCount = epochCount;
      return this;
    }

    public Builder dimension(int dimension) {
      this.dimension = dimension;
      return this;
    }

    public Builder contextWindowSize(int contextWindowSize) {
      this.contextWindowSize = contextWindowSize;
      return this;
    }

    public WordVectorsTrainer build() {
      return new WordVectorsTrainer(this);
    }
  }

  public FastText train(Path corpus) {
    Args.model_name m = builder.type == ModelType.SKIP_GRAM ?
        model_name.skipGram : model_name.cbow;
    Args args = Args.forWordVectors(m);
    args.dim = builder.dimension;
    args.wordNgrams = builder.wordNgramOrder;
    args.thread = builder.threadCount;
    args.epoch = builder.epochCount;
    args.lr = builder.learningRate;
    args.ws = builder.contextWindowSize;
    SubWordHashProvider p = builder.subWordHashProvider;
    args.subWordHashProvider = p;
    args.minn = p.getMinN();
    args.maxn = p.getMaxN();
    args.minCount = builder.minWordCount;

    FastTextTrainer trainer = new FastTextTrainer(args);

    // for catching and forwarding progress events.
    trainer.getEventBus().register(this);

    try {
      return trainer.train(corpus);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Subscribe
  public void trainingProgress(FastTextTrainer.Progress progress) {
    this.eventBus.post(progress);
  }


}
