package zemberek.classification;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import zemberek.apps.fasttext.EvaluateClassifier;
import zemberek.apps.fasttext.TrainClassifier;
import zemberek.core.ScoredItem;
import zemberek.core.embeddings.FastText.EvaluationResult;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.text.TextIO;

public class ItemFindExperiment {

  public static void main(String[] args) throws IOException {

    Path root = Paths.get("/home/aaa/data/foo");
    Path none = Paths.get("/home/aaa/data/foo/none-utf8.txt");
    Path test = Paths.get("/home/aaa/data/foo/test");

    Path courpusRaw = Paths.get("/home/aaa/data/foo/es50k.tok");
    Path corpus = Paths.get("/home/aaa/data/foo/es50k-processed");

    processCorpus(courpusRaw, corpus);
    Path labeled = Paths.get("/home/aaa/data/foo/true.txt");
    Path train = Paths.get("/home/aaa/data/foo/train.txt");

    generateTraining(labeled, corpus, 30000, train);
    createTestSet(none, labeled, test);

    trainModel(root, train, "foo");
    //testModel(root, test, "foo");

    testGrams(root, test, "__label__rec_notice", "foo");
  }

  static void createTestSet(Path p, Path labeled, Path out) throws IOException {
    List<String> allNone = TextIO.loadLines(p);
    allNone = allNone.stream().map(s -> s.replaceAll("[(].+?[)]", " ")
        .replaceAll("\\s+", " ").trim())
        .collect(Collectors.toList());
    List<String> test = new ArrayList<>();
    for (String s : allNone) {
      test.add("__label__none " + s);
    }

    Random rnd = new Random(2345);

    List<String> allLabeled = TextIO.loadLines(labeled);
    for (String s : allNone) {
      ArrayList<String> tokens = new ArrayList<>(Splitter.on(" ").splitToList(s));
      String rndLine = allLabeled.get(rnd.nextInt(allLabeled.size()));
      tokens.add(rnd.nextInt(tokens.size()), rndLine);
      test.add("__label__rec_notice " + String.join(" ", tokens));
    }

    Files.write(out, test);
  }

  static void testGrams(Path root, Path test, String labelTarget, String name) throws IOException {

    Path modelPath = root.resolve(name + ".model");
    Path predictions = root.resolve(name + ".predictions");

    FastTextClassifier classifier = FastTextClassifier.load(modelPath);

    try (PrintWriter pw = new PrintWriter(predictions.toFile(), "utf-8")) {

      List<String> all = Files.readAllLines(test);
      for (String s : all) {
        List<String> tokens = Splitter.on(" ").splitToList(s);
        List<String> rest = tokens.subList(1, tokens.size());

        List<String> grams = getGrams(rest, 7);
        List<Hit> hits = new ArrayList<>();
        for (String gram : grams) {
          List<ScoredItem<String>> res = classifier.predict(gram, 2);
          for (ScoredItem<String> re : res) {
            float p = (float) Math.exp(re.score);
            if (re.item.equals(labelTarget) && p > 0.45) {
              hits.add(new Hit(gram, re));
            }
          }
        }

        pw.println(s);
        for (Hit hit : hits) {
          pw.println(hit);
        }
        pw.println("-----------------------");

      }
    }
  }

  static class Hit {

    String ngram;
    ScoredItem<String> item;

    public Hit(String ngram, ScoredItem<String> item) {
      this.ngram = ngram;
      this.item = new ScoredItem<>(item.item, (float)Math.exp(item.score));
    }

    @Override
    public String toString() {
      return ngram + " " + item.item.replace("__label__", "") + " " + item.score;
    }
  }

  static List<String> getGrams(List<String> tokens, int gram) {
    if (gram > tokens.size()) {
      String s = String.join(" ", tokens);
      return Lists.newArrayList(s);
    }
    List<String> result = new ArrayList<>();
    for (int i = 0; i < tokens.size() - gram; i++) {
      List<String> g = tokens.subList(i, i + gram);
      result.add(String.join(" ", g));
    }
    return result;
  }


  static void trainModel(Path root, Path train, String name) {
    Path modelPath = root.resolve(name + ".model");
    if (!modelPath.toFile().exists()) {
      new TrainClassifier().execute(
          "-i", train.toString(),
          "-o", modelPath.toString(),
          "--learningRate", "0.1",
          "--epochCount", "70",
          "--dimension", "100",
          "--wordNGrams", "2" /*,
          "--applyQuantization",
          "--cutOff", "25000"*/
      );
    }
  }

  static void testModel(Path root, Path test, String name) {
    Log.info("Testing...");
    Path modelPath = root.resolve(name + ".model");
    Path predictions = root.resolve(name + ".predictions");
    new EvaluateClassifier().execute(
        "-i", test.toString(),
        "-m", modelPath.toString(),
        "-o", predictions.toString(),
        "-k", "1"
    );
  }


  static void generateTraining(Path labeled, Path junk, int junkCount, Path out)
      throws IOException {
    Random rnd = new Random(1234);
    List<String> allTrue = TextIO.loadLines(labeled);
    List<String> junkAll = TextIO.loadLines(junk);
    if (junkCount > junkAll.size()) {
      junkCount = junkAll.size();
    }
    Collections.shuffle(junkAll, rnd);
    List<String> junkLabeled = new ArrayList<>(junkAll.subList(0, junkCount));

    List<String> set = new ArrayList<>();
    for (String s : junkLabeled) {
      set.add("__label__none " + s);
    }

    for (int i = 0; i < 5; i++) {
      for (String s : allTrue) {
        set.add("__label__rec_notice " + s);
      }
    }

    Collections.shuffle(set, rnd);
    Files.write(out, set);
  }

  static Locale es = new Locale("es", "ES");

  static void processCorpus(Path in, Path out)
      throws IOException {
    BlockTextLoader loader = BlockTextLoader.fromPath(in, 10000);
    try (PrintWriter pw = new PrintWriter(out.toFile(), "utf-8")) {
      for (TextChunk chunk : loader) {
        LinkedHashSet<String> unique = new LinkedHashSet<>(chunk.getData());
        for (String l : unique) {
          if (!Strings.containsNone(l, "[]#~|")) {
            continue;
          }
          l = l.toLowerCase(es).replaceAll("[^0-9a-zñáéíóúü]", " ")
              .replaceAll("\\s+", " ").trim();
          if (l.length() == 0) {
            continue;
          }
          if (l.length() < 20) {
            continue;
          }
          pw.println(l);
        }
      }
    }
  }


}
