package zemberek.normalization.v2;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.deasciifier.latticedeasciifier.QuantizedWeightTable;
import zemberek.normalization.v2.NormV2Types.SentenceLattice;
import zemberek.normalization.v2.NormV2Types.SpanArc;
import zemberek.normalization.v2.NormV2Types.SpanToken;

/**
 * Joint Semi-Markov Averaged Perceptron Turkish sentence normalizer operating over a multi-span
 * candidate DAG ({@link SentenceLattice}).
 *
 * <p>Scores {@code 1:1} rewrites, {@code 1:2} clitic/compound splits, and {@code 2:1}/{@code 3:1}
 * merges jointly using {@code 20} continuous language-model/channel features and {@code 30} hashed
 * sparse feature templates stored in a {@link QuantizedWeightTable}, decoded via beam-pruned
 * Semi-Markov Viterbi search ({@code B = 4}).
 */
public final class SpanLatticePerceptronNormalizer {

  /** Number of continuous features scored alongside sparse binary templates on every arc. */
  public static final int DENSE_DIM = 20;

  private static final int BEAM_WIDTH = 4;
  private static final String DEFAULT_LM_RESOURCE = "/normalization/lm.2gram.slm.gz";
  private static final String DEFAULT_MODEL_RESOURCE = "/normalization/v2/perceptron.bin.gz";

  // Continuous feature indices
  private static final int F_SMOOTH_FWD2 = 0;
  private static final int F_SMOOTH_RIGHT2 = 1;
  private static final int F_SMOOTH_INTERNAL = 2;
  private static final int F_SMOOTH_SPAN_LLR = 3;
  private static final int F_CLEAN_FWD3 = 4;
  private static final int F_CLEAN_RIGHT3 = 5;
  private static final int F_CLEAN_SPAN_LLR = 6;
  private static final int F_PARALLEL_PROB = 7;
  private static final int F_PARALLEL_LOG_CNT = 8;
  private static final int F_SUFFIX_PRECISION = 9;
  private static final int F_EDIT_COST = 10;
  private static final int F_DIACRITIC_DIFFS = 11;
  private static final int F_IS_IDENTITY_LOWER = 12;
  private static final int F_IS_REGULAR_MORPH = 13;
  private static final int F_CHAR_PHONOTACTICS = 14;
  private static final int F_RESERVED_ZERO = 15;
  private static final int F_ASCII_SENT_RATIO = 16;
  private static final int F_INFORMAL_EXACT_VS_STRIP = 17;
  private static final int F_CLITIC_SKIP_LLR = 18;
  private static final int F_QUESTION_VOWEL_HARMONY = 19;

  // Hashed sparse feature template IDs
  private static final int T_LEX_LOWER_PAIR = 1;
  private static final int T_LEX_SURF_PAIR = 2;
  private static final int T_ID_OOV_MORPH_SRC = 3;
  private static final int T_SRC_ASCII_DIAC = 4;
  private static final int T_RIGHT_LEX_BIGRAM = 5;
  private static final int T_LEX_PAIR_NEXT = 6;
  private static final int T_PREV_LEX_PAIR = 7;
  private static final int T_SPLIT_TAIL_HEAD_NEXT = 8;
  private static final int T_SPLIT_TAIL_QMARK = 9;
  private static final int T_MERGE_RAW_NEXT = 10;
  private static final int T_MERGE_PREV_NORM = 11;
  private static final int T_SUBJ_VERB_AGREE = 12;
  private static final int T_CASING_TRANSITION = 13;
  private static final int T_APOSTROPHE_ADD = 14;
  private static final int T_MORPH_NEXT_SHAPE = 15;
  private static final int T_SPLIT_TAIL_HEAD = 16;
  private static final int T_APOSTROPHE_DROP = 17;
  private static final int T_MERGE_SRC_PROP = 18;
  private static final int T_MERGE_MORPH_NEXT = 19;
  private static final int T_TRANS_LEX_BIGRAM = 20;
  private static final int T_TRANS_MORPH_BIGRAM = 21;
  private static final int T_TRANS_WORD_TAG = 22;
  private static final int T_TRANS_TAG_WORD = 23;
  private static final int T_POLARITY_MATCH = 24;
  private static final int T_SRC_LEN_COST_GAIN = 25;
  private static final int T_SRC_PHONO_GAIN = 26;
  private static final int T_SRC_RAW_CLEAN_ATTEST = 27;
  private static final int T_PRON_LOC_RIGHT = 28;
  private static final int T_TRANS_PRON_LOC_LEFT = 29;
  private static final int T_TRANS_QUESTION_HARMONY = 30;

