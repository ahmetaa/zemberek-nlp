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
import zemberek.ner.PerceptronNer.Gazetteers;

public class NerExperiment {

  public static void main(String[] args) throws IOException {


    Path trainPath = Paths.get("experiment/src/main/resources/ner/reyyan.train.txt");
    Path testPath = Paths.get("experiment/src/main/resources/ner/reyyan.test.txt");
    Path modelRoot = Paths.get("experiment/src/main/resources/ner/model");
    Path reportPath = Paths.get("experiment/src/main/resources/ner/test-result.txt");
//    trainAndTest(trainPath, testPath, modelRoot, reportPath);


    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    PerceptronNer ner = PerceptronNer.loadModel(modelRoot, morphology);

    Stopwatch sw = Stopwatch.createStarted();

    Path input = Paths.get("experiment/src/main/resources/ner/test.txt");
    Path output = Paths.get("experiment/src/main/resources/ner/test.out.txt");
    List<String> sentences = Files.readAllLines(input);

    try (PrintWriter pw = new PrintWriter(output.toFile(), "UTF-8")) {
      for (String sentence : sentences) {
        pw.println(sentence);
        NerSentence result = ner.findNamedEntities(sentence);
        for (NamedEntity entity : result.getNamedEntities()) {
          pw.println(entity.toString());
        }
        pw.println("-------------");
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

    // empty, not used.
    Gazetteers gazetteers = new Gazetteers();

    TurkishMorphology morphology = TurkishMorphology.builder()
        .addDefaultDictionaries()
        .build();

    PerceptronNer ner = new PerceptronNerTrainer(morphology, gazetteers)
        .train(trainingSet, testSet, 10, 0.1f);

    ner.saveModelAsText(modelRoot);

    Log.info("Testing %d sentences.", testSet.sentences.size());
    NerDataSet testResult = ner.test(testSet);

    PerceptronNerTrainer.testReport(testSet, testResult, reportPath);
    Log.info("Done.");
  }

}
