package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.notHave;
import static zemberek.morphology.morphotactics.Conditions.rootIs;
import static zemberek.morphology.morphotactics.Conditions.rootIsAny;
import static zemberek.morphology.morphotactics.Conditions.rootIsNone;
import static zemberek.morphology.morphotactics.Conditions.rootIsNot;
import static zemberek.morphology.morphotactics.TurkishMorphotactics.*;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.CurrentGroupContainsAny;
import zemberek.morphology.morphotactics.Conditions.HasTailSequence;

/**
 * Morphotactic transition wiring for pronouns (personal, demonstrative, quantitive, question,
 * reflexive, and post-relativizer pronouns) and pronoun-derived predicates ({@code pvVerbRoot_S}).
 */
final class PronounMorphotactics {

  private PronounMorphotactics() {
  }

  static void connect(MorphotacticStates s, RootLexicon lexicon) {
    connectPronounStates(s, lexicon);
    connectVerbAfterPronoun(s);
  }

  private static void connectPronounStates(MorphotacticStates s, RootLexicon lexicon) {
    // ----------- Personal Pronouns ----------------------------
    DictionaryItem ben = lexicon.getItemById("ben_Pron_Pers");
    DictionaryItem sen = lexicon.getItemById("sen_Pron_Pers");
    DictionaryItem o = lexicon.getItemById("o_Pron_Pers");
    DictionaryItem biz = lexicon.getItemById("biz_Pron_Pers");
    DictionaryItem siz = lexicon.getItemById("siz_Pron_Pers");
    DictionaryItem falan = lexicon.getItemById("falan_Pron_Pers");
    DictionaryItem falanca = lexicon.getItemById("falanca_Pron_Pers");

    s.pronPers_S.addEmpty(s.pA1sg_S, rootIs(ben));
    s.pronPers_S.addEmpty(s.pA2sg_S, rootIs(sen));
    s.pronPers_S.addEmpty(s.pA3sg_S, rootIsAny(o, falan, falanca));
    s.pronPers_S.add(s.pA3pl_S, "nlAr", rootIs(o));
    s.pronPers_S.add(s.pA3pl_S, "lAr", rootIsAny(falan, falanca));
    s.pronPers_S.addEmpty(s.pA1pl_S, rootIs(biz));
    s.pronPers_S.add(s.pA1pl_S, "lAr", rootIs(biz));
    s.pronPers_S.addEmpty(s.pA2pl_S, rootIs(siz));
    s.pronPers_S.add(s.pA2pl_S, "lAr", rootIs(siz));

    // modified `ben-sen` special state and transitions (ban-a, san-a)
    s.pronPers_Mod_S.addEmpty(s.pA1sgMod_S, rootIs(ben));
    s.pronPers_Mod_S.addEmpty(s.pA2sgMod_S, rootIs(sen));
    s.pA1sgMod_S.addEmpty(s.pPnonMod_S);
    s.pA2sgMod_S.addEmpty(s.pPnonMod_S);
    s.pPnonMod_S.add(s.pDat_ST, "A");

    s.pA1sg_S.addEmpty(s.pPnon_S);
    s.pA2sg_S.addEmpty(s.pPnon_S);
    s.pA3sg_S.addEmpty(s.pPnon_S);
    s.pA1pl_S.addEmpty(s.pPnon_S);
    s.pA2pl_S.addEmpty(s.pPnon_S);
    s.pA3pl_S.addEmpty(s.pPnon_S);

    // ------------ Noun -> Rel -> Pron (masanınki) ---------------------------
    s.pronAfterRel_S.addEmpty(s.pA3sgRel_S);
    s.pronAfterRel_S.add(s.pA3plRel_S, "lAr");
    s.pA3sgRel_S.addEmpty(s.pPnonRel_S);
    s.pA3plRel_S.addEmpty(s.pPnonRel_S);
    s.pPnonRel_S
        .addEmpty(s.pNom_ST)
        .add(s.pDat_ST, "+nA")
        .add(s.pAcc_ST, "+nI")
        .add(s.pAbl_ST, "+ndAn")
        .add(s.pLoc_ST, "+ndA")
        .add(s.pIns_ST, "+ylA")
        .add(s.pGen_ST, "+nIn");

    // ------------ Demonstrative Pronouns ------------------------
    DictionaryItem bu = lexicon.getItemById("bu_Pron_Demons");
    DictionaryItem su = lexicon.getItemById("şu_Pron_Demons");
    DictionaryItem o_demons = lexicon.getItemById("o_Pron_Demons");

    s.pronDemons_S.addEmpty(s.pA3sg_S);
    s.pronDemons_S.add(s.pA3pl_S, "nlAr");

    // ------------ Quantitive Pronouns ----------------------------
    DictionaryItem birbiri = lexicon.getItemById("birbiri_Pron_Quant");
    DictionaryItem biri = lexicon.getItemById("biri_Pron_Quant");
    DictionaryItem bazi = lexicon.getItemById("bazı_Pron_Quant");
    DictionaryItem bircogu = lexicon.getItemById("birçoğu_Pron_Quant");
    DictionaryItem birkaci = lexicon.getItemById("birkaçı_Pron_Quant");
    DictionaryItem beriki = lexicon.getItemById("beriki_Pron_Quant");
    DictionaryItem cogu = lexicon.getItemById("çoğu_Pron_Quant");
    DictionaryItem cumlesi = lexicon.getItemById("cümlesi_Pron_Quant");
    DictionaryItem hep = lexicon.getItemById("hep_Pron_Quant");
    DictionaryItem herbiri = lexicon.getItemById("herbiri_Pron_Quant");
    DictionaryItem herkes = lexicon.getItemById("herkes_Pron_Quant");
    DictionaryItem hicbiri = lexicon.getItemById("hiçbiri_Pron_Quant");
    DictionaryItem hepsi = lexicon.getItemById("hepsi_Pron_Quant");
    DictionaryItem kimi = lexicon.getItemById("kimi_Pron_Quant");
    DictionaryItem kimse = lexicon.getItemById("kimse_Pron_Quant");
    DictionaryItem oburku = lexicon.getItemById("öbürkü_Pron_Quant");
    DictionaryItem oburu = lexicon.getItemById("öbürü_Pron_Quant");
    DictionaryItem tumu = lexicon.getItemById("tümü_Pron_Quant");
    DictionaryItem topu = lexicon.getItemById("topu_Pron_Quant");
    DictionaryItem umum = lexicon.getItemById("umum_Pron_Quant");

    // herkes and hep cannot be singular.
    s.pronQuant_S.addEmpty(s.pQuantA3sg_S,
        rootIsNone(herkes, umum, hepsi, cumlesi, hep, tumu, birkaci, topu));

    s.pronQuant_S.add(s.pQuantA3pl_S, "lAr",
        rootIsNone(hep, hepsi, birkaci, umum, cumlesi, cogu, bircogu, herbiri, tumu, hicbiri, topu,
            oburu));

    // bazılarınız -> A1pl+P1pl / A2pl+P2pl
    s.pronQuant_S.add(s.pQuantA1pl_S, "lAr", rootIsAny(bazi));
    s.pronQuant_S.add(s.pQuantA2pl_S, "lAr", rootIsAny(bazi));

    // Herkes is implicitly plural.
    s.pronQuant_S.addEmpty(s.pQuantA3pl_S,
        rootIsAny(herkes, umum, birkaci, hepsi, cumlesi, cogu, bircogu, tumu, topu));

    // connect "kimse" to Noun-A3sg and Noun-A3pl. It behaves like a noun.
    s.pronQuant_S.addEmpty(s.a3sg_S, rootIs(kimse));
    s.pronQuant_S.add(s.a3pl_S, "lAr", rootIsAny(kimse));

    // for `birbiri-miz`, `hep-imiz`, `birbiri-niz`, `hep-iniz`
    Condition quantPlPers = rootIsAny(
        biri, bazi, birbiri, birkaci, herbiri, hep, kimi, cogu, bircogu, tumu, topu, hicbiri);
    s.pronQuant_S.addEmpty(s.pQuantA1pl_S, quantPlPers);
    s.pronQuant_S.addEmpty(s.pQuantA2pl_S, quantPlPers);

    // birbir-ler-i, çok-lar-ı, birçok-lar-ı
    s.pronQuantModified_S.addEmpty(s.pQuantModA3pl_S);
    s.pQuantModA3pl_S.add(s.pP3pl_S, "lArI");

    // both `biri-ne` and `birisi-ne` or `birbirine` and `birbirisine` are accepted.
    s.pQuantA3sg_S.addEmpty(s.pP3sg_S,
        rootIsAny(biri, birbiri, kimi, herbiri, hicbiri, oburu, oburku, beriki)
            .and(notHave(PhoneticAttribute.ModifiedPronoun)));
    s.pQuantA3sg_S.add(s.pP3sg_S, "sI",
        rootIsAny(biri, bazi, kimi, birbiri, herbiri, hicbiri, oburku)
            .and(notHave(PhoneticAttribute.ModifiedPronoun)));

    // no connection from pQuantA3pl to Pnon for preventing `biriler` (except herkes etc.)
    s.pQuantA3pl_S.add(s.pP3pl_S, "I", rootIsAny(biri, bazi, birbiri, kimi, oburku, beriki));
    s.pQuantA3pl_S.addEmpty(s.pP3pl_S, rootIsAny(hepsi, birkaci, cumlesi, cogu, tumu, topu, bircogu));
    s.pQuantA3pl_S.addEmpty(s.pPnon_S, rootIsAny(herkes, umum, oburku, beriki));

    s.pQuantA1pl_S.add(s.pP1pl_S, "ImIz");
    s.pQuantA2pl_S.add(s.pP2pl_S, "InIz");

    // ------------ Question Pronouns ----------------------------
    DictionaryItem ne = lexicon.getItemById("ne_Pron_Ques");
    DictionaryItem nere = lexicon.getItemById("nere_Pron_Ques");
    DictionaryItem kim = lexicon.getItemById("kim_Pron_Ques");

    s.pronQues_S.addEmpty(s.pQuesA3sg_S);
    s.pronQues_S.add(s.pQuesA3pl_S, "lAr");

    s.pQuesA3sg_S.addEmpty(s.pPnon_S)
        .add(s.pP3sg_S, "+sI")
        .add(s.pP1sg_S, "Im", rootIsNot(ne))
        .add(s.pP1sg_S, "yIm", rootIs(ne))
        .add(s.pP2sg_S, "In", rootIsNot(ne))
        .add(s.pP2sg_S, "yIn", rootIs(ne))
        .add(s.pP1pl_S, "ImIz", rootIsNot(ne))
        .add(s.pP1pl_S, "yImIz", rootIs(ne))
        .add(s.pP2pl_S, "InIz", rootIsNot(ne))
        .add(s.pP2pl_S, "yInIz", rootIs(ne));

    s.pQuesA3pl_S.addEmpty(s.pPnon_S)
        .add(s.pP3sg_S, "I")
        .add(s.pP1sg_S, "Im")
        .add(s.pP2sg_S, "In")
        .add(s.pP1pl_S, "ImIz")
        .add(s.pP2pl_S, "InIz");

    // ------------ Reflexive Pronouns (`kendi`) ----------------------------
    DictionaryItem kendi = lexicon.getItemById("kendi_Pron_Reflex");
    s.pronReflex_S.addEmpty(s.pReflexA1sg_S)
        .addEmpty(s.pReflexA2sg_S)
        .addEmpty(s.pReflexA3sg_S)
        .addEmpty(s.pReflexA1pl_S)
        .addEmpty(s.pReflexA2pl_S)
        .addEmpty(s.pReflexA3pl_S);

    s.pReflexA1sg_S.add(s.pP1sg_S, "Im");
    s.pReflexA2sg_S.add(s.pP2sg_S, "In");
    s.pReflexA3sg_S.add(s.pP3sg_S, "+sI").addEmpty(s.pP3sg_S);
    s.pReflexA1pl_S.add(s.pP1pl_S, "ImIz");
    s.pReflexA2pl_S.add(s.pP2pl_S, "InIz");
    s.pReflexA3pl_S.add(s.pP3pl_S, "lArI");

    // ------------ Case Connections for All Pronouns ----------------------------
    Condition nGroup = rootIsNone(ne, nere, falan, falanca, hep, herkes);
    Condition yGroup = rootIsAny(ne, nere, falan, falanca, hep, herkes);

    s.pPnon_S.addEmpty(s.pNom_ST)
        // not allowing `ben-e` and `sen-e`. `ban-a` and `san-a` use pronPers_Mod_S
        .add(s.pDat_ST, "+nA", rootIsNone(ben, sen, ne, nere, falan, falanca, herkes))
        .add(s.pDat_ST, "+yA", yGroup)
        .add(s.pAcc_ST, "+nI", nGroup)
        .add(s.pAcc_ST, "+yI", yGroup)
        .add(s.pLoc_ST, "+ndA", nGroup)
        .add(s.pLoc_ST, ">dA", yGroup)
        .add(s.pAbl_ST, "+ndAn", nGroup)
        .add(s.pAbl_ST, ">dAn", yGroup)
        .add(s.pGen_ST, "+nIn", nGroup.and(rootIsNone(biz, ben, sen)))
        .add(s.pGen_ST, "im", rootIsAny(ben, biz))
        .add(s.pGen_ST, "in", rootIs(sen))
        .add(s.pGen_ST, "+yIn", yGroup.and(rootIsNone(biz)))
        .add(s.pEqu_ST, ">cA", yGroup)
        .add(s.pEqu_ST, ">cA", nGroup)
        .add(s.pIns_ST, "+ylA", yGroup)
        .add(s.pIns_ST, "+nlA", nGroup)
        .add(s.pIns_ST, "+nInlA", nGroup.and(rootIsAny(bu, su, o, sen)))
        .add(s.pIns_ST, "inle", rootIs(siz))
        .add(s.pIns_ST, "imle", rootIsAny(biz, ben));

    Condition conditionpP1sg_S = rootIsAny(kim, ben, ne, nere, kendi);
    s.pP1sg_S
        .addEmpty(s.pNom_ST)
        .add(s.pDat_ST, "+nA", nGroup)
        .add(s.pAcc_ST, "+nI", nGroup)
        .add(s.pDat_ST, "+yA", yGroup)
        .add(s.pAcc_ST, "+yI", yGroup)
        .add(s.pLoc_ST, "+ndA", rootIsAny(kendi))
        .add(s.pAbl_ST, "+ndAn", rootIsAny(kendi))
        .add(s.pEqu_ST, "+ncA", rootIsAny(kendi))
        .add(s.pIns_ST, "+nlA", conditionpP1sg_S)
        .add(s.pGen_ST, "+nIn", conditionpP1sg_S);

    Condition conditionP2sg = rootIsAny(kim, sen, ne, nere, kendi);
    s.pP2sg_S
        .addEmpty(s.pNom_ST)
        .add(s.pDat_ST, "+nA", nGroup)
        .add(s.pAcc_ST, "+nI", nGroup)
        .add(s.pDat_ST, "+yA", yGroup)
        .add(s.pAcc_ST, "+yI", yGroup)
        .add(s.pLoc_ST, "+ndA", rootIsAny(kendi))
        .add(s.pAbl_ST, "+ndAn", rootIsAny(kendi))
        .add(s.pEqu_ST, "+ncA", rootIsAny(kendi))
        .add(s.pIns_ST, "+nlA", conditionP2sg)
        .add(s.pGen_ST, "+nIn", conditionP2sg);

    Condition p3sgCond = rootIsAny(
        kendi, kim, ne, nere, o, bazi, biri, birbiri, herbiri, hep, kimi, hicbiri);
    s.pP3sg_S
        .addEmpty(s.pNom_ST)
        .add(s.pDat_ST, "+nA", nGroup)
        .add(s.pAcc_ST, "+nI", nGroup)
        .add(s.pDat_ST, "+yA", yGroup)
        .add(s.pAcc_ST, "+yI", yGroup)
        .add(s.pLoc_ST, "+ndA", p3sgCond)
        .add(s.pAbl_ST, "+ndAn", p3sgCond)
        .add(s.pGen_ST, "+nIn", p3sgCond)
        .add(s.pEqu_ST, "ncA", p3sgCond)
        .add(s.pIns_ST, "+ylA", p3sgCond);

    Condition hepCnd = rootIsAny(
        kendi, kim, ne, nere, biz, siz, biri, birbiri, birkaci, herbiri, hep, kimi, cogu, bircogu,
        tumu, topu, bazi, hicbiri);
    addPronounPlPossessiveCases(s, s.pP1pl_S, nGroup, yGroup, hepCnd);
    addPronounPlPossessiveCases(s, s.pP2pl_S, nGroup, yGroup, hepCnd);

    Condition hepsiCnd = rootIsAny(
        kendi, kim, ne, nere, o, bazi, biri, herkes, umum, birkaci, hepsi, cumlesi, cogu,
        bircogu, birbiri, tumu, kimi, topu);
    s.pP3pl_S
        .addEmpty(s.pNom_ST)
        .add(s.pDat_ST, "+nA", nGroup)
        .add(s.pAcc_ST, "+nI", nGroup)
        .add(s.pDat_ST, "+yA", yGroup)
        .add(s.pAcc_ST, "+yI", yGroup)
        .add(s.pLoc_ST, "+ndA", hepsiCnd)
        .add(s.pAbl_ST, "+ndAn", hepsiCnd)
        .add(s.pGen_ST, "+nIn", hepsiCnd.or(rootIsAny(sen, siz)))
        .add(s.pEqu_ST, "+ncA", hepsiCnd)
        .add(s.pIns_ST, "+ylA", hepsiCnd);

    s.pNom_ST.add(s.with_S, "+nlI", rootIsAny(bu, su, o_demons, ben, sen, o, biz, siz));
    s.pNom_ST.add(s.with_S, "lI", rootIsAny(nere));
    s.pNom_ST.add(s.with_S, "+ylI", rootIsAny(ne));
    s.pNom_ST.add(s.without_S, "+nsIz",
        rootIsAny(nere, bu, su, o_demons, ben, sen, o, biz, siz));
    s.pNom_ST.add(s.without_S, "+ysIz", rootIsAny(ne));
    s.pGen_ST.add(s.relToPron_S, "ki",
        rootIsAny(nere, bu, su, o_demons, ne, ben, sen, o, biz, siz, kim, kendi));

    Condition notRelRepetition = new HasTailSequence(rel, adj, zero, noun, a3sg, pnon, loc).not();
    s.pLoc_ST.add(s.rel_S, "ki", notRelRepetition);

    s.pIns_ST.add(s.vWhile_S, "+yken");

    // ------------- Derivation connections ---------
    s.pNom_ST.addEmpty(s.pronZeroDeriv_S, Conditions.HAS_TAIL);
    s.pDat_ST.addEmpty(s.pronZeroDeriv_S, Conditions.HAS_TAIL);
    s.pLoc_ST.addEmpty(s.pronZeroDeriv_S, Conditions.HAS_TAIL);
    s.pAbl_ST.addEmpty(s.pronZeroDeriv_S, Conditions.HAS_TAIL);
    s.pGen_ST.addEmpty(s.pronZeroDeriv_S, Conditions.HAS_TAIL);
    s.pIns_ST.addEmpty(s.pronZeroDeriv_S, Conditions.HAS_TAIL);
    s.pronZeroDeriv_S.addEmpty(s.pvVerbRoot_S);
  }

