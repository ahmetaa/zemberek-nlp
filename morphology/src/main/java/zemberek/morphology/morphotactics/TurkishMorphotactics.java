package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Conditions.has;
import static zemberek.morphology.morphotactics.Conditions.lastDerivationIs;
import static zemberek.morphology.morphotactics.Conditions.not;
import static zemberek.morphology.morphotactics.Conditions.notHave;
import static zemberek.morphology.morphotactics.Conditions.previousStateIs;
import static zemberek.morphology.morphotactics.Conditions.previousStateIsNot;
import static zemberek.morphology.morphotactics.Conditions.rootIs;
import static zemberek.morphology.morphotactics.Conditions.rootIsAny;
import static zemberek.morphology.morphotactics.Conditions.rootIsNone;
import static zemberek.morphology.morphotactics.Conditions.rootIsNot;
import static zemberek.morphology.morphotactics.Morpheme.derivational;
import static zemberek.morphology.morphotactics.Morpheme.instance;
import static zemberek.morphology.morphotactics.MorphemeState.builder;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminal;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminalDerivative;
import static zemberek.morphology.morphotactics.MorphemeState.terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.StemTransitions;
import zemberek.morphology.analysis.StemTransitionsMapBased;
import zemberek.morphology.analysis.StemTransitionsTrieBased;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Conditions.ContainsMorpheme;
import zemberek.morphology.morphotactics.Conditions.ContainsMorphemeSequence;
import zemberek.morphology.morphotactics.Conditions.CurrentGroupContainsAny;
import zemberek.morphology.morphotactics.Conditions.HasTailSequence;
import zemberek.morphology.morphotactics.Conditions.LastDerivationIs;
import zemberek.morphology.morphotactics.Conditions.NoSurfaceAfterDerivation;
import zemberek.morphology.morphotactics.Conditions.PreviousMorphemeIsAny;
import zemberek.morphology.morphotactics.Conditions.PreviousStateIsAny;
import zemberek.morphology.morphotactics.Conditions.RootSurfaceIs;
import zemberek.morphology.morphotactics.Conditions.RootSurfaceIsAny;
import zemberek.morphology.morphotactics.Conditions.SecondaryPosIs;

public class TurkishMorphotactics {

  private static Map<String, Morpheme> morphemeMap = new HashMap<>();

  public static final Morpheme root = addMorpheme(instance("Root", "Root"));
  public static final Morpheme noun = addMorpheme(instance("Noun", "Noun", PrimaryPos.Noun));
  public static final Morpheme adj = addMorpheme(
      instance("Adjective", "Adj", PrimaryPos.Adjective));
  public static final Morpheme verb = addMorpheme(instance("Verb", "Verb", PrimaryPos.Verb));
  public static final Morpheme pron = addMorpheme(
      instance("Pronoun", "Pron", PrimaryPos.Pronoun));
  public static final Morpheme adv = addMorpheme(instance("Adverb", "Adv", PrimaryPos.Adverb));
  public static final Morpheme conj = addMorpheme(
      instance("Conjunction", "Conj", PrimaryPos.Conjunction));
  public static final Morpheme punc = addMorpheme(
      instance("Punctuation", "Punc", PrimaryPos.Punctuation));
  public static final Morpheme ques = addMorpheme(
      instance("Question", "Ques", PrimaryPos.Question));
  public static final Morpheme postp = addMorpheme(instance("PostPositive", "Postp",
      PrimaryPos.PostPositive));
  public static final Morpheme det = addMorpheme(
      instance("Determiner", "Det", PrimaryPos.Determiner));
  public static final Morpheme num = addMorpheme(
      instance("Numeral", "Num", PrimaryPos.Numeral));
  public static final Morpheme dup = addMorpheme(
      instance("Duplicator", "Dup", PrimaryPos.Duplicator));
  public static final Morpheme interj = addMorpheme(instance("Interjection", "Interj",
      PrimaryPos.Interjection));

  // Number-Person agreement.

  public static final Morpheme a1sg = addMorpheme(instance("FirstPersonSingular", "A1sg"));
  public static final Morpheme a2sg = addMorpheme(instance("SecondPersonSingular", "A2sg"));
  public static final Morpheme a3sg = addMorpheme(instance("ThirdPersonSingular", "A3sg"));
  public static final Morpheme a1pl = addMorpheme(instance("FirstPersonPlural", "A1pl"));
  public static final Morpheme a2pl = addMorpheme(instance("SecondPersonPlural", "A2pl"));
  public static final Morpheme a3pl = addMorpheme(instance("ThirdPersonPlural", "A3pl"));

  // Possessive

  // No possession suffix. This is not a real Morpheme but adds information to analysis. "elma = apple"
  public static final Morpheme pnon = addMorpheme(instance("NoPosession", "Pnon"));

  // First person singular possession suffix.  "elma-m = my apple"
  public static final Morpheme p1sg = addMorpheme(
      instance("FirstPersonSingularPossessive", "P1sg"));

  public static final Morpheme p2sg = addMorpheme(
      instance("SecondPersonSingularPossessive", "P2sg"));

  // Third person singular possession suffix. "elma-sı = his/her apple"
  public static final Morpheme p3sg = addMorpheme(
      instance("ThirdPersonSingularPossessive", "P3sg"));

  // First person plural possession suffix.
  public static final Morpheme p1pl = addMorpheme(
      instance("FirstPersonPluralPossessive", "P1pl"));

  public static final Morpheme p2pl = addMorpheme(
      instance("SecondPersonPluralPossessive", "P2pl"));

  public static final Morpheme p3pl = addMorpheme(
      instance("ThirdPersonPluralPossessive", "P3pl"));

  // Case suffixes

  // elma
  public static final Morpheme nom = addMorpheme(instance("Nominal", "Nom"));
  // elmaya
  public static final Morpheme dat = addMorpheme(instance("Dative", "Dat"));
  // elmayı
  public static final Morpheme acc = addMorpheme(instance("Accusative", "Acc"));
  // elmadan
  public static final Morpheme abl = addMorpheme(instance("Ablative", "Abl"));
  // elmada
  public static final Morpheme loc = addMorpheme(instance("Locative", "Loc"));
  // elmayla
  public static final Morpheme ins = addMorpheme(instance("Instrumental", "Ins"));
  // elmanın
  public static final Morpheme gen = addMorpheme(instance("Genitive", "Gen"));
  // elmaca
  public static final Morpheme equ = addMorpheme(instance("Equ", "Equ"));

  // Derivation suffixes

  // elmacık (Noun)
  public static final Morpheme dim = addMorpheme(derivational("Diminutive", "Dim"));
  // elmalık (Noun) TODO: Find better name.
  public static final Morpheme ness = addMorpheme(derivational("Ness", "Ness"));
  // elmalı (Adj)
  public static final Morpheme with = addMorpheme(derivational("With", "With"));
  // elmasız (Adj)
  public static final Morpheme without = addMorpheme(derivational("Without", "Without"));
  // elmasal (Adj)
  public static final Morpheme related = addMorpheme(derivational("Related", "Related"));
  // tahtamsı (Adj)
  public static final Morpheme justLike = addMorpheme(derivational("JustLike", "JustLike"));
  // tahtadaki (Adj)
  public static final Morpheme rel = addMorpheme(derivational("Relation", "Rel"));
  // elmacı (Noun)
  public static final Morpheme agt = addMorpheme(derivational("Agentive", "Agt"));
  // tahtalaş (Verb)
  public static final Morpheme become = addMorpheme(derivational("Become", "Become"));
  // tahtalan (Verb)
  public static final Morpheme acquire = addMorpheme(derivational("Acquire", "Acquire"));

  // yeşilce (Adj->Adv)
  public static final Morpheme ly = addMorpheme(derivational("Ly", "Ly"));
  // oku-t oku-t-tur (Verb)
  public static final Morpheme caus = addMorpheme(derivational("Causative", "Caus"));
  // konuş-uş (Verb)
  public static final Morpheme recip = addMorpheme(derivational("Reciprocal", "Recip"));
  // kaşınmak (Verb) For now Reflexive suffixes are only implicit. Meaning that
  // dictionary contains "kaşınmak" with Reflexive attribute.
  public static final Morpheme reflex = addMorpheme(derivational("Reflexive", "Reflex"));
  // oku-yabil (Verb)
  public static final Morpheme able = addMorpheme(derivational("Ability", "Able"));
  // oku-n, oku-nul (Verb)
  public static final Morpheme pass = addMorpheme(derivational("Passive", "Pass"));
  // okumak (Noun)
  public static final Morpheme inf1 = addMorpheme(derivational("Infinitive1", "Inf1"));
  // okuma (Noun)
  public static final Morpheme inf2 = addMorpheme(derivational("Infinitive2", "Inf2"));
  // okuyuş (Noun)
  public static final Morpheme inf3 = addMorpheme(derivational("Infinitive3", "Inf3"));
  // okumaca (Noun)
  public static final Morpheme actOf = addMorpheme(derivational("ActOf", "ActOf"));
  // okuduğum kitap (Adj, Noun)
  public static final Morpheme pastPart = addMorpheme(
      derivational("PastParticiple", "PastPart"));
  // okumuşlarımız (Adj, Noun)
  public static final Morpheme narrPart = addMorpheme(
      derivational("NarrativeParticiple", "NarrPart"));
  // okuyacağım kitap (Adj, Noun)
  public static final Morpheme futPart = addMorpheme(
      derivational("FutureParticiple", "FutPart"));
  // okuyan (Adj, Noun)
  public static final Morpheme presPart = addMorpheme(
      derivational("PresentParticiple", "PresPart"));
  // okurluk (Noun)
  public static final Morpheme aorPart = addMorpheme(
      derivational("AoristParticiple", "AorPart"));
  // okumazlık - okumamazlık (Noun)
  public static final Morpheme notState = addMorpheme(derivational("NotState", "NotState"));
  // okuyan (Adj, Noun)
  public static final Morpheme feelLike = addMorpheme(derivational("FeelLike", "FeelLike"));
  // okuyagel (Verb)
  public static final Morpheme everSince = addMorpheme(
      derivational("EverSince", "EverSince"));
  // okuyadur, okuyagör (Verb)
  public static final Morpheme repeat = addMorpheme(derivational("Repeat", "Repeat"));
  // okuyayaz (Verb)
  public static final Morpheme almost = addMorpheme(derivational("Almost", "Almost"));
  // okuyuver (Verb)
  public static final Morpheme hastily = addMorpheme(derivational("Hastily", "Hastily"));
  // okuyakal (Verb)
  public static final Morpheme stay = addMorpheme(derivational("Stay", "Stay"));
  // okuyakoy (Verb)
  public static final Morpheme start = addMorpheme(derivational("Start", "Start"));
  // okurcasına (Adv,Adj)
  public static final Morpheme asIf = addMorpheme(derivational("AsIf", "AsIf"));
  // okurken (Adv)
  public static final Morpheme while_ = addMorpheme(derivational("While", "While"));
  // okuyunca (Adv)
  public static final Morpheme when = addMorpheme(derivational("When", "When"));
  // okuyalı (Adv)
  public static final Morpheme sinceDoingSo = addMorpheme(
      derivational("SinceDoingSo", "SinceDoingSo"));
  // okudukça (Adv)
  public static final Morpheme asLongAs = addMorpheme(derivational("AsLongAs", "AsLongAs"));
  // okuyarak (Adv)
  public static final Morpheme byDoingSo = addMorpheme(
      derivational("ByDoingSo", "ByDoingSo"));
  // okuyasıya (Adv)
  public static final Morpheme adamantly = addMorpheme(
      derivational("Adamantly", "Adamantly"));
  // okuyup (Adv)
  public static final Morpheme afterDoingSo = addMorpheme(
      derivational("AfterDoingSo", "AfterDoingSo"));
  // okumadan, okumaksızın (Adv)
  public static final Morpheme withoutHavingDoneSo =
      addMorpheme(derivational("WithoutHavingDoneSo", "WithoutHavingDoneSo"));
  //okuyamadan (Adv)
  public static final Morpheme withoutBeingAbleToHaveDoneSo =
      addMorpheme(
          derivational("WithoutBeingAbleToHaveDoneSo", "WithoutBeingAbleToHaveDoneSo"));

  // Zero derivation
  public static final Morpheme zero = addMorpheme(derivational("Zero", "Zero"));

  // Verb specific
  public static final Morpheme cop = addMorpheme(instance("Copula", "Cop"));

  // Negative Verb
  public static final Morpheme neg = addMorpheme(instance("Negative", "Neg"));
  // Unable (Negative - Ability such as "okuyamıyorum - I cannot read, I am unable to read.")
  public static final Morpheme unable = addMorpheme(instance("Unable", "Unable"));

  // Tense
  public static final Morpheme pres = addMorpheme(instance("PresentTense", "Pres"));
  public static final Morpheme past = addMorpheme(instance("PastTense", "Past"));
  public static final Morpheme narr = addMorpheme(instance("NarrativeTense", "Narr"));
  public static final Morpheme cond = addMorpheme(instance("Condition", "Cond"));
  // oku-yor
  public static final Morpheme prog1 = addMorpheme(instance("Progressive1", "Prog1"));
  // oku-makta
  public static final Morpheme prog2 = addMorpheme(instance("Progressive2", "Prog2"));
  // oku-r
  public static final Morpheme aor = addMorpheme(instance("Aorist", "Aor"));
  // oku-yacak
  public static final Morpheme fut = addMorpheme(instance("Future", "Fut"));

  // gel, gel-sin
  public static final Morpheme imp = addMorpheme(instance("Imparative", "Imp"));
  // oku-ya
  public static final Morpheme opt = addMorpheme(instance("Optative", "Opt"));
  // oku-sa
  public static final Morpheme desr = addMorpheme(instance("Desire", "Desr"));
  // oku-malı
  public static final Morpheme neces = addMorpheme(instance("Necessity", "Neces"));

