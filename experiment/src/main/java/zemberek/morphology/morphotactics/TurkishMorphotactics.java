package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.has;
import static zemberek.morphology.morphotactics.Conditions.notHave;
import static zemberek.morphology.morphotactics.Conditions.notHaveAny;
import static zemberek.morphology.morphotactics.Conditions.rootIs;
import static zemberek.morphology.morphotactics.Conditions.rootIsAny;
import static zemberek.morphology.morphotactics.Conditions.rootIsNone;
import static zemberek.morphology.morphotactics.Conditions.rootIsNot;
import static zemberek.morphology.morphotactics.MorphemeState.builder;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminal;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminalDerivative;
import static zemberek.morphology.morphotactics.MorphemeState.terminal;

import java.util.EnumSet;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.ContainsMorpheme;
import zemberek.morphology.morphotactics.Conditions.CurrentGroupContains;
import zemberek.morphology.morphotactics.Conditions.NoSurfaceAfterDerivation;

public class TurkishMorphotactics {

  public static final Morpheme root = new Morpheme("Root", "Root");
  public static final Morpheme noun = new Morpheme("Noun", "Noun", PrimaryPos.Noun);
  public static final Morpheme adj = new Morpheme("Adjective", "Adj", PrimaryPos.Adjective);
  public static final Morpheme verb = new Morpheme("Verb", "Verb", PrimaryPos.Verb);
  public static final Morpheme pron = new Morpheme("Pronoun", "Pron", PrimaryPos.Pronoun);
  public static final Morpheme adv = new Morpheme("Adverb", "Adv", PrimaryPos.Adverb);
  public static final Morpheme conj = new Morpheme("Conjunction", "Conj", PrimaryPos.Conjunction);

  // Number-Person agreement.

  public static final Morpheme a1sg = new Morpheme("FirstPersonSingular", "A1sg");
  public static final Morpheme a2sg = new Morpheme("SecondPersonSingular", "A2sg");
  public static final Morpheme a3sg = new Morpheme("ThirdPersonSingular", "A3sg");
  public static final Morpheme a1pl = new Morpheme("FirstPersonPlural", "A1pl");
  public static final Morpheme a2pl = new Morpheme("SecondPersonPlural", "A2pl");
  public static final Morpheme a3pl = new Morpheme("ThirdPersonPlural", "A3pl");

  // Possessive

  // No possession suffix. This is not a real Morpheme but adds information to analysis. "elma = apple"
  public static final Morpheme pnon = new Morpheme("NoPosession", "Pnon");

  // First person singular possession suffix.  "elma-m = my apple"
  public static final Morpheme p1sg = new Morpheme("FirstPersonSingularPossessive", "P1sg");

  public static final Morpheme p2sg = new Morpheme("SecondPersonSingularPossessive", "P2sg");

  // Third person singular possession suffix. "elma-sı = his/her apple"
  public static final Morpheme p3sg = new Morpheme("ThirdPersonSingularPossessive", "P3sg");

  // First person plural possession suffix.
  public static final Morpheme p1pl = new Morpheme("FirstPersonPluralPossessive", "P1pl");

  public static final Morpheme p2pl = new Morpheme("SecondPersonPluralPossessive", "P2pl");

  public static final Morpheme p3pl = new Morpheme("ThirdPersonPluralPossessive", "P3pl");

  // Case suffixes

  // Nominal case suffix. It has no surface form (no letters). "elma = apple"
  public static final Morpheme nom = new Morpheme("Nominal", "Nom");
  // Dative case suffix. "elmaya = to apple"
  public static final Morpheme dat = new Morpheme("Dative", "Dat");
  // Accusative case suffix. "elmayı = ~the apple"
  public static final Morpheme acc = new Morpheme("Accusative", "Acc");

  // Derivation suffixes

  // Diminutive suffix. Noun to Noun conversion. "elmacık = small apple, poor apple"
  public static final Morpheme dim = new Morpheme("Diminutive", "Dim");
  // With suffix. Noun to Adjective conversion. "elmalı = with apple"
  public static final Morpheme with = new Morpheme("With", "With");

  public static final Morpheme justLike = new Morpheme("JustLike", "JustLike");

  // Zero derivation
  public static final Morpheme zero = new Morpheme("Zero", "Zero");

  // Verb specific
  public static final Morpheme cop = new Morpheme("Copula", "Cop");

  // Negative Verb
  public static final Morpheme neg = new Morpheme("Negative", "Neg");

  // Tense
  public static final Morpheme pres = new Morpheme("PresentTense", "Pres");
  public static final Morpheme past = new Morpheme("PastTense", "Past");
  public static final Morpheme narr = new Morpheme("NarrativeTense", "Narr");
  public static final Morpheme cond = new Morpheme("Condition", "Cond");
  public static final Morpheme prog1 = new Morpheme("Progressive1", "Prog1");
  public static final Morpheme aor = new Morpheme("Aorist", "Aor");

  // Verb
  public static final Morpheme imp = new Morpheme("Imparative", "Imp");
  public static final Morpheme caus = new Morpheme("Causative", "Caus");

  //-------------- States ----------------------------
  // _ST = Terminal state _S = Non Terminal State.
  // A terminal state means that a walk in the graph can end there.

  // root of the graph.
  MorphemeState root_S = nonTerminal("root_S", root);

  //-------------- Noun States ------------------------

  MorphemeState noun_S = builder("noun_S", noun).posRoot().build();
  MorphemeState nounCompoundRoot_S =
      builder("nounCompoundRoot_S", noun).posRoot().build();

