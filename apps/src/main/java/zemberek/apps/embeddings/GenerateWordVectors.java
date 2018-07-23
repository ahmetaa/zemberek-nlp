package zemberek.apps.embeddings;

import com.beust.jcommander.Parameter;
import com.google.common.eventbus.Subscribe;
import java.io.IOException;
import java.nio.file.Path;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import zemberek.apps.ConsoleApp;
import zemberek.core.embeddings.FastText;
import zemberek.core.embeddings.FastTextTrainer;
import zemberek.core.embeddings.WordVectorsTrainer;
import zemberek.core.embeddings.WordVectorsTrainer.ModelType;
import zemberek.core.logging.Log;

public class GenerateWordVectors extends ConsoleApp {

  @Parameter(names = {"--input", "-i"},
      required = true,
      description = "Input corpus text file. Assumed to be one sentence per line, "
          + "tokenized and in UTF-8 encoding.")
  public Path input;

  @Parameter(names = {"--output", "-o"},
      required = true,
      description = "Output model file.")
  public Path output;

  @Parameter(names = {"--type", "-t"},
      description = "Model type.")
  public WordVectorsTrainer.ModelType modelType = ModelType.SKIP_GRAM;

  @Parameter(names = {"--learningRate", "-lr"},
      description = "Learning rate. Should be between 0.01-1.0")
  public float learningRate = WordVectorsTrainer.DEFAULT_LR;

  @Parameter(names = {"--wordNGrams", "-wng"},
      description = "Word N-Gram order.")
  public int wordNGrams = WordVectorsTrainer.DEFAULT_WORD_NGRAM;

  @Parameter(names = {"--dimension", "-dim"},
      description = "Vector dimension.")
  public int dimension = WordVectorsTrainer.DEFAULT_DIMENSION;

  @Parameter(names = {"--contextWindowSize", "-ws"},
      description = "Context window size.")
  public int contextWindowSize = WordVectorsTrainer.DEFAULT_CONTEXT_WINDOW_SIZE;

  @Parameter(names = {"--threadCount", "-tc"},
      description = "Thread Count.")
  public int threadCount = WordVectorsTrainer.DEFAULT_TC;

  @Parameter(names = {"--epochCount", "-ec"},
      description = "Epoch Count.")
  public int epochCount = WordVectorsTrainer.DEFAULT_EPOCH;

  @Parameter(names = {"--minWordCount", "-minc"},
      description = "Words with lower than this count will be ignored..")
  public int minWordCount = WordVectorsTrainer.DEFAULT_MIN_WORD_COUNT;

  @Override
  public String description() {
    return "Generates word vectors using a text corpus. Uses java port of fastText project.";
  }

  ProgressBar pb;

  @Override
  public void run() throws IOException {

    Log.info("Generating word vectors from %s", input);

    WordVectorsTrainer trainer = WordVectorsTrainer.builder()
        .epochCount(epochCount)
        .learningRate(learningRate)
        .modelType(modelType)
        .minWordCount(minWordCount)
        .threadCount(threadCount)
        .wordNgramOrder(wordNGrams)
        .dimension(dimension)
        .contextWindowSize(contextWindowSize)
        .build();

    Log.info("Training Started.");
    trainer.getEventBus().register(this);

    FastText fastText = trainer.train(input);
    if(pb!=null) {
      pb.close();
    }

    Log.info("Saving vectors in text format to %s", output);
    fastText.saveVectors(output);
  }

  @Subscribe
  public void trainingProgress(FastTextTrainer.Progress progress) {

    synchronized (this) {
      if (pb == null) {
        System.setProperty("org.jline.terminal.dumb", "true");
        pb = new ProgressBar("", progress.total, ProgressBarStyle.ASCII);
      }
    }
    pb.stepTo(progress.current);
    pb.setExtraMessage(String.format("lr: %.6f", progress.learningRate));
  }

  public static void main(String[] args) {
    new GenerateWordVectors().execute(args);
  }

}
