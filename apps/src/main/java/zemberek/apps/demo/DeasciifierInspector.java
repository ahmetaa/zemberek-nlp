package zemberek.apps.demo;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.normalization.deasciifier.latticedeasciifier.Candidate;
import zemberek.normalization.deasciifier.latticedeasciifier.DeasciifierModel;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier.CircumflexMode;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * Runs and inspects Zemberek's deasciifier engines and modes side-by-side for the Web Demo Studio
 * ({@code LatticeDeasciifier} across {@code DISAMBIGUATION_ONLY}, {@code NONE}, {@code FULL}, and
 * {@code KnownWordsOnly} modes, plus the classic {@code Deasciifier} decision-list pattern table).
 */
public final class DeasciifierInspector {

  private static final int MAX_MORPH_INSPECT_TOKENS = 120;

  private final TurkishMorphology morphology;
  private final DeasciifierModel deasciifierModel;
  private final LatticeDeasciifier latticeDefault;
  private final LatticeReflection latticeReflection;

  public DeasciifierInspector(TurkishMorphology morphology) {
    this.morphology = morphology;
    this.deasciifierModel = DeasciifierModel.loadDefault();
    this.latticeDefault = new LatticeDeasciifier(deasciifierModel, morphology);
    this.latticeReflection = LatticeReflection.tryInit();
  }

  public record DeasciifierEngineMeta(
      String id,
      String label,
      String shortLabel,
      String ramLabel,
      String sentAccLabel,
      String wordAccLabel,
      String pinSafetyLabel,
      String modeNote) {}

  public record LatticeCandidateDto(
      String surface,
      String lemma,
      String morphSummary,
      double empPrior,
      double charScore,
      double unaryScore,
      double seqScore,
      boolean oovFallback,
      boolean winner) {}

  public record TokenEngineVariantDto(
      String engineId,
      String shortLabel,
      String surface,
      boolean matchesSelected,
      List<String> morphParses) {}

  public record DeasciifierTokenDto(
      int index,
      int start,
      int end,
      String rawSurface,
      String restoredSurface,
      boolean changed,
      String changeKind,
      int charDiffCount,
      boolean immutable,
      boolean hasPinnedTurkish,
      String casing,
      boolean engineDisagreement,
      List<String> selectedMorphParses,
      List<LatticeCandidateDto> latticeCandidates,
      List<TokenEngineVariantDto> variants) {}

  public record DeasciifierComparisonDto(
      String id,
      String label,
      String shortLabel,
      String ramLabel,
      String outputText,
      double latencyMs,
      int changedWords,
      int changedChars,
      int circumflexCount,
      int strippedPinCount,
      boolean differsFromSelected) {}

  public record DeasciifyResponse(
      String inputText,
      String engine,
      String deasciifiedText,
      double latencyMs,
      int totalTokens,
      int changedWords,
      int changedChars,
      int circumflexCount,
      int strippedPinCount,
      int disagreementCount,
      boolean sameLength,
      List<DeasciifierTokenDto> tokens,
      List<DeasciifierComparisonDto> comparisons) {}

  private record EngineRunResult(DeasciifierEngineMeta meta, String output, double latencyMs) {}

  public static List<DeasciifierEngineMeta> engineMetadata() {
    return List.of(
        new DeasciifierEngineMeta(
            "lattice_disambig",
            "LatticeDeasciifier — Disambiguating â/î/û (Default)",
            "Lattice (Disambig â/î/û)",
            "3.58 MB",
            "94.88% / 89.80%",
            "99.57%",
            "0 stripped",
            "Morphological lattice + structured margin perceptron; emits â/î/û on minimal pairs (kâr/kar, hâlâ/hala, âdet, hâkim, resmî) + OOV vowel harmony"),
        new DeasciifierEngineMeta(
            "lattice_none",
            "LatticeDeasciifier — Plain Diacritics (No Circumflex)",
            "Lattice (No â/î/û)",
            "3.58 MB (shared)",
            "94.88% (No^)",
            "99.57%",
            "0 stripped",
            "Restores modern Turkish diacritics (ç, ğ, ı, İ, ö, ş, ü) only; never inserts new circumflexes"),
        new DeasciifierEngineMeta(
            "lattice_full",
            "LatticeDeasciifier — Full Circumflex (Lexicon â/î/û)",
            "Lattice (Full â/î/û)",
            "3.58 MB (shared)",
            "94.88% / 89.80%",
            "99.57%",
            "0 stripped",
            "Emits circumflexes on all lexicon-supported words (e.g. rüzgâr, hikâye, dükkân, imkân) as well as minimal pairs"),
        new DeasciifierEngineMeta(
            "lattice_known",
            "LatticeDeasciifier — Known Words Only (OOV-Safe)",
            "Lattice (Known Only)",
            "3.58 MB (shared)",
            "OOV-Safe Mode",
            "99.57% (in-vocab)",
            "0 stripped · OOV kept",
            "Restores lexicon, clitic, and numeral-backed tokens only; leaves OOV typos & unknown foreign words untouched"),
        new DeasciifierEngineMeta(
            "legacy_pattern",
            "Legacy Deasciifier (2006 Decision-List Pattern Table)",
            "Legacy Pattern (2006)",
            "572 KB",
            "83.76% / 77.16%",
            "98.52%",
            "12 stripped / 500",
            "Classic character-context (±10 window) decision list; fast baseline without morphology, circumflexes, or foreign-stem shields"));
  }

