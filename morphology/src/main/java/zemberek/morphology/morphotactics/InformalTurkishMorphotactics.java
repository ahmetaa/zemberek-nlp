package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.notHave;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminal;
import static zemberek.morphology.morphotactics.MorphemeState.terminal;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.analysis.StemTransitionsMapBased;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.RootSurfaceIsAny;

public class InformalTurkishMorphotactics extends TurkishMorphotactics {

  public InformalTurkishMorphotactics(RootLexicon lexicon) {
    this.lexicon = lexicon;
    makeGraph();
    addGraph();
    this.stemTransitions = new StemTransitionsMapBased(lexicon, this);
  }

  public static final Morpheme a1plInformal = addMorpheme(
      Morpheme.builder("A1pl_Informal", "A1pl_Informal")
          .informal().mappedMorpheme(a1pl).build());

  public static final Morpheme a1sgInformal = addMorpheme(
      Morpheme.builder("A1sg_Informal", "A1sg_Informal")
          .informal().mappedMorpheme(a1sg).build());

  public static final Morpheme prog1Informal = addMorpheme(
      Morpheme.builder("Prog1_Informal", "Prog1_Informal")
          .informal().mappedMorpheme(prog1).build());

  public static final Morpheme futInformal = addMorpheme(
      Morpheme.builder("Fut_Informal", "Fut_Informal")
          .informal().mappedMorpheme(fut).build());

  // TODO: not used yet.
  public static final Morpheme quesSuffixInformal = addMorpheme(
      Morpheme.builder("QuesSuffix_Informal", "QuesSuffix_Informal")
          .informal().mappedMorpheme(ques).build());

  public static final Morpheme negInformal = addMorpheme(
      Morpheme.builder("Neg_Informal", "Neg_Informal")
          .informal().mappedMorpheme(neg).build());

  public static final Morpheme unableInformal = addMorpheme(
      Morpheme.builder("Unable_Informal", "Unable_Informal")
          .informal().mappedMorpheme(unable).build());

  public static final Morpheme optInformal = addMorpheme(
      Morpheme.builder("Opt_Informal", "Opt_Informal")
          .informal().mappedMorpheme(opt).build());

  MorphemeState vA1pl_ST_Inf = terminal("vA1pl_ST_Inf", a1plInformal);
  MorphemeState vA1sg_ST_Inf = terminal("vA1sg_ST_Inf", a1sgInformal);
  MorphemeState vProgYor_S_Inf = nonTerminal("vProgYor_S_Inf", prog1Informal);

  MorphemeState vFut_S_Inf = nonTerminal("vFut_S_Inf", futInformal);
  MorphemeState vFut_S_Inf2 = nonTerminal("vFut_S_Inf2", futInformal);
  MorphemeState vFut_S_Inf3 = nonTerminal("vFut_S_Inf3", futInformal);

  MorphemeState vQues_S_Inf = nonTerminal("vQues_S_Inf", quesSuffixInformal);

  MorphemeState vNeg_S_Inf = nonTerminal("vNeg_S_Inf", negInformal);
  MorphemeState vUnable_S_Inf = nonTerminal("vUnable_S_Inf", unableInformal);

  MorphemeState vOpt_S_Inf = nonTerminal("vOpt_S_Inf", optInformal);
  MorphemeState vOpt_S_Empty_Inf = nonTerminal("vOpt_S_Empty_Inf", optInformal);
  MorphemeState vOpt_S_Empty_Inf2 = nonTerminal("vOpt_S_Empty_Inf2", optInformal);

  void addGraph() {

    // yap-ıyo

    verbRoot_S.add(vProgYor_S_Inf, "Iyo", notHave(PhoneticAttribute.LastLetterVowel));
    verbRoot_VowelDrop_S.add(vProgYor_S_Inf, "Iyo");
    vProgYor_S_Inf
        .add(vA1sg_ST, "m")
        .add(vA2sg_ST, "sun")
        .add(vA2sg_ST, "n")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "z")
        .add(vA2pl_ST, "sunuz")
        .add(vA2pl_ST, "nuz")
        .add(vA3pl_ST, "lar")
        .add(vCond_S, "sa")
        .add(vPastAfterTense_S, "du")
        .add(vNarrAfterTense_S, "muş")
        .add(vCopBeforeA3pl_S, "dur")
        .add(vWhile_S, "ken");

