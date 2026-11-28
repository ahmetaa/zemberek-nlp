package zemberek.morphology.analysis.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * Morpheme segmentation and interlinear glossing formatters designed for subword/LLM tokenization
 * experiments (MorphoChallenge, BPE vs. morphological pre-tokenization) and linguistic glossing
 * (Leipzig Glossing Rules).
 */
public final class SegmentedSurfaceFormatter implements AnalysisFormatter {

  public enum Mode {
    /** Canonical stem + space-separated surface suffix allomorphs ({@code kitap ımız dan}). */
    CANONICAL_STEM_SPACE,
    /** Canonical stem + {@code " + "}-separated surface suffix allomorphs ({@code kitap + ımız + dan}). */
    CANONICAL_STEM_PLUS,
    /** Two-tier Leipzig Glossing Rules TSV ({@code kitab-ımız-dan\tkitap-POSS.1PL-ABL}). */
    LEIPZIG_INTERLINEAR
  }

  public static final SegmentedSurfaceFormatter CANONICAL_SPACE =
      new SegmentedSurfaceFormatter(Mode.CANONICAL_STEM_SPACE);
  public static final SegmentedSurfaceFormatter CANONICAL_PLUS =
      new SegmentedSurfaceFormatter(Mode.CANONICAL_STEM_PLUS);
  public static final SegmentedSurfaceFormatter LEIPZIG =
      new SegmentedSurfaceFormatter(Mode.LEIPZIG_INTERLINEAR);

  private static final Map<Morpheme, String> LEIPZIG_GLOSS = Map.ofEntries(
      Map.entry(TurkishMorphotactics.a1sg, "1SG"),
      Map.entry(TurkishMorphotactics.a2sg, "2SG"),
      Map.entry(TurkishMorphotactics.a3sg, "3SG"),
      Map.entry(TurkishMorphotactics.a1pl, "1PL"),
      Map.entry(TurkishMorphotactics.a2pl, "2PL"),
      Map.entry(TurkishMorphotactics.a3pl, "PL"),
      Map.entry(TurkishMorphotactics.p1sg, "POSS.1SG"),
      Map.entry(TurkishMorphotactics.p2sg, "POSS.2SG"),
      Map.entry(TurkishMorphotactics.p3sg, "POSS.3SG"),
      Map.entry(TurkishMorphotactics.p1pl, "POSS.1PL"),
      Map.entry(TurkishMorphotactics.p2pl, "POSS.2PL"),
      Map.entry(TurkishMorphotactics.p3pl, "POSS.3PL"),
      Map.entry(TurkishMorphotactics.acc, "ACC"),
      Map.entry(TurkishMorphotactics.dat, "DAT"),
      Map.entry(TurkishMorphotactics.loc, "LOC"),
      Map.entry(TurkishMorphotactics.abl, "ABL"),
      Map.entry(TurkishMorphotactics.gen, "GEN"),
      Map.entry(TurkishMorphotactics.ins, "INS"),
      Map.entry(TurkishMorphotactics.equ, "EQU"),
      Map.entry(TurkishMorphotactics.past, "PST"),
      Map.entry(TurkishMorphotactics.narr, "EVID"),
      Map.entry(TurkishMorphotactics.prog1, "PROG"),
      Map.entry(TurkishMorphotactics.prog2, "PROG"),
      Map.entry(TurkishMorphotactics.aor, "AOR"),
      Map.entry(TurkishMorphotactics.fut, "FUT"),
      Map.entry(TurkishMorphotactics.cond, "COND"),
      Map.entry(TurkishMorphotactics.desr, "COND"),
      Map.entry(TurkishMorphotactics.opt, "OPT"),
      Map.entry(TurkishMorphotactics.neces, "NEC"),
      Map.entry(TurkishMorphotactics.imp, "IMP"),
      Map.entry(TurkishMorphotactics.cop, "COP"),
      Map.entry(TurkishMorphotactics.neg, "NEG"),
      Map.entry(TurkishMorphotactics.able, "ABIL"),
      Map.entry(TurkishMorphotactics.unable, "ABIL.NEG"),
      Map.entry(TurkishMorphotactics.pass, "PASS"),
      Map.entry(TurkishMorphotactics.caus, "CAUS"),
      Map.entry(TurkishMorphotactics.reflex, "REFL"),
      Map.entry(TurkishMorphotactics.recip, "RECP"),
      Map.entry(TurkishMorphotactics.inf1, "INF"),
      Map.entry(TurkishMorphotactics.inf2, "VN"),
      Map.entry(TurkishMorphotactics.inf3, "VN"),
      Map.entry(TurkishMorphotactics.presPart, "PTCP.PRS"),
      Map.entry(TurkishMorphotactics.pastPart, "PTCP.PST"),
      Map.entry(TurkishMorphotactics.narrPart, "PTCP.EVID"),
      Map.entry(TurkishMorphotactics.futPart, "PTCP.FUT"),
      Map.entry(TurkishMorphotactics.aorPart, "PTCP.AOR"),
      Map.entry(TurkishMorphotactics.byDoingSo, "CVB.MNR"),
      Map.entry(TurkishMorphotactics.afterDoingSo, "CVB.SEQ"),
      Map.entry(TurkishMorphotactics.when, "CVB.TEMP"),
      Map.entry(TurkishMorphotactics.while_, "CVB.SIM"),
      Map.entry(TurkishMorphotactics.asLongAs, "CVB.DUR"),
      Map.entry(TurkishMorphotactics.sinceDoingSo, "CVB.ABL"),
      Map.entry(TurkishMorphotactics.withoutHavingDoneSo, "CVB.PRIV"),
      Map.entry(TurkishMorphotactics.withoutBeingAbleToHaveDoneSo, "ABIL.NEG.CVB"),
      Map.entry(TurkishMorphotactics.with, "WITH"),
      Map.entry(TurkishMorphotactics.without, "PRIV"),
      Map.entry(TurkishMorphotactics.ness, "NMLZ"),
      Map.entry(TurkishMorphotactics.dim, "DIM"),
      Map.entry(TurkishMorphotactics.agt, "AGT"),
      Map.entry(TurkishMorphotactics.rel, "REL"),
      Map.entry(TurkishMorphotactics.become, "INCH"),
      Map.entry(TurkishMorphotactics.acquire, "VRBZ"),
      Map.entry(TurkishMorphotactics.ly, "ADVZ"));

