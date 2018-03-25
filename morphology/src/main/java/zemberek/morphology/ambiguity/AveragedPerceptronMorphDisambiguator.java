package zemberek.morphology.ambiguity;

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
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.collections.UIntMap;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.morphology._ambiguity._PerceptronAmbiguityResolver;

/**
 * Based on "Haşim Sak, Tunga Güngör, and Murat Saraçlar. Morphological disambiguation of Turkish
 * text with perceptron algorithm. In CICLing 2007, volume LNCS 4394, pages 107-118, 2007" Original
 * Perl implementation is from <a href="http://www.cmpe.boun.edu.tr/~hasim">Haşim Sak</a>
 */
public class AveragedPerceptronMorphDisambiguator extends AbstractDisambiguator {

  Model weights = new Model();
  Model averagedWeights;
  IntValueMap<String> counts = new IntValueMap<>();

  Random random = new Random(1);

  public AveragedPerceptronMorphDisambiguator(File modelFile) throws IOException {
    this.averagedWeights = Model.loadFromTextFile(modelFile);
  }

  private AveragedPerceptronMorphDisambiguator() {
    this.averagedWeights = new Model(new FloatValueMap<>(10000));
  }

  static void train(File trainFile, File devFile, File modelFile) throws IOException {
    AveragedPerceptronMorphDisambiguator disambiguator = new AveragedPerceptronMorphDisambiguator();
    DataSet trainingSet = com.google.common.io.Files
        .readLines(trainFile, Charsets.UTF_8, new DataSetLoader());
    int numExamples = 0;
    for (int i = 0; i < 10; i++) {
      System.out.println("Iteration:" + i);
      for (SentenceData sentence : trainingSet) {
        numExamples++;
        ParseResult result = disambiguator.bestParse(sentence, true);
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
    Path root = Paths.get("/home/aaa/apps/MD-Release");
    Path train = root.resolve("data.train.txt");
    Path dev = root.resolve("data.dev.txt");
    Path model = root.resolve("model");
    Path test = root.resolve("data.text.txt");
    AveragedPerceptronMorphDisambiguator.train(train.toFile(), dev.toFile(), model.toFile());
    new AveragedPerceptronMorphDisambiguator(model.toFile()).test(test.toFile());
  }

  public void test(File testFile) throws IOException {
    DataSet testSet = com.google.common.io.Files
        .readLines(testFile, Charsets.UTF_8, new DataSetLoader());
    int hit = 0, total = 0;
    Stopwatch sw = Stopwatch.createStarted();
    for (SentenceData sentence : testSet.sentences) {
      ParseResult result = bestParse(sentence, true);
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
      List<String> trigram = Lists.newArrayList(
          seq.get(i - 2),
          seq.get(i - 1),
          seq.get(i)
      );
      extractTrigramFeatures(trigram, featureModel);
    }
    return featureModel;
  }

  void extractTrigramFeatures(List<String> trigram, IntValueMap<String> feats) {
    WordParse w1 = new WordParse(trigram.get(0));
    WordParse w2 = new WordParse(trigram.get(1));
    WordParse w3 = new WordParse(trigram.get(2));
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

  /**
   * Calculates the best path using Viterbi decoding.
   *
   * @param sentence sentece with ambiguous wrods.
   * @param useAveragedWeights if true, average weights are used for scoring, else, normal weights
   * are used.
   * @return best parse sequence and its score.
   */
  ParseResult bestParse(SentenceData sentence, boolean useAveragedWeights) {

    sentence.allWordAnalyses.add(WordData.SENTENCE_END);
    IntValueMap<StateId> stateIds = new IntValueMap<>();
    UIntMap<State> bestPath = new UIntMap<>();

    // initial path and state
    bestPath.put(0, new State(-1, 0, null));
    stateIds.put(new StateId("<s>", "<s>"), 0);

    int bestStateNum = 0;
    float bestScore = -100000;
    int n = 0;
    for (WordData word : sentence.allWordAnalyses) {

      IntValueMap<StateId> nextStates = new IntValueMap<>();
      // shuffle the parses for randomness.
      List<String> allAnalyses = Lists.newArrayList(word.allParses);
      //Collections.shuffle(allAnalyses, random);
      bestScore = -100000;

      for (String analysis : allAnalyses) {

        for (StateId id : stateIds) {
          int stateNum = stateIds.get(id);
          State state = bestPath.get(stateNum);
          List<String> trigram = Lists.newArrayList(
              id.first,
              id.second,
              analysis
          );

          IntValueMap<String> features = new IntValueMap<>();
          extractTrigramFeatures(trigram, features);

          float trigramScore = 0;
          for (String key : features) {
            if (useAveragedWeights) {
              trigramScore += averagedWeights.weight(key) * features.get(key);
            } else {
              trigramScore += weights.weight(key) * features.get(key);
            }
          }

          float newScore = trigramScore + state.score;

          StateId newStateId = new StateId(id.second, analysis);
          if (!nextStates.contains(newStateId)) {
            nextStates.put(newStateId, ++n);
          }

          // Viterbi path selection
          int nextStateNum = nextStates.get(newStateId);

          if (bestPath.containsKey(nextStateNum)) {
            State s = bestPath.get(nextStateNum);
            if (newScore > s.score) {
              bestPath.put(nextStateNum, new State(stateNum, newScore, analysis));
            }
          } else {
            bestPath.put(nextStateNum, new State(stateNum, newScore, analysis));
          }

          if (newScore > bestScore) {
            bestScore = newScore;
            bestStateNum = nextStateNum;
          }
        }
      }
      stateIds = nextStates;
    }

    LinkedList<String> best = Lists.newLinkedList();
    int stateNum = bestStateNum;
    while (stateNum > 0) {
      State s = bestPath.get(stateNum);
      best.addFirst(s.parse);
      stateNum = s.previous;
    }
    best.removeLast();
    return new ParseResult(best, bestScore);
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

  //represents a state in Viterbi search.
  class State {

    int previous; // previous state index.
    float score; // score
    String parse;

    State(int previous, float score, String parse) {
      this.previous = previous;
      this.score = score;
      this.parse = parse;
    }
  }

  // represents the ID of the state
  class StateId {

    String first;
    String second;

    StateId(String first, String second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StateId stateId = (StateId) o;
      return Objects.equals(first, stateId.first) &&
          Objects.equals(second, stateId.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }
  }
}