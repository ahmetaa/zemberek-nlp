package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

public class TurkishMorphotactics {

  //-------------- Morphemes ------------------------

  Morpheme root = new Morpheme("Root");

  Morpheme noun = new Morpheme("Noun");

  // Number-Person agreement.

  // Third person singular suffix. "elma = apple"
  Morpheme a3sg = new Morpheme("A3sg");
  // Third person plural suffix. "elma-lar = apples"
  Morpheme a3pl = new Morpheme("A3pl");

  // Possessive

  // No possession suffix. This is not a real morpheme but adds information to analysis. "elma = apple"
  Morpheme pnon = new Morpheme("Pnon");
  // First person singular possession suffix.  "elma-m = my apple"
  Morpheme p1sg = new Morpheme("P1sg");
  // Third person singular possession suffix. "elma-sı = his/her apple"
  Morpheme p3sg = new Morpheme("P3sg");

  // Case suffixes

  // Nominal case suffix. It has no surface form (no letters). "elma = apple"
  Morpheme nom = new Morpheme("Nom");
  // Dative case suffix. "elmaya = to apple"
  Morpheme dat = new Morpheme("Dat");

  // Derivation suffixes

  // Diminutive suffix. Noun to Noun conversion. "elmacık = small apple, poor apple"
  Morpheme dim = new Morpheme("Dim");

  //-------------- States ------------------------
  // _ST = Terminal state _SnT = Non Terminal State.
  // A terminal state means that a walk in the graph can end there.

  // root of the graph.
  MorphemeState root_SnT = MorphemeState.nonTerminal("root_Snt", root);

  MorphemeState noun_SnT = MorphemeState.nonTerminal("noun_SnT", noun);

  // Number-Person agreement

  MorphemeState a3sg_SnT = MorphemeState.nonTerminal("a3sg_SnT", a3sg);
  MorphemeState a3pl_SnT = MorphemeState.nonTerminal("a3pl_SnT", a3pl);

  // Possessive

  MorphemeState pnon_SnT = MorphemeState.nonTerminal("pnon_SnT", pnon);
  MorphemeState p1sg_SnT = MorphemeState.nonTerminal("p1sg_SnT", p1sg);
  MorphemeState p3sg_SnT = MorphemeState.nonTerminal("p3sg_SnT", p3sg);

  // Case

  MorphemeState nom_ST = MorphemeState.terminal("nom_ST", nom);
  MorphemeState nom_SnT = MorphemeState.nonTerminal("nom_SnT", nom);
  MorphemeState dat_ST = MorphemeState.terminal("dat_ST", dat);

  // Derivation

  MorphemeState dim_SnT = MorphemeState.nonTerminalDerivative("dim_SnT", dim);

  private RootLexicon lexicon;

  public TurkishMorphotactics(RootLexicon lexicon) {
    this.lexicon = lexicon;
    addNounTransitions();
  }