  //-------------- States ----------------------------
  // _ST = Terminal state _S = Non Terminal State.
  // A terminal state means that a walk in the graph can end there.

  // root of the graph.
  MorphemeState root_S = nonTerminal("root_S", root);

  MorphemeState puncRoot_ST = builder("puncRoot_ST", punc).terminal().posRoot().build();

  //-------------- Noun States ------------------------

  MorphemeState noun_S = builder("noun_S", noun).posRoot().build();
  MorphemeState nounCompoundRoot_S = builder("nounCompoundRoot_S", noun).posRoot().build();
  MorphemeState nounSuRoot_S = builder("nounSuRoot_S", noun).posRoot().build();
  MorphemeState nounInf1Root_S = builder("nounInf1Root_S", noun).posRoot().build();
  MorphemeState nounActOfRoot_S = builder("nounActOfRoot_S", noun).posRoot().build();

  // Number-Person agreement

  MorphemeState a3sg_S = nonTerminal("a3sg_S", a3sg);
  MorphemeState a3sgSu_S = nonTerminal("a3sgSu_S", a3sg);
  MorphemeState a3sgCompound_S = nonTerminal("a3sgCompound_S", a3sg);
  MorphemeState a3sgInf1_S = nonTerminal("a3sgInf1_S", a3sg);
  MorphemeState a3sgActOf_S = nonTerminal("a3sgActOf_S", a3sg);
  MorphemeState a3pl_S = nonTerminal("a3pl_S", a3pl);
  MorphemeState a3plActOf_S = nonTerminal("a3plActOf_S", a3pl);
  MorphemeState a3plCompound_S = nonTerminal("a3plCompound_S", a3pl);
  MorphemeState a3plCompound2_S = nonTerminal("a3plCompound2_S", a3pl);

  // Possessive

  MorphemeState pnon_S = nonTerminal("pnon_S", pnon);
  MorphemeState pnonCompound_S = nonTerminal("pnonCompound_S", pnon);
  MorphemeState pnonCompound2_S = nonTerminal("pnonCompound2_S", pnon);
  MorphemeState pnonInf1_S = nonTerminal("pnonInf1_S", pnon);
  MorphemeState pnonActOf = nonTerminal("pnonActOf", pnon);
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
  MorphemeState gen_ST = terminal("gen_ST", gen);
  MorphemeState equ_ST = terminal("equ_ST", equ);

  // Derivation

  MorphemeState dim_S = nonTerminalDerivative("dim_S", dim);
  MorphemeState ness_S = nonTerminalDerivative("ness_S", ness);
  MorphemeState agt_S = nonTerminalDerivative("agt_S", agt);
  MorphemeState related_S = nonTerminalDerivative("related_S", related);
  MorphemeState rel_S = nonTerminalDerivative("rel_S", rel);
  MorphemeState relToPron_S = nonTerminalDerivative("relToPron_S", rel);
  MorphemeState with_S = nonTerminalDerivative("with_S", with);
  MorphemeState without_S = nonTerminalDerivative("without_S", without);
  MorphemeState justLike_S = nonTerminalDerivative("justLike_S", justLike);
  MorphemeState nounZeroDeriv_S = nonTerminalDerivative("nounZeroDeriv_S", zero);
  MorphemeState become_S = nonTerminalDerivative("become_S", become);
  MorphemeState acquire_S = nonTerminalDerivative("acquire_S", acquire);

  //-------------- Conditions ------------------------------

  protected RootLexicon lexicon;

  StemTransitions stemTransitions;

  protected TurkishMorphotactics() {
  }

  protected static Morpheme addMorpheme(Morpheme morpheme) {
    morphemeMap.put(morpheme.id, morpheme);
    return morpheme;
  }

  public StemTransitions getStemTransitions() {
    return stemTransitions;
  }

  public RootLexicon getRootLexicon() {
    return lexicon;
  }

  public static Morpheme getMorpheme(String id) {
    return morphemeMap.get(id);
  }

  public static List<Morpheme> getMorphemes(List<String> ids) {
    return ids.stream().map(TurkishMorphotactics::getMorpheme).collect(Collectors.toList());
  }

  public static List<Morpheme> getAllMorphemes() {
    List<Morpheme> morphemes = new ArrayList<>(morphemeMap.values());
    morphemes.sort(Comparator.comparing(a -> a.id));
    return morphemes;
  }


  public List<Morpheme> getMorphemes(String... ids) {
    List<Morpheme> morphemes = new ArrayList<>(ids.length);
    for (String id : ids) {
      if(id.length()==0) {
        continue;
      }
      morphemes.add(getMorpheme(id));
    }
    return morphemes;
  }

  public TurkishMorphotactics(RootLexicon lexicon) {
    this.lexicon = lexicon;
    makeGraph();
    this.stemTransitions = new StemTransitionsMapBased(lexicon, this);
  }

  protected void makeGraph() {
    mapSpecialItemsToRootStates();
    connectNounStates();
    connectProperNounsAndAbbreviations();
    connectAdjectiveStates();
    connectNumeralStates();
    connectVerbAfterNounAdjStates();
    connectPronounStates();
    connectVerbAfterPronoun();
    connectVerbs();
    connectQuestion();
    connectAdverbs();
    connectLastVowelDropWords();
    connectPostpositives();
    connectImek();
    handlePostProcessingConnections();
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

    // ---- For compund derivations -----------------
    pnonCompound_S.addEmpty(nom_S);
    nom_S.add(become_S, "lAş");
    nom_S.add(acquire_S, "lAn");
    // for "zeytinyağlı"
    nom_S.add(with_S, "lI", new ContainsMorpheme(with, without).not());
    // for "zeytinyağsız"
    nom_S.add(without_S, "sIz", new ContainsMorpheme(with, without).not());
    // for "zeytinyağlık"
    ContainsMorpheme containsNess = new ContainsMorpheme(ness);
    nom_S.add(ness_S, "lI~k", not(containsNess));
    nom_S.add(ness_S, "lI!ğ", not(containsNess));
    // for "zeytinyağcı"
    nom_S.add(agt_S, ">cI", not(new ContainsMorpheme(agt)));
    // for "zeytinyağsı"
    nom_S.add(justLike_S, "+msI", not(new ContainsMorpheme(justLike)));
    // for "zeytinyağcık"
    nom_S.add(dim_S, ">cI~k",
        Conditions.HAS_NO_SURFACE.andNot(new ContainsMorpheme(dim)));
    nom_S.add(dim_S, ">cI!ğ",
        Conditions.HAS_NO_SURFACE.andNot(new ContainsMorpheme(dim)));
    // "zeytinyağcağız"
    nom_S.add(dim_S, "cAğIz", Conditions.HAS_NO_SURFACE);

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

    // this path is used for plural analysis (A3pl+Pnon+Nom) of compound words.
    a3plCompound2_S.addEmpty(pnonCompound2_S);
    pnonCompound2_S.addEmpty(nom_ST);

    // ------


    // do not allow possessive suffixes for abbreviations or words like "annemler"
    Condition rootIsAbbrv = new SecondaryPosIs(SecondaryPos.Abbreviation);

    Condition possessionCond = notHave(RootAttribute.FamilyMember)
        .andNot(rootIsAbbrv);

    a3sg_S
        .addEmpty(pnon_S, notHave(RootAttribute.FamilyMember))        // ev
        .add(p1sg_S, "Im", possessionCond)       // evim
        .add(p2sg_S, "In", possessionCond
            .andNot(new Conditions.PreviousGroupContainsMorpheme(justLike)))  // evin
        .add(p3sg_S, "+sI", possessionCond)      // evi, odası
        .addEmpty(p3sg_S,
            has(RootAttribute.CompoundP3sg))  // "zeytinyağı" has two analyses. Pnon and P3sg.
        .add(p1pl_S, "ImIz", possessionCond)     // evimiz
        .add(p2pl_S, "InIz", possessionCond
            .andNot(new Conditions.PreviousGroupContainsMorpheme(justLike)))  // eviniz
        .add(p3pl_S, "lArI", possessionCond);    // evleri

    // ev-ler-ε-?
    a3pl_S.addEmpty(pnon_S, notHave(RootAttribute.FamilyMember));

    // ev-ler-im-?
    a3pl_S
        .add(p1sg_S, "Im", possessionCond)
        .add(p2sg_S, "In", possessionCond)
        .addEmpty(p1sg_S, has(RootAttribute.ImplicitP1sg)) // for words like "annemler"
        .addEmpty(p2sg_S, has(RootAttribute.ImplicitP2sg)) // for words like "annenler"
        .add(p3sg_S, "I", possessionCond)
        .add(p1pl_S, "ImIz", possessionCond)
        .add(p2pl_S, "InIz", possessionCond)
        .add(p3pl_S, "I", possessionCond);

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
    pnon_S.addEmpty(nom_ST, notHave(RootAttribute.FamilyMember));

    Condition equCond =
        Conditions.prviousMorphemeIs(a3pl).or(
            new Conditions.ContainsMorpheme(adj, futPart, presPart, narrPart, pastPart).not())
            .or(new Conditions.ContainsMorphemeSequence(able, verb,
                pastPart)); // allow `yapabildiğince`

    // Not allow "zetinyağı-ya" etc.
    pnon_S
        .add(dat_ST, "+yA", notHave(RootAttribute.CompoundP3sg))   // ev-e
        .add(abl_ST, ">dAn", notHave(RootAttribute.CompoundP3sg))  // ev-den
        .add(loc_ST, ">dA", notHave(RootAttribute.CompoundP3sg))   // evde
        .add(acc_ST, "+yI", notHave(RootAttribute.CompoundP3sg))   // evi
        .add(gen_ST, "+nIn", previousStateIsNot(a3sgSu_S))         // evin, zeytinyağının
        .add(gen_ST, "yIn", previousStateIs(a3sgSu_S))             // suyun
        .add(equ_ST, ">cA", notHave(RootAttribute.CompoundP3sg).and(equCond))   // evce
        .add(ins_ST, "+ylA");                                      // evle, zeytinyağıyla

    pnon_S.add(dat_ST, "+nA", has(RootAttribute.CompoundP3sg))   // zeytinyağı-na
        .add(abl_ST, "+ndAn", has(RootAttribute.CompoundP3sg))   // zeytinyağı-ndan
        .add(loc_ST, "+ndA", has(RootAttribute.CompoundP3sg))    // zeytinyağı-nda
        .add(equ_ST, "+ncA", has(RootAttribute.CompoundP3sg).and(equCond))    // zeytinyağı-nca
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
        .add(gen_ST, "In")   // evimin
        .add(equ_ST, "cA", equCond.or(new Conditions.ContainsMorpheme(pastPart)))   // evimce
        .add(acc_ST, "I");   // evimi

    p2sg_S
        .addEmpty(nom_ST)    // evin
        .add(dat_ST, "A")    // evine
        .add(loc_ST, "dA")   // evinde
        .add(abl_ST, "dAn")  // evinden
        .add(ins_ST, "lA")   // evinle
        .add(gen_ST, "In")   // evinin
        .add(equ_ST, "cA", equCond.or(new Conditions.ContainsMorpheme(pastPart)))   // evince
        .add(acc_ST, "I");   // evini

    p3sg_S
        .addEmpty(nom_ST)    // evi
        .add(dat_ST, "nA")   // evine
        .add(loc_ST, "ndA")  // evinde
        .add(abl_ST, "ndAn") // evinden
        .add(ins_ST, "ylA")  // eviyle
        .add(gen_ST, "nIn")  // evinin
        .add(equ_ST, "ncA", equCond.or(new Conditions.ContainsMorpheme(pastPart)))// evince
        .add(acc_ST, "nI");  // evini

    p1pl_S
        .addEmpty(nom_ST)    // evimiz
        .add(dat_ST, "A")    // evimize
        .add(loc_ST, "dA")   // evimizde
        .add(abl_ST, "dAn")  // evimizden
        .add(ins_ST, "lA")   // evimizden
        .add(gen_ST, "In")   // evimizin
        .add(equ_ST, "cA", equCond.or(new Conditions.ContainsMorpheme(pastPart)))   // evimizce
        .add(acc_ST, "I");   // evimizi

    p2pl_S
        .addEmpty(nom_ST)    // eviniz
        .add(dat_ST, "A")    // evinize
        .add(loc_ST, "dA")   // evinizde
        .add(abl_ST, "dAn")  // evinizden
        .add(ins_ST, "lA")   // evinizle
        .add(gen_ST, "In")   // evinizin
        .add(equ_ST, "cA", equCond.or(new Conditions.ContainsMorpheme(pastPart)))  // evinizce
        .add(acc_ST, "I");   // evinizi

    p3pl_S
        .addEmpty(nom_ST)     // evleri
        .add(dat_ST, "nA")    // evlerine
        .add(loc_ST, "ndA")   // evlerinde
        .add(abl_ST, "ndAn")  // evlerinden
        .add(ins_ST, "ylA")   // evleriyle
        .add(gen_ST, "nIn")   // evlerinin
        // For now we omit equCond check because adj+..+A3pl+..+Equ fails.
        .add(equ_ST, "+ncA")   // evlerince.
        .add(acc_ST, "nI");   // evlerini

    // ev-ε-ε-ε-cik (evcik). Disallow this path if visitor contains any non empty surface suffix.
    // There are two almost identical suffix transitions with templates ">cI~k" and ">cI!ğ"
    // This was necessary for some simplification during analysis. This way there will be only one
    // surface form generated for each transition.
    nom_ST.add(dim_S, ">cI~k", Conditions.HAS_NO_SURFACE.andNot(rootIsAbbrv));
    nom_ST.add(dim_S, ">cI!ğ", Conditions.HAS_NO_SURFACE.andNot(rootIsAbbrv));

    // ev-ε-ε-ε-ceğiz (evceğiz)
    nom_ST.add(dim_S, "cAğIz", Conditions.HAS_NO_SURFACE.andNot(rootIsAbbrv));

    // connect dim to the noun root.
    dim_S.addEmpty(noun_S);

    Condition emptyAdjNounSeq = new ContainsMorphemeSequence(adj, zero, noun, a3sg, pnon, nom);

    nom_ST.add(ness_S, "lI~k",
        Conditions.CURRENT_GROUP_EMPTY
            .andNot(containsNess)
            .andNot(emptyAdjNounSeq)
            .andNot(rootIsAbbrv));
    nom_ST.add(ness_S, "lI!ğ",
        Conditions.CURRENT_GROUP_EMPTY
            .andNot(containsNess)
            .andNot(emptyAdjNounSeq)
            .andNot(rootIsAbbrv));

    // connect `ness` to the noun root.
    ness_S.addEmpty(noun_S);

    nom_ST.add(agt_S, ">cI",
        Conditions.CURRENT_GROUP_EMPTY.andNot(new ContainsMorpheme(adj, agt)));

    // connect `ness` to the noun root.
    agt_S.addEmpty(noun_S);

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

    // elma-nın-ım elma-nın-dı
    gen_ST.addEmpty(nounZeroDeriv_S, noun2VerbZeroDerivationCondition);

    nounZeroDeriv_S.addEmpty(nVerb_S);

    // meyve-li
    Condition noSurfaceAfterDerivation = new NoSurfaceAfterDerivation();
    nom_ST.add(with_S, "lI",
        noSurfaceAfterDerivation
            .andNot(new ContainsMorpheme(with, without))
            .andNot(rootIsAbbrv));

    nom_ST.add(without_S, "sIz",
        noSurfaceAfterDerivation
            .andNot(new ContainsMorpheme(with, without, inf1))
            .andNot(rootIsAbbrv));

    nom_ST.add(justLike_S, "+msI",
        noSurfaceAfterDerivation
            .andNot(new ContainsMorpheme(justLike, futPart, pastPart, presPart, adj))
            .andNot(rootIsAbbrv));

    nom_ST.add(justLike_S, "ImsI",
        notHave(PhoneticAttribute.LastLetterVowel)
            .and(noSurfaceAfterDerivation)
            .andNot(new ContainsMorpheme(justLike, futPart, pastPart, presPart, adj))
            .andNot(rootIsAbbrv));

    nom_ST.add(related_S, "sAl",
        noSurfaceAfterDerivation
            .andNot(new ContainsMorpheme(with, without, related))
            .andNot(rootIsAbbrv));

    // connect With to Adjective root.
    with_S.addEmpty(adjectiveRoot_ST);
    without_S.addEmpty(adjectiveRoot_ST);
    related_S.addEmpty(adjectiveRoot_ST);

    justLike_S.addEmpty(adjectiveRoot_ST);

    // meyve-de-ki
    Condition notRelRepetition = new HasTailSequence(rel, adj, zero, noun, a3sg, pnon, loc).not();
    loc_ST.add(rel_S, "ki", notRelRepetition);
    rel_S.addEmpty(adjectiveRoot_ST);

    // for covering dünkü, anki, yarınki etc. Unlike Oflazer, We also allow dündeki etc.
    // TODO: Use a more general grouping, not using Secondary Pos
    Condition time = Conditions.CURRENT_GROUP_EMPTY.and(
        new SecondaryPosIs(SecondaryPos.Time));
    DictionaryItem dun = lexicon.getItemById("dün_Noun_Time");
    DictionaryItem gun = lexicon.getItemById("gün_Noun_Time");
    DictionaryItem bugun = lexicon.getItemById("bugün_Noun_Time");
    DictionaryItem ileri = lexicon.getItemById("ileri_Noun");
    DictionaryItem geri = lexicon.getItemById("geri_Noun");
    DictionaryItem ote = lexicon.getItemById("öte_Noun");
    DictionaryItem beri = lexicon.getItemById("beri_Noun");

    Condition time2 = Conditions.rootIsAny(dun, gun, bugun);
    nom_ST.add(rel_S, "ki", time.andNot(time2));
    nom_ST.add(rel_S, "ki", Conditions.rootIsAny(ileri, geri, ote, beri));
    nom_ST.add(rel_S, "kü", time2.and(time));

    // After Genitive suffix, Rel suffix makes a Pronoun derivation.
    gen_ST.add(relToPron_S, "ki");
    relToPron_S.addEmpty(pronAfterRel_S);

    ContainsMorpheme verbDeriv = new ContainsMorpheme(inf1, inf2, inf3, pastPart, futPart);

    nom_ST.add(become_S, "lAş",
        noSurfaceAfterDerivation.andNot(new ContainsMorpheme(adj))
            .andNot(verbDeriv)
            .andNot(rootIsAbbrv));
    become_S.addEmpty(verbRoot_S);

    nom_ST.add(acquire_S, "lAn",
        noSurfaceAfterDerivation.andNot(new ContainsMorpheme(adj))
            .andNot(verbDeriv)
            .andNot(rootIsAbbrv));

    acquire_S.addEmpty(verbRoot_S);

    // Inf1 mak makes noun derivation. However, it cannot get any possessive or plural suffix.
    // Also cannot be followed by Dat, Gen, Acc case suffixes.
    // So we create a path only for it.
    nounInf1Root_S.addEmpty(a3sgInf1_S);
    a3sgInf1_S.addEmpty(pnonInf1_S);
    pnonInf1_S.addEmpty(nom_ST);
    pnonInf1_S.add(abl_ST, "tAn");
    pnonInf1_S.add(loc_ST, "tA");
    pnonInf1_S.add(ins_ST, "lA");

    nounActOfRoot_S.addEmpty(a3sgActOf_S);
    nounActOfRoot_S.add(a3plActOf_S, "lar");
    a3sgActOf_S.addEmpty(pnonActOf);
    a3plActOf_S.addEmpty(pnonActOf);
    pnonActOf.addEmpty(nom_ST);

  }

