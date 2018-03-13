package zemberek.morphology._morphotactics;

import java.util.EnumSet;

public class EnumBitSet<E extends Enum<E>> {

  private int bits;

  public EnumBitSet() {
    this(0);
  }

  private EnumBitSet(int initialValue) {
    this.bits = initialValue;
  }

  public static EnumBitSet copyOf(EnumBitSet other) {
    return new EnumBitSet(other.bits);
  }

  public static <E extends Enum<E>> EnumBitSet<E> fromSet(EnumSet<E> enumSet) {
    EnumBitSet<E> res = new EnumBitSet<>();
    for (E e : enumSet) {
      res.add(e);
    }
    return res;
  }

  public static  <E extends Enum<E>> EnumBitSet<E> of(E e1) {
    EnumBitSet<E> res = new EnumBitSet<>();
    res.add(e1);
    return res;
  }

  public static  <E extends Enum<E>> EnumBitSet<E> of(E e1, E e2) {
    EnumBitSet<E> res = new EnumBitSet<>();
    res.add(e1);
    res.add(e2);
    return res;
  }
  public static  <E extends Enum<E>> EnumBitSet<E> of(E e1, E e2, E e3) {
    EnumBitSet<E> res = new EnumBitSet<>();
    res.add(e1);
    res.add(e2);
    res.add(e3);
    return res;
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

  public boolean containsBoth(E e1, E e2) {
    return contains(e1) && contains(e2);
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
    if (other == null || !(other instanceof EnumBitSet)) {
      return false;
    }
    return bits == ((EnumBitSet)other).bits;
  }

  public int getBits() {
    return bits;
  }

}