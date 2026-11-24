package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.has;
import static zemberek.morphology.morphotactics.Conditions.not;
import static zemberek.morphology.morphotactics.Conditions.notHave;
import static zemberek.morphology.morphotactics.Conditions.previousStateIs;
import static zemberek.morphology.morphotactics.Conditions.previousStateIsNot;
import static zemberek.morphology.morphotactics.TurkishMorphotactics.*;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.ContainsMorpheme;
import zemberek.morphology.morphotactics.Conditions.ContainsMorphemeSequence;
import zemberek.morphology.morphotactics.Conditions.HasTailSequence;
import zemberek.morphology.morphotactics.Conditions.NoSurfaceAfterDerivation;
import zemberek.morphology.morphotactics.Conditions.SecondaryPosIs;

/**
 * Morphotactic transition wiring for nouns (including compounds, proper nouns, abbreviations, and
 * last-vowel-drop forms), adjectives, and numerals.
 */
final class NominalMorphotactics {

  private NominalMorphotactics() {
  }

  static void connect(MorphotacticStates s, RootLexicon lexicon) {
    connectNounStates(s, lexicon);
    connectProperNounsAndAbbreviations(s);
    connectAdjectiveStates(s);
    connectNumeralStates(s);
    connectLastVowelDropWords(s);
  }

