package zemberek.apps.demo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.normalization.v2.NormV2Types;
import zemberek.normalization.v2.NormV2Types.SentenceLattice;
import zemberek.normalization.v2.NormV2Types.SpanArc;
import zemberek.normalization.v2.NormV2Types.SpanToken;
import zemberek.normalization.v2.NormalizationKnowledge;
import zemberek.normalization.v2.NormalizationKnowledge.RewriteEntry;
import zemberek.normalization.v2.SpanCandidateGenerator;
import zemberek.normalization.v2.SpanLatticePerceptronNormalizer;

/**
 * Live normalization and multi-span lattice inspector for {@link SpanLatticePerceptronNormalizer}
 * ({@code v2.4}) alongside {@code Zemberek HEAD} ({@link TurkishSentenceNormalizer}).
 */
public final class NormalizationInspector {

  private final SpanLatticePerceptronNormalizer perceptron;
  private final TurkishSentenceNormalizer zemberekHead;

  public NormalizationInspector(
      SpanLatticePerceptronNormalizer perceptron, TurkishSentenceNormalizer zemberekHead) {
    this.perceptron = perceptron;
    this.zemberekHead = zemberekHead;
  }

  public record CandidateView(
      int startIdx,
      int endIdx,
      String normSurface,
      String spanKind,
      int wordCount,
      float logit,
      float memmProb,
      float cleanSpanLlr,
      float smoothSpanLlr,
      float cliticSkipLlr,
      float parallelProb,
      float editCost,
      boolean regularMorph,
      List<String> sources,
      boolean winner) {}

  public record GateSignals(
      int wordLen,
      boolean hasTurkishDiacritics,
      boolean hasAsciiAmbiguous,
      boolean hasRepeatedChars,
      boolean cliticTailHint,
      boolean nextMergeHint,
      boolean strictMorph,
      float charPhonotactics,
      float parallelKeepProb,
      float cleanUnigramLogProb) {}

  public record SlotInspection(
      int tokenIdx,
      String rawSurface,
      String tokenType,
      boolean immutable,
      float gateLogit,
      float gateProb,
      float gateThreshold,
      boolean gateSkipped,
      String gateNote,
      GateSignals gateSignals,
      List<CandidateView> candidates) {}

  public record DecodedSpanView(
      int startIdx,
      int endIdx,
      String rawSurface,
      String normSurface,
      boolean changed,
      String changeKind,
      boolean attachToPrev,
      boolean spaceAfter,
      float gateProb,
      boolean gateSkipped,
      float winningLogit,
      float winningProb,
      List<String> sources) {}

  public record LineTrace(
      int lineIndex,
      String rawLine,
      String normalizedLine,
      int tokenCount,
      int editedSpanCount,
      List<DecodedSpanView> decodedSpans,
      List<SlotInspection> slots) {}

  public record EngineComparison(
      String engineId, String label, String sizeLabel, String normalizedText, double latencyMs) {}

  public record NormalizeResponse(
      String engineId,
      String engineLabel,
      String normalizedText,
      double latencyMs,
      double totalMs,
      int totalTokens,
      int totalEditedSpans,
      List<LineTrace> lines,
      List<EngineComparison> comparisons) {}

