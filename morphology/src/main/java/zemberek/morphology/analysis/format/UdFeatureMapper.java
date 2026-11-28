package zemberek.morphology.analysis.format;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeGroup;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * Universal Dependencies v2 ({@code UD_Turkish-BOUN} / UD Turkic Harmonized and
 * {@code UD_Turkish-IMST}) morphosyntactic projector for Zemberek {@link SingleAnalysis} results.
 */
final class UdFeatureMapper {

  record ProjectedToken(
      String lemma,
      String upos,
      String xpos,
      String feats,
      String derivedFrom) {}

  static final Set<String> LEXICAL_DERIVATION_IDS = Set.of(
      "Dim", "Ness", "With", "Without", "Related", "JustLike", "Agt", "Become", "Acquire", "Ly");

  private static final Set<String> PARTICIPLE_IDS =
      Set.of("PresPart", "PastPart", "NarrPart", "FutPart", "AorPart", "FeelLike");

  private static final Set<String> VNOUN_IDS =
      Set.of("Inf1", "Inf2", "Inf3", "ActOf", "NotState");

  private static final Set<String> CONVERB_IDS = Set.of(
      "ByDoingSo", "While", "When", "SinceDoingSo", "AsLongAs", "AfterDoingSo",
      "Adamantly", "AsIf", "WithoutHavingDoneSo", "WithoutBeingAbleToHaveDoneSo");

  private static final Set<String> SUBORDINATING_CONJUNCTIONS = Set.of(
      "ki", "eğer", "çünkü", "şayet", "madem", "mademki", "diye", "halbuki", "oysaki");

  private static final Set<String> FOCUS_PARTICLE_CONJUNCTIONS = Set.of("da", "de");

  private static final Set<String> LOCATIVE_PRONOUNS = Set.of("bura", "şura", "ora");
  private static final Set<String> NEGATIVE_PRONOUNS = Set.of("hiçbiri", "kimse");

  /**
   * Official CoNLL-U v2 feature-key comparator: alphabetical case-insensitive with bracketed
   * sub-features sorted immediately after their base feature ({@code Number < Number[psor]}).
   */
  static final Comparator<String> CONLLU_FEAT_KEY_ORDER = (a, b) -> {
    int cmp = stripBracketsLower(a).compareTo(stripBracketsLower(b));
    return cmp != 0 ? cmp : a.toLowerCase(Locale.ROOT).compareTo(b.toLowerCase(Locale.ROOT));
  };

  private static String stripBracketsLower(String s) {
    return s.replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
  }

  private UdFeatureMapper() {}

  static ProjectedToken projectUnknown(String surface) {
    if (surface != null && !surface.isEmpty() && surface.chars().noneMatch(Character::isLetterOrDigit)) {
      return new ProjectedToken(surface, "PUNCT", "Punc", "_", null);
    }
    if (surface != null && !surface.isEmpty() && Character.isUpperCase(surface.charAt(0))) {
      return new ProjectedToken(surface, "PROPN", "Prop", "Case=Nom|Number=Sing|Person=3", null);
    }
    String low = surface == null ? "_" : surface.toLowerCase(Turkish.LOCALE);
    return new ProjectedToken(low, "X", "_", "_", null);
  }

  static ProjectedToken projectFull(
      SingleAnalysis analysis,
      String surface,
      ConlluFormatter.Dialect dialect) {
    return projectSlice(
        analysis, 0, analysis.groupCount() - 1, analysis.getMorphemeDataList(), surface, dialect);
  }

  static ProjectedToken projectGroupRange(
      SingleAnalysis analysis,
      int startGroup,
      int endGroup,
      List<MorphemeData> explicitMorphemes,
      String surface,
      ConlluFormatter.Dialect dialect) {
    List<MorphemeData> mds = explicitMorphemes;
    if (mds == null) {
      mds = new ArrayList<>();
      for (int g = startGroup; g <= endGroup; g++) {
        mds.addAll(analysis.getGroup(g).getMorphemes());
      }
    }
    return projectSlice(analysis, startGroup, endGroup, mds, surface, dialect);
  }

