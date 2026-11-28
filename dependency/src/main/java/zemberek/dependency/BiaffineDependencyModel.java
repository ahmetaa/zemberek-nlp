package zemberek.dependency;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.analysis.format.ConlluSentence;
import zemberek.morphology.analysis.format.ConlluToken;

/**
 * Pure-Java inference engine for the {@code ZDEP} v1 compact neural dependency parser:
 *
 * <pre>
 *   [LemmaEmb ; CharCNN ; UposEmb ; XposEmb ; MeanFeatEmb]
 *     -> Linear + LayerNorm + ReLU
 *     -> L-layer Bidirectional GRU (PyTorch gate order: r, z, n)
 *     -> Arc MLP (LeakyReLU 0.1) + Deep Biaffine -> Chu-Liu/Edmonds Single-Root MST
 *     -> Rel MLP (LeakyReLU 0.1) + Deep Biaffine -> Winning Arc Labels
 * </pre>
 *
 * <p>Thread-safe: all model weights are immutable and per-sentence scratch buffers are pooled via
 * {@link ThreadLocal}.
 */
public final class BiaffineDependencyModel {

  static final int MAGIC = 0x5A444550; // "ZDEP"
  static final int FORMAT_VERSION = 1;

  static final int PAD_ID = 0;
  static final int ROOT_ID = 1;
  static final int UNK_ID = 2;

  static final int CHAR_PAD_ID = 0;
  static final int CHAR_BOS_ID = 1;
  static final int CHAR_EOS_ID = 2;
  static final int CHAR_UNK_ID = 3;

  static final int MAX_WORD_CHARS = 32;
  static final float LEAKY_RELU_SLOPE = 0.1f;
  static final float LAYER_NORM_EPS = 1e-5f;

  static final class GruDirectionWeights {
    final QuantizedMatrix weightIh; // [3 * hidden, inDim]
    final float[] biasIh;           // [3 * hidden]
    final QuantizedMatrix weightHh; // [3 * hidden, hidden]
    final float[] biasHh;           // [3 * hidden]

    GruDirectionWeights(
        QuantizedMatrix weightIh, float[] biasIh, QuantizedMatrix weightHh, float[] biasHh) {
      this.weightIh = weightIh;
      this.biasIh = biasIh;
      this.weightHh = weightHh;
      this.biasHh = biasHh;
    }

    void write(DataOutputStream out) throws IOException {
      weightIh.write(out);
      writeFloatArray(out, biasIh);
      weightHh.write(out);
      writeFloatArray(out, biasHh);
    }

    static GruDirectionWeights read(DataInputStream in) throws IOException {
      QuantizedMatrix wIh = QuantizedMatrix.read(in);
      float[] bIh = readFloatArray(in);
      QuantizedMatrix wHh = QuantizedMatrix.read(in);
      float[] bHh = readFloatArray(in);
      return new GruDirectionWeights(wIh, bIh, wHh, bHh);
    }
  }

  static final class RawPredictions {
    final double[][] arcScores;  // [(N+1) x (N+1)] where arcScores[dep][head] is score(head -> dep)
    final int[] heads;           // [N] 0-indexed for tokens 1..N (values in 0..N)
    final String[] deprels;      // [N] predicted UD labels
    final float[][] relLogits;   // [N][numLabels] logits on the winning (dep, head[dep]) pairs

    RawPredictions(double[][] arcScores, int[] heads, String[] deprels, float[][] relLogits) {
      this.arcScores = arcScores;
      this.heads = heads;
      this.deprels = deprels;
      this.relLogits = relLogits;
    }
  }

  // Hyperparameters
  final int lemmaDim;
  final int charEmbDim;
  final int[] charKernelSizes;
  final int charNumFilters;
  final int charOutDim;
  final int uposDim;
  final int xposDim;
  final int featDim;
  final int rawInputDim;
  final int inputProjDim;
  final int gruLayers;
  final int gruHidden;
  final int encoderOutDim;
  final int arcMlpDim;
  final int relMlpDim;

  // Vocabularies
  final String[] lemmaVocab;
  final String[] charVocab;
  final String[] uposVocab;
  final String[] xposVocab;
  final String[] featVocab;
  final String[] labelVocab;

  private final Map<String, Integer> lemmaToId;
  private final Map<Integer, Integer> charToId;
  private final Map<String, Integer> uposToId;
  private final Map<String, Integer> xposToId;
  private final Map<String, Integer> featToId;
  private final int rootLabelIndex;

  // Embeddings & CharCNN
  final QuantizedMatrix lemmaEmb;    // [V_lemma, lemmaDim]
  final QuantizedMatrix charEmb;     // [V_char, charEmbDim]
  final QuantizedMatrix[] charConvW; // per kernel k: [charNumFilters, k * charEmbDim]
  final float[][] charConvB;         // per kernel k: [charNumFilters]
  final QuantizedMatrix uposEmb;     // [V_upos, uposDim]
  final QuantizedMatrix xposEmb;     // [V_xpos, xposDim]
  final QuantizedMatrix featEmb;     // [V_feat, featDim]

