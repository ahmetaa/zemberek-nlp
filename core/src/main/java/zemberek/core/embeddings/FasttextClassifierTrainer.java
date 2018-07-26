package zemberek.core.embeddings;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.nio.file.Path;
import zemberek.core.concurrency.ConcurrencyUtil;
import zemberek.core.embeddings.Args.loss_name;

public class FasttextClassifierTrainer {

  public static final int DEFAULT_DIMENSION = 100;
  public static final float DEFAULT_LR = 1f;
  public static final int DEFAULT_EPOCH = 5;
  public static final int DEFAULT_MIN_WORD_COUNT = 1;
  public static final int DEFAULT_WORD_NGRAM = 1;
  public static final int DEFAULT_CONTEXT_WINDOW_SIZE = 5;
  public static final int DEFAULT_TC = Runtime.getRuntime().availableProcessors() / 2;

  private EventBus eventBus = new EventBus();

  private Builder builder;

  FasttextClassifierTrainer(Builder builder) {
    this.builder = builder;
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  public static Builder builder() {
    return new Builder();
  }

  public enum LossType {
    SOFTMAX, HIERARCHICAL_SOFTMAX
  }


  public static class Builder {

    LossType type = LossType.SOFTMAX;
    int wordNgramOrder = DEFAULT_WORD_NGRAM;
    SubWordHashProvider subWordHashProvider =
        new EmbeddingHashProviders.EmptySubwordHashProvider();
    float learningRate = DEFAULT_LR;
    int threadCount = DEFAULT_TC;
    int contextWindowSize = DEFAULT_CONTEXT_WINDOW_SIZE;
    int epochCount = DEFAULT_EPOCH;
    int dimension = DEFAULT_DIMENSION;
    int minWordCount = DEFAULT_MIN_WORD_COUNT;

    public Builder modelType(LossType type) {
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

    public FasttextClassifierTrainer build() {
      return new FasttextClassifierTrainer(this);
    }
  }

  public FastText train(Path corpus) {
    Args args = Args.forSupervised();
    args.loss = builder.type == LossType.SOFTMAX ?
        loss_name.softmax : loss_name.hierarchicalSoftmax;
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
