package zemberek.normalization.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.normalization.v2.CompactStemEndingSpellChecker.ScoredCandidate;
import zemberek.normalization.v2.NormV2Types.CasingShape;
import zemberek.normalization.v2.NormV2Types.SpanToken;
import zemberek.normalization.v2.NormalizationKnowledge.CasingCounts;
import zemberek.normalization.v2.NormalizationKnowledge.RewriteEntry;
import zemberek.normalization.v2.NormalizationKnowledge.SuffixRule;
import zemberek.normalization.v2.NormalizationKnowledge.TargetCount;
import zemberek.normalization.v2.SpanCandidateGenerator.SlotDraftBuilder;

/**
 * Generates single-token colloquial, morphological, TDK apostrophe/numeral, elongation,
 * question-clitic, and guarded spellcheck candidates.
 */
final class SingleTokenRewriteGenerator {

  private static final Set<String> COMMON_UNITS =
      Set.of(
          "gb", "mb", "kb", "tb", "tl", "usd", "eur", "mhz", "ghz", "mah", "kg", "gr", "mg", "cm",
          "mm", "km", "ml", "lt", "dk", "sn", "saat", "gün", "ay", "yıl", "adet");

  private static final Set<String> UPPERCASE_UNITS =
      Set.of("gb", "mb", "kb", "tb", "tl", "usd", "eur", "mhz", "ghz", "hd", "fps");

  private static final Set<String> REAL_AGENTIVE_CIM_WORDS =
      Set.of(
          "öğrencim", "takipçim", "izleyicim", "dinleyicim", "okuyucum", "yardımcım", "temsilcim",
          "sunucum", "oyuncum", "yöneticim", "eğitimcim", "işçim", "bekçim", "aşçım", "avcım",
          "yolcum", "komşum", "ciğim", "cığım", "cuğum", "cüğüm", "çiğim", "çığım", "çuğum",
          "çüğüm");

  private static final String[] OOV_PROPER_INFLECT_SUFS = {
    "dayız", "deyiz", "tayız", "teyiz", "dayım", "deyim", "tayım", "teyim", "ndaki", "ndeki",
    "daki", "deki", "taki", "teki", "lılar", "liler", "lular", "lüler", "dır", "dir", "dur", "dür",
    "tır", "tir", "tur", "tür", "ydı", "ydi", "ydu", "ydü", "dan", "den", "tan", "ten", "da", "de",
    "ta", "te", "nın", "nin", "nun", "nün", "ın", "in", "un", "ün", "ya", "ye", "yı", "yi", "yu",
    "yü", "yla", "yle", "la", "le", "li", "lı", "lu", "lü", "lik", "lık", "luk", "lük", "ler",
    "lar", "di", "dı", "du", "dü", "ti", "tı", "tu", "tü", "a", "e", "ı", "i", "u", "ü"
  };

  private static final String[] TDK_NO_APOSTROPHE_DERIV_PREFIXES = {
    "lı", "li", "lu", "lü", "lık", "lik", "luk", "lük", "lar", "ler", "ca", "ce", "ça", "çe",
    "cı", "ci", "cu", "cü", "çı", "çi", "çu", "çü", "gil", "cığ", "ciğ", "cuğ", "cüğ", "çığ",
    "çiğ", "çuğ", "çüğ", "cug", "cüg", "cım", "cim", "cum", "cüm"
  };

  private static final Map<String, String> HAPLOLOGY_STEMS =
      Map.of(
          "burda", "burada",
          "orda", "orada",
          "şurda", "şurada",
          "nerde", "nerede",
          "nerden", "nereden",
          "burdan", "buradan",
          "ordan", "oradan",
          "şurdan", "şuradan");

  private static final String[][] INTERNAL_PROG_PATTERNS = {
    {"iyod", "iyord"},
    {"ıyod", "ıyord"},
    {"uyod", "uyord"},
    {"üyod", "üyord"},
    {"iod", "iyord"},
    {"ıod", "ıyord"},
    {"iyomuş", "iyormuş"},
    {"ıyomuş", "ıyormuş"},
    {"uyomuş", "uyormuş"},
    {"üyomuş", "üyormuş"},
    {"iyomus", "iyormuş"},
    {"iomuş", "iyormuş"},
    {"iomus", "iyormuş"},
    {"iyosa", "iyorsa"},
    {"ıyosa", "ıyorsa"},
    {"uyosa", "uyorsa"},
    {"üyosa", "üyorsa"}
  };

  private static final String[] NEG_OPT_ENDINGS = {
    "miyim", "mıyım", "muyum", "müyüm", "miyiz", "mıyız", "muyuz", "müyüz"
  };

  private static final String[] HYPOCORISTIC_ENDINGS = {
    "cım", "cim", "cum", "cüm", "çım", "çim", "çum", "çüm", "cm"
  };

