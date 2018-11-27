package zemberek.examples.classification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import zemberek.apps.fasttext.EvaluateClassifier;
import zemberek.apps.fasttext.TrainClassifier;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;

public class NewsTitleCategoryFinder extends ClassificationExampleBase {

  public static final int TEST_SIZE = 1000;

  public static void main(String[] args) throws IOException {
    NewsTitleCategoryFinder experiment = new NewsTitleCategoryFinder();
    // Download data set `news-title-category-set`
    // from https://drive.google.com/drive/folders/1JBPExAeRctAXL2oGW2U6CbqfwIJ84BG7
    // and change the line below.
    String set = "/home/aaa/data/zemberek/classification/news-title-category-set";

    morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .build();

    Path dataPath = Paths.get(set);
    Path root = dataPath.getParent();
    if (root == null) {
      root = Paths.get("");
    }
    List<String> lines = Files.readAllLines(dataPath, StandardCharsets.UTF_8);
    String name = dataPath.toFile().getName();
    experiment.dataInfo(lines);
    Log.info("------------ Evaluation with raw data ------------------");
    experiment.evaluate(dataPath, TEST_SIZE);

    Path tokenizedPath = root.resolve(name + ".tokenized");
    Log.info("------------ Evaluation with tokenized - lowercase data ------------");
    experiment.generateSetTokenized(lines, tokenizedPath);
    experiment.evaluate(tokenizedPath, TEST_SIZE);

    Path lemmasPath = root.resolve(name + ".lemmas");
    Log.info("------------ Evaluation with lemma - lowercase data ------------");
    if (!lemmasPath.toFile().exists()) {
      experiment.generateSetWithLemmas(lines, lemmasPath);
    }
    experiment.evaluate(lemmasPath, TEST_SIZE);

    Path splitPath = root.resolve(name + ".split");
    Log.info("------------ Evaluation with Stem-Ending - lowercase data ------------");
    if (!splitPath.toFile().exists()) {
      experiment.generateSetWithSplit(lines, splitPath);
    }
    experiment.evaluate(splitPath, TEST_SIZE);

  }

  private void evaluate(Path set, int testSize) throws IOException {

    // Create training and test sets.
    List<String> lines = Files.readAllLines(set, StandardCharsets.UTF_8);
    Path root = set.getParent();
    if (root == null) {
      root = Paths.get("");
    }
    String name = set.toFile().getName();

    Path train = root.resolve(name + ".train");
    Path testPath = root.resolve(name + ".test");

    Files.write(train, lines.subList(testSize, lines.size()));
    Files.write(testPath, lines.subList(0, testSize));

    //Create model if it does not exist.
    Path modelPath = root.resolve(name + ".model");
    if (!modelPath.toFile().exists()) {
      new TrainClassifier().execute(
          "-i", train.toString(),
          "-o", modelPath.toString(),
          "--learningRate", "0.1",
          "--epochCount", "70",
          "--dimension", "100",
          "--wordNGrams", "2"/*,
          "--applyQuantization",
          "--cutOff", "25000"*/
      );
    }
    Log.info("Testing...");
    test(testPath, root.resolve(name + ".predictions"), modelPath);
    // test quantized models.
/*    Log.info("Testing with quantized model...");
    test(testPath, root.resolve(name + ".predictions.q"), root.resolve(name + ".model.q"));*/
  }

  private void test(Path testPath, Path predictionsPath, Path modelPath) {
    new EvaluateClassifier().execute(
        "-i", testPath.toString(),
        "-m", modelPath.toString(),
        "-o", predictionsPath.toString(),
        "-k", "1"
    );
  }

  void dataInfo(List<String> lines) {
    Log.info("Total lines = " + lines.size());
    Histogram<String> hist = new Histogram<>();
    lines.stream()
        .map(s -> s.substring(0, s.indexOf(' ')))
        .forEach(hist::add);
    Log.info("Categories :");
    for (String s : hist.getSortedList()) {
      Log.info(s + " " + hist.getCount(s));
    }
  }
}
