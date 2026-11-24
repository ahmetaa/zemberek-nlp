package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.has;
import static zemberek.morphology.morphotactics.Conditions.lastDerivationIs;
import static zemberek.morphology.morphotactics.Conditions.notHave;
import static zemberek.morphology.morphotactics.TurkishMorphotactics.*;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.morphotactics.Conditions.ContainsMorpheme;
import zemberek.morphology.morphotactics.Conditions.PreviousMorphemeIsAny;
import zemberek.morphology.morphotactics.Conditions.PreviousStateIsAny;
import zemberek.morphology.morphotactics.Conditions.RootSurfaceIs;
import zemberek.morphology.morphotactics.Conditions.RootSurfaceIsAny;

/**
 * Morphotactic transition wiring for Turkish verbs: voice/causative/passive, negation/ability,
 * tenses/person/copula, participles/infinitives, compound verbs, adverbials, and {@code de/ye}.
 */
final class VerbMorphotactics {

  private VerbMorphotactics() {
  }

  static void connect(MorphotacticStates s) {
    Condition cMultiVerb = new PreviousMorphemeIsAny(
        everSince, repeat, almost, hastily, stay, start).not();

    connectImperativeAndVoice(s);
    connectTensesAndPerson(s);
    connectNegativeAndUnable(s);
    connectParticiplesAndInfinitives(s);
    connectDeYeVerb(s, cMultiVerb);
    connectCompoundVerbsAndAdverbials(s, cMultiVerb);
  }

  static void connectPostProcessing(MorphotacticStates s) {
    // Passive has an exception for some verbs like `kavurmak` or `savurmak`.
    // add passive state connection to modified root `kavr` etc.
    s.verbLastVowelDropModRoot_S.add(s.vPass_S, "Il");
    // for not allowing `kavur-ul`, copy all verb connections to unmodified `kavur` root and
    // remove only the passive.
    s.verbLastVowelDropUnmodRoot_S.copyOutgoingTransitionsFrom(s.verbRoot_S);
    s.verbLastVowelDropUnmodRoot_S.removeTransitionsTo(pass);
  }

  private static void connectImperativeAndVoice(MorphotacticStates s) {
    // Imperative.
    s.verbRoot_S.addEmpty(s.vImp_S);
    s.vImp_S
        .addEmpty(s.vA2sg_ST)
        .add(s.vA2sg_ST, "sAnA")
        .add(s.vA3sg_ST, "sIn")
        .add(s.vA2pl_ST, "+yIn")
        .add(s.vA2pl_ST, "+yInIz")
        .add(s.vA2pl_ST, "sAnIzA")
        .add(s.vA3pl_ST, "sInlAr");

    // Causative suffixes ("t" and ">dIr").
    s.verbRoot_S.add(s.vCausT_S, "t", has(RootAttribute.Causative_t)
        .or(new Conditions.LastDerivationIs(s.vCausTır_S))
        .andNot(new Conditions.LastDerivationIsAny(s.vCausT_S, s.vPass_S, s.vAble_S)));

    s.verbRoot_S.add(s.vCausTır_S, ">dIr",
        has(PhoneticAttribute.LastLetterConsonant)
            .andNot(new Conditions.LastDerivationIsAny(s.vCausTır_S, s.vPass_S, s.vAble_S)));

    s.vCausT_S.addEmpty(s.verbRoot_S);
    s.vCausTır_S.addEmpty(s.verbRoot_S);

    // Reciprocal & Reflexive
    s.vRecip_S.addEmpty(s.verbRoot_S);
    s.vImplicitRecipRoot_S.addEmpty(s.vRecip_S);

    s.vImplicitReflexRoot_S.addEmpty(s.vReflex_S);
    s.vReflex_S.addEmpty(s.verbRoot_S);

    // Passive ("In", "InIl", "+nIl")
    s.verbRoot_S.add(s.vPass_S, "In", has(RootAttribute.Passive_In)
        .andNot(new ContainsMorpheme(pass)));
    s.verbRoot_S.add(s.vPass_S, "InIl", has(RootAttribute.Passive_In)
        .andNot(new ContainsMorpheme(pass)));
    s.verbRoot_S.add(s.vPass_S, "+nIl",
        new PreviousStateIsAny(s.vCausT_S, s.vCausTır_S)
            .or(notHave(RootAttribute.Passive_In).andNot(new ContainsMorpheme(pass))));
    s.vPass_S.addEmpty(s.verbRoot_S);

    // Positive Ability.
    s.verbRoot_S.add(s.vAble_S, "+yAbil", lastDerivationIs(s.vAble_S).not());
    s.vAble_S.addEmpty(s.verbRoot_S);

    // Ability before negative: only negative connections are possible from vAbleNegDerivRoot_S.
    s.vAbleNeg_S.addEmpty(s.vAbleNegDerivRoot_S);
    s.vAbleNegDerivRoot_S.add(s.vNeg_S, "mA");
    s.vAbleNegDerivRoot_S.add(s.vNegProg1_S, "m");
  }

