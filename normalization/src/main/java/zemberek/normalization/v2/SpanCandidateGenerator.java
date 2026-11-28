package zemberek.normalization.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.LmVocabulary;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier.CircumflexMode;
import zemberek.normalization.v2.LatticeMorphologySupport.CachedMorphItem;
import zemberek.normalization.v2.LatticeMorphologySupport.MorphSummary;
import zemberek.normalization.v2.NormV2Types.CasingShape;
import zemberek.normalization.v2.NormV2Types.SentenceLattice;
import zemberek.normalization.v2.NormV2Types.SpanArc;
import zemberek.normalization.v2.NormV2Types.SpanToken;
import zemberek.normalization.v2.NormalizationKnowledge.CleanWordLm;
import zemberek.normalization.v2.NormalizationKnowledge.RewriteEntry;
import zemberek.normalization.v2.NormalizationKnowledge.TargetCount;
import zemberek.tokenization.Token;

/**
 * Builds the multi-span candidate DAG ({@link SentenceLattice}) scored by {@link
 * SpanLatticePerceptronNormalizer}.
 */
public final class SpanCandidateGenerator {

  private static final int MAX_ARCS_PER_START = 14;
  private static final byte NEXT_STATE_EXISTENTIAL = 10;
  private static final byte NEXT_STATE_CLAUSE_END = 11;

  private static final Set<String> XIAOMI_MI_PRODUCT_TAILS =
      Set.of("band", "box", "note", "mix", "stick", "pad", "tv");

  private static final Set<String> GIVEN_NAME_HOMOGRAPHS =
      Set.of(
          "ümit", "metin", "lale", "çağrı", "zafer", "savaş", "barış", "deniz", "umut", "zeki",
          "mimar", "sultan", "kemal", "yaşar");

  private static final Set<String> GAZETTEER_TITLE_HEAD_PREFIXES =
      Set.of(
          "sokak", "sokağı", "cadde", "caddesi", "bulvar", "bulvarı", "mahalle", "mahallesi",
          "üniversite", "üniversitesi", "okulu", "lisesi", "ilkokulu", "ortaokulu", "klinik",
          "kliniği", "hastanesi", "kompleksi", "şampiyonası", "barajı", "köprü", "köprüsü", "dağı",
          "gölü", "nehri", "adaları", "ceza", "içmesi");

  private static final Set<String> TDK_HOLIDAY_MODIFIERS_EXT =
      Set.of(
          "sevgililer", "anneler", "babalar", "öğretmenler", "kadınlar", "cumhuriyet", "zafer",
          "gençlik");

  private static final Set<String> DOTTED_GAZETTEER_OR_HONORIFIC =
      Set.of(
          "mah.", "cad.", "sok.", "apt.", "malz.", "san.", "tic.", "ltd.", "şti.", "bey", "hanım");

  private static final Set<String> HARD_END_PUNCT = Set.of(".", "!", "?", "..", "...", "…");
  private static final Set<String> SOFT_BOUNDARY_PUNCT = Set.of(":", ";", "-", "–", "—");

  private static final int VALID_SINGLE_WORD_MASK =
      NormV2Types.SRC_DEASCIIFIER
          | NormV2Types.SRC_FORMAL_MORPH
          | NormV2Types.SRC_INFORMAL_MORPH
          | NormV2Types.SRC_INFORMAL_EXACT
          | NormV2Types.SRC_OOV_APOSTROPHE
          | NormV2Types.SRC_SUFFIX_RULE
          | NormV2Types.SRC_ELONGATION
          | NormV2Types.SRC_SHORTHAND;

  private record SentenceProfile(
      boolean[] strictKnown,
      boolean sentenceAllCaps,
      boolean sentenceTitleNoisy,
      boolean startsCap,
      float sentenceAsciiRatio,
      byte overtSubjectPerson,
      boolean hasQuestionMark,
      String[] deascByTok) {}

  static final class CandidateDraft {
    final String normSurface;
    final String normLower;
    int sourceMask;
    float parallelProb;
    float parallelLogCount;
    float suffixPrecision;
    float editCost;
    final int diacriticDiffs;

    CandidateDraft(
        String normSurface,
        String normLower,
        int sourceMask,
        float parallelProb,
        float parallelLogCount,
        float suffixPrecision,
        float editCost,
        int diacriticDiffs) {
      this.normSurface = normSurface;
      this.normLower = normLower;
      this.sourceMask = sourceMask;
      this.parallelProb = parallelProb;
      this.parallelLogCount = parallelLogCount;
      this.suffixPrecision = suffixPrecision;
      this.editCost = editCost;
      this.diacriticDiffs = diacriticDiffs;
    }

    String firstWordLower() {
      int sp = normLower.indexOf(' ');
      return sp >= 0 ? normLower.substring(0, sp) : normLower;
    }

    String lastWordLower() {
      int sp = normLower.lastIndexOf(' ');
      return sp >= 0 ? normLower.substring(sp + 1) : normLower;
    }
  }

  /**
   * Accumulates single-slot candidates in deterministic {@link LinkedHashMap} insertion order for
   * consistent tie-breaking and {@link #MAX_ARCS_PER_START} pruning.
   */
  static final class SlotDraftBuilder {
    final SpanToken tok;
    final boolean capHere;
    final boolean sentenceAllCaps;
    private final NormalizationKnowledge knowledge;
    private final TurkishMorphology strictMorphology;
    private Map<String, CandidateDraft> drafts = new LinkedHashMap<>(10);

    SlotDraftBuilder(
        SpanToken tok,
        boolean capHere,
        boolean sentenceAllCaps,
        NormalizationKnowledge knowledge,
        TurkishMorphology strictMorphology) {
      this.tok = tok;
      this.capHere = capHere;
      this.sentenceAllCaps = sentenceAllCaps;
      this.knowledge = knowledge;
      this.strictMorphology = strictMorphology;
    }

    String truecase(String normLower) {
      return knowledge.truecase(
          normLower, tok.shape(), tok.surface(), capHere, sentenceAllCaps, strictMorphology);
    }

    void add(
        String normSurface,
        String normLower,
        int sourceMask,
        float parallelProb,
        float parallelLogCount,
        float suffixPrecision,
        float editCost,
        int diacriticDiffs) {
      putOrMerge(
          drafts,
          normSurface,
          normLower,
          sourceMask,
          parallelProb,
          parallelLogCount,
          suffixPrecision,
          editCost,
          diacriticDiffs);
    }

    void addRule(
        String normSurface,
        String normLower,
        int sourceMask,
        float suffixPrecision,
        float editCost,
        int diacriticDiffs) {
      add(normSurface, normLower, sourceMask, 0.0f, 0.0f, suffixPrecision, editCost, diacriticDiffs);
    }

    void addTruecased(
        String normLower,
        int sourceMask,
        float suffixPrecision,
        float editCost,
        int diacriticDiffs) {
      add(
          truecase(normLower),
          normLower,
          sourceMask,
          0.0f,
          0.0f,
          suffixPrecision,
          editCost,
          diacriticDiffs);
    }

    void addTruecasedAutoCost(String normLower, int sourceMask, float suffixPrecision) {
      String low = tok.lower();
      add(
          truecase(normLower),
          normLower,
          sourceMask,
          0.0f,
          0.0f,
          suffixPrecision,
          computeWeightedEditCost(low, normLower),
          countDiacriticDiffs(low, normLower));
    }

    boolean hasLowCostMorphDraft() {
      int mask =
          NormV2Types.SRC_DEASCIIFIER
              | NormV2Types.SRC_FORMAL_MORPH
              | NormV2Types.SRC_INFORMAL_EXACT;
      for (CandidateDraft d : drafts.values()) {
        if ((d.sourceMask & mask) != 0 && d.editCost <= 0.30f) {
          return true;
        }
      }
      return false;
    }

    boolean hasValidSingleWordDraft() {
      for (CandidateDraft d : drafts.values()) {
        if (d.normLower.indexOf(' ') < 0 && (d.sourceMask & VALID_SINGLE_WORD_MASK) != 0) {
          return true;
        }
      }
      return false;
    }

