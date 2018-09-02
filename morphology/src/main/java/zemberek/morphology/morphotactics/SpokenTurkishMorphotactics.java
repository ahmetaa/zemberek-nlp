package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.notHave;
import static zemberek.morphology.morphotactics.MorphemeState.fakeNonTerminal;
import static zemberek.morphology.morphotactics.MorphemeState.fakeTerminal;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.analysis.StemTransitionsMapBased;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.RootSurfaceIsAny;

public class SpokenTurkishMorphotactics extends TurkishMorphotactics {

  public SpokenTurkishMorphotactics(RootLexicon lexicon) {
    this.lexicon = lexicon;
    makeGraph();
    addGraph();
    this.stemTransitions = new StemTransitionsMapBased(lexicon, this);
  }

  MorphemeState vA1pl_ST_Fake = fakeTerminal("vA1pl_ST_Fake", a1pl);
  MorphemeState vA1sg_ST_Fake = fakeTerminal("vA1sg_ST_Fake", a1sg);
  MorphemeState vProgYor_S_Fake = fakeNonTerminal("vProgYor_S_Fake", prog1);

  MorphemeState vFut_S_Fake = fakeNonTerminal("vFut_S_Fake", fut);
  MorphemeState vFut_S_Fake2 = fakeNonTerminal("vFut_S_Fake2", fut);

  MorphemeState vQues_S_Fake = fakeNonTerminal("vQues_S_Fake", ques);

  MorphemeState vNeg_S_Fake = fakeNonTerminal("vNeg_S_Fake", neg);

  MorphemeState vOpt_S_Fake = fakeNonTerminal("vOpt_S_Fake", opt);
  MorphemeState vOpt_S_Empty_Fake = fakeNonTerminal("vOpt_S_Empty_Fake", opt);

  void addGraph() {

    // yap-ıyo

    verbRoot_S.add(vProgYor_S_Fake, "Iyo", notHave(PhoneticAttribute.LastLetterVowel));
    verbRoot_Prog_S.add(vProgYor_S_Fake, "Iyo");
    vProgYor_S_Fake
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

    vNegProg1_S.add(vProgYor_S_Fake, "Iyo");

    RootSurfaceIsAny diYiCondition = new RootSurfaceIsAny("di", "yi");
    vDeYeRoot_S
        .add(vProgYor_S, "yo", diYiCondition);

    // yap-a-k

    vOpt_S
        .add(vA1pl_ST_Fake,"k");

    // Future tense deformation
    // yap-ıca-m yap-aca-m yap-ca-m

    verbRoot_S.add(vNeg_S_Fake, "mI");

    verbRoot_S
        .add(vFut_S_Fake, "+ycA~k")  // yap-cak-sın oku-ycak
        .add(vFut_S_Fake, "+ycA!ğ")  // yap-cağ-ım
        .add(vFut_S_Fake2, "+ycA")   // yap-ca-m oku-yca-m
        .add(vFut_S_Fake2, "+yIcA")  // yap-ıca-m oku-yuca-m
        .add(vFut_S_Fake2, "+yAcA");  // yap-aca-m oku-yaca-m

    vNeg_S_Fake
        .add(vFut_S, "yAcA~k")  // yap-mı-yacak-sın
        .add(vFut_S, "yAcA!ğ")  // yap-mı-yacağ-ım
        .add(vFut_S_Fake, "ycA~k")   // yap-mı-ycağ-ım
        .add(vFut_S_Fake, "ycA!ğ")   // yap-mı-ycak-sın
        .add(vFut_S_Fake2, "ycA");   // yap-mı-ycak

    vNeg_S
        .add(vFut_S_Fake, "yAcA")   // yap-ma-yaca-m
        .add(vFut_S_Fake, "yAcAk");   // yap-ma-yacak-(A3sg|A3pl)

    vFut_S_Fake
        .add(vA1sg_ST, "+Im")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "Iz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr");

    vFut_S_Fake2
        .add(vA1sg_ST, "m") // yap-ca-m
        .add(vA2sg_ST, "n") // yap-ca-n
        .add(vA1pl_ST, "z") // yap-ca-z
        .add(vA1pl_ST, "nIz"); // yap-ca-nız


    vFut_S_Fake.add(vCond_S, "sA");
    vFut_S_Fake.add(vPastAfterTense_S, "tI");
    vFut_S_Fake.add(vNarrAfterTense_S, "mIş");
    vFut_S_Fake.add(vCopBeforeA3pl_S, "tIr");
    vFut_S_Fake.add(vWhile_S, "ken");

    // Handling of yapıyim, okuyim, bakıyim

    verbRoot_S.add(vOpt_S_Fake, "I", Conditions.has(PhoneticAttribute.LastLetterConsonant));
    verbRoot_S.addEmpty(vOpt_S_Empty_Fake, Conditions.has(PhoneticAttribute.LastLetterVowel));

    vOpt_S_Fake.add(vA1sg_ST_Fake, "+yIm");
    vOpt_S_Empty_Fake.add(vA1sg_ST_Fake, "+yim");


    // yap-tı-mı Connected Question word.
    // After past and narrative, a person suffix is required.
    // yap-tı-m-mı
    // After progressive, future, question can come before.
    // yap-ıyor-mu-yum yap-acak-mı-yız


  }

}
