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
  MorphemeState vProgYor_S_Fake = fakeNonTerminal("vProgYor_S_Fake", prog1);
  MorphemeState vFut_S_Fake = fakeNonTerminal("vFut_S", fut);

  void addGraph() {

    // yap-ıyo

    verbRoot_S.add(vProgYor_S_Fake, "Iyo", notHave(PhoneticAttribute.LastLetterVowel));
    verbRoot_Prog_S.add(vProgYor_S_Fake, "Iyo");
    vProgYor_S_Fake
        .add(vA1sg_ST, "m")
        .add(vA2sg_ST, "sun")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "z")
        .add(vA2pl_ST, "sunuz")
        .add(vA3pl_ST, "lar")
        .add(vCond_S, "sa")
        .add(vPastAfterTense_S, "du")
        .add(vNarrAfterTense_S, "muş")
        .add(vCopBeforeA3pl_S, "dur")
        .add(vWhile_S, "ken");
    vNegProg1_S.add(vProgYor_S, "Iyo");

    RootSurfaceIsAny diYiCondition = new RootSurfaceIsAny("di", "yi");
    vDeYeRoot_S
        .add(vProgYor_S, "yor", diYiCondition);

    // yap-a-k

    vOpt_S
        .add(vA1pl_ST_Fake,"Ak");


  }

}