    void applyContextualTruecasing(int tokCaseCtx) {
      Map<String, CandidateDraft> ctxDrafts = new LinkedHashMap<>(drafts.size());
      for (CandidateDraft d : drafts.values()) {
        String surf = d.normSurface;
        String baseTruecased =
            knowledge.truecase(
                d.normLower,
                tok.shape(),
                tok.surface(),
                capHere,
                sentenceAllCaps,
                strictMorphology,
                0);
        if (surf.equals(baseTruecased)) {
          surf =
              knowledge.truecase(
                  d.normLower,
                  tok.shape(),
                  tok.surface(),
                  capHere,
                  sentenceAllCaps,
                  strictMorphology,
                  tokCaseCtx);
        }
        putOrMerge(
            ctxDrafts,
            surf,
            d.normLower,
            d.sourceMask,
            d.parallelProb,
            d.parallelLogCount,
            d.suffixPrecision,
            d.editCost,
            d.diacriticDiffs);
      }
      this.drafts = ctxDrafts;
    }

    Collection<CandidateDraft> values() {
      return drafts.values();
    }

    private static void putOrMerge(
        Map<String, CandidateDraft> target,
        String normSurface,
        String normLower,
        int sourceMask,
        float parallelProb,
        float parallelLogCount,
        float suffixPrecision,
        float editCost,
        int diacriticDiffs) {
      CandidateDraft existing = target.get(normSurface);
      if (existing == null) {
        target.put(
            normSurface,
            new CandidateDraft(
                normSurface,
                normLower,
                sourceMask,
                parallelProb,
                parallelLogCount,
                suffixPrecision,
                editCost,
                diacriticDiffs));
      } else {
        existing.sourceMask |= sourceMask;
        existing.parallelProb = Math.max(existing.parallelProb, parallelProb);
        existing.parallelLogCount = Math.max(existing.parallelLogCount, parallelLogCount);
        existing.suffixPrecision = Math.max(existing.suffixPrecision, suffixPrecision);
        existing.editCost = Math.min(existing.editCost, editCost);
      }
    }
  }

  private final LatticeMorphologySupport morph;
  private final LatticeDeasciifier latticeDeasciifier;
  private final CompactStemEndingSpellChecker spellChecker;
  private final SmoothLm lm;
  private final LmVocabulary lmVocab;
  private final NormalizationKnowledge mined;
  private final SingleTokenRewriteGenerator singleRewriter;
  private final ShorthandExpander shorthandExpander;
  private final SplitMergeSpanGenerator splitMerger;

  public SpanCandidateGenerator(
      TurkishMorphology strictMorphology,
      CompactStemEndingSpellChecker spellChecker,
      SmoothLm lm,
      NormalizationKnowledge mined) {
    this.morph = new LatticeMorphologySupport(strictMorphology, mined);
    this.latticeDeasciifier =
        LatticeDeasciifier.createWithDefaults(strictMorphology, morph.informalAsciiMorphology)
            .withCircumflexMode(CircumflexMode.NONE);
    this.spellChecker = spellChecker;
    this.lm = lm;
    this.lmVocab = lm != null ? lm.getVocabulary() : null;
    this.mined = mined;
    this.singleRewriter = new SingleTokenRewriteGenerator(mined, morph, spellChecker);
    this.shorthandExpander = new ShorthandExpander(mined, morph);
    this.splitMerger = new SplitMergeSpanGenerator(mined, morph, lmVocab);
  }

  public NormalizationKnowledge mined() {
    return mined;
  }

  // =========================================================================
  // Sentence Lattice Construction
  // =========================================================================

  @SuppressWarnings({"unchecked", "rawtypes"})
  public SentenceLattice buildLattice(String rawSentence) {
    List<SpanToken> tokens = NormV2Types.spanTokenize(rawSentence);
    int n = tokens.size();
    List<SpanArc>[] outgoing = new List[n];
    for (int i = 0; i < n; i++) {
      outgoing[i] = new ArrayList<>(4);
    }
    if (n == 0) {
      return new SentenceLattice(tokens, outgoing, 0f, (byte) 0, false, false);
    }

    SentenceProfile profile = analyzeSentence(rawSentence, tokens);
    boolean[] capStartAt =
        computeSentenceStarts(
            tokens, profile.startsCap, profile.sentenceAllCaps, profile.sentenceTitleNoisy);
    int[] casingCtx =
        computeTokenCasingContexts(tokens, profile.sentenceAllCaps, profile.sentenceTitleNoisy);

    for (int i = 0; i < n; i++) {
      SpanToken tok = tokens.get(i);
      boolean capHere = capStartAt[i];
      int tokCaseCtx = casingCtx[i];

      if (tok.isImmutable()) {
        short punctTag = LatticeMorphologySupport.encodeMorphTag(PrimaryPos.Punctuation, null);
        outgoing[i].add(
            SpanArc.plain(
                i, i + 1, tok.surface(), tok.lower(), NormV2Types.SRC_IDENTITY, punctTag));
        splitMerger.addMultiTokenMerges(
            this, tokens, i, capHere, profile.sentenceAllCaps, tokCaseCtx, outgoing[i]);
        continue;
      }

      RewriteEntry parEntry = mined.parallel1ToN.get(tok.lower());
      SlotDraftBuilder slot =
          populateSlotCandidates(tokens, i, profile, capHere, tokCaseCtx, parEntry);
      emitPrunedSlotArcs(tokens, i, tok, slot, parEntry, outgoing[i]);
      splitMerger.addMultiTokenMerges(
          this, tokens, i, capHere, profile.sentenceAllCaps, tokCaseCtx, outgoing[i]);
    }

    return new SentenceLattice(
        tokens,
        outgoing,
        profile.sentenceAsciiRatio,
        profile.overtSubjectPerson,
        profile.sentenceAllCaps,
        profile.hasQuestionMark);
  }

  private SentenceProfile analyzeSentence(String rawSentence, List<SpanToken> tokens) {
    int n = tokens.size();
    int alphaWords = 0;
    int allUpperWords = 0;
    int titleCaseWords = 0;
    int asciiCandidateWords = 0;
    int pureAsciiWords = 0;
    int explicitDiacriticWords = 0;
    int oovAsciiWords = 0;
    boolean firstAlphaSeen = false;
    boolean firstAlphaCap = false;
    boolean hasQuestionMark = false;
    byte overtSubjectPerson = 0;

    boolean[] strictKnown = new boolean[n];
    for (int i = 0; i < n; i++) {
      SpanToken t = tokens.get(i);
      if (t.type() == Token.Type.Punctuation && t.surface().indexOf('?') >= 0) {
        hasQuestionMark = true;
      }
      if (t.isImmutable()) {
        continue;
      }
      if (t.shape() != CasingShape.NON_ALPHA) {
        alphaWords++;
        if (t.shape() == CasingShape.UPPER) {
          allUpperWords++;
        } else if (t.shape() == CasingShape.TITLE) {
          titleCaseWords++;
        }
        if (!firstAlphaSeen) {
          firstAlphaSeen = true;
          firstAlphaCap = Character.isUpperCase(t.surface().charAt(0));
        }
      }
      boolean reg = morph.hasRegularStrictAnalysis(t.lower());
      strictKnown[i] = reg;
      boolean explicitDiac = hasExplicitTurkishDiacritics(t.lower());
      if (explicitDiac) {
        explicitDiacriticWords++;
      }
      boolean upperAmbI = t.hasUpperAsciiAmbiguousI();
      if (hasAsciiAmbiguousChars(t.lower()) || upperAmbI) {
        asciiCandidateWords++;
        if (upperAmbI || !explicitDiac) {
          pureAsciiWords++;
        }
        if (!reg) {
          oovAsciiWords++;
        }
      }
      if (overtSubjectPerson == 0) {
        overtSubjectPerson = overtSubjectPersonCode(t.lower());
      }
    }

    boolean sentenceAllCaps = alphaWords >= 3 && allUpperWords * 10 >= alphaWords * 8;
    boolean sentenceTitleNoisy =
        !sentenceAllCaps && alphaWords >= 4 && titleCaseWords * 100 >= alphaWords * 45;
    boolean startsCap = sentenceAllCaps || firstAlphaCap;
    float sentenceAsciiRatio =
        asciiCandidateWords > 0 ? (pureAsciiWords / (float) asciiCandidateWords) : 0.0f;

    boolean shouldRunSentenceDeasc =
        asciiCandidateWords > 0
            && (oovAsciiWords > 0
                || explicitDiacriticWords == 0
                || (pureAsciiWords >= 2 && pureAsciiWords * 2 >= alphaWords));
    String[] deascByTok =
        shouldRunSentenceDeasc
            ? deasciifySentenceTokens(rawSentence, tokens, sentenceAllCaps, pureAsciiWords > 0)
            : null;

    return new SentenceProfile(
        strictKnown,
        sentenceAllCaps,
        sentenceTitleNoisy,
        startsCap,
        sentenceAsciiRatio,
        overtSubjectPerson,
        hasQuestionMark,
        deascByTok);
  }

