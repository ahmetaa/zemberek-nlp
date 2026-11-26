package zemberek.apps.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import zemberek.normalization.v2.NormV2Types;
import zemberek.normalization.v2.NormV2Types.SentenceLattice;
import zemberek.normalization.v2.NormV2Types.SpanArc;
import zemberek.normalization.v2.NormV2Types.SpanToken;
import zemberek.normalization.v2.NormalizationKnowledge;
import zemberek.normalization.v2.NormalizationKnowledge.RewriteEntry;
import zemberek.normalization.v2.SpanCandidateGenerator;
import zemberek.normalization.v2.SpanLatticePerceptronNormalizer;

/**
 * Live normalization and multi-span lattice inspector for {@link SpanLatticePerceptronNormalizer}.
 */
public final class NormalizationInspector {

  private final SpanLatticePerceptronNormalizer perceptron;

  public NormalizationInspector(SpanLatticePerceptronNormalizer perceptron) {
    this.perceptron = perceptron;
  }

  public boolean hasZemberekHead() {
    return false;
  }

  public record CandidateView(
      int startIdx,
      int endIdx,
      String normSurface,
      String spanKind,
      float arcScore,
      float softmaxProb,
      float cleanSpanLlr,
      float cliticSkipLlr,
      float editCost,
      List<String> sources,
      boolean winner) {}

  public record TokenSignals(
      int wordLen,
      boolean hasTurkishDiacritics,
      boolean hasAsciiAmbiguous,
      boolean cliticTailHint,
      boolean nextMergeHint,
      boolean strictMorph,
      float charPhonotactics,
      float parallelKeepProb) {}

  public record SlotInspection(
      int tokenIdx,
      String rawSurface,
      float editLogOdds,
      float editProb,
      String note,
      TokenSignals signals,
      List<CandidateView> candidates) {}

  public record DecodedSpanView(
      int startIdx,
      int endIdx,
      String rawSurface,
      String normSurface,
      boolean changed,
      String changeKind,
      boolean spaceAfter,
      boolean immutable,
      float winningProb,
      List<String> sources) {}

  public record LineTrace(
      int lineIndex,
      String normalizedLine,
      int tokenCount,
      int editedSpanCount,
      List<DecodedSpanView> decodedSpans,
      List<SlotInspection> slots) {}

  public record EngineComparison(String label, String normalizedText, double latencyMs) {}

  public record NormalizeResponse(
      String normalizedText,
      double latencyMs,
      int totalTokens,
      int totalEditedSpans,
      List<LineTrace> lines,
      List<EngineComparison> comparisons) {}

