package zemberek.morphology.morphotactics;

import java.util.EnumSet;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.morphology.lexicon.DictionaryItem;

public class StemTransition extends MorphemeTransition {

  public final String surface;
  public final DictionaryItem item;
  EnumSet<PhoneticAttribute> phoneticAttributes;
  EnumSet<PhoneticExpectation> phoneticExpectations;

  public StemTransition(
      String surface,
      DictionaryItem item,
      EnumSet<PhoneticAttribute> phoneticAttributes,
      EnumSet<PhoneticExpectation> phoneticExpectations,
      MorphemeState toState) {
    this.surface = surface;
    this.item = item;
    this.phoneticAttributes = phoneticAttributes;
    this.phoneticExpectations = phoneticExpectations;
    this.to = toState;
  }

  public EnumSet<PhoneticAttribute> getPhoneticAttributes() {
    return phoneticAttributes;
  }

  public EnumSet<PhoneticExpectation> getPhoneticExpectations() {
    return phoneticExpectations;
  }

}
