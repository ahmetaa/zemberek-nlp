package zemberek.ner;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;

public class NerExperiment {

  public static void main(String[] args) throws IOException {

    Path root = Paths.get("/home/ahmetaa/data/nlp/ner");

    Path trainPath = root.resolve("sentences.50k.result.txt");
    Path testPath = root.resolve("reyyan.test.txt");
    Path modelRoot = root.resolve("ner/model-toy");
    Path reportPath = root.resolve("test-result.txt");
    trainAndTest(trainPath, testPath, modelRoot, reportPath);

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    PerceptronNer ner = PerceptronNer.loadModel(modelRoot, morphology);

    Stopwatch sw = Stopwatch.createStarted();

    Path input = root.resolve("sentences.1k");
    Path output = root.resolve("sentences.1k.result.txt");
    List<String> sentences = Files.readAllLines(input);

    try (PrintWriter pw = new PrintWriter(output.toFile(), "UTF-8")) {
      for (String sentence : sentences) {
        if (sentence.contains("[") || sentence.contains("]")) {
          continue;
        }
        NerSentence result = ner.findNamedEntities(sentence);
        pw.println(result.getAsTrainingSentence());
      }
    }

    System.out.println("Elapsed = " + sw.elapsed(TimeUnit.MILLISECONDS));
  }

  public static void trainAndTest(
      Path trainPath,
      Path testPath,
      Path modelRoot,
      Path reportPath) throws IOException {

    NerDataSet trainingSet = NerDataSet.loadBracketTurkishCorpus(trainPath);
    new NerDataSet.Info(trainingSet).log();

    NerDataSet testSet = NerDataSet.loadBracketTurkishCorpus(testPath);
    new NerDataSet.Info(testSet).log();

    TurkishMorphology morphology = TurkishMorphology.builder()
        .addDefaultDictionaries()
        .build();

    PerceptronNer ner = new PerceptronNerTrainer(morphology)
        .train(trainingSet, testSet, 7, 0.1f);

    Files.createDirectories(modelRoot);
    ner.saveModelAsText(modelRoot);

    Log.info("Testing %d sentences.", testSet.sentences.size());
    NerDataSet testResult = ner.test(testSet);

    PerceptronNerTrainer.testReport(testSet, testResult, reportPath);
    Log.info("Done.");
  }

}
