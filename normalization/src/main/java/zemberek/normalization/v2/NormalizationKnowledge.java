package zemberek.normalization.v2;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.normalization.v2.NormV2Types.CasingShape;

/**
 * Immutable runtime linguistic and empirical knowledge tables used by {@link
 * SpanCandidateGenerator} and {@link SpanLatticePerceptronNormalizer}.
 *
 * <p>Holds pre-mined {@code 1:1}/{@code 1:2} rewrite tables, {@code 2:1}/{@code 3:1} merge tables,
 * productive informal suffix rules, clean-corpus casing statistics, {@link CleanWordLm} (3-gram
 * clean Turkish LM), {@link CharPhonotacticModel} (character 4-gram model), and context-aware
 * deterministic Turkish truecasing — without carrying any offline corpus-mining dependencies.
 */
public final class NormalizationKnowledge {

  static final int SNAPSHOT_MAGIC = 0x5A4E5632; // "ZNV2"
  static final int SNAPSHOT_VERSION = 3;

  // ===============================================================================================
  // Contextual Truecasing Bitflags (B2 Layer 1 — zero lattice-arc duplication)
  // ===============================================================================================

  /** Sentence has noisy Title-Case-Every-Word style ({@code >=45%} of words capitalized). */
  public static final int CTX_SENT_TITLE_NOISY = 1 << 0;

  /** Immediately adjacent to a calendar day number ({@code 15 Nisan}) or 4-digit year ({@code Mayıs 2024}). */
  public static final int CTX_ADJ_DATE_NUM = 1 << 1;

  /** Immediately precedes a Gazetteer / Institutional / Street / Honorific head ({@code Konur Sokak'taki}). */
  public static final int CTX_BEFORE_GAZETTEER_OR_HONORIFIC = 1 << 2;

  /** Immediately follows a numeric token ({@code 5000 mAh} vs {@code Yıldızevler Mah}). */
  public static final int CTX_AFTER_NUMBER = 1 << 3;

  /** TDK two-word holiday, post-name honorific, or given-name before ProperNoun surname ({@code Sevgililer Günü}, {@code Lale Hanım}, {@code Ümit Özdağ}). */
  public static final int CTX_PROMOTE_PROPER_IN_CONTEXT = 1 << 4;

  private static final Set<String> TDK_MONTHS_AND_DAYS =
      Set.of(
          "ocak",
          "şubat",
          "mart",
          "nisan",
          "mayıs",
          "haziran",
          "temmuz",
          "ağustos",
          "eylül",
          "ekim",
          "kasım",
          "aralık",
          "pazartesi",
          "salı",
          "çarşamba",
          "perşembe",
          "cuma",
          "cumartesi",
          "pazar");

  private static final Set<String> LOWERCASE_HEADLINE_BIAS_STOPWORDS =
      Set.of(
          "pro", "max", "plus", "mini", "ultra", "air", "ban", "old", "new", "free", "full",
          "two", "top", "big", "good", "bad", "come", "get", "set", "run", "aka", "prof", "vs",
          "vb", "edp", "edt", "angel", "price", "grey", "express", "number", "super", "rootlu",
          "demokrat", "bankası", "üniversitesi", "müdürlüğü", "başkanı", "başkanlığı",
          "bakanlığı", "belediyesi", "paşa", "reis", "makbule", "menteşe", "çat", "ikra");

  private static final Set<String> LOWERCASE_APOSTROPHE_SLANG_STEMS =
      Set.of(
          "entry", "pp", "dm", "pm", "sub", "nick", "post", "story", "tweet", "fav", "rt", "mod",
          "ban", "bug", "lag", "ping", "fps", "btc-e", "recession", "spree");

  private static final Set<String> CANONICAL_UPPER_ACRONYM_STEMS =
      Set.of("seo", "php", "mp3", "mp4", "llm", "html", "sql", "smm", "asp", "iddk");

  private static final Map<String, String> CANONICAL_DOTTED_ABBR = buildCanonicalDottedAbbr();
  private static final Map<String, String> CANONICAL_TECH_TOKENS = buildCanonicalTechTokens();

  private static Map<String, String> buildCanonicalDottedAbbr() {
    Map<String, String> m = new HashMap<>(48);
    m.put("vb.", "vb.");
    m.put("vs.", "vs.");
    m.put("dr.", "Dr.");
    m.put("prof.", "Prof.");
    m.put("doç.", "Doç.");
    m.put("av.", "Av.");
    m.put("org.", "Org.");
    m.put("alb.", "Alb.");
    m.put("yrd.", "Yrd.");
    m.put("arş.", "Arş.");
    m.put("gör.", "Gör.");
    m.put("uzm.", "Uzm.");
    m.put("ecz.", "Ecz.");
    m.put("müh.", "Müh.");
    m.put("mim.", "Mim.");
    m.put("vet.", "Vet.");
    m.put("hz.", "Hz.");
    m.put("ltd.", "Ltd.");
    m.put("tic.", "Tic.");
    m.put("şti.", "Şti.");
    m.put("san.", "San.");
    m.put("mah.", "Mah.");
    m.put("cad.", "Cad.");
    m.put("sok.", "Sok.");
    m.put("apt.", "Apt.");
    m.put("no.", "No.");
    m.put("tel.", "Tel.");
    return Map.copyOf(m);
  }

