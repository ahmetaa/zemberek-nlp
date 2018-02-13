package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.contains;
import static zemberek.morphology.morphotactics.Conditions.notContains;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.HasAnySuffixSurface;

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
  MorphemeState nounCompoundRoot_SnT = MorphemeState.nonTerminal("nounCompoundRoot_SnT", noun);

  // Number-Person agreement

  MorphemeState a3sg_SnT = MorphemeState.nonTerminal("a3sg_SnT", a3sg);
  MorphemeState a3sgCompound_SnT = MorphemeState.nonTerminal("a3sgCompound_SnT", a3sg);
  MorphemeState a3pl_SnT = MorphemeState.nonTerminal("a3pl_SnT", a3pl);
  MorphemeState a3plCompound_SnT = MorphemeState.nonTerminal("a3plCompound_SnT", a3pl);

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
    noun_SnT.addEmpty(a3sg_SnT, notContains(RootAttribute.ImplicitPlural));

    // ev-ler-?-?.
    noun_SnT.add(
        a3pl_SnT, "lAr",
        notContains(RootAttribute.ImplicitPlural));

    // Allow only implicit plural `hayvanat`.
    noun_SnT.addEmpty(a3pl_SnT, contains(RootAttribute.ImplicitPlural));

    // for compound roots like "zeytinyağ-" generate two transitions
    // NounCompound--(ε)--> a3sgCompound --(ε)--> pNon_SnT
    nounCompoundRoot_SnT.addEmpty(
        a3sgCompound_SnT,
        contains(RootAttribute.CompoundP3sgRoot));

    a3sgCompound_SnT.addEmpty(pnon_SnT);

    // for compound roots like "zeytinyağ-lar-ı" generate two transition
    // NounCompound--(lAr)--> a3plCompound --(I)--> p3sg_SnT
    nounCompoundRoot_SnT.add(
        a3plCompound_SnT,
        "lar",
        contains(RootAttribute.CompoundP3sgRoot));

    a3plCompound_SnT
        .add(p3sg_SnT, "I")
        .add(p1sg_SnT, "Im");

    // ev-ε-ε-?
    a3sg_SnT.addEmpty(pnon_SnT);

    DictionaryItem suRoot = lexicon.getItemById("su_Noun");
    // ev-ε-im oda-ε-m
    a3sg_SnT.add(p1sg_SnT, "Im", notContains(suRoot));

    // su-ε-yum. Only for "su"
    a3sg_SnT.add(p1sg_SnT, "yum", contains(suRoot));

    // ev-ε-i oda-ε-sı
    a3sg_SnT.add(p3sg_SnT, "+sI", notContains(suRoot));

    // "zeytinyağı" has two analyses. Pnon and P3sg.
    a3sg_SnT.addEmpty(p3sg_SnT, contains(RootAttribute.CompoundP3sg));

    // su-ε-yu. Only for "su"
    a3sg_SnT.add(p3sg_SnT, "yu", contains(suRoot));

    // ev-ler-ε-?
    a3pl_SnT.addEmpty(pnon_SnT, notContains(RootAttribute.CompoundP3sg));

    // ev-ler-im-?
    a3pl_SnT.add(p1sg_SnT, "Im");
    // ev-ler-i oda-lar-ı

    a3pl_SnT.add(p3sg_SnT, "I");

    // ev-?-ε-ε (ev, evler).
    pnon_SnT.addEmpty(nom_ST, notContains(PhoneticAttribute.ExpectsVowel));

    // This transition is for not allowing inputs like "kitab" or "zeytinyağ".
    // They will fail because nominal case state is non terminal (nom_SnT)
    pnon_SnT.addEmpty(nom_SnT,
        contains(RootAttribute.CompoundP3sgRoot).or(contains(PhoneticAttribute.ExpectsVowel)));

    // ev-?-ε-e (eve, evlere). Not allow "zetinyağı-ya"
    pnon_SnT.add(dat_ST, "+yA", notContains(RootAttribute.CompoundP3sg));

    // zeytinyağı-ε-ε-na
    pnon_SnT.add(dat_ST, "+nA", contains(RootAttribute.CompoundP3sg));

    // This transition is for words like "içeri" or "dışarı". Those words implicitly contains Dative suffix.
    // But It is also possible to add dative suffix +yA to those words such as "içeri-ye".
    pnon_SnT.addEmpty(dat_ST, contains(RootAttribute.ImplicitDative));

    // ev-?-im-ε (evim, evlerim)
    p1sg_SnT.addEmpty(nom_ST);
    // ev-?-im-e (evime, evlerime)
    p1sg_SnT.add(dat_ST, "A");

    //ev-?-i-ε (evi, evleri)
    p3sg_SnT.addEmpty(nom_ST);
    //ev-?-i-ε (evine, evlerine)
    p3sg_SnT.add(dat_ST, "nA");

    // ev-ε-ε-ε-cik (evcik). Disallow this path if visitor contains dim suffix.
    // There are two almost identical suffix transitions with templates ">cI~k" and ">cI!ğ"
    // This was necessary for some simplification during analysis. This way there will be only one
    // surface form generated per transition.

    // do not allow repetition and only empty suffixes can come before.
    nom_ST.add(dim_SnT, ">cI~k", new HasAnySuffixSurface().not());

    nom_SnT.add(dim_SnT, ">cI!ğ", new HasAnySuffixSurface().not());

    // ev-ε-ε-ε-ceğiz (evceğiz)
    // TODO: consider making this a separate morpheme.
    nom_ST.add(dim_SnT, "cAğIz", new HasAnySuffixSurface().not());

    // connect dim to the noun root.
    dim_SnT.addEmpty(noun_SnT);

  }

  public MorphemeState getRootState(DictionaryItem dictionaryItem) {
    switch (dictionaryItem.primaryPos) {
      case Noun:
        if (dictionaryItem.hasAttribute(RootAttribute.CompoundP3sgRoot)) {
          return nounCompoundRoot_SnT;
        } else {
          return noun_SnT;
        }
      default:
        return noun_SnT;
    }
  }

}