  // Number-Person agreement

  MorphemeState a3sg_S = nonTerminal("a3sg_S", a3sg);
  MorphemeState a3sgCompound_S = nonTerminal("a3sgCompound_S", a3sg);
  MorphemeState a3pl_S = nonTerminal("a3pl_S", a3pl);
  MorphemeState a3plCompound_S = nonTerminal("a3plCompound_S", a3pl);

  // Possessive

  MorphemeState pnon_S = nonTerminal("pnon_S", pnon);
  MorphemeState pnonCompound_S = nonTerminal("pnonCompound_S", pnon);
  MorphemeState p1sg_S = nonTerminal("p1sg_S", p1sg);
  MorphemeState p2sg_S = nonTerminal("p2sg_S", p2sg);
  MorphemeState p3sg_S = nonTerminal("p3sg_S", p3sg);
  MorphemeState p1pl_S = nonTerminal("p1pl_S", p1pl);

  // Case

  MorphemeState nom_ST = terminal("nom_ST", nom);
  MorphemeState nom_S = nonTerminal("nom_S", nom);

  MorphemeState dat_ST = terminal("dat_ST", dat);
  MorphemeState acc_ST = terminal("acc_ST", acc);

  // Derivation

  MorphemeState dim_S = nonTerminalDerivative("dim_S", dim);
  MorphemeState with_S = nonTerminalDerivative("with_S", with);
  MorphemeState justLike_S = nonTerminalDerivative("justLike_S", justLike);
  MorphemeState nounZeroDeriv_S = nonTerminalDerivative("nounZeroDeriv_S", zero);

  //-------------- Conditions ------------------------------

  private RootLexicon lexicon;

  public TurkishMorphotactics(RootLexicon lexicon) {
    this.lexicon = lexicon;
    connectNounStates();
    connectAdjectiveStates();
    connectVerbAfterNounAdjStates();
    connectPronounStates();
    connectVerbs();
  }

