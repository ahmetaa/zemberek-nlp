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
    return "Generates Turkish Named Entity Recognition model.";
  }

  @Override
  public void run() throws Exception {

    initializeOutputDir();
    IOUtil.checkFileArgument(trainDataPath,"Training file");

    Path modelRoot = outDir.resolve("model");
    Path logPath = outDir.resolve("train-log");
    Log.addFileHandler(logPath);

    if (developmentPath != null) {
      IOUtil.checkFileArgument(developmentPath, "Development file");
    }

    NerDataSet trainingSet = NerDataSet.load(trainDataPath, annotationStyle);
    Log.info(trainingSet.info());

    NerDataSet devSet = null;
    if (developmentPath != null) {
      devSet = NerDataSet.load(developmentPath, annotationStyle);
      Log.info(devSet.info());
    }

    TurkishMorphology morphology = TurkishMorphology.builder()
        .addDefaultBinaryDictionary()
        .build();

    Log.info("------------ Training Started --------------------");
    PerceptronNer ner = new PerceptronNerTrainer(morphology)
        .train(trainingSet, devSet, iterationCount, learningRate);

    Files.createDirectories(modelRoot);
    ner.saveModelAsText(modelRoot);
    Log.info("NER model is created in %s", modelRoot);
  }

  public static void main(String[] args) {
    new TrainNerModel().execute(args);
  }
}
