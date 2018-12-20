package zemberek.core.collections;

import zemberek.core.IntPair;

/**
 * A simple hashmap with integer keys and integer values. Implements open address linear probing
 * algorithm. Constraints: <pre>
 * - Supports int key values in range (Integer.MIN_VALUE+1..Integer.MAX_VALUE];
 * - Does not implement Map interface
 * - Capacity can be max 1 << 30
 * - Load factor is 0.5.
 * - Max size is 2^29 (~537M elements)
 * - Does not implement Iterable.
 * - Class is not thread safe.
 * </pre>
 */
public final class IntIntMap extends CompactIntMapBase {

  public IntIntMap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  public IntIntMap(int capacity) {
    super(capacity);
  }

  private void setValue(int i, int value) {
    entries[i] = (entries[i] & 0x0000_0000_FFFF_FFFFL) | ((value & 0xFFFF_FFFFL) << 32);
  }

  private void setKeyValue(int i, int key, int value) {
    entries[i] = (key & 0xFFFF_FFFFL) | ((long) value << 32);
  }

  private int getValue(int i) {
    return (int) (entries[i] >>> 32);
  }

  public void put(int key, int value) {
    checkKey(key);
    expandIfNecessary();
    int loc = locate(key);
    if (loc >= 0) {
      setValue(loc, value);
    } else {
      setKeyValue(-loc - 1, key, value);
      keyCount++;
    }
  }

  /**
   * Used only when expanding.
   */
  private void putSafe(int key, int value) {
    int loc = firstProbe(key);
    while (true) {
      if (getKey(loc) == EMPTY) {
        setKeyValue(loc, key, value);
        return;
      }
      loc = probe(loc);
    }
  }

  public void increment(int key, int value) {
    checkKey(key);
    expandIfNecessary();
    int loc = locate(key);
    if (loc >= 0) {
      setValue(loc, value + getValue(loc));
    } else {
      setKeyValue(-loc - 1, key, value);
      keyCount++;
    }
  }

  /**
   * @return The value {@code T} that is mapped to given {@code key}. or {@code NO_RESULT} If key
   * does not exist,
   * @throws IllegalArgumentException if key is {@code EMPTY} or {@code DELETED}.
   */
  public int get(int key) {
    checkKey(key);
    int slot = firstProbe(key);
    while (true) {
      final long entry = entries[slot];
      final int t = (int) (entry & 0xFFFF_FFFFL);
      if (t == key) {
        return (int) (entry >>> 32);
      }
      if (t == EMPTY) {
        return NO_RESULT;
      }
      slot = probe(slot);
      // DELETED slots are skipped.
    }
  }

  /**
   * @return The array of values in the map. Not ordered.
   */
  public int[] getValues() {
    int[] valueArray = new int[keyCount];
    for (int i = 0, j = 0; i < entries.length; i++) {
      if (hasKey(i)) {
        valueArray[j++] = getValue(i);
      }
    }
    return valueArray;
  }

  public IntPair[] getAsPairs() {
    IntPair[] pairs = new IntPair[keyCount];
    int c = 0;
    for (int i = 0; i < entries.length; i++) {
      if (hasKey(i)) {
        pairs[c++] = new IntPair(getKey(i), getValue(i));
      }
    }
    return pairs;
  }

  /**
   * Resize backing arrays. If there are no removed keys, doubles the capacity.
   */
  void expand() {
    int capacity = newCapacity();
    IntIntMap h = new IntIntMap(capacity);
    for (int i = 0; i < entries.length; i++) {
      if (hasKey(i)) {
        h.putSafe(getKey(i), getValue(i));
      }
    }
    this.entries = h.entries;
    this.removedKeyCount = 0;
    this.threshold = h.threshold;
  }
}