  public NormalizeResponse normalizeAndInspect(
      String rawText, String engineId, boolean includeInspection, boolean compareAll) {
    String text = rawText == null ? "" : rawText.replace("\r\n", "\n").replace('\r', '\n');

    String[] rawLines = text.split("\n", -1);
    StringBuilder fullOut = new StringBuilder(text.length() + 16);

    long tInferNs = 0L;
    String[] pureLinePreds = new String[rawLines.length];
    for (int i = 0; i < rawLines.length; i++) {
      String rl = rawLines[i];
      if (rl.isBlank()) {
        pureLinePreds[i] = rl;
      } else {
        long t0 = System.nanoTime();
        pureLinePreds[i] = perceptron.normalize(rl);
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
      LineTrace lt = buildLineTrace(i, rawLines[i], pureLinePreds[i], includeInspection);
      totalTokens += lt.tokenCount();
      totalEdited += lt.editedSpanCount();
      lineTraces.add(lt);
    }

    double inferMs = tInferNs / 1e6;
    String normalizedFull = fullOut.toString();

    List<EngineComparison> comparisons =
        compareAll && !text.isBlank()
            ? List.of(
                new EngineComparison("Span-Lattice Perceptron (v2.4)", normalizedFull, inferMs))
            : null;

    return new NormalizeResponse(
        normalizedFull, inferMs, totalTokens, totalEdited, lineTraces, comparisons);
  }

  private LineTrace buildLineTrace(
      int lineIndex,
      String rawLine,
      String normalizedLine,
      boolean includeInspection) {
    if (rawLine.isBlank()) {
      return new LineTrace(lineIndex, normalizedLine, 0, 0, List.of(), List.of());
    }

    SpanCandidateGenerator gen = perceptron.generator();
    NormalizationKnowledge knowledge = gen.mined();
    SentenceLattice lattice = gen.buildLattice(rawLine);

    List<SpanToken> toks = lattice.tokens();
    int n = toks.size();
    if (n == 0) {
      return new LineTrace(lineIndex, normalizedLine, 0, 0, List.of(), List.of());
    }

    List<SpanArc> decodedPath = perceptron.decodeLattice(lattice);

    SpanArc[] winnerByStart = new SpanArc[n];
    String[] prev2At = new String[n + 1];
    String[] prev1At = new String[n + 1];
    short[] prevMorphAt = new short[n + 1];
    Arrays.fill(prevMorphAt, (short) -1);
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

    float[] lmBuf = new float[6];
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
        float[] scores = new float[arcs.size()];
        float maxScore = Float.NEGATIVE_INFINITY;
        for (int a = 0; a < arcs.size(); a++) {
          float sc =
              arcs.size() == 1 ? 0.0f : perceptron.scoreArc(lattice, p2, p1, pMorph, arcs.get(a));
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
            winProbByStart[i] = round4(prob);
          }
          if (includeInspection) {
            gen.computeSpanLmComparison(lattice, p2, p1, cand, lmBuf);
            float cLlr = lmBuf[2] - lmBuf[3];
            float cliticSkip = gen.computeCliticTransparencyLlr(lattice, cand);
            cViews.add(
                new CandidateView(
                    cand.startIdx,
                    cand.endIdx,
                    cand.normSurface,
                    describeSpanKind(cand),
                    round3(scores[a]),
                    round4(prob),
                    round3(cLlr),
                    round3(cliticSkip),
                    round3(cand.editCost),
                    decodeSources(cand.sourceMask),
                    isWin));
          }
        }

        if (includeInspection) {
          float editProb =
              t.isImmutable() ? 0.0f : Math.max(0.0f, Math.min(1.0f, 1.0f - identityKeepMass));
          float editLogOdds =
              editProb <= 1e-4f
                  ? -9.2f
                  : editProb >= 1.0f - 1e-4f
                      ? 9.2f
                      : (float) Math.log(editProb / (1.0f - editProb));

          cViews.sort(
              Comparator.comparing(CandidateView::winner)
                  .reversed()
                  .thenComparing(
                      Comparator.comparingDouble(CandidateView::softmaxProb).reversed()));

          TokenSignals signals = null;
          String slotNote;
          if (t.isImmutable()) {
            slotNote = "Immutable token (" + t.type().name() + ")";
          } else {
            String low = t.lower();
            boolean strictMorph = gen.hasRegularStrictAnalysis(low);
            RewriteEntry re = knowledge.parallel1ToN.get(low);
            float parKeep = re != null ? re.keepProb() : (strictMorph ? 0.90f : 0.50f);
            signals =
                new TokenSignals(
                    low.length(),
                    SpanCandidateGenerator.hasExplicitTurkishDiacritics(low),
                    SpanCandidateGenerator.hasAsciiAmbiguousChars(low),
                    hasCliticTailCue(low),
                    hasNextMergeCue(toks, i, knowledge),
                    strictMorph,
                    round3(knowledge.charPhonotactics.scoreWord(low)),
                    round3(parKeep));
            slotNote = arcs.size() + " candidate arc(s) scored in Semi-Markov Viterbi beam (B=4)";
          }

          slotList.add(
              new SlotInspection(
                  i, t.surface(), round3(editLogOdds), round4(editProb), slotNote, signals, cViews));
        }

        SpanArc stepArc = winArc != null ? winArc : arcs.get(0);
        rollMorph = SpanLatticePerceptronNormalizer.advanceLexMorphTag(lattice, pMorph, stepArc);
        SpanCandidateGenerator.advanceLexicalHistory(lattice, p2, p1, stepArc, nextLex2);
        rollPrev2 = nextLex2[0];
        rollPrev1 = nextLex2[1];
      }
    }

    String recon = NormV2Types.reconstructText(toks, decodedPath);
    List<DecodedSpanView> spanViews = new ArrayList<>(decodedPath.size());
    int editedCount = 0;
    int pos = 0;

    for (SpanArc arc : decodedPath) {
      String rawSpan = rawSpanText(rawLine, toks, arc.startIdx, arc.endIdx);
      boolean changed = !rawSpan.equals(arc.normSurface);
      if (changed) {
        editedCount++;
      }
      String val = arc.normSurface;
      boolean spAfter = false;
      if (val != null && !val.isEmpty()) {
        if (pos < recon.length() && recon.charAt(pos) == ' ') {
          pos++;
        }
        pos += val.length();
        spAfter = pos < recon.length() && recon.charAt(pos) == ' ';
      }
      spanViews.add(
          new DecodedSpanView(
              arc.startIdx,
              arc.endIdx,
              rawSpan,
              arc.normSurface,
              changed,
              classifyChangeKind(toks, arc, changed),
              spAfter,
              toks.get(arc.startIdx).isImmutable(),
              winProbByStart[arc.startIdx],
              decodeSources(arc.sourceMask)));
    }

    return new LineTrace(lineIndex, recon, n, editedCount, spanViews, slotList);
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
    return nLow.equals("de")
        || nLow.equals("da")
        || nLow.equals("te")
        || nLow.equals("ta")
        || nLow.equals("ki")
        || nLow.equals("bir")
        || knowledge.parallel2To1.containsKey(toks.get(i).lower() + "\t" + nLow);
  }

  private static String rawSpanText(
      String rawLine, List<SpanToken> toks, int startIdx, int endIdx) {
    SpanToken first = toks.get(startIdx);
    SpanToken last = toks.get(Math.min(toks.size() - 1, endIdx - 1));
    if (first.start() >= 0 && last.end() >= first.start() && last.end() + 1 <= rawLine.length()) {
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

  private static String classifyChangeKind(List<SpanToken> toks, SpanArc arc, boolean changed) {
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
    if ((mask & NormV2Types.SRC_SHORTHAND) != 0) out.add("Shorthand");
    return out;
  }

  private static float round3(float v) {
    return Float.isNaN(v) || Float.isInfinite(v) ? 0.0f : Math.round(v * 1000.0f) / 1000.0f;
  }

  private static float round4(float v) {
    return Float.isNaN(v) || Float.isInfinite(v) ? 0.0f : Math.round(v * 10000.0f) / 10000.0f;
  }
}