  /**
   * Turkish Nouns always have Noun-Person-Possession-Case morphemes.  Even there are no suffix
   * characters. elma -> Noun:elma - A3sg:ε - Pnon:ε - Nom:ε (Third person singular, No possession,
   * Nominal Case)
   */
  public void connectNounStates() {

    // ev-ε-?-?
    noun_S.addEmpty(a3sg_S, notHave(RootAttribute.ImplicitPlural));

    // ev-ler-?-?.
    noun_S.add(a3pl_S, "lAr",
        notHave(RootAttribute.ImplicitPlural)
            .and(notHave(RootAttribute.CompoundP3sg)));

    // Allow only implicit plural `hayvanat`.
    noun_S.addEmpty(a3pl_S, has(RootAttribute.ImplicitPlural));

    // for compound roots like "zeytinyağ-" generate two transitions
    // NounCompound--(ε)--> a3sgCompound --(ε)--> pNonCompound_S --> Nom_S
    nounCompoundRoot_S.addEmpty(
        a3sgCompound_S,
        has(RootAttribute.CompoundP3sgRoot));

    a3sgCompound_S.addEmpty(pnonCompound_S);
    pnonCompound_S.addEmpty(nom_S);

    // for compound roots like "zeytinyağ-lar-ı" generate two transition
    // NounCompound--(lAr)--> a3plCompound ---> p3sg_S, P1sg etc.
    nounCompoundRoot_S.add(
        a3plCompound_S,
        "lar",
        has(RootAttribute.CompoundP3sgRoot));

    a3plCompound_S
        .add(p3sg_S, "I")
        .add(p2sg_S, "In")
        .add(p1sg_S, "Im");

    DictionaryItem su = lexicon.getItemById("su_Noun");
    Condition noFamily = notHave(RootAttribute.FamilyMember);
    Condition defaultPossCond = rootIsNot(su).and(noFamily);

    // ev-ε-ε-? Reject "annemler" etc.
    a3sg_S.addEmpty(pnon_S, noFamily);

    // ev-ε-im oda-ε-m
    a3sg_S.add(p1sg_S, "Im", defaultPossCond);
    // su-ε-yum. 
    a3sg_S.add(p1sg_S, "yum", rootIs(su));

    // ev-ε-im oda-ε-m
    a3sg_S.add(p2sg_S, "In", defaultPossCond);
    // su-ε-yun. 
    a3sg_S.add(p2sg_S, "yun", rootIs(su));

    // ev-ε-i oda-ε-sı
    a3sg_S.add(p3sg_S, "+sI", defaultPossCond);
    // "zeytinyağı" has two analyses. Pnon and P3sg.
    a3sg_S.addEmpty(p3sg_S, has(RootAttribute.CompoundP3sg));
    // su-ε-yu. Only for "su"
    a3sg_S.add(p3sg_S, "yu", rootIs(su));

    a3sg_S.add(p1pl_S, "ImIz", defaultPossCond);
    a3sg_S.add(p1pl_S, "yumuz", rootIs(su));

    // ev-ler-ε-?
    a3pl_S.addEmpty(pnon_S, noFamily);

    // ev-ler-im-?
    a3pl_S.add(p1sg_S, "Im", noFamily);
    a3pl_S.add(p2sg_S, "In", noFamily);

    // for words like "annemler" and "annenler"
    a3pl_S.addEmpty(p1sg_S, has(RootAttribute.ImplicitP1sg));
    a3pl_S.addEmpty(p2sg_S, has(RootAttribute.ImplicitP2sg));

    // ev-ler-i oda-lar-ı
    a3pl_S.add(p3sg_S, "I", noFamily);

    a3pl_S.add(p1pl_S, "ImIz", noFamily);

    // ev-?-ε-ε (ev, evler).
    pnon_S.addEmpty(nom_ST,
        notHave(PhoneticAttribute.ExpectsVowel)
            .and(notHaveAny(RootAttribute.CompoundP3sgRoot, RootAttribute.FamilyMember)));

    // This transition is for not allowing inputs like "kitab" or "zeytinyağ".
    // They will fail because nominal case state is non terminal (nom_S)
    pnon_S.addEmpty(nom_S,
        has(RootAttribute.CompoundP3sgRoot)
            .or(has(PhoneticAttribute.ExpectsVowel)));

    // ev-?-ε-e (eve, evlere). Not allow "zetinyağı-ya"
    pnon_S.add(dat_ST, "+yA", notHave(RootAttribute.CompoundP3sg));

    // zeytinyağı-ε-ε-na
    pnon_S.add(dat_ST, "+nA", has(RootAttribute.CompoundP3sg));

    // ev-?-ε-e (ev-i, ev-ler-i, e-vim-i). Not allow "zetinyağı-yı"
    pnon_S.add(acc_ST, "+yI", notHave(RootAttribute.CompoundP3sg));

    // zeytinyağı-ε-ε-nı
    pnon_S.add(acc_ST, "+nI", has(RootAttribute.CompoundP3sg));

    // This transition is for words like "içeri" or "dışarı".
    // Those words implicitly contains Dative suffix.
    // But It is also possible to add dative suffix +yA to those words such as "içeri-ye".
    pnon_S.addEmpty(dat_ST, has(RootAttribute.ImplicitDative));

    // ev-?-im-ε (evim, evlerim)
    p1sg_S.addEmpty(nom_ST);
    // ev-?-im-e (evime, evlerime)
    p1sg_S.add(dat_ST, "A");
    // ev-?-im-i (ev-i, ev-ler-i, e-vim-i).
    p1sg_S.add(acc_ST, "I");

    // evin, evlerin
    p2sg_S.addEmpty(nom_ST);
    // evine, evlerine
    p2sg_S.add(dat_ST, "A");
    // evini
    p2sg_S.add(acc_ST, "I");

    //ev-?-i-ε (evi, evleri)
    p3sg_S.addEmpty(nom_ST);
    //ev-?-i-ε (evine, evlerine)
    p3sg_S.add(dat_ST, "nA");
    //ev-?-i-ε (ev-i-ni, ev-ler-i-ni)
    p3sg_S.add(acc_ST, "nI");

    // evimiz
    p1pl_S.addEmpty(nom_ST);
    // evimize
    p1pl_S.add(dat_ST, "A");
    // evimizi
    p1pl_S.add(acc_ST, "I");

    // ev-ε-ε-ε-cik (evcik). Disallow this path if visitor contains any non empty surface suffix.
    // There are two almost identical suffix transitions with templates ">cI~k" and ">cI!ğ"
    // This was necessary for some simplification during analysis. This way there will be only one
    // surface form generated per transition.
    nom_ST.add(dim_S, ">cI~k", Conditions.HAS_NO_SURFACE);
    nom_ST.add(dim_S, ">cI!ğ", Conditions.HAS_NO_SURFACE);

    // ev-ε-ε-ε-ceğiz (evceğiz)
    nom_ST.add(dim_S, "cAğIz", Conditions.HAS_NO_SURFACE);

    // connect dim to the noun root.
    dim_S.addEmpty(noun_S);

    // here we do not allow an adjective to pass here.
    // such as, adj->zero->noun->ε-ε-ε->zero->Verb is not acceptable because there is already a
    // adj->zero->Verb path.
    Condition noun2VerbZeroDerivationCondition = Conditions.HAS_TAIL
        .andNot(Conditions.CURRENT_GROUP_EMPTY
            .and(new Conditions.LastDerivationIs(adjZeroDeriv_S)));
    nom_ST.addEmpty(nounZeroDeriv_S, noun2VerbZeroDerivationCondition);

    // elma-ya-yım elma-ya-ydı
    dat_ST.addEmpty(nounZeroDeriv_S, noun2VerbZeroDerivationCondition);

    nounZeroDeriv_S.addEmpty(nVerb_S);

    // meyve-li
    nom_ST.add(with_S, "lI",
        new NoSurfaceAfterDerivation()
            .and(new ContainsMorpheme(with).not()));

    nom_ST.add(justLike_S, "+msI",
        new NoSurfaceAfterDerivation()
            .and(new ContainsMorpheme(justLike, adj).not()));

    nom_ST.add(justLike_S, "ImsI",
        notHave(PhoneticAttribute.LastLetterVowel)
            .and(new NoSurfaceAfterDerivation())
            .and(new ContainsMorpheme(justLike, adj).not()));

    // connect With to Adjective root.
    with_S.addEmpty(adj_ST);

    justLike_S.addEmpty(adj_ST);

  }

  //-------------- Adjective States ------------------------

  MorphemeState adj_ST = terminal("adj_ST", adj);

  MorphemeState adjZeroDeriv_S = nonTerminalDerivative("adjZeroDeriv_S", zero);

