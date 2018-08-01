package zemberek.examples.classification;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.apps.fasttext.TrainClassifier;
import zemberek.classification.FastTextClassifier;
import zemberek.core.ScoredItem;
import zemberek.core.collections.Histogram;
import zemberek.core.embeddings.FastText.EvaluationResult;
import zemberek.core.logging.Log;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class NewsTitleCategoryFinder {

  TurkishMorphology morphology;

  public static void main(String[] args) throws IOException {
    NewsTitleCategoryFinder experiment = new NewsTitleCategoryFinder();
    // Download data set `news-title-category-set`
    // from https://drive.google.com/drive/folders/1JBPExAeRctAXL2oGW2U6CbqfwIJ84BG7
    String set = "/home/ahmetaa/data/zemberek/news-title-category-set";

    Path dataPath = Paths.get(set);
    Path root = dataPath.getParent();
    List<String> lines = Files.readAllLines(dataPath, StandardCharsets.UTF_8);
    String name = dataPath.toFile().getName();
    experiment.dataInfo(lines);
    Log.info("------------ Evaluation with raw data ------------------");
    experiment.evaluate(dataPath, 1000);

    Path tokenizedPath = root.resolve(name + ".tokenized");
    Log.info("------------ Evaluation with tokenized - lowercase data ------------");
    experiment.generateSetTokenized(lines, tokenizedPath);
    experiment.evaluate(tokenizedPath, 1000);

    Path lemmasPath = root.resolve(name + ".lemmas");
    Log.info("------------ Evaluation with lemma - lowercase data ------------");
    if (!lemmasPath.toFile().exists()) {
      experiment.generateSetWithLemmas(lines, lemmasPath);
    }
    experiment.evaluate(lemmasPath, 1000);

  }

  private void generateSetWithLemmas(List<String> lines, Path lemmasPath) throws IOException {
    morphology = TurkishMorphology.createWithDefaults();

    List<String> lemmas = lines
        .stream()
        .map(this::replaceWordsWithLemma)
        .map(this::removeNonWords)
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(lemmasPath, lemmas);
  }

  private void generateSetTokenized(List<String> lines, Path tokenizedPath) throws IOException {
    List<String> tokenized = lines
        .stream()
        .map(s -> String.join(" ", TurkishTokenizer.DEFAULT.tokenizeToStrings(s)))
        .map(this::removeNonWords)
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(tokenizedPath, tokenized);
  }


  private String replaceWordsWithLemma(String sentence) {

    List<String> tokens = Splitter.on(" ").splitToList(sentence);
    // assume first is label. Remove label from sentence for morphological analysis.
    String label = tokens.get(0);
    tokens = tokens.subList(1, tokens.size());
    sentence = String.join(" ", tokens);

    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);
    List<String> res = new ArrayList<>();
    // add label first.
    res.add(label);
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

  private String removeNonWords(String sentence) {
    List<Token> docTokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    List<String> reduced = new ArrayList<>(docTokens.size());
    for (Token token : docTokens) {
      if (
          token.getType() == TurkishLexer.PercentNumeral ||
              token.getType() == TurkishLexer.Number ||
              token.getType() == TurkishLexer.Punctuation ||
              token.getType() == TurkishLexer.RomanNumeral ||
              token.getType() == TurkishLexer.Time ||
              token.getType() == TurkishLexer.UnknownWord ||
              token.getType() == TurkishLexer.Unknown) {
        if (!token.getText().contains("__")) {
          continue;
        }
      }
      String tokenStr = token.getText();
      reduced.add(tokenStr);
    }
    return String.join(" ", reduced);
  }

  private void evaluate(Path set, int testSize) throws IOException {

    // Create training and test sets.
    List<String> lines = Files.readAllLines(set, StandardCharsets.UTF_8);
    Path root = set.getParent();
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
          "--epochCount", "50",
          "--wordNGrams", "2",
          "--applyQuantization",
          "--cutOff", "15000"
      );
    }
    Log.info("Testing...");
    test(testPath, root.resolve(name + ".predictions"), modelPath);
    // test quantized models.
    Log.info("Testing with quantized model...");
    test(testPath, root.resolve(name + ".predictions.q"), root.resolve(name + ".model.q"));
  }

  private void test(Path testPath, Path predictionsPath, Path modelPath) throws IOException {
    FastTextClassifier classifier = FastTextClassifier.load(modelPath);

    EvaluationResult result = classifier.evaluate(testPath, 1);
    Log.info(result.toString());

    List<String> testLines = Files.readAllLines(testPath, StandardCharsets.UTF_8);
    try (PrintWriter pw = new PrintWriter(predictionsPath.toFile(), "utf-8")) {
      for (String testLine : testLines) {
        List<ScoredItem<String>> res = classifier.predict(testLine, 3);
        List<String> predictedCategories = new ArrayList<>();
        for (ScoredItem<String> re : res) {
          predictedCategories.add(String.format("%s (%.6f)",
              re.item.replaceAll("__label__", "").replaceAll("_", " "), re.score));
        }
        pw.println(testLine);
        pw.println("Predictions   = " + String.join(", ", predictedCategories));
        pw.println();
      }
    }
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
