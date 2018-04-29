package zemberek.morphology.morphotactics;

import java.util.Objects;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.lexicon.DictionaryItem;

public class StemTransition extends MorphemeTransition {

  public final String surface;
  public final DictionaryItem item;
  AttributeSet<PhoneticAttribute> phoneticAttributes;

  int cachedHash;

  public StemTransition(
      String surface,
      DictionaryItem item,
      AttributeSet<PhoneticAttribute> phoneticAttributes,
      MorphemeState toState) {
    this.surface = surface;
    this.item = item;
    this.phoneticAttributes = phoneticAttributes;
    this.to = toState;
    this.cachedHash = hashCode();
  }

  public StemTransition getCopy() {
    StemTransition t = new StemTransition(surface, item, phoneticAttributes, to);
    t.from = from;
    return t;
  }

  public AttributeSet<PhoneticAttribute> getPhoneticAttributes() {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StemTransition that = (StemTransition) o;
    return Objects.equals(surface, that.surface) &&
        Objects.equals(item, that.item) &&
        Objects.equals(phoneticAttributes, that.phoneticAttributes);
  }

  @Override
  public int hashCode() {
    if (cachedHash != 0) {
      return cachedHash;
    }
    return Objects.hash(surface, item, phoneticAttributes);
  }
}