  private void connectAdjectiveStates() {

    // zero morpheme derivation. Words like "yeşil-i" requires Adj to Noun conversion.
    // Since noun suffixes are not derivational a "Zero" morpheme is used for this.
    // Transition has a HAS_TAIL condition because Adj->Zero->Noun+A3sg+Pnon+Nom) is not allowed.
    adj_ST.addEmpty(adjZeroDeriv_S, Conditions.HAS_TAIL);

    adjZeroDeriv_S.addEmpty(noun_S);

    adjZeroDeriv_S.addEmpty(nVerb_S);

    adj_ST.add(justLike_S, "+msI",
        new NoSurfaceAfterDerivation()
            .and(new ContainsMorpheme(justLike).not()));

    adj_ST.add(justLike_S, "ImsI",
        notHave(PhoneticAttribute.LastLetterVowel)
            .and(new NoSurfaceAfterDerivation())
            .and(new ContainsMorpheme(justLike).not()));
  }

  //-------------- Adjective-Noun connected Verb States ------------------------

  MorphemeState nVerb_S = builder("nVerb_S", verb).posRoot().build();
  MorphemeState nVerbDegil_S = builder("nVerbDegil_S", verb).posRoot().build();

  MorphemeState nPresent_S = nonTerminal("nPresent_S", pres);
  MorphemeState nPast_S = nonTerminal("nPast_S", past);
  MorphemeState nNarr_S = nonTerminal("nNarr_S", narr);
  MorphemeState nCond_S = nonTerminal("nCond_S", cond);
  MorphemeState nA1sg_ST = terminal("nA1sg_ST", a1sg);
  MorphemeState nA2sg_ST = terminal("nA2sg_ST", a2sg);
  MorphemeState nA1pl_ST = terminal("nA1pl_ST", a1pl);
  MorphemeState nA3sg_ST = terminal("nA3sg_ST", a3sg);
  MorphemeState nA3sg_S = nonTerminal("nA3sg_S", a3sg);
  MorphemeState nA3pl_ST = terminal("nA3pl_ST", a3pl);
  MorphemeState nCop_ST = terminal("nCop_ST", cop);

  MorphemeState nNeg_S = nonTerminal("nNeg_S", neg);

  private void connectVerbAfterNounAdjStates() {

    //elma-..-ε-yım
    nVerb_S.addEmpty(nPresent_S);
    // elma-ydı, çorap-tı
    nVerb_S.add(nPast_S, "+y>dI");
    // elma-ymış
    nVerb_S.add(nNarr_S, "+ymIş");

    // word "değil" is special. It contains negative suffix implicitly. Also it behaves like
    // noun->Verb Zero morpheme derivation. because it cannot have most Verb suffixes.
    // So we connect it to a separate root state "nVerbDegil" instead of Verb
    DictionaryItem degilRoot = lexicon.getItemById("değil_Verb");
    nVerbDegil_S.addEmpty(nNeg_S, rootIs(degilRoot));
    // copy transitions from nVerb_S
    nNeg_S.copyOutgoingTransitionsFrom(nVerb_S);

    Condition noFamily = notHave(RootAttribute.FamilyMember);
    // for preventing elmamım, elmamdım
    // pP1sg_S, pDat_ST, pA1sg_S, pA1pl_S, pA3pl_S, pP2sg_S, pP1pl_S, pP3sg_S, pP1sg_S
    Condition allowA1sgTrans =
        notHave(RootAttribute.FamilyMember)
            .andNot(Conditions.rootPrimaryPos(PrimaryPos.Pronoun))
            .andNot(new Conditions.PreviousGroupContains(p1sg_S));
    Condition allowA2sgTrans =
        notHave(RootAttribute.FamilyMember)
            .andNot(Conditions.rootPrimaryPos(PrimaryPos.Pronoun))
            .andNot(new Conditions.PreviousGroupContains(p2sg_S));
    // TODO: add p3pl once implemented
    Condition allowA3plTrans =
        notHave(RootAttribute.FamilyMember)
            .andNot(Conditions.rootPrimaryPos(PrimaryPos.Pronoun))
            .andNot(new Conditions.PreviousGroupContains(a3pl_S, a3plCompound_S));
    Condition allowA1plTrans =
        notHave(RootAttribute.FamilyMember)
            .andNot(Conditions.rootPrimaryPos(PrimaryPos.Pronoun))
            .andNot(new Conditions.PreviousGroupContains(p1pl_S));
    // elma-yım
    nPresent_S.add(nA1sg_ST, "+yIm", allowA1sgTrans);

    // elma-ε-ε-dır to non terminal A3sg. We do not allow ending with A3sg from empty Present tense.
    nPresent_S.addEmpty(nA3sg_S);

    nPresent_S.add(nCond_S, "+ysA");

    // we allow `değil` to end with terminal A3sg from Present tense.
    nPresent_S.addEmpty(nA3sg_ST, rootIs(degilRoot));

    // elma-lar, elma-da-lar as Verb.
    nPresent_S.add(nA3pl_ST, "lAr",
        notHave(RootAttribute.CompoundP3sg)
            .and(allowA3plTrans));

    // elma-ydı-m. Do not allow "elmaya-yım" (Oflazer accepts this)
    nPast_S.add(nA1sg_ST, "m", allowA1sgTrans);
    nNarr_S.add(nA1sg_ST, "Im", allowA1sgTrans);

    nPast_S.add(nA2sg_ST, "n", allowA2sgTrans);
    nNarr_S.add(nA2sg_ST, "sIn", allowA2sgTrans);

    nPast_S.add(nA1pl_ST, "k", allowA1plTrans);
    nNarr_S.add(nA1pl_ST, "Iz", allowA1plTrans);
    nPresent_S.add(nA1pl_ST, "+yIz", allowA1plTrans);

    // elma-ydı-lar.
    nPast_S.add(nA3pl_ST, "lAr",
        notHave(RootAttribute.CompoundP3sg)
            .and(allowA3plTrans));
    // elma-ymış-lar.
    nNarr_S.add(nA3pl_ST, "lAr",
        notHave(RootAttribute.CompoundP3sg)
            .and(allowA3plTrans));

    // elma-ydı-ε
    nPast_S.addEmpty(nA3sg_ST);
    // elma-ymış-ε
    nNarr_S.addEmpty(nA3sg_ST);

    // narr+cons is allowed but not past+cond
    nNarr_S.add(nCond_S, "sA");

    nCond_S.add(nA1sg_ST, "m", allowA1sgTrans);
    nCond_S.add(nA1pl_ST, "k", allowA1plTrans);
    nCond_S.addEmpty(nA3sg_ST);
    nCond_S.add(nA3pl_ST, "lAr");

    // for not allowing "elma-ydı-m-dır"
    Condition rejectNoCopula = new CurrentGroupContains(nPast_S, nCond_S).not();

    //elma-yım-dır
    nA1sg_ST.add(nCop_ST, "dIr", rejectNoCopula);
    nA1pl_ST.add(nCop_ST, "dIr", rejectNoCopula);

    nA3sg_S.add(nCop_ST, ">dIr", rejectNoCopula);

    nA3pl_ST.add(nCop_ST, "dIr", rejectNoCopula);
  }

