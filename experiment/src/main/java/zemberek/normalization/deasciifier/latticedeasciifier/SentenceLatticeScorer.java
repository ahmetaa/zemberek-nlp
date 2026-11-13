package zemberek.normalization.deasciifier.latticedeasciifier;

import java.util.List;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * Single source of truth for sentence-lattice feature extraction and candidate scoring.
 *
 * <p>Operates over any {@link WeightTable} so online Perceptron training and quantized runtime
 * decoding execute identical feature templates and heuristics.
 */
final class SentenceLatticeScorer {

  static final int FEATURE_BUFFER_SIZE = 48;

  // Unary & ASCII-lookahead feature namespaces:
  private static final int FEAT_UNIGRAM_SURFACE = 501;
  private static final int FEAT_UNIGRAM_LEMMA = 502;
  private static final int FEAT_RIGHT_PUNCT_LEMMA = 503;
  private static final int FEAT_RIGHT_PUNCT_MORPH = 504;
  private static final int FEAT_SENT_END_PUNCT_MORPH = 505;
  private static final int FEAT_CASING_PROPER_POS = 506;
  private static final int FEAT_CHAR_SCORE_BUCKET = 507;

  private static final int FEAT_PREV_SUFFIX_LEMMA = 510;
  private static final int FEAT_NEXT_SUFFIX_LEMMA = 511;
  private static final int FEAT_PREV_ASCII_LEMMA = 512;
  private static final int FEAT_PREV_ASCII_SURFACE = 513;
  private static final int FEAT_PREV_PREFIX_LEMMA = 514;
  private static final int FEAT_NEXT_ASCII_LEMMA = 515;
  private static final int FEAT_NEXT_ASCII_SURFACE = 516;
  private static final int FEAT_NEXT_PREFIX_LEMMA = 517;
  private static final int FEAT_SANDWICH_ASCII_SURFACE = 518;
  private static final int FEAT_WIDE_ASCII_STEM_LEMMA = 519;

  // Decoded transition feature namespaces:
  private static final int FEAT_BIGRAM_SURFACE = 520;
  private static final int FEAT_BIGRAM_LEMMA = 521;
  private static final int FEAT_PREV_MORPH_CUR_LEMMA = 522;
  private static final int FEAT_PREV_LEMMA_CUR_MORPH = 523;
  private static final int FEAT_TRIGRAM_LEMMA = 524;
  private static final int FEAT_TRIGRAM_MORPH_LEMMA = 525;
  private static final int FEAT_PREV_CASE_CUR_LEMMA = 526;
  private static final int FEAT_PREV_POS_CUR_POS = 527;
  private static final int FEAT_PREV_HARMONY_SURFACE = 528;
  private static final int FEAT_PREV_HARMONY_QUESTION = 529;
  private static final int FEAT_PREV_POS_NUM_CUR_NUM = 530;

  // Pass-2 wide content-lemma feature namespaces:
  private static final int FEAT_WIDE_CONTENT_LEMMA = 541;
  private static final int FEAT_WIDE_DIRECTIONAL_LEMMA = 542;

  private SentenceLatticeScorer() {}

  static float scoreUnary(
      int tokenIndex,
      List<Token> tokens,
      char sentenceEndPunct,
      boolean sentenceCapitalized,
      Candidate cand,
      float empiricalSurfaceLogProb,
      float charRelativeScore,
      LexiconPriors priors,
      WeightTable weights,
      long[] featBuf) {
    float deterministicBase =
        computeDeterministicPrior(
            tokenIndex,
            tokens,
            sentenceCapitalized,
            cand,
            empiricalSurfaceLogProb,
            charRelativeScore,
            priors);
    int m =
        extractUnaryFeatures(
            tokenIndex,
            tokens,
            sentenceEndPunct,
            sentenceCapitalized,
            cand,
            charRelativeScore,
            featBuf);
    return deterministicBase + weights.sum(featBuf, m);
  }

