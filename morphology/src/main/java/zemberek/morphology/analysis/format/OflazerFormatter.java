package zemberek.morphology.analysis.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeGroup;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * Formats {@link SingleAnalysis} instances into strict Kemal Oflazer / METU-Sabancı / Haşim Sak /
 * Starlang (KeNet) morphological strings.
 *
 * <p>Unlike {@code AnalysisFormatters.OFLAZER_STYLE}, this formatter restores unmutated dictionary
 * lemmas ({@code kitap+Noun+...}, {@code git+Verb+...}), reconstructs zero-surface nominal
 * {@code +Pnon} and {@code +Nom} slots, inserts explicit {@code +Pos} verbal polarity, expands
 * {@code +Unable} into {@code ^DB+Verb+Able+Neg}, maps secondary POS tags ({@code DemonsP},
 * {@code PersP}, {@code QuesP}, {@code ReflexP}, {@code QuantP}, {@code Distrib}), and supports
 * dialect-specific IG rules via {@link Dialect}.
 */
public final class OflazerFormatter implements AnalysisFormatter {

  public enum Dialect {
    /** Classic Oflazer (1994): {@code Adverb} label, 3-participle folding, copula IG split. */
    OFLAZER_94,
    /** METU-Sabancı Treebank (2003): {@code Adv} label, capitalized {@code Prop}, copula IG split. */
    METU_SABANCI,
    /** Haşim Sak (2008): lowercase roots, {@code Adv} label, flat compound tenses. */
    SAK_2008,
    /** Starlang / KeNet WordNet / PropBank: uppercase tags after lemma ({@code kitap+NOUN+...}). */
    STARLANG_UPPER
  }

  private static final Set<String> AGREEMENT_IDS =
      Set.of("A1sg", "A2sg", "A3sg", "A1pl", "A2pl", "A3pl");

  private static final Set<String> POSSESSIVE_IDS =
      Set.of("Pnon", "P1sg", "P2sg", "P3sg", "P1pl", "P2pl", "P3pl");

  private static final Set<String> CASE_IDS =
      Set.of("Nom", "Dat", "Acc", "Abl", "Loc", "Ins", "Gen", "Equ");

  private static final Set<String> VERB_STEM_DERIV_IDS = Set.of(
      "Pass", "Caus", "Reflex", "Recip", "Able",
      "EverSince", "Repeat", "Almost", "Hastily", "Stay", "Start");

  private static final Set<String> PRIMARY_TAM_IDS = Set.of(
      "Pres", "Past", "Narr", "Cond", "Prog1", "Prog2",
      "Aor", "Fut", "Imp", "Opt", "Desr", "Neces");

  private static final Set<String> SECONDARY_TAM_IDS = Set.of("Past", "Narr", "Cond");

  private record GroupView(Morpheme derivation, PrimaryPos pos, List<Morpheme> tail) {
    static GroupView of(DictionaryItem item, MorphemeGroup group, int groupIndex) {
      List<MorphemeData> mds = group.getMorphemes();
      if (groupIndex == 0) {
        return new GroupView(null, item.primaryPos, extractMorphemes(mds, 1));
      }
      Morpheme deriv = mds.getFirst().morpheme;
      PrimaryPos targetPos = mds.size() > 1 ? mds.get(1).morpheme.pos : PrimaryPos.Unknown;
      int start = (mds.size() > 1 && targetPos != null) ? 2 : 1;
      PrimaryPos resolvedPos = targetPos != null ? targetPos : PrimaryPos.Unknown;
      return new GroupView(deriv, resolvedPos, extractMorphemes(mds, start));
    }

    private static List<Morpheme> extractMorphemes(List<MorphemeData> mds, int from) {
      List<Morpheme> list = new ArrayList<>(Math.max(0, mds.size() - from));
      for (int i = from; i < mds.size(); i++) {
        list.add(mds.get(i).morpheme);
      }
      return list;
    }
  }

  private final Dialect dialect;

  public OflazerFormatter(Dialect dialect) {
    this.dialect = dialect;
  }

  public static OflazerFormatter oflazer94() {
    return new OflazerFormatter(Dialect.OFLAZER_94);
  }

  public static OflazerFormatter metuSabanci() {
    return new OflazerFormatter(Dialect.METU_SABANCI);
  }

  public static OflazerFormatter sak2008() {
    return new OflazerFormatter(Dialect.SAK_2008);
  }

  public static OflazerFormatter starlangUpper() {
    return new OflazerFormatter(Dialect.STARLANG_UPPER);
  }

  public Dialect getDialect() {
    return dialect;
  }

