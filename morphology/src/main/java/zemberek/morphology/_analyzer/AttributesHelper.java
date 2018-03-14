package zemberek.morphology._analyzer;

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
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.morphology._morphotactics.AttributeSet;

/** Helper class for calculating morphemic attributes. */
public class AttributesHelper {

  private static final List<PhoneticAttribute> NO_VOWEL_ATTRIBUTES = Arrays
      .asList(LastLetterConsonant, FirstLetterConsonant, HasNoVowel);

  public static AttributeSet<PhoneticAttribute> getMorphemicAttributes(String seq) {
      return getMorphemicAttributes(seq, AttributeSet.emptySet());
  }

  public static AttributeSet<PhoneticAttribute> getMorphemicAttributes(
      String str,
      AttributeSet<PhoneticAttribute> predecessorAttrs) {
    return getMorphemicAttributes(new TurkishLetterSequence(str, TurkishAlphabet.INSTANCE),
        predecessorAttrs);
  }

  public static AttributeSet<PhoneticAttribute> getMorphemicAttributes(
      TurkishLetterSequence seq,
      AttributeSet<PhoneticAttribute> predecessorAttrs) {
    if (seq.length() == 0) {
      return predecessorAttrs.copy();
    }
    AttributeSet<PhoneticAttribute> attrs = new AttributeSet<>();
    if (seq.hasVowel()) {
      if (seq.lastVowel().isFrontal()) {
        attrs.add(LastVowelFrontal);
      } else {
        attrs.add(LastVowelBack);
      }
      if (seq.lastVowel().isRounded()) {
        attrs.add(LastVowelRounded);
      } else {
        attrs.add(LastVowelUnrounded);
      }
      if (seq.lastLetter().isVowel()) {
        attrs.add(LastLetterVowel);
      } else {
        attrs.add(LastLetterConsonant);
      }
      if (seq.firstLetter().isVowel()) {
        attrs.add(FirstLetterVowel);
      } else {
        attrs.add(FirstLetterConsonant);
      }
    } else {
      // we transfer vowel attributes from the predecessor attributes.
      attrs.copyFrom(predecessorAttrs);
      attrs.addAll(NO_VOWEL_ATTRIBUTES);
      attrs.remove(LastLetterVowel);
    }
    if (seq.lastLetter().isVoiceless()) {
      attrs.add(LastLetterVoiceless);
      if (seq.lastLetter().isStopConsonant()) {
        // kitap
        attrs.add(LastLetterVoicelessStop);
      }
    } else {
      attrs.add(LastLetterVoiced);
    }
    return attrs;
  }

  public static AttributeSet<PhoneticAttribute> getMorphemicAttributes(TurkishLetterSequence seq) {
    return getMorphemicAttributes(seq, AttributeSet.emptySet());
  }

}