  /**
   * Turkish Nouns always have Noun-Person-Possession-Case morphemes. Even there are no suffix
   * characters. elma -> Noun:elma - A3sg:ε - Pnon:ε - Nom:ε (Third person singular, No possession,
   * Nominal Case)
   */
  public void addNounTransitions() {

    // ev-ε-?-?
    noun_SnT.transition(a3sg_SnT)
        .addRule(Rules.rejectIfContains(RootAttribute.ImplicitPlural))
        .add();

    // ev-ler-?-?.
    noun_SnT.transition(a3pl_SnT, "lAr")
        .addRule(Rules.rejectIfContains(RootAttribute.ImplicitPlural))
        .add();

    noun_SnT.transition(a3pl_SnT)
        .addRule(Rules.allowOnly(RootAttribute.ImplicitPlural))
        .add();

    // ev-ε-ε-?
    a3sg_SnT.transition(pnon_SnT).add();
    DictionaryItem suRoot = lexicon.getItemById("su_Noun");
    // ev-ε-im oda-ε-m
    a3sg_SnT.transition(p1sg_SnT, "Im")
        .addRule(Rules.rejectIfContains(suRoot))
        .add();
    // su-ε-yum. Only for "su"
    a3sg_SnT.transition(p1sg_SnT, "yum")
        .addRule(Rules.allowOnly(suRoot))
        .add();
    // ev-ε-i oda-ε-sı
    a3sg_SnT.transition(p3sg_SnT, "+sI")
        .addRule(Rules.rejectIfContains(suRoot))
        .add();
    // su-ε-yu. Only for "su"
    a3sg_SnT.transition(p3sg_SnT, "yu")
        .addRule(Rules.allowOnly(suRoot))
        .add();

    // ev-ler-ε-?
    a3pl_SnT.transition(pnon_SnT).add();
    // ev-ler-im-?
    a3pl_SnT.transition(p1sg_SnT, "Im").add();
    // ev-ler-i oda-lar-ı
    a3pl_SnT.transition(p3sg_SnT, "I").add();

    // ev-?-ε-ε (ev, evler)
    pnon_SnT.transition(nom_ST)
        .addRule(Rules.rejectIfContains(PhoneticAttribute.ExpectsVowel))
       // .addRule(Rules.rejectIfContains(RootAttribute.CompoundP3sgRoot))
        .add();

    // This transition is for not allowing inputs like "kitab" or "zeytinyağ".
    // They will fail because nominal case state is non terminal (nom_SnT)
    pnon_SnT.transition(nom_SnT)
        .addRule(Rules.allowOnly(PhoneticAttribute.ExpectsVowel))
      //  .addRule(Rules.allowOnly(RootAttribute.CompoundP3sgRoot))
        .add();
    // ev-?-ε-e (eve, evlere)
    pnon_SnT.transition(dat_ST).surfaceTemplate("+yA").add();
    // This transition is for words like "içeri" or "dışarı". Those words implicitly contains Dative suffix.
    // But It is also possible to add dative suffix +yA to those words such as "içeri-ye".
    pnon_SnT.transition(dat_ST)
        .addRule(Rules.allowOnly(RootAttribute.ImplicitDative))
        .add();

    // ev-?-im-ε (evim, evlerim)
    p1sg_SnT.transition(nom_ST).add();
    // ev-?-im-e (evime, evlerime)
    p1sg_SnT.transition(dat_ST, "A").add();

    //ev-?-i-ε (evi, evleri)
    p3sg_SnT.transition(nom_SnT).add();
    //ev-?-i-ε (evine, evlerine)
    p3sg_SnT.transition(dat_ST, "nA").add();

    // ev-ε-ε-ε-cik (evcik). Disallow this path if visitor contains dim suffix.
    // There are two almost identical suffix transitions with templates ">cI~k" and ">cI!ğ"
    // This was necessary for some simplification during analysis. This way there will be only one
    // surface form generated per transition.

    // do not allow repetition and only empty suffixes can come before.
    nom_ST.transition(dim_SnT, ">cI~k")
        .addRule(new Rules.RejectIfHasAnySuffixSurface())
        .add();
    nom_SnT.transition(dim_SnT, ">cI!ğ")
        .addRule(new Rules.RejectIfHasAnySuffixSurface())
        .add();

    // ev-ε-ε-ε-ceğiz (evceğiz)
    // TODO: consider making this a separate morpheme.
    nom_ST.transition(dim_SnT, "cAğIz")
        .addRule(new Rules.RejectIfHasAnySuffixSurface())
        .add();

    // connect dim to the noun root.
    dim_SnT.transition(noun_SnT).add();

  }

  public MorphemeState getRootState(DictionaryItem dictionaryItem) {
    switch (dictionaryItem.primaryPos) {
      case Noun:
        return noun_SnT;
      default:
        return noun_SnT;
    }
  }

}