  private static void connectTensesAndPerson(MorphotacticStates s) {
    // Progressive1 "-Iyor"
    s.verbRoot_S.add(s.vProgYor_S, "Iyor", notHave(PhoneticAttribute.LastLetterVowel));
    s.verbRoot_VowelDrop_S.add(s.vProgYor_S, "Iyor");
    s.vProgYor_S
        .add(s.vA1sg_ST, "um")
        .add(s.vA2sg_ST, "sun")
        .addEmpty(s.vA3sg_ST)
        .add(s.vA1pl_ST, "uz")
        .add(s.vA2pl_ST, "sunuz")
        .add(s.vA3pl_ST, "lar")
        .add(s.vCond_S, "sa")
        .add(s.vPastAfterTense_S, "du")
        .add(s.vNarrAfterTense_S, "muş")
        .add(s.vCopBeforeA3pl_S, "dur")
        .add(s.vWhile_S, "ken");

    // Progressive2 "-mAktA"
    s.verbRoot_S.add(s.vProgMakta_S, "mAktA");
    addVowelStemPersonAgreement(s, s.vProgMakta_S);
    s.vProgMakta_S
        .add(s.vCond_S, "ysA")
        .add(s.vPastAfterTense_S, "ydI")
        .add(s.vNarrAfterTense_S, "ymIş")
        .add(s.vCopBeforeA3pl_S, "dIr")
        .add(s.vWhile_S, "yken");

    // Positive Aorist Tense.
    s.verbRoot_S.add(s.vAor_S, "Ir",
        has(RootAttribute.Aorist_I).or(Conditions.HAS_SURFACE));
    s.verbRoot_S.add(s.vAor_S, "Ar",
        has(RootAttribute.Aorist_A).and(Conditions.HAS_NO_SURFACE));
    addStandardPersonAgreement(s, s.vAor_S);
    s.vAor_S
        .add(s.vPastAfterTense_S, "dI")
        .add(s.vNarrAfterTense_S, "mIş")
        .add(s.vCond_S, "sA")
        .add(s.vCopBeforeA3pl_S, "dIr")
        .add(s.vWhile_S, "ken");

    // Condition "oku-r-sa"
    addPastTypePersonAgreement(s, s.vCond_S);

    // Past "oku-du"
    s.verbRoot_S.add(s.vPast_S, ">dI");
    addPastTypePersonAgreement(s, s.vPast_S);
    s.vPast_S.add(s.vCond_S, "ysA");

    // Narrative "oku-muş"
    s.verbRoot_S.add(s.vNarr_S, "mIş");
    addStandardPersonAgreement(s, s.vNarr_S);
    s.vNarr_S
        .add(s.vCond_S, "sA")
        .add(s.vPastAfterTense_S, "tI")
        .add(s.vCopBeforeA3pl_S, "tIr")
        .add(s.vWhile_S, "ken")
        .add(s.vNarrAfterTense_S, "mIş");

    // Past after tense "oku-muş-tu"
    addPastTypePersonAgreement(s, s.vPastAfterTense_S);

    // Narrative after tense "oku-r-muş"
    addStandardPersonAgreement(s, s.vNarrAfterTense_S);
    s.vNarrAfterTense_S
        .add(s.vWhile_S, "ken")
        .add(s.vCopBeforeA3pl_S, "tIr");

    // Future "oku-yacak"
    s.verbRoot_S.add(s.vFut_S, "+yAcA~k");
    s.verbRoot_S.add(s.vFut_S, "+yAcA!ğ");
    addStandardPersonAgreement(s, s.vFut_S);
    s.vFut_S
        .add(s.vCond_S, "sA")
        .add(s.vPastAfterTense_S, "tI")
        .add(s.vNarrAfterTense_S, "mIş")
        .add(s.vCopBeforeA3pl_S, "tIr")
        .add(s.vWhile_S, "ken");

    // Optative (gel-e, gel-eyim, gel-me-ye-yim)
    s.verbRoot_S.add(s.vOpt_S, "+yA");
    s.vOpt_S
        .add(s.vA1sg_ST, "yIm")
        .add(s.vA2sg_ST, "sIn")
        .addEmpty(s.vA3sg_ST)
        .add(s.vA1pl_ST, "lIm")
        .add(s.vA2pl_ST, "sInIz")
        .add(s.vA3pl_ST, "lAr")
        .add(s.vPastAfterTense_S, "ydI")
        .add(s.vNarrAfterTense_S, "ymIş");

    // Desire (gel-se, gel-se-m, gel-me-se-m)
    s.verbRoot_S.add(s.vDesr_S, "sA");
    addPastTypePersonAgreement(s, s.vDesr_S);
    s.vDesr_S
        .add(s.vPastAfterTense_S, "ydI")
        .add(s.vNarrAfterTense_S, "ymIş");

    // Necessity (-mAlI)
    s.verbRoot_S.add(s.vNeces_S, "mAlI");
    addVowelStemPersonAgreement(s, s.vNeces_S);
    s.vNeces_S
        .add(s.vPastAfterTense_S, "ydI")
        .add(s.vCond_S, "ysA")
        .add(s.vNarrAfterTense_S, "ymIş")
        .add(s.vCopBeforeA3pl_S, "dIr")
        .add(s.vWhile_S, "yken");

    // A3pl exception: "yapar-lar-dı" vs "yapar-dı-lar" (preventing "yapar-dı-lar-dı").
    Condition previousNotPastNarrCond =
        new PreviousStateIsAny(s.vPastAfterTense_S, s.vNarrAfterTense_S, s.vCond_S).not();
    s.vA3pl_ST.add(s.vPastAfterTense_ST, "dI", previousNotPastNarrCond);
    s.vA3pl_ST.add(s.vNarrAfterTense_ST, "mIş", previousNotPastNarrCond);
    s.vA3pl_ST.add(s.vCond_ST, "sA", previousNotPastNarrCond);

    Condition a3plCopWhile = new PreviousMorphemeIsAny(prog1, prog2, neces, fut, narr, aor);
    s.vA3pl_ST.add(s.vCop_ST, "dIr", a3plCopWhile);
    s.vA3pl_ST.add(s.vWhile_S, "ken", a3plCopWhile);

    Condition personCop = new PreviousMorphemeIsAny(prog1, prog2, neces, fut, narr, aor);
    s.vA1sg_ST.add(s.vCop_ST, "dIr", personCop);
    s.vA2sg_ST.add(s.vCop_ST, "dIr", personCop);
    s.vA3sg_ST.add(s.vCop_ST, ">dIr", personCop);
    s.vA1pl_ST.add(s.vCop_ST, "dIr", personCop);
    s.vA2pl_ST.add(s.vCop_ST, "dIr", personCop);

    s.vCopBeforeA3pl_S.add(s.vA3pl_ST, "lAr");

    // Allow Past+Person+Cond (geldinse, geldinizse)
    Condition previousPast = new Conditions.PreviousMorphemeIs(past)
        .andNot(new ContainsMorpheme(cond, desr));
    s.vA2pl_ST.add(s.vCondAfterPerson_ST, "sA", previousPast);
    s.vA2sg_ST.add(s.vCondAfterPerson_ST, "sA", previousPast);
    s.vA1sg_ST.add(s.vCondAfterPerson_ST, "sA", previousPast);
    s.vA1pl_ST.add(s.vCondAfterPerson_ST, "sA", previousPast);

    s.vA3sg_ST.add(s.vAsIf_S, ">cAsInA", new PreviousMorphemeIsAny(aor, narr));
  }