  public NormalizeResponse normalizeAndInspect(
      String rawText, String engineId, boolean includeInspection, boolean compareAll) {
    long tTotal0 = System.nanoTime();
    String text = rawText == null ? "" : rawText;
    String eng =
        "zemberek_head".equalsIgnoreCase(engineId) ? "zemberek_head" : "perceptron";

    String[] rawLines = text.split("\n", -1);
    StringBuilder fullOut = new StringBuilder(text.length() + 16);

    long tInferNs = 0L;
    String[] pureLinePreds = new String[rawLines.length];
    for (int i = 0; i < rawLines.length; i++) {
      String rl = rawLines[i];
      if (rl.endsWith("\r")) {
        rl = rl.substring(0, rl.length() - 1);
      }
      if (rl.isBlank()) {
        pureLinePreds[i] = rl;
      } else {
        long t0 = System.nanoTime();
        pureLinePreds[i] = runSingleEngine(eng, rl);
        tInferNs += (System.nanoTime() - t0);
      }
      if (i > 0) {
        fullOut.append('\n');
      }
      fullOut.append(pureLinePreds[i]);
    }

    List<LineTrace> lineTraces = new ArrayList<>(rawLines.length);
    int totalTokens = 0;
    int totalEdited = 0;
    for (int i = 0; i < rawLines.length; i++) {
      String rl = rawLines[i];
      if (rl.endsWith("\r")) {
        rl = rl.substring(0, rl.length() - 1);
      }
      LineTrace lt = buildLineTrace(i, rl, pureLinePreds[i], eng, includeInspection);
      totalTokens += lt.tokenCount();
      totalEdited += lt.editedSpanCount();
      lineTraces.add(lt);
    }

    List<EngineComparison> comparisons = null;
    if (compareAll && !text.isBlank()) {
      comparisons = new ArrayList<>(2);
      String percSize =
          String.format(
              java.util.Locale.US, "%.2f MB", perceptron.modelRamBytes() / (1024.0 * 1024.0));
      comparisons.add(
          runComparison("perceptron", "Span-Lattice Perceptron (v2.4)", percSize, rawLines));
      if (zemberekHead != null) {
        comparisons.add(
            runComparison("zemberek_head", "Zemberek HEAD", "Rule + 2-gram", rawLines));
      }
    }

    double inferMs = tInferNs / 1e6;
    double totalMs = (System.nanoTime() - tTotal0) / 1e6;
    return new NormalizeResponse(
        eng,
        engineLabel(eng),
        fullOut.toString(),
        inferMs,
        totalMs,
        totalTokens,
        totalEdited,
        lineTraces,
        comparisons);
  }

  private EngineComparison runComparison(
      String engId, String label, String sizeLabel, String[] rawLines) {
    StringBuilder sb = new StringBuilder();
    long t0 = System.nanoTime();
    for (int i = 0; i < rawLines.length; i++) {
      String rl = rawLines[i];
      if (rl.endsWith("\r")) {
        rl = rl.substring(0, rl.length() - 1);
      }
      if (i > 0) {
        sb.append('\n');
      }
      sb.append(rl.isBlank() ? rl : runSingleEngine(engId, rl));
    }
    double ms = (System.nanoTime() - t0) / 1e6;
    return new EngineComparison(engId, label, sizeLabel, sb.toString(), ms);
  }

  private String runSingleEngine(String engineId, String line) {
    if ("zemberek_head".equals(engineId) && zemberekHead != null) {
      return zemberekHead.normalize(line);
    }
    return perceptron.normalize(line);
  }

  private static String engineLabel(String engineId) {
    return "zemberek_head".equals(engineId)
        ? "Zemberek HEAD (Rule + SmoothLm)"
        : "Span-Lattice Perceptron (v2.4 · 2.62 MB)";
  }

