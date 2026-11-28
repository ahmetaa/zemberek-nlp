package zemberek.normalization.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.LmVocabulary;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.normalization.v2.LatticeMorphologySupport.MorphSummary;
import zemberek.normalization.v2.NormV2Types.CasingShape;
import zemberek.normalization.v2.NormV2Types.SpanArc;
import zemberek.normalization.v2.NormV2Types.SpanToken;
import zemberek.normalization.v2.NormalizationKnowledge.CleanWordLm;
import zemberek.normalization.v2.NormalizationKnowledge.MergeEntry;
import zemberek.normalization.v2.NormalizationKnowledge.TargetCount;
import zemberek.normalization.v2.SpanCandidateGenerator.SlotDraftBuilder;
import zemberek.tokenization.Token;

/**
 * Generates {@code 1:2} clitic/compound split candidates and multi-token {@code 2:1}/{@code 3:1}
 * merge arcs for {@link SpanCandidateGenerator}.
 */
final class SplitMergeSpanGenerator {

  static final Set<String> QUESTION_TAILS =
      Set.of(
          "mı", "mi", "mu", "mü", "mıyım", "miyim", "muyum", "müyüm", "mısın", "misin", "musun",
          "müsün", "mıyız", "miyiz", "muyuz", "müyüz", "mısınız", "misiniz", "musunuz", "müsünüz",
          "mıydı", "miydi", "muydu", "müydü", "mıydım", "miydim", "muydum", "müydüm", "mıydın",
          "miydin", "muydun", "müydün", "mıydık", "miydik", "muyduk", "müydük", "mıydınız",
          "miydiniz", "muydunuz", "müydünüz", "mıymış", "miymiş", "muymuş", "müymüş");

  static final Set<String> LOC_OR_ABL_PARTICLES =
      Set.of("de", "da", "te", "ta", "den", "dan", "ten", "tan");

  private static final Set<String> LOCATIVE_CLITIC_TAILS = Set.of("de", "da", "te", "ta");

  private static final Set<String> LEXICAL_KI_WORDS =
      Set.of(
          "sanki", "oysaki", "mademki", "belki", "halbuki", "çünkü", "meğerki", "illaki",
          "benimki", "seninki", "onunki", "bizimki", "sizinki", "onlarınki", "dünkü", "bugünkü",
          "yarınki", "akşamki", "sabahki", "önceki", "sonraki", "öteki", "beriki", "karşıki",
          "yukarıki", "aşağıki");

  private static final Set<String> NON_SPLITTABLE_LOC_ADVERBS =
      Set.of("işte", "elbette", "adeta", "birlikte");

  private static final Set<String> BLOCKED_LOCATIVE_MERGES =
      Set.of("yada", "bide", "birde", "hemde");

  private static final Set<String> CLOSED_COMPOUND_PREFIXES =
      Set.of(
          "hiçbir", "birkaç", "birçok", "herhangi", "birbiri", "vazgeç", "başvur", "öngör",
          "varsay");

  private static final Set<String> DETACHED_PROPER_NONLOC_SUFFIXES =
      Set.of(
          "ya", "ye", "a", "e", "yı", "yi", "yu", "yü", "nın", "nin", "nun", "nün", "ın", "in",
          "un", "ün", "yla", "yle", "la", "le", "lı", "li", "lu", "lü", "lık", "lik", "luk", "lük",
          "lar", "ler", "ları", "leri", "ların", "lerin", "lisini", "lısını", "lusunu", "lüsünü",
          "cılar", "ciler", "cular", "cüler", "çılar", "çiler", "çular", "çüler");

  private static final Set<String> SPLIT_COPULA_TAILS =
      Set.of(
          "dı", "di", "du", "dü", "tı", "ti", "tu", "tü", "dım", "dim", "dum", "düm", "tım", "tim",
          "tum", "tüm", "dın", "din", "dun", "dün", "tın", "tin", "tun", "tün", "dık", "dik", "duk",
          "dük", "tık", "tik", "tuk", "tük", "mış", "miş", "muş", "müş", "ydı", "ydi", "ydu", "ydü",
          "sa", "se", "ysa", "yse");

  private static final String[] SPLIT_HEAD_PROPER_SUFS = {
    "nın", "nin", "nun", "nün", "ın", "in", "un", "ün", "ya", "ye", "yı", "yi", "yu", "yü", "da",
    "de", "ta", "te", "dan", "den", "tan", "ten", "yla", "yle", "la", "le", "a", "e", "ı", "i", "u",
    "ü"
  };

  private static final String[] SEY_PREFIXES = {
    "hiçbirşey", "hicbirsey", "hiçbişey", "hicbisey",
    "herşey", "hersey",
    "birşey", "birsey", "bişey", "bisey", "bişi", "bisi"
  };

  private static final String[] BIR_YER_DAHA_PREFIXES = {
    "biryer", "biyer", "bıyer", "birdaha", "bidaha", "bıdaha"
  };

  private static final Set<String> QUANTIFIED_COUNT_NOUNS =
      Set.of("gün", "ay", "yıl", "kez", "kere", "defa");

  private static final Set<String> COUNT_MODIFIERS =
      Set.of(
          "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz", "on", "kaç",
          "birkaç");

  private static final Set<String> PARTICIPLE_SUFFIXES =
      Set.of(
          "dığı", "diği", "duğu", "düğü", "tığı", "tiği", "tuğu", "tüğü", "dığımız", "diğimiz",
          "tiğimiz", "tığımız");

  private record SplitHead(String customSurface, String normLower, boolean isProper) {}

  private record StemClassification(boolean isKnownAcronym, boolean isProperOrAcronym) {}

  private final class MergeContext {
    final SpanCandidateGenerator gen;
    final List<SpanToken> tokens;
    final int index;
    final boolean capHere;
    final boolean sentenceAllCaps;
    final int noisyOnlyCtx;
    final List<SpanArc> outArcs;

    MergeContext(
        SpanCandidateGenerator gen,
        List<SpanToken> tokens,
        int index,
        boolean capHere,
        boolean sentenceAllCaps,
        int tokCaseCtx,
        List<SpanArc> outArcs) {
      this.gen = gen;
      this.tokens = tokens;
      this.index = index;
      this.capHere = capHere;
      this.sentenceAllCaps = sentenceAllCaps;
      this.noisyOnlyCtx = tokCaseCtx & NormalizationKnowledge.CTX_SENT_TITLE_NOISY;
      this.outArcs = outArcs;
    }

    SpanToken first() {
      return tokens.get(index);
    }

    String truecaseFromFirst(String normLower) {
      SpanToken t1 = first();
      return knowledge.truecase(
          normLower,
          t1.shape(),
          t1.surface(),
          capHere,
          sentenceAllCaps,
          morph.strictMorphology,
          noisyOnlyCtx);
    }

