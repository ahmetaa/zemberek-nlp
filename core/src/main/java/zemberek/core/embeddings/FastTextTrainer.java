package zemberek.core.embeddings;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import zemberek.core.collections.IntVector;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.text.TextIO;

public class FastTextTrainer {

  private static EventBus eventBus = new EventBus();

  public EventBus getEventBus() {
    return eventBus;
  }

  private Args args_;

  public FastTextTrainer(Args args) {
    this.args_ = args;
  }

  /**
   * Trains a model for the input with given arguments, returns a FastText instance. Input can be a
   * text corpus, or a corpus with text and labels.
   */
  public FastText train(Path input) throws Exception {

    Dictionary dict_ = Dictionary.readFromFile(input, args_);
    Matrix input_ = null;
    if (args_.pretrainedVectors.length() != 0) {
      //TODO: implement this.
      //loadVectors(args_->pretrainedVectors);
    } else {
      input_ = new Matrix(dict_.nwords() + args_.bucket, args_.dim);
      input_.uniform(1.0f / args_.dim);
    }

    Matrix output_;
    if (args_.model == Args.model_name.supervised) {
      output_ = new Matrix(dict_.nlabels(), args_.dim);
    } else {
      output_ = new Matrix(dict_.nwords(), args_.dim);
    }

    Model model_ = new Model(input_, output_, args_, 0);
    if (args_.model == Args.model_name.supervised) {
      model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_LABEL));
    } else {
      model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_WORD));
    }

    Stopwatch stopwatch = Stopwatch.createStarted();
    AtomicLong tokenCount = new AtomicLong(0);

    ExecutorService es = Executors.newFixedThreadPool(args_.thread);
    CompletionService<Model> completionService = new ExecutorCompletionService<>(es);
    long charCount = TextIO.charCount(input, StandardCharsets.UTF_8);
    Log.info("Training started.");
    Stopwatch sw = Stopwatch.createStarted();
    for (int i = 0; i < args_.thread; i++) {
      // Here a model per thread is generated. It uses references to global model's input and output matrices.
      // AFAIK, original Fasttext does not care about thread safety of those matrices.
      Model threadModel = new Model(model_, i);

      completionService.submit(new TrainTask(
          i,
          input,
          (int) (i * charCount / args_.thread),
          threadModel,
          stopwatch,
          dict_,
          args_,
          tokenCount));
    }
    es.shutdown();

    int c = 0;
    while (c < args_.thread) {
      completionService.take().get();
      c++;
    }
    return new FastText(args_, dict_, model_);
  }

  public static class Progress {

    public long total;
    public long current;
    public float percentProgress;
    public float wordsPerSecond;
    public float learningRate;
    public float loss;
    public String eta;

    public Progress(float percentProgress, float wordsPerSecond, float learningRate, float loss,
        String eta) {
      this.percentProgress = percentProgress;
      this.wordsPerSecond = wordsPerSecond;
      this.learningRate = learningRate;
      this.loss = loss;
      this.eta = eta;
    }
  }

  private static class TrainTask implements Callable<Model> {

    int threadId;
    Path input;
    int startCharIndex;
    Stopwatch stopwatch;
    AtomicLong tokenCount;
    Model model;
    Dictionary dictionary;
    Args args_;

    TrainTask(int threadId,
        Path input,
        int startCharIndex,
        Model model,
        Stopwatch stopwatch,
        Dictionary dictionary,
        Args args_,
        AtomicLong tokenCount) {
      this.threadId = threadId;
      this.input = input;
      this.startCharIndex = startCharIndex;
      this.model = model;
      this.stopwatch = stopwatch;
      this.tokenCount = tokenCount;
      this.dictionary = dictionary;
      this.args_ = args_;
    }

    private void printInfo(float progress, float loss) {
      float t = stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000f;
      float wst = (float) tokenCount.get() / t;
      float lr = (float) (args_.lr * (1.0f - progress));
      int eta = (int) (t / progress * (1 - progress) / args_.thread);
      int etah = eta / 3600;
      int etam = (eta - etah * 3600) / 60;

      Progress p = new Progress(
          100 * progress,
          wst,
          lr,
          loss,
          String.format("%dh%dm", etah, etam)
      );
      p.total = args_.epoch * dictionary.ntokens();
      p.current = tokenCount.get();

      eventBus.post(p);

    }

    private void supervised(Model model,
        float lr,
        int[] line,
        int[] labels) {
      if (labels.length == 0 || line.length == 0) {
        return;
      }
      int i = model.getRng().nextInt(labels.length);
      model.update(line, labels[i], lr);
    }

    private void cbow(Model model, float lr, int[] line) {
      for (int w = 0; w < line.length; w++) {
        int boundary = model.getRng().nextInt(args_.ws) + 1; // [1..args.ws]
        IntVector bow = new IntVector();
        for (int c = -boundary; c <= boundary; c++) {
          if (c != 0 && w + c >= 0 && w + c < line.length) {
            int[] ngrams = dictionary.getSubWords(line[w + c]);
            bow.addAll(ngrams);
          }
        }
        model.update(bow.copyOf(), line[w], lr);
      }
    }

    private void skipgram(Model model, float lr, int[] line) {
      for (int w = 0; w < line.length; w++) {
        int boundary = model.getRng().nextInt(args_.ws) + 1; // [1..args.ws]
        int[] ngrams = dictionary.getSubWords(line[w]);
        for (int c = -boundary; c <= boundary; c++) {
          if (c != 0 && w + c >= 0 && w + c < line.length) {
            model.update(ngrams, line[w + c], lr);
          }
        }
      }
    }

    @Override
    public Model call() {

      if (args_.model == Args.model_name.supervised) {
        model.setTargetCounts(dictionary.getCounts(Dictionary.TYPE_LABEL));
      } else {
        model.setTargetCounts(dictionary.getCounts(Dictionary.TYPE_WORD));
      }

      long ntokens = dictionary.ntokens();
      long localTokenCount = 0;

      Iterator<TextChunk> it = BlockTextLoader
          .iteratorFromCharIndex(input, 1000, startCharIndex);
      float progress = 0f;
      while (true) {
        while (it.hasNext()) {
          List<String> lines = it.next().getData();
          for (String lineStr : lines) {
            if (tokenCount.get() >= args_.epoch * ntokens) {
              if (threadId == 0 && args_.verbose > 0) {
                printInfo(1.0f, model.getLoss());
              }
              return model;
            }
            IntVector line = new IntVector(15);
            progress = (float) ((1.0 * tokenCount.get()) / (args_.epoch * ntokens));
            float lr = (float) (args_.lr * (1.0 - progress));

            if (args_.model == Args.model_name.supervised) {
              IntVector labels = new IntVector();
              localTokenCount += dictionary.getLine(lineStr, line, labels);
              supervised(model, lr, line.copyOf(), labels.copyOf());
            } else if (args_.model == Args.model_name.cbow) {
              localTokenCount += dictionary.getLine(lineStr, line, model.getRng());
              cbow(model, lr, line.copyOf());
            } else if (args_.model == Args.model_name.skipGram) {
              localTokenCount += dictionary.getLine(lineStr, line, model.getRng());
              skipgram(model, lr, line.copyOf());
            }
            if (localTokenCount > args_.lrUpdateRate) {
              tokenCount.getAndAdd(localTokenCount);
              localTokenCount = 0;
            }
          }
          if (threadId == 0 && args_.verbose > 1) {
            printInfo(progress, model.getLoss());
          }
        }
        // start from the beginning again.
        it = BlockTextLoader.singlePathIterator(input, 1000);
      }
    }
  }


}