  static float computeDeterministicPrior(
      int tokenIndex,
      List<Token> tokens,
      boolean sentenceCapitalized,
      Candidate cand,
      float empiricalSurfaceLogProb,
      float charRelativeScore,
      LexiconPriors priors) {
    Token token = tokens.get(tokenIndex);
    float s = 0.75f * empiricalSurfaceLogProb + 0.25f * charRelativeScore;

    if (!cand.oovFallback && empiricalSurfaceLogProb == 0.0f && priors.lemmaPriorSize() > 0) {
      float lemmaLogCount = priors.getLemmaLogCount(cand);
      s += (lemmaLogCount > 0.0f) ? 0.14f * (lemmaLogCount - 4.0f) : -0.28f;
    }

    if (cand.oovFallback) {
      s -= 1.60f;
    } else {
      s -= 0.18f * Math.max(0, cand.morphemeGroupCount - 1);
      if (cand.informal) {
        s -= 0.35f;
      }
      if (cand.secondaryPos == SecondaryPos.Abbreviation && token.casing() != Casing.UPPER) {
        s -= 2.2f;
      }
      if (token.casing() == Casing.TITLE && token.hasApostrophe()) {
        s += (cand.secondaryPos == SecondaryPos.ProperNoun) ? 1.25f : -0.35f;
      } else if (!token.sentenceStart() && token.casing() == Casing.TITLE) {
        boolean bareProperNoun =
            cand.secondaryPos == SecondaryPos.ProperNoun
                && "Nom".equals(cand.caseMorpheme)
                && "Pnon".equals(cand.possessiveMorpheme);
        if (bareProperNoun) {
          s += 0.65f;
        }
      } else if (token.casing() == Casing.LOWER && cand.secondaryPos == SecondaryPos.ProperNoun) {
        s -= (sentenceCapitalized && !token.sentenceStart()) ? 1.25f : 0.45f;
      }

      if (cand.pos == PrimaryPos.Duplicator) {
        boolean reduplicated =
            (tokenIndex > 0 && tokens.get(tokenIndex - 1).asciiLower().equals(token.asciiLower()))
                || (tokenIndex + 1 < tokens.size()
                    && tokens.get(tokenIndex + 1).asciiLower().equals(token.asciiLower()));
        if (reduplicated) {
          s += 1.6f;
        }
      }

      if (cand.pos == PrimaryPos.Noun
          && cand.lastGroupPos == PrimaryPos.Verb
          && !"Pnon".equals(cand.possessiveMorpheme)
          && (cand.surface.endsWith("sın")
              || cand.surface.endsWith("sin")
              || cand.surface.endsWith("sun")
              || cand.surface.endsWith("sün"))) {
        s -= 1.6f;
      }
    }

    if (priors.isTabooSurface(cand.surface) && !token.original().equals(cand.surface)) {
      s -= 4.5f;
    }
    return s;
  }