  private static byte overtSubjectPersonCode(String wordLower) {
    return switch (wordLower) {
      case "ben" -> 1;
      case "sen" -> 2;
      case "biz" -> 4;
      case "siz" -> 5;
      default -> 0;
    };
  }

  private String[] deasciifySentenceTokens(
      String rawSentence,
      List<SpanToken> tokens,
      boolean sentenceAllCaps,
      boolean hasPureAsciiWords) {
    int n = tokens.size();
    boolean loweredAllCaps =
        sentenceAllCaps && hasPureAsciiWords && !NormV2Types.hasUpperTurkishDiacritics(rawSentence);
    String deascInput =
        loweredAllCaps ? NormV2Types.trLower(rawSentence).replace('ı', 'i') : rawSentence;
    String deascSent = latticeDeasciifier.deasciifyKnownWords(deascInput);
    if (deascSent.equals(deascInput)) {
      return null;
    }
    List<SpanToken> dToks = NormV2Types.spanTokenize(deascSent);
    List<SpanToken> inToks = loweredAllCaps ? NormV2Types.spanTokenize(deascInput) : tokens;
    if (dToks.size() != n || inToks.size() != n) {
      return null;
    }
    String[] deascByTok = new String[n];
    for (int i = 0; i < n; i++) {
      String dLow = dToks.get(i).lower();
      String inLow = inToks.get(i).lower();
      if (!dLow.equals(inLow) || morph.hasRegularStrictAnalysis(dLow)) {
        deascByTok[i] = dLow;
      }
    }
    return deascByTok;
  }

  private SlotDraftBuilder populateSlotCandidates(
      List<SpanToken> tokens,
      int i,
      SentenceProfile profile,
      boolean capHere,
      int tokCaseCtx,
      RewriteEntry parEntry) {
    SpanToken tok = tokens.get(i);
    boolean strictKnown = profile.strictKnown[i];
    SlotDraftBuilder slot =
        new SlotDraftBuilder(
            tok, capHere, profile.sentenceAllCaps, mined, morph.strictMorphology);
    char expectedPrevQ =
        computeImmediatePrevQuestionVowel(tokens, profile.deascByTok, i, profile.sentenceAllCaps);

    float rawKeepProb = parEntry != null ? parEntry.keepProb() : 0.0f;
    float rawKeepLogCnt = parEntry != null ? (float) Math.log1p(parEntry.keepCount()) : 0.0f;

    addIdentityCandidate(
        slot, tok, strictKnown, tokCaseCtx, parEntry, expectedPrevQ, rawKeepProb, rawKeepLogCnt);
    addParallelRewriteCandidates(slot, tokens, i, profile, capHere, parEntry);

    if (!tok.isRandomLaugh()) {
      addDeasciifierAndMorphCandidates(
          slot, tok, strictKnown, profile, capHere, i, expectedPrevQ, rawKeepProb, rawKeepLogCnt);
      if (!strictKnown) {
        singleRewriter.addSuffixAndHaplologyCandidates(slot);
      }
      if (!strictKnown
          && !tok.containsDigit()
          && !(tok.shape() == CasingShape.UPPER && !profile.sentenceAllCaps)) {
        shorthandExpander.addCandidates(slot);
      }
      singleRewriter.addProperAcronymAndNumeralCandidates(slot);
      if (hasRepeatedLetters(tok.lower())) {
        singleRewriter.addElongationCandidates(slot, expectedPrevQ, strictKnown);
      }
      splitMerger.addAmbiguityPreservingSplits(slot, strictKnown);
      if (shouldRunSpellChecker(slot, tok, strictKnown, profile.sentenceAllCaps, parEntry)) {
        boolean protectMidTitle =
            !profile.sentenceAllCaps
                && !profile.sentenceTitleNoisy
                && !capHere
                && tok.shape() == CasingShape.TITLE;
        singleRewriter.addGuardedSpellCandidates(slot, protectMidTitle);
      }
    }

    if (tokCaseCtx != 0) {
      slot.applyContextualTruecasing(tokCaseCtx);
    }
    return slot;
  }

  private void addIdentityCandidate(
      SlotDraftBuilder slot,
      SpanToken tok,
      boolean strictKnown,
      int tokCaseCtx,
      RewriteEntry parEntry,
      char expectedPrevQ,
      float rawKeepProb,
      float rawKeepLogCnt) {
    boolean disharmonicQuestion =
        expectedPrevQ != 0
            && isStandaloneQuestionClitic(tok.lower())
            && tok.lower().length() >= 2
            && tok.lower().charAt(1) != expectedPrevQ
            && isSameRoundingQuestionVowel(tok.lower().charAt(1), expectedPrevQ);
    float idParProb = disharmonicQuestion ? Math.min(rawKeepProb, 0.12f) : rawKeepProb;
    float idParLogCnt = disharmonicQuestion ? Math.min(rawKeepLogCnt, 1.0f) : rawKeepLogCnt;
    String idSurf =
        mined.truecase(
            tok.lower(),
            tok.shape(),
            tok.surface(),
            slot.capHere,
            slot.sentenceAllCaps,
            morph.strictMorphology,
            tokCaseCtx);
    int srcMask =
        NormV2Types.SRC_IDENTITY
            | (strictKnown ? NormV2Types.SRC_FORMAL_MORPH : 0)
            | (parEntry != null && parEntry.keepCount() > 0 ? NormV2Types.SRC_PARALLEL_1TO1 : 0);
    slot.add(idSurf, tok.lower(), srcMask, idParProb, idParLogCnt, 0.0f, 0.0f, 0);
  }

  private void addParallelRewriteCandidates(
      SlotDraftBuilder slot,
      List<SpanToken> tokens,
      int i,
      SentenceProfile profile,
      boolean capHere,
      RewriteEntry parEntry) {
    if (parEntry == null) {
      return;
    }
    SpanToken tok = tokens.get(i);
    boolean protectExplicitUpperAcronym =
        tok.shape() == CasingShape.UPPER
            && !profile.sentenceAllCaps
            && (mined.isKnownAcronymStem(tok.lower())
                || morph.isKnownStrictAbbreviation(tok.lower()));
    boolean protectMidTitlePhrase =
        !profile.sentenceAllCaps
            && !profile.sentenceTitleNoisy
            && !capHere
            && tok.shape() == CasingShape.TITLE
            && i > 0
            && tokens.get(i - 1).shape() == CasingShape.TITLE;

    for (TargetCount tc : parEntry.targets()) {
      if (tc.normLower().equals(tok.lower())) {
        continue;
      }
      if ((protectExplicitUpperAcronym || protectMidTitlePhrase)
          && !TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(tok.lower(), tc.normLower())) {
        continue;
      }
      int srcFlag =
          tc.normLower().indexOf(' ') >= 0
              ? NormV2Types.SRC_PARALLEL_SPLIT
              : NormV2Types.SRC_PARALLEL_1TO1;
      slot.add(
          slot.truecase(tc.normLower()),
          tc.normLower(),
          srcFlag,
          parEntry.probOf(tc.normLower()),
          (float) Math.log1p(tc.count()),
          0.0f,
          computeWeightedEditCost(tok.lower(), tc.normLower()),
          countDiacriticDiffs(tok.lower(), tc.normLower()));
    }
  }

