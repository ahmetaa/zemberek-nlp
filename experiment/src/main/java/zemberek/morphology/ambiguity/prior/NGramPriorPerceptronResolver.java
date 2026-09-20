package zemberek.morphology.ambiguity.prior;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.CompressedWeights;
import zemberek.core.data.WeightLookup;
import zemberek.core.data.Weights;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

/**
 * An Averaged Perceptron Morphological Ambiguity Resolver enhanced with N-gram statistical priors.
 * Evaluates candidate analyses using both local structural morphotactic features and large-corpus
 * unigram/bigram/trigram collocation priors.
 */
public class NGramPriorPerceptronResolver implements AmbiguityResolver {

  public static final int DEFAULT_BEAM_SIZE = 8;

  private final PriorDecoder decoder;
  private final NGramPriorStore priorStore;

  public NGramPriorPerceptronResolver(
      WeightLookup model,
      PriorFeatureExtractor extractor,
      NGramPriorStore priorStore) {
    this.priorStore = priorStore != null ? priorStore : new NGramPriorStore();
    this.decoder = new PriorDecoder(model, extractor);
  }

  public static NGramPriorPerceptronResolver fromModelFile(Path modelFile, NGramPriorStore priorStore) throws IOException {
    CompressedWeights weights = CompressedWeights.deserialize(modelFile);
    PriorFeatureExtractor extractor = new PriorFeatureExtractor(true, priorStore);
    return new NGramPriorPerceptronResolver(weights, extractor, priorStore);
  }

  public static NGramPriorPerceptronResolver fromModelFile(Path modelFile) throws IOException {
    return fromModelFile(modelFile, new NGramPriorStore());
  }

  public WeightLookup getModel() {
    return decoder.model;
  }

  public PriorDecoder getDecoder() {
    return decoder;
  }

