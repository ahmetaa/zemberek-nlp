package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.contains;
import static zemberek.morphology.morphotactics.Conditions.notContain;
import static zemberek.morphology.morphotactics.MorphemeState.builder;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminal;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminalDerivative;
import static zemberek.morphology.morphotactics.MorphemeState.terminal;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.CurrentGroupContains;
import zemberek.morphology.morphotactics.Conditions.NoSurfaceAfterDerivation;
import zemberek.morphology.morphotactics.Conditions.PreviousNonEmptyMorphemeIs;

public class TurkishMorphotactics {

  //-------------- Morphemes ------------------------

  Morpheme root = new Morpheme("Root", "Root");

  Morpheme noun = new Morpheme("Noun", "Noun", PrimaryPos.Noun);

  Morpheme adj = new Morpheme("Adjective", "Adj", PrimaryPos.Adjective);

  Morpheme verb = new Morpheme("Verb", "Verb", PrimaryPos.Verb);

  // Number-Person agreement.

  Morpheme a1sg = new Morpheme("FirstPersonSingular", "A1sg");
  // Third person singular suffix. "elma = apple"
  Morpheme a3sg = new Morpheme("ThirdPersonSingular", "A3sg");
  // Third person plural suffix. "elma-lar = apples"
  Morpheme a3pl = new Morpheme("ThirdPersonPlural", "A3pl");

  // Possessive

  // No possession suffix. This is not a real morpheme but adds information to analysis. "elma = apple"
  Morpheme pnon = new Morpheme("NoPosession", "Pnon");
  // First person singular possession suffix.  "elma-m = my apple"
  Morpheme p1sg = new Morpheme("FirstPersonSingularPosessive", "P1sg");
  // Third person singular possession suffix. "elma-sı = his/her apple"
  Morpheme p3sg = new Morpheme("ThirdPersonSingularPossesive", "P3sg");

  // Case suffixes

  // Nominal case suffix. It has no surface form (no letters). "elma = apple"
  Morpheme nom = new Morpheme("Nominal", "Nom");
  // Dative case suffix. "elmaya = to apple"
  Morpheme dat = new Morpheme("Dative", "Dat");
  // Accusative case suffix. "elmayı = ~the apple"
  Morpheme acc = new Morpheme("Accusative", "Acc");

  // Derivation suffixes

  // Diminutive suffix. Noun to Noun conversion. "elmacık = small apple, poor apple"
  Morpheme dim = new Morpheme("Diminutive", "Dim");
  // With suffix. Noun to Adjective conversion. "elmalı = with apple"
  Morpheme with = new Morpheme("With", "With");

  // Zero derivation
  Morpheme zero = new Morpheme("Zero", "Zero");


  // Present Tense
  Morpheme pres = new Morpheme("PresentTense", "Pres");
  Morpheme past = new Morpheme("PastTense", "Past");

  // Verb specific
  Morpheme cop = new Morpheme("Copula", "Cop");

  //-------------- States ----------------------------
  // _ST = Terminal state _SnT = Non Terminal State.
  // A terminal state means that a walk in the graph can end there.

  // root of the graph.
  MorphemeState root_SnT = nonTerminal("root_Snt", root);

  //-------------- Noun States ------------------------

  MorphemeState noun_SnT = builder("noun_SnT", noun).posRoot().build();
  MorphemeState nounCompoundRoot_SnT = builder("nounCompoundRoot_SnT", noun).posRoot().build();

  // Number-Person agreement

  MorphemeState a3sg_SnT = nonTerminal("a3sg_SnT", a3sg);
  MorphemeState a3sgCompound_SnT = nonTerminal("a3sgCompound_SnT", a3sg);
  MorphemeState a3pl_SnT = nonTerminal("a3pl_SnT", a3pl);
  MorphemeState a3plCompound_SnT = nonTerminal("a3plCompound_SnT", a3pl);

  // Possessive

