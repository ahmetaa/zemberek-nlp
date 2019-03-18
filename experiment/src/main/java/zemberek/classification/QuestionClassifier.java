package zemberek.classification;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import zemberek.apps.fasttext.EvaluateClassifier;
import zemberek.apps.fasttext.TrainClassifier;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;

public class QuestionClassifier {

  public static final int TEST_SIZE = 5000;

  private static void collectCoarseData() throws IOException {

    Path root = Paths.get("/media/ahmetaa/depo/corpora/open-subtitles-tr-2018-small");
    BlockTextLoader corpusProvider = BlockTextLoader.fromDirectory(root);

    LinkedHashSet<String> questions = new LinkedHashSet<>();
    LinkedHashSet<String> notQuestions = new LinkedHashSet<>();

    Random rnd = new Random();

    int quesCount = 200_000;
    int noQuesCount = 300_000;

    for (TextChunk chunk : corpusProvider) {
      for (String line : chunk) {
        if (line.length() > 80) {
          continue;
        }
        if (line.endsWith("?") && questions.size() < quesCount) {
          int r = rnd.nextInt(3);
          if (r < 2) {
            questions.add("__label__question " + line);
          } else {
            questions.add("__label__question " + line.replaceAll("\\?", "").trim());
          }
        } else {
          if (notQuestions.size() < noQuesCount) {
            notQuestions.add("__label__not_question " + line);
          }
        }
      }
      if (questions.size() == quesCount && notQuestions.size() == noQuesCount) {
        break;
      }
    }

    Path outQ = Paths.get("/media/ahmetaa/depo/classification/question/coarse/questions-raw");
    Files.write(outQ, questions, StandardCharsets.UTF_8);
    Path outNotQ = Paths
        .get("/media/ahmetaa/depo/classification/question/coarse/not_questions-raw");
    Files.write(outNotQ, questions, StandardCharsets.UTF_8);

    List<String> all = new ArrayList<>(questions);
    all.addAll(notQuestions);
    Collections.shuffle(all);
    Path allData = Paths.get("/media/ahmetaa/depo/classification/question/coarse/all-raw");
    Files.write(allData, all, StandardCharsets.UTF_8);
  }

  TurkishMorphology morphology;

  public static void main(String[] args) throws IOException {

    collectCoarseData();

    QuestionClassifier experiment = new QuestionClassifier();
    String set = "/media/ahmetaa/depo/classification/question/coarse/all-raw";

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

    if (sentence.length() == 0) {
      return sentence;
    }
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

  private String splitWords(String sentence) {

    List<String> tokens = Splitter.on(" ").splitToList(sentence);
    // assume first is label. Remove label from sentence for morphological analysis.
    String label = tokens.get(0);
    tokens = tokens.subList(1, tokens.size());
    sentence = String.join(" ", tokens);

    if (sentence.length() == 0) {
      return sentence;
    }
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
      if (!token.getText().equals("?") && (
          token.getType() == Type.PercentNumeral ||
              token.getType() == Token.Type.Number ||
              token.getType() == Token.Type.Punctuation ||
              token.getType() == Token.Type.RomanNumeral ||
              token.getType() == Token.Type.Time ||
              token.getType() == Token.Type.UnknownWord ||
              token.getType() == Token.Type.Unknown)) {
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
          "--epochCount", "50",
          "--wordNGrams", "2",
          "--applyQuantization",
          "--cutOff", "25000"
      );
    }
    Log.info("Testing...");
    test(testPath, root.resolve(name + ".predictions"), modelPath);
    // test quantized models.
    Log.info("Testing with quantized model...");
    test(testPath, root.resolve(name + ".predictions.q"), root.resolve(name + ".model.q"));
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