  private final Mode mode;

  public SegmentedSurfaceFormatter(Mode mode) {
    this.mode = mode;
  }

  @Override
  public String format(SingleAnalysis analysis) {
    if (analysis.isUnknown()) {
      String surf = analysis.surfaceForm();
      return mode == Mode.LEIPZIG_INTERLINEAR ? (surf + "\t?") : surf;
    }

    DictionaryItem item = analysis.getDictionaryItem();
    String canonicalStem = resolveCanonicalStem(analysis, item);
    List<MorphemeData> mds = analysis.getMorphemeDataList();

    return switch (mode) {
      case LEIPZIG_INTERLINEAR -> formatLeipzig(analysis, canonicalStem, mds);
      case CANONICAL_STEM_SPACE -> formatCanonicalSegments(canonicalStem, mds, " ");
      case CANONICAL_STEM_PLUS -> formatCanonicalSegments(canonicalStem, mds, " + ");
    };
  }

  private static String formatCanonicalSegments(
      String canonicalStem, List<MorphemeData> mds, String separator) {
    List<String> pieces = new ArrayList<>(mds.size());
    pieces.add(canonicalStem);
    for (int i = 1; i < mds.size(); i++) {
      String s = mds.get(i).surface;
      if (!s.isEmpty()) {
        pieces.add(s);
      }
    }
    return String.join(separator, pieces);
  }

  private static String formatLeipzig(
      SingleAnalysis analysis, String canonicalStem, List<MorphemeData> mds) {
    List<String> surfSegments = new ArrayList<>(mds.size());
    List<String> glossSegments = new ArrayList<>(mds.size());
    surfSegments.add(analysis.getStem());
    glossSegments.add(canonicalStem);

    for (int i = 1; i < mds.size(); i++) {
      MorphemeData md = mds.get(i);
      Morpheme m = md.morpheme;
      if (!md.surface.isEmpty()) {
        surfSegments.add(md.surface);
        glossSegments.add(LEIPZIG_GLOSS.getOrDefault(m, m.id.toUpperCase()));
      } else if (m != TurkishMorphotactics.a3sg
          && m != TurkishMorphotactics.pres
          && m != TurkishMorphotactics.zero
          && m.pos == null) {
        String g = LEIPZIG_GLOSS.get(m);
        if (g != null) {
          int last = glossSegments.size() - 1;
          glossSegments.set(last, glossSegments.getLast() + "." + g);
        }
      }
    }
    return String.join("-", surfSegments) + "\t" + String.join("-", glossSegments);
  }

  private static String resolveCanonicalStem(SingleAnalysis analysis, DictionaryItem item) {
    if (item.primaryPos == PrimaryPos.Punctuation) {
      return analysis.surfaceForm();
    }
    if (item.primaryPos == PrimaryPos.Verb) {
      return item.root;
    }
    return switch (item.secondaryPos) {
      case ProperNoun, Abbreviation, RegularAbbreviation -> item.lemma;
      default -> (item.primaryPos == PrimaryPos.Numeral
              && analysis.getStem().chars().anyMatch(Character::isDigit))
          ? analysis.getStem()
          : item.normalizedLemma();
    };
  }
}
