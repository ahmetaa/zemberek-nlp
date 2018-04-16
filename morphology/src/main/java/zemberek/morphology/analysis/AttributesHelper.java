package zemberek.morphology.analysis;

import static zemberek.core.turkish.PhoneticAttribute.ExpectsConsonant;
import static zemberek.core.turkish.PhoneticAttribute.FirstLetterConsonant;
import static zemberek.core.turkish.PhoneticAttribute.FirstLetterVowel;
import static zemberek.core.turkish.PhoneticAttribute.HasNoVowel;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterConsonant;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterVoiced;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterVoiceless;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterVoicelessStop;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterVowel;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelBack;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelFrontal;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelRounded;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelUnrounded;

import java.util.Arrays;
import java.util.List;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.morphotactics.AttributeSet;

/**
 * Helper class for calculating morphemic attributes.
 */
public class AttributesHelper {

  private static final List<PhoneticAttribute> NO_VOWEL_ATTRIBUTES = Arrays
      .asList(LastLetterConsonant, FirstLetterConsonant, HasNoVowel);

  public static AttributeSet<PhoneticAttribute> getMorphemicAttributes(CharSequence seq) {
    return getMorphemicAttributes(seq, AttributeSet.emptySet());
  }

  private static TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

  public static AttributeSet<PhoneticAttribute> getMorphemicAttributes(
      CharSequence seq,
      AttributeSet<PhoneticAttribute> predecessorAttrs) {
    if (seq.length() == 0) {
      return predecessorAttrs.copy();
    }
    AttributeSet<PhoneticAttribute> attrs = new AttributeSet<>();
    if (alphabet.containsVowel(seq)) {

      TurkicLetter last = alphabet.getLastLetter(seq);
      if (last.isVowel()) {
        attrs.add(LastLetterVowel);
      } else {
        attrs.add(LastLetterConsonant);
      }

      TurkicLetter lastVowel = last.isVowel() ? last : alphabet.getLastVowel(seq);

      if (lastVowel.isFrontal()) {
        attrs.add(LastVowelFrontal);
      } else {
        attrs.add(LastVowelBack);
      }
      if (lastVowel.isRounded()) {
        attrs.add(LastVowelRounded);
      } else {
        attrs.add(LastVowelUnrounded);
      }

      if (alphabet.getFirstLetter(seq).isVowel()) {
        attrs.add(FirstLetterVowel);
      } else {
        attrs.add(FirstLetterConsonant);
      }
    } else {
      // we transfer vowel attributes from the predecessor attributes.
      attrs.copyFrom(predecessorAttrs);
      attrs.addAll(NO_VOWEL_ATTRIBUTES);
      attrs.remove(LastLetterVowel);
      attrs.remove(ExpectsConsonant);
    }

    TurkicLetter last = alphabet.getLastLetter(seq);

    if (last.isVoiceless()) {
      attrs.add(LastLetterVoiceless);
      if (last.isStopConsonant()) {
        // kitap
        attrs.add(LastLetterVoicelessStop);
      }
    } else {
      attrs.add(LastLetterVoiced);
    }
    return attrs;
  }

}
