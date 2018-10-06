package zemberek.apps.ner;

import com.beust.jcommander.Parameter;
import com.google.common.base.Stopwatch;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.ner.NerDataSet;
import zemberek.ner.PerceptronNer;
import zemberek.ner.PerceptronNerTrainer;
import zemberek.ner.PerceptronNerTrainer.TestResult;

public class EvaluateNer extends NerAppBase {

  @Parameter(
      names = {"--modelRoot", "-m"},
      required = true,
      description = "Annotated training data file path. ")
  public Path modelRoot;

  @Parameter(
      names = {"--testData", "-t"},
      required = true,
      description = "Annotated test data file path. This is optional.")
  public Path testPath;


  @Override
  public String description() {
    return "Evaluates an annotated NER data set with a model.";
  }

  @Override
  public void run() throws Exception {

    initializeOutputDir();
    IOUtil.checkDirectoryArgument(modelRoot, "Model Root");
    IOUtil.checkFileArgument(testPath, "Test File");

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    PerceptronNer ner = PerceptronNer.loadModel(modelRoot, morphology);

    NerDataSet testSet = NerDataSet.load(testPath, annotationStyle);
    Log.info(testSet.info());

    Stopwatch sw = Stopwatch.createStarted();
    NerDataSet testResult = ner.evaluate(testSet);
    double secs = sw.elapsed(TimeUnit.MILLISECONDS) / 1000d;
    Log.info("Test file processed in %.4f seconds.", secs);

    Path reportPath = outDir.resolve("eval-report");

    PerceptronNerTrainer.evaluationReport(testSet, testResult, reportPath);

    TestResult result = PerceptronNerTrainer.testLog(testSet, testResult);
    Log.info("Result:");
    Log.info(result.dump());

    Log.info("Evaluation report is written in %s", reportPath);
  }

  public static void main(String[] args) {
    new EvaluateNer().execute(args);
  }

}