    SpanArc addArc(
        int spanLen,
        String normSurface,
        String normLower,
        int sourceMask,
        float parallelProb,
        float parallelLogCount,
        float suffixPrecision,
        float editCost,
        int diacriticDiffs) {
      SpanArc arc =
          gen.buildDirectArc(
              index,
              index + spanLen,
              normSurface,
              normLower,
              sourceMask,
              parallelProb,
              parallelLogCount,
              suffixPrecision,
              editCost,
              diacriticDiffs);
      outArcs.add(arc);
      return arc;
    }

    void addRuleArc(
        int spanLen,
        String normSurface,
        String normLower,
        int sourceMask,
        float suffixPrecision,
        float editCost) {
      addArc(spanLen, normSurface, normLower, sourceMask, 0.0f, 0.0f, suffixPrecision, editCost, 0);
    }
  }

  private final NormalizationKnowledge knowledge;
  private final LatticeMorphologySupport morph;
  private final LmVocabulary lmVocab;

  SplitMergeSpanGenerator(
      NormalizationKnowledge knowledge, LatticeMorphologySupport morph, LmVocabulary lmVocab) {
    this.knowledge = knowledge;
    this.morph = morph;
    this.lmVocab = lmVocab;
  }

  // =========================================================================
  // 1:2 Clitic & Open-Compound Splits
  // =========================================================================

  void addAmbiguityPreservingSplits(SlotDraftBuilder slot, boolean strictKnown) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    int len = low.length();
    if (len < 3) {
      return;
    }
    if (len == 3 && !tok.containsDigit()) {
      addShortPronounSplits(slot, low);
      return;
    }

    int ap = low.indexOf('\'');
    if (ap > 0 && ap < len - 1) {
      addApostropheCliticSplits(slot, low.substring(0, ap), low.substring(ap + 1));
      return;
    }
    if (tok.containsDigit() || ap >= 0) {
      return;
    }

    addCamelCaseRunOnSplit(slot);

