package zemberek.normalization.v2;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.deasciifier.latticedeasciifier.QuantizedWeightTable;
import zemberek.normalization.v2.NormV2Types.SentenceLattice;
import zemberek.normalization.v2.NormV2Types.SpanArc;
import zemberek.normalization.v2.NormV2Types.SpanToken;
import zemberek.normalization.v2.NormV2Types.TextNormalizer;

/**
 * Joint Semi-Markov Averaged Perceptron Turkish sentence normalizer ({@code v2.4}) operating over
 * a multi-span candidate DAG ({@link SentenceLattice}).
 *
 * <p>Scores {@code 1:1} rewrites, {@code 1:2} clitic/compound splits, and {@code 2:1}/{@code 3:1}
 * merges jointly using {@code 20} calibrated continuous language-model/channel features and {@code
 * 31} hashed sparse feature templates stored in a quantized Minimal Perfect Hash Function ({@link
 * QuantizedWeightTable}, ~2.6 MB RAM) via beam-pruned Semi-Markov Viterbi search ({@code B = 4}).
 *
 * <p><b>Typical Usage:</b>
 *
 * <pre>{@code
 * TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
 * SpanLatticePerceptronNormalizer normalizer =
 *     SpanLatticePerceptronNormalizer.fromDirectory(morphology, modelDir, lm2GramPath);
 * String clean = normalizer.normalize("slm bugun geliyomusun :)");
 * }</pre>
 */
public final class SpanLatticePerceptronNormalizer implements TextNormalizer {

  /** Number of calibrated continuous features scored alongside sparse binary templates on every arc. */
  public static final int DENSE_DIM = 20;

  // ===============================================================================================
  // Continuous Feature Slots (0 .. DENSE_DIM - 1)
  // ===============================================================================================

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
  private static final int F_EXTERNAL_ARC_SCORE = 15;
  private static final int F_ASCII_SENT_RATIO = 16;
  private static final int F_INFORMAL_EXACT_VS_STRIP = 17;
  private static final int F_CLITIC_SKIP_LLR = 18;
  private static final int F_QUESTION_VOWEL_HARMONY = 19;

  // ===============================================================================================
  // Sparse 64-Bit Feature Template Namespace Salts (hashed via hash2 / hash3)
  // ===============================================================================================

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

  private final String modelName;
  private final SpanCandidateGenerator generator;
  private final float[] denseWeights;
  private final QuantizedWeightTable sparseWeights;

  public SpanLatticePerceptronNormalizer(
      String modelName,
      SpanCandidateGenerator generator,
      float[] denseWeights,
      QuantizedWeightTable sparseWeights) {
    if (denseWeights.length != DENSE_DIM) {
      throw new IllegalArgumentException(
          "Expected denseWeights of length " + DENSE_DIM + ", got " + denseWeights.length);
    }
    this.modelName = modelName;
    this.generator = generator;
    this.denseWeights = denseWeights.clone();
    this.sparseWeights = sparseWeights;
  }

  /**
   * Loads a ready-to-use {@link SpanLatticePerceptronNormalizer} from a model directory containing
   * {@code mined_v23.bin} (or {@code knowledge.bin}) and {@code perceptron_v23.bin} (or {@code
   * perceptron.bin}), plus a 2-gram {@code SmoothLm} file ({@code lm.2gram.slm}).
   */
  public static SpanLatticePerceptronNormalizer fromDirectory(
      TurkishMorphology morphology, Path modelDir, Path smoothLmPath) throws IOException {
    SmoothLm smoothLm = SmoothLm.builder(smoothLmPath).logBase(Math.E).build();
    Path knowledgeFile =
        Files.exists(modelDir.resolve("knowledge.bin"))
            ? modelDir.resolve("knowledge.bin")
            : modelDir.resolve("mined_v23.bin");
    Path perceptronFile =
        Files.exists(modelDir.resolve("perceptron.bin"))
            ? modelDir.resolve("perceptron.bin")
            : modelDir.resolve("perceptron_v23.bin");
    return load(morphology, smoothLm, knowledgeFile, perceptronFile);
  }

