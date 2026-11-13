package zemberek.normalization.deasciifier.latticedeasciifier;

import java.util.List;
import java.util.Random;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier.TokenLattice;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * End-to-end trainer for {@link LatticeDeasciifier}.
 *
 * <p>Reuses {@link LatticeDeasciifier} and {@link SentenceLatticeScorer} directly during online
 * Margin-Perceptron updates so training and inference share identical lattices and features.
 */
public final class LatticeDeasciifierTrainer {

  /** Training hyperparameters and model-capacity ceilings. */
  public static final class Config {
    public int maxConfusions = 36_000;
    public int maxProperConfusions = 8_000;
    public int maxForeignKeep = 2_500;
    public int maxLemmaPriors = 40_000;
    public int charEpochs = 3;
    public int charMaxFeatures = 150_000;
    public double typoAugmentRate = 0.14;
    public int sentenceEpochs = 4;
    public int sentenceMaxFeatures = 750_000;
    public float margin = 1.25f;
    public float learningRate = 0.08f;
    public long randomSeed = 1337L;
  }

  private LatticeDeasciifierTrainer() {}

  public static DeasciifierModel train(List<String> cleanSentences, Config config) {
    return train(cleanSentences, new MorphologicalCandidateGenerator(), config);
  }

  public static DeasciifierModel train(
      List<String> cleanSentences, TurkishMorphology morphology, Config config) {
    return train(cleanSentences, new MorphologicalCandidateGenerator(morphology), config);
  }

  public static DeasciifierModel train(
      List<String> cleanSentences,
      MorphologicalCandidateGenerator morphGenerator,
      Config config) {
    return train(cleanSentences, null, cleanSentences, null, morphGenerator, config);
  }

  /**
   * Trains a complete {@link DeasciifierModel}:
   * <ol>
   *   <li>Mines {@link LexiconPriors} from {@code lexiconCorpus}
   *   <li>Trains {@link CharacterHarmonyModel} on {@code trainingSentences}
   *   <li>Trains sentence Margin-Perceptron weights on {@code trainingSentences}
   * </ol>
   */
  public static DeasciifierModel train(
      List<String> lexiconCorpus,
      boolean[] lexiconCircumflexIntact,
      List<String> trainingSentences,
      boolean[] trainCircumflexIntact,
      MorphologicalCandidateGenerator morphGenerator,
      Config config) {
    LexiconPriors priors =
        LexiconPriors.mineFromCorpus(
            lexiconCorpus,
            lexiconCircumflexIntact,
            morphGenerator,
            config.maxConfusions,
            config.maxProperConfusions,
            config.maxForeignKeep,
            config.maxLemmaPriors);

    CharacterHarmonyModel charModel =
        CharacterHarmonyModel.train(
            trainingSentences,
            trainCircumflexIntact,
            priors,
            config.charEpochs,
            config.charMaxFeatures,
            config.typoAugmentRate,
            config.randomSeed);

    QuantizedWeightTable sentenceWeights =
        trainSentencePerceptron(
            trainingSentences, trainCircumflexIntact, priors, charModel, morphGenerator, config);

    return new DeasciifierModel(priors, charModel, sentenceWeights);
  }

