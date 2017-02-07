package zemberek.morphology.morphotactics;

public class TurkishMorphotactics {

    //-------------- Morphemes ------------------------

    Morpheme noun = new Morpheme("Noun");

    // case suffixes

    Morpheme nom = new Morpheme("Nom");
    Morpheme dat = new Morpheme("Dat");

    // possessive

    Morpheme pnon = new Morpheme("Pnon");
    Morpheme p1sg = new Morpheme("P1sg");
    Morpheme p3sg = new Morpheme("P3sg");

    // Number-Person agreement

    Morpheme a3sg = new Morpheme("A3sg");
    Morpheme a1pl = new Morpheme("A1pl");

    //-------------- States ------------------------

    MorphemeState nounS = MorphemeState.nonTerminal("nounS", noun);

    // case suffixes

    MorphemeState nomST = MorphemeState.terminal("nomST", nom);
    MorphemeState nomSnT = MorphemeState.nonTerminal("nomSnT", nom);
    MorphemeState datST = MorphemeState.terminal("datST", dat);

    // possessive

    MorphemeState pnonSnT = MorphemeState.nonTerminal("pnonSnT", pnon);
    MorphemeState p1sgSnT = MorphemeState.nonTerminal("p1sgSnT", p1sg);
    MorphemeState p3sgSnT = MorphemeState.nonTerminal("p3sgSnT", p3sg);

    // Number-Person agreement

    MorphemeState a3sgSnT = MorphemeState.nonTerminal("a3sgSnT", a3sg);
    MorphemeState a1plSnT = MorphemeState.nonTerminal("a1plSnT", a1pl);

    // ------------- Transitions ---------------------------------
    // Turkish Nouns always have Noun-Person-Possession-Case morphemes.
    // Event there is no suffix characters.
    // elma -> Noun:elma - A3sg:ε - Pnon:ε - Nom:ε (Third person singular, No possession, Nominal Case)

    MorphemeTransition nounS_a3sgS_eps = transition(nounS, a3sgSnT, ""); // ev-ε-?-?
    MorphemeTransition nounS_a1plS_lAr = transition(nounS, a1plSnT, "lAr"); // ev-ler-?-?

    MorphemeTransition a3sgS_pnonS_eps = transition(a3sgSnT, pnonSnT, ""); // ev-ε-ε-?
    MorphemeTransition a3sgS_p1sgS_Im = transition(a3sgSnT, p1sgSnT, "+Im"); //ev-ε-im oda-ε-m
    MorphemeTransition a3sgS_p1sgS_yum = transition(a3sgSnT, p1sgSnT, "yum"); //su-ε-yum. Only for "su"
    MorphemeTransition a3sgS_p3sgS_sI = transition(a3sgSnT, p3sgSnT, "+sI"); //ev-ε-i oda-ε-sı
    MorphemeTransition a3sgS_p3sgS_yu = transition(a3sgSnT, p3sgSnT, "yu"); //su-ε-yu. Only for "su"

    MorphemeTransition a1plS_pnonS_lAr = transition(a1plSnT, pnonSnT, "lAr"); // ev-ler-ε-?
    MorphemeTransition a1plS_p1sgS_Im = transition(a1plSnT, p1sgSnT, "Im"); //ev-ler-im oda-lar-ım
    MorphemeTransition a1plS_p3sgS_I = transition(a1plSnT, p3sgSnT, "I"); //ev-ler-i oda-lar-ı

    MorphemeTransition pnonS_nomS_eps = transition(pnonSnT, nomST, ""); //ev-?-ε-ε (ev, evler)
    MorphemeTransition pnonS_nomSnT_eps = transition(pnonSnT, nomSnT, ""); // for disallowing Voicing->ε-ε-ε chain like 'kitab'
    MorphemeTransition pnonS_datS_yA = transition(pnonSnT, datST, "+yA"); //ev-?-ε-e (eve, evlere)
    MorphemeTransition pnonS_datS_eps = transition(pnonSnT, datST, ""); //içeri-ε-ε (Only for some words)

    MorphemeTransition p1sgS_nomS_eps = transition(p1sgSnT, nomST, ""); //ev-?-im-ε (evim, evlerim)
    MorphemeTransition p1sgS_datS_yA = transition(p1sgSnT, datST, "A"); //ev-?-im-e (evime, evlerime)

    MorphemeTransition p3sgS_nomS_eps = transition(p3sgSnT, nomST, ""); //ev-?-i-ε (evi, evleri)
    MorphemeTransition p3sgS_datS_yA = transition(p3sgSnT, datST, "+nA"); //ev-?-i-ne (evine, evlerine)

    private static MorphemeTransition transition(MorphemeState from, MorphemeState to, String format) {
        return new MorphemeTransition(from, to, format);
    }

    public static void main(String[] args) {
        TurkishMorphotactics tm = new TurkishMorphotactics();
    }

}