  private record BackPtr(
      SpanArc arc,
      BackPtr prev,
      String prev2Lex,
      String prev1Lex,
      short prevMorphTag,
      float score) {}

  private final SpanCandidateGenerator generator;
  private final float[] denseWeights;
  private final QuantizedWeightTable sparseWeights;

  public SpanLatticePerceptronNormalizer(
      SpanCandidateGenerator generator,
      float[] denseWeights,
      QuantizedWeightTable sparseWeights) {
    if (denseWeights.length != DENSE_DIM) {
      throw new IllegalArgumentException(
          "Expected denseWeights of length " + DENSE_DIM + ", got " + denseWeights.length);
    }
    this.generator = generator;
    this.denseWeights = denseWeights.clone();
    this.sparseWeights = sparseWeights;
  }

  // =========================================================================
  // Model Loading & Factories
  // =========================================================================

  /** Loads the bundled default 2-gram {@link SmoothLm} ({@code /normalization/lm.2gram.slm.gz}). */
  public static SmoothLm loadDefaultSmoothLm() throws IOException {
    try (InputStream is =
        SpanLatticePerceptronNormalizer.class.getResourceAsStream(DEFAULT_LM_RESOURCE)) {
      if (is == null) {
        throw new IOException("Missing classpath resource: " + DEFAULT_LM_RESOURCE);
      }
      return SmoothLm.builder(NormalizationKnowledge.maybeUnzip(is)).logBase(Math.E).build();
    }
  }

  /**
   * Creates a ready-to-use {@link SpanLatticePerceptronNormalizer} using Zemberek's bundled
   * {@code knowledge.bin.gz}, {@code perceptron.bin.gz}, and {@code lm.2gram.slm.gz} resources.
   */
  public static SpanLatticePerceptronNormalizer createWithDefaults(TurkishMorphology morphology)
      throws IOException {
    return createWithDefaults(morphology, loadDefaultSmoothLm());
  }

  /**
   * Creates a ready-to-use {@link SpanLatticePerceptronNormalizer} using Zemberek's bundled
   * {@code knowledge.bin.gz} and {@code perceptron.bin.gz} resources and a provided {@link SmoothLm}.
   */
  public static SpanLatticePerceptronNormalizer createWithDefaults(
      TurkishMorphology morphology, SmoothLm smoothLm) throws IOException {
    CompactStemEndingSpellChecker spellChecker = new CompactStemEndingSpellChecker(morphology);
    NormalizationKnowledge knowledge = NormalizationKnowledge.loadDefault();
    SpanCandidateGenerator generator =
        new SpanCandidateGenerator(morphology, spellChecker, smoothLm, knowledge);
    try (InputStream is =
        SpanLatticePerceptronNormalizer.class.getResourceAsStream(DEFAULT_MODEL_RESOURCE)) {
      if (is == null) {
        throw new IOException("Missing classpath resource: " + DEFAULT_MODEL_RESOURCE);
      }
      return loadModel(is, generator);
    }
  }

  /**
   * Loads a {@link SpanLatticePerceptronNormalizer} from an external model directory containing
   * {@code knowledge.bin[.gz]} and {@code perceptron.bin[.gz]}, plus a 2-gram {@code SmoothLm}.
   */
  public static SpanLatticePerceptronNormalizer fromDirectory(
      TurkishMorphology morphology, Path modelDir, Path smoothLmPath) throws IOException {
    SmoothLm smoothLm;
    try (InputStream lmIn = Files.newInputStream(smoothLmPath)) {
      smoothLm = SmoothLm.builder(NormalizationKnowledge.maybeUnzip(lmIn)).logBase(Math.E).build();
    }
    Path knowledgeFile =
        firstExisting(modelDir, "knowledge.bin.gz", "knowledge.bin", "mined_v23.bin");
    Path perceptronFile =
        firstExisting(modelDir, "perceptron.bin.gz", "perceptron.bin", "perceptron_v23.bin");
    return load(morphology, smoothLm, knowledgeFile, perceptronFile);
  }

  private static Path firstExisting(Path dir, String... names) {
    for (String name : names) {
      Path p = dir.resolve(name);
      if (Files.exists(p)) {
        return p;
      }
    }
    return dir.resolve(names[0]);
  }