  // Input projection + LayerNorm
  final QuantizedMatrix inputProjW;  // [inputProjDim, rawInputDim]
  final float[] inputProjB;          // [inputProjDim]
  final float[] inputLnGamma;        // [inputProjDim]
  final float[] inputLnBeta;         // [inputProjDim]

  // Bidirectional GRU stack
  final GruDirectionWeights[] gruFwd; // [gruLayers]
  final GruDirectionWeights[] gruBwd; // [gruLayers]

  // Arc MLP + Biaffine
  final QuantizedMatrix arcDepW;     // [arcMlpDim, encoderOutDim]
  final float[] arcDepB;             // [arcMlpDim]
  final QuantizedMatrix arcHeadW;    // [arcMlpDim, encoderOutDim]
  final float[] arcHeadB;            // [arcMlpDim]
  final QuantizedMatrix arcBiaffine; // [arcMlpDim + 1, arcMlpDim + 1]

  // Rel MLP + Biaffine
  final QuantizedMatrix relDepW;     // [relMlpDim, encoderOutDim]
  final float[] relDepB;             // [relMlpDim]
  final QuantizedMatrix relHeadW;    // [relMlpDim, encoderOutDim]
  final float[] relHeadB;            // [relMlpDim]
  final QuantizedMatrix[] relBiaffine; // [numLabels], each [relMlpDim + 1, relMlpDim + 1]

  private final ThreadLocal<Scratch> scratchLocal;

  BiaffineDependencyModel(
      int lemmaDim,
      int charEmbDim,
      int[] charKernelSizes,
      int charNumFilters,
      int uposDim,
      int xposDim,
      int featDim,
      int inputProjDim,
      int gruLayers,
      int gruHidden,
      int arcMlpDim,
      int relMlpDim,
      String[] lemmaVocab,
      String[] charVocab,
      String[] uposVocab,
      String[] xposVocab,
      String[] featVocab,
      String[] labelVocab,
      QuantizedMatrix lemmaEmb,
      QuantizedMatrix charEmb,
      QuantizedMatrix[] charConvW,
      float[][] charConvB,
      QuantizedMatrix uposEmb,
      QuantizedMatrix xposEmb,
      QuantizedMatrix featEmb,
      QuantizedMatrix inputProjW,
      float[] inputProjB,
      float[] inputLnGamma,
      float[] inputLnBeta,
      GruDirectionWeights[] gruFwd,
      GruDirectionWeights[] gruBwd,
      QuantizedMatrix arcDepW,
      float[] arcDepB,
      QuantizedMatrix arcHeadW,
      float[] arcHeadB,
      QuantizedMatrix arcBiaffine,
      QuantizedMatrix relDepW,
      float[] relDepB,
      QuantizedMatrix relHeadW,
      float[] relHeadB,
      QuantizedMatrix[] relBiaffine) {
    this.lemmaDim = lemmaDim;
    this.charEmbDim = charEmbDim;
    this.charKernelSizes = charKernelSizes.clone();
    this.charNumFilters = charNumFilters;
    this.charOutDim = charKernelSizes.length * charNumFilters;
    this.uposDim = uposDim;
    this.xposDim = xposDim;
    this.featDim = featDim;
    this.rawInputDim = lemmaDim + charOutDim + uposDim + xposDim + featDim;
    this.inputProjDim = inputProjDim;
    this.gruLayers = gruLayers;
    this.gruHidden = gruHidden;
    this.encoderOutDim = 2 * gruHidden;
    this.arcMlpDim = arcMlpDim;
    this.relMlpDim = relMlpDim;

    this.lemmaVocab = lemmaVocab;
    this.charVocab = charVocab;
    this.uposVocab = uposVocab;
    this.xposVocab = xposVocab;
    this.featVocab = featVocab;
    this.labelVocab = labelVocab;

    this.lemmaToId = buildVocabMap(lemmaVocab);
    this.uposToId = buildVocabMap(uposVocab);
    this.xposToId = buildVocabMap(xposVocab);
    this.featToId = buildVocabMap(featVocab);
    this.charToId = new HashMap<>(charVocab.length * 2);
    for (int i = CHAR_UNK_ID + 1; i < charVocab.length; i++) {
      if (!charVocab[i].isEmpty()) {
        this.charToId.put(charVocab[i].codePointAt(0), i);
      }
    }
    int rootIdx = -1;
    for (int i = 0; i < labelVocab.length; i++) {
      if ("root".equals(labelVocab[i])) {
        rootIdx = i;
        break;
      }
    }
    this.rootLabelIndex = rootIdx;

    this.lemmaEmb = lemmaEmb;
    this.charEmb = charEmb;
    this.charConvW = charConvW;
    this.charConvB = charConvB;
    this.uposEmb = uposEmb;
    this.xposEmb = xposEmb;
    this.featEmb = featEmb;
    this.inputProjW = inputProjW;
    this.inputProjB = inputProjB;
    this.inputLnGamma = inputLnGamma;
    this.inputLnBeta = inputLnBeta;
    this.gruFwd = gruFwd;
    this.gruBwd = gruBwd;
    this.arcDepW = arcDepW;
    this.arcDepB = arcDepB;
    this.arcHeadW = arcHeadW;
    this.arcHeadB = arcHeadB;
    this.arcBiaffine = arcBiaffine;
    this.relDepW = relDepW;
    this.relDepB = relDepB;
    this.relHeadW = relHeadW;
    this.relHeadB = relHeadB;
    this.relBiaffine = relBiaffine;

    this.scratchLocal = ThreadLocal.withInitial(() -> new Scratch(this));
  }