  //-------- Morphotactics for modified forms of words like "içeri->içerde"
  public MorphemeState nounLastVowelDropRoot_S =
      builder("nounLastVowelDropRoot_S", noun).posRoot().build();
  public MorphemeState adjLastVowelDropRoot_S =
      builder("adjLastVowelDropRoot_S", adj).posRoot().build();
  public MorphemeState postpLastVowelDropRoot_S =
      builder("postpLastVowelDropRoot_S", postp).posRoot().build();
  MorphemeState a3PlLastVowelDrop_S = nonTerminal("a3PlLastVowelDrop_S", a3pl);
  MorphemeState a3sgLastVowelDrop_S = nonTerminal("a3sgLastVowelDrop_S", a3sg);
  MorphemeState pNonLastVowelDrop_S = nonTerminal("pNonLastVowelDrop_S", pnon);
  MorphemeState zeroLastVowelDrop_S = nonTerminalDerivative("zeroLastVowelDrop_S", zero);

  private void connectLastVowelDropWords() {
    nounLastVowelDropRoot_S.addEmpty(a3sgLastVowelDrop_S);
    nounLastVowelDropRoot_S.add(a3PlLastVowelDrop_S, "lAr");
    a3sgLastVowelDrop_S.addEmpty(pNonLastVowelDrop_S);
    a3PlLastVowelDrop_S.addEmpty(pNonLastVowelDrop_S);
    pNonLastVowelDrop_S.add(loc_ST, ">dA");
    pNonLastVowelDrop_S.add(abl_ST, ">dAn");

    adjLastVowelDropRoot_S.addEmpty(zeroLastVowelDrop_S);
    postpLastVowelDropRoot_S.addEmpty(zeroLastVowelDrop_S);
    zeroLastVowelDrop_S.addEmpty(nounLastVowelDropRoot_S);
  }

  MorphemeState nounProper_S = builder("nounProper_S", noun).posRoot().build();
  MorphemeState nounAbbrv_S = builder("nounAbbrv_S", noun).posRoot().build();
  // this will be used for proper noun separation.
  MorphemeState puncProperSeparator_S = nonTerminal("puncProperSeparator_S", punc);

  MorphemeState nounNoSuffix_S = builder("nounNoSuffix_S", noun).posRoot().build();
  MorphemeState nounA3sgNoSuffix_S = nonTerminal("nounA3sgNoSuffix_S", a3sg);
  MorphemeState nounPnonNoSuffix_S = nonTerminal("nounPnonNoSuffix_S", pnon);
  MorphemeState nounNomNoSuffix_ST = terminal("nounNomNoSuffix_S", nom);

  private void connectProperNounsAndAbbreviations() {
    // ---- Proper noun handling -------
    // TODO: consider adding single quote after an overhaul.
    // nounProper_S.add(puncProperSeparator_S, "'");
    nounProper_S.addEmpty(a3sg_S);
    nounProper_S.add(a3pl_S, "lAr");
    puncProperSeparator_S.addEmpty(a3sg_S);
    puncProperSeparator_S.add(a3pl_S, "lAr");

    // ---- Abbreviation Handling -------
    // TODO: consider restricting possessive, most derivation and plural suffixes.
    nounAbbrv_S.addEmpty(a3sg_S);
    nounAbbrv_S.add(a3pl_S, "lAr");

    //----- This is for catching words that cannot have a suffix.
    nounNoSuffix_S.addEmpty(nounA3sgNoSuffix_S);
    nounA3sgNoSuffix_S.addEmpty(nounPnonNoSuffix_S);
    nounPnonNoSuffix_S.addEmpty(nounNomNoSuffix_ST);
  }

  //-------------- Adjective States ------------------------

  MorphemeState adjectiveRoot_ST = builder("adjectiveRoot_ST", adj).terminal().posRoot().build();
  MorphemeState adjAfterVerb_S = builder("adjAfterVerb_S", adj).posRoot().build();
  MorphemeState adjAfterVerb_ST = builder("adjAfterVerb_ST", adj).terminal().posRoot().build();

  MorphemeState adjZeroDeriv_S = nonTerminalDerivative("adjZeroDeriv_S", zero);

  // After verb->adj derivations Adj can get possesive suffixes.
  // Such as "oku-duğ-um", "okuyacağı"
  MorphemeState aPnon_ST = terminal("aPnon_ST", pnon);
  MorphemeState aP1sg_ST = terminal("aP1sg_ST", p1sg);
  MorphemeState aP2sg_ST = terminal("aP2sg_ST", p2sg);
  MorphemeState aP3sg_ST = terminal("aP3sg_ST", p3sg);
  MorphemeState aP1pl_ST = terminal("aP3sg_ST", p1pl);
  MorphemeState aP2pl_ST = terminal("aP2pl_ST", p2pl);
  MorphemeState aP3pl_ST = terminal("aP3pl_ST", p3pl);

  MorphemeState aLy_S = nonTerminalDerivative("aLy_S", ly);
  MorphemeState aAsIf_S = nonTerminalDerivative("aAsIf_S", asIf);
  MorphemeState aAgt_S = nonTerminalDerivative("aAgt_S", agt);

  private void connectAdjectiveStates() {

    // zero morpheme derivation. Words like "yeşil-i" requires Adj to Noun conversion.
    // Since noun suffixes are not derivational a "Zero" morpheme is used for this.
    // Transition has a HAS_TAIL condition because Adj->Zero->Noun+A3sg+Pnon+Nom) is not allowed.
    adjectiveRoot_ST.addEmpty(adjZeroDeriv_S, Conditions.HAS_TAIL);

    adjZeroDeriv_S.addEmpty(noun_S);

    adjZeroDeriv_S.addEmpty(nVerb_S);

    adjectiveRoot_ST.add(aLy_S, ">cA");
    aLy_S.addEmpty(advRoot_ST);

    adjectiveRoot_ST
        .add(aAsIf_S, ">cA", new Conditions.ContainsMorpheme(asIf, ly, agt, with, justLike).not());
    aAsIf_S.addEmpty(adjectiveRoot_ST);

    adjectiveRoot_ST
        .add(aAgt_S, ">cI", new Conditions.ContainsMorpheme(asIf, ly, agt, with, justLike).not());
    aAgt_S.addEmpty(noun_S);

    adjectiveRoot_ST.add(justLike_S, "+msI",
        new NoSurfaceAfterDerivation()
            .and(new ContainsMorpheme(justLike).not()));

    adjectiveRoot_ST.add(justLike_S, "ImsI",
        notHave(PhoneticAttribute.LastLetterVowel)
            .and(new NoSurfaceAfterDerivation())
            .and(new ContainsMorpheme(justLike).not()));

    adjectiveRoot_ST.add(become_S, "lAş", new NoSurfaceAfterDerivation());
    adjectiveRoot_ST.add(acquire_S, "lAn", new NoSurfaceAfterDerivation());

    Condition c1 = new Conditions.PreviousMorphemeIsAny(futPart, pastPart);

    adjAfterVerb_S.addEmpty(aPnon_ST, c1);
    adjAfterVerb_S.add(aP1sg_ST, "Im", c1);
    adjAfterVerb_S.add(aP2sg_ST, "In", c1);
    adjAfterVerb_S.add(aP3sg_ST, "I", c1);
    adjAfterVerb_S.add(aP1pl_ST, "ImIz", c1);
    adjAfterVerb_S.add(aP2pl_ST, "InIz", c1);
    adjAfterVerb_S.add(aP3pl_ST, "lArI", c1);

    adjectiveRoot_ST.add(ness_S, "lI~k");
    adjectiveRoot_ST.add(ness_S, "lI!ğ");

    adjAfterVerb_ST.add(ness_S, "lI~k", new Conditions.PreviousMorphemeIs(aorPart));
    adjAfterVerb_ST.add(ness_S, "lI!ğ", new Conditions.PreviousMorphemeIs(aorPart));
  }

  //--------------------- Numeral Root --------------------------------------------------
  MorphemeState numeralRoot_ST = builder("numeralRoot_ST", num).terminal().posRoot().build();
  MorphemeState numZeroDeriv_S = nonTerminalDerivative("numZeroDeriv_S", zero);

