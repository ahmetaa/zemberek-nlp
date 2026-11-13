package zemberek.morphology.ambiguity.fast;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.CompressedWeights;
import zemberek.core.data.WeightLookup;
import zemberek.core.data.Weights;
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

  public static final SingleAnalysis sentenceBegin = SingleAnalysis.unknown("<s>");
  public static final SingleAnalysis sentenceEnd = SingleAnalysis.unknown("</s>");

  public enum DecodeMode {
    VITERBI,
    BEAM,
    GREEDY
  }

  public static final DecodeMode DEFAULT_MODE = DecodeMode.VITERBI;
  public static final int DEFAULT_BEAM_SIZE = 8;
  public static final String DEFAULT_MODEL_RESOURCE = "/tr/ambiguity/model_10k_argon_v24.bin";

  private static final class DefaultModelHolder {
    static final WeightLookup INSTANCE = loadDefaultModel();

    private static WeightLookup loadDefaultModel() {
      try {
        return loadLookupFromResource(DEFAULT_MODEL_RESOURCE);
      } catch (IOException e) {
        throw new RuntimeException(
            "Cannot initialize FastPerceptronAmbiguityResolver from resource "
                + DEFAULT_MODEL_RESOURCE,
            e);
      }
    }
  }

  private final FastDecoder decoder;

  public FastPerceptronAmbiguityResolver(
      WeightLookup model,
      FastFeatureExtractor extractor) {
    this.decoder = new FastDecoder(model, extractor);
  }

  public FastPerceptronAmbiguityResolver(WeightLookup model) {
    this(model, new FastFeatureExtractor(false));
  }

  public static FastPerceptronAmbiguityResolver createDefault() {
    return new FastPerceptronAmbiguityResolver(DefaultModelHolder.INSTANCE);
  }

  public static FastPerceptronAmbiguityResolver fromModelFile(Path modelFile) throws IOException {
    WeightLookup weights = CompressedWeights.isCompressed(modelFile)
        ? CompressedWeights.deserialize(modelFile)
        : Weights.loadFromFile(modelFile);
    return new FastPerceptronAmbiguityResolver(weights);
  }

  private static WeightLookup loadLookupFromResource(String resourcePath) throws IOException {
    return CompressedWeights.isCompressed(resourcePath)
        ? CompressedWeights.deserialize(resourcePath)
        : Weights.loadFromResource(resourcePath);
  }

  public static FastPerceptronAmbiguityResolver fromResource(String resourcePath) throws IOException {
    if (DEFAULT_MODEL_RESOURCE.equals(resourcePath)) {
      return createDefault();
    }
    return new FastPerceptronAmbiguityResolver(loadLookupFromResource(resourcePath));
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
    if (allAnalyses == null || allAnalyses.isEmpty()) {
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

  /**
   * Immutable, model-independent structural representation of a {@link SingleAnalysis}.
   * Cached in a compact L1 reference table; pre-scored {@link CandidateContext} instances are
   * cached per decoder without retaining transient unigram feature strings.
   */
  static final class AnalysisTemplate {
    private static final int L1_SIZE = 1 << 12; // 4,096 slots
    private static final int L1_MASK = L1_SIZE - 1;
    private static final int MAX_ID_ENTRIES = 16_384;
    private static final AnalysisTemplate[] L1_CACHE = new AnalysisTemplate[L1_SIZE];

    private static final ConcurrentHashMap<String, Integer> LAST_GROUP_IDS = new ConcurrentHashMap<>(4_096);
    private static final ConcurrentHashMap<String, Integer> IG_JOINED_IDS = new ConcurrentHashMap<>(8_192);
    private static final ConcurrentHashMap<String, Integer> RIG_IDS = new ConcurrentHashMap<>(16_384);

    private static final AtomicInteger NEXT_LAST_GROUP_ID = new AtomicInteger(1);
    private static final AtomicInteger NEXT_IG_JOINED_ID = new AtomicInteger(1);
    private static final AtomicInteger NEXT_RIG_ID = new AtomicInteger(1);

    static final String[] F20_PREFIXES = new String[16];
    static final String[] F22_KEYS = new String[16];

    static {
      for (int i = 0; i < 16; i++) {
        F20_PREFIXES[i] = "20:" + i + "-";
        F22_KEYS[i] = "22:" + i;
      }
    }

    static final AnalysisTemplate BEGIN_TEMPLATE = new AnalysisTemplate(sentenceBegin);
    static final AnalysisTemplate END_TEMPLATE = new AnalysisTemplate(sentenceEnd);

    private static int assignId(ConcurrentHashMap<String, Integer> map, AtomicInteger counter, String key) {
      Integer existing = map.get(key);
      if (existing != null) {
        return existing;
      }
      if (map.size() < MAX_ID_ENTRIES) {
        int created = counter.getAndIncrement();
        Integer prev = map.putIfAbsent(key, created);
        return prev != null ? prev : created;
      }
      return (key.hashCode() & 0x0FFFFF) | 0x100000;
    }

    final SingleAnalysis sa;
    final String lemma;
    final String[] igsArray;
    final String rIg;
    final String lastGroup;
    final boolean isVerb;
    final boolean isNonBeginVerb;
    final boolean isEndOrDot;
    final boolean firstCharUpperProper;
    final boolean firstCharLowerProper;

    final int lastGroupId;
    final int igJoinedId;
    final int rIgId;
    final int biStateId;

    final String f2Prefix;
    final String f3Prefix;
    final String f9Prefix;
    final String f17Prefix;

    private AnalysisTemplate(SingleAnalysis sa) {
      this.sa = sa;
      String igJoined;
      String surface;
      boolean isSpecial;
      if (sa == sentenceBegin) {
        this.lemma = "<s>";
        this.igsArray = new String[]{this.lemma};
        igJoined = this.lemma;
        this.rIg = "<s>+<s>";
        this.lastGroup = this.lemma;
        surface = "<s>";
        isSpecial = true;
      } else if (sa == sentenceEnd) {
        this.lemma = "</s>";
        this.igsArray = new String[]{this.lemma};
        igJoined = this.lemma;
        this.rIg = "</s>+</s>";
        this.lastGroup = this.lemma;
        surface = "</s>";
        isSpecial = true;
      } else {
        String rawLemma = sa.getDictionaryItem().lemma;
        this.lemma = rawLemma != null ? rawLemma : "";
        SecondaryPos secPos = sa.getDictionaryItem().secondaryPos;
        String sp = secPos == SecondaryPos.None ? "" : secPos.name();

        int groupCount = sa.groupCount();
        String[] arr = new String[groupCount];
        for (int i = 0; i < groupCount; i++) {
          String s = sa.getGroup(i).lexicalForm();
          if (i == 0 && !sp.isEmpty()) {
            s = sp + s;
          }
          arr[i] = s;
        }
        this.igsArray = arr;
        igJoined = (groupCount == 1 ? arr[0] : String.join("+", arr));
        this.rIg = this.lemma + "+" + igJoined;
        this.lastGroup = (groupCount > 0 ? arr[groupCount - 1] : "");
        surface = sa.surfaceForm();
        isSpecial = (surface != null && surface.startsWith("<"));
      }

      boolean isProperNoun = (!isSpecial
          && sa.getDictionaryItem() != null
          && sa.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun);
      this.isVerb = (!isSpecial
          && sa.getDictionaryItem() != null
          && sa.getDictionaryItem().primaryPos == PrimaryPos.Verb);
      this.isNonBeginVerb = (sa != sentenceBegin && this.isVerb);
      this.isEndOrDot = (sa == sentenceEnd || ".".equals(surface));

      if (!isSpecial && isProperNoun && surface != null && !surface.isEmpty()) {
        char firstChar = surface.charAt(0);
        this.firstCharUpperProper = Character.isUpperCase(firstChar);
        this.firstCharLowerProper = Character.isLowerCase(firstChar);
      } else {
        this.firstCharUpperProper = false;
        this.firstCharLowerProper = false;
      }

      this.lastGroupId = assignId(LAST_GROUP_IDS, NEXT_LAST_GROUP_ID, this.lastGroup);
      this.igJoinedId = assignId(IG_JOINED_IDS, NEXT_IG_JOINED_ID, igJoined);
      this.rIgId = assignId(RIG_IDS, NEXT_RIG_ID, this.rIg);
      this.biStateId = (this.rIgId << 2) | (this.isNonBeginVerb ? 2 : 0) | (this.isEndOrDot ? 1 : 0);

      this.f2Prefix = "2:" + this.rIg + "-";
      this.f3Prefix = "3:" + this.rIg + "-";
      this.f9Prefix = "9:" + this.lemma + "-";
      this.f17Prefix = "17:" + this.lastGroup + "-";
    }

    static AnalysisTemplate get(SingleAnalysis sa) {
      if (sa == sentenceBegin) {
        return BEGIN_TEMPLATE;
      }
      if (sa == sentenceEnd) {
        return END_TEMPLATE;
      }
      int slot = (int) ((sa.hashCode() * 0x9E3779B97F4A7C15L) >>> 52) & L1_MASK;
      AnalysisTemplate l1 = L1_CACHE[slot];
      if (l1 != null && (l1.sa == sa || l1.sa.equals(sa))) {
        return l1;
      }
      AnalysisTemplate created = new AnalysisTemplate(sa);
      L1_CACHE[slot] = created;
      return created;
    }
  }

  public static class WordData {
    public final String lemma;
    public final List<String> igs;

    public WordData(String lemma, List<String> igs) {
      this.lemma = lemma;
      this.igs = igs;
    }

    public static WordData fromAnalysis(SingleAnalysis sa) {
      AnalysisTemplate t = AnalysisTemplate.get(sa);
      return new WordData(t.lemma, Arrays.asList(t.igsArray));
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
      AnalysisTemplate t1 = AnalysisTemplate.get(trigram[0]);
      AnalysisTemplate t2 = AnalysisTemplate.get(trigram[1]);
      AnalysisTemplate t3 = AnalysisTemplate.get(trigram[2]);

      // Baseline Perceptron features
      feats.addOrIncrement(t1.f2Prefix + t3.rIg);
      feats.addOrIncrement(t2.f3Prefix + t3.rIg);
      feats.addOrIncrement("4:" + t3.rIg);
      feats.addOrIncrement(t2.f9Prefix + t3.lemma);
      feats.addOrIncrement("10:" + t3.lemma);
      feats.addOrIncrement("10b:" + t2.lemma);
      feats.addOrIncrement("10c:" + t1.lemma);

      String f15Prefix = "15:" + t1.lastGroup + "-" + t2.lastGroup + "-";
      String f17Prefix = t2.f17Prefix;
      String[] w3Igs = t3.igsArray;
      for (int i = 0; i < w3Igs.length; i++) {
        String ig = w3Igs[i];
        feats.addOrIncrement(f15Prefix + ig);
        feats.addOrIncrement(f17Prefix + ig);
      }

      for (int k = 0; k < w3Igs.length; k++) {
        String p20 = k < AnalysisTemplate.F20_PREFIXES.length
            ? AnalysisTemplate.F20_PREFIXES[k]
            : ("20:" + k + "-");
        feats.addOrIncrement(p20 + w3Igs[k]);
      }
      String f22 = w3Igs.length < AnalysisTemplate.F22_KEYS.length
          ? AnalysisTemplate.F22_KEYS[w3Igs.length]
          : ("22:" + w3Igs.length);
      feats.addOrIncrement(f22);

      // --- Surface & Linguistic Rules (PROPER & ENDSVERB) ---
      if (t3.firstCharUpperProper) {
        boolean isSentenceInitial = (trigram[0] == sentenceBegin && trigram[1] == sentenceBegin);
        if (isSentenceInitial) {
          feats.addOrIncrement("P:INIT_PROPER:" + t3.lemma);
        } else {
          feats.addOrIncrement("P:PROPER");
          feats.addOrIncrement("P:PROPER:" + t3.lemma);
        }
      } else if (t3.firstCharLowerProper) {
        feats.addOrIncrement("P:LOWER_PROPER");
      }

      if (t3.isEndOrDot && t2.isNonBeginVerb) {
        feats.addOrIncrement("P:ENDSVERB");
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
    public final String rIg;
    public final String lastGroup;
    public final boolean isVerb;
    public final float uniScore;
    public final float c10bScore;
    public final float c10cScore;
    public final int index;
    public final String f2Prefix;
    public final String f3Prefix;
    public final String f9Prefix;
    public final String f17Prefix;

    final String[] igsArray;
    final boolean isNonBeginVerb;
    final boolean isEndOrDot;
    final int lastGroupId;
    final int igJoinedId;
    final int rIgId;
    final int biStateId;
    final CandidateContext initialVariant;

    public CandidateContext(SingleAnalysis sa, WeightLookup model, boolean isSentenceInitial, int index) {
      this(AnalysisTemplate.get(sa), model, isSentenceInitial, index);
    }

    CandidateContext(AnalysisTemplate t, WeightLookup model, boolean isSentenceInitial, int index) {
      this(
          t,
          computeUniScore(t, model, isSentenceInitial),
          model.get("10b:", t.lemma),
          model.get("10c:", t.lemma),
          index,
          Arrays.asList(t.igsArray),
          null);
    }

    private static float computeUniScore(
        AnalysisTemplate t, WeightLookup model, boolean isSentenceInitial) {
      float base = computeBaseUniScore(t, model);
      if (t.firstCharUpperProper) {
        return isSentenceInitial
            ? base + model.get("P:INIT_PROPER:", t.lemma)
            : base + model.get("P:PROPER") + model.get("P:PROPER:", t.lemma);
      }
      return t.firstCharLowerProper ? base + model.get("P:LOWER_PROPER") : base;
    }

    static float computeBaseUniScore(AnalysisTemplate t, WeightLookup model) {
      String[] igs = t.igsArray;
      String f22 = igs.length < AnalysisTemplate.F22_KEYS.length
          ? AnalysisTemplate.F22_KEYS[igs.length]
          : ("22:" + igs.length);
      float base = model.get("4:", t.rIg) + model.get("10:", t.lemma) + model.get(f22);
      for (int k = 0; k < igs.length; k++) {
        String p20 = k < AnalysisTemplate.F20_PREFIXES.length
            ? AnalysisTemplate.F20_PREFIXES[k]
            : ("20:" + k + "-");
        base += model.get(p20, igs[k]);
      }
      return base;
    }

    static CandidateContext createCached(
        AnalysisTemplate t,
        WeightLookup model,
        float properWeight,
        float lowerProperWeight) {
      float c10b = model.get("10b:", t.lemma);
      float c10c = model.get("10c:", t.lemma);
      float base = computeBaseUniScore(t, model);
      if (t.firstCharUpperProper) {
        float uniInit = base + model.get("P:INIT_PROPER:", t.lemma);
        float uniNonInit = base + properWeight + model.get("P:PROPER:", t.lemma);
        CandidateContext initCtx = new CandidateContext(t, uniInit, c10b, c10c, 0, null, null);
        return new CandidateContext(t, uniNonInit, c10b, c10c, 0, null, initCtx);
      }
      float uni = t.firstCharLowerProper ? (base + lowerProperWeight) : base;
      return new CandidateContext(t, uni, c10b, c10c, 0, null, null);
    }

    private CandidateContext(
        AnalysisTemplate t,
        float uniScore,
        float c10bScore,
        float c10cScore,
        int index,
        List<String> igs,
        CandidateContext initialVariant) {
      this.sa = t.sa;
      this.index = index;
      this.lemma = t.lemma;
      this.igs = igs;
      this.igsArray = t.igsArray;
      this.rIg = t.rIg;
      this.lastGroup = t.lastGroup;
      this.isVerb = t.isVerb;
      this.isNonBeginVerb = t.isNonBeginVerb;
      this.isEndOrDot = t.isEndOrDot;
      this.lastGroupId = t.lastGroupId;
      this.igJoinedId = t.igJoinedId;
      this.rIgId = t.rIgId;
      this.biStateId = t.biStateId;
      this.f2Prefix = t.f2Prefix;
      this.f3Prefix = t.f3Prefix;
      this.f9Prefix = t.f9Prefix;
      this.f17Prefix = t.f17Prefix;
      this.uniScore = uniScore;
      this.c10bScore = c10bScore;
      this.c10cScore = c10cScore;
      this.initialVariant = initialVariant != null ? initialVariant : this;
    }
  }

  public static class FastDecoder {
    private static final int L1_SCORED_SIZE = 1 << 15; // 32,768 slots
    private static final int L1_SCORED_MASK = L1_SCORED_SIZE - 1;
    private static final int MAX_SCORED_ENTRIES = 32_768;
    // 65,536 slots -> 512 KB long[] and 256 KB float[] arrays (~3 MB total per thread, below G1GC humongous threshold).
    private static final int TRANSITION_CACHE_BITS = 16;
    private static final int TRANSITION_CACHE_SIZE = 1 << TRANSITION_CACHE_BITS;
    private static final int TRANSITION_CACHE_MASK = TRANSITION_CACHE_SIZE - 1;
    private static final int TRANSITION_HASH_SHIFT = 64 - TRANSITION_CACHE_BITS;

    private static int mixSlot(long key) {
      long z = (key ^ (key >>> 30)) * 0xbf58476d1ce4e5b9L;
      z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
      return (int) (z >>> TRANSITION_HASH_SHIFT);
    }

    private static final class DecoderCache {
      final CandidateContext[] l1Scored = new CandidateContext[L1_SCORED_SIZE];
      final long[] biKeys = new long[TRANSITION_CACHE_SIZE];
      final float[] biVals = new float[TRANSITION_CACHE_SIZE];
      final long[] f2Keys = new long[TRANSITION_CACHE_SIZE];
      final float[] f2Vals = new float[TRANSITION_CACHE_SIZE];
      final long[] f15Keys = new long[TRANSITION_CACHE_SIZE];
      final float[] f15Vals = new float[TRANSITION_CACHE_SIZE];
      final long[] f17Keys = new long[TRANSITION_CACHE_SIZE];
      final float[] f17Vals = new float[TRANSITION_CACHE_SIZE];
    }

    public final WeightLookup model;
    public final FastFeatureExtractor extractor;
    private final boolean immutableModel;
    private final float properWeight;
    private final float lowerProperWeight;
    private final float endsVerbWeight;
    private final ConcurrentHashMap<SingleAnalysis, CandidateContext> scoredMap;
    private final Thread ownerThread;
    private final DecoderCache mainCache;
    private final ThreadLocal<DecoderCache> localCache;
    private final CandidateContext beginContext;
    private final CandidateContext endContext;
    private final CandidateContext[] beginCandidates;
    private DecodeMode mode = DEFAULT_MODE;
    private int beamSize = -1;

    public FastDecoder(WeightLookup model, FastFeatureExtractor extractor) {
      this.model = Objects.requireNonNull(model, "model cannot be null");
      this.extractor = extractor != null ? extractor : new FastFeatureExtractor(false);
      this.immutableModel = (model instanceof CompressedWeights);
      if (this.immutableModel) {
        this.properWeight = model.get("P:PROPER");
        this.lowerProperWeight = model.get("P:LOWER_PROPER");
        this.endsVerbWeight = model.get("P:ENDSVERB");
        this.scoredMap = new ConcurrentHashMap<>(2_048);
        this.ownerThread = Thread.currentThread();
        this.mainCache = new DecoderCache();
        this.localCache = ThreadLocal.withInitial(DecoderCache::new);
      } else {
        this.properWeight = 0f;
        this.lowerProperWeight = 0f;
        this.endsVerbWeight = 0f;
        this.scoredMap = null;
        this.ownerThread = null;
        this.mainCache = null;
        this.localCache = null;
      }
      this.beginContext = new CandidateContext(AnalysisTemplate.BEGIN_TEMPLATE, model, false, 0);
      this.endContext = new CandidateContext(AnalysisTemplate.END_TEMPLATE, model, false, 0);
      this.beginCandidates = new CandidateContext[]{this.beginContext};
    }

    public FastDecoder(WeightLookup model) {
      this(model, new FastFeatureExtractor(false));
    }

    private DecoderCache currentCache() {
      if (!immutableModel) {
        return null;
      }
      return (Thread.currentThread() == ownerThread) ? mainCache : localCache.get();
    }

    private CandidateContext getCandidateContext(
        DecoderCache cache,
        SingleAnalysis sa,
        boolean isSentenceInitial,
        int index) {
      if (cache != null) {
        int slot = (int) ((sa.hashCode() * 0x9E3779B97F4A7C15L) >>> 49) & L1_SCORED_MASK;
        CandidateContext sc = cache.l1Scored[slot];
        if (sc == null || (sc.sa != sa && !sc.sa.equals(sa))) {
          sc = scoredMap.get(sa);
          if (sc == null) {
            sc = CandidateContext.createCached(
                AnalysisTemplate.get(sa), model, properWeight, lowerProperWeight);
            if (scoredMap.size() < MAX_SCORED_ENTRIES) {
              CandidateContext prev = scoredMap.putIfAbsent(sa, sc);
              if (prev != null) {
                sc = prev;
              }
            }
          }
          cache.l1Scored[slot] = sc;
        }
        return isSentenceInitial ? sc.initialVariant : sc;
      }
      return new CandidateContext(AnalysisTemplate.get(sa), model, isSentenceInitial, index);
    }

    private float getEndsVerbWeight() {
      return immutableModel ? endsVerbWeight : model.get("P:ENDSVERB");
    }

    private float getCachedF17(DecoderCache cache, CandidateContext w2, CandidateContext w3) {
      long key = (((long) w2.lastGroupId) << 32) | (w3.igJoinedId & 0xFFFFFFFFL);
      long[] keys = cache.f17Keys;
      float[] vals = cache.f17Vals;
      int slot = mixSlot(key);
      if (keys[slot] == key) {
        return vals[slot];
      }
      int slot2 = (slot + 1) & TRANSITION_CACHE_MASK;
      if (keys[slot2] == key) {
        return vals[slot2];
      }
      float s17 = 0f;
      String f17Prefix = w2.f17Prefix;
      String[] w3Igs = w3.igsArray;
      for (int i = 0; i < w3Igs.length; i++) {
        s17 += model.get(f17Prefix, w3Igs[i]);
      }
      keys[slot2] = keys[slot];
      vals[slot2] = vals[slot];
      keys[slot] = key;
      vals[slot] = s17;
      return s17;
    }

    private float getCachedBigram(DecoderCache cache, CandidateContext w2, CandidateContext w3) {
      if (cache == null) {
        return computeBigramScore(w2, w3);
      }
      long key = (((long) w2.biStateId) << 32) | (w3.biStateId & 0xFFFFFFFFL);
      long[] keys = cache.biKeys;
      float[] vals = cache.biVals;
      int slot = mixSlot(key);
      if (keys[slot] == key) {
        return vals[slot];
      }
      int slot2 = (slot + 1) & TRANSITION_CACHE_MASK;
      if (keys[slot2] == key) {
        return vals[slot2];
      }
      float score = model.get(w2.f3Prefix, w3.rIg)
          + model.get(w2.f9Prefix, w3.lemma)
          + w2.c10bScore
          + getCachedF17(cache, w2, w3);
      if (w3.isEndOrDot && w2.isNonBeginVerb) {
        score += endsVerbWeight;
      }
      keys[slot2] = keys[slot];
      vals[slot2] = vals[slot];
      keys[slot] = key;
      vals[slot] = score;
      return score;
    }

    private float getCachedF2(DecoderCache cache, CandidateContext w1, CandidateContext w3) {
      if (cache == null) {
        return model.get(w1.f2Prefix, w3.rIg) + w1.c10cScore;
      }
      long key = (((long) w1.rIgId) << 32) | (w3.rIgId & 0xFFFFFFFFL);
      long[] keys = cache.f2Keys;
      float[] vals = cache.f2Vals;
      int slot = mixSlot(key);
      if (keys[slot] == key) {
        return vals[slot];
      }
      int slot2 = (slot + 1) & TRANSITION_CACHE_MASK;
      if (keys[slot2] == key) {
        return vals[slot2];
      }
      float score = model.get(w1.f2Prefix, w3.rIg) + w1.c10cScore;
      keys[slot2] = keys[slot];
      vals[slot2] = vals[slot];
      keys[slot] = key;
      vals[slot] = score;
      return score;
    }

    private float getCachedF15(
        DecoderCache cache,
        CandidateContext w1,
        CandidateContext w2,
        CandidateContext w3) {
      if (cache == null || ((w1.lastGroupId | w2.lastGroupId | w3.igJoinedId) >>> 21) != 0) {
        String prefix = "15:" + w1.lastGroup + "-" + w2.lastGroup + "-";
        float score = 0f;
        String[] igs = w3.igsArray;
        for (int i = 0; i < igs.length; i++) {
          score += model.get(prefix, igs[i]);
        }
        return score;
      }
      long key = (((long) w1.lastGroupId) << 42)
          | (((long) w2.lastGroupId) << 21)
          | (w3.igJoinedId & 0x1FFFFFL);
      long[] keys = cache.f15Keys;
      float[] vals = cache.f15Vals;
      int slot = mixSlot(key);
      if (keys[slot] == key) {
        return vals[slot];
      }
      int slot2 = (slot + 1) & TRANSITION_CACHE_MASK;
      if (keys[slot2] == key) {
        return vals[slot2];
      }
      String prefix = "15:" + w1.lastGroup + "-" + w2.lastGroup + "-";
      float score = 0f;
      String[] igs = w3.igsArray;
      for (int i = 0; i < igs.length; i++) {
        score += model.get(prefix, igs[i]);
      }
      keys[slot2] = keys[slot];
      vals[slot2] = vals[slot];
      keys[slot] = key;
      vals[slot] = score;
      return score;
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
      float score = model.get(w2.f3Prefix + w3.rIg)
          + model.get(w2.f9Prefix + w3.lemma)
          + w2.c10bScore;
      String f17 = w2.f17Prefix;
      String[] w3Igs = w3.igsArray;
      for (int i = 0; i < w3Igs.length; i++) {
        score += model.get(f17 + w3Igs[i]);
      }

      if (w3.isEndOrDot && w2.isNonBeginVerb) {
        score += getEndsVerbWeight();
      }
      return score;
    }

    public float computeTrigramScore(CandidateContext w1, CandidateContext w2, CandidateContext w3) {
      float score = model.get(w1.f2Prefix + w3.rIg) + w1.c10cScore;
      String prefix = "15:" + w1.lastGroup + "-" + w2.lastGroup + "-";
      String[] w3Igs = w3.igsArray;
      for (int i = 0; i < w3Igs.length; i++) {
        score += model.get(prefix + w3Igs[i]);
      }
      return score;
    }

    public float computeTrigramScore(FastHypothesis h, CandidateContext w3, float f2Score) {
      float score = f2Score;
      String prefix = h.getF15Prefix();
      String[] w3Igs = w3.igsArray;
      for (int i = 0; i < w3Igs.length; i++) {
        score += model.get(prefix + w3Igs[i]);
      }
      return score;
    }

    public float computeTrigramScore(FastHypothesis h, CandidateContext w3) {
      float f2 = model.get(h.prevCtx.f2Prefix + w3.rIg) + h.prevCtx.c10cScore;
      return computeTrigramScore(h, w3, f2);
    }

    public FastDecodeResult bestPath(List<WordAnalysis> sentence) {
      int sentenceSize = sentence.size();
      if (sentenceSize == 0) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      final DecoderCache cache = currentCache();
      FastHypothesis[] currentList = new FastHypothesis[]{
          new FastHypothesis(beginContext, beginContext, null, 0f)
      };
      int currentSize = 1;

      CandidateContext[] prevPrevCandidates = beginCandidates;
      CandidateContext[] prevCandidates = beginCandidates;
      boolean isBeam = (mode == DecodeMode.BEAM && beamSize > 0);

      for (int wordIdx = 0; wordIdx < sentenceSize; wordIdx++) {
        WordAnalysis analysisData = sentence.get(wordIdx);
        List<SingleAnalysis> analyses = analysisData.getAnalysisResults();
        int K = analyses.size();
        boolean isSentenceInitial = (wordIdx == 0);
        CandidateContext[] candidates;
        if (K == 0) {
          K = 1;
          candidates = new CandidateContext[]{
              getCandidateContext(cache, SingleAnalysis.unknown(analysisData.getInput()), isSentenceInitial, 0)
          };
        } else {
          candidates = new CandidateContext[K];
          for (int i = 0; i < K; i++) {
            candidates[i] = getCandidateContext(cache, analyses.get(i), isSentenceInitial, i);
          }
        }

        // Fast path when there is only 1 active parent hypothesis (always true at wordIdx == 0
        // and immediately after any pair of unambiguous tokens)
        if (currentSize == 1) {
          FastHypothesis h = currentList[0];
          CandidateContext w1 = h.prevCtx;
          CandidateContext w2 = h.currCtx;
          int w2Idx = h.currIdx;
          if (K == 1) {
            CandidateContext cand = candidates[0];
            float totalScore = h.score
                + cand.uniScore
                + getCachedBigram(cache, w2, cand)
                + getCachedF2(cache, w1, cand)
                + getCachedF15(cache, w1, w2, cand);
            currentList = new FastHypothesis[]{new FastHypothesis(w2, cand, h, totalScore, w2Idx, 0)};
            currentSize = 1;
          } else {
            FastHypothesis[] nextList = new FastHypothesis[K];
            float hScore = h.score;
            for (int k = 0; k < K; k++) {
              CandidateContext cand = candidates[k];
              float totalScore = hScore
                  + cand.uniScore
                  + getCachedBigram(cache, w2, cand)
                  + getCachedF2(cache, w1, cand)
                  + getCachedF15(cache, w1, w2, cand);
              nextList[k] = new FastHypothesis(w2, cand, h, totalScore, w2Idx, k);
            }

            if (isBeam && K > beamSize) {
              Arrays.sort(nextList, (a, b) -> Float.compare(b.score, a.score));
              currentList = Arrays.copyOf(nextList, beamSize);
              currentSize = beamSize;
            } else {
              currentList = nextList;
              currentSize = K;
            }
          }
          prevPrevCandidates = prevCandidates;
          prevCandidates = candidates;
          continue;
        }

        int PP = prevPrevCandidates.length;
        int P = prevCandidates.length;

        if (P == 1 && K == 1) {
          CandidateContext w2 = prevCandidates[0];
          CandidateContext cand = candidates[0];
          float baseScore = cand.uniScore + getCachedBigram(cache, w2, cand);
          FastHypothesis bestParent = null;
          float bestScore = -Float.MAX_VALUE;
          for (int hIdx = 0; hIdx < currentSize; hIdx++) {
            FastHypothesis h = currentList[hIdx];
            CandidateContext w1 = h.prevCtx;
            float s = h.score
                + baseScore
                + getCachedF2(cache, w1, cand)
                + getCachedF15(cache, w1, w2, cand);
            if (s > bestScore) {
              bestScore = s;
              bestParent = h;
            }
          }
          currentList = new FastHypothesis[]{new FastHypothesis(w2, cand, bestParent, bestScore, 0, 0)};
          currentSize = 1;
          prevPrevCandidates = prevCandidates;
          prevCandidates = candidates;
          continue;
        }

        // Precompute bigram scores for all (prevCandidate, candidate) pairs with state ID deduplication
        float[][] biScores = new float[P][];
        for (int j = 0; j < P; j++) {
          CandidateContext p = prevCandidates[j];
          int pBiId = p.biStateId;
          int sameJ = -1;
          for (int j0 = 0; j0 < j; j0++) {
            if (prevCandidates[j0].biStateId == pBiId) {
              sameJ = j0;
              break;
            }
          }
          if (sameJ >= 0) {
            biScores[j] = biScores[sameJ];
            continue;
          }

          float[] biRow = new float[K];
          for (int k = 0; k < K; k++) {
            CandidateContext cand = candidates[k];
            int cBiId = cand.biStateId;
            int sameK = -1;
            for (int k0 = 0; k0 < k; k0++) {
              if (candidates[k0].biStateId == cBiId) {
                sameK = k0;
                break;
              }
            }
            biRow[k] = (sameK >= 0) ? biRow[sameK] : getCachedBigram(cache, p, cand);
          }
          biScores[j] = biRow;
        }

        // Precompute Feature 2 scores for all (prevPrevCandidate, candidate) pairs with rIgId deduplication
        float[][] f2Scores = new float[PP][];
        for (int pp = 0; pp < PP; pp++) {
          CandidateContext prevPrev = prevPrevCandidates[pp];
          int ppRigId = prevPrev.rIgId;
          int samePp = -1;
          for (int pp0 = 0; pp0 < pp; pp0++) {
            if (prevPrevCandidates[pp0].rIgId == ppRigId) {
              samePp = pp0;
              break;
            }
          }
          if (samePp >= 0) {
            f2Scores[pp] = f2Scores[samePp];
            continue;
          }
          float[] row = new float[K];
          for (int k = 0; k < K; k++) {
            CandidateContext cand = candidates[k];
            int cRigId = cand.rIgId;
            int sameK = -1;
            for (int k0 = 0; k0 < k; k0++) {
              if (candidates[k0].rIgId == cRigId) {
                sameK = k0;
                break;
              }
            }
            row[k] = (sameK >= 0) ? row[sameK] : getCachedF2(cache, prevPrev, cand);
          }
          f2Scores[pp] = row;
        }

        // Precompute Feature 15 (trigram morpheme transition) scores with 3-level ID deduplication
        float[][][] f15Scores = new float[PP][][];
        for (int pp = 0; pp < PP; pp++) {
          CandidateContext prevPrev = prevPrevCandidates[pp];
          int w1LastId = prevPrev.lastGroupId;
          int samePp = -1;
          for (int pp0 = 0; pp0 < pp; pp0++) {
            if (prevPrevCandidates[pp0].lastGroupId == w1LastId) {
              samePp = pp0;
              break;
            }
          }
          if (samePp >= 0) {
            f15Scores[pp] = f15Scores[samePp];
            continue;
          }

          float[][] slice = new float[P][];
          for (int p = 0; p < P; p++) {
            CandidateContext prev = prevCandidates[p];
            int w2LastId = prev.lastGroupId;
            int sameP = -1;
            for (int p0 = 0; p0 < p; p0++) {
              if (prevCandidates[p0].lastGroupId == w2LastId) {
                sameP = p0;
                break;
              }
            }
            if (sameP >= 0) {
              slice[p] = slice[sameP];
              continue;
            }

            float[] row = new float[K];
            for (int k = 0; k < K; k++) {
              CandidateContext cand = candidates[k];
              int cIgId = cand.igJoinedId;
              int sameK = -1;
              for (int k0 = 0; k0 < k; k0++) {
                if (candidates[k0].igJoinedId == cIgId) {
                  sameK = k0;
                  break;
                }
              }
              row[k] = (sameK >= 0) ? row[sameK] : getCachedF15(cache, prevPrev, prev, cand);
            }
            slice[p] = row;
          }
          f15Scores[pp] = slice;
        }

        // 2nd-order Markov grid recombination: state is (candidate at t-1, candidate at t)
        float[][] bestScoreGrid = new float[P][K];
        FastHypothesis[][] bestParentGrid = new FastHypothesis[P][K];
        for (int j = 0; j < P; j++) {
          Arrays.fill(bestScoreGrid[j], -Float.MAX_VALUE);
        }

        for (int hIdx = 0; hIdx < currentSize; hIdx++) {
          FastHypothesis h = currentList[hIdx];
          int prevIdx = h.currIdx;
          int ppIdx = h.prevIdx;
          float hScore = h.score;
          float[] biRow = biScores[prevIdx];
          float[] f2Row = f2Scores[ppIdx];
          float[] f15Row = f15Scores[ppIdx][prevIdx];
          float[] bestScoreRow = bestScoreGrid[prevIdx];
          FastHypothesis[] bestParentRow = bestParentGrid[prevIdx];

          for (int k = 0; k < K; k++) {
            float totalScore = hScore + candidates[k].uniScore + biRow[k] + f2Row[k] + f15Row[k];
            if (totalScore > bestScoreRow[k]) {
              bestScoreRow[k] = totalScore;
              bestParentRow[k] = h;
            }
          }
        }

        FastHypothesis[] nextList = new FastHypothesis[P * K];
        int nextSize = 0;
        for (int j = 0; j < P; j++) {
          FastHypothesis[] parentRow = bestParentGrid[j];
          float[] scoreRow = bestScoreGrid[j];
          for (int k = 0; k < K; k++) {
            FastHypothesis parent = parentRow[k];
            if (parent != null) {
              nextList[nextSize++] = new FastHypothesis(parent.currCtx, candidates[k], parent, scoreRow[k], j, k);
            }
          }
        }

        if (isBeam && nextSize > beamSize) {
          Arrays.sort(nextList, 0, nextSize, (a, b) -> Float.compare(b.score, a.score));
          currentList = Arrays.copyOf(nextList, beamSize);
          currentSize = beamSize;
        } else {
          currentList = nextList;
          currentSize = nextSize;
        }

        prevPrevCandidates = prevCandidates;
        prevCandidates = candidates;
      }

      // Sentence end scoring
      int PP = prevPrevCandidates.length;
      int P = prevCandidates.length;
      float endUni = endContext.uniScore;

      FastHypothesis best = null;
      if (currentSize == 1) {
        FastHypothesis h = currentList[0];
        h.score += endUni
            + getCachedBigram(cache, h.currCtx, endContext)
            + getCachedF2(cache, h.prevCtx, endContext)
            + getCachedF15(cache, h.prevCtx, h.currCtx, endContext);
        best = h;
      } else {
        float[] endBiScores = new float[P];
        for (int j = 0; j < P; j++) {
          CandidateContext p = prevCandidates[j];
          int pBiId = p.biStateId;
          int sameJ = -1;
          for (int j0 = 0; j0 < j; j0++) {
            if (prevCandidates[j0].biStateId == pBiId) {
              sameJ = j0;
              break;
            }
          }
          endBiScores[j] = (sameJ >= 0) ? endBiScores[sameJ] : getCachedBigram(cache, p, endContext);
        }

        float[] endF2Scores = new float[PP];
        for (int pp = 0; pp < PP; pp++) {
          CandidateContext prevPrev = prevPrevCandidates[pp];
          int ppRigId = prevPrev.rIgId;
          int samePp = -1;
          for (int pp0 = 0; pp0 < pp; pp0++) {
            if (prevPrevCandidates[pp0].rIgId == ppRigId) {
              samePp = pp0;
              break;
            }
          }
          endF2Scores[pp] = (samePp >= 0) ? endF2Scores[samePp] : getCachedF2(cache, prevPrev, endContext);
        }

        float[][] endF15Scores = new float[PP][];
        for (int pp = 0; pp < PP; pp++) {
          CandidateContext prevPrev = prevPrevCandidates[pp];
          int w1LastId = prevPrev.lastGroupId;
          int samePp = -1;
          for (int pp0 = 0; pp0 < pp; pp0++) {
            if (prevPrevCandidates[pp0].lastGroupId == w1LastId) {
              samePp = pp0;
              break;
            }
          }
          if (samePp >= 0) {
            endF15Scores[pp] = endF15Scores[samePp];
            continue;
          }
          float[] row = new float[P];
          for (int p = 0; p < P; p++) {
            CandidateContext prev = prevCandidates[p];
            int w2LastId = prev.lastGroupId;
            int sameP = -1;
            for (int p0 = 0; p0 < p; p0++) {
              if (prevCandidates[p0].lastGroupId == w2LastId) {
                sameP = p0;
                break;
              }
            }
            row[p] = (sameP >= 0) ? row[sameP] : getCachedF15(cache, prevPrev, prev, endContext);
          }
          endF15Scores[pp] = row;
        }

        for (int hIdx = 0; hIdx < currentSize; hIdx++) {
          FastHypothesis h = currentList[hIdx];
          int prevIdx = h.currIdx;
          int ppIdx = h.prevIdx;
          h.score += endUni + endBiScores[prevIdx] + endF2Scores[ppIdx] + endF15Scores[ppIdx][prevIdx];
          if (best == null || h.score > best.score) {
            best = h;
          }
        }
      }

      float bestScore = (best != null) ? best.score : 0;
      SingleAnalysis[] parsed = new SingleAnalysis[sentenceSize];
      int pos = sentenceSize - 1;
      while (best != null && best.previous != null && pos >= 0) {
        parsed[pos--] = best.current;
        best = best.previous;
      }

      return new FastDecodeResult(Arrays.asList(parsed), bestScore);
    }

    public FastDecodeResult bestPathGreedy(List<WordAnalysis> sentence) {
      if (sentence.isEmpty()) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      final DecoderCache cache = currentCache();
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
          CandidateContext cand = getCandidateContext(cache, sa, isSentenceInitial, i);
          float stepScore = cand.uniScore
              + getCachedBigram(cache, prev, cand)
              + getCachedF2(cache, prevPrev, cand)
              + getCachedF15(cache, prevPrev, prev, cand);
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
          + getCachedBigram(cache, prev, endContext)
          + getCachedF2(cache, prevPrev, endContext)
          + getCachedF15(cache, prevPrev, prev, endContext);
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
    final int prevIdx;
    final int currIdx;
    private String f15Prefix;
    public float score;
    private int hash;

    public FastHypothesis(
        CandidateContext prevCtx,
        CandidateContext currCtx,
        FastHypothesis previous,
        float score) {
      this(prevCtx, currCtx, previous, score, prevCtx.index, currCtx.index);
    }

    FastHypothesis(
        CandidateContext prevCtx,
        CandidateContext currCtx,
        FastHypothesis previous,
        float score,
        int prevIdx,
        int currIdx) {
      this.prevCtx = prevCtx;
      this.currCtx = currCtx;
      this.prev = prevCtx.sa;
      this.current = currCtx.sa;
      this.previous = previous;
      this.score = score;
      this.prevIdx = prevIdx;
      this.currIdx = currIdx;
    }

    public String getF15Prefix() {
      String p = this.f15Prefix;
      if (p == null) {
        p = "15:" + prevCtx.lastGroup + "-" + currCtx.lastGroup + "-";
        this.f15Prefix = p;
      }
      return p;
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
      int h = this.hash;
      if (h == 0) {
        h = 31 * this.prev.hashCode() + this.current.hashCode();
        this.hash = h;
      }
      return h;
    }

    @Override
    public float getScore() {
      return score;
    }
  }
}