  // ----------- Pronoun states --------------------------

  // Pronouns have states similar with Nouns.
  MorphemeState pronPers_S = builder("pronPers_S", pron).posRoot().build();

  MorphemeState pronDemons_S = builder("pronDemons_S", pron).posRoot().build();
  public MorphemeState pronQuant_S = builder("pronQuant_S", pron).posRoot().build();
  public MorphemeState pronQuantModified_S =
      builder("pronQuantModified_S", pron).posRoot().build();
  public MorphemeState pronQues_S = builder("pronQues_S", pron).posRoot().build();
  public MorphemeState pronReflex_S = builder("pronReflex_S", pron).posRoot().build();

  // used for ben-sen modification
  public MorphemeState pronPers_Mod_S = builder("pronPers_Mod_S", pron).posRoot().build();

  MorphemeState pA1sg_S = nonTerminal("pA1sg_S", a1sg);
  MorphemeState pA2sg_S = nonTerminal("pA2sg_S", a2sg);

  MorphemeState pA1sgMod_S = nonTerminal("pA1sgMod_S", a1sg); // for modified ben
  MorphemeState pA2sgMod_S = nonTerminal("pA2sgMod_S", a2sg); // for modified sen

  MorphemeState pA3sg_S = nonTerminal("pA3sg_S", a3sg);
  MorphemeState pA1pl_S = nonTerminal("pA1pl_S", a1pl);
  MorphemeState pA2pl_S = nonTerminal("pA2pl_S", a2pl);

  MorphemeState pA3pl_S = nonTerminal("pA3pl_S", a3pl);

  MorphemeState pQuantA3sg_S = nonTerminal("pQuantA3sg_S", a3sg);
  MorphemeState pQuantA3pl_S = nonTerminal("pQuantA3pl_S", a3pl);
  MorphemeState pQuantModA3pl_S = nonTerminal("pQuantModA3pl_S", a3pl); // for birbirleri etc.
  MorphemeState pQuantA1pl_S = nonTerminal("pQuantA1pl_S", a1pl);
  MorphemeState pQuantA2pl_S = nonTerminal("pQuantA2pl_S", a2pl);

  MorphemeState pQuesA3sg_S = nonTerminal("pQuesA3sg_S", a3sg);
  MorphemeState pQuesA3pl_S = nonTerminal("pQuesA3pl_S", a3pl);

  MorphemeState pReflexA3sg_S = nonTerminal("pReflexA3sg_S", a3sg);
  MorphemeState pReflexA3pl_S = nonTerminal("pReflexA3pl_S", a3pl);
  MorphemeState pReflexA1sg_S = nonTerminal("pReflexA1sg_S", a1sg);
  MorphemeState pReflexA2sg_S = nonTerminal("pReflexA2sg_S", a2sg);
  MorphemeState pReflexA1pl_S = nonTerminal("pReflexA1pl_S", a1pl);
  MorphemeState pReflexA2pl_S = nonTerminal("pReflexA2pl_S", a2pl);

  // Possessive

  MorphemeState pPnon_S = nonTerminal("pPnon_S", pnon);
  MorphemeState pPnonMod_S = nonTerminal("pPnonMod_S", pnon); // for modified ben-sen
  MorphemeState pP1sg_S = nonTerminal("pP1sg_S", p1sg); // kimim
  MorphemeState pP2sg_S = nonTerminal("pP2sg_S", p2sg);
  MorphemeState pP3sg_S = nonTerminal("pP3sg_S", p3sg); // for `birisi` etc
  MorphemeState pP1pl_S = nonTerminal("pP1pl_S", p1pl); // for `birbirimiz` etc
  MorphemeState pP2pl_S = nonTerminal("pP2pl_S", p2pl); // for `birbiriniz` etc
  MorphemeState pP3pl_S = nonTerminal("pP3pl_S", p3pl); // for `birileri` etc

  // Case

  MorphemeState pNom_ST = terminal("pNom_ST", nom);
  MorphemeState pDat_ST = terminal("pDat_ST", dat);
  MorphemeState pAcc_ST = terminal("pAcc_ST", acc);

