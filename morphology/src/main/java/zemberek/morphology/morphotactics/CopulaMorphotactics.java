package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.notHave;
import static zemberek.morphology.morphotactics.Conditions.rootIs;
import static zemberek.morphology.morphotactics.TurkishMorphotactics.*;

import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.ContainsMorpheme;
import zemberek.morphology.morphotactics.Conditions.CurrentGroupContainsAny;
import zemberek.morphology.morphotactics.Conditions.HasTailSequence;
import zemberek.morphology.morphotactics.Conditions.PreviousMorphemeIsAny;

/**
 * Morphotactic transition wiring for nominal/adjectival predicates ({@code nVerb_S}, {@code değil}),
 * the question clitic ({@code mı/mi/mu/mü}), and the copular verb {@code imek}.
 */
final class CopulaMorphotactics {

  private CopulaMorphotactics() {
  }

  static void connect(MorphotacticStates s, RootLexicon lexicon) {
    connectVerbAfterNounAdjStates(s, lexicon);
    connectQuestion(s);
    connectImek(s);
  }

  private static void connectVerbAfterNounAdjStates(MorphotacticStates s, RootLexicon lexicon) {
    // elma-..-ε-yım
    s.nVerb_S.addEmpty(s.nPresent_S);
    // elma-ydı, çorap-tı
    s.nVerb_S.add(s.nPast_S, "+y>dI");
    // elma-ymış
    s.nVerb_S.add(s.nNarr_S, "+ymIş");
    s.nVerb_S.add(s.nCond_S, "+ysA");
    s.nVerb_S.add(s.vWhile_S, "+yken", new HasTailSequence(narrPart, adj, zero, verb).not());

    // word "değil" is special. It contains negative suffix implicitly. Also it behaves like
    // noun->Verb Zero morpheme derivation because it cannot have most Verb suffixes.
    // So we connect it to a separate root state "nVerbDegil" instead of Verb.
    DictionaryItem degilRoot = lexicon.getItemById("değil_Verb");
    s.nVerbDegil_S.addEmpty(s.nNeg_S, rootIs(degilRoot));
    // copy transitions from nVerb_S
    s.nNeg_S.copyOutgoingTransitionsFrom(s.nVerb_S);

    Condition noFamily = notHave(RootAttribute.FamilyMember);
    // for preventing elmamım, elmamdım
    Condition verbDeriv = new ContainsMorpheme(inf1)
        .or(new ContainsMorpheme(inf2, inf3, pastPart, futPart, feelLike)
            .andNot(new Conditions.PreviousGroupContains(s.loc_ST, s.abl_ST, s.ins_ST)));
    Condition allowA1sgTrans = noFamily
        .andNot(new Conditions.ContainsMorphemeSequence(p1sg, nom))
        .andNot(verbDeriv);
    Condition allowA2sgTrans = noFamily
        .andNot(new Conditions.ContainsMorphemeSequence(p2sg, nom))
        .andNot(verbDeriv);
    Condition allowA3plTrans = noFamily
        .andNot(new Conditions.PreviousGroupContains(s.a3pl_S))
        .andNot(new Conditions.ContainsMorphemeSequence(p3pl, nom))
        .andNot(verbDeriv);
    Condition allowA2plTrans = noFamily
        .andNot(new Conditions.ContainsMorphemeSequence(p2pl, nom))
        .andNot(verbDeriv);
    Condition allowA1plTrans = noFamily
        .andNot(new Conditions.ContainsMorphemeSequence(p1sg, nom))
        .andNot(new Conditions.ContainsMorphemeSequence(p1pl, nom))
        .andNot(verbDeriv);

    // elma-yım
    s.nPresent_S.add(s.nA1sg_ST, "+yIm", allowA1sgTrans);
    s.nPresent_S.add(s.nA2sg_ST, "sIn", allowA2sgTrans);
    // elma-ε-ε-dır to non terminal A3sg. We do not allow ending with A3sg from empty Present tense.
    s.nPresent_S.addEmpty(s.nA3sg_S);
    // we allow `değil` to end with terminal A3sg from Present tense.
    s.nPresent_S.addEmpty(s.nA3sg_ST, rootIs(degilRoot));
    // elma-lar, elma-da-lar as Verb.
    s.nPresent_S.add(s.nA3pl_ST, "lAr",
        notHave(RootAttribute.CompoundP3sg)
            // do not allow "okumak-lar"
            .andNot(new Conditions.PreviousGroupContainsMorpheme(inf1))
            .and(allowA3plTrans));

    // elma-ydı-m. Do not allow "elmaya-yım" (Oflazer accepts this)
    s.nPast_S.add(s.nA1sg_ST, "m", allowA1sgTrans);
    s.nNarr_S.add(s.nA1sg_ST, "Im", allowA1sgTrans);

    s.nPast_S.add(s.nA2sg_ST, "n", allowA2sgTrans);
    s.nNarr_S.add(s.nA2sg_ST, "sIn", allowA2sgTrans);

    s.nPast_S.add(s.nA1pl_ST, "k", allowA1plTrans);
    s.nNarr_S.add(s.nA1pl_ST, "Iz", allowA1plTrans);
    s.nPresent_S.add(s.nA1pl_ST, "+yIz", allowA1plTrans);

    s.nPast_S.add(s.nA2pl_ST, "InIz", allowA2plTrans);
    s.nNarr_S.add(s.nA2pl_ST, "sInIz", allowA2plTrans);
    s.nPresent_S.add(s.nA2pl_ST, "sInIz", allowA2plTrans);

    // elma-ydı-lar, elma-ymış-lar
    Condition a3plAfterTense = notHave(RootAttribute.CompoundP3sg).and(allowA3plTrans);
    s.nPast_S.add(s.nA3pl_ST, "lAr", a3plAfterTense);
    s.nNarr_S.add(s.nA3pl_ST, "lAr", a3plAfterTense);

    // elma-ydı-ε, elma-ymış-ε
    s.nPast_S.addEmpty(s.nA3sg_ST);
    s.nNarr_S.addEmpty(s.nA3sg_ST);

    // narr+cond is allowed but not past+cond
    s.nNarr_S.add(s.nCond_S, "sA");

    s.nCond_S.add(s.nA1sg_ST, "m", allowA1sgTrans);
    s.nCond_S.add(s.nA2sg_ST, "n", allowA2sgTrans);
    s.nCond_S.add(s.nA1pl_ST, "k", allowA1plTrans);
    s.nCond_S.add(s.nA2pl_ST, "nIz", allowA2plTrans);
    s.nCond_S.addEmpty(s.nA3sg_ST);
    s.nCond_S.add(s.nA3pl_ST, "lAr");

    // for not allowing "elma-ydı-m-dır"
    Condition rejectNoCopula = new CurrentGroupContainsAny(s.nPast_S, s.nCond_S, s.nCopBeforeA3pl_S)
        .not();

    s.nA1sg_ST.add(s.nCop_ST, "dIr", rejectNoCopula);
    s.nA2sg_ST.add(s.nCop_ST, "dIr", rejectNoCopula);
    s.nA1pl_ST.add(s.nCop_ST, "dIr", rejectNoCopula);
    s.nA2pl_ST.add(s.nCop_ST, "dIr", rejectNoCopula);
    s.nA3sg_S.add(s.nCop_ST, ">dIr", rejectNoCopula);
    s.nA3pl_ST.add(s.nCop_ST, "dIr", rejectNoCopula);

    PreviousMorphemeIsAny asIfCond = new PreviousMorphemeIsAny(narr);
    s.nA3sg_ST.add(s.vAsIf_S, ">cAsInA", asIfCond);
    s.nA1sg_ST.add(s.vAsIf_S, ">cAsInA", asIfCond);
    s.nA2sg_ST.add(s.vAsIf_S, ">cAsInA", asIfCond);
    s.nA1pl_ST.add(s.vAsIf_S, ">cAsInA", asIfCond);
    s.nA2pl_ST.add(s.vAsIf_S, ">cAsInA", asIfCond);
    s.nA3pl_ST.add(s.vAsIf_S, ">cAsInA", asIfCond);

    // Allow A3pl before Past/Narr/Cond/While on oblique nominal predicates: "evdelerdi", "evdelermiş".
    Condition nA3plAfterCase = Conditions.previousStateIs(s.nPresent_S)
        .and(new Conditions.PreviousGroupContains(s.loc_ST, s.abl_ST, s.dat_ST, s.ins_ST));
    s.nA3pl_ST.add(s.vPastAfterTense_ST, "dI", nA3plAfterCase);
    s.nA3pl_ST.add(s.vNarrAfterTense_ST, "mIş", nA3plAfterCase);
    s.nA3pl_ST.add(s.vCond_ST, "sA", nA3plAfterCase);
    s.nA3pl_ST.add(s.vWhile_S, "ken", nA3plAfterCase);

    // Copula can come before A3pl.
    s.nPresent_S.add(s.nCopBeforeA3pl_S, ">dIr");
    s.nCopBeforeA3pl_S.add(s.nA3pl_ST, "lAr", a3plAfterTense);
  }

