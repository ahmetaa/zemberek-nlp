package zemberek.morphology.old_ambiguity;

import static java.lang.String.format;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;

/**
 * Based on "Haşim Sak, Tunga Güngör, and Murat Saraçlar. Morphological disambiguation of Turkish
 * text with perceptron algorithm. In CICLing 2007, volume LNCS 4394, pages 107-118, 2007" Original
 * Perl implementation is from <a href="http://www.cmpe.boun.edu.tr/~hasim">Haşim Sak</a>
 */
class PerceptronDisambiguatorPort extends AbstractDisambiguator {

  private Model averagedModel;
  private Decoder decoder;

  public static PerceptronDisambiguatorPort fromModelFile(Path modelFile) throws IOException {
    Model model = Model.loadFromTextFile(modelFile);
    FeatureExtractor extractor = new FeatureExtractor(false);
    return new PerceptronDisambiguatorPort(model, extractor);
  }

  private PerceptronDisambiguatorPort(
      Model averagedModel,
      FeatureExtractor extractor) {
    this.averagedModel = averagedModel;
    this.decoder = new Decoder(averagedModel, extractor);
  }

  public static void main(String[] args) throws IOException {
    //Path root = Paths.get("/home/ahmetaa/apps/MD-Release");
    Path root = Paths.get("/home/ahmetaa/apps/Hasim_Sak_Data");
    Path train = root.resolve("data.train.txt");
    Path dev = root.resolve("data.dev.txt");
    Path model = root.resolve("model");
    Path test = root.resolve("data.test.txt");
    PerceptronDisambiguatorPort trained = new Trainer().train(train, dev);
    Log.info("Model key count before pruning = %d", trained.averagedModel.size());
    trained.averagedModel.pruneNearZeroWeights();
    Log.info("Model key count after pruning = %d", trained.averagedModel.size());
    trained.averagedModel.saveAsText(model);

    PerceptronDisambiguatorPort.fromModelFile(model).test(test.toFile());
  }

  static class Trainer {

    Model weights = new Model();
    Model averagedWeights = new Model();
    IntValueMap<String> counts = new IntValueMap<>();

    public PerceptronDisambiguatorPort train(Path trainFile, Path devFile)
        throws IOException {

      FeatureExtractor extractor = new FeatureExtractor(true);
      Decoder decoder = new Decoder(weights, extractor);

      DataSet trainingSet = com.google.common.io.Files
          .asCharSource(trainFile.toFile(), Charsets.UTF_8).readLines(new DataSetLoader());
      int numExamples = 0;
      for (int i = 0; i < 4; i++) {
        Log.info("Iteration:" + i);
        for (SentenceData sentence : trainingSet) {
          if (sentence.size() == 0) {
            continue;
          }
          numExamples++;
          ParseResult result = decoder.bestPath(sentence);
          if (numExamples % 500 == 0) {
            Log.info("%d sentences processed.", numExamples);
          }
          if (sentence.correctParse.equals(result.bestParse)) {
            continue;
          }
          if (sentence.correctParse.size() != result.bestParse.size()) {
            throw new IllegalStateException(
                "Best parse result must have same amount of tokens with Correct parse." +
                    " \nCorrect = " + sentence.correctParse + " \nBest = " + result.bestParse);
          }

          IntValueMap<String> correctFeatures =
              extractor.extractFromSentence(sentence.correctParse);
          IntValueMap<String> bestFeatures =
              extractor.extractFromSentence(result.bestParse);
          updateModel(correctFeatures, bestFeatures, numExamples);
        }

        for (String feat : averagedWeights) {
          updateAveragedWeights(feat, numExamples);
          counts.put(feat, numExamples);
        }

        Log.info("Testing development set.");
        PerceptronDisambiguatorPort disambiguator =
            new PerceptronDisambiguatorPort(averagedWeights, extractor);
        disambiguator.test(devFile.toFile());

      }
      return new PerceptronDisambiguatorPort(averagedWeights, new FeatureExtractor(false));
    }

    private void updateModel(
        IntValueMap<String> correctFeatures,
        IntValueMap<String> bestFeatures,
        int numExamples) {
      Set<String> keySet = Sets.newHashSet();
      keySet.addAll(correctFeatures.getKeyList());
      keySet.addAll(bestFeatures.getKeyList());

      for (String feat : keySet) {

        updateAveragedWeights(feat, numExamples);

        weights.increment(
            feat,
            (correctFeatures.get(feat) - bestFeatures.get(feat)));

        counts.put(feat, numExamples);

        // reduce model by eliminating near zero weights.
        float wa = averagedWeights.get(feat);
        if (Math.abs(wa) <= Model.epsilon) {
          averagedWeights.data.remove(feat);
        }
        float w = weights.get(feat);
        if (Math.abs(w) <= Model.epsilon) {
          weights.data.remove(feat);
        }
      }
    }

    private void updateAveragedWeights(String feat, int numExamples) {
      int featureCount = counts.get(feat);
      float updatedWeight = (averagedWeights.get(feat) * featureCount
          + (numExamples - featureCount) * weights.get(feat))
          / numExamples;

      averagedWeights.put(
          feat,
          updatedWeight);
    }
  }

  static class FeatureExtractor {

    boolean useCache;

    ConcurrentHashMap<String, IntValueMap<String>> featureCache =
        new ConcurrentHashMap<>();

    public FeatureExtractor(boolean useCache) {
      this.useCache = useCache;
    }