  private static void addPronounPlPossessiveCases(
      MorphotacticStates s,
      MorphemeState possessiveState,
      Condition nGroup,
      Condition yGroup,
      Condition hepCnd) {
    possessiveState
        .addEmpty(s.pNom_ST)
        .add(s.pDat_ST, "+nA", nGroup)
        .add(s.pAcc_ST, "+nI", nGroup)
        .add(s.pDat_ST, "+yA", yGroup)
        .add(s.pAcc_ST, "+yI", yGroup)
        .add(s.pLoc_ST, "+ndA", hepCnd)
        .add(s.pAbl_ST, "+ndAn", hepCnd)
        .add(s.pGen_ST, "+nIn", hepCnd)
        .add(s.pEqu_ST, "+ncA", hepCnd)
        .add(s.pIns_ST, "+nlA", hepCnd);
  }

  private static void connectVerbAfterPronoun(MorphotacticStates s) {
    s.pvVerbRoot_S.addEmpty(s.pvPresent_S);
    s.pvVerbRoot_S.add(s.vWhile_S, "+yken");
    s.pvVerbRoot_S.add(s.pvPast_S, "+ydI");
    s.pvVerbRoot_S.add(s.pvNarr_S, "+ymIş");
    s.pvVerbRoot_S.add(s.pvCond_S, "+ysA");

    // disallow `benin, bizim with A1sg analysis etc.`
    Condition allowA1sgTrans = new Conditions.PreviousGroupContains(s.pA1pl_S, s.pP1sg_S).not();
    Condition allowA1plTrans =
        new Conditions.PreviousGroupContains(s.pA1sg_S, s.pA2sg_S, s.pP1sg_S, s.pP2sg_S).not();
    Condition allowA2sgTrans = new Conditions.PreviousGroupContains(s.pA2pl_S, s.pP2sg_S).not();
    Condition allowA2plTrans = new Conditions.PreviousGroupContains(s.pA2sg_S, s.pP2pl_S).not();

    s.pvPresent_S.add(s.pvA1sg_ST, "+yIm", allowA1sgTrans);
    s.pvPresent_S.add(s.pvA2sg_ST, "sIn", allowA2sgTrans);
    // We do not allow ending with A3sg from empty Present tense.
    s.pvPresent_S.addEmpty(s.pvA3sg_S);
    s.pvPresent_S.add(s.pvA1pl_ST, "+yIz", allowA1plTrans);
    s.pvPresent_S.add(s.pvA2pl_ST, "sInIz");
    s.pvPresent_S.add(s.pvA3pl_ST, "lAr", new Conditions.PreviousGroupContains(s.pLoc_ST));

    s.pvPast_S.add(s.pvA1sg_ST, "m", allowA1sgTrans);
    s.pvPast_S.add(s.pvA2sg_ST, "n", allowA2sgTrans);
    s.pvPast_S.add(s.pvA1pl_ST, "k", allowA1plTrans);
    s.pvPast_S.add(s.pvA2pl_ST, "InIz");
    s.pvPast_S.add(s.pvA3pl_ST, "lAr");
    s.pvPast_S.addEmpty(s.pvA3sg_ST);

    s.pvNarr_S.add(s.pvA1sg_ST, "Im", allowA1sgTrans);
    s.pvNarr_S.add(s.pvA2sg_ST, "sIn", allowA2sgTrans);
    s.pvNarr_S.add(s.pvA1pl_ST, "Iz", allowA1plTrans);
    s.pvNarr_S.add(s.pvA2pl_ST, "sInIz");
    s.pvNarr_S.add(s.pvA3pl_ST, "lAr");
    s.pvNarr_S.addEmpty(s.pvA3sg_ST);
    // narr+cond is allowed but not past+cond
    s.pvNarr_S.add(s.pvCond_S, "sA");

    s.pvCond_S.add(s.pvA1sg_ST, "m", allowA1sgTrans);
    s.pvCond_S.add(s.pvA2sg_ST, "n", allowA2sgTrans);
    s.pvCond_S.add(s.pvA1pl_ST, "k", allowA1plTrans);
    s.pvCond_S.add(s.pvA2pl_ST, "nIz", allowA2plTrans);
    s.pvCond_S.addEmpty(s.pvA3sg_ST);
    s.pvCond_S.add(s.pvA3pl_ST, "lAr");

    Condition rejectNoCopula =
        new CurrentGroupContainsAny(s.pvPast_S, s.pvCond_S, s.pvCopBeforeA3pl_S).not();
    s.pvA1sg_ST.add(s.pvCop_ST, "dIr", rejectNoCopula);
    s.pvA2sg_ST.add(s.pvCop_ST, "dIr", rejectNoCopula);
    s.pvA1pl_ST.add(s.pvCop_ST, "dIr", rejectNoCopula);
    s.pvA2pl_ST.add(s.pvCop_ST, "dIr", rejectNoCopula);
    s.pvA3sg_S.add(s.pvCop_ST, ">dIr", rejectNoCopula);
    s.pvA3pl_ST.add(s.pvCop_ST, "dIr", rejectNoCopula);

    Condition pvA3plAfterLoc = Conditions.previousStateIs(s.pvPresent_S);
    s.pvA3pl_ST.add(s.vPastAfterTense_ST, "dI", pvA3plAfterLoc);
    s.pvA3pl_ST.add(s.vNarrAfterTense_ST, "mIş", pvA3plAfterLoc);
    s.pvA3pl_ST.add(s.vCond_ST, "sA", pvA3plAfterLoc);
    s.pvA3pl_ST.add(s.vWhile_S, "ken", pvA3plAfterLoc);

    // Copula can come before A3pl.
    s.pvPresent_S.add(s.pvCopBeforeA3pl_S, ">dIr");
    s.pvCopBeforeA3pl_S.add(s.pvA3pl_ST, "lAr");
  }
}
