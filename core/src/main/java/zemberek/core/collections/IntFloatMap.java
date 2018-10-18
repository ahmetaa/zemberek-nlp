package zemberek.core.collections;

import java.util.Arrays;

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
public final class IntFloatMap {

  public static int NO_RESULT = Integer.MIN_VALUE;
  private static int DEFAULT_INITIAL_CAPACITY = 4;
  // Special values to mark empty and deleted cells.
  private static int EMPTY = NO_RESULT;
  private static int DELETED = EMPTY + 1;

  private static int MAX_CAPACITY = 1 << 30;
  // Backing array for keys and values. Each 64 bit slot is used for storing
  // 32 bit key, value pairs.
  private long[] entries;
  // Number of keys in the map = size of the map.
  private int keyCount;
  // Number of Removed keys.
  private int removedKeyCount;

  public IntFloatMap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * @param capacity initial internal array size for capacity amount of key - values. It must be a
   * positive number. If value is not a power of two, size will be the nearest larger power of two.
   */
  public IntFloatMap(int capacity) {
    capacity = nearestPowerOf2Capacity(capacity, MAX_CAPACITY);
    entries = new long[capacity];
    Arrays.fill(entries, EMPTY);
  }

  private int rehash(int hash) {
    // 0x9E3779B9 is int phi, it has some nice distributing characteristics.
    final int h = hash * 0x9E3779B9;
    return h ^ (h >> 16);
  }

  private int nearestPowerOf2Capacity(int capacity, int maxCapacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("Capacity must be > 0: " + capacity);
    }
    long k = 1;
    while (k < capacity) {
      k <<= 1;
    }
    if (k > maxCapacity) {
      throw new IllegalArgumentException("Map too large: " + capacity);
    }
    return (int) k;
  }

  private void checkKey(int key) {
    if (key <= DELETED) {
      throw new IllegalArgumentException("Illegal key: " + key);
    }
  }

  public int capacity() {
    return entries.length;
  }

  public int size() {
    return keyCount;
  }

  private void setKey(int i, int key) {
    entries[i] = (entries[i] & 0xFFFF_FFFF_0000_0000L) | key;
  }

  private int getKey(int i) {
    return (int) (entries[i] & 0xFFFF_FFFFL);
  }

  private void setValue(int i, float value) {
    entries[i] = (entries[i] & 0x0000_0000_FFFF_FFFFL) | (((long) Float.floatToIntBits(value)) << 32);
  }

  private void setKeyValue(int i, int key, float value) {
    entries[i] = (key & 0xFFFF_FFFFL) | (((long) Float.floatToIntBits(value)) << 32);
  }

  private float getValue(int i) {
    return Float.intBitsToFloat((int)(entries[i] >>> 32));
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

  private void expandIfNecessary() {
    if (keyCount + removedKeyCount > entries.length >> 1) {
      expand();
    }
  }

  /**
   * Map capacity is always a power of 2. With this property, integer modulo operation (key %
   * capacity) can be replaced with (key & (capacity - 1)).
   */
  private int firstProbe(int key) {
    return rehash(key) & (entries.length - 1);
  }

  private int probe(int slot) {
    return (slot + 1) & (entries.length - 1);
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

  // Only marks the slot as DELETED. In get and locate methods, deleted slots are skipped.
  public void remove(int key) {
    checkKey(key);
    int loc = locate(key);
    if (loc >= 0) {
      setKey(loc, DELETED);
      removedKeyCount++;
      keyCount--;
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
        return  Float.intBitsToFloat((int)(entry >>> 32));
      }
      if (t == EMPTY) {
        return NO_RESULT;
      }
      slot = probe(slot);
      // DELETED slots are skipped.
    }
  }

  public boolean containsKey(int key) {
    return locate(key) >= 0;
  }

  private boolean hasKey(int i) {
    return getKey(i) > DELETED;
  }

  /**
   * @return The array of keys in the map. Not ordered.
   */
  public int[] getKeys() {
    int[] keyArray = new int[keyCount];
    int c = 0;
    for (int i = 0; i < entries.length; i++) {
      if (hasKey(i)) {
        keyArray[c++] = getKey(i);
      }
    }
    return keyArray;
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

  private int locate(int key) {
    int slot = firstProbe(key);
    while (true) {
      final int k = getKey(slot);
      // If slot is empty, return its location
      // return -slot -1 to tell that slot is empty, -1 is for slot = 0.
      if (k == EMPTY) {
        return -slot - 1;
      }
      if (k == key) {
        return slot;
      }
      // DELETED slots are ignored.
      slot = probe(slot);
    }
  }

  private int newCapacity() {
    int newCapacity = nearestPowerOf2Capacity(keyCount, MAX_CAPACITY) * 2;
    if (newCapacity > MAX_CAPACITY) {
      throw new RuntimeException("Map size is too large.");
    }
    return newCapacity;
  }

  /**
   * Resize backing arrays. If there are no removed keys, doubles the capacity.
   */
  private void expand() {
    int capacity = newCapacity();
    IntFloatMap h = new IntFloatMap(capacity);
    for (int i = 0; i < entries.length; i++) {
      if (hasKey(i)) {
        h.putSafe(getKey(i), getValue(i));
      }
    }
    this.entries = h.entries;
    this.removedKeyCount = 0;
  }
}