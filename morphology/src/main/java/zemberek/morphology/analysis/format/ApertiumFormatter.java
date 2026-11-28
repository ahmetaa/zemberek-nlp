package zemberek.morphology.analysis.format;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeGroup;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * Formats {@link SingleAnalysis}, {@link WordAnalysis}, and {@link SentenceAnalysis} into the
 * Apertium Turkic tagset and lexical stream format ({@code ^kitaplarımızdan/kitap<n><pl><px1pl><abl>$})
 * shared across {@code apertium-tur}, {@code apertium-kaz}, {@code apertium-tat},
 * {@code apertium-uzb}, {@code apertium-kir}, {@code apertium-uig}, and {@code apertium-aze}.
 */
public final class ApertiumFormatter implements AnalysisFormatter {

  public static final ApertiumFormatter INSTANCE = new ApertiumFormatter();

  private static final Set<String> SUBORDINATING_CONJ = Set.of(
      "ki", "eğer", "çünkü", "şayet", "madem", "mademki", "diye", "halbuki", "oysaki");

  @Override
  public String format(SingleAnalysis analysis) {
    if (analysis.isUnknown()) {
      return "*" + analysis.surfaceForm();
    }

    DictionaryItem item = analysis.getDictionaryItem();
    if (item.primaryPos == PrimaryPos.Punctuation) {
      String surf = analysis.surfaceForm();
      return surf + mapPunctuationTag(surf);
    }

    if (item.primaryPos == PrimaryPos.Question) {
      StringBuilder q = new StringBuilder("mi<qst>");
      List<MorphemeData> mds = analysis.getMorphemeDataList();
      if (analysis.hasEnding()) {
        q.append("<cop>");
        appendVerbalMorphemes(q, mds.subList(1, mds.size()), true);
      }
      return q.toString();
    }

    StringBuilder sb = new StringBuilder(analysis.getMorphemeDataList().size() * 6 + 12);
    sb.append(resolveLemma(analysis, item));
    appendRootPosTags(sb, item);

    int gc = analysis.groupCount();
    for (int g = 0; g < gc; g++) {
      MorphemeGroup grp = analysis.getGroup(g);
      List<MorphemeData> mds = grp.getMorphemes();
      int startIdx = 1;
      PrimaryPos groupPos = item.primaryPos;
      boolean isZeroCopula = false;

      if (g > 0) {
        Morpheme deriv = mds.getFirst().morpheme;
        PrimaryPos targetPos = mds.size() > 1 ? mds.get(1).morpheme.pos : PrimaryPos.Unknown;
        startIdx = (mds.size() > 1 && targetPos != null) ? 2 : 1;
        groupPos = (targetPos != null && targetPos != PrimaryPos.Unknown) ? targetPos : groupPos;
        isZeroCopula = deriv == TurkishMorphotactics.zero && groupPos == PrimaryPos.Verb;
        appendDerivationTag(sb, deriv, targetPos);
      }

      List<MorphemeData> tail = mds.subList(startIdx, mds.size());
      switch (groupPos) {
        case Verb -> appendVerbalMorphemes(sb, tail, isZeroCopula);
        case Pronoun -> appendNominalMorphemes(sb, tail, true);
        default -> appendNominalMorphemes(sb, tail, false);
      }
    }
    return sb.toString();
  }

  /**
   * Formats all distinct morphological readings of a word as an Apertium lexical unit:
   * {@code ^surface/reading1/reading2$} (or {@code ^surface/*surface$} when unknown).
   */
  public String formatWordStream(WordAnalysis wordAnalysis) {
    String surface = escapeApertium(wordAnalysis.getInput());
    if (!wordAnalysis.isCorrect()) {
      return "^" + surface + "/*" + surface + "$";
    }
    LinkedHashSet<String> readings = new LinkedHashSet<>();
    for (SingleAnalysis sa : wordAnalysis) {
      readings.add(format(sa));
    }
    return "^" + surface + "/" + String.join("/", readings) + "$";
  }

  /**
   * Formats a disambiguated sentence as space-separated Apertium lexical units
   * ({@code ^word1/best1$ ^word2/best2$}).
   */
  public String formatSentenceStream(SentenceAnalysis sentenceAnalysis) {
    List<SentenceWordAnalysis> words = sentenceAnalysis.getWordAnalyses();
    StringBuilder sb = new StringBuilder(words.size() * 28);
    for (int i = 0; i < words.size(); i++) {
      if (i > 0) {
        sb.append(' ');
      }
      SentenceWordAnalysis swa = words.get(i);
      String surface = escapeApertium(swa.getWordAnalysis().getInput());
      SingleAnalysis best = swa.getBestAnalysis();
      sb.append('^').append(surface).append('/')
          .append(best.isUnknown() ? "*" + surface : format(best))
          .append('$');
    }
    return sb.toString();
  }