  MorphemeState pronZeroDeriv_S = nonTerminalDerivative("pronZeroDeriv_S", zero);

  private void connectPronounStates() {

    //----------- Personal Pronouns ----------------------------
    DictionaryItem ben = lexicon.getItemById("ben_Pron_Pers");
    DictionaryItem sen = lexicon.getItemById("sen_Pron_Pers");
    DictionaryItem o = lexicon.getItemById("o_Pron_Pers");
    DictionaryItem biz = lexicon.getItemById("biz_Pron_Pers");
    DictionaryItem siz = lexicon.getItemById("siz_Pron_Pers");
    DictionaryItem falan = lexicon.getItemById("falan_Pron_Pers");
    DictionaryItem falanca = lexicon.getItemById("falanca_Pron_Pers");

    pronPers_S.addEmpty(pA1sg_S, rootIs(ben));
    pronPers_S.addEmpty(pA2sg_S, rootIs(sen));
    pronPers_S.addEmpty(pA3sg_S, rootIsAny(o, falan, falanca));
    pronPers_S
        .add(pA3pl_S, "nlAr", rootIs(o)); // Oflazer does not have "onlar" as Pronoun root.
    pronPers_S
        .add(pA3pl_S, "lAr", rootIsAny(falan, falanca));
    pronPers_S.addEmpty(pA1pl_S, rootIs(biz));
    pronPers_S.addEmpty(pA2pl_S, rootIs(siz));

    // --- modified `ben-sen` special state and transitions
    pronPers_Mod_S.addEmpty(pA1sgMod_S, rootIs(ben));
    pronPers_Mod_S.addEmpty(pA2sgMod_S, rootIs(sen));
    pA1sgMod_S.addEmpty(pPnonMod_S);
    pA2sgMod_S.addEmpty(pPnonMod_S);
    pPnonMod_S.add(pDat_ST, "A");
    // ----

    pA1sg_S.addEmpty(pPnon_S);
    pA2sg_S.addEmpty(pPnon_S);
    pA3sg_S.addEmpty(pPnon_S);
    pA1pl_S.addEmpty(pPnon_S);
    pA2pl_S.addEmpty(pPnon_S);
    pA3pl_S.addEmpty(pPnon_S);

    //------------ Demonstrative pronouns. ------------------------

    pronDemons_S.addEmpty(pA3sg_S);
    pronDemons_S.add(pA3pl_S, "nlAr");

    //------------ Quantitiva Pronouns ----------------------------

    DictionaryItem birbiri = lexicon.getItemById("birbiri_Pron_Quant");
    DictionaryItem biri = lexicon.getItemById("biri_Pron_Quant");
    DictionaryItem bircogu = lexicon.getItemById("birçoğu_Pron_Quant");
    DictionaryItem birkaci = lexicon.getItemById("birkaçı_Pron_Quant");
    DictionaryItem beriki = lexicon.getItemById("beriki_Pron_Quant");
    DictionaryItem cogu = lexicon.getItemById("çoğu_Pron_Quant");
    DictionaryItem cumlesi = lexicon.getItemById("cümlesi_Pron_Quant");
    DictionaryItem hep = lexicon.getItemById("hep_Pron_Quant");
    DictionaryItem herbiri = lexicon.getItemById("herbiri_Pron_Quant");
    DictionaryItem herkes = lexicon.getItemById("herkes_Pron_Quant");
    DictionaryItem hicbiri = lexicon.getItemById("hiçbiri_Pron_Quant");
    DictionaryItem hepsi = lexicon.getItemById("hepsi_Pron_Quant");
    DictionaryItem kimi = lexicon.getItemById("kimi_Pron_Quant");
    DictionaryItem kimse = lexicon.getItemById("kimse_Pron_Quant");
    DictionaryItem oburku = lexicon.getItemById("öbürkü_Pron_Quant");
    DictionaryItem oburu = lexicon.getItemById("öbürü_Pron_Quant");
    DictionaryItem tumu = lexicon.getItemById("tümü_Pron_Quant");
    DictionaryItem topu = lexicon.getItemById("topu_Pron_Quant");
    DictionaryItem umum = lexicon.getItemById("umum_Pron_Quant");

    // we have separate A3pl and A3sg states for Quantitive Pronouns.
    // herkes and hep cannot be singular.
    pronQuant_S.addEmpty(pQuantA3sg_S,
        rootIsNone(herkes, umum, hepsi, cumlesi, hep, tumu, birkaci, topu));

    pronQuant_S.add(pQuantA3pl_S, "lAr",
        rootIsNone(hep, hepsi, birkaci, umum, cumlesi, cogu, bircogu, herbiri, tumu, hicbiri, topu,
            oburu));

    // Herkes is implicitly plural.
    pronQuant_S.addEmpty(pQuantA3pl_S,
        rootIsAny(herkes, umum, birkaci, hepsi, cumlesi, cogu, bircogu, tumu, topu));

    // connect "kimse" to Noun-A3sg and Noun-A3pl. It behaves like a noun.
    pronQuant_S.addEmpty(a3sg_S, rootIs(kimse));
    pronQuant_S.add(a3pl_S, "lAr", rootIsAny(kimse));

    // for `birbiri-miz` `hep-imiz`
    pronQuant_S.addEmpty(pQuantA1pl_S,
        rootIsAny(biri, birbiri, birkaci, herbiri, hep, kimi, cogu, bircogu, tumu, topu, hicbiri));

    // for `birbiri-niz` and `hep-iniz`
    pronQuant_S.addEmpty(pQuantA2pl_S,
        rootIsAny(biri, birbiri, birkaci, herbiri, hep, kimi, cogu, bircogu, tumu, topu, hicbiri));

    // this is used for birbir-ler-i, çok-lar-ı, birçok-lar-ı separate root and A3pl states are
    // used for this.
    pronQuantModified_S.addEmpty(pQuantModA3pl_S);
    pQuantModA3pl_S.add(pP3pl_S, "lArI");

    // both `biri-ne` and `birisi-ne` or `birbirine` and `birbirisine` are accepted.
    pQuantA3sg_S.addEmpty(pP3sg_S,
        rootIsAny(biri, birbiri, kimi, herbiri, hicbiri, oburu, oburku, beriki)
            .and(notHave(PhoneticAttribute.ModifiedPronoun)));

    pQuantA3sg_S.add(pP3sg_S, "sI",
        rootIsAny(biri, birbiri, herbiri, hicbiri, oburku)
            .and(notHave(PhoneticAttribute.ModifiedPronoun)));

    // there is no connection from pQuantA3pl to Pnon for preventing `biriler` (except herkes)
    pQuantA3pl_S.add(pP3pl_S, "I", rootIsAny(biri, birbiri, kimi, oburku, beriki));
    pQuantA3pl_S.addEmpty(pP3pl_S, rootIsAny(hepsi, birkaci, cumlesi, cogu, tumu, topu, bircogu));
    pQuantA3pl_S.addEmpty(pPnon_S, rootIsAny(herkes, umum, oburku, beriki));

    pQuantA1pl_S.add(pP1pl_S, "ImIz");
    pQuantA2pl_S.add(pP2pl_S, "InIz");

    //------------ Question Pronouns ----------------------------
    // `kim` (kim_Pron_Ques), `ne` and `nere`
    DictionaryItem ne = lexicon.getItemById("ne_Pron_Ques");
    DictionaryItem nere = lexicon.getItemById("nere_Pron_Ques");

    pronQues_S.addEmpty(pQuesA3sg_S);
    pronQues_S.add(pQuesA3pl_S, "lAr");

    pQuesA3sg_S.addEmpty(pPnon_S)
        .add(pP3sg_S, "+sI")
        .add(pP1sg_S, "Im", rootIsNot(ne))
        .add(pP1sg_S, "yIm", rootIs(ne))
        .add(pP2sg_S, "In", rootIsNot(ne))
        .add(pP2sg_S, "yIn", rootIs(ne))
        .add(pP1pl_S, "ImIz", rootIsNot(ne))
        .add(pP1pl_S, "yImIz", rootIs(ne));

    pQuesA3pl_S.addEmpty(pPnon_S)
        .add(pP3sg_S, "I")
        .add(pP1sg_S, "Im")
        .add(pP1pl_S, "ImIz");

    //------------ Reflexive Pronouns ----------------------------
    // `kendi`
    pronReflex_S.addEmpty(pReflexA1sg_S)
        .addEmpty(pReflexA2sg_S)
        .addEmpty(pReflexA3sg_S)
        .addEmpty(pReflexA1pl_S)
        .addEmpty(pReflexA2pl_S)
        .addEmpty(pReflexA3pl_S);

    pReflexA1sg_S.add(pP1sg_S, "Im");
    pReflexA2sg_S.add(pP2sg_S, "In");
    pReflexA3sg_S.add(pP3sg_S, "+sI").addEmpty(pP3sg_S);
    pReflexA1pl_S.add(pP1pl_S, "ImIz");
    pReflexA2pl_S.add(pP2pl_S, "InIz");
    pReflexA3pl_S.add(pP3pl_S, "lArI");

    // ------------------------
    // Case connections for all
    Condition nGroup = rootIsNone(ne, nere, falan, falanca);
    Condition yGroup = rootIsAny(ne, nere, falan, falanca);

    pPnon_S.addEmpty(pNom_ST)
        // not allowing `ben-e` and `sen-e`. `ban-a` and `san-a` are using different states
        .add(pDat_ST, "+nA", rootIsNone(ben, sen, ne, nere, falan, falanca))
        .add(pAcc_ST, "+nI", nGroup)
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+yI", yGroup);

    pP1sg_S.addEmpty(pNom_ST)
        .add(pDat_ST, "+nA", nGroup)
        .add(pAcc_ST, "+nI", nGroup)
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+yI", yGroup);

    // copy from pP1sg_S
    pP2sg_S.copyOutgoingTransitionsFrom(pP1sg_S);
    pP3sg_S.copyOutgoingTransitionsFrom(pP1sg_S);
    pP1pl_S.copyOutgoingTransitionsFrom(pP1sg_S);
    pP2pl_S.copyOutgoingTransitionsFrom(pP1sg_S);
    pP3pl_S.copyOutgoingTransitionsFrom(pP1sg_S);

    //------------- Derivation connections ---------

    pNom_ST.addEmpty(pronZeroDeriv_S, Conditions.HAS_TAIL);
    pDat_ST.addEmpty(pronZeroDeriv_S, Conditions.HAS_TAIL);

    pronZeroDeriv_S.addEmpty(nVerb_S);
  }

