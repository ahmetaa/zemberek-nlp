package zemberek.core.collections;

/**
 * A simple hashmap with integer keys and float values. Implements open address linear probing
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
public final class IntFloatMap extends CompactIntMapBase {

  public IntFloatMap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * @param capacity initial internal array size for capacity amount of key - values. It must be a
   * positive number. If value is not a power of two, size will be the nearest larger power of two.
   */
  public IntFloatMap(int capacity) {
    super(capacity);
  }

  private void setValue(int i, float value) {
    entries[i] =
        (entries[i] & 0x0000_0000_FFFF_FFFFL) | (((long) Float.floatToIntBits(value)) << 32);
  }

  private void setKeyValue(int i, int key, float value) {
    entries[i] = (key & 0xFFFF_FFFFL) | (((long) Float.floatToIntBits(value)) << 32);
  }

  private float getValue(int i) {
    return Float.intBitsToFloat((int) (entries[i] >>> 32));
  }

  public void put(int key, float value) {
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
  private void putSafe(int key, float value) {
    int loc = firstProbe(key);
    while (true) {
      if (getKey(loc) == EMPTY) {
        setKeyValue(loc, key, value);
        return;
      }
      loc = probe(loc);
    }
  }

  public void increment(int key, float value) {
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
  public float get(int key) {
    checkKey(key);
    int slot = firstProbe(key);
    while (true) {
      final long entry = entries[slot];
      final int t = (int) (entry & 0xFFFF_FFFFL);
      if (t == key) {
        return Float.intBitsToFloat((int) (entry >>> 32));
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
  public float[] getValues() {
    float[] valueArray = new float[keyCount];
    for (int i = 0, j = 0; i < entries.length; i++) {
      if (hasKey(i)) {
        valueArray[j++] = getValue(i);
      }
    }
    return valueArray;
  }

  /**
   * Resize backing arrays. If there are no removed keys, doubles the capacity.
   */
  void expand() {
    int capacity = newCapacity();
    IntFloatMap h = new IntFloatMap(capacity);
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