  private void addDeasciifierAndMorphCandidates(
      SlotDraftBuilder slot,
      SpanToken tok,
      boolean strictKnown,
      SentenceProfile profile,
      boolean capHere,
      int i,
      char expectedPrevQ,
      float rawKeepProb,
      float rawKeepLogCnt) {
    String[] deascByTok = profile.deascByTok;
    if (deascByTok != null && deascByTok[i] != null && !deascByTok[i].equals(tok.lower())) {
      String dl = deascByTok[i];
      if (!LatticeMorphologySupport.stripsExplicitDiacritic(tok.lower(), dl)) {
        int dDiffs = countDiacriticDiffs(tok.lower(), dl);
        slot.addTruecased(dl, NormV2Types.SRC_DEASCIIFIER, 0.0f, dDiffs * 0.12f, dDiffs);
      }
    }
    boolean shortAmbiguousAscii =
        tok.lower().length() <= 6
            && !hasExplicitTurkishDiacritics(tok.lower())
            && hasAsciiAmbiguousChars(tok.lower());
    if (!strictKnown || tok.hasUpperAsciiAmbiguousI() || shortAmbiguousAscii) {
      addMorphologicalRealizations(slot);
    }
    boolean midSentenceTitle =
        !profile.sentenceAllCaps && !capHere && tok.shape() == CasingShape.TITLE;
    if (!midSentenceTitle) {
      singleRewriter.addQuestionCliticDiacriticTwins(
          slot, tok.lower(), expectedPrevQ, rawKeepProb, rawKeepLogCnt, false);
    }
  }

  private boolean shouldRunSpellChecker(
      SlotDraftBuilder slot,
      SpanToken tok,
      boolean strictKnown,
      boolean sentenceAllCaps,
      RewriteEntry parEntry) {
    String low = tok.lower();
    if (spellChecker == null
        || strictKnown
        || tok.containsDigit()
        || low.length() < 3
        || low.length() > 18
        || (parEntry != null && parEntry.totalCount() >= 8)
        || mined.isKnownAcronymStem(low)
        || containsForeignWxq(low)
        || slot.hasLowCostMorphDraft()) {
      return false;
    }
    if (!sentenceAllCaps && tok.shape() == CasingShape.UPPER) {
      return false;
    }
    if (morph.isStrictProperOrAbbrOnly(low)
        && (tok.shape() != CasingShape.LOWER
            || (mined.cleanLm != null && mined.cleanLm.unigramCount(low) >= 10))) {
      return false;
    }
    return !isCleanLoanword(low, parEntry);
  }

  private boolean isCleanLoanword(String low, RewriteEntry parEntry) {
    if (mined.cleanLm == null
        || (parEntry != null && parEntry.keepCount() < parEntry.totalCount())) {
      return false;
    }
    int cleanCount = mined.cleanLm.unigramCount(low);
    if (low.length() <= 4 && cleanCount >= 12) {
      return true;
    }
    return low.length() <= 6 && !hasExplicitTurkishDiacritics(low) && cleanCount >= 35;
  }

  private void emitPrunedSlotArcs(
      List<SpanToken> tokens,
      int i,
      SpanToken tok,
      SlotDraftBuilder slot,
      RewriteEntry parEntry,
      List<SpanArc> arcsHere) {
    int rawParTotal = parEntry != null ? parEntry.totalCount() : 0;
    boolean rawNegCue = morph.hasRawNegativeVerbCue(tok.lower());
    String nextLexForUnary = nextLexicalWordOrNull(tokens, i + 1);
    byte nextState = computeNextSyntacticState(tokens, i + 1, nextLexForUnary);
    int count = 0;
    for (CandidateDraft d : slot.values()) {
      if (count >= MAX_ARCS_PER_START && (d.sourceMask & NormV2Types.SRC_IDENTITY) == 0) {
        continue;
      }
      SpanArc arc = materializeArc(i, i + 1, tok, nextLexForUnary, d);
      arc.rawParallelTotal = rawParTotal;
      arc.rawNegVerbCue = rawNegCue;
      arc.nextLexState = nextState;
      arcsHere.add(arc);
      count++;
    }
  }

  private void addMorphologicalRealizations(SlotDraftBuilder slot) {
    String low = slot.tok.lower();
    if (low.length() < 2 || slot.tok.containsDigit()) {
      return;
    }
    for (CachedMorphItem item : morph.morphRealizations(low)) {
      String surf =
          item.fixedProperSurface() != null
              ? item.fixedProperSurface()
              : slot.truecase(item.normLower());
      slot.addRule(
          surf,
          item.normLower(),
          item.sourceMask(),
          item.suffixPrecision(),
          item.editCost(),
          item.diacriticDiffs());
    }
  }

  private SpanArc materializeArc(
      int start, int end, SpanToken rawTok, String nextLexLower, CandidateDraft d) {
    String firstW = d.firstWordLower();
    String lastW = d.lastWordLower();
    MorphSummary msLast = morph.summarizeMorph(lastW);
    MorphSummary msFirst = firstW.equals(lastW) ? msLast : morph.summarizeMorph(firstW);
    boolean caseChanged =
        !d.normSurface.equals(rawTok.surface()) && d.normLower.equals(rawTok.lower());
    boolean apAdded = rawTok.surface().indexOf('\'') < 0 && d.normSurface.indexOf('\'') >= 0;
    boolean apRemoved = rawTok.surface().indexOf('\'') >= 0 && d.normSurface.indexOf('\'') < 0;
    boolean stripsDiac =
        LatticeMorphologySupport.stripsExplicitDiacritic(rawTok.lower(), d.normLower);
    float phono = mined.charPhonotactics.scoreWord(lastW);
    float intLm = d.normLower.indexOf(' ') >= 0 ? computeInternalLm(d.normLower) : 0.0f;

    SpanArc arc =
        new SpanArc(
            start,
            end,
            d.normSurface,
            d.normLower,
            d.sourceMask,
            msFirst.coarseTag(),
            msLast.coarseTag(),
            msFirst.syntaxClass(),
            msLast.verbPerson(),
            msLast.isRegular() && msFirst.isRegular(),
            msLast.isProperOrAbbr() || d.normSurface.indexOf('\'') > 0,
            d.parallelProb,
            d.parallelLogCount,
            d.suffixPrecision,
            d.editCost,
            d.diacriticDiffs,
            caseChanged,
            apAdded,
            apRemoved,
            stripsDiac,
            phono,
            intLm);
    arc.verbPolarity = msLast.verbPolarity();
    arc.rawCleanCount = mined.cleanLm != null ? mined.cleanLm.unigramCount(rawTok.lower()) : 0;
    arc.rawCharPhono = mined.charPhonotactics.scoreWord(rawTok.lower());
    if (mined.cleanLm != null && !d.normLower.equals(rawTok.lower())) {
      float uniDiff =
          mined.cleanLm.unigramLogProb(lastW) - mined.cleanLm.unigramLogProb(rawTok.lower());
      float bgDiff =
          nextLexLower != null
              ? mined.cleanLm.bigramLogProb(lastW, nextLexLower)
                  - mined.cleanLm.bigramLogProb(rawTok.lower(), nextLexLower)
              : 0.0f;
      arc.unaryCleanGain = uniDiff + bgDiff;
    }
    return arc;
  }

  SpanArc buildDirectArc(
      int start,
      int end,
      String normSurf,
      String normLow,
      int sourceMask,
      float parProb,
      float parLogCnt,
      float sufPrec,
      float editCost,
      int diacDiffs) {
    int spFirst = normLow.indexOf(' ');
    int spLast = normLow.lastIndexOf(' ');
    String firstW = spFirst >= 0 ? normLow.substring(0, spFirst) : normLow;
    String lastW = spLast >= 0 ? normLow.substring(spLast + 1) : normLow;
    MorphSummary msLast = morph.summarizeMorph(lastW);
    MorphSummary msFirst = firstW.equals(lastW) ? msLast : morph.summarizeMorph(firstW);
    float phono = mined.charPhonotactics.scoreWord(lastW);
    float intLm = spFirst >= 0 ? computeInternalLm(normLow) : 0.0f;

    SpanArc arc =
        new SpanArc(
            start,
            end,
            normSurf,
            normLow,
            sourceMask,
            msFirst.coarseTag(),
            msLast.coarseTag(),
            msFirst.syntaxClass(),
            msLast.verbPerson(),
            msLast.isRegular() && msFirst.isRegular(),
            msLast.isProperOrAbbr() || normSurf.indexOf('\'') > 0,
            parProb,
            parLogCnt,
            sufPrec,
            editCost,
            diacDiffs,
            false,
            normSurf.indexOf('\'') >= 0,
            false,
            false,
            phono,
            intLm);
    arc.verbPolarity = msLast.verbPolarity();
    return arc;
  }

