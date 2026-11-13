package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.Token;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleDeasciifier.CircumflexMode;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleDeasciifier.TokenColumn;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleLexicon.CorpusBuckets;

/**
 * Self-supervised trainer for {@link NimbleDeasciifier} producing a compact {@link NimbleModel}.
 *
 * <p>Operates cleanly without runtime morphology dependencies; an optional offline {@link
 * Predicate} may be supplied to filter corpus mining noise against a reference dictionary, or
 * omitted ({@code null}) for 100% dictionary-free corpus training.
 */
public final class NimbleDeasciifierTrainer {

  public static final class Config {
    public int charEpochs = 3;
    public int charMaxFeatures = 70_000;
    public float charLearningRate = 0.15f;
    public double charTypoAugmentRate = 0.10;

    public int maxLowerConfusions = 6_500;
    public int maxSingleResiduals = 12_500;
    public int maxProperConfusions = 4_000;
    public int maxForeignKeep = 750;

    public int sentenceEpochs = 4;
    public int sentenceMaxFeatures = 94_000;
    public float margin = 1.25f;
    public float learningRate = 0.20f;
    public long randomSeed = 20261113L;
  }

  private NimbleDeasciifierTrainer() {}

  public static NimbleModel train(
      List<String> cleanSentences, Predicate<String> knownValidSurface, Config cfg) {
    return train(cleanSentences, null, cleanSentences, null, knownValidSurface, cfg);
  }

  public static NimbleModel train(
      List<String> lexiconSentences,
      boolean[] lexiconCircumflexIntact,
      List<String> trainSentences,
      boolean[] trainCircumflexIntact,
      Predicate<String> knownValidSurface,
      Config cfg) {
    Config c = cfg != null ? cfg : new Config();

    long t0 = System.currentTimeMillis();
    CorpusBuckets buckets =
        NimbleLexicon.collectBuckets(
            lexiconSentences, lexiconCircumflexIntact, knownValidSurface, c.maxForeignKeep);

    NimbleCharModel charModel =
        NimbleCharModel.train(
            trainSentences,
            buckets.foreignAndSeedSet(),
            c.charEpochs,
            c.charMaxFeatures,
            c.charLearningRate,
            c.charTypoAugmentRate,
            c.randomSeed);
    long t1 = System.currentTimeMillis();
    System.out.printf(
        "  [NimbleCharModel] trained %,d features in %.2fs (RAM=%.2f MB)%n",
        charModel.featureCount(),
        (t1 - t0) / 1000.0,
        charModel.ramBytesUsed() / (1024.0 * 1024.0));

    NimbleLexicon lexicon =
        NimbleLexicon.buildFromBuckets(
            buckets,
            charModel,
            c.maxLowerConfusions,
            c.maxSingleResiduals,
            c.maxProperConfusions);
    long t2 = System.currentTimeMillis();
    System.out.printf(
        "  [NimbleLexicon] mined lowerConf=%,d | residualSingles=%,d | proper=%,d in %.2fs%n",
        lexicon.lowerConfusionCount(),
        lexicon.residualSingleCount(),
        lexicon.properConfusionCount(),
        (t2 - t1) / 1000.0);

    NimbleFp16WeightTable sentenceWeights =
        trainSentencePerceptron(trainSentences, trainCircumflexIntact, lexicon, charModel, c);
    long t3 = System.currentTimeMillis();
    System.out.printf(
        "  [NimbleSentencePerceptron] trained %,d FP16 features in %.2fs (RAM=%.2f MB)%n",
        sentenceWeights.size(),
        (t3 - t2) / 1000.0,
        sentenceWeights.ramBytesUsed() / (1024.0 * 1024.0));

    return new NimbleModel(lexicon, charModel, sentenceWeights);
  }