  /**
   * Turkish Nouns always have Noun-Person-Possession-Case morphemes even when there are no suffix
   * characters: {@code elma -> Noun:elma - A3sg:ε - Pnon:ε - Nom:ε}.
   */
  private static void connectNounStates(MorphotacticStates s, RootLexicon lexicon) {
    // ev-ε-?-?
    s.noun_S.addEmpty(s.a3sg_S, notHave(RootAttribute.ImplicitPlural));

    // ev-ler-?-?.
    s.noun_S.add(s.a3pl_S, "lAr",
        notHave(RootAttribute.ImplicitPlural)
            .and(notHave(RootAttribute.CompoundP3sg)));

    // Allow only implicit plural `hayvanat`.
    s.noun_S.addEmpty(s.a3pl_S, has(RootAttribute.ImplicitPlural));

    // --- Compound Handling ---------
    // for compound roots like "zeytinyağ-" generate two transitions
    // NounCompound--(ε)--> a3sgCompound --(ε)--> pNonCompound_S --> Nom_S
    s.nounCompoundRoot_S.addEmpty(s.a3sgCompound_S, has(RootAttribute.CompoundP3sgRoot));

    s.a3sgCompound_S.addEmpty(s.pnonCompound_S);
    s.a3sgCompound_S.add(s.p3pl_S, "lArI");

    // ---- For compound derivations -----------------
    s.pnonCompound_S.addEmpty(s.nom_S);
    s.nom_S.add(s.become_S, "lAş");
    s.nom_S.add(s.acquire_S, "lAn");
    // for "zeytinyağlı"
    s.nom_S.add(s.with_S, "lI", new ContainsMorpheme(with, without).not());
    // for "zeytinyağsız"
    s.nom_S.add(s.without_S, "sIz", new ContainsMorpheme(with, without).not());
    // for "zeytinyağlık"
    ContainsMorpheme containsNess = new ContainsMorpheme(ness);
    s.nom_S.add(s.ness_S, "lI~k", not(containsNess));
    s.nom_S.add(s.ness_S, "lI!ğ", not(containsNess));
    // for "zeytinyağcı"
    s.nom_S.add(s.agt_S, ">cI", not(new ContainsMorpheme(agt)));
    // for "zeytinyağsı"
    s.nom_S.add(s.justLike_S, "+msI", not(new ContainsMorpheme(justLike)));
    // for "zeytinyağcık"
    s.nom_S.add(s.dim_S, ">cI~k", Conditions.HAS_NO_SURFACE.andNot(new ContainsMorpheme(dim)));
    s.nom_S.add(s.dim_S, ">cI!ğ", Conditions.HAS_NO_SURFACE.andNot(new ContainsMorpheme(dim)));
    // "zeytinyağcağız"
    s.nom_S.add(s.dim_S, "cAğIz", Conditions.HAS_NO_SURFACE);

    // for compound roots like "zeytinyağ-lar-ı"
    s.nounCompoundRoot_S.add(s.a3plCompound_S, "lAr", has(RootAttribute.CompoundP3sgRoot));
    // for pnon connection, use lArI
    s.nounCompoundRoot_S.add(s.a3plCompound2_S, "lArI", has(RootAttribute.CompoundP3sgRoot));

    s.a3plCompound_S
        .add(s.p3sg_S, "I")
        .add(s.p2sg_S, "In")
        .add(s.p1sg_S, "Im")
        .add(s.p1pl_S, "ImIz")
        .add(s.p2pl_S, "InIz")
        .add(s.p3pl_S, "I");

    // plural analysis (A3pl+Pnon+Nom) of compound words.
    s.a3plCompound2_S.addEmpty(s.pnonCompound2_S);
    s.pnonCompound2_S.addEmpty(s.nom_ST);

    // do not allow 1st/2nd person possessive suffixes for abbreviations or words like "annemler",
    // while allowing 3rd person possessive (CEO'su, CEO'ları, GSYH'si, DNA'sı).
    Condition rootIsAbbrv = new SecondaryPosIs(SecondaryPos.Abbreviation);
    Condition p3PossessionCond = notHave(RootAttribute.FamilyMember);
    Condition possessionCond = p3PossessionCond.andNot(rootIsAbbrv);

    s.a3sg_S
        .addEmpty(s.pnon_S, notHave(RootAttribute.FamilyMember))
        .add(s.p1sg_S, "Im", possessionCond)
        .add(s.p2sg_S, "In", possessionCond
            .andNot(new Conditions.PreviousGroupContainsMorpheme(justLike)))
        .add(s.p3sg_S, "+sI", p3PossessionCond)
        .addEmpty(s.p3sg_S,
            has(RootAttribute.CompoundP3sg)) // "zeytinyağı" has two analyses: Pnon and P3sg
        .add(s.p1pl_S, "ImIz", possessionCond)
        .add(s.p2pl_S, "InIz", possessionCond
            .andNot(new Conditions.PreviousGroupContainsMorpheme(justLike)))
        .add(s.p3pl_S, "lArI", p3PossessionCond);

    // ev-ler-ε-?
    s.a3pl_S.addEmpty(s.pnon_S, notHave(RootAttribute.FamilyMember));

    // ev-ler-im-?
    s.a3pl_S
        .add(s.p1sg_S, "Im", possessionCond)
        .add(s.p2sg_S, "In", possessionCond)
        .addEmpty(s.p1sg_S, has(RootAttribute.ImplicitP1sg)) // for words like "annemler"
        .addEmpty(s.p2sg_S, has(RootAttribute.ImplicitP2sg)) // for words like "annenler"
        .add(s.p3sg_S, "I", p3PossessionCond)
        .add(s.p1pl_S, "ImIz", possessionCond)
        .add(s.p2pl_S, "InIz", possessionCond)
        .add(s.p3pl_S, "I", p3PossessionCond);

    // --- handle su - akarsu roots. ----
    s.nounSuRoot_S.addEmpty(s.a3sgSu_S);
    s.nounSuRoot_S.add(s.a3pl_S, "lar");
    s.a3sgSu_S
        .addEmpty(s.pnon_S)
        .add(s.p1sg_S, "yum")
        .add(s.p2sg_S, "yun")
        .add(s.p3sg_S, "yu")
        .add(s.p1pl_S, "yumuz")
        .add(s.p2pl_S, "yunuz")
        .add(s.p3pl_S, "lArI");

    // ev-?-ε-ε (ev, evler).
    s.pnon_S.addEmpty(s.nom_ST, notHave(RootAttribute.FamilyMember));

    Condition equCond = Conditions.prviousMorphemeIs(a3pl)
        .or(new ContainsMorpheme(adj, futPart, presPart, narrPart, pastPart).not())
        .or(new ContainsMorphemeSequence(able, verb, pastPart)); // allow `yapabildiğince`

    // Not allow "zeytinyağı-ya" etc.
    s.pnon_S
        .add(s.dat_ST, "+yA", notHave(RootAttribute.CompoundP3sg))
        .add(s.abl_ST, ">dAn", notHave(RootAttribute.CompoundP3sg))
        .add(s.loc_ST, ">dA", notHave(RootAttribute.CompoundP3sg))
        .add(s.acc_ST, "+yI", notHave(RootAttribute.CompoundP3sg))
        .add(s.gen_ST, "+nIn", previousStateIsNot(s.a3sgSu_S))
        .add(s.gen_ST, "yIn", previousStateIs(s.a3sgSu_S))
        .add(s.equ_ST, ">cA", notHave(RootAttribute.CompoundP3sg).and(equCond))
        .add(s.ins_ST, "+ylA");

    s.pnon_S
        .add(s.dat_ST, "+nA", has(RootAttribute.CompoundP3sg))
        .add(s.abl_ST, "+ndAn", has(RootAttribute.CompoundP3sg))
        .add(s.loc_ST, "+ndA", has(RootAttribute.CompoundP3sg))
        .add(s.equ_ST, "+ncA", has(RootAttribute.CompoundP3sg).and(equCond))
        .add(s.acc_ST, "+nI", has(RootAttribute.CompoundP3sg));

    // Words like "içeri" or "dışarı" implicitly contain Dative suffix.
    s.pnon_S.addEmpty(s.dat_ST, has(RootAttribute.ImplicitDative));

    Condition equWithPastPart = equCond.or(new ContainsMorpheme(pastPart));
    addNonThirdPossessiveCases(s, s.p1sg_S, equWithPastPart);
    addNonThirdPossessiveCases(s, s.p2sg_S, equWithPastPart);
    addNonThirdPossessiveCases(s, s.p1pl_S, equWithPastPart);
    addNonThirdPossessiveCases(s, s.p2pl_S, equWithPastPart);

    s.p3sg_S
        .addEmpty(s.nom_ST)
        .add(s.dat_ST, "nA")
        .add(s.loc_ST, "ndA")
        .add(s.abl_ST, "ndAn")
        .add(s.ins_ST, "ylA")
        .add(s.gen_ST, "nIn")
        .add(s.equ_ST, "ncA", equWithPastPart)
        .add(s.acc_ST, "nI");

    s.p3pl_S
        .addEmpty(s.nom_ST)
        .add(s.dat_ST, "nA")
        .add(s.loc_ST, "ndA")
        .add(s.abl_ST, "ndAn")
        .add(s.ins_ST, "ylA")
        .add(s.gen_ST, "nIn")
        // Omit equCond check because adj+..+A3pl+..+Equ fails.
        .add(s.equ_ST, "+ncA")
        .add(s.acc_ST, "nI");

    // ev-ε-ε-ε-cik (evcik) / ev-ε-ε-ε-ceğiz (evceğiz)
    s.nom_ST.add(s.dim_S, ">cI~k", Conditions.HAS_NO_SURFACE.andNot(rootIsAbbrv));
    s.nom_ST.add(s.dim_S, ">cI!ğ", Conditions.HAS_NO_SURFACE.andNot(rootIsAbbrv));
    s.nom_ST.add(s.dim_S, "cAğIz", Conditions.HAS_NO_SURFACE.andNot(rootIsAbbrv));
    s.dim_S.addEmpty(s.noun_S);

    Condition emptyAdjNounSeq = new ContainsMorphemeSequence(adj, zero, noun, a3sg, pnon, nom);
    s.nom_ST.add(s.ness_S, "lI~k",
        Conditions.CURRENT_GROUP_EMPTY.andNot(containsNess).andNot(emptyAdjNounSeq));
    s.nom_ST.add(s.ness_S, "lI!ğ",
        Conditions.CURRENT_GROUP_EMPTY.andNot(containsNess).andNot(emptyAdjNounSeq));
    s.ness_S.addEmpty(s.noun_S);

    s.nom_ST.add(s.agt_S, ">cI",
        Conditions.CURRENT_GROUP_EMPTY.andNot(new ContainsMorpheme(adj, agt)));
    s.agt_S.addEmpty(s.noun_S);

    // Do not allow adj->zero->noun->ε-ε-ε->zero->Verb because adj->zero->Verb already exists.
    Condition noun2VerbZeroDerivationCondition = Conditions.HAS_TAIL
        .andNot(Conditions.CURRENT_GROUP_EMPTY.and(new Conditions.LastDerivationIs(s.adjZeroDeriv_S)));

    s.nom_ST.addEmpty(s.nounZeroDeriv_S, noun2VerbZeroDerivationCondition);
    s.dat_ST.addEmpty(s.nounZeroDeriv_S, noun2VerbZeroDerivationCondition);
    s.abl_ST.addEmpty(s.nounZeroDeriv_S, noun2VerbZeroDerivationCondition);
    s.loc_ST.addEmpty(s.nounZeroDeriv_S, noun2VerbZeroDerivationCondition);
    s.ins_ST.addEmpty(s.nounZeroDeriv_S, noun2VerbZeroDerivationCondition);
    s.gen_ST.addEmpty(s.nounZeroDeriv_S, noun2VerbZeroDerivationCondition);
    s.nounZeroDeriv_S.addEmpty(s.nVerb_S);

    // meyve-li / meyve-siz / ...
    Condition noSurfaceAfterDerivation = new NoSurfaceAfterDerivation();
    s.nom_ST.add(s.with_S, "lI",
        noSurfaceAfterDerivation.andNot(new ContainsMorpheme(with, without)));
    s.nom_ST.add(s.without_S, "sIz",
        noSurfaceAfterDerivation.andNot(new ContainsMorpheme(with, without, inf1)));

    s.nom_ST.add(s.justLike_S, "+msI",
        noSurfaceAfterDerivation
            .andNot(new ContainsMorpheme(justLike, futPart, pastPart, presPart, adj))
            .andNot(rootIsAbbrv));
    s.nom_ST.add(s.justLike_S, "ImsI",
        notHave(PhoneticAttribute.LastLetterVowel)
            .and(noSurfaceAfterDerivation)
            .andNot(new ContainsMorpheme(justLike, futPart, pastPart, presPart, adj))
            .andNot(rootIsAbbrv));

    s.nom_ST.add(s.related_S, "sAl",
        noSurfaceAfterDerivation
            .andNot(new ContainsMorpheme(with, without, related))
            .andNot(rootIsAbbrv));

    s.with_S.addEmpty(s.adjectiveRoot_ST);
    s.without_S.addEmpty(s.adjectiveRoot_ST);
    s.related_S.addEmpty(s.adjectiveRoot_ST);
    s.justLike_S.addEmpty(s.adjectiveRoot_ST);

    // meyve-de-ki
    Condition notRelRepetition = new HasTailSequence(rel, adj, zero, noun, a3sg, pnon, loc).not();
    s.loc_ST.add(s.rel_S, "ki", notRelRepetition);
    s.rel_S.addEmpty(s.adjectiveRoot_ST);

    // dünkü, anki, yarınki, ileriki, etc.
    Condition time = Conditions.CURRENT_GROUP_EMPTY.and(new SecondaryPosIs(SecondaryPos.Time));
    DictionaryItem dun = lexicon.getItemById("dün_Noun_Time");
    DictionaryItem gun = lexicon.getItemById("gün_Noun_Time");
    DictionaryItem bugun = lexicon.getItemById("bugün_Noun_Time");
    DictionaryItem ileri = lexicon.getItemById("ileri_Noun");
    DictionaryItem geri = lexicon.getItemById("geri_Noun");
    DictionaryItem ote = lexicon.getItemById("öte_Noun");
    DictionaryItem beri = lexicon.getItemById("beri_Noun");

    Condition time2 = Conditions.rootIsAny(dun, gun, bugun);
    s.nom_ST.add(s.rel_S, "ki", time.andNot(time2));
    s.nom_ST.add(s.rel_S, "ki", Conditions.rootIsAny(ileri, geri, ote, beri));
    s.nom_ST.add(s.rel_S, "kü", time2.and(time));

    // After Genitive suffix, Rel suffix makes a Pronoun derivation.
    s.gen_ST.add(s.relToPron_S, "ki");
    s.relToPron_S.addEmpty(s.pronAfterRel_S);

    ContainsMorpheme verbDeriv = new ContainsMorpheme(inf1, inf2, inf3, pastPart, futPart);
    Condition becomeAcquireCond = noSurfaceAfterDerivation
        .andNot(new ContainsMorpheme(adj))
        .andNot(verbDeriv)
        .andNot(rootIsAbbrv);

    s.nom_ST.add(s.become_S, "lAş", becomeAcquireCond);
    s.become_S.addEmpty(s.verbRoot_S);

    s.nom_ST.add(s.acquire_S, "lAn", becomeAcquireCond);
    s.acquire_S.addEmpty(s.verbRoot_S);

    // Inf1 mak makes noun derivation without possessive, plural, Dat, Gen, Acc.
    s.nounInf1Root_S.addEmpty(s.a3sgInf1_S);
    s.a3sgInf1_S.addEmpty(s.pnonInf1_S);
    s.pnonInf1_S.addEmpty(s.nom_ST);
    s.pnonInf1_S.add(s.abl_ST, "tAn");
    s.pnonInf1_S.add(s.loc_ST, "tA");
    s.pnonInf1_S.add(s.ins_ST, "lA");

    s.nounActOfRoot_S.addEmpty(s.a3sgActOf_S);
    s.nounActOfRoot_S.add(s.a3plActOf_S, "lar");
    s.a3sgActOf_S.addEmpty(s.pnonActOf);
    s.a3plActOf_S.addEmpty(s.pnonActOf);
    s.pnonActOf.addEmpty(s.nom_ST);
  }

