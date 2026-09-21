package zemberek.morphology.ambiguity.fast;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.CompressedWeights;
import zemberek.core.data.WeightLookup;
import zemberek.core.dynamic.Scorable;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

/**
 * A high-throughput, factored Averaged Perceptron Morphological Ambiguity Resolver.
 * Evaluates candidate analyses using local structural morphotactic features, linguistic surface
 * rules, factored scoring prefixes, and multi-mode decoding (Viterbi, Beam Search, Greedy).
 */
public class FastPerceptronAmbiguityResolver implements AmbiguityResolver {

  public enum DecodeMode {
    VITERBI,
    BEAM,
    GREEDY
  }

  public static final DecodeMode DEFAULT_MODE = DecodeMode.VITERBI;
  public static final int DEFAULT_BEAM_SIZE = 8;

  private final FastDecoder decoder;

  public FastPerceptronAmbiguityResolver(
      WeightLookup model,
      FastFeatureExtractor extractor) {
    this.decoder = new FastDecoder(model, extractor);
  }

  public FastPerceptronAmbiguityResolver(WeightLookup model) {
    this(model, new FastFeatureExtractor(false));
  }

  public static FastPerceptronAmbiguityResolver fromModelFile(Path modelFile) throws IOException {
    CompressedWeights weights = CompressedWeights.deserialize(modelFile);
    FastFeatureExtractor extractor = new FastFeatureExtractor(true);
    return new FastPerceptronAmbiguityResolver(weights, extractor);
  }

  public WeightLookup getModel() {
    return decoder.model;
  }

  public FastDecoder getDecoder() {
    return decoder;
  }

  public DecodeMode getDecodeMode() {
    return decoder.getDecodeMode();
  }

  public void setDecodeMode(DecodeMode mode) {
    decoder.setDecodeMode(mode);
  }

  public boolean isExactViterbi() {
    return decoder.isExactViterbi();
  }

  public void setExactViterbi() {
    decoder.setExactViterbi();
  }

  public void setBeamSize(int beamSize) {
    decoder.setBeamSize(beamSize);
  }

  public int getBeamSize() {
    return decoder.getBeamSize();
  }

  public void setGreedy(boolean greedy) {
    decoder.setGreedy(greedy);
  }

  public boolean isGreedy() {
    return decoder.isGreedy();
  }

