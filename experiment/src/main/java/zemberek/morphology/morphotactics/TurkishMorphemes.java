package zemberek.morphology.morphotactics;

public class TurkishMorphemes {

    //-------------- States ------------------------

    MorphemeState noun = morpheme("Noun");

    // case suffixes

    MorphemeState nom = morpheme("Nom");
    MorphemeState dat = morpheme("Dat");
    MorphemeState loc = morpheme("Loc");
    MorphemeState abl = morpheme("Abl");
    MorphemeState gen = morpheme("Gen");
    MorphemeState acc = morpheme("Acc");
    MorphemeState inst = morpheme("Inst");

    // possessive

    MorphemeState pnon = morpheme("Pnon");
    MorphemeState p1sg = morpheme("P1sg");
    MorphemeState p2sg = morpheme("P2sg");
    MorphemeState p3sg = morpheme("P3sg");
    MorphemeState p1pl = morpheme("P1pl");
    MorphemeState p2pl = morpheme("P2pl");
    MorphemeState p3pl = morpheme("P3pl");

    // Number-Person agreement

    MorphemeState a1sg = morpheme("A1sg");
    MorphemeState a2sg = morpheme("A2sg");
    MorphemeState a3sg = morpheme("A3sg");
    MorphemeState a1pl = morpheme("A1pl");
    MorphemeState a2pl = morpheme("A2pl");
    MorphemeState a3pl = morpheme("A3pl");

    // ------------- Transitions ---------------------------------

    MorphemeTransition noun_a3sg_eps = transition(noun, a3sg, ""); // ev-ε-?-?

    MorphemeTransition a3sg_pnon_eps = transition(a3sg, pnon, ""); // ev-ε-ε-?
    MorphemeTransition a3sg_p1sg_Im = transition(a3sg, p1sg, "+Im"); //ev-ε-im oda-ε-m
    MorphemeTransition a3sg_p1sg_yIm = transition(a3sg, p1sg, "+yIm"); //su-ε-yum. Only for "su"
    MorphemeTransition a3sg_p2sg_In = transition(a3sg, p2sg, "+In"); //ev-ε-in oda-ε-n
    MorphemeTransition a3sg_p2sg_yIn = transition(a3sg, p2sg, "+yIn"); //su-ε-yun Only for "su"

    MorphemeTransition a1pl_pnon_lAr = transition(a1pl, pnon, "lAr"); // ev-ler-ε-?
    MorphemeTransition a1pl_p1sg_Im = transition(a1pl, p1sg, "+Im"); //ev-ler-im oda-lar-ım
    MorphemeTransition a1pl_p2sg_In = transition(a1pl, p2sg, "+In"); //ev-ler-in oda-lar-ın

    MorphemeTransition pnon_nom_eps = transition(pnon, nom, ""); //ev-?-ε-ε (ev, evler)
    MorphemeTransition pnon_dat_yA = transition(pnon, dat, "+yA"); //ev-?-ε-e (eve, evlere)
    MorphemeTransition pnon_dat_eps = transition(pnon, dat, ""); //içeri-ε-ε (Only for some words)

    MorphemeTransition p1sg_nom_eps = transition(p1sg, nom, ""); //ev-?-im-ε (evim, evlerim)
    MorphemeTransition p1sg_dat_yA = transition(p1sg, dat, "+yA"); //ev-?-in-e (evime, evlerime)

    MorphemeTransition p2sg_nom_eps = transition(p2sg, nom, ""); //ev-?-in-ε (evin, evlerin)
    MorphemeTransition p2sg_dat_yA = transition(p2sg, dat, "+yA"); //ev-?-in-e (evine, evlerine)

    private static MorphemeState morpheme(String id) {
        return new MorphemeState(id); 
    }

    private static MorphemeTransition transition(MorphemeState from, MorphemeState to, String format) {
        return new MorphemeTransition(from, to, format);
    }

    enum TransitionRule {
        ALLOW_ONLY,
        ALLOW_EXCEPT
    }

    

}