  /** Parses syntactic dependencies for a projected {@link ConlluSentence}. */
  public DependencyTree parse(ConlluSentence projected) {
    if (projected.size() == 0) {
      return new DependencyTree(projected);
    }
    RawPredictions raw = predictRaw(projected);
    return new DependencyTree(projected.withDependencies(raw.heads, raw.deprels));
  }

  /** Full forward pass returning raw arc scores, MST heads, labels, and winning relation logits. */
  RawPredictions predictRaw(ConlluSentence sentence) {
    int n = sentence.size();
    int seqLen = n + 1; // index 0 = <ROOT>, 1..n = syntactic tokens
    Scratch sc = scratchLocal.get();
    sc.ensureSeqLen(seqLen);

    // 1. Token embeddings + Input projection + LayerNorm + ReLU -> sc.layerIn[0..seqLen)
    encodeTokenInputs(sentence, sc);

    // 2. L-layer Bidirectional GRU
    float[] layerIn = sc.layerIn;
    float[] layerOut = sc.layerOut;
    for (int l = 0; l < gruLayers; l++) {
      int inDim = (l == 0) ? inputProjDim : encoderOutDim;
      runGruDirection(gruFwd[l], layerIn, inDim, seqLen, true, layerOut, 0, sc);
      runGruDirection(gruBwd[l], layerIn, inDim, seqLen, false, layerOut, gruHidden, sc);
      if (l + 1 < gruLayers) {
        float[] tmp = layerIn;
        layerIn = layerOut;
        layerOut = tmp;
      }
    }
    float[] enc = layerOut; // [seqLen * encoderOutDim]

    // 3. Arc MLP (LeakyReLU 0.1) + Biaffine arc score matrix
    int arcDim1 = arcMlpDim + 1;
    float[] arcDep = sc.arcDep;   // [seqLen * (arcMlpDim + 1)], last slot = 1.0f
    float[] arcHead = sc.arcHead; // [seqLen * (arcMlpDim + 1)], last slot = 1.0f
    for (int t = 0; t < seqLen; t++) {
      int encOff = t * encoderOutDim;
      int arcOff = t * arcDim1;
      arcDepW.matVecAddBias(enc, encOff, arcDepB, arcDep, arcOff);
      applyLeakyRelu(arcDep, arcOff, arcMlpDim);
      arcDep[arcOff + arcMlpDim] = 1.0f;

      arcHeadW.matVecAddBias(enc, encOff, arcHeadB, arcHead, arcOff);
      applyLeakyRelu(arcHead, arcOff, arcMlpDim);
      arcHead[arcOff + arcMlpDim] = 1.0f;
    }

    double[][] arcScores = new double[seqLen][seqLen];
    float[] tmpArc = sc.tmpArc;
    for (int dep = 1; dep < seqLen; dep++) {
      arcBiaffine.vecMat(arcDep, dep * arcDim1, tmpArc, 0);
      double[] row = arcScores[dep];
      for (int head = 0; head < seqLen; head++) {
        if (head == dep) {
          row[head] = -1e9;
          continue;
        }
        int hOff = head * arcDim1;
        float dot = 0f;
        for (int k = 0; k < arcDim1; k++) {
          dot += tmpArc[k] * arcHead[hOff + k];
        }
        row[head] = dot;
      }
    }

    // 4. Exact Single-Root MST Decoding
    int[] mstHeads = ChuLiuEdmonds.decodeSingleRoot(arcScores);

    // 5. Rel MLP (LeakyReLU 0.1) + Label Biaffine only on winning (dep, head) edges
    int relDim1 = relMlpDim + 1;
    float[] relDep = sc.relDep;
    float[] relHead = sc.relHead;
    for (int t = 0; t < seqLen; t++) {
      int encOff = t * encoderOutDim;
      int relOff = t * relDim1;
      relDepW.matVecAddBias(enc, encOff, relDepB, relDep, relOff);
      applyLeakyRelu(relDep, relOff, relMlpDim);
      relDep[relOff + relMlpDim] = 1.0f;

      relHeadW.matVecAddBias(enc, encOff, relHeadB, relHead, relOff);
      applyLeakyRelu(relHead, relOff, relMlpDim);
      relHead[relOff + relMlpDim] = 1.0f;
    }

    int numLabels = labelVocab.length;
    int[] headsOut = new int[n];
    String[] deprelsOut = new String[n];
    float[][] relLogitsOut = new float[n][numLabels];
    float[] uRel = sc.uRel;
    float[] vRel = sc.vRel;

    for (int dep = 1; dep <= n; dep++) {
      int head = mstHeads[dep];
      headsOut[dep - 1] = head;

      System.arraycopy(relDep, dep * relDim1, uRel, 0, relDim1);
      System.arraycopy(relHead, head * relDim1, vRel, 0, relDim1);

      float[] logits = relLogitsOut[dep - 1];
      for (int c = 0; c < numLabels; c++) {
        logits[c] = relBiaffine[c].bilinear(uRel, vRel);
      }
      int bestLabel;
      if (head == 0 && rootLabelIndex >= 0) {
        bestLabel = rootLabelIndex;
      } else {
        bestLabel = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int c = 0; c < numLabels; c++) {
          if (c == rootLabelIndex && head != 0) {
            continue;
          }
          if (logits[c] > bestScore) {
            bestScore = logits[c];
            bestLabel = c;
          }
        }
      }
      deprelsOut[dep - 1] = labelVocab[bestLabel];
    }

