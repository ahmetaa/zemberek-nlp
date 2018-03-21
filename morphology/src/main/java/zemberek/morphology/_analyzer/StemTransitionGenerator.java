package zemberek.morphology._analyzer;

import static zemberek.core.turkish.RootAttribute.CompoundP3sg;
import static zemberek.core.turkish.RootAttribute.CompoundP3sgRoot;
import static zemberek.core.turkish.RootAttribute.Doubling;
import static zemberek.core.turkish.RootAttribute.InverseHarmony;
import static zemberek.core.turkish.RootAttribute.LastVowelDrop;
import static zemberek.core.turkish.RootAttribute.ProgressiveVowelDrop;
import static zemberek.core.turkish.RootAttribute.Voicing;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.morphology._morphotactics.AttributeSet;
import zemberek.morphology._morphotactics.MorphemeState;
import zemberek.morphology._morphotactics.StemTransition;
import zemberek.morphology._morphotactics.TurkishMorphotactics;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.LexiconException;


/**
 * This class generates StemNode objects from Dictionary Items. Generated Nodes are not connected.
 */
public class StemTransitionGenerator {

  TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

  TurkishMorphotactics morphotactics;

  EnumSet<RootAttribute> modifiers = EnumSet.of(
      Doubling,
      LastVowelDrop,
      ProgressiveVowelDrop,
      InverseHarmony,
      Voicing,
      CompoundP3sg,
      CompoundP3sgRoot
  );

  public StemTransitionGenerator(TurkishMorphotactics morphotactics) {
    this.morphotactics = morphotactics;
  }

  /**
   * Generates StemTransition objects from the dictionary item. <p>Most of the time a single
   * StemNode is generated.
   *
   * @param item DictionaryItem
   * @return one or more StemTransition objects.
   */
  public List<StemTransition> generate(DictionaryItem item) {
    if (specialRoots.contains(item.id)) {
      return handleSpecialRoots(item);
    }
    if (hasModifierAttribute(item)) {
      return generateModifiedRootNodes(item);
    } else {
      AttributeSet<PhoneticAttribute> phoneticAttributes = calculateAttributes(item.pronunciation);
      StemTransition transition = new StemTransition(
          item.root,
          item,
          phoneticAttributes,
          morphotactics.getRootState(item, phoneticAttributes)
      );

      return Lists.newArrayList(transition);
    }
  }

  private boolean hasModifierAttribute(DictionaryItem item) {
    for (RootAttribute attr : modifiers) {
      if (item.attributes.contains(attr)) {
        return true;
      }
    }
    return false;
  }

  private AttributeSet<PhoneticAttribute> calculateAttributes(String input) {
    return AttributesHelper.getMorphemicAttributes(input);
  }

  private AttributeSet<PhoneticAttribute> calculateAttributes(TurkishLetterSequence sequence) {
    return AttributesHelper.getMorphemicAttributes(sequence);
  }

  private List<StemTransition> generateModifiedRootNodes(DictionaryItem dicItem) {

    TurkishLetterSequence modifiedSeq = new TurkishLetterSequence(dicItem.pronunciation, alphabet);
    AttributeSet<PhoneticAttribute> originalAttrs = calculateAttributes(dicItem.pronunciation);
    AttributeSet<PhoneticAttribute> modifiedAttrs = originalAttrs.copy();

    for (RootAttribute attribute : dicItem.attributes) {

      // generate other boundary attributes and modified root state.
      switch (attribute) {
        case Voicing:
          TurkicLetter last = modifiedSeq.lastLetter();
          TurkicLetter modifiedLetter = alphabet.voice(last);
          if (modifiedLetter == null) {
            throw new LexiconException("Voicing letter is not proper in:" + dicItem);
          }
          if (dicItem.lemma.endsWith("nk")) {
            modifiedLetter = TurkishAlphabet.L_g;
          }
          modifiedSeq.changeLetter(modifiedSeq.length() - 1, modifiedLetter);
          modifiedAttrs.remove(PhoneticAttribute.LastLetterVoicelessStop);
          originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
          modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
          // TODO: find a better way for this.
          modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          break;
        case Doubling:
          modifiedSeq.append(modifiedSeq.lastLetter());
          originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
          modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
          modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          break;
        case LastVowelDrop:
          if (modifiedSeq.lastLetter().isVowel()) {
            modifiedSeq.delete(modifiedSeq.length() - 1);
            modifiedAttrs.add(PhoneticAttribute.ExpectsConsonant);
            modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          } else {
            modifiedSeq.delete(modifiedSeq.length() - 2);
            if (!dicItem.primaryPos.equals(PrimaryPos.Verb)) {
              originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
            }
            modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
            modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          }
          break;
        case InverseHarmony:
          originalAttrs.add(PhoneticAttribute.LastVowelFrontal);
          originalAttrs.remove(PhoneticAttribute.LastVowelBack);
          modifiedAttrs.add(PhoneticAttribute.LastVowelFrontal);
          modifiedAttrs.remove(PhoneticAttribute.LastVowelBack);
          break;
        case ProgressiveVowelDrop:
          modifiedSeq.delete(modifiedSeq.length() - 1);
          if (modifiedSeq.hasVowel()) {
            modifiedAttrs = calculateAttributes(modifiedSeq);
          }
          modifiedAttrs.add(PhoneticAttribute.LastVowelDropped);
          break;
        default:
          break;
      }
    }

    StemTransition original = new StemTransition(
        dicItem.root,
        dicItem,
        originalAttrs,
        morphotactics.getRootState(dicItem, originalAttrs));

    StemTransition modified = new StemTransition(
        modifiedSeq.toString(),
        dicItem,
        modifiedAttrs,
        morphotactics.getRootState(dicItem, modifiedAttrs));

    if (original.equals(modified)) {
      return Collections.singletonList(original);
    }
    return Lists.newArrayList(original, modified);
  }