  MorphemeState pnon_SnT = nonTerminal("pnon_SnT", pnon);
  MorphemeState pnonCompound_SnT = nonTerminal("pnonCompound_SnT", pnon);
  MorphemeState p1sg_SnT = nonTerminal("p1sg_SnT", p1sg);
  MorphemeState p3sg_SnT = nonTerminal("p3sg_SnT", p3sg);

  // Case

  MorphemeState nom_ST = terminal("nom_ST", nom);
  MorphemeState nom_SnT = nonTerminal("nom_SnT", nom);

  MorphemeState dat_ST = terminal("dat_ST", dat);
  MorphemeState acc_ST = terminal("acc_ST", acc);

  // Derivation

  MorphemeState dim_SnT = nonTerminalDerivative("dim_SnT", dim);

  MorphemeState with_SnT = nonTerminalDerivative("with_SnT", with);

  MorphemeState nounZeroDeriv_SnT = nonTerminalDerivative("nounZeroDeriv_SnT", zero);

  //-------------- Adjective States ------------------------

  MorphemeState adj_ST = terminal("adj_ST", adj);

  MorphemeState adjZeroDeriv_SnT = nonTerminalDerivative("adjZeroDeriv_SnT", zero);

  //-------------- Adjective-Noun connected Verb States ------------------------

  MorphemeState nVerb_SnT = builder("NVerb_SnT", verb).posRoot().build();
  MorphemeState nPresent_SnT = nonTerminal("NPresent_SnT", pres);
  MorphemeState nPast_SnT = nonTerminal("NPast_SnT", past);
  MorphemeState nA1sg_ST = terminal("NA1sg_ST", a1sg);
  MorphemeState nA3sg_ST = terminal("NA3sg_ST", a3sg);
  MorphemeState nA3sg_SnT = terminal("NA3sg_SnT", a3sg);
  MorphemeState nCop_ST = terminal("NCop_ST", cop);

  //-------------- Conditions ------------------------------

  private RootLexicon lexicon;

  public TurkishMorphotactics(RootLexicon lexicon) {
    this.lexicon = lexicon;
    connectNounStates();
    connectAdjectiveStates();
    connectVerbAfterNounAdjStates();
  }

