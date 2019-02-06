package zemberek.apps.ner;

import com.beust.jcommander.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.ner.NerDataSet;
import zemberek.ner.PerceptronNer;
import zemberek.ner.PerceptronNerTrainer;

public class TrainNerModel extends NerAppBase {

  @Parameter(
      names = {"--trainData", "-t"},
      required = true,
      description = "Annotated training data file path. ")
  public Path trainDataPath;

  @Parameter(
      names = {"--devData", "-d"},
      description = "Annotated development data file path. This is optional.")
  public Path developmentPath;

  @Parameter(
      names = {"--iterationCount", "-it"},
      description = "Training iteration count. Default is 7")
  public int iterationCount = 7;

  @Parameter(names = {"--learningRate", "-lr"},
      description =
          "Learning Rate, This is the amount perceptron weights are increased or decreased. " +
              "Default is 0.1")
  public float learningRate = 0.1f;

  @Override
  public String description() {
    return "Generates Turkish Named Entity Recognition model. There will be two model sets in the "
        + "output directory, one is text models (in [model] directory)"
        + ", other is compressed lossy model"
        + " (in [model-compressed] directory). Usually compressed"
        + " model is four times smaller than the text model.";
  }

  @Override
  public void run() throws Exception {

    initializeOutputDir();
    IOUtil.checkFileArgument(trainDataPath, "Training file");

    Path modelRoot = outDir.resolve("model");
    Path modelRootCompressed = outDir.resolve("model-compressed");
    Path logPath = outDir.resolve("train-log");
    Log.addFileHandler(logPath);

    if (developmentPath != null) {
      IOUtil.checkFileArgument(developmentPath, "Development file");
    }

    NerDataSet trainingSet = NerDataSet.load(trainDataPath, annotationStyle);
    Log.info("Training set information:");
    Log.info(trainingSet.info());

    NerDataSet devSet = null;
    if (developmentPath != null) {
      devSet = NerDataSet.load(developmentPath, annotationStyle);
      Log.info("Development set information:");
      Log.info(devSet.info());
    }

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    Log.info("------------ Training Started --------------------");
    PerceptronNer ner = new PerceptronNerTrainer(morphology)
        .train(trainingSet, devSet, iterationCount, learningRate);

    Files.createDirectories(modelRoot);
    Files.createDirectories(modelRootCompressed);
    ner.saveModelAsText(modelRoot);
    ner.saveModelCompressed(modelRootCompressed);
    Log.info("Text model is created in %s", modelRoot);
    Log.info("Compressed model is created in %s", modelRootCompressed);
  }

  public static void main(String[] args) {
    new TrainNerModel().execute(args);
  }
}
