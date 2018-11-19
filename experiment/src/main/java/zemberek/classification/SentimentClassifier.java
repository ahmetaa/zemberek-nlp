package zemberek.classification;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.apps.fasttext.EvaluateClassifier;
import zemberek.apps.fasttext.TrainClassifier;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class SentimentClassifier {

  static TurkishMorphology morphology;
  static TurkishSentenceNormalizer normalizer;

  static Path root = Paths.get("/media/ahmetaa/depo/sentiment");
  static Path t1out = root.resolve("t1");
  static Path trainRaw = t1out.resolve("train-raw");
  static Path testRaw = t1out.resolve("test-raw");


  public static void main(String[] args) throws IOException {

    SentimentClassifier experiment = new SentimentClassifier();
    morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .build();

    Path dataRoot = Paths.get("/home/ahmetaa/zemberek-data");
    normalizer = new TurkishSentenceNormalizer(
        morphology,
        dataRoot.resolve("normalization"),
        dataRoot.resolve("lm/lm.2gram.slm"));

    experiment.generateData();

    List<String> trainRawLines = TextIO.loadLines(trainRaw);
    List<String> testRawLines = TextIO.loadLines(testRaw);

    Log.info("Train data:");
    experiment.dataInfo(trainRawLines);
    Log.info("Test data:");
    experiment.dataInfo(testRawLines);

    Path tokenizedTrain = t1out.resolve("train-tokenized");
    Path tokenizedTest = t1out.resolve("test-tokenized");

    experiment.generateSetTokenized(trainRawLines, tokenizedTrain);
    experiment.generateSetTokenized(testRawLines, tokenizedTest);

    experiment.evaluate(t1out, tokenizedTrain, tokenizedTest, "tokenized");

    Path lemmaTrain = t1out.resolve("train-lemma");
    Path lemmaTest = t1out.resolve("test-lemma");

    experiment.generateSetWithLemmas(trainRawLines, lemmaTrain);
    experiment.generateSetWithLemmas(testRawLines, lemmaTest);

    experiment.evaluate(t1out, lemmaTrain, lemmaTest, "lemma");

    Path splitTrain = t1out.resolve("train-split");
    Path splitTest = t1out.resolve("test-split");

    experiment.generateSetWithSplit(trainRawLines, splitTrain);
    experiment.generateSetWithSplit(testRawLines, splitTest);

    experiment.evaluate(t1out, splitTrain, splitTest, "split");

  }

  void generateData() throws IOException {
    Path t1Train = root.resolve("raw/t1/train.csv");
    Path t1Test = root.resolve("raw/t1/test.csv");
    Files.createDirectories(t1out);
    Files.write(trainRaw, addLabels(t1Train));
    Files.write(testRaw, addLabels(t1Test));
  }

  static List<String> addLabels(Path input) throws IOException {
    List<String> lines = TextIO.loadLines(input);
    List<String> result = new ArrayList<>();
    for (String line : lines) {
      int i = line.indexOf('\t');
      if (i == -1) {
        continue;
      }
      String content = line.substring(0, i).trim();
      content = normalizer.normalize(content);
      String label = "__label__" + line.substring(i).trim();
      result.add(label + " " + content);
    }
    return result;
  }

  private void generateSetWithLemmas(List<String> lines, Path lemmasPath) throws IOException {
    List<String> lemmas = lines
        .stream()
        .map(this::replaceWordsWithLemma)
        .map(this::removeNonWords)
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(lemmasPath, lemmas);
  }

  private void generateSetWithSplit(List<String> lines, Path splitPath) throws IOException {
    List<String> lemmas = lines
        .stream()
        .map(this::splitWords)
        .map(this::removeNonWords)
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(splitPath, lemmas);
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
      String input = e.getWordAnalysis().getInput();
      if (best.isUnknown()) {
        res.add(input);
        continue;
      }
      List<String> lemmas = best.getLemmas();
      String l = lemmas.get(lemmas.size() - 1);
      if (l.length() < input.length()) {
        res.add(l);
        res.add("_" + input.substring(l.length()));
      } else {
        res.add(l);
      }
    }
    return String.join(" ", res);
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

  private String removeNonWords(String sentence) {
    List<Token> docTokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    List<String> reduced = new ArrayList<>(docTokens.size());
    for (Token token : docTokens) {
      String tokenStr = token.getText();

      if (tokenStr.startsWith("_")) {
        reduced.add(tokenStr);
        continue;
      }
      if (
          token.getType() == TurkishLexer.PercentNumeral ||
              token.getType() == TurkishLexer.Number ||
              token.getType() == TurkishLexer.Mention ||
              token.getType() == TurkishLexer.HashTag ||
              token.getType() == TurkishLexer.Punctuation ||
              token.getType() == TurkishLexer.RomanNumeral ||
              token.getType() == TurkishLexer.Time ||
              token.getType() == TurkishLexer.UnknownWord ||
              token.getType() == TurkishLexer.Unknown) {
        if (!token.getText().contains("__")) {
          continue;
        }
      }
      reduced.add(tokenStr);
    }
    return String.join(" ", reduced);
  }

  private void evaluate(Path root, Path train, Path test, String name) {

    //Create model if it does not exist.
    Path modelPath = root.resolve(name + ".model");
    if (!modelPath.toFile().exists()) {
      new TrainClassifier().execute(
          "-i", train.toString(),
          "-o", modelPath.toString(),
          "--learningRate", "0.1",
          "--epochCount", "70",
          "--dimension", "100",
          "--wordNGrams", "3"/*,
          "--applyQuantization",
          "--cutOff", "25000"*/
      );
    }
    Log.info("Testing...");
    test(test, root.resolve(name + ".predictions"), modelPath);
    // test quantized models.
/*
    Log.info("Testing with quantized model...");
    test(test, root.resolve(name + ".predictions.q"), root.resolve(name + ".model.q"));
*/
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

