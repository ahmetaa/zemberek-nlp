package zemberek.morphology.ambiguity;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.CompressedWeights;
import zemberek.core.data.WeightLookup;
import zemberek.core.data.Weights;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

/**
 * This is a class for applying morphological ambiguity resolution for Turkish sentences. Algorithm
 * is based on "Haşim Sak, Tunga Güngör, and Murat Saraçlar. Morphological disambiguation of Turkish
 * text with perceptron algorithm. In CICLing 2007, volume LNCS 4394, pages 107-118, 2007".
 *
 * @see <a href="http://www.cmpe.boun.edu.tr/~hasim">Haşim Sak</a>
 * <p>
 * This code is adapted from the Author's original Perl implementation. However, this is not a
 * direct port, many changes needed to be applied for Zemberek integration and it has a cleaner
 * and faster design.
 * <p>
 * For Training, use {@link PerceptronAmbiguityResolverTrainer} class.
 */
public class PerceptronAmbiguityResolver implements AmbiguityResolver {

  private Decoder decoder;

  PerceptronAmbiguityResolver(WeightLookup averagedModel, FeatureExtractor extractor) {
    this.decoder = new Decoder(averagedModel, extractor);
  }

  WeightLookup getModel() {
    return decoder.model;
  }

  Decoder getDecoder() {
    return decoder;
  }

  public static PerceptronAmbiguityResolver fromModelFile(Path modelFile) throws IOException {

    WeightLookup lookup;
    if (CompressedWeights.isCompressed(modelFile)) {
      lookup = CompressedWeights.deserialize(modelFile);
    } else {
      lookup = Weights.loadFromFile(modelFile);
    }
    FeatureExtractor extractor = new FeatureExtractor(false);
    return new PerceptronAmbiguityResolver(lookup, extractor);
  }

  public static PerceptronAmbiguityResolver fromResource(String resourcePath) throws IOException {

    WeightLookup lookup;
    if (CompressedWeights.isCompressed(resourcePath)) {
      lookup = CompressedWeights.deserialize(resourcePath);
    } else {
      lookup = Weights.loadFromResource(resourcePath);
    }
    FeatureExtractor extractor = new FeatureExtractor(false);
    return new PerceptronAmbiguityResolver(lookup, extractor);
  }