  private LineTrace buildLineTrace(
      int lineIndex,
      String rawLine,
      String normalizedLine,
      String engineId,
      boolean includeInspection) {
    if (rawLine.isBlank()) {
      return new LineTrace(lineIndex, rawLine, normalizedLine, 0, 0, List.of(), List.of());
    }

    if ("zemberek_head".equals(engineId)) {
      List<SpanToken> rawToks = NormV2Types.spanTokenize(rawLine);
      List<DecodedSpanView> single =
          List.of(
              new DecodedSpanView(
                  0,
                  rawToks.size(),
                  rawLine,
                  normalizedLine,
                  !rawLine.trim().equals(normalizedLine.trim()),
                  rawLine.trim().equals(normalizedLine.trim()) ? "keep" : "rewrite",
                  false,
                  false,
                  0.0f,
                  false,
                  0.0f,
                  1.0f,
                  List.of("ZemberekHEAD")));
      return new LineTrace(
          lineIndex,
          rawLine,
          normalizedLine,
          rawToks.size(),
          rawLine.trim().equals(normalizedLine.trim()) ? 0 : 1,
          single,
          List.of());
    }

    SpanCandidateGenerator gen = perceptron.generator();
    NormalizationKnowledge knowledge = gen.mined();
    SentenceLattice lattice = gen.buildLattice(rawLine);

    List<SpanToken> toks = lattice.tokens();
    int n = toks.size();
    if (n == 0) {
      return new LineTrace(lineIndex, rawLine, normalizedLine, 0, 0, List.of(), List.of());
    }

    List<SpanArc> decodedPath = perceptron.decodeLattice(lattice);

    SpanArc[] winnerByStart = new SpanArc[n];
    String[] prev2At = new String[n + 1];
    String[] prev1At = new String[n + 1];
    short[] prevMorphAt = new short[n + 1];
    java.util.Arrays.fill(prevMorphAt, (short) -1);
    String[] nextLex2 = new String[2];
    for (SpanArc wa : decodedPath) {
      if (wa.startIdx >= 0 && wa.startIdx < n) {
        winnerByStart[wa.startIdx] = wa;
      }
      short nextMorph =
          SpanLatticePerceptronNormalizer.advanceLexMorphTag(
              lattice, prevMorphAt[wa.startIdx], wa);
      SpanCandidateGenerator.advanceLexicalHistory(
          lattice, prev2At[wa.startIdx], prev1At[wa.startIdx], wa, nextLex2);
      if (wa.endIdx <= n) {
        prev2At[wa.endIdx] = nextLex2[0];
        prev1At[wa.endIdx] = nextLex2[1];
        prevMorphAt[wa.endIdx] = nextMorph;
      }
    }

    float[][] arcLm6 = new float[32][6];
    float[] slotEditProb = new float[n];
    float[] winLogitByStart = new float[n];
    float[] winProbByStart = new float[n];
    List<SlotInspection> slotList = includeInspection ? new ArrayList<>(n) : List.of();

    String rollPrev2 = null;
    String rollPrev1 = null;
    short rollMorph = -1;

    for (int i = 0; i < n; i++) {
      SpanToken t = toks.get(i);
      boolean onWinnerBoundary = winnerByStart[i] != null || i == 0;
      String p2 = onWinnerBoundary ? prev2At[i] : rollPrev2;
      String p1 = onWinnerBoundary ? prev1At[i] : rollPrev1;
      short pMorph = onWinnerBoundary ? prevMorphAt[i] : rollMorph;

      List<SpanArc> arcs = lattice.outgoing()[i];
      SpanArc winArc = winnerByStart[i];
      winProbByStart[i] = 1.0f;

      if (!arcs.isEmpty()) {
        if (arcLm6.length < arcs.size()) {
          arcLm6 = new float[arcs.size() + 8][6];
        }
        float[] scores = new float[arcs.size()];
        float maxScore = Float.NEGATIVE_INFINITY;
        for (int a = 0; a < arcs.size(); a++) {
          SpanArc cand = arcs.get(a);
          gen.computeSpanLmComparison(lattice, p2, p1, cand, arcLm6[a]);
          float sc = arcs.size() == 1 ? 0.0f : perceptron.scoreArc(lattice, p2, p1, pMorph, cand);
          scores[a] = sc;
          if (sc > maxScore) {
            maxScore = sc;
          }
        }
        double denomSum = 0.0;
        for (float sc : scores) {
          denomSum += Math.exp(sc - maxScore);
        }
        float logDenom = maxScore + (float) Math.log(Math.max(1e-12, denomSum));

        float identityKeepMass = 0.0f;
        List<CandidateView> cViews = includeInspection ? new ArrayList<>(arcs.size()) : null;
        for (int a = 0; a < arcs.size(); a++) {
          SpanArc cand = arcs.get(a);
          float prob = (float) Math.exp(scores[a] - logDenom);
          if (cand.endIdx == cand.startIdx + 1 && cand.normLower.equals(t.lower())) {
            identityKeepMass += prob;
          }
          boolean isWin =
              winArc != null
                  && cand.endIdx == winArc.endIdx
                  && cand.normSurface.equals(winArc.normSurface);
          if (isWin) {
            winLogitByStart[i] = round3(scores[a]);
            winProbByStart[i] = round4(prob);
          }
          if (includeInspection) {
            float sLlr = arcLm6[a][0] - arcLm6[a][1];
            float cLlr = arcLm6[a][2] - arcLm6[a][3];
            float cliticSkip = gen.computeCliticTransparencyLlr(lattice, cand);
            cViews.add(
                new CandidateView(
                    cand.startIdx,
                    cand.endIdx,
                    cand.normSurface,
                    describeSpanKind(cand),
                    cand.wordCount,
                    round3(scores[a]),
                    round4(prob),
                    round3(cLlr),
                    round3(sLlr),
                    round3(cliticSkip),
                    round3(cand.parallelProb),
                    round3(cand.editCost),
                    cand.isRegularMorph,
                    decodeSources(cand.sourceMask),
                    isWin));
          }
        }

        float editProb = t.isImmutable() ? 0.0f : Math.max(0.0f, Math.min(1.0f, 1.0f - identityKeepMass));
        float editLogit =
            editProb <= 1e-4f
                ? -9.2f
                : (editProb >= 1.0f - 1e-4f
                    ? 9.2f
                    : (float) Math.log(editProb / (1.0f - editProb)));
        slotEditProb[i] = round4(editProb);

        if (includeInspection) {
          cViews.sort(
              Comparator.comparing(CandidateView::winner)
                  .reversed()
                  .thenComparing(Comparator.comparingDouble(CandidateView::memmProb).reversed()));

          boolean immutable = t.isImmutable();
          GateSignals gs = null;
          String slotNote;
          if (immutable) {
            slotNote = "Immutable token (" + t.type().name() + ")";
          } else {
            String low = t.lower();
            boolean strictMorph = gen.hasRegularStrictAnalysis(low);
            boolean hasTrDiac = SpanCandidateGenerator.hasExplicitTurkishDiacritics(low);
            boolean hasAscAmb = SpanCandidateGenerator.hasAsciiAmbiguousChars(low);
            boolean hasRep = SpanCandidateGenerator.hasRepeatedLetters(low);
            boolean cliticHint = hasCliticTailCue(low);
            boolean mergeHint = hasNextMergeCue(toks, i, knowledge);
            RewriteEntry re = knowledge.parallel1ToN.get(low);
            float parKeep = re != null ? re.keepProb() : (strictMorph ? 0.90f : 0.50f);
            float phono = knowledge.charPhonotactics.scoreWord(low);
            float cleanUni = knowledge.cleanLm.unigramLogProb(low);
            gs =
                new GateSignals(
                    low.length(),
                    hasTrDiac,
                    hasAscAmb,
                    hasRep,
                    cliticHint,
                    mergeHint,
                    strictMorph,
                    round3(phono),
                    round3(parKeep),
                    round3(cleanUni));
            slotNote =
                arcs.size() + " candidate arc(s) scored in Semi-Markov Viterbi beam (B=4)";
          }

          slotList.add(
              new SlotInspection(
                  i,
                  t.surface(),
                  t.type().name(),
                  immutable,
                  round3(editLogit),
                  round4(editProb),
                  0.50f,
                  immutable,
                  slotNote,
                  gs,
                  cViews));
        }

        SpanArc stepArc = winArc != null ? winArc : arcs.get(0);
        rollMorph =
            SpanLatticePerceptronNormalizer.advanceLexMorphTag(lattice, pMorph, stepArc);
        SpanCandidateGenerator.advanceLexicalHistory(lattice, p2, p1, stepArc, nextLex2);
        rollPrev2 = nextLex2[0];
        rollPrev1 = nextLex2[1];
      }
    }

    List<DecodedSpanView> spanViews = new ArrayList<>(decodedPath.size());
    int editedCount = 0;
    boolean[] spaceBefore = computeSpaceBeforeFlags(toks, decodedPath);

    for (int pIdx = 0; pIdx < decodedPath.size(); pIdx++) {
      SpanArc arc = decodedPath.get(pIdx);
      String rawSpan = rawSpanText(rawLine, toks, arc.startIdx, arc.endIdx);
      boolean changed = !rawSpan.equals(arc.normSurface);
      if (changed) {
        editedCount++;
      }
      String changeKind = classifyChangeKind(toks, arc, rawSpan, changed);
      boolean attachPrev = pIdx > 0 && !spaceBefore[pIdx];
      boolean spAfter = (pIdx + 1 < decodedPath.size()) && spaceBefore[pIdx + 1];
      spanViews.add(
          new DecodedSpanView(
              arc.startIdx,
              arc.endIdx,
              rawSpan,
              arc.normSurface,
              changed,
              changeKind,
              attachPrev,
              spAfter,
              slotEditProb[arc.startIdx],
              toks.get(arc.startIdx).isImmutable(),
              winLogitByStart[arc.startIdx],
              winProbByStart[arc.startIdx],
              decodeSources(arc.sourceMask)));
    }

    String recon = NormV2Types.reconstructText(toks, decodedPath);
    return new LineTrace(lineIndex, rawLine, recon, n, editedCount, spanViews, slotList);
  }