  public void warmup() {
    String sample = "Bu yil sirketin net kar orani artti ama kar yagisi hala suruyor.";
    latticeDefault.deasciify(sample, CircumflexMode.DISAMBIGUATION_ONLY);
    latticeDefault.deasciify(sample, CircumflexMode.NONE);
    latticeDefault.deasciify(sample, CircumflexMode.FULL);
    latticeDefault.deasciifyKnownWords(sample, CircumflexMode.DISAMBIGUATION_ONLY);
    Deasciifier.deasciify(sample);
  }

  public DeasciifyResponse deasciifyAndInspect(
      String text, String requestedEngine, boolean compareAll) {
    String input = text == null ? "" : text;
    List<DeasciifierEngineMeta> metas = engineMetadata();
    DeasciifierEngineMeta primaryMeta =
        metas.stream()
            .filter(m -> m.id().equals(requestedEngine))
            .findFirst()
            .orElse(metas.get(0));
    String selectedId = primaryMeta.id();

    if (input.isBlank()) {
      return new DeasciifyResponse(
          input, selectedId, input, 0.0, 0, 0, 0, 0, 0, 0, true, List.of(), List.of());
    }

    EngineRunResult primaryRun = runSingleEngine(primaryMeta, input);
    String primaryOut = primaryRun.output();
    boolean sameLength = primaryOut.length() == input.length();
    boolean sentenceHasNonDotlessTr = TurkishTextCodec.containsNonDotlessTurkishDiacritic(input);

    Map<String, EngineRunResult> allRuns = new LinkedHashMap<>();
    if (compareAll) {
      for (DeasciifierEngineMeta m : metas) {
        if (m.id().equals(selectedId)) {
          allRuns.put(m.id(), primaryRun);
        } else {
          allRuns.put(m.id(), runSingleEngine(m, input));
        }
      }
    }

    List<Token> codecTokens = TurkishTextCodec.tokenize(input);
    List<List<LatticeCandidateDto>> perTokenLatticeCands =
        codecTokens.size() <= MAX_MORPH_INSPECT_TOKENS && latticeReflection != null
            ? latticeReflection.inspectSentenceLattice(latticeDefault, deasciifierModel, input)
            : List.of();
    Map<String, List<String>> morphCache = new LinkedHashMap<>();

    int changedWords = 0;
    int disagreementCount = 0;
    List<DeasciifierTokenDto> tokenDtos = new ArrayList<>(codecTokens.size());

    for (int i = 0; i < codecTokens.size(); i++) {
      Token tok = codecTokens.get(i);
      String rawSurf = tok.original();
      String restoredSurf =
          sameLength ? primaryOut.substring(tok.start(), tok.end()) : rawSurf;
      boolean changed = !rawSurf.equals(restoredSurf);
      if (changed) {
        changedWords++;
      }

      int charDiffs = countCharDiffs(rawSurf, restoredSurf);
      String changeKind = classifyTokenChange(rawSurf, restoredSurf, sentenceHasNonDotlessTr);

      boolean inspectMorph = i < MAX_MORPH_INSPECT_TOKENS;
      List<String> selectedParses =
          inspectMorph ? lookupMorphParses(restoredSurf, morphCache) : List.of();
      List<LatticeCandidateDto> latCands =
          i < perTokenLatticeCands.size() ? perTokenLatticeCands.get(i) : List.of();

      boolean disagrees = false;
      List<TokenEngineVariantDto> variants = List.of();
      if (compareAll && !allRuns.isEmpty()) {
        Set<String> distinctSurfaces = new LinkedHashSet<>();
        List<TokenEngineVariantDto> vList = new ArrayList<>(allRuns.size());
        for (EngineRunResult er : allRuns.values()) {
          String vSurf =
              er.output().length() == input.length()
                  ? er.output().substring(tok.start(), tok.end())
                  : rawSurf;
          distinctSurfaces.add(vSurf);
          List<String> vParses =
              inspectMorph ? lookupMorphParses(vSurf, morphCache) : List.of();
          vList.add(
              new TokenEngineVariantDto(
                  er.meta().id(),
                  er.meta().shortLabel(),
                  vSurf,
                  vSurf.equals(restoredSurf),
                  vParses));
        }
        disagrees = distinctSurfaces.size() > 1;
        if (disagrees) {
          disagreementCount++;
        }
        variants = vList;
      }

      tokenDtos.add(
          new DeasciifierTokenDto(
              i,
              tok.start(),
              tok.end(),
              rawSurf,
              restoredSurf,
              changed,
              changeKind,
              charDiffs,
              tok.immutable(),
              tok.hasPinnedTurkish(),
              tok.casing().name(),
              disagrees,
              selectedParses,
              latCands,
              variants));
    }

    CharStats primaryCharStats = computeCharStats(input, primaryOut, sentenceHasNonDotlessTr);

    List<DeasciifierComparisonDto> compDtos = new ArrayList<>();
    if (compareAll) {
      for (EngineRunResult er : allRuns.values()) {
        String out = er.output();
        int cWords = countChangedTokens(codecTokens, input, out);
        CharStats cs = computeCharStats(input, out, sentenceHasNonDotlessTr);
        compDtos.add(
            new DeasciifierComparisonDto(
                er.meta().id(),
                er.meta().label(),
                er.meta().shortLabel(),
                er.meta().ramLabel(),
                out,
                roundMs(er.latencyMs()),
                cWords,
                cs.changedChars(),
                cs.circumflexCount(),
                cs.strippedPinCount(),
                !out.equals(primaryOut)));
      }
    }

    return new DeasciifyResponse(
        input,
        selectedId,
        primaryOut,
        roundMs(primaryRun.latencyMs()),
        codecTokens.size(),
        changedWords,
        primaryCharStats.changedChars(),
        primaryCharStats.circumflexCount(),
        primaryCharStats.strippedPinCount(),
        disagreementCount,
        sameLength,
        tokenDtos,
        compDtos);
  }

