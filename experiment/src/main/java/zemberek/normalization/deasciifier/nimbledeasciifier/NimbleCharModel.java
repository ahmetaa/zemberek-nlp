package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.Token;

/**
 * Morphology-free 5-state Turkish Vowel-Harmony Character Perceptron.
 *
 * <p>Decodes any token or apostrophe stem slice strictly within intra-token boundaries using flat
 * primitive DP arrays (zero heap-matrix allocations on top-1 decoding), apostrophe-transparent
 * character windows, 2-left/2-right surrounding vowel cues, intervocalic yumuşak-g features, and
 * 5-state Turkish vowel harmony transitions.
 */
public final class NimbleCharModel {

  public record ScoredSurface(String surface, float score) {}

  private static final int NUM_HARMONY_STATES = 5;
  private static final int CONTEXT_RADIUS = 6;
  private static final int FEAT_BUF_SIZE = 40;

  private static final char[] OPTS_C = {'c', 'ç'};
  private static final char[] OPTS_G = {'g', 'ğ'};
  private static final char[] OPTS_I = {'i', 'ı'};
  private static final char[] OPTS_O = {'o', 'ö'};
  private static final char[] OPTS_S = {'s', 'ş'};
  private static final char[] OPTS_U = {'u', 'ü'};

  private final NimbleWeightTable weights;

  NimbleCharModel(NimbleWeightTable weights) {
    this.weights = weights;
  }

  public int featureCount() {
    return weights.size();
  }

  public long ramBytesUsed() {
    return weights instanceof NimbleFp16WeightTable fp16 ? fp16.ramBytesUsed() : 0L;
  }

  public void serialize(DataOutputStream out) throws IOException {
    if (!(weights instanceof NimbleFp16WeightTable fp16)) {
      throw new IllegalStateException("Only NimbleFp16WeightTable can be serialized.");
    }
    fp16.serialize(out);
  }

  public static NimbleCharModel deserialize(DataInputStream in) throws IOException {
    return new NimbleCharModel(NimbleFp16WeightTable.deserialize(in));
  }

  /**
   * Scores candidate surfaces at indices where they diverge or have different running harmony
   * states, returning zero-max relative scores clamped to {@code [-6.0f, 0.0f]}.
   */
  public float[] scoreCandidateDifferences(Token token, String[] surfaces, int count) {
    float[] relativeScores = new float[count];
    if (count <= 1) {
      return relativeScores;
    }

    String wordAscii = token.asciiLower();
    int wordLen = wordAscii.length();
    for (int ci = 0; ci < count; ci++) {
      if (surfaces[ci].length() != wordLen) {
        return relativeScores;
      }
    }

    int[] harmonyState = new int[count];
    float[] rawScores = new float[count];
    long[] featBuf = new long[FEAT_BUF_SIZE];
    long wordPrefix4 = NimbleHash.hashPrefix(wordAscii, 4);
    long wordPrefix5 = NimbleHash.hashPrefix(wordAscii, 5);
    long wordSuffix4 = NimbleHash.hashSuffix(wordAscii, 4);
    long wordSuffix5 = NimbleHash.hashSuffix(wordAscii, 5);

    for (int pos = 0; pos < wordLen; pos++) {
      char firstChar = TurkishTextCodec.stripCircumflex(surfaces[0].charAt(pos));
      int firstHarmony = harmonyState[0];
      boolean allIdenticalStep = true;
      for (int ci = 1; ci < count; ci++) {
        if (TurkishTextCodec.stripCircumflex(surfaces[ci].charAt(pos)) != firstChar
            || harmonyState[ci] != firstHarmony) {
          allIdenticalStep = false;
          break;
        }
      }
      if (allIdenticalStep) {
        int nextH = advanceHarmony(firstHarmony, firstChar);
        Arrays.fill(harmonyState, nextH);
        continue;
      }

      long contextWindow = encodeIntraTokenWindow(wordAscii, 0, wordLen, pos);
      long r2 = pos == 0 ? ((contextWindow >>> 32) & 0x3FFL) : 0L;
      long surroundingVowels = packSurroundingAsciiVowels(wordAscii, 0, wordLen, pos);
      int intervocalicG = isIntervocalicG(wordAscii, 0, wordLen, pos);

      for (int ci = 0; ci < count; ci++) {
        char ch = TurkishTextCodec.stripCircumflex(surfaces[ci].charAt(pos));
        int prevH = harmonyState[ci];
        int nextH = advanceHarmony(prevH, ch);

        int m =
            extractEmissionFeatures(
                contextWindow,
                wordPrefix4,
                wordPrefix5,
                wordSuffix4,
                wordSuffix5,
                surroundingVowels,
                intervocalicG,
                pos,
                wordLen,
                ch,
                featBuf);
        m = appendTransitionFeatures(prevH, ch, nextH, pos, wordLen, r2, featBuf, m);
        rawScores[ci] += weights.sum(featBuf, m);
        harmonyState[ci] = nextH;
      }
    }

    float best = Float.NEGATIVE_INFINITY;
    for (float s : rawScores) {
      if (s > best) {
        best = s;
      }
    }
    for (int ci = 0; ci < count; ci++) {
      relativeScores[ci] = Math.clamp(rawScores[ci] - best, -6.0f, 0.0f);
    }
    return relativeScores;
  }

