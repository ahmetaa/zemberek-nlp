package zemberek.morphology.ambiguity;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.core.text.TextConsumer;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._SentenceWordAnalysis;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._SingleAnalysis.MorphemeGroup;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology._analyzer._WordAnalysis;

public class _AmbiguityResolver implements _Disambiguator {

  private Decoder decoder;
  private _TurkishMorphologicalAnalyzer analyzer;

  private _AmbiguityResolver(
      Model averagedModel,
      FeatureExtractor extractor,
      _TurkishMorphologicalAnalyzer analyzer) {
    this.decoder = new Decoder(averagedModel, extractor);
    this.analyzer = analyzer;
  }

  public static _AmbiguityResolver fromModelFile(
      Path modelFile,
      _TurkishMorphologicalAnalyzer analyzer) throws IOException {
    Model model = Model.loadFromTextFile(modelFile);
    FeatureExtractor extractor = new FeatureExtractor(false);
    return new _AmbiguityResolver(model, extractor, analyzer);
  }

  public static void main(String[] args) throws IOException {

    Path path = Paths.get("data/ambiguity/www.aljazeera.com.tr-rule-result.txt");

    _TurkishMorphologicalAnalyzer analyzer = _TurkishMorphologicalAnalyzer.createDefault();

    DataSet set = DataSet.load(path, analyzer);
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

  public void test(Path testFile) throws IOException {
    DataSet testSet = DataSet.load(testFile, analyzer);
    int hit = 0, total = 0;
    Stopwatch sw = Stopwatch.createStarted();
    for (_SentenceAnalysis sentence : testSet.sentences) {
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

  static class Trainer {

    Model weights = new Model();
    Model averagedWeights = new Model();
    IntValueMap<String> counts = new IntValueMap<>();
    _TurkishMorphologicalAnalyzer analyzer;

    public Trainer(_TurkishMorphologicalAnalyzer analyzer) {
      this.analyzer = analyzer;
    }

    public _AmbiguityResolver train(Path trainFile, Path devFile)
        throws IOException {

      FeatureExtractor extractor = new FeatureExtractor(true);
      Decoder decoder = new Decoder(weights, extractor);

      DataSet trainingSet = DataSet.load(trainFile, analyzer);
      int numExamples = 0;
      for (int i = 0; i < 4; i++) {
        Log.info("Iteration:" + i);
        for (_SentenceAnalysis sentence : trainingSet.sentences) {
          if (sentence.size() == 0) {
            continue;
          }
          numExamples++;
          ParseResult result = decoder.bestPath(sentence.allAnalyses());
          if (numExamples % 500 == 0) {
            Log.info("%d sentences processed.", numExamples);
          }
          if (sentence.bestAnalysis().equals(result.bestParse)) {
            continue;
          }
          if (sentence.bestAnalysis().size() != result.bestParse.size()) {
            throw new IllegalStateException(
                "Best parse result must have same amount of tokens with Correct parse." +
                    " \nCorrect = " + sentence.bestAnalysis() + " \nBest = " + result.bestParse);
          }

          IntValueMap<String> correctFeatures =
              extractor.extractFromSentence(sentence.bestAnalysis());
          IntValueMap<String> bestFeatures =
              extractor.extractFromSentence(result.bestParse);
          updateModel(correctFeatures, bestFeatures, numExamples);
        }

        for (String feat : averagedWeights) {
          updateAveragedWeights(feat, numExamples);
          counts.put(feat, numExamples);
        }

        Log.info("Testing development set.");
        _AmbiguityResolver disambiguator =
            new _AmbiguityResolver(averagedWeights, extractor, analyzer);
        disambiguator.test(devFile);

      }
      return new _AmbiguityResolver(averagedWeights, new FeatureExtractor(false), analyzer);
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

    ConcurrentHashMap<_SingleAnalysis[], IntValueMap<String>> featureCache =
        new ConcurrentHashMap<>();

    public FeatureExtractor(boolean useCache) {
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
      String r1 = w1.getItem().lemma;
      String r2 = w2.getItem().lemma;
      String r3 = w3.getItem().lemma;
      String ig1 = w1.formatMorphemesLexical();
      String ig2 = w2.formatMorphemesLexical();
      String ig3 = w3.formatMorphemesLexical();

      String r1Ig1 = r1 + ig1;
      String r2Ig2 = r2 + ig2;
      String r3Ig3 = r3 + ig3;

      feats.addOrIncrement("1:" + r1Ig1 + "-" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("2:" + r1 + ig2 + r3Ig3);
      feats.addOrIncrement("3:" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("4:" + r3Ig3);
      //feats.addOrIncrement(format("5:%s%s-%s", r2, ig2, ig3));
      //feats.addOrIncrement(format("6:%s%s-%s", r1, ig1, ig3));

      //feats.addOrIncrement(format("7:%s-%s-%s", r1, r2, r3));
      //feats.addOrIncrement(format("8:%s-%s", r1, r3));
      feats.addOrIncrement("9:" + r2 + "-" + r3);
      feats.addOrIncrement("10:" + r3);

      //feats.addOrIncrement(format("11:%s-%s-%s", ig1, ig2, ig3));
      //feats.addOrIncrement(format("12:%s-%s", ig1, ig3));
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
        //  feats.addOrIncrement(format("16:%s-%s", ig1s[ig1s.length - 1], ig));
        feats.addOrIncrement("17:" + w2LastGroup + ig);
        //  feats.addOrIncrement(format("18:%s", ig));
      }

      //for (int k = 0; k < ig3s.length - 1; k++) {
      //  feats.addOrIncrement(format("19:%s-%s", ig3s[k], ig3s[k + 1]));
      //}

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

  private static class Decoder {

    Model model;
    FeatureExtractor extractor;

    public Decoder(Model model,
        FeatureExtractor extractor) {
      this.model = model;
      this.extractor = extractor;
    }

    ParseResult bestPath(List<_WordAnalysis> sentence) {

      if (sentence.size() == 0) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      ActiveList currentList = new ActiveList();
      currentList.add(new Hypothesis(sentenceBegin, sentenceBegin, null, 0));

      for (_WordAnalysis analysisData : sentence) {

        ActiveList nextList = new ActiveList();

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

  private static class ParseResult {

    List<_SingleAnalysis> bestParse;
    float score;

    private ParseResult(List<_SingleAnalysis> bestParse, float score) {
      this.bestParse = bestParse;
      this.score = score;
    }
  }

  static class Hypothesis {

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
  }

  static class ActiveList implements Iterable<Hypothesis> {

    static float DEFAULT_LOAD_FACTOR = 0.65f;

    static int DEFAULT_INITIAL_CAPACITY = 8;

    private Hypothesis[] hypotheses;

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
     * Finds either an empty slot location in Hypotheses array or the location of an equivalent
     * Hypothesis. If an empty slot is found, it returns -(slot index)-1, if an equivalent
     * Hypotheses is found, returns equal hypothesis's slot index.
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

      // if not exist, add.
      if (slot < 0) {
        slot = -slot - 1;
        hypotheses[slot] = hypothesis;
        size++;
      } else {
        // If exist, check score and if score is better, replace it.
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

  static class DataSet {

    List<_SentenceAnalysis> sentences;

    DataSet(List<_SentenceAnalysis> sentences) {
      this.sentences = sentences;
    }

    static DataSet load(Path path, _TurkishMorphologicalAnalyzer analyzer) throws IOException {
      List<SentenceDataStr> sentencesFromTextFile = DataSet.loadTrainingDataText(path);
      return new DataSet(DataSet.convert(sentencesFromTextFile, analyzer));
    }

    static List<_SentenceAnalysis> convert(
        List<SentenceDataStr> set,
        _TurkishMorphologicalAnalyzer analyzer) {

      List<_SentenceAnalysis> trainingSentences = new ArrayList<>();
      // Find actual analysis equivalents.
      for (SentenceDataStr sentenceStr : set) {

        String sentence = sentenceStr.sentence;

        List<_WordAnalysis> sentenceAnalysis = analyzer.analyzeSentence(sentence);

        if (sentenceAnalysis.size() != sentenceStr.wordList.size()) {
          Log.warn("Actual analysis token size [%d] and sentence from file token size [%d] "
                  + "does not match for sentence [%s]",
              sentenceAnalysis.size(), sentenceStr.wordList.size(), sentence);
        }

        List<_SentenceWordAnalysis> unambigiousAnalyses = new ArrayList<>();

        BAD_SENTENCE:
        for (int i = 0; i < sentenceAnalysis.size(); i++) {

          _WordAnalysis w = sentenceAnalysis.get(i);
          WordDataStr s = sentenceStr.wordList.get(i);

          if (!w.getInput().equals(s.word)) {
            Log.warn(
                "Actual analysis token [%s] at index [%d] is different than word from training [%s] "
                    + " for sentence [%s]", w.getInput(), i, s.word, sentence);
          }

          if (w.analysisCount() != s.wordAnalysis.size()) {
            Log.warn(
                "Actual analysis token [%s] has [%d] analyses but word from training file has [%d] "
                    + " analyses for sentence [%s]",
                w.getInput(), i, s.wordAnalysis.size(), sentence);
            break;
          }

          Map<String, _SingleAnalysis> analysisMap = new HashMap<>();
          for (_SingleAnalysis single : w) {
            analysisMap.put(single.format(), single);
          }

          for (String analysis : s.wordAnalysis) {
            if (!analysisMap.containsKey(analysis)) {
              Log.warn(
                  "Anaysis [%s] from training set cannot be generated by Analyzer for sentence "
                      + " [%s]. Skipping sentence.", analysis, sentence);
              break BAD_SENTENCE;
            }
          }

          if (analysisMap.containsKey(s.correctAnalysis)) {
            _SingleAnalysis correct = analysisMap.get(s.correctAnalysis);
            unambigiousAnalyses.add(new _SentenceWordAnalysis(correct, w));
          } else {
            break;
          }
        }

        if (unambigiousAnalyses.size() == sentenceStr.wordList.size()) {
          trainingSentences.add(new _SentenceAnalysis(unambigiousAnalyses));
        }
      }
      Log.info("There are %d sentences in trainig set.", trainingSentences.size());
      Log.info("Total token count is %d.",
          trainingSentences.stream().mapToInt(_SentenceAnalysis::size).sum());

      return trainingSentences;

    }

    static List<SentenceDataStr> loadTrainingDataText(Path input)
        throws IOException {

      List<String> allLines = TextIO.loadLines(input);

      List<SentenceDataStr> set = new ArrayList<>();

      TextConsumer tc = new TextConsumer(allLines);
      while (!tc.finished()) {
        List<String> sentenceData = new ArrayList<>();
        sentenceData.add(tc.current());
        tc.advance();
        sentenceData.addAll(tc.moveUntil(s -> s.startsWith("S:")));

        List<WordDataStr> wordDataStrList = new ArrayList<>();
        TextConsumer tw = new TextConsumer(sentenceData);
        String sentence = tw.getAndAdvance().substring(2);

        boolean ignoreSentence = false;
        while (!tw.finished()) {
          String word = tw.getAndAdvance();
          List<String> analysesFromLines = tw.moveUntil(s -> !s.startsWith("["));
          analysesFromLines = analysesFromLines
              .stream()
              .map(s -> s.endsWith("-") ? s.substring(0, s.length() - 1) : s)
              .collect(Collectors.toList());

          String selected = null;
          if (analysesFromLines.size() == 1) {
            selected = analysesFromLines.get(0);
            analysesFromLines.set(0, selected);
          } else {
            int i = 0;
            int index = -1;
            for (String s : analysesFromLines) {
              if (s.endsWith("*")) {
                selected = s.substring(0, s.length() - 1);
                index = i;
                break;
              }
              i++;
            }
            if (index >= 0) {
              analysesFromLines.set(index, selected);
            }
          }

          WordDataStr w = new WordDataStr(word, selected, analysesFromLines);
          if (w.correctAnalysis == null) {
            Log.warn("Sentence [%s] contains ambiguous analysis for word %s. It will be ignored.",
                sentence, word);
            ignoreSentence = true;
            break;
          } else {
            wordDataStrList.add(w);
          }
        }

        if (!ignoreSentence) {
          set.add(new SentenceDataStr(sentence, wordDataStrList));
        }
      }
      Log.info("%d sentences are loaded. ", set.size());
      Log.info("Total token count is %d.", set.stream().mapToInt(s -> s.wordList.size()).sum());
      return set;
    }
  }


  static class SentenceDataStr {

    String sentence;
    List<WordDataStr> wordList;

    public SentenceDataStr(String sentence,
        List<WordDataStr> wordList) {
      this.sentence = sentence;
      this.wordList = wordList;
    }
  }

  static class WordDataStr {

    String word;
    String correctAnalysis;
    List<String> wordAnalysis;

    public WordDataStr(String word, String correctAnalysis,
        List<String> wordAnalysis) {
      this.word = word;
      this.correctAnalysis = correctAnalysis;
      this.wordAnalysis = wordAnalysis;
    }
  }

}
