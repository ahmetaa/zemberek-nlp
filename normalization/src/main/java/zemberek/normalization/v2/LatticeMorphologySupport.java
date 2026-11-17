package zemberek.normalization.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.normalization.v2.NormalizationKnowledge.CleanWordLm;

/**
 * Wraps Zemberek formal, informal, and ASCII-tolerant morphological analyzers with bounded caches,
 * TDK apostrophe formatting, and Turkish vowel-harmony helpers.
 */
final class LatticeMorphologySupport {

  static final Set<String> DETACHED_NUM_SUFFIXES =
      Set.of(
          "da", "de", "ta", "te", "dan", "den", "tan", "ten", "a", "e", "ya", "ye", "ı", "i", "u",
          "ü", "yı", "yi", "yu", "yü", "ın", "in", "un", "ün", "nın", "nin", "nun", "nün", "la",
          "le", "yla", "yle", "ar", "er", "şar", "şer", "ıncı", "inci", "uncu", "üncü", "ncı",
          "nci", "ncu", "ncü", "sini", "sını", "sunu", "sünü", "ini", "ını", "unu", "ünü", "si",
          "sı", "su", "sü", "nde", "nda", "nden", "ndan", "lı", "li", "lu", "lü", "lık", "lik",
          "luk", "lük", "lılar", "liler", "lular", "lüler", "lar", "ler", "dı", "di", "du", "dü",
          "tı", "ti", "tu", "tü", "dır", "dir", "dur", "dür", "tır", "tir", "tur", "tür");

  static final Set<String> DETACHED_WORD_APOSTROPHE_SUFFIXES =
      Set.of(
          "da", "de", "ta", "te", "dan", "den", "tan", "ten", "a", "e", "ya", "ye", "ı", "i", "u",
          "ü", "yı", "yi", "yu", "yü", "ın", "in", "un", "ün", "nın", "nin", "nun", "nün", "ini",
          "ını", "unu", "ünü", "si", "sı", "su", "sü", "nde", "nda", "nden", "ndan", "la", "le",
          "yla", "yle", "lı", "li", "lu", "lü", "lık", "lik", "luk", "lük", "lılar", "liler",
          "lular", "lüler", "lar", "ler", "dı", "di", "du", "dü", "tı", "ti", "tu", "tü");

  private static final String[] NEG_VERB_ASCII_INFIXES = {
    "miyor", "muyor", "mayacak", "meyecek", "micak", "micek", "mucak", "mucek"
  };

  private static final String[] NEG_VERB_ASCII_SUFFIXES = {
    "miyo", "muyo", "miyolar", "muyolar", "mazlar", "mezler", "mazsin", "mezsin"
  };

  private static final String NO_REALIZATION = "";

  record MorphSummary(
      short coarseTag,
      byte syntaxClass,
      byte verbPerson,
      byte verbPolarity,
      boolean isRegular,
      boolean isProperOrAbbr,
      boolean isStrictRegularCommon) {}

  record CachedMorphItem(
      String fixedProperSurface,
      String normLower,
      int sourceMask,
      float suffixPrecision,
      float editCost,
      int diacriticDiffs) {}

  @FunctionalInterface
  interface AsciiRealizationConsumer {
    void accept(SingleAnalysis analysis, String normLower, boolean informal);
  }

  final TurkishMorphology strictMorphology;
  final TurkishMorphology informalAsciiMorphology;
  private final TurkishMorphology exactInformalMorphology;
  private final InformalAnalysisConverter informalConverter;
  private final NormalizationKnowledge knowledge;

  private final ConcurrentHashMap<String, MorphSummary> morphSummaryCache =
      new ConcurrentHashMap<>(32_768);
  private final ConcurrentHashMap<String, String> realizeWordCache =
      new ConcurrentHashMap<>(32_768);
  private final ConcurrentHashMap<String, List<CachedMorphItem>> morphRealizeCache =
      new ConcurrentHashMap<>(32_768);
  private final ConcurrentHashMap<String, Character> questionVowelCache =
      new ConcurrentHashMap<>(16_384);