  private static Map<String, String> buildCanonicalTechTokens() {
    Map<String, String> m = new HashMap<>(32);
    m.put("2k", "2K");
    m.put("4k", "4K");
    m.put("8k", "8K");
    m.put("i3", "i3");
    m.put("i5", "i5");
    m.put("i7", "i7");
    m.put("i9", "i9");
    m.put("ghz", "GHz");
    m.put("mhz", "MHz");
    m.put("mbps", "Mbps");
    m.put("gbps", "Gbps");
    m.put("mp3", "MP3");
    m.put("mp4", "MP4");
    m.put("seo", "SEO");
    m.put("php", "PHP");
    m.put("html", "HTML");
    m.put("sql", "SQL");
    return Map.copyOf(m);
  }

  public record TargetCount(String normLower, String bestSurface, int count) {}

  public record RewriteEntry(int totalCount, int keepCount, TargetCount[] targets) {
    public float keepProb() {
      return (keepCount + 1.5f) / (totalCount + 3.0f);
    }

    public float probOf(String normLower) {
      for (TargetCount tc : targets) {
        if (tc.normLower.equals(normLower)) {
          return tc.count / (totalCount + 3.0f);
        }
      }
      return 0.0f;
    }

    public int countOf(String normLower) {
      for (TargetCount tc : targets) {
        if (tc.normLower.equals(normLower)) {
          return tc.count;
        }
      }
      return 0;
    }
  }

  public record MergeEntry(int cooccurCount, TargetCount[] mergedTargets) {
    public int totalMergedCount() {
      int s = 0;
      for (TargetCount tc : mergedTargets) {
        s += tc.count;
      }
      return s;
    }

    public float mergeProb(TargetCount tc) {
      return tc.count / (Math.max(cooccurCount, totalMergedCount()) + 3.0f);
    }
  }

  public record SuffixRule(
      String rawSuffix, String normSuffix, int support, int totalRawSuffixSeen, float precision) {}

  public static final class CasingCounts {
    public int lower;
    public int title;
    public int upper;
    public int mixed;
    public int apostropheStem;
    public String bestMixedSurface;
    public int bestMixedCount;

    public int total() {
      return lower + title + upper + mixed;
    }
  }

  /** Fast primitive open-addressing {@code long -> int} hash table with zero object overhead. */
  static final class OpenLongIntMap {
    private final long[] keys;
    private final int[] vals;
    private final int mask;
    private int size;

    OpenLongIntMap(int initialPow2) {
      int cap = 1;
      while (cap < initialPow2) {
        cap <<= 1;
      }
      this.keys = new long[cap];
      this.vals = new int[cap];
      this.mask = cap - 1;
    }

    int get(long k) {
      if (k == 0L) {
        k = 1L;
      }
      int slot = slotOf(k) & mask;
      while (true) {
        long cur = keys[slot];
        if (cur == 0L) {
          return 0;
        }
        if (cur == k) {
          return vals[slot];
        }
        slot = (slot + 1) & mask;
      }
    }

    int size() {
      return size;
    }

    long estimatedBytes() {
      return (long) keys.length * Long.BYTES + (long) vals.length * Integer.BYTES + 32L;
    }

    static OpenLongIntMap deserialize(DataInputStream in) throws IOException {
      int cap = in.readInt();
      int count = in.readInt();
      OpenLongIntMap map = new OpenLongIntMap(cap);
      for (int i = 0; i < count; i++) {
        long k = in.readLong();
        int v = in.readInt();
        int slot = slotOf(k) & map.mask;
        while (map.keys[slot] != 0L) {
          slot = (slot + 1) & map.mask;
        }
        map.keys[slot] = k;
        map.vals[slot] = v;
      }
      map.size = count;
      return map;
    }

    private static int slotOf(long k) {
      long z = k * 0x9E3779B97F4A7C15L;
      z ^= (z >>> 32);
      return (int) z;
    }
  }

  /**
   * Lock-free primitive 64-bit hashed Unigram + Bigram + Trigram Turkish language model using
   * Witten-Bell / Jelinek-Mercer normalized probability interpolation.
   */
  public static final class CleanWordLm {
    static final long BOS_HASH = 0x517cc1b727220a95L;
    static final long EOS_HASH = 0x9e3779b97f4a7c15L;

    private final OpenLongIntMap unigrams;
    private final OpenLongIntMap bigrams;
    private final OpenLongIntMap trigrams;
    private final double denomUnigram;

    CleanWordLm(
        OpenLongIntMap unigrams,
        OpenLongIntMap bigrams,
        OpenLongIntMap trigrams,
        double denomUnigram) {
      this.unigrams = unigrams;
      this.bigrams = bigrams;
      this.trigrams = trigrams;
      this.denomUnigram = denomUnigram;
    }

    public static CleanWordLm empty() {
      return new CleanWordLm(
          new OpenLongIntMap(16), new OpenLongIntMap(16), new OpenLongIntMap(16), 250_000.0);
    }

    static CleanWordLm deserialize(DataInputStream in) throws IOException {
      double denom = in.readDouble();
      OpenLongIntMap u = OpenLongIntMap.deserialize(in);
      OpenLongIntMap b = OpenLongIntMap.deserialize(in);
      OpenLongIntMap t = OpenLongIntMap.deserialize(in);
      return new CleanWordLm(u, b, t, denom);
    }

    public int unigramCount(String lowerWord) {
      if (lowerWord == null || lowerWord.isEmpty()) {
        return 0;
      }
      return unigrams.get(wordHash(lowerWord));
    }

