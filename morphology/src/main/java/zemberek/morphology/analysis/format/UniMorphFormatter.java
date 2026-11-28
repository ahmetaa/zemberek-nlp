package zemberek.morphology.analysis.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeGroup;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * Formats {@link SingleAnalysis} instances into the Universal Morphology ({@code UniMorph} /
 * SIGMORPHON shared task) 3-column tab-separated schema ({@code lemma\tsurface\tBUNDLE}), matching
 * {@code unimorph/tur} conventions for Turkish nouns, verbs, and copular predicates while covering
 * voice and non-finite verb forms via official UniMorph 3.0 / 4.0 tags.
 */
public final class UniMorphFormatter implements AnalysisFormatter {

  public static final UniMorphFormatter TAB_ROW = new UniMorphFormatter(true);
  public static final UniMorphFormatter BUNDLE_ONLY = new UniMorphFormatter(false);

  private static final Set<String> LEXICAL_DERIVATION_IDS = Set.of(
      "Dim", "Ness", "With", "Without", "Related", "JustLike", "Agt", "Become", "Acquire", "Ly");

  private final boolean includeLemmaAndSurface;

  public UniMorphFormatter(boolean includeLemmaAndSurface) {
    this.includeLemmaAndSurface = includeLemmaAndSurface;
  }

  @Override
  public String format(SingleAnalysis analysis) {
    String surface = analysis.surfaceForm();
    if (analysis.isUnknown()) {
      return includeLemmaAndSurface ? (surface + "\t" + surface + "\tUNK") : "UNK";
    }
    String bundle = formatBundle(analysis);
    return includeLemmaAndSurface
        ? (resolveCitationLemma(analysis) + "\t" + surface + "\t" + bundle)
        : bundle;
  }

  public String formatBundle(SingleAnalysis analysis) {
    if (analysis.isUnknown()) {
      return "UNK";
    }
    DictionaryItem item = analysis.getDictionaryItem();
    if (item.primaryPos == PrimaryPos.Punctuation) {
      return "PUNCT";
    }

    int gc = analysis.groupCount();
    int lastLexGroup = 0;
    PrimaryPos basePos = item.primaryPos;
    for (int g = 1; g < gc; g++) {
      MorphemeGroup grp = analysis.getGroup(g);
      Morpheme deriv = grp.getMorphemes().getFirst().morpheme;
      if (LEXICAL_DERIVATION_IDS.contains(deriv.id)) {
        lastLexGroup = g;
        if (grp.getPos() != PrimaryPos.Unknown) {
          basePos = grp.getPos();
        }
      }
    }

    List<Morpheme> baseMorphemes = new ArrayList<>();
    List<Morpheme> copulaMorphemes = null;

    for (int g = lastLexGroup; g < gc; g++) {
      MorphemeGroup grp = analysis.getGroup(g);
      List<MorphemeData> mds = grp.getMorphemes();
      boolean isZeroVerbGroup = g > lastLexGroup
          && !mds.isEmpty()
          && mds.getFirst().morpheme == TurkishMorphotactics.zero
          && grp.getPos() == PrimaryPos.Verb;

      int start = (g == 0) ? 1 : (mds.size() > 1 && mds.get(1).morpheme.pos != null ? 2 : 1);
      if (isZeroVerbGroup) {
        copulaMorphemes = new ArrayList<>(mds.size() - start);
        for (int i = start; i < mds.size(); i++) {
          copulaMorphemes.add(mds.get(i).morpheme);
        }
      } else {
        if (g > lastLexGroup && !mds.isEmpty() && !LEXICAL_DERIVATION_IDS.contains(mds.getFirst().morpheme.id)) {
          baseMorphemes.add(mds.getFirst().morpheme);
        }
        for (int i = start; i < mds.size(); i++) {
          baseMorphemes.add(mds.get(i).morpheme);
        }
      }
    }

    if (basePos == PrimaryPos.Verb) {
      if (copulaMorphemes != null) {
        baseMorphemes.addAll(copulaMorphemes);
      }
      boolean nominalizedTail =
          analysis.getPos() == PrimaryPos.Noun || analysis.getPos() == PrimaryPos.Pronoun;
      return buildVerbBundle(item, baseMorphemes, nominalizedTail);
    }
    if (basePos == PrimaryPos.Noun || basePos == PrimaryPos.Pronoun) {
      return buildNominalBundle(basePos, baseMorphemes, copulaMorphemes);
    }
    if (basePos == PrimaryPos.Adjective && copulaMorphemes != null) {
      return buildPredicativeAdjectiveBundle(copulaMorphemes);
    }

    return switch (basePos) {
      case Adjective -> "ADJ";
      case Adverb, Duplicator -> "ADV";
      case Numeral -> "NUM";
      case Determiner -> "DET";
      case PostPositive -> "ADP";
      case Conjunction -> "CONJ";
      case Interjection -> "INTJ";
      case Question -> "PART;INTR";
      default -> "X";
    };
  }