  LatticeMorphologySupport(TurkishMorphology strictMorphology, NormalizationKnowledge knowledge) {
    this.strictMorphology = strictMorphology;
    this.knowledge = knowledge;
    this.exactInformalMorphology =
        TurkishMorphology.builder()
            .setLexicon(strictMorphology.getLexicon())
            .useInformalAnalysis()
            .setCache(AnalysisCache.builder().dynamicCacheSize(16_384, 65_536).build())
            .setAmbiguityResolver((s, a) -> new SentenceAnalysis(s, Collections.emptyList()))
            .build();
    this.informalAsciiMorphology =
        TurkishMorphology.builder()
            .setLexicon(strictMorphology.getLexicon())
            .useInformalAnalysis()
            .ignoreDiacriticsInAnalysis()
            .setCache(AnalysisCache.builder().dynamicCacheSize(16_384, 65_536).build())
            .setAmbiguityResolver((s, a) -> new SentenceAnalysis(s, Collections.emptyList()))
            .build();
    this.informalConverter = new InformalAnalysisConverter(strictMorphology.getWordGenerator());
  }

  // =========================================================================
  // Morphological Summary & Syntactic Role Tagging
  // =========================================================================

  MorphSummary summarizeMorph(String wordLower) {
    MorphSummary cached = morphSummaryCache.get(wordLower);
    if (cached != null) {
      return cached;
    }
    MorphSummary computed = computeMorphSummary(wordLower);
    if (morphSummaryCache.size() < 65_536) {
      morphSummaryCache.put(wordLower, computed);
    }
    return computed;
  }

  private MorphSummary computeMorphSummary(String wordLower) {
    int ap = wordLower.indexOf('\'');
    String query = ap > 0 ? wordLower.substring(0, ap) + wordLower.substring(ap + 1) : wordLower;

    SingleAnalysis bestReg = null;
    SingleAnalysis bestAny = null;
    boolean seenNegVerb = false;
    boolean seenPosVerb = false;
    boolean seenNonVerb = false;

    for (SingleAnalysis a : strictMorphology.analyze(query)) {
      if (a.isUnknown()) {
        continue;
      }
      if (bestAny == null) {
        bestAny = a;
      }
      if (a.isRegularWord() && !isExtInterjection(a.getDictionaryItem())) {
        if (bestReg == null) {
          bestReg = a;
        }
        if (a.getPos() == PrimaryPos.Verb) {
          List<Morpheme> ms = a.getMorphemes();
          if (ms.contains(TurkishMorphotactics.neg) || ms.contains(TurkishMorphotactics.unable)) {
            seenNegVerb = true;
          } else {
            seenPosVerb = true;
          }
        } else {
          seenNonVerb = true;
        }
      }
    }

    SingleAnalysis chosen = bestReg != null ? bestReg : bestAny;
    if (chosen == null) {
      return unknownMorphSummary(wordLower, ap > 0);
    }

    byte polarity = 0;
    if (!seenNonVerb && seenNegVerb != seenPosVerb) {
      polarity = seenNegVerb ? (byte) -1 : (byte) 1;
    }
    return buildMorphSummary(chosen, ap, bestReg != null, polarity);
  }

  private MorphSummary unknownMorphSummary(String wordLower, boolean hasApostrophe) {
    boolean cleanAttested =
        knowledge.cleanLm != null && knowledge.cleanLm.unigramCount(wordLower) >= 8;
    return new MorphSummary(
        (short) 0,
        cleanAttested ? NormV2Types.HEAD_SYNTAX_NOMINAL : NormV2Types.HEAD_SYNTAX_OTHER,
        (byte) 0,
        (byte) 0,
        cleanAttested,
        hasApostrophe,
        false);
  }

