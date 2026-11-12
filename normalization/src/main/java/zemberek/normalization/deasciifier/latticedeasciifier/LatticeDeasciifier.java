package zemberek.normalization.deasciifier.latticedeasciifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.deasciifier.latticedeasciifier.CharacterHarmonyModel.ScoredSurface;
import zemberek.normalization.deasciifier.latticedeasciifier.LexiconPriors.ConfusionSet;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * High-accuracy Turkish deasciifier based on a Morphological Candidate Lattice + Structured Margin
 * Perceptron.
 *
 * <p>Architecture overview:
 * <ol>
 *   <li><b>Stage 1 — Candidate Lattice</b>: Common plain words resolve through a bounded
 *       {@code 32,768}-entry cache; rare, inflected, partial-ASCII, and OOV tokens combine
 *       {@link MorphologicalCandidateGenerator}, {@link LexiconPriors}, and
 *       {@link CharacterHarmonyModel}.
 *   <li><b>Stage 2 — Pass-1 Trigram Beam Search</b>: Left-to-right Viterbi beam search
 *       ({@code beamWidth = 4}) with agglutinative stem lookahead resolves adjacent collocations,
 *       grammatical case/number agreement, and clitic vowel harmony.
 *   <li><b>Stage 3 — Pass-2 Bidirectional Refinement</b>: Right-to-left (head-final SOV) and
 *       left-to-right coordinate sweeps refine ambiguous tokens using wide content-word context
 *       ({@code ±2..6} lemmas).
 * </ol>
 *
 * <p>Thread-safe after construction.
 */
public final class LatticeDeasciifier {

  public static final int DEFAULT_BEAM_WIDTH = 4;
  private static final int MAX_PLAIN_TOKEN_CACHE = 32_768;
  private static final float[] SINGLE_ZERO_PRIOR = {0.0f};

  /** Lattice column for a single token in a sentence. */
  static final class TokenLattice {
    final List<Candidate> candidates;
    final float[] empiricalLogPriors;
    final float[] charRelativeScores;
    float[] unaryScores;

    TokenLattice(
        List<Candidate> candidates, float[] empiricalLogPriors, float[] charRelativeScores) {
      this.candidates = candidates;
      this.empiricalLogPriors = empiricalLogPriors;
      this.charRelativeScores = charRelativeScores;
    }

    boolean isAmbiguous() {
      return candidates.size() >= 2;
    }
  }

  private record CachedPlainToken(
      List<Candidate> baseMorph,
      List<Candidate> mergedCandidates,
      float[] empiricalLogPriors,
      boolean midSentenceTitleReady) {}

  private record BeamHyp(
      Candidate prev1, Candidate current, float score, BeamHyp previous) {}

  private final DeasciifierModel model;
  private final MorphologicalCandidateGenerator morphGenerator;
  private final int beamWidth;
  private final ConcurrentHashMap<String, CachedPlainToken> plainTokenCache =
      new ConcurrentHashMap<>(4_096);

  /**
   * Creates a self-contained {@link LatticeDeasciifier} using the bundled default model resource
   * and compact internal Zemberek caches.
   */
  public static LatticeDeasciifier createWithDefaults() {
    return new LatticeDeasciifier(DeasciifierModel.loadDefault());
  }

  /**
   * Creates a {@link LatticeDeasciifier} using the bundled default model resource and an injected
   * {@link TurkishMorphology} instance (leaving its cache untouched).
   */
  public static LatticeDeasciifier createWithDefaults(TurkishMorphology morphology) {
    return new LatticeDeasciifier(DeasciifierModel.loadDefault(), morphology);
  }

  /**
   * Creates a {@link LatticeDeasciifier} using the bundled default model resource and sharing both
   * formal and informal/diacritic-tolerant {@link TurkishMorphology} instances (such as those
   * owned by {@code TurkishSentenceNormalizer}).
   */
  public static LatticeDeasciifier createWithDefaults(
      TurkishMorphology strictMorphology, TurkishMorphology asciiTolerantInformalMorphology) {
    return new LatticeDeasciifier(
        DeasciifierModel.loadDefault(), strictMorphology, asciiTolerantInformalMorphology);
  }

  /** Creates a self-contained {@link LatticeDeasciifier} with compact internal Zemberek caches. */
  public LatticeDeasciifier(DeasciifierModel model) {
    this(model, new MorphologicalCandidateGenerator(), DEFAULT_BEAM_WIDTH);
  }