  private static void connectNegativeAndUnable(MorphotacticStates s) {
    // Negative
    s.verbRoot_S.add(s.vNeg_S, "mA", Conditions.previousMorphemeIsNot(able));

    s.vNeg_S.addEmpty(s.vImp_S)
        .add(s.vPast_S, "dI")
        .add(s.vFut_S, "yAcA~k")
        .add(s.vFut_S, "yAcA!ğ")
        .add(s.vNarr_S, "mIş")
        .add(s.vProgMakta_S, "mAktA")
        .add(s.vOpt_S, "yA")
        .add(s.vDesr_S, "sA")
        .add(s.vNeces_S, "mAlI")
        .add(s.vInf1_S, "mAk")
        .add(s.vInf2_S, "mA")
        .add(s.vInf3_S, "yIş")
        .add(s.vActOf_S, "mAcA")
        .add(s.vPastPart_S, "dI~k")
        .add(s.vPastPart_S, "dI!ğ")
        .add(s.vFutPart_S, "yAcA~k")
        .add(s.vFutPart_S, "yAcA!ğ")
        .add(s.vPresPart_S, "yAn")
        .add(s.vNarrPart_S, "mIş")
        .add(s.vSinceDoingSo_S, "yAlI")
        .add(s.vByDoingSo_S, "yArAk")
        .add(s.vHastily_S, "yIver")
        .add(s.vEverSince_S, "yAgör")
        .add(s.vAfterDoing_S, "yIp")
        .add(s.vWhen_S, "yIncA")
        .add(s.vAsLongAs_S, "dIkçA")
        .add(s.vNotState_S, "mAzlI~k")
        .add(s.vNotState_S, "mAzlI!ğ")
        .add(s.vFeelLike_S, "yAsI");

    // Negative form is "m" before progressive "Iyor" because last vowel drops.
    s.verbRoot_S.add(s.vNegProg1_S, "m");
    s.vNegProg1_S.add(s.vProgYor_S, "Iyor");

    // Negative Aorist ("z" or empty).
    s.vNeg_S.add(s.vAorNeg_S, "z");
    s.vNeg_S.addEmpty(s.vAorNegEmpty_S);
    s.vAorNeg_S
        .add(s.vA2sg_ST, "sIn")
        .addEmpty(s.vA3sg_ST)
        .add(s.vA2pl_ST, "sInIz")
        .add(s.vA3pl_ST, "lAr")
        .add(s.vPastAfterTense_S, "dI")
        .add(s.vNarrAfterTense_S, "mIş")
        .add(s.vCond_S, "sA")
        .add(s.vCopBeforeA3pl_S, "dIr")
        .add(s.vWhile_S, "ken");
    s.vAorNegEmpty_S
        .add(s.vA1sg_ST, "m")
        .add(s.vA1pl_ST, "yIz");

    s.vNeg_S.add(s.vAorPartNeg_S, "z");
    s.vAorPartNeg_S.addEmpty(s.adjAfterVerb_ST);

    // Ability derivation after negative.
    s.vNeg_S.add(s.vAble_S, "yAbil");

    // Unable: copy all outgoing transitions from vNeg_S (must happen after all vNeg_S edges).
    s.verbRoot_S.add(s.vUnable_S, "+yAmA", Conditions.previousMorphemeIsNot(able));
    s.vUnable_S.copyOutgoingTransitionsFrom(s.vNeg_S);
    s.verbRoot_S.add(s.vUnableProg1_S, "+yAm");
    s.vUnableProg1_S.add(s.vProgYor_S, "Iyor");
  }

