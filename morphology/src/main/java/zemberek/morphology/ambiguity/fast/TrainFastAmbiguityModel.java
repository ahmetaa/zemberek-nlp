package zemberek.morphology.ambiguity.fast;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;

/**
 * Command-line entry point and training utility for {@link FastPerceptronAmbiguityResolver}.
 * Located next to {@link FastPerceptronAmbiguityResolver} in the morphology core module.
 */
public class TrainFastAmbiguityModel extends FastPerceptronAmbiguityResolverTrainer {

  public TrainFastAmbiguityModel() {
    super();
  }

  public TrainFastAmbiguityModel(double pruneThreshold) {
    super(pruneThreshold);
  }

  /**
   * Backward-compatible alias for {@link FastPerceptronAmbiguityResolverTrainer}.
   */
  public static class FastPerceptronTrainer extends FastPerceptronAmbiguityResolverTrainer {
    public FastPerceptronTrainer() {
      super();
    }

    public FastPerceptronTrainer(double pruneThreshold) {
      super(pruneThreshold);
    }
  }

  public static void main(String[] args) {
    Path trainPath = null;
    Path devPath = null;
    Path outputPath = null;
    int iterationCount = 7;
    double pruneWeight = 0.0;
    boolean exportText = false;

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      switch (arg) {
        case "-t":
        case "--train":
          trainPath = Paths.get(args[++i]);
          break;
        case "-d":
        case "--dev":
          devPath = Paths.get(args[++i]);
          break;
        case "-o":
        case "--output":
          outputPath = Paths.get(args[++i]);
          break;
        case "-it":
        case "--iterations":
          iterationCount = Integer.parseInt(args[++i]);
          break;
        case "-pw":
        case "--pruneWeight":
          pruneWeight = Double.parseDouble(args[++i]);
          break;
        case "--exportText":
          exportText = true;
          break;
        case "-h":
        case "--help":
          printUsage();
          return;
        default:
          System.err.println("Unknown argument: " + arg);
          printUsage();
          System.exit(1);
      }
    }

    if (trainPath == null || outputPath == null) {
      System.err.println("Error: --train (-t) and --output (-o) parameters are required.");
      printUsage();
      System.exit(1);
    }

    try {
      TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

      Log.info("Loading training dataset from: %s", trainPath);
      DataSet trainingSet = DataSet.load(trainPath, morphology);
      trainingSet.info();

      DataSet devSet = null;
      if (devPath != null) {
        Log.info("Loading development dataset from: %s", devPath);
        devSet = DataSet.load(devPath, morphology);
        devSet.info();
      }

      TrainFastAmbiguityModel trainer = new TrainFastAmbiguityModel(pruneWeight);
      Log.info("Starting fast perceptron training (%d iterations)...", iterationCount);
      trainer.train(trainingSet, devSet, iterationCount);

      trainer.saveModel(outputPath, exportText);
      Log.info("Training finished successfully.");
    } catch (IOException e) {
      Log.error("Training failed: %s", e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void printUsage() {
    System.out.println("Usage: TrainFastAmbiguityModel [options]");
    System.out.println("Options:");
    System.out.println("  -t,  --train <path>        Path to training dataset (.txt) [REQUIRED]");
    System.out.println("  -o,  --output <path>       Path to output binary compressed model (.bin) [REQUIRED]");
    System.out.println("  -d,  --dev <path>          Path to development dataset (.txt) [OPTIONAL]");
    System.out.println("  -it, --iterations <n>      Number of training iterations (epochs). Default: 7");
    System.out.println("  -pw, --pruneWeight <val>   Prune threshold for removing near-zero weights. Default: 0.0");
    System.out.println("  --exportText               Export uncompressed human-readable weights file (.txt)");
    System.out.println("  -h,  --help                Print this help message");
  }
}