  @Override
  public SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> allAnalyses) {
    if (allAnalyses.isEmpty()) {
      return new SentenceAnalysis(sentence, Collections.emptyList());
    }
    FastDecodeResult best = decoder.isGreedy()
        ? decoder.bestPathGreedy(allAnalyses)
        : decoder.bestPath(allAnalyses);
    List<SentenceWordAnalysis> list = new ArrayList<>(allAnalyses.size());
    for (int i = 0; i < allAnalyses.size(); i++) {
      WordAnalysis wordAnalysis = allAnalyses.get(i);
      SingleAnalysis analysis = best.bestParse.get(i);
      list.add(new SentenceWordAnalysis(analysis, wordAnalysis));
    }
    return new SentenceAnalysis(sentence, list);
  }

  // --- Internal Data Structures ---

  public static class WordData {
    public final String lemma;
    public final List<String> igs;

    public WordData(String lemma, List<String> igs) {
      this.lemma = lemma;
      this.igs = igs;
    }

    public static WordData fromAnalysis(SingleAnalysis sa) {
      if (sa == sentenceBegin) {
        return new WordData("<s>", Collections.singletonList("<s>"));
      }
      if (sa == sentenceEnd) {
        return new WordData("</s>", Collections.singletonList("</s>"));
      }
      String lemma = sa.getDictionaryItem().lemma;
      SecondaryPos secPos = sa.getDictionaryItem().secondaryPos;
      String sp = secPos == SecondaryPos.None ? "" : secPos.name();

      List<String> igs = new ArrayList<>(sa.groupCount());
      for (int i = 0; i < sa.groupCount(); i++) {
        String s = sa.getGroup(i).lexicalForm();
        if (i == 0) {
          s = sp + s;
        }
        igs.add(s);
      }
      return new WordData(lemma, igs);
    }

    public String lastGroup() {
      if (igs.isEmpty()) {
        return "";
      }
      return igs.get(igs.size() - 1);
    }
  }

  public static class FastFeatureExtractor {
    private final boolean useCache;

    private static final class TrigramKey {
      final SingleAnalysis a1, a2, a3;
      final int hash;

      TrigramKey(SingleAnalysis a1, SingleAnalysis a2, SingleAnalysis a3) {
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        int h = a1.hashCode();
        h = 31 * h + a2.hashCode();
        h = 31 * h + a3.hashCode();
        this.hash = h;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrigramKey that = (TrigramKey) o;
        return a1.equals(that.a1) && a2.equals(that.a2) && a3.equals(that.a3);
      }

      @Override
      public int hashCode() {
        return hash;
      }
    }

    private final Cache<TrigramKey, IntValueMap<String>> featureCache =
        CacheBuilder.newBuilder()
            .maximumSize(50_000)
            .build();

    public FastFeatureExtractor(boolean useCache) {
      this.useCache = useCache;
    }

    public IntValueMap<String> extractFeatureCounts(List<SingleAnalysis> bestSequence) {
      List<SingleAnalysis> seq = new ArrayList<>(bestSequence.size() + 3);
      seq.add(sentenceBegin);
      seq.add(sentenceBegin);
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

    public IntValueMap<String> extractFromTrigram(SingleAnalysis[] trigram) {
      TrigramKey key = null;
      if (useCache) {
        key = new TrigramKey(trigram[0], trigram[1], trigram[2]);
        IntValueMap<String> cached = featureCache.getIfPresent(key);
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

      String r2Ig2 = r2 + "+" + ig2;
      String r3Ig3 = r3 + "+" + ig3;

      // Baseline Perceptron features
      feats.addOrIncrement("2:" + r1 + "-" + ig1 + "-" + r3 + "-" + ig3);
      feats.addOrIncrement("3:" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("4:" + r3Ig3);
      feats.addOrIncrement("9:" + r2 + "-" + r3);
      feats.addOrIncrement("10:" + r3);
      feats.addOrIncrement("10b:" + r2);
      feats.addOrIncrement("10c:" + r1);

      String w1LastGroup = w1.lastGroup();
      String w2LastGroup = w2.lastGroup();

      for (String ig : w3.igs) {
        feats.addOrIncrement("15:" + w1LastGroup + "-" + w2LastGroup + "-" + ig);
        feats.addOrIncrement("17:" + w2LastGroup + "-" + ig);
      }

      for (int k = 0; k < w3.igs.size(); k++) {
        feats.addOrIncrement("20:" + k + "-" + w3.igs.get(k));
      }
      feats.addOrIncrement("22:" + trigram[2].groupCount());

      // --- Surface & Linguistic Rules (PROPER & ENDSVERB) ---
      String s1 = trigram[0].surfaceForm();
      String s2 = trigram[1].surfaceForm();
      String s3 = trigram[2].surfaceForm();

      if (!s3.isEmpty() && trigram[2] != sentenceEnd && !s3.startsWith("<")) {
        char firstChar = s3.charAt(0);
        boolean isProperNoun = (trigram[2].getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun);
        boolean isSentenceInitial = (trigram[0] == sentenceBegin && trigram[1] == sentenceBegin);
        if (Character.isUpperCase(firstChar) && isProperNoun) {
          if (isSentenceInitial) {
            feats.addOrIncrement("P:INIT_PROPER:" + r3);
          } else {
            feats.addOrIncrement("P:PROPER");
            feats.addOrIncrement("P:PROPER:" + r3);
          }
        } else if (Character.isLowerCase(firstChar) && isProperNoun) {
          feats.addOrIncrement("P:LOWER_PROPER");
        }
      }

      if (trigram[2] == sentenceEnd || s3.equals(".")) {
        if (trigram[1] != sentenceBegin && trigram[1].getDictionaryItem().primaryPos == PrimaryPos.Verb) {
          feats.addOrIncrement("P:ENDSVERB");
        }
      }

      if (useCache && key != null) {
        featureCache.put(key, feats);
      }
      return feats;
    }
  }

  public static class CandidateContext {
    public final SingleAnalysis sa;
    public final String lemma;
    public final List<String> igs;
    public final String igJoined;
    public final String rIg;
    public final String lastGroup;
    public final String surface;
    public final boolean isSpecial;
    public final boolean isProperNoun;
    public final boolean isVerb;
    public final int groupCount;
    public final float uniScore;
    public final float c10bScore;
    public final float c10cScore;
    public final int index;
    public final String f2Prefix;
    public final String f3Prefix;
    public final String f9Prefix;
    public final String f17Prefix;

    public CandidateContext(SingleAnalysis sa, WeightLookup model, boolean isSentenceInitial, int index) {
      this.sa = sa;
      this.index = index;
      WordData wd = WordData.fromAnalysis(sa);
      this.lemma = wd.lemma;
      this.igs = wd.igs;
      this.igJoined = String.join("+", igs);
      this.rIg = lemma + "+" + igJoined;
      this.lastGroup = wd.lastGroup();
      this.surface = sa.surfaceForm();
      this.isSpecial = (sa == sentenceBegin || sa == sentenceEnd || (surface != null && surface.startsWith("<")));
      this.isProperNoun = (!isSpecial && sa.getDictionaryItem() != null && sa.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun);
      this.isVerb = (!isSpecial && sa.getDictionaryItem() != null && sa.getDictionaryItem().primaryPos == PrimaryPos.Verb);
      this.groupCount = sa.groupCount();

      this.f2Prefix = "2:" + rIg + "-";
      this.f3Prefix = "3:" + rIg + "-";
      this.f9Prefix = "9:" + lemma + "-";
      this.f17Prefix = "17:" + lastGroup + "-";

      this.c10bScore = model.get("10b:" + lemma);
      this.c10cScore = model.get("10c:" + lemma);

      // Precompute unigram score against model
      float score = 0;
      score += model.get("4:" + rIg);
      score += model.get("10:" + lemma);
      for (int k = 0; k < igs.size(); k++) {
        score += model.get("20:" + k + "-" + igs.get(k));
      }
      score += model.get("22:" + groupCount);

      if (surface != null && !surface.isEmpty() && sa != sentenceEnd && !surface.startsWith("<")) {
        char firstChar = surface.charAt(0);
        if (Character.isUpperCase(firstChar) && isProperNoun) {
          if (isSentenceInitial) {
            score += model.get("P:INIT_PROPER:" + lemma);
          } else {
            score += model.get("P:PROPER");
            score += model.get("P:PROPER:" + lemma);
          }
        } else if (Character.isLowerCase(firstChar) && isProperNoun) {
          score += model.get("P:LOWER_PROPER");
        }
      }
      this.uniScore = score;
    }
  }

  public static class FastDecoder {
    public final WeightLookup model;
    public final FastFeatureExtractor extractor;
    private final CandidateContext beginContext;
    private final CandidateContext endContext;
    private final float pEndsVerbScore;
    private DecodeMode mode = DEFAULT_MODE;
    private int beamSize = -1;

    public FastDecoder(WeightLookup model, FastFeatureExtractor extractor) {
      this.model = model;
      this.extractor = extractor != null ? extractor : new FastFeatureExtractor(false);
      this.beginContext = new CandidateContext(sentenceBegin, model, false, 0);
      this.endContext = new CandidateContext(sentenceEnd, model, false, 0);
      this.pEndsVerbScore = model.get("P:ENDSVERB");
    }

    public DecodeMode getDecodeMode() {
      return this.mode;
    }

    public void setDecodeMode(DecodeMode mode) {
      if (mode == null) {
        throw new IllegalArgumentException("DecodeMode cannot be null");
      }
      this.mode = mode;
      if (mode == DecodeMode.VITERBI) {
        this.beamSize = -1;
      } else if (mode == DecodeMode.BEAM) {
        if (this.beamSize <= 0) {
          this.beamSize = DEFAULT_BEAM_SIZE;
        }
      }
    }

    public boolean isExactViterbi() {
      return this.mode == DecodeMode.VITERBI;
    }

    public void setExactViterbi() {
      setDecodeMode(DecodeMode.VITERBI);
    }

    public void setBeamSize(int beamSize) {
      if (beamSize <= 0) {
        setDecodeMode(DecodeMode.VITERBI);
      } else {
        this.mode = DecodeMode.BEAM;
        this.beamSize = beamSize;
      }
    }

    public int getBeamSize() {
      return this.beamSize;
    }

    public void setGreedy(boolean greedy) {
      if (greedy) {
        setDecodeMode(DecodeMode.GREEDY);
      } else {
        setDecodeMode(DecodeMode.VITERBI);
      }
    }

    public boolean isGreedy() {
      return this.mode == DecodeMode.GREEDY;
    }

    public float computeBigramScore(CandidateContext w2, CandidateContext w3) {
      float score = 0;
      score += model.get(w2.f3Prefix + w3.rIg);
      score += model.get(w2.f9Prefix + w3.lemma);
      score += w2.c10bScore;
      String f17 = w2.f17Prefix;
      List<String> w3Igs = w3.igs;
      for (int i = 0; i < w3Igs.size(); i++) {
        score += model.get(f17 + w3Igs.get(i));
      }

      if (w3.sa == sentenceEnd || (w3.surface != null && w3.surface.equals("."))) {
        if (w2.sa != sentenceBegin && w2.isVerb) {
          score += pEndsVerbScore;
        }
      }
      return score;
    }

    public float computeTrigramScore(CandidateContext w1, CandidateContext w2, CandidateContext w3) {
      float score = 0;
      score += model.get(w1.f2Prefix + w3.rIg);
      score += w1.c10cScore;
      String w1w2 = w1.lastGroup + "-" + w2.lastGroup;
      List<String> w3Igs = w3.igs;
      for (int i = 0; i < w3Igs.size(); i++) {
        score += model.get("15:" + w1w2 + "-" + w3Igs.get(i));
      }
      return score;
    }

    public float computeTrigramScore(FastHypothesis h, CandidateContext w3, float f2Score) {
      float score = f2Score;
      List<String> w3Igs = w3.igs;
      for (int i = 0; i < w3Igs.size(); i++) {
        score += model.get(h.f15Prefix + w3Igs.get(i));
      }
      return score;
    }

    public float computeTrigramScore(FastHypothesis h, CandidateContext w3) {
      float f2 = model.get(h.prevCtx.f2Prefix + w3.rIg) + h.prevCtx.c10cScore;
      return computeTrigramScore(h, w3, f2);
    }

    public FastDecodeResult bestPath(List<WordAnalysis> sentence) {
      if (sentence.isEmpty()) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      List<FastHypothesis> currentList = Collections.singletonList(
          new FastHypothesis(beginContext, beginContext, null, 0));

      List<CandidateContext> prevPrevCandidates = Collections.singletonList(beginContext);
      List<CandidateContext> prevCandidates = Collections.singletonList(beginContext);

      for (int wordIdx = 0; wordIdx < sentence.size(); wordIdx++) {
        WordAnalysis analysisData = sentence.get(wordIdx);
        List<SingleAnalysis> analyses = analysisData.getAnalysisResults();
        if (analyses.isEmpty()) {
          analyses = Collections.singletonList(SingleAnalysis.unknown(analysisData.getInput()));
        }

        boolean isSentenceInitial = (wordIdx == 0);
        List<CandidateContext> candidates = new ArrayList<>(analyses.size());
        for (int i = 0; i < analyses.size(); i++) {
          candidates.add(new CandidateContext(analyses.get(i), model, isSentenceInitial, i));
        }

        // Precompute bigram scores for all (prevCandidate, candidate) pairs
        float[][] biScores = new float[prevCandidates.size()][candidates.size()];
        for (int j = 0; j < prevCandidates.size(); j++) {
          CandidateContext p = prevCandidates.get(j);
          for (int k = 0; k < candidates.size(); k++) {
            biScores[j][k] = computeBigramScore(p, candidates.get(k));
          }
        }

        // Precompute Feature 2 scores for all (prevPrevCandidate, candidate) pairs
        float[][] f2Scores = new float[prevPrevCandidates.size()][candidates.size()];
        for (int pp = 0; pp < prevPrevCandidates.size(); pp++) {
          CandidateContext prevPrev = prevPrevCandidates.get(pp);
          String prefix = prevPrev.f2Prefix;
          float c10c = prevPrev.c10cScore;
          for (int k = 0; k < candidates.size(); k++) {
            f2Scores[pp][k] = model.get(prefix + candidates.get(k).rIg) + c10c;
          }
        }

        // 2nd-order Markov grid recombination: state is (candidate at t-1, candidate at t)
        float[][] bestScoreGrid = new float[prevCandidates.size()][candidates.size()];
        FastHypothesis[][] bestParentGrid = new FastHypothesis[prevCandidates.size()][candidates.size()];
        for (float[] row : bestScoreGrid) {
          Arrays.fill(row, -Float.MAX_VALUE);
        }

        for (CandidateContext cand : candidates) {
          float uni = cand.uniScore;
          int candIdx = cand.index;
          for (FastHypothesis h : currentList) {
            int prevIdx = h.currCtx.index;
            float bi = biScores[prevIdx][candIdx];
            float tri = computeTrigramScore(h, cand, f2Scores[h.prevCtx.index][candIdx]);
            float totalScore = h.score + uni + bi + tri;
            if (totalScore > bestScoreGrid[prevIdx][candIdx]) {
              bestScoreGrid[prevIdx][candIdx] = totalScore;
              bestParentGrid[prevIdx][candIdx] = h;
            }
          }
        }

        List<FastHypothesis> nextList = new ArrayList<>(prevCandidates.size() * candidates.size());
        for (int j = 0; j < prevCandidates.size(); j++) {
          for (int k = 0; k < candidates.size(); k++) {
            FastHypothesis parent = bestParentGrid[j][k];
            if (parent != null) {
              nextList.add(new FastHypothesis(parent.currCtx, candidates.get(k), parent, bestScoreGrid[j][k]));
            }
          }
        }

        if (mode == DecodeMode.BEAM && beamSize > 0 && nextList.size() > beamSize) {
          nextList.sort((a, b) -> Float.compare(b.score, a.score));
          currentList = new ArrayList<>(nextList.subList(0, beamSize));
        } else {
          currentList = nextList;
        }

        prevPrevCandidates = prevCandidates;
        prevCandidates = candidates;
      }

      // Sentence end scoring
      float[] endBiScores = new float[prevCandidates.size()];
      for (int j = 0; j < prevCandidates.size(); j++) {
        endBiScores[j] = computeBigramScore(prevCandidates.get(j), endContext);
      }
      float[] endF2Scores = new float[prevPrevCandidates.size()];
      for (int pp = 0; pp < prevPrevCandidates.size(); pp++) {
        CandidateContext prevPrev = prevPrevCandidates.get(pp);
        endF2Scores[pp] = model.get(prevPrev.f2Prefix + endContext.rIg) + prevPrev.c10cScore;
      }
      float endUni = endContext.uniScore;

      FastHypothesis best = null;
      for (FastHypothesis h : currentList) {
        float bi = endBiScores[h.currCtx.index];
        float tri = computeTrigramScore(h, endContext, endF2Scores[h.prevCtx.index]);
        h.score += (endUni + bi + tri);
        if (best == null || h.score > best.score) {
          best = h;
        }
      }

      float bestScore = (best != null) ? best.score : 0;
      List<SingleAnalysis> result = new ArrayList<>();

      while (best != null && best.previous != null) {
        result.add(best.current);
        best = best.previous;
      }

      Collections.reverse(result);
      return new FastDecodeResult(result, bestScore);
    }

    public FastDecodeResult bestPathGreedy(List<WordAnalysis> sentence) {
      if (sentence.isEmpty()) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      CandidateContext prevPrev = beginContext;
      CandidateContext prev = beginContext;
      float totalScore = 0;
      List<SingleAnalysis> result = new ArrayList<>(sentence.size());

      for (int wordIdx = 0; wordIdx < sentence.size(); wordIdx++) {
        WordAnalysis analysisData = sentence.get(wordIdx);
        List<SingleAnalysis> analyses = analysisData.getAnalysisResults();
        if (analyses.isEmpty()) {
          analyses = Collections.singletonList(SingleAnalysis.unknown(analysisData.getInput()));
        }

        boolean isSentenceInitial = (wordIdx == 0);
        CandidateContext bestCandidate = null;
        float bestStepScore = -Float.MAX_VALUE;

        for (int i = 0; i < analyses.size(); i++) {
          SingleAnalysis sa = analyses.get(i);
          CandidateContext cand = new CandidateContext(sa, model, isSentenceInitial, i);
          float stepScore = cand.uniScore
              + computeBigramScore(prev, cand)
              + computeTrigramScore(prevPrev, prev, cand);
          if (stepScore > bestStepScore || bestCandidate == null) {
            bestStepScore = stepScore;
            bestCandidate = cand;
          }
        }

        totalScore += bestStepScore;
        result.add(bestCandidate.sa);
        prevPrev = prev;
        prev = bestCandidate;
      }

      // Sentence end scoring
      float endStepScore = endContext.uniScore
          + computeBigramScore(prev, endContext)
          + computeTrigramScore(prevPrev, prev, endContext);
      totalScore += endStepScore;

      return new FastDecodeResult(result, totalScore);
    }
  }

  public static class FastDecodeResult {
    public final List<SingleAnalysis> bestParse;
    public final float score;

    public FastDecodeResult(List<SingleAnalysis> bestParse, float score) {
      this.bestParse = bestParse;
      this.score = score;
    }
  }

  public static class FastHypothesis implements Scorable {
    public final SingleAnalysis prev;
    public final SingleAnalysis current;
    public final CandidateContext prevCtx;
    public final CandidateContext currCtx;
    public final FastHypothesis previous;
    public final String w1w2LastGroup;
    public final String f15Prefix;
    public float score;
    private final int hash;

    public FastHypothesis(
        CandidateContext prevCtx,
        CandidateContext currCtx,
        FastHypothesis previous,
        float score) {
      this.prevCtx = prevCtx;
      this.currCtx = currCtx;
      this.prev = prevCtx.sa;
      this.current = currCtx.sa;
      this.w1w2LastGroup = prevCtx.lastGroup + "-" + currCtx.lastGroup;
      this.f15Prefix = "15:" + this.w1w2LastGroup + "-";
      this.previous = previous;
      this.score = score;
      this.hash = 31 * this.prev.hashCode() + this.current.hashCode();
    }

    public FastHypothesis(
        SingleAnalysis prev,
        SingleAnalysis current,
        FastHypothesis previous,
        float score) {
      this.prev = prev;
      this.current = current;
      this.prevCtx = null;
      this.currCtx = null;
      this.w1w2LastGroup = "";
      this.f15Prefix = "15:-";
      this.previous = previous;
      this.score = score;
      this.hash = 31 * prev.hashCode() + current.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FastHypothesis that = (FastHypothesis) o;
      return prev.equals(that.prev) && current.equals(that.current);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public float getScore() {
      return score;
    }
  }

  public static final SingleAnalysis sentenceBegin = SingleAnalysis.unknown("<s>");
  public static final SingleAnalysis sentenceEnd = SingleAnalysis.unknown("</s>");
}