    public int bigramCount(String prevLower, String curLower) {
      long h1 = prevLower == null ? BOS_HASH : wordHash(prevLower);
      long h2 = curLower == null ? EOS_HASH : wordHash(curLower);
      return bigrams.get(bigramHash(h1, h2));
    }

    public float unigramLogProb(String lowerWord) {
      long h = lowerWord == null ? EOS_HASH : wordHash(lowerWord);
      return (float) Math.log(unigramProbByHash(h));
    }

    public float bigramLogProb(String prevLower, String curLower) {
      long h1 = prevLower == null ? BOS_HASH : wordHash(prevLower);
      long h2 = curLower == null ? EOS_HASH : wordHash(curLower);
      return (float) Math.log(bigramProbByHash(h1, h2));
    }

    public float trigramLogProb(String prev2Lower, String prev1Lower, String curLower) {
      long h1 = prev1Lower == null ? BOS_HASH : wordHash(prev1Lower);
      long h2 = curLower == null ? EOS_HASH : wordHash(curLower);
      if (prev1Lower == null) {
        return (float) Math.log(bigramProbByHash(BOS_HASH, h2));
      }
      long h0 = prev2Lower == null ? BOS_HASH : wordHash(prev2Lower);
      return (float) Math.log(trigramProbByHash(h0, h1, h2));
    }

    private double unigramProbByHash(long h) {
      int c = unigrams.get(h);
      return (c + 0.5) / denomUnigram;
    }

    private double bigramProbByHash(long hPrev, long hCur) {
      double p1 = unigramProbByHash(hCur);
      int cPrev = unigrams.get(hPrev);
      if (cPrev <= 0) {
        return p1;
      }
      double lambda2 = 0.82 * (cPrev / (cPrev + 8.0));
      int cBi = bigrams.get(bigramHash(hPrev, hCur));
      if (cBi <= 0) {
        return (1.0 - lambda2) * p1;
      }
      double mle2 = Math.min(1.0, cBi / (double) cPrev);
      return lambda2 * mle2 + (1.0 - lambda2) * p1;
    }

    private double trigramProbByHash(long hPrev2, long hPrev1, long hCur) {
      double p2 = bigramProbByHash(hPrev1, hCur);
      int cBiHist = bigrams.get(bigramHash(hPrev2, hPrev1));
      if (cBiHist <= 0) {
        return p2;
      }
      double lambda3 = 0.72 * (cBiHist / (cBiHist + 6.0));
      int cTri = trigrams.get(trigramHash(hPrev2, hPrev1, hCur));
      if (cTri <= 0) {
        return (1.0 - lambda3) * p2;
      }
      double mle3 = Math.min(1.0, cTri / (double) cBiHist);
      return lambda3 * mle3 + (1.0 - lambda3) * p2;
    }

    public long estimatedBytes() {
      return unigrams.estimatedBytes() + bigrams.estimatedBytes() + trigrams.estimatedBytes();
    }

    static long wordHash(String s) {
      long h = 0xcbf29ce484222325L;
      for (int i = 0; i < s.length(); i++) {
        h ^= s.charAt(i);
        h *= 0x100000001b3L;
      }
      h = mix64(h);
      if (h == 0L || h == BOS_HASH || h == EOS_HASH) {
        h ^= 0x27d4eb2f165667c5L;
      }
      return h;
    }

    static long bigramHash(long h1, long h2) {
      long z = mix64(h1 ^ 0x9E3779B97F4A7C15L) ^ mix64(h2 * 0xbf58476d1ce4e5b9L);
      return z == 0L ? 1L : z;
    }

    static long trigramHash(long h0, long h1, long h2) {
      long z = mix64(h0 * 0x94d049bb133111ebL);
      z = mix64(z ^ h1);
      z = mix64(z ^ (h2 * 0xbf58476d1ce4e5b9L));
      return z == 0L ? 1L : z;
    }

    private static long mix64(long z) {
      z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
      z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
      return z ^ (z >>> 31);
    }
  }

  /** Compact smoothed hashed character 4-gram Turkishness scorer (~512 KB). */
  public static final class CharPhonotacticModel {
    private static final int TABLE_BITS = 16;
    private static final int TABLE_SIZE = 1 << TABLE_BITS;
    private static final int MASK = TABLE_SIZE - 1;

    private final int[] ngram4Counts;
    private final int[] prefix3Counts;
    private final float defaultLogProb;

    CharPhonotacticModel(int[] ngram4Counts, int[] prefix3Counts) {
      this.ngram4Counts = ngram4Counts;
      this.prefix3Counts = prefix3Counts;
      this.defaultLogProb = -4.2f;
    }

    public static CharPhonotacticModel empty() {
      return new CharPhonotacticModel(new int[TABLE_SIZE], new int[TABLE_SIZE]);
    }

    public float scoreWord(String lowerWord) {
      if (lowerWord == null || lowerWord.isEmpty()) {
        return defaultLogProb;
      }
      float penalty = 0.0f;
      for (int i = 0; i < lowerWord.length(); i++) {
        char ch = lowerWord.charAt(i);
        if (ch == 'q' || ch == 'w' || ch == 'x') {
          penalty -= 1.35f;
        }
      }
      String padded = "^" + lowerWord + "$";
      if (padded.length() < 4) {
        return -2.4f + penalty;
      }
      float sum = 0.0f;
      int cnt = 0;
      for (int i = 0; i <= padded.length() - 4; i++) {
        int slot4 = hashChars(padded, i, 4) & MASK;
        int slot3 = hashChars(padded, i, 3) & MASK;
        int c4 = ngram4Counts[slot4];
        int c3 = Math.max(c4, prefix3Counts[slot3]);
        sum += c4 > 0 ? (float) Math.log((c4 + 0.5) / (c3 + 29.0)) : defaultLogProb;
        cnt++;
      }
      return (sum / Math.max(1, cnt)) + penalty;
    }