  private void connectNumeralStates() {
    numeralRoot_ST.add(ness_S, "lI~k");
    numeralRoot_ST.add(ness_S, "lI!ğ");
    numeralRoot_ST.addEmpty(numZeroDeriv_S, Conditions.HAS_TAIL);
    numZeroDeriv_S.addEmpty(noun_S);
    numZeroDeriv_S.addEmpty(nVerb_S);

    numeralRoot_ST.add(justLike_S, "+msI",
        new NoSurfaceAfterDerivation()
            .and(new ContainsMorpheme(justLike).not()));

    numeralRoot_ST.add(justLike_S, "ImsI",
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
  MorphemeState nA2pl_ST = terminal("nA2pl_ST", a2pl);
  MorphemeState nA3sg_ST = terminal("nA3sg_ST", a3sg);
  MorphemeState nA3sg_S = nonTerminal("nA3sg_S", a3sg);
  MorphemeState nA3pl_ST = terminal("nA3pl_ST", a3pl);

  MorphemeState nCop_ST = terminal("nCop_ST", cop);
  MorphemeState nCopBeforeA3pl_S = nonTerminal("nCopBeforeA3pl_S", cop);

  MorphemeState nNeg_S = nonTerminal("nNeg_S", neg);

  private void connectVerbAfterNounAdjStates() {

    //elma-..-ε-yım
    nVerb_S.addEmpty(nPresent_S);

    // elma-ydı, çorap-tı
    nVerb_S.add(nPast_S, "+y>dI");
    // elma-ymış
    nVerb_S.add(nNarr_S, "+ymIş");

    nVerb_S.add(nCond_S, "+ysA");

    nVerb_S.add(vWhile_S, "+yken");

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
    // TODO: below causes "beklemedeyiz" to fail.
    ContainsMorpheme verbDeriv = new ContainsMorpheme(inf1, inf2, inf3, pastPart, futPart);
    Condition allowA1sgTrans =
        noFamily
            .andNot(new Conditions.ContainsMorphemeSequence(p1sg, nom))
            .andNot(verbDeriv);
    Condition allowA2sgTrans =
        noFamily
            .andNot(new Conditions.ContainsMorphemeSequence(p2sg, nom))
            .andNot(verbDeriv);
    Condition allowA3plTrans =
        noFamily
            .andNot(new Conditions.PreviousGroupContains(a3pl_S))
            .andNot(new Conditions.ContainsMorphemeSequence(p3pl, nom))
            .andNot(verbDeriv);
    Condition allowA2plTrans =
        noFamily
            .andNot(new Conditions.ContainsMorphemeSequence(p2pl, nom))
            .andNot(verbDeriv);
    Condition allowA1plTrans =
        noFamily
            .andNot(new Conditions.ContainsMorphemeSequence(p1sg, nom))
            .andNot(new Conditions.ContainsMorphemeSequence(p1pl, nom))
            .andNot(verbDeriv);
    // elma-yım
    nPresent_S.add(nA1sg_ST, "+yIm", allowA1sgTrans);
    nPresent_S.add(nA2sg_ST, "sIn", allowA2sgTrans);

    // elma-ε-ε-dır to non terminal A3sg. We do not allow ending with A3sg from empty Present tense.
    nPresent_S.addEmpty(nA3sg_S);

    // we allow `değil` to end with terminal A3sg from Present tense.
    nPresent_S.addEmpty(nA3sg_ST, rootIs(degilRoot));

    // elma-lar, elma-da-lar as Verb.
    // TODO: consider disallowing this for "elmalar" case.
    nPresent_S.add(nA3pl_ST, "lAr",
        notHave(RootAttribute.CompoundP3sg)
            // do not allow "okumak-lar"
            .andNot(new Conditions.PreviousGroupContainsMorpheme(inf1))
            .and(allowA3plTrans));

    // elma-ydı-m. Do not allow "elmaya-yım" (Oflazer accepts this)
    nPast_S.add(nA1sg_ST, "m", allowA1sgTrans);
    nNarr_S.add(nA1sg_ST, "Im", allowA1sgTrans);

    nPast_S.add(nA2sg_ST, "n", allowA2sgTrans);
    nNarr_S.add(nA2sg_ST, "sIn", allowA2sgTrans);

    nPast_S.add(nA1pl_ST, "k", allowA1plTrans);
    nNarr_S.add(nA1pl_ST, "Iz", allowA1plTrans);
    nPresent_S.add(nA1pl_ST, "+yIz", allowA1plTrans);

    nPast_S.add(nA2pl_ST, "InIz", allowA2plTrans);
    nNarr_S.add(nA2pl_ST, "sInIz", allowA2plTrans);
    nPresent_S.add(nA2pl_ST, "sInIz", allowA2plTrans);

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
    nCond_S.add(nA2sg_ST, "n", allowA2sgTrans);
    nCond_S.add(nA1pl_ST, "k", allowA1plTrans);
    nCond_S.add(nA2pl_ST, "nIz", allowA2plTrans);
    nCond_S.addEmpty(nA3sg_ST);
    nCond_S.add(nA3pl_ST, "lAr");

    // for not allowing "elma-ydı-m-dır"
    Condition rejectNoCopula = new CurrentGroupContainsAny(nPast_S, nCond_S, nCopBeforeA3pl_S)
        .not();

    //elma-yım-dır
    nA1sg_ST.add(nCop_ST, "dIr", rejectNoCopula);
    // elmasındır
    nA2sg_ST.add(nCop_ST, "dIr", rejectNoCopula);
    // elmayızdır
    nA1pl_ST.add(nCop_ST, "dIr", rejectNoCopula);
    // elmasınızdır
    nA2pl_ST.add(nCop_ST, "dIr", rejectNoCopula);

    nA3sg_S.add(nCop_ST, ">dIr", rejectNoCopula);

    nA3pl_ST.add(nCop_ST, "dIr", rejectNoCopula);

    PreviousMorphemeIsAny asIfCond = new PreviousMorphemeIsAny(narr);
    nA3sg_ST.add(vAsIf_S, ">cAsInA", asIfCond);
    nA1sg_ST.add(vAsIf_S, ">cAsInA", asIfCond);
    nA2sg_ST.add(vAsIf_S, ">cAsInA", asIfCond);
    nA1pl_ST.add(vAsIf_S, ">cAsInA", asIfCond);
    nA2pl_ST.add(vAsIf_S, ">cAsInA", asIfCond);
    nA3pl_ST.add(vAsIf_S, ">cAsInA", asIfCond);

    // Copula can come before A3pl.
    nPresent_S.add(nCopBeforeA3pl_S, ">dIr");
    nCopBeforeA3pl_S.add(nA3pl_ST, "lAr");

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
  // A root for noun->Rel->Pron derivation.
  public MorphemeState pronAfterRel_S = builder("pronAfterRel_S", pron).posRoot().build();

  MorphemeState pA1sg_S = nonTerminal("pA1sg_S", a1sg);
  MorphemeState pA2sg_S = nonTerminal("pA2sg_S", a2sg);

  MorphemeState pA1sgMod_S = nonTerminal("pA1sgMod_S", a1sg); // for modified ben
  MorphemeState pA2sgMod_S = nonTerminal("pA2sgMod_S", a2sg); // for modified sen

  MorphemeState pA3sg_S = nonTerminal("pA3sg_S", a3sg);
  MorphemeState pA3sgRel_S = nonTerminal("pA3sgRel_S", a3sg);
  MorphemeState pA1pl_S = nonTerminal("pA1pl_S", a1pl);
  MorphemeState pA2pl_S = nonTerminal("pA2pl_S", a2pl);

  MorphemeState pA3pl_S = nonTerminal("pA3pl_S", a3pl);
  MorphemeState pA3plRel_S = nonTerminal("pA3plRel_S", a3pl);

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
  MorphemeState pPnonRel_S = nonTerminal("pPnonRel_S", pnon);
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
  MorphemeState pAbl_ST = terminal("pAbl_ST", abl);
  MorphemeState pLoc_ST = terminal("pLoc_ST", loc);
  MorphemeState pGen_ST = terminal("pGen_ST", gen);
  MorphemeState pIns_ST = terminal("pIns_ST", ins);
  MorphemeState pEqu_ST = terminal("pEqu_ST", equ);

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
    pronPers_S.add(pA1pl_S, "lAr", rootIs(biz));
    pronPers_S.addEmpty(pA2pl_S, rootIs(siz));
    pronPers_S.add(pA2pl_S, "lAr", rootIs(siz));

    // --- modified `ben-sen` special state and transitions
    pronPers_Mod_S.addEmpty(pA1sgMod_S, rootIs(ben));
    pronPers_Mod_S.addEmpty(pA2sgMod_S, rootIs(sen));
    pA1sgMod_S.addEmpty(pPnonMod_S);
    pA2sgMod_S.addEmpty(pPnonMod_S);
    pPnonMod_S.add(pDat_ST, "A");
    // ----

    // Possesive connecitons are not used.
    pA1sg_S.addEmpty(pPnon_S);
    pA2sg_S.addEmpty(pPnon_S);
    pA3sg_S.addEmpty(pPnon_S);
    pA1pl_S.addEmpty(pPnon_S);
    pA2pl_S.addEmpty(pPnon_S);
    pA3pl_S.addEmpty(pPnon_S);

    //------------ Noun -> Rel -> Pron ---------------------------
    // masanınki
    pronAfterRel_S.addEmpty(pA3sgRel_S);
    pronAfterRel_S.add(pA3plRel_S, "lAr");
    pA3sgRel_S.addEmpty(pPnonRel_S);
    pA3plRel_S.addEmpty(pPnonRel_S);
    pPnonRel_S.addEmpty(pNom_ST);
    pPnonRel_S.add(pDat_ST, "+nA");
    pPnonRel_S.add(pAcc_ST, "+nI");
    pPnonRel_S.add(pAbl_ST, "+ndAn");
    pPnonRel_S.add(pLoc_ST, "+ndA");
    pPnonRel_S.add(pIns_ST, "+ylA");
    pPnonRel_S.add(pGen_ST, "+nIn");

    //------------ Demonstrative pronouns. ------------------------

    DictionaryItem bu = lexicon.getItemById("bu_Pron_Demons");
    DictionaryItem su = lexicon.getItemById("şu_Pron_Demons");
    DictionaryItem o_demons = lexicon.getItemById("o_Pron_Demons");

    pronDemons_S.addEmpty(pA3sg_S);
    pronDemons_S.add(pA3pl_S, "nlAr");

    //------------ Quantitiva Pronouns ----------------------------

    DictionaryItem birbiri = lexicon.getItemById("birbiri_Pron_Quant");
    DictionaryItem biri = lexicon.getItemById("biri_Pron_Quant");
    DictionaryItem bazi = lexicon.getItemById("bazı_Pron_Quant");
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

    // bazılarınız -> A1pl+P1pl
    pronQuant_S.add(pQuantA1pl_S, "lAr", rootIsAny(bazi));
    pronQuant_S.add(pQuantA2pl_S, "lAr", rootIsAny(bazi));

    // Herkes is implicitly plural.
    pronQuant_S.addEmpty(pQuantA3pl_S,
        rootIsAny(herkes, umum, birkaci, hepsi, cumlesi, cogu, bircogu, tumu, topu));

    // connect "kimse" to Noun-A3sg and Noun-A3pl. It behaves like a noun.
    pronQuant_S.addEmpty(a3sg_S, rootIs(kimse));
    pronQuant_S.add(a3pl_S, "lAr", rootIsAny(kimse));

    // for `birbiri-miz` `hep-imiz`
    pronQuant_S.addEmpty(pQuantA1pl_S,
        rootIsAny(biri, bazi, birbiri, birkaci, herbiri, hep, kimi,
            cogu, bircogu, tumu, topu, hicbiri));

    // for `birbiri-niz` and `hep-iniz`
    pronQuant_S.addEmpty(pQuantA2pl_S,
        rootIsAny(biri, bazi, birbiri, birkaci, herbiri, hep, kimi, cogu, bircogu, tumu, topu,
            hicbiri));

    // this is used for birbir-ler-i, çok-lar-ı, birçok-lar-ı separate root and A3pl states are
    // used for this.
    pronQuantModified_S.addEmpty(pQuantModA3pl_S);
    pQuantModA3pl_S.add(pP3pl_S, "lArI");

    // both `biri-ne` and `birisi-ne` or `birbirine` and `birbirisine` are accepted.
    pQuantA3sg_S.addEmpty(pP3sg_S,
        rootIsAny(biri, birbiri, kimi, herbiri, hicbiri, oburu, oburku, beriki)
            .and(notHave(PhoneticAttribute.ModifiedPronoun)));

    pQuantA3sg_S.add(pP3sg_S, "sI",
        rootIsAny(biri, bazi, kimi, birbiri, herbiri, hicbiri, oburku)
            .and(notHave(PhoneticAttribute.ModifiedPronoun)));

    // there is no connection from pQuantA3pl to Pnon for preventing `biriler` (except herkes)
    pQuantA3pl_S.add(pP3pl_S, "I", rootIsAny(biri, bazi, birbiri, kimi, oburku, beriki));
    pQuantA3pl_S.addEmpty(pP3pl_S, rootIsAny(hepsi, birkaci, cumlesi, cogu, tumu, topu, bircogu));
    pQuantA3pl_S.addEmpty(pPnon_S, rootIsAny(herkes, umum, oburku, beriki));

    pQuantA1pl_S.add(pP1pl_S, "ImIz");
    pQuantA2pl_S.add(pP2pl_S, "InIz");

    //------------ Question Pronouns ----------------------------
    // `kim` (kim_Pron_Ques), `ne` and `nere`
    DictionaryItem ne = lexicon.getItemById("ne_Pron_Ques");
    DictionaryItem nere = lexicon.getItemById("nere_Pron_Ques");
    DictionaryItem kim = lexicon.getItemById("kim_Pron_Ques");

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
    DictionaryItem kendi = lexicon.getItemById("kendi_Pron_Reflex");
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
    Condition nGroup = rootIsNone(ne, nere, falan, falanca, hep, herkes);
    Condition yGroup = rootIsAny(ne, nere, falan, falanca, hep, herkes);

    pPnon_S.addEmpty(pNom_ST)
        // not allowing `ben-e` and `sen-e`. `ban-a` and `san-a` are using different states
        .add(pDat_ST, "+nA", rootIsNone(ben, sen, ne, nere, falan, falanca, herkes))
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+nI", nGroup)
        .add(pAcc_ST, "+yI", yGroup)
        .add(pLoc_ST, "+ndA", nGroup)
        .add(pLoc_ST, ">dA", yGroup)
        .add(pAbl_ST, "+ndAn", nGroup)
        .add(pAbl_ST, ">dAn", yGroup)
        .add(pGen_ST, "+nIn", nGroup.and(rootIsNone(biz, ben, sen)))
        .add(pGen_ST, "im", rootIsAny(ben, biz)) // benim, senin, bizim are genitive.
        .add(pGen_ST, "in", rootIs(sen))
        .add(pGen_ST, "+yIn", yGroup.and(rootIsNone(biz)))
        .add(pEqu_ST, ">cA", yGroup)
        .add(pEqu_ST, ">cA", nGroup)
        .add(pIns_ST, "+ylA", yGroup)
        .add(pIns_ST, "+nlA", nGroup)
        .add(pIns_ST, "+nInlA", nGroup.and(rootIsAny(bu, su, o, sen)))
        .add(pIns_ST, "inle", rootIs(siz))
        .add(pIns_ST, "imle", rootIsAny(biz, ben));

    Condition conditionpP1sg_S = Conditions.rootIsAny(kim, ben, ne, nere, kendi);

    pP1sg_S
        .addEmpty(pNom_ST)
        .add(pDat_ST, "+nA", nGroup)
        .add(pAcc_ST, "+nI", nGroup)
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+yI", yGroup)
        .add(pLoc_ST, "+ndA", rootIsAny(kendi))
        .add(pAbl_ST, "+ndAn", rootIsAny(kendi))
        .add(pEqu_ST, "+ncA", rootIsAny(kendi))
        .add(pIns_ST, "+nlA", conditionpP1sg_S)
        .add(pGen_ST, "+nIn", conditionpP1sg_S);

    Condition conditionP2sg = Conditions.rootIsAny(kim, sen, ne, nere, kendi);
    pP2sg_S
        .addEmpty(pNom_ST)
        .add(pDat_ST, "+nA", nGroup)
        .add(pAcc_ST, "+nI", nGroup)
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+yI", yGroup)
        .add(pLoc_ST, "+ndA", rootIsAny(kendi))
        .add(pAbl_ST, "+ndAn", rootIsAny(kendi))
        .add(pEqu_ST, "+ncA", rootIsAny(kendi))
        .add(pIns_ST, "+nlA", conditionP2sg)
        .add(pGen_ST, "+nIn", conditionP2sg);

    Condition p3sgCond = Conditions.rootIsAny(
        kendi, kim, ne, nere, o, bazi, biri, birbiri, herbiri, hep, kimi, hicbiri);

    pP3sg_S
        .addEmpty(pNom_ST)
        .add(pDat_ST, "+nA", nGroup)
        .add(pAcc_ST, "+nI", nGroup)
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+yI", yGroup)
        .add(pLoc_ST, "+ndA", p3sgCond)
        .add(pAbl_ST, "+ndAn", p3sgCond)
        .add(pGen_ST, "+nIn", p3sgCond)
        .add(pEqu_ST, "ncA", p3sgCond)
        .add(pIns_ST, "+ylA", p3sgCond);

    Condition hepCnd = Conditions.rootIsAny(
        kendi, kim, ne, nere, biz, siz, biri, birbiri, birkaci, herbiri, hep, kimi, cogu, bircogu,
        tumu, topu, bazi, hicbiri);
    pP1pl_S
        .addEmpty(pNom_ST)
        .add(pDat_ST, "+nA", nGroup)
        .add(pAcc_ST, "+nI", nGroup)
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+yI", yGroup)
        .add(pLoc_ST, "+ndA", hepCnd)
        .add(pAbl_ST, "+ndAn", hepCnd)
        .add(pGen_ST, "+nIn", hepCnd)
        .add(pEqu_ST, "+ncA", hepCnd)
        .add(pIns_ST, "+nlA", hepCnd);

    pP2pl_S
        .addEmpty(pNom_ST)
        .add(pDat_ST, "+nA", nGroup)
        .add(pAcc_ST, "+nI", nGroup)
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+yI", yGroup)
        .add(pLoc_ST, "+ndA", hepCnd)
        .add(pAbl_ST, "+ndAn", hepCnd)
        .add(pGen_ST, "+nIn", hepCnd)
        .add(pEqu_ST, "+ncA", hepCnd)
        .add(pIns_ST, "+nlA", hepCnd);

    Condition hepsiCnd = Conditions.rootIsAny(
        kendi, kim, ne, nere, o, bazi, biri, herkes, umum, birkaci, hepsi, cumlesi, cogu,
        bircogu, birbiri, tumu, kimi, topu);

    pP3pl_S
        .addEmpty(pNom_ST)
        .add(pDat_ST, "+nA", nGroup)
        .add(pAcc_ST, "+nI", nGroup)
        .add(pDat_ST, "+yA", yGroup)
        .add(pAcc_ST, "+yI", yGroup)
        .add(pLoc_ST, "+ndA", hepsiCnd)
        .add(pAbl_ST, "+ndAn", hepsiCnd)
        .add(pGen_ST, "+nIn", hepsiCnd.or(Conditions.rootIsAny(sen, siz)))
        .add(pEqu_ST, "+ncA", hepsiCnd)
        .add(pIns_ST, "+ylA", hepsiCnd);

    pNom_ST.add(with_S, "+nlI", Conditions.rootIsAny(bu, su, o_demons, ben, sen, o, biz, siz));
    pNom_ST.add(with_S, "lI", Conditions.rootIsAny(nere));
    pNom_ST.add(with_S, "+ylI", Conditions.rootIsAny(ne));
    pNom_ST.add(without_S, "+nsIz",
        Conditions.rootIsAny(nere, bu, su, o_demons, ben, sen, o, biz, siz));
    pNom_ST.add(without_S, "+ysIz", Conditions.rootIsAny(ne));
    pGen_ST.add(rel_S, "ki", Conditions.rootIsAny(nere, bu, su, o_demons, ne, sen, o, biz, siz));

    Condition notRelRepetition = new HasTailSequence(rel, adj, zero, noun, a3sg, pnon, loc).not();
    pLoc_ST.add(rel_S, "ki", notRelRepetition);

    pIns_ST.add(vWhile_S, "+yken");

    //------------- Derivation connections ---------

    pNom_ST.addEmpty(pronZeroDeriv_S, Conditions.HAS_TAIL);
    pDat_ST.addEmpty(pronZeroDeriv_S, Conditions.HAS_TAIL);
    pLoc_ST.addEmpty(pronZeroDeriv_S, Conditions.HAS_TAIL);
    pAbl_ST.addEmpty(pronZeroDeriv_S, Conditions.HAS_TAIL);
    pGen_ST.addEmpty(pronZeroDeriv_S, Conditions.HAS_TAIL);
    pIns_ST.addEmpty(pronZeroDeriv_S, Conditions.HAS_TAIL);

    pronZeroDeriv_S.addEmpty(pvVerbRoot_S);
  }

  MorphemeState pvPresent_S = nonTerminal("pvPresent_S", pres);
  MorphemeState pvPast_S = nonTerminal("pvPast_S", past);
  MorphemeState pvNarr_S = nonTerminal("pvNarr_S", narr);
  MorphemeState pvCond_S = nonTerminal("pvCond_S", cond);
  MorphemeState pvA1sg_ST = terminal("pvA1sg_ST", a1sg);
  MorphemeState pvA2sg_ST = terminal("pvA2sg_ST", a2sg);
  MorphemeState pvA3sg_ST = terminal("pvA3sg_ST", a3sg);
  MorphemeState pvA3sg_S = nonTerminal("pvA3sg_S", a3sg);
  MorphemeState pvA1pl_ST = terminal("pvA1pl_ST", a1pl);
  MorphemeState pvA2pl_ST = terminal("pvA2pl_ST", a2pl);
  MorphemeState pvA3pl_ST = terminal("pvA3pl_ST", a3pl);

  MorphemeState pvCopBeforeA3pl_S = nonTerminal("pvCopBeforeA3pl_S", cop);
  MorphemeState pvCop_ST = terminal("pvCop_ST", cop);

  MorphemeState pvVerbRoot_S = builder("pvVerbRoot_S", verb).posRoot().build();

  private void connectVerbAfterPronoun() {

    pvVerbRoot_S.addEmpty(pvPresent_S);

    pvVerbRoot_S.add(vWhile_S, "+yken");

    pvVerbRoot_S.add(pvPast_S, "+ydI");

    pvVerbRoot_S.add(pvNarr_S, "+ymIş");

    pvVerbRoot_S.add(pvCond_S, "+ysA");

    // disallow `benin, bizim with A1sg analysis etc.`
    Condition allowA1sgTrans = new Conditions.PreviousGroupContains(pA1pl_S, pP1sg_S).not();
    Condition allowA1plTrans =
        new Conditions.PreviousGroupContains(pA1sg_S, pA2sg_S, pP1sg_S, pP2sg_S).not();
    Condition allowA2sgTrans = new Conditions.PreviousGroupContains(pA2pl_S, pP2sg_S).not();
    Condition allowA2plTrans = new Conditions.PreviousGroupContains(pA2sg_S, pP2pl_S).not();

    pvPresent_S.add(pvA1sg_ST, "+yIm", allowA1sgTrans);
    pvPresent_S.add(pvA2sg_ST, "sIn", allowA2sgTrans);
    // We do not allow ending with A3sg from empty Present tense.
    pvPresent_S.addEmpty(nA3sg_S);
    pvPresent_S.add(pvA1pl_ST, "+yIz", allowA1plTrans);
    pvPresent_S.add(pvA2pl_ST, "sInIz");
    pvPresent_S.add(pvA3pl_ST, "lAr", new Conditions.PreviousGroupContains(pLoc_ST));

    pvPast_S.add(pvA1sg_ST, "m", allowA1sgTrans);
    pvPast_S.add(pvA2sg_ST, "n", allowA2sgTrans);
    pvPast_S.add(pvA1pl_ST, "k", allowA1plTrans);
    pvPast_S.add(pvA2pl_ST, "InIz");
    pvPast_S.add(pvA3pl_ST, "lAr");
    pvPast_S.addEmpty(pvA3sg_ST);

    pvNarr_S.add(pvA1sg_ST, "Im", allowA1sgTrans);
    pvNarr_S.add(pvA2sg_ST, "sIn", allowA2sgTrans);
    pvNarr_S.add(pvA1pl_ST, "Iz", allowA1plTrans);
    pvNarr_S.add(pvA2pl_ST, "sInIz");
    pvNarr_S.add(pvA3pl_ST, "lAr");
    pvNarr_S.addEmpty(pvA3sg_ST);
    // narr+cons is allowed but not past+cond
    pvNarr_S.add(pvCond_S, "sA");

    pvCond_S.add(pvA1sg_ST, "m", allowA1sgTrans);
    pvCond_S.add(pvA2sg_ST, "n", allowA2sgTrans);
    pvCond_S.add(pvA1pl_ST, "k", allowA1plTrans);
    pvCond_S.add(pvA2pl_ST, "nIz", allowA2plTrans);
    pvCond_S.addEmpty(pvA3sg_ST);
    pvCond_S.add(pvA3pl_ST, "lAr");

    Condition rejectNoCopula = new CurrentGroupContainsAny(pvPast_S, pvCond_S, pvCopBeforeA3pl_S)
        .not();

    pvA1sg_ST.add(pvCop_ST, "dIr", rejectNoCopula);
    pvA2sg_ST.add(pvCop_ST, "dIr", rejectNoCopula);
    pvA1pl_ST.add(pvCop_ST, "dIr", rejectNoCopula);
    pvA2pl_ST.add(pvCop_ST, "dIr", rejectNoCopula);

    pvA3sg_S.add(pvCop_ST, ">dIr", rejectNoCopula);

    pvA3pl_ST.add(pvCop_ST, "dIr", rejectNoCopula);

    // Copula can come before A3pl.
    pvPresent_S.add(pvCopBeforeA3pl_S, ">dIr");
    pvCopBeforeA3pl_S.add(pvA3pl_ST, "lAr");

  }

  // ------------- Adverbs -----------------

  MorphemeState advRoot_ST = builder("advRoot_ST", adv).posRoot().terminal().build();
  MorphemeState advNounRoot_ST = builder("advRoot_ST", adv).posRoot().terminal().build();
  MorphemeState advForVerbDeriv_ST =
      builder("advForVerbDeriv_ST", adv).posRoot().terminal().build();

  MorphemeState avNounAfterAdvRoot_ST = builder("advToNounRoot_ST", noun).posRoot().build();
  MorphemeState avA3sg_S = nonTerminal("avA3sg_S", a3sg);
  MorphemeState avPnon_S = nonTerminal("avPnon_S", pnon);
  MorphemeState avDat_ST = terminal("avDat_ST", dat);

  MorphemeState avZero_S = nonTerminalDerivative("avZero_S", zero);
  MorphemeState avZeroToVerb_S = nonTerminalDerivative("avZeroToVerb_S", zero);

  private void connectAdverbs() {
    advNounRoot_ST.addEmpty(avZero_S);
    avZero_S.addEmpty(avNounAfterAdvRoot_ST);
    avNounAfterAdvRoot_ST.addEmpty(avA3sg_S);
    avA3sg_S.addEmpty(avPnon_S);
    avPnon_S.add(avDat_ST, "+yA");

    advForVerbDeriv_ST.addEmpty(avZeroToVerb_S);
    avZeroToVerb_S.addEmpty(nVerb_S);
  }

  // ------------- Interjection, Conjunctions, Determiner and Duplicator  -----------------

  MorphemeState conjRoot_ST = builder("conjRoot_ST", conj).posRoot().terminal().build();
  MorphemeState interjRoot_ST = builder("interjRoot_ST", interj).posRoot().terminal().build();
  MorphemeState detRoot_ST = builder("detRoot_ST", det).posRoot().terminal().build();
  MorphemeState dupRoot_ST = builder("dupRoot_ST", dup).posRoot().terminal().build();

  // ------------- Post Positive ------------------------------------------------

  MorphemeState postpRoot_ST = builder("postpRoot_ST", postp).posRoot().terminal().build();
  MorphemeState postpZero_S = nonTerminalDerivative("postpZero_S", zero);

  MorphemeState po2nRoot_S = nonTerminal("po2nRoot_S", noun);

  MorphemeState po2nA3sg_S = nonTerminal("po2nA3sg_S", a3sg);
  MorphemeState po2nA3pl_S = nonTerminal("po2nA3pl_S", a3pl);

  MorphemeState po2nP3sg_S = nonTerminal("po2nP3sg_S", p3sg);
  MorphemeState po2nP1sg_S = nonTerminal("po2nP1sg_S", p1sg);
  MorphemeState po2nP2sg_S = nonTerminal("po2nP2sg_S", p2sg);
  MorphemeState po2nP1pl_S = nonTerminal("po2nP1pl_S", p1pl);
  MorphemeState po2nP2pl_S = nonTerminal("po2nP2pl_S", p2pl);
  MorphemeState po2nPnon_S = nonTerminal("po2nPnon_S", pnon);


  MorphemeState po2nNom_ST = terminal("po2nNom_ST", nom);
  MorphemeState po2nDat_ST = terminal("po2nDat_ST", dat);
  MorphemeState po2nAbl_ST = terminal("po2nAbl_ST", abl);
  MorphemeState po2nLoc_ST = terminal("po2nLoc_ST", loc);
  MorphemeState po2nIns_ST = terminal("po2nIns_ST", ins);
  MorphemeState po2nAcc_ST = terminal("po2nAcc_ST", acc);
  MorphemeState po2nGen_ST = terminal("po2nGen_ST", gen);
  MorphemeState po2nEqu_ST = terminal("po2nEqu_ST", equ);

  private void connectPostpositives() {

    postpRoot_ST.addEmpty(postpZero_S);
    postpZero_S.addEmpty(nVerb_S);

    // gibi is kind of special.
    DictionaryItem gibiGen = lexicon.getItemById("gibi_Postp_PCGen");
    DictionaryItem gibiNom = lexicon.getItemById("gibi_Postp_PCNom");
    DictionaryItem sonraAbl = lexicon.getItemById("sonra_Postp_PCAbl");

    postpZero_S.addEmpty(po2nRoot_S, rootIsAny(gibiGen, gibiNom, sonraAbl));

    po2nRoot_S.addEmpty(po2nA3sg_S);
    po2nRoot_S.add(po2nA3pl_S, "lAr");

    // gibisi, gibim-e, gibi-e, gibi-mize
    po2nA3sg_S.add(po2nP3sg_S, "+sI");
    po2nA3sg_S.add(po2nP1sg_S, "m", Conditions.rootIsAny(gibiGen, gibiNom));
    po2nA3sg_S.add(po2nP2sg_S, "n", Conditions.rootIsAny(gibiGen, gibiNom));
    po2nA3sg_S.add(po2nP1pl_S, "miz", Conditions.rootIsAny(gibiGen, gibiNom));
    po2nA3sg_S.add(po2nP2pl_S, "niz", Conditions.rootIsAny(gibiGen, gibiNom));

    // gibileri
    po2nA3pl_S.add(po2nP3sg_S, "+sI");
    po2nA3pl_S.addEmpty(po2nPnon_S);

    po2nP3sg_S
        .addEmpty(po2nNom_ST)
        .add(po2nDat_ST, "nA")
        .add(po2nLoc_ST, "ndA")
        .add(po2nAbl_ST, "ndAn")
        .add(po2nIns_ST, "ylA")
        .add(po2nGen_ST, "nIn")
        .add(po2nAcc_ST, "nI");

    po2nPnon_S
        .addEmpty(po2nNom_ST)
        .add(po2nDat_ST, "A")
        .add(po2nLoc_ST, "dA")
        .add(po2nAbl_ST, "dAn")
        .add(po2nIns_ST, "lA")
        .add(po2nGen_ST, "In")
        .add(po2nEqu_ST, "cA")
        .add(po2nAcc_ST, "I");

    po2nP1sg_S.add(po2nDat_ST, "e");
    po2nP2sg_S.add(po2nDat_ST, "e");
    po2nP1pl_S.add(po2nDat_ST, "e");
    po2nP2pl_S.add(po2nDat_ST, "e");
  }

  // ------------- Verbs -----------------------------------

  public MorphemeState verbRoot_S = builder("verbRoot_S", verb).posRoot().build();
  public MorphemeState verbLastVowelDropModRoot_S =
      builder("verbLastVowelDropModRoot_S", verb).posRoot().build();
  public MorphemeState verbLastVowelDropUnmodRoot_S =
      builder("verbLastVowelDropUnmodRoot_S", verb).posRoot().build();

  MorphemeState vA1sg_ST = terminal("vA1sg_ST", a1sg);
  MorphemeState vA2sg_ST = terminal("vA2sg_ST", a2sg);
  MorphemeState vA3sg_ST = terminal("vA3sg_ST", a3sg);
  MorphemeState vA1pl_ST = terminal("vA1pl_ST", a1pl);
  MorphemeState vA2pl_ST = terminal("vA2pl_ST", a2pl);
  MorphemeState vA3pl_ST = terminal("vA3pl_ST", a3pl);

  MorphemeState vPast_S = nonTerminal("vPast_S", past);
  MorphemeState vNarr_S = nonTerminal("vNarr_S", narr);
  MorphemeState vCond_S = nonTerminal("vCond_S", cond);
  MorphemeState vCondAfterPerson_ST = terminal("vCondAfterPerson_ST", cond);
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
  MorphemeState vCopBeforeA3pl_S = nonTerminal("vCopBeforeA3pl_S", cop);

  MorphemeState vNeg_S = nonTerminal("vNeg_S", neg);
  MorphemeState vUnable_S = nonTerminal("vUnable_S", unable);
  // for negative before progressive-1 "Iyor"
  MorphemeState vNegProg1_S = nonTerminal("vNegProg1_S", neg);
  MorphemeState vUnableProg1_S = nonTerminal("vUnableProg1_S", unable);


  MorphemeState vImp_S = nonTerminal("vImp_S", imp);
  MorphemeState vImpYemekYi_S = nonTerminal("vImpYemekYi_S", imp);
  MorphemeState vImpYemekYe_S = nonTerminal("vImpYemekYe_S", imp);

  MorphemeState vCausT_S = nonTerminalDerivative("vCaus_S", caus);
  MorphemeState vCausTır_S = nonTerminalDerivative("vCausTır_S", caus);

  MorphemeState vRecip_S = nonTerminalDerivative("vRecip_S", recip);
  MorphemeState vImplicitRecipRoot_S = builder("vImplicitRecipRoot_S", verb).posRoot().build();

  MorphemeState vReflex_S = nonTerminalDerivative("vReflex_S", reflex);
  MorphemeState vImplicitReflexRoot_S = builder("vImplicitReflexRoot_S", verb).posRoot().build();

  // for progressive vowel drop.
  MorphemeState verbRoot_VowelDrop_S = builder("verbRoot_VowelDrop_S", verb).posRoot().build();

  MorphemeState vAor_S = nonTerminal("vAor_S", aor);
  MorphemeState vAorNeg_S = nonTerminal("vAorNeg_S", aor);
  MorphemeState vAorNegEmpty_S = nonTerminal("vAorNegEmpty_S", aor);
  MorphemeState vAorPartNeg_S = nonTerminalDerivative("vAorPartNeg_S", aorPart);
  MorphemeState vAorPart_S = nonTerminalDerivative("vAorPart_S", aorPart);

  MorphemeState vAble_S = nonTerminalDerivative("vAble_S", able);
  MorphemeState vAbleNeg_S = nonTerminalDerivative("vAbleNeg_S", able);
  MorphemeState vAbleNegDerivRoot_S = builder("vAbleNegDerivRoot_S", verb).posRoot().build();

  MorphemeState vPass_S = nonTerminalDerivative("vPass_S", pass);

  MorphemeState vOpt_S = nonTerminal("vOpt_S", opt);
  MorphemeState vDesr_S = nonTerminal("vDesr_S", desr);
  MorphemeState vNeces_S = nonTerminal("vNeces_S", neces);

  MorphemeState vInf1_S = nonTerminalDerivative("vInf1_S", inf1);
  MorphemeState vInf2_S = nonTerminalDerivative("vInf2_S", inf2);
  MorphemeState vInf3_S = nonTerminalDerivative("vInf3_S", inf3);

  MorphemeState vAgt_S = nonTerminalDerivative("vAgt_S", agt);
  MorphemeState vActOf_S = nonTerminalDerivative("vActOf_S", actOf);

  MorphemeState vPastPart_S = nonTerminalDerivative("vPastPart_S", pastPart);
  MorphemeState vFutPart_S = nonTerminalDerivative("vFutPart_S", futPart);
  MorphemeState vPresPart_S = nonTerminalDerivative("vPresPart_S", presPart);
  MorphemeState vNarrPart_S = nonTerminalDerivative("vNarrPart_S", narrPart);

  MorphemeState vFeelLike_S = nonTerminalDerivative("vFeelLike_S", feelLike);

  MorphemeState vNotState_S = nonTerminalDerivative("vNotState_S", notState);

  MorphemeState vEverSince_S = nonTerminalDerivative("vEverSince_S", everSince);
  MorphemeState vRepeat_S = nonTerminalDerivative("vRepeat_S", repeat);
  MorphemeState vAlmost_S = nonTerminalDerivative("vAlmost_S", almost);
  MorphemeState vHastily_S = nonTerminalDerivative("vHastily_S", hastily);
  MorphemeState vStay_S = nonTerminalDerivative("vStay_S", stay);
  MorphemeState vStart_S = nonTerminalDerivative("vStart_S", start);

  MorphemeState vWhile_S = nonTerminalDerivative("vWhile_S", while_);
  MorphemeState vWhen_S = nonTerminalDerivative("vWhen_S", when);
  MorphemeState vAsIf_S = nonTerminalDerivative("vAsIf_S", asIf);
  MorphemeState vSinceDoingSo_S = nonTerminalDerivative("vSinceDoingSo_S", sinceDoingSo);
  MorphemeState vAsLongAs_S = nonTerminalDerivative("vAsLongAs_S", asLongAs);
  MorphemeState vByDoingSo_S = nonTerminalDerivative("vByDoingSo_S", byDoingSo);
  MorphemeState vAdamantly_S = nonTerminalDerivative("vAdamantly_S", adamantly);
  MorphemeState vAfterDoing_S = nonTerminalDerivative("vAfterDoing_S", afterDoingSo);
  MorphemeState vWithoutHavingDoneSo_S =
      nonTerminalDerivative("vWithoutHavingDoneSo_S", withoutHavingDoneSo);
  MorphemeState vWithoutBeingAbleToHaveDoneSo_S =
      nonTerminalDerivative("vWithoutBeingAbleToHaveDoneSo_S", withoutBeingAbleToHaveDoneSo);

  public MorphemeState vDeYeRoot_S = builder("vDeYeRoot_S", verb).posRoot().build();

  private void connectVerbs() {

    // Imperative.
    verbRoot_S.addEmpty(vImp_S);

    vImp_S
        .addEmpty(vA2sg_ST)       // oku
        .add(vA2sg_ST, "sAnA")    // oku
        .add(vA3sg_ST, "sIn")     // okusun
        .add(vA2pl_ST, "+yIn")    // okuyun
        .add(vA2pl_ST, "+yInIz")  // okuyunuz
        .add(vA2pl_ST, "sAnIzA")  // okuyunuz
        .add(vA3pl_ST, "sInlAr"); // okusunlar

    // Causative suffixes
    // Causes Verb-Verb derivation. There are three forms: "t", "tIr" and "Ir".
    // 1- "t" form is used if verb ends with a vowel, or immediately after "tIr" Causative.
    // 2- "tIr" form is used if verb ends with a consonant or immediately after "t" Causative.
    // 3- "Ir" form appears after some specific verbs but currently we treat them as separate verb.
    // such as "pişmek - pişirmek". Oflazer parses them as causative.

    verbRoot_S.add(vCausT_S, "t", has(RootAttribute.Causative_t)
        .or(new Conditions.LastDerivationIs(vCausTır_S))
        .andNot(new Conditions.LastDerivationIsAny(vCausT_S, vPass_S, vAble_S)));

    verbRoot_S.add(vCausTır_S, ">dIr",
        has(PhoneticAttribute.LastLetterConsonant)
            .andNot(new Conditions.LastDerivationIsAny(vCausTır_S, vPass_S, vAble_S)));

    vCausT_S.addEmpty(verbRoot_S);
    vCausTır_S.addEmpty(verbRoot_S);

    // Progressive1 suffix. "-Iyor"
    // if last letter is a vowel, this is handled with verbRoot_VowelDrop_S root.
    verbRoot_S.add(vProgYor_S, "Iyor", notHave(PhoneticAttribute.LastLetterVowel));

    // For "aramak", the modified root "ar" connects to verbRoot_VowelDrop_S. Here it is connected to
    // progressive "Iyor" suffix. We use a separate root state for these for convenience.
    verbRoot_VowelDrop_S.add(vProgYor_S, "Iyor");
    vProgYor_S
        .add(vA1sg_ST, "um")
        .add(vA2sg_ST, "sun")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "uz")
        .add(vA2pl_ST, "sunuz")
        .add(vA3pl_ST, "lar")
        .add(vCond_S, "sa")
        .add(vPastAfterTense_S, "du")
        .add(vNarrAfterTense_S, "muş")
        .add(vCopBeforeA3pl_S, "dur")
        .add(vWhile_S, "ken");

    // Progressive - 2 "-mAktA"
    verbRoot_S.add(vProgMakta_S, "mAktA");
    vProgMakta_S
        .add(vA1sg_ST, "yIm")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "yIz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr")
        .add(vCond_S, "ysA")
        .add(vPastAfterTense_S, "ydI")
        .add(vNarrAfterTense_S, "ymIş")
        .add(vCopBeforeA3pl_S, "dIr")
        .add(vWhile_S, "yken");

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
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr")
        .add(vPastAfterTense_S, "dI")
        .add(vNarrAfterTense_S, "mIş")
        .add(vCond_S, "sA")
        .add(vCopBeforeA3pl_S, "dIr")
        .add(vWhile_S, "ken");

    // Negative
    verbRoot_S
        .add(vNeg_S, "mA", Conditions.previousMorphemeIsNot(able));

    vNeg_S.addEmpty(vImp_S)
        .add(vPast_S, "dI")
        .add(vFut_S, "yAcA~k")
        .add(vFut_S, "yAcA!ğ")
        .add(vNarr_S, "mIş")
        .add(vProgMakta_S, "mAktA")
        .add(vOpt_S, "yA")
        .add(vDesr_S, "sA")
        .add(vNeces_S, "mAlI")
        .add(vInf1_S, "mAk")
        .add(vInf2_S, "mA")
        .add(vInf3_S, "yIş")
        .add(vActOf_S, "mAcA")
        .add(vPastPart_S, "dI~k")
        .add(vPastPart_S, "dI!ğ")
        .add(vFutPart_S, "yAcA~k")
        .add(vFutPart_S, "yAcA!ğ")
        .add(vPresPart_S, "yAn")
        .add(vNarrPart_S, "mIş")
        .add(vSinceDoingSo_S, "yAlI")
        .add(vByDoingSo_S, "yArAk")
        .add(vHastily_S, "yIver")
        .add(vEverSince_S, "yAgör")
        .add(vAfterDoing_S, "yIp")
        .add(vWhen_S, "yIncA")
        .add(vAsLongAs_S, "dIkçA")
        .add(vNotState_S, "mAzlI~k")
        .add(vNotState_S, "mAzlI!ğ")
        .add(vFeelLike_S, "yAsI");

    // Negative form is "m" before progressive "Iyor" because last vowel drops.
    // We use a separate negative state for this.
    verbRoot_S.add(vNegProg1_S, "m");
    vNegProg1_S.add(vProgYor_S, "Iyor");

    // Negative Aorist
    // Aorist tense forms differently after negative. It can be "z" or empty.
    vNeg_S.add(vAorNeg_S, "z");
    vNeg_S.addEmpty(vAorNegEmpty_S);
    vAorNeg_S
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr")
        .add(vPastAfterTense_S, "dI")
        .add(vNarrAfterTense_S, "mIş")
        .add(vCond_S, "sA")
        .add(vCopBeforeA3pl_S, "dIr")
        .add(vWhile_S, "ken");
    vAorNegEmpty_S
        .add(vA1sg_ST, "m")
        .add(vA1pl_ST, "yIz");
    // oku-maz-ım TODO: not sure here.
    vNeg_S.add(vAorPartNeg_S, "z");
    vAorPartNeg_S.addEmpty(adjAfterVerb_ST);

    //Positive Ability.
    // This makes a Verb-Verb derivation.
    verbRoot_S.add(vAble_S, "+yAbil", lastDerivationIs(vAble_S).not());

    vAble_S.addEmpty(verbRoot_S);

    // Also for ability that comes before negative, we add a new root state.
    // From there only negative connections is possible.
    vAbleNeg_S.addEmpty(vAbleNegDerivRoot_S);
    vAbleNegDerivRoot_S.add(vNeg_S, "mA");
    vAbleNegDerivRoot_S.add(vNegProg1_S, "m");

    // it is possible to have abil derivation after negative.
    vNeg_S.add(vAble_S, "yAbil");

    // Unable.
    verbRoot_S
        .add(vUnable_S, "+yAmA", Conditions.previousMorphemeIsNot(able));
    // careful here. We copy all outgoing transitions to "unable"
    vUnable_S.copyOutgoingTransitionsFrom(vNeg_S);
    verbRoot_S.add(vUnableProg1_S, "+yAm");
    vUnableProg1_S.add(vProgYor_S, "Iyor");

    // Infinitive 1 "mAk"
    // Causes Verb to Noun derivation. It is connected to a special noun root state.
    verbRoot_S.add(vInf1_S, "mA~k");
    vInf1_S.addEmpty(nounInf1Root_S);

    // Infinitive 2 "mA"
    // Causes Verb to Noun derivation.
    verbRoot_S.add(vInf2_S, "mA");
    vInf2_S.addEmpty(noun_S);

    // Infinitive 3 "+yUş"
    // Causes Verb to Noun derivation.
    verbRoot_S.add(vInf3_S, "+yIş");
    vInf3_S.addEmpty(noun_S);

    // Agt 3 "+yIcI"
    // Causes Verb to Noun and Adj derivation.
    verbRoot_S.add(vAgt_S, "+yIcI");
    vAgt_S.addEmpty(noun_S);
    vAgt_S.addEmpty(adjAfterVerb_ST);

    // ActOf "mAcA"
    // Causes Verb to Noun and Adj derivation.
    verbRoot_S.add(vActOf_S, "mAcA");
    vActOf_S.addEmpty(nounActOfRoot_S);

    // PastPart "oku-duğ-um"
    verbRoot_S.add(vPastPart_S, ">dI~k");
    verbRoot_S.add(vPastPart_S, ">dI!ğ");
    vPastPart_S.addEmpty(noun_S);
    vPastPart_S.addEmpty(adjAfterVerb_S);

    // FutPart "oku-yacağ-ım kitap"
    verbRoot_S.add(vFutPart_S, "+yAcA~k");
    verbRoot_S.add(vFutPart_S, "+yAcA!ğ");
    vFutPart_S.addEmpty(noun_S, Conditions.HAS_TAIL);
    vFutPart_S.addEmpty(adjAfterVerb_S);

    // FutPart "oku-yacağ-ım kitap"
    verbRoot_S.add(vNarrPart_S, "mIş");
    vNarrPart_S.addEmpty(adjectiveRoot_ST);

    // AorPart "okunabilir-lik"
    verbRoot_S.add(vAorPart_S, "Ir",
        has(RootAttribute.Aorist_I).or(Conditions.HAS_SURFACE));
    verbRoot_S.add(vAorPart_S, "Ar",
        has(RootAttribute.Aorist_A).and(Conditions.HAS_NO_SURFACE));
    vAorPart_S.addEmpty(adjAfterVerb_ST);

    // PresPart
    verbRoot_S.add(vPresPart_S, "+yAn");
    vPresPart_S.addEmpty(noun_S, Conditions.HAS_TAIL);
    vPresPart_S.addEmpty(adjAfterVerb_ST); // connect to terminal Adj

    // FeelLike
    verbRoot_S.add(vFeelLike_S, "+yAsI");
    vFeelLike_S.addEmpty(noun_S, Conditions.HAS_TAIL);
    vFeelLike_S.addEmpty(adjAfterVerb_ST); // connect to terminal Adj

    // NotState
    verbRoot_S.add(vNotState_S, "mAzlI~k");
    verbRoot_S.add(vNotState_S, "mAzlI!ğ");
    vNotState_S.addEmpty(noun_S);

    // reciprocal
    // TODO: for reducing ambiguity for now remove reciprocal
/*
    verbRoot_S.add(vRecip_S, "Iş", notHaveAny(RootAttribute.Reciprocal, RootAttribute.NonReciprocal)
        .andNot(new Conditions.ContainsMorpheme(recip)));
*/
    vRecip_S.addEmpty(verbRoot_S);
    vImplicitRecipRoot_S.addEmpty(vRecip_S);

    // reflexive
    vImplicitReflexRoot_S.addEmpty(vReflex_S);
    vReflex_S.addEmpty(verbRoot_S);

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
    verbRoot_S.add(vPass_S, "+nIl",
        new Conditions.PreviousStateIsAny(vCausT_S, vCausTır_S)
            .or(notHave(RootAttribute.Passive_In).andNot(new Conditions.ContainsMorpheme(pass))));
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
    vNarr_S.add(vCopBeforeA3pl_S, "tIr");
    vNarr_S.add(vWhile_S, "ken");
    vNarr_S.add(vNarrAfterTense_S, "mIş");

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
    vNarrAfterTense_S.add(vWhile_S, "ken");
    vNarrAfterTense_S.add(vCopBeforeA3pl_S, "tIr");

    // Future "oku-yacak"
    verbRoot_S.add(vFut_S, "+yAcA~k");
    verbRoot_S.add(vFut_S, "+yAcA!ğ");

    vFut_S
        .add(vA1sg_ST, "Im")
        .add(vA2sg_ST, "sIn")
        .addEmpty(vA3sg_ST)
        .add(vA1pl_ST, "Iz")
        .add(vA2pl_ST, "sInIz")
        .add(vA3pl_ST, "lAr");
    vFut_S.add(vCond_S, "sA");
    vFut_S.add(vPastAfterTense_S, "tI");
    vFut_S.add(vNarrAfterTense_S, "mIş");
    vFut_S.add(vCopBeforeA3pl_S, "tIr");
    vFut_S.add(vWhile_S, "ken");

    // `demek` and `yemek` are special because they are the only two verbs with two letters
    // and ends with a vowel.
    // Their root transform as:
    // No change: de-di, de-miş, de-dir
    // Change : di-yecek di-yor de-r
    // "ye" has similar behavior but not the same. Such as "yi-yin" but for "de", "de-yin"
    // TODO: this can be achieved with less repetition.
    RootSurfaceIsAny diYiCondition = new RootSurfaceIsAny("di", "yi");
    RootSurfaceIsAny deYeCondition = new RootSurfaceIsAny("de", "ye");
    Condition cMultiVerb = new Conditions.PreviousMorphemeIsAny(
        everSince, repeat, almost, hastily, stay, start).not();

    vDeYeRoot_S
        .add(vFut_S, "yece~k", diYiCondition)
        .add(vFut_S, "yece!ğ", diYiCondition)
        .add(vProgYor_S, "yor", diYiCondition)
        .add(vAble_S, "yebil", diYiCondition)
        .add(vAbleNeg_S, "ye", diYiCondition)
        .add(vInf3_S, "yiş", new RootSurfaceIsAny("yi"))
        .add(vFutPart_S, "yece~k", diYiCondition)
        .add(vFutPart_S, "yece!ğ", diYiCondition)
        .add(vPresPart_S, "yen", diYiCondition)
        .add(vEverSince_S, "yegel", diYiCondition.and(cMultiVerb))
        .add(vRepeat_S, "yedur", diYiCondition.and(cMultiVerb))
        .add(vRepeat_S, "yegör", diYiCondition.and(cMultiVerb))
        .add(vAlmost_S, "yeyaz", diYiCondition.and(cMultiVerb))
        .add(vStart_S, "yekoy", diYiCondition.and(cMultiVerb))
        .add(vSinceDoingSo_S, "yeli", diYiCondition)
        .add(vByDoingSo_S, "yerek", diYiCondition)
        .add(vFeelLike_S, "yesi", diYiCondition)
        .add(vAfterDoing_S, "yip", diYiCondition)
        .add(vWithoutBeingAbleToHaveDoneSo_S, "yemeden", diYiCondition)
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
        .add(vInf1_S, "mek", deYeCondition)
        .add(vInf2_S, "me", deYeCondition)
        .add(vInf3_S, "yiş", new RootSurfaceIsAny("de"))
        .add(vPastPart_S, "di~k", deYeCondition)
        .add(vPastPart_S, "di!ğ", deYeCondition)
        .add(vNarrPart_S, "miş", deYeCondition)
        .add(vHastily_S, "yiver", diYiCondition.and(cMultiVerb))
        .add(vAsLongAs_S, "dikçe")
        .add(vWithoutHavingDoneSo_S, "meden")
        .add(vWithoutHavingDoneSo_S, "meksizin")
        .add(vNeces_S, "meli")
        .add(vNotState_S, "mezli~k")
        .add(vNotState_S, "mezli!ğ")
        .addEmpty(vImp_S, new RootSurfaceIs("de"))
        .addEmpty(vImpYemekYe_S, new RootSurfaceIs("ye"))
        .addEmpty(vImpYemekYi_S, new RootSurfaceIs("yi"));

    // verb `yemek` has an exception case for some imperatives.
    vImpYemekYi_S
        .add(vA2pl_ST, "yin")
        .add(vA2pl_ST, "yiniz");
    vImpYemekYe_S
        .addEmpty(vA2sg_ST)
        .add(vA2sg_ST, "sene")
        .add(vA3sg_ST, "sin")
        .add(vA2pl_ST, "senize")
        .add(vA3pl_ST, "sinler");

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
        .add(vCond_S, "ysA")
        .add(vNarrAfterTense_S, "ymIş")
        .add(vCopBeforeA3pl_S, "dIr")
        .add(vWhile_S, "yken");

    // A3pl exception case.
    // A3pl can appear before or after some tense suffixes.
    // "yapar-lar-dı" - "yapar-dı-lar"
    // For preventing "yapar-dı-lar-dı", conditions are added.
    Condition previousNotPastNarrCond = new PreviousStateIsAny(
        vPastAfterTense_S, vNarrAfterTense_S, vCond_S).not();
    vA3pl_ST.add(vPastAfterTense_ST, "dI", previousNotPastNarrCond);
    vA3pl_ST.add(vNarrAfterTense_ST, "mIş", previousNotPastNarrCond);
    vA3pl_ST.add(vCond_ST, "sA", previousNotPastNarrCond);

    Condition a3plCopWhile =
        new PreviousMorphemeIsAny(prog1, prog2, neces, fut, narr, aor);
    vA3pl_ST.add(vCop_ST, "dIr", a3plCopWhile);
    vA3pl_ST.add(vWhile_S, "ken", a3plCopWhile);

    Condition a3sgCopWhile =
        new PreviousMorphemeIsAny(prog1, prog2, neces, fut, narr, aor);
    vA1sg_ST.add(vCop_ST, "dIr", a3sgCopWhile);
    vA2sg_ST.add(vCop_ST, "dIr", a3sgCopWhile);
    vA3sg_ST.add(vCop_ST, ">dIr", a3sgCopWhile);
    vA1pl_ST.add(vCop_ST, "dIr", a3sgCopWhile);
    vA2pl_ST.add(vCop_ST, "dIr", a3sgCopWhile);

    vCopBeforeA3pl_S.add(vA3pl_ST, "lAr");

    // Allow Past+A2pl+Cond  Past+A2sg+Cond (geldinse, geldinizse)
    Condition previousPast = new Conditions.PreviousMorphemeIs(past)
        .andNot(new ContainsMorpheme(cond, desr));
    vA2pl_ST.add(vCondAfterPerson_ST, "sA", previousPast);
    vA2sg_ST.add(vCondAfterPerson_ST, "sA", previousPast);
    vA1sg_ST.add(vCondAfterPerson_ST, "sA", previousPast);
    vA1pl_ST.add(vCondAfterPerson_ST, "sA", previousPast);

    verbRoot_S.add(vEverSince_S, "+yAgel", cMultiVerb);
    verbRoot_S.add(vRepeat_S, "+yAdur", cMultiVerb);
    verbRoot_S.add(vRepeat_S, "+yAgör", cMultiVerb);
    verbRoot_S.add(vAlmost_S, "+yAyaz", cMultiVerb);
    verbRoot_S.add(vHastily_S, "+yIver", cMultiVerb);
    verbRoot_S.add(vStay_S, "+yAkal", cMultiVerb);
    verbRoot_S.add(vStart_S, "+yAkoy", cMultiVerb);

    vEverSince_S.addEmpty(verbRoot_S);
    vRepeat_S.addEmpty(verbRoot_S);
    vAlmost_S.addEmpty(verbRoot_S);
    vHastily_S.addEmpty(verbRoot_S);
    vStay_S.addEmpty(verbRoot_S);
    vStart_S.addEmpty(verbRoot_S);

    vA3sg_ST.add(vAsIf_S, ">cAsInA", new Conditions.PreviousMorphemeIsAny(aor, narr));

    verbRoot_S.add(vWhen_S, "+yIncA");
    verbRoot_S.add(vSinceDoingSo_S, "+yAlI");
    verbRoot_S.add(vByDoingSo_S, "+yArAk");
    verbRoot_S.add(vAdamantly_S, "+yAsIyA");
    verbRoot_S.add(vAfterDoing_S, "+yIp");
    verbRoot_S.add(vWithoutBeingAbleToHaveDoneSo_S, "+yAmAdAn");
    verbRoot_S.add(vAsLongAs_S, ">dIkçA");
    verbRoot_S.add(vWithoutHavingDoneSo_S, "mAdAn");
    verbRoot_S.add(vWithoutHavingDoneSo_S, "mAksIzIn");

    vAsIf_S.addEmpty(advRoot_ST);
    vSinceDoingSo_S.addEmpty(advRoot_ST);
    vByDoingSo_S.addEmpty(advRoot_ST);
    vAdamantly_S.addEmpty(advRoot_ST);
    vAfterDoing_S.addEmpty(advRoot_ST);
    vWithoutBeingAbleToHaveDoneSo_S.addEmpty(advRoot_ST);
    vAsLongAs_S.addEmpty(advRoot_ST);
    vWithoutHavingDoneSo_S.addEmpty(advRoot_ST);
    vWhile_S.addEmpty(advRoot_ST);
    vWhen_S.addEmpty(advNounRoot_ST);
  }