  /**
   * Creates a {@link LatticeDeasciifier} sharing an existing {@link TurkishMorphology} instance
   * without altering its cache.
   */
  public LatticeDeasciifier(DeasciifierModel model, TurkishMorphology morphology) {
    this(model, new MorphologicalCandidateGenerator(morphology), DEFAULT_BEAM_WIDTH);
  }

  /**
   * Creates a {@link LatticeDeasciifier} sharing both formal and informal/diacritic-tolerant
   * {@link TurkishMorphology} instances.
   */
  public LatticeDeasciifier(
      DeasciifierModel model,
      TurkishMorphology strictMorphology,
      TurkishMorphology asciiTolerantInformalMorphology) {
    this(
        model,
        new MorphologicalCandidateGenerator(strictMorphology, asciiTolerantInformalMorphology),
        DEFAULT_BEAM_WIDTH);
  }

  LatticeDeasciifier(
      DeasciifierModel model, MorphologicalCandidateGenerator morphGenerator, int beamWidth) {
    this.model = Objects.requireNonNull(model, "model must not be null");
    this.morphGenerator = Objects.requireNonNull(morphGenerator, "morphGenerator must not be null");
    this.beamWidth = Math.max(1, beamWidth);
  }

  public int cachedTokenCount() {
    return plainTokenCache.size();
  }

  /** Restores Turkish diacritics on {@code text} while preserving formatting and pinned chars. */
  public String deasciify(String text) {
    return deasciify(text, true);
  }

  /**
   * Restores Turkish diacritics only on tokens backed by morphological analysis, numeral/acronym
   * rules, or glued-clitic analysis, leaving OOV tokens untouched so downstream spell-checkers can
   * recover typos and preserve foreign proper nouns.
   */
  public String deasciifyKnownWords(String text) {
    return deasciify(text, false);
  }

  private String deasciify(String text, boolean allowOovFallback) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    List<Token> tokens = TurkishTextCodec.tokenize(text);
    if (tokens.isEmpty()) {
      return text;
    }

    TokenLattice[] lattice = buildSentenceLattice(text, tokens);
    Candidate[] chosen = decodeSentence(tokens, lattice, model.sentenceWeights());

