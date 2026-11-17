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
import java.util.zip.GZIPInputStream;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.normalization.v2.NormV2Types.CasingShape;

/**
 * Runtime linguistic and empirical knowledge tables used by {@link SpanCandidateGenerator} and
 * {@link SpanLatticePerceptronNormalizer}: {@code 1:1}/{@code 1:2} rewrites, {@code 2:1}/{@code 3:1}
 * merges, productive suffix rules, clean-corpus casing statistics, {@link CleanWordLm}, {@link
 * CharPhonotacticModel}, and context-aware Turkish truecasing.
 */
public final class NormalizationKnowledge {

  static final int SNAPSHOT_MAGIC = 0x5A4E5632; // "ZNV2"
  static final int SNAPSHOT_VERSION_LEGACY = 3;
  static final int SNAPSHOT_VERSION = 5;
  static final String DEFAULT_RESOURCE = "/normalization/v2/knowledge.bin.gz";

  // Contextual Truecasing Bitflags
  public static final int CTX_SENT_TITLE_NOISY = 1 << 0;
  public static final int CTX_ADJ_DATE_NUM = 1 << 1;
  public static final int CTX_BEFORE_GAZETTEER_OR_HONORIFIC = 1 << 2;
  public static final int CTX_AFTER_NUMBER = 1 << 3;
  public static final int CTX_PROMOTE_PROPER_IN_CONTEXT = 1 << 4;

  private static final Set<String> TDK_MONTHS_AND_DAYS =
      Set.of(
          "ocak", "şubat", "mart", "nisan", "mayıs", "haziran", "temmuz", "ağustos", "eylül",
          "ekim", "kasım", "aralık", "pazartesi", "salı", "çarşamba", "perşembe", "cuma",
          "cumartesi", "pazar");

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

  private static final Set<String> TDK_HOLIDAY_MODIFIERS =
      Set.of("sevgililer", "anneler", "babalar", "öğretmenler", "kadınlar");

  private static final Map<String, String> CANONICAL_DOTTED_ABBR =
      Map.ofEntries(
          Map.entry("vb.", "vb."),
          Map.entry("vs.", "vs."),
          Map.entry("dr.", "Dr."),
          Map.entry("prof.", "Prof."),
          Map.entry("doç.", "Doç."),
          Map.entry("av.", "Av."),
          Map.entry("org.", "Org."),
          Map.entry("alb.", "Alb."),
          Map.entry("yrd.", "Yrd."),
          Map.entry("arş.", "Arş."),
          Map.entry("gör.", "Gör."),
          Map.entry("uzm.", "Uzm."),
          Map.entry("ecz.", "Ecz."),
          Map.entry("müh.", "Müh."),
          Map.entry("mim.", "Mim."),
          Map.entry("vet.", "Vet."),
          Map.entry("hz.", "Hz."),
          Map.entry("ltd.", "Ltd."),
          Map.entry("tic.", "Tic."),
          Map.entry("şti.", "Şti."),
          Map.entry("san.", "San."),
          Map.entry("mah.", "Mah."),
          Map.entry("cad.", "Cad."),
          Map.entry("sok.", "Sok."),
          Map.entry("apt.", "Apt."),
          Map.entry("no.", "No."),
          Map.entry("tel.", "Tel."));

  private static final Map<String, String> CANONICAL_TECH_TOKENS =
      Map.ofEntries(
          Map.entry("2k", "2K"),
          Map.entry("4k", "4K"),
          Map.entry("8k", "8K"),
          Map.entry("i3", "i3"),
          Map.entry("i5", "i5"),
          Map.entry("i7", "i7"),
          Map.entry("i9", "i9"),
          Map.entry("ghz", "GHz"),
          Map.entry("mhz", "MHz"),
          Map.entry("mbps", "Mbps"),
          Map.entry("gbps", "Gbps"),
          Map.entry("mp3", "MP3"),
          Map.entry("mp4", "MP4"),
          Map.entry("seo", "SEO"),
          Map.entry("php", "PHP"),
          Map.entry("html", "HTML"),
          Map.entry("sql", "SQL"));

  private static final CasingCounts[] PURE_LOWER_CACHE = new CasingCounts[129];

  static {
    for (int i = 1; i < PURE_LOWER_CACHE.length; i++) {
      CasingCounts cc = new CasingCounts();
      cc.lower = i;
      PURE_LOWER_CACHE[i] = cc;
    }
  }