  /** Fast top-1 decoding for an entire token with zero DP-matrix heap allocations. */
  public String decodeTop1(Token token, boolean pinDotlessI) {
    return decodeSliceTop1(token, 0, token.original().length(), pinDotlessI);
  }

  /** Fast top-1 decoding for a token slice {@code [sliceStart, sliceEnd)}. */
  public String decodeSliceTop1(Token token, int sliceStart, int sliceEnd, boolean pinDotlessI) {
    return decodeSliceInternal(token, sliceStart, sliceEnd, pinDotlessI, null);
  }

  /** Decodes top-1 surface along with its raw path score (used for dotless-i rescue voting). */
  public ScoredSurface decodeTop1WithScore(Token token, boolean pinDotlessI) {
    float[] scoreOut = new float[1];
    String surf = decodeSliceInternal(token, 0, token.original().length(), pinDotlessI, scoreOut);
    return new ScoredSurface(surf, scoreOut[0]);
  }

  private String decodeSliceInternal(
      Token token, int sliceStart, int sliceEnd, boolean pinDotlessI, float[] scoreOut) {
    String orig = token.original();
    String wordAscii = token.asciiLower();
    int len = sliceEnd - sliceStart;
    if (len <= 0) {
      if (scoreOut != null) {
        scoreOut[0] = 0.0f;
      }
      return "";
    }

    float[] prevDp = new float[NUM_HARMONY_STATES];
    float[] nextDp = new float[NUM_HARMONY_STATES];
    Arrays.fill(prevDp, Float.NEGATIVE_INFINITY);
    prevDp[0] = 0.0f;

    int[] backPtrs = new int[len * NUM_HARMONY_STATES];
    long[] featBuf = new long[FEAT_BUF_SIZE];
    long wordPrefix4 = NimbleHash.hashSlice(wordAscii, sliceStart, Math.min(sliceEnd, sliceStart + 4));
    long wordPrefix5 = NimbleHash.hashSlice(wordAscii, sliceStart, Math.min(sliceEnd, sliceStart + 5));
    long wordSuffix4 = NimbleHash.hashSlice(wordAscii, Math.max(sliceStart, sliceEnd - 4), sliceEnd);
    long wordSuffix5 = NimbleHash.hashSlice(wordAscii, Math.max(sliceStart, sliceEnd - 5), sliceEnd);
    float[] emitScores = new float[2];

    for (int pos = 0; pos < len; pos++) {
      int tokPos = sliceStart + pos;
      char rawIn = orig.charAt(tokPos);
      char asciiCh = wordAscii.charAt(tokPos);
      char[] opts = allowedOptions(rawIn, asciiCh, token.hasPinnedTurkish(), pinDotlessI);
      long r2 =
          pos == 0 ? (encodeWindowSide(wordAscii, sliceStart, sliceEnd, tokPos + 1, 1) & 0x3FFL) : 0L;

      if (opts.length > 1) {
        long contextWindow = encodeIntraTokenWindow(wordAscii, sliceStart, sliceEnd, tokPos);
        long surroundingVowels =
            packSurroundingAsciiVowels(wordAscii, sliceStart, sliceEnd, tokPos);
        int intervocalicG = isIntervocalicG(wordAscii, sliceStart, sliceEnd, tokPos);
        for (int oi = 0; oi < opts.length; oi++) {
          int m =
              extractEmissionFeatures(
                  contextWindow,
                  wordPrefix4,
                  wordPrefix5,
                  wordSuffix4,
                  wordSuffix5,
                  surroundingVowels,
                  intervocalicG,
                  pos,
                  len,
                  opts[oi],
                  featBuf);
          emitScores[oi] = weights.sum(featBuf, m);
        }
      } else {
        emitScores[0] = 0.0f;
      }

      Arrays.fill(nextDp, Float.NEGATIVE_INFINITY);
      int rowOffset = pos * NUM_HARMONY_STATES;
      for (int prevH = 0; prevH < NUM_HARMONY_STATES; prevH++) {
        float prevScore = prevDp[prevH];
        if (prevScore == Float.NEGATIVE_INFINITY) {
          continue;
        }
        for (int oi = 0; oi < opts.length; oi++) {
          char ch = opts[oi];
          int nextH = advanceHarmony(prevH, ch);
          int tm = appendTransitionFeatures(prevH, ch, nextH, pos, len, r2, featBuf, 0);
          float candScore = prevScore + emitScores[oi] + weights.sum(featBuf, tm);
          if (candScore > nextDp[nextH]) {
            nextDp[nextH] = candScore;
            backPtrs[rowOffset + nextH] = (prevH << 16) | ch;
          }
        }
      }

      float[] tmp = prevDp;
      prevDp = nextDp;
      nextDp = tmp;
    }

    int bestFinalH = 0;
    float bestScore = Float.NEGATIVE_INFINITY;
    for (int h = 0; h < NUM_HARMONY_STATES; h++) {
      if (prevDp[h] > bestScore) {
        bestScore = prevDp[h];
        bestFinalH = h;
      }
    }
    if (scoreOut != null) {
      scoreOut[0] = bestScore;
    }

    char[] bestChars = new char[len];
    int curH = bestFinalH;
    for (int pos = len - 1; pos >= 0; pos--) {
      int packed = backPtrs[pos * NUM_HARMONY_STATES + curH];
      bestChars[pos] = (char) (packed & 0xFFFF);
      curH = packed >>> 16;
    }
    return new String(bestChars);
  }

