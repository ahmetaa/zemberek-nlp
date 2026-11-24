package zemberek.morphology.morphotactics;

import java.util.Map;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.DictionaryItem;

/**
 * Resolves the initial {@link MorphemeState} for a given {@link DictionaryItem} and stem phonetic
 * attributes.
 */
final class RootStateResolver {

  private final MorphotacticStates s;
  private final Map<String, MorphemeState> specialItemStates;

  RootStateResolver(MorphotacticStates states) {
    this.s = states;
    this.specialItemStates = Map.of(
        "değil_Verb", states.nVerbDegil_S,
        "imek_Verb", states.imekRoot_S,
        "su_Noun", states.nounSuRoot_S,
        "akarsu_Noun", states.nounSuRoot_S,
        "öyle_Adv", states.advForVerbDeriv_ST,
        "böyle_Adv", states.advForVerbDeriv_ST,
        "şöyle_Adv", states.advForVerbDeriv_ST);
  }

  MorphemeState resolve(
      DictionaryItem item,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {

    MorphemeState root = specialItemStates.get(item.id);
    if (root != null) {
      return root;
    }

    // Verbs like "aramak" drop their last vowel before progressive "-Iyor".
    if (phoneticAttributes.contains(PhoneticAttribute.LastLetterDropped)) {
      return s.verbRoot_VowelDrop_S;
    }

    if (item.hasAttribute(RootAttribute.Reciprocal)) {
      return s.vImplicitRecipRoot_S;
    }

    if (item.hasAttribute(RootAttribute.Reflexive)) {
      return s.vImplicitReflexRoot_S;
    }

    return switch (item.primaryPos) {
      case Noun -> switch (item.secondaryPos) {
        case ProperNoun, Email, Url, HashTag, Mention -> s.nounProper_S;
        case Abbreviation -> s.nounAbbrv_S;
        case Emoticon, RomanNumeral -> s.nounNoSuffix_S;
        default -> item.hasAttribute(RootAttribute.CompoundP3sgRoot)
            ? s.nounCompoundRoot_S
            : s.noun_S;
      };
      case Adjective -> s.adjectiveRoot_ST;
      case Pronoun -> switch (item.secondaryPos) {
        case PersonalPron -> s.pronPers_S;
        case DemonstrativePron -> s.pronDemons_S;
        case QuantitivePron -> s.pronQuant_S;
        case QuestionPron -> s.pronQues_S;
        case ReflexivePron -> s.pronReflex_S;
        default -> s.pronQuant_S;
      };
      case Adverb -> s.advRoot_ST;
      case Conjunction -> s.conjRoot_ST;
      case Question -> s.questionRoot_S;
      case Interjection -> s.interjRoot_ST;
      case Verb -> s.verbRoot_S;
      case Punctuation -> s.puncRoot_ST;
      case Determiner -> s.detRoot_ST;
      case PostPositive -> s.postpRoot_ST;
      case Numeral -> s.numeralRoot_ST;
      case Duplicator -> s.dupRoot_ST;
      default -> s.noun_S;
    };
  }
}