  public static SpanLatticePerceptronNormalizer load(
      TurkishMorphology morphology,
      SmoothLm smoothLm,
      Path knowledgeFile,
      Path perceptronModelFile)
      throws IOException {
    CompactStemEndingSpellChecker spellChecker = new CompactStemEndingSpellChecker(morphology);
    NormalizationKnowledge knowledge = NormalizationKnowledge.load(knowledgeFile);
    SpanCandidateGenerator generator =
        new SpanCandidateGenerator(morphology, spellChecker, smoothLm, knowledge);
    return loadModel(perceptronModelFile, generator);
  }

  public static SpanLatticePerceptronNormalizer loadModel(
      Path perceptronModelFile, SpanCandidateGenerator generator) throws IOException {
    try (InputStream fis = Files.newInputStream(perceptronModelFile)) {
      return loadModel(fis, generator);
    }
  }

  public static SpanLatticePerceptronNormalizer loadModel(
      InputStream stream, SpanCandidateGenerator generator) throws IOException {
    DataInputStream in = new DataInputStream(NormalizationKnowledge.maybeUnzip(stream));
    int magic = in.readInt();
    int ver = in.readInt();
    if (magic != NormalizationKnowledge.SNAPSHOT_MAGIC
        || (ver != NormalizationKnowledge.SNAPSHOT_VERSION_LEGACY
            && ver != NormalizationKnowledge.SNAPSHOT_VERSION)) {
      throw new IOException(
          "Unexpected SpanLatticePerceptronNormalizer header: magic="
              + Integer.toHexString(magic)
              + " ver="
              + ver);
    }
    int len = in.readInt();
    float[] dw = new float[len];
    for (int i = 0; i < len; i++) {
      dw[i] = in.readFloat();
    }
    return new SpanLatticePerceptronNormalizer(generator, dw, QuantizedWeightTable.deserialize(in));
  }

  // =========================================================================
  // Public Inference API
  // =========================================================================

  public SpanCandidateGenerator generator() {
    return generator;
  }

  public long modelRamBytes() {
    return sparseWeights.ramBytesUsed() + DENSE_DIM * 4L + 64L;
  }

  public int activeSparseFeatureCount() {
    return sparseWeights.size();
  }

  public String normalize(String rawSentence) {
    if (rawSentence == null || rawSentence.isEmpty()) {
      return "";
    }
    SentenceLattice lattice = generator.buildLattice(rawSentence);
    if (lattice.tokens().isEmpty()) {
      return rawSentence;
    }
    return NormV2Types.reconstructText(lattice.tokens(), decodeLattice(lattice));
  }

  public List<SpanArc> decodeLattice(SentenceLattice lattice) {
    return lattice.tokens().isEmpty()
        ? List.of()
        : decodeViterbi(lattice, generator, denseWeights, sparseWeights);
  }

  /** Computes the exact Perceptron score of {@code arc} given lexical history (used by inspectors). */
  public float scoreArc(
      SentenceLattice lattice,
      String prev2Lex,
      String prev1Lex,
      short prevMorphTag,
      SpanArc arc) {
    long[] featBuf = new long[64];
    float[] denseBuf = new float[DENSE_DIM];
    float[] lm6Buf = new float[6];

    float score =
        dotSparse(sparseWeights, featBuf, extractUnarySparseFeatures(lattice, generator, arc, featBuf));
    extractDenseFeatures(lattice, prev2Lex, prev1Lex, arc, generator, lm6Buf, denseBuf);
    score += dotDense(denseWeights, denseBuf);
    score +=
        dotSparse(
            sparseWeights,
            featBuf,
            extractTransitionSparseFeatures(
                lattice, generator, prev1Lex, prevMorphTag, arc, featBuf));
    return score;
  }

  // =========================================================================
  // Semi-Markov Beam Viterbi Decoder
  // =========================================================================