  public static NimbleCharModel train(
      List<String> cleanSentences, int epochs, int maxFeatures, double typoAugmentRate, long seed) {
    return train(cleanSentences, Set.of(), epochs, maxFeatures, 0.15f, typoAugmentRate, seed);
  }

  /**
   * Trains a {@link NimbleCharModel} on clean Turkish sentences with foreign-token shielding,
   * synthetic typo augmentation, and stochastic partial-ASCII masking.
   */
  public static NimbleCharModel train(
      List<String> cleanSentences,
      Set<String> skipAsciiWords,
      int epochs,
      int maxFeatures,
      float learningRate,
      double typoAugmentRate,
      long seed) {
    NimbleOnlineWeights onlineWeights = new NimbleOnlineWeights(1 << 21);
    NimbleCharModel onlineDecoder = new NimbleCharModel(onlineWeights);
    Random rng = new Random(seed);

    int n = cleanSentences.size();
    int[] order = new int[n];
    for (int i = 0; i < n; i++) {
      order[i] = i;
    }

    long[] featBuf = new long[FEAT_BUF_SIZE];
    for (int epoch = 0; epoch < epochs; epoch++) {
      shuffle(order, rng);
      for (int idx : order) {
        String gold = cleanSentences.get(idx);
        if (gold.length() < 4) {
          continue;
        }
        if (typoAugmentRate > 0.0 && rng.nextDouble() < typoAugmentRate) {
          gold = injectSyntheticTypo(gold, rng);
        }
        String input = createStochasticAsciiInput(gold, rng);
        if (input.length() != gold.length()) {
          continue;
        }

        for (Token tok : TurkishTextCodec.tokenize(input)) {
          if (tok.immutable() || tok.numeralApostrophe()) {
            continue;
          }
          String ascii = tok.asciiLower();
          if (skipAsciiWords != null && skipAsciiWords.contains(ascii)) {
            continue;
          }
          String goldWord =
              TurkishTextCodec.stripCircumflex(
                  TurkishTextCodec.toTurkishLower(gold.substring(tok.start(), tok.end())));
          if (goldWord.length() != tok.original().length()
              || !TurkishTextCodec.hasCoreDiacriticAscii(ascii)) {
            continue;
          }
          onlineWeights.advanceStep();
          String predWord = onlineDecoder.decodeTop1(tok, true);
          if (!predWord.equals(goldWord)) {
            updateWordPath(tok, goldWord, +learningRate, onlineWeights, featBuf);
            updateWordPath(tok, predWord, -learningRate, onlineWeights, featBuf);
          }
        }
      }
    }
    return new NimbleCharModel(onlineWeights.buildFp16Averaged(maxFeatures, 2));
  }

