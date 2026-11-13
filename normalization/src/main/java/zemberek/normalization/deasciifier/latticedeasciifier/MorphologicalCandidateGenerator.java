package zemberek.normalization.deasciifier.latticedeasciifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.SqueezedWord;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * Generates morphologically valid Turkish restoration candidates for input tokens using Zemberek's
 * diacritic-insensitive analyzer and surface generator.
 *
 * <p>Supports three initialization modes:
 * <ul>
 *   <li>Self-contained ({@link #MorphologicalCandidateGenerator()}) with compact internal caches
 *       and no ambiguity-resolver overhead
 *   <li>Single injected {@link TurkishMorphology} ({@link
 *       #MorphologicalCandidateGenerator(TurkishMorphology)}), leaving host caches untouched
 *   <li>Dual injected formal + informal/diacritic-tolerant {@link TurkishMorphology} instances
 *       ({@link #MorphologicalCandidateGenerator(TurkishMorphology, TurkishMorphology)}), sharing
 *       analyzers already constructed by {@code TurkishSentenceNormalizer}
 * </ul>
 */
public final class MorphologicalCandidateGenerator {

  private static final int INTERNAL_STRICT_CACHE_MAX = 1_024;

  private static final AmbiguityResolver NOOP_AMBIGUITY_RESOLVER =
      (sentence, analyses) -> {
        throw new UnsupportedOperationException(
            "Internal deasciifier TurkishMorphology does not run sentence disambiguation.");
      };

  // Canonical regular Turkish nouns for each (vowelClass 0..7, endingType 0..2) used to synthesize
  // harmonized suffixes after acronyms, foreign stems, and unlisted proper nouns.
  // endingType: 0 = vowel, 1 = voiced consonant, 2 = voiceless consonant (f,s,t,k,ç,ş,h,p).
  private static final String[][] PROTOTYPE_STEMS = {
    {"oda", "dal", "at"}, // a
    {"kale", "bel", "et"}, // e
    {"kapı", "kıl", "kış"}, // ı
    {"kedi", "dil", "iş"}, // i
    {"koro", "kol", "not"}, // o
    {"banliyö", "göl", "çöp"}, // ö
    {"kutu", "kul", "kuş"}, // u
    {"ütü", "gül", "üç"} // ü
  };

  private static final String[] GLUED_CLITICS = {
    "misiniz", "musunuz", "miyiz", "muyuz", "miyim", "muyum", "misin", "musun", "midir", "mudur",
    "miydi", "muydu", "mi", "mu", "da", "de", "ta", "te"
  };

  private final TurkishMorphology strictMorphology;
  private final RuleBasedAnalyzer asciiInformalAnalyzer;
  private final WordGenerator formalGenerator;
  private final WordGenerator informalGenerator;

  /**
   * Creates a self-contained generator with a small internal cache ({@code 1,024} entries, static
   * cache disabled) and no ambiguity-resolver overhead.
   */
  public MorphologicalCandidateGenerator() {
    RootLexicon lexicon = RootLexicon.getDefault();
    AnalysisCache strictCache =
        AnalysisCache.builder()
            .dynamicCacheSize(256, INTERNAL_STRICT_CACHE_MAX)
            .disableStaticCache()
            .build();
    this.strictMorphology =
        TurkishMorphology.builder()
            .setLexicon(lexicon)
            .useInformalAnalysis()
            .disableUnidentifiedTokenAnalyzer()
            .setCache(strictCache)
            .setAmbiguityResolver(NOOP_AMBIGUITY_RESOLVER)
            .build();
    TurkishMorphotactics sharedInformal = this.strictMorphology.getMorphotactics();
    this.asciiInformalAnalyzer = RuleBasedAnalyzer.ignoreDiacriticsInstance(sharedInformal);
    this.formalGenerator = this.strictMorphology.getWordGenerator();
    this.informalGenerator = this.formalGenerator;
  }

  /**
   * Creates a generator sharing an externally managed {@link TurkishMorphology} instance without
   * altering its cache configuration.
   */
  public MorphologicalCandidateGenerator(TurkishMorphology injectedMorphology) {
    Objects.requireNonNull(injectedMorphology, "injectedMorphology must not be null");
    this.strictMorphology = injectedMorphology;
    this.formalGenerator = injectedMorphology.getWordGenerator();
    TurkishMorphotactics tactics = injectedMorphology.getMorphotactics();
    if (tactics instanceof InformalTurkishMorphotactics) {
      this.asciiInformalAnalyzer = RuleBasedAnalyzer.ignoreDiacriticsInstance(tactics);
      this.informalGenerator = this.formalGenerator;
    } else {
      InformalTurkishMorphotactics informalTactics =
          new InformalTurkishMorphotactics(injectedMorphology.getLexicon());
      this.asciiInformalAnalyzer = RuleBasedAnalyzer.ignoreDiacriticsInstance(informalTactics);
      this.informalGenerator = new WordGenerator(informalTactics);
    }
  }

  /**
   * Creates a generator reusing both a strict {@link TurkishMorphology} and a diacritic-tolerant
   * informal {@link TurkishMorphology} (such as those already held by {@code
   * TurkishSentenceNormalizer}).
   */
  public MorphologicalCandidateGenerator(
      TurkishMorphology strictMorphology, TurkishMorphology asciiTolerantInformalMorphology) {
    Objects.requireNonNull(strictMorphology, "strictMorphology must not be null");
    Objects.requireNonNull(
        asciiTolerantInformalMorphology, "asciiTolerantInformalMorphology must not be null");
    this.strictMorphology = strictMorphology;
    this.formalGenerator = strictMorphology.getWordGenerator();
    this.asciiInformalAnalyzer = asciiTolerantInformalMorphology.getAnalyzer();
    this.informalGenerator = asciiTolerantInformalMorphology.getWordGenerator();
  }

  /** Generates morphologically valid candidates compatible with {@code token}. */
  public List<Candidate> generate(Token token, LexiconPriors priors) {
    if (token.immutable()) {
      return List.of(Candidate.oov(token.asciiLower(), token.original().length()));
    }

    // 1. Numeral + apostrophe (e.g. "3'uncu" -> "3'üncü", "6'sar" -> "6'şar", "1923'un" -> "1923'ün")
    if (token.numeralApostrophe() && token.apostropheIndex() > 0) {
      String restored = TurkishTextCodec.restoreNumeralApostrophe(token);
      if (TurkishTextCodec.isCompatibleWithInput(
          token.original(), restored, token.hasPinnedTurkish())) {
        return List.of(
            new Candidate(
                restored,
                token.original().substring(0, token.apostropheIndex()),
                PrimaryPos.Numeral,
                SecondaryPos.None,
                PrimaryPos.Numeral,
                "Nom",
                "Pnon",
                false,
                false,
                false,
                false,
                false,
                false,
                token.apostropheIndex(),
                1));
      }
    }

    // 2. Word + apostrophe (e.g. "Turkiye'nin", "TUIK'in", "ISO'ya", "WhatsApp'tan")
    if (token.hasApostrophe()) {
      return generateApostropheWordCandidates(token, priors);
    }

    // 3. All-caps acronyms & foreign ASCII-keep shield
    List<Candidate> out = new ArrayList<>(4);
    if (token.casing() == Casing.UPPER) {
      String acronym = priors != null ? priors.getAllCapsTurkishAcronym(token.asciiLower()) : null;
      if (acronym != null
          && TurkishTextCodec.isCompatibleWithInput(
              token.original(), acronym, token.hasPinnedTurkish())) {
        return List.of(createAbbreviationCandidate(acronym));
      }
      if ("yok".equals(token.asciiLower())
          && TurkishTextCodec.isCompatibleWithInput(
              token.original(), "yök", token.hasPinnedTurkish())) {
        out.add(createAbbreviationCandidate("yök"));
      }
    }

    if (priors != null
        && priors.isForeignAsciiKeep(token.asciiLower(), token.casing())
        && TurkishTextCodec.isCompatibleWithInput(
            token.original(), token.asciiLower(), token.hasPinnedTurkish())) {
      out.add(Candidate.oov(token.asciiLower(), token.original().length()));
      if (out.size() == 1) {
        return out;
      }
    }

    // 4. Standard & informal lexicon candidates (plus elongation / clitic fallbacks)
    String origForPins = token.hasPinnedTurkish() ? token.original() : null;
    List<Candidate> base = analyzePlainWithFallbacks(token.asciiLower(), origForPins);
    if (!token.hasPinnedTurkish() && out.isEmpty() && !base.isEmpty()) {
      return base;
    }
    for (Candidate c : base) {
      if (!token.hasPinnedTurkish()
          || TurkishTextCodec.isCompatibleWithInput(token.original(), c.surface, true)) {
        Candidate.addIfSurfaceAbsent(out, c);
      }
    }
    return out;
  }

  /**
   * Uncached plain-word candidate lookup (including expressive elongation and glued-clitic
   * fallbacks) invoked by {@link LatticeDeasciifier}'s bounded L1 token cache.
   */
  public List<Candidate> analyzePlainWithFallbacks(String asciiLower, String originalForPins) {
    List<Candidate> base = analyzePlainAsciiUncached(asciiLower);
    if (!base.isEmpty() && !isOnlyProperOrAbbrev(base)) {
      return base;
    }
    List<Candidate> out = new ArrayList<>(base);
    if (out.isEmpty() && TurkishTextCodec.hasLetterElongation(asciiLower)) {
      for (int maxRun = 1; maxRun <= 2 && out.isEmpty(); maxRun++) {
        SqueezedWord squeezed = TurkishTextCodec.squeezeElongations(asciiLower, maxRun);
        for (Candidate mc : analyzePlainAsciiUncached(squeezed.squeezedAscii())) {
          String projected = squeezed.projectToOriginalLength(mc.surface);
          if (originalForPins == null
              || TurkishTextCodec.isCompatibleWithInput(originalForPins, projected)) {
            Candidate.addIfSurfaceAbsent(out, copyWithSurface(mc, projected, mc.informal, mc.question));
          }
        }
      }
    }
    if (asciiLower.length() >= 5) {
      // Also run when base only had obscure ProperNoun/Abbreviation matches (e.g. "sevdimi" ->
      // [Sevdi:Noun,Prop]+m+i blocking glued question clitic "sevdi+mi").
      addGluedCliticCandidates(asciiLower, originalForPins, out);
    }
    if (out.isEmpty() && TurkishTextCodec.hasWordFinalDoubleLetter(asciiLower)) {
      String trimmed = asciiLower.substring(0, asciiLower.length() - 1);
      for (Candidate mc : analyzePlainAsciiUncached(trimmed)) {
        if (mc.morphemeGroupCount > 2 || mc.informal) {
          continue;
        }
        String projected = mc.surface + mc.surface.charAt(mc.surface.length() - 1);
        if (originalForPins == null
            || TurkishTextCodec.isCompatibleWithInput(originalForPins, projected)) {
          Candidate.addIfSurfaceAbsent(out, copyWithSurface(mc, projected, true, mc.question));
        }
      }
    }
    return out;
  }

  public List<Candidate> analyzePlainAsciiUncached(String asciiLower) {
    List<SingleAnalysis> analysis = analyzeAsciiInformal(asciiLower);
    if (analysis.isEmpty()) {
      return Collections.emptyList();
    }
    CandidateCollector collector = new CandidateCollector(4);
    for (SingleAnalysis sa : analysis) {
      if (sa.isUnknown()) {
        continue;
      }
      DictionaryItem item = sa.getDictionaryItem();
      // Filter known Zemberek lexicon false-friends on un-apostrophized plain words:
      // 1) Inflected abbreviations without apostrophe ("tuslarla" = [Tus:Abbrv]+lar+la, "komsu" = [Ko:Abbrv]+msu)
      // 2) Proper nouns deriving into verbs without apostrophe ("acmış" = [Ac:Prop]+Zero->Verb+mış)
      // 3) English/ASCII proper nouns whose 'I' was lowercased into 'ı' in proper-from-corpus.dict ("ıstanbul")
      if (item.secondaryPos == SecondaryPos.Abbreviation && sa.getMorphemes().size() > 1) {
        continue;
      }
      if (item.secondaryPos == SecondaryPos.ProperNoun && sa.getPos() == PrimaryPos.Verb) {
        continue;
      }
      if (isCorruptedDotlessProperNoun(item)) {
        continue;
      }

      for (WordGenerator.Result generated : generatorFor(sa).generate(item, sa.getMorphemes())) {
        String surf = generated.surface.toLowerCase(Turkish.LOCALE);
        if (surf.length() != asciiLower.length()
            || !TurkishTextCodec.toAsciiLower(surf).equals(asciiLower)) {
          continue;
        }
        collector.register(surf, sa, false, 0);

        String circSurf = projectCircumflexIfPresent(surf, item);
        if (circSurf != null && TurkishTextCodec.toAsciiLower(circSurf).equals(asciiLower)) {
          collector.register(circSurf, sa, true, 0);
        }
      }
    }
    if (collector.isEmpty()) {
      return Collections.emptyList();
    }
    List<Candidate> sorted = collector.toList();
    sorted.sort(
        (a, b) -> {
          int cmp = Integer.compare(a.morphemeGroupCount, b.morphemeGroupCount);
          return cmp != 0 ? cmp : Boolean.compare(a.informal, b.informal);
        });
    return Collections.unmodifiableList(sorted);
  }

  /**
   * Synthesizes morphophonologically harmonized suffixes after acronyms, foreign stems, and OOV
   * proper nouns (e.g. {@code Altınkapı + ' + nin -> Altınkapı'nın}).
   */
  public List<Candidate> harmonizeSuffixAfterStem(
      String chosenStemLower,
      char apostropheChar,
      String suffixAscii,
      String pronunciationSource,
      int foreignStemLen,
      String originalToken,
      boolean pinDotlessI) {
    return harmonizeSuffixAfterStem(
        chosenStemLower,
        apostropheChar,
        suffixAscii,
        pronunciationSource,
        foreignStemLen,
        originalToken,
        pinDotlessI,
        false);
  }

  private List<Candidate> harmonizeSuffixAfterStem(
      String chosenStemLower,
      char apostropheChar,
      String suffixAscii,
      String pronunciationSource,
      int foreignStemLen,
      String originalToken,
      boolean pinDotlessI,
      boolean isUpperAcronym) {
    for (String proto : selectPrototypeNouns(pronunciationSource, isUpperAcronym)) {
      String probe = TurkishTextCodec.toAsciiLower(proto) + suffixAscii;
      List<SingleAnalysis> analysis = analyzeAsciiInformal(probe);
      CandidateCollector collector = new CandidateCollector(2);
      for (SingleAnalysis sa : analysis) {
        if (sa.isUnknown()
            || !TurkishTextCodec.toAsciiLower(sa.getStemAndEnding().stem)
                .equals(TurkishTextCodec.toAsciiLower(proto))) {
          continue;
        }
        for (WordGenerator.Result r :
            generatorFor(sa).generate(sa.getDictionaryItem(), sa.getMorphemes())) {
          String surf = r.surface.toLowerCase(Turkish.LOCALE);
          if (surf.length() == proto.length() + suffixAscii.length() && surf.startsWith(proto)) {
            String full = chosenStemLower + apostropheChar + surf.substring(proto.length());
            if (TurkishTextCodec.isCompatibleWithInput(originalToken, full, pinDotlessI)) {
              collector.register(full, sa, false, foreignStemLen);
            }
          }
        }
      }
      if (!collector.isEmpty()) {
        return collector.toList();
      }
    }
    return Collections.emptyList();
  }

  private volatile RuleBasedAnalyzer strictInformalFallback;

  /** Returns the best strict lexicon analysis for offline corpus mining, or {@code null}. */
  public Candidate analyzeStrictBest(String lowerTurkishWord) {
    if (lowerTurkishWord == null || lowerTurkishWord.length() < 2) {
      return null;
    }
    boolean hasCircumflex = TurkishAlphabet.INSTANCE.containsCircumflex(lowerTurkishWord);
    String query =
        hasCircumflex ? TurkishTextCodec.stripCircumflex(lowerTurkishWord) : lowerTurkishWord;
    WordAnalysis wa = strictMorphology.analyze(query);
    SingleAnalysis best = selectBestLexiconAnalysis(wa, hasCircumflex);
    if (best == null
        && !(strictMorphology.getMorphotactics() instanceof InformalTurkishMorphotactics)) {
      String norm = TurkishMorphology.normalizeForAnalysis(query);
      if (!norm.isEmpty()) {
        RuleBasedAnalyzer fallback = strictInformalFallback;
        if (fallback == null) {
          fallback = RuleBasedAnalyzer.instance(asciiInformalAnalyzer.getMorphotactics());
          strictInformalFallback = fallback;
        }
        for (SingleAnalysis sa : fallback.analyze(norm)) {
          if (isLexiconAnalysis(sa)
              && (best == null
                  || preferenceScore(sa, hasCircumflex) > preferenceScore(best, hasCircumflex))) {
            best = sa;
          }
        }
      }
    }
    if (best == null) {
      return null;
    }
    return buildCandidate(lowerTurkishWord, best, hasCircumflex, 0);
  }

  private List<Candidate> generateApostropheWordCandidates(Token token, LexiconPriors priors) {
    int apos = token.apostropheIndex();
    char aposChar = token.original().charAt(apos);
    String stemOrig = token.original().substring(0, apos);
    String stemAscii = token.asciiLower().substring(0, apos);
    String suffixAscii = token.asciiLower().substring(apos + 1);
    Casing stemCasing = TurkishTextCodec.detectCasing(stemOrig, stemOrig.length());
    boolean pinDotlessI = token.hasPinnedTurkish();

    if (stemCasing == Casing.UPPER && priors != null) {
      String trAcronym = priors.getAllCapsTurkishAcronym(stemAscii);
      if (trAcronym != null) {
        List<Candidate> res =
            harmonizeSuffixAfterStem(
                trAcronym, aposChar, suffixAscii, trAcronym, 0, token.original(), pinDotlessI, true);
        if (!res.isEmpty()) {
          return res;
        }
      }
    }

    if (priors != null && priors.isForeignAsciiKeep(stemAscii, stemCasing)) {
      List<Candidate> res =
          harmonizeSuffixAfterStem(
              stemAscii,
              aposChar,
              suffixAscii,
              stemAscii,
              stemOrig.length(),
              token.original(),
              pinDotlessI,
              stemCasing == Casing.UPPER);
      if (!res.isEmpty()) {
        return res;
      }
    }

    String withoutQuoteAscii = stemAscii + suffixAscii;
    List<SingleAnalysis> analysis = analyzeAsciiInformal(withoutQuoteAscii);
    CandidateCollector collector = new CandidateCollector(4);
    for (SingleAnalysis sa : analysis) {
      if (sa.isUnknown()) {
        continue;
      }
      String saStemAscii = TurkishTextCodec.toAsciiLower(sa.getStemAndEnding().stem);
      if (saStemAscii.length() > stemAscii.length() || !stemAscii.startsWith(saStemAscii)) {
        continue;
      }
      if (saStemAscii.length() < stemAscii.length()
          && !sa.containsMorpheme(TurkishMorphotactics.p3sg)) {
        continue;
      }
      DictionaryItem item = sa.getDictionaryItem();
      if (isCorruptedDotlessProperNoun(item)) {
        continue;
      }
      for (WordGenerator.Result r : generatorFor(sa).generate(item, sa.getMorphemes())) {
        String surfNoQuote = r.surface.toLowerCase(Turkish.LOCALE);
        if (surfNoQuote.length() != withoutQuoteAscii.length()) {
          continue;
        }
        String withApos = surfNoQuote.substring(0, apos) + aposChar + surfNoQuote.substring(apos);
        if (TurkishTextCodec.isCompatibleWithInput(token.original(), withApos, pinDotlessI)) {
          collector.register(withApos, sa, false, 0);
        }
        String circNoQuote = projectCircumflexIfPresent(surfNoQuote, item);
        if (circNoQuote != null) {
          String circWithApos =
              circNoQuote.substring(0, apos) + aposChar + circNoQuote.substring(apos);
          if (TurkishTextCodec.isCompatibleWithInput(
              token.original(), circWithApos, pinDotlessI)) {
            collector.register(circWithApos, sa, true, 0);
          }
        }
      }
    }
    if (!collector.isEmpty()) {
      return collector.toList();
    }

    List<Candidate> stemCandidates = analyzePlainAsciiUncached(stemAscii);
    if (!stemCandidates.isEmpty()) {
      List<Candidate> merged = new ArrayList<>(stemCandidates.size());
      for (Candidate sc : stemCandidates) {
        for (Candidate hc :
            harmonizeSuffixAfterStem(
                sc.surface, aposChar, suffixAscii, sc.surface, 0, token.original(), pinDotlessI)) {
          Candidate.addIfSurfaceAbsent(merged, hc);
        }
      }
      if (!merged.isEmpty()) {
        return merged;
      }
    }

    if (TurkishTextCodec.isCamelCaseIdentifier(stemOrig)) {
      return harmonizeSuffixAfterStem(
          stemAscii,
          aposChar,
          suffixAscii,
          stemAscii,
          stemOrig.length(),
          token.original(),
          pinDotlessI);
    }
    return Collections.emptyList();
  }

  private List<SingleAnalysis> analyzeAsciiInformal(String asciiQuery) {
    if (asciiQuery == null || asciiQuery.isEmpty()) {
      return Collections.emptyList();
    }
    String normalized = asciiQuery.indexOf('.') >= 0 ? asciiQuery.replace(".", "") : asciiQuery;
    String key = normalized.isEmpty() ? asciiQuery : normalized;
    List<SingleAnalysis> res = asciiInformalAnalyzer.analyze(key);
    if (res.size() == 1 && res.getFirst().getDictionaryItem().isUnknown()) {
      return Collections.emptyList();
    }
    return res;
  }

  private void addGluedCliticCandidates(
      String asciiLower, String originalForPins, List<Candidate> out) {
    for (String clitic : GLUED_CLITICS) {
      if (asciiLower.length() <= clitic.length() + 2 || !asciiLower.endsWith(clitic)) {
        continue;
      }
      String stemAscii = asciiLower.substring(0, asciiLower.length() - clitic.length());
      boolean added = false;
      for (Candidate stemCand : analyzePlainAsciiUncached(stemAscii)) {
        String proto = selectWordPrototypeNoun(stemCand.surface);
        boolean front = isFrontVowelWord(proto);
        boolean rounded = isRoundedVowelWord(proto);
        String restoredClitic = harmonizeClitic(clitic, front, rounded);
        String full = stemCand.surface + restoredClitic;
        if ((originalForPins == null
                || TurkishTextCodec.isCompatibleWithInput(originalForPins, full))
            && Candidate.addIfSurfaceAbsent(
                out, copyWithSurface(stemCand, full, true, clitic.startsWith("m")))) {
          added = true;
        }
      }
      if (added) {
        return;
      }
    }
  }

  private WordGenerator generatorFor(SingleAnalysis sa) {
    return sa.containsInformalMorpheme() ? informalGenerator : formalGenerator;
  }

  private static String projectCircumflexIfPresent(String generatedLower, DictionaryItem item) {
    String normLemma = item.normalizedLemma().toLowerCase(Turkish.LOCALE);
    if (!TurkishAlphabet.INSTANCE.containsCircumflex(normLemma)) {
      return null;
    }
    String projected = projectLemmaCircumflex(generatedLower, normLemma);
    return projected.equals(generatedLower) ? null : projected;
  }

  private static boolean isOnlyProperOrAbbrev(List<Candidate> cands) {
    for (Candidate c : cands) {
      if (c.secondaryPos != SecondaryPos.ProperNoun
          && c.secondaryPos != SecondaryPos.Abbreviation) {
        return false;
      }
    }
    return true;
  }

  private static String harmonizeClitic(String asciiClitic, boolean front, boolean rounded) {
    if (asciiClitic.equals("da")
        || asciiClitic.equals("de")
        || asciiClitic.equals("ta")
        || asciiClitic.equals("te")) {
      return "" + asciiClitic.charAt(0) + (front ? 'e' : 'a');
    }
    char highVowel = TurkishTextCodec.highHarmonicVowel(front, rounded);
    StringBuilder sb = new StringBuilder(asciiClitic.length());
    for (int i = 0; i < asciiClitic.length(); i++) {
      char c = asciiClitic.charAt(i);
      sb.append((c == 'i' || c == 'u') ? highVowel : c);
    }
    return sb.toString();
  }

  private static List<String> selectPrototypeNouns(String spoken, boolean isUpperAcronym) {
    if (spoken == null || spoken.isEmpty()) {
      return List.of("kale");
    }
    int digitStart = spoken.length();
    while (digitStart > 0 && Character.isDigit(spoken.charAt(digitStart - 1))) {
      digitStart--;
    }
    if (digitStart < spoken.length()) {
      String spokenNumber = TurkishTextCodec.spokenTurkishNumberStem(spoken.substring(digitStart));
      return List.of(selectWordPrototypeNoun(spokenNumber));
    }

    String lower = spoken.toLowerCase(Turkish.LOCALE);
    String wordProto = selectWordPrototypeNoun(lower);
    if (!isUpperAcronym) {
      return List.of(wordProto);
    }

    char lastChar = lower.charAt(lower.length() - 1);
    String letterNameProto = lastChar == 'k' ? "oda" : "kale";
    if (isLetterByLetterInitialism(lower)) {
      return letterNameProto.equals(wordProto)
          ? List.of(letterNameProto)
          : List.of(letterNameProto, wordProto);
    }
    return wordProto.equals(letterNameProto)
        ? List.of(wordProto)
        : List.of(wordProto, letterNameProto);
  }

  private static boolean isLetterByLetterInitialism(String lower) {
    if (lower.length() <= 2) {
      return true;
    }
    boolean anyVowel = false;
    for (int i = 0; i < lower.length(); i++) {
      if (TurkishTextCodec.isTurkishVowel(lower.charAt(i))) {
        anyVowel = true;
        break;
      }
    }
    if (!anyVowel) {
      return true;
    }
    int n = lower.length();
    return !TurkishTextCodec.isTurkishVowel(lower.charAt(n - 1))
        && !TurkishTextCodec.isTurkishVowel(lower.charAt(n - 2));
  }

  private static String selectWordPrototypeNoun(String spoken) {
    String lower = spoken.toLowerCase(Turkish.LOCALE);
    int vowelClass = -1;
    for (int i = lower.length() - 1; i >= 0; i--) {
      vowelClass = TurkishTextCodec.vowelIndex(lower.charAt(i));
      if (vowelClass >= 0) {
        break;
      }
    }
    if (vowelClass < 0) {
      char lastChar = lower.isEmpty() ? 'e' : lower.charAt(lower.length() - 1);
      return lastChar == 'k' ? "oda" : "kale";
    }
    char last = lower.charAt(lower.length() - 1);
    int endingType =
        TurkishTextCodec.isTurkishVowel(last) ? 0 : ("fstkçşhp".indexOf(last) >= 0 ? 2 : 1);
    return PROTOTYPE_STEMS[vowelClass][endingType];
  }

  private static boolean isFrontVowelWord(String s) {
    for (int i = s.length() - 1; i >= 0; i--) {
      char c = s.charAt(i);
      if (TurkishTextCodec.isTurkishVowel(c)) {
        return TurkishTextCodec.isFrontVowel(c);
      }
    }
    return true;
  }

  private static boolean isRoundedVowelWord(String s) {
    for (int i = s.length() - 1; i >= 0; i--) {
      char c = s.charAt(i);
      if (TurkishTextCodec.isTurkishVowel(c)) {
        return TurkishTextCodec.isRoundedVowel(c);
      }
    }
    return false;
  }

  private static String projectLemmaCircumflex(String generatedLower, String normLemmaLower) {
    int limit = Math.min(generatedLower.length(), normLemmaLower.length());
    char[] chars = null;
    for (int i = 0; i < limit; i++) {
      char lc = normLemmaLower.charAt(i);
      if (lc == 'â' || lc == 'î' || lc == 'û') {
        char base = TurkishAlphabet.INSTANCE.normalizeCircumflex(lc);
        if (generatedLower.charAt(i) == base) {
          if (chars == null) {
            chars = generatedLower.toCharArray();
          }
          chars[i] = lc;
        }
      }
    }
    return chars == null ? generatedLower : new String(chars);
  }

  private static Candidate createAbbreviationCandidate(String lowerSurface) {
    return new Candidate(
        lowerSurface,
        lowerSurface,
        PrimaryPos.Noun,
        SecondaryPos.Abbreviation,
        PrimaryPos.Noun,
        "Nom",
        "Pnon",
        false,
        false,
        false,
        false,
        false,
        false,
        0,
        1);
  }

  private static Candidate copyWithSurface(
      Candidate src, String surface, boolean informal, boolean question) {
    return new Candidate(
        surface,
        src.lemma,
        src.pos,
        src.secondaryPos,
        src.lastGroupPos,
        src.caseMorpheme,
        src.possessiveMorpheme,
        src.plural,
        informal,
        src.negativeOrImperative,
        question,
        src.circumflexVariant,
        false,
        0,
        src.morphemeGroupCount);
  }

  private static final class CandidateCollector {
    private record ScoredCandidate(Candidate candidate, int score) {}

    private final Map<String, ScoredCandidate> bySurface;

    CandidateCollector(int expectedSize) {
      this.bySurface = new LinkedHashMap<>(expectedSize);
    }

    boolean isEmpty() {
      return bySurface.isEmpty();
    }

    void register(
        String surface, SingleAnalysis sa, boolean isCircumflexVariant, int foreignStemLen) {
      int scoreNew = preferenceScore(sa, isCircumflexVariant);
      ScoredCandidate existing = bySurface.get(surface);
      if (existing != null && scoreNew < existing.score()) {
        return;
      }
      bySurface.put(
          surface,
          new ScoredCandidate(
              buildCandidate(surface, sa, isCircumflexVariant, foreignStemLen), scoreNew));
    }

    List<Candidate> toList() {
      List<Candidate> list = new ArrayList<>(bySurface.size());
      for (ScoredCandidate sc : bySurface.values()) {
        list.add(sc.candidate());
      }
      return list;
    }
  }

  private static Candidate buildCandidate(
      String surface, SingleAnalysis sa, boolean isCircumflexVariant, int foreignStemLen) {
    DictionaryItem item = sa.getDictionaryItem();
    String caseId = "Nom";
    String possId = "Pnon";
    boolean plural = false;
    boolean negOrImp = false;
    boolean question =
        item.primaryPos == PrimaryPos.Question || sa.containsMorpheme(TurkishMorphotactics.ques);

    for (Morpheme m : sa.getMorphemes()) {
      switch (m.id) {
        case "Acc", "Dat", "Loc", "Abl", "Gen", "Ins", "Equ" -> caseId = m.id;
        case "P1sg", "P2sg", "P3sg", "P1pl", "P2pl", "P3pl" -> possId = m.id;
        case "A3pl" -> plural = true;
        case "Neg", "Imp", "Without" -> negOrImp = true;
        default -> {}
      }
    }

    String lemma = item.lemma.toLowerCase(Turkish.LOCALE);
    if (!isCircumflexVariant) {
      lemma = TurkishAlphabet.INSTANCE.normalizeCircumflex(lemma);
    }

    return new Candidate(
        surface,
        lemma,
        item.primaryPos,
        item.secondaryPos,
        sa.getPos(),
        caseId,
        possId,
        plural,
        sa.containsInformalMorpheme(),
        negOrImp,
        question,
        isCircumflexVariant,
        false,
        foreignStemLen,
        sa.groupCount());
  }

  private static SingleAnalysis selectBestLexiconAnalysis(
      WordAnalysis wa, boolean surfaceHasCircumflex) {
    SingleAnalysis best = null;
    int bestScore = Integer.MIN_VALUE;
    for (SingleAnalysis sa : wa) {
      if (isLexiconAnalysis(sa)) {
        int s = preferenceScore(sa, surfaceHasCircumflex);
        if (s > bestScore) {
          bestScore = s;
          best = sa;
        }
      }
    }
    return best;
  }

  private static boolean isLexiconAnalysis(SingleAnalysis sa) {
    return !sa.isUnknown()
        && !sa.getDictionaryItem().hasAttribute(RootAttribute.Runtime)
        && !isCorruptedDotlessProperNoun(sa.getDictionaryItem());
  }

  /**
   * Filters English/ASCII proper nouns in Zemberek's {@code proper-from-corpus.dict} (such as
   * {@code Istanbul}, {@code Ibrahim}, {@code Internet}, {@code Italya}, {@code Isviçre}) whose
   * uppercase ASCII {@code 'I'} was lowercased with {@code Turkish.LOCALE} into dotless {@code 'ı'},
   * while preserving genuine Turkish dotless-{@code I} proper nouns ({@code Iğdır}, {@code Ilgaz},
   * {@code Ilgın}, {@code Ilıca}, {@code Irak}, {@code Irmak}, {@code Isfahan}, {@code Isparta},
   * {@code Istranca}, {@code Ihlara}, {@code Işık}, {@code Işıl}, {@code Itri}).
   */
  private static boolean isCorruptedDotlessProperNoun(DictionaryItem item) {
    if (item.secondaryPos != SecondaryPos.ProperNoun || !item.root.startsWith("ı")) {
      return false;
    }
    String r = item.root;
    return !(r.startsWith("ığ")
        || r.startsWith("ıhl")
        || r.startsWith("ıld")
        || r.startsWith("ılgaz")
        || r.startsWith("ılgın")
        || r.startsWith("ılı")
        || r.startsWith("ırak")
        || r.startsWith("ıraz")
        || r.startsWith("ırg")
        || r.startsWith("ırmak")
        || r.startsWith("ısfahan")
        || r.startsWith("ısparta")
        || r.startsWith("ıstranca")
        || r.startsWith("ıstıranca")
        || r.startsWith("ışı")
        || r.startsWith("ıtr"));
  }

  private static int preferenceScore(SingleAnalysis sa, boolean surfaceHasCircumflex) {
    DictionaryItem item = sa.getDictionaryItem();
    int s = -sa.groupCount() * 10 + Math.min(8, item.lemma.length()) * 2;
    if (sa.containsInformalMorpheme()) {
      s -= 8;
    }
    if (item.secondaryPos == SecondaryPos.ProperNoun) {
      s -= 4;
    }
    if (item.secondaryPos == SecondaryPos.Abbreviation) {
      s -= 6;
    }
    if (item.hasAttribute(RootAttribute.Ext)) {
      s -= 4;
    }
    boolean lemmaHasCirc =
        TurkishAlphabet.INSTANCE.containsCircumflex(
            item.normalizedLemma().toLowerCase(Turkish.LOCALE));
    if (surfaceHasCircumflex == lemmaHasCirc) {
      s += 4;
    }
    return s;
  }
}