  private static QuantizedWeightTable trainSentencePerceptron(
      List<String> cleanSentences,
      boolean[] circumflexIntact,
      LexiconPriors priors,
      CharacterHarmonyModel charModel,
      MorphologicalCandidateGenerator morphGenerator,
      Config cfg) {
    AveragedPerceptronWeights onlineWeights = new AveragedPerceptronWeights(1 << 23);
    LatticeDeasciifier latticeBuilder =
        new LatticeDeasciifier(
            new DeasciifierModel(priors, charModel, QuantizedWeightTable.empty()),
            morphGenerator,
            LatticeDeasciifier.DEFAULT_BEAM_WIDTH);

    Random rng = new Random(cfg.randomSeed + 77L);
    int nSent = cleanSentences.size();
    int[] order = new int[nSent];
    for (int i = 0; i < nSent; i++) {
      order[i] = i;
    }

    long[] featBuf = new long[SentenceLatticeScorer.FEATURE_BUFFER_SIZE];

    for (int epoch = 0; epoch < cfg.sentenceEpochs; epoch++) {
      CharacterHarmonyModel.shuffle(order, rng);
      for (int sentIdx : order) {
        String gold = cleanSentences.get(sentIdx);
        if (gold.length() < 4) {
          continue;
        }
        boolean circOk = circumflexIntact == null || circumflexIntact[sentIdx];
        String input = CharacterHarmonyModel.createStochasticAsciiInput(gold, rng);
        if (input.length() != gold.length()) {
          continue;
        }

        List<Token> tokens = TurkishTextCodec.tokenize(input);
        int n = tokens.size();
        if (n == 0) {
          continue;
        }

        TokenLattice[] lattice = latticeBuilder.buildSentenceLattice(input, tokens);
        if (!LatticeDeasciifier.hasAmbiguousToken(lattice)) {
          continue;
        }

        Candidate[] goldSeq = new Candidate[n];
        for (int i = 0; i < n; i++) {
          List<Candidate> cands = lattice[i].candidates;
          if (cands.size() == 1) {
            goldSeq[i] = cands.getFirst();
            continue;
          }
          Token tok = tokens.get(i);
          String goldLower =
              TurkishTextCodec.toTurkishLower(gold.substring(tok.start(), tok.end()));
          goldSeq[i] = findMatchingCandidate(cands, goldLower, circOk);
        }

        latticeBuilder.computeAmbiguousUnaryScores(tokens, lattice, onlineWeights, featBuf);
        Candidate[] predicted =
            LatticeDeasciifier.runPass1BeamSearch(
                lattice, onlineWeights, LatticeDeasciifier.DEFAULT_BEAM_WIDTH, featBuf);

        char sentEndPunct = tokens.getLast().rightPunct();
        boolean sentCapitalized = tokens.getFirst().casing() == Casing.TITLE;

        for (int i = 0; i < n; i++) {
          TokenLattice col = lattice[i];
          if (!col.isAmbiguous()) {
            continue;
          }
          if (!circOk
              && (priors.hasCircumflexReading(tokens.get(i).asciiLower())
                  || hasCircumflexSurfaceCollision(col.candidates))) {
            continue;
          }
          Candidate goldCand = goldSeq[i];
          if (goldCand == null) {
            continue;
          }

          Candidate prev2 = i >= 2 ? predicted[i - 2] : Candidate.BOUNDARY;
          Candidate prev1 = i >= 1 ? predicted[i - 1] : Candidate.BOUNDARY;
          Candidate next1 = i + 1 < n ? predicted[i + 1] : null;
          Candidate next2 = i + 2 < n ? predicted[i + 2] : null;

          int bestViolatorIdx = -1;
          float bestViolatorScore = Float.NEGATIVE_INFINITY;
          int goldIdx = -1;
          List<Candidate> cands = col.candidates;

          for (int ci = 0; ci < cands.size(); ci++) {
            Candidate cand = cands.get(ci);
            if (cand == goldCand) {
              goldIdx = ci;
            }
            float score =
                SentenceLatticeScorer.scoreCandidateInSequence(
                    i, predicted, n, cand, col.unaryScores[ci], onlineWeights, featBuf);

            float loss =
                cand == goldCand
                    ? 0.0f
                    : (priors.isTabooSurface(cand.surface) ? 2.5f : cfg.margin);
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
                prev2,
                prev1,
                next1,
                next2,
                predicted,
                n,
                goldCand,
                col.charRelativeScores[goldIdx],
                +cfg.learningRate,
                onlineWeights,
                featBuf);
            applyCandidateUpdate(
                i,
                tokens,
                sentEndPunct,
                sentCapitalized,
                prev2,
                prev1,
                next1,
                next2,
                predicted,
                n,
                cands.get(bestViolatorIdx),
                col.charRelativeScores[bestViolatorIdx],
                -cfg.learningRate,
                onlineWeights,
                featBuf);
          }
        }
      }
    }
    return onlineWeights.buildQuantizedAveraged(cfg.sentenceMaxFeatures);
  }

  private static void applyCandidateUpdate(
      int tokenIndex,
      List<Token> tokens,
      char sentEndPunct,
      boolean sentCapitalized,
      Candidate prev2,
      Candidate prev1,
      Candidate next1,
      Candidate next2,
      Candidate[] contextSeq,
      int seqLen,
      Candidate target,
      float charRelativeScore,
      float delta,
      AveragedPerceptronWeights weights,
      long[] featBuf) {
    int m =
        SentenceLatticeScorer.extractUnaryFeatures(
            tokenIndex, tokens, sentEndPunct, sentCapitalized, target, charRelativeScore, featBuf);
    weights.addBatch(featBuf, m, delta);

    m = SentenceLatticeScorer.extractTransitionFeatures(prev2, prev1, target, featBuf);
    weights.addBatch(featBuf, m, delta);

    if (next1 != null) {
      m = SentenceLatticeScorer.extractTransitionFeatures(prev1, target, next1, featBuf);
      weights.addBatch(featBuf, m, delta);
      if (next2 != null) {
        m = SentenceLatticeScorer.extractTrigramFeatures(target, next1, next2, featBuf);
        weights.addBatch(featBuf, m, delta);
      }
    }

    m =
        SentenceLatticeScorer.extractWideContextFeatures(
            tokenIndex, contextSeq, seqLen, target, featBuf);
    weights.addBatch(featBuf, m, delta);
  }

  private static Candidate findMatchingCandidate(
      List<Candidate> cands, String goldLower, boolean circOk) {
    for (Candidate c : cands) {
      if (c.surface.equals(goldLower)) {
        return c;
      }
    }
    if (!circOk) {
      String stripped = TurkishTextCodec.stripCircumflex(goldLower);
      for (Candidate c : cands) {
        if (TurkishTextCodec.stripCircumflex(c.surface).equals(stripped)) {
          return c;
        }
      }
    }
    return null;
  }

  private static boolean hasCircumflexSurfaceCollision(List<Candidate> cands) {
    int k = cands.size();
    for (int i = 0; i < k; i++) {
      String strippedI = TurkishTextCodec.stripCircumflex(cands.get(i).surface);
      for (int j = i + 1; j < k; j++) {
        if (strippedI.equals(TurkishTextCodec.stripCircumflex(cands.get(j).surface))) {
          return true;
        }
      }
    }
    return false;
  }
}