  private EngineRunResult runSingleEngine(DeasciifierEngineMeta meta, String input) {
    long t0 = System.nanoTime();
    String out =
        switch (meta.id()) {
          case "lattice_disambig" ->
              latticeDefault.deasciify(input, CircumflexMode.DISAMBIGUATION_ONLY);
          case "lattice_none" -> latticeDefault.deasciify(input, CircumflexMode.NONE);
          case "lattice_full" -> latticeDefault.deasciify(input, CircumflexMode.FULL);
          case "lattice_known" ->
              latticeDefault.deasciifyKnownWords(input, CircumflexMode.DISAMBIGUATION_ONLY);
          case "legacy_pattern" -> Deasciifier.deasciify(input);
          default -> latticeDefault.deasciify(input, CircumflexMode.DISAMBIGUATION_ONLY);
        };
    double ms = (System.nanoTime() - t0) / 1e6;
    return new EngineRunResult(meta, out, ms);
  }

  private List<String> lookupMorphParses(String surface, Map<String, List<String>> cache) {
    if (surface == null || surface.isBlank()) {
      return List.of();
    }
    List<String> cached = cache.get(surface);
    if (cached != null) {
      return cached;
    }
    List<String> res = new ArrayList<>(3);
    try {
      WordAnalysis wa = morphology.analyze(surface);
      for (SingleAnalysis sa : wa) {
        if (!sa.isUnknown()) {
          res.add(sa.formatLong());
          if (res.size() >= 4) {
            break;
          }
        }
      }
    } catch (Exception ignored) {
      // Ignore unanalyzable punctuation/tokens
    }
    cache.put(surface, res);
    return res;
  }