  @Override
  public SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> allAnalyses) {
    DecodeResult best = decoder.bestPath(allAnalyses);
    List<SentenceWordAnalysis> l = new ArrayList<>();
    for (int i = 0; i < allAnalyses.size(); i++) {
      WordAnalysis wordAnalysis = allAnalyses.get(i);
      SingleAnalysis analysis = best.bestParse.get(i);
      l.add(new SentenceWordAnalysis(analysis, wordAnalysis));
    }
    return new SentenceAnalysis(sentence, l);
  }

  static class WordData {

    String lemma;
    List<String> igs;

    public WordData(String lemma, List<String> igs) {
      this.lemma = lemma;
      this.igs = igs;
    }

    static WordData fromAnalysis(SingleAnalysis sa) {
      String lemma = sa.getDictionaryItem().lemma;
      SecondaryPos secPos = sa.getDictionaryItem().secondaryPos;
      String sp = secPos == SecondaryPos.None ? "" : secPos.name();

      List<String> igs = new ArrayList<>(sa.groupCount());
      for (int i = 0; i < sa.groupCount(); i++) {
        String s = sa.getGroup(0).lexicalForm();
        if (i == 0) {
          s = sp + s;
        }
        igs.add(s);
      }
      return new WordData(lemma, igs);
    }

    String lastGroup() {
      return igs.get(igs.size() - 1);
    }
  }

  static class FeatureExtractor {

    boolean useCache;

    ConcurrentHashMap<SingleAnalysis[], IntValueMap<String>> featureCache =
        new ConcurrentHashMap<>();

    FeatureExtractor(boolean useCache) {
      this.useCache = useCache;
    }

    // This is used for training. Extracts feature counts from current best analysis sequence.
    // Trainer then uses this counts to update weights for those features.
    IntValueMap<String> extractFeatureCounts(List<SingleAnalysis> bestSequence) {
      List<SingleAnalysis> seq = Lists.newArrayList(sentenceBegin, sentenceBegin);
      seq.addAll(bestSequence);
      seq.add(sentenceEnd);
      IntValueMap<String> featureCounts = new IntValueMap<>();
      for (int i = 2; i < seq.size(); i++) {
        SingleAnalysis[] trigram = {
            seq.get(i - 2),
            seq.get(i - 1),
            seq.get(i)};
        IntValueMap<String> trigramFeatures = extractFromTrigram(trigram);
        for (IntValueMap.Entry<String> s : trigramFeatures.iterableEntries()) {
          featureCounts.incrementByAmount(s.key, s.count);
        }
      }
      return featureCounts;
    }

    IntValueMap<String> extractFromTrigram(SingleAnalysis[] trigram) {

      if (useCache) {
        IntValueMap<String> cached = featureCache.get(trigram);
        if (cached != null) {
          return cached;
        }
      }

      IntValueMap<String> feats = new IntValueMap<>();
      WordData w1 = WordData.fromAnalysis(trigram[0]);
      WordData w2 = WordData.fromAnalysis(trigram[1]);
      WordData w3 = WordData.fromAnalysis(trigram[2]);

      String r1 = w1.lemma;
      String r2 = w2.lemma;
      String r3 = w3.lemma;

      String ig1 = String.join("+", w1.igs);
      String ig2 = String.join("+", w2.igs);
      String ig3 = String.join("+", w3.igs);

      String r1Ig1 = r1 + "+" + ig1;
      String r2Ig2 = r2 + "+" + ig2;
      String r3Ig3 = r3 + "+" + ig3;

      //feats.addOrIncrement("1:" + r1Ig1 + "-" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("2:" + r1 + ig2 + r3Ig3);
      feats.addOrIncrement("3:" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("4:" + r3Ig3);
      //feats.addOrIncrement("5:" + r2 + ig2 + "-" + ig3);
      //feats.addOrIncrement("6:" + r1 + ig1 + "-" + ig3);

      //feats.addOrIncrement("7:" + r1 + "-" + r2 + "-" + r3);
      //feats.addOrIncrement("8:" + r1 + "-" + r3);
      feats.addOrIncrement("9:" + r2 + "-" + r3);
      feats.addOrIncrement("10:" + r3);
      feats.addOrIncrement("10b:" + r2);
      feats.addOrIncrement("10c:" + r1);

      //feats.addOrIncrement("11:" + ig1 + "-" + ig2 + "-" + ig3);
      //feats.addOrIncrement("12:" + ig1 + "-" + ig3);
      //feats.addOrIncrement("13:" + ig2 + "-" + ig3);
      //feats.addOrIncrement("14:" + ig3);

      String w1LastGroup = w1.lastGroup();
      String w2LastGroup = w2.lastGroup();

      for (String ig : w3.igs) {
        feats.addOrIncrement("15:" + w1LastGroup + "-" + w2LastGroup + "-" + ig);
        //feats.addOrIncrement("16:" + w1LastGroup + "-" + ig);
        feats.addOrIncrement("17:" + w2LastGroup + ig);
        //feats.addOrIncrement("18:" + ig);
      }

//      for (int k = 0; k < w3.igs.size() - 1; k++) {
//        feats.addOrIncrement("19:" + w3.igs.get(k) + "-" + w3.igs.get(k + 1));
//      }

      for (int k = 0; k < w3.igs.size(); k++) {
        feats.addOrIncrement("20:" + k + "-" + w3.igs.get(k));
      }

/*      if (Character.isUpperCase(r3.charAt(0))
          && trigram[2].getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun) {
        feats.addOrIncrement("21:PROPER-"+r3);
      } else {
        feats.addOrIncrement("21b:NOT_PROPER-" + r3);
      }*/

      feats.addOrIncrement("22:" + trigram[2].groupCount());
      //
/*
      if ((trigram[2] == sentenceEnd || trigram[2].getDictionaryItem().lemma.equals("."))
          && trigram[2].getDictionaryItem().primaryPos == PrimaryPos.Verb) {
        feats.addOrIncrement("23:ENDSVERB");
      }
*/
      if (useCache) {
        featureCache.put(trigram, feats);
      }
      return feats;
    }
  }

  private static final SingleAnalysis sentenceBegin = SingleAnalysis.unknown("<s>");
  private static final SingleAnalysis sentenceEnd = SingleAnalysis.unknown("</s>");

  /**
   * Decoder finds the best path from multiple word analyses using Viterbi search algorithm.
   */
  static class Decoder {

    WeightLookup model;
    FeatureExtractor extractor;

    Decoder(WeightLookup model,
        FeatureExtractor extractor) {
      this.model = model;
      this.extractor = extractor;
    }

    DecodeResult bestPath(List<WordAnalysis> sentence) {

      if (sentence.size() == 0) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      // holds the current active paths. initially it contains a single empty Hypothesis.
      ActiveList<Hypothesis> currentList = new ActiveList<>();
      currentList.add(new Hypothesis(sentenceBegin, sentenceBegin, null, 0));

      for (WordAnalysis analysisData : sentence) {

        ActiveList<Hypothesis> nextList = new ActiveList<>();

        // this is necessary because word analysis may contain zero SingleAnalysis
        // So we add an unknown SingleAnalysis to it.
        List<SingleAnalysis> analyses = analysisData.getAnalysisResults();
        if (analyses.size() == 0) {
          analyses = new ArrayList<>(1);
          analyses.add(SingleAnalysis.unknown(analysisData.getInput()));
        }

        for (SingleAnalysis analysis : analyses) {

          for (Hypothesis h : currentList) {

            SingleAnalysis[] trigram = {h.prev, h.current, analysis};
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
        SingleAnalysis[] trigram = {h.prev, h.current, sentenceEnd};
        IntValueMap<String> features = extractor.extractFromTrigram(trigram);

        float trigramScore = 0;
        for (String key : features) {
          trigramScore += (model.get(key) * features.get(key));
        }
        h.score += trigramScore;
      }

      Hypothesis best = currentList.getBest();
      float bestScore = best.score;
      List<SingleAnalysis> result = Lists.newArrayList();

      // backtrack. from end to begin, we add words from Hypotheses.
      while (best.previous != null) {
        result.add(best.current);
        best = best.previous;
      }

      // because we collect from end to begin, reverse is required.
      Collections.reverse(result);
      return new DecodeResult(result, bestScore);
    }
  }

  static class DecodeResult {

    List<SingleAnalysis> bestParse;
    float score;

    private DecodeResult(List<SingleAnalysis> bestParse, float score) {
      this.bestParse = bestParse;
      this.score = score;
    }
  }

  static class Hypothesis implements Scorable {

    SingleAnalysis prev; // previous word analysis result String
    SingleAnalysis current; // current word analysis result String
    Hypothesis previous; // previous Hypothesis.
    float score;

    Hypothesis(
        SingleAnalysis prev,
        SingleAnalysis current,
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

}