  private static boolean hasCliticTailCue(String low) {
    if (SpanCandidateGenerator.isStandaloneQuestionClitic(low)) {
      return true;
    }
    return low.length() >= 4
        && (low.endsWith("de")
            || low.endsWith("da")
            || low.endsWith("te")
            || low.endsWith("ta")
            || low.endsWith("ki")
            || low.endsWith("mi")
            || low.endsWith("mı")
            || low.endsWith("mu")
            || low.endsWith("mü")
            || low.contains("şey")
            || low.contains("sey"));
  }

  private static boolean hasNextMergeCue(
      List<SpanToken> toks, int i, NormalizationKnowledge knowledge) {
    if (i + 1 >= toks.size()) {
      return false;
    }
    String nLow = toks.get(i + 1).lower();
    if (nLow.equals("de")
        || nLow.equals("da")
        || nLow.equals("te")
        || nLow.equals("ta")
        || nLow.equals("ki")
        || nLow.equals("bir")) {
      return true;
    }
    return knowledge.parallel2To1.containsKey(toks.get(i).lower() + "\t" + nLow);
  }

  private static boolean[] computeSpaceBeforeFlags(List<SpanToken> toks, List<SpanArc> path) {
    boolean[] spaceBefore = new boolean[path.size()];
    StringBuilder acc = new StringBuilder();
    for (int k = 0; k < path.size(); k++) {
      String partial = NormV2Types.reconstructText(toks, path.subList(0, k + 1));
      spaceBefore[k] =
          k > 0 && partial.length() > acc.length() && partial.charAt(acc.length()) == ' ';
      acc.setLength(0);
      acc.append(partial);
    }
    return spaceBefore;
  }

