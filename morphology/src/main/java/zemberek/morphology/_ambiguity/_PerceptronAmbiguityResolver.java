package zemberek.morphology._ambiguity;

import static java.lang.String.format;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.morphology.ambiguity.AbstractDisambiguator;

public class _PerceptronAmbiguityResolver extends AbstractDisambiguator {


  Model weights = new Model();
  Model averagedWeights;
  IntValueMap<String> counts = new IntValueMap<>();

  Random random = new Random(1);

  public _PerceptronAmbiguityResolver(File modelFile) throws IOException {
    this.averagedWeights = Model.loadFromTextFile(modelFile);
  }

  private _PerceptronAmbiguityResolver() {
    this.averagedWeights = new Model(new FloatValueMap<>(10000));
  }

  static void train(File trainFile, File devFile, File modelFile) throws IOException {
    _PerceptronAmbiguityResolver disambiguator = new _PerceptronAmbiguityResolver();
    DataSet trainingSet = com.google.common.io.Files
        .readLines(trainFile, Charsets.UTF_8, new DataSetLoader());
    int numExamples = 0;
    for (int i = 0; i < 10; i++) {
      System.out.println("Iteration:" + i);
      for (SentenceData sentence : trainingSet) {
        numExamples++;
        ParseResult result = disambiguator._bestParse(sentence);
        if (sentence.correctParse.equals(result.bestParse)) {
          continue;
        }
        IntValueMap<String> correctFeatures = disambiguator.extractFeatures(sentence.correctParse);
        IntValueMap<String> bestFeatures = disambiguator.extractFeatures(result.bestParse);
        disambiguator.updateWeights(correctFeatures, bestFeatures, numExamples);
      }
      for (String key : disambiguator.averagedWeights) {
        disambiguator.updateAverageWeights(numExamples, key);
      }
      Log.info("Testing development set.");
      disambiguator.test(devFile);
    }
    disambiguator.averagedWeights.saveAsText(modelFile);
  }

  public static void main(String[] args) throws IOException {
    Path train = Paths.get("/home/ahmetaa/Downloads/MD-Release/MD-Release/data.train.txt");
    //Path train = Paths.get("/home/ahmetaa/Downloads/MD-Release/MD-Release/tiny.txt");
    //Path dev = Paths.get("/home/ahmetaa/Downloads/MD-Release/MD-Release/tiny.txt");
    Path dev = Paths.get("/home/ahmetaa/Downloads/MD-Release/MD-Release/data.dev.txt");
    Path model = Paths.get("/home/ahmetaa/Downloads/MD-Release/MD-Release/model");
    Path test = Paths.get("/home/ahmetaa/Downloads/MD-Release/MD-Release/data.test.txt");
    //Path test = Paths.get("/home/ahmetaa/Downloads/MD-Release/MD-Release/test.merge");
    _PerceptronAmbiguityResolver.train(train.toFile(), dev.toFile(), model.toFile());
    new _PerceptronAmbiguityResolver(model.toFile()).test(test.toFile());
  }

  public void test(File testFile) throws IOException {
    DataSet testSet = com.google.common.io.Files
        .readLines(testFile, Charsets.UTF_8, new DataSetLoader());
    int hit = 0, total = 0;
    Stopwatch sw = Stopwatch.createStarted();
    for (SentenceData sentence : testSet.sentences) {
      ParseResult result = _bestParse(sentence);
      int i = 0;
      for (String best : result.bestParse) {
        if (sentence.correctParse.get(i).equals(best)) {
          hit++;
        }
        total++;
        i++;
      }
    }
    Log.info("Elapsed: " + sw.elapsed(TimeUnit.MILLISECONDS));
    Log.info(
        "Word count:" + total + " hit=" + hit + String.format(" Accuracy:%f", hit * 1.0 / total));
  }

