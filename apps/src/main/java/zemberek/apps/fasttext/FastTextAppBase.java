package zemberek.apps.fasttext;

import com.beust.jcommander.Parameter;
import com.google.common.eventbus.Subscribe;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import zemberek.apps.ConsoleApp;
import zemberek.core.embeddings.FastTextTrainer;
import zemberek.core.embeddings.WordVectorsTrainer;

public abstract class FastTextAppBase extends ConsoleApp {

  @Parameter(names = {"--wordNGrams", "-wng"},
      description = "Word N-Gram order.")
  int wordNGrams = WordVectorsTrainer.DEFAULT_WORD_NGRAM;

  @Parameter(names = {"--dimension", "-dim"},
      description = "Vector dimension.")
  int dimension = WordVectorsTrainer.DEFAULT_DIMENSION;

  @Parameter(names = {"--contextWindowSize", "-ws"},
      description = "Context window size.")
  int contextWindowSize = WordVectorsTrainer.DEFAULT_CONTEXT_WINDOW_SIZE;

  @Parameter(names = {"--threadCount", "-tc"},
      description = "Thread Count.")
  int threadCount = WordVectorsTrainer.DEFAULT_TC;

  @Parameter(names = {"--minWordCount", "-minc"},
      description = "Words with lower than this count will be ignored..")
  int minWordCount = WordVectorsTrainer.DEFAULT_MIN_WORD_COUNT;

  ProgressBar pb;

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

}