  // ------------- Adverb and Conjunctions -----------------

  MorphemeState advRoot_ST = builder("advRoot_ST", adv).posRoot().terminal().build();
  MorphemeState conjRoot_ST = builder("conjRoot_ST", conj).posRoot().terminal().build();

  // ------------- Verbs -----------------------------------

  MorphemeState verbRoot_S = builder("verbRoot_S", verb).posRoot().build();

  MorphemeState vA1sg_ST = terminal("vA1sg_ST", a1sg);
  MorphemeState vA2sg_ST = terminal("vA2sg_ST", a2sg);
  MorphemeState vA1pl_ST = terminal("vA1pl_ST", a1pl);
  MorphemeState vA3sg_ST = terminal("vA3sg_ST", a3sg);
  MorphemeState vA3sg_S = nonTerminal("vA3sg_S", a3sg);
  MorphemeState vA3pl_ST = terminal("vA3pl_ST", a3pl);

  MorphemeState vPast_S = nonTerminal("vPast_S", past);
  MorphemeState vNarr_S = nonTerminal("vNarr_S", narr);
  MorphemeState vCond_S = nonTerminal("vCond_S", cond);
  MorphemeState vProgYor_S = nonTerminal("vProgYor_S", prog1);

  MorphemeState vCop_ST = terminal("vCop_ST", cop);