  private void updateWeights(IntValueMap<String> correctFeatures, IntValueMap<String> bestFeatures,
      int numExamples) {
    Set<String> keySet = Sets.newHashSet();
    keySet.addAll(correctFeatures.getKeyList());
    keySet.addAll(bestFeatures.getKeyList());

    for (String feat : keySet) {
      updateAverageWeights(numExamples, feat);
      weights.increment(feat, (correctFeatures.get(feat) - bestFeatures.get(feat)));
      if (averagedWeights.weight(feat) == 0) {
        averagedWeights.data.remove(feat);
      }
      if (weights.weight(feat) == 0) {
        weights.data.remove(feat);
      }
    }
  }

  private void updateAverageWeights(int numExamples, String feat) {
    int featureCount = counts.get(feat);
    averagedWeights.put(
        feat,
        (averagedWeights.weight(feat) * featureCount + (numExamples - featureCount) *
            weights.weight(feat))
            / numExamples);
    counts.put(feat, numExamples);
  }

  IntValueMap<String> extractFeatures(List<String> parseSequence) {
    List<String> seq = Lists.newArrayList("<s>", "<s>");
    seq.addAll(parseSequence);
    seq.add("</s>");
    IntValueMap<String> featureModel = new IntValueMap<>();
    for (int i = 2; i < seq.size(); i++) {
      String[] trigram = {
          seq.get(i - 2),
          seq.get(i - 1),
          seq.get(i)};
      extractTrigramFeatures(trigram, featureModel);
    }
    return featureModel;
  }

  void extractTrigramFeatures(String[] trigram, IntValueMap<String> feats) {
    WordParse w1 = new WordParse(trigram[0]);
    WordParse w2 = new WordParse(trigram[1]);
    WordParse w3 = new WordParse(trigram[2]);
    String r1 = w1.root;
    String r2 = w2.root;
    String r3 = w3.root;
    String ig1 = w1.allIgs;
    String ig2 = w2.allIgs;
    String ig3 = w3.allIgs;

    feats.addOrIncrement(format("1:%s%s-%s%s-%s%s", r1, ig1, r2, ig2, r3, ig3));
    feats.addOrIncrement(format("2:%s%s-%s%s", r1, ig2, r3, ig3));
    feats.addOrIncrement(format("3:%s%s-%s%s", r2, ig2, r3, ig3));
    feats.addOrIncrement(format("4:%s%s", r3, ig3));
    //feats.addOrIncrement(format("5:%s%s-%s", r2, ig2, ig3));
    //feats.addOrIncrement(format("6:%s%s-%s", r1, ig1, ig3));

    //feats.addOrIncrement(format("7:%s-%s-%s", r1, r2, r3));
    //feats.addOrIncrement(format("8:%s-%s", r1, r3));
    feats.addOrIncrement(format("9:%s-%s", r2, r3));
    feats.addOrIncrement(format("10:%s", r3));

    //feats.addOrIncrement(format("11:%s-%s-%s", ig1, ig2, ig3));
    //feats.addOrIncrement(format("12:%s-%s", ig1, ig3));
    feats.addOrIncrement(format("13:%s-%s", ig2, ig3));
    feats.addOrIncrement(format("14:%s", ig3));

    String ig1s[] = ig1.split("[ ]");
    String ig2s[] = ig2.split("[ ]");
    String ig3s[] = ig3.split("[ ]");

    for (String ig : ig3s) {
      feats.addOrIncrement(format("15:%s-%s-%s", ig1s[ig1s.length - 1], ig2s[ig2s.length - 1], ig));
      //  feats.addOrIncrement(format("16:%s-%s", ig1s[ig1s.length - 1], ig));
      feats.addOrIncrement(format("17:%s-%s", ig2s[ig2s.length - 1], ig));
      //  feats.addOrIncrement(format("18:%s", ig));
    }

    //for (int k = 0; k < ig3s.length - 1; k++) {
    //  feats.addOrIncrement(format("19:%s-%s", ig3s[k], ig3s[k + 1]));
    //}

    for (int k = 0; k < ig3s.length; k++) {
      feats.addOrIncrement(format("20:%d-%s", k, ig3s[k]));
    }

    if (Character.isUpperCase(r3.charAt(0)) && w3.igs.contains("Prop")) {
      feats.addOrIncrement("21:PROPER");
    }

    feats.addOrIncrement(format("22:%d", ig3s.length));
    if (w3.all.contains(".+Punc") && w3.igs.contains("Verb")) {
      feats.addOrIncrement("23:ENDSVERB");
    }
  }