  static ProjectedToken projectBoundCopula(
      List<MorphemeData> copulaMorphemes,
      ConlluFormatter.Dialect dialect) {
    Map<String, String> feats = new LinkedHashMap<>();
    TreeSet<String> moods = new TreeSet<>();
    extractInflectionalFeats(copulaMorphemes, false, dialect, feats, new ArrayList<>(), moods);
    if (!moods.isEmpty()) {
      feats.put("Mood", String.join("", moods));
    } else if (dialect == ConlluFormatter.Dialect.UD_IMST) {
      feats.put("Mood", "Ind");
    }
    if (!feats.containsKey("VerbForm")) {
      feats.putIfAbsent("Number", "Sing");
      feats.putIfAbsent("Person", "3");
      feats.putIfAbsent("Polarity", "Pos");
      feats.putIfAbsent("Tense", "Pres");
    } else if (dialect == ConlluFormatter.Dialect.UD_IMST) {
      feats.putIfAbsent("Aspect", "Perf");
      feats.putIfAbsent("Tense", "Pres");
      feats.remove("Polarity");
    }
    return new ProjectedToken("i", "AUX", "Zero", formatFeats(feats), null);
  }

  static ProjectedToken projectBoundKi(
      List<MorphemeData> kiTailMorphemes,
      boolean isInflectedPronominal,
      ConlluFormatter.Dialect dialect) {
    if (!isInflectedPronominal) {
      String upos = dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN ? "PART" : "ADP";
      String xpos = dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN ? "Attr" : "Rel";
      return new ProjectedToken("ki", upos, xpos, "_", null);
    }
    Map<String, String> feats = new LinkedHashMap<>();
    extractInflectionalFeats(kiTailMorphemes, false, dialect, feats, new ArrayList<>(), new TreeSet<>());
    feats.putIfAbsent("Case", "Nom");
    feats.putIfAbsent("Number", "Sing");
    feats.putIfAbsent("Person", "3");
    return new ProjectedToken("ki", "PRON", "Rel", formatFeats(feats), null);
  }