  private record CharStats(int changedChars, int circumflexCount, int strippedPinCount) {}

  private static CharStats computeCharStats(
      String input, String output, boolean sentenceHasNonDotlessTr) {
    int n = Math.min(input.length(), output.length());
    int changed = Math.abs(input.length() - output.length());
    int circ = 0;
    int stripped = 0;
    for (int i = 0; i < n; i++) {
      char r = input.charAt(i);
      char o = output.charAt(i);
      if (r == o) {
        continue;
      }
      changed++;
      if (isCircumflex(o) && !isCircumflex(r)) {
        circ++;
      }
      if (TurkishTextCodec.isPinnedTurkishDiacritic(r)
          && !TurkishTextCodec.isPinnedTurkishDiacritic(o)) {
        if (!(r == 'ı' && o == 'i' && !sentenceHasNonDotlessTr)) {
          stripped++;
        }
      }
    }
    return new CharStats(changed, circ, stripped);
  }

  private static int countChangedTokens(List<Token> tokens, String input, String output) {
    if (input.length() != output.length()) {
      return 0;
    }
    int count = 0;
    for (Token t : tokens) {
      if (!t.original().equals(output.substring(t.start(), t.end()))) {
        count++;
      }
    }
    return count;
  }

  private static int countCharDiffs(String a, String b) {
    int n = Math.min(a.length(), b.length());
    int diffs = Math.abs(a.length() - b.length());
    for (int i = 0; i < n; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        diffs++;
      }
    }
    return diffs;
  }

  private static String classifyTokenChange(
      String raw, String restored, boolean sentenceHasNonDotlessTr) {
    if (raw.equals(restored)) {
      return "unchanged";
    }
    boolean strippedPin = false;
    boolean addedCircumflex = false;
    int n = Math.min(raw.length(), restored.length());
    for (int i = 0; i < n; i++) {
      char r = raw.charAt(i);
      char o = restored.charAt(i);
      if (r == o) {
        continue;
      }
      if (TurkishTextCodec.isPinnedTurkishDiacritic(r)
          && !TurkishTextCodec.isPinnedTurkishDiacritic(o)) {
        if (!(r == 'ı' && o == 'i' && !sentenceHasNonDotlessTr)) {
          strippedPin = true;
        }
      }
      if (isCircumflex(o) && !isCircumflex(r)) {
        addedCircumflex = true;
      }
    }
    if (strippedPin) {
      return "stripped_pin";
    }
    if (addedCircumflex) {
      return "circumflex";
    }
    return "diacritic";
  }

  private static boolean isCircumflex(char c) {
    return c == 'â' || c == 'Â' || c == 'î' || c == 'Î' || c == 'û' || c == 'Û';
  }

  private static double roundMs(double ms) {
    return Math.round(ms * 1000.0) / 1000.0;
  }

  private static double round2(double v) {
    return Math.round(v * 100.0) / 100.0;
  }

  private static final class LatticeReflection {
    private final Method buildSentenceLattice;
    private final Method computeAmbiguousUnaryScores;
    private final Method decodeSentence;
    private final Method scoreCandidateInSequence;
    private final Field fCandidates;
    private final Field fEmpPriors;
    private final Field fCharScores;
    private final Field fUnaryScores;

    private LatticeReflection(
        Method buildSentenceLattice,
        Method computeAmbiguousUnaryScores,
        Method decodeSentence,
        Method scoreCandidateInSequence,
        Field fCandidates,
        Field fEmpPriors,
        Field fCharScores,
        Field fUnaryScores) {
      this.buildSentenceLattice = buildSentenceLattice;
      this.computeAmbiguousUnaryScores = computeAmbiguousUnaryScores;
      this.decodeSentence = decodeSentence;
      this.scoreCandidateInSequence = scoreCandidateInSequence;
      this.fCandidates = fCandidates;
      this.fEmpPriors = fEmpPriors;
      this.fCharScores = fCharScores;
      this.fUnaryScores = fUnaryScores;
    }

    static LatticeReflection tryInit() {
      try {
        Class<?> tlClass =
            Class.forName(
                "zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier$TokenLattice");
        Class<?> tlArrayClass = Array.newInstance(tlClass, 0).getClass();
        Class<?> wtClass =
            Class.forName("zemberek.normalization.deasciifier.latticedeasciifier.WeightTable");
        Class<?> scorerClass =
            Class.forName(
                "zemberek.normalization.deasciifier.latticedeasciifier.SentenceLatticeScorer");

        Method mBuild =
            LatticeDeasciifier.class.getDeclaredMethod(
                "buildSentenceLattice", String.class, List.class);
        mBuild.setAccessible(true);

        Method mUnary =
            LatticeDeasciifier.class.getDeclaredMethod(
                "computeAmbiguousUnaryScores", List.class, tlArrayClass, wtClass, long[].class);
        mUnary.setAccessible(true);

        Method mDecode =
            LatticeDeasciifier.class.getDeclaredMethod(
                "decodeSentence", List.class, tlArrayClass, wtClass);
        mDecode.setAccessible(true);

        Method mScoreSeq =
            scorerClass.getDeclaredMethod(
                "scoreCandidateInSequence",
                int.class,
                Candidate[].class,
                int.class,
                Candidate.class,
                float.class,
                wtClass,
                long[].class);
        mScoreSeq.setAccessible(true);

        Field fc = tlClass.getDeclaredField("candidates");
        fc.setAccessible(true);
        Field fp = tlClass.getDeclaredField("empiricalLogPriors");
        fp.setAccessible(true);
        Field fch = tlClass.getDeclaredField("charRelativeScores");
        fch.setAccessible(true);
        Field fu = tlClass.getDeclaredField("unaryScores");
        fu.setAccessible(true);

        return new LatticeReflection(mBuild, mUnary, mDecode, mScoreSeq, fc, fp, fch, fu);
      } catch (Exception e) {
        return null;
      }
    }

    @SuppressWarnings("unchecked")
    List<List<LatticeCandidateDto>> inspectSentenceLattice(
        LatticeDeasciifier deasciifier, DeasciifierModel model, String input) {
      try {
        List<Token> mutTokens = TurkishTextCodec.tokenize(input);
        int n = mutTokens.size();
        if (n == 0) {
          return List.of();
        }
        Object cols = buildSentenceLattice.invoke(deasciifier, input, mutTokens);
        long[] featBuf = new long[48];
        Object weights = model.sentenceWeights();
        computeAmbiguousUnaryScores.invoke(deasciifier, mutTokens, cols, weights, featBuf);
        Candidate[] chosen = (Candidate[]) decodeSentence.invoke(deasciifier, mutTokens, cols, weights);

        List<List<LatticeCandidateDto>> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
          Token tok = mutTokens.get(i);
          Object col = Array.get(cols, i);
          List<Candidate> cands = (List<Candidate>) fCandidates.get(col);
          float[] priors = (float[]) fEmpPriors.get(col);
          float[] chars = (float[]) fCharScores.get(col);
          float[] unary = (float[]) fUnaryScores.get(col);

          List<LatticeCandidateDto> dtos = new ArrayList<>(cands.size());
          for (int k = 0; k < cands.size(); k++) {
            Candidate c = cands.get(k);
            String casedSurf = TurkishTextCodec.applyCasingAndPins(tok, c);
            String morphSummary =
                c.oovFallback
                    ? "OOV"
                    : (c.pos.shortForm
                        + (!"None".equals(c.secondaryPos.shortForm)
                            ? "/" + c.secondaryPos.shortForm
                            : "")
                        + " · "
                        + c.caseMorpheme
                        + " · "
                        + c.possessiveMorpheme);
            float u = unary != null && k < unary.length ? unary[k] : 0.0f;
            float seq =
                cands.size() >= 2
                    ? (float)
                        scoreCandidateInSequence.invoke(
                            null, i, chosen, n, c, u, weights, featBuf)
                    : u;
            dtos.add(
                new LatticeCandidateDto(
                    casedSurf,
                    c.lemma,
                    morphSummary,
                    round2(k < priors.length ? priors[k] : 0.0),
                    round2(k < chars.length ? chars[k] : 0.0),
                    round2(u),
                    round2(seq),
                    c.oovFallback,
                    c == chosen[i]));
          }
          dtos.sort((a, b) -> Double.compare(b.seqScore(), a.seqScore()));
          out.add(dtos);
        }
        return out;
      } catch (Exception e) {
        return List.of();
      }
    }
  }
}
