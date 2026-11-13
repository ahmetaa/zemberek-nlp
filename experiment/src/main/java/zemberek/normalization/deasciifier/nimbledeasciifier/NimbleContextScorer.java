package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.util.List;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.Token;

/**
 * Single source of truth for morphology-free sentence-context feature extraction and scoring
 * across Pass-1 beam decoding, Pass-2 SOV refinement, and online Perceptron training.
 */
final class NimbleContextScorer {

  static final int FEATURE_BUFFER_SIZE = 64;

  private static final int FEAT_UNIGRAM_SURFACE = 501;
  private static final int FEAT_UNIGRAM_STEM4 = 502;
  private static final int FEAT_RIGHT_PUNCT_STEM4 = 503;
  private static final int FEAT_SENT_END_PUNCT_SUF3 = 504;
  private static final int FEAT_CASING_SURFACE = 505;
  private static final int FEAT_CHAR_SCORE_BUCKET = 506;
  private static final int FEAT_REDUPLICATION = 507;

  private static final int FEAT_PREV_ASCII_SURFACE = 510;
  private static final int FEAT_PREV_ASCII_STEM4 = 511;
  private static final int FEAT_PREV_STEM5_SURFACE = 512;
  private static final int FEAT_PREV_STEM5_STEM4 = 513;
  private static final int FEAT_PREV_SUF3_SURFACE = 514;
  private static final int FEAT_PREV_SUF3_STEM4 = 515;
  private static final int FEAT_NEXT_ASCII_SURFACE = 516;
  private static final int FEAT_NEXT_ASCII_STEM4 = 517;
  private static final int FEAT_NEXT_STEM5_SURFACE = 518;
  private static final int FEAT_NEXT_STEM5_STEM4 = 519;

  private static final int FEAT_BIGRAM_SURFACE = 520;
  private static final int FEAT_BIGRAM_STEM4 = 521;
  private static final int FEAT_PREV_SUF2_CUR_STEM4 = 522;
  private static final int FEAT_PREV_HARMONY_SURFACE = 523;
  private static final int FEAT_TRIGRAM_STEM4 = 524;
  private static final int FEAT_PREV_STEM4_STEM3 = 526;
  private static final int FEAT_NEXT_STEM4_STEM3 = 528;
  private static final int FEAT_LOCAL_CONTENT_STEM3 = 529;
  private static final int FEAT_BIGRAM_STEM3 = 530;

  private static final int FEAT_NEXT_SUF3_STEM4 = 535;
  private static final int FEAT_SANDWICH_ASCII_SURFACE = 536;
  private static final int FEAT_SANDWICH_STEM4 = 537;
  private static final int FEAT_LOCAL_CONTENT_ASCII = 538;

  private static final int FEAT_WIDE_CONTENT_STEM4 = 541;
  private static final int FEAT_WIDE_DIRECTIONAL_STEM4 = 542;
  private static final int FEAT_WIDE_CONTENT_STEM3 = 544;

  private NimbleContextScorer() {}

  static float scoreUnary(
      int tokenIndex,
      List<Token> tokens,
      char sentenceEndPunct,
      boolean sentenceCapitalized,
      NimbleCandidate cand,
      NimbleLexicon lexicon,
      NimbleWeightTable weights,
      long[] featBuf) {
    float prior = computeDeterministicPrior(tokens.get(tokenIndex), cand, sentenceCapitalized, lexicon);
    int m =
        extractUnaryFeatures(
            tokenIndex, tokens, sentenceEndPunct, sentenceCapitalized, cand, featBuf);
    return prior + weights.sum(featBuf, m);
  }

  static float computeDeterministicPrior(
      Token token, NimbleCandidate cand, boolean sentenceCapitalized, NimbleLexicon lexicon) {
    float s = 0.78f * cand.empiricalLogProb + 0.25f * cand.charRelScore;
    if (cand.oovFallback) {
      s -= 1.20f;
    } else if (!token.sentenceStart() && token.casing() == Casing.TITLE) {
      s += cand.properCandidate ? 0.65f : -0.25f;
    } else if (token.casing() == Casing.LOWER && cand.properCandidate) {
      s -= (sentenceCapitalized && !token.sentenceStart()) ? 2.20f : 0.45f;
    }
    if (lexicon.isTabooSurface(cand.surface)
        && !token.original().equalsIgnoreCase(cand.surface)) {
      s -= 4.5f;
    }
    return s;
  }