  private static String resolveLemma(SingleAnalysis analysis, DictionaryItem item) {
    if ("imek".equals(item.lemma)) {
      return "i";
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

  private static void appendRootPosTags(StringBuilder sb, DictionaryItem item) {
    if ("imek".equals(item.lemma)) {
      sb.append("<cop>");
      return;
    }
    if ("değil".equals(item.root) && item.primaryPos == PrimaryPos.Verb) {
      sb.append("<vaux><neg>");
      return;
    }
    switch (item.primaryPos) {
      case Noun -> sb.append(switch (item.secondaryPos) {
        case ProperNoun -> "<np><al>";
        case Abbreviation, RegularAbbreviation -> "<n><abbr>";
        default -> "<n>";
      });
      case Verb -> sb.append("<v>");
      case Adjective -> sb.append("<adj>");
      case Adverb, Duplicator -> sb.append("<adv>");
      case Pronoun -> {
        sb.append("<prn>");
        if ("birbiri".equals(item.root)) {
          sb.append("<recip>");
        } else {
          sb.append(switch (item.secondaryPos) {
            case PersonalPron -> "<pers>";
            case DemonstrativePron -> "<dem>";
            case QuestionPron -> "<itg>";
            case ReflexivePron -> "<ref>";
            case QuantitivePron -> "<qnt>";
            default -> "";
          });
        }
      }
      case Determiner -> sb.append(switch (item.root) {
        case "bu", "şu", "o" -> "<det><dem>";
        default -> "<det><ind>";
      });
      case Numeral -> sb.append(switch (item.secondaryPos) {
        case Ordinal -> "<num><ord>";
        case Distribution -> "<num><dist>";
        case Percentage -> "<num><percent>";
        default -> "<num>";
      });
      case PostPositive -> sb.append("<post>");
      case Conjunction -> {
        String low = item.lemma.toLowerCase(Turkish.LOCALE);
        sb.append(SUBORDINATING_CONJ.contains(low) ? "<cnjsub>" : "<cnjcoo>");
      }
      case Interjection -> sb.append("<ij>");
      default -> {}
    }
  }

  private static void appendDerivationTag(StringBuilder sb, Morpheme deriv, PrimaryPos targetPos) {
    sb.append(switch (deriv.id) {
      case "Zero" -> switch (targetPos) {
        case Noun -> "<subst>";
        case Verb -> "<cop>";
        default -> "";
      };
      case "Ness" -> "<abstr><n>";
      case "With" -> "<with><adj>";
      case "Without" -> "<without><adj>";
      case "Dim" -> "<dim><n>";
      case "Agt" -> targetPos == PrimaryPos.Adjective ? "<agt><adj>" : "<agt><n>";
      case "Rel" -> targetPos == PrimaryPos.Pronoun ? "<rel><prn>" : "<rel><adj>";
      case "Related" -> "<rel><adj>";
      case "JustLike" -> "<sim><adj>";
      case "Ly" -> "<advl>";
      case "Become" -> "<der_become><v>";
      case "Acquire" -> "<der_acquire><v>";
      case "Caus" -> "<caus>";
      case "Pass" -> "<pass>";
      case "Reflex" -> "<ref>";
      case "Recip" -> "<coop>";
      case "Able" -> "<abil>";
      case "Hastily" -> "<iver>";
      case "Almost" -> "<eyaz>";
      case "Inf1" -> "<inf>";
      case "Inf2", "ActOf" -> "<ger>";
      case "Inf3" -> "<ger_abst>";
      case "NotState" -> "<neg><ger_abst>";
      case "PresPart" -> targetPos == PrimaryPos.Noun ? "<gpr_rsub><subst>" : "<gpr_rsub>";
      case "PastPart", "NarrPart" -> targetPos == PrimaryPos.Noun ? "<ger_past>" : "<gpr_past>";
      case "FutPart" -> targetPos == PrimaryPos.Noun ? "<ger_fut>" : "<gpr_fut>";
      case "AorPart" -> "<gpr_aor>";
      case "FeelLike" -> "<gpr_pot>";
      case "AfterDoingSo" -> "<gna_perf>";
      case "ByDoingSo" -> "<gna_impf>";
      case "When" -> "<gna_when>";
      case "While" -> "<gna_ken>";
      case "AsLongAs" -> "<gna_cond>";
      case "WithoutHavingDoneSo" -> "<gna_neg>";
      case "WithoutBeingAbleToHaveDoneSo" -> "<abil><gna_neg>";
      default -> "<" + deriv.id.toLowerCase() + ">";
    });
  }

  private static void appendNominalMorphemes(
      StringBuilder sb, List<MorphemeData> tail, boolean isPronoun) {
    boolean sawCase = false;
    boolean sawAgreement = false;
    for (MorphemeData md : tail) {
      switch (md.morpheme.id) {
        case "A1sg" -> { sb.append("<p1><sg>"); sawAgreement = true; }
        case "A2sg" -> { sb.append("<p2><sg>"); sawAgreement = true; }
        case "A3sg" -> { if (isPronoun) sb.append("<p3><sg>"); sawAgreement = true; }
        case "A1pl" -> { sb.append("<p1><pl>"); sawAgreement = true; }
        case "A2pl" -> { sb.append("<p2><pl>"); sawAgreement = true; }
        case "A3pl" -> { sb.append(isPronoun ? "<p3><pl>" : "<pl>"); sawAgreement = true; }
        case "P1sg" -> sb.append("<px1sg>");
        case "P2sg" -> sb.append("<px2sg>");
        case "P3sg" -> sb.append("<px3sp>");
        case "P1pl" -> sb.append("<px1pl>");
        case "P2pl" -> sb.append("<px2pl>");
        case "P3pl" -> sb.append("<px3pl>");
        case "Nom" -> { sb.append("<nom>"); sawCase = true; }
        case "Gen" -> { sb.append("<gen>"); sawCase = true; }
        case "Dat" -> { sb.append("<dat>"); sawCase = true; }
        case "Acc" -> { sb.append("<acc>"); sawCase = true; }
        case "Loc" -> { sb.append("<loc>"); sawCase = true; }
        case "Abl" -> { sb.append("<abl>"); sawCase = true; }
        case "Ins" -> { sb.append("<ins>"); sawCase = true; }
        case "Equ" -> { sb.append("<sim>"); sawCase = true; }
        default -> {}
      }
    }
    if (sawAgreement && !sawCase) {
      sb.append("<nom>");
    }
  }

  private static void appendVerbalMorphemes(
      StringBuilder sb, List<MorphemeData> tail, boolean isCopula) {
    boolean seenPrimaryTam = false;
    for (MorphemeData md : tail) {
      switch (md.morpheme.id) {
        case "Neg" -> { if (!isCopula) sb.append("<neg>"); }
        case "Unable" -> sb.append("<abil><neg>");
        case "Past" -> { sb.append("<ifi>"); seenPrimaryTam = true; }
        case "Narr" -> { sb.append(seenPrimaryTam || isCopula ? "<evid>" : "<past>"); seenPrimaryTam = true; }
        case "Prog1", "Prog2" -> { sb.append("<prog>"); seenPrimaryTam = true; }
        case "Aor" -> { sb.append("<aor>"); seenPrimaryTam = true; }
        case "Fut" -> { sb.append("<fut>"); seenPrimaryTam = true; }
        case "Neces" -> { sb.append("<nec>"); seenPrimaryTam = true; }
        case "Opt" -> { sb.append("<opt>"); seenPrimaryTam = true; }
        case "Cond", "Desr" -> { sb.append("<cond>"); seenPrimaryTam = true; }
        case "Imp" -> { sb.append("<imp>"); seenPrimaryTam = true; }
        case "Cop" -> sb.append("<gm>");
        case "A1sg" -> sb.append("<p1><sg>");
        case "A2sg" -> sb.append("<p2><sg>");
        case "A3sg" -> sb.append("<p3><sg>");
        case "A1pl" -> sb.append("<p1><pl>");
        case "A2pl" -> sb.append("<p2><pl>");
        case "A3pl" -> sb.append("<p3><pl>");
        default -> {}
      }
    }
  }

  private static String mapPunctuationTag(String surf) {
    return switch (surf) {
      case ".", "!", "?", "...", ":", ";" -> "<sent>";
      case "," -> "<cm>";
      case "-", "–", "—" -> "<guio>";
      case "(", "[", "{" -> "<lpar>";
      case ")", "]", "}" -> "<rpar>";
      case "\"", "'", "«", "»" -> "<quot>";
      default -> "<sym>";
    };
  }

  private static String escapeApertium(String text) {
    return text.replace("\\", "\\\\")
        .replace("/", "\\/")
        .replace("^", "\\^")
        .replace("$", "\\$")
        .replace("*", "\\*");
  }
}
