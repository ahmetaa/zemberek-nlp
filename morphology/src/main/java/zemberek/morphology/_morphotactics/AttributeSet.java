package zemberek.morphology._morphotactics;

import javax.management.Attribute;

public class AttributeSet<E extends Enum<E>> {

  private static final AttributeSet EMPTY_SET = new AttributeSet<>();

  private int bits;

  public AttributeSet() {
    this(0);
  }

  private AttributeSet(int initialValue) {
    this.bits = initialValue;
  }

  public static  <E extends Enum<E>> AttributeSet<E> of(E e1) {
    AttributeSet<E> res = new AttributeSet<>();
    res.add(e1);
    return res;
  }

  public static  <E extends Enum<E>> AttributeSet<E> of(E e1, E e2) {
    AttributeSet<E> res = new AttributeSet<>();
    res.add(e1);
    res.add(e2);
    return res;
  }
  public static  <E extends Enum<E>> AttributeSet<E> of(E e1, E e2, E e3) {
    AttributeSet<E> res = new AttributeSet<>();
    res.add(e1);
    res.add(e2);
    res.add(e3);
    return res;
  }

  @SuppressWarnings("unchecked")
  public static <E extends Enum<E>> AttributeSet<E> emptySet() {
    return (AttributeSet<E>) EMPTY_SET;
  }

  public void copyFrom(AttributeSet<E> other) {
    this.bits = other.bits;
  }

  public void add(E en) {
    bits |= mask(en);
  }

  public void add(E... enums) {
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

  public void removeAll(Iterable<E> enums) {
    for (E en : enums) {
      remove(en);
    }
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
    if (other == null || !(other instanceof AttributeSet)) {
      return false;
    }
    return bits == ((AttributeSet)other).bits;
  }

  public int getBits() {
    return bits;
  }

}