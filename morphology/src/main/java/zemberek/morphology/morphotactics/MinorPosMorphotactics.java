package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.rootIsAny;

import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * Morphotactic transition wiring for adverbs and postpositives.
 */
final class MinorPosMorphotactics {

  private MinorPosMorphotactics() {
  }

  static void connect(MorphotacticStates s, RootLexicon lexicon) {
    connectAdverbs(s);
    connectPostpositives(s, lexicon);
  }

  private static void connectAdverbs(MorphotacticStates s) {
    s.advNounRoot_ST.addEmpty(s.avZero_S);
    s.avZero_S.addEmpty(s.avNounAfterAdvRoot_ST);
    s.avNounAfterAdvRoot_ST.addEmpty(s.avA3sg_S);
    s.avA3sg_S.addEmpty(s.avPnon_S);
    s.avPnon_S.add(s.avDat_ST, "+yA");

    s.advForVerbDeriv_ST.addEmpty(s.avZeroToVerb_S);
    s.avZeroToVerb_S.addEmpty(s.nVerb_S);
  }

  private static void connectPostpositives(MorphotacticStates s, RootLexicon lexicon) {
    s.postpRoot_ST.addEmpty(s.postpZero_S);
    s.postpZero_S.addEmpty(s.nVerb_S);

    // gibi is kind of special.
    DictionaryItem gibiGen = lexicon.getItemById("gibi_Postp_PCGen");
    DictionaryItem gibiNom = lexicon.getItemById("gibi_Postp_PCNom");
    DictionaryItem sonraAbl = lexicon.getItemById("sonra_Postp_PCAbl");

    s.postpZero_S.addEmpty(s.po2nRoot_S, rootIsAny(gibiGen, gibiNom, sonraAbl));

    s.po2nRoot_S.addEmpty(s.po2nA3sg_S);
    s.po2nRoot_S.add(s.po2nA3pl_S, "lAr");

    // gibisi, gibim-e, gibi-e, gibi-mize
    Condition isGibi = rootIsAny(gibiGen, gibiNom);
    s.po2nA3sg_S.add(s.po2nP3sg_S, "+sI");
    s.po2nA3sg_S.add(s.po2nP1sg_S, "m", isGibi);
    s.po2nA3sg_S.add(s.po2nP2sg_S, "n", isGibi);
    s.po2nA3sg_S.add(s.po2nP1pl_S, "miz", isGibi);
    s.po2nA3sg_S.add(s.po2nP2pl_S, "niz", isGibi);

    // gibileri
    s.po2nA3pl_S.add(s.po2nP3sg_S, "+sI");
    s.po2nA3pl_S.addEmpty(s.po2nPnon_S);

    s.po2nP3sg_S
        .addEmpty(s.po2nNom_ST)
        .add(s.po2nDat_ST, "nA")
        .add(s.po2nLoc_ST, "ndA")
        .add(s.po2nAbl_ST, "ndAn")
        .add(s.po2nIns_ST, "ylA")
        .add(s.po2nGen_ST, "nIn")
        .add(s.po2nAcc_ST, "nI");

    s.po2nPnon_S
        .addEmpty(s.po2nNom_ST)
        .add(s.po2nDat_ST, "A")
        .add(s.po2nLoc_ST, "dA")
        .add(s.po2nAbl_ST, "dAn")
        .add(s.po2nIns_ST, "lA")
        .add(s.po2nGen_ST, "In")
        .add(s.po2nEqu_ST, "cA")
        .add(s.po2nAcc_ST, "I");

    s.po2nP1sg_S.add(s.po2nDat_ST, "e");
    s.po2nP2sg_S.add(s.po2nDat_ST, "e");
    s.po2nP1pl_S.add(s.po2nDat_ST, "e");
    s.po2nP2pl_S.add(s.po2nDat_ST, "e");
  }
}
