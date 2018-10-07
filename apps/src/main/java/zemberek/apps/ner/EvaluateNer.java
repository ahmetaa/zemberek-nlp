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
      description = "Model directory. "
          + "This is required if only no separate hypothesis file is provided.")
  public Path modelRoot;

  @Parameter(
      names = {"--reference", "-r"},
      required = true,
      description = "Annotated reference data file path. This is the reference data. "
          + "If no hypothesis is provided, system will run NER on this file with given model and "
          + "evaluate results against this data.")
  public Path referencePath;

  @Parameter(
      names = {"--hypothesis", "-h"},
      description = "This is the result of a NER system. If this file is provided, system will "
          + "evaluate it against reference file directly. Hypothesis and reference sentences count"
          + "and order must be the same. "
          + "If hypothesis is not provided, system will apply NER "
          + "on reference with given model and evaluate its result against reference data.")
  public Path hypothesisPath;

  @Override
  public String description() {
    return
        "Evaluates an annotated NER data set (reference) by either actually running NER with a "
            + "given model or against an already generated hypothesis data.";
  }

  @Override
  public void run() throws Exception {

    initializeOutputDir();
    if (hypothesisPath == null) {
      IOUtil.checkDirectoryArgument(modelRoot, "Model Root");
    } else {
      IOUtil.checkFileArgument(referencePath, "Hypothesis File");
    }
    IOUtil.checkFileArgument(referencePath, "Reference File");

    NerDataSet hypothesis;

    NerDataSet reference = NerDataSet.load(referencePath, annotationStyle);
    Log.info("Reference :");
    Log.info(reference.info());

    if (hypothesisPath == null) {

      TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
      PerceptronNer ner = PerceptronNer.loadModel(modelRoot, morphology);
      Stopwatch sw = Stopwatch.createStarted();
      hypothesis = ner.evaluate(reference);
      double secs = sw.elapsed(TimeUnit.MILLISECONDS) / 1000d;
      Log.info("NER is applied to reference data in %.4f seconds.", secs);
    } else {
      hypothesis = NerDataSet.load(hypothesisPath, annotationStyle);
    }
    Log.info("Hypothesis :");
    Log.info(hypothesis.info());

    Path reportPath = outDir.resolve("eval-report");

    PerceptronNerTrainer.evaluationReport(reference, hypothesis, reportPath);

    TestResult result = PerceptronNerTrainer.collectEvaluationData(reference, hypothesis);

    Log.info("Evaluation Result:");
    Log.info(result.dump());

    Log.info("Detailed evaluation report is written in %s", reportPath);
  }

  public static void main(String[] args) {
    new EvaluateNer().execute(args);
  }

}