  private static void connectQuestion(MorphotacticStates s) {
    // mı, mıydı, mıymış
    s.questionRoot_S.addEmpty(s.qPresent_S);
    s.questionRoot_S.add(s.qPast_S, "ydI");
    s.questionRoot_S.add(s.qNarr_S, "ymIş");

    s.qPresent_S.add(s.qA1sg_ST, "yIm");
    s.qPresent_S.add(s.qA2sg_ST, "sIn");
    s.qPresent_S.addEmpty(s.qA3sg_ST);

    s.qPast_S.add(s.qA1sg_ST, "m");
    s.qNarr_S.add(s.qA1sg_ST, "Im");

    s.qPast_S.add(s.qA2sg_ST, "n");
    s.qNarr_S.add(s.qA2sg_ST, "sIn");

    s.qPast_S.add(s.qA1pl_ST, "k");
    s.qNarr_S.add(s.qA1pl_ST, "Iz");
    s.qPresent_S.add(s.qA1pl_ST, "+yIz");

    s.qPast_S.add(s.qA2pl_ST, "InIz");
    s.qNarr_S.add(s.qA2pl_ST, "sInIz");
    s.qPresent_S.add(s.qA2pl_ST, "sInIz");

    s.qPast_S.add(s.qA3pl_ST, "lAr");
    s.qNarr_S.add(s.qA3pl_ST, "lAr");

    s.qPast_S.addEmpty(s.qA3sg_ST);
    s.qNarr_S.addEmpty(s.qA3sg_ST);

    // for not allowing "mı-ydı-m-dır"
    Condition rejectNoCopula = new CurrentGroupContainsAny(s.qPast_S).not();
    s.qA1sg_ST.add(s.qCop_ST, "dIr", rejectNoCopula);
    s.qA2sg_ST.add(s.qCop_ST, "dIr", rejectNoCopula);
    s.qA3sg_ST.add(s.qCop_ST, ">dIr", rejectNoCopula);
    s.qA1pl_ST.add(s.qCop_ST, "dIr", rejectNoCopula);
    s.qA2pl_ST.add(s.qCop_ST, "dIr", rejectNoCopula);

    // Copula can come before A3pl.
    s.qPresent_S.add(s.qCopBeforeA3pl_S, ">dIr");
    s.qCopBeforeA3pl_S.add(s.qA3pl_ST, "lAr");
  }

