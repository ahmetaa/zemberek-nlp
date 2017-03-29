package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Rules.allowOnly;
import static zemberek.morphology.morphotactics.Rules.rejectAny;
import static zemberek.morphology.morphotactics.Rules.rejectOnly;

public class TurkishMorphotactics {

    //-------------- Morpheme Groups ------------------
    // These only carries some semantic information and may be useful for debugging purposes.
    // Names are similar to the ones used in Ali Ok's TRNLTK project.

    MorphemeGroup nounRoot = new MorphemeGroup("NOUN_ROOT");
    MorphemeGroup nounAgreement = new MorphemeGroup("NOUN_AGREEMENT");
    MorphemeGroup nounPossession = new MorphemeGroup("NOUN_POSSESSION");
    MorphemeGroup nounCase = new MorphemeGroup("NOUN_CASE");
    MorphemeGroup nounNounDerivation = new MorphemeGroup("NOUN_NOUN_DERIVATION");

    //-------------- Morphemes ------------------------

    Morpheme noun = new Morpheme("Noun", nounRoot);

    // Number-Person agreement.

    // Third person singular suffix. "elma = apple"
    Morpheme a3sg = new Morpheme("A3sg", nounAgreement);
    // Third person plural suffix. "elma-lar = apples"
    Morpheme a3pl = new Morpheme("A3pl", nounAgreement);

    // Possessive

    // No possession suffix. This is not a real morpheme but adds information to analysis. "elma = apple"
    Morpheme pnon = new Morpheme("Pnon", nounPossession);
    // First person singular possession suffix.  "elma-m = my apple"
    Morpheme p1sg = new Morpheme("P1sg", nounPossession);
    // Third person singular possession suffix. "elma-sı = his/her apple"
    Morpheme p3sg = new Morpheme("P3sg", nounPossession);

    // Case suffixes

    // Nominal case suffix. It has no surface form (no letters). "elma = apple"
    Morpheme nom = new Morpheme("Nom", nounCase);
    // Dative case suffix. "elmaya = to apple"
    Morpheme dat = new Morpheme("Dat", nounCase);

    // Derivation suffixes

    // Diminutive suffix. Noun to Noun conversion. "elmacık = small apple, poor apple"
    Morpheme dim = new Morpheme("Dim", nounNounDerivation);

    //-------------- States ------------------------
    // _ST = Terminal state _SnT = Non Terminal State.
    // A terminal state means that a walk in the graph can end there.

    LexicalState noun_SnT = LexicalState.nonTerminal("noun_SnT", noun);

    // Number-Person agreement

    LexicalState a3sg_SnT = LexicalState.nonTerminal("a3sg_SnT", a3sg);
    LexicalState a3pl_SnT = LexicalState.nonTerminal("a3pl_SnT", a3pl);

    // Possessive

    LexicalState pnon_SnT = LexicalState.nonTerminal("pnon_SnT", pnon);
    LexicalState p1sg_SnT = LexicalState.nonTerminal("p1sg_SnT", p1sg);
    LexicalState p3sg_SnT = LexicalState.nonTerminal("p3sg_SnT", p3sg);

    // Case

    LexicalState nom_ST = LexicalState.terminal("nom_ST", nom);
    LexicalState nom_SnT = LexicalState.nonTerminal("nom_SnT", nom);
    LexicalState dat_ST = LexicalState.terminal("dat_ST", dat);

    // Derivation

    LexicalState dim_SnT = LexicalState.nonTerminal("dim_SnT", dim);

    /**
     * Turkish Nouns always have Noun-Person-Possession-Case morphemes.
     * Even there are no suffix characters.
     * elma -> Noun:elma - A3sg:ε - Pnon:ε - Nom:ε (Third person singular, No possession, Nominal Case)
     */
    public void addNounTransitions() {

        // ev-ε-?-?
        noun_SnT.newTransition(a3sg_SnT).empty().build();

        // ev-ler-?-?. Rejects inputs like "kitab-lar, burn-lar"
        noun_SnT.newTransition(a3pl_SnT)
                .surfaceTemplate("lAr")
                .addRule(rejectAny("dim-suffix"))
                .build();

        // ev-ε-ε-?
        a3sg_SnT.newTransition(pnon_SnT).empty().build();

        // ev-ε-im oda-ε-m
        a3sg_SnT.newTransition(p1sg_SnT)
                .surfaceTemplate("+Im")
                .addRule(rejectOnly("su-root"))
                .build();

        // su-ε-yum. Only for "su"
        a3sg_SnT.newTransition(p1sg_SnT)
                .surfaceTemplate("yum")
                .addRule(allowOnly("su-root"))
                .build();

        // ev-ε-i oda-ε-sı
        a3sg_SnT.newTransition(p3sg_SnT)
                .surfaceTemplate("+sI")
                .addRule(rejectOnly("su-root"))
                .build();

        // su-ε-yu. Only for "su"
        a3sg_SnT.newTransition(p3sg_SnT)
                .surfaceTemplate("yu")
                .addRule(allowOnly("su-root"))
                .build();

        // ev-ler-ε-?
        a3pl_SnT.newTransition(pnon_SnT).empty().build();

        // ev-ler-im-?
        a3pl_SnT.newTransition(p1sg_SnT).surfaceTemplate("Im").build();

        // ev-ler-i oda-lar-ı
        a3pl_SnT.newTransition(pnon_SnT).surfaceTemplate("I").build();

        // ev-?-ε-ε (ev, evler)
        pnon_SnT.newTransition(nom_ST)
                .build();

        // ev-ε-ε-ε-cik (evcik)
        // TODO: add morpheme rules.
        nom_ST.newTransition(dim_SnT)
                .surfaceTemplate(">cI~k")
                .build();

        // ev-ε-ε-ε-ceğiz (evceğiz)
        nom_ST.newTransition(dim_SnT)
                .surfaceTemplate("cAğIz")
                .build();

        // connect to the noun root. Reject dim suffix after this point.
        dim_SnT.newTransition(noun_SnT)
                .addRule(rejectAny("dim-suffix"))
                .build();

        // This is for blocking inputs like "kitab". Here because nominal case state is non terminal (nom_SnT)
        // analysis path will fail.
        pnon_SnT.newTransition(nom_SnT)
                .addRule(allowOnly(RuleNames.WovelExpecting))
                .build();

        // ev-?-ε-e (eve, evlere)
        pnon_SnT.newTransition(dat_ST).surfaceTemplate("+yA").build();

        // This transition is for words like "içeri" or "dışarı". Those words implicitly contains Dative suffix.
        // But It is also possible to add explicit dative suffix to those words such as "içeri-ye".
        pnon_SnT.newTransition(dat_ST)
                .addRule(allowOnly(RuleNames.ImplicitDative))
                .build();

        // ev-?-im-ε (evim, evlerim)
        p1sg_SnT.newTransition(nom_ST).build();

        // ev-?-im-e (evime, evlerime)
        p1sg_SnT.newTransition(dat_ST).surfaceTemplate("A").build();

        //ev-?-i-ε (evi, evleri)
        p3sg_SnT.newTransition(nom_SnT).build();

        //ev-?-i-ε (evine, evlerine)
        p3sg_SnT.newTransition(dat_ST).surfaceTemplate("nA").build();

    }

    public TurkishMorphotactics() {
        addNounTransitions();
    }

    public static void main(String[] args) {
        TurkishMorphotactics tm = new TurkishMorphotactics();
    }

}