  static class Hypothesis {

    String prev;
    String current;
    Hypothesis previous;
    float score;

    public Hypothesis(String prev, String current, Hypothesis previous, float score) {
      this.prev = prev;
      this.current = current;
      this.previous = previous;
      this.score = score;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Hypothesis that = (Hypothesis) o;

      if (!prev.equals(that.prev)) {
        return false;
      }
      return current.equals(that.current);
    }

    @Override
    public int hashCode() {
      int result = prev.hashCode();
      result = 31 * result + current.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Hypothesis{" +
          "prev='" + prev + '\'' +
          ", current='" + current + '\'' +
          ", score=" + score +
          '}';
    }
  }

  /**
   * Calculates the best path using Viterbi decoding.
   *
   * @param sentence sentece with ambiguous wrods.
   * @return best parse sequence and its score.
   */
  ParseResult _bestParse(SentenceData sentence) {

    sentence.allWordAnalyses.add(WordData.SENTENCE_END);

    Hypothesis initialHypothesis = new Hypothesis("<s>", "<s>", null, 0);
    ActiveList currentList = new ActiveList();
    currentList.add(initialHypothesis);

    for (WordData wordAnalysis : sentence.allWordAnalyses) {

      ActiveList nextList = new ActiveList();

      for (String analysis : wordAnalysis.allParses) {

        for (Hypothesis h : currentList) {

          String[] trigram = {h.prev, h.current, analysis};
          IntValueMap<String> features = new IntValueMap<>();
          extractTrigramFeatures(trigram, features);

          float trigramScore = 0;
          for (String key : features) {
            trigramScore += (averagedWeights.weight(key) * features.get(key));
          }

          Hypothesis newHyp = new Hypothesis(h.current, analysis, h, h.score + trigramScore);
          nextList.add(newHyp);
        }
      }
      currentList = nextList;
    }

    Hypothesis best = currentList.getBest();
    LinkedList<String> result = Lists.newLinkedList();
    while (best.previous != null) {
      result.addFirst(best.current);
      best = best.previous;
    }
    result.removeLast();
    return new ParseResult(result, best.score);
  }

  static class Model implements Iterable<String> {

    FloatValueMap<String> data;

    Model(FloatValueMap<String> data) {
      this.data = data;
    }

    Model() {
      data = new FloatValueMap<>(10000);
    }

    public static Model loadFromTextFile(File file) throws IOException {
      FloatValueMap<String> data = new FloatValueMap<>(10000);
      List<String> all = SimpleTextReader.trimmingUTF8Reader(file).asStringList();
      for (String s : all) {
        float weight = Float.parseFloat(Strings.subStringUntilFirst(s, " "));
        String key = Strings.subStringAfterFirst(s, " ");
        data.set(key, weight);
      }
      Log.info("Model Loaded.");
      return new Model(data);
    }

    public void saveAsText(File file) throws IOException {
      SimpleTextWriter stw = SimpleTextWriter.keepOpenUTF8Writer(file);
      for (String s : data.getKeyList()) {
        stw.writeLine(data.get(s) + " " + s);
      }
      stw.close();
    }

    float weight(String key) {
      return data.get(key);
    }

    void put(String key, float value) {
      this.data.set(key, value);
    }

    float increment(String key, float value) {
      return data.incrementByAmount(key, value);
    }