  //-------- Question (mi) -----------------------------------------------

  MorphemeState qPresent_S = nonTerminal("qPresent_S", pres);
  MorphemeState qPast_S = nonTerminal("qPast_S", past);
  MorphemeState qNarr_S = nonTerminal("qNarr_S", narr);
  MorphemeState qA1sg_ST = terminal("qA1sg_ST", a1sg);
  MorphemeState qA2sg_ST = terminal("qA2sg_ST", a2sg);
  MorphemeState qA3sg_ST = terminal("qA3sg_ST", a3sg);
  MorphemeState qA1pl_ST = terminal("qA1pl_ST", a1pl);
  MorphemeState qA2pl_ST = terminal("qA2pl_ST", a2pl);
  MorphemeState qA3pl_ST = terminal("qA3pl_ST", a3pl);

  MorphemeState qCopBeforeA3pl_S = nonTerminal("qCopBeforeA3pl_S", cop);
  MorphemeState qCop_ST = terminal("qCop_ST", cop);

  MorphemeState questionRoot_S = builder("questionRoot_S", ques).posRoot().build();

  private void connectQuestion() {
    //mı
    questionRoot_S.addEmpty(qPresent_S);
    // mıydı
    questionRoot_S.add(qPast_S, "ydI");
    // mıymış
    questionRoot_S.add(qNarr_S, "ymIş");

    // mıyım
    qPresent_S.add(qA1sg_ST, "yIm");
    // mısın
    qPresent_S.add(qA2sg_ST, "sIn");
    // mı
    qPresent_S.addEmpty(qA3sg_ST);

    // mıydım
    qPast_S.add(qA1sg_ST, "m");
    // mıymışım
    qNarr_S.add(qA1sg_ST, "Im");

    //mıydın
    qPast_S.add(qA2sg_ST, "n");
    //mıymışsın
    qNarr_S.add(qA2sg_ST, "sIn");

    //mıydık
    qPast_S.add(qA1pl_ST, "k");
    //mıymışız
    qNarr_S.add(qA1pl_ST, "Iz");
    // mıyız
    qPresent_S.add(qA1pl_ST, "+yIz");

    // mıydınız
    qPast_S.add(qA2pl_ST, "InIz");
    // mıymışsınız
    qNarr_S.add(qA2pl_ST, "sInIz");
    // mısınız
    qPresent_S.add(qA2pl_ST, "sInIz");

    // mıydılar
    qPast_S.add(qA3pl_ST, "lAr");
    // mıymışlar
    qNarr_S.add(qA3pl_ST, "lAr");

    // mıydı
    qPast_S.addEmpty(qA3sg_ST);
    // mıymış
    qNarr_S.addEmpty(qA3sg_ST);

    // for not allowing "mı-ydı-m-dır"
    Condition rejectNoCopula = new CurrentGroupContainsAny(qPast_S).not();

    // mıyımdır
    qA1sg_ST.add(qCop_ST, "dIr", rejectNoCopula);
    // mısındır
    qA2sg_ST.add(qCop_ST, "dIr", rejectNoCopula);
    // mıdır
    qA3sg_ST.add(qCop_ST, ">dIr", rejectNoCopula);
    // mıyızdır
    qA1pl_ST.add(qCop_ST, "dIr", rejectNoCopula);
    // mısınızdır
    qA2pl_ST.add(qCop_ST, "dIr", rejectNoCopula);

    // Copula can come before A3pl.
    qPresent_S.add(pvCopBeforeA3pl_S, "dIr");
    qCopBeforeA3pl_S.add(qA3pl_ST, "lAr");
  }