  /**
   * Loads a ready-to-use {@link SpanLatticePerceptronNormalizer} from explicit knowledge and model
   * snapshot files.
   */
  public static SpanLatticePerceptronNormalizer load(
      TurkishMorphology morphology,
      SmoothLm smoothLm,
      Path knowledgeFile,
      Path perceptronModelFile)
      throws IOException {
    CompactStemEndingSpellChecker spellChecker =
        new CompactStemEndingSpellChecker(morphology, true);
    NormalizationKnowledge knowledge = NormalizationKnowledge.load(knowledgeFile);
    SpanCandidateGenerator generator =
        new SpanCandidateGenerator(
            morphology, spellChecker, smoothLm, knowledge, SpanCandidateGenerator.Config.full());
    return loadModel(perceptronModelFile, generator);
  }

  /** Loads serialized Perceptron weights from disk and binds them to {@code generator}. */
  public static SpanLatticePerceptronNormalizer loadModel(
      Path perceptronModelFile, SpanCandidateGenerator generator) throws IOException {
    try (InputStream fis = Files.newInputStream(perceptronModelFile)) {
      return loadModel(fis, generator, "SpanLatticePerceptronNormalizer (v2.4)");
    }
  }

  /** Loads serialized Perceptron weights from an {@link InputStream} and binds them to {@code generator}. */
  public static SpanLatticePerceptronNormalizer loadModel(
      InputStream stream, SpanCandidateGenerator generator, String modelName) throws IOException {
    DataInputStream in = new DataInputStream(new BufferedInputStream(stream, 1 << 16));
    int magic = in.readInt();
    int ver = in.readInt();
    if (magic != NormalizationKnowledge.SNAPSHOT_MAGIC
        || ver != NormalizationKnowledge.SNAPSHOT_VERSION) {
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
    QuantizedWeightTable sw = QuantizedWeightTable.deserialize(in);
    return new SpanLatticePerceptronNormalizer(modelName, generator, dw, sw);
  }

  @Override
  public String name() {
    return modelName;
  }

  public SpanCandidateGenerator generator() {
    return generator;
  }

  public long modelRamBytes() {
    return sparseWeights.ramBytesUsed() + DENSE_DIM * 4L + 64L;
  }

  public int activeSparseFeatureCount() {
    return sparseWeights.size();
  }

  public List<SpanArc> decodeLattice(SentenceLattice lattice) {
    if (lattice.tokens().isEmpty()) {
      return List.of();
    }
    return decodeViterbi(lattice, generator, denseWeights, sparseWeights);
  }

  /**
   * Computes the exact Perceptron transition + unary + dense score of {@code arc} given lexical
   * history {@code (prev2Lex, prev1Lex, prevMorphTag)} (used by interactive lattice inspectors).
   */
  public float scoreArc(
      SentenceLattice lattice,
      String prev2Lex,
      String prev1Lex,
      short prevMorphTag,
      SpanArc arc) {
    long[] featBuf = new long[64];
    float[] denseBuf = new float[DENSE_DIM];
    float[] lm6Buf = new float[6];

    int nUnary = extractUnarySparseFeatures(lattice, generator, arc, featBuf);
    float score = 0.0f;
    for (int f = 0; f < nUnary; f++) {
      score += sparseWeights.get(featBuf[f]);
    }
    extractDenseFeatures(lattice, prev2Lex, prev1Lex, arc, generator, lm6Buf, denseBuf);
    for (int d = 0; d < DENSE_DIM; d++) {
      score += denseWeights[d] * denseBuf[d];
    }
    int nTrans =
        extractTransitionSparseFeatures(
            lattice, generator, prev1Lex, prevMorphTag, arc, featBuf, 0);
    for (int f = 0; f < nTrans; f++) {
      score += sparseWeights.get(featBuf[f]);
    }
    return score;
  }

  @Override
  public String normalize(String rawSentence) {
    if (rawSentence == null || rawSentence.isEmpty()) {
      return "";
    }
    SentenceLattice lattice = generator.buildLattice(rawSentence);
    if (lattice.tokens().isEmpty()) {
      return rawSentence;
    }
    List<SpanArc> bestPath = decodeLattice(lattice);
    return NormV2Types.reconstructText(lattice.tokens(), bestPath);
  }

  public List<String> normalizeAll(List<String> sentences) {
    List<String> result = new ArrayList<>(sentences.size());
    for (String s : sentences) {
      result.add(normalize(s));
    }
    return result;
  }

  /**
   * Semi-Markov Viterbi over {@link SentenceLattice} tracking punctuation-bridging lexical history
   * {@code (prev2Lex, prev1Lex, prevMorphTag)} pruned to top beam {@code B = 4} states per boundary.
   */
  private static List<SpanArc> decodeViterbi(
      SentenceLattice lat,
      SpanCandidateGenerator generator,
      float[] wDense,
      QuantizedWeightTable evalSparse) {

    int n = lat.tokens().size();
    final int beamK = 4;

    @SuppressWarnings({"unchecked", "rawtypes"})
    List<BackPtr>[] chart = new List[n + 1];
    for (int i = 0; i <= n; i++) {
      chart[i] = new ArrayList<>(beamK + 1);
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
      List<SpanArc> arcs = lat.outgoing()[i];
      for (int aIdx = 0; aIdx < arcs.size(); aIdx++) {
        SpanArc arc = arcs.get(aIdx);

        int nUnary = extractUnarySparseFeatures(lat, generator, arc, featBuf);
        float unarySparseScore = 0.0f;
        for (int f = 0; f < nUnary; f++) {
          unarySparseScore += evalSparse.get(featBuf[f]);
        }

        BackPtr bestPrevForArc = null;
        float bestTotalForArc = Float.NEGATIVE_INFINITY;

        for (int b = 0; b < incoming.size(); b++) {
          BackPtr prevBp = incoming.get(b);
          extractDenseFeatures(
              lat, prevBp.prev2Lex, prevBp.prev1Lex, arc, generator, lm6Buf, denseBuf);
          float denseScore = 0.0f;
          for (int d = 0; d < DENSE_DIM; d++) {
            denseScore += wDense[d] * denseBuf[d];
          }
          int nTrans =
              extractTransitionSparseFeatures(
                  lat, generator, prevBp.prev1Lex, prevBp.prevMorphTag, arc, featBuf, 0);
          float transSparseScore = 0.0f;
          for (int f = 0; f < nTrans; f++) {
            transSparseScore += evalSparse.get(featBuf[f]);
          }
          float candTotal = prevBp.score + unarySparseScore + denseScore + transSparseScore;
          if (candTotal > bestTotalForArc) {
            bestTotalForArc = candTotal;
            bestPrevForArc = prevBp;
          }
        }

        if (bestPrevForArc != null && arc.endIdx <= n) {
          short nextTag = advanceLexMorphTag(lat, bestPrevForArc.prevMorphTag, arc);
          SpanCandidateGenerator.advanceLexicalHistory(
              lat, bestPrevForArc.prev2Lex, bestPrevForArc.prev1Lex, arc, nextLex2);
          insertTopK(
              chart[arc.endIdx],
              new BackPtr(
                  arc, bestPrevForArc, nextLex2[0], nextLex2[1], nextTag, bestTotalForArc),
              beamK);
        }
      }
    }

    List<BackPtr> finals = chart[n];
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
    BackPtr cur = best;
    while (cur != null && cur.arc != null) {
      out.add(cur.arc);
      cur = cur.prev;
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
          && java.util.Objects.equals(ex.prev1Lex, item.prev1Lex)
          && java.util.Objects.equals(ex.prev2Lex, item.prev2Lex)) {
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

  private record BackPtr(
      SpanArc arc,
      BackPtr prev,
      String prev2Lex,
      String prev1Lex,
      short prevMorphTag,
      float score) {}

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
    float smoothSpanLlr = lm6Buf[0] - lm6Buf[1];
    float cleanFwd3 = lm6Buf[4];
    float cleanRight3 = lm6Buf[5];
    float cleanSpanLlr = lm6Buf[2] - lm6Buf[3];
    float cliticSkipLlr = gen.computeCliticTransparencyLlr(lat, arc);

    SpanToken rawTok = lat.tokens().get(arc.startIdx);
    boolean isIdentityLower =
        (arc.endIdx == arc.startIdx + 1) && arc.normLower.equals(rawTok.lower());

    out[F_SMOOTH_FWD2] = fwdSmooth2 * 0.20f;
    out[F_SMOOTH_RIGHT2] = rightSmooth2 * 0.20f;
    out[F_SMOOTH_INTERNAL] = intSmooth * 0.20f;
    out[F_SMOOTH_SPAN_LLR] = smoothSpanLlr * 0.20f;
    out[F_CLEAN_FWD3] = cleanFwd3 * 0.20f;
    out[F_CLEAN_RIGHT3] = cleanRight3 * 0.20f;
    out[F_CLEAN_SPAN_LLR] = cleanSpanLlr * 0.20f;
    out[F_PARALLEL_PROB] = arc.parallelProb;
    out[F_PARALLEL_LOG_CNT] = arc.parallelLogCount * 0.25f;
    out[F_SUFFIX_PRECISION] = arc.suffixPrecision;
    out[F_EDIT_COST] = arc.editCost;
    out[F_DIACRITIC_DIFFS] = arc.diacriticDiffs * 0.35f;
    out[F_IS_IDENTITY_LOWER] = isIdentityLower ? 1.0f : 0.0f;
    out[F_IS_REGULAR_MORPH] = arc.isRegularMorph ? 1.0f : 0.0f;
    out[F_CHAR_PHONOTACTICS] = clampPhono(arc.charPhonotacticScore) * 0.25f;
    out[F_EXTERNAL_ARC_SCORE] = 0.0f;
    out[F_ASCII_SENT_RATIO] =
        ((arc.sourceMask & (NormV2Types.SRC_DEASCIIFIER | NormV2Types.SRC_FORMAL_MORPH)) != 0
                && arc.diacriticDiffs > 0)
            ? lat.sentenceAsciiRatio()
            : 0.0f;
    out[F_INFORMAL_EXACT_VS_STRIP] =
        (arc.sourceMask & NormV2Types.SRC_INFORMAL_EXACT) != 0
            ? 1.0f
            : (arc.stripsExplicitDiacritic ? -1.0f : 0.0f);
    out[F_CLITIC_SKIP_LLR] = cliticSkipLlr * 0.20f;
    out[F_QUESTION_VOWEL_HARMONY] = gen.computeQuestionCliticHarmony(lat, prev1Lex, arc) * 1.5f;
  }

  private static float clampPhono(float v) {
    if (Float.isNaN(v) || Float.isInfinite(v)) {
      return -4.5f;
    }
    return Math.max(-6.0f, Math.min(0.0f, v));
  }

  private static int extractUnarySparseFeatures(
      SentenceLattice lat, SpanCandidateGenerator gen, SpanArc arc, long[] buf) {
    int p = 0;
    List<SpanToken> toks = lat.tokens();
    SpanToken t0 = toks.get(arc.startIdx);
    String rawSpanLow =
        arc.endIdx == arc.startIdx + 1
            ? t0.lower()
            : (arc.endIdx == arc.startIdx + 2
                ? t0.lower() + "+" + toks.get(arc.startIdx + 1).lower()
                : t0.lower() + "+3");

    String prevRaw = arc.startIdx > 0 ? toks.get(arc.startIdx - 1).lower() : "^";
    String nextLex = SpanCandidateGenerator.nextLexicalWordOrNull(toks, arc.endIdx);
    String nextLexKey = nextLex == null ? "$" : nextLex;
    boolean isIdLow = (arc.endIdx == arc.startIdx + 1) && arc.normLower.equals(t0.lower());
    int asciiBkt =
        lat.sentenceAsciiRatio() > 0.45f ? 2 : (lat.sentenceAsciiRatio() > 0.10f ? 1 : 0);

    // 1. Exact lexical rewrite pair (skip context-free vowel bias on standalone mI when left word determines harmony)
    if (arc.rawParallelTotal >= 3) {
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
    }

    // 2. Identity vs edit x raw-OOV-status x candidate morphological validity x full 17-bit source mask
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

    // 3. Right lexical context lookahead
    buf[p++] = hash2(T_RIGHT_LEX_BIGRAM, strHash(arc.lastWordLower), strHash(nextLexKey));

    // 4. Split clitic features (de/da, mi, ki) x HEAD word POS/syntax x right context
    if (arc.wordCount >= 2) {
      int headState = ((arc.firstWordMorphTag & 0xFFFF) << 3) | (arc.headSyntaxClass & 0x7);
      buf[p++] =
          hash3(T_SPLIT_TAIL_HEAD_NEXT, strHash(arc.lastWordLower), headState, strHash(nextLexKey));
      buf[p++] = hash2(T_SPLIT_TAIL_HEAD, strHash(arc.lastWordLower), headState);
      buf[p++] = hash2(T_SPLIT_TAIL_QMARK, strHash(arc.lastWordLower), lat.hasQuestionMark() ? 1 : 0);
    }

    // 5. Merge features (2:1 / 3:1)
    if (arc.endIdx - arc.startIdx >= 2) {
      buf[p++] = hash2(T_MERGE_RAW_NEXT, strHash(rawSpanLow), strHash(nextLexKey));
      buf[p++] = hash2(T_MERGE_PREV_NORM, strHash(prevRaw), strHash(arc.normLower));
      buf[p++] = hash2(T_MERGE_SRC_PROP, compactSrc, arc.isProperOrAbbr ? 1 : 0);
      buf[p++] =
          hash3(T_MERGE_MORPH_NEXT, arc.coarseMorphTag & 0xFFFF, compactSrc, strHash(nextLexKey));
    }

    // 6. Sentence-wide overt pronoun -> finite verb person agreement
    if (lat.overtSubjectPerson() != 0 && arc.verbPerson != 0) {
      buf[p++] = hash3(T_SUBJ_VERB_AGREE, lat.overtSubjectPerson(), arc.verbPerson, isIdLow ? 1 : 0);
    }

    // 7. Casing & TDK apostrophe addition / erroneous apostrophe removal features
    if (arc.caseChanged
        || arc.apostropheAdded
        || arc.apostropheRemoved
        || t0.shape() != NormV2Types.CasingShape.LOWER) {
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
    }

    // 8. Coarse morph tag + right token shape
    int nextShape = arc.endIdx < toks.size() ? toks.get(arc.endIdx).shape().ordinal() : 5;
    buf[p++] = hash2(T_MORPH_NEXT_SHAPE, arc.coarseMorphTag, nextShape);

    // 9. Finite verb negation polarity match
    if (!isIdLow && (arc.rawNegVerbCue || arc.verbPolarity < 0)) {
      buf[p++] = hash3(T_POLARITY_MATCH, arc.rawNegVerbCue ? 1 : 0, arc.verbPolarity + 2, compactSrc);
    }

    // 10. Non-linear typo-confidence & clean-attestation interactions for unseen OOV 1:1 rewrites
    if (arc.endIdx == arc.startIdx + 1 && !isIdLow) {
      int lenBkt =
          t0.lower().length() >= 8
              ? 3
              : (t0.lower().length() >= 6 ? 2 : (t0.lower().length() >= 5 ? 1 : 0));
      int costBkt =
          arc.editCost <= 0.35f
              ? 0
              : (arc.editCost <= 0.85f ? 1 : (arc.editCost <= 1.25f ? 2 : 3));
      int gainBkt =
          arc.unaryCleanGain >= 8.0f
              ? 4
              : (arc.unaryCleanGain >= 4.0f
                  ? 3
                  : (arc.unaryCleanGain >= 1.5f ? 2 : (arc.unaryCleanGain >= -0.5f ? 1 : 0)));
      float phonoGain = arc.charPhonotacticScore - arc.rawCharPhono;
      int phonoBkt =
          phonoGain >= 1.0f ? 3 : (phonoGain >= 0.35f ? 2 : (phonoGain >= -0.15f ? 1 : 0));
      int cleanCntBkt =
          arc.rawCleanCount >= 80
              ? 4
              : (arc.rawCleanCount >= 20
                  ? 3
                  : (arc.rawCleanCount >= 5 ? 2 : (arc.rawCleanCount >= 1 ? 1 : 0)));
      buf[p++] = hash3(T_SRC_LEN_COST_GAIN, compactSrc, (lenBkt << 3) | costBkt, gainBkt);
      buf[p++] = hash3(T_SRC_PHONO_GAIN, compactSrc, lenBkt, phonoBkt);
      buf[p++] = hash3(T_SRC_RAW_CLEAN_ATTEST, compactSrc, cleanCntBkt, t0.shape().ordinal());
    }

    // 11. Pronoun/nominal locative ("bende uyandırmıyor", "rekor bende.") vs. additive ("ben de geldim") syntax
    if (arc.endIdx == arc.startIdx + 1) {
      int pronCode = locativePronounCode(t0.lower());
      if (pronCode != 0) {
        buf[p++] = hash3(T_PRON_LOC_RIGHT, pronCode, arc.wordCount, arc.nextLexState);
      } else if (arc.wordCount == 2
          && ("de".equals(arc.lastWordLower) || "da".equals(arc.lastWordLower))) {
        buf[p++] = hash3(T_PRON_LOC_RIGHT, 7, arc.headSyntaxClass, arc.nextLexState);
      }
    }

    return p;
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
      long[] buf,
      int offset) {
    int p = offset;
    long prevW = prev1Lex == null ? 0x1111L : strHash(prev1Lex);
    int prevTag = prevMorphTag;

    buf[p++] = hash2(T_TRANS_LEX_BIGRAM, prevW, strHash(arc.firstWordLower));
    buf[p++] = hash2(T_TRANS_MORPH_BIGRAM, prevTag, arc.firstWordMorphTag);
    buf[p++] = hash2(T_TRANS_WORD_TAG, prevW, arc.firstWordMorphTag);
    buf[p++] = hash2(T_TRANS_TAG_WORD, prevTag, strHash(arc.firstWordLower));
    if (arc.endIdx == arc.startIdx + 1) {
      if (arc.nextLexState != 0) {
        String rawLow = arc.normLower.length() <= 6 ? arc.firstWordLower : "";
        if ("ben".equals(rawLow)
            || "bende".equals(rawLow)
            || "sen".equals(rawLow)
            || "sende".equals(rawLow)
            || "o".equals(rawLow)
            || "onda".equals(rawLow)
            || "oda".equals(rawLow)
            || "biz".equals(rawLow)
            || "bizde".equals(rawLow)
            || "siz".equals(rawLow)
            || "sizde".equals(rawLow)) {
          buf[p++] = hash3(T_TRANS_PRON_LOC_LEFT, prevTag, arc.wordCount, arc.nextLexState);
        }
      }
      float qh = gen.computeQuestionCliticHarmony(lat, prev1Lex, arc);
      if (qh != 0.0f) {
        boolean isIdLow = arc.normLower.equals(lat.tokens().get(arc.startIdx).lower());
        buf[p++] = hash2(T_TRANS_QUESTION_HARMONY, qh > 0.0f ? 1 : -1, isIdLow ? 1 : 0);
      }
    }
    return p - offset;
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