  static int extractUnaryFeatures(
      int tokenIndex,
      List<Token> tokens,
      char sentenceEndPunct,
      boolean sentenceCapitalized,
      Candidate cand,
      float charRelativeScore,
      long[] out) {
    Token token = tokens.get(tokenIndex);
    int n = tokens.size();
    int m = 0;

    out[m++] = FeatureHash.of(FEAT_UNIGRAM_SURFACE, cand.surfaceHash);
    out[m++] = FeatureHash.of(FEAT_UNIGRAM_LEMMA, cand.lemmaHash);

    if (token.rightPunct() != ' ') {
      out[m++] = FeatureHash.of(FEAT_RIGHT_PUNCT_LEMMA, cand.lemmaHash, token.rightPunct());
      out[m++] = FeatureHash.of(FEAT_RIGHT_PUNCT_MORPH, cand.morphTagHash, token.rightPunct());
    }
    if (sentenceEndPunct == '?' || sentenceEndPunct == '!') {
      out[m++] = FeatureHash.of(FEAT_SENT_END_PUNCT_MORPH, cand.morphTagHash, sentenceEndPunct);
    }

    long shapeKey =
        ((long) token.casing().ordinal() << 2)
            | (token.sentenceStart() ? 2L : 0L)
            | (sentenceCapitalized ? 1L : 0L);
    out[m++] =
        FeatureHash.of(
            FEAT_CASING_PROPER_POS,
            shapeKey,
            cand.secondaryPos == SecondaryPos.ProperNoun ? 1L : 0L,
            cand.pos.ordinal());

    int charBucket = Math.clamp(Math.round(charRelativeScore), -6, 0);
    out[m++] = FeatureHash.of(FEAT_CHAR_SCORE_BUCKET, charBucket, cand.oovFallback ? 1L : 0L);

    long prevAsciiHash = 0L;
    if (tokenIndex > 0) {
      String prevAscii = tokens.get(tokenIndex - 1).asciiLower();
      prevAsciiHash = FeatureHash.hashString(prevAscii);
      out[m++] = FeatureHash.of(FEAT_PREV_ASCII_LEMMA, prevAsciiHash, cand.lemmaHash);
      out[m++] = FeatureHash.of(FEAT_PREV_ASCII_SURFACE, prevAsciiHash, cand.surfaceHash);
      if (prevAscii.length() >= 3) {
        out[m++] =
            FeatureHash.of(
                FEAT_PREV_PREFIX_LEMMA, FeatureHash.hashPrefix(prevAscii, 5), cand.lemmaHash);
        out[m++] =
            FeatureHash.of(
                FEAT_PREV_SUFFIX_LEMMA, FeatureHash.hashSuffix(prevAscii, 3), cand.lemmaHash);
      }
    }

    long nextAsciiHash = 0L;
    if (tokenIndex + 1 < n) {
      String nextAscii = tokens.get(tokenIndex + 1).asciiLower();
      nextAsciiHash = FeatureHash.hashString(nextAscii);
      out[m++] = FeatureHash.of(FEAT_NEXT_ASCII_LEMMA, cand.lemmaHash, nextAsciiHash);
      out[m++] = FeatureHash.of(FEAT_NEXT_ASCII_SURFACE, cand.surfaceHash, nextAsciiHash);
      if (nextAscii.length() >= 3) {
        out[m++] =
            FeatureHash.of(
                FEAT_NEXT_PREFIX_LEMMA, cand.lemmaHash, FeatureHash.hashPrefix(nextAscii, 5));
        out[m++] =
            FeatureHash.of(
                FEAT_NEXT_SUFFIX_LEMMA, cand.lemmaHash, FeatureHash.hashSuffix(nextAscii, 2));
      }
    }

    if (prevAsciiHash != 0L && nextAsciiHash != 0L) {
      out[m++] =
          FeatureHash.of(FEAT_SANDWICH_ASCII_SURFACE, prevAsciiHash, cand.surfaceHash, nextAsciiHash);
    }

    // Wide agglutinative stem context (±2..5 words): gives Pass-1 beam search immediate visibility
    // into right-hand heads and sentence topic ("sinir ... ağlarının", "kaza ... öldü").
    int wStart = Math.max(0, tokenIndex - 5);
    int wEnd = Math.min(n - 1, tokenIndex + 5);
    for (int j = wStart; j <= wEnd; j++) {
      if (j >= tokenIndex - 1 && j <= tokenIndex + 1) {
        continue;
      }
      String ctxAscii = tokens.get(j).asciiLower();
      if (ctxAscii.length() >= 4) {
        out[m++] =
            FeatureHash.of(
                FEAT_WIDE_ASCII_STEM_LEMMA, cand.lemmaHash, FeatureHash.hashPrefix(ctxAscii, 5));
      }
    }
    return m;
  }

  static float scoreTransition(
      Candidate prev2, Candidate prev1, Candidate cur, WeightTable weights, long[] featBuf) {
    int m = extractTransitionFeatures(prev2, prev1, cur, featBuf);
    return weights.sum(featBuf, m);
  }

  static float scoreTrigramOnly(
      Candidate prev2, Candidate prev1, Candidate cur, WeightTable weights, long[] featBuf) {
    int m = extractTrigramFeatures(prev2, prev1, cur, featBuf);
    return weights.sum(featBuf, m);
  }

  static int extractTrigramFeatures(
      Candidate prev2, Candidate prev1, Candidate cur, long[] out) {
    out[0] = FeatureHash.of(FEAT_TRIGRAM_LEMMA, prev2.lemmaHash, prev1.lemmaHash, cur.lemmaHash);
    out[1] =
        FeatureHash.of(
            FEAT_TRIGRAM_MORPH_LEMMA, prev2.morphTagHash, prev1.morphTagHash, cur.lemmaHash);
    return 2;
  }

