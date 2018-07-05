package zemberek.morphology.morphotactics;

 /**
  * A class for representing a set of enums efficiently.
  *
  *</p>
  * Note: Uses ordinals as bit indexes of an int, so only works for maximum of
  * 32 different enum values. If serialized as is, the ordinals of enums must
  * not change.
  */
public class AttributeSet<E extends Enum<E>> {

  private int bits;

  public AttributeSet() {
    this(0);
  }

  private AttributeSet(int initialValue) {
    this.bits = initialValue;
  }

  @SafeVarargs
  public static  <E extends Enum<E>> AttributeSet<E> of(E... enums) {
    AttributeSet<E> res = new AttributeSet<>();
    for (E en : enums) {
      res.add(en);
    }
    return res;
  }

  public static <E extends Enum<E>> AttributeSet<E> emptySet() {
    return new AttributeSet<>();
  }

  public void copyFrom(AttributeSet<E> other) {
    this.bits = other.bits;
  }

  public void add(E en) {
    if (en.ordinal() > 31) {
      throw new IllegalArgumentException("Set can contain enums with max ordinal of 31.");
    }
    bits |= mask(en);
  }

  @SafeVarargs
  public final void add(E... enums) {
    for (E en : enums) {
      add(en);
    }
  }

  public void addAll(Iterable<E> enums) {
    for (E en : enums) {
      add(en);
    }
  }

  public void remove(E en) {
    bits &= ~mask(en);
  }

  public boolean contains(E en) {
    return (bits & mask(en)) != 0;
  }

  private int mask(E e) {
    return 1 << e.ordinal();
  }

  public AttributeSet<E> copy() {
    return new AttributeSet<>(this.bits);
  }

  @Override
  public int hashCode() {
    return bits;
  }

  @Override
  public boolean equals(Object other) {
    if(this == other) {
      return  true;
    }
    if (!(other instanceof AttributeSet)) {
      return false;
    }
    return bits == ((AttributeSet)other).bits;
  }

  public int getBits() {
    return bits;
  }

}