    IntValueMap<String> extractFromSentence(List<String> parseSequence) {
      List<String> seq = Lists.newArrayList("<s>", "<s>");
      seq.addAll(parseSequence);
      seq.add("</s>");
      IntValueMap<String> featureModel = new IntValueMap<>();
      for (int i = 2; i < seq.size(); i++) {
        String[] trigram = {
            seq.get(i - 2),
            seq.get(i - 1),
            seq.get(i)};
        IntValueMap<String> trigramFeatures = extractFromTrigram(trigram);
        for (IntValueMap.Entry<String> s : trigramFeatures.iterableEntries()) {
          featureModel.incrementByAmount(s.key, s.count);
        }
      }
      return featureModel;
    }

    IntValueMap<String> extractFromTrigram(String[] trigram) {

      if (useCache) {
        String concat = String.join("", trigram);
        IntValueMap<String> cached = featureCache.get(concat);
        if (cached != null) {
          return cached;
        }
      }

      IntValueMap<String> feats = new IntValueMap<>();
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
        feats.addOrIncrement(
            format("15:%s-%s-%s", ig1s[ig1s.length - 1], ig2s[ig2s.length - 1], ig));
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
      if (useCache) {
        String concat = String.join("", trigram);
        featureCache.put(concat, feats);
      }
      return feats;
    }
  }

  public void test(File testFile) throws IOException {
    DataSet testSet = com.google.common.io.Files
        .asCharSource(testFile, Charsets.UTF_8).readLines(new DataSetLoader());
    int hit = 0, total = 0;
    Stopwatch sw = Stopwatch.createStarted();
    for (SentenceData sentence : testSet.sentences) {
      ParseResult result = decoder.bestPath(sentence);
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
        "Word count:" + total + " hit=" + hit + String.format(
            Locale.ENGLISH, " Accuracy:%f", hit * 1.0 / total));
  }

  static class Hypothesis implements Scorable {

    String prev; // previous word analysis result String
    String current; // current word analysis result String
    Hypothesis previous; // previous Hypothesis.
    float score;

    Hypothesis(String prev, String current, Hypothesis previous, float score) {
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

    @Override
    public float getScore() {
      return score;
    }
  }

  private static class Decoder {

    Model model;
    FeatureExtractor extractor;

    public Decoder(Model model,
        FeatureExtractor extractor) {
      this.model = model;
      this.extractor = extractor;
    }

    /**
     * Calculates the best path using Viterbi decoding.
     *
     * @param sentence sentence with ambiguous words.
     * @return best parse sequence and its score.
     */
    ParseResult bestPath(SentenceData sentence) {

      if (sentence.size() == 0) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      ActiveList<Hypothesis> currentList = new ActiveList<>();
      currentList.add(new Hypothesis("<s>", "<s>", null, 0));

      for (WordData analysisData : sentence.allWordAnalyses) {

        ActiveList<Hypothesis> nextList = new ActiveList<>();

        for (String analysis : analysisData.ambiguousAnalyses) {

          for (Hypothesis h : currentList) {

            String[] trigram = {h.prev, h.current, analysis};
            IntValueMap<String> features = extractor.extractFromTrigram(trigram);

            float trigramScore = 0;
            for (String key : features) {
              trigramScore += (model.get(key) * features.get(key));
            }

            Hypothesis newHyp = new Hypothesis(
                h.current,
                analysis,
                h,
                h.score + trigramScore);
            nextList.add(newHyp);
          }
        }
        currentList = nextList;
      }

      // score for sentence end. No need to create new hypotheses.
      for (Hypothesis h : currentList) {
        String sentenceEnd = "</s>";
        String[] trigram = {h.prev, h.current, sentenceEnd};
        IntValueMap<String> features = extractor.extractFromTrigram(trigram);

        float trigramScore = 0;
        for (String key : features) {
          trigramScore += (model.get(key) * features.get(key));
        }
        h.score += trigramScore;
      }

      Hypothesis best = currentList.getBest();
      float bestScore = best.score;
      List<String> result = Lists.newArrayList();

      // backtrack. from end to begin, we add words from Hypotheses.
      while (best.previous != null) {
        result.add(best.current);
        best = best.previous;
      }

      // because we collect from end to begin, reverse is required.
      Collections.reverse(result);
      return new ParseResult(result, bestScore);
    }
  }

  static class Model implements Iterable<String> {

    static float epsilon = 0.001f;

    FloatValueMap<String> data;

    Model(FloatValueMap<String> data) {
      this.data = data;
    }

    Model() {
      data = new FloatValueMap<>(10000);
    }

    public int size() {
      return data.size();
    }

    static Model loadFromTextFile(Path file) throws IOException {
      FloatValueMap<String> data = new FloatValueMap<>(10000);
      List<String> all = TextIO.loadLines(file);
      for (String s : all) {
        float weight = Float.parseFloat(Strings.subStringUntilFirst(s, " "));
        String key = Strings.subStringAfterFirst(s, " ");
        data.set(key, weight);
      }
      Log.info("Model Loaded.");
      return new Model(data);
    }

    void saveAsText(Path file) throws IOException {
      try (PrintWriter pw = new PrintWriter(file.toFile(), "utf-8")) {
        for (String s : data.getKeyList()) {
          pw.println(data.get(s) + " " + s);
        }
      }
    }

    void pruneNearZeroWeights() {
      FloatValueMap<String> pruned = new FloatValueMap<>();

      for (String key : data) {
        float w = data.get(key);
        if (Math.abs(w) > epsilon) {
          pruned.set(key, w);
        }
      }
      this.data = pruned;
    }

    float get(String key) {
      return data.get(key);
    }

    void put(String key, float value) {
      this.data.set(key, value);
    }

    void increment(String key, float value) {
      data.incrementByAmount(key, value);
    }

    @Override
    public Iterator<String> iterator() {
      return data.iterator();
    }

  }

  private static class ParseResult {

    List<String> bestParse;
    float score;

    private ParseResult(List<String> bestParse, float score) {
      this.bestParse = bestParse;
      this.score = score;
    }
  }
}