  private static String rawSpanText(
      String rawLine, List<SpanToken> toks, int startIdx, int endIdx) {
    SpanToken first = toks.get(startIdx);
    SpanToken last = toks.get(Math.min(toks.size() - 1, endIdx - 1));
    if (first.start() >= 0
        && last.end() >= first.start()
        && last.end() + 1 <= rawLine.length()) {
      return rawLine.substring(first.start(), last.end() + 1);
    }
    StringBuilder sb = new StringBuilder();
    for (int i = startIdx; i < endIdx && i < toks.size(); i++) {
      if (i > startIdx && !toks.get(i).adjacentToPrev()) {
        sb.append(' ');
      }
      sb.append(toks.get(i).surface());
    }
    return sb.toString();
  }

  private static String describeSpanKind(SpanArc arc) {
    int spanLen = arc.endIdx - arc.startIdx;
    if (spanLen >= 2) {
      return spanLen + ":1 Merge";
    }
    if (arc.wordCount >= 2) {
      return "1:" + arc.wordCount + " Split";
    }
    return "1:1";
  }

  private static String classifyChangeKind(
      List<SpanToken> toks, SpanArc arc, String rawSpan, boolean changed) {
    if (!changed) {
      return "keep";
    }
    if (arc.endIdx - arc.startIdx >= 2) {
      return "merge";
    }
    if (arc.wordCount >= 2) {
      return "split";
    }
    if (arc.apostropheAdded || arc.apostropheRemoved) {
      return "apostrophe";
    }
    if (arc.endIdx == arc.startIdx + 1 && toks.get(arc.startIdx).lower().equals(arc.normLower)) {
      return "casing";
    }
    return "rewrite";
  }

