package zemberek.apps.demo;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
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
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * Runs and inspects Zemberek's default {@link LatticeDeasciifier} alongside the classic
 * pattern-based {@link Deasciifier}.
 */
public final class DeasciifierInspector {

  private static final int MAX_MORPH_INSPECT_TOKENS = 120;

  private static final List<DeasciifierEngineMeta> ENGINE_METAS =
      List.of(
          new DeasciifierEngineMeta(
              "lattice",
              "Lattice Deasciifier",
              "Morphological lattice + structured perceptron; restores Turkish diacritics, minimal-pair circumflexes (kâr/kar, hâlâ/hala), and OOV vowel harmony."),
          new DeasciifierEngineMeta(
              "legacy",
              "Legacy Deasciifier",
              "Classic character-context decision list without morphological analysis or foreign-stem shielding."));

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

  public record DeasciifierEngineMeta(String id, String name, String description) {}

  public record LatticeCandidateDto(
      String surface,
      String lemma,
      String morphSummary,
      double empPrior,
      double charScore,
      double unaryScore,
      double seqScore,
      boolean winner) {}

  public record TokenEngineVariantDto(
      String name, String surface, boolean matchesSelected, List<String> morphParses) {}

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
      String name,
      String outputText,
      double latencyMs,
      int changedWords,
      int changedChars,
      int circumflexCount,
      int strippedPinCount) {}

  public record DeasciifyResponse(
      String inputText,
      String engine,
      String deasciifiedText,
      double latencyMs,
      int totalTokens,
      int changedWords,
      int changedChars,
      int circumflexCount,
      int disagreementCount,
      boolean sameLength,
      List<DeasciifierTokenDto> tokens,
      List<DeasciifierComparisonDto> comparisons) {}

  private record EngineRunResult(DeasciifierEngineMeta meta, String output, double latencyMs) {}

  private record CharStats(int changedChars, int circumflexCount, int strippedPinCount) {}

  public static List<DeasciifierEngineMeta> engineMetadata() {
    return ENGINE_METAS;
  }

  public void warmup() {
    String sample = "Bu yil sirketin net kar orani artti ama kar yagisi hala suruyor.";
    latticeDefault.deasciify(sample);
    Deasciifier.deasciify(sample);
  }

  public DeasciifyResponse deasciifyAndInspect(
      String text, String requestedEngine, boolean compareAll) {
    String input = text == null ? "" : text;
    DeasciifierEngineMeta primaryMeta =
        ENGINE_METAS.stream()
            .filter(m -> m.id().equals(requestedEngine))
            .findFirst()
            .orElse(ENGINE_METAS.get(0));
    String selectedId = primaryMeta.id();

    if (input.isBlank()) {
      return new DeasciifyResponse(
          input, selectedId, input, 0.0, 0, 0, 0, 0, 0, true, List.of(), List.of());
    }

    EngineRunResult primaryRun = runSingleEngine(primaryMeta, input);
    String primaryOut = primaryRun.output();
    boolean sameLength = primaryOut.length() == input.length();
    boolean sentenceHasNonDotlessTr = TurkishTextCodec.containsNonDotlessTurkishDiacritic(input);

    Map<String, EngineRunResult> allRuns = new LinkedHashMap<>();
    if (compareAll) {
      for (DeasciifierEngineMeta m : ENGINE_METAS) {
        allRuns.put(m.id(), m.id().equals(selectedId) ? primaryRun : runSingleEngine(m, input));
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
      String restoredSurf = sameLength ? primaryOut.substring(tok.start(), tok.end()) : rawSurf;
      boolean changed = !rawSurf.equals(restoredSurf);
      if (changed) {
        changedWords++;
      }

      CharStats tokStats = computeCharStats(rawSurf, restoredSurf, sentenceHasNonDotlessTr);
      String changeKind =
          !changed
              ? "unchanged"
              : tokStats.strippedPinCount() > 0
                  ? "stripped_pin"
                  : tokStats.circumflexCount() > 0 ? "circumflex" : "diacritic";

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
          List<String> vParses = inspectMorph ? lookupMorphParses(vSurf, morphCache) : List.of();
          vList.add(
              new TokenEngineVariantDto(
                  er.meta().name(), vSurf, vSurf.equals(restoredSurf), vParses));
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
              tokStats.changedChars(),
              tok.immutable(),
              tok.hasPinnedTurkish(),
              tok.casing().name(),
              disagrees,
              selectedParses,
              latCands,
              variants));
    }

    CharStats primaryCharStats = computeCharStats(input, primaryOut, sentenceHasNonDotlessTr);
    List<DeasciifierComparisonDto> compDtos = new ArrayList<>(allRuns.size());
    for (EngineRunResult er : allRuns.values()) {
      String out = er.output();
      CharStats cs = computeCharStats(input, out, sentenceHasNonDotlessTr);
      compDtos.add(
          new DeasciifierComparisonDto(
              er.meta().id(),
              er.meta().name(),
              out,
              roundMs(er.latencyMs()),
              countChangedTokens(codecTokens, input, out),
              cs.changedChars(),
              cs.circumflexCount(),
              cs.strippedPinCount()));
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
        disagreementCount,
        sameLength,
        tokenDtos,
        compDtos);
  }

  private EngineRunResult runSingleEngine(DeasciifierEngineMeta meta, String input) {
    long t0 = System.nanoTime();
    String out =
        "legacy".equals(meta.id()) ? Deasciifier.deasciify(input) : latticeDefault.deasciify(input);
    return new EngineRunResult(meta, out, (System.nanoTime() - t0) / 1e6);
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
      if (hasCircumflex(o) && !hasCircumflex(r)) {
        circ++;
      }
      if (TurkishTextCodec.isPinnedTurkishDiacritic(r)
          && !TurkishTextCodec.isPinnedTurkishDiacritic(o)
          && !(r == 'ı' && o == 'i' && !sentenceHasNonDotlessTr)) {
        stripped++;
      }
    }
    return new CharStats(changed, circ, stripped);
  }

  private static boolean hasCircumflex(char c) {
    return TurkishTextCodec.stripCircumflex(c) != c;
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

  private static double roundMs(double ms) {
    return Math.round(ms * 1000.0) / 1000.0;
  }

  private static double round2(double v) {
    return Math.round(v * 100.0) / 100.0;
  }

  private record LatticeReflection(
      Method buildSentenceLattice,
      Method computeAmbiguousUnaryScores,
      Method decodeSentence,
      Method scoreCandidateInSequence,
      Field fCandidates,
      Field fEmpPriors,
      Field fCharScores,
      Field fUnaryScores) {

    private static Field accessibleField(Class<?> cls, String name) throws NoSuchFieldException {
      Field f = cls.getDeclaredField(name);
      f.setAccessible(true);
      return f;
    }

    private static Method accessibleMethod(Class<?> cls, String name, Class<?>... params)
        throws NoSuchMethodException {
      Method m = cls.getDeclaredMethod(name, params);
      m.setAccessible(true);
      return m;
    }

    static LatticeReflection tryInit() {
      try {
        String pkg = "zemberek.normalization.deasciifier.latticedeasciifier.";
        Class<?> tlClass = Class.forName(pkg + "LatticeDeasciifier$TokenLattice");
        Class<?> tlArrayClass = Array.newInstance(tlClass, 0).getClass();
        Class<?> wtClass = Class.forName(pkg + "WeightTable");
        Class<?> scorerClass = Class.forName(pkg + "SentenceLatticeScorer");

        return new LatticeReflection(
            accessibleMethod(
                LatticeDeasciifier.class, "buildSentenceLattice", String.class, List.class),
            accessibleMethod(
                LatticeDeasciifier.class,
                "computeAmbiguousUnaryScores",
                List.class,
                tlArrayClass,
                wtClass,
                long[].class),
            accessibleMethod(
                LatticeDeasciifier.class, "decodeSentence", List.class, tlArrayClass, wtClass),
            accessibleMethod(
                scorerClass,
                "scoreCandidateInSequence",
                int.class,
                Candidate[].class,
                int.class,
                Candidate.class,
                float.class,
                wtClass,
                long[].class),
            accessibleField(tlClass, "candidates"),
            accessibleField(tlClass, "empiricalLogPriors"),
            accessibleField(tlClass, "charRelativeScores"),
            accessibleField(tlClass, "unaryScores"));
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
        Candidate[] chosen =
            (Candidate[]) decodeSentence.invoke(deasciifier, mutTokens, cols, weights);

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
                        scoreCandidateInSequence.invoke(null, i, chosen, n, c, u, weights, featBuf)
                    : u;
            dtos.add(
                new LatticeCandidateDto(
                    TurkishTextCodec.applyCasingAndPins(tok, c),
                    c.lemma,
                    morphSummary,
                    round2(k < priors.length ? priors[k] : 0.0),
                    round2(k < chars.length ? chars[k] : 0.0),
                    round2(u),
                    round2(seq),
                    c == chosen[i]));
          }
          dtos.sort(Comparator.comparingDouble(LatticeCandidateDto::seqScore).reversed());
          out.add(dtos);
        }
        return out;
      } catch (Exception e) {
        return List.of();
      }
    }
  }
}