    public long estimatedBytes() {
      return (long) (ngram4Counts.length + prefix3Counts.length) * Integer.BYTES + 32L;
    }

    static CharPhonotacticModel deserialize(DataInputStream in) throws IOException {
      int len = in.readInt();
      int[] n4 = new int[len];
      int[] p3 = new int[len];
      for (int i = 0; i < len; i++) {
        n4[i] = in.readInt();
      }
      for (int i = 0; i < len; i++) {
        p3[i] = in.readInt();
      }
      return new CharPhonotacticModel(n4, p3);
    }

    static int hashChars(String s, int off, int len) {
      int h = 0x811c9dc5;
      for (int i = 0; i < len; i++) {
        h ^= s.charAt(off + i);
        h *= 0x01000193;
      }
      return h ^ (h >>> 16);
    }
  }

  public final Map<String, RewriteEntry> parallel1ToN;
  public final Map<String, MergeEntry> parallel2To1;
  public final Map<String, MergeEntry> parallel3To1;
  public final Map<String, List<SuffixRule>> suffixRulesByLastChar;
  public final Map<String, CasingCounts> casingLexicon;
  public final Map<String, Integer> cleanOpenBigrams;
  public final Map<String, Integer> cleanClosedWords;
  public final CleanWordLm cleanLm;
  public final CharPhonotacticModel charPhonotactics;
  public final int trainedSentenceCount;
  public final int cleanSentenceCount;

  public NormalizationKnowledge(
      Map<String, RewriteEntry> parallel1ToN,
      Map<String, MergeEntry> parallel2To1,
      Map<String, MergeEntry> parallel3To1,
      Map<String, List<SuffixRule>> suffixRulesByLastChar,
      Map<String, CasingCounts> casingLexicon,
      Map<String, Integer> cleanOpenBigrams,
      Map<String, Integer> cleanClosedWords,
      CleanWordLm cleanLm,
      CharPhonotacticModel charPhonotactics,
      int trainedSentenceCount,
      int cleanSentenceCount) {
    this.parallel1ToN = parallel1ToN;
    this.parallel2To1 = parallel2To1;
    this.parallel3To1 = parallel3To1;
    this.suffixRulesByLastChar = suffixRulesByLastChar;
    this.casingLexicon = casingLexicon;
    this.cleanOpenBigrams = cleanOpenBigrams;
    this.cleanClosedWords = cleanClosedWords;
    this.cleanLm = cleanLm;
    this.charPhonotactics = charPhonotactics;
    this.trainedSentenceCount = trainedSentenceCount;
    this.cleanSentenceCount = cleanSentenceCount;
  }

  /** Creates an empty knowledge container (useful for morphology/spellcheck-only unit tests). */
  public static NormalizationKnowledge empty() {
    return new NormalizationKnowledge(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        CleanWordLm.empty(),
        CharPhonotacticModel.empty(),
        0,
        0);
  }

  /** Loads a binary knowledge snapshot ({@code mined_v23.bin}) from disk. */
  public static NormalizationKnowledge load(Path path) throws IOException {
    try (InputStream fis = Files.newInputStream(path)) {
      return load(fis);
    }
  }

  /** Loads a binary knowledge snapshot ({@code mined_v23.bin}) from an {@link InputStream}. */
  public static NormalizationKnowledge load(InputStream stream) throws IOException {
    DataInputStream in = new DataInputStream(new BufferedInputStream(stream, 1 << 16));
    int magic = in.readInt();
    int ver = in.readInt();
    if (magic != SNAPSHOT_MAGIC || (ver != 2 && ver != SNAPSHOT_VERSION)) {
      throw new IOException(
          "Unexpected NormalizationKnowledge header: magic="
              + Integer.toHexString(magic)
              + " ver="
              + ver);
    }
    int trainedSents = in.readInt();
    int cleanSents = in.readInt();

    int n1 = in.readInt();
    Map<String, RewriteEntry> p1 = new HashMap<>(n1 * 4 / 3 + 16);
    for (int i = 0; i < n1; i++) {
      String k = in.readUTF();
      int tot = in.readInt();
      int keep = in.readInt();
      TargetCount[] targets = readTargets(in);
      p1.put(k, new RewriteEntry(tot, keep, targets));
    }

    Map<String, MergeEntry> p2 = readMergeMap(in);
    Map<String, MergeEntry> p3 = readMergeMap(in);

    int nSufKeys = in.readInt();
    Map<String, List<SuffixRule>> sufMap = new HashMap<>(nSufKeys * 4 / 3 + 16);
    for (int i = 0; i < nSufKeys; i++) {
      String chKey = in.readUTF();
      int cnt = in.readInt();
      List<SuffixRule> rules = new ArrayList<>(cnt);
      for (int j = 0; j < cnt; j++) {
        rules.add(
            new SuffixRule(in.readUTF(), in.readUTF(), in.readInt(), in.readInt(), in.readFloat()));
      }
      sufMap.put(chKey, rules);
    }

    int nCase = in.readInt();
    Map<String, CasingCounts> casing = new HashMap<>(nCase * 4 / 3 + 16);
    for (int i = 0; i < nCase; i++) {
      String w = in.readUTF();
      CasingCounts cc = new CasingCounts();
      cc.lower = in.readInt();
      cc.title = in.readInt();
      cc.upper = in.readInt();
      cc.mixed = in.readInt();
      cc.apostropheStem = in.readInt();
      if (in.readBoolean()) {
        cc.bestMixedSurface = in.readUTF();
        cc.bestMixedCount = in.readInt();
      }
      casing.put(w, cc);
    }

    Map<String, Integer> openBg = readStrIntMap(in);
    Map<String, Integer> closedW = readStrIntMap(in);
    CleanWordLm cleanLm = CleanWordLm.deserialize(in);
    CharPhonotacticModel phono = CharPhonotacticModel.deserialize(in);

    return new NormalizationKnowledge(
        p1, p2, p3, sufMap, casing, openBg, closedW, cleanLm, phono, trainedSents, cleanSents);
  }