  private static String resolveCitationLemma(SingleAnalysis analysis) {
    DictionaryItem item = analysis.getDictionaryItem();
    int gc = analysis.groupCount();
    String built = analysis.getGroup(0).surfaceForm();
    String lexStem = null;
    PrimaryPos lexPos = item.primaryPos;

    for (int g = 1; g < gc; g++) {
      MorphemeGroup grp = analysis.getGroup(g);
      MorphemeData d = grp.getMorphemes().getFirst();
      if (LEXICAL_DERIVATION_IDS.contains(d.morpheme.id)) {
        String cand = built + d.surface;
        if ((d.morpheme == TurkishMorphotactics.ness || d.morpheme == TurkishMorphotactics.dim)
            && cand.endsWith("ğ")) {
          cand = cand.substring(0, cand.length() - 1) + "k";
        }
        lexStem = cand;
        if (grp.getPos() != PrimaryPos.Unknown) {
          lexPos = grp.getPos();
        }
      }
      built += grp.surfaceForm();
    }

    if (lexStem != null) {
      return lexPos == PrimaryPos.Verb ? appendInfinitiveSuffix(lexStem) : lexStem;
    }
    return item.lemma;
  }

  private static String appendInfinitiveSuffix(String stem) {
    for (int i = stem.length() - 1; i >= 0; i--) {
      char c = stem.charAt(i);
      if ("eioöüî".indexOf(c) >= 0) return stem + "mek";
      if ("aıouû".indexOf(c) >= 0) return stem + "mak";
    }
    return stem + "mak";
  }

  private static String buildNominalBundle(
      PrimaryPos basePos, List<Morpheme> nominalMorphemes, List<Morpheme> copulaMorphemes) {
    String posTag = basePos == PrimaryPos.Pronoun ? "PRO" : "N";

    String caseTag = "NOM";
    String numTag = "SG";
    String possTag = null;

    for (Morpheme m : nominalMorphemes) {
      switch (m.id) {
        case "A1pl", "A2pl", "A3pl" -> numTag = "PL";
        case "P1sg" -> possTag = "PSS1S";
        case "P2sg" -> possTag = "PSS2S";
        case "P3sg" -> possTag = "PSS3S";
        case "P1pl" -> possTag = "PSS1P";
        case "P2pl" -> possTag = "PSS2P";
        case "P3pl" -> possTag = "PSS3P";
        case "Acc" -> caseTag = "ACC";
        case "Dat" -> caseTag = "DAT";
        case "Loc" -> caseTag = "LOC";
        case "Abl" -> caseTag = "ABL";
        case "Gen" -> caseTag = "GEN";
        case "Ins" -> caseTag = "INS";
        case "Equ" -> caseTag = "EQTV";
        default -> {}
      }
    }

    if (copulaMorphemes != null) {
      String argNo = "PL".equals(numTag) ? "ARGNO3P" : "ARGNO3S";
      String tam = "PRS";
      for (Morpheme m : copulaMorphemes) {
        switch (m.id) {
          case "A1sg" -> argNo = "ARGNO1S";
          case "A2sg" -> argNo = "ARGNO2S";
          case "A3sg" -> { /* keep noun-derived 3S/3P */ }
          case "A1pl" -> argNo = "ARGNO1P";
          case "A2pl" -> argNo = "ARGNO2P";
          case "A3pl" -> argNo = "ARGNO3P";
          case "Past" -> tam = "PST";
          case "Narr" -> tam = "LGSPEC2";
          case "Cond" -> tam = "COND";
          default -> {}
        }
      }
      return posTag + ";" + argNo + ";" + numTag + ";" + tam;
    }

    return possTag == null
        ? (posTag + ";" + caseTag + ";" + numTag)
        : (posTag + ";" + caseTag + ";" + numTag + ";" + possTag);
  }

  private static String buildPredicativeAdjectiveBundle(List<Morpheme> copulaMorphemes) {
    String tam = "PRS";
    String person = "3";
    String num = "SG";
    for (Morpheme m : copulaMorphemes) {
      switch (m.id) {
        case "Past" -> tam = "PST";
        case "Narr" -> tam = "LGSPEC2";
        case "A1sg" -> { person = "1"; num = "SG"; }
        case "A2sg" -> { person = "2"; num = "SG"; }
        case "A3sg" -> { person = "3"; num = "SG"; }
        case "A1pl" -> { person = "1"; num = "PL"; }
        case "A2pl" -> { person = "2"; num = "PL"; }
        case "A3pl" -> { person = "3"; num = "PL"; }
        default -> {}
      }
    }
    return "ADJ;LGSPEC1;" + tam + ";" + person + ";" + num + ";POS;DECL";
  }