  @Override
  public String format(SingleAnalysis analysis) {
    if (analysis.isUnknown()) {
      return analysis.surfaceForm() + "+?";
    }
    DictionaryItem item = analysis.getDictionaryItem();
    if (item.primaryPos == PrimaryPos.Punctuation) {
      return analysis.surfaceForm() + "+" + tag("Punc");
    }

    StringBuilder out = new StringBuilder(analysis.getMorphemeDataList().size() * 6 + 16);
    out.append(resolveRoot(analysis, item));

    int groupCount = analysis.groupCount();
    for (int g = 0; g < groupCount; g++) {
      GroupView cur = GroupView.of(item, analysis.getGroup(g), g);
      Morpheme nextDeriv = (g + 1 < groupCount)
          ? analysis.getGroup(g + 1).getMorphemes().getFirst().morpheme
          : null;
      appendGroup(out, item, cur, g, nextDeriv);
    }
    return out.toString();
  }

  private String resolveRoot(SingleAnalysis analysis, DictionaryItem item) {
    if (item.primaryPos == PrimaryPos.Verb || dialect == Dialect.SAK_2008) {
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

  private void appendGroup(
      StringBuilder out,
      DictionaryItem item,
      GroupView group,
      int groupIndex,
      Morpheme nextDeriv) {
    PrimaryPos effectivePos = group.pos();

    if (groupIndex == 0) {
      out.append('+').append(tag(posName(effectivePos)));
      String sec = mapSecondaryPos(item.primaryPos, item.secondaryPos);
      if (sec != null) {
        out.append('+').append(tag(sec));
      }
    } else {
      Morpheme deriv = group.derivation();
      if (deriv == TurkishMorphotactics.withoutBeingAbleToHaveDoneSo
          && dialect != Dialect.STARLANG_UPPER) {
        out.append("^DB+").append(tag("Verb"))
            .append('+').append(tag("Able"))
            .append('+').append(tag("Neg"))
            .append("^DB+").append(tag(posName(PrimaryPos.Adverb)))
            .append('+').append(tag("WithoutHavingDoneSo"));
        return;
      }
      effectivePos = appendDerivationHeader(out, deriv, effectivePos);
    }

    switch (effectivePos) {
      case Noun, Pronoun -> appendNominalTail(out, group.tail());
      case Verb -> appendVerbalTail(
          out,
          item,
          groupIndex,
          group.derivation() == TurkishMorphotactics.zero,
          group.tail(),
          nextDeriv);
      default -> {
        for (Morpheme m : group.tail()) {
          out.append('+').append(tag(m.id));
        }
      }
    }
  }

  private PrimaryPos appendDerivationHeader(
      StringBuilder out, Morpheme deriv, PrimaryPos targetPos) {
    boolean expandTreebankIg = dialect == Dialect.OFLAZER_94 || dialect == Dialect.METU_SABANCI;

    // Substantivized PresPart / NarrPart: ^DB+Noun+PresPart -> ^DB+Adj+PresPart^DB+Noun+Zero
    if (expandTreebankIg
        && targetPos == PrimaryPos.Noun
        && (deriv == TurkishMorphotactics.presPart || deriv == TurkishMorphotactics.narrPart)) {
      out.append("^DB+").append(tag("Adj"))
          .append('+').append(tag(mapDerivId(deriv)))
          .append("^DB+").append(tag("Noun"))
          .append('+').append(tag("Zero"));
      return PrimaryPos.Noun;
    }

    // Genitive -ki: ^DB+Pron+Rel -> ^DB+Noun+Rel in Oflazer-94 / METU-Sabancı
    if (expandTreebankIg && targetPos == PrimaryPos.Pronoun && deriv == TurkishMorphotactics.rel) {
      out.append("^DB+").append(tag("Noun")).append('+').append(tag("Rel"));
      return PrimaryPos.Noun;
    }

    out.append("^DB+");
    if (targetPos != PrimaryPos.Unknown) {
      out.append(tag(posName(targetPos))).append('+');
    }
    out.append(tag(mapDerivId(deriv)));
    return targetPos;
  }

  private void appendNominalTail(StringBuilder out, List<Morpheme> tail) {
    int agrIdx = -1;
    for (int i = 0; i < tail.size(); i++) {
      if (AGREEMENT_IDS.contains(tail.get(i).id)) {
        agrIdx = i;
        break;
      }
    }
    if (agrIdx < 0) {
      for (Morpheme m : tail) {
        out.append('+').append(tag(m.id));
      }
      return;
    }

    for (int i = 0; i <= agrIdx; i++) {
      out.append('+').append(tag(tail.get(i).id));
    }

    int idx = agrIdx + 1;
    if (idx < tail.size() && POSSESSIVE_IDS.contains(tail.get(idx).id)) {
      out.append('+').append(tag(tail.get(idx++).id));
    } else {
      out.append('+').append(tag("Pnon"));
    }

    if (idx < tail.size() && CASE_IDS.contains(tail.get(idx).id)) {
      out.append('+').append(tag(tail.get(idx++).id));
    } else {
      out.append('+').append(tag("Nom"));
    }

    while (idx < tail.size()) {
      out.append('+').append(tag(tail.get(idx++).id));
    }
  }

  private void appendVerbalTail(
      StringBuilder out,
      DictionaryItem item,
      int groupIndex,
      boolean isZeroCopula,
      List<Morpheme> tail,
      Morpheme nextDeriv) {
    boolean isDegil = groupIndex == 0 && "değil".equals(item.root);
    boolean isImek = groupIndex == 0 && "imek".equals(item.lemma);

    int idx = 0;
    boolean handledPolarity = false;

    if (idx < tail.size() && tail.get(idx) == TurkishMorphotactics.unable) {
      out.append("^DB+").append(tag("Verb"))
          .append('+').append(tag("Able"))
          .append('+').append(tag("Neg"));
      idx++;
      handledPolarity = true;
    } else if (idx < tail.size() && tail.get(idx) == TurkishMorphotactics.neg) {
      if (!isDegil || dialect == Dialect.STARLANG_UPPER) {
        out.append('+').append(tag("Neg"));
      }
      idx++;
      handledPolarity = true;
    }

    if (!handledPolarity && !isDegil && !isImek && !isZeroCopula) {
      boolean nextContinuesVerbStem = nextDeriv != null
          && (VERB_STEM_DERIV_IDS.contains(nextDeriv.id)
              || (nextDeriv == TurkishMorphotactics.withoutBeingAbleToHaveDoneSo
                  && dialect != Dialect.STARLANG_UPPER));
      if (!nextContinuesVerbStem) {
        out.append('+').append(tag("Pos"));
      }
    }

    List<Morpheme> rem = tail.subList(idx, tail.size());

    if (dialect == Dialect.OFLAZER_94 || dialect == Dialect.METU_SABANCI) {
      int splitIdx = findSecondaryTamIndex(rem);
      if (splitIdx > 0) {
        List<Morpheme> hostTam = rem.subList(0, splitIdx);
        List<Morpheme> copulaTam = new ArrayList<>(rem.subList(splitIdx, rem.size()));
        if (copulaTam.stream().noneMatch(m -> AGREEMENT_IDS.contains(m.id))) {
          copulaTam.add(TurkishMorphotactics.a3sg);
        }
        emitVerbMorphemes(out, hostTam);
        out.append("^DB+").append(tag("Verb")).append('+').append(tag("Zero"));
        emitVerbMorphemes(out, copulaTam);
        return;
      }
    }

    emitVerbMorphemes(out, rem);
  }

  private static int findSecondaryTamIndex(List<Morpheme> morphemes) {
    boolean seenPrimary = false;
    for (int i = 0; i < morphemes.size(); i++) {
      String id = morphemes.get(i).id;
      if (!seenPrimary) {
        seenPrimary = PRIMARY_TAM_IDS.contains(id);
      } else if (SECONDARY_TAM_IDS.contains(id)) {
        return i;
      }
    }
    return -1;
  }

  private void emitVerbMorphemes(StringBuilder out, List<Morpheme> morphemes) {
    for (int i = 0; i < morphemes.size(); i++) {
      Morpheme cur = morphemes.get(i);
      if (cur == TurkishMorphotactics.a3sg
          && i + 1 < morphemes.size()
          && morphemes.get(i + 1) == TurkishMorphotactics.cop) {
        out.append('+').append(tag("Cop")).append('+').append(tag("A3sg"));
        i++;
      } else {
        out.append('+').append(tag(cur.id));
      }
    }
  }

  private String posName(PrimaryPos pos) {
    return (pos == PrimaryPos.Adverb && dialect == Dialect.OFLAZER_94) ? "Adverb" : pos.shortForm;
  }

  private String mapSecondaryPos(PrimaryPos primaryPos, SecondaryPos sec) {
    if (sec == null || sec == SecondaryPos.None || sec == SecondaryPos.UnknownSec) {
      return null;
    }
    return switch (sec) {
      case DemonstrativePron -> "DemonsP";
      case PersonalPron -> dialect == Dialect.STARLANG_UPPER ? "Pers" : "PersP";
      case QuestionPron -> "QuesP";
      case ReflexivePron -> "ReflexP";
      case QuantitivePron -> "QuantP";
      case Distribution -> "Distrib";
      case RomanNumeral -> "Card";
      case Abbreviation, RegularAbbreviation -> primaryPos == PrimaryPos.Noun ? "Prop" : null;
      case Time -> dialect == Dialect.OFLAZER_94 ? "Time" : null;
      case Email, Url, Mention, HashTag, Emoticon, Clock, Date ->
          dialect == Dialect.STARLANG_UPPER ? sec.shortForm : null;
      default -> sec.shortForm;
    };
  }

  private String mapDerivId(Morpheme m) {
    return switch (dialect) {
      case OFLAZER_94 -> switch (m.id) {
        case "NarrPart" -> "PastPart";
        case "AorPart" -> "PresPart";
        default -> m.id;
      };
      case STARLANG_UPPER -> "NotState".equals(m.id) ? "NotAbleState" : m.id;
      default -> m.id;
    };
  }

  private String tag(String raw) {
    return dialect == Dialect.STARLANG_UPPER ? raw.toUpperCase(Locale.ROOT) : raw;
  }
}