  private static ProjectedToken projectSlice(
      SingleAnalysis analysis,
      int startGroup,
      int endGroup,
      List<MorphemeData> mds,
      String surface,
      ConlluFormatter.Dialect dialect) {
    if (analysis.isUnknown()) {
      return projectUnknown(surface);
    }

    DictionaryItem item = analysis.getDictionaryItem();

    if (item.primaryPos == PrimaryPos.Punctuation) {
      return new ProjectedToken(analysis.surfaceForm(), "PUNCT", "Punc", "_", null);
    }

    if (item.primaryPos == PrimaryPos.Question) {
      return projectQuestionParticle(mds, dialect);
    }

    if ("imek".equals(item.lemma) || (item.primaryPos == PrimaryPos.Verb && "değil".equals(item.root))) {
      return projectOvertAuxiliary(item, mds, dialect);
    }

    if ((item.primaryPos == PrimaryPos.Adjective || item.primaryPos == PrimaryPos.Noun)
        && ("var".equals(item.root) || "yok".equals(item.root))
        && startGroup == 0 && endGroup == 0) {
      return projectExistential(item, mds, dialect);
    }

    // Walk groups [0 .. endGroup] to compute lexical LEMMA, DerivedFrom, and effective POS
    String baseRootLemma = computeBaseLemma(analysis, item);
    String currentLemma = baseRootLemma;
    String derivedFrom = null;
    PrimaryPos effectivePos = item.primaryPos;
    String builtSurface = analysis.getGroup(0).surfaceForm();

    boolean hasParticiple = false;
    boolean hasVnoun = false;
    boolean hasInf1 = false;
    boolean hasConverb = false;
    boolean zeroNominalizedAdj = false;
    boolean zeroNominalizedNum = false;

    for (int g = 0; g <= endGroup && g < analysis.groupCount(); g++) {
      MorphemeGroup grp = analysis.getGroup(g);
      if (g > 0 && !grp.getMorphemes().isEmpty()) {
        MorphemeData derivMd = grp.getMorphemes().getFirst();
        Morpheme deriv = derivMd.morpheme;
        PrimaryPos gPos = grp.getPos();

        if (LEXICAL_DERIVATION_IDS.contains(deriv.id)) {
          currentLemma = unvoiceDerivedLemma(builtSurface, derivMd.surface, deriv);
          if (derivedFrom == null) {
            derivedFrom = baseRootLemma;
          }
          if (gPos != PrimaryPos.Unknown) {
            effectivePos = gPos;
          }
          hasParticiple = false;
          hasVnoun = false;
          hasInf1 = false;
          hasConverb = false;
          zeroNominalizedAdj = false;
          zeroNominalizedNum = false;
        } else if (PARTICIPLE_IDS.contains(deriv.id)) {
          hasParticiple = true;
        } else if (VNOUN_IDS.contains(deriv.id)) {
          hasVnoun = true;
          if ("Inf1".equals(deriv.id)) {
            hasInf1 = true;
          }
        } else if (CONVERB_IDS.contains(deriv.id)) {
          hasConverb = true;
        } else if (gPos != PrimaryPos.Unknown && deriv != TurkishMorphotactics.zero) {
          effectivePos = gPos;
        } else if (deriv == TurkishMorphotactics.zero
            && gPos == PrimaryPos.Noun
            && !hasParticiple && !hasVnoun) {
          if (effectivePos == PrimaryPos.Numeral) {
            // Substantivized/inflected numerals (1990'da, ikisi) remain UPOS=NUM (XPOS=NNum)
            zeroNominalizedNum = true;
          } else if (effectivePos == PrimaryPos.Adjective
              && dialect == ConlluFormatter.Dialect.UD_IMST) {
            // UD_Turkish-IMST preserves UPOS=ADJ with XPOS=NAdj on zero-derived nominalized adjectives
            zeroNominalizedAdj = true;
          } else {
            effectivePos = PrimaryPos.Noun;
          }
        }
        builtSurface += grp.surfaceForm();
      }
    }

    for (MorphemeData md : mds) {
      String id = md.morpheme.id;
      if (PARTICIPLE_IDS.contains(id)) hasParticiple = true;
      else if (VNOUN_IDS.contains(id)) {
        hasVnoun = true;
        if ("Inf1".equals(id)) hasInf1 = true;
      } else if (CONVERB_IDS.contains(id)) hasConverb = true;
    }

    boolean isDerived = derivedFrom != null;
    String upos = determineUpos(
        item, effectivePos, hasParticiple, hasVnoun, hasConverb, surface, isDerived, dialect);
    String xpos = determineXpos(
        item, upos, hasParticiple, hasVnoun, hasConverb,
        zeroNominalizedAdj, zeroNominalizedNum, dialect);

    Map<String, String> feats = new LinkedHashMap<>();
    List<String> voices = new ArrayList<>(2);
    TreeSet<String> moods = new TreeSet<>();

    applyRootLexicalFeats(item, upos, isDerived, dialect, feats);
    extractInflectionalFeats(mds, false, dialect, feats, voices, moods);

    if (!voices.isEmpty()) {
      feats.put("Voice", String.join("", voices));
    }
    if (!moods.isEmpty()) {
      feats.put("Mood", String.join("", moods));
    } else if (dialect == ConlluFormatter.Dialect.UD_IMST && "VERB".equals(upos)) {
      feats.put("Mood", "Ind");
    }

    applyDefaultCategoryFeats(
        upos, hasParticiple, hasVnoun, hasInf1, hasConverb,
        zeroNominalizedAdj, zeroNominalizedNum, dialect, feats);
    return new ProjectedToken(currentLemma, upos, xpos, formatFeats(feats), derivedFrom);
  }