  MorphemeState vNeg_S = nonTerminal("vNeg_S", neg);

  MorphemeState vImp_S = nonTerminal("vImp_S", imp);

  MorphemeState vCausT_S = nonTerminalDerivative("vCaus_S", caus);
  MorphemeState vCausTR_S = nonTerminalDerivative("vCausTR_S", caus);

  // for progressive vowel drop.
  MorphemeState verbRoot_Prog_S = nonTerminal("verbRoot_Prog_S", verb);

  MorphemeState vAor_S = nonTerminal("vAor_S", aor);

  private void connectVerbs() {

    // Imperative.
    verbRoot_S.addEmpty(vImp_S);

    vImp_S.addEmpty(vA2sg_ST)     // oku
        .add(vA3sg_ST, "+sIn") // okusun
        .add(vA3pl_ST, "+sInlAr"); // okusunlar

    verbRoot_S.add(vCausT_S, "t", has(RootAttribute.Causative_t)
        .or(new Conditions.LastDerivationIs(vCausTR_S))
        .andNot(new Conditions.LastDerivationIs(vCausT_S)));

    verbRoot_S.add(vCausTR_S, ">dIr",
        has(PhoneticAttribute.LastLetterConsonant)
            .andNot(new Conditions.LastDerivationIs(vCausTR_S)));

    vCausT_S.addEmpty(verbRoot_S);
    vCausTR_S.addEmpty(verbRoot_S);

    // "gidiyor" Progressive1 suffix.
    // if last letter is a vowel, this is handled with verbRoot_Prog_S root.
    verbRoot_S.add(vProgYor_S, "Iyor", notHave(PhoneticAttribute.LastLetterVowel));

    // For "aramak", the modified root "ar" connects to verbRoot_Prog_S. Here it is connected to
    // progressive "Iyor" suffix. We use a separate root state for these for convenience.
    verbRoot_Prog_S.add(vProgYor_S, "Iyor");
    vProgYor_S.add(vA1sg_ST, "um")
        .add(vA2sg_ST, "sun")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "uz")
        .add(vA3pl_ST, "lar");

    // Aorist Tense.
    // For single syllable words, it forms as "ar-er". For others "ir-ır-ur-ür"
    // However there are exceptions to it as well. So dictionary items are marked as Aorist_I and
    // Aorist_A. Also, for convenience, we use separate root forms for "ar-er" form.
    // If there is any morpheme already in the SearchPath
    verbRoot_S.add(vAor_S, "Ir",
        has(RootAttribute.Aorist_I).or(Conditions.HAS_SURFACE));
    verbRoot_S.add(vAor_S, "Ar",
        has(RootAttribute.Aorist_A).and(Conditions.HAS_NO_SURFACE));

    vAor_S.add(vA1sg_ST, "Im")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "Iz")
        .add(vA3pl_ST, "lar");
  }

  //--------------------------------------------------------

  public MorphemeState getRootState(
      DictionaryItem dictionaryItem,
      EnumSet<PhoneticAttribute> phoneticAttributes) {

    //TODO: consider a generic mechanism for such items.
    if (dictionaryItem.id.equals("değil_Verb")) {
      return nVerbDegil_S;
    }

    // Verbs like "aramak" drops their last vowel when  connected to "Iyor" Progressive suffix.
    // those modified roots are connected to a separate root state called verbRoot_Prog_S.
    if (phoneticAttributes.contains(PhoneticAttribute.LastVowelDropped)) {
      return verbRoot_Prog_S;
    }

    switch (dictionaryItem.primaryPos) {
      case Noun:
        if (dictionaryItem.hasAttribute(RootAttribute.CompoundP3sgRoot)) {
          return nounCompoundRoot_S;
        } else {
          return noun_S;
        }
      case Adjective:
      case Numeral:
        return adj_ST;
      case Pronoun:
        switch (dictionaryItem.secondaryPos) {
          case PersonalPron:
            return pronPers_S;
          case DemonstrativePron:
            return pronDemons_S;
          case QuantitivePron:
            return pronQuant_S;
          case QuestionPron:
            return pronQues_S;
          case ReflexivePron:
            return pronReflex_S;
          default:
            return pronQuant_S;
          //throw new IllegalStateException("Cannot find root for Pronoun " + dictionaryItem);
        }
      case Adverb:
        return advRoot_ST;
      case Conjunction:
        return conjRoot_ST;
      case Verb:
        return verbRoot_S;
      default:
        return noun_S;

    }
  }

}