  private static void connectParticiplesAndInfinitives(MorphotacticStates s) {
    // Infinitive 1 "mAk"
    s.verbRoot_S.add(s.vInf1_S, "mA~k");
    s.vInf1_S.addEmpty(s.nounInf1Root_S);

    // Infinitive 2 "mA"
    s.verbRoot_S.add(s.vInf2_S, "mA");
    s.vInf2_S.addEmpty(s.noun_S);

    // Infinitive 3 "+yIş"
    s.verbRoot_S.add(s.vInf3_S, "+yIş");
    s.vInf3_S.addEmpty(s.noun_S);

    // Agt "+yIcI"
    s.verbRoot_S.add(s.vAgt_S, "+yIcI");
    s.vAgt_S.addEmpty(s.noun_S);
    s.vAgt_S.addEmpty(s.adjAfterVerb_ST);

    // ActOf "mAcA"
    s.verbRoot_S.add(s.vActOf_S, "mAcA");
    s.vActOf_S.addEmpty(s.nounActOfRoot_S);

    // PastPart "oku-duğ-um"
    s.verbRoot_S.add(s.vPastPart_S, ">dI~k");
    s.verbRoot_S.add(s.vPastPart_S, ">dI!ğ");
    s.vPastPart_S.addEmpty(s.noun_S);
    s.vPastPart_S.addEmpty(s.adjAfterVerb_S);

    // FutPart "oku-yacağ-ım kitap"
    s.verbRoot_S.add(s.vFutPart_S, "+yAcA~k");
    s.verbRoot_S.add(s.vFutPart_S, "+yAcA!ğ");
    s.vFutPart_S.addEmpty(s.noun_S, Conditions.HAS_TAIL);
    s.vFutPart_S.addEmpty(s.adjAfterVerb_S);

    // NarrPart
    s.verbRoot_S.add(s.vNarrPart_S, "mIş");
    s.vNarrPart_S.addEmpty(s.adjectiveRoot_ST);

    // AorPart "okunabilir-lik"
    s.verbRoot_S.add(s.vAorPart_S, "Ir",
        has(RootAttribute.Aorist_I).or(Conditions.HAS_SURFACE));
    s.verbRoot_S.add(s.vAorPart_S, "Ar",
        has(RootAttribute.Aorist_A).and(Conditions.HAS_NO_SURFACE));
    s.vAorPart_S.addEmpty(s.adjAfterVerb_ST);

    // PresPart
    s.verbRoot_S.add(s.vPresPart_S, "+yAn");
    s.vPresPart_S.addEmpty(s.noun_S, Conditions.HAS_TAIL);
    s.vPresPart_S.addEmpty(s.adjAfterVerb_ST);

    // FeelLike
    s.verbRoot_S.add(s.vFeelLike_S, "+yAsI");
    s.vFeelLike_S.addEmpty(s.noun_S, Conditions.HAS_TAIL);
    s.vFeelLike_S.addEmpty(s.adjAfterVerb_ST);

    // NotState
    s.verbRoot_S.add(s.vNotState_S, "mAzlI~k");
    s.verbRoot_S.add(s.vNotState_S, "mAzlI!ğ");
    s.vNotState_S.addEmpty(s.noun_S);
  }