  private static void addNonThirdPossessiveCases(
      MorphotacticStates s, MorphemeState possessiveState, Condition equCond) {
    possessiveState
        .addEmpty(s.nom_ST)
        .add(s.dat_ST, "A")
        .add(s.loc_ST, "dA")
        .add(s.abl_ST, "dAn")
        .add(s.ins_ST, "lA")
        .add(s.gen_ST, "In")
        .add(s.equ_ST, "cA", equCond)
        .add(s.acc_ST, "I");
  }

  private static void connectLastVowelDropWords(MorphotacticStates s) {
    s.nounLastVowelDropRoot_S.addEmpty(s.a3sgLastVowelDrop_S);
    s.nounLastVowelDropRoot_S.add(s.a3PlLastVowelDrop_S, "lAr");
    s.a3sgLastVowelDrop_S.addEmpty(s.pNonLastVowelDrop_S);
    s.a3PlLastVowelDrop_S.addEmpty(s.pNonLastVowelDrop_S);
    s.pNonLastVowelDrop_S.add(s.loc_ST, ">dA");
    s.pNonLastVowelDrop_S.add(s.abl_ST, ">dAn");

    s.adjLastVowelDropRoot_S.addEmpty(s.zeroLastVowelDrop_S);
    s.postpLastVowelDropRoot_S.addEmpty(s.zeroLastVowelDrop_S);
    s.zeroLastVowelDrop_S.addEmpty(s.nounLastVowelDropRoot_S);
  }

