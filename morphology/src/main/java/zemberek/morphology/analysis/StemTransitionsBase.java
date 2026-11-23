package zemberek.morphology.analysis;

import static zemberek.core.turkish.RootAttribute.CompoundP3sg;
import static zemberek.core.turkish.RootAttribute.CompoundP3sgRoot;
import static zemberek.core.turkish.RootAttribute.Doubling;
import static zemberek.core.turkish.RootAttribute.InverseHarmony;
import static zemberek.core.turkish.RootAttribute.LastVowelDrop;
import static zemberek.core.turkish.RootAttribute.ProgressiveVowelDrop;
import static zemberek.core.turkish.RootAttribute.Voicing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.LexiconException;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.MorphemeState;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

abstract class StemTransitionsBase {

  private static final TurkishAlphabet ALPHABET = TurkishAlphabet.INSTANCE;

  private static final EnumSet<RootAttribute> MODIFIERS = EnumSet.of(
      Doubling,
      LastVowelDrop,
      ProgressiveVowelDrop,
      InverseHarmony,
      Voicing,
      CompoundP3sg,
      CompoundP3sgRoot
  );

  private static final Set<String> SPECIAL_ROOTS = Set.of(
      "içeri_Noun", "içeri_Adj",
      "dışarı_Noun", "dışarı_Adj", "dışarı_Postp",
      "yukarı_Noun", "yukarı_Adj",
      "ileri_Noun",
      "şura_Noun", "bura_Noun", "ora_Noun",
      "ben_Pron_Pers", "sen_Pron_Pers",
      "demek_Verb", "yemek_Verb", "imek_Verb",
      "birbiri_Pron_Quant", "çoğu_Pron_Quant", "öbürü_Pron_Quant", "birçoğu_Pron_Quant"
  );

  TurkishMorphotactics morphotactics;
  RootLexicon lexicon;

  /**
   * Generates StemTransition objects from the dictionary item. Most of the time a single StemNode
   * is generated.
   */
  public List<StemTransition> generate(DictionaryItem item) {
    if (SPECIAL_ROOTS.contains(item.id)) {
      return handleSpecialRoots(item);
    }
    List<StemTransition> transitions;
    if (!Collections.disjoint(item.attributes, MODIFIERS)) {
      transitions = generateModifiedRootNodes(item);
    } else {
      AttributeSet<PhoneticAttribute> phoneticAttributes = calculateAttributes(item.pronunciation);
      if (isNonDroppedLastVowelDropVariant(item)) {
        phoneticAttributes.add(PhoneticAttribute.ExpectsVowel);
        phoneticAttributes.add(PhoneticAttribute.CannotTerminate);
      }
      transitions = List.of(stemTransition(item.root, item, phoneticAttributes, null));
    }
    if (!item.hasAttribute(RootAttribute.LocaleEn)) {
      return transitions;
    }
    List<StemTransition> withLocaleEn = new ArrayList<>(transitions.size() + 2);
    withLocaleEn.addAll(transitions);
    StemTransition base = transitions.get(0);
    String trStem = ALPHABET.normalizeCircumflex(item.lemma.toLowerCase(Turkish.LOCALE))
        .replace("'", "").replace(".", "");
    if (trStem.length() == base.surface.length() && !trStem.equals(base.surface)) {
      withLocaleEn.add(new StemTransition(trStem, item, base.getPhoneticAttributes(), base.to));
    }
    for (StemTransition st : transitions) {
      String dotless = st.surface.replace('i', 'ı');
      if (!dotless.equals(st.surface) && !dotless.equals(trStem)) {
        withLocaleEn.add(new StemTransition(dotless, item, st.getPhoneticAttributes(), st.to));
      }
    }
    return withLocaleEn;
  }

  private boolean isNonDroppedLastVowelDropVariant(DictionaryItem item) {
    DictionaryItem ref = item.getReferenceItem();
    return ref != null
        && ref.hasAttribute(LastVowelDrop)
        && !item.hasAttribute(LastVowelDrop)
        && item.root.equals(ref.root);
  }

  private AttributeSet<PhoneticAttribute> calculateAttributes(CharSequence input) {
    return AttributesHelper.getMorphemicAttributes(input);
  }

  private StemTransition stemTransition(
      String surface,
      DictionaryItem item,
      AttributeSet<PhoneticAttribute> attrs,
      MorphemeState state) {
    MorphemeState target = state != null ? state : morphotactics.getRootState(item, attrs);
    return new StemTransition(surface, item, attrs, target);
  }