  private static ProjectedToken projectQuestionParticle(
      List<MorphemeData> mds, ConlluFormatter.Dialect dialect) {
    Map<String, String> qFeats = new LinkedHashMap<>();
    TreeSet<String> moods = new TreeSet<>();
    extractInflectionalFeats(mds, true, dialect, qFeats, new ArrayList<>(), moods);
    qFeats.putIfAbsent("Number", "Sing");
    qFeats.putIfAbsent("Person", "3");
    qFeats.putIfAbsent("Tense", "Pres");
    if ("Pres".equals(qFeats.get("Tense"))) {
      qFeats.putIfAbsent(
          "Aspect", dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN ? "Imp" : "Perf");
    }
    if (!moods.isEmpty()) {
      qFeats.put("Mood", String.join("", moods));
    } else if (dialect == ConlluFormatter.Dialect.UD_IMST) {
      qFeats.put("Mood", "Ind");
    }
    return new ProjectedToken("mi", "AUX", "Ques", formatFeats(qFeats), null);
  }

  private static ProjectedToken projectOvertAuxiliary(
      DictionaryItem item, List<MorphemeData> mds, ConlluFormatter.Dialect dialect) {
    boolean isDegil = "değil".equals(item.root);
    Map<String, String> auxFeats = new LinkedHashMap<>();
    TreeSet<String> moods = new TreeSet<>();
    extractInflectionalFeats(mds, false, dialect, auxFeats, new ArrayList<>(), moods);
    if (!moods.isEmpty()) {
      auxFeats.put("Mood", String.join("", moods));
    } else if (dialect == ConlluFormatter.Dialect.UD_IMST && !auxFeats.containsKey("VerbForm")) {
      auxFeats.put("Mood", "Ind");
    }
    if (!auxFeats.containsKey("VerbForm")) {
      auxFeats.putIfAbsent("Number", "Sing");
      auxFeats.putIfAbsent("Person", "3");
      auxFeats.putIfAbsent("Tense", "Pres");
      if (isDegil && dialect == ConlluFormatter.Dialect.UD_IMST && "Pres".equals(auxFeats.get("Tense"))) {
        auxFeats.putIfAbsent("Aspect", "Perf");
      }
    }
    auxFeats.put("Polarity", isDegil ? "Neg" : "Pos");
    String xpos = (isDegil && dialect == ConlluFormatter.Dialect.UD_IMST) ? "Neg" : "Overt";
    return new ProjectedToken(isDegil ? "değil" : "i", "AUX", xpos, formatFeats(auxFeats), null);
  }

  private static ProjectedToken projectExistential(
      DictionaryItem item, List<MorphemeData> mds, ConlluFormatter.Dialect dialect) {
    if (dialect == ConlluFormatter.Dialect.UD_IMST) {
      return new ProjectedToken(item.root, "ADJ", "Adj", "_", null);
    }
    Map<String, String> exFeats = new LinkedHashMap<>();
    extractInflectionalFeats(mds, false, dialect, exFeats, new ArrayList<>(), new TreeSet<>());
    exFeats.putIfAbsent("Number", "Sing");
    exFeats.putIfAbsent("Person", "3");
    exFeats.put("Polarity", "yok".equals(item.root) ? "Neg" : "Pos");
    return new ProjectedToken(item.root, "NOUN", "Exist", formatFeats(exFeats), null);
  }