  // =========================================================================
  // Language Model, Clitic & Harmony Scoring Helpers (used by Perceptron & Inspector)
  // =========================================================================

  private byte computeNextSyntacticState(
      List<SpanToken> tokens, int nextIdx, String nextLexLower) {
    if (nextIdx >= tokens.size()) {
      return NEXT_STATE_CLAUSE_END;
    }
    SpanToken nt = tokens.get(nextIdx);
    if (nt.type() == Token.Type.Emoticon || nt.type() == Token.Type.Emoji) {
      return NEXT_STATE_CLAUSE_END;
    }
    if (nt.type() == Token.Type.Punctuation) {
      String s = nt.surface();
      if (isSentenceBoundaryPunct(nt) || s.equals(";") || s.equals(":")) {
        return NEXT_STATE_CLAUSE_END;
      }
    }
    if (nextLexLower == null) {
      return NEXT_STATE_CLAUSE_END;
    }
    if (nextLexLower.equals("var")
        || nextLexLower.equals("yok")
        || nextLexLower.startsWith("vardı")
        || nextLexLower.startsWith("yoktu")
        || nextLexLower.startsWith("mevcut")) {
      return NEXT_STATE_EXISTENTIAL;
    }
    return morph.summarizeMorph(nextLexLower).verbPerson();
  }

  public boolean hasRegularStrictAnalysis(String wordLower) {
    return morph.hasRegularStrictAnalysis(wordLower);
  }

  public boolean hasRawNegativeVerbCue(String rawLower) {
    return morph.hasRawNegativeVerbCue(rawLower);
  }

  public char expectedTurkishQuestionVowel(String prevWordLower) {
    return morph.expectedTurkishQuestionVowel(prevWordLower);
  }

  public static String nextLexicalWordOrNull(List<SpanToken> toks, int fromIdx) {
    int n = toks.size();
    for (int k = fromIdx; k < n; k++) {
      SpanToken t = toks.get(k);
      if (isSentenceBoundaryPunct(t)) {
        return null;
      }
      if (!t.isImmutable()) {
        return t.lower();
      }
    }
    return null;
  }

  public static boolean isSentenceBoundaryPunct(SpanToken t) {
    if (t.type() != Token.Type.Punctuation) {
      return false;
    }
    String s = t.surface();
    return s.indexOf('.') >= 0 || s.indexOf('?') >= 0 || s.indexOf('!') >= 0;
  }

  /**
   * Computes boundary-to-boundary generative chain-rule log-probabilities over {@code
   * [arc.startIdx, arc.endIdx) -> nextLexicalWord} for both {@code SmoothLm} (2-gram) and {@link
   * CleanWordLm} (3-gram).
   */
  public void computeSpanLmComparison(
      SentenceLattice lat, String prev2Word, String prevWord, SpanArc arc, float[] out6) {
    List<SpanToken> toks = lat.tokens();
    String nextLex = nextLexicalWordOrNull(toks, arc.endIdx);
    CleanWordLm cleanLm = mined.cleanLm;

    float rawSmooth = 0.0f;
    float rawClean3 = 0.0f;
    String rPrev2 = prev2Word;
    String rPrev1 = prevWord;
    for (int k = arc.startIdx; k < arc.endIdx; k++) {
      SpanToken rt = toks.get(k);
      if (rt.isImmutable()) {
        if (isSentenceBoundaryPunct(rt)) {
          rPrev2 = null;
          rPrev1 = null;
        }
        continue;
      }
      String rw = rt.lower();
      rawSmooth += NormV2Types.clampLm(lmBigram(rPrev1, rw));
      rawClean3 += cleanLm.trigramLogProb(rPrev2, rPrev1, rw);
      rPrev2 = rPrev1;
      rPrev1 = rw;
    }
    rawSmooth += NormV2Types.clampLm(lmBigram(rPrev1, nextLex));
    rawClean3 += cleanLm.trigramLogProb(rPrev2, rPrev1, nextLex);

    float candSmooth = 0.0f;
    float candClean3 = 0.0f;
    String cPrev2 = prev2Word;
    String cPrev1 = prevWord;
    float cleanFwd3 = cleanLm.trigramLogProb(cPrev2, cPrev1, arc.firstWordLower);

    if (arc.wordCount == 1) {
      String cw = arc.firstWordLower;
      candSmooth += NormV2Types.clampLm(lmBigram(cPrev1, cw));
      candClean3 += cleanFwd3;
      cPrev2 = cPrev1;
      cPrev1 = cw;
    } else {
      String nl = arc.normLower;
      int start = 0;
      for (int idx = 0; idx <= nl.length(); idx++) {
        if (idx == nl.length() || nl.charAt(idx) == ' ') {
          if (idx > start) {
            String cw = nl.substring(start, idx);
            candSmooth += NormV2Types.clampLm(lmBigram(cPrev1, cw));
            candClean3 += cleanLm.trigramLogProb(cPrev2, cPrev1, cw);
            cPrev2 = cPrev1;
            cPrev1 = cw;
          }
          start = idx + 1;
        }
      }
    }
    float cleanRight3 = cleanLm.trigramLogProb(cPrev2, cPrev1, nextLex);
    candSmooth += NormV2Types.clampLm(lmBigram(cPrev1, nextLex));
    candClean3 += cleanRight3;

    out6[0] = candSmooth;
    out6[1] = rawSmooth;
    out6[2] = candClean3;
    out6[3] = rawClean3;
    out6[4] = cleanFwd3;
    out6[5] = cleanRight3;
  }

  public float computeCliticTransparencyLlr(SentenceLattice lat, SpanArc arc) {
    CleanWordLm cleanLm = mined.cleanLm;
    if (cleanLm == null) {
      return 0.0f;
    }
    List<SpanToken> toks = lat.tokens();
    String nextLex = nextLexicalWordOrNull(toks, arc.endIdx);
    if (arc.endIdx == arc.startIdx + 1 && arc.wordCount == 2) {
      String lw = arc.lastWordLower;
      if ("de".equals(lw)
          || "da".equals(lw)
          || SplitMergeSpanGenerator.QUESTION_TAILS.contains(lw)) {
        String rawLow = toks.get(arc.startIdx).lower();
        float skipBg = cleanLm.bigramLogProb(arc.firstWordLower, nextLex);
        float closedBg = cleanLm.bigramLogProb(rawLow, nextLex);
        return Math.clamp(skipBg - closedBg, -12.0f, 12.0f);
      }
      return 0.0f;
    }
    if (arc.endIdx == arc.startIdx + 2 && arc.wordCount == 1 && arc.startIdx + 1 < toks.size()) {
      String t2Low = toks.get(arc.startIdx + 1).lower();
      if (SplitMergeSpanGenerator.LOC_OR_ABL_PARTICLES.contains(t2Low)) {
        String t1Low = toks.get(arc.startIdx).lower();
        float mergedBg = cleanLm.bigramLogProb(arc.lastWordLower, nextLex);
        float skipBg = cleanLm.bigramLogProb(t1Low, nextLex);
        return Math.clamp(mergedBg - skipBg, -12.0f, 12.0f);
      }
    }
    return 0.0f;
  }