  private static void updateWordPath(
      Token token,
      String targetLower,
      float delta,
      NimbleOnlineWeights weights,
      long[] featBuf) {
    String wordAscii = token.asciiLower();
    int len = wordAscii.length();
    long wordPrefix4 = NimbleHash.hashPrefix(wordAscii, 4);
    long wordPrefix5 = NimbleHash.hashPrefix(wordAscii, 5);
    long wordSuffix4 = NimbleHash.hashSuffix(wordAscii, 4);
    long wordSuffix5 = NimbleHash.hashSuffix(wordAscii, 5);
    int prevH = 0;

    for (int pos = 0; pos < len; pos++) {
      char rawIn = token.original().charAt(pos);
      char asciiCh = wordAscii.charAt(pos);
      char ch = targetLower.charAt(pos);
      int nextH = advanceHarmony(prevH, ch);
      long r2 = pos == 0 ? (encodeWindowSide(wordAscii, 0, len, pos + 1, 1) & 0x3FFL) : 0L;

      if (allowedOptions(rawIn, asciiCh, token.hasPinnedTurkish(), true).length > 1) {
        long contextWindow = encodeIntraTokenWindow(wordAscii, 0, len, pos);
        long surroundingVowels = packSurroundingAsciiVowels(wordAscii, 0, len, pos);
        int intervocalicG = isIntervocalicG(wordAscii, 0, len, pos);
        int m =
            extractEmissionFeatures(
                contextWindow,
                wordPrefix4,
                wordPrefix5,
                wordSuffix4,
                wordSuffix5,
                surroundingVowels,
                intervocalicG,
                pos,
                len,
                ch,
                featBuf);
        weights.addBatch(featBuf, m, delta);
      }
      int tm = appendTransitionFeatures(prevH, ch, nextH, pos, len, r2, featBuf, 0);
      weights.addBatch(featBuf, tm, delta);
      prevH = nextH;
    }
  }

  private static int extractEmissionFeatures(
      long contextWindow,
      long wordPrefix4,
      long wordPrefix5,
      long wordSuffix4,
      long wordSuffix5,
      long surroundingVowels,
      int intervocalicG,
      int posInWord,
      int wordLen,
      char candidateChar,
      long[] out) {
    long l1 = contextWindow & 0x1FL;
    long l2 = contextWindow & 0x3FFL;
    long l3 = contextWindow & 0x7FFFL;
    long l4 = contextWindow & 0xFFFFFL;
    long l5 = contextWindow & 0x1FFFFFFL;
    long l6 = contextWindow & 0x3FFFFFFFL;
    long r1 = (contextWindow >>> 32) & 0x1FL;
    long r2 = (contextWindow >>> 32) & 0x3FFL;
    long r3 = (contextWindow >>> 32) & 0x7FFFL;
    long r4 = (contextWindow >>> 32) & 0xFFFFFL;
    long r5 = (contextWindow >>> 32) & 0x1FFFFFFL;
    long r6 = (contextWindow >>> 32) & 0x3FFFFFFFL;

    int distRight = Math.min(6, wordLen - 1 - posInWord);
    int distLeft = Math.min(6, posInWord);
    long ch = candidateChar;

    int m = 0;
    out[m++] = NimbleHash.of(101, ch);
    out[m++] = NimbleHash.of(102, l1, ch);
    out[m++] = NimbleHash.of(103, l2, ch);
    out[m++] = NimbleHash.of(104, l3, ch);
    out[m++] = NimbleHash.of(105, l4, ch);
    out[m++] = NimbleHash.of(106, l5, ch);
    out[m++] = NimbleHash.of(107, l6, ch);
    out[m++] = NimbleHash.of(108, r1, ch);
    out[m++] = NimbleHash.of(109, r2, ch);
    out[m++] = NimbleHash.of(110, r3, ch);
    out[m++] = NimbleHash.of(111, r4, ch);
    out[m++] = NimbleHash.of(112, r5, ch);
    out[m++] = NimbleHash.of(113, r6, ch);
    out[m++] = NimbleHash.of(114, l1, r1, ch);
    out[m++] = NimbleHash.of(115, l2, r1, ch);
    out[m++] = NimbleHash.of(116, l1, r2, ch);
    out[m++] = NimbleHash.of(117, l2, r2, ch);
    out[m++] = NimbleHash.of(118, l3, r2, ch);
    out[m++] = NimbleHash.of(119, l2, r3, ch);
    out[m++] = NimbleHash.of(120, l3, r3, ch);
    out[m++] = NimbleHash.of(121, l4, r2, ch);
    out[m++] = NimbleHash.of(122, l2, r4, ch);
    out[m++] = NimbleHash.of(123, (distLeft << 4) | distRight, ch);
    out[m++] = NimbleHash.of(124, wordSuffix4, distRight, ch);
    out[m++] = NimbleHash.of(125, wordSuffix5, distRight, ch);
    out[m++] = NimbleHash.of(126, wordPrefix4, distLeft, ch);
    out[m++] = NimbleHash.of(127, wordPrefix5, distLeft, ch);
    out[m++] = NimbleHash.of(128, surroundingVowels, distRight, ch);
    if (intervocalicG != 0) {
      out[m++] = NimbleHash.of(129, surroundingVowels, ch);
    }
    return m;
  }

