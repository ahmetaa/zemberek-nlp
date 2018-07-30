package zemberek.examples.fasttext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.apps.fasttext.TrainFastTextClassifier;
import zemberek.core.collections.Histogram;
import zemberek.core.embeddings.FastText;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.TurkishTokenizer;

public class NewsTitleCategoryFinder {

  TurkishMorphology morphology;

  public static void main(String[] args) throws IOException {
    NewsTitleCategoryFinder experiment = new NewsTitleCategoryFinder();
    String set = "/media/ahmetaa/depo/zemberek/data/classification/news-title-category-set";
    Path dataPath = Paths.get(set);
    experiment.preprocess(dataPath);
  }

  public void preprocess(Path dataPath) throws IOException {

    Path root = dataPath.getParent();
    int testSize = 1000;
    evaluate(dataPath, testSize);

    List<String> lines = Files.readAllLines(dataPath, StandardCharsets.UTF_8);

    // creates a tokenized set
    Path tokenizedPath = root.resolve(dataPath.toFile().getName() + ".tokenized");
    List<String> tokenized = lines
        .stream()
        .map(s -> String.join(" ", TurkishTokenizer.DEFAULT.tokenizeToStrings(s)))
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(tokenizedPath, tokenized);

    evaluate(tokenizedPath, testSize);

    // creates a set containin only lemmas.
    morphology = TurkishMorphology.createWithDefaults();
    Path lemmasPath = root.resolve(dataPath.toFile().getName() + ".lemmas");
    List<String> lemmas = lines
        .stream()
        .map(this::replaceWordsWithLemma)
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(lemmasPath, lemmas);

    evaluate(lemmasPath, testSize);

  }

  private String replaceWordsWithLemma(String sentence) {
    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);
    List<String> res = new ArrayList<>();
    for (SentenceWordAnalysis e : analysis) {
      SingleAnalysis best = e.getBestAnalysis();
      if (best.isUnknown()) {
        res.add(e.getWordAnalysis().getInput());
        continue;
      }
      List<String> lemmas = best.getLemmas();
      res.add(lemmas.get(lemmas.size() - 1));
    }
    return String.join(" ", res);
  }

  private void evaluate(Path set, int testSize) throws IOException {

    // Create training and test sets.
    List<String> lines = Files.readAllLines(set, StandardCharsets.UTF_8);
    Path root = set.getParent();
    String name = set.toFile().getName();

    Path train = root.resolve(name + ".train");
    Path test = root.resolve(name + ".test");

    Files.write(train, lines.subList(testSize, lines.size()));
    Files.write(test, lines.subList(0, testSize));

    //Create model
    Path modelPath = root.resolve(name + ".model");
    new TrainFastTextClassifier().execute(
        "-i", train.toString(), "-o", modelPath.toString()
    );

    FastText fastText = FastText.load(modelPath);
    fastText.test(test, 1);
  }

  void dataInfo(List<String> lines) {
    System.out.println("Total lines = " + lines.size());
    Histogram<String> hist = new Histogram<>();
    lines.stream()
        .map(s -> s.substring(0, s.indexOf(' ')))
        .forEach(hist::add);
    System.out.println("Categories :");
    for (String s : hist.getSortedList()) {
      System.out.println(s + " " + hist.getCount(s));
    }
  }


}