    boolean hasValidSingleWord = strictKnown || slot.hasValidSingleWordDraft();
    addLocativeCliticSplits(slot, hasValidSingleWord);
    addQuestionCliticSplits(slot);
    addConjunctionKiSplits(slot);
    addEncliticYaSplit(slot, strictKnown);
    addFixedPrefixSplits(slot, strictKnown);
    addCleanBigramCompoundSplits(slot, strictKnown);
    addGluedTwoWordSplit(slot, strictKnown);
  }

  private static void addShortPronounSplits(SlotDraftBuilder slot, String low) {
    if (low.equals("oda")) {
      slot.addTruecased("o da", NormV2Types.SRC_CLITIC_SPLIT, 0.0f, 0.20f, 0);
    } else if (low.equals("omu")) {
      slot.addTruecased("o mu", NormV2Types.SRC_CLITIC_SPLIT, 0.88f, 0.18f, 0);
    }
  }

  /** Handles apostrophized clitic errors like {@code "Ali'de geldi" -> "Ali de"}. */
  private static void addApostropheCliticSplits(
      SlotDraftBuilder slot, String stemLow, String tailLow) {
    boolean numericStem = NormV2Types.isAllDigits(stemLow);
    if (LOCATIVE_CLITIC_TAILS.contains(tailLow)) {
      if (!numericStem) {
        String clitic = harmonizedDeDaClitic(stemLow);
        slot.addTruecased(
            stemLow + " " + clitic,
            NormV2Types.SRC_CLITIC_SPLIT | NormV2Types.SRC_APOSTROPHE_DROP,
            0.0f,
            0.24f,
            0);
      }
      return;
    }
    if (isDoubledLocativeTail(tailLow)) {
      String splitLow = stemLow + "'" + tailLow.substring(0, 2) + " " + tailLow.substring(2);
      String splitSurf = numericStem ? splitLow : slot.truecase(splitLow);
      slot.addRule(splitSurf, splitLow, NormV2Types.SRC_CLITIC_SPLIT, 0.90f, 0.18f, 0);
      return;
    }
    if (QUESTION_TAILS.contains(tailLow) && !numericStem) {
      String qt =
          harmonizeQuestionClitic(tailLow, LatticeMorphologySupport.lastTurkishVowel(stemLow));
      slot.addTruecased(
          stemLow + " " + qt,
          NormV2Types.SRC_CLITIC_SPLIT | NormV2Types.SRC_APOSTROPHE_DROP,
          0.90f,
          0.18f,
          0);
    }
  }

  private static boolean isDoubledLocativeTail(String tailLow) {
    return tailLow.length() == 4
        && (tailLow.endsWith("de") || tailLow.endsWith("da"))
        && LOCATIVE_CLITIC_TAILS.contains(tailLow.substring(0, 2));
  }

  /** Splits glued CamelCase words like {@code "DindarıSolcusu" -> "Dindarı Solcusu"}. */
  private void addCamelCaseRunOnSplit(SlotDraftBuilder slot) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    int len = low.length();
    if (tok.shape() != CasingShape.MIXED
        || len < 6
        || len > 24
        || knowledge.isKnownBrandMixedCase(low)) {
      return;
    }
    int camelCut = findSingleCamelBoundary(tok.surface());
    if (camelCut < 3 || camelCut > len - 3) {
      return;
    }
    String rawW1 = low.substring(0, camelCut);
    String rawW2 = low.substring(camelCut);
    String realW1 = realizeValidOrAttestedStem(rawW1);
    String realW2 = realizeValidOrAttestedStem(rawW2);
    if (realW1 == null
        || realW2 == null
        || LatticeMorphologySupport.stripsExplicitStemDiacritic(rawW1, realW1)
        || LatticeMorphologySupport.stripsExplicitStemDiacritic(rawW2, realW2)) {
      return;
    }
    String surfW1 =
        Character.isUpperCase(tok.surface().charAt(0))
            ? NormV2Types.trCapitalize(realW1)
            : realW1;
    String surfW2 =
        knowledge.truecase(
            realW2,
            CasingShape.TITLE,
            tok.surface().substring(camelCut),
            false,
            slot.sentenceAllCaps,
            morph.strictMorphology);
    int diacDiffs =
        SpanCandidateGenerator.countDiacriticDiffs(rawW1, realW1)
            + SpanCandidateGenerator.countDiacriticDiffs(rawW2, realW2);
    slot.addRule(
        surfW1 + " " + surfW2,
        realW1 + " " + realW2,
        NormV2Types.SRC_CLITIC_SPLIT,
        0.90f,
        0.18f,
        diacDiffs);
  }

  /** Splits glued {@code de/da} clitics ({@code "bende" -> "ben de"}). */
  private void addLocativeCliticSplits(SlotDraftBuilder slot, boolean hasValidSingleWord) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    if (!endsWithLocativeClitic(low) || NON_SPLITTABLE_LOC_ADVERBS.contains(low)) {
      return;
    }
    String rawHead = low.substring(0, low.length() - 2);
    if (rawHead.length() < 2) {
      return;
    }
    boolean unvoicedTail = low.endsWith("te") || low.endsWith("ta");
    String queryHead = queryForAsciiUpperI(tok, rawHead);
    for (SplitHead sh : resolveSplitHeads(queryHead, rawHead, slot)) {
      String headReal = sh.normLower();
      String clitic = harmonizedDeDaClitic(headReal);
      String splitLow = headReal + " " + clitic;
      String splitSurf = formatSplitSurface(slot, sh, clitic, splitLow);
      MorphSummary headMs = morph.summarizeMorph(headReal);
      int dDiffs = SpanCandidateGenerator.countDiacriticDiffs(rawHead, headReal);
      int srcMask =
          NormV2Types.SRC_CLITIC_SPLIT
              | (dDiffs > 0 ? NormV2Types.SRC_DEASCIIFIER : 0)
              | (sh.isProper() ? NormV2Types.SRC_OOV_APOSTROPHE : 0);
      float splitPrec =
          headMs.syntaxClass() == NormV2Types.HEAD_SYNTAX_FINITE_OR_CONVERB
              ? 0.92f
              : ((sh.isProper() || !hasValidSingleWord) ? 0.80f : 0.0f);
      slot.addRule(splitSurf, splitLow, srcMask, splitPrec, unvoicedTail ? 0.26f : 0.22f, dDiffs);
    }
  }

  /** Splits glued question clitics ({@code "olurmu" -> "olur mu"}). */
  private void addQuestionCliticSplits(SlotDraftBuilder slot) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    int len = low.length();
    for (String qt : QUESTION_TAILS) {
      if (len < qt.length() + 2
          || (!low.endsWith(qt) && !tok.ascii().endsWith(NormV2Types.toAscii(qt)))) {
        continue;
      }
      String rawHead = low.substring(0, len - qt.length());
      String queryHead = queryForAsciiUpperI(tok, rawHead);
      List<SplitHead> qHeads = resolveSplitHeads(queryHead, rawHead, slot);
      if (qHeads.isEmpty()) {
        continue;
      }
      boolean emittedAny = false;
      for (SplitHead sh : qHeads) {
        String headReal = sh.normLower();
        if (!sh.isProper()
            && (morph.isBareImperativeVerbOnly(headReal)
                || (qt.length() >= 5 && headReal.equals("al")))) {
          continue;
        }
        char dictHv = morph.expectedTurkishQuestionVowel(headReal);
        char vowel = dictHv != 0 ? dictHv : LatticeMorphologySupport.lastTurkishVowel(headReal);
        String harmonizedQt = harmonizeQuestionClitic(qt, vowel);
        String splitLow = headReal + " " + harmonizedQt;
        String splitSurf = formatSplitSurface(slot, sh, harmonizedQt, splitLow);
        int dDiffs = SpanCandidateGenerator.countDiacriticDiffs(rawHead, headReal);
        int srcMask =
            NormV2Types.SRC_CLITIC_SPLIT
                | (dDiffs > 0 ? NormV2Types.SRC_DEASCIIFIER : 0)
                | (sh.isProper() ? NormV2Types.SRC_OOV_APOSTROPHE : 0);
        slot.addRule(splitSurf, splitLow, srcMask, 0.88f, 0.18f, dDiffs);
        emittedAny = true;
      }
      if (emittedAny) {
        return;
      }
    }
  }

  /** Splits glued conjunction {@code ki} outside lexical {@code sanki/belki/...} and {@code -daki}. */
  private void addConjunctionKiSplits(SlotDraftBuilder slot) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    if (low.length() < 4
        || (!low.endsWith("ki") && !low.endsWith("kı"))
        || LEXICAL_KI_WORDS.contains(low)
        || endsWithLocativeKi(low)) {
      return;
    }
    String rawHead = low.substring(0, low.length() - 2);
    String queryHead = queryForAsciiUpperI(tok, rawHead);
    for (SplitHead sh : resolveSplitHeads(queryHead, rawHead, slot)) {
      String headReal = sh.normLower();
      if (LEXICAL_KI_WORDS.contains(headReal + "ki")) {
        continue;
      }
      String splitLow = headReal + " ki";
      String splitSurf = formatSplitSurface(slot, sh, "ki", splitLow);
      int dDiffs = SpanCandidateGenerator.countDiacriticDiffs(rawHead, headReal);
      int srcMask = NormV2Types.SRC_CLITIC_SPLIT | (dDiffs > 0 ? NormV2Types.SRC_DEASCIIFIER : 0);
      slot.addRule(splitSurf, splitLow, srcMask, 0.82f, 0.22f, dDiffs);
    }
  }

  /** Splits finite-verb + enclitic {@code ya} ({@code "uzaklasiyoruzya" -> "uzaklaşıyoruz ya"}). */
  private void addEncliticYaSplit(SlotDraftBuilder slot, boolean strictKnown) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    if (strictKnown || low.length() < 5 || !low.endsWith("ya")) {
      return;
    }
    String rawHead = low.substring(0, low.length() - 2);
    String queryHead = queryForAsciiUpperI(tok, rawHead);
    String headReal = morph.realizeValidWordOrNull(queryHead);
    if (headReal == null
        || LatticeMorphologySupport.stripsExplicitStemDiacritic(rawHead, headReal)
        || morph.summarizeMorph(headReal).syntaxClass()
            != NormV2Types.HEAD_SYNTAX_FINITE_OR_CONVERB) {
      return;
    }
    int dDiffs = SpanCandidateGenerator.countDiacriticDiffs(rawHead, headReal);
    slot.addTruecased(
        headReal + " ya",
        NormV2Types.SRC_CLITIC_SPLIT | (dDiffs > 0 ? NormV2Types.SRC_DEASCIIFIER : 0),
        0.88f,
        0.20f,
        dDiffs);
  }

  /** Handles {@code bir/her/hiçbir şey*}, {@code bir yer/daha*}, {@code nap/nol*}, and {@code kimo}. */
  private void addFixedPrefixSplits(SlotDraftBuilder slot, boolean strictKnown) {
    if (strictKnown) {
      return;
    }
    String low = slot.tok.lower();
    if (low.length() >= 5) {
      addSeyPrefixSplit(slot, low);
      addBirYerDahaSplit(slot, low);
      addNeYapOlSplit(slot, low);
    }
    if (low.equals("acabao") || low.equals("kimo") || low.equals("nediro")) {
      slot.addTruecased(
          low.substring(0, low.length() - 1) + " o",
          NormV2Types.SRC_CLITIC_SPLIT,
          0.92f,
          0.18f,
          0);
    }
  }

  private void addSeyPrefixSplit(SlotDraftBuilder slot, String low) {
    for (String pref : SEY_PREFIXES) {
      if (!low.startsWith(pref)) {
        continue;
      }
      String tail = low.substring(pref.length());
      if ((pref.equals("bişi") || pref.equals("bisi")) && tail.isEmpty()) {
        continue;
      }
      String seyReal = tail.isEmpty() ? "şey" : morph.realizeValidWordOrNull("şey" + tail);
      if (seyReal != null && seyReal.startsWith("şey")) {
        String det = determinerForSeyPrefix(pref);
        slot.addTruecased(
            det + " " + seyReal,
            NormV2Types.SRC_CLITIC_SPLIT,
            0.90f,
            0.22f,
            SpanCandidateGenerator.countDiacriticDiffs("şey" + tail, seyReal));
        return;
      }
    }
  }

  private static String determinerForSeyPrefix(String prefix) {
    if (prefix.startsWith("hiç") || prefix.startsWith("hic")) {
      return "hiçbir";
    }
    return prefix.startsWith("her") ? "her" : "bir";
  }

  private void addBirYerDahaSplit(SlotDraftBuilder slot, String low) {
    for (String bp : BIR_YER_DAHA_PREFIXES) {
      if (!low.startsWith(bp)) {
        continue;
      }
      String baseHead = bp.endsWith("yer") ? "yer" : "daha";
      String tail = low.substring(bp.length());
      String secondReal =
          tail.isEmpty() ? baseHead : morph.realizeValidWordOrNull(baseHead + tail);
      if (secondReal != null && secondReal.startsWith(baseHead)) {
        slot.addTruecased(
            "bir " + secondReal,
            NormV2Types.SRC_CLITIC_SPLIT,
            0.88f,
            0.22f,
            SpanCandidateGenerator.countDiacriticDiffs(baseHead + tail, secondReal));
        return;
      }
    }
  }

  private void addNeYapOlSplit(SlotDraftBuilder slot, String low) {
    if (!low.startsWith("nap") && !low.startsWith("nol")) {
      return;
    }
    String verbBase = low.startsWith("nap") ? "yap" : "ol";
    String tail = low.substring(3);
    String verbReal = morph.realizeValidWordOrNull(verbBase + tail);
    if (verbReal != null && verbReal.startsWith(verbBase)) {
      slot.addTruecased(
          "ne " + verbReal,
          NormV2Types.SRC_CLITIC_SPLIT | NormV2Types.SRC_INFORMAL_MORPH,
          0.92f,
          0.22f,
          SpanCandidateGenerator.countDiacriticDiffs(verbBase + tail, verbReal));
    }
  }

  /** Splits open compounds attested as clean bigrams ({@code "haftasonunda" -> "hafta sonunda"}). */
  private void addCleanBigramCompoundSplits(SlotDraftBuilder slot, boolean strictKnown) {
    String low = slot.tok.lower();
    int len = low.length();
    if (len < 5 || len > 22) {
      return;
    }
    int closedFreq = knowledge.cleanLm.unigramCount(low);
    for (int cut = 2; cut <= len - 2; cut++) {
      String w1 = low.substring(0, cut);
      String w2 = low.substring(cut);
      int bgCnt = knowledge.cleanLm.bigramCount(w1, w2);
      if (bgCnt >= 10 && (!strictKnown || bgCnt >= 80 || bgCnt > closedFreq * 3)) {
        String splitLow = w1 + " " + w2;
        slot.add(
            slot.truecase(splitLow),
            splitLow,
            NormV2Types.SRC_CLITIC_SPLIT,
            0.0f,
            (float) Math.log1p(bgCnt),
            0.80f,
            0.25f,
            0);
      }
    }
  }

  /** Splits missing-space / deasciified OOV glued words using clean bigram scoring. */
  private void addGluedTwoWordSplit(SlotDraftBuilder slot, boolean strictKnown) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    int len = low.length();
    CleanWordLm cleanLm = knowledge.cleanLm;
    if (strictKnown || len < 7 || len > 24 || slot.hasLowCostMorphDraft() || cleanLm == null) {
      return;
    }
    String bestSplitLow = null;
    int bestDiacDiffs = 0;
    float bestBigram = -100.0f;
    String queryFull = queryForAsciiUpperI(tok, low);
    for (int cut = 3; cut <= len - 3; cut++) {
      String rawSlice1 = low.substring(0, cut);
      String rawSlice2 = low.substring(cut);
      String w1 = resolveGluedFirstWord(queryFull.substring(0, cut), rawSlice1, cleanLm);
      if (w1 == null) {
        continue;
      }
      String w2 = resolveGluedSecondWord(queryFull.substring(cut), rawSlice2, w1, cleanLm);
      if (w2 == null) {
        continue;
      }
      float bg = cleanLm.bigramLogProb(w1, w2);
      if (bg > bestBigram) {
        bestBigram = bg;
        bestSplitLow = w1 + " " + w2;
        bestDiacDiffs =
            SpanCandidateGenerator.countDiacriticDiffs(rawSlice1, w1)
                + SpanCandidateGenerator.countDiacriticDiffs(rawSlice2, w2);
      }
    }
    if (bestSplitLow != null && bestBigram > -11.5f) {
      slot.addTruecased(
          bestSplitLow,
          NormV2Types.SRC_CLITIC_SPLIT | (bestDiacDiffs > 0 ? NormV2Types.SRC_DEASCIIFIER : 0),
          bestBigram > -6.5f ? 0.84f : 0.72f,
          0.28f,
          bestDiacDiffs);
    }
  }

  private String resolveGluedFirstWord(String query1, String rawSlice1, CleanWordLm cleanLm) {
    if (morph.hasRegularStrictAnalysis(query1) || cleanLm.unigramCount(query1) >= 20) {
      return query1;
    }
    if (query1.length() <= 9 && SpanCandidateGenerator.hasAsciiAmbiguousChars(query1)) {
      String r1 = morph.realizeValidWordOrNull(query1);
      if (r1 != null
          && !LatticeMorphologySupport.stripsExplicitStemDiacritic(rawSlice1, r1)
          && cleanLm.unigramCount(r1) >= 40) {
        return r1;
      }
    }
    return null;
  }

  private String resolveGluedSecondWord(
      String query2, String rawSlice2, String w1, CleanWordLm cleanLm) {
    if (morph.hasRegularStrictAnalysis(query2) || cleanLm.unigramCount(query2) >= 20) {
      return query2;
    }
    if (cleanLm.unigramCount(w1) >= 40 && SpanCandidateGenerator.hasAsciiAmbiguousChars(query2)) {
      String r2 = morph.realizeValidWordOrNull(query2);
      if (r2 != null && !LatticeMorphologySupport.stripsExplicitStemDiacritic(rawSlice2, r2)) {
        return r2;
      }
    }
    return null;
  }

  // =========================================================================
  // Split-Head Resolution Helpers
  // =========================================================================

  private List<SplitHead> resolveSplitHeads(
      String queryHead, String rawHead, SlotDraftBuilder slot) {
    List<SplitHead> out = new ArrayList<>(3);
    boolean strictReg = morph.hasRegularStrictAnalysis(queryHead);
    if (strictReg && !LatticeMorphologySupport.stripsExplicitStemDiacritic(rawHead, queryHead)) {
      out.add(new SplitHead(null, queryHead, false));
    }
    if (!strictReg) {
      String exactInf = morph.firstExactInformalOrNull(queryHead, rawHead);
      if (exactInf != null) {
        addUniqueSplitHead(out, new SplitHead(null, exactInf, false));
      }
    }
    if (!strictReg || isShortPureAmbiguousAscii(queryHead)) {
      collectAsciiSplitHeads(queryHead, rawHead, slot, out);
    }
    if (out.isEmpty() && queryHead.length() >= 4) {
      SplitHead oovProper = findOovProperSplitHead(queryHead, slot);
      if (oovProper != null) {
        addUniqueSplitHead(out, oovProper);
      }
    }
    if (queryHead.indexOf('x') >= 0 || queryHead.indexOf('w') >= 0) {
      String adapted =
          morph.realizeValidWordOrNull(queryHead.replace("x", "ks").replace("w", "v"));
      if (adapted != null) {
        addUniqueSplitHead(out, new SplitHead(null, adapted, false));
      }
    }
    if (out.isEmpty() && isAttestedFallbackHead(queryHead)) {
      addUniqueSplitHead(out, new SplitHead(null, queryHead, false));
    }
    return out;
  }

  private void collectAsciiSplitHeads(
      String queryHead, String rawHead, SlotDraftBuilder slot, List<SplitHead> out) {
    List<String> regCands = new ArrayList<>(4);
    SplitHead[] bestProper = new SplitHead[1];
    morph.forEachAsciiRealization(
        queryHead,
        rawHead,
        (a, nl, informal) -> {
          DictionaryItem item = a.getDictionaryItem();
          if (item.isAbbreviation()) {
            return;
          }
          if (!item.isProperNoun()) {
            if (!regCands.contains(nl)) {
              regCands.add(nl);
            }
          } else if (!informal && bestProper[0] == null) {
            String fmt = LatticeMorphologySupport.formatProperOrAbbrTDK(a, nl);
            if (fmt != null && !fmt.isEmpty()) {
              String surf = slot.sentenceAllCaps ? NormV2Types.trUpper(fmt) : fmt;
              bestProper[0] = new SplitHead(surf, NormV2Types.trLower(fmt), true);
            }
          }
        });
    CleanWordLm cleanLm = knowledge.cleanLm;
    if (regCands.size() > 1 && cleanLm != null) {
      regCands.sort((x, y) -> Float.compare(cleanLm.unigramLogProb(y), cleanLm.unigramLogProb(x)));
    }
    int addedReg = 0;
    for (String rc : regCands) {
      if (addUniqueSplitHead(out, new SplitHead(null, rc, false)) && ++addedReg >= 2) {
        break;
      }
    }
    if (bestProper[0] != null && out.size() < 3) {
      addUniqueSplitHead(out, bestProper[0]);
    }
  }

  private SplitHead findOovProperSplitHead(String queryHead, SlotDraftBuilder slot) {
    for (String suf : SPLIT_HEAD_PROPER_SUFS) {
      if (queryHead.length() - suf.length() < 2 || !queryHead.endsWith(suf)) {
        continue;
      }
      String stemLow = queryHead.substring(0, queryHead.length() - suf.length());
      boolean isAcronym = knowledge.isKnownAcronymStem(stemLow);
      if (isAcronym || knowledge.isKnownProperOrAcronymStem(stemLow)) {
        String candLow = stemLow + "'" + suf;
        String candSurf =
            isAcronym ? NormV2Types.trUpper(stemLow) + "'" + suf : slot.truecase(candLow);
        return new SplitHead(candSurf, candLow, true);
      }
    }
    return null;
  }

  private boolean isAttestedFallbackHead(String queryHead) {
    return queryHead.length() >= 3
        && ((knowledge.cleanLm != null && knowledge.cleanLm.unigramCount(queryHead) >= 4)
            || (lmVocab != null && lmVocab.contains(queryHead))
            || knowledge.isKnownProperOrAcronymStem(queryHead));
  }

  private static boolean isShortPureAmbiguousAscii(String wordLower) {
    return wordLower.length() <= 6
        && !SpanCandidateGenerator.hasExplicitTurkishDiacritics(wordLower)
        && SpanCandidateGenerator.hasAsciiAmbiguousChars(wordLower);
  }

  private static String formatSplitSurface(
      SlotDraftBuilder slot, SplitHead head, String cliticLower, String splitLower) {
    if (head.customSurface() == null) {
      return slot.truecase(splitLower);
    }
    String tailSurf = slot.sentenceAllCaps ? NormV2Types.trUpper(cliticLower) : cliticLower;
    return head.customSurface() + " " + tailSurf;
  }

  private static boolean addUniqueSplitHead(List<SplitHead> list, SplitHead cand) {
    for (SplitHead sh : list) {
      if (sh.normLower().equals(cand.normLower())) {
        return false;
      }
    }
    list.add(cand);
    return true;
  }

  private String realizeValidOrAttestedStem(String rawHead) {
    String real = morph.realizeValidWordOrNull(rawHead);
    if (real != null) {
      return real;
    }
    if (rawHead.length() >= 3
        && ((knowledge.cleanLm != null && knowledge.cleanLm.unigramCount(rawHead) >= 4)
            || (lmVocab != null && lmVocab.contains(rawHead)))) {
      return rawHead;
    }
    return null;
  }

  // =========================================================================
  // 2:1 and 3:1 Multi-Token Merges
  // =========================================================================

  void addMultiTokenMerges(
      SpanCandidateGenerator gen,
      List<SpanToken> tokens,
      int i,
      boolean capHere,
      boolean sentenceAllCaps,
      int tokCaseCtx,
      List<SpanArc> outArcs) {
    int n = tokens.size();
    MergeContext ctx =
        new MergeContext(gen, tokens, i, capHere, sentenceAllCaps, tokCaseCtx, outArcs);

    if (i + 2 < n) {
      addThreeTokenMerges(ctx);
    }
    if (i + 1 >= n) {
      return;
    }

    SpanToken t1 = tokens.get(i);
    SpanToken t2 = tokens.get(i + 1);
    boolean orphansAdjacentThird = isOrphaningAdjacentThirdToken(tokens, i);
    boolean syntacticallyBlocked =
        isSyntacticallyBlockedMerge(tokens, i, t1.lower(), t2.lower());

    if (!orphansAdjacentThird && !syntacticallyBlocked) {
      addTwoTokenParallelMerges(ctx, t1, t2);
    }
    addDetachedNumeralTwoTokenMerge(ctx, t1, t2);
    addDetachedWordTwoTokenMerges(ctx, t1, t2, syntacticallyBlocked);
  }

  private void addThreeTokenMerges(MergeContext ctx) {
    SpanToken t1 = ctx.tokens.get(ctx.index);
    SpanToken t2 = ctx.tokens.get(ctx.index + 1);
    SpanToken t3 = ctx.tokens.get(ctx.index + 2);

    MergeEntry me3 =
        knowledge.parallel3To1.get(t1.lower() + "\t" + t2.lower() + "\t" + t3.lower());
    if (me3 != null) {
      for (TargetCount tc : me3.mergedTargets()) {
        ctx.addArc(
            3,
            ctx.truecaseFromFirst(tc.normLower()),
            tc.normLower(),
            NormV2Types.SRC_PARALLEL_MERGE,
            me3.mergeProb(tc),
            (float) Math.log1p(tc.count()),
            0.0f,
            0.20f,
            0);
      }
    }

    if (("'".equals(t2.surface()) || "’".equals(t2.surface()))
        && !t1.isImmutable()
        && LatticeMorphologySupport.DETACHED_WORD_APOSTROPHE_SUFFIXES.contains(t3.lower())) {
      String joinedLow = t1.lower() + "'" + t3.lower();
      String joinedSurf =
          t1.type() == Token.Type.Number
              ? t1.surface() + "'" + t3.lower()
              : ctx.truecaseFromFirst(joinedLow);
      ctx.addRuleArc(
          3,
          joinedSurf,
          joinedLow,
          NormV2Types.SRC_NUMERAL_JOIN | NormV2Types.SRC_OOV_APOSTROPHE,
          0.92f,
          0.12f);
    }

    if (".".equals(t2.surface())
        && NormV2Types.isAllDigits(t1.surface())
        && LatticeMorphologySupport.DETACHED_NUM_SUFFIXES.contains(t3.lower())) {
      String ord = SingleTokenRewriteGenerator.buildOrdinalInflected(t1.surface(), t3.lower());
      if (ord != null) {
        ctx.addRuleArc(3, ord, ord, NormV2Types.SRC_NUMERAL_JOIN, 0.90f, 0.15f);
      }
    }

    if ("$".equals(t2.surface())
        && t2.adjacentToPrev()
        && t3.adjacentToPrev()
        && !t1.isImmutable()
        && !t3.isImmutable()) {
      addDollarLeetThreeTokenMerge(ctx, t1.lower() + "ş" + t3.lower());
    }
  }

  private void addDollarLeetThreeTokenMerge(MergeContext ctx, String leetJoined) {
    String real = morph.realizeValidWordOrNull(leetJoined);
    if (real != null) {
      ctx.addRuleArc(
          3, ctx.truecaseFromFirst(real), real, NormV2Types.SRC_COMPOUND_MERGE, 0.90f, 0.20f);
      return;
    }
    for (String qt : QUESTION_TAILS) {
      if (leetJoined.length() <= qt.length() + 2 || !leetJoined.endsWith(qt)) {
        continue;
      }
      String headReal =
          morph.realizeValidWordOrNull(leetJoined.substring(0, leetJoined.length() - qt.length()));
      if (headReal != null) {
        String splitLow =
            headReal
                + " "
                + harmonizeQuestionClitic(qt, LatticeMorphologySupport.lastTurkishVowel(headReal));
        ctx.addRuleArc(
            3,
            ctx.truecaseFromFirst(splitLow),
            splitLow,
            NormV2Types.SRC_COMPOUND_MERGE | NormV2Types.SRC_CLITIC_SPLIT,
            0.90f,
            0.20f);
        return;
      }
    }
  }

  private static boolean isOrphaningAdjacentThirdToken(List<SpanToken> tokens, int i) {
    if (i + 2 >= tokens.size()) {
      return false;
    }
    String sep = tokens.get(i + 1).surface();
    if (!"$".equals(sep) && !"'".equals(sep) && !"’".equals(sep)) {
      return false;
    }
    SpanToken t3 = tokens.get(i + 2);
    return t3.adjacentToPrev()
        || LatticeMorphologySupport.DETACHED_WORD_APOSTROPHE_SUFFIXES.contains(t3.lower());
  }

  private void addTwoTokenParallelMerges(MergeContext ctx, SpanToken t1, SpanToken t2) {
    MergeEntry me2 = knowledge.parallel2To1.get(t1.lower() + "\t" + t2.lower());
    if (me2 == null) {
      return;
    }
    int rawCooccur = Math.max(me2.cooccurCount(), me2.totalMergedCount());
    for (TargetCount tc : me2.mergedTargets()) {
      SpanArc arc =
          ctx.addArc(
              2,
              ctx.truecaseFromFirst(tc.normLower()),
              tc.normLower(),
              NormV2Types.SRC_PARALLEL_MERGE,
              me2.mergeProb(tc),
              (float) Math.log1p(tc.count()),
              0.0f,
              0.15f,
              0);
      arc.rawParallelTotal = rawCooccur;
    }
  }

  private static void addDetachedNumeralTwoTokenMerge(
      MergeContext ctx, SpanToken t1, SpanToken t2) {
    if (!LatticeMorphologySupport.DETACHED_NUM_SUFFIXES.contains(t2.lower())) {
      return;
    }
    if (t1.type() == Token.Type.Number) {
      String numSurf = t1.surface();
      if (numSurf.endsWith(".") && numSurf.length() > 1) {
        String ord =
            SingleTokenRewriteGenerator.buildOrdinalInflected(
                numSurf.substring(0, numSurf.length() - 1), t2.lower());
        if (ord != null) {
          ctx.addRuleArc(2, ord, ord, NormV2Types.SRC_NUMERAL_JOIN, 0.90f, 0.15f);
        }
      } else if (isNumericSurface(numSurf)) {
        String joined = numSurf + "'" + t2.lower();
        ctx.addRuleArc(2, joined, joined, NormV2Types.SRC_NUMERAL_JOIN, 0.85f, 0.15f);
      }
      return;
    }
    if (!t1.isImmutable() && t1.containsDigit() && t1.surface().length() <= 6) {
      String stemSurf =
          isLettersThenDigits(t1.lower()) ? NormV2Types.trUpper(t1.lower()) : t1.surface();
      ctx.addRuleArc(
          2,
          stemSurf + "'" + t2.lower(),
          t1.lower() + "'" + t2.lower(),
          NormV2Types.SRC_NUMERAL_JOIN | NormV2Types.SRC_OOV_APOSTROPHE,
          0.84f,
          0.16f);
    }
  }

  private void addDetachedWordTwoTokenMerges(
      MergeContext ctx, SpanToken t1, SpanToken t2, boolean syntacticallyBlocked) {
    if (t1.isImmutable()
        || t2.isImmutable()
        || t2.adjacentToPrev()
        || t1.containsDigit()
        || t2.containsDigit()) {
      return;
    }
    String t2Low = t2.lower();
    StemClassification stemInfo = classifyFirstTokenStem(t1, ctx.capHere, ctx.sentenceAllCaps);

    boolean isLocOrAbl = LOC_OR_ABL_PARTICLES.contains(t2Low);
    if (isLocOrAbl && t1.lower().length() >= 2) {
      addDetachedLocativeOrAblativeMerges(ctx, t1, t2Low, stemInfo);
    }
    if ((t2Low.equals("ki") || t2Low.equals("kü") || t2Low.equals("kı"))
        && t1.lower().length() >= 3) {
      addDetachedRelativizerKiMerge(ctx, t1, t2Low);
    }
    if (!isLocOrAbl && !t2Low.equals("ki") && !t2Low.equals("kü")) {
      addClosedCompoundOrCopulaMerge(ctx, t1, t2, syntacticallyBlocked);
      addDetachedProperSuffixMerge(ctx, t1, t2Low, stemInfo);
    }
  }

  private StemClassification classifyFirstTokenStem(
      SpanToken t1, boolean capHere, boolean sentenceAllCaps) {
    String low = t1.lower();
    boolean isShortUpperAcronym =
        t1.shape() == CasingShape.UPPER
            && !sentenceAllCaps
            && t1.surface().length() >= 2
            && t1.surface().length() <= 5
            && !morph.hasRegularStrictAnalysis(low);
    boolean isKnownAcronym = knowledge.isKnownAcronymStem(low) || isShortUpperAcronym;
    boolean isProperOrAcronym =
        isKnownAcronym
            || knowledge.isKnownProperOrAcronymStem(low)
            || morph.isStrictProperOrAbbrOnly(low)
            || (!capHere
                && t1.shape() == CasingShape.TITLE
                && !morph.hasRegularStrictAnalysis(low));
    return new StemClassification(isKnownAcronym, isProperOrAcronym);
  }

  private void addDetachedLocativeOrAblativeMerges(
      MergeContext ctx, SpanToken t1, String t2Low, StemClassification stemInfo) {
    String stemReal = realizeOrKeep(t1.lower());
    String candMerged = stemReal + assimilateLocOrAblSuffix(stemReal, t2Low.length() == 3);
    int closedCnt = knowledge.cleanLm.unigramCount(candMerged);
    boolean isInfinitiveProg =
        t2Low.length() == 2 && (stemReal.endsWith("mak") || stemReal.endsWith("mek"));

    if (morph.hasRegularStrictAnalysis(candMerged)
        && !BLOCKED_LOCATIVE_MERGES.contains(candMerged)
        && (isInfinitiveProg || closedCnt >= 8)) {
      ctx.addArc(
          2,
          ctx.truecaseFromFirst(candMerged),
          candMerged,
          NormV2Types.SRC_COMPOUND_MERGE,
          0.0f,
          closedCnt > 0 ? (float) Math.log1p(closedCnt) : 0.0f,
          isInfinitiveProg ? 0.72f : 0.58f,
          0.20f,
          SpanCandidateGenerator.countDiacriticDiffs(t1.lower() + t2Low, candMerged));
    }

    if (stemInfo.isProperOrAcronym()) {
      String propSuf = assimilateLocOrAblSuffix(t1.lower(), t2Low.length() == 3);
      String apLow = t1.lower() + "'" + propSuf;
      String apSurf =
          stemInfo.isKnownAcronym()
              ? NormV2Types.trUpper(t1.lower()) + "'" + propSuf
              : ctx.truecaseFromFirst(apLow);
      ctx.addRuleArc(
          2,
          apSurf,
          apLow,
          NormV2Types.SRC_OOV_APOSTROPHE | NormV2Types.SRC_COMPOUND_MERGE,
          0.76f,
          0.18f);
    }
  }

  private void addDetachedRelativizerKiMerge(MergeContext ctx, SpanToken t1, String t2Low) {
    String stemReal = realizeOrKeep(t1.lower());
    for (String kiVar : new String[] {stemReal + "ki", stemReal + "kü"}) {
      if (LEXICAL_KI_WORDS.contains(kiVar) || morph.hasRegularStrictAnalysis(kiVar)) {
        int closedCnt = knowledge.cleanLm.unigramCount(kiVar);
        ctx.addArc(
            2,
            ctx.truecaseFromFirst(kiVar),
            kiVar,
            NormV2Types.SRC_COMPOUND_MERGE,
            0.0f,
            closedCnt > 0 ? (float) Math.log1p(closedCnt) : 0.0f,
            0.78f,
            0.18f,
            SpanCandidateGenerator.countDiacriticDiffs(t1.lower() + t2Low, kiVar));
        return;
      }
    }
  }

  private void addClosedCompoundOrCopulaMerge(
      MergeContext ctx, SpanToken t1, SpanToken t2, boolean syntacticallyBlocked) {
    if (syntacticallyBlocked) {
      return;
    }
    String t2Low = t2.lower();
    String concatRaw = t1.lower() + t2Low;
    String realized = morph.realizeValidWordOrNull(concatRaw);
    if (realized == null
        || realized.contains("şey")
        || realized.equals("yada")
        || realized.equals("şuan")) {
      return;
    }
    boolean isClosedPrefix = startsWithAny(realized, CLOSED_COMPOUND_PREFIXES);
    boolean isSplitAbility =
        (t1.lower().endsWith("a") || t1.lower().endsWith("e"))
            && (t2Low.startsWith("bil") || t2.ascii().startsWith("bil"));
    boolean isSplitCopula = isSplitCopulaMerge(t1.lower(), t2Low, realized);
    int closedFreq = knowledge.cleanLm.unigramCount(realized);

    if (isClosedPrefix || isSplitAbility || isSplitCopula || closedFreq >= 80) {
      ctx.addArc(
          2,
          ctx.truecaseFromFirst(realized),
          realized,
          NormV2Types.SRC_COMPOUND_MERGE,
          0.0f,
          closedFreq > 0 ? (float) Math.log1p(closedFreq) : 0.0f,
          0.85f,
          0.20f,
          SpanCandidateGenerator.countDiacriticDiffs(concatRaw, realized));
    }
  }

  private boolean isSplitCopulaMerge(String t1Low, String t2Low, String realized) {
    if (!SPLIT_COPULA_TAILS.contains(t2Low) || !morph.hasRegularStrictAnalysis(realized)) {
      return false;
    }
    if (t2Low.equals("sa") || t2Low.equals("se")) {
      return morph.summarizeMorph(t1Low).syntaxClass()
          == NormV2Types.HEAD_SYNTAX_FINITE_OR_CONVERB;
    }
    return true;
  }

  private static void addDetachedProperSuffixMerge(
      MergeContext ctx, SpanToken t1, String t2Low, StemClassification stemInfo) {
    if (!stemInfo.isProperOrAcronym()
        || t1.lower().length() < 2
        || !DETACHED_PROPER_NONLOC_SUFFIXES.contains(t2Low)) {
      return;
    }
    boolean derivSuf = t2Low.startsWith("l") && !t2Low.equals("la") && !t2Low.equals("le");
    if (derivSuf && !stemInfo.isKnownAcronym()) {
      String closedLow = t1.lower() + t2Low;
      String closedSurf =
          !ctx.sentenceAllCaps && t1.shape() == CasingShape.MIXED
              ? t1.surface() + t2Low
              : NormV2Types.trCapitalize(closedLow);
      ctx.addRuleArc(2, closedSurf, closedLow, NormV2Types.SRC_COMPOUND_MERGE, 0.78f, 0.18f);
      return;
    }
    String apLow = t1.lower() + "'" + t2Low;
    String apSurf =
        stemInfo.isKnownAcronym()
            ? NormV2Types.trUpper(t1.lower()) + "'" + t2Low
            : ctx.truecaseFromFirst(apLow);
    ctx.addRuleArc(
        2,
        apSurf,
        apLow,
        NormV2Types.SRC_OOV_APOSTROPHE | NormV2Types.SRC_COMPOUND_MERGE,
        stemInfo.isKnownAcronym() ? 0.84f : 0.78f,
        0.18f);
  }

  // =========================================================================
  // Phonology & Token Shape Helpers
  // =========================================================================

  private String realizeOrKeep(String wordLower) {
    String real = morph.realizeValidWordOrNull(wordLower);
    return real != null ? real : wordLower;
  }

  private static String queryForAsciiUpperI(SpanToken tok, String rawHead) {
    return tok.hasUpperAsciiAmbiguousI() ? rawHead.replace('ı', 'i') : rawHead;
  }

  private static boolean endsWithLocativeClitic(String low) {
    return low.endsWith("de") || low.endsWith("da") || low.endsWith("te") || low.endsWith("ta");
  }

  private static boolean endsWithLocativeKi(String low) {
    return low.endsWith("daki")
        || low.endsWith("deki")
        || low.endsWith("taki")
        || low.endsWith("teki");
  }

  private static String harmonizedDeDaClitic(String stemLower) {
    return LatticeMorphologySupport.isFrontVowel(
            LatticeMorphologySupport.lastTurkishVowel(stemLower))
        ? "de"
        : "da";
  }

  private static String assimilateLocOrAblSuffix(String stemLower, boolean ablative) {
    String vowel =
        LatticeMorphologySupport.isFrontVowel(LatticeMorphologySupport.lastTurkishVowel(stemLower))
            ? "e"
            : "a";
    String cons =
        TurkishAlphabet.INSTANCE.isVoicelessConsonant(stemLower.charAt(stemLower.length() - 1))
            ? "t"
            : "d";
    return ablative ? cons + vowel + "n" : cons + vowel;
  }

  private static boolean startsWithAny(String word, Set<String> prefixes) {
    for (String p : prefixes) {
      if (word.startsWith(p)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSyntacticallyBlockedMerge(
      List<SpanToken> tokens, int i, String t1Low, String t2Low) {
    if (i <= 0) {
      return false;
    }
    SpanToken prev = tokens.get(i - 1);
    String prevLow = prev.lower();
    if (QUANTIFIED_COUNT_NOUNS.contains(t1Low)
        && (prev.type() == Token.Type.Number || COUNT_MODIFIERS.contains(prevLow))) {
      return true;
    }
    if (t1Low.equals("bu") && t2Low.startsWith("gün") && prevLow.length() >= 5) {
      for (String suf : PARTICIPLE_SUFFIXES) {
        if (prevLow.endsWith(suf)) {
          return true;
        }
      }
    }
    return (t1Low.equals("de") || t1Low.equals("da"))
        && t1Low.equals(t2Low)
        && (prevLow.endsWith("mak") || prevLow.endsWith("mek"));
  }

  static String harmonizeQuestionClitic(String rawQt, char stemVowel) {
    char hv =
        switch (stemVowel) {
          case 'e', 'i' -> 'i';
          case 'ö', 'ü' -> 'ü';
          case 'o', 'u' -> 'u';
          default -> 'ı';
        };
    if (rawQt.length() == 2) {
      return "m" + hv;
    }
    return switch (NormV2Types.toAscii(rawQt)) {
      case "miyim", "muyum" -> "m" + hv + "y" + hv + "m";
      case "misin", "musun" -> "m" + hv + "s" + hv + "n";
      case "miyiz", "muyuz" -> "m" + hv + "y" + hv + "z";
      case "misiniz", "musunuz" -> "m" + hv + "s" + hv + "n" + hv + "z";
      case "miydi", "muydu" -> "m" + hv + "yd" + hv;
      case "miydim", "muydum" -> "m" + hv + "yd" + hv + "m";
      case "miydin", "muydun" -> "m" + hv + "yd" + hv + "n";
      case "miydik", "muyduk" -> "m" + hv + "yd" + hv + "k";
      case "miydiniz", "muydunuz" -> "m" + hv + "yd" + hv + "n" + hv + "z";
      case "miymis", "muymus" -> "m" + hv + "ym" + hv + "ş";
      default -> rawQt;
    };
  }

  private static int findSingleCamelBoundary(String surface) {
    int cut = -1;
    for (int i = 1; i < surface.length(); i++) {
      char prev = surface.charAt(i - 1);
      char cur = surface.charAt(i);
      if (!Character.isLetter(cur)) {
        return -1;
      }
      if (Character.isUpperCase(cur)) {
        if (!Character.isLowerCase(prev) || cut != -1) {
          return -1;
        }
        cut = i;
      }
    }
    return cut;
  }

  private static boolean isNumericSurface(String s) {
    int n = s.length();
    if (n == 0 || !Character.isDigit(s.charAt(n - 1))) {
      return false;
    }
    boolean seenDigit = false;
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      if (Character.isDigit(c)) {
        seenDigit = true;
      } else if (c != '.' && c != ',' && c != '%' && c != '/' && c != ':') {
        return false;
      }
    }
    return seenDigit;
  }

  private static boolean isLettersThenDigits(String s) {
    int n = s.length();
    if (n < 2) {
      return false;
    }
    int i = 0;
    while (i < n && Character.isLetter(s.charAt(i))) {
      i++;
    }
    if (i == 0 || i > 4 || i == n) {
      return false;
    }
    while (i < n) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
      i++;
    }
    return true;
  }
}