  public NGramPriorStore getPriorStore() {
    return priorStore;
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

  public void setExactViterbi() {
    decoder.setExactViterbi();
  }

  @Override
  public SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> allAnalyses) {
    if (allAnalyses.isEmpty()) {
      return new SentenceAnalysis(sentence, Collections.emptyList());
    }
    PriorDecodeResult best = decoder.isGreedy()
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
      return igs.get(igs.size() - 1);
    }
  }

  public static class PriorFeatureExtractor {
    private final boolean useCache;
    private final NGramPriorStore priorStore;

    private static final class TrigramKey {
      final SingleAnalysis a1, a2, a3;
      final int hash;

      TrigramKey(SingleAnalysis a1, SingleAnalysis a2, SingleAnalysis a3) {
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.hash = 31 * (31 * a1.hashCode() + a2.hashCode()) + a3.hashCode();
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

    public PriorFeatureExtractor(boolean useCache, NGramPriorStore priorStore) {
      this.useCache = useCache;
      this.priorStore = priorStore != null ? priorStore : new NGramPriorStore();
    }

    public NGramPriorStore getPriorStore() {
      return priorStore;
    }

    public IntValueMap<String> extractFeatureCounts(List<SingleAnalysis> bestSequence) {
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

      // --- N-Gram Prior Features ---
      if (!s2.startsWith("<") && !s3.startsWith("<")) {
        int biBin = priorStore.getBigramBin(s2, s3);
        if (biBin > 0) {
          feats.addOrIncrement("P:BI_BIN:" + biBin);
          feats.addOrIncrement("P:BI_POS:" + biBin + "_" + w3.lastGroup());
        }

        if (priorStore.hasCollocation(s2, s3)) {
          feats.addOrIncrement("P:COLLOC");
          feats.addOrIncrement("P:COLLOC_POS:" + w3.lastGroup());
        }

        int lemmaBiBin = priorStore.getBigramBin(r2, r3);
        if (lemmaBiBin > 0) {
          feats.addOrIncrement("P:LEMMA_BI:" + lemmaBiBin);
        }
      }

      if (!s1.startsWith("<") && !s2.startsWith("<") && !s3.startsWith("<")) {
        int triBin = priorStore.getTrigramBin(s1, s2, s3);
        if (triBin > 0) {
          feats.addOrIncrement("P:TRI_BIN:" + triBin);
          feats.addOrIncrement("P:TRI_POS:" + triBin + "_" + w3.lastGroup());
        }
      }

      if (useCache && key != null) {
        featureCache.put(key, feats);
      }
      return feats;
    }
  }

  private static final String[] BI_BIN_KEYS = new String[20];
  private static final String[] TRI_BIN_KEYS = new String[20];
  static {
    for (int i = 0; i < 20; i++) {
      BI_BIN_KEYS[i] = "P:BI_BIN:" + i;
      TRI_BIN_KEYS[i] = "P:TRI_BIN:" + i;
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

  public static class PriorDecoder {
    public final WeightLookup model;
    public final PriorFeatureExtractor extractor;
    private final CandidateContext beginContext;
    private final CandidateContext endContext;
    private int beamSize = DEFAULT_BEAM_SIZE;
    private boolean greedy = false;

    public PriorDecoder(WeightLookup model, PriorFeatureExtractor extractor) {
      this.model = model;
      this.extractor = extractor != null ? extractor : new PriorFeatureExtractor(false, null);
      this.beginContext = new CandidateContext(sentenceBegin, model, false, 0);
      this.endContext = new CandidateContext(sentenceEnd, model, false, 0);
    }

    public void setBeamSize(int beamSize) {
      this.beamSize = beamSize;
    }

    public int getBeamSize() {
      return this.beamSize;
    }

    public void setGreedy(boolean greedy) {
      this.greedy = greedy;
    }

    public boolean isGreedy() {
      return this.greedy;
    }

    public void setExactViterbi() {
      this.beamSize = -1;
      this.greedy = false;
    }

    public float computeBigramScore(CandidateContext w2, CandidateContext w3) {
      float score = 0;
      score += model.get("3:" + w2.rIg + "-" + w3.rIg);
      score += model.get("9:" + w2.lemma + "-" + w3.lemma);
      score += w2.c10bScore;
      for (String ig : w3.igs) {
        score += model.get("17:" + w2.lastGroup + "-" + ig);
      }

      if (w3.sa == sentenceEnd || (w3.surface != null && w3.surface.equals("."))) {
        if (w2.sa != sentenceBegin && w2.isVerb) {
          score += model.get("P:ENDSVERB");
        }
      }

      if (!w2.isSpecial && !w3.isSpecial) {
        NGramPriorStore store = extractor.getPriorStore();
        int biBin = store.getBigramBin(w2.surface, w3.surface);
        if (biBin > 0) {
          String biBinKey = (biBin < BI_BIN_KEYS.length) ? BI_BIN_KEYS[biBin] : ("P:BI_BIN:" + biBin);
          score += model.get(biBinKey);
          score += model.get("P:BI_POS:" + biBin + "_" + w3.lastGroup);
        }

        if (store.hasCollocation(w2.surface, w3.surface)) {
          score += model.get("P:COLLOC");
          score += model.get("P:COLLOC_POS:" + w3.lastGroup);
        }

        int lemmaBiBin = store.getBigramBin(w2.lemma, w3.lemma);
        if (lemmaBiBin > 0) {
          score += model.get("P:LEMMA_BI:" + lemmaBiBin);
        }
      }
      return score;
    }

    public float computeTrigramScore(CandidateContext w1, CandidateContext w2, CandidateContext w3) {
      float score = 0;
      score += model.get("2:" + w1.rIg + "-" + w3.rIg);
      score += w1.c10cScore;
      String w1w2 = w1.lastGroup + "-" + w2.lastGroup;
      for (String ig : w3.igs) {
        score += model.get("15:" + w1w2 + "-" + ig);
      }

      if (!w1.isSpecial && !w2.isSpecial && !w3.isSpecial) {
        int triBin = extractor.getPriorStore().getTrigramBin(w1.surface, w2.surface, w3.surface);
        if (triBin > 0) {
          String triBinKey = (triBin < TRI_BIN_KEYS.length) ? TRI_BIN_KEYS[triBin] : ("P:TRI_BIN:" + triBin);
          score += model.get(triBinKey);
          score += model.get("P:TRI_POS:" + triBin + "_" + w3.lastGroup);
        }
      }
      return score;
    }

    public float computeTrigramScore(PriorHypothesis h, CandidateContext w3) {
      float score = 0;
      score += model.get("2:" + h.prevCtx.rIg + "-" + w3.rIg);
      score += h.prevCtx.c10cScore;
      for (String ig : w3.igs) {
        score += model.get("15:" + h.w1w2LastGroup + "-" + ig);
      }

      if (!h.prevCtx.isSpecial && !h.currCtx.isSpecial && !w3.isSpecial) {
        int triBin = extractor.getPriorStore().getTrigramBin(h.prevCtx.surface, h.currCtx.surface, w3.surface);
        if (triBin > 0) {
          String triBinKey = (triBin < TRI_BIN_KEYS.length) ? TRI_BIN_KEYS[triBin] : ("P:TRI_BIN:" + triBin);
          score += model.get(triBinKey);
          score += model.get("P:TRI_POS:" + triBin + "_" + w3.lastGroup);
        }
      }
      return score;
    }

    public PriorDecodeResult bestPath(List<WordAnalysis> sentence) {
      if (sentence.isEmpty()) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      List<PriorHypothesis> currentList = Collections.singletonList(
          new PriorHypothesis(beginContext, beginContext, null, 0));

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

        // 2nd-order Markov grid recombination: state is (candidate at t-1, candidate at t)
        PriorHypothesis[][] nextGrid = new PriorHypothesis[prevCandidates.size()][candidates.size()];
        for (CandidateContext cand : candidates) {
          float uni = cand.uniScore;
          int candIdx = cand.index;
          for (PriorHypothesis h : currentList) {
            int prevIdx = h.currCtx.index;
            float bi = biScores[prevIdx][candIdx];
            float tri = computeTrigramScore(h, cand);
            float totalScore = h.score + uni + bi + tri;
            PriorHypothesis existing = nextGrid[prevIdx][candIdx];
            if (existing == null || totalScore > existing.score) {
              nextGrid[prevIdx][candIdx] = new PriorHypothesis(h.currCtx, cand, h, totalScore);
            }
          }
        }

        List<PriorHypothesis> nextList = new ArrayList<>(prevCandidates.size() * candidates.size());
        for (int j = 0; j < prevCandidates.size(); j++) {
          for (int k = 0; k < candidates.size(); k++) {
            PriorHypothesis h = nextGrid[j][k];
            if (h != null) {
              nextList.add(h);
            }
          }
        }

        if (beamSize > 0 && nextList.size() > beamSize) {
          nextList.sort((a, b) -> Float.compare(b.score, a.score));
          currentList = new ArrayList<>(nextList.subList(0, beamSize));
        } else {
          currentList = nextList;
        }

        prevCandidates = candidates;
      }

      // Sentence end scoring
      float[] endBiScores = new float[prevCandidates.size()];
      for (int j = 0; j < prevCandidates.size(); j++) {
        endBiScores[j] = computeBigramScore(prevCandidates.get(j), endContext);
      }
      float endUni = endContext.uniScore;

      PriorHypothesis best = null;
      for (PriorHypothesis h : currentList) {
        float bi = endBiScores[h.currCtx.index];
        float tri = computeTrigramScore(h, endContext);
        h.score += (endUni + bi + tri);
        if (best == null || h.score > best.score) {
          best = h;
        }
      }

      float bestScore = (best != null) ? best.score : 0;
      List<SingleAnalysis> result = Lists.newArrayList();

      while (best != null && best.previous != null) {
        result.add(best.current);
        best = best.previous;
      }

      Collections.reverse(result);
      return new PriorDecodeResult(result, bestScore);
    }

    public PriorDecodeResult bestPathGreedy(List<WordAnalysis> sentence) {
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

      return new PriorDecodeResult(result, totalScore);
    }
  }

  public static class PriorDecodeResult {
    public final List<SingleAnalysis> bestParse;
    public final float score;

    public PriorDecodeResult(List<SingleAnalysis> bestParse, float score) {
      this.bestParse = bestParse;
      this.score = score;
    }
  }

  public static class PriorHypothesis implements Scorable {
    public final SingleAnalysis prev;
    public final SingleAnalysis current;
    public final CandidateContext prevCtx;
    public final CandidateContext currCtx;
    public final PriorHypothesis previous;
    public final String w1w2LastGroup;
    public float score;
    private final int hash;

    public PriorHypothesis(
        CandidateContext prevCtx,
        CandidateContext currCtx,
        PriorHypothesis previous,
        float score) {
      this.prevCtx = prevCtx;
      this.currCtx = currCtx;
      this.prev = prevCtx.sa;
      this.current = currCtx.sa;
      this.w1w2LastGroup = prevCtx.lastGroup + "-" + currCtx.lastGroup;
      this.previous = previous;
      this.score = score;
      this.hash = 31 * this.prev.hashCode() + this.current.hashCode();
    }

    public PriorHypothesis(
        SingleAnalysis prev,
        SingleAnalysis current,
        PriorHypothesis previous,
        float score) {
      this.prev = prev;
      this.current = current;
      this.prevCtx = null;
      this.currCtx = null;
      this.w1w2LastGroup = "";
      this.previous = previous;
      this.score = score;
      this.hash = 31 * prev.hashCode() + current.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PriorHypothesis that = (PriorHypothesis) o;
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
