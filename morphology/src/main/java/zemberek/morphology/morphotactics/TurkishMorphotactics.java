package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.Morpheme.derivational;
import static zemberek.morphology.morphotactics.Morpheme.instance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.analysis.StemTransitions;
import zemberek.morphology.analysis.StemTransitionsMapBased;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

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
  // okuyamadan (Adv)
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

  //-------------- Shared State Registry & Public Root States ------------------------

  final MorphotacticStates states = new MorphotacticStates();
  private final RootStateResolver rootStateResolver = new RootStateResolver(states);

  public final MorphemeState nounLastVowelDropRoot_S = states.nounLastVowelDropRoot_S;
  public final MorphemeState adjLastVowelDropRoot_S = states.adjLastVowelDropRoot_S;
  public final MorphemeState postpLastVowelDropRoot_S = states.postpLastVowelDropRoot_S;

  public final MorphemeState pronQuant_S = states.pronQuant_S;
  public final MorphemeState pronQuantModified_S = states.pronQuantModified_S;
  public final MorphemeState pronQues_S = states.pronQues_S;
  public final MorphemeState pronReflex_S = states.pronReflex_S;
  public final MorphemeState pronPers_Mod_S = states.pronPers_Mod_S;
  public final MorphemeState pronAfterRel_S = states.pronAfterRel_S;

  public final MorphemeState verbRoot_S = states.verbRoot_S;
  public final MorphemeState verbLastVowelDropModRoot_S = states.verbLastVowelDropModRoot_S;
  public final MorphemeState verbLastVowelDropUnmodRoot_S = states.verbLastVowelDropUnmodRoot_S;
  public final MorphemeState vDeYeRoot_S = states.vDeYeRoot_S;
  public final MorphemeState imekRoot_S = states.imekRoot_S;

  protected RootLexicon lexicon;
  StemTransitions stemTransitions;

  protected TurkishMorphotactics() {
  }

  public TurkishMorphotactics(RootLexicon lexicon) {
    this.lexicon = lexicon;
    makeGraph();
    this.stemTransitions = new StemTransitionsMapBased(lexicon, this);
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
      if (id.isEmpty()) {
        continue;
      }
      morphemes.add(getMorpheme(id));
    }
    return morphemes;
  }

  protected void makeGraph() {
    NominalMorphotactics.connect(states, lexicon);
    CopulaMorphotactics.connect(states, lexicon);
    PronounMorphotactics.connect(states, lexicon);
    MinorPosMorphotactics.connect(states, lexicon);
    VerbMorphotactics.connect(states);
    VerbMorphotactics.connectPostProcessing(states);
  }

  public MorphemeState getRootState(
      DictionaryItem item,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {
    return rootStateResolver.resolve(item, phoneticAttributes);
  }
}
