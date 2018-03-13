package zemberek.morphology._analyzer;

import static zemberek.core.turkish.RootAttribute.CompoundP3sg;
import static zemberek.core.turkish.RootAttribute.CompoundP3sgRoot;
import static zemberek.core.turkish.RootAttribute.Doubling;
import static zemberek.core.turkish.RootAttribute.InverseHarmony;
import static zemberek.core.turkish.RootAttribute.LastVowelDrop;
import static zemberek.core.turkish.RootAttribute.ProgressiveVowelDrop;
import static zemberek.core.turkish.RootAttribute.Special;
import static zemberek.core.turkish.RootAttribute.Voicing;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.morphology._morphotactics.MorphemeState;
import zemberek.morphology._morphotactics.PhoneticAttributeSet;
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
      Special,
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
    if (hasModifierAttribute(item)) {
      return generateModifiedRootNodes(item);
    } else {
      PhoneticAttributeSet phoneticAttributes = calculateAttributes(item.pronunciation);
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

  private PhoneticAttributeSet calculateAttributes(String input) {
    return calculateAttributes(new TurkishLetterSequence(input, alphabet));
  }

  private PhoneticAttributeSet calculateAttributes(TurkishLetterSequence sequence) {
    PhoneticAttributeSet attrs = new PhoneticAttributeSet();
    // general phonetic attributes.
    if (sequence.vowelCount() > 0) {
      if (sequence.lastVowel().isRounded()) {
        attrs.add(PhoneticAttribute.LastVowelRounded);
      } else {
        attrs.add(PhoneticAttribute.LastVowelUnrounded);
      }
      if (sequence.lastVowel().isFrontal()) {
        attrs.add(PhoneticAttribute.LastVowelFrontal);
      } else {
        attrs.add(PhoneticAttribute.LastVowelBack);
      }
    }
    if (sequence.lastLetter().isVowel()) {
      // elma
      attrs.add(PhoneticAttribute.LastLetterVowel);
    } else {
      attrs.add(PhoneticAttribute.LastLetterConsonant);
    }
    if (sequence.lastLetter().isVoiceless()) {
      attrs.add(PhoneticAttribute.LastLetterVoiceless);
      if (sequence.lastLetter().isStopConsonant()) {
        // kitap
        attrs.add(PhoneticAttribute.LastLetterVoicelessStop);
      }
    } else {
      attrs.add(PhoneticAttribute.LastLetterVoiced);
    }
    return attrs;
  }

  private List<StemTransition> generateModifiedRootNodes(DictionaryItem dicItem) {

    if (dicItem.hasAttribute(RootAttribute.Special)) {
      return handleSpecialRoots(dicItem);
    }

    TurkishLetterSequence modifiedSeq = new TurkishLetterSequence(dicItem.pronunciation, alphabet);
    PhoneticAttributeSet originalAttrs = calculateAttributes(dicItem.pronunciation);
    PhoneticAttributeSet modifiedAttrs = originalAttrs.clone();

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
          break;
        case Doubling:
          modifiedSeq.append(modifiedSeq.lastLetter());
          originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
          modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
          break;
        case LastVowelDrop:
          if (modifiedSeq.lastLetter().isVowel()) {
            modifiedSeq.delete(modifiedSeq.length() - 1);
            modifiedAttrs.add(PhoneticAttribute.ExpectsConsonant);
          } else {
            modifiedSeq.delete(modifiedSeq.length() - 2);
            if (!dicItem.primaryPos.equals(PrimaryPos.Verb)) {
              originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
            }
            modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
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

  private List<StemTransition> handleSpecialRoots(DictionaryItem item) {

    String id = item.getId();
    PhoneticAttributeSet originalAttrs = calculateAttributes(item.pronunciation);
    StemTransition original, modified;
    MorphemeState unmodifiedRootState = morphotactics.getRootState(item, originalAttrs);

    if (id.equals("ben_Pron_Pers") || id.equals("sen_Pron_Pers")) {
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
    } else if (id.equals("demek_Verb") || id.equals("yemek_Verb")) {
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

    } else if (id.equals("birbiri_Pron_Quant")
        || id.equals("çoğu_Pron_Quant")
        || id.equals("öbürü_Pron_Quant")
        || id.equals("birçoğu_Pron_Quant")) {
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
    } else {
      throw new IllegalArgumentException(
          "Lexicon Item with special stem change cannot be handled:" + item);
    }


  }

}