  //-------- Verb `imek` -----------------------------------------------

  public MorphemeState imekRoot_S = builder("imekRoot_S", verb).posRoot().build();

  MorphemeState imekPast_S = nonTerminal("imekPast_S", past);
  MorphemeState imekNarr_S = nonTerminal("imekNarr_S", narr);

  MorphemeState imekCond_S = nonTerminal("imekCond_S", cond);

  MorphemeState imekA1sg_ST = terminal("imekA1sg_ST", a1sg);
  MorphemeState imekA2sg_ST = terminal("imekA2sg_ST", a2sg);
  MorphemeState imekA3sg_ST = terminal("imekA3sg_ST", a3sg);
  MorphemeState imekA1pl_ST = terminal("imekA1pl_ST", a1pl);
  MorphemeState imekA2pl_ST = terminal("imekA2pl_ST", a2pl);
  MorphemeState imekA3pl_ST = terminal("imekA3pl_ST", a3pl);

  MorphemeState imekCop_ST = terminal("qCop_ST", cop);

  private void connectImek() {
    // idi
    imekRoot_S.add(imekPast_S, "di");
    // imiş
    imekRoot_S.add(imekNarr_S, "miş");
    // ise
    imekRoot_S.add(imekCond_S, "se");

    // idim, idin, idi, idik, idiniz, idiler
    imekPast_S.add(imekA1sg_ST, "m");
    imekPast_S.add(imekA2sg_ST, "n");
    imekPast_S.addEmpty(imekA3sg_ST);
    imekPast_S.add(imekA1pl_ST, "k");
    imekPast_S.add(imekA2pl_ST, "niz");
    imekPast_S.add(imekA3pl_ST, "ler");

    // imişim, imişsin, imiş, imişiz, imişsiniz, imişler
    imekNarr_S.add(imekA1sg_ST, "im");
    imekNarr_S.add(imekA2sg_ST, "sin");
    imekNarr_S.addEmpty(imekA3sg_ST);
    imekNarr_S.add(imekA1pl_ST, "iz");
    imekNarr_S.add(imekA2pl_ST, "siniz");
    imekNarr_S.add(imekA3pl_ST, "ler");

    imekPast_S.add(imekCond_S, "yse");
    imekNarr_S.add(imekCond_S, "se");

    imekCond_S.add(imekA1sg_ST, "m");
    imekCond_S.add(imekA2sg_ST, "n");
    imekCond_S.addEmpty(imekA3sg_ST);
    imekCond_S.add(imekA1pl_ST, "k");
    imekCond_S.add(imekA2pl_ST, "niz");
    imekCond_S.add(imekA3pl_ST, "ler");

    // for not allowing "i-di-m-dir"
    Condition rejectNoCopula = new CurrentGroupContainsAny(imekPast_S).not();

    // imişimdir, imişsindir etc.
    imekA1sg_ST.add(imekCop_ST, "dir", rejectNoCopula);
    imekA2sg_ST.add(imekCop_ST, "dir", rejectNoCopula);
    imekA3sg_ST.add(imekCop_ST, "tir", rejectNoCopula);
    imekA1pl_ST.add(imekCop_ST, "dir", rejectNoCopula);
    imekA2pl_ST.add(imekCop_ST, "dir", rejectNoCopula);
    imekA3pl_ST.add(imekCop_ST, "dir", rejectNoCopula);
  }

  private void handlePostProcessingConnections() {

    // Passive has an exception for some verbs like `kavurmak` or `savurmak`.
    // add passive state connection to modified root `kavr` etc.
    verbLastVowelDropModRoot_S.add(vPass_S, "Il");
    // for not allowing `kavur-ul` add all verb connections to
    // unmodified `kavur` root and remove only the passive.
    verbLastVowelDropUnmodRoot_S.copyOutgoingTransitionsFrom(verbRoot_S);
    verbLastVowelDropUnmodRoot_S.removeTransitionsTo(pass);
  }

  //--------------------------------------------------------

  Map<String, MorphemeState> itemRootStateMap = new HashMap<>();

  void mapSpecialItemsToRootStates() {
    itemRootStateMap.put("değil_Verb", nVerbDegil_S);
    itemRootStateMap.put("imek_Verb", imekRoot_S);
    itemRootStateMap.put("su_Noun", nounSuRoot_S);
    itemRootStateMap.put("akarsu_Noun", nounSuRoot_S);
    itemRootStateMap.put("öyle_Adv", advForVerbDeriv_ST);
    itemRootStateMap.put("böyle_Adv", advForVerbDeriv_ST);
    itemRootStateMap.put("şöyle_Adv", advForVerbDeriv_ST);
  }

  public MorphemeState getRootState(
      DictionaryItem item,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {

    MorphemeState root = itemRootStateMap.get(item.id);
    if (root != null) {
      return root;
    }

    // Verbs like "aramak" drops their last vowel when  connected to "Iyor" Progressive suffix.
    // those modified roots are connected to a separate root state called verbRoot_VowelDrop_S.
    if (phoneticAttributes.contains(PhoneticAttribute.LastLetterDropped)) {
      return verbRoot_VowelDrop_S;
    }

    if (item.hasAttribute(RootAttribute.Reciprocal)) {
      return vImplicitRecipRoot_S;
    }

    if (item.hasAttribute(RootAttribute.Reflexive)) {
      return vImplicitReflexRoot_S;
    }

    switch (item.primaryPos) {
      case Noun:

        switch (item.secondaryPos) {
          case ProperNoun:
            return nounProper_S;
          case Abbreviation:
            return nounAbbrv_S;
          case Email:
          case Url:
          case HashTag:
          case Mention:
            return nounProper_S;
          case Emoticon:
          case RomanNumeral:
            return nounNoSuffix_S;
          default:
            break;
        }

        if (item.hasAttribute(RootAttribute.CompoundP3sgRoot)) {
          return nounCompoundRoot_S;
        } else {
          return noun_S;
        }

      case Adjective:
        return adjectiveRoot_ST;
      case Pronoun:
        switch (item.secondaryPos) {
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
      case Question:
        return questionRoot_S;
      case Interjection:
        return interjRoot_ST;
      case Verb:
        return verbRoot_S;
      case Punctuation:
        return puncRoot_ST;
      case Determiner:
        return detRoot_ST;
      case PostPositive:
        return postpRoot_ST;
      case Numeral:
        return numeralRoot_ST;
      case Duplicator:
        return dupRoot_ST;
      default:
        return noun_S;
    }
  }
}