    StringBuilder out = null;
    int cursor = 0;
    for (int i = 0; i < tokens.size(); i++) {
      Token tok = tokens.get(i);
      if (tok.immutable() || (!allowOovFallback && chosen[i].oovFallback)) {
        continue;
      }
      String restored = TurkishTextCodec.applyCasingAndPins(tok, chosen[i]);
      if (!restored.equals(tok.original())) {
        if (out == null) {
          out = new StringBuilder(text.length());
        }
        out.append(text, cursor, tok.start());
        out.append(restored);
        cursor = tok.end();
      }
    }
    if (out == null) {
      return text;
    }
    out.append(text, cursor, text.length());
    return out.toString();
  }

  TokenLattice[] buildSentenceLattice(String text, List<Token> tokens) {
    int n = tokens.size();
    TokenLattice[] lattice = new TokenLattice[n];
    if (!TurkishTextCodec.containsNonDotlessTurkishDiacritic(text)
        && TurkishTextCodec.containsDotlessI(text)
        && hasCorruptedDotlessIToken(tokens)) {
      // Input was lowercased with Turkish.LOCALE from ASCII ("ISTANBUL IYI MI" -> "ıstanbul ıyı mı")
      // or suffers from dotless-'ı' encoding corruption: treat 'ı' as unpinned across the sentence.
      for (int i = 0; i < n; i++) {
        Token tok = tokens.get(i);
        if (!tok.immutable() && TurkishTextCodec.containsDotlessI(tok.original())) {
          tokens.set(i, tok.withoutDotlessIPin());
        }
      }
    }

    for (int i = 0; i < n; i++) {
      Token tok = tokens.get(i);
      if (tok.immutable()) {
        Candidate keep = Candidate.oov(tok.asciiLower(), tok.original().length());
        lattice[i] = new TokenLattice(List.of(keep), SINGLE_ZERO_PRIOR, SINGLE_ZERO_PRIOR);
        continue;
      }

      if (tok.isPlainCacheableWord()) {
        CachedPlainToken cached = getOrBuildPlainToken(tok.asciiLower());
        boolean readyDirectly =
            cached.mergedCandidates != null
                && (tok.casing() == Casing.LOWER
                    || (tok.sentenceStart()
                        && (model.lexiconPriors().hasLowercaseConfusion(tok.asciiLower())
                            || !model.lexiconPriors().hasProperConfusion(tok.asciiLower())))
                    || cached.midSentenceTitleReady);
        if (readyDirectly) {
          List<Candidate> cands = cached.mergedCandidates;
          float[] charScores =
              cands.size() >= 2
                  ? model.characterModel().scoreCandidateDifferences(text, tok, cands)
                  : SINGLE_ZERO_PRIOR;
          lattice[i] = new TokenLattice(cands, cached.empiricalLogPriors, charScores);
          continue;
        }
        lattice[i] = buildGeneralTokenLattice(text, tok, cached.baseMorph);
        continue;
      }

      lattice[i] = buildGeneralTokenLattice(text, tok, null);
    }
    return lattice;
  }

  private CachedPlainToken getOrBuildPlainToken(String asciiLower) {
    CachedPlainToken existing = plainTokenCache.get(asciiLower);
    if (existing != null) {
      return existing;
    }
    CachedPlainToken built = computePlainToken(asciiLower);
    if (plainTokenCache.size() < MAX_PLAIN_TOKEN_CACHE && asciiLower.length() <= 24) {
      CachedPlainToken race = plainTokenCache.putIfAbsent(asciiLower, built);
      if (race != null) {
        return race;
      }
    }
    return built;
  }

  private CachedPlainToken computePlainToken(String asciiLower) {
    LexiconPriors priors = model.lexiconPriors();
    if (priors.isForeignAsciiKeep(asciiLower, Casing.LOWER)) {
      List<Candidate> keep = List.of(Candidate.oov(asciiLower, asciiLower.length()));
      return new CachedPlainToken(keep, keep, SINGLE_ZERO_PRIOR, false);
    }

    List<Candidate> baseMorph = morphGenerator.analyzePlainWithFallbacks(asciiLower, null);
    ConfusionSet confusion = priors.getConfusion(asciiLower, Casing.LOWER, false);

    if (baseMorph.isEmpty() && confusion == null) {
      return new CachedPlainToken(baseMorph, null, SINGLE_ZERO_PRIOR, false);
    }
    if (!baseMorph.isEmpty() && confusion == null && isObscureOnlyMatch(baseMorph)) {
      return new CachedPlainToken(baseMorph, null, SINGLE_ZERO_PRIOR, false);
    }

    List<Candidate> merged = new ArrayList<>(baseMorph.size() + 2);
    merged.addAll(baseMorph);
    if (confusion != null) {
      for (String surf : confusion.surfaces()) {
        if (surf.length() == asciiLower.length()
            && TurkishTextCodec.toAsciiLower(surf).equals(asciiLower)
            && !containsSurface(merged, surf)) {
          merged.add(Candidate.oov(surf, 0));
        }
      }
    }
    if (merged.isEmpty()) {
      return new CachedPlainToken(baseMorph, null, SINGLE_ZERO_PRIOR, false);
    }

    boolean hasProperNoun = false;
    for (Candidate c : merged) {
      if (c.secondaryPos == SecondaryPos.ProperNoun) {
        hasProperNoun = true;
        break;
      }
    }
    boolean midTitleReady =
        !priors.hasProperConfusion(asciiLower)
            && (confusion != null || hasProperNoun)
            && !isObscureOnlyMatch(merged);
    float[] logPriors = buildEmpiricalPriors(merged, confusion);
    return new CachedPlainToken(baseMorph, List.copyOf(merged), logPriors, midTitleReady);
  }

  private TokenLattice buildGeneralTokenLattice(
      String text, Token token, List<Candidate> precomputedMorph) {
    LexiconPriors priors = model.lexiconPriors();
    boolean pinDotlessI = token.hasPinnedTurkish();
    List<Candidate> candidates =
        new ArrayList<>(
            precomputedMorph != null ? precomputedMorph : morphGenerator.generate(token, priors));

    ConfusionSet confusion =
        priors.getConfusion(token.asciiLower(), token.casing(), token.sentenceStart());
    if (confusion != null) {
      for (String surf : confusion.surfaces()) {
        if (TurkishTextCodec.isCompatibleWithInput(token.original(), surf, pinDotlessI)
            && !containsSurface(candidates, surf)) {
          candidates.add(Candidate.oov(surf, 0));
        }
      }
    }

    boolean supplementWithCharModel =
        candidates.isEmpty()
            || (confusion == null && needsCharacterSupplement(token, candidates));

    if (supplementWithCharModel) {
      if (candidates.isEmpty() && !token.numeralApostrophe() && token.hasApostrophe()) {
        addUnlistedApostropheCandidates(text, token, candidates);
      }

      int maxCharCands = candidates.isEmpty() ? 2 : 1;
      for (ScoredSurface cs :
          model.characterModel().decodeTopCandidates(text, token, maxCharCands)) {
        if (TurkishTextCodec.isCompatibleWithInput(token.original(), cs.surface(), pinDotlessI)
            && !containsSurface(candidates, cs.surface())) {
          candidates.add(Candidate.oov(cs.surface(), 0));
        }
      }

      if (!token.hasApostrophe()
          && ((!token.sentenceStart() && token.casing() == Casing.TITLE)
              || token.casing() == Casing.UPPER)
          && TurkishTextCodec.isCompatibleWithInput(
              token.original(), token.asciiLower(), pinDotlessI)
          && !containsSurface(candidates, token.asciiLower())) {
        candidates.add(Candidate.oov(token.asciiLower(), token.original().length()));
      }
    }

    if (candidates.isEmpty()) {
      String fallback =
          pinDotlessI ? TurkishTextCodec.toTurkishLower(token.original()) : token.asciiLower();
      candidates.add(Candidate.oov(fallback, 0));
    }

    float[] logPriors = buildEmpiricalPriors(candidates, confusion, token, priors);
    float[] charScores =
        candidates.size() >= 2
            ? model.characterModel().scoreCandidateDifferences(text, token, candidates)
            : SINGLE_ZERO_PRIOR;
    return new TokenLattice(candidates, logPriors, charScores);
  }

  private void addUnlistedApostropheCandidates(
      String text, Token token, List<Candidate> outCandidates) {
    int apos = token.apostropheIndex();
    String stemOrig = token.original().substring(0, apos);
    boolean pinStem =
        token.hasPinnedTurkish() && TurkishTextCodec.containsPinnedTurkish(stemOrig);
    Token stemToken =
        new Token(
            token.start(),
            token.start() + apos,
            stemOrig,
            token.asciiLower().substring(0, apos),
            token.casing(),
            -1,
            false,
            false,
            token.sentenceStart(),
            '\'',
            pinStem);
    char aposChar = token.original().charAt(apos);
    String suffixAscii = token.asciiLower().substring(apos + 1);
    for (ScoredSurface stemSurf : model.characterModel().decodeTopCandidates(text, stemToken, 2)) {
      for (Candidate harmonized :
          morphGenerator.harmonizeSuffixAfterStem(
              stemSurf.surface(),
              aposChar,
              suffixAscii,
              stemSurf.surface(),
              0,
              token.original(),
              token.hasPinnedTurkish())) {
        if (!containsSurface(outCandidates, harmonized.surface)) {
          outCandidates.add(harmonized);
        }
      }
    }
  }

  private Candidate[] decodeSentence(
      List<Token> tokens, TokenLattice[] lattice, WeightTable sentenceWeights) {
    int n = tokens.size();
    boolean anyAmbiguous = false;
    for (TokenLattice col : lattice) {
      if (col.isAmbiguous()) {
        anyAmbiguous = true;
        break;
      }
    }
    if (!anyAmbiguous) {
      Candidate[] trivial = new Candidate[n];
      for (int i = 0; i < n; i++) {
        trivial[i] = lattice[i].candidates.getFirst();
      }
      return trivial;
    }

    long[] featBuf = new long[SentenceLatticeScorer.FEATURE_BUFFER_SIZE];
    computeAmbiguousUnaryScores(tokens, lattice, sentenceWeights, featBuf);
    Candidate[] chosen = runPass1BeamSearch(lattice, sentenceWeights, beamWidth, featBuf);
    refineWithWideContext(lattice, chosen, sentenceWeights, featBuf);
    return chosen;
  }

  void computeAmbiguousUnaryScores(
      List<Token> tokens, TokenLattice[] lattice, WeightTable sentenceWeights, long[] featBuf) {
    int n = tokens.size();
    char sentEndPunct = tokens.getLast().rightPunct();
    boolean sentCapitalized = tokens.getFirst().casing() == Casing.TITLE;
    LexiconPriors priors = model.lexiconPriors();

    for (int i = 0; i < n; i++) {
      TokenLattice col = lattice[i];
      if (!col.isAmbiguous()) {
        continue;
      }
      int k = col.candidates.size();
      float[] unary = new float[k];
      for (int ci = 0; ci < k; ci++) {
        unary[ci] =
            SentenceLatticeScorer.scoreUnary(
                i,
                tokens,
                sentEndPunct,
                sentCapitalized,
                col.candidates.get(ci),
                col.empiricalLogPriors[ci],
                col.charRelativeScores[ci],
                priors,
                sentenceWeights,
                featBuf);
      }
      col.unaryScores = unary;
    }
  }

  /**
   * Left-to-right trigram Viterbi beam search. Unambiguous stretches skip feature extraction when
   * {@code beam.size() == 1} and collapse back to a single hypothesis after two unambiguous words.
   */
  static Candidate[] runPass1BeamSearch(
      TokenLattice[] lattice, WeightTable sentenceWeights, int beamWidth, long[] featBuf) {
    int n = lattice.length;
    List<BeamHyp> beam =
        List.of(new BeamHyp(Candidate.BOUNDARY, Candidate.BOUNDARY, 0.0f, null));

    for (int i = 0; i < n; i++) {
      TokenLattice col = lattice[i];
      List<Candidate> cands = col.candidates;
      int k = cands.size();

      if (k == 1 && beam.size() == 1) {
        BeamHyp only = beam.getFirst();
        beam = List.of(new BeamHyp(only.current, cands.getFirst(), only.score, only));
        continue;
      }

      if (k == 1 && i >= 1 && !lattice[i - 1].isAmbiguous()) {
        Candidate onlyCand = cands.getFirst();
        BeamHyp bestParent = beam.getFirst();
        float bestScore = Float.NEGATIVE_INFINITY;
        for (BeamHyp hyp : beam) {
          float s =
              hyp.score
                  + SentenceLatticeScorer.scoreTrigramOnly(
                      hyp.prev1, hyp.current, onlyCand, sentenceWeights, featBuf);
          if (s > bestScore) {
            bestScore = s;
            bestParent = hyp;
          }
        }
        beam = List.of(new BeamHyp(bestParent.current, onlyCand, bestScore, bestParent));
        continue;
      }

      List<BeamHyp> nextBeam = new ArrayList<>(beamWidth + 1);
      for (BeamHyp hyp : beam) {
        for (int ci = 0; ci < k; ci++) {
          Candidate cand = cands.get(ci);
          float uScore = k == 1 ? 0.0f : col.unaryScores[ci];
          float tScore =
              SentenceLatticeScorer.scoreTransition(
                  hyp.prev1, hyp.current, cand, sentenceWeights, featBuf);
          insertTopBeam(
              nextBeam,
              new BeamHyp(hyp.current, cand, hyp.score + uScore + tScore, hyp),
              beamWidth);
        }
      }
      beam = nextBeam;
    }

    Candidate[] chosen = new Candidate[n];
    BeamHyp cur = beam.getFirst();
    for (int i = n - 1; i >= 0; i--) {
      chosen[i] = cur.current;
      cur = cur.previous;
    }
    return chosen;
  }

  private static void insertTopBeam(List<BeamHyp> beam, BeamHyp candidate, int maxBeam) {
    if (beam.size() == maxBeam && candidate.score <= beam.get(maxBeam - 1).score) {
      return;
    }
    int pos = beam.size();
    while (pos > 0 && candidate.score > beam.get(pos - 1).score) {
      pos--;
    }
    beam.add(pos, candidate);
    if (beam.size() > maxBeam) {
      beam.remove(maxBeam);
    }
  }

  /**
   * Pass-2 coordinate refinement: Sweep 0 runs right-to-left (resolving head-final Turkish verbs
   * and possessed heads first), followed by Sweep 1 left-to-right if any token flipped.
   */
  private static void refineWithWideContext(
      TokenLattice[] lattice, Candidate[] chosen, WeightTable sentenceWeights, long[] featBuf) {
    int n = lattice.length;
    for (int sweep = 0; sweep < 2; sweep++) {
      boolean changed = false;
      int start = (sweep == 0) ? n - 1 : 0;
      int step = (sweep == 0) ? -1 : 1;

      for (int i = start; i >= 0 && i < n; i += step) {
        TokenLattice col = lattice[i];
        if (!col.isAmbiguous()) {
          continue;
        }
        Candidate prev2 = i >= 2 ? chosen[i - 2] : Candidate.BOUNDARY;
        Candidate prev1 = i >= 1 ? chosen[i - 1] : Candidate.BOUNDARY;
        Candidate next1 = i + 1 < n ? chosen[i + 1] : null;
        Candidate next2 = i + 2 < n ? chosen[i + 2] : null;

        Candidate bestCand = chosen[i];
        float bestScore = Float.NEGATIVE_INFINITY;
        List<Candidate> cands = col.candidates;
        for (int ci = 0; ci < cands.size(); ci++) {
          Candidate cand = cands.get(ci);
          float s =
              col.unaryScores[ci]
                  + SentenceLatticeScorer.scoreTransition(
                      prev2, prev1, cand, sentenceWeights, featBuf);
          if (next1 != null) {
            s +=
                SentenceLatticeScorer.scoreTransition(
                    prev1, cand, next1, sentenceWeights, featBuf);
            if (next2 != null) {
              s +=
                  SentenceLatticeScorer.scoreTrigramOnly(
                      cand, next1, next2, sentenceWeights, featBuf);
            }
          }
          s += SentenceLatticeScorer.scoreWideContext(i, chosen, n, cand, sentenceWeights, featBuf);
          if (s > bestScore) {
            bestScore = s;
            bestCand = cand;
          }
        }
        if (bestCand != chosen[i]) {
          chosen[i] = bestCand;
          changed = true;
        }
      }
      if (!changed) {
        break;
      }
    }
  }

  private static float[] buildEmpiricalPriors(
      List<Candidate> candidates, ConfusionSet confusion, Token token, LexiconPriors priors) {
    if (confusion != null || token == null || !token.hasApostrophe() || token.numeralApostrophe()) {
      return buildEmpiricalPriors(candidates, confusion);
    }
    int apos = token.apostropheIndex();
    ConfusionSet stemConfusion =
        priors.getConfusion(
            token.asciiLower().substring(0, apos), token.casing(), token.sentenceStart());
    if (stemConfusion == null) {
      return buildEmpiricalPriors(candidates, null);
    }
    int k = candidates.size();
    float[] scores = new float[k];
    for (int i = 0; i < k; i++) {
      Candidate c = candidates.get(i);
      if (c.surface.length() > apos && TurkishTextCodec.isApostrophe(c.surface.charAt(apos))) {
        scores[i] =
            stemConfusion.logProbOf(
                c.surface.substring(0, apos), c.oovFallback ? -6.0f : -4.2f);
      }
    }
    return scores;
  }

  private static float[] buildEmpiricalPriors(List<Candidate> candidates, ConfusionSet confusion) {
    int k = candidates.size();
    if (confusion == null) {
      return k == 1 ? SINGLE_ZERO_PRIOR : new float[k];
    }
    float[] priors = new float[k];
    for (int i = 0; i < k; i++) {
      Candidate c = candidates.get(i);
      priors[i] = confusion.logProbOf(c.surface, c.oovFallback ? -6.0f : -4.2f);
    }
    return priors;
  }

  private boolean hasCorruptedDotlessIToken(List<Token> tokens) {
    LexiconPriors priors = model.lexiconPriors();
    for (Token tok : tokens) {
      if (tok.immutable() || !TurkishTextCodec.containsDotlessI(tok.original())) {
        continue;
      }
      List<Candidate> pinnedMatches = morphGenerator.generate(tok, priors);
      if (pinnedMatches.isEmpty() || isObscureOnlyMatch(pinnedMatches)) {
        List<Candidate> unpinnedMatches = morphGenerator.generate(tok.withoutDotlessIPin(), priors);
        if (!unpinnedMatches.isEmpty() && !isObscureOnlyMatch(unpinnedMatches)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isObscureOnlyMatch(List<Candidate> morphCands) {
    for (Candidate c : morphCands) {
      if (c.morphemeGroupCount <= 3
          && c.pos != PrimaryPos.Duplicator
          && c.secondaryPos != SecondaryPos.Abbreviation) {
        return false;
      }
    }
    return true;
  }

  private boolean needsCharacterSupplement(Token token, List<Candidate> morphCands) {
    if (token.casing() == Casing.UPPER
        && model.lexiconPriors().getAllCapsTurkishAcronym(token.asciiLower()) != null) {
      return false;
    }
    if (isObscureOnlyMatch(morphCands)) {
      return true;
    }
    if (!token.sentenceStart() && token.casing() == Casing.TITLE && !token.hasApostrophe()) {
      for (Candidate c : morphCands) {
        if (c.secondaryPos == SecondaryPos.ProperNoun) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private static boolean containsSurface(List<Candidate> list, String surface) {
    for (Candidate c : list) {
      if (c.surface.equals(surface)) {
        return true;
      }
    }
    return false;
  }
}