  private static int appendTransitionFeatures(
      int prevHarmony,
      char candidateChar,
      int nextHarmony,
      int posInWord,
      int wordLen,
      long r2,
      long[] out,
      int m) {
    out[m++] = NimbleHash.of(130, prevHarmony, candidateChar);
    out[m++] = NimbleHash.of(131, prevHarmony, nextHarmony, Math.min(5, posInWord));
    if (posInWord == wordLen - 1) {
      // Condition word-final vowel on preceding syllable's harmony, never candidateChar itself.
      out[m++] = NimbleHash.of(132, prevHarmony, candidateChar);
    }
    if (posInWord == 0) {
      out[m++] = NimbleHash.of(133, candidateChar, r2);
    }
    return m;
  }

  /**
   * Encodes up to 6 left and 6 right characters strictly within {@code [sliceStart, sliceEnd)}
   * (stepping cleanly over internal apostrophes and halting at slice/token boundaries).
   */
  private static long encodeIntraTokenWindow(
      String tokenAscii, int sliceStart, int sliceEnd, int centerIndex) {
    return encodeWindowSide(tokenAscii, sliceStart, sliceEnd, centerIndex - 1, -1)
        | (encodeWindowSide(tokenAscii, sliceStart, sliceEnd, centerIndex + 1, 1) << 32);
  }

  private static long encodeWindowSide(
      String tokenAscii, int sliceStart, int sliceEnd, int startIndex, int step) {
    long packed = 0L;
    int p = startIndex;
    int collected = 0;
    while (collected < CONTEXT_RADIUS) {
      int sym;
      if (p < sliceStart || p >= sliceEnd) {
        int prevP = p - step;
        sym = (collected == 0 || (prevP >= sliceStart && prevP < sliceEnd)) ? 28 : 27;
        p += step;
      } else {
        char c = tokenAscii.charAt(p);
        p += step;
        if (TurkishTextCodec.isApostrophe(c)) {
          continue;
        }
        sym = encodeSymbol5Bit(c);
      }
      packed |= ((long) sym) << (collected * 5);
      collected++;
    }
    return packed;
  }

  private static int encodeSymbol5Bit(char c) {
    char a = TurkishTextCodec.toAsciiLower(c);
    if (a >= 'a' && a <= 'z') {
      return a - 'a' + 1;
    }
    if (Character.isDigit(c)) {
      return 30;
    }
    return 31;
  }