  private static MorphSummary buildMorphSummary(
      SingleAnalysis chosen, int apostropheIdx, boolean hasRegularAnalysis, byte polarity) {
    DictionaryItem item = chosen.getDictionaryItem();
    PrimaryPos finalPos = chosen.getPos();
    Morpheme lastCase = null;
    byte person = 0;
    boolean isVerb = finalPos == PrimaryPos.Verb;

    for (Morpheme m : chosen.getMorphemes()) {
      if (caseCode(m) != 0) {
        lastCase = m;
      }
      if (isVerb) {
        byte p = verbPersonCode(m);
        if (p != 0) {
          person = p;
        }
      }
    }

    boolean properNoQuoteDerivative =
        item.isProperNoun() && chosen.hasEnding() && !properNounTakesApostropheTDK(chosen);
    boolean takesApostrophe =
        item.isAbbreviation() || (item.isProperNoun() && !properNoQuoteDerivative);

    return new MorphSummary(
        encodeMorphTag(finalPos, lastCase),
        classifySyntaxRole(item.primaryPos, finalPos),
        person,
        polarity,
        hasRegularAnalysis || properNoQuoteDerivative,
        takesApostrophe,
        apostropheIdx <= 0 && hasRegularAnalysis);
  }

  private static byte classifySyntaxRole(PrimaryPos rootPos, PrimaryPos finalPos) {
    if (finalPos == PrimaryPos.Verb
        || (rootPos == PrimaryPos.Verb && finalPos == PrimaryPos.Adverb)) {
      return NormV2Types.HEAD_SYNTAX_FINITE_OR_CONVERB;
    }
    if (finalPos == PrimaryPos.Pronoun
        || finalPos == PrimaryPos.Adverb
        || finalPos == PrimaryPos.Conjunction
        || finalPos == PrimaryPos.PostPositive) {
      return NormV2Types.HEAD_SYNTAX_PRON_ADV_CONJ;
    }
    if (finalPos == PrimaryPos.Noun
        || finalPos == PrimaryPos.Adjective
        || finalPos == PrimaryPos.Numeral) {
      return NormV2Types.HEAD_SYNTAX_NOMINAL;
    }
    return NormV2Types.HEAD_SYNTAX_OTHER;
  }

  private static byte verbPersonCode(Morpheme m) {
    if (m == TurkishMorphotactics.a1sg) return 1;
    if (m == TurkishMorphotactics.a2sg) return 2;
    if (m == TurkishMorphotactics.a3sg) return 3;
    if (m == TurkishMorphotactics.a1pl) return 4;
    if (m == TurkishMorphotactics.a2pl) return 5;
    if (m == TurkishMorphotactics.a3pl) return 6;
    return 0;
  }

  private static int caseCode(Morpheme caseMorph) {
    if (caseMorph == TurkishMorphotactics.nom) return 1;
    if (caseMorph == TurkishMorphotactics.acc) return 2;
    if (caseMorph == TurkishMorphotactics.dat) return 3;
    if (caseMorph == TurkishMorphotactics.loc) return 4;
    if (caseMorph == TurkishMorphotactics.abl) return 5;
    if (caseMorph == TurkishMorphotactics.gen) return 6;
    if (caseMorph == TurkishMorphotactics.ins) return 7;
    return 0;
  }

  static short encodeMorphTag(PrimaryPos pos, Morpheme caseMorph) {
    int p = pos == null ? 0 : (pos.ordinal() + 1);
    return (short) ((p << 4) | caseCode(caseMorph));
  }

  // =========================================================================
  // Lexical & Polarity Predicates
  // =========================================================================

  boolean hasRegularStrictAnalysis(String wordLower) {
    return !wordLower.isEmpty()
        && wordLower.indexOf('\'') < 0
        && summarizeMorph(wordLower).isStrictRegularCommon;
  }