  private static void connectImek(MorphotacticStates s) {
    // idi, imiş, ise
    s.imekRoot_S.add(s.imekPast_S, "di");
    s.imekRoot_S.add(s.imekNarr_S, "miş");
    s.imekRoot_S.add(s.imekCond_S, "se");

    // idim, idin, idi, idik, idiniz, idiler
    s.imekPast_S
        .add(s.imekA1sg_ST, "m")
        .add(s.imekA2sg_ST, "n")
        .addEmpty(s.imekA3sg_ST)
        .add(s.imekA1pl_ST, "k")
        .add(s.imekA2pl_ST, "niz")
        .add(s.imekA3pl_ST, "ler");

    // imişim, imişsin, imiş, imişiz, imişsiniz, imişler
    s.imekNarr_S
        .add(s.imekA1sg_ST, "im")
        .add(s.imekA2sg_ST, "sin")
        .addEmpty(s.imekA3sg_ST)
        .add(s.imekA1pl_ST, "iz")
        .add(s.imekA2pl_ST, "siniz")
        .add(s.imekA3pl_ST, "ler");

    s.imekPast_S.add(s.imekCond_S, "yse");
    s.imekNarr_S.add(s.imekCond_S, "se");

    s.imekCond_S
        .add(s.imekA1sg_ST, "m")
        .add(s.imekA2sg_ST, "n")
        .addEmpty(s.imekA3sg_ST)
        .add(s.imekA1pl_ST, "k")
        .add(s.imekA2pl_ST, "niz")
        .add(s.imekA3pl_ST, "ler");

    // for not allowing "i-di-m-dir"
    Condition rejectNoCopula = new CurrentGroupContainsAny(s.imekPast_S).not();
    s.imekA1sg_ST.add(s.imekCop_ST, "dir", rejectNoCopula);
    s.imekA2sg_ST.add(s.imekCop_ST, "dir", rejectNoCopula);
    s.imekA3sg_ST.add(s.imekCop_ST, "tir", rejectNoCopula);
    s.imekA1pl_ST.add(s.imekCop_ST, "dir", rejectNoCopula);
    s.imekA2pl_ST.add(s.imekCop_ST, "dir", rejectNoCopula);
    s.imekA3pl_ST.add(s.imekCop_ST, "dir", rejectNoCopula);
  }
}