  static int extractUnaryFeatures(
      int tokenIndex,
      List<Token> tokens,
      char sentenceEndPunct,
      boolean sentenceCapitalized,
      NimbleCandidate cand,
      long[] out) {
    Token token = tokens.get(tokenIndex);
    int n = tokens.size();
    int m = 0;

    out[m++] = NimbleHash.of(FEAT_UNIGRAM_SURFACE, cand.surfaceHash);
    out[m++] = NimbleHash.of(FEAT_UNIGRAM_STEM4, cand.stem4Hash);

    if (token.rightPunct() != ' ') {
      out[m++] = NimbleHash.of(FEAT_RIGHT_PUNCT_STEM4, cand.stem4Hash, token.rightPunct());
    }
    if (sentenceEndPunct == '?' || sentenceEndPunct == '!') {
      out[m++] = NimbleHash.of(FEAT_SENT_END_PUNCT_SUF3, cand.suf3Hash, sentenceEndPunct);
    }

    long shapeKey =
        ((long) token.casing().ordinal() << 2)
            | (token.sentenceStart() ? 2L : 0L)
            | (sentenceCapitalized ? 1L : 0L);
    out[m++] = NimbleHash.of(FEAT_CASING_SURFACE, shapeKey, cand.surfaceHash);

    int charBucket = Math.clamp(Math.round(cand.charRelScore), -6, 0);
    out[m++] = NimbleHash.of(FEAT_CHAR_SCORE_BUCKET, charBucket, cand.oovFallback ? 1L : 0L);

    boolean reduplicated =
        (tokenIndex > 0 && tokens.get(tokenIndex - 1).asciiLower().equals(token.asciiLower()))
            || (tokenIndex + 1 < n
                && tokens.get(tokenIndex + 1).asciiLower().equals(token.asciiLower()));
    if (reduplicated) {
      out[m++] = NimbleHash.of(FEAT_REDUPLICATION, cand.surfaceHash, 1L);
    }

    long prevAsciiHash = 0L;
    long prevStem4Hash = 0L;
    if (tokenIndex > 0) {
      String prevAscii = tokens.get(tokenIndex - 1).asciiLower();
      prevAsciiHash = NimbleHash.hashString(prevAscii);
      prevStem4Hash = NimbleHash.hashPrefix(prevAscii, 4);
      out[m++] = NimbleHash.of(FEAT_PREV_ASCII_SURFACE, prevAsciiHash, cand.surfaceHash);
      out[m++] = NimbleHash.of(FEAT_PREV_ASCII_STEM4, prevAsciiHash, cand.stem4Hash);
      out[m++] = NimbleHash.of(FEAT_PREV_STEM4_STEM3, prevStem4Hash, cand.stem3Hash);
      if (prevAscii.length() >= 3) {
        long prevStem5 = NimbleHash.hashPrefix(prevAscii, 5);
        long prevSuf3 = NimbleHash.hashSuffix(prevAscii, 3);
        out[m++] = NimbleHash.of(FEAT_PREV_STEM5_SURFACE, prevStem5, cand.surfaceHash);
        out[m++] = NimbleHash.of(FEAT_PREV_STEM5_STEM4, prevStem5, cand.stem4Hash);
        out[m++] = NimbleHash.of(FEAT_PREV_SUF3_SURFACE, prevSuf3, cand.surfaceHash);
        out[m++] = NimbleHash.of(FEAT_PREV_SUF3_STEM4, prevSuf3, cand.stem4Hash);
      }
    }

    long nextAsciiHash = 0L;
    long nextStem4Hash = 0L;
    if (tokenIndex + 1 < n) {
      String nextAscii = tokens.get(tokenIndex + 1).asciiLower();
      nextAsciiHash = NimbleHash.hashString(nextAscii);
      nextStem4Hash = NimbleHash.hashPrefix(nextAscii, 4);
      out[m++] = NimbleHash.of(FEAT_NEXT_ASCII_SURFACE, cand.surfaceHash, nextAsciiHash);
      out[m++] = NimbleHash.of(FEAT_NEXT_ASCII_STEM4, cand.stem4Hash, nextAsciiHash);
      out[m++] = NimbleHash.of(FEAT_NEXT_STEM4_STEM3, cand.stem3Hash, nextStem4Hash);
      if (nextAscii.length() >= 3) {
        long nextStem5 = NimbleHash.hashPrefix(nextAscii, 5);
        long nextSuf3 = NimbleHash.hashSuffix(nextAscii, 3);
        out[m++] = NimbleHash.of(FEAT_NEXT_STEM5_SURFACE, cand.surfaceHash, nextStem5);
        out[m++] = NimbleHash.of(FEAT_NEXT_STEM5_STEM4, cand.stem4Hash, nextStem5);
        out[m++] = NimbleHash.of(FEAT_NEXT_SUF3_STEM4, cand.stem4Hash, nextSuf3);
      }
    }

    if (prevAsciiHash != 0L && nextAsciiHash != 0L) {
      out[m++] =
          NimbleHash.of(
              FEAT_SANDWICH_ASCII_SURFACE, prevAsciiHash, cand.surfaceHash, nextAsciiHash);
      out[m++] =
          NimbleHash.of(FEAT_SANDWICH_STEM4, prevStem4Hash, cand.stem4Hash, nextStem4Hash);
    }

    int wStart = Math.max(0, tokenIndex - 4);
    int wEnd = Math.min(n - 1, tokenIndex + 4);
    for (int j = wStart; j <= wEnd; j++) {
      if (j >= tokenIndex - 1 && j <= tokenIndex + 1) {
        continue;
      }
      String ctxAscii = tokens.get(j).asciiLower();
      if (!NimbleCandidate.isStopword(ctxAscii)) {
        long dir = j < tokenIndex ? 1L : 2L;
        out[m++] =
            NimbleHash.of(
                FEAT_LOCAL_CONTENT_ASCII,
                dir,
                NimbleHash.hashPrefix(ctxAscii, 5),
                cand.stem4Hash);
        out[m++] =
            NimbleHash.of(
                FEAT_LOCAL_CONTENT_STEM3,
                dir,
                NimbleHash.hashPrefix(ctxAscii, 4),
                cand.stem3Hash);
      }
    }
    return m;
  }