    vNegProg1_S.add(vProgYor_S_Inf, "Iyo");
    vUnableProg1_S.add(vProgYor_S_Inf, "Iyo");

    RootSurfaceIsAny diYiCondition = new RootSurfaceIsAny("di", "yi");
    vDeYeRoot_S
        .add(vProgYor_S_Inf, "yo", diYiCondition);

    // yap-a-k

    vOpt_S
        .add(vA1pl_ST_Inf, "k");

    // Future tense deformation
    // yap-ıca-m yap-aca-m yap-ca-m

    verbRoot_S.add(vNeg_S_Inf, "mI");
    verbRoot_S.add(vUnable_S_Inf, "+yAmI");

    verbRoot_S
        .add(vFut_S_Inf, "+ycA~k")  // yap-cak-sın oku-ycak
        .add(vFut_S_Inf, "+ycA!ğ")  // yap-cağ-ım
        .add(vFut_S_Inf2, "+ycA")   // yap-ca-m oku-yca-m
        .add(vFut_S_Inf2, "+yIcA")  // yap-ıca-m oku-yuca-m
        .add(vFut_S_Inf2, "+yAcA");  // yap-aca-m oku-yaca-m

    vNeg_S_Inf
        .add(vFut_S, "yAcA~k")  // yap-mı-yacak-sın
        .add(vFut_S, "yAcA!ğ")  // yap-mı-yacağ-ım
        .add(vFut_S_Inf, "ycA~k")   // yap-mı-ycağ-ım
        .add(vFut_S_Inf, "ycA!ğ")   // yap-mı-ycak-sın
        .add(vFut_S_Inf2, "ycA");   // yap-mı-ycak

    vUnable_S_Inf
        .add(vFut_S, "yAcA~k")  // yap-amı-yacak-sın
        .add(vFut_S, "yAcA!ğ")  // yap-amı-yacağ-ım
        .add(vFut_S_Inf, "ycA~k")   // yap-amı-ycağ-ım
        .add(vFut_S_Inf, "ycA!ğ")   // yap-amı-ycak-sın
        .add(vFut_S_Inf2, "ycA");   // yap-amı-ycak

    vNeg_S
        .add(vFut_S_Inf, "yAcA")   // yap-ma-yaca-m
        .add(vFut_S_Inf, "yAcAk");   // yap-ma-yacak-(A3sg|A3pl)

    vUnable_S
        .add(vFut_S_Inf, "yAcA")   // yap-ama-yaca-m
        .add(vFut_S_Inf, "yAcAk");   // yap-ama-yacak-(A3sg|A3pl)

    vFut_S_Inf
        .add(vA1sg_ST, "+Im")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "Iz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr");

    vFut_S_Inf2
        .add(vA1sg_ST, "m") // yap-ca-m
        .add(vA2sg_ST, "n") // yap-ca-n
        .add(vA1pl_ST, "z") // yap-ca-z
        .add(vA1pl_ST, "nIz"); // yap-ca-nız

    vFut_S_Inf.add(vCond_S, "sA");
    vFut_S_Inf.add(vPastAfterTense_S, "tI");
    vFut_S_Inf.add(vNarrAfterTense_S, "mIş");
    vFut_S_Inf.add(vCopBeforeA3pl_S, "tIr");
    vFut_S_Inf.add(vWhile_S, "ken");

    // Handling of yapıyim, okuyim, bakıyim. TODO: not yet finished

    verbRoot_S.add(vOpt_S_Inf, "I", Conditions.has(PhoneticAttribute.LastLetterConsonant));

    // for handling "arıyim, okuyim"
    verbRoot_VowelDrop_S.add(vOpt_S_Inf, "I");

    verbRoot_S.addEmpty(vOpt_S_Empty_Inf, Conditions.has(PhoneticAttribute.LastLetterVowel));

    vOpt_S_Inf.add(vA1sg_ST_Inf, "+yIm");
    vOpt_S_Inf.add(vA1sg_ST_Inf, "+yim");
    vOpt_S_Empty_Inf.add(vA1sg_ST_Inf, "+yim");

    // handling of 'babacım, kuzucum' or 'babacıım, kuzucuum'

    // handling of 'abim-gil' 'Ahmet'gil'

    // yap-tı-mı Connected Question word.
    // After past and narrative, a person suffix is required.
    // yap-tı-m-mı
    // After progressive, future, question can come before.
    // yap-ıyor-mu-yum yap-acak-mı-yız

  }

}