  boolean isStrictProperOrAbbrOnly(String wordLower) {
    boolean hasProp = false;
    for (SingleAnalysis a : strictMorphology.analyze(wordLower)) {
      if (!a.isKnownLexical()) {
        continue;
      }
      DictionaryItem item = a.getDictionaryItem();
      if (item.isProperNoun() || item.isAbbreviation()) {
        if (!a.hasEnding()) {
          hasProp = true;
        }
      } else {
        return false;
      }
    }
    return hasProp;
  }

  boolean isBareImperativeVerbOnly(String wordLower) {
    boolean hasBareVerb = false;
    for (SingleAnalysis a : strictMorphology.analyze(wordLower)) {
      if (!a.isKnownLexical()) {
        continue;
      }
      if (a.getDictionaryItem().primaryPos == PrimaryPos.Verb && !a.hasEnding()) {
        hasBareVerb = true;
      } else {
        return false;
      }
    }
    return hasBareVerb;
  }

  boolean isVerbRootCandidate(String stemLower) {
    for (SingleAnalysis a : informalAsciiMorphology.analyze(stemLower)) {
      if (a.isKnownLexical()
          && a.getDictionaryItem().primaryPos == PrimaryPos.Verb
          && !a.hasEnding()) {
        return true;
      }
    }
    return false;
  }

  boolean isKnownStrictAbbreviation(String wordLower) {
    for (SingleAnalysis a : strictMorphology.analyze(wordLower)) {
      if (a.isKnownLexical() && !a.hasEnding() && a.getDictionaryItem().isAbbreviation()) {
        return true;
      }
    }
    return false;
  }

  boolean hasRawNegativeVerbCue(String rawLower) {
    if (rawLower == null || rawLower.length() < 5) {
      return false;
    }
    MorphSummary ms = summarizeMorph(rawLower);
    if (ms.verbPolarity != 0) {
      return ms.verbPolarity < 0;
    }
    String asc = NormV2Types.toAscii(rawLower);
    for (String infix : NEG_VERB_ASCII_INFIXES) {
      if (asc.contains(infix)) {
        return true;
      }
    }
    for (String suf : NEG_VERB_ASCII_SUFFIXES) {
      if (asc.endsWith(suf)) {
        return true;
      }
    }
    if ((asc.endsWith("marlar") || asc.endsWith("merler")) && rawLower.length() >= 8) {
      return isVerbRootCandidate(rawLower.substring(0, rawLower.length() - 6));
    }
    return false;
  }

  // =========================================================================
  // Formal / Informal / ASCII Word Realization
  // =========================================================================

  List<CachedMorphItem> morphRealizations(String low) {
    List<CachedMorphItem> items = morphRealizeCache.get(low);
    if (items == null) {
      items = computeMorphRealizations(low);
      if (morphRealizeCache.size() < 65_536) {
        morphRealizeCache.put(low, items);
      }
    }
    return items;
  }

