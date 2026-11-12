package zemberek.normalization.deasciifier.latticedeasciifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * 5-state Turkish Vowel-Harmony Character Perceptron.
 *
 * <p>Serves two focused roles in the Lattice Perceptron architecture:
 * <ol>
 *   <li><b>OOV & Typo Fallback ({@link #decodeTopCandidates})</b>: restores core Turkish diacritics
 *       on words outside Zemberek's lexicon ({@code kübitlerinin}, {@code şarzım}, {@code
 *       öğretmnler}) without hallucinating circumflexes on productive/slang stems.
 *   <li><b>Ambiguous Candidate Scoring ({@link #scoreCandidateDifferences})</b>: computes local
 *       character-harmony scores strictly at character indices where candidates diverge.
 * </ol>
 */
public final class CharacterHarmonyModel {

  public record ScoredSurface(String surface, float score) {}

  private static final int NUM_HARMONY_STATES = 5;
  private static final int CONTEXT_RADIUS = 6;

  private static final char[] OPTS_C = {'c', 'ç'};
  private static final char[] OPTS_G = {'g', 'ğ'};
  private static final char[] OPTS_I = {'i', 'ı'};
  private static final char[] OPTS_O = {'o', 'ö'};
  private static final char[] OPTS_S = {'s', 'ş'};
  private static final char[] OPTS_U = {'u', 'ü'};

  private final WeightTable weights;

  CharacterHarmonyModel(WeightTable weights) {
    this.weights = weights;
  }

  public int featureCount() {
    return weights.size();
  }

  public void serialize(DataOutputStream out) throws IOException {
    if (!(weights instanceof QuantizedWeightTable quantized)) {
      throw new IllegalStateException("Only QuantizedWeightTable can be serialized.");
    }
    quantized.serialize(out);
  }

  public static CharacterHarmonyModel deserialize(DataInputStream in) throws IOException {
    return new CharacterHarmonyModel(QuantizedWeightTable.deserialize(in));
  }

  /**
   * Scores ambiguous candidate surfaces by evaluating character emission and vowel-harmony
   * transition features only at positions where candidates diverge. Returns zero-max relative
   * scores clamped to {@code [-6.0f, 0.0f]}.
   */
  public float[] scoreCandidateDifferences(
      String sentence, Token token, List<Candidate> candidates) {
    int k = candidates.size();
    float[] relativeScores = new float[k];
    if (k <= 1) {
      return relativeScores;
    }

    String wordAscii = token.asciiLower();
    int wordLen = wordAscii.length();
    for (Candidate c : candidates) {
      if (c.surface.length() != wordLen) {
        return relativeScores;
      }
    }

    int[] harmonyState = new int[k];
    float[] rawScores = new float[k];
    long[] featBuf = new long[24];
    long wordPrefix4 = FeatureHash.hashPrefix(wordAscii, 4);
    long wordSuffix4 = FeatureHash.hashSuffix(wordAscii, 4);

    for (int pos = 0; pos < wordLen; pos++) {
      char firstChar = candidates.getFirst().surface.charAt(pos);
      int firstHarmony = harmonyState[0];
      boolean allIdenticalStep = true;
      for (int ci = 1; ci < k; ci++) {
        if (candidates.get(ci).surface.charAt(pos) != firstChar
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

      int absPos = token.start() + pos;
      long contextWindow = encodeSurroundingWindow(sentence, absPos);
      long surroundingVowels = packSurroundingAsciiVowels(wordAscii, pos);

      for (int ci = 0; ci < k; ci++) {
        char ch = candidates.get(ci).surface.charAt(pos);
        int prevH = harmonyState[ci];
        int nextH = advanceHarmony(prevH, ch);

        int m =
            extractEmissionFeatures(
                contextWindow,
                wordPrefix4,
                wordSuffix4,
                surroundingVowels,
                pos,
                wordLen,
                ch,
                featBuf);
        m = appendTransitionFeatures(prevH, ch, nextH, pos, wordLen, featBuf, m);
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
    for (int ci = 0; ci < k; ci++) {
      relativeScores[ci] = Math.clamp(rawScores[ci] - best, -6.0f, 0.0f);
    }
    return relativeScores;
  }

  /**
   * Restores top-{@code maxCandidates} lowercase surfaces for an OOV or misspelled token using
   * 5-state vowel-harmony Viterbi decoding.
   */
  public List<ScoredSurface> decodeTopCandidates(String sentence, Token token, int maxCandidates) {
    String orig = token.original();
    String wordAscii = token.asciiLower();
    boolean pinTurkish = token.hasPinnedTurkish();
    int len = orig.length();
    if (len == 0) {
      return List.of(new ScoredSurface("", 0.0f));
    }

    float[][] dp = new float[len + 1][NUM_HARMONY_STATES];
    int[][] backState = new int[len][NUM_HARMONY_STATES];
    char[][] backChar = new char[len][NUM_HARMONY_STATES];
    for (int p = 0; p <= len; p++) {
      Arrays.fill(dp[p], Float.NEGATIVE_INFINITY);
    }
    dp[0][0] = 0.0f;

    long[] featBuf = new long[24];
    long wordPrefix4 = FeatureHash.hashPrefix(wordAscii, 4);
    long wordSuffix4 = FeatureHash.hashSuffix(wordAscii, 4);

    char[][] optionsAt = new char[len][];
    float[][] emitScoresAt = new float[len][];

    for (int pos = 0; pos < len; pos++) {
      char rawIn = orig.charAt(pos);
      char asciiCh = wordAscii.charAt(pos);
      char[] opts = allowedOptions(rawIn, asciiCh, pinTurkish);
      optionsAt[pos] = opts;

      float[] emitScores = new float[opts.length];
      emitScoresAt[pos] = emitScores;
      if (opts.length > 1) {
        long contextWindow = encodeSurroundingWindow(sentence, token.start() + pos);
        long surroundingVowels = packSurroundingAsciiVowels(wordAscii, pos);
        for (int oi = 0; oi < opts.length; oi++) {
          int m =
              extractEmissionFeatures(
                  contextWindow,
                  wordPrefix4,
                  wordSuffix4,
                  surroundingVowels,
                  pos,
                  len,
                  opts[oi],
                  featBuf);
          emitScores[oi] = weights.sum(featBuf, m);
        }
      }

      for (int prevH = 0; prevH < NUM_HARMONY_STATES; prevH++) {
        float prevScore = dp[pos][prevH];
        if (prevScore == Float.NEGATIVE_INFINITY) {
          continue;
        }
        for (int oi = 0; oi < opts.length; oi++) {
          char ch = opts[oi];
          int nextH = advanceHarmony(prevH, ch);
          int tm = appendTransitionFeatures(prevH, ch, nextH, pos, len, featBuf, 0);
          float candScore = prevScore + emitScores[oi] + weights.sum(featBuf, tm);
          if (candScore > dp[pos + 1][nextH]) {
            dp[pos + 1][nextH] = candScore;
            backState[pos][nextH] = prevH;
            backChar[pos][nextH] = ch;
          }
        }
      }
    }

    int bestFinalH = 0;
    float bestScore = Float.NEGATIVE_INFINITY;
    for (int h = 0; h < NUM_HARMONY_STATES; h++) {
      if (dp[len][h] > bestScore) {
        bestScore = dp[len][h];
        bestFinalH = h;
      }
    }

    char[] bestChars = new char[len];
    int curH = bestFinalH;
    for (int pos = len - 1; pos >= 0; pos--) {
      bestChars[pos] = backChar[pos][curH];
      curH = backState[pos][curH];
    }
    String top1 = new String(bestChars);

    List<ScoredSurface> results = new ArrayList<>(Math.min(maxCandidates, 3));
    results.add(new ScoredSurface(top1, bestScore));
    if (maxCandidates <= 1) {
      return results;
    }

    for (int pos = 0; pos < len; pos++) {
      char[] opts = optionsAt[pos];
      if (opts.length <= 1) {
        continue;
      }
      for (char alt : opts) {
        if (alt == bestChars[pos]) {
          continue;
        }
        char[] mutated = bestChars.clone();
        mutated[pos] = alt;
        String surf = new String(mutated);
        if (containsSurface(results, surf)) {
          continue;
        }
        float score = scoreFullPath(mutated, optionsAt, emitScoresAt, featBuf);
        results.add(new ScoredSurface(surf, score));
      }
    }
    results.sort((a, b) -> Float.compare(b.score(), a.score()));
    return results.size() > maxCandidates ? results.subList(0, maxCandidates) : results;
  }

  private float scoreFullPath(
      char[] path, char[][] optionsAt, float[][] emitScoresAt, long[] featBuf) {
    float total = 0.0f;
    int prevH = 0;
    int len = path.length;
    for (int pos = 0; pos < len; pos++) {
      char ch = path[pos];
      char[] opts = optionsAt[pos];
      for (int oi = 0; oi < opts.length; oi++) {
        if (opts[oi] == ch) {
          total += emitScoresAt[pos][oi];
          break;
        }
      }
      int nextH = advanceHarmony(prevH, ch);
      int tm = appendTransitionFeatures(prevH, ch, nextH, pos, len, featBuf, 0);
      total += weights.sum(featBuf, tm);
      prevH = nextH;
    }
    return total;
  }

  /**
   * Trains a {@link CharacterHarmonyModel} on clean Turkish sentences with synthetic typo
   * augmentation so it generalizes cleanly to OOV words and noisy stems.
   */
  public static CharacterHarmonyModel train(
      List<String> cleanSentences,
      boolean[] circumflexIntact,
      LexiconPriors priors,
      int epochs,
      int maxFeatures,
      double typoAugmentRate,
      long seed) {
    AveragedPerceptronWeights onlineWeights = new AveragedPerceptronWeights(1 << 20);
    CharacterHarmonyModel onlineDecoder = new CharacterHarmonyModel(onlineWeights);
    Random rng = new Random(seed);

    int n = cleanSentences.size();
    int[] order = new int[n];
    for (int i = 0; i < n; i++) {
      order[i] = i;
    }

    long[] featBuf = new long[24];
    for (int epoch = 0; epoch < epochs; epoch++) {
      shuffle(order, rng);
      for (int idx : order) {
        String gold = cleanSentences.get(idx);
        if (gold.length() < 4) {
          continue;
        }
        boolean circOk = circumflexIntact == null || circumflexIntact[idx];
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
          String goldWord =
              TurkishTextCodec.toTurkishLower(gold.substring(tok.start(), tok.end()));
          if (goldWord.length() != tok.original().length()
              || !TurkishTextCodec.hasAmbiguousAscii(tok.asciiLower())) {
            continue;
          }
          if (!circOk && priors != null && priors.hasCircumflexReading(tok.asciiLower())) {
            continue;
          }
          // Strip circumflex for core vowel-harmony training when training OOV character transitions;
          // circumflex emission weights are still learned when goldWord contains â/î/û in circOk lines.
          String targetWord = circOk ? goldWord : TurkishTextCodec.stripCircumflex(goldWord);
          onlineWeights.advanceStep();
          String predWord = onlineDecoder.decodeTopCandidates(input, tok, 1).getFirst().surface();
          if (!predWord.equals(TurkishTextCodec.stripCircumflex(targetWord))) {
            updateWordPath(input, tok, targetWord, +1.0f, onlineWeights, featBuf);
            updateWordPath(input, tok, predWord, -1.0f, onlineWeights, featBuf);
          } else if (circOk && !targetWord.equals(predWord)) {
            updateWordPath(input, tok, targetWord, +0.5f, onlineWeights, featBuf);
            updateWordPath(input, tok, predWord, -0.5f, onlineWeights, featBuf);
          }
        }
      }
    }
    return new CharacterHarmonyModel(onlineWeights.buildQuantizedAveraged(maxFeatures));
  }

  private static void updateWordPath(
      String sentence,
      Token token,
      String targetLower,
      float delta,
      AveragedPerceptronWeights weights,
      long[] featBuf) {
    String wordAscii = token.asciiLower();
    int len = wordAscii.length();
    long wordPrefix4 = FeatureHash.hashPrefix(wordAscii, 4);
    long wordSuffix4 = FeatureHash.hashSuffix(wordAscii, 4);
    int prevH = 0;

    for (int pos = 0; pos < len; pos++) {
      char rawIn = token.original().charAt(pos);
      char asciiCh = wordAscii.charAt(pos);
      char ch = targetLower.charAt(pos);
      int nextH = advanceHarmony(prevH, ch);
      boolean isAmbiguous =
          allowedOptions(rawIn, asciiCh, token.hasPinnedTurkish()).length > 1
              || ch == 'â'
              || ch == 'î'
              || ch == 'û';

      if (isAmbiguous) {
        long contextWindow = encodeSurroundingWindow(sentence, token.start() + pos);
        long surroundingVowels = packSurroundingAsciiVowels(wordAscii, pos);
        int m =
            extractEmissionFeatures(
                contextWindow,
                wordPrefix4,
                wordSuffix4,
                surroundingVowels,
                pos,
                len,
                ch,
                featBuf);
        weights.addBatch(featBuf, m, delta);
      }
      int tm = appendTransitionFeatures(prevH, ch, nextH, pos, len, featBuf, 0);
      weights.addBatch(featBuf, tm, delta);
      prevH = nextH;
    }
  }

  private static int extractEmissionFeatures(
      long contextWindow,
      long wordPrefix4,
      long wordSuffix4,
      long surroundingVowels,
      int posInWord,
      int wordLen,
      char candidateChar,
      long[] out) {
    long l1 = contextWindow & 0x1FL;
    long l2 = contextWindow & 0x3FFL;
    long l3 = contextWindow & 0x7FFFL;
    long l5 = contextWindow & 0x1FFFFFFL;
    long r1 = (contextWindow >>> 32) & 0x1FL;
    long r2 = (contextWindow >>> 32) & 0x3FFL;
    long r3 = (contextWindow >>> 32) & 0x7FFFL;
    long r5 = (contextWindow >>> 32) & 0x1FFFFFFL;

    int distRight = Math.min(6, wordLen - 1 - posInWord);
    int distLeft = Math.min(6, posInWord);
    long ch = candidateChar;

    int m = 0;
    out[m++] = FeatureHash.of(101, ch);
    out[m++] = FeatureHash.of(102, l1, ch);
    out[m++] = FeatureHash.of(103, l2, ch);
    out[m++] = FeatureHash.of(104, l3, ch);
    out[m++] = FeatureHash.of(105, l5, ch);
    out[m++] = FeatureHash.of(106, r1, ch);
    out[m++] = FeatureHash.of(107, r2, ch);
    out[m++] = FeatureHash.of(108, r3, ch);
    out[m++] = FeatureHash.of(109, r5, ch);
    out[m++] = FeatureHash.of(110, l1, r1, ch);
    out[m++] = FeatureHash.of(111, l2, r1, ch);
    out[m++] = FeatureHash.of(112, l1, r2, ch);
    out[m++] = FeatureHash.of(113, l2, r2, ch);
    out[m++] = FeatureHash.of(114, l3, r2, ch);
    out[m++] = FeatureHash.of(115, l2, r3, ch);
    out[m++] = FeatureHash.of(116, (distLeft << 4) | distRight, ch);
    out[m++] = FeatureHash.of(117, wordSuffix4, distRight, ch);
    out[m++] = FeatureHash.of(118, wordPrefix4, distLeft, ch);
    out[m++] = FeatureHash.of(119, surroundingVowels, distRight, ch);
    return m;
  }

  private static int appendTransitionFeatures(
      int prevHarmony,
      char candidateChar,
      int nextHarmony,
      int posInWord,
      int wordLen,
      long[] out,
      int m) {
    out[m++] = FeatureHash.of(130, prevHarmony, candidateChar);
    out[m++] = FeatureHash.of(131, prevHarmony, nextHarmony, Math.min(5, posInWord));
    if (posInWord == wordLen - 1) {
      out[m++] = FeatureHash.of(132, prevHarmony, candidateChar);
    }
    return m;
  }

  private static long encodeSurroundingWindow(String sentence, int centerIndex) {
    return encodeWindowSide(sentence, centerIndex - 1, -1)
        | (encodeWindowSide(sentence, centerIndex + 1, 1) << 32);
  }

  private static long encodeWindowSide(String sentence, int startIndex, int step) {
    int n = sentence.length();
    long packed = 0L;
    int p = startIndex;
    boolean hitSpace = false;
    for (int k = 0; k < CONTEXT_RADIUS; k++) {
      int sym;
      if (p < 0 || p >= n) {
        sym = 27;
      } else {
        char c = sentence.charAt(p);
        p += step;
        if (Character.isWhitespace(c)) {
          if (hitSpace) {
            sym = 27;
          } else {
            hitSpace = true;
            sym = 28;
          }
        } else {
          sym = encodeSymbol5Bit(c);
        }
      }
      packed |= ((long) sym) << (k * 5);
    }
    return packed;
  }

  private static int encodeSymbol5Bit(char c) {
    char a = TurkishTextCodec.toAsciiLower(c);
    if (a >= 'a' && a <= 'z') {
      return a - 'a' + 1;
    }
    if (TurkishTextCodec.isApostrophe(c)) {
      return 29;
    }
    if (Character.isDigit(c)) {
      return 30;
    }
    return 31;
  }

  private static long packSurroundingAsciiVowels(String wordAscii, int pos) {
    char prevV = findNearestAsciiVowel(wordAscii, pos - 1, -1);
    char nextV = findNearestAsciiVowel(wordAscii, pos + 1, 1);
    return ((long) prevV << 16) | nextV;
  }

  private static char findNearestAsciiVowel(String wordAscii, int start, int step) {
    for (int i = start; i >= 0 && i < wordAscii.length(); i += step) {
      char c = wordAscii.charAt(i);
      if (isAsciiVowel(c)) {
        return c;
      }
    }
    return '_';
  }

  private static boolean isAsciiVowel(char c) {
    return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
  }

  private static int advanceHarmony(int prevHarmony, char ch) {
    return switch (ch) {
      case 'a', 'ı' -> 1;
      case 'o', 'u' -> 2;
      case 'e', 'i', 'â', 'î' -> 3;
      case 'ö', 'ü', 'û' -> 4;
      default -> prevHarmony;
    };
  }

  private static char[] allowedOptions(char rawIn, char asciiLower, boolean pinTurkish) {
    if (!TurkishTextCodec.isLetter(rawIn)) {
      return new char[] {rawIn};
    }
    if (pinTurkish && TurkishTextCodec.isPinnedTurkishDiacritic(rawIn)) {
      return new char[] {TurkishTextCodec.toTurkishLower(rawIn)};
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

  private static boolean containsSurface(List<ScoredSurface> list, String surface) {
    for (ScoredSurface s : list) {
      if (s.surface().equals(surface)) {
        return true;
      }
    }
    return false;
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

  static void shuffle(int[] array, Random rng) {
    for (int i = array.length - 1; i > 0; i--) {
      int j = rng.nextInt(i + 1);
      int tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }
}