  private static void connectDeYeVerb(MorphotacticStates s, Condition cMultiVerb) {
    // `demek` and `yemek` special stem alternation rules (de/di, ye/yi).
    RootSurfaceIsAny diYiCondition = new RootSurfaceIsAny("di", "yi");
    RootSurfaceIsAny deYeCondition = new RootSurfaceIsAny("de", "ye");

    s.vDeYeRoot_S
        .add(s.vFut_S, "yece~k", diYiCondition)
        .add(s.vFut_S, "yece!ğ", diYiCondition)
        .add(s.vProgYor_S, "yor", diYiCondition)
        .add(s.vAble_S, "yebil", diYiCondition)
        .add(s.vAbleNeg_S, "ye", diYiCondition)
        .add(s.vInf3_S, "yiş", new RootSurfaceIsAny("yi"))
        .add(s.vFutPart_S, "yece~k", diYiCondition)
        .add(s.vFutPart_S, "yece!ğ", diYiCondition)
        .add(s.vPresPart_S, "yen", diYiCondition)
        .add(s.vEverSince_S, "yegel", diYiCondition.and(cMultiVerb))
        .add(s.vRepeat_S, "yedur", diYiCondition.and(cMultiVerb))
        .add(s.vRepeat_S, "yegör", diYiCondition.and(cMultiVerb))
        .add(s.vAlmost_S, "yeyaz", diYiCondition.and(cMultiVerb))
        .add(s.vStart_S, "yekoy", diYiCondition.and(cMultiVerb))
        .add(s.vSinceDoingSo_S, "yeli", diYiCondition)
        .add(s.vByDoingSo_S, "yerek", diYiCondition)
        .add(s.vFeelLike_S, "yesi", diYiCondition)
        .add(s.vAfterDoing_S, "yip", new RootSurfaceIsAny("de"))
        .add(s.vWithoutBeingAbleToHaveDoneSo_S, "yemeden", diYiCondition)
        .add(s.vOpt_S, "ye", diYiCondition);

    s.vDeYeRoot_S
        .add(s.vCausTır_S, "dir", deYeCondition)
        .add(s.vPass_S, "n", deYeCondition)
        .add(s.vPass_S, "nil", deYeCondition)
        .add(s.vPast_S, "di", deYeCondition)
        .add(s.vNarr_S, "miş", deYeCondition)
        .add(s.vAor_S, "r", deYeCondition)
        .add(s.vNeg_S, "me", deYeCondition)
        .add(s.vNegProg1_S, "m", deYeCondition)
        .add(s.vProgMakta_S, "mekte", deYeCondition)
        .add(s.vDesr_S, "se", deYeCondition)
        .add(s.vInf1_S, "mek", deYeCondition)
        .add(s.vInf2_S, "me", deYeCondition)
        .add(s.vInf3_S, "yiş", new RootSurfaceIsAny("de"))
        .add(s.vPastPart_S, "di~k", deYeCondition)
        .add(s.vPastPart_S, "di!ğ", deYeCondition)
        .add(s.vNarrPart_S, "miş", deYeCondition)
        .add(s.vHastily_S, "yiver", diYiCondition.and(cMultiVerb))
        .add(s.vAsLongAs_S, "dikçe", deYeCondition)
        .add(s.vWithoutHavingDoneSo_S, "meden", deYeCondition)
        .add(s.vWithoutHavingDoneSo_S, "meksizin", deYeCondition)
        .add(s.vNeces_S, "meli", deYeCondition)
        .add(s.vNotState_S, "mezli~k", deYeCondition)
        .add(s.vNotState_S, "mezli!ğ", deYeCondition)
        .add(s.vAfterDoing_S, "yip", new RootSurfaceIsAny("yi"))
        .addEmpty(s.vImp_S, new RootSurfaceIs("de"))
        .addEmpty(s.vImpYemekYe_S, new RootSurfaceIs("ye"))
        .addEmpty(s.vImpYemekYi_S, new RootSurfaceIs("yi"));

    // verb `yemek` imperative exceptions
    s.vImpYemekYi_S
        .add(s.vA2pl_ST, "yin")
        .add(s.vA2pl_ST, "yiniz");
    s.vImpYemekYe_S
        .addEmpty(s.vA2sg_ST)
        .add(s.vA2sg_ST, "sene")
        .add(s.vA3sg_ST, "sin")
        .add(s.vA2pl_ST, "senize")
        .add(s.vA3pl_ST, "sinler");
  }