  Set<String> specialRoots = Sets.newHashSet(
      "içeri_Noun", "içeri_Adj", "dışarı_Adj",
      "dışarı_Noun", "dışarı_Postp", "yukarı_Noun", "yukarı_Adj",
      "ben_Pron_Pers", "sen_Pron_Pers", "demek_Verb", "yemek_Verb",
      "birbiri_Pron_Quant", "çoğu_Pron_Quant", "öbürü_Pron_Quant", "birçoğu_Pron_Quant"
  );

  private List<StemTransition> handleSpecialRoots(DictionaryItem item) {

    String id = item.getId();
    AttributeSet<PhoneticAttribute> originalAttrs = calculateAttributes(item.pronunciation);
    StemTransition original, modified;
    MorphemeState unmodifiedRootState = morphotactics.getRootState(item, originalAttrs);

    switch (id) {
      case "içeri_Noun":
      case "içeri_Adj":
      case "dışarı_Adj":
      case "dışarı_Noun":
      case "dışarı_Postp":
      case "yukarı_Noun":
      case "yukarı_Adj":
        original = new StemTransition(item.root, item, originalAttrs, unmodifiedRootState);
        String m = item.root.substring(0, item.root.length() - 1);
        modified = new StemTransition(m, item, calculateAttributes(m), unmodifiedRootState);
        modified.getPhoneticAttributes().add(PhoneticAttribute.ExpectsConsonant);
        modified.getPhoneticAttributes().add(PhoneticAttribute.CannotTerminate);
        return Lists.newArrayList(original, modified);

      case "ben_Pron_Pers":
      case "sen_Pron_Pers":
        original = new StemTransition(item.root, item, originalAttrs, unmodifiedRootState);
        if (item.lemma.equals("ben")) {
          modified = new StemTransition("ban", item, calculateAttributes("ban"),
              morphotactics.pronPers_Mod_S);
        } else {
          modified = new StemTransition("san", item, calculateAttributes("san"),
              morphotactics.pronPers_Mod_S);
        }
        original.getPhoneticAttributes().add(PhoneticAttribute.UnModifiedPronoun);
        modified.getPhoneticAttributes().add(PhoneticAttribute.ModifiedPronoun);
        return Lists.newArrayList(original, modified);
      case "demek_Verb":
      case "yemek_Verb":
        original = new StemTransition(item.root, item, originalAttrs, morphotactics.vDeYeRoot_S);
        switch (item.lemma) {
          case "demek":
            modified = new StemTransition("di", item, calculateAttributes("di"),
                morphotactics.vDeYeRoot_S);
            break;
          default:
            modified = new StemTransition("yi", item, calculateAttributes("yi"),
                morphotactics.vDeYeRoot_S);
        }
        return Lists.newArrayList(original, modified);

      case "birbiri_Pron_Quant":
      case "çoğu_Pron_Quant":
      case "öbürü_Pron_Quant":
      case "birçoğu_Pron_Quant":
        original = new StemTransition(item.root, item, originalAttrs, morphotactics.pronQuant_S);

        switch (item.lemma) {
          case "birbiri":
            modified = new StemTransition("birbir", item, calculateAttributes("birbir"),
                morphotactics.pronQuantModified_S);
            break;
          case "çoğu":
            modified = new StemTransition("çok", item, calculateAttributes("çok"),
                morphotactics.pronQuantModified_S);
            break;
          case "öbürü":
            modified = new StemTransition("öbür", item, calculateAttributes("öbür"),
                morphotactics.pronQuantModified_S);
            break;
          default:
            modified = new StemTransition("birçok", item, calculateAttributes("birçok"),
                morphotactics.pronQuantModified_S);
            break;
        }
        original.getPhoneticAttributes().add(PhoneticAttribute.UnModifiedPronoun);
        modified.getPhoneticAttributes().add(PhoneticAttribute.ModifiedPronoun);
        return Lists.newArrayList(original, modified);
      default:
        throw new IllegalArgumentException(
            "Lexicon Item with special stem change cannot be handled:" + item);
    }


  }

}