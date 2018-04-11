package zemberek.morphology._ambiguity;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scoreable;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology._ambiguity._PerceptronAmbiguityResolverTrainer.DataSet;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._SentenceWordAnalysis;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._SingleAnalysis.MorphemeGroup;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology._analyzer._WordAnalysis;

public class _PerceptronAmbiguityResolver
    implements _MorphologicalAmbiguityResolver {

  private Decoder decoder;
  private _TurkishMorphologicalAnalyzer analyzer;

  _PerceptronAmbiguityResolver(
      Model averagedModel,
      FeatureExtractor extractor,
      _TurkishMorphologicalAnalyzer analyzer) {
    this.decoder = new Decoder(averagedModel, extractor);
    this.analyzer = analyzer;
  }

  Model getModel() {
    return decoder.model;
  }

  public static _PerceptronAmbiguityResolver fromModelFile(
      Path modelFile,
      _TurkishMorphologicalAnalyzer analyzer) throws IOException {
    Model model = Model.loadFromTextFile(modelFile);
    FeatureExtractor extractor = new FeatureExtractor(false);
    return new _PerceptronAmbiguityResolver(model, extractor, analyzer);
  }

  public static void main(String[] args) throws IOException {

    Path train = Paths.get("data/ambiguity/www.aljazeera.com.tr-rule-result.txt");
    Path dev = Paths.get("data/ambiguity/aljazeera.test.txt");
    Path model = Paths.get("data/ambiguity/model");
    _TurkishMorphologicalAnalyzer analyzer = _TurkishMorphologicalAnalyzer.createDefault();

    _PerceptronAmbiguityResolver resolver =
        new _PerceptronAmbiguityResolverTrainer(analyzer).train(train, dev);
    resolver.getModel().pruneNearZeroWeights();
    resolver.getModel().saveAsText(model);
    Path test = Paths.get("data/ambiguity/open-subtitles-rule-result.txt");
    resolver.test(test);
  }

  @Override
  public _SentenceAnalysis disambiguate(List<_WordAnalysis> allAnalyses) {
    ParseResult best = decoder.bestPath(allAnalyses);
    List<_SentenceWordAnalysis> l = new ArrayList<>();
    for (int i = 0; i < allAnalyses.size(); i++) {
      l.add(new _SentenceWordAnalysis(best.bestParse.get(i), allAnalyses.get(i)));
    }
    return new _SentenceAnalysis(l);
  }

  public void test(Path testFilePath) throws IOException {
    DataSet testSet = DataSet.load(testFilePath, analyzer);
    test(testSet);
  }

  public void test(DataSet set) {
    int hit = 0, total = 0;
    Stopwatch sw = Stopwatch.createStarted();
    for (_SentenceAnalysis sentence : set.sentences) {
      ParseResult result = decoder.bestPath(sentence.allAnalyses());
      int i = 0;
      List<_SingleAnalysis> bestExpected = sentence.bestAnalysis();
      for (_SingleAnalysis bestActual : result.bestParse) {
        if (bestExpected.get(i).equals(bestActual)) {
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

  static class FeatureExtractor {

    boolean useCache;

    ConcurrentHashMap<_SingleAnalysis[], IntValueMap<String>> featureCache =
        new ConcurrentHashMap<>();

    FeatureExtractor(boolean useCache) {
      this.useCache = useCache;
    }

    IntValueMap<String> extractFromSentence(List<_SingleAnalysis> parseSequence) {
      List<_SingleAnalysis> seq = Lists.newArrayList(sentenceBegin, sentenceBegin);
      seq.addAll(parseSequence);
      seq.add(sentenceEnd);
      IntValueMap<String> featureModel = new IntValueMap<>();
      for (int i = 2; i < seq.size(); i++) {
        _SingleAnalysis[] trigram = {
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

    IntValueMap<String> extractFromTrigram(_SingleAnalysis[] trigram) {

      if (useCache) {
        IntValueMap<String> cached = featureCache.get(trigram);
        if (cached != null) {
          return cached;
        }
      }

      IntValueMap<String> feats = new IntValueMap<>();
      _SingleAnalysis w1 = trigram[0];
      _SingleAnalysis w2 = trigram[1];
      _SingleAnalysis w3 = trigram[2];

      String r1 = w1.getItem().id;
      String r2 = w2.getItem().id;
      String r3 = w3.getItem().id;

      String ig1 = w1.formatMorphemesLexical();
      String ig2 = w2.formatMorphemesLexical();
      String ig3 = w3.formatMorphemesLexical();

      String r1Ig1 = r1 + "+" + ig1;
      String r2Ig2 = r2 + "+" + ig2;
      String r3Ig3 = r3 + "+" + ig3;

      feats.addOrIncrement("1:" + r1Ig1 + "-" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("2:" + r1 + ig2 + r3Ig3);
      feats.addOrIncrement("3:" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("4:" + r3Ig3);
      feats.addOrIncrement("5:" + r2 + ig2 + "-" + ig3);
      feats.addOrIncrement("6:" + r1 + ig1 + "-" + ig3);

      feats.addOrIncrement("7:" + r1 + "-" + r2 + "-" + r3);
      feats.addOrIncrement("8:" + r1 + "-" + r3);
      feats.addOrIncrement("9:" + r2 + "-" + r3);
      feats.addOrIncrement("10:" + r3);

      feats.addOrIncrement("11:" + ig1 + "-" + ig2 + "-" + ig3);
      feats.addOrIncrement("12:" + ig1 + "-" + ig3);
      feats.addOrIncrement("13:" + ig2 + "-" + ig3);
      feats.addOrIncrement("14:" + ig3);

      MorphemeGroup[] lastWordGroups = w3.getGroups();
      String[] lastWordGroupsLex = new String[lastWordGroups.length];
      for (int i = 0; i < lastWordGroupsLex.length; i++) {
        lastWordGroupsLex[i] = lastWordGroups[i].lexicalForm();
      }

      String w1LastGroup = w1.getLastGroup().lexicalForm();
      String w2LastGroup = w2.getLastGroup().lexicalForm();

      for (String ig : lastWordGroupsLex) {
        feats.addOrIncrement("15:" + w1LastGroup + "-" + w2LastGroup + "-" + ig);
        feats.addOrIncrement("16:" + w1LastGroup + "-" + ig);
        feats.addOrIncrement("17:" + w2LastGroup + ig);
        feats.addOrIncrement("18:" + ig);
      }

      for (int k = 0; k < lastWordGroupsLex.length - 1; k++) {
        feats.addOrIncrement("19:" +
            lastWordGroupsLex[k] + "-" + lastWordGroupsLex[k + 1]);
      }

      for (int k = 0; k < lastWordGroupsLex.length; k++) {
        feats.addOrIncrement("20:" + k + "-" + lastWordGroupsLex[k]);
      }

      if (Character.isUpperCase(r3.charAt(0))
          && w3.getItem().secondaryPos == SecondaryPos.ProperNoun) {
        feats.addOrIncrement("21:PROPER");
      }

      feats.addOrIncrement("22:" + w3.groupCount());
      //
      if ((w3 == sentenceEnd || w3.getItem().lemma.equals("."))
          && w3.getItem().primaryPos == PrimaryPos.Verb) {
        feats.addOrIncrement("23:ENDSVERB");
      }
      if (useCache) {
        featureCache.put(trigram, feats);
      }
      return feats;
    }
  }

  private static final _SingleAnalysis sentenceBegin = _SingleAnalysis.unknown("<s>");
  private static final _SingleAnalysis sentenceEnd = _SingleAnalysis.unknown("</s>");

  static class Decoder {

    Model model;
    FeatureExtractor extractor;

    Decoder(Model model,
        FeatureExtractor extractor) {
      this.model = model;
      this.extractor = extractor;
    }

    ParseResult bestPath(List<_WordAnalysis> sentence) {

      if (sentence.size() == 0) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      ActiveList<Hypothesis> currentList = new ActiveList<>();
      currentList.add(new Hypothesis(sentenceBegin, sentenceBegin, null, 0));

      for (_WordAnalysis analysisData : sentence) {

        ActiveList<Hypothesis> nextList = new ActiveList<>();

        for (_SingleAnalysis analysis : analysisData) {

          for (Hypothesis h : currentList) {

            _SingleAnalysis[] trigram = {h.prev, h.current, analysis};
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
        _SingleAnalysis[] trigram = {h.prev, h.current, sentenceEnd};
        IntValueMap<String> features = extractor.extractFromTrigram(trigram);

        float trigramScore = 0;
        for (String key : features) {
          trigramScore += (model.get(key) * features.get(key));
        }
        h.score += trigramScore;
      }

      Hypothesis best = currentList.getBest();
      float bestScore = best.score;
      List<_SingleAnalysis> result = Lists.newArrayList();

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

  static class ParseResult {

    List<_SingleAnalysis> bestParse;
    float score;

    private ParseResult(List<_SingleAnalysis> bestParse, float score) {
      this.bestParse = bestParse;
      this.score = score;
    }
  }

  static class Hypothesis implements Scoreable {

    _SingleAnalysis prev; // previous word analysis result String
    _SingleAnalysis current; // current word analysis result String
    Hypothesis previous; // previous Hypothesis.
    float score;

    Hypothesis(
        _SingleAnalysis prev,
        _SingleAnalysis current,
        Hypothesis previous,
        float score) {
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

  static class Model implements Iterable<String> {

    static float epsilon = 0.0001f;

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

}