  static int extractTransitionFeatures(
      Candidate prev2, Candidate prev1, Candidate cur, long[] out) {
    int m = 0;
    out[m++] = FeatureHash.of(FEAT_BIGRAM_SURFACE, prev1.surfaceHash, cur.surfaceHash);
    out[m++] = FeatureHash.of(FEAT_BIGRAM_LEMMA, prev1.lemmaHash, cur.lemmaHash);
    out[m++] = FeatureHash.of(FEAT_PREV_MORPH_CUR_LEMMA, prev1.morphTagHash, cur.lemmaHash);
    out[m++] = FeatureHash.of(FEAT_PREV_LEMMA_CUR_MORPH, prev1.lemmaHash, cur.morphTagHash);
    out[m++] = FeatureHash.of(FEAT_TRIGRAM_LEMMA, prev2.lemmaHash, prev1.lemmaHash, cur.lemmaHash);
    out[m++] =
        FeatureHash.of(
            FEAT_TRIGRAM_MORPH_LEMMA, prev2.morphTagHash, prev1.morphTagHash, cur.lemmaHash);
    out[m++] = FeatureHash.of(FEAT_PREV_CASE_CUR_LEMMA, prev1.caseHash, cur.lemmaHash);
    out[m++] =
        FeatureHash.of(FEAT_PREV_POS_CUR_POS, prev1.lastGroupPos.ordinal(), cur.pos.ordinal());
    out[m++] =
        FeatureHash.of(
            FEAT_PREV_POS_NUM_CUR_NUM,
            (prev1.pos.ordinal() << 1) | (prev1.plural ? 1 : 0),
            (cur.pos.ordinal() << 1) | (cur.plural ? 1 : 0));
    out[m++] = FeatureHash.of(FEAT_PREV_HARMONY_SURFACE, prev1.lastVowelHarmony, cur.surfaceHash);
    if (cur.question) {
      out[m++] =
          FeatureHash.of(FEAT_PREV_HARMONY_QUESTION, prev1.lastVowelHarmony, cur.surfaceHash);
    }
    return m;
  }

  /**
   * Computes the full neighborhood + wide-context score for {@code cand} at {@code tokenIndex}
   * within {@code sequence}, shared by Pass-2 refinement and online Margin-Perceptron updates.
   */
  static float scoreCandidateInSequence(
      int tokenIndex,
      Candidate[] sequence,
      int seqLen,
      Candidate cand,
      float unaryScore,
      WeightTable weights,
      long[] featBuf) {
    Candidate prev2 = tokenIndex >= 2 ? sequence[tokenIndex - 2] : Candidate.BOUNDARY;
    Candidate prev1 = tokenIndex >= 1 ? sequence[tokenIndex - 1] : Candidate.BOUNDARY;
    float score = unaryScore + scoreTransition(prev2, prev1, cand, weights, featBuf);
    if (tokenIndex + 1 < seqLen && sequence[tokenIndex + 1] != null) {
      Candidate next1 = sequence[tokenIndex + 1];
      score += scoreTransition(prev1, cand, next1, weights, featBuf);
      if (tokenIndex + 2 < seqLen && sequence[tokenIndex + 2] != null) {
        score += scoreTrigramOnly(cand, next1, sequence[tokenIndex + 2], weights, featBuf);
      }
    }
    return score + scoreWideContext(tokenIndex, sequence, seqLen, cand, weights, featBuf);
  }

  static float scoreWideContext(
      int tokenIndex,
      Candidate[] sequence,
      int seqLen,
      Candidate cand,
      WeightTable weights,
      long[] featBuf) {
    int m = extractWideContextFeatures(tokenIndex, sequence, seqLen, cand, featBuf);
    return weights.sum(featBuf, m);
  }

  static int extractWideContextFeatures(
      int tokenIndex, Candidate[] sequence, int seqLen, Candidate cand, long[] out) {
    int m = 0;
    int start = Math.max(0, tokenIndex - 6);
    int end = Math.min(seqLen - 1, tokenIndex + 6);
    for (int j = start; j <= end; j++) {
      int dist = j - tokenIndex;
      if (dist >= -1 && dist <= 1) {
        continue;
      }
      Candidate ctx = sequence[j];
      if (ctx == null || !ctx.contentWord) {
        continue;
      }
      out[m++] = FeatureHash.of(FEAT_WIDE_CONTENT_LEMMA, cand.lemmaHash, ctx.lemmaHash);
      if (dist >= -3 && dist <= 3) {
        out[m++] =
            FeatureHash.of(
                FEAT_WIDE_DIRECTIONAL_LEMMA, cand.lemmaHash, ctx.lemmaHash, dist < 0 ? -1L : 1L);
      }
    }
    return m;
  }
}