  /**
   * Turkish Nouns always have Noun-Person-Possession-Case morphemes. Even there are no suffix
   * characters. elma -> Noun:elma - A3sg:ε - Pnon:ε - Nom:ε (Third person singular, No possession,
   * Nominal Case)
   */
  public void connectNounStates() {

    // ev-ε-?-?
    noun_SnT.addEmpty(a3sg_SnT, notContain(RootAttribute.ImplicitPlural));

    // ev-ler-?-?.
    noun_SnT.add(a3pl_SnT, "lAr",
        notContain(RootAttribute.ImplicitPlural)
            .and(notContain(RootAttribute.CompoundP3sg)));

    // Allow only implicit plural `hayvanat`.
    noun_SnT.addEmpty(a3pl_SnT, contains(RootAttribute.ImplicitPlural));

    // for compound roots like "zeytinyağ-" generate two transitions
    // NounCompound--(ε)--> a3sgCompound --(ε)--> pNonCompound_SnT --> Nom_SnT
    nounCompoundRoot_SnT.addEmpty(
        a3sgCompound_SnT,
        contains(RootAttribute.CompoundP3sgRoot));

    a3sgCompound_SnT.addEmpty(pnonCompound_SnT);
    pnonCompound_SnT.addEmpty(nom_SnT);

    // for compound roots like "zeytinyağ-lar-ı" generate two transition
    // NounCompound--(lAr)--> a3plCompound ---> p3sg_SnT, P1sg etc.
    nounCompoundRoot_SnT.add(
        a3plCompound_SnT,
        "lar",
        contains(RootAttribute.CompoundP3sgRoot));

    a3plCompound_SnT
        .add(p3sg_SnT, "I")
        .add(p1sg_SnT, "Im");

    // ev-ε-ε-? Reject "annemler" etc.
    a3sg_SnT.addEmpty(pnon_SnT, notContain(RootAttribute.FamilyMember));

    DictionaryItem suRoot = lexicon.getItemById("su_Noun");
    // ev-ε-im oda-ε-m
    a3sg_SnT.add(p1sg_SnT, "Im",
        notContain(suRoot).and(notContain(RootAttribute.FamilyMember)));

    // su-ε-yum. Only for "su"
    a3sg_SnT.add(p1sg_SnT, "yum", contains(suRoot));

    // ev-ε-i oda-ε-sı
    a3sg_SnT.add(p3sg_SnT, "+sI",
        notContain(suRoot).and(notContain(RootAttribute.FamilyMember)));

    // "zeytinyağı" has two analyses. Pnon and P3sg.
    a3sg_SnT.addEmpty(p3sg_SnT, contains(RootAttribute.CompoundP3sg));

    // su-ε-yu. Only for "su"
    a3sg_SnT.add(p3sg_SnT, "yu", contains(suRoot));

    // ev-ler-ε-?
    a3pl_SnT.addEmpty(pnon_SnT, notContain(RootAttribute.FamilyMember));

    // ev-ler-im-?
    a3pl_SnT.add(p1sg_SnT, "Im", notContain(RootAttribute.FamilyMember));
    // for words like "annemler".
    a3pl_SnT.addEmpty(p1sg_SnT, contains(RootAttribute.ImplicitP1sg));

    // ev-ler-i oda-lar-ı
    a3pl_SnT.add(p3sg_SnT, "I", notContain(RootAttribute.FamilyMember));

    // ev-?-ε-ε (ev, evler).
    pnon_SnT.addEmpty(nom_ST,
        notContain(PhoneticAttribute.ExpectsVowel)
            .and(notContain(RootAttribute.CompoundP3sgRoot))
            .and(notContain(RootAttribute.FamilyMember)));

    // This transition is for not allowing inputs like "kitab" or "zeytinyağ".
    // They will fail because nominal case state is non terminal (nom_SnT)
    pnon_SnT.addEmpty(nom_SnT,
        contains(RootAttribute.CompoundP3sgRoot)
            .or(contains(PhoneticAttribute.ExpectsVowel)));

    // ev-?-ε-e (eve, evlere). Not allow "zetinyağı-ya"
    pnon_SnT.add(dat_ST, "+yA", notContain(RootAttribute.CompoundP3sg));

    // zeytinyağı-ε-ε-na
    pnon_SnT.add(dat_ST, "+nA", contains(RootAttribute.CompoundP3sg));

    // ev-?-ε-e (ev-i, ev-ler-i, e-vim-i). Not allow "zetinyağı-yı"
    pnon_SnT.add(acc_ST, "+yI", notContain(RootAttribute.CompoundP3sg));

    // zeytinyağı-ε-ε-nı
    pnon_SnT.add(acc_ST, "+nI", contains(RootAttribute.CompoundP3sg));

    // This transition is for words like "içeri" or "dışarı". Those words implicitly contains Dative suffix.
    // But It is also possible to add dative suffix +yA to those words such as "içeri-ye".
    pnon_SnT.addEmpty(dat_ST, contains(RootAttribute.ImplicitDative));

    // ev-?-im-ε (evim, evlerim)
    p1sg_SnT.addEmpty(nom_ST);
    // ev-?-im-e (evime, evlerime)
    p1sg_SnT.add(dat_ST, "A");
    // ev-?-im-i (ev-i, ev-ler-i, e-vim-i).
    p1sg_SnT.add(acc_ST, "I");

    //ev-?-i-ε (evi, evleri)
    p3sg_SnT.addEmpty(nom_ST);
    //ev-?-i-ε (evine, evlerine)
    p3sg_SnT.add(dat_ST, "nA");
    //ev-?-i-ε (ev-i-ni, ev-ler-i-ni)
    p3sg_SnT.add(acc_ST, "nI");

    // ev-ε-ε-ε-cik (evcik). Disallow this path if visitor contains any non empty surface suffix.
    // There are two almost identical suffix transitions with templates ">cI~k" and ">cI!ğ"
    // This was necessary for some simplification during analysis. This way there will be only one
    // surface form generated per transition.
    nom_ST.add(dim_SnT, ">cI~k", Conditions.HAS_NO_SURFACE);
    nom_ST.add(dim_SnT, ">cI!ğ", Conditions.HAS_NO_SURFACE);

    // ev-ε-ε-ε-ceğiz (evceğiz)
    nom_ST.add(dim_SnT, "cAğIz", Conditions.HAS_NO_SURFACE);

    // connect dim to the noun root.
    dim_SnT.addEmpty(noun_SnT);

    // here we do not allow an adjective to pass here.
    // such as, adj->zero->noun->ε-ε-ε->zero->Verb is not acceptable because there is already a
    // adj->zero->Verb path.
    Condition noun2VerbZeroDerivationCondition = Conditions.HAS_TAIL
        .andNot(Conditions.CURRENT_GROUP_EMPTY.and(
            new Conditions.LastDerivationIs(adjZeroDeriv_SnT)));
    nom_ST.addEmpty(nounZeroDeriv_SnT, noun2VerbZeroDerivationCondition);

    // elma-ya-yım elma-ya-ydı
    dat_ST.addEmpty(nounZeroDeriv_SnT, noun2VerbZeroDerivationCondition);

    nounZeroDeriv_SnT.addEmpty(nVerb_SnT);

    // meyve-li
    nom_ST.add(with_SnT, "lI",
        new NoSurfaceAfterDerivation().and(
            new PreviousNonEmptyMorphemeIs(with_SnT).not()));

    // connect With to Adjective root.
    with_SnT.addEmpty(adj_ST);

  }