  private static Map<String, MergeEntry> readMergeMap(DataInputStream in) throws IOException {
    int sz = in.readInt();
    Map<String, MergeEntry> out = new HashMap<>(sz * 4 / 3 + 16);
    for (int i = 0; i < sz; i++) {
      String k = in.readUTF();
      int co = in.readInt();
      out.put(k, new MergeEntry(co, readTargets(in)));
    }
    return out;
  }

  private static TargetCount[] readTargets(DataInputStream in) throws IOException {
    int k = in.readInt();
    TargetCount[] arr = new TargetCount[k];
    for (int i = 0; i < k; i++) {
      arr[i] = new TargetCount(in.readUTF(), in.readUTF(), in.readInt());
    }
    return arr;
  }

  private static Map<String, Integer> readStrIntMap(DataInputStream in) throws IOException {
    int sz = in.readInt();
    Map<String, Integer> out = new HashMap<>(sz * 4 / 3 + 16);
    for (int i = 0; i < sz; i++) {
      out.put(in.readUTF(), in.readInt());
    }
    return out;
  }

  /**
   * Restores natural Turkish casing (and stem capitalization before apostrophes) for a candidate
   * lowercase string {@code normLower}.
   */
  public String truecase(
      String normLower,
      CasingShape rawShape,
      String rawSurface,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      TurkishMorphology morphology) {
    return truecase(normLower, rawShape, rawSurface, capitalizeStart, sentenceAllCaps, morphology, 0);
  }

  /**
   * Context-aware deterministic Turkish truecaser (<b>B2 Layer 1</b>: emits a single canonical
   * surface string per candidate without duplicating lattice arcs).
   */
  public String truecase(
      String normLower,
      CasingShape rawShape,
      String rawSurface,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      TurkishMorphology morphology,
      int casingCtx) {
    if (normLower == null || normLower.isEmpty()) {
      return "";
    }
    int sp = normLower.indexOf(' ');
    if (sp > 0) {
      String[] parts = normLower.split(" ");
      int noisyFlag = casingCtx & CTX_SENT_TITLE_NOISY;
      StringBuilder sb = new StringBuilder(normLower.length() + 2);
      for (int i = 0; i < parts.length; i++) {
        if (i > 0) {
          sb.append(' ');
        }
        int partCtx =
            i == 0
                ? (casingCtx & (CTX_SENT_TITLE_NOISY | CTX_ADJ_DATE_NUM | CTX_AFTER_NUMBER))
                : (i == parts.length - 1
                    ? (noisyFlag | (casingCtx & CTX_BEFORE_GAZETTEER_OR_HONORIFIC))
                    : noisyFlag);
        if (parts.length == 2
            && i == 0
            && isTdkHolidayModifier(parts[0])
            && parts[1].startsWith("günü")) {
          partCtx |= CTX_PROMOTE_PROPER_IN_CONTEXT;
        } else if (parts.length == 2
            && i == 1
            && isTdkHolidayModifier(parts[0])
            && parts[1].startsWith("günü")) {
          partCtx |= CTX_PROMOTE_PROPER_IN_CONTEXT;
        }
        sb.append(
            truecaseSingleWord(
                parts[i],
                i == 0 ? rawShape : CasingShape.LOWER,
                i == 0 ? rawSurface : parts[i],
                i == 0 && capitalizeStart,
                sentenceAllCaps,
                morphology,
                partCtx));
      }
      return sb.toString();
    }
    return truecaseSingleWord(
        normLower,
        rawShape,
        rawSurface,
        capitalizeStart,
        sentenceAllCaps,
        morphology,
        casingCtx);
  }