    return new RawPredictions(arcScores, headsOut, deprelsOut, relLogitsOut);
  }

  private void encodeTokenInputs(ConlluSentence sentence, Scratch sc) {
    int n = sentence.size();
    float[] raw = sc.rawToken;
    float[] proj = sc.projToken;

    int offChar = lemmaDim;
    int offUpos = offChar + charOutDim;
    int offXpos = offUpos + uposDim;
    int offFeat = offXpos + xposDim;

    // t = 0: synthetic <ROOT>
    Arrays.fill(raw, 0f);
    lemmaEmb.rowToFloat(ROOT_ID, raw, 0);
    // char-CNN output for <ROOT> is zeros
    uposEmb.rowToFloat(ROOT_ID, raw, offUpos);
    xposEmb.rowToFloat(ROOT_ID, raw, offXpos);
    // feat bag for <ROOT> is zeros
    projectAndNorm(raw, proj, sc.layerIn, 0);

    // t = 1..n: syntactic CoNLL-U tokens
    List<ConlluToken> tokens = sentence.tokens();
    for (int i = 0; i < n; i++) {
      ConlluToken tok = tokens.get(i);
      Arrays.fill(raw, 0f);

      // Lemma embedding (Turkish lowercase)
      String normLemma = tok.lemma().toLowerCase(Turkish.LOCALE);
      int lemmaId = lemmaToId.getOrDefault(normLemma, UNK_ID);
      lemmaEmb.rowToFloat(lemmaId, raw, 0);

      // Character CNN over surface form
      encodeCharCnn(tok.form(), raw, offChar, sc);

      // UPOS & XPOS embeddings
      int uposId = uposToId.getOrDefault(tok.upos(), UNK_ID);
      int xposId = xposToId.getOrDefault(tok.xpos(), UNK_ID);
      uposEmb.rowToFloat(uposId, raw, offUpos);
      xposEmb.rowToFloat(xposId, raw, offXpos);

      // Mean UD morphological feature-bag embedding
      List<String> featPairs = tok.featPairs();
      int matchedFeats = 0;
      for (String fp : featPairs) {
        Integer fid = featToId.get(fp);
        if (fid != null) {
          featEmb.addRowToFloat(fid, raw, offFeat);
          matchedFeats++;
        }
      }
      if (matchedFeats > 1) {
        float inv = 1.0f / matchedFeats;
        for (int c = 0; c < featDim; c++) {
          raw[offFeat + c] *= inv;
        }
      }

      projectAndNorm(raw, proj, sc.layerIn, (i + 1) * inputProjDim);
    }
  }

  private void encodeCharCnn(String form, float[] out, int outOff, Scratch sc) {
    if (charOutDim == 0 || form == null || form.isEmpty()) {
      return;
    }
    int[] charIds = sc.charIds;
    int len = 0;
    charIds[len++] = CHAR_BOS_ID;
    int cpCount = 0;
    for (int idx = 0; idx < form.length() && cpCount < MAX_WORD_CHARS; ) {
      int cp = form.codePointAt(idx);
      charIds[len++] = charToId.getOrDefault(cp, CHAR_UNK_ID);
      idx += Character.charCount(cp);
      cpCount++;
    }
    charIds[len++] = CHAR_EOS_ID;

    int maxK = sc.maxKernelSize;
    int paddedLen = Math.max(len, maxK);
    for (int i = len; i < paddedLen; i++) {
      charIds[i] = CHAR_PAD_ID;
    }

    // Lookup char embeddings into sc.charSeq [paddedLen * charEmbDim]
    float[] charSeq = sc.charSeq;
    for (int i = 0; i < paddedLen; i++) {
      charEmb.rowToFloat(charIds[i], charSeq, i * charEmbDim);
    }

    float[] convBuf = sc.convOut;
    int cursor = outOff;
    for (int ki = 0; ki < charKernelSizes.length; ki++) {
      int k = charKernelSizes[ki];
      int effLen = Math.max(len, k);
      int numWindows = effLen - k + 1;
      QuantizedMatrix w = charConvW[ki];
      float[] b = charConvB[ki];

      Arrays.fill(out, cursor, cursor + charNumFilters, Float.NEGATIVE_INFINITY);
      for (int pos = 0; pos < numWindows; pos++) {
        w.matVecAddBias(charSeq, pos * charEmbDim, b, convBuf, 0);
        for (int f = 0; f < charNumFilters; f++) {
          float v = convBuf[f] > 0f ? convBuf[f] : 0f; // ReLU
          if (v > out[cursor + f]) {
            out[cursor + f] = v;
          }
        }
      }
      cursor += charNumFilters;
    }
  }

  private void projectAndNorm(float[] raw, float[] proj, float[] dest, int destOff) {
    inputProjW.matVecAddBias(raw, 0, inputProjB, proj, 0);
    // LayerNorm (eps = 1e-5) + ReLU
    int d = inputProjDim;
    float mean = 0f;
    for (int i = 0; i < d; i++) {
      mean += proj[i];
    }
    mean /= d;
    float var = 0f;
    for (int i = 0; i < d; i++) {
      float diff = proj[i] - mean;
      var += diff * diff;
    }
    var /= d;
    float invStd = (float) (1.0 / Math.sqrt(var + LAYER_NORM_EPS));
    for (int i = 0; i < d; i++) {
      float norm = (proj[i] - mean) * invStd * inputLnGamma[i] + inputLnBeta[i];
      dest[destOff + i] = norm > 0f ? norm : 0f; // ReLU
    }
  }

  private void runGruDirection(
      GruDirectionWeights w,
      float[] input,
      int inDim,
      int seqLen,
      boolean forward,
      float[] out,
      int outDirOffset,
      Scratch sc) {
    int h = gruHidden;
    int h2 = 2 * h;
    float[] prevH = sc.gruPrevH;
    float[] gi = sc.gruGi;
    float[] gh = sc.gruGh;
    Arrays.fill(prevH, 0f);

    int start = forward ? 0 : seqLen - 1;
    int end = forward ? seqLen : -1;
    int step = forward ? 1 : -1;

    for (int t = start; t != end; t += step) {
      w.weightIh.matVecAddBias(input, t * inDim, w.biasIh, gi, 0);
      w.weightHh.matVecAddBias(prevH, 0, w.biasHh, gh, 0);
      int outOff = t * encoderOutDim + outDirOffset;
      for (int i = 0; i < h; i++) {
        float r = sigmoid(gi[i] + gh[i]);
        float z = sigmoid(gi[h + i] + gh[h + i]);
        float n = (float) Math.tanh(gi[h2 + i] + r * gh[h2 + i]);
        float ht = (1.0f - z) * n + z * prevH[i];
        prevH[i] = ht;
        out[outOff + i] = ht;
      }
    }
  }

  private static float sigmoid(float x) {
    return (float) (1.0 / (1.0 + Math.exp(-x)));
  }

  private static void applyLeakyRelu(float[] arr, int off, int len) {
    for (int i = 0; i < len; i++) {
      float v = arr[off + i];
      if (v < 0f) {
        arr[off + i] = v * LEAKY_RELU_SLOPE;
      }
    }
  }

  private static Map<String, Integer> buildVocabMap(String[] vocab) {
    Map<String, Integer> map = new HashMap<>(vocab.length * 2);
    for (int i = 0; i < vocab.length; i++) {
      map.putIfAbsent(vocab[i], i);
    }
    return map;
  }

  // ==================== Binary Serialization (ZDEP v1) ====================

  public static BiaffineDependencyModel load(Path path) throws IOException {
    try (InputStream raw = Files.newInputStream(path)) {
      return load(raw);
    }
  }

  public static BiaffineDependencyModel load(InputStream rawStream) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(rawStream, 65536);
    bis.mark(2);
    int b1 = bis.read();
    int b2 = bis.read();
    bis.reset();
    InputStream effective = (b1 == 0x1f && b2 == 0x8b) ? new GZIPInputStream(bis, 65536) : bis;
    DataInputStream in = new DataInputStream(new BufferedInputStream(effective, 65536));

    int magic = in.readInt();
    if (magic != MAGIC) {
      throw new IOException(String.format("Invalid ZDEP model magic: 0x%08X", magic));
    }
    int version = in.readInt();
    if (version != FORMAT_VERSION) {
      throw new IOException("Unsupported ZDEP model version: " + version);
    }

    int lemmaDim = readPositiveInt(in, "lemmaDim");
    int charEmbDim = readNonNegativeInt(in, "charEmbDim");
    int numKernels = readNonNegativeInt(in, "numKernels");
    int[] charKernelSizes = new int[numKernels];
    for (int i = 0; i < numKernels; i++) {
      charKernelSizes[i] = readPositiveInt(in, "charKernelSizes[" + i + "]");
    }
    int charNumFilters = readNonNegativeInt(in, "charNumFilters");
    int uposDim = readPositiveInt(in, "uposDim");
    int xposDim = readPositiveInt(in, "xposDim");
    int featDim = readPositiveInt(in, "featDim");
    int inputProjDim = readPositiveInt(in, "inputProjDim");
    int gruLayers = readPositiveInt(in, "gruLayers");
    int gruHidden = readPositiveInt(in, "gruHidden");
    int arcMlpDim = readPositiveInt(in, "arcMlpDim");
    int relMlpDim = readPositiveInt(in, "relMlpDim");

    int charOutDim;
    int rawInputDim;
    int encOutDim;
    int gruGateDim;
    try {
      charOutDim = Math.multiplyExact(numKernels, charNumFilters);
      rawInputDim = Math.addExact(
          Math.addExact(Math.addExact(lemmaDim, charOutDim), Math.addExact(uposDim, xposDim)),
          featDim);
      encOutDim = Math.multiplyExact(2, gruHidden);
      gruGateDim = Math.multiplyExact(3, gruHidden);
    } catch (ArithmeticException e) {
      throw new IOException("ZDEP header dimension overflow", e);
    }

    String[] lemmaVocab = readStringArray(in, 3, "lemmaVocab");
    String[] charVocab = readStringArray(in, 4, "charVocab");
    String[] uposVocab = readStringArray(in, 3, "uposVocab");
    String[] xposVocab = readStringArray(in, 3, "xposVocab");
    String[] featVocab = readStringArray(in, 2, "featVocab");
    String[] labelVocab = readStringArray(in, 1, "labelVocab");

    QuantizedMatrix lemmaEmb = checkShape(QuantizedMatrix.read(in), lemmaVocab.length, lemmaDim, "lemmaEmb");
    QuantizedMatrix charEmb = checkShape(QuantizedMatrix.read(in), charVocab.length, charEmbDim, "charEmb");
    QuantizedMatrix[] charConvW = new QuantizedMatrix[numKernels];
    float[][] charConvB = new float[numKernels][];
    for (int i = 0; i < numKernels; i++) {
      int kWidth;
      try {
        kWidth = Math.multiplyExact(charKernelSizes[i], charEmbDim);
      } catch (ArithmeticException e) {
        throw new IOException("CharCNN kernel width overflow at index " + i, e);
      }
      charConvW[i] = checkShape(QuantizedMatrix.read(in), charNumFilters, kWidth, "charConvW[" + i + "]");
      charConvB[i] = checkLen(readFloatArray(in), charNumFilters, "charConvB[" + i + "]");
    }
    QuantizedMatrix uposEmb = checkShape(QuantizedMatrix.read(in), uposVocab.length, uposDim, "uposEmb");
    QuantizedMatrix xposEmb = checkShape(QuantizedMatrix.read(in), xposVocab.length, xposDim, "xposEmb");
    QuantizedMatrix featEmb = checkShape(QuantizedMatrix.read(in), featVocab.length, featDim, "featEmb");

    QuantizedMatrix inputProjW = checkShape(QuantizedMatrix.read(in), inputProjDim, rawInputDim, "inputProjW");
    float[] inputProjB = checkLen(readFloatArray(in), inputProjDim, "inputProjB");
    float[] inputLnGamma = checkLen(readFloatArray(in), inputProjDim, "inputLnGamma");
    float[] inputLnBeta = checkLen(readFloatArray(in), inputProjDim, "inputLnBeta");

    GruDirectionWeights[] gruFwd = new GruDirectionWeights[gruLayers];
    GruDirectionWeights[] gruBwd = new GruDirectionWeights[gruLayers];
    for (int l = 0; l < gruLayers; l++) {
      int layerInDim = (l == 0) ? inputProjDim : encOutDim;
      gruFwd[l] = validateGruWeights(GruDirectionWeights.read(in), gruGateDim, layerInDim, gruHidden, "gruFwd[" + l + "]");
      gruBwd[l] = validateGruWeights(GruDirectionWeights.read(in), gruGateDim, layerInDim, gruHidden, "gruBwd[" + l + "]");
    }

    QuantizedMatrix arcDepW = checkShape(QuantizedMatrix.read(in), arcMlpDim, encOutDim, "arcDepW");
    float[] arcDepB = checkLen(readFloatArray(in), arcMlpDim, "arcDepB");
    QuantizedMatrix arcHeadW = checkShape(QuantizedMatrix.read(in), arcMlpDim, encOutDim, "arcHeadW");
    float[] arcHeadB = checkLen(readFloatArray(in), arcMlpDim, "arcHeadB");
    QuantizedMatrix arcBiaffine = checkShape(QuantizedMatrix.read(in), arcMlpDim + 1, arcMlpDim + 1, "arcBiaffine");

    QuantizedMatrix relDepW = checkShape(QuantizedMatrix.read(in), relMlpDim, encOutDim, "relDepW");
    float[] relDepB = checkLen(readFloatArray(in), relMlpDim, "relDepB");
    QuantizedMatrix relHeadW = checkShape(QuantizedMatrix.read(in), relMlpDim, encOutDim, "relHeadW");
    float[] relHeadB = checkLen(readFloatArray(in), relMlpDim, "relHeadB");
    QuantizedMatrix[] relBiaffine = new QuantizedMatrix[labelVocab.length];
    for (int c = 0; c < labelVocab.length; c++) {
      relBiaffine[c] = checkShape(
          QuantizedMatrix.read(in), relMlpDim + 1, relMlpDim + 1, "relBiaffine[" + c + "]");
    }

    return new BiaffineDependencyModel(
        lemmaDim, charEmbDim, charKernelSizes, charNumFilters,
        uposDim, xposDim, featDim, inputProjDim, gruLayers, gruHidden, arcMlpDim, relMlpDim,
        lemmaVocab, charVocab, uposVocab, xposVocab, featVocab, labelVocab,
        lemmaEmb, charEmb, charConvW, charConvB, uposEmb, xposEmb, featEmb,
        inputProjW, inputProjB, inputLnGamma, inputLnBeta,
        gruFwd, gruBwd,
        arcDepW, arcDepB, arcHeadW, arcHeadB, arcBiaffine,
        relDepW, relDepB, relHeadW, relHeadB, relBiaffine);
  }

  void write(DataOutputStream out) throws IOException {
    out.writeInt(MAGIC);
    out.writeInt(FORMAT_VERSION);

    out.writeInt(lemmaDim);
    out.writeInt(charEmbDim);
    out.writeInt(charKernelSizes.length);
    for (int k : charKernelSizes) {
      out.writeInt(k);
    }
    out.writeInt(charNumFilters);
    out.writeInt(uposDim);
    out.writeInt(xposDim);
    out.writeInt(featDim);
    out.writeInt(inputProjDim);
    out.writeInt(gruLayers);
    out.writeInt(gruHidden);
    out.writeInt(arcMlpDim);
    out.writeInt(relMlpDim);

    writeStringArray(out, lemmaVocab);
    writeStringArray(out, charVocab);
    writeStringArray(out, uposVocab);
    writeStringArray(out, xposVocab);
    writeStringArray(out, featVocab);
    writeStringArray(out, labelVocab);

    lemmaEmb.write(out);
    charEmb.write(out);
    for (int i = 0; i < charKernelSizes.length; i++) {
      charConvW[i].write(out);
      writeFloatArray(out, charConvB[i]);
    }
    uposEmb.write(out);
    xposEmb.write(out);
    featEmb.write(out);

    inputProjW.write(out);
    writeFloatArray(out, inputProjB);
    writeFloatArray(out, inputLnGamma);
    writeFloatArray(out, inputLnBeta);

    for (int l = 0; l < gruLayers; l++) {
      gruFwd[l].write(out);
      gruBwd[l].write(out);
    }

    arcDepW.write(out);
    writeFloatArray(out, arcDepB);
    arcHeadW.write(out);
    writeFloatArray(out, arcHeadB);
    arcBiaffine.write(out);

    relDepW.write(out);
    writeFloatArray(out, relDepB);
    relHeadW.write(out);
    writeFloatArray(out, relHeadB);
    for (QuantizedMatrix m : relBiaffine) {
      m.write(out);
    }
  }

  private static void writeFloatArray(DataOutputStream out, float[] arr) throws IOException {
    out.writeInt(arr.length);
    for (float v : arr) {
      out.writeFloat(v);
    }
  }

  private static float[] readFloatArray(DataInputStream in) throws IOException {
    int len = in.readInt();
    if (len < 0) {
      throw new IOException("Negative float array length: " + len);
    }
    float[] arr = new float[len];
    for (int i = 0; i < len; i++) {
      arr[i] = in.readFloat();
    }
    return arr;
  }

  private static void writeStringArray(DataOutputStream out, String[] arr) throws IOException {
    out.writeInt(arr.length);
    for (String s : arr) {
      byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
      out.writeInt(bytes.length);
      out.write(bytes);
    }
  }

  private static String[] readStringArray(DataInputStream in, int minLen, String name)
      throws IOException {
    int len = in.readInt();
    if (len < minLen) {
      throw new IOException(
          "Invalid ZDEP " + name + " length: " + len + " (expected >= " + minLen + ")");
    }
    String[] arr = new String[len];
    for (int i = 0; i < len; i++) {
      int byteLen = in.readInt();
      if (byteLen < 0) {
        throw new IOException("Negative UTF-8 string length in " + name + "[" + i + "]: " + byteLen);
      }
      byte[] bytes = new byte[byteLen];
      in.readFully(bytes);
      arr[i] = new String(bytes, StandardCharsets.UTF_8);
    }
    return arr;
  }

  private static int readPositiveInt(DataInputStream in, String name) throws IOException {
    int val = in.readInt();
    if (val <= 0) {
      throw new IOException("Invalid ZDEP header " + name + ": " + val + " (expected > 0)");
    }
    return val;
  }

  private static int readNonNegativeInt(DataInputStream in, String name) throws IOException {
    int val = in.readInt();
    if (val < 0) {
      throw new IOException("Invalid ZDEP header " + name + ": " + val + " (expected >= 0)");
    }
    return val;
  }

  private static QuantizedMatrix checkShape(
      QuantizedMatrix m, int expectedRows, int expectedCols, String name) throws IOException {
    if (m.rows != expectedRows || m.cols != expectedCols) {
      throw new IOException(
          "Invalid ZDEP matrix shape for " + name + ": got " + m.rows + "x" + m.cols
              + ", expected " + expectedRows + "x" + expectedCols);
    }
    return m;
  }

  private static float[] checkLen(float[] arr, int expectedLen, String name) throws IOException {
    if (arr.length != expectedLen) {
      throw new IOException(
          "Invalid ZDEP vector length for " + name + ": got " + arr.length
              + ", expected " + expectedLen);
    }
    return arr;
  }

  private static GruDirectionWeights validateGruWeights(
      GruDirectionWeights w, int gateDim, int inDim, int hidden, String name) throws IOException {
    checkShape(w.weightIh, gateDim, inDim, name + ".weightIh");
    checkLen(w.biasIh, gateDim, name + ".biasIh");
    checkShape(w.weightHh, gateDim, hidden, name + ".weightHh");
    checkLen(w.biasHh, gateDim, name + ".biasHh");
    return w;
  }

  /** Reusable per-thread scratch buffers to keep per-sentence heap allocation minimal. */
  private static final class Scratch {
    final int maxLayerDim;
    final int arcDim1;
    final int relDim1;
    final int maxKernelSize;

    final float[] rawToken;
    final float[] projToken;
    final int[] charIds;
    final float[] charSeq;
    final float[] convOut;
    final float[] gruPrevH;
    final float[] gruGi;
    final float[] gruGh;
    final float[] tmpArc;
    final float[] uRel;
    final float[] vRel;

    int seqCap;
    float[] layerIn;
    float[] layerOut;
    float[] arcDep;
    float[] arcHead;
    float[] relDep;
    float[] relHead;

    Scratch(BiaffineDependencyModel m) {
      this.maxLayerDim = Math.max(m.inputProjDim, m.encoderOutDim);
      this.arcDim1 = m.arcMlpDim + 1;
      this.relDim1 = m.relMlpDim + 1;
      int mk = 1;
      for (int k : m.charKernelSizes) {
        if (k > mk) {
          mk = k;
        }
      }
      this.maxKernelSize = mk;

      this.rawToken = new float[m.rawInputDim];
      this.projToken = new float[m.inputProjDim];
      int maxCharsBuf = MAX_WORD_CHARS + 2 + mk;
      this.charIds = new int[maxCharsBuf];
      this.charSeq = new float[maxCharsBuf * m.charEmbDim];
      this.convOut = new float[Math.max(1, m.charNumFilters)];
      this.gruPrevH = new float[m.gruHidden];
      this.gruGi = new float[3 * m.gruHidden];
      this.gruGh = new float[3 * m.gruHidden];
      this.tmpArc = new float[arcDim1];
      this.uRel = new float[relDim1];
      this.vRel = new float[relDim1];
      ensureSeqLen(64);
    }

    void ensureSeqLen(int needed) {
      if (needed <= seqCap) {
        return;
      }
      int cap = Math.max(needed, Math.max(64, seqCap * 2));
      this.seqCap = cap;
      this.layerIn = new float[cap * maxLayerDim];
      this.layerOut = new float[cap * maxLayerDim];
      this.arcDep = new float[cap * arcDim1];
      this.arcHead = new float[cap * arcDim1];
      this.relDep = new float[cap * relDim1];
      this.relHead = new float[cap * relDim1];
    }
  }
}