    @Override
    public Iterator<String> iterator() {
      return data.iterator();
    }

  }

  private static class ParseResult {

    List<String> bestParse;
    float score;

    private ParseResult(LinkedList<String> bestParse, float score) {
      this.bestParse = bestParse;
      this.score = score;
    }
  }


  static class ActiveList implements Iterable<Hypothesis> {

    public static float DEFAULT_LOAD_FACTOR = 0.7f;

    public static int DEFAULT_INITIAL_CAPACITY = 8;

    private Hypothesis[] hypotheses;
    private int capacity;

    private int modulo;
    private int size;
    private int expandLimit;

    ActiveList() {
      this(DEFAULT_INITIAL_CAPACITY);
    }

    ActiveList(int size) {
      if (size < 1) {
        throw new IllegalArgumentException("Size must be a positive value. But it is " + size);
      }
      int k = 1;
      while (k < size) {
        k <<= 1;
      }
      hypotheses = new Hypothesis[k];
      expandLimit = (int) (k * DEFAULT_LOAD_FACTOR);
      modulo = k - 1;
    }

    private int firstProbe(int hashCode) {
      return hashCode & modulo;
    }

    private int nextProbe(int index) {
      return index & modulo;
    }

    /**
     * Finds either an empty slot location in Hypotheses array or the location of an equivalent Hypothesis.
     * If an empty slot is found, it returns -(slot index)-1, if an equivalent Hypotheses is found, returns
     * equal hypothesis's slot index.
     */
    private int locate(Hypothesis hyp) {
      int slot = firstProbe(hyp.hashCode());
      while (true) {
        final Hypothesis h = hypotheses[slot];
        if (h == null) {
          return (-slot - 1);
        }
        if (h.equals(hyp)) {
          return slot;
        }
        slot = nextProbe(slot + 1);
      }
    }

    /**
     * Adds a new hypothesis to the list.
     **/
    public void add(Hypothesis hypothesis) {

      int slot = locate(hypothesis);

      if (slot < 0) {
        slot = -slot - 1;
        hypotheses[slot] = hypothesis;
        size++;
      } else {
        // Viterbi merge.
        if (hypotheses[slot].score < hypothesis.score) {
          hypotheses[slot] = hypothesis;
        }
      }
      if (size == expandLimit) {
        expand();
      }
    }

    private void expand() {
      ActiveList expandedList = new ActiveList(hypotheses.length * 2);
      // put hypotheses to new list.
      for (int i = 0; i < hypotheses.length; i++) {
        Hypothesis hyp = hypotheses[i];
        if (hyp == null) {
          continue;
        }
        int slot = firstProbe(hyp.hashCode());
        while (true) {
          final Hypothesis h = expandedList.hypotheses[slot];
          if (h == null) {
            expandedList.hypotheses[slot] = hyp;
            break;
          }
          slot = nextProbe(slot + 1);
        }
      }
      this.modulo = expandedList.modulo;
      this.capacity = expandedList.capacity;
      this.expandLimit = expandedList.expandLimit;
      this.hypotheses = expandedList.hypotheses;
    }

    Hypothesis getBest() {
      Hypothesis best = null;
      for (Hypothesis hypothesis : hypotheses) {
        if (hypothesis == null) {
          continue;
        }
        if (best == null || hypothesis.score > best.score) {
          best = hypothesis;
        }
      }
      return best;
    }

    @Override
    public Iterator<Hypothesis> iterator() {
      return new HIterator();
    }

    class HIterator implements Iterator<Hypothesis> {

      int pointer = 0;
      int count = 0;
      Hypothesis current;

      @Override
      public boolean hasNext() {
        if (count == size) {
          return false;
        }
        while (hypotheses[pointer] == null) {
          pointer++;
        }
        current = hypotheses[pointer];
        count++;
        pointer++;
        return true;
      }

      @Override
      public Hypothesis next() {
        return current;
      }
    }

  }


}