  public float computeQuestionCliticHarmony(SentenceLattice lat, String prev1Lex, SpanArc arc) {
    if (arc.endIdx == arc.startIdx + 1
        && arc.wordCount == 2
        && SplitMergeSpanGenerator.QUESTION_TAILS.contains(arc.lastWordLower)
        && arc.lastWordLower.length() >= 2) {
      char headV = morph.expectedTurkishQuestionVowel(arc.firstWordLower);
      return headV == 0 ? 0.0f : (arc.lastWordLower.charAt(1) == headV ? 1.0f : -1.0f);
    }
    if (arc.endIdx != arc.startIdx + 1
        || arc.wordCount != 1
        || arc.startIdx <= 0
        || prev1Lex == null
        || !SplitMergeSpanGenerator.QUESTION_TAILS.contains(arc.normLower)) {
      return 0.0f;
    }
    List<SpanToken> toks = lat.tokens();
    boolean allCapsSent = lat.sentenceAllCaps();
    SpanToken curTok = toks.get(arc.startIdx);
    if ((curTok.shape() == CasingShape.TITLE && !allCapsSent)
        || isXiaomiMiProductContext(toks, arc.startIdx, arc.endIdx)) {
      return 0.0f;
    }
    SpanToken prevTok = toks.get(arc.startIdx - 1);
    if (prevTok.isImmutable()
        || (prevTok.shape() == CasingShape.UPPER && !allCapsSent)
        || (prevTok.containsDigit() && prev1Lex.indexOf('\'') < 0)) {
      return 0.0f;
    }
    char expectedV = morph.expectedTurkishQuestionVowel(prev1Lex);
    if (expectedV == 0 || arc.normLower.length() < 2) {
      return 0.0f;
    }
    return arc.normLower.charAt(1) == expectedV ? 1.0f : -1.0f;
  }

  private static boolean isXiaomiMiProductContext(List<SpanToken> toks, int curIdx, int nextIdx) {
    if (!toks.get(curIdx).lower().equals("mi") || nextIdx >= toks.size()) {
      return false;
    }
    SpanToken nextTok = toks.get(nextIdx);
    return nextTok.containsDigit() || XIAOMI_MI_PRODUCT_TAILS.contains(nextTok.lower());
  }

  public static void advanceLexicalHistory(
      SentenceLattice lat, String prev2, String prev1, SpanArc arc, String[] out2) {
    if (arc.wordCount == 1 && arc.endIdx == arc.startIdx + 1) {
      SpanToken t = lat.tokens().get(arc.startIdx);
      if (t.isImmutable()) {
        if (isSentenceBoundaryPunct(t)) {
          out2[0] = null;
          out2[1] = null;
        } else {
          out2[0] = prev2;
          out2[1] = prev1;
        }
        return;
      }
      out2[0] = prev1;
      out2[1] = arc.lastWordLower;
      return;
    }
    if (arc.wordCount == 1) {
      out2[0] = prev1;
      out2[1] = arc.lastWordLower;
      return;
    }
    if (arc.wordCount == 2) {
      out2[0] = arc.firstWordLower;
      out2[1] = arc.lastWordLower;
      return;
    }
    int lastSp = arc.normLower.lastIndexOf(' ');
    int prevSp = arc.normLower.lastIndexOf(' ', Math.max(0, lastSp - 1));
    out2[0] = arc.normLower.substring(prevSp + 1, lastSp);
    out2[1] = arc.lastWordLower;
  }

  public float lmBigram(String prevWordLower, String curWordLower) {
    if (lm == null) {
      return mined.cleanLm != null
          ? mined.cleanLm.bigramLogProb(prevWordLower, curWordLower)
          : 0.0f;
    }
    int prevIdx =
        prevWordLower == null
            ? lmVocab.getSentenceStartIndex()
            : lmVocab.indexOfTurkishWord(prevWordLower);
    int curIdx =
        curWordLower == null
            ? lmVocab.getSentenceEndIndex()
            : lmVocab.indexOfTurkishWord(curWordLower);
    if (prevIdx < 0) {
      prevIdx = lmVocab.getUnknownWordIndex();
    }
    if (curIdx < 0) {
      curIdx = lmVocab.getUnknownWordIndex();
    }
    return lm.getProbability(prevIdx, curIdx);
  }

  float computeInternalLm(String multiWordLower) {
    String[] parts = multiWordLower.split(" ");
    if (parts.length < 2) {
      return 0.0f;
    }
    float s = 0.0f;
    for (int i = 1; i < parts.length; i++) {
      s += lmBigram(parts[i - 1], parts[i]);
    }
    return s;
  }

  public static boolean isStandaloneQuestionClitic(String wordLower) {
    if (wordLower == null || wordLower.length() < 2) {
      return false;
    }
    if (SplitMergeSpanGenerator.QUESTION_TAILS.contains(wordLower)) {
      return true;
    }
    String asc = NormV2Types.toAscii(wordLower);
    return asc.equals("miymis") || asc.equals("muymus");
  }

  private static boolean isSameRoundingQuestionVowel(char v1, char v2) {
    return ((v1 == 'i' || v1 == 'ı') && (v2 == 'i' || v2 == 'ı'))
        || ((v1 == 'u' || v1 == 'ü') && (v2 == 'u' || v2 == 'ü'));
  }

  private char computeImmediatePrevQuestionVowel(
      List<SpanToken> tokens, String[] deascByTok, int idx, boolean sentenceAllCaps) {
    if (idx <= 0) {
      return 0;
    }
    SpanToken cur = tokens.get(idx);
    if ((!sentenceAllCaps && cur.shape() == CasingShape.TITLE)
        || isXiaomiMiProductContext(tokens, idx, idx + 1)) {
      return 0;
    }
    SpanToken prev = tokens.get(idx - 1);
    if (prev.isImmutable() || (!sentenceAllCaps && prev.shape() == CasingShape.UPPER)) {
      return 0;
    }
    if (prev.containsDigit()
        && prev.lower().indexOf('\'') < 0
        && prev.surface().indexOf('’') < 0) {
      return 0;
    }
    String prevQuery =
        prev.surface().indexOf('’') >= 0 ? prev.lower().replace('’', '\'') : prev.lower();
    if (deascByTok != null
        && deascByTok[idx - 1] != null
        && !deascByTok[idx - 1].equals(prevQuery)) {
      char deascExp = morph.expectedTurkishQuestionVowel(deascByTok[idx - 1]);
      if (deascExp != 0) {
        return deascExp;
      }
    }
    if (cur.lower().indexOf('ı') >= 0
        && !hasExplicitTurkishDiacritics(prevQuery)
        && hasAsciiAmbiguousChars(prevQuery)) {
      return 0;
    }
    return morph.expectedTurkishQuestionVowel(prevQuery);
  }

  // =========================================================================
  // Sentence-Boundary & Contextual Truecasing Signals
  // =========================================================================

  private static final Set<String> NON_SENT_END_ABBREVS =
      Set.of(
          "vs", "vb", "mah", "cad", "sok", "sk", "apt", "no", "blv", "dr", "prof",
          "doç", "doc", "op", "uzm", "av", "arb", "hz", "bkz", "bknz", "yy", "kr",
          "tl", "dk", "sn", "st", "gr", "kg", "cm", "mm", "km", "lt", "ltd", "şti",
          "sti", "inc", "co", "mr", "mrs", "ms", "inş", "ins", "ör", "örn", "yak",
          "ort", "maks", "min", "tel", "faks", "fax", "cep", "www", "http", "https",
          "alb", "yb", "gn", "yd", "yrd", "ogr", "öğr", "gör", "gor", "arş", "ars",
          "vet", "ecz", "dt", "mim", "müh", "muh");

  private static final Set<String> DOMAIN_OR_FILE_EXTS =
      Set.of(
          "com", "net", "org", "tr", "gov", "edu", "io", "co", "uk", "de", "ru",
          "nl", "fr", "it", "info", "biz", "php", "html", "htm", "asp", "aspx",
          "jsp", "js", "css", "pdf", "jpg", "jpeg", "png", "gif", "mp3", "mp4",
          "exe", "zip", "rar", "txt");

  private static final Set<String> QUOTED_DETACHED_SUFFIXES =
      Set.of(
          "ler", "lar", "da", "de", "ta", "te", "dan", "den", "tan", "ten",
          "nin", "nın", "nun", "nün", "in", "ın", "un", "ün", "ye", "ya",
          "yi", "yı", "yu", "yü", "le", "la", "li", "lı", "lu", "lü");

  private static boolean isDotOrEllipsisSurface(String s) {
    return s.equals(".") || s.equals("..") || s.equals("...") || s.equals("…");
  }

  private static boolean isClosingQuoteOrBracket(String s) {
    return s.equals("\"")
        || s.equals("'")
        || s.equals("”")
        || s.equals("’")
        || s.equals(")")
        || s.equals("]");
  }