  static float scoreTransition(
      NimbleCandidate prev2,
      NimbleCandidate prev1,
      NimbleCandidate cur,
      NimbleWeightTable weights,
      long[] featBuf) {
    int m = extractTransitionFeatures(prev2, prev1, cur, featBuf);
    return weights.sum(featBuf, m);
  }

  static float scoreTrigramOnly(
      NimbleCandidate prev2,
      NimbleCandidate prev1,
      NimbleCandidate cur,
      NimbleWeightTable weights,
      long[] featBuf) {
    featBuf[0] =
        NimbleHash.of(FEAT_TRIGRAM_STEM4, prev2.stem4Hash, prev1.stem4Hash, cur.stem4Hash);
    return weights.sum(featBuf, 1);
  }

  static int extractTransitionFeatures(
      NimbleCandidate prev2, NimbleCandidate prev1, NimbleCandidate cur, long[] out) {
    int m = 0;
    out[m++] = NimbleHash.of(FEAT_BIGRAM_SURFACE, prev1.surfaceHash, cur.surfaceHash);
    out[m++] = NimbleHash.of(FEAT_BIGRAM_STEM4, prev1.stem4Hash, cur.stem4Hash);
    out[m++] = NimbleHash.of(FEAT_BIGRAM_STEM3, prev1.stem3Hash, cur.stem3Hash);
    out[m++] = NimbleHash.of(FEAT_PREV_SUF2_CUR_STEM4, prev1.suf2Hash, cur.stem4Hash);
    out[m++] = NimbleHash.of(FEAT_PREV_HARMONY_SURFACE, prev1.lastVowelHarmony, cur.surfaceHash);
    out[m++] =
        NimbleHash.of(FEAT_TRIGRAM_STEM4, prev2.stem4Hash, prev1.stem4Hash, cur.stem4Hash);
    return m;
  }

  static float scoreCandidateInSequence(
      int tokenIndex,
      NimbleCandidate[] sequence,
      int seqLen,
      NimbleCandidate cand,
      float unaryScore,
      NimbleWeightTable weights,
      long[] featBuf) {
    NimbleCandidate prev2 = tokenIndex >= 2 ? sequence[tokenIndex - 2] : NimbleCandidate.BOUNDARY;
    NimbleCandidate prev1 = tokenIndex >= 1 ? sequence[tokenIndex - 1] : NimbleCandidate.BOUNDARY;
    float score = unaryScore + scoreTransition(prev2, prev1, cand, weights, featBuf);
    if (tokenIndex + 1 < seqLen && sequence[tokenIndex + 1] != null) {
      NimbleCandidate next1 = sequence[tokenIndex + 1];
      score += scoreTransition(prev1, cand, next1, weights, featBuf);
      if (tokenIndex + 2 < seqLen && sequence[tokenIndex + 2] != null) {
        score += scoreTrigramOnly(cand, next1, sequence[tokenIndex + 2], weights, featBuf);
      }
    }
    return score + scoreWideContext(tokenIndex, sequence, seqLen, cand, weights, featBuf);
  }

  static float scoreWideContext(
      int tokenIndex,
      NimbleCandidate[] sequence,
      int seqLen,
      NimbleCandidate cand,
      NimbleWeightTable weights,
      long[] featBuf) {
    int m = extractWideContextFeatures(tokenIndex, sequence, seqLen, cand, featBuf);
    return weights.sum(featBuf, m);
  }

  static int extractWideContextFeatures(
      int tokenIndex, NimbleCandidate[] sequence, int seqLen, NimbleCandidate cand, long[] out) {
    int m = 0;
    int start = Math.max(0, tokenIndex - 6);
    int end = Math.min(seqLen - 1, tokenIndex + 6);
    for (int j = start; j <= end; j++) {
      int dist = j - tokenIndex;
      if (dist >= -1 && dist <= 1) {
        continue;
      }
      NimbleCandidate ctx = sequence[j];
      if (ctx == null || !ctx.contentWord) {
        continue;
      }
      out[m++] = NimbleHash.of(FEAT_WIDE_CONTENT_STEM4, cand.stem4Hash, ctx.stem4Hash);
      out[m++] = NimbleHash.of(FEAT_WIDE_CONTENT_STEM3, cand.stem3Hash, ctx.stem4Hash);
      if (dist >= -4 && dist <= 4) {
        out[m++] =
            NimbleHash.of(
                FEAT_WIDE_DIRECTIONAL_STEM4,
                cand.stem4Hash,
                ctx.stem4Hash,
                dist < 0 ? 1L : 2L);
      }
    }
    return m;
  }
}