  private static String buildVerbBundle(
      DictionaryItem item, List<Morpheme> morphemes, boolean nominalizedTail) {
    List<String> voices = new ArrayList<>(2);
    boolean pot = false;
    String polarity = "değil".equals(item.root) ? "NEG" : "POS";
    String nonFinite = null;
    String mood = "IND";
    String evidential = null;
    String tense = null;
    String aspect = null;
    String person = "3";
    String number = "SG";
    String caseTag = null;
    String possTag = null;
    boolean seenPrimaryTam = false;

    for (Morpheme m : morphemes) {
      switch (m.id) {
        case "Caus" -> voices.add("CAUS");
        case "Pass" -> voices.add("PASS");
        case "Reflex" -> voices.add("REFL");
        case "Recip" -> voices.add("RECP");
        case "Able" -> pot = true;
        case "Unable" -> { pot = true; polarity = "NEG"; }
        case "Neg" -> polarity = "NEG";

        // Non-finite projections
        case "PresPart" -> { nonFinite = "V.PTCP"; tense = "PRS"; }
        case "PastPart", "NarrPart" -> { nonFinite = "V.PTCP"; tense = "PST"; }
        case "FutPart" -> { nonFinite = "V.PTCP"; tense = "FUT"; }
        case "AorPart" -> { nonFinite = "V.PTCP"; aspect = "HAB"; }
        case "Inf1", "Inf2", "Inf3", "ActOf" -> nonFinite = "V.MSDR";
        case "NotState" -> { nonFinite = "V.MSDR"; polarity = "NEG"; }
        case "ByDoingSo", "AfterDoingSo", "When", "While",
             "SinceDoingSo", "AsLongAs", "Adamantly", "AsIf" -> nonFinite = "V.CVB";
        case "WithoutHavingDoneSo" -> { nonFinite = "V.CVB"; polarity = "NEG"; }
        case "WithoutBeingAbleToHaveDoneSo" -> { nonFinite = "V.CVB"; pot = true; polarity = "NEG"; }

        // Finite TAM
        case "Prog1", "Prog2" -> { tense = "PRS"; aspect = "PROG"; seenPrimaryTam = true; }
        case "Aor" -> { tense = "PRS"; aspect = "HAB"; seenPrimaryTam = true; }
        case "Fut" -> { tense = "FUT"; seenPrimaryTam = true; }
        case "Past" -> {
          if (seenPrimaryTam) {
            if ("FUT".equals(tense)) aspect = "PROSP";
            else if ("PST".equals(tense)) aspect = "PFV";
          }
          tense = "PST";
          seenPrimaryTam = true;
        }
        case "Narr" -> {
          evidential = "INFR";
          if (!seenPrimaryTam) tense = "PST";
          seenPrimaryTam = true;
        }
        case "Neces" -> {
          mood = "OBLIG";
          if (tense == null) tense = "PRS";
          seenPrimaryTam = true;
        }
        case "Imp" -> mood = "IMP";
        case "Opt" -> mood = "OPT";
        case "Desr", "Cond" -> mood = "COND";

        // Agreement & nominalized suffixes
        case "A1sg" -> { person = "1"; number = "SG"; }
        case "A2sg" -> { person = "2"; number = "SG"; }
        case "A3sg" -> { person = "3"; number = "SG"; }
        case "A1pl" -> { person = "1"; number = "PL"; }
        case "A2pl" -> { person = "2"; number = "PL"; }
        case "A3pl" -> { person = "3"; number = "PL"; }
        case "P1sg" -> possTag = "PSS1S";
        case "P2sg" -> possTag = "PSS2S";
        case "P3sg" -> possTag = "PSS3S";
        case "P1pl" -> possTag = "PSS1P";
        case "P2pl" -> possTag = "PSS2P";
        case "P3pl" -> possTag = "PSS3P";
        case "Acc", "Dat", "Loc", "Abl", "Gen", "Ins" -> caseTag = m.id.toUpperCase();
        default -> {}
      }
    }

    List<String> tags = new ArrayList<>(10);
    if (nonFinite != null) {
      tags.add(nonFinite);
      tags.addAll(voices);
      if (pot) tags.add("POT");
      if (tense != null) tags.add(tense);
      if (aspect != null) tags.add(aspect);
      tags.add(polarity);
      if (nominalizedTail && caseTag == null) {
        caseTag = "NOM";
      }
      if (caseTag != null) tags.add(caseTag);
      if ("PL".equals(number)) tags.add("PL");
      if (possTag != null) tags.add(possTag);
      return String.join(";", tags);
    }

    tags.add("V");
    tags.addAll(voices);
    if (pot) tags.add("POT");

    if ("IMP".equals(mood)) {
      tags.add("IMP");
      tags.add(person);
      tags.add(number);
      tags.add(polarity);
      tags.add("DECL");
      return String.join(";", tags);
    }

    if ("OBLIG".equals(mood)) {
      tags.add("OBLIG");
      if (evidential != null) tags.add(evidential);
    } else if (evidential != null) {
      tags.add(evidential);
      if (!"IND".equals(mood)) tags.add(mood);
    } else {
      tags.add(mood);
    }

    if (tense != null) tags.add(tense);
    if (aspect != null) tags.add(aspect);
    tags.add(person);
    tags.add(number);
    tags.add(polarity);
    tags.add("DECL");
    return String.join(";", tags);
  }
}