  /** Packs 2 left + 2 right ASCII vowels inside {@code [sliceStart, sliceEnd)}. */
  private static long packSurroundingAsciiVowels(
      String wordAscii, int sliceStart, int sliceEnd, int pos) {
    int lv1 = '_', lv2 = '_';
    for (int i = pos - 1; i >= sliceStart; i--) {
      char c = wordAscii.charAt(i);
      if (isAsciiVowel(c)) {
        if (lv1 == '_') {
          lv1 = c;
        } else {
          lv2 = c;
          break;
        }
      }
    }
    int rv1 = '_', rv2 = '_';
    for (int i = pos + 1; i < sliceEnd; i++) {
      char c = wordAscii.charAt(i);
      if (isAsciiVowel(c)) {
        if (rv1 == '_') {
          rv1 = c;
        } else {
          rv2 = c;
          break;
        }
      }
    }
    return ((long) lv2 << 24) | ((long) lv1 << 16) | ((long) rv1 << 8) | rv2;
  }

  private static int isIntervocalicG(String wordAscii, int sliceStart, int sliceEnd, int pos) {
    if (pos <= sliceStart || pos + 1 >= sliceEnd || wordAscii.charAt(pos) != 'g') {
      return 0;
    }
    return (isAsciiVowel(wordAscii.charAt(pos - 1)) && isAsciiVowel(wordAscii.charAt(pos + 1)))
        ? 1
        : 0;
  }

  private static boolean isAsciiVowel(char c) {
    return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
  }

  static int advanceHarmony(int prevHarmony, char ch) {
    return switch (ch) {
      case 'a', 'ı', 'â' -> 1;
      case 'e', 'i', 'î' -> 2;
      case 'o', 'u' -> 3;
      case 'ö', 'ü', 'û' -> 4;
      default -> prevHarmony;
    };
  }

  private static char[] allowedOptions(
      char rawIn, char asciiLower, boolean hasPinnedTurkish, boolean pinDotlessI) {
    if (!TurkishTextCodec.isLetter(rawIn)) {
      return new char[] {rawIn};
    }
    if (hasPinnedTurkish) {
      boolean pinned =
          pinDotlessI
              ? TurkishTextCodec.isPinnedTurkishDiacritic(rawIn)
              : TurkishTextCodec.isNonDotlessTurkishDiacritic(rawIn);
      if (pinned) {
        return new char[] {TurkishTextCodec.toTurkishLower(rawIn)};
      }
    }
    return switch (asciiLower) {
      case 'c' -> OPTS_C;
      case 'g' -> OPTS_G;
      case 'i' -> OPTS_I;
      case 'o' -> OPTS_O;
      case 's' -> OPTS_S;
      case 'u' -> OPTS_U;
      default -> new char[] {asciiLower};
    };
  }

  static String createStochasticAsciiInput(String gold, Random rng) {
    if (rng.nextDouble() < 0.78) {
      return TurkishTextCodec.asciify(gold);
    }
    double stripProb = 0.35 + 0.50 * rng.nextDouble();
    char[] chars = gold.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      if (rng.nextDouble() < stripProb) {
        chars[i] = TurkishTextCodec.toAsciiPreserveCase(chars[i]);
      }
    }
    return new String(chars);
  }

  private static String injectSyntheticTypo(String sentence, Random rng) {
    String replaced =
        sentence
            .replace("yalnız", "yanlız")
            .replace("şarj", "şarz")
            .replace("her şey", "herşey")
            .replace("bir şey", rng.nextBoolean() ? "birşey" : "bişey")
            .replace("tabii ki", "tabiki");
    if (!replaced.equals(sentence)) {
      return replaced;
    }
    List<Token> tokens = TurkishTextCodec.tokenize(sentence);
    if (tokens.isEmpty()) {
      return sentence;
    }
    Token target = tokens.get(rng.nextInt(tokens.size()));
    String w = target.original();
    if (w.length() < 6 || target.hasApostrophe() || target.immutable()) {
      return sentence;
    }
    if (rng.nextDouble() < 0.35 && TurkishTextCodec.isLetter(w.charAt(w.length() - 1))) {
      char last = w.charAt(w.length() - 1);
      String elongated = w + last + (rng.nextBoolean() ? String.valueOf(last) : "");
      return sentence.substring(0, target.start()) + elongated + sentence.substring(target.end());
    }
    if (w.length() < 7) {
      return sentence;
    }
    int dropPos = 2 + rng.nextInt(w.length() - 4);
    String mutated = w.substring(0, dropPos) + w.substring(dropPos + 1);
    return sentence.substring(0, target.start()) + mutated + sentence.substring(target.end());
  }

  private static void shuffle(int[] array, Random rng) {
    for (int i = array.length - 1; i > 0; i--) {
      int j = rng.nextInt(i + 1);
      int tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }
}
