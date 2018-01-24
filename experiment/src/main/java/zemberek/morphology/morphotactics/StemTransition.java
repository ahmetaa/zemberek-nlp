package zemberek.morphology.morphotactics;

import java.util.EnumSet;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.morphology.lexicon.DictionaryItem;

public class StemTransition {

  public final String surface;
  public final DictionaryItem item;
  EnumSet<PhoneticAttribute> phoneticAttributes;
  EnumSet<PhoneticExpectation> phoneticExpectations;
  MorphemeState toState;

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
    this.toState = toState;
  }
}