  public static List<String> decodeSources(int mask) {
    List<String> out = new ArrayList<>(4);
    if ((mask & NormV2Types.SRC_IDENTITY) != 0) out.add("Identity");
    if ((mask & NormV2Types.SRC_PARALLEL_1TO1) != 0) out.add("Parallel 1:1");
    if ((mask & NormV2Types.SRC_PARALLEL_SPLIT) != 0) out.add("Parallel Split");
    if ((mask & NormV2Types.SRC_PARALLEL_MERGE) != 0) out.add("Parallel Merge");
    if ((mask & NormV2Types.SRC_FORMAL_MORPH) != 0) out.add("Formal Morph");
    if ((mask & NormV2Types.SRC_INFORMAL_EXACT) != 0) out.add("Informal Exact");
    if ((mask & NormV2Types.SRC_INFORMAL_MORPH) != 0) out.add("Informal Morph");
    if ((mask & NormV2Types.SRC_DEASCIIFIER) != 0) out.add("Deasciifier");
    if ((mask & NormV2Types.SRC_SUFFIX_RULE) != 0) out.add("Suffix Rule");
    if ((mask & NormV2Types.SRC_OOV_APOSTROPHE) != 0) out.add("TDK Apostrophe");
    if ((mask & NormV2Types.SRC_APOSTROPHE_DROP) != 0) out.add("Apostrophe Drop");
    if ((mask & NormV2Types.SRC_ELONGATION) != 0) out.add("Elongation");
    if ((mask & NormV2Types.SRC_SPELLCHECK) != 0) out.add("CSR Spellcheck");
    if ((mask & NormV2Types.SRC_CLITIC_SPLIT) != 0) out.add("Clitic/Compound Split");
    if ((mask & NormV2Types.SRC_COMPOUND_MERGE) != 0) out.add("Compound/Case Merge");
    if ((mask & NormV2Types.SRC_NUMERAL_JOIN) != 0) out.add("Numeral Join");
    if ((mask & NormV2Types.SRC_SHORTHAND_ABJAD) != 0) out.add("Shorthand/Abjad");
    return out;
  }

  private static float round3(float v) {
    if (Float.isNaN(v) || Float.isInfinite(v)) {
      return 0.0f;
    }
    return Math.round(v * 1000.0f) / 1000.0f;
  }

  private static float round4(float v) {
    if (Float.isNaN(v) || Float.isInfinite(v)) {
      return 0.0f;
    }
    return Math.round(v * 10000.0f) / 10000.0f;
  }
}
