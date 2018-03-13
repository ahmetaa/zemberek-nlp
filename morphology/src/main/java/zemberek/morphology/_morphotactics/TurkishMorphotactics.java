package zemberek.morphology._morphotactics;

import static zemberek.morphology._morphotactics.Conditions.has;
import static zemberek.morphology._morphotactics.Conditions.notHave;
import static zemberek.morphology._morphotactics.Conditions.notHaveAny;
import static zemberek.morphology._morphotactics.Conditions.rootIs;
import static zemberek.morphology._morphotactics.Conditions.rootIsAny;
import static zemberek.morphology._morphotactics.Conditions.rootIsNone;
import static zemberek.morphology._morphotactics.Conditions.rootIsNot;
import static zemberek.morphology._morphotactics.MorphemeState.builder;
import static zemberek.morphology._morphotactics.MorphemeState.nonTerminal;
import static zemberek.morphology._morphotactics.MorphemeState.nonTerminalDerivative;
import static zemberek.morphology._morphotactics.MorphemeState.terminal;

import java.util.HashMap;
import java.util.Map;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology._morphotactics.Conditions.ContainsMorpheme;
import zemberek.morphology._morphotactics.Conditions.CurrentGroupContains;
import zemberek.morphology._morphotactics.Conditions.NoSurfaceAfterDerivation;
import zemberek.morphology._morphotactics.Conditions.PreviousStateIsAny;
import zemberek.morphology._morphotactics.Conditions.RootSurfaceIsAny;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

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
  public static final Morpheme abl = new Morpheme("Ablative", "Abl");
  public static final Morpheme loc = new Morpheme("Locative", "Loc");
  public static final Morpheme ins = new Morpheme("Instrumental", "Ins");

  // Derivation suffixes

  // Diminutive suffix. Noun to Noun conversion. "elmacık = small apple, poor apple"
  public static final Morpheme dim = new Morpheme("Diminutive", "Dim");
  // With suffix. Noun to Adjective conversion. "elmalı = with apple"
  public static final Morpheme with = new Morpheme("With", "With");

  public static final Morpheme justLike = new Morpheme("JustLike", "JustLike");
  public static final Morpheme become = new Morpheme("Become", "Become");

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
  // -ıyor
  public static final Morpheme prog1 = new Morpheme("Progressive1", "Prog1");
  // -makta
  public static final Morpheme prog2 = new Morpheme("Progressive2", "Prog2");
  public static final Morpheme aor = new Morpheme("Aorist", "Aor");
  public static final Morpheme fut = new Morpheme("Future", "Fut");

  // Verb
  public static final Morpheme imp = new Morpheme("Imparative", "Imp");
  public static final Morpheme caus = new Morpheme("Causative", "Caus");
  public static final Morpheme able = new Morpheme("Ability", "Able");
  public static final Morpheme pass = new Morpheme("Passive", "Pass");
  public static final Morpheme opt = new Morpheme("Optative", "Opt");
  public static final Morpheme desr = new Morpheme("Desire", "Desr");
  public static final Morpheme neces = new Morpheme("Necessity", "Neces");

  //-------------- States ----------------------------
  // _ST = Terminal state _S = Non Terminal State.
  // A terminal state means that a walk in the graph can end there.

  // root of the graph.
  MorphemeState root_S = nonTerminal("root_S", root);

  //-------------- Noun States ------------------------

  MorphemeState noun_S = builder("noun_S", noun).posRoot().build();
  MorphemeState nounCompoundRoot_S = builder("nounCompoundRoot_S", noun).posRoot().build();
  MorphemeState nounSuRoot_S = builder("nounSuRoot_S", noun).posRoot().build();

  // Number-Person agreement

  MorphemeState a3sg_S = nonTerminal("a3sg_S", a3sg);
  MorphemeState a3sgSu_S = nonTerminal("a3sgSu_S", a3sg);
  MorphemeState a3sgCompound_S = nonTerminal("a3sgCompound_S", a3sg);
  MorphemeState a3pl_S = nonTerminal("a3pl_S", a3pl);
  MorphemeState a3plCompound_S = nonTerminal("a3plCompound_S", a3pl);
  MorphemeState a3plCompound2_S = nonTerminal("a3plCompound2_S", a3pl);

  // Possessive

  MorphemeState pnon_S = nonTerminal("pnon_S", pnon);
  MorphemeState pnonCompound_S = nonTerminal("pnonCompound_S", pnon);
  MorphemeState pnonCompound2_S = nonTerminal("pnonCompound2_S", pnon);
  MorphemeState p1sg_S = nonTerminal("p1sg_S", p1sg);
  MorphemeState p2sg_S = nonTerminal("p2sg_S", p2sg);
  MorphemeState p3sg_S = nonTerminal("p3sg_S", p3sg);
  MorphemeState p1pl_S = nonTerminal("p1pl_S", p1pl);
  MorphemeState p2pl_S = nonTerminal("p2pl_S", p2pl);
  MorphemeState p3pl_S = nonTerminal("p3pl_S", p3pl);

  // Case

  MorphemeState nom_ST = terminal("nom_ST", nom);
  MorphemeState nom_S = nonTerminal("nom_S", nom);

  MorphemeState dat_ST = terminal("dat_ST", dat);
  MorphemeState abl_ST = terminal("abl_ST", abl);
  MorphemeState loc_ST = terminal("loc_ST", loc);
  MorphemeState ins_ST = terminal("ins_ST", ins);
  MorphemeState acc_ST = terminal("acc_ST", acc);

  // Derivation

  MorphemeState dim_S = nonTerminalDerivative("dim_S", dim);
  MorphemeState with_S = nonTerminalDerivative("with_S", with);
  MorphemeState justLike_S = nonTerminalDerivative("justLike_S", justLike);
  MorphemeState nounZeroDeriv_S = nonTerminalDerivative("nounZeroDeriv_S", zero);
  MorphemeState become_S = nonTerminalDerivative("become_S", become);

  //-------------- Conditions ------------------------------

  private RootLexicon lexicon;

  public TurkishMorphotactics(RootLexicon lexicon) {
    this.lexicon = lexicon;
    mapSpecialItemsToRootStates();
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

    // --- Compound Handling ---------
    // for compound roots like "zeytinyağ-" generate two transitions
    // NounCompound--(ε)--> a3sgCompound --(ε)--> pNonCompound_S --> Nom_S
    nounCompoundRoot_S.addEmpty(
        a3sgCompound_S,
        has(RootAttribute.CompoundP3sgRoot));

    a3sgCompound_S.addEmpty(pnonCompound_S);
    a3sgCompound_S.add(p3pl_S, "lArI");

    pnonCompound_S.addEmpty(nom_S);
    nom_S.add(become_S, "lAş");

    // for compound roots like "zeytinyağ-lar-ı" generate two transition
    // NounCompound--(lAr)--> a3plCompound ---> p3sg_S, P1sg etc.
    nounCompoundRoot_S.add(
        a3plCompound_S,
        "lAr",
        has(RootAttribute.CompoundP3sgRoot));

    // but for pnon connection, we use lArI
    nounCompoundRoot_S.add(
        a3plCompound2_S,
        "lArI",
        has(RootAttribute.CompoundP3sgRoot));

    a3plCompound_S
        .add(p3sg_S, "I")
        .add(p2sg_S, "In")
        .add(p1sg_S, "Im")
        .add(p1pl_S, "ImIz")
        .add(p2pl_S, "InIz")
        .add(p3pl_S, "I");

    a3plCompound2_S.addEmpty(pnonCompound2_S);
    pnonCompound2_S.addEmpty(nom_ST);

    // ------

    Condition noFamily = notHave(RootAttribute.FamilyMember);

    // ev-ε-ε-? Reject "annemler" etc.
    a3sg_S
        .addEmpty(pnon_S, noFamily)        // ev
        .add(p1sg_S, "Im", noFamily)       // evim
        .add(p2sg_S, "In", noFamily)       // evin
        .add(p3sg_S, "+sI", noFamily)      // evi, odası
        .addEmpty(p3sg_S,
            has(RootAttribute.CompoundP3sg))  // "zeytinyağı" has two analyses. Pnon and P3sg.
        .add(p1pl_S, "ImIz", noFamily)     // evimiz
        .add(p2pl_S, "InIz", noFamily)     // eviniz
        .add(p3pl_S, "lArI", noFamily);    // evleri

    // ev-ler-ε-?
    a3pl_S.addEmpty(pnon_S, noFamily);

    // ev-ler-im-?
    a3pl_S
        .add(p1sg_S, "Im", noFamily)
        .add(p2sg_S, "In", noFamily)
        .addEmpty(p1sg_S, has(RootAttribute.ImplicitP1sg)) // for words like "annemler"
        .addEmpty(p2sg_S, has(RootAttribute.ImplicitP2sg)) // for words like "annenler"
        .add(p3sg_S, "I", noFamily)
        .add(p1pl_S, "ImIz", noFamily)
        .add(p2pl_S, "InIz", noFamily)
        .add(p3pl_S, "I", noFamily);

    // --- handle su - akarsu roots. ----
    nounSuRoot_S.addEmpty(a3sgSu_S);
    nounSuRoot_S.add(a3pl_S, "lar");
    a3sgSu_S
        .addEmpty(pnon_S)
        .add(p1sg_S, "yum")
        .add(p2sg_S, "yun")
        .add(p3sg_S, "yu")
        .add(p1pl_S, "yumuz")
        .add(p2pl_S, "yunuz")
        .add(p3pl_S, "lArI");

    // ev-?-ε-ε (ev, evler).
    pnon_S.addEmpty(nom_ST,
        notHave(PhoneticAttribute.ExpectsVowel)
            .and(notHaveAny(RootAttribute.CompoundP3sgRoot, RootAttribute.FamilyMember)));

    // This transition is for not allowing inputs like "kitab" or "zeytinyağ".
    // They will fail because nominal case state is non terminal (nom_S)
    pnon_S.addEmpty(nom_S,
        has(RootAttribute.CompoundP3sgRoot)
            .or(has(PhoneticAttribute.ExpectsVowel)));

    // Not allow "zetinyağı-ya" etc.
    pnon_S
        .add(dat_ST, "+yA", notHave(RootAttribute.CompoundP3sg))   // ev-e
        .add(abl_ST, ">dAn", notHave(RootAttribute.CompoundP3sg))  // ev-den
        .add(loc_ST, ">dA", notHave(RootAttribute.CompoundP3sg))   // evde
        .add(acc_ST, "+yI", notHave(RootAttribute.CompoundP3sg))   // evi
        .add(ins_ST, "+ylA");   // evle

    pnon_S.add(dat_ST, "+nA", has(RootAttribute.CompoundP3sg))   // zeytinyağı-na
        .add(abl_ST, "+ndAn", has(RootAttribute.CompoundP3sg))   // zeytinyağı-ndan
        .add(loc_ST, "+ndA", has(RootAttribute.CompoundP3sg))    // zeytinyağı-nda
        .add(acc_ST, "+nI", has(RootAttribute.CompoundP3sg));    // zeytinyağı-nı

    // This transition is for words like "içeri" or "dışarı".
    // Those words implicitly contains Dative suffix.
    // But It is also possible to add dative suffix +yA to those words such as "içeri-ye".
    pnon_S.addEmpty(dat_ST, has(RootAttribute.ImplicitDative));

    p1sg_S
        .addEmpty(nom_ST)    // evim
        .add(dat_ST, "A")    // evime
        .add(loc_ST, "dA")   // evimde
        .add(abl_ST, "dAn")  // evimden
        .add(ins_ST, "lA")   // evimle
        .add(acc_ST, "I");   // evimi

    p2sg_S
        .addEmpty(nom_ST)    // evin
        .add(dat_ST, "A")    // evine
        .add(loc_ST, "dA")   // evinde
        .add(abl_ST, "dAn")  // evinden
        .add(ins_ST, "lA")   // evinle
        .add(acc_ST, "I");   // evini

    p3sg_S
        .addEmpty(nom_ST)    // evi
        .add(dat_ST, "nA")   // evine
        .add(loc_ST, "dA")   // evinde
        .add(abl_ST, "ndAn") // evinden
        .add(ins_ST, "lA")   // eviyle
        .add(acc_ST, "nI");  // evini

    p1pl_S
        .addEmpty(nom_ST)    // evimiz
        .add(dat_ST, "A")    // evimize
        .add(loc_ST, "dA")   // evimizde
        .add(abl_ST, "dAn")  // evimizden
        .add(ins_ST, "lA")   // evimizden
        .add(acc_ST, "I");   // evimizi

    p2pl_S
        .addEmpty(nom_ST)    // eviniz
        .add(dat_ST, "A")    // evinize
        .add(loc_ST, "dA")   // evinizde
        .add(abl_ST, "dAn")  // evinizden
        .add(ins_ST, "lA")   // evinizle
        .add(acc_ST, "I");   // evinizi

    p3pl_S
        .addEmpty(nom_ST)     // evleri
        .add(dat_ST, "nA")    // evlerine
        .add(loc_ST, "ndA")   // evlerinde
        .add(abl_ST, "ndAn")  // evlerinden
        .add(ins_ST, "ylA")   // evleriyle
        .add(acc_ST, "nI");   // evlerini

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

    // elma-dan-ım elma-dan-dı
    abl_ST.addEmpty(nounZeroDeriv_S, noun2VerbZeroDerivationCondition);

    // elma-da-yım elma-da-ydı
    loc_ST.addEmpty(nounZeroDeriv_S, noun2VerbZeroDerivationCondition);

    // elma-yla-yım elma-yla-ydı
    ins_ST.addEmpty(nounZeroDeriv_S, noun2VerbZeroDerivationCondition);

    nounZeroDeriv_S.addEmpty(nVerb_S);

    // meyve-li
    Condition noSurfaceAfterDerivation = new NoSurfaceAfterDerivation();
    nom_ST.add(with_S, "lI",
        noSurfaceAfterDerivation
            .and(new ContainsMorpheme(with).not()));

    nom_ST.add(justLike_S, "+msI",
        noSurfaceAfterDerivation
            .and(new ContainsMorpheme(justLike, adj).not()));

    nom_ST.add(justLike_S, "ImsI",
        notHave(PhoneticAttribute.LastLetterVowel)
            .and(noSurfaceAfterDerivation)
            .and(new ContainsMorpheme(justLike, adj).not()));

    // connect With to Adjective root.
    with_S.addEmpty(adj_ST);

    justLike_S.addEmpty(adj_ST);

    nom_ST.add(become_S,"lAş",
        noSurfaceAfterDerivation.andNot(new ContainsMorpheme(adj)));
    become_S.addEmpty(verbRoot_S);

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

    adj_ST.add(become_S,"lAş", new NoSurfaceAfterDerivation());
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
            .andNot(new Conditions.PreviousGroupContains(p1pl_S, p1sg_S));
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

  public MorphemeState verbRoot_S = builder("verbRoot_S", verb).posRoot().build();

  MorphemeState vA1sg_ST = terminal("vA1sg_ST", a1sg);
  MorphemeState vA2sg_ST = terminal("vA2sg_ST", a2sg);
  MorphemeState vA3sg_ST = terminal("vA3sg_ST", a3sg);
  MorphemeState vA1pl_ST = terminal("vA1pl_ST", a1pl);
  MorphemeState vA2pl_ST = terminal("vA2pl_ST", a2pl);
  MorphemeState vA3pl_ST = terminal("vA3pl_ST", a3pl);

  MorphemeState vPast_S = nonTerminal("vPast_S", past);
  MorphemeState vNarr_S = nonTerminal("vNarr_S", narr);
  MorphemeState vCond_S = nonTerminal("vCond_S", cond);
  MorphemeState vPastAfterTense_S = nonTerminal("vPastAfterTense_S", past);
  MorphemeState vNarrAfterTense_S = nonTerminal("vNarrAfterTense_S", narr);

  // terminal cases are used if A3pl comes before NarrAfterTense, PastAfterTense or vCond
  MorphemeState vPastAfterTense_ST = terminal("vPastAfterTense_ST", past);
  MorphemeState vNarrAfterTense_ST = terminal("vNarrAfterTense_ST", narr);
  MorphemeState vCond_ST = terminal("vCond_ST", cond);

  MorphemeState vProgYor_S = nonTerminal("vProgYor_S", prog1);
  MorphemeState vProgMakta_S = nonTerminal("vProgMakta_S", prog2);
  MorphemeState vFut_S = nonTerminal("vFut_S", fut);

  MorphemeState vCop_ST = terminal("vCop_ST", cop);

  MorphemeState vNeg_S = nonTerminal("vNeg_S", neg);
  // for negative before progressive-1 "Iyor"
  MorphemeState vNegProg1_S = nonTerminal("vNegProg1_S", neg);

  MorphemeState vImp_S = nonTerminal("vImp_S", imp);

  MorphemeState vCausT_S = nonTerminalDerivative("vCaus_S", caus);
  MorphemeState vCausTır_S = nonTerminalDerivative("vCausTır_S", caus);

  // for progressive vowel drop.
  MorphemeState verbRoot_Prog_S = builder("verbRoot_Prog_S", verb).posRoot().build();

  MorphemeState vAor_S = nonTerminal("vAor_S", aor);
  MorphemeState vAorNeg_S = nonTerminal("vAorNeg_S", aor);

  MorphemeState vAble_S = nonTerminalDerivative("vAble_S", able);
  MorphemeState vAbleNeg_S = nonTerminalDerivative("vAbleNeg_S", able);
  MorphemeState vAbleNegDerivRoot_S = builder("vAbleNegDerivRoot_S", verb).posRoot().build();

  MorphemeState vPass_S = nonTerminalDerivative("vPass_S", pass);

  MorphemeState vOpt_S = nonTerminalDerivative("vOpt_S", opt);
  MorphemeState vDesr_S = nonTerminalDerivative("vDesr_S", desr);
  MorphemeState vNeces_S = nonTerminalDerivative("vNeces_S", neces);

  public MorphemeState vDeYeRoot_S = builder("vDeYeRoot_S", verb).posRoot().build();

  private void connectVerbs() {

    // Imperative.
    verbRoot_S.addEmpty(vImp_S);

    vImp_S.addEmpty(vA2sg_ST)     // oku
        .add(vA3sg_ST, "sIn") // okusun
        .add(vA2pl_ST, "+yIn") // okuyun
        .add(vA2pl_ST, "+yInIz") // okuyunuz
        .add(vA3pl_ST, "sInlAr"); // okusunlar

    // Causative suffixes
    // Causes Verb-Verb derivation. There are three forms: "t", "tIr" and "Ir".
    // 1- "t" form is used if verb ends with a vowel, or immediately after "tIr" Causative.
    // 2- "tIr" form is used if verb ends with a consonant or immediately after "t" Causative.
    // 3- "Ir" form appears after some specific verbs but currently we treat them as separate verb.
    // such as "pişmek - pişirmek"

    verbRoot_S.add(vCausT_S, "t", has(RootAttribute.Causative_t)
        .or(new Conditions.LastDerivationIs(vCausTır_S))
        .andNot(new Conditions.LastDerivationIsAny(vCausT_S, vPass_S, vAble_S)));

    verbRoot_S.add(vCausTır_S, ">dIr",
        has(PhoneticAttribute.LastLetterConsonant)
            .andNot(new Conditions.LastDerivationIsAny(vCausTır_S, vPass_S, vAble_S)));

    vCausT_S.addEmpty(verbRoot_S);
    vCausTır_S.addEmpty(verbRoot_S);

    // Progressive1 suffix. "-Iyor"
    // if last letter is a vowel, this is handled with verbRoot_Prog_S root.
    verbRoot_S.add(vProgYor_S, "Iyor", notHave(PhoneticAttribute.LastLetterVowel));

    // For "aramak", the modified root "ar" connects to verbRoot_Prog_S. Here it is connected to
    // progressive "Iyor" suffix. We use a separate root state for these for convenience.
    verbRoot_Prog_S.add(vProgYor_S, "Iyor");
    vProgYor_S
        .add(vA1sg_ST, "um")
        .add(vA2sg_ST, "sun")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "uz")
        .add(vA2pl_ST, "sunuz")
        .add(vA3pl_ST, "lar");
    vProgYor_S.add(vCond_S, "sa");
    vProgYor_S.add(vPastAfterTense_S, "du");
    vProgYor_S.add(vNarrAfterTense_S, "muş");

    // Progressive - 2 "-mAktA"
    verbRoot_S.add(vProgMakta_S, "mAktA");
    vProgMakta_S
        .add(vA1sg_ST, "yIm")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "yIz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr");
    vProgMakta_S.add(vCond_S, "ysA");
    vProgMakta_S.add(vPastAfterTense_S, "ydI");
    vProgMakta_S.add(vNarrAfterTense_S, "ymIş");

    // Positive Aorist Tense.
    // For single syllable words, it forms as "ar-er". For others "ir-ır-ur-ür"
    // However there are exceptions to it as well. So dictionary items are marked as Aorist_I and
    // Aorist_A.
    verbRoot_S.add(vAor_S, "Ir",
        has(RootAttribute.Aorist_I).or(Conditions.HAS_SURFACE));
    verbRoot_S.add(vAor_S, "Ar",
        has(RootAttribute.Aorist_A).and(Conditions.HAS_NO_SURFACE));
    vAor_S
        .add(vA1sg_ST, "Im")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "Iz")
        .add(vA1pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr");
    vAor_S.add(vPastAfterTense_S, "dI");
    vAor_S.add(vNarrAfterTense_S, "mIş");
    vAor_S.add(vCond_S, "sA");

    // Negative
    verbRoot_S.add(vNeg_S, "mA");
    vNeg_S.addEmpty(vImp_S);
    vNeg_S.add(vPast_S, "dI");
    vNeg_S.add(vFut_S, "yAcA~k");
    vNeg_S.add(vFut_S, "yAcA!ğ");
    vNeg_S.add(vNarr_S, "mIş");
    vNeg_S.add(vProgMakta_S, "mAktA");
    vNeg_S.add(vOpt_S, "yA");
    vNeg_S.add(vDesr_S, "sA");
    vNeg_S.add(vNeces_S, "mAlI");

    // Negative form is "m" before progressive "Iyor" because last vowel drops.
    // We use a separate negative state for this.
    verbRoot_S.add(vNegProg1_S, "m");
    vNegProg1_S.add(vProgYor_S, "Iyor");

    // Negative Aorist
    // Aorist tense forms differently after negative.
    vNeg_S.addEmpty(vAorNeg_S);
    vAorNeg_S
        .add(vA1sg_ST, "m")
        .add(vA2sg_ST, "zsIn")
        .add(vA3sg_ST, "z")
        .add(vA1pl_ST, "yIz")
        .add(vA2pl_ST, "zsInIz")
        .add(vA3pl_ST, "zlAr");
    vAorNeg_S.add(vPastAfterTense_S, "zdI");
    vAorNeg_S.add(vNarrAfterTense_S, "zmIş");
    vAorNeg_S.add(vCond_S, "zsA");

    //Positive Ability.
    // This makes a Verb-Verb derivation.
    verbRoot_S.add(vAble_S, "+yAbil", new Conditions.LastDerivationIs(vAble_S).not());

    // for ability derivation we use another root. This prevents adding a lot of conditions
    // to other derivative suffix transitions. Such as for preventing Able+Verb+Caus
    vAble_S.addEmpty(verbRoot_S);

    // Negative ability.
    verbRoot_S.add(vAbleNeg_S, "+yA");
    // Also for ability that comes before negative, we add a new root state.
    // From there only negative connections is possible.
    vAbleNeg_S.addEmpty(vAbleNegDerivRoot_S);
    vAbleNegDerivRoot_S.add(vNeg_S, "mA");
    vAbleNegDerivRoot_S.add(vNegProg1_S, "m");

    // it is possible to have abil derivation after negative.
    vNeg_S.add(vAble_S, "yAbil");

    // Passive
    // Causes Verb-Verb derivation. Passive morpheme has three forms.
    // 1- If Verb ends with a vowel: "In"
    // 2- If Verb ends with letter 'l' : "InIl"
    // 3- If Verb ends with other consonants: "nIl"
    // When loading dictionary, first and second case items are marked with Passive_In

    verbRoot_S.add(vPass_S, "In", has(RootAttribute.Passive_In)
        .andNot(new Conditions.ContainsMorpheme(pass)));
    verbRoot_S.add(vPass_S, "InIl", has(RootAttribute.Passive_In)
        .andNot(new Conditions.ContainsMorpheme(pass)));
    verbRoot_S.add(vPass_S, "+nIl", notHave(RootAttribute.Passive_In)
        .andNot(new Conditions.ContainsMorpheme(pass)));
    vPass_S.addEmpty(verbRoot_S);

    // Condition "oku-r-sa"
    vCond_S
        .add(vA1sg_ST, "m")
        .add(vA2sg_ST, "n")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "k")
        .add(vA2pl_ST, "nIz")
        .add(vA3pl_ST, "lAr");

    // Past "oku-du"
    verbRoot_S.add(vPast_S, ">dI");
    vPast_S
        .add(vA1sg_ST, "m")
        .add(vA2sg_ST, "n")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "k")
        .add(vA2pl_ST, "nIz")
        .add(vA3pl_ST, "lAr");
    vPast_S.add(vCond_S, "ysA");

    // Narrative "oku-muş"
    verbRoot_S.add(vNarr_S, "mIş");
    vNarr_S
        .add(vA1sg_ST, "Im")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "Iz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr");
    vNarr_S.add(vCond_S, "sA");
    vNarr_S.add(vPastAfterTense_S, "tI");

    // Past after tense "oku-muş-tu"
    vPastAfterTense_S
        .add(vA1sg_ST, "m")
        .add(vA2sg_ST, "n")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "k")
        .add(vA2pl_ST, "nIz")
        .add(vA3pl_ST, "lAr");

    // Narrative after tense "oku-r-muş"
    vNarrAfterTense_S
        .add(vA1sg_ST, "Im")
        .add(vA2sg_ST, "sIn")
        // for preventing yap+ar+lar(A3pl)+mış+A3sg
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "Iz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr");

    // Future "oku-yacak"
    verbRoot_S.add(vFut_S, "+yAcA~k");
    verbRoot_S.add(vFut_S, "+yAcA!ğ");
    vFut_S
        .add(vA1sg_ST, "Im")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST, has(PhoneticAttribute.ExpectsConsonant)) // for preventing "geleceğ"
        .add(vA1pl_ST, "Iz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr");
    vFut_S.add(vCond_S, "sA");
    vFut_S.add(vPastAfterTense_S, "tI");

    // `demek` and `yemek` are special because they are the only two verbs with two letters
    // and ends with a vowel.
    // Their root transform as:
    // No chabge: de-di, de-miş, de-dir
    // Change : di-yecek di-yor de-r
    // "ye" has similar behavior but not the same. Such as "yi-yin" but for "de", "de-yin"
    // TODO: this can be achieved with less repetition.
    RootSurfaceIsAny diYiCondition = new RootSurfaceIsAny("di", "yi");
    RootSurfaceIsAny deYeCondition = new RootSurfaceIsAny("de", "ye");
    vDeYeRoot_S
        .add(vFut_S, "yece~k", diYiCondition)
        .add(vFut_S, "yece!ğ", diYiCondition)
        .add(vProgYor_S, "yor", diYiCondition)
        .add(vAble_S, "yebil", diYiCondition)
        .add(vAbleNeg_S, "ye", diYiCondition)
        .add(vOpt_S, "ye", diYiCondition);

    vDeYeRoot_S
        .add(vCausTır_S, "dir", deYeCondition)
        .add(vPass_S, "n", deYeCondition)
        .add(vPass_S, "nil", deYeCondition)
        .add(vPast_S, "di", deYeCondition)
        .add(vNarr_S, "miş", deYeCondition)
        .add(vAor_S, "r", deYeCondition)
        .add(vNeg_S, "me", deYeCondition)
        .add(vNegProg1_S, "m", deYeCondition)
        .add(vProgMakta_S, "mekte", deYeCondition)
        .add(vDesr_S, "se", deYeCondition)
        .addEmpty(vImp_S, deYeCondition);

    // Optative (gel-e, gel-eyim gel-me-ye-yim)
    verbRoot_S.add(vOpt_S, "+yA");
    vOpt_S
        .add(vA1sg_ST, "yIm")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "lIm")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr")
        .add(vPastAfterTense_S, "ydI")
        .add(vNarrAfterTense_S, "ymIş");

    // Desire (gel-se, gel-se-m gel-me-se-m)
    verbRoot_S.add(vDesr_S, "sA");
    vDesr_S
        .add(vA1sg_ST, "m")
        .add(vA2sg_ST, "n")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "k")
        .add(vA2pl_ST, "nIz")
        .add(vA3pl_ST, "lAr")
        .add(vPastAfterTense_S, "ydI")
        .add(vNarrAfterTense_S, "ymIş");

    verbRoot_S.add(vNeces_S, "mAlI");
    vNeces_S
        .add(vA1sg_ST, "yIm")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "yIz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr")
        .add(vPastAfterTense_S, "ydI")
        .add(vNarrAfterTense_S, "ymIş");

    // A3pl exception case.
    // A3pl can appear before or after some tense suffixes.
    // "yapar-lar-dı" - "yapar-dı-lar"
    // For preventing "yapar-dı-lar-dı", conditions are added.
    Condition previousNotPastNarrCond = new PreviousStateIsAny(
        vPastAfterTense_S, vNarrAfterTense_S, vCond_S).not();
    vA3pl_ST.add(vPastAfterTense_ST, "dI", previousNotPastNarrCond);
    vA3pl_ST.add(vNarrAfterTense_ST, "mIş", previousNotPastNarrCond);
    vA3pl_ST.add(vCond_ST, "sA", previousNotPastNarrCond);
  }

  Map<String, MorphemeState> itemRootStateMap = new HashMap<>();

  void mapSpecialItemsToRootStates() {
    itemRootStateMap.put("değil_Verb", nVerbDegil_S);
    itemRootStateMap.put("su_Noun", nounSuRoot_S);
    itemRootStateMap.put("akarsu_Noun", nounSuRoot_S);
  }

  //--------------------------------------------------------

  public MorphemeState getRootState(
      DictionaryItem dictionaryItem,
      PhoneticAttributeSet phoneticAttributes) {

    MorphemeState root = itemRootStateMap.get(dictionaryItem.id);
    if (root != null) {
      return root;
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