  private static String computeBaseLemma(SingleAnalysis analysis, DictionaryItem item) {
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

  private static String unvoiceDerivedLemma(String prevSurface, String derivSurface, Morpheme deriv) {
    if (derivSurface.isEmpty()) {
      return prevSurface;
    }
    String combined = prevSurface + derivSurface;
    if ((deriv == TurkishMorphotactics.ness || deriv == TurkishMorphotactics.dim)
        && combined.endsWith("ğ")) {
      return combined.substring(0, combined.length() - 1) + "k";
    }
    return combined;
  }

  private static String determineUpos(
      DictionaryItem item,
      PrimaryPos effectivePos,
      boolean hasParticiple,
      boolean hasVnoun,
      boolean hasConverb,
      String surface,
      boolean isLexicallyDerived,
      ConlluFormatter.Dialect dialect) {
    if (hasParticiple || hasVnoun || hasConverb) {
      return "VERB";
    }
    if (!isLexicallyDerived
        && item.primaryPos == PrimaryPos.Noun
        && (item.secondaryPos == SecondaryPos.ProperNoun
            || (item.secondaryPos == SecondaryPos.Abbreviation
                && surface != null && !surface.isEmpty() && Character.isUpperCase(surface.charAt(0))))) {
      return "PROPN";
    }
    return switch (effectivePos) {
      case Noun -> "NOUN";
      case Verb -> "VERB";
      case Adjective -> "ADJ";
      case Adverb, Duplicator -> "ADV";
      case Pronoun -> "PRON";
      case Determiner -> "DET";
      case Numeral -> "NUM";
      case PostPositive -> "ADP";
      case Conjunction -> {
        String low = (surface != null ? surface : item.lemma).toLowerCase(Turkish.LOCALE);
        if (dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN) {
          if (FOCUS_PARTICLE_CONJUNCTIONS.contains(low)) {
            yield "PART";
          }
          if (SUBORDINATING_CONJUNCTIONS.contains(low)) {
            yield "SCONJ";
          }
        }
        yield "CCONJ";
      }
      case Interjection -> "INTJ";
      case Question -> "AUX";
      case Punctuation -> "PUNCT";
      default -> "X";
    };
  }

  private static String determineXpos(
      DictionaryItem item,
      String upos,
      boolean hasParticiple,
      boolean hasVnoun,
      boolean hasConverb,
      boolean zeroNominalizedAdj,
      boolean zeroNominalizedNum,
      ConlluFormatter.Dialect dialect) {
    if ("PART".equals(upos) && item.primaryPos == PrimaryPos.Conjunction) {
      return "Emph";
    }
    if ("NUM".equals(upos)) {
      if (zeroNominalizedNum) {
        return "NNum";
      }
      return dialect == ConlluFormatter.Dialect.UD_IMST ? "ANum" : "ANum";
    }
    if ("ADJ".equals(upos) && zeroNominalizedAdj) {
      return "NAdj";
    }
    if ("VERB".equals(upos)) {
      if (dialect == ConlluFormatter.Dialect.UD_IMST) {
        return "Verb";
      }
      if (hasParticiple) return "Ptcp";
      if (hasVnoun) return "Vnoun";
      if (hasConverb) return "Conv";
      return "_";
    }
    if (item.secondaryPos != null
        && item.secondaryPos != SecondaryPos.None
        && item.secondaryPos != SecondaryPos.UnknownSec) {
      if (item.primaryPos == PrimaryPos.PostPositive || item.primaryPos == PrimaryPos.Pronoun) {
        return "birbiri".equals(item.root) ? "Recip" : item.secondaryPos.shortForm;
      }
      if (item.secondaryPos == SecondaryPos.ProperNoun
          && dialect == ConlluFormatter.Dialect.UD_IMST) {
        return "Prop";
      }
    }
    if (dialect == ConlluFormatter.Dialect.UD_IMST) {
      return switch (upos) {
        case "NOUN" -> "Noun";
        case "ADJ" -> "Adj";
        case "ADV" -> "Adverb";
        case "DET" -> "Det";
        case "CCONJ", "SCONJ" -> "Conj";
        default -> "_";
      };
    }
    return "ADJ".equals(upos) ? "Adj" : "_";
  }

  private static void applyRootLexicalFeats(
      DictionaryItem item,
      String upos,
      boolean isLexicallyDerived,
      ConlluFormatter.Dialect dialect,
      Map<String, String> feats) {
    if (isLexicallyDerived) {
      return;
    }
    if (item.secondaryPos == SecondaryPos.Abbreviation
        || item.secondaryPos == SecondaryPos.RegularAbbreviation) {
      feats.put("Abbr", "Yes");
    }
    if (item.primaryPos == PrimaryPos.Duplicator) {
      feats.put("Echo", "Rdp");
    }
    switch (upos) {
      case "NUM" -> feats.put("NumType", switch (item.secondaryPos) {
        case Ordinal -> "Ord";
        case Distribution -> "Dist";
        default -> "Card";
      });
      case "DET" -> feats.put("PronType", switch (item.root) {
        case "bu", "şu", "o", "her", "tüm", "bütün" -> "Dem";
        case "hiçbir" -> "Neg";
        case "hangi", "kaç" -> "Int";
        default -> "Ind";
      });
      case "PRON" -> {
        if ("birbiri".equals(item.root)) {
          feats.put("PronType", "Rcp");
        } else if (NEGATIVE_PRONOUNS.contains(item.root)) {
          feats.put("PronType", "Neg");
        } else if (LOCATIVE_PRONOUNS.contains(item.root)) {
          feats.put("PronType", "Loc");
        } else {
          switch (item.secondaryPos) {
            case PersonalPron -> feats.put("PronType", "Prs");
            case DemonstrativePron -> feats.put("PronType", "Dem");
            case QuestionPron -> feats.put("PronType", "Int");
            case ReflexivePron -> {
              feats.put("PronType", "Prs");
              if (dialect == ConlluFormatter.Dialect.UD_IMST) {
                feats.put("Reflex", "Yes");
              }
            }
            case QuantitivePron -> feats.put("PronType", "Ind");
            default -> {}
          }
        }
      }
      default -> {}
    }
  }

  private static void extractInflectionalFeats(
      List<MorphemeData> mds,
      boolean isQuestionParticle,
      ConlluFormatter.Dialect dialect,
      Map<String, String> feats,
      List<String> voices,
      TreeSet<String> moods) {
    boolean seenPast = false;
    boolean seenNarr = false;
    String futAspect = dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN ? "Imp" : "Perf";

    for (MorphemeData md : mds) {
      String id = md.morpheme.id;
      if (LEXICAL_DERIVATION_IDS.contains(id) || "Rel".equals(id)) {
        feats.keySet().removeAll(
            Set.of("Case", "Number", "Person", "Number[psor]", "Person[psor]"));
        voices.clear();
        moods.clear();
        continue;
      }

      switch (id) {
        // Person-number agreement
        case "A1sg" -> { feats.put("Number", "Sing"); feats.put("Person", "1"); }
        case "A2sg" -> { feats.put("Number", "Sing"); feats.put("Person", "2"); }
        case "A3sg" -> { feats.put("Number", "Sing"); feats.put("Person", "3"); }
        case "A1pl" -> { feats.put("Number", "Plur"); feats.put("Person", "1"); }
        case "A2pl" -> { feats.put("Number", "Plur"); feats.put("Person", "2"); }
        case "A3pl" -> { feats.put("Number", "Plur"); feats.put("Person", "3"); }

        // Possessive
        case "P1sg" -> { feats.put("Number[psor]", "Sing"); feats.put("Person[psor]", "1"); }
        case "P2sg" -> { feats.put("Number[psor]", "Sing"); feats.put("Person[psor]", "2"); }
        case "P3sg" -> { feats.put("Number[psor]", "Sing"); feats.put("Person[psor]", "3"); }
        case "P1pl" -> { feats.put("Number[psor]", "Plur"); feats.put("Person[psor]", "1"); }
        case "P2pl" -> { feats.put("Number[psor]", "Plur"); feats.put("Person[psor]", "2"); }
        case "P3pl" -> { feats.put("Number[psor]", "Plur"); feats.put("Person[psor]", "3"); }

        // Case
        case "Nom", "Acc", "Dat", "Loc", "Abl", "Gen", "Ins", "Equ" -> feats.put("Case", id);

        // Voice (ordered: CauPass, CauCau)
        case "Caus" -> voices.add("Cau");
        case "Pass" -> voices.add("Pass");
        case "Reflex" -> voices.add("Rfl");
        case "Recip" -> voices.add("Rcp");

        // Polarity & Modality
        case "Neg" -> feats.put("Polarity", "Neg");
        case "Able" -> moods.add("Pot");
        case "Unable" -> { moods.add("Pot"); feats.put("Polarity", "Neg"); }

        // Postverbial lexical aspects
        case "Hastily" -> feats.put("Aspect", "Rapid");
        case "Stay", "EverSince", "Repeat" -> feats.put("Aspect", "Dur");
        case "Almost" -> feats.put("Aspect", "Prosp");

        // Finite TAM
        case "Past" -> {
          if (seenNarr) {
            feats.put("Tense", "Pqp");
            feats.putIfAbsent("Aspect", "Perf");
            feats.remove("Evident");
          } else {
            feats.putIfAbsent("Aspect", "Perf");
            if (dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN) {
              feats.put("Evident", "Fh");
            }
            feats.put("Tense", "Past");
          }
          seenPast = true;
        }
        case "Narr" -> {
          if (seenPast) {
            feats.put("Tense", "Pqp");
            feats.putIfAbsent("Aspect", "Perf");
          } else {
            if (dialect == ConlluFormatter.Dialect.UD_IMST) {
              feats.putIfAbsent("Aspect", "Perf");
            }
            feats.put("Evident", "Nfh");
            feats.put("Tense", "Past");
          }
          seenNarr = true;
        }
        case "Prog1", "Prog2" -> { feats.put("Aspect", "Prog"); feats.put("Tense", "Pres"); }
        case "Aor" -> { feats.put("Aspect", "Hab"); feats.put("Tense", "Pres"); }
        case "Fut" -> { feats.put("Aspect", futAspect); feats.put("Tense", "Fut"); }
        case "Pres" -> { if (!isQuestionParticle) feats.putIfAbsent("Tense", "Pres"); }
        case "Imp" -> {
          moods.add("Imp");
          if (dialect == ConlluFormatter.Dialect.UD_IMST) {
            feats.putIfAbsent("Aspect", "Perf");
            feats.putIfAbsent("Tense", "Pres");
          }
        }
        case "Opt" -> moods.add("Opt");
        case "Desr" -> moods.add("Des");
        case "Cond" -> moods.add("Cnd");
        case "Neces" -> { moods.add("Nec"); feats.putIfAbsent("Tense", "Pres"); }
        case "Cop" -> moods.add("Gen");

        // Non-finite Participles
        case "PresPart" -> {
          feats.put("VerbForm", "Part");
          if (dialect == ConlluFormatter.Dialect.UD_IMST) {
            feats.putIfAbsent("Aspect", "Perf");
          }
          feats.put("Tense", "Pres");
          clearAgreement(feats);
        }
        case "PastPart" -> {
          feats.put("VerbForm", "Part");
          feats.put("Aspect", "Perf");
          feats.put("Tense", "Past");
          clearAgreement(feats);
        }
        case "NarrPart" -> {
          feats.put("VerbForm", "Part");
          feats.put("Aspect", "Perf");
          feats.put("Evident", "Nfh");
          feats.put("Tense", "Past");
          clearAgreement(feats);
        }
        case "FutPart" -> {
          feats.put("VerbForm", "Part");
          feats.put("Aspect", futAspect);
          feats.put("Tense", "Fut");
          clearAgreement(feats);
        }
        case "AorPart" -> {
          feats.put("VerbForm", "Part");
          feats.put("Aspect", "Hab");
          feats.put("Tense", "Pres");
        }
        case "FeelLike" -> {
          feats.put("VerbForm", "Part");
          moods.add("Des");
        }

        // Verbal Nouns
        case "Inf1", "Inf2", "Inf3", "ActOf", "NotState" -> {
          feats.put("VerbForm", "Vnoun");
          if ("NotState".equals(id)) {
            feats.put("Polarity", "Neg");
          }
          if (dialect == ConlluFormatter.Dialect.UD_IMST) {
            feats.putIfAbsent("Aspect", "Perf");
            feats.putIfAbsent("Tense", "Pres");
          }
          clearAgreement(feats);
        }

        // Converbs
        case "ByDoingSo", "While", "When", "SinceDoingSo", "AsLongAs", "AfterDoingSo",
             "Adamantly", "AsIf", "WithoutHavingDoneSo", "WithoutBeingAbleToHaveDoneSo" -> {
          feats.put("VerbForm", "Conv");
          if (dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN) {
            switch (id) {
              case "ByDoingSo" -> feats.putIfAbsent("Aspect", "Imp");
              case "When" -> feats.putIfAbsent("Aspect", "Perf");
              default -> {}
            }
          } else {
            feats.putIfAbsent("Aspect", "Perf");
            feats.putIfAbsent("Tense", "Pres");
          }
          if ("WithoutHavingDoneSo".equals(id)) {
            feats.put("Polarity", "Neg");
          } else if ("WithoutBeingAbleToHaveDoneSo".equals(id)) {
            moods.add("Pot");
            feats.put("Polarity", "Neg");
          }
          // Finite stems taking '-ken' (e.g. okurken, otururlarken) retain subject Number/Person
          if (!"While".equals(id)) {
            clearAgreement(feats);
          }
        }
        default -> {}
      }
    }
  }

  private static void clearAgreement(Map<String, String> feats) {
    feats.remove("Number");
    feats.remove("Person");
  }

  private static void applyDefaultCategoryFeats(
      String upos,
      boolean hasParticiple,
      boolean hasVnoun,
      boolean hasInf1,
      boolean hasConverb,
      boolean zeroNominalizedAdj,
      boolean zeroNominalizedNum,
      ConlluFormatter.Dialect dialect,
      Map<String, String> feats) {
    if ("NOUN".equals(upos) || "PROPN".equals(upos) || "PRON".equals(upos)
        || zeroNominalizedAdj || zeroNominalizedNum) {
      feats.putIfAbsent("Case", "Nom");
      feats.putIfAbsent("Number", "Sing");
      feats.putIfAbsent("Person", "3");
      return;
    }
    if ("VERB".equals(upos)) {
      feats.putIfAbsent("Polarity", "Pos");
      if (hasVnoun) {
        feats.putIfAbsent("Case", "Nom");
        if (dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN) {
          if (hasInf1) {
            clearAgreement(feats);
          } else {
            feats.putIfAbsent("Number", "Sing");
            feats.putIfAbsent("Person", "3");
          }
        } else if (!"Plur".equals(feats.get("Number"))) {
          clearAgreement(feats);
        }
      } else if (hasParticiple) {
        boolean hasNominalCaseOrPlural =
            feats.containsKey("Case") || "Plur".equals(feats.get("Number"));
        if (hasNominalCaseOrPlural) {
          feats.putIfAbsent("Case", "Nom");
          if (dialect == ConlluFormatter.Dialect.UD_TURKIC_BOUN
              || "Plur".equals(feats.get("Number"))) {
            feats.putIfAbsent("Number", "Sing");
            feats.putIfAbsent("Person", "3");
          } else {
            clearAgreement(feats);
          }
        } else {
          clearAgreement(feats);
        }
      } else if (!hasConverb) {
        feats.putIfAbsent("Number", "Sing");
        feats.putIfAbsent("Person", "3");
      }
    }
  }

  static String formatFeats(Map<String, String> feats) {
    if (feats.isEmpty()) {
      return "_";
    }
    List<String> keys = new ArrayList<>(feats.keySet());
    keys.sort(CONLLU_FEAT_KEY_ORDER);
    StringBuilder sb = new StringBuilder(keys.size() * 12);
    for (int i = 0; i < keys.size(); i++) {
      if (i > 0) {
        sb.append('|');
      }
      String k = keys.get(i);
      sb.append(k).append('=').append(feats.get(k));
    }
    return sb.toString();
  }
}
