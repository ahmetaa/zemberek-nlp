package zemberek.core.enums;

import java.util.EnumSet;
import zemberek.core.turkish.PhoneticAttribute;

public class EnumBitSet {

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

  // TODO: This is lame, make it generic.
  public static EnumBitSet fromSet(EnumSet<PhoneticAttribute> enumSet) {
    EnumBitSet res = new EnumBitSet();
    for (PhoneticAttribute attr : enumSet) {
      res.add(attr);
    }
    return res;
  }

  public static EnumBitSet of(BitmapEnum e1) {
    EnumBitSet res = new EnumBitSet();
    res.add(e1);
    return res;
  }

  public static EnumBitSet of(BitmapEnum e1, BitmapEnum e2) {
    EnumBitSet res = new EnumBitSet();
    res.add(e1);
    res.add(e2);
    return res;
  }

  public static EnumBitSet of(BitmapEnum e1, BitmapEnum e2, BitmapEnum e3) {
    EnumBitSet res = new EnumBitSet();
    res.add(e1);
    res.add(e2);
    res.add(e3);
    return res;
  }

  public void add(BitmapEnum en) {
    bits |= en.getBitMask();
  }

  public void add(BitmapEnum... enums) {
    for (BitmapEnum en : enums) {
      add(en);
    }
  }

  public void addAll(Iterable<BitmapEnum> enums) {
    for (BitmapEnum en : enums) {
      add(en);
    }
  }

  public void remove(BitmapEnum en) {
    bits &= ~en.getBitMask();
  }

  public void remove(BitmapEnum... enums) {
    for (BitmapEnum en : enums) {
      remove(en);
    }
  }

  public void removeAll(Iterable<BitmapEnum> enums) {
    for (BitmapEnum en : enums) {
      remove(en);
    }
  }

  public boolean contains(BitmapEnum en) {
    return (bits & en.getBitMask()) != 0;
  }

  public boolean containsBoth(BitmapEnum en1, BitmapEnum en2) {
    return contains(en1) && contains(en2);
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
