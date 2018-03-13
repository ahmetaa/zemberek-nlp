package zemberek.morphology._morphotactics;

import java.util.EnumSet;
import java.util.Objects;
import zemberek.core.turkish.PhoneticAttribute;

// Encapsulates a set of PhoneticAttribute enums.
// TODO: Make generic
public class PhoneticAttributeSet {
  private EnumSet<PhoneticAttribute> attrs;
  private EnumBitSet<PhoneticAttribute> battrs;

  public PhoneticAttributeSet() {
    attrs = EnumSet.noneOf(PhoneticAttribute.class);
  }

  public PhoneticAttributeSet(EnumSet<PhoneticAttribute> attrs) {
    this.attrs = attrs.clone();
  }

  public static PhoneticAttributeSet copyOf(PhoneticAttributeSet other) {
    return new PhoneticAttributeSet(other.attrs);
  }

  public void add(PhoneticAttribute attr) {
    attrs.add(attr);
  }

  public void remove(PhoneticAttribute attr) {
    attrs.remove(attr);
  }

  public void addAll(Iterable<PhoneticAttribute> attributes) {
    for (PhoneticAttribute attr : attributes) {
      attrs.add(attr);
    }
  }

  public boolean contains(PhoneticAttribute attr) {
    return attrs.contains(attr);
  }

  @Override
  public PhoneticAttributeSet clone() {
    return new PhoneticAttributeSet(this.attrs);
  }

  @Override
  public int hashCode() {
    return attrs.hashCode();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PhoneticAttributeSet that = (PhoneticAttributeSet) o;
    return Objects.equals(attrs, that.attrs);
  }
}