  private List<StemTransition> generateModifiedRootNodes(DictionaryItem dicItem) {
    StringBuilder modifiedSeq = new StringBuilder(dicItem.pronunciation);
    AttributeSet<PhoneticAttribute> originalAttrs = calculateAttributes(dicItem.pronunciation);
    AttributeSet<PhoneticAttribute> modifiedAttrs = originalAttrs.copy();

    MorphemeState modifiedRootState = null;
    MorphemeState unmodifiedRootState = null;

    for (RootAttribute attribute : dicItem.attributes) {
      switch (attribute) {
        case Voicing -> {
          char last = ALPHABET.lastChar(modifiedSeq);
          char voiced = ALPHABET.voice(last);
          if (last == voiced) {
            throw new LexiconException("Voicing letter is not proper in:" + dicItem);
          }
          if (dicItem.lemma.endsWith("nk")) {
            voiced = 'g';
          }
          modifiedSeq.setCharAt(modifiedSeq.length() - 1, voiced);
          modifiedAttrs.remove(PhoneticAttribute.LastLetterVoicelessStop);
          originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
          modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
          modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
        }
        case Doubling -> {
          modifiedSeq.append(ALPHABET.lastChar(modifiedSeq));
          originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
          modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
          modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
        }
        case LastVowelDrop -> {
          TurkicLetter lastLetter = ALPHABET.getLastLetter(modifiedSeq);
          if (lastLetter.isVowel()) {
            modifiedSeq.deleteCharAt(modifiedSeq.length() - 1);
            modifiedAttrs.add(PhoneticAttribute.ExpectsConsonant);
            modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          } else {
            modifiedSeq.deleteCharAt(modifiedSeq.length() - 2);
            if (dicItem.primaryPos == PrimaryPos.Verb) {
              unmodifiedRootState = morphotactics.verbLastVowelDropUnmodRoot_S;
              modifiedRootState = morphotactics.verbLastVowelDropModRoot_S;
            } else {
              originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
            }
            modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
            modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          }
        }
        case InverseHarmony -> {
          originalAttrs.add(PhoneticAttribute.LastVowelFrontal);
          originalAttrs.remove(PhoneticAttribute.LastVowelBack);
          modifiedAttrs.add(PhoneticAttribute.LastVowelFrontal);
          modifiedAttrs.remove(PhoneticAttribute.LastVowelBack);
        }
        case ProgressiveVowelDrop -> {
          if (modifiedSeq.length() > 1) {
            modifiedSeq.deleteCharAt(modifiedSeq.length() - 1);
            if (ALPHABET.containsVowel(modifiedSeq)) {
              modifiedAttrs = calculateAttributes(modifiedSeq);
            }
            modifiedAttrs.add(PhoneticAttribute.LastLetterDropped);
          }
        }
        default -> {
        }
      }
    }

    StemTransition original =
        stemTransition(dicItem.root, dicItem, originalAttrs, unmodifiedRootState);
    StemTransition modified =
        stemTransition(modifiedSeq.toString(), dicItem, modifiedAttrs, modifiedRootState);

    if (original.equals(modified)) {
      return List.of(original);
    }
    if (isNonDroppedLastVowelDropVariant(dicItem) && dicItem.hasAttribute(Voicing)) {
      return List.of(modified);
    }
    return List.of(original, modified);
  }

  private List<StemTransition> handleSpecialRoots(DictionaryItem item) {
    AttributeSet<PhoneticAttribute> originalAttrs = calculateAttributes(item.pronunciation);
    return switch (item.getId()) {
      case "içeri_Noun", "içeri_Adj", "dışarı_Adj", "dışarı_Noun", "dışarı_Postp",
           "yukarı_Noun", "yukarı_Adj", "ileri_Noun", "şura_Noun", "bura_Noun", "ora_Noun" -> {
        MorphemeState rootForModified = switch (item.primaryPos) {
          case Noun -> morphotactics.nounLastVowelDropRoot_S;
          case Adjective, PostPositive -> morphotactics.adjLastVowelDropRoot_S;
          default -> throw new IllegalStateException("No root morpheme state found for " + item);
        };
        String truncated = item.root.substring(0, item.root.length() - 1);
        AttributeSet<PhoneticAttribute> modAttrs = calculateAttributes(truncated);
        modAttrs.add(PhoneticAttribute.ExpectsConsonant);
        modAttrs.add(PhoneticAttribute.CannotTerminate);
        yield List.of(
            stemTransition(item.root, item, originalAttrs, null),
            new StemTransition(truncated, item, modAttrs, rootForModified));
      }
      case "ben_Pron_Pers", "sen_Pron_Pers" ->
          pronounTransitions(
              item,
              originalAttrs,
              null,
              item.lemma.equals("ben") ? "ban" : "san",
              morphotactics.pronPers_Mod_S);
      case "demek_Verb", "yemek_Verb" -> {
        String alt = item.lemma.equals("demek") ? "di" : "yi";
        yield List.of(
            new StemTransition(item.root, item, originalAttrs, morphotactics.vDeYeRoot_S),
            new StemTransition(alt, item, calculateAttributes(alt), morphotactics.vDeYeRoot_S));
      }
      case "imek_Verb" ->
          List.of(new StemTransition(item.root, item, originalAttrs, morphotactics.imekRoot_S));
      case "birbiri_Pron_Quant", "çoğu_Pron_Quant", "öbürü_Pron_Quant", "birçoğu_Pron_Quant" -> {
        String modStem = switch (item.lemma) {
          case "birbiri" -> "birbir";
          case "çoğu" -> "çok";
          case "öbürü" -> "öbür";
          default -> "birçok";
        };
        yield pronounTransitions(
            item,
            originalAttrs,
            morphotactics.pronQuant_S,
            modStem,
            morphotactics.pronQuantModified_S);
      }
      default ->
          throw new IllegalArgumentException(
              "Lexicon Item with special stem change cannot be handled:" + item);
    };
  }

  private List<StemTransition> pronounTransitions(
      DictionaryItem item,
      AttributeSet<PhoneticAttribute> originalAttrs,
      MorphemeState unmodState,
      String modifiedStem,
      MorphemeState modState) {
    StemTransition original = stemTransition(item.root, item, originalAttrs, unmodState);
    StemTransition modified =
        new StemTransition(modifiedStem, item, calculateAttributes(modifiedStem), modState);
    original.getPhoneticAttributes().add(PhoneticAttribute.UnModifiedPronoun);
    modified.getPhoneticAttributes().add(PhoneticAttribute.ModifiedPronoun);
    return List.of(original, modified);
  }
}