  private static void connectCompoundVerbsAndAdverbials(
      MorphotacticStates s, Condition cMultiVerb) {
    s.verbRoot_S.add(s.vEverSince_S, "+yAgel", cMultiVerb);
    s.verbRoot_S.add(s.vRepeat_S, "+yAdur", cMultiVerb);
    s.verbRoot_S.add(s.vRepeat_S, "+yAgör", cMultiVerb);
    s.verbRoot_S.add(s.vAlmost_S, "+yAyaz", cMultiVerb);
    s.verbRoot_S.add(s.vHastily_S, "+yIver", cMultiVerb);
    s.verbRoot_S.add(s.vStay_S, "+yAkal", cMultiVerb);
    s.verbRoot_S.add(s.vStart_S, "+yAkoy", cMultiVerb);

    s.vEverSince_S.addEmpty(s.verbRoot_S);
    s.vRepeat_S.addEmpty(s.verbRoot_S);
    s.vAlmost_S.addEmpty(s.verbRoot_S);
    s.vHastily_S.addEmpty(s.verbRoot_S);
    s.vStay_S.addEmpty(s.verbRoot_S);
    s.vStart_S.addEmpty(s.verbRoot_S);

    s.verbRoot_S.add(s.vWhen_S, "+yIncA");
    s.verbRoot_S.add(s.vSinceDoingSo_S, "+yAlI");
    s.verbRoot_S.add(s.vByDoingSo_S, "+yArAk");
    s.verbRoot_S.add(s.vAdamantly_S, "+yAsIyA");
    s.verbRoot_S.add(s.vAfterDoing_S, "+yIp");
    s.verbRoot_S.add(s.vWithoutBeingAbleToHaveDoneSo_S, "+yAmAdAn");
    s.verbRoot_S.add(s.vAsLongAs_S, ">dIkçA");
    s.verbRoot_S.add(s.vWithoutHavingDoneSo_S, "mAdAn");
    s.verbRoot_S.add(s.vWithoutHavingDoneSo_S, "mAksIzIn");

    s.vAsIf_S.addEmpty(s.advRoot_ST);
    s.vSinceDoingSo_S.addEmpty(s.advRoot_ST);
    s.vByDoingSo_S.addEmpty(s.advRoot_ST);
    s.vAdamantly_S.addEmpty(s.advRoot_ST);
    s.vAfterDoing_S.addEmpty(s.advRoot_ST);
    s.vWithoutBeingAbleToHaveDoneSo_S.addEmpty(s.advRoot_ST);
    s.vAsLongAs_S.addEmpty(s.advRoot_ST);
    s.vWithoutHavingDoneSo_S.addEmpty(s.advRoot_ST);
    s.vWhile_S.addEmpty(s.advRoot_ST);
    s.vWhen_S.addEmpty(s.advNounRoot_ST);
  }

  private static void addPastTypePersonAgreement(MorphotacticStates s, MorphemeState state) {
    state
        .add(s.vA1sg_ST, "m")
        .add(s.vA2sg_ST, "n")
        .addEmpty(s.vA3sg_ST)
        .add(s.vA1pl_ST, "k")
        .add(s.vA2pl_ST, "nIz")
        .add(s.vA3pl_ST, "lAr");
  }

  private static void addStandardPersonAgreement(MorphotacticStates s, MorphemeState state) {
    state
        .add(s.vA1sg_ST, "Im")
        .add(s.vA2sg_ST, "sIn")
        .addEmpty(s.vA3sg_ST)
        .add(s.vA1pl_ST, "Iz")
        .add(s.vA2pl_ST, "sInIz")
        .add(s.vA3pl_ST, "lAr");
  }

  private static void addVowelStemPersonAgreement(MorphotacticStates s, MorphemeState state) {
    state
        .add(s.vA1sg_ST, "yIm")
        .add(s.vA2sg_ST, "sIn")
        .addEmpty(s.vA3sg_ST)
        .add(s.vA1pl_ST, "yIz")
        .add(s.vA2pl_ST, "sInIz")
        .add(s.vA3pl_ST, "lAr");
  }
}