  private static boolean isTrueInternalSentenceBoundary(List<SpanToken> tokens, int i) {
    int n = tokens.size();
    String s = tokens.get(i).surface();
    if (!s.equals(".") && !s.equals("!") && !s.equals("?")) {
      return false;
    }
    if (i == 0) {
      return false;
    }
    if (s.equals(".")) {
      if (isDotOrEllipsisSurface(tokens.get(i - 1).surface())
          || (i + 1 < n && isDotOrEllipsisSurface(tokens.get(i + 1).surface()))) {
        return false;
      }
    } else if (i + 1 < n) {
      String ns = tokens.get(i + 1).surface();
      if (ns.equals("!") || ns.equals("?")) {
        return false;
      }
    }

    SpanToken prev = tokens.get(i - 1);
    if (s.equals(".")) {
      String ps = prev.surface();
      String pl = prev.lower();
      if (prev.type() == Token.Type.Number
          || prev.type() == Token.Type.URL
          || prev.type() == Token.Type.Email
          || prev.type() == Token.Type.Mention
          || prev.type() == Token.Type.Date
          || prev.type() == Token.Type.Time
          || prev.type() == Token.Type.RomanNumeral
          || prev.containsDigit()
          || ps.indexOf('/') >= 0
          || ps.indexOf(':') >= 0) {
        return false;
      }
      if ((pl.length() == 1 && Character.isLetter(pl.charAt(0)))
          || NON_SENT_END_ABBREVS.contains(pl)) {
        return false;
      }
    }

    int j = i + 1;
    boolean hadQuote = false;
    while (j < n && isClosingQuoteOrBracket(tokens.get(j).surface())) {
      hadQuote = true;
      j++;
    }
    if (j >= n) {
      return false;
    }
    SpanToken nxt = tokens.get(j);
    if (nxt.isImmutable() || nxt.type() == Token.Type.Number || nxt.containsDigit()) {
      return false;
    }
    String nl = nxt.lower();
    boolean hasAlpha = false;
    for (int k = 0; k < nl.length(); k++) {
      if (Character.isLetter(nl.charAt(k))) {
        hasAlpha = true;
        break;
      }
    }
    if (!hasAlpha) {
      return false;
    }
    if (s.equals(".") && DOMAIN_OR_FILE_EXTS.contains(nl)) {
      return false;
    }
    if (j + 2 < n
        && tokens.get(j + 1).surface().equals(".")
        && DOMAIN_OR_FILE_EXTS.contains(tokens.get(j + 2).lower())) {
      return false;
    }
    if (j + 1 < n
        && (tokens.get(j + 1).type() == Token.Type.Mention
            || tokens.get(j + 1).type() == Token.Type.Email)) {
      return false;
    }
    if (hadQuote && QUOTED_DETACHED_SUFFIXES.contains(nl)) {
      return false;
    }
    return true;
  }

  private static boolean[] computeSentenceStarts(
      List<SpanToken> tokens,
      boolean startsCap,
      boolean sentenceAllCaps,
      boolean sentenceTitleNoisy) {
    int n = tokens.size();
    boolean[] out = new boolean[n];

    int firstTrueBound = -1;
    for (int i = 0; i < n - 1; i++) {
      if (tokens.get(i).type() == Token.Type.Punctuation
          && isTrueInternalSentenceBoundary(tokens, i)) {
        firstTrueBound = i;
        break;
      }
    }

    boolean pendingStart = startsCap;
    if (!pendingStart && firstTrueBound > 0) {
      int idx0 = 0;
      while (idx0 < firstTrueBound
          && (tokens.get(idx0).type() == Token.Type.Mention
              || tokens.get(idx0).type() == Token.Type.Emoji
              || tokens.get(idx0).type() == Token.Type.Emoticon)) {
        idx0++;
      }
      if (idx0 < firstTrueBound) {
        SpanToken firstTok = tokens.get(idx0);
        if (!firstTok.isImmutable()
            && firstTok.type() != Token.Type.Number
            && !firstTok.containsDigit()) {
          pendingStart = true;
        }
      }
    }

    boolean afterTrueEndPunct = false;
    boolean afterEndPunct = false;
    boolean softBoundary = false;

    for (int i = 0; i < n; i++) {
      SpanToken t = tokens.get(i);
      if (t.type() == Token.Type.Punctuation) {
        String s = t.surface();
        if (HARD_END_PUNCT.contains(s)) {
          if (isTrueInternalSentenceBoundary(tokens, i)) {
            afterTrueEndPunct = true;
            afterEndPunct = false;
          } else {
            afterEndPunct = true;
          }
        } else if (SOFT_BOUNDARY_PUNCT.contains(s)) {
          softBoundary = true;
        }
        continue;
      }
      if (t.type() == Token.Type.Emoji || t.type() == Token.Type.Emoticon) {
        if (!pendingStart && !afterTrueEndPunct) {
          softBoundary = true;
        }
        continue;
      }
      if (t.isImmutable()) {
        if (t.type() == Token.Type.URL
            || t.type() == Token.Type.Email
            || t.type() == Token.Type.Date
            || t.type() == Token.Type.Time
            || t.type() == Token.Type.PercentNumeral) {
          pendingStart = false;
          afterTrueEndPunct = false;
          afterEndPunct = false;
          softBoundary = false;
        }
        continue;
      }
      if (t.type() == Token.Type.Number || t.containsDigit()) {
        pendingStart = false;
        afterTrueEndPunct = false;
        afterEndPunct = false;
        softBoundary = false;
        continue;
      }
      boolean startsUpper = !t.surface().isEmpty() && Character.isUpperCase(t.surface().charAt(0));
      if (pendingStart) {
        out[i] = true;
        pendingStart = false;
      } else if (afterTrueEndPunct) {
        out[i] = true;
      } else if (afterEndPunct) {
        if (sentenceAllCaps || startsUpper) {
          out[i] = true;
        }
      } else if (softBoundary && !sentenceAllCaps && !sentenceTitleNoisy && startsUpper) {
        out[i] = true;
      }
      afterTrueEndPunct = false;
      afterEndPunct = false;
      softBoundary = false;
    }
    return out;
  }

  private int[] computeTokenCasingContexts(
      List<SpanToken> tokens, boolean sentenceAllCaps, boolean sentenceTitleNoisy) {
    int n = tokens.size();
    int[] out = new int[n];
    int baseFlags = sentenceTitleNoisy ? NormalizationKnowledge.CTX_SENT_TITLE_NOISY : 0;
    for (int i = 0; i < n; i++) {
      int flags = baseFlags;
      SpanToken cur = tokens.get(i);
      if (cur.isImmutable()) {
        out[i] = flags;
        continue;
      }
      SpanToken prev = i > 0 ? tokens.get(i - 1) : null;
      SpanToken next = i + 1 < n ? tokens.get(i + 1) : null;
      String curLow = cur.lower();

      if (prev != null && (prev.type() == Token.Type.Number || prev.containsDigit())) {
        flags |= NormalizationKnowledge.CTX_AFTER_NUMBER;
      }
      if ((prev != null && isCalendarDayOrYearNumber(prev.surface()))
          || (next != null && isCalendarYearNumber(next.surface()))) {
        flags |= NormalizationKnowledge.CTX_ADJ_DATE_NUM;
      }
      if (next != null && isGazetteerOrHonorificHead(next, sentenceAllCaps, sentenceTitleNoisy)) {
        flags |= NormalizationKnowledge.CTX_BEFORE_GAZETTEER_OR_HONORIFIC;
      }
      if (shouldPromoteProperInContext(
          tokens, i, curLow, prev, next, sentenceAllCaps, sentenceTitleNoisy)) {
        flags |= NormalizationKnowledge.CTX_PROMOTE_PROPER_IN_CONTEXT;
      }
      out[i] = flags;
    }
    return out;
  }

  private boolean shouldPromoteProperInContext(
      List<SpanToken> tokens,
      int idx,
      String curLow,
      SpanToken prev,
      SpanToken next,
      boolean sentenceAllCaps,
      boolean sentenceTitleNoisy) {
    if (isHolidayWordInPhrase(curLow, prev, next)) {
      return true;
    }
    if (isPostNominalTitle(curLow, prev, sentenceAllCaps, sentenceTitleNoisy)) {
      return true;
    }
    if (isGivenNameBeforeTitleOrSurname(curLow, next, sentenceAllCaps, sentenceTitleNoisy)) {
      return true;
    }
    return NormV2Types.isPersonalInitial(curLow)
        && hasInitialSurnameChainAhead(tokens, idx, sentenceAllCaps, sentenceTitleNoisy);
  }