  private List<CachedMorphItem> computeMorphRealizations(String low) {
    List<CachedMorphItem> out = new ArrayList<>(6);
    for (String nl : collectExactInformalRealizations(low, low, Integer.MAX_VALUE)) {
      out.add(
          new CachedMorphItem(
              null,
              nl,
              NormV2Types.SRC_INFORMAL_EXACT | NormV2Types.SRC_INFORMAL_MORPH,
              0.93f,
              SpanCandidateGenerator.computeWeightedEditCost(low, nl),
              SpanCandidateGenerator.countDiacriticDiffs(low, nl)));
    }

    List<CachedMorphItem> asciiPool = new ArrayList<>(8);
    forEachAsciiRealization(
        low,
        low,
        (a, nl, informal) -> {
          if (informal) {
            asciiPool.add(
                new CachedMorphItem(
                    null,
                    nl,
                    NormV2Types.SRC_INFORMAL_MORPH,
                    0.82f,
                    SpanCandidateGenerator.computeWeightedEditCost(low, nl),
                    SpanCandidateGenerator.countDiacriticDiffs(low, nl)));
          } else {
            boolean isProp = a.getDictionaryItem().isProperNoun();
            if (!nl.equals(low) || isProp) {
              String fixedProp = isProp ? formatProperOrAbbrTDK(a, nl) : null;
              String finalLow = fixedProp != null ? NormV2Types.trLower(fixedProp) : nl;
              asciiPool.add(
                  new CachedMorphItem(
                      fixedProp,
                      finalLow,
                      NormV2Types.SRC_FORMAL_MORPH,
                      0.0f,
                      SpanCandidateGenerator.computeWeightedEditCost(low, finalLow),
                      SpanCandidateGenerator.countDiacriticDiffs(low, finalLow)));
            }
          }
        });

    CleanWordLm cleanLm = knowledge.cleanLm;
    if (asciiPool.size() > 1 && cleanLm != null) {
      asciiPool.sort(
          (x, y) ->
              Float.compare(
                  cleanLm.unigramLogProb(y.normLower()), cleanLm.unigramLogProb(x.normLower())));
    }
    int limit = Math.min(6, asciiPool.size());
    for (int i = 0; i < limit; i++) {
      out.add(asciiPool.get(i));
    }
    return out.isEmpty() ? Collections.emptyList() : List.copyOf(out);
  }

  String realizeValidWordOrNull(String candidateLower) {
    if (candidateLower.isEmpty()) {
      return null;
    }
    String cached = realizeWordCache.get(candidateLower);
    if (cached != null) {
      return cached.isEmpty() ? null : cached;
    }
    String computed = computeRealizeValidWordOrNull(candidateLower);
    if (realizeWordCache.size() < 65_536) {
      realizeWordCache.put(candidateLower, computed != null ? computed : NO_REALIZATION);
    }
    return computed;
  }

  private String computeRealizeValidWordOrNull(String candidateLower) {
    if (hasRegularStrictAnalysis(candidateLower)) {
      return candidateLower;
    }
    String exactInf = firstExactInformalOrNull(candidateLower, candidateLower);
    if (exactInf != null) {
      return exactInf;
    }

    final class BestPicker {
      String bestRegular;
      float bestRegularLogProb = -1000.0f;
      String fallbackProper;
    }
    BestPicker picker = new BestPicker();
    CleanWordLm cleanLm = knowledge.cleanLm;

    forEachAsciiRealization(
        candidateLower,
        candidateLower,
        (a, nl, informal) -> {
          DictionaryItem item = a.getDictionaryItem();
          if (item.isAbbreviation()) {
            return;
          }
          if (!item.isProperNoun()) {
            float lp = cleanLm != null ? cleanLm.unigramLogProb(nl) : 0.0f;
            if (picker.bestRegular == null || lp > picker.bestRegularLogProb) {
              picker.bestRegular = nl;
              picker.bestRegularLogProb = lp;
            }
          } else if (picker.fallbackProper == null) {
            picker.fallbackProper = nl;
          }
        });
    return picker.bestRegular != null ? picker.bestRegular : picker.fallbackProper;
  }

  String firstExactInformalOrNull(String query, String rawForDiacCheck) {
    List<String> matches = collectExactInformalRealizations(query, rawForDiacCheck, 1);
    return matches.isEmpty() ? null : matches.get(0);
  }

  private List<String> collectExactInformalRealizations(
      String query, String rawForDiacCheck, int limit) {
    List<String> out = new ArrayList<>(Math.min(4, limit));
    for (SingleAnalysis a : exactInformalMorphology.analyze(query)) {
      if (!a.isKnownLexical()
          || !a.containsInformalMorpheme()
          || isExtInterjection(a.getDictionaryItem())) {
        continue;
      }
      Result conv = informalConverter.convert(query, a);
      if (conv != null && !stripsExplicitStemDiacritic(rawForDiacCheck, conv.surface)) {
        out.add(NormV2Types.trLower(conv.surface));
        if (out.size() >= limit) {
          break;
        }
      }
    }
    return out;
  }