  private final NormalizationKnowledge knowledge;
  private final LatticeMorphologySupport morph;
  private final CompactStemEndingSpellChecker spellChecker;

  SingleTokenRewriteGenerator(
      NormalizationKnowledge knowledge,
      LatticeMorphologySupport morph,
      CompactStemEndingSpellChecker spellChecker) {
    this.knowledge = knowledge;
    this.morph = morph;
    this.spellChecker = spellChecker;
  }

  // =========================================================================
  // Informal Suffix, Haplology & Colloquial Verb Rewrites
  // =========================================================================

  void addSuffixAndHaplologyCandidates(SlotDraftBuilder slot) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    if (low.length() < 4 || tok.containsDigit()) {
      return;
    }
    addHaplologyCandidates(slot, low);
    addForeignPhoneticAdaptation(slot, low);
    addInternalProgressiveCandidate(slot, low);
    addSoftGDroppedCandidate(slot, low);
    addNegativeOptativeCandidates(slot, low);
    addHypocoristicCandidate(slot, low);
    addMinedSuffixRuleCandidates(slot, low);
  }

  private void addHaplologyCandidates(SlotDraftBuilder slot, String low) {
    for (Map.Entry<String, String> e : HAPLOLOGY_STEMS.entrySet()) {
      String bad = e.getKey();
      if (low.startsWith(bad) || slot.tok.ascii().startsWith(NormV2Types.toAscii(bad))) {
        String realized = morph.realizeValidWordOrNull(e.getValue() + low.substring(bad.length()));
        if (realized != null) {
          slot.addTruecasedAutoCost(realized, NormV2Types.SRC_SUFFIX_RULE, 0.92f);
        }
      }
    }
  }

  /** Adapts foreign {@code x -> ks} and {@code w -> v} ({@code "indexlenmesini" -> "indekslenmesini"}). */
  private void addForeignPhoneticAdaptation(SlotDraftBuilder slot, String low) {
    if (low.length() < 5
        || (low.indexOf('x') < 0 && low.indexOf('w') < 0)
        || isMidSentenceTitle(slot)) {
      return;
    }
    String adapted = low.replace("x", "ks").replace("w", "v");
    String realized = morph.realizeValidWordOrNull(adapted);
    if (realized != null) {
      slot.addTruecased(
          realized,
          NormV2Types.SRC_SUFFIX_RULE,
          0.88f,
          SpanCandidateGenerator.computeWeightedEditCost(low, realized),
          SpanCandidateGenerator.countDiacriticDiffs(adapted, realized));
    }
  }

  /** Restores dropped {@code r} in internal progressive + copula ({@code "geliyomuş" -> "geliyormuş"}). */
  private void addInternalProgressiveCandidate(SlotDraftBuilder slot, String low) {
    if (low.length() < 6 || low.indexOf('o') < 2) {
      return;
    }
    String progCand = expandInternalInformalProgressive(low);
    if (progCand == null) {
      return;
    }
    String realized = morph.realizeValidWordOrNull(progCand);
    if (realized != null && !LatticeMorphologySupport.stripsExplicitStemDiacritic(low, realized)) {
      slot.addTruecased(
          realized,
          NormV2Types.SRC_SUFFIX_RULE | NormV2Types.SRC_INFORMAL_MORPH,
          0.90f,
          SpanCandidateGenerator.computeWeightedEditCost(low, realized),
          SpanCandidateGenerator.countDiacriticDiffs(progCand, realized));
    }
  }

  /** Restores spoken {@code ğ}-dropped double vowels ({@code "oynadıım" -> "oynadığım"}). */
  private void addSoftGDroppedCandidate(SlotDraftBuilder slot, String low) {
    if (low.length() < 5) {
      return;
    }
    for (int k = 2; k + 2 < low.length(); k++) {
      char v = low.charAt(k);
      if (v == low.charAt(k + 1) && isHighTurkishVowel(v)) {
        String gCand = low.substring(0, k + 1) + 'ğ' + low.substring(k + 1);
        String realized = morph.realizeValidWordOrNull(gCand);
        if (realized != null
            && realized.indexOf('ğ') >= 0
            && !LatticeMorphologySupport.stripsExplicitStemDiacritic(low, realized)) {
          slot.addTruecased(
              realized,
              NormV2Types.SRC_SUFFIX_RULE | NormV2Types.SRC_INFORMAL_MORPH,
              0.88f,
              SpanCandidateGenerator.computeWeightedEditCost(low, realized),
              SpanCandidateGenerator.countDiacriticDiffs(gCand, realized));
        }
        return;
      }
    }
  }

  /** Expands spoken 1sg/1pl negative optative & progressive ({@code "almiyim" -> "almayayım"}). */
  private void addNegativeOptativeCandidates(SlotDraftBuilder slot, String low) {
    if (low.length() < 6) {
      return;
    }
    for (String ending : NEG_OPT_ENDINGS) {
      if (!low.endsWith(ending)) {
        continue;
      }
      String vStem = low.substring(0, low.length() - ending.length());
      if (vStem.length() >= 2 && morph.isVerbRootCandidate(vStem)) {
        String[] formalTails =
            ending.endsWith("z")
                ? new String[] {"mayalım", "meyelim"}
                : new String[] {"mayayım", "meyeyim", "mıyorum", "miyorum", "muyorum", "müyorum"};
        for (String ft : formalTails) {
          String realized = morph.realizeValidWordOrNull(vStem + ft);
          if (realized != null
              && !LatticeMorphologySupport.stripsExplicitStemDiacritic(low, realized)) {
            int stemCheckLen = Math.min(vStem.length(), realized.length());
            slot.addTruecased(
                realized,
                NormV2Types.SRC_SUFFIX_RULE | NormV2Types.SRC_INFORMAL_MORPH,
                ft.endsWith("yorum") ? 0.80f : 0.90f,
                SpanCandidateGenerator.computeWeightedEditCost(low, realized),
                SpanCandidateGenerator.countDiacriticDiffs(
                    vStem, realized.substring(0, stemCheckLen)));
          }
        }
      }
      return;
    }
  }

  /** Expands hypocoristic diminutives ({@code "annecim" -> "anneciğim"}). */
  private void addHypocoristicCandidate(SlotDraftBuilder slot, String low) {
    if (low.length() < 5) {
      return;
    }
    String[] hypPair = expandHypocoristic(low);
    if (hypPair == null) {
      return;
    }
    String stem = hypPair[0];
    String hyp = hypPair[1];
    boolean forceTitle =
        morph.isStrictProperOrAbbrOnly(stem) || knowledge.isKnownProperOrAcronymStem(stem);
    String surf =
        forceTitle && !slot.sentenceAllCaps ? NormV2Types.trCapitalize(hyp) : slot.truecase(hyp);
    slot.addRule(
        surf,
        hyp,
        NormV2Types.SRC_SUFFIX_RULE,
        0.88f,
        SpanCandidateGenerator.computeWeightedEditCost(low, hyp),
        SpanCandidateGenerator.countDiacriticDiffs(low, hyp));
  }

  private void addMinedSuffixRuleCandidates(SlotDraftBuilder slot, String low) {
    List<SuffixRule> rules =
        knowledge.suffixRulesByLastChar.get(String.valueOf(low.charAt(low.length() - 1)));
    if (rules == null) {
      return;
    }
    int added = 0;
    for (SuffixRule sr : rules) {
      if (low.length() <= sr.rawSuffix().length() + 1 || !low.endsWith(sr.rawSuffix())) {
        continue;
      }
      String stem = low.substring(0, low.length() - sr.rawSuffix().length());
      String realized = morph.realizeValidWordOrNull(stem + sr.normSuffix());
      if (realized != null
          && !LatticeMorphologySupport.stripsExplicitStemDiacritic(low, realized)) {
        slot.add(
            slot.truecase(realized),
            realized,
            NormV2Types.SRC_SUFFIX_RULE,
            0.0f,
            (float) Math.log1p(sr.support()),
            sr.precision(),
            SpanCandidateGenerator.computeWeightedEditCost(low, realized),
            SpanCandidateGenerator.countDiacriticDiffs(low, realized));
        if (++added >= 4) {
          return;
        }
      }
    }
  }

  private static String expandInternalInformalProgressive(String low) {
    for (String[] p : INTERNAL_PROG_PATTERNS) {
      int idx = low.lastIndexOf(p[0]);
      if (idx >= 2) {
        return low.substring(0, idx) + p[1] + low.substring(idx + p[0].length());
      }
    }
    return null;
  }

  private String[] expandHypocoristic(String low) {
    if (REAL_AGENTIVE_CIM_WORDS.contains(low)
        || (knowledge.cleanLm != null && knowledge.cleanLm.unigramCount(low) >= 25)) {
      return null;
    }
    for (String end : HYPOCORISTIC_ENDINGS) {
      if (!low.endsWith(end) || low.length() - end.length() < 3) {
        continue;
      }
      String rawStem = low.substring(0, low.length() - end.length());
      String stem = morph.realizeValidWordOrNull(rawStem);
      if (stem == null) {
        stem = rawStem;
      }
      char lastV = LatticeMorphologySupport.lastTurkishVowel(stem);
      if (lastV == 0) {
        return null;
      }
      String cons =
          TurkishAlphabet.INSTANCE.isVoicelessConsonant(stem.charAt(stem.length() - 1))
              ? "ç"
              : "c";
      String suf =
          switch (lastV) {
            case 'e', 'i' -> cons + "iğim";
            case 'ö', 'ü' -> cons + "üğüm";
            case 'o', 'u' -> cons + "uğum";
            default -> cons + "ığım";
          };
      return new String[] {stem, stem + suf};
    }
    return null;
  }

  // =========================================================================
  // TDK Proper Nouns, Acronyms, Numerals & Erroneous Apostrophe Removal
  // =========================================================================

  void addProperAcronymAndNumeralCandidates(SlotDraftBuilder slot) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    int apIdx = low.indexOf('\'');
    if (apIdx > 0 && apIdx < low.length() - 1) {
      addApostropheCleanupCandidates(slot, low.substring(0, apIdx), low.substring(apIdx + 1));
      return;
    }
    if (apIdx >= 0) {
      return;
    }
    if (tok.containsDigit()) {
      addAlphanumericTokenCandidates(slot, low);
      return;
    }
    addLexiconProperAndAbbrCandidates(slot, low);
    addOovProperOrAcronymCandidates(slot, low);
  }

  /** Removes erroneous apostrophes ({@code "azerice'ye" -> "Azericeye"}, {@code "altın'ın" -> "altının"}). */
  private void addApostropheCleanupCandidates(SlotDraftBuilder slot, String stem, String tail) {
    if (NormV2Types.isDigitsOrPunctOnly(stem)) {
      return;
    }
    SpanToken tok = slot.tok;
    String joined = stem + tail;
    boolean isAcronym = knowledge.isKnownAcronymStem(stem);

    for (SingleAnalysis a : morph.strictMorphology.analyze(joined)) {
      if (!a.isKnownLexical()) {
        continue;
      }
      if (a.getDictionaryItem().isProperNoun()
          && !LatticeMorphologySupport.properNounTakesApostropheTDK(a)
          && !isAcronym
          && !(tok.shape() == CasingShape.UPPER && !slot.sentenceAllCaps)) {
        String fmt = LatticeMorphologySupport.formatProperOrAbbrTDK(a);
        if (fmt != null && fmt.indexOf('\'') < 0) {
          String fl = NormV2Types.trLower(fmt);
          slot.addRule(
              fmt,
              fl,
              NormV2Types.SRC_APOSTROPHE_DROP | NormV2Types.SRC_FORMAL_MORPH,
              0.92f,
              0.15f,
              SpanCandidateGenerator.countDiacriticDiffs(joined, fl));
        }
      }
    }

    if (isAcronym) {
      return;
    }
    if (morph.hasRegularStrictAnalysis(joined)) {
      boolean commonLower = isPredominantlyLowerStem(stem, 5);
      if (commonLower || tok.shape() == CasingShape.LOWER) {
        String surf = slot.capHere ? NormV2Types.trCapitalize(joined) : joined;
        slot.addRule(
            surf,
            joined,
            NormV2Types.SRC_APOSTROPHE_DROP | NormV2Types.SRC_FORMAL_MORPH,
            commonLower ? 0.85f : 0.55f,
            0.20f,
            0);
      }
    } else if (startsWithNoApostropheDeriv(tail)) {
      String candLow = stem + assimilateDerivationalTail(stem, tail);
      String surf = formatDerivedWordWithoutApostrophe(slot, stem, candLow);
      slot.addRule(
          surf,
          candLow,
          NormV2Types.SRC_APOSTROPHE_DROP,
          0.86f,
          0.18f,
          SpanCandidateGenerator.countDiacriticDiffs(joined, candLow));
    }
  }

  private String formatDerivedWordWithoutApostrophe(
      SlotDraftBuilder slot, String stem, String candLow) {
    boolean keepLowerUnlessStart =
        isPredominantlyLowerStem(stem, 4)
            || (slot.tok.shape() == CasingShape.LOWER
                && !knowledge.isKnownProperOrAcronymStem(stem));
    if (keepLowerUnlessStart) {
      return slot.capHere ? NormV2Types.trCapitalize(candLow) : candLow;
    }
    return NormV2Types.trCapitalize(candLow);
  }

  private boolean isPredominantlyLowerStem(String stem, int minLowerCount) {
    CasingCounts cc = knowledge.lookupCasing(stem);
    return cc != null
        && cc.lower >= minLowerCount
        && cc.lower > (cc.title + cc.upper + cc.apostropheStem) * 2;
  }

  /** Handles glued digit+unit/suffix tokens ({@code "16gb" -> "16 GB"}, {@code "2024te" -> "2024'te"}). */
  private void addAlphanumericTokenCandidates(SlotDraftBuilder slot, String low) {
    int dEnd = 0;
    while (dEnd < low.length() && Character.isDigit(low.charAt(dEnd))) {
      dEnd++;
    }
    if (dEnd > 0 && dEnd < low.length()) {
      addLeadingDigitsCandidates(slot, low.substring(0, dEnd), low.substring(dEnd));
    } else if (dEnd == 0) {
      addAlphanumericStemSuffixCandidate(slot, low);
    }
  }

  private void addLeadingDigitsCandidates(SlotDraftBuilder slot, String numPart, String rest) {
    if (rest.startsWith(".") && rest.length() > 1) {
      String ordJoined = buildOrdinalInflected(numPart, rest.substring(1));
      if (ordJoined != null) {
        slot.addRule(ordJoined, ordJoined, NormV2Types.SRC_OOV_APOSTROPHE, 0.90f, 0.2f, 0);
      }
      return;
    }
    if (COMMON_UNITS.contains(rest)) {
      String unitNorm = formatMeasurementUnit(rest);
      String splitUnit = numPart + " " + unitNorm;
      slot.addRule(
          splitUnit, NormV2Types.trLower(splitUnit), NormV2Types.SRC_CLITIC_SPLIT, 0.90f, 0.2f, 0);
      return;
    }
    if (LatticeMorphologySupport.DETACHED_NUM_SUFFIXES.contains(rest)) {
      String joined = numPart + "'" + rest;
      slot.addRule(joined, joined, NormV2Types.SRC_OOV_APOSTROPHE, 0.88f, 0.2f, 0);
      return;
    }
    if (isTechModelUnitPlusSuffix(rest)) {
      char unitCh = rest.charAt(0);
      String modelStem =
          numPart + (unitCh == 's' ? "s" : String.valueOf(Character.toUpperCase(unitCh)));
      String joinedSurf = modelStem + "'" + rest.substring(1);
      slot.addRule(
          joinedSurf,
          NormV2Types.trLower(joinedSurf),
          NormV2Types.SRC_OOV_APOSTROPHE,
          0.86f,
          0.2f,
          0);
      return;
    }
    if (rest.length() >= 2) {
      String realWord = morph.realizeValidWordOrNull(rest);
      if (realWord != null) {
        String splitNumWord = numPart + " " + realWord;
        slot.addRule(
            splitNumWord,
            splitNumWord,
            NormV2Types.SRC_CLITIC_SPLIT,
            0.85f,
            0.2f,
            SpanCandidateGenerator.countDiacriticDiffs(rest, realWord));
      }
    }
  }

  private static String formatMeasurementUnit(String unitLower) {
    if (UPPERCASE_UNITS.contains(unitLower)) {
      return NormV2Types.trUpper(unitLower);
    }
    return unitLower.equals("mah") ? "mAh" : unitLower;
  }

  private static boolean isTechModelUnitPlusSuffix(String rest) {
    if (rest.length() < 2) {
      return false;
    }
    char c0 = rest.charAt(0);
    return (c0 == 's' || c0 == 'g' || c0 == 'k' || c0 == 'd')
        && LatticeMorphologySupport.DETACHED_WORD_APOSTROPHE_SUFFIXES.contains(rest.substring(1));
  }

  /** Handles alphanumeric brand stems with glued case suffixes ({@code "tv8e" -> "TV8'e"}). */
  private static void addAlphanumericStemSuffixCandidate(SlotDraftBuilder slot, String low) {
    int lastDigit = -1;
    for (int k = 0; k < low.length(); k++) {
      if (Character.isDigit(low.charAt(k))) {
        lastDigit = k;
      }
    }
    if (lastDigit < 1 || lastDigit > 5 || lastDigit >= low.length() - 1) {
      return;
    }
    String stem = low.substring(0, lastDigit + 1);
    String suf = low.substring(lastDigit + 1);
    if (LatticeMorphologySupport.DETACHED_NUM_SUFFIXES.contains(suf)) {
      slot.addRule(
          NormV2Types.trUpper(stem) + "'" + suf,
          stem + "'" + suf,
          NormV2Types.SRC_OOV_APOSTROPHE,
          0.86f,
          0.2f,
          0);
    }
  }

  private void addLexiconProperAndAbbrCandidates(SlotDraftBuilder slot, String low) {
    for (SingleAnalysis a : morph.strictMorphology.analyze(low)) {
      if (!a.isKnownLexical() || !a.hasEnding()) {
        continue;
      }
      DictionaryItem item = a.getDictionaryItem();
      if (item.isProperNoun() || item.isAbbreviation()) {
        String formatted = LatticeMorphologySupport.formatProperOrAbbrTDK(a);
        if (formatted != null && !formatted.equals(slot.tok.surface())) {
          String fl = NormV2Types.trLower(formatted);
          slot.addRule(
              formatted,
              fl,
              NormV2Types.SRC_OOV_APOSTROPHE,
              0.85f,
              0.15f,
              SpanCandidateGenerator.countDiacriticDiffs(low, fl));
        }
      }
    }
  }

  /** Handles OOV brand / acronym + suffix ({@code "ramdi" -> "RAM'di"}, {@code "akpli" -> "AKP'li"}). */
  private void addOovProperOrAcronymCandidates(SlotDraftBuilder slot, String low) {
    if (low.length() < 4 || low.length() > 20) {
      return;
    }
    SpanToken tok = slot.tok;
    boolean regularWord = morph.hasRegularStrictAnalysis(low);
    for (String suf : OOV_PROPER_INFLECT_SUFS) {
      if (low.length() - suf.length() < 2 || !low.endsWith(suf)) {
        continue;
      }
      String stemLow = low.substring(0, low.length() - suf.length());
      boolean isAcronym = knowledge.isKnownAcronymStem(stemLow);
      if (isAcronymOnlySuffix(suf) && !isAcronym) {
        continue;
      }
      if (tok.shape() == CasingShape.LOWER && ShorthandExpander.isCuratedShorthand(low)) {
        continue;
      }
      if (regularWord && !isAcronym) {
        continue;
      }
      boolean knownProper = isAcronym || knowledge.isKnownProperOrAcronymStem(stemLow);
      boolean midSentTitleOov =
          !knownProper
              && !slot.capHere
              && tok.shape() == CasingShape.TITLE
              && stemLow.length() >= 3
              && !morph.hasRegularStrictAnalysis(stemLow);
      if (knownProper || midSentTitleOov) {
        String candLow = stemLow + "'" + suf;
        String candSurf =
            isAcronym ? NormV2Types.trUpper(stemLow) + "'" + suf : slot.truecase(candLow);
        float prec = isAcronym ? 0.86f : (knownProper ? 0.80f : 0.72f);
        slot.addRule(candSurf, candLow, NormV2Types.SRC_OOV_APOSTROPHE, prec, 0.22f, 0);
        return;
      }
    }
  }

  private static boolean isAcronymOnlySuffix(String suf) {
    return suf.startsWith("li")
        || suf.startsWith("lı")
        || suf.startsWith("lu")
        || suf.startsWith("lü")
        || suf.equals("ler")
        || suf.equals("lar");
  }

  private static boolean startsWithNoApostropheDeriv(String tail) {
    for (String p : TDK_NO_APOSTROPHE_DERIV_PREFIXES) {
      if (tail.startsWith(p)) {
        return true;
      }
    }
    return false;
  }

  private static String assimilateDerivationalTail(String stem, String tail) {
    if (stem.isEmpty() || tail.isEmpty()) {
      return tail;
    }
    boolean voiceless =
        TurkishAlphabet.INSTANCE.isVoicelessConsonant(stem.charAt(stem.length() - 1));
    if (tail.startsWith("cug")) {
      return (voiceless ? "çuğ" : "cuğ") + tail.substring(3);
    }
    if (tail.startsWith("cüg")) {
      return (voiceless ? "çüğ" : "cüğ") + tail.substring(3);
    }
    if (tail.startsWith("cig")) {
      return (voiceless ? "çiğ" : "ciğ") + tail.substring(3);
    }
    if (tail.startsWith("cıg")) {
      return (voiceless ? "çığ" : "cığ") + tail.substring(3);
    }
    if (voiceless && tail.startsWith("c")) {
      return "ç" + tail.substring(1);
    }
    return tail;
  }

  // =========================================================================
  // Expressive Elongation, Question-Clitic Twins & Guarded Spellchecker
  // =========================================================================

  void addElongationCandidates(SlotDraftBuilder slot, char expectedPrevQ, boolean strictKnown) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    int maxRun = maxRepeatedLetterRun(low);
    if (maxRun < 3 && isProtectedFromDoubleLetterSqueeze(slot, low, strictKnown)) {
      return;
    }
    boolean mStemQuestionBoundary = maxRun < 3 && isMStemPlusQuestionClitic(tok);
    String sq1 = mStemQuestionBoundary ? low : squeezeRuns(low, 1);
    String sq2 = squeezeRuns(low, 2);
    String sqTrail = squeezeTrailingRun(low);
    String sqVow = squeezeVowelRuns(low, 1);

    List<String> uniqueSq = new ArrayList<>(4);
    for (String candSq : new String[] {sq1, sq2, sqTrail, sqVow}) {
      if (candSq.length() >= 2 && !candSq.equals(low) && !uniqueSq.contains(candSq)) {
        uniqueSq.add(candSq);
      }
    }
    for (String sq : uniqueSq) {
      RewriteEntry re = knowledge.parallel1ToN.get(sq);
      if (re != null) {
        for (TargetCount tc : re.targets()) {
          slot.add(
              slot.truecase(tc.normLower()),
              tc.normLower(),
              NormV2Types.SRC_ELONGATION | NormV2Types.SRC_PARALLEL_1TO1,
              re.probOf(tc.normLower()),
              (float) Math.log1p(tc.count()),
              0.0f,
              0.35f,
              SpanCandidateGenerator.countDiacriticDiffs(sq, tc.normLower()));
        }
      }
      String realized = morph.realizeValidWordOrNull(sq);
      if (realized != null) {
        boolean disharmonicSqQuestion =
            expectedPrevQ != 0
                && SpanCandidateGenerator.isStandaloneQuestionClitic(realized)
                && realized.length() >= 2
                && realized.charAt(1) != expectedPrevQ;
        slot.addTruecased(
            realized,
            NormV2Types.SRC_ELONGATION,
            disharmonicSqQuestion ? 0.25f : 0.80f,
            0.35f,
            SpanCandidateGenerator.countDiacriticDiffs(sq, realized));
      }
      if (!isMidSentenceTitle(slot)) {
        addQuestionCliticDiacriticTwins(slot, sq, expectedPrevQ, 0.0f, 0.0f, true);
      }
    }
  }

  private boolean isProtectedFromDoubleLetterSqueeze(
      SlotDraftBuilder slot, String low, boolean strictKnown) {
    if (strictKnown
        || low.length() <= 3
        || morph.isStrictProperOrAbbrOnly(low)
        || knowledge.isKnownProperOrAcronymStem(low)) {
      return true;
    }
    if (slot.sentenceAllCaps) {
      return false;
    }
    CasingShape shape = slot.tok.shape();
    return shape == CasingShape.UPPER || (!slot.capHere && shape == CasingShape.TITLE);
  }

  void addQuestionCliticDiacriticTwins(
      SlotDraftBuilder slot,
      String candidateBaseLower,
      char expectedPrevQ,
      float rawKeepProb,
      float rawKeepLogCnt,
      boolean fromElongation) {
    if (!SpanCandidateGenerator.isStandaloneQuestionClitic(candidateBaseLower)) {
      return;
    }
    char curV = candidateBaseLower.length() >= 2 ? candidateBaseLower.charAt(1) : 0;
    char[] sameRoundingVowels = sameRoundingHighVowelsOrNull(curV);
    if (sameRoundingVowels == null) {
      return;
    }
    for (char hv : sameRoundingVowels) {
      String variant = SplitMergeSpanGenerator.harmonizeQuestionClitic(candidateBaseLower, hv);
      if (!fromElongation && variant.equals(slot.tok.lower())) {
        continue;
      }
      int rawDiffs = SpanCandidateGenerator.countDiacriticDiffs(candidateBaseLower, variant);
      int effectiveDiffs = Math.min(1, rawDiffs);
      boolean matchesPrevHarmony = expectedPrevQ != 0 && hv == expectedPrevQ;
      int srcMask =
          NormV2Types.SRC_FORMAL_MORPH
              | (rawDiffs > 0 ? NormV2Types.SRC_DEASCIIFIER : 0)
              | (fromElongation ? NormV2Types.SRC_ELONGATION : 0)
              | (matchesPrevHarmony && rawKeepProb > 0.0f ? NormV2Types.SRC_PARALLEL_1TO1 : 0);
      float parProb = matchesPrevHarmony && !fromElongation ? Math.max(0.85f, rawKeepProb) : 0.0f;
      float parLogCnt = matchesPrevHarmony && !fromElongation ? rawKeepLogCnt : 0.0f;
      float sufPrec = questionTwinPrecision(matchesPrevHarmony, fromElongation, expectedPrevQ);
      float cost = (fromElongation ? 0.32f : 0.0f) + effectiveDiffs * 0.08f;
      slot.add(
          slot.truecase(variant),
          variant,
          srcMask,
          parProb,
          parLogCnt,
          sufPrec,
          cost,
          effectiveDiffs);
    }
  }

  private static char[] sameRoundingHighVowelsOrNull(char vowel) {
    if (vowel == 'i' || vowel == 'ı') {
      return new char[] {'i', 'ı'};
    }
    if (vowel == 'u' || vowel == 'ü') {
      return new char[] {'u', 'ü'};
    }
    return null;
  }

  private static float questionTwinPrecision(
      boolean matchesPrevHarmony, boolean fromElongation, char expectedPrevQ) {
    if (matchesPrevHarmony) {
      return 0.92f;
    }
    return fromElongation && expectedPrevQ == 0 ? 0.78f : 0.0f;
  }

  void addGuardedSpellCandidates(SlotDraftBuilder slot, boolean protectMidTitle) {
    String low = slot.tok.lower();
    List<ScoredCandidate> cands = spellChecker.suggestScored(low);
    boolean rawNegCue = morph.hasRawNegativeVerbCue(low);
    String squeezed1 = isMStemPlusQuestionClitic(slot.tok) ? squeezeRuns(low, 1) : null;
    int added = 0;
    for (ScoredCandidate sc : cands) {
      String nl = NormV2Types.trLower(sc.surface());
      if (nl.equals(low) || (squeezed1 != null && nl.equals(squeezed1))) {
        continue;
      }
      if (protectMidTitle && sc.editCost() > 0.35f) {
        continue;
      }
      if (!morph.hasRegularStrictAnalysis(nl)) {
        continue;
      }
      if (LatticeMorphologySupport.stripsExplicitDiacritic(low, nl) && sc.editCost() > 0.65f) {
        continue;
      }
      float effEditCost = sc.editCost();
      if (rawNegCue && morph.summarizeMorph(nl).verbPolarity() >= 0) {
        effEditCost += 0.55f;
      }
      slot.addTruecased(nl, NormV2Types.SRC_SPELLCHECK, 0.0f, effEditCost, sc.diacriticDiffs());
      if (++added >= 5) {
        return;
      }
    }
  }

  boolean isMStemPlusQuestionClitic(SpanToken tok) {
    String low = tok.lower();
    for (String qt : SplitMergeSpanGenerator.QUESTION_TAILS) {
      if (low.length() >= qt.length() + 3
          && (low.endsWith(qt) || tok.ascii().endsWith(NormV2Types.toAscii(qt)))) {
        String rawHead = low.substring(0, low.length() - qt.length());
        if (rawHead.endsWith("m")
            && (morph.hasRegularStrictAnalysis(rawHead)
                || morph.realizeValidWordOrNull(rawHead) != null)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isMidSentenceTitle(SlotDraftBuilder slot) {
    return !slot.sentenceAllCaps && !slot.capHere && slot.tok.shape() == CasingShape.TITLE;
  }

  private static boolean isHighTurkishVowel(char v) {
    return v == 'ı' || v == 'i' || v == 'u' || v == 'ü';
  }

  static String buildOrdinalInflected(String digits, String suf) {
    if (digits.isEmpty()) {
      return null;
    }
    char lastDigit = digits.charAt(digits.length() - 1);
    if (digits.endsWith("10")
        || digits.endsWith("30")
        || digits.endsWith("40")
        || digits.endsWith("60")
        || digits.endsWith("90")) {
      lastDigit = 'X';
    }
    String ordBase =
        switch (lastDigit) {
          case '1', '5', '8' -> "inci";
          case '2', '7' -> "nci";
          case '3', '4' -> "üncü";
          case '6' -> "ncı";
          case '9', '0' -> "uncu";
          default -> "uncu";
        };
    if (suf.startsWith("nc")
        || suf.startsWith("ınc")
        || suf.startsWith("inc")
        || suf.startsWith("unc")
        || suf.startsWith("ünc")) {
      return digits + "'" + suf;
    }
    char lv = LatticeMorphologySupport.lastTurkishVowel(ordBase);
    String harmonizedSuf = suf;
    if (suf.equals("da") || suf.equals("de")) {
      harmonizedSuf = LatticeMorphologySupport.isFrontVowel(lv) ? "de" : "da";
    } else if (suf.equals("dan") || suf.equals("den")) {
      harmonizedSuf = LatticeMorphologySupport.isFrontVowel(lv) ? "den" : "dan";
    }
    return digits + "'" + ordBase + harmonizedSuf;
  }

  static int maxRepeatedLetterRun(String s) {
    int maxRun = 1;
    int run = 1;
    for (int i = 1; i < s.length(); i++) {
      if (s.charAt(i) == s.charAt(i - 1) && Character.isLetter(s.charAt(i))) {
        if (++run > maxRun) {
          maxRun = run;
        }
      } else {
        run = 1;
      }
    }
    return maxRun;
  }

  static String squeezeRuns(String s, int maxRun) {
    StringBuilder sb = new StringBuilder(s.length());
    int run = 0;
    char prev = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (i > 0 && c == prev && Character.isLetter(c)) {
        if (++run <= maxRun) {
          sb.append(c);
        }
      } else {
        prev = c;
        run = 1;
        sb.append(c);
      }
    }
    return sb.toString();
  }

  static String squeezeTrailingRun(String s) {
    int n = s.length();
    if (n < 2) {
      return s;
    }
    char last = s.charAt(n - 1);
    if (!Character.isLetter(last)) {
      return s;
    }
    int cut = n - 1;
    while (cut > 0 && s.charAt(cut - 1) == last) {
      cut--;
    }
    return cut < n - 1 ? s.substring(0, cut + 1) : s;
  }

  static String squeezeVowelRuns(String s, int maxVowelRun) {
    StringBuilder sb = new StringBuilder(s.length());
    int run = 0;
    char prev = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (i > 0 && c == prev && "aeıioöuü".indexOf(c) >= 0) {
        if (++run <= maxVowelRun) {
          sb.append(c);
        }
      } else {
        prev = c;
        run = 1;
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