  private static void connectProperNounsAndAbbreviations(MorphotacticStates s) {
    // ---- Proper noun handling -------
    s.nounProper_S.addEmpty(s.a3sg_S);
    s.nounProper_S.add(s.a3pl_S, "lAr");
    s.puncProperSeparator_S.addEmpty(s.a3sg_S);
    s.puncProperSeparator_S.add(s.a3pl_S, "lAr");

    // ---- Abbreviation Handling -------
    s.nounAbbrv_S.addEmpty(s.a3sg_S);
    s.nounAbbrv_S.add(s.a3pl_S, "lAr");

    // ---- Words that cannot have a suffix ----
    s.nounNoSuffix_S.addEmpty(s.nounA3sgNoSuffix_S);
    s.nounA3sgNoSuffix_S.addEmpty(s.nounPnonNoSuffix_S);
    s.nounPnonNoSuffix_S.addEmpty(s.nounNomNoSuffix_ST);
  }

  private static void connectAdjectiveStates(MorphotacticStates s) {
    // Zero derivation: "yeşil-i" requires Adj -> Zero -> Noun.
    // HAS_TAIL prevents Adj->Zero->Noun+A3sg+Pnon+Nom.
    s.adjectiveRoot_ST.addEmpty(s.adjZeroDeriv_S, Conditions.HAS_TAIL);
    s.adjZeroDeriv_S.addEmpty(s.noun_S);
    s.adjZeroDeriv_S.addEmpty(s.nVerb_S);

    s.adjectiveRoot_ST.add(s.aLy_S, ">cA");
    s.aLy_S.addEmpty(s.advRoot_ST);

    Condition notRepeatedAdjDeriv = new ContainsMorpheme(asIf, ly, agt, with, justLike).not();
    s.adjectiveRoot_ST.add(s.aAsIf_S, ">cA", notRepeatedAdjDeriv);
    s.aAsIf_S.addEmpty(s.adjectiveRoot_ST);

    s.adjectiveRoot_ST.add(s.aAgt_S, ">cI", notRepeatedAdjDeriv);
    s.aAgt_S.addEmpty(s.noun_S);

    s.adjectiveRoot_ST.add(s.justLike_S, "+msI",
        new NoSurfaceAfterDerivation().and(new ContainsMorpheme(justLike).not()));
    s.adjectiveRoot_ST.add(s.justLike_S, "ImsI",
        notHave(PhoneticAttribute.LastLetterVowel)
            .and(new NoSurfaceAfterDerivation())
            .and(new ContainsMorpheme(justLike).not()));

    s.adjectiveRoot_ST.add(s.become_S, "lAş", new NoSurfaceAfterDerivation());
    s.adjectiveRoot_ST.add(s.acquire_S, "lAn", new NoSurfaceAfterDerivation());

    Condition c1 = new Conditions.PreviousMorphemeIsAny(futPart, pastPart);
    s.adjAfterVerb_S.addEmpty(s.aPnon_ST, c1);
    s.adjAfterVerb_S.add(s.aP1sg_ST, "Im", c1);
    s.adjAfterVerb_S.add(s.aP2sg_ST, "In", c1);
    s.adjAfterVerb_S.add(s.aP3sg_ST, "I", c1);
    s.adjAfterVerb_S.add(s.aP1pl_ST, "ImIz", c1);
    s.adjAfterVerb_S.add(s.aP2pl_ST, "InIz", c1);
    s.adjAfterVerb_S.add(s.aP3pl_ST, "lArI", c1);

    s.adjectiveRoot_ST.add(s.ness_S, "lI~k");
    s.adjectiveRoot_ST.add(s.ness_S, "lI!ğ");

    s.adjAfterVerb_ST.add(s.ness_S, "lI~k", new Conditions.PreviousMorphemeIs(aorPart));
    s.adjAfterVerb_ST.add(s.ness_S, "lI!ğ", new Conditions.PreviousMorphemeIs(aorPart));

    // Allow adverbial ablative after negative aorist participle: "bilmezden", "görmezden".
    s.adjAfterVerb_ST.addEmpty(s.aorPartNegZero_S, Conditions.previousStateIs(s.vAorPartNeg_S));
    s.aorPartNegZero_S.addEmpty(s.aorPartNegNoun_S);
    s.aorPartNegNoun_S.addEmpty(s.aorPartNegA3sg_S);
    s.aorPartNegA3sg_S.addEmpty(s.aorPartNegPnon_S);
    s.aorPartNegPnon_S.add(s.aorPartNegAbl_ST, "dAn");
  }

  private static void connectNumeralStates(MorphotacticStates s) {
    s.numeralRoot_ST.add(s.ness_S, "lI~k");
    s.numeralRoot_ST.add(s.ness_S, "lI!ğ");
    s.numeralRoot_ST.addEmpty(s.numZeroDeriv_S, Conditions.HAS_TAIL);
    s.numZeroDeriv_S.addEmpty(s.noun_S);
    s.numZeroDeriv_S.addEmpty(s.nVerb_S);

    s.numeralRoot_ST.add(s.justLike_S, "+msI",
        new NoSurfaceAfterDerivation().and(new ContainsMorpheme(justLike).not()));
    s.numeralRoot_ST.add(s.justLike_S, "ImsI",
        notHave(PhoneticAttribute.LastLetterVowel)
            .and(new NoSurfaceAfterDerivation())
            .and(new ContainsMorpheme(justLike).not()));
  }
}
