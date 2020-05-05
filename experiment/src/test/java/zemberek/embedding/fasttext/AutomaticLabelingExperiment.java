package zemberek.embedding.fasttext;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import zemberek.core.ScoredItem;
import zemberek.core.collections.Histogram;
import zemberek.core.embeddings.Args;
import zemberek.core.embeddings.FastText;
import zemberek.core.embeddings.FastTextTrainer;
import zemberek.core.logging.Log;
import zemberek.core.turkish.Turkish;
import zemberek.corpus.WebCorpus;
import zemberek.corpus.WebDocument;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class AutomaticLabelingExperiment {

  TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;
  private Path experimentRoot;
  private Path rawCorpusRoot;
  TurkishMorphology morphology ;

  private AutomaticLabelingExperiment(Path experimentRoot, Path rawCorpusRoot) throws IOException {
    this.experimentRoot = experimentRoot;
    this.rawCorpusRoot = rawCorpusRoot;
    this.morphology = TurkishMorphology.createWithDefaults();
  }

  /**
   * run with -Xmx8G or more.
   */
  public static void main(String[] args) throws Exception {

    Path expRoot = Paths.get("/media/data/corpora/label-exp");

    new AutomaticLabelingExperiment(
        expRoot,
        expRoot.resolve("raw-data")).runExperiment();
  }

  static void saveSets(Path train, Path test, Set<String> set) throws IOException {

    String[] sources = {"cnnturk.com", "dunya.com", "iha.com", "ntv.com", "t24.com",
        "yenisafak.com"};
    List<String> testSet = new ArrayList<>();
    for (String source : sources) {
      int i = 0;
      for (String s : set) {
        if (s.contains(source)) {
          testSet.add(s);
          i++;
          if (i == 1000) {
            break;
          }
        }
      }
    }

    for (String s : testSet) {
      set.remove(s);
    }

    try (PrintWriter pwTrain = new PrintWriter(train.toFile(), "utf-8");
        PrintWriter pwTest = new PrintWriter(test.toFile(), "utf-8")) {
      for (String s : testSet) {
        pwTest.println(s);
      }
      for (String s : set) {
        pwTrain.println(s);
      }
    }
    Log.info("There are %d samples in test set, %d samples in training set.", testSet.size(),
        set.size());

  }

  public void runExperiment() throws Exception {

    Path corpusPath = experimentRoot.resolve("label.corpus");
    Path trainData = experimentRoot.resolve("labels.train");
    Path testData = experimentRoot.resolve("labels.test");
    Path modelPath = experimentRoot.resolve("labels.model");
    Path predictionPath = experimentRoot.resolve("labels.prediction");

    //extractLabeledDocuments(rawCorpusRoot, corpusPath);
    Set<String> set = generateSetForLabelExperiment(corpusPath, morphology, true);
    saveSets(trainData, testData, set);
    FastText fastText = getOrTrainFastText(trainData, modelPath);
    test(corpusPath, testData, predictionPath, fastText);
  }

  private void test(
      Path corpusPath,
      Path testData,
      Path predictionPath,
      FastText fastText) throws IOException {
    WebCorpus corpus = new WebCorpus("label", "label");
    corpus.addDocuments(WebCorpus.loadDocuments(corpusPath));

    Log.info("Testing started.");
    List<String> testLines = Files.readAllLines(testData, StandardCharsets.UTF_8);
    Stopwatch sw = Stopwatch.createStarted();
    try (PrintWriter pw = new PrintWriter(predictionPath.toFile(), "utf-8")) {
      for (String testLine : testLines) {
        String id = testLine.substring(0, testLine.indexOf(' ')).substring(1);
        WebDocument doc = corpus.getDocument(id);
        List<ScoredItem<String>> res = fastText.predict(testLine, 7);
        List<String> predictedLabels = new ArrayList<>();
        for (ScoredItem<String> re : res) {
          predictedLabels.add(String.format(Locale.ENGLISH, "%s (%.2f)",
              re.item.replaceAll("__label__", "").replaceAll("_", " "), re.score));
        }

        pw.println("id = " + id);
        pw.println();
        pw.println(doc.getContentAsString().replaceAll("[\n\r]+", "\n"));
        pw.println();
        pw.println("Actual Labels = " + String.join(", ", doc.getLabels()));
        pw.println("Predictions   = " + String.join(", ", predictedLabels));
        pw.println();
        pw.println("------------------------------------------------------");
        pw.println();
      }
    }
    Log.info("Done. in %d ms.", sw.elapsed(TimeUnit.MILLISECONDS));
  }

  private FastText getOrTrainFastText(Path train, Path modelPath) throws Exception {
    FastText fastText;
    if (modelPath.toFile().exists()) {
      fastText = FastText.load(modelPath);
    } else {
      Args argz = Args.forSupervised();
      argz.thread = 16;
      argz.loss = Args.loss_name.hierarchicalSoftmax;
      argz.epoch = 100;
      argz.wordNgrams = 2;
      argz.minCount = 10;
      argz.lr = 0.2;
      argz.dim = 250;
      argz.bucket = 7_000_000;

      fastText = new FastTextTrainer(argz).train(train);
      fastText.saveModel(modelPath);
    }
    return fastText;
  }

  Set<String> generateSetForLabelExperiment(
      Path input,
      TurkishMorphology analyzer,
      boolean useRoots) throws IOException {
    WebCorpus corpus = new WebCorpus("label", "labeled");
    corpus.addDocuments(WebCorpus.loadDocuments(input));
    List<String> set = new ArrayList<>(corpus.documentCount());

    Log.info("Extracting data.");

    Histogram<String> labelCounts = new Histogram<>();
    for (WebDocument document : corpus.getDocuments()) {
      List<String> labels = document.getLabels();
      List<String> lowerCase = labels.stream().
          filter(s -> s.length() > 1)
          .map(s -> s.toLowerCase(Turkish.LOCALE)).collect(Collectors.toList());
      labelCounts.add(lowerCase);
    }

    labelCounts.saveSortedByCounts(experimentRoot.resolve("labels-all"), " ");

    Log.info("All label count = %d", labelCounts.size());
    labelCounts.removeSmaller(15);
    Log.info("Reduced label count = %d", labelCounts.size());
    labelCounts.saveSortedByCounts(experimentRoot.resolve("labels-reduced"), " ");
    Log.info("Extracting data from %d documents ", corpus.documentCount());
    int c = 0;

    Set<Long> contentHash = new HashSet<>();

    for (WebDocument document : corpus.getDocuments()) {
      Long hash = document.getHash();
      if (contentHash.contains(hash)) {
        continue;
      }
      contentHash.add(hash);

      List<String> labelTags = new ArrayList<>();

      boolean labelFound = false;
      for (String label : document.getLabels()) {
        if (labelCounts.contains(label)) {
          labelTags.add("__label__" + label.replaceAll("[ ]+", "_").toLowerCase(Turkish.LOCALE));
          labelFound = true;
        }
      }
      if (!labelFound) {
        continue;
      }
      String labelStr = String.join(" ", labelTags);

      String content = document.getContentAsString();
      String processed = processContent(morphology, content, useRoots);
      if (processed.length() < 200) {
        continue;
      }
      set.add("#" + document.getId() + " " + labelStr + " " + processed);

      if (c++ % 1000 == 0) {
        Log.info("%d processed.", c);
      }
    }
    Log.info("Generate train and test set.");
    Collections.shuffle(set, new Random(1));
    return new LinkedHashSet<>(set);
  }

  public String processContent(TurkishMorphology analyzer, String content, boolean useRoots) {
    List<Token> docTokens = lexer.tokenize(content);

    List<String> reduced = new ArrayList<>(docTokens.size());
    for (Token token : docTokens) {
      if (token.getType() == Token.Type.PercentNumeral ||
          token.getType() == Token.Type.Number ||
          token.getType() == Token.Type.Punctuation ||
          token.getType() == Token.Type.RomanNumeral ||
          token.getType() == Token.Type.Time ||
          token.getType() == Token.Type.UnknownWord ||
          token.getType() == Token.Type.Unknown) {
        continue;
      }
      String tokenStr = token.getText();
      reduced.add(tokenStr);
    }
    String joined = String.join(" ", reduced);
    if (useRoots) {
      SentenceAnalysis analysis = analyzer.analyzeAndDisambiguate(joined);
      List<String> res = new ArrayList<>();
      for (SentenceWordAnalysis e : analysis) {
        SingleAnalysis best = e.getBestAnalysis();
        if (best.isUnknown()) {
          res.add(e.getWordAnalysis().getInput());
          continue;
        }
        List<String> lemmas = best.getLemmas();
        if (lemmas.size() == 0) {
          continue;
        }
        res.add(lemmas.get(lemmas.size() - 1));
      }
      joined = String.join(" ", res);
    }
    return joined.replaceAll("[']", "").toLowerCase(Turkish.LOCALE);
  }

  private void extractLabeledDocuments(Path root, Path labeledFile) throws IOException {
    List<Path> files = Files.walk(root).filter(s -> s.toFile().isFile())
        .collect(Collectors.toList());
    files.sort(Comparator.comparing(Path::toString));
    WebCorpus corpus = new WebCorpus("label", "label");
    for (Path file : files) {
      if (file.toFile().isDirectory()) {
        continue;
      }
      Log.info("Adding %s", file);
      List<WebDocument> doc = WebCorpus.loadDocuments(file);
      List<WebDocument> labeled = doc.stream()
          .filter(s -> s.getLabels().size() > 0 && s.getContentAsString().length() > 200)
          .collect(Collectors.toList());
      corpus.addDocuments(labeled);
    }
    Log.info("Total amount of files = %d", corpus.getDocuments().size());
    WebCorpus noDuplicates = corpus.copyNoDuplicates();
    Log.info("Corpus size = %d, After removing duplicates = %d",
        corpus.documentCount(),
        noDuplicates.documentCount());
    Log.info("Saving corpus to %s", labeledFile);
    noDuplicates.save(labeledFile, false);
  }
}