  void forEachAsciiRealization(
      String query, String rawForDiacCheck, AsciiRealizationConsumer consumer) {
    for (SingleAnalysis a : informalAsciiMorphology.analyze(query)) {
      if (!a.isKnownLexical()) {
        continue;
      }
      DictionaryItem item = a.getDictionaryItem();
      if (isExtInterjection(item)) {
        continue;
      }
      if (a.containsInformalMorpheme()) {
        Result conv = informalConverter.convert(query, a);
        if (conv != null && !stripsExplicitStemDiacritic(rawForDiacCheck, conv.surface)) {
          consumer.accept(a, NormV2Types.trLower(conv.surface), true);
        }
      } else if (!item.isAbbreviation()) {
        for (Result r : strictMorphology.getWordGenerator().generate(item, a.getMorphemes())) {
          if (TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(query, r.surface)) {
            String nl = NormV2Types.trLower(r.surface);
            if (!stripsExplicitDiacritic(rawForDiacCheck, nl)) {
              consumer.accept(a, nl, false);
            }
          }
        }
      }
    }
  }

  // =========================================================================
  // TDK ProperNoun / Abbreviation Formatting & Vowel Harmony
  // =========================================================================

  static String formatProperOrAbbrTDK(SingleAnalysis a) {
    return formatProperOrAbbrTDK(a, null);
  }

  static String formatProperOrAbbrTDK(SingleAnalysis a, String generatedLowerOrNull) {
    DictionaryItem item = a.getDictionaryItem();
    Locale loc = item.hasAttribute(RootAttribute.LocaleEn) ? Locale.ENGLISH : Turkish.LOCALE;
    String ending = resolveAnalysisEnding(a, item, generatedLowerOrNull);

    if (item.isAbbreviation()) {
      String upperLemma = item.normalizedLemma().toUpperCase(loc);
      return ending.isEmpty() ? upperLemma : upperLemma + "'" + ending;
    }
    if (item.isProperNoun()) {
      String titleLemma = NormV2Types.trCapitalize(item.normalizedLemma());
      if (ending.isEmpty()) {
        return titleLemma;
      }
      if (properNounTakesApostropheTDK(a)) {
        return titleLemma + "'" + ending;
      }
      if (generatedLowerOrNull != null) {
        return NormV2Types.trCapitalize(generatedLowerOrNull);
      }
      String stemSurf =
          item.hasAttribute(RootAttribute.NoQuote) ? item.normalizedLemma() : a.getStem();
      return NormV2Types.trCapitalize(stemSurf + ending);
    }
    return null;
  }

  private static String resolveAnalysisEnding(
      SingleAnalysis a, DictionaryItem item, String generatedLowerOrNull) {
    String ending = a.getEnding();
    if (generatedLowerOrNull != null && !ending.isEmpty()) {
      String lemmaLow = NormV2Types.trLower(item.normalizedLemma());
      int stemLen =
          generatedLowerOrNull.startsWith(lemmaLow)
              ? lemmaLow.length()
              : Math.min(generatedLowerOrNull.length(), a.getStem().length());
      if (stemLen < generatedLowerOrNull.length()) {
        return generatedLowerOrNull.substring(stemLen);
      }
    }
    return ending;
  }

  static boolean properNounTakesApostropheTDK(SingleAnalysis a) {
    if (!a.hasEnding() || a.getDictionaryItem().hasAttribute(RootAttribute.NoQuote)) {
      return false;
    }
    List<SingleAnalysis.MorphemeData> mdList = a.getMorphemeDataList();
    for (int i = 1; i < mdList.size(); i++) {
      SingleAnalysis.MorphemeData md = mdList.get(i);
      if (!md.surface.isEmpty()) {
        return md.morpheme != TurkishMorphotactics.a3pl && !md.morpheme.derivational;
      }
    }
    return false;
  }