  private static NimbleFp16WeightTable trainSentencePerceptron(
      List<String> cleanSentences,
      boolean[] circumflexIntact,
      NimbleLexicon lexicon,
      NimbleCharModel charModel,
      Config cfg) {
    NimbleOnlineWeights onlineWeights = new NimbleOnlineWeights(1 << 21);
    NimbleDeasciifier onlineDecoder =
        new NimbleDeasciifier(
            lexicon,
            charModel,
            onlineWeights,
            NimbleDeasciifier.DEFAULT_BEAM_WIDTH,
            CircumflexMode.FULL);

    Random rng = new Random(cfg.randomSeed + 77L);
    int nSent = cleanSentences.size();
    int[] order = new int[nSent];
    for (int i = 0; i < nSent; i++) {
      order[i] = i;
    }

    long[] featBuf = new long[NimbleContextScorer.FEATURE_BUFFER_SIZE];

    for (int epoch = 0; epoch < cfg.sentenceEpochs; epoch++) {
      shuffle(order, rng);
      for (int sentIdx : order) {
        String gold = cleanSentences.get(sentIdx);
        if (gold.length() < 4) {
          continue;
        }
        boolean circOk = circumflexIntact == null || circumflexIntact[sentIdx];
        String input = NimbleCharModel.createStochasticAsciiInput(gold, rng);
        if (input.length() != gold.length()) {
          continue;
        }

        List<Token> tokens = TurkishTextCodec.tokenize(input);
        int n = tokens.size();
        if (n == 0) {
          continue;
        }

        TokenColumn[] columns = onlineDecoder.buildSentenceColumns(input, tokens, true);
        if (!NimbleDeasciifier.hasAmbiguousColumn(columns)) {
          continue;
        }

        NimbleCandidate[] goldSeq = new NimbleCandidate[n];
        for (int i = 0; i < n; i++) {
          NimbleCandidate[] cands = columns[i].candidates;
          if (cands.length == 1) {
            goldSeq[i] = cands[0];
            continue;
          }
          Token tok = tokens.get(i);
          String goldLower =
              TurkishTextCodec.toTurkishLower(gold.substring(tok.start(), tok.end()));
          goldSeq[i] = findMatchingCandidate(cands, goldLower, circOk);
        }

        onlineDecoder.computeUnaryScores(tokens, columns, featBuf);
        NimbleCandidate[] predicted = onlineDecoder.decodeBeamPass1(tokens, columns, true, featBuf);

        char sentEndPunct = tokens.getLast().rightPunct();
        boolean sentCapitalized = NimbleDeasciifier.isSentenceMajorityCapitalized(tokens);

        for (int i = 0; i < n; i++) {
          TokenColumn col = columns[i];
          if (!col.isAmbiguous()) {
            continue;
          }
          if (!circOk && hasSameStrippedCircumflexPair(col.candidates)) {
            continue;
          }
          NimbleCandidate goldCand = goldSeq[i];
          if (goldCand == null) {
            continue;
          }

          int bestViolatorIdx = -1;
          float bestViolatorScore = Float.NEGATIVE_INFINITY;
          int goldIdx = -1;
          NimbleCandidate[] cands = col.candidates;

          for (int ci = 0; ci < cands.length; ci++) {
            NimbleCandidate cand = cands[ci];
            if (cand == goldCand) {
              goldIdx = ci;
            }
            float score =
                NimbleContextScorer.scoreCandidateInSequence(
                    i, predicted, n, cand, col.unaryScores[ci], onlineWeights, featBuf);
            float loss =
                cand == goldCand
                    ? 0.0f
                    : (lexicon.isTabooSurface(cand.surface) ? 2.5f : cfg.margin);
            if (score + loss > bestViolatorScore) {
              bestViolatorScore = score + loss;
              bestViolatorIdx = ci;
            }
          }

          onlineWeights.advanceStep();
          if (goldIdx >= 0 && bestViolatorIdx >= 0 && bestViolatorIdx != goldIdx) {
            applyCandidateUpdate(
                i,
                tokens,
                sentEndPunct,
                sentCapitalized,
                predicted,
                n,
                goldCand,
                +cfg.learningRate,
                onlineWeights,
                featBuf);
            applyCandidateUpdate(
                i,
                tokens,
                sentEndPunct,
                sentCapitalized,
                predicted,
                n,
                cands[bestViolatorIdx],
                -cfg.learningRate,
                onlineWeights,
                featBuf);
          }
        }
      }
    }
    return onlineWeights.buildFp16Averaged(cfg.sentenceMaxFeatures, 2);
  }

  private static void applyCandidateUpdate(
      int tokenIndex,
      List<Token> tokens,
      char sentEndPunct,
      boolean sentCapitalized,
      NimbleCandidate[] predicted,
      int seqLen,
      NimbleCandidate target,
      float delta,
      NimbleOnlineWeights weights,
      long[] featBuf) {
    NimbleCandidate prev2 = tokenIndex >= 2 ? predicted[tokenIndex - 2] : NimbleCandidate.BOUNDARY;
    NimbleCandidate prev1 = tokenIndex >= 1 ? predicted[tokenIndex - 1] : NimbleCandidate.BOUNDARY;

    int um =
        NimbleContextScorer.extractUnaryFeatures(
            tokenIndex, tokens, sentEndPunct, sentCapitalized, target, featBuf);
    weights.addBatch(featBuf, um, delta);

    int tmLeft = NimbleContextScorer.extractTransitionFeatures(prev2, prev1, target, featBuf);
    weights.addBatch(featBuf, tmLeft, delta);

    if (tokenIndex + 1 < seqLen && predicted[tokenIndex + 1] != null) {
      NimbleCandidate next1 = predicted[tokenIndex + 1];
      int tmRight = NimbleContextScorer.extractTransitionFeatures(prev1, target, next1, featBuf);
      weights.addBatch(featBuf, tmRight, delta);

      if (tokenIndex + 2 < seqLen && predicted[tokenIndex + 2] != null) {
        featBuf[0] =
            NimbleHash.of(
                524,
                target.stem4Hash,
                next1.stem4Hash,
                predicted[tokenIndex + 2].stem4Hash);
        weights.add(featBuf[0], delta);
      }
    }

    int wm =
        NimbleContextScorer.extractWideContextFeatures(
            tokenIndex, predicted, seqLen, target, featBuf);
    weights.addBatch(featBuf, wm, delta);
  }

  private static NimbleCandidate findMatchingCandidate(
      NimbleCandidate[] candidates, String goldLower, boolean circOk) {
    for (NimbleCandidate c : candidates) {
      if (c.surface.equals(goldLower)) {
        return c;
      }
    }
    if (!circOk) {
      String goldNoCirc = TurkishTextCodec.stripCircumflex(goldLower);
      for (NimbleCandidate c : candidates) {
        if (TurkishTextCodec.stripCircumflex(c.surface).equals(goldNoCirc)) {
          return c;
        }
      }
    }
    return null;
  }

  private static boolean hasSameStrippedCircumflexPair(NimbleCandidate[] candidates) {
    for (int a = 0; a < candidates.length; a++) {
      String sa = TurkishTextCodec.stripCircumflex(candidates[a].surface);
      for (int b = a + 1; b < candidates.length; b++) {
        if (sa.equals(TurkishTextCodec.stripCircumflex(candidates[b].surface))) {
          return true;
        }
      }
    }
    return false;
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