  private String truecaseSingleWord(
      String wordLower,
      CasingShape rawShape,
      String rawSurface,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      TurkishMorphology morphology,
      int casingCtx) {
    boolean noisyTitleSent = (casingCtx & CTX_SENT_TITLE_NOISY) != 0;

    // 1. Canonical TDK dotted abbreviations ("DR."->"Dr.", "LTD."->"Ltd.", "VB."->"vb.") & personal initials
    String dotted = CANONICAL_DOTTED_ABBR.get(wordLower);
    if (dotted != null) {
      return capitalizeStart && Character.isLowerCase(dotted.charAt(0))
          ? NormV2Types.trCapitalize(dotted)
          : dotted;
    }
    if (wordLower.length() == 2
        && wordLower.charAt(1) == '.'
        && Character.isLetter(wordLower.charAt(0))) {
      if ((casingCtx & CTX_PROMOTE_PROPER_IN_CONTEXT) != 0
          || capitalizeStart
          || (!sentenceAllCaps && rawShape == CasingShape.TITLE)) {
        return NormV2Types.trUpper(wordLower);
      }
      return wordLower;
    }

    // 2. Canonical tech tokens ("2k"->"2K", "4k"->"4K", "i7"->"i7", "ghz"->"GHz", "seo"->"SEO", "php"->"PHP")
    String canonTech = CANONICAL_TECH_TOKENS.get(wordLower);
    if (canonTech != null) {
      return canonTech;
    }
    if (wordLower.equals("mah")) {
      if ((casingCtx & CTX_AFTER_NUMBER) != 0) {
        return "mAh";
      }
      if (!sentenceAllCaps
          && !noisyTitleSent
          && (rawShape == CasingShape.TITLE
              || (casingCtx & CTX_BEFORE_GAZETTEER_OR_HONORIFIC) != 0)) {
        return "Mah";
      }
      return capitalizeStart ? "Mah" : "mah";
    }

    // 3. Alphanumeric hardware/spec shapes ("4070Ti", "5600X", "13700HX", "550W", "3000-2450M", "10K'ya", "300Mbps")
    if (containsDigit(wordLower)) {
      String canonNumUnit = formatNumericTechSpecOrNull(wordLower);
      if (canonNumUnit != null) {
        return canonNumUnit;
      }
      if (!sentenceAllCaps && rawSurface != null) {
        String rawNormAp = rawSurface.replace('’', '\'');
        if (hasAnyUpperLetter(rawNormAp) && NormV2Types.trLower(rawNormAp).equals(wordLower)) {
          return rawNormAp;
        }
        int apIdx = wordLower.indexOf('\'');
        int rawApIdx = rawNormAp.indexOf('\'');
        if (apIdx > 0
            && rawApIdx == apIdx
            && hasAnyUpperLetter(rawNormAp.substring(0, rawApIdx))
            && NormV2Types.trLower(rawNormAp.substring(0, rawApIdx))
                .equals(wordLower.substring(0, apIdx))) {
          return rawNormAp.substring(0, rawApIdx) + wordLower.substring(apIdx);
        }
      }
    }

    // 4. Apostrophized stems ("Türkiye'de", "TR'de", "LLM'lerdeki", "SEO'ya", "WMAracı'nı" vs slang "entry'mde")
    int ap = wordLower.indexOf('\'');
    if (ap > 0) {
      String stemLower = wordLower.substring(0, ap);
      String tailLower = wordLower.substring(ap);
      if (isDigitsOrPunctOnly(stemLower)) {
        return stemLower + tailLower;
      }
      if (CANONICAL_UPPER_ACRONYM_STEMS.contains(stemLower)) {
        return NormV2Types.trUpper(stemLower) + tailLower;
      }
      if (!sentenceAllCaps && rawSurface != null) {
        String rawNormAp = rawSurface.replace('’', '\'');
        int rawAp = rawNormAp.indexOf('\'');
        String rawStem = rawAp > 0 ? rawNormAp.substring(0, rawAp) : rawNormAp;
        CasingShape stemShape = NormV2Types.detectShape(rawStem);
        if (stemShape == CasingShape.UPPER && stemLower.length() >= 2 && stemLower.length() <= 6) {
          return NormV2Types.trUpper(stemLower) + tailLower;
        }
        if (stemShape == CasingShape.MIXED
            && !noisyTitleSent
            && NormV2Types.trLower(rawStem).equals(stemLower)
            && !analyzeLexiconRole(stemLower, morphology).hasCommon()) {
          return rawStem + tailLower;
        }
      }
      CasingCounts stemCounts = casingLexicon.get(stemLower);
      if (stemCounts != null
          && stemCounts.bestMixedSurface != null
          && stemCounts.mixed >= 4
          && stemCounts.mixed > (stemCounts.lower + stemCounts.title)) {
        return stemCounts.bestMixedSurface + tailLower;
      }
      if (stemCounts != null
          && stemCounts.upper >= 5
          && stemCounts.upper > (stemCounts.lower + stemCounts.title) * 2
          && stemLower.length() >= 2
          && stemLower.length() <= 6) {
        return NormV2Types.trUpper(stemLower) + tailLower;
      }
      if (!sentenceAllCaps
          && rawShape == CasingShape.UPPER
          && stemLower.length() >= 2
          && stemLower.length() <= 6) {
        return NormV2Types.trUpper(stemLower) + tailLower;
      }
      if (!capitalizeStart && rawShape == CasingShape.LOWER) {
        if (stemCounts != null
            && stemCounts.lower >= 15
            && stemCounts.lower
                > (stemCounts.title + stemCounts.upper + stemCounts.apostropheStem) * 4) {
          return stemLower + tailLower;
        }
        boolean rawHadApostrophe =
            rawSurface != null
                && (rawSurface.indexOf('\'') >= 0 || rawSurface.indexOf('’') >= 0);
        if (rawHadApostrophe && LOWERCASE_APOSTROPHE_SLANG_STEMS.contains(stemLower)) {
          return stemLower + tailLower;
        }
      }
      return NormV2Types.trCapitalize(stemLower) + tailLower;
    }

    // 5. Foreign >5-char ALL-CAPS ASCII 'I' names/brands ("DAVID"->"David", "AIBA"->"AIBA")
    if (!sentenceAllCaps
        && rawShape == CasingShape.UPPER
        && rawSurface != null
        && rawSurface.indexOf('I') >= 0
        && !NormV2Types.hasUpperTurkishDiacritics(rawSurface)
        && wordLower.indexOf('ı') >= 0) {
      CasingCounts curCc = casingLexicon.get(wordLower);
      if ((curCc == null || curCc.total() < 3)
          && !analyzeLexiconRole(wordLower, morphology).hasAny()) {
        String iFolded = wordLower.replace('ı', 'i');
        CasingCounts altCc = casingLexicon.get(iFolded);
        if (altCc != null && altCc.total() >= 4) {
          wordLower = iFolded;
        }
      }
    }

    // 6. Contextual TDK promotion ("Sevgililer Günü", "Lale Hanım", "Ümit Özdağ", "Mimar Sinan")
    if (!sentenceAllCaps && (casingCtx & CTX_PROMOTE_PROPER_IN_CONTEXT) != 0) {
      return NormV2Types.trCapitalize(wordLower);
    }

    // 7. TDK Month & Day rule: lowercase when raw is lowercase without adjacent date/gazetteer context
    if (TDK_MONTHS_AND_DAYS.contains(wordLower)) {
      boolean dateOrGazCtx =
          (casingCtx & (CTX_ADJ_DATE_NUM | CTX_BEFORE_GAZETTEER_OR_HONORIFIC)) != 0;
      boolean explicitAuthorTitle =
          !sentenceAllCaps && !noisyTitleSent && rawShape == CasingShape.TITLE;
      if (capitalizeStart || dateOrGazCtx || explicitAuthorTitle) {
        return NormV2Types.trCapitalize(wordLower);
      }
      return wordLower;
    }

    // 8. Clean-corpus casing lexicon (guarded against headline Title-Case bias on common & English words)
    CasingCounts cc = casingLexicon.get(wordLower);
    if (cc != null) {
      int tot = cc.total();
      if (cc.bestMixedSurface != null
          && cc.mixed >= 4
          && cc.mixed > (cc.lower + cc.title + cc.upper)) {
        return cc.bestMixedSurface;
      }
      if (cc.upper >= 8
          && cc.upper > (cc.lower + cc.title) * 2
          && wordLower.length() >= 2
          && wordLower.length() <= 6) {
        return NormV2Types.trUpper(wordLower);
      }
      if (tot >= 6 && cc.title > (cc.lower * 3) && cc.title >= 5) {
        if (!capitalizeStart && rawShape == CasingShape.LOWER) {
          if (!LOWERCASE_HEADLINE_BIAS_STOPWORDS.contains(wordLower)) {
            return NormV2Types.trCapitalize(wordLower);
          }
        } else {
          return NormV2Types.trCapitalize(wordLower);
        }
      }
    }

    // 9. Check Zemberek lexicon when unseen/rare in corpus casing table
    if ((cc == null || (cc.total() < 5 && cc.lower < 2)) && wordLower.length() >= 2) {
      LexiconRole role = analyzeLexiconRole(wordLower, morphology);
      if (!role.hasCommon() && role.onlyBareAbbr() && wordLower.length() <= 5) {
        return NormV2Types.trUpper(wordLower);
      }
      if (!role.hasCommon() && role.hasProperNoApostrophe()) {
        return NormV2Types.trCapitalize(wordLower);
      }
    }

    // 10. Preserve explicit author CamelCase / hyphenated compounds ("IPS-Level", "AtanamayanTarım", "YouTuberların")
    if (!sentenceAllCaps
        && !noisyTitleSent
        && rawShape == CasingShape.MIXED
        && rawSurface != null) {
      String rawNorm = rawSurface.replace('’', '\'');
      if (wordLower.indexOf('-') > 0 && NormV2Types.trLower(rawNorm).equals(wordLower)) {
        return rawNorm;
      }
      if ((cc == null || cc.lower < 5)
          && !isShiftSlipPrefix(rawNorm)
          && !analyzeLexiconRole(wordLower, morphology).hasCommon()) {
        if (NormV2Types.trLower(rawNorm).equals(wordLower)) {
          return rawNorm;
        }
        String rawLow = NormV2Types.trLower(rawNorm);
        if (rawLow.length() >= 4
            && wordLower.length() > rawLow.length()
            && wordLower.startsWith(rawLow)) {
          return rawNorm + wordLower.substring(rawNorm.length());
        }
      }
    }

    // 11. Preserve explicit mid-sentence TitleCase on OOV names/brands & Gazetteer modifiers (suppressed in noisy Every-Word-Title sentences)
    if (!sentenceAllCaps && rawShape == CasingShape.TITLE && !capitalizeStart) {
      if (!noisyTitleSent) {
        if ((casingCtx & CTX_BEFORE_GAZETTEER_OR_HONORIFIC) != 0
            || cc == null
            || cc.lower < 25
            || cc.title * 5 >= cc.lower) {
          return NormV2Types.trCapitalize(wordLower);
        }
      } else if ((cc == null || cc.lower < 3)
          && !analyzeLexiconRole(wordLower, morphology).hasCommon()) {
        return NormV2Types.trCapitalize(wordLower);
      }
    }

    // 12. Preserve explicit mid-sentence UPPER acronyms & tech brands ("ATT", "RYZEN", "HÜDA", "AIBA")
    if (!sentenceAllCaps && rawShape == CasingShape.UPPER && wordLower.length() >= 2) {
      if (wordLower.length() <= 5 && (cc == null || cc.lower < 15)) {
        return NormV2Types.trUpper(wordLower);
      }
      if (wordLower.length() <= 8
          && cc != null
          && cc.upper >= 4
          && cc.upper > (cc.lower + cc.title) * 2) {
        return NormV2Types.trUpper(wordLower);
      }
    }

    if (capitalizeStart) {
      return NormV2Types.trCapitalize(wordLower);
    }
    return wordLower;
  }