  char expectedTurkishQuestionVowel(String prevWordLower) {
    if (prevWordLower == null || prevWordLower.isEmpty()) {
      return 0;
    }
    int ap = prevWordLower.lastIndexOf('\'');
    if (ap >= 0) {
      if (ap == 0 || ap + 1 >= prevWordLower.length()) {
        return 0;
      }
      String suf = prevWordLower.substring(ap + 1);
      if (!DETACHED_NUM_SUFFIXES.contains(suf)
          && !DETACHED_WORD_APOSTROPHE_SUFFIXES.contains(suf)) {
        return 0;
      }
      return vowelToQuestionHighVowel(lastTurkishVowel(suf));
    }
    Character cached = questionVowelCache.get(prevWordLower);
    if (cached != null) {
      return cached;
    }
    char computed = computeExpectedTurkishQuestionVowel(prevWordLower);
    if (questionVowelCache.size() < 32_768) {
      questionVowelCache.put(prevWordLower, computed);
    }
    return computed;
  }

  private char computeExpectedTurkishQuestionVowel(String prevWordLower) {
    boolean hasInverse = false;
    boolean hasNormal = false;
    for (SingleAnalysis a : strictMorphology.analyze(prevWordLower)) {
      DictionaryItem item = a.getDictionaryItem();
      if (!a.isRegularWord()
          || item.hasAttribute(RootAttribute.LocaleEn)
          || isExtInterjection(item)) {
        continue;
      }
      if (!a.hasEnding() && item.hasAttribute(RootAttribute.InverseHarmony)) {
        hasInverse = true;
      } else {
        hasNormal = true;
      }
    }
    if (hasInverse == hasNormal) {
      return 0;
    }
    char lv = lastTurkishVowel(prevWordLower);
    if (lv == 0) {
      return 0;
    }
    if (hasInverse) {
      lv = frontHarmonizedTwin(lv);
    }
    return vowelToQuestionHighVowel(lv);
  }

  private static char frontHarmonizedTwin(char v) {
    return switch (v) {
      case 'a', 'â' -> 'e';
      case 'ı' -> 'i';
      case 'o' -> 'ö';
      case 'u', 'û' -> 'ü';
      default -> v;
    };
  }

  private static char vowelToQuestionHighVowel(char v) {
    return switch (v) {
      case 'e', 'i', 'î' -> 'i';
      case 'ö', 'ü' -> 'ü';
      case 'o', 'u', 'û' -> 'u';
      case 'a', 'ı', 'â' -> 'ı';
      default -> 0;
    };
  }

  private static boolean isExtInterjection(DictionaryItem item) {
    return item.primaryPos == PrimaryPos.Interjection && item.isExtended();
  }

  static char lastTurkishVowel(String s) {
    return TurkishAlphabet.INSTANCE.getLastVowel(s).charValue;
  }

  /** Treats {@code 'û'} as a back vowel ({@code e, i, ö, ü, î} front). */
  static boolean isFrontVowel(char v) {
    return v == 'e' || v == 'i' || v == 'ö' || v == 'ü' || v == 'î';
  }

  static boolean stripsExplicitDiacritic(String srcLower, String candLower) {
    return TurkishAlphabet.INSTANCE.stripsExplicitTurkishDiacritic(srcLower, candLower);
  }

  static boolean stripsExplicitStemDiacritic(String srcLower, String candSurface) {
    String cl = NormV2Types.trLower(candSurface);
    int n = Math.min(3, Math.min(srcLower.length(), cl.length()));
    return n > 0
        && TurkishAlphabet.INSTANCE.stripsExplicitTurkishDiacritic(
            srcLower.substring(0, n), cl.substring(0, n));
  }
}
