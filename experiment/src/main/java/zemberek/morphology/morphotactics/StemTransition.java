package zemberek.morphology.morphotactics;

import java.util.EnumSet;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.lexicon.DictionaryItem;

public class StemTransition extends MorphemeTransition {

  public final String surface;
  public final DictionaryItem item;
  EnumSet<PhoneticAttribute> phoneticAttributes;

  public StemTransition(
      String surface,
      DictionaryItem item,
      EnumSet<PhoneticAttribute> phoneticAttributes,
      MorphemeState toState) {
    this.surface = surface;
    this.item = item;
    this.phoneticAttributes = phoneticAttributes;
    this.to = toState;
  }

  public EnumSet<PhoneticAttribute> getPhoneticAttributes() {
    return phoneticAttributes;
  }

  public String debugForm() {
    return "[(Dict:" + item.toString() + "):" + surface +
        " → " + to.toString() + "]";
  }

  @Override
  public String toString() {
    return debugForm();
  }
}