  public record TargetCount(String normLower, int count) {}

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
  }

  public record MergeEntry(int cooccurCount, TargetCount[] mergedTargets) {
    public int totalMergedCount() {
      int sum = 0;
      for (TargetCount tc : mergedTargets) {
        sum += tc.count;
      }
      return sum;
    }

    public float mergeProb(TargetCount tc) {
      return tc.count / (Math.max(cooccurCount, totalMergedCount()) + 3.0f);
    }
  }

  public record SuffixRule(String rawSuffix, String normSuffix, int support, float precision) {}

  public static final class CasingCounts {
    public int lower;
    public int title;
    public int upper;
    public int mixed;
    public int apostropheStem;
    public String bestMixedSurface;

    public int total() {
      return lower + title + upper + mixed;
    }
  }

  // =========================================================================
  // Clean 3-Gram Language Model & Phonotactic Scorer
  // =========================================================================

  interface LongCountMap {
    int get(long k);
  }

  /** Primitive open-addressing {@code long -> int} hash table (used by legacy v2/v3 snapshots). */
  static final class OpenLongIntMap implements LongCountMap {
    private final long[] keys;
    private final int[] vals;
    private final int mask;

    OpenLongIntMap(int initialPow2) {
      int cap = 1;
      while (cap < initialPow2) {
        cap <<= 1;
      }
      this.keys = new long[cap];
      this.vals = new int[cap];
      this.mask = cap - 1;
    }

    @Override
    public int get(long k) {
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

    static OpenLongIntMap deserialize(DataInputStream in) throws IOException {
      int cap = in.readInt();
      int count = in.readInt();
      OpenLongIntMap map = new OpenLongIntMap(cap);
      for (int i = 0; i < count; i++) {
        map.putUnique(in.readLong(), in.readInt());
      }
      return map;
    }

    private void putUnique(long k, int v) {
      int slot = slotOf(k) & mask;
      while (keys[slot] != 0L) {
        slot = (slot + 1) & mask;
      }
      keys[slot] = k;
      vals[slot] = v;
    }

    private static int slotOf(long k) {
      long z = k * 0x9E3779B97F4A7C15L;
      z ^= (z >>> 32);
      return (int) z;
    }
  }

  /**
   * Compact MPHF-backed {@code long -> int} count table using {@link MultiLevelMphf}, 8/12/16-bit
   * collision fingerprints, and a 256-entry count codebook.
   */
  static final class MphfCountMap implements LongCountMap {
    private final MultiLevelMphf mphf;
    private final int fpBits;
    private final short[] fp16;
    private final byte[] fpPacked;
    private final byte[] codes;
    private final int[] codebook;

    MphfCountMap(
        MultiLevelMphf mphf,
        int fpBits,
        short[] fp16,
        byte[] fpPacked,
        byte[] codes,
        int[] codebook) {
      this.mphf = mphf;
      this.fpBits = fpBits;
      this.fp16 = fp16;
      this.fpPacked = fpPacked;
      this.codes = codes;
      this.codebook = codebook;
    }

    @Override
    public int get(long k) {
      if (mphf == null || codes.length == 0) {
        return 0;
      }
      if (k == 0L) {
        k = 1L;
      }
      int k0 = (int) k;
      int k1 = (int) (k >>> 32);
      int slot = mphf.get(k0, k1, MultiLevelMphf.hash(k0, k1, -1));
      int wantFp = fingerprint(k, fpBits);
      int gotFp =
          switch (fpBits) {
            case 16 -> fp16[slot] & 0xFFFF;
            case 8 -> fpPacked[slot] & 0xFF;
            default -> get12(fpPacked, slot);
          };
      return gotFp == wantFp ? codebook[codes[slot] & 0xFF] : 0;
    }

    static MphfCountMap deserialize(DataInputStream in) throws IOException {
      int fpBits = in.readUnsignedByte();
      int count = readVarInt(in);
      if (count == 0) {
        return new MphfCountMap(null, fpBits, new short[0], new byte[0], new byte[0], new int[256]);
      }
      MultiLevelMphf mphf = MultiLevelMphf.deserialize(in);
      short[] fp16 = null;
      byte[] fpPacked = null;
      if (fpBits == 16) {
        fp16 = new short[count];
        for (int i = 0; i < count; i++) {
          fp16[i] = in.readShort();
        }
      } else if (fpBits == 8) {
        fpPacked = new byte[count];
        in.readFully(fpPacked);
      } else {
        fpPacked = new byte[(count * 3 + 1) / 2];
        in.readFully(fpPacked);
      }
      byte[] codes = new byte[count];
      in.readFully(codes);
      int[] codebook = new int[256];
      for (int i = 0; i < 256; i++) {
        codebook[i] = readVarInt(in);
      }
      return new MphfCountMap(mphf, fpBits, fp16, fpPacked, codes, codebook);
    }

    static int fingerprint(long k, int bits) {
      long z = (k ^ (k >>> 23) ^ (k >>> 47)) * 0x9E3779B97F4A7C15L;
      return ((int) (z >>> (64 - bits))) & ((1 << bits) - 1);
    }

    private static int get12(byte[] arr, int idx) {
      int byteIdx = (idx * 3) >>> 1;
      return (idx & 1) == 0
          ? (((arr[byteIdx] & 0xFF) << 4) | ((arr[byteIdx + 1] & 0xF0) >>> 4))
          : (((arr[byteIdx] & 0x0F) << 8) | (arr[byteIdx + 1] & 0xFF));
    }
  }

  /** Lock-free 64-bit hashed Unigram + Bigram + Trigram Turkish language model. */
  public static final class CleanWordLm {
    static final long BOS_HASH = 0x517cc1b727220a95L;
    static final long EOS_HASH = 0x9e3779b97f4a7c15L;

    private final LongCountMap unigrams;
    private final LongCountMap bigrams;
    private final LongCountMap trigrams;
    private final double denomUnigram;

    CleanWordLm(
        LongCountMap unigrams, LongCountMap bigrams, LongCountMap trigrams, double denomUnigram) {
      this.unigrams = unigrams;
      this.bigrams = bigrams;
      this.trigrams = trigrams;
      this.denomUnigram = denomUnigram;
    }

    public static CleanWordLm empty() {
      LongCountMap empty = new OpenLongIntMap(16);
      return new CleanWordLm(empty, empty, empty, 250_000.0);
    }

    static CleanWordLm deserialize(DataInputStream in, boolean v5) throws IOException {
      double denom = in.readDouble();
      if (v5) {
        return new CleanWordLm(
            MphfCountMap.deserialize(in),
            MphfCountMap.deserialize(in),
            MphfCountMap.deserialize(in),
            denom);
      }
      return new CleanWordLm(
          OpenLongIntMap.deserialize(in),
          OpenLongIntMap.deserialize(in),
          OpenLongIntMap.deserialize(in),
          denom);
    }

    public int unigramCount(String lowerWord) {
      return lowerWord == null || lowerWord.isEmpty() ? 0 : unigrams.get(wordHash(lowerWord));
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
      return (unigrams.get(h) + 0.5) / denomUnigram;
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
      return lambda2 * Math.min(1.0, cBi / (double) cPrev) + (1.0 - lambda2) * p1;
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
      return lambda3 * Math.min(1.0, cTri / (double) cBiHist) + (1.0 - lambda3) * p2;
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

  /** Smoothed hashed character 4-gram Turkishness scorer. */
  public static final class CharPhonotacticModel {
    private static final int TABLE_BITS = 16;
    private static final int TABLE_SIZE = 1 << TABLE_BITS;
    private static final int MASK = TABLE_SIZE - 1;
    private static final float DEFAULT_LOG_PROB = -4.2f;

    private final int[] ngram4Counts;
    private final int[] prefix3Counts;

    CharPhonotacticModel(int[] ngram4Counts, int[] prefix3Counts) {
      this.ngram4Counts = ngram4Counts;
      this.prefix3Counts = prefix3Counts;
    }

    public static CharPhonotacticModel empty() {
      return new CharPhonotacticModel(new int[TABLE_SIZE], new int[TABLE_SIZE]);
    }

    public float scoreWord(String lowerWord) {
      if (lowerWord == null || lowerWord.isEmpty()) {
        return DEFAULT_LOG_PROB;
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
        int c4 = ngram4Counts[hashChars(padded, i, 4) & MASK];
        int c3 = Math.max(c4, prefix3Counts[hashChars(padded, i, 3) & MASK]);
        sum += c4 > 0 ? (float) Math.log((c4 + 0.5) / (c3 + 29.0)) : DEFAULT_LOG_PROB;
        cnt++;
      }
      return (sum / Math.max(1, cnt)) + penalty;
    }

    static CharPhonotacticModel deserialize(DataInputStream in, boolean v5) throws IOException {
      int len = v5 ? readVarInt(in) : in.readInt();
      int[] n4 = new int[len];
      int[] p3 = new int[len];
      if (v5) {
        byte[] q4 = new byte[len];
        byte[] q3 = new byte[len];
        in.readFully(q4);
        in.readFully(q3);
        int[] cb = new int[256];
        for (int i = 0; i < 256; i++) {
          cb[i] = readVarInt(in);
        }
        for (int i = 0; i < len; i++) {
          int c4 = q4[i] & 0xFF;
          int c3 = q3[i] & 0xFF;
          if (c4 > 0) n4[i] = cb[c4 - 1];
          if (c3 > 0) p3[i] = cb[c3 - 1];
        }
        return new CharPhonotacticModel(n4, p3);
      }
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

  // =========================================================================
  // Fields, Factories & Binary Deserialization
  // =========================================================================

  public final Map<String, RewriteEntry> parallel1ToN;
  public final Map<String, MergeEntry> parallel2To1;
  public final Map<String, MergeEntry> parallel3To1;
  public final Map<String, List<SuffixRule>> suffixRulesByLastChar;
  public final Map<String, CasingCounts> casingLexicon;
  public final List<String> shorthandWords;
  public final CleanWordLm cleanLm;
  public final CharPhonotacticModel charPhonotactics;

  public NormalizationKnowledge(
      Map<String, RewriteEntry> parallel1ToN,
      Map<String, MergeEntry> parallel2To1,
      Map<String, MergeEntry> parallel3To1,
      Map<String, List<SuffixRule>> suffixRulesByLastChar,
      Map<String, CasingCounts> casingLexicon,
      List<String> shorthandWords,
      CleanWordLm cleanLm,
      CharPhonotacticModel charPhonotactics) {
    this.parallel1ToN = parallel1ToN;
    this.parallel2To1 = parallel2To1;
    this.parallel3To1 = parallel3To1;
    this.suffixRulesByLastChar = suffixRulesByLastChar;
    this.casingLexicon = casingLexicon;
    this.shorthandWords = shorthandWords;
    this.cleanLm = cleanLm;
    this.charPhonotactics = charPhonotactics;
  }

  /** Creates an empty knowledge container (useful for morphology/spellcheck-only unit tests). */
  public static NormalizationKnowledge empty() {
    return new NormalizationKnowledge(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        List.of(),
        CleanWordLm.empty(),
        CharPhonotacticModel.empty());
  }

  /**
   * Looks up empirical casing counts for {@code wordLower}, falling back to pure-lowercase clean
   * unigram frequency when pure-lowercase entries were omitted from {@code casingLexicon}.
   */
  public CasingCounts lookupCasing(String wordLower) {
    CasingCounts cc = casingLexicon.get(wordLower);
    if (cc != null) {
      return cc;
    }
    int u = cleanLm.unigramCount(wordLower);
    if (u <= 0) {
      return null;
    }
    if (u < PURE_LOWER_CACHE.length) {
      return PURE_LOWER_CACHE[u];
    }
    CasingCounts syn = new CasingCounts();
    syn.lower = u;
    return syn;
  }

  /** Loads the bundled default knowledge snapshot ({@code /normalization/v2/knowledge.bin.gz}). */
  public static NormalizationKnowledge loadDefault() throws IOException {
    try (InputStream is = NormalizationKnowledge.class.getResourceAsStream(DEFAULT_RESOURCE)) {
      if (is == null) {
        throw new IOException("Missing classpath resource: " + DEFAULT_RESOURCE);
      }
      return load(is);
    }
  }

  /** Loads a binary knowledge snapshot ({@code knowledge.bin.gz} or {@code knowledge.bin}) from disk. */
  public static NormalizationKnowledge load(Path path) throws IOException {
    try (InputStream fis = Files.newInputStream(path)) {
      return load(fis);
    }
  }

  static InputStream maybeUnzip(InputStream raw) throws IOException {
    BufferedInputStream buf = new BufferedInputStream(raw, 1 << 16);
    buf.mark(2);
    int b0 = buf.read();
    int b1 = buf.read();
    buf.reset();
    if (b0 == 0x1f && b1 == 0x8b) {
      return new BufferedInputStream(new GZIPInputStream(buf, 1 << 16), 1 << 16);
    }
    return buf;
  }

  static int readVarInt(DataInputStream in) throws IOException {
    int val = 0;
    int shift = 0;
    while (true) {
      int b = in.readUnsignedByte();
      val |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return val;
      }
      shift += 7;
    }
  }

  /** Loads a binary knowledge snapshot from an {@link InputStream} (plain or gzip-compressed). */
  public static NormalizationKnowledge load(InputStream stream) throws IOException {
    DataInputStream in = new DataInputStream(maybeUnzip(stream));
    int magic = in.readInt();
    int ver = in.readInt();
    if (magic != SNAPSHOT_MAGIC || ver < 2 || ver > SNAPSHOT_VERSION) {
      throw new IOException(
          "Unexpected NormalizationKnowledge header: magic="
              + Integer.toHexString(magic)
              + " ver="
              + ver);
    }
    boolean v5 = ver >= SNAPSHOT_VERSION;
    if (!v5) {
      in.readInt(); // trainedSentenceCount
      in.readInt(); // cleanSentenceCount
    }

    Map<String, RewriteEntry> p1 = readParallel1ToN(in, v5);
    Map<String, MergeEntry> p2 = readMergeMap(in, v5);
    Map<String, MergeEntry> p3 = readMergeMap(in, v5);
    Map<String, List<SuffixRule>> sufMap = readSuffixRules(in, v5);
    Map<String, CasingCounts> casing = readCasingLexicon(in, v5);

    List<String> shorthandList;
    if (v5) {
      shorthandList = List.of(readFrontCodedStrings(in));
    } else {
      skipLegacyStrIntMap(in); // cleanOpenBigrams
      skipLegacyStrIntMap(in); // cleanClosedWords
      shorthandList = List.of();
    }

    CleanWordLm cleanLm = CleanWordLm.deserialize(in, v5);
    CharPhonotacticModel phono = CharPhonotacticModel.deserialize(in, v5);
    return new NormalizationKnowledge(p1, p2, p3, sufMap, casing, shorthandList, cleanLm, phono);
  }

  private static Map<String, RewriteEntry> readParallel1ToN(DataInputStream in, boolean v5)
      throws IOException {
    if (v5) {
      String[] keys = readFrontCodedStrings(in);
      Map<String, RewriteEntry> p1 = new HashMap<>(keys.length * 4 / 3 + 16);
      for (String k : keys) {
        p1.put(k, new RewriteEntry(readVarInt(in), readVarInt(in), readTargets(in, true)));
      }
      return p1;
    }
    int n1 = in.readInt();
    Map<String, RewriteEntry> p1 = new HashMap<>(n1 * 4 / 3 + 16);
    for (int i = 0; i < n1; i++) {
      p1.put(in.readUTF(), new RewriteEntry(in.readInt(), in.readInt(), readTargets(in, false)));
    }
    return p1;
  }

  private static Map<String, List<SuffixRule>> readSuffixRules(DataInputStream in, boolean v5)
      throws IOException {
    int nSufKeys = v5 ? readVarInt(in) : in.readInt();
    Map<String, List<SuffixRule>> sufMap = new HashMap<>(nSufKeys * 4 / 3 + 16);
    for (int i = 0; i < nSufKeys; i++) {
      String chKey = in.readUTF();
      int cnt = v5 ? readVarInt(in) : in.readInt();
      List<SuffixRule> rules = new ArrayList<>(cnt);
      for (int j = 0; j < cnt; j++) {
        String rawSuf = in.readUTF();
        String normSuf = in.readUTF();
        int support = v5 ? readVarInt(in) : in.readInt();
        if (!v5) {
          in.readInt(); // totalRawSuffixSeen
        }
        rules.add(new SuffixRule(rawSuf, normSuf, support, in.readFloat()));
      }
      sufMap.put(chKey, rules);
    }
    return sufMap;
  }

  private static Map<String, CasingCounts> readCasingLexicon(DataInputStream in, boolean v5)
      throws IOException {
    if (v5) {
      String[] cKeys = readFrontCodedStrings(in);
      Map<String, CasingCounts> casing = new HashMap<>(cKeys.length * 4 / 3 + 16);
      for (String w : cKeys) {
        int flags = in.readUnsignedByte();
        CasingCounts cc = new CasingCounts();
        if ((flags & 1) != 0) cc.lower = readVarInt(in);
        if ((flags & 2) != 0) cc.title = readVarInt(in);
        if ((flags & 4) != 0) cc.upper = readVarInt(in);
        if ((flags & 8) != 0) cc.mixed = readVarInt(in);
        if ((flags & 16) != 0) cc.apostropheStem = readVarInt(in);
        if ((flags & 32) != 0) cc.bestMixedSurface = in.readUTF();
        casing.put(w, cc);
      }
      return casing;
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
        in.readInt(); // bestMixedCount
      }
      casing.put(w, cc);
    }
    return casing;
  }

  private static String[] readFrontCodedStrings(DataInputStream in) throws IOException {
    int n = readVarInt(in);
    String[] out = new String[n];
    String prev = "";
    for (int i = 0; i < n; i++) {
      int shared = readVarInt(in);
      String suffix = in.readUTF();
      String cur = shared == 0 ? suffix : prev.substring(0, shared) + suffix;
      out[i] = cur;
      prev = cur;
    }
    return out;
  }

  private static Map<String, MergeEntry> readMergeMap(DataInputStream in, boolean v5)
      throws IOException {
    if (v5) {
      String[] keys = readFrontCodedStrings(in);
      Map<String, MergeEntry> out = new HashMap<>(keys.length * 4 / 3 + 16);
      for (String k : keys) {
        out.put(k, new MergeEntry(readVarInt(in), readTargets(in, true)));
      }
      return out;
    }
    int sz = in.readInt();
    Map<String, MergeEntry> out = new HashMap<>(sz * 4 / 3 + 16);
    for (int i = 0; i < sz; i++) {
      out.put(in.readUTF(), new MergeEntry(in.readInt(), readTargets(in, false)));
    }
    return out;
  }

  private static TargetCount[] readTargets(DataInputStream in, boolean v5) throws IOException {
    int k = v5 ? readVarInt(in) : in.readInt();
    TargetCount[] arr = new TargetCount[k];
    for (int i = 0; i < k; i++) {
      String normLower = in.readUTF();
      if (!v5) {
        in.readUTF(); // legacy bestSurface
      }
      arr[i] = new TargetCount(normLower, v5 ? readVarInt(in) : in.readInt());
    }
    return arr;
  }

  private static void skipLegacyStrIntMap(DataInputStream in) throws IOException {
    int sz = in.readInt();
    for (int i = 0; i < sz; i++) {
      in.readUTF();
      in.readInt();
    }
  }

  // =========================================================================
  // Context-Aware Deterministic Turkish Truecasing
  // =========================================================================

  public String truecase(
      String normLower,
      CasingShape rawShape,
      String rawSurface,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      TurkishMorphology morphology) {
    return truecase(normLower, rawShape, rawSurface, capitalizeStart, sentenceAllCaps, morphology, 0);
  }

  /** Context-aware deterministic Turkish truecaser emitting one canonical surface per candidate. */
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
    if (normLower.indexOf(' ') > 0) {
      return truecaseMultiWordPhrase(
          normLower, rawShape, rawSurface, capitalizeStart, sentenceAllCaps, morphology, casingCtx);
    }
    return truecaseSingleWord(
        normLower, rawShape, rawSurface, capitalizeStart, sentenceAllCaps, morphology, casingCtx);
  }

  private String truecaseMultiWordPhrase(
      String normLower,
      CasingShape rawShape,
      String rawSurface,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      TurkishMorphology morphology,
      int casingCtx) {
    String[] parts = normLower.split(" ");
    int noisyFlag = casingCtx & CTX_SENT_TITLE_NOISY;
    boolean isTwoWordHoliday =
        parts.length == 2
            && TDK_HOLIDAY_MODIFIERS.contains(parts[0])
            && parts[1].startsWith("günü");
    StringBuilder sb = new StringBuilder(normLower.length() + 2);
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        sb.append(' ');
      }
      int partCtx = multiWordPartContext(i, parts.length, casingCtx, noisyFlag, isTwoWordHoliday);
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

  private static int multiWordPartContext(
      int index, int count, int casingCtx, int noisyFlag, boolean isTwoWordHoliday) {
    int ctx;
    if (index == 0) {
      ctx = casingCtx & (CTX_SENT_TITLE_NOISY | CTX_ADJ_DATE_NUM | CTX_AFTER_NUMBER);
    } else if (index == count - 1) {
      ctx = noisyFlag | (casingCtx & CTX_BEFORE_GAZETTEER_OR_HONORIFIC);
    } else {
      ctx = noisyFlag;
    }
    return isTwoWordHoliday ? (ctx | CTX_PROMOTE_PROPER_IN_CONTEXT) : ctx;
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

    // 1. Canonical TDK dotted abbreviations & personal initials
    String dottedOrInitial =
        truecaseDottedOrInitialOrNull(
            wordLower, rawShape, capitalizeStart, sentenceAllCaps, casingCtx);
    if (dottedOrInitial != null) {
      return dottedOrInitial;
    }

    // 2. Canonical tech tokens ("2K", "4K", "i7", "GHz", "SEO", "PHP") & "mAh" vs "Mah"
    String canonTech = CANONICAL_TECH_TOKENS.get(wordLower);
    if (canonTech != null) {
      return canonTech;
    }
    if (wordLower.equals("mah")) {
      return truecaseMahToken(rawShape, capitalizeStart, sentenceAllCaps, noisyTitleSent, casingCtx);
    }

    // 3. Alphanumeric hardware/spec shapes ("4070Ti", "5600X", "550W", "300Mbps")
    if (NormV2Types.containsDigit(wordLower)) {
      String alphaNum = truecaseAlphanumericOrNull(wordLower, rawSurface, sentenceAllCaps);
      if (alphaNum != null) {
        return alphaNum;
      }
    }

    // 4. Apostrophized stems ("Türkiye'de", "TR'de", "WMAracı'nı" vs slang "entry'mde")
    int ap = wordLower.indexOf('\'');
    if (ap > 0) {
      return truecaseApostrophizedWord(
          wordLower.substring(0, ap),
          wordLower.substring(ap),
          rawShape,
          rawSurface,
          capitalizeStart,
          sentenceAllCaps,
          noisyTitleSent,
          morphology);
    }

    // 5. Foreign ALL-CAPS ASCII 'I' names/brands ("DAVID" -> "David", "AIBA" -> "AIBA")
    wordLower =
        resolveForeignUpperAsciiI(wordLower, rawShape, rawSurface, sentenceAllCaps, morphology);

    // 6. Contextual TDK promotion ("Sevgililer Günü", "Lale Hanım", "Ümit Özdağ")
    if (!sentenceAllCaps && (casingCtx & CTX_PROMOTE_PROPER_IN_CONTEXT) != 0) {
      return NormV2Types.trCapitalize(wordLower);
    }

    // 7. TDK Month & Day rule
    if (TDK_MONTHS_AND_DAYS.contains(wordLower)) {
      boolean dateOrGazCtx =
          (casingCtx & (CTX_ADJ_DATE_NUM | CTX_BEFORE_GAZETTEER_OR_HONORIFIC)) != 0;
      boolean explicitAuthorTitle =
          !sentenceAllCaps && !noisyTitleSent && rawShape == CasingShape.TITLE;
      return (capitalizeStart || dateOrGazCtx || explicitAuthorTitle)
          ? NormV2Types.trCapitalize(wordLower)
          : wordLower;
    }

    // 8. Clean-corpus casing lexicon & Zemberek lexicon fallback
    CasingCounts cc = lookupCasing(wordLower);
    String corpusCased = truecaseFromCorpusCountsOrNull(wordLower, rawShape, capitalizeStart, cc);
    if (corpusCased != null) {
      return corpusCased;
    }
    String lexiconCased = truecaseFromMorphLexiconOrNull(wordLower, cc, morphology);
    if (lexiconCased != null) {
      return lexiconCased;
    }

    // 9. Preserve explicit author CamelCase, mid-sentence TitleCase, or UPPER acronyms
    String authorCased =
        preserveAuthorCasingOrNull(
            wordLower,
            rawShape,
            rawSurface,
            capitalizeStart,
            sentenceAllCaps,
            noisyTitleSent,
            casingCtx,
            cc,
            morphology);
    if (authorCased != null) {
      return authorCased;
    }

    return capitalizeStart ? NormV2Types.trCapitalize(wordLower) : wordLower;
  }

  private static String truecaseDottedOrInitialOrNull(
      String wordLower,
      CasingShape rawShape,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      int casingCtx) {
    String dotted = CANONICAL_DOTTED_ABBR.get(wordLower);
    if (dotted != null) {
      return capitalizeStart && Character.isLowerCase(dotted.charAt(0))
          ? NormV2Types.trCapitalize(dotted)
          : dotted;
    }
    if (NormV2Types.isPersonalInitial(wordLower)) {
      boolean upperInitial =
          (casingCtx & CTX_PROMOTE_PROPER_IN_CONTEXT) != 0
              || capitalizeStart
              || (!sentenceAllCaps && rawShape == CasingShape.TITLE);
      return upperInitial ? NormV2Types.trUpper(wordLower) : wordLower;
    }
    return null;
  }

  private static String truecaseMahToken(
      CasingShape rawShape,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      boolean noisyTitleSent,
      int casingCtx) {
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

  private static String truecaseAlphanumericOrNull(
      String wordLower, String rawSurface, boolean sentenceAllCaps) {
    String canonNumUnit = formatNumericTechSpecOrNull(wordLower);
    if (canonNumUnit != null) {
      return canonNumUnit;
    }
    if (sentenceAllCaps || rawSurface == null) {
      return null;
    }
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
    return null;
  }

  private String truecaseApostrophizedWord(
      String stemLower,
      String tailLower,
      CasingShape rawShape,
      String rawSurface,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      boolean noisyTitleSent,
      TurkishMorphology morphology) {
    if (NormV2Types.isDigitsOrPunctOnly(stemLower)) {
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
    CasingCounts stemCounts = lookupCasing(stemLower);
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
          rawSurface != null && (rawSurface.indexOf('\'') >= 0 || rawSurface.indexOf('’') >= 0);
      if (rawHadApostrophe && LOWERCASE_APOSTROPHE_SLANG_STEMS.contains(stemLower)) {
        return stemLower + tailLower;
      }
    }
    return NormV2Types.trCapitalize(stemLower) + tailLower;
  }

  private String resolveForeignUpperAsciiI(
      String wordLower,
      CasingShape rawShape,
      String rawSurface,
      boolean sentenceAllCaps,
      TurkishMorphology morphology) {
    if (sentenceAllCaps
        || rawShape != CasingShape.UPPER
        || rawSurface == null
        || rawSurface.indexOf('I') < 0
        || NormV2Types.hasUpperTurkishDiacritics(rawSurface)
        || wordLower.indexOf('ı') < 0) {
      return wordLower;
    }
    CasingCounts curCc = lookupCasing(wordLower);
    if ((curCc == null || curCc.total() < 3) && !analyzeLexiconRole(wordLower, morphology).hasAny()) {
      String iFolded = wordLower.replace('ı', 'i');
      CasingCounts altCc = lookupCasing(iFolded);
      if (altCc != null && altCc.total() >= 4) {
        return iFolded;
      }
    }
    return wordLower;
  }

  private static String truecaseFromCorpusCountsOrNull(
      String wordLower, CasingShape rawShape, boolean capitalizeStart, CasingCounts cc) {
    if (cc == null) {
      return null;
    }
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
    if (cc.total() >= 6 && cc.title > (cc.lower * 3) && cc.title >= 5) {
      if (capitalizeStart
          || rawShape != CasingShape.LOWER
          || !LOWERCASE_HEADLINE_BIAS_STOPWORDS.contains(wordLower)) {
        return NormV2Types.trCapitalize(wordLower);
      }
    }
    return null;
  }

  private static String truecaseFromMorphLexiconOrNull(
      String wordLower, CasingCounts cc, TurkishMorphology morphology) {
    if ((cc != null && (cc.total() >= 5 || cc.lower >= 2)) || wordLower.length() < 2) {
      return null;
    }
    LexiconRole role = analyzeLexiconRole(wordLower, morphology);
    if (!role.hasCommon() && role.onlyBareAbbr() && wordLower.length() <= 5) {
      return NormV2Types.trUpper(wordLower);
    }
    if (!role.hasCommon() && role.hasProperNoApostrophe()) {
      return NormV2Types.trCapitalize(wordLower);
    }
    return null;
  }

  private static String preserveAuthorCasingOrNull(
      String wordLower,
      CasingShape rawShape,
      String rawSurface,
      boolean capitalizeStart,
      boolean sentenceAllCaps,
      boolean noisyTitleSent,
      int casingCtx,
      CasingCounts cc,
      TurkishMorphology morphology) {
    if (sentenceAllCaps) {
      return null;
    }
    if (!noisyTitleSent && rawShape == CasingShape.MIXED && rawSurface != null) {
      String mixed = preserveCamelOrHyphenCaseOrNull(wordLower, rawSurface, cc, morphology);
      if (mixed != null) {
        return mixed;
      }
    }
    if (rawShape == CasingShape.TITLE && !capitalizeStart) {
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
    if (rawShape == CasingShape.UPPER && wordLower.length() >= 2) {
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
    return null;
  }

  private static String preserveCamelOrHyphenCaseOrNull(
      String wordLower, String rawSurface, CasingCounts cc, TurkishMorphology morphology) {
    String rawNorm = rawSurface.replace('’', '\'');
    if (wordLower.indexOf('-') > 0 && NormV2Types.trLower(rawNorm).equals(wordLower)) {
      return rawNorm;
    }
    if ((cc == null || cc.lower < 5)
        && !isShiftSlipPrefix(rawNorm)
        && !analyzeLexiconRole(wordLower, morphology).hasCommon()) {
      String rawLow = NormV2Types.trLower(rawNorm);
      if (rawLow.equals(wordLower)) {
        return rawNorm;
      }
      if (rawLow.length() >= 4
          && wordLower.length() > rawLow.length()
          && wordLower.startsWith(rawLow)) {
        return rawNorm + wordLower.substring(rawNorm.length());
      }
    }
    return null;
  }

  private record LexiconRole(
      boolean hasCommon, boolean onlyBareAbbr, boolean hasProperNoApostrophe, boolean hasAnyProper) {
    boolean hasAny() {
      return hasCommon || hasAnyProper;
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
      if (!a.isKnownLexical()) {
        continue;
      }
      var item = a.getDictionaryItem();
      if (item.isAbbreviation()) {
        anyProper = true;
        if (!a.hasEnding()) {
          onlyBareAbbr = true;
        }
      } else if (item.isProperNoun()) {
        anyProper = true;
        if (!a.hasEnding()
            || (!LatticeMorphologySupport.properNounTakesApostropheTDK(a)
                && !a.getMorphemes().contains(TurkishMorphotactics.a3pl))) {
          properNoAp = true;
        }
      } else {
        hasCommon = true;
      }
    }
    return new LexiconRole(hasCommon, onlyBareAbbr, properNoAp, anyProper);
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
    return switch (wordLower.substring(dEnd)) {
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
    CasingCounts cc = lookupCasing(stemLower);
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
    CasingCounts cc = lookupCasing(stemLower);
    return cc != null
        && stemLower.length() >= 2
        && stemLower.length() <= 6
        && cc.upper >= 6
        && cc.upper > (cc.lower + cc.title) * 2;
  }

  public boolean isKnownBrandMixedCase(String wordLower) {
    CasingCounts cc = lookupCasing(wordLower);
    return cc != null
        && cc.bestMixedSurface != null
        && cc.mixed >= 3
        && cc.mixed > (cc.lower + cc.title);
  }
}
