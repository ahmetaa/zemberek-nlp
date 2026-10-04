package zemberek.morphology.ambiguity.dataset;

import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import zemberek.apps.ConsoleApp;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolverTrainer;

/**
 * Command-line runner for training a Fast Averaged Perceptron morphological disambiguation model.
 * Delegates core training logic to {@link FastPerceptronAmbiguityResolverTrainer}.
 */
public class TrainFastAmbiguityModel extends ConsoleApp {

  @Parameter(
      names = {"--train", "-t"},
      required = true,
      description = "Path to annotated training dataset (.jsonl or .txt)")
  public Path trainPath;

  @Parameter(
      names = {"--dev", "-d"},
      description = "Path to development dataset (.jsonl or .txt). If omitted, development testing is skipped.")
  public Path devPath;

  @Parameter(
      names = {"--output", "-o"},
      required = true,
      description = "Path to output binary compressed model (.bin)")
  public Path outputPath;

  @Parameter(
      names = {"--iterations", "-it"},
      description = "Number of perceptron training iterations (epochs). Default is 7.")
  public int iterationCount = 7;

  @Parameter(
      names = {"--pruneWeight", "-pw"},
      description = "Prune threshold for removing near-zero weights. Default is 0.0")
  public double pruneWeight = 0.0;

  @Parameter(
      names = {"--exportText"},
      description = "Also export uncompressed human-readable weights file (.txt)")
  public boolean exportText = false;

  @Parameter(
      names = {"--filterUnreachable", "-fu"},
      description = "Filter out sentences containing unreachable gold analyses. Default is true.")
  public boolean filterUnreachable = true;

  public static void main(String[] args) {
    new TrainFastAmbiguityModel().execute(args);
  }

  @Override
  public String description() {
    return "Trains a Fast Averaged Perceptron morphological ambiguity resolver.";
  }

  @Override
  public void run() throws Exception {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    Log.info("Loading training dataset from: %s", trainPath);
    DataSet trainingSet = TrainAmbiguityModel.loadDataSet(trainPath, morphology, filterUnreachable);
    trainingSet.info();

    DataSet devSet = null;
    if (devPath != null) {
      Log.info("Loading development dataset from: %s", devPath);
      devSet = TrainAmbiguityModel.loadDataSet(devPath, morphology, filterUnreachable);
      devSet.info();
    }

    FastPerceptronAmbiguityResolverTrainer trainer =
        new FastPerceptronAmbiguityResolverTrainer(pruneWeight);
    Log.info("Starting perceptron training (%d iterations)...", iterationCount);
    FastPerceptronAmbiguityResolver resolver = trainer.train(trainingSet, devSet, iterationCount);

    trainer.saveModel(outputPath, exportText);
    Log.info("Training finished successfully. Feature count: %d", trainer.getAveragedWeights().size());
  }

  /**
   * Backward-compatible alias for {@link FastPerceptronAmbiguityResolverTrainer}.
   */
  public static class FastPerceptronTrainer extends FastPerceptronAmbiguityResolverTrainer {
    public FastPerceptronTrainer(double pruneThreshold) {
      super(pruneThreshold);
    }
  }
}