  private record LexiconRole(
      boolean hasCommon, boolean onlyBareAbbr, boolean hasProperNoApostrophe, boolean hasAnyProper) {
    boolean hasProperOrAbbr() {
      return onlyBareAbbr || hasProperNoApostrophe || hasAnyProper;
    }

    boolean hasAny() {
      return hasCommon || hasProperOrAbbr();
    }
  }

  private static LexiconRole analyzeLexiconRole(String wordLower, TurkishMorphology morphology) {
    if (morphology == null || wordLower.length() < 2) {
      return new LexiconRole(false, false, false, false);
    }
    boolean onlyBareAbbr = false;
    boolean properNoAp = false;
    boolean anyProper = false;
    boolean hasCommon = false;
    for (SingleAnalysis a : morphology.analyze(wordLower)) {
      if (a.isUnknown() || a.isRuntime()) {
        continue;
      }
      SecondaryPos sp = a.getDictionaryItem().secondaryPos;
      if (sp == SecondaryPos.Abbreviation) {
        anyProper = true;
        if (a.getEnding().isEmpty()) {
          onlyBareAbbr = true;
        }
      } else if (sp == SecondaryPos.ProperNoun) {
        anyProper = true;
        if (a.getEnding().isEmpty()) {
          properNoAp = true;
        } else if (!SpanCandidateGenerator.properNounTakesApostropheTDK(a)
            && !a.getMorphemes().contains(TurkishMorphotactics.a3pl)) {
          properNoAp = true;
        }
      } else {
        hasCommon = true;
      }
    }
    return new LexiconRole(hasCommon, onlyBareAbbr, properNoAp, anyProper);
  }