  private static List<SpanArc> decodeViterbi(
      SentenceLattice lat,
      SpanCandidateGenerator generator,
      float[] wDense,
      QuantizedWeightTable evalSparse) {
    int n = lat.tokens().size();
    @SuppressWarnings({"unchecked", "rawtypes"})
    List<BackPtr>[] chart = new List[n + 1];
    for (int i = 0; i <= n; i++) {
      chart[i] = new ArrayList<>(BEAM_WIDTH + 1);
    }
    chart[0].add(new BackPtr(null, null, null, null, (short) -1, 0.0f));

    long[] featBuf = new long[64];
    float[] denseBuf = new float[DENSE_DIM];
    float[] lm6Buf = new float[6];
    String[] nextLex2 = new String[2];

    for (int i = 0; i < n; i++) {
      List<BackPtr> incoming = chart[i];
      if (incoming.isEmpty()) {
        continue;
      }
      for (SpanArc arc : lat.outgoing()[i]) {
        float unarySparseScore =
            dotSparse(evalSparse, featBuf, extractUnarySparseFeatures(lat, generator, arc, featBuf));

        BackPtr bestPrev = null;
        float bestTotal = Float.NEGATIVE_INFINITY;

        for (BackPtr prevBp : incoming) {
          extractDenseFeatures(
              lat, prevBp.prev2Lex, prevBp.prev1Lex, arc, generator, lm6Buf, denseBuf);
          float denseScore = dotDense(wDense, denseBuf);
          float transSparseScore =
              dotSparse(
                  evalSparse,
                  featBuf,
                  extractTransitionSparseFeatures(
                      lat, generator, prevBp.prev1Lex, prevBp.prevMorphTag, arc, featBuf));
          float candTotal = prevBp.score + unarySparseScore + denseScore + transSparseScore;
          if (candTotal > bestTotal) {
            bestTotal = candTotal;
            bestPrev = prevBp;
          }
        }

        if (bestPrev != null && arc.endIdx <= n) {
          short nextTag = advanceLexMorphTag(lat, bestPrev.prevMorphTag, arc);
          SpanCandidateGenerator.advanceLexicalHistory(
              lat, bestPrev.prev2Lex, bestPrev.prev1Lex, arc, nextLex2);
          insertTopK(
              chart[arc.endIdx],
              new BackPtr(arc, bestPrev, nextLex2[0], nextLex2[1], nextTag, bestTotal),
              BEAM_WIDTH);
        }
      }
    }
    return backtrackBestPath(lat, chart[n]);
  }

