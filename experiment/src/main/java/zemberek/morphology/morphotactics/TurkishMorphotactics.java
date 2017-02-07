package zemberek.morphology.morphotactics;

public class TurkishMorphotactics {

    //-------------- Morphemes ------------------------

    Morpheme noun = morpheme("Noun");

    // case suffixes

    Morpheme nom = morpheme("Nom");
    Morpheme dat = morpheme("Dat");

    // possessive

    Morpheme pnon = morpheme("Pnon");
    Morpheme p1sg = morpheme("P1sg");
    Morpheme p3sg = morpheme("P3sg");

    // Number-Person agreement

    Morpheme a3sg = morpheme("A3sg");
    Morpheme a1pl = morpheme("A1pl");

    //-------------- States ------------------------

    MorphemeState nounS = state(noun);

    // case suffixes

    MorphemeState nomS = state(nom);
    MorphemeState datS = state(dat);

    // possessive

    MorphemeState pnonS = state(pnon);
    MorphemeState p1sgS = state(p1sg);
    MorphemeState p3sgS = state(p3sg);

    // Number-Person agreement

    MorphemeState a3sgS = state(a3sg);
    MorphemeState a1plS = state(a1pl);

    // ------------- Transitions ---------------------------------

    MorphemeTransition nounS_a3sgS_eps = transition(nounS, a3sgS, ""); // ev-ε-?-?

    MorphemeTransition a3sgS_pnonS_eps = transition(a3sgS, pnonS, ""); // ev-ε-ε-?
    MorphemeTransition a3sgS_p1sgS_Im = transition(a3sgS, p1sgS, "+Im"); //ev-ε-im oda-ε-m
    MorphemeTransition a3sgS_p1sgS_yum = transition(a3sgS, p1sgS, "yum"); //su-ε-yum. Only for "su"
    MorphemeTransition a3sgS_p3sgS_sI = transition(a3sgS, p3sgS, "+sI"); //ev-ε-i oda-ε-sı

    MorphemeTransition a1plS_pnonS_lAr = transition(a1plS, pnonS, "lAr"); // ev-ler-ε-?
    MorphemeTransition a1plS_p1sgS_Im = transition(a1plS, p1sgS, "Im"); //ev-ler-im oda-lar-ım
    MorphemeTransition a1plS_p3sgS_I = transition(a1plS, p3sgS, "I"); //ev-ler-i oda-lar-ı

    MorphemeTransition pnonS_nomS_eps = transition(pnonS, nomS, ""); //ev-?-ε-ε (ev, evler)
    MorphemeTransition pnonS_datS_yA = transition(pnonS, datS, "+yA"); //ev-?-ε-e (eve, evlere)
    MorphemeTransition pnonS_datS_eps = transition(pnonS, datS, ""); //içeri-ε-ε (Only for some words)

    MorphemeTransition p1sgS_nomS_eps = transition(p1sgS, nomS, ""); //ev-?-im-ε (evim, evlerim)
    MorphemeTransition p1sgS_datS_yA = transition(p1sgS, datS, "A"); //ev-?-im-e (evime, evlerime)

    MorphemeTransition p3sgS_nomS_eps = transition(p3sgS, nomS, ""); //ev-?-i-ε (evi, evleri)
    MorphemeTransition p3sgS_datS_yA = transition(p3sgS, datS, "+nA"); //ev-?-i-ne (evine, evlerine)

    private static MorphemeState state(String id, Morpheme morpheme) {
        return new MorphemeState(id, morpheme);
    }

    private static MorphemeState state(Morpheme morpheme) {
        return new MorphemeState(morpheme.id + "S", morpheme);
    }


    private static Morpheme morpheme(String id) {
        return new Morpheme(id);
    }

    private static MorphemeTransition transition(MorphemeState from, MorphemeState to, String format) {
        return new MorphemeTransition(from, to, format);
    }

}