  private static boolean isTdkHolidayModifier(String w) {
    return w.equals("sevgililer")
        || w.equals("anneler")
        || w.equals("babalar")
        || w.equals("öğretmenler")
        || w.equals("kadınlar");
  }

  private static boolean hasAnyUpperLetter(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLetter(s.charAt(i)) && Character.isUpperCase(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isShiftSlipPrefix(String s) {
    if (s.length() < 3
        || !Character.isUpperCase(s.charAt(0))
        || !Character.isUpperCase(s.charAt(1))) {
      return false;
    }
    for (int i = 2; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isLetter(c) && Character.isUpperCase(c)) {
        return false;
      }
    }
    return true;
  }

  private static String formatNumericTechSpecOrNull(String wordLower) {
    int n = wordLower.length();
    int dEnd = 0;
    while (dEnd < n && Character.isDigit(wordLower.charAt(dEnd))) {
      dEnd++;
    }
    if (dEnd == 0 || dEnd >= n) {
      return null;
    }
    String digits = wordLower.substring(0, dEnd);
    String unit = wordLower.substring(dEnd);
    return switch (unit) {
      case "ghz" -> digits + "GHz";
      case "mhz" -> digits + "MHz";
      case "khz" -> digits + "kHz";
      case "hz" -> digits + "Hz";
      case "mbps" -> digits + "Mbps";
      case "gbps" -> digits + "Gbps";
      case "kbps" -> digits + "Kbps";
      case "mah" -> digits + "mAh";
      default -> null;
    };
  }

  public boolean isKnownProperOrAcronymStem(String stemLower) {
    CasingCounts cc = casingLexicon.get(stemLower);
    if (cc == null || stemLower.length() < 2) {
      return false;
    }
    int propVotes = cc.title + cc.upper + cc.mixed;
    if (cc.apostropheStem >= 2 && (propVotes + cc.apostropheStem * 2) > cc.lower * 2) {
      return true;
    }
    return propVotes >= 4 && propVotes > cc.lower * 4;
  }

  public boolean isKnownAcronymStem(String stemLower) {
    CasingCounts cc = casingLexicon.get(stemLower);
    if (cc == null || stemLower.length() < 2 || stemLower.length() > 6) {
      return false;
    }
    return cc.upper >= 6 && cc.upper > (cc.lower + cc.title) * 2;
  }

  public boolean isKnownBrandMixedCase(String wordLower) {
    CasingCounts cc = casingLexicon.get(wordLower);
    return cc != null
        && cc.bestMixedSurface != null
        && cc.mixed >= 3
        && cc.mixed > (cc.lower + cc.title);
  }

  public long estimatedBytes() {
    long bytes = cleanLm.estimatedBytes() + charPhonotactics.estimatedBytes();
    bytes += (parallel1ToN.size() + parallel2To1.size() + parallel3To1.size()) * 96L;
    bytes += casingLexicon.size() * 64L;
    bytes += (cleanOpenBigrams.size() + cleanClosedWords.size()) * 48L;
    return bytes;
  }

  private static boolean containsDigit(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isDigit(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDigitsOrPunctOnly(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLetter(s.charAt(i))) {
        return false;
      }
    }
    return !s.isEmpty();
  }
}