  private void connectAdjectiveStates() {

    // zero morpheme derivation. Words like "yeşil-i" requires Adj to Noun conversion.
    // Since noun suffixes are not derivational a "Zero" morpheme is used for this.
    // It has a HAS_TAIL condition because empty Noun derivation
    // Adj->(A3sg+Pnon+Nom) is not allowed.
    adj_ST.addEmpty(adjZeroDeriv_SnT, Conditions.HAS_TAIL);

    adjZeroDeriv_SnT.addEmpty(noun_SnT);

    adjZeroDeriv_SnT.addEmpty(nVerb_SnT);
  }

  private void connectVerbAfterNounAdjStates() {

    //elma-..-ε-yım
    nVerb_SnT.addEmpty(nPresent_SnT);

    // elma-ydı, çorap-tı
    nVerb_SnT.add(nPast_SnT, "+y>dI");

    // elma-yım
    nPresent_SnT.add(nA1sg_ST, "+yIm",
        notContain(RootAttribute.FamilyMember));
    // elma-ε-ε-dır non termınal.
    nPresent_SnT.addEmpty(nA3sg_SnT);

    // elma-ydı-m
    nPast_SnT.add(nA1sg_ST, "m");
    // elma-ydı-ε
    nPast_SnT.addEmpty(nA3sg_ST);

    // for not allowing "elma-ydı-m-dır"
    Condition rejectNoCopula = new CurrentGroupContains(nPast_SnT).not();

    //elma-yım-dır
    nA1sg_ST.add(nCop_ST, "dIr", rejectNoCopula);

    nA3sg_SnT.add(nCop_ST, ">dIr", rejectNoCopula);

  }


  public MorphemeState getRootState(DictionaryItem dictionaryItem) {
    switch (dictionaryItem.primaryPos) {
      case Noun:
        if (dictionaryItem.hasAttribute(RootAttribute.CompoundP3sgRoot)) {
          return nounCompoundRoot_SnT;
        } else {
          return noun_SnT;
        }
      case Adjective:
        return adj_ST;
      default:
        return noun_SnT;
    }
  }

}