  private static boolean isHolidayWordInPhrase(String curLow, SpanToken prev, SpanToken next) {
    if (TDK_HOLIDAY_MODIFIERS_EXT.contains(curLow) && next != null) {
      return isHolidayHead(next.lower());
    }
    return isHolidayHead(curLow)
        && prev != null
        && TDK_HOLIDAY_MODIFIERS_EXT.contains(prev.lower());
  }

  private static boolean isHolidayHead(String wordLower) {
    return wordLower.startsWith("günü") || wordLower.startsWith("bayramı");
  }

  private boolean isPostNominalTitle(
      String curLow, SpanToken prev, boolean sentenceAllCaps, boolean sentenceTitleNoisy) {
    boolean isTitleHonorific =
        curLow.equals("hanım")
            || curLow.startsWith("hanım'")
            || curLow.equals("bey")
            || curLow.startsWith("bey'");
    if (!isTitleHonorific || prev == null || prev.isImmutable()) {
      return false;
    }
    return (!sentenceAllCaps && !sentenceTitleNoisy && prev.shape() == CasingShape.TITLE)
        || morph.isStrictProperOrAbbrOnly(prev.lower());
  }

  private boolean isGivenNameBeforeTitleOrSurname(
      String curLow, SpanToken next, boolean sentenceAllCaps, boolean sentenceTitleNoisy) {
    if (!GIVEN_NAME_HOMOGRAPHS.contains(curLow) || next == null || next.isImmutable()) {
      return false;
    }
    String nLow = next.lower();
    if (nLow.equals("bey") || nLow.equals("hanım")) {
      return true;
    }
    return !sentenceAllCaps
        && !sentenceTitleNoisy
        && next.shape() == CasingShape.TITLE
        && isLikelyProperSurname(next);
  }

  private static boolean isCalendarDayOrYearNumber(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    int start = 0;
    while (start < s.length() && (s.charAt(start) == '(' || s.charAt(start) == '[')) {
      start++;
    }
    int end = start;
    while (end < s.length() && Character.isDigit(s.charAt(end))) {
      end++;
    }
    if (end == start || end - start > 4) {
      return false;
    }
    int v = Integer.parseInt(s.substring(start, end));
    if (!((v >= 1 && v <= 31) || (v >= 1850 && v <= 2099))) {
      return false;
    }
    if (end == s.length() || (end == s.length() - 1 && s.charAt(end) == '.')) {
      return true;
    }
    char sep = s.charAt(end);
    return (sep == '-' || sep == '/')
        && end + 1 < s.length()
        && Character.isDigit(s.charAt(end + 1));
  }

  private static boolean isCalendarYearNumber(String s) {
    if (s == null || s.length() < 4) {
      return false;
    }
    String clean =
        (s.endsWith(")") || s.endsWith("]") || s.endsWith("."))
            ? s.substring(0, s.length() - 1)
            : s;
    int ap = Math.max(clean.indexOf('\''), clean.indexOf('’'));
    if (ap == 4) {
      clean = clean.substring(0, 4);
    }
    if (clean.length() != 4 || !NormV2Types.isAllDigits(clean)) {
      return false;
    }
    int v = Integer.parseInt(clean);
    return v >= 1850 && v <= 2099;
  }

  private static boolean isGazetteerOrHonorificHead(
      SpanToken nt, boolean sentenceAllCaps, boolean sentenceTitleNoisy) {
    String low = nt.lower();
    if (DOTTED_GAZETTEER_OR_HONORIFIC.contains(low)) {
      return true;
    }
    if (!sentenceAllCaps && !sentenceTitleNoisy && nt.shape() == CasingShape.TITLE) {
      int ap = Math.max(low.indexOf('\''), low.indexOf('’'));
      String stem = ap > 0 ? low.substring(0, ap) : low;
      return stem.equals("mah") || GAZETTEER_TITLE_HEAD_PREFIXES.contains(stem);
    }
    return false;
  }

  private boolean isLikelyProperSurname(SpanToken nt) {
    String surf = nt.surface();
    if (surf.indexOf('\'') > 0 || surf.indexOf('’') > 0) {
      return true;
    }
    return morph.isStrictProperOrAbbrOnly(nt.lower())
        || mined.isKnownProperOrAcronymStem(nt.lower());
  }

  private boolean hasInitialSurnameChainAhead(
      List<SpanToken> tokens, int idx, boolean sentenceAllCaps, boolean sentenceTitleNoisy) {
    int n = tokens.size();
    for (int j = idx + 1; j < Math.min(n, idx + 4); j++) {
      SpanToken t = tokens.get(j);
      String l = t.lower();
      if (NormV2Types.isPersonalInitial(l)) {
        if (!sentenceAllCaps && t.shape() == CasingShape.TITLE) {
          return true;
        }
        continue;
      }
      return !sentenceAllCaps
          && !sentenceTitleNoisy
          && t.shape() == CasingShape.TITLE
          && (isLikelyProperSurname(t) || l.length() >= 3);
    }
    return false;
  }

  // =========================================================================
  // Character & Weighted Edit-Distance Helpers
  // =========================================================================

  public static boolean hasAsciiAmbiguousChars(String lower) {
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      if (c == 'c' || c == 'g' || c == 'i' || c == 'o' || c == 's' || c == 'u') {
        return true;
      }
    }
    return false;
  }

  public static boolean hasExplicitTurkishDiacritics(String lower) {
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      if (c == 'ç' || c == 'ğ' || c == 'ı' || c == 'ö' || c == 'ş' || c == 'ü') {
        return true;
      }
    }
    return false;
  }

  private static boolean containsForeignWxq(String lower) {
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      if (c == 'w' || c == 'x' || c == 'q') {
        return true;
      }
    }
    return false;
  }

  public static boolean hasRepeatedLetters(String s) {
    return SingleTokenRewriteGenerator.maxRepeatedLetterRun(s) >= 2;
  }

  static int countDiacriticDiffs(String a, String b) {
    if (a.length() != b.length()) {
      return 0;
    }
    int diffs = 0;
    for (int i = 0; i < a.length(); i++) {
      char ca = a.charAt(i);
      char cb = b.charAt(i);
      if (ca != cb
          && TurkishAlphabet.INSTANCE.getAsciiEqual(ca)
              == TurkishAlphabet.INSTANCE.getAsciiEqual(cb)) {
        diffs++;
      }
    }
    return diffs;
  }

  /** Weighted Levenshtein distance (diacritic diff = 0.15, space/' = 0.20, full = 0.85..0.90). */
  public static float computeWeightedEditCost(String a, String b) {
    if (a.equals(b)) {
      return 0.0f;
    }
    int n = a.length();
    int m = b.length();
    if (Math.abs(n - m) > 8) {
      return Math.abs(n - m);
    }
    float[] prev = new float[m + 1];
    float[] curr = new float[m + 1];
    for (int j = 0; j <= m; j++) {
      prev[j] = j * 0.85f;
    }
    for (int i = 1; i <= n; i++) {
      curr[0] = i * 0.85f;
      char ca = a.charAt(i - 1);
      for (int j = 1; j <= m; j++) {
        char cb = b.charAt(j - 1);
        float subCost = charSubstitutionCost(ca, cb);
        float insCost = (cb == '\'' || cb == ' ') ? 0.20f : 0.85f;
        float delCost = (ca == '\'' || (i > 1 && ca == a.charAt(i - 2))) ? 0.20f : 0.85f;
        curr[j] =
            Math.min(prev[j - 1] + subCost, Math.min(curr[j - 1] + insCost, prev[j] + delCost));
      }
      float[] tmp = prev;
      prev = curr;
      curr = tmp;
    }
    return prev[m];
  }

  private static float charSubstitutionCost(char ca, char cb) {
    if (ca == cb) {
      return 0.0f;
    }
    if (TurkishAlphabet.INSTANCE.getAsciiEqual(ca) == TurkishAlphabet.INSTANCE.getAsciiEqual(cb)) {
      return 0.15f;
    }
    return (cb == '\'' || cb == ' ') ? 0.25f : 0.90f;
  }
}