  private static List<SpanArc> backtrackBestPath(SentenceLattice lat, List<BackPtr> finals) {
    int n = lat.tokens().size();
    if (finals.isEmpty()) {
      List<SpanArc> fallback = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        fallback.add(lat.outgoing()[i].get(0));
      }
      return fallback;
    }
    BackPtr best = finals.get(0);
    for (int i = 1; i < finals.size(); i++) {
      if (finals.get(i).score > best.score) {
        best = finals.get(i);
      }
    }
    List<SpanArc> out = new ArrayList<>(n);
    for (BackPtr cur = best; cur != null && cur.arc != null; cur = cur.prev) {
      out.add(cur.arc);
    }
    Collections.reverse(out);
    return out;
  }

  public static short advanceLexMorphTag(SentenceLattice lat, short prevTag, SpanArc arc) {
    if (arc.wordCount == 1 && arc.endIdx == arc.startIdx + 1) {
      SpanToken t = lat.tokens().get(arc.startIdx);
      if (t.isImmutable()) {
        return SpanCandidateGenerator.isSentenceBoundaryPunct(t) ? (short) -1 : prevTag;
      }
    }
    return arc.coarseMorphTag;
  }

  private static void insertTopK(List<BackPtr> list, BackPtr item, int k) {
    for (int i = 0; i < list.size(); i++) {
      BackPtr ex = list.get(i);
      if (ex.prevMorphTag == item.prevMorphTag
          && Objects.equals(ex.prev1Lex, item.prev1Lex)
          && Objects.equals(ex.prev2Lex, item.prev2Lex)) {
        if (item.score > ex.score) {
          list.set(i, item);
        }
        return;
      }
    }
    if (list.size() < k) {
      list.add(item);
      return;
    }
    int minIdx = 0;
    float minScore = list.get(0).score;
    for (int i = 1; i < list.size(); i++) {
      if (list.get(i).score < minScore) {
        minScore = list.get(i).score;
        minIdx = i;
      }
    }
    if (item.score > minScore) {
      list.set(minIdx, item);
    }
  }

  private static float dotSparse(QuantizedWeightTable table, long[] featBuf, int count) {
    float sum = 0.0f;
    for (int i = 0; i < count; i++) {
      sum += table.get(featBuf[i]);
    }
    return sum;
  }

  private static float dotDense(float[] weights, float[] vals) {
    float sum = 0.0f;
    for (int i = 0; i < DENSE_DIM; i++) {
      sum += weights[i] * vals[i];
    }
    return sum;
  }

  // =========================================================================
  // Dense & Sparse Feature Extraction
  // =========================================================================

  static void extractDenseFeatures(
      SentenceLattice lat,
      String prev2Lex,
      String prev1Lex,
      SpanArc arc,
      SpanCandidateGenerator gen,
      float[] lm6Buf,
      float[] out) {
    String nextLex = SpanCandidateGenerator.nextLexicalWordOrNull(lat.tokens(), arc.endIdx);
    gen.computeSpanLmComparison(lat, prev2Lex, prev1Lex, arc, lm6Buf);

    float fwdSmooth2 = NormV2Types.clampLm(gen.lmBigram(prev1Lex, arc.firstWordLower));
    float rightSmooth2 = NormV2Types.clampLm(gen.lmBigram(arc.lastWordLower, nextLex));
    float intSmooth = arc.wordCount >= 2 ? NormV2Types.clampLm(arc.internalLmBonus) : 0.0f;
    float cliticSkipLlr = gen.computeCliticTransparencyLlr(lat, arc);

    SpanToken rawTok = lat.tokens().get(arc.startIdx);
    boolean isIdentityLower =
        (arc.endIdx == arc.startIdx + 1) && arc.normLower.equals(rawTok.lower());

    out[F_SMOOTH_FWD2] = fwdSmooth2 * 0.20f;
    out[F_SMOOTH_RIGHT2] = rightSmooth2 * 0.20f;
    out[F_SMOOTH_INTERNAL] = intSmooth * 0.20f;
    out[F_SMOOTH_SPAN_LLR] = (lm6Buf[0] - lm6Buf[1]) * 0.20f;
    out[F_CLEAN_FWD3] = lm6Buf[4] * 0.20f;
    out[F_CLEAN_RIGHT3] = lm6Buf[5] * 0.20f;
    out[F_CLEAN_SPAN_LLR] = (lm6Buf[2] - lm6Buf[3]) * 0.20f;
    out[F_PARALLEL_PROB] = arc.parallelProb;
    out[F_PARALLEL_LOG_CNT] = arc.parallelLogCount * 0.25f;
    out[F_SUFFIX_PRECISION] = arc.suffixPrecision;
    out[F_EDIT_COST] = arc.editCost;
    out[F_DIACRITIC_DIFFS] = arc.diacriticDiffs * 0.35f;
    out[F_IS_IDENTITY_LOWER] = isIdentityLower ? 1.0f : 0.0f;
    out[F_IS_REGULAR_MORPH] = arc.isRegularMorph ? 1.0f : 0.0f;
    out[F_CHAR_PHONOTACTICS] = clampPhono(arc.charPhonotacticScore) * 0.25f;
    out[F_RESERVED_ZERO] = 0.0f;
    out[F_ASCII_SENT_RATIO] =
        ((arc.sourceMask & (NormV2Types.SRC_DEASCIIFIER | NormV2Types.SRC_FORMAL_MORPH)) != 0
                && arc.diacriticDiffs > 0)
            ? lat.sentenceAsciiRatio()
            : 0.0f;
    out[F_INFORMAL_EXACT_VS_STRIP] = informalExactVsStripFeature(arc);
    out[F_CLITIC_SKIP_LLR] = cliticSkipLlr * 0.20f;
    out[F_QUESTION_VOWEL_HARMONY] = gen.computeQuestionCliticHarmony(lat, prev1Lex, arc) * 1.5f;
  }

  private static float informalExactVsStripFeature(SpanArc arc) {
    if ((arc.sourceMask & NormV2Types.SRC_INFORMAL_EXACT) != 0) {
      return 1.0f;
    }
    return arc.stripsExplicitDiacritic ? -1.0f : 0.0f;
  }

  private static float clampPhono(float v) {
    return Float.isNaN(v) || Float.isInfinite(v) ? -4.5f : Math.clamp(v, -6.0f, 0.0f);
  }

  private static int extractUnarySparseFeatures(
      SentenceLattice lat, SpanCandidateGenerator gen, SpanArc arc, long[] buf) {
    int p = 0;
    List<SpanToken> toks = lat.tokens();
    SpanToken t0 = toks.get(arc.startIdx);
    String rawSpanLow = rawSpanKey(toks, arc);
    String prevRaw = arc.startIdx > 0 ? toks.get(arc.startIdx - 1).lower() : "^";
    String nextLex = SpanCandidateGenerator.nextLexicalWordOrNull(toks, arc.endIdx);
    String nextLexKey = nextLex == null ? "$" : nextLex;
    boolean isIdLow = (arc.endIdx == arc.startIdx + 1) && arc.normLower.equals(t0.lower());
    int asciiBkt = bucketAsciiRatio(lat.sentenceAsciiRatio());

    if (arc.rawParallelTotal >= 3) {
      p = emitParallelLexicalFeatures(lat, gen, arc, t0, rawSpanLow, prevRaw, nextLexKey, buf, p);
    }

    List<SpanArc> slotArcs = lat.outgoing()[arc.startIdx];
    boolean rawRegMorph = !slotArcs.isEmpty() && slotArcs.get(0).isRegularMorph;
    int compactSrc = arc.sourceMask & 0x1FFFF;
    buf[p++] =
        hash3(
            T_ID_OOV_MORPH_SRC,
            (isIdLow ? 2 : 0) | (rawRegMorph ? 1 : 0),
            arc.isRegularMorph ? 1 : 0,
            compactSrc);
    buf[p++] =
        hash3(
            T_SRC_ASCII_DIAC,
            compactSrc,
            (asciiBkt << 1) | (rawRegMorph ? 1 : 0),
            arc.diacriticDiffs > 0 ? 1 : 0);
    buf[p++] = hash2(T_RIGHT_LEX_BIGRAM, strHash(arc.lastWordLower), strHash(nextLexKey));

    p = emitSplitAndMergeFeatures(lat, arc, rawSpanLow, prevRaw, nextLexKey, compactSrc, buf, p);

    if (lat.overtSubjectPerson() != 0 && arc.verbPerson != 0) {
      buf[p++] =
          hash3(T_SUBJ_VERB_AGREE, lat.overtSubjectPerson(), arc.verbPerson, isIdLow ? 1 : 0);
    }

    p = emitCasingAndApostropheFeatures(t0, arc, compactSrc, buf, p);

    int nextShape = arc.endIdx < toks.size() ? toks.get(arc.endIdx).shape().ordinal() : 5;
    buf[p++] = hash2(T_MORPH_NEXT_SHAPE, arc.coarseMorphTag, nextShape);

    if (!isIdLow && (arc.rawNegVerbCue || arc.verbPolarity < 0)) {
      buf[p++] =
          hash3(T_POLARITY_MATCH, arc.rawNegVerbCue ? 1 : 0, arc.verbPolarity + 2, compactSrc);
    }

    if (arc.endIdx == arc.startIdx + 1) {
      if (!isIdLow) {
        p = emitSingleTokenBucketFeatures(t0, arc, compactSrc, buf, p);
      }
      p = emitLocativePronounRightFeature(t0, arc, buf, p);
    }
    return p;
  }

  private static String rawSpanKey(List<SpanToken> toks, SpanArc arc) {
    SpanToken t0 = toks.get(arc.startIdx);
    int spanLen = arc.endIdx - arc.startIdx;
    if (spanLen == 1) {
      return t0.lower();
    }
    if (spanLen == 2) {
      return t0.lower() + "+" + toks.get(arc.startIdx + 1).lower();
    }
    return t0.lower() + "+3";
  }

  private static int emitParallelLexicalFeatures(
      SentenceLattice lat,
      SpanCandidateGenerator gen,
      SpanArc arc,
      SpanToken t0,
      String rawSpanLow,
      String prevRaw,
      String nextLexKey,
      long[] buf,
      int p) {
    boolean rawExplicitDotlessI = rawSpanLow.indexOf('ı') >= 0;
    boolean prevPureAmbiguousAscii =
        !SpanCandidateGenerator.hasExplicitTurkishDiacritics(prevRaw)
            && SpanCandidateGenerator.hasAsciiAmbiguousChars(prevRaw);
    boolean contextGovernedQuestionTwin =
        arc.wordCount == 1
            && arc.startIdx > 0
            && !(rawExplicitDotlessI && prevPureAmbiguousAscii)
            && SpanCandidateGenerator.isStandaloneQuestionClitic(rawSpanLow)
            && SpanCandidateGenerator.isStandaloneQuestionClitic(arc.normLower)
            && gen.computeQuestionCliticHarmony(lat, prevRaw, arc) != 0.0f;
    if (!contextGovernedQuestionTwin) {
      buf[p++] = hash2(T_LEX_LOWER_PAIR, strHash(rawSpanLow), strHash(arc.normLower));
      buf[p++] = hash2(T_LEX_SURF_PAIR, strHash(t0.surface()), strHash(arc.normSurface));
    }
    buf[p++] =
        hash3(T_LEX_PAIR_NEXT, strHash(rawSpanLow), strHash(arc.normLower), strHash(nextLexKey));
    buf[p++] =
        hash3(T_PREV_LEX_PAIR, strHash(prevRaw), strHash(rawSpanLow), strHash(arc.normLower));
    return p;
  }

  private static int emitSplitAndMergeFeatures(
      SentenceLattice lat,
      SpanArc arc,
      String rawSpanLow,
      String prevRaw,
      String nextLexKey,
      int compactSrc,
      long[] buf,
      int p) {
    if (arc.wordCount >= 2) {
      int headState = ((arc.firstWordMorphTag & 0xFFFF) << 3) | (arc.headSyntaxClass & 0x7);
      buf[p++] =
          hash3(T_SPLIT_TAIL_HEAD_NEXT, strHash(arc.lastWordLower), headState, strHash(nextLexKey));
      buf[p++] = hash2(T_SPLIT_TAIL_HEAD, strHash(arc.lastWordLower), headState);
      buf[p++] =
          hash2(T_SPLIT_TAIL_QMARK, strHash(arc.lastWordLower), lat.hasQuestionMark() ? 1 : 0);
    }
    if (arc.endIdx - arc.startIdx >= 2) {
      buf[p++] = hash2(T_MERGE_RAW_NEXT, strHash(rawSpanLow), strHash(nextLexKey));
      buf[p++] = hash2(T_MERGE_PREV_NORM, strHash(prevRaw), strHash(arc.normLower));
      buf[p++] = hash2(T_MERGE_SRC_PROP, compactSrc, arc.isProperOrAbbr ? 1 : 0);
      buf[p++] =
          hash3(T_MERGE_MORPH_NEXT, arc.coarseMorphTag & 0xFFFF, compactSrc, strHash(nextLexKey));
    }
    return p;
  }

  private static int emitCasingAndApostropheFeatures(
      SpanToken t0, SpanArc arc, int compactSrc, long[] buf, int p) {
    if (!arc.caseChanged
        && !arc.apostropheAdded
        && !arc.apostropheRemoved
        && t0.shape() == NormV2Types.CasingShape.LOWER) {
      return p;
    }
    buf[p++] =
        hash3(
            T_CASING_TRANSITION,
            t0.shape().ordinal(),
            arc.caseChanged ? 1 : 0,
            (arc.startIdx == 0 ? 2 : 0) | (arc.isProperOrAbbr ? 1 : 0));
    if (arc.apostropheAdded) {
      buf[p++] = hash2(T_APOSTROPHE_ADD, arc.isProperOrAbbr ? 1 : 0, compactSrc);
    }
    if (arc.apostropheRemoved) {
      buf[p++] = hash2(T_APOSTROPHE_DROP, arc.isProperOrAbbr ? 1 : 0, compactSrc);
    }
    return p;
  }

  private static int emitSingleTokenBucketFeatures(
      SpanToken t0, SpanArc arc, int compactSrc, long[] buf, int p) {
    int lenBkt = bucketWordLength(t0.lower().length());
    int costBkt = bucketEditCost(arc.editCost);
    int gainBkt = bucketCleanGain(arc.unaryCleanGain);
    int phonoBkt = bucketPhonoGain(arc.charPhonotacticScore - arc.rawCharPhono);
    int cleanCntBkt = bucketCleanCount(arc.rawCleanCount);

    buf[p++] = hash3(T_SRC_LEN_COST_GAIN, compactSrc, (lenBkt << 3) | costBkt, gainBkt);
    buf[p++] = hash3(T_SRC_PHONO_GAIN, compactSrc, lenBkt, phonoBkt);
    buf[p++] = hash3(T_SRC_RAW_CLEAN_ATTEST, compactSrc, cleanCntBkt, t0.shape().ordinal());
    return p;
  }

  private static int emitLocativePronounRightFeature(
      SpanToken t0, SpanArc arc, long[] buf, int p) {
    int pronCode = locativePronounCode(t0.lower());
    if (pronCode != 0) {
      buf[p++] = hash3(T_PRON_LOC_RIGHT, pronCode, arc.wordCount, arc.nextLexState);
    } else if (arc.wordCount == 2
        && ("de".equals(arc.lastWordLower) || "da".equals(arc.lastWordLower))) {
      buf[p++] = hash3(T_PRON_LOC_RIGHT, 7, arc.headSyntaxClass, arc.nextLexState);
    }
    return p;
  }

  private static int bucketAsciiRatio(float ratio) {
    if (ratio > 0.45f) return 2;
    if (ratio > 0.10f) return 1;
    return 0;
  }

  private static int bucketWordLength(int length) {
    if (length >= 8) return 3;
    if (length >= 6) return 2;
    if (length >= 5) return 1;
    return 0;
  }

  private static int bucketEditCost(float cost) {
    if (cost <= 0.35f) return 0;
    if (cost <= 0.85f) return 1;
    if (cost <= 1.25f) return 2;
    return 3;
  }

  private static int bucketCleanGain(float gain) {
    if (gain >= 8.0f) return 4;
    if (gain >= 4.0f) return 3;
    if (gain >= 1.5f) return 2;
    if (gain >= -0.5f) return 1;
    return 0;
  }

  private static int bucketPhonoGain(float phonoGain) {
    if (phonoGain >= 1.0f) return 3;
    if (phonoGain >= 0.35f) return 2;
    if (phonoGain >= -0.15f) return 1;
    return 0;
  }

  private static int bucketCleanCount(int count) {
    if (count >= 80) return 4;
    if (count >= 20) return 3;
    if (count >= 5) return 2;
    if (count >= 1) return 1;
    return 0;
  }

  private static int locativePronounCode(String rawLow) {
    return switch (rawLow) {
      case "bende" -> 1;
      case "sende" -> 2;
      case "onda", "oda" -> 3;
      case "bizde" -> 4;
      case "sizde" -> 5;
      default -> 0;
    };
  }

  private static int extractTransitionSparseFeatures(
      SentenceLattice lat,
      SpanCandidateGenerator gen,
      String prev1Lex,
      short prevMorphTag,
      SpanArc arc,
      long[] buf) {
    int p = 0;
    long prevW = prev1Lex == null ? 0x1111L : strHash(prev1Lex);
    int prevTag = prevMorphTag;

    buf[p++] = hash2(T_TRANS_LEX_BIGRAM, prevW, strHash(arc.firstWordLower));
    buf[p++] = hash2(T_TRANS_MORPH_BIGRAM, prevTag, arc.firstWordMorphTag);
    buf[p++] = hash2(T_TRANS_WORD_TAG, prevW, arc.firstWordMorphTag);
    buf[p++] = hash2(T_TRANS_TAG_WORD, prevTag, strHash(arc.firstWordLower));

    if (arc.endIdx == arc.startIdx + 1) {
      if (arc.nextLexState != 0
          && arc.normLower.length() <= 6
          && isPronounOrLocativePronoun(arc.firstWordLower)) {
        buf[p++] = hash3(T_TRANS_PRON_LOC_LEFT, prevTag, arc.wordCount, arc.nextLexState);
      }
      float qh = gen.computeQuestionCliticHarmony(lat, prev1Lex, arc);
      if (qh != 0.0f) {
        boolean isIdLow = arc.normLower.equals(lat.tokens().get(arc.startIdx).lower());
        buf[p++] = hash2(T_TRANS_QUESTION_HARMONY, qh > 0.0f ? 1 : -1, isIdLow ? 1 : 0);
      }
    }
    return p;
  }

  private static boolean isPronounOrLocativePronoun(String wordLower) {
    return switch (wordLower) {
      case "ben", "bende", "sen", "sende", "o", "onda", "oda", "biz", "bizde", "siz", "sizde" ->
          true;
      default -> false;
    };
  }

  private static long strHash(String s) {
    if (s == null) {
      return 0L;
    }
    long h = 0xcbf29ce484222325L;
    for (int i = 0; i < s.length(); i++) {
      h ^= s.charAt(i);
      h *= 0x100000001b3L;
    }
    return h;
  }

  private static long hash2(int template, long a, long b) {
    long h = 0x9E3779B97F4A7C15L * (template + 17L);
    h = mix64(h ^ a);
    h = mix64(h ^ b);
    return h == 0L ? 1L : h;
  }

  private static long hash3(int template, long a, long b, long c) {
    long h = 0x9E3779B97F4A7C15L * (template + 31L);
    h = mix64(h ^ a);
    h = mix64(h ^ b);
    h = mix64(h ^ c);
    return h == 0L ? 1L : h;
  }

  private static long mix64(long z) {
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }
}
