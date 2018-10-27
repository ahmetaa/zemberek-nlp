package zemberek.examples.ner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.ner.NerDataSet;
import zemberek.ner.NerDataSet.AnnotationStyle;
import zemberek.ner.PerceptronNer;
import zemberek.ner.PerceptronNerTrainer;

/**
 * This example shows how programmatically train NER model.
 * Alternatively, {@link zemberek.apps.ner.TrainNerModel} console application can be used.
 */
public class GenerateNerModel {

  public static void main(String[] args) throws IOException {

    // you will need ner-train and ner-test files to run this example.

    Path trainPath = Paths.get("ner-train");
    Path testPath = Paths.get("ner-test");
    Path modelRoot = Paths.get("my-model");

    NerDataSet trainingSet = NerDataSet.load(trainPath, AnnotationStyle.BRACKET);
    Log.info(trainingSet.info()); // prints information

    NerDataSet testSet = NerDataSet.load(testPath, AnnotationStyle.BRACKET);
    Log.info(testSet.info());

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    // Training occurs here. Result is a PerceptronNer instance.
    // There will be 7 iterations with 0.1 learning rate.
    PerceptronNer ner = new PerceptronNerTrainer(morphology)
        .train(trainingSet, testSet, 13, 0.1f);

    Files.createDirectories(modelRoot);
    ner.saveModelAsText(modelRoot);
  }

}
