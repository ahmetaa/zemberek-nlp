package zemberek.core.collections;

import java.util.Arrays;

public class LongUIntMap {

  public static final int EMPTY_VALUE = -1;
  public static final int DELETED_VALUE = -2;
  static final int INITIAL_SIZE = 8;
  static final double DEFAULT_LOAD_FACTOR = 0.55;
  // Key array.
  long[] keys;
  // Carries unsigned int values.
  int[] values;
  int keyCount;
  int removeCount;
  // When structure has this amount of keys, it expands the key and count arrays.
  int threshold;
  // This is the size-1 of the key and value array length. Array length is a value power of two
  private int modulo;

  public LongUIntMap() {
    this(INITIAL_SIZE);
  }

  public LongUIntMap(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Size must be a positive value. But it is " + size);
    }
    int k = 1;
    while (k < size) {
      k <<= 1;
    }
    keys = new long[k];
    values = new int[k];
    Arrays.fill(values, EMPTY_VALUE);
    threshold = (int) (k * DEFAULT_LOAD_FACTOR);
    modulo = k - 1;
  }

  private int hash(long key) {
    long h = key * 0x9E3779B97F4A7C15L;
    return (int) (h ^ (h >>> 32));
  }

  private int locate(long key) {
    int slot = hash(key) & modulo;
    int pointer = -1;
    while (true) {
      final int t = values[slot];
      if (t == EMPTY_VALUE) {
        return pointer < 0 ? (-slot - 1) : (-pointer - 1);
      }
      if (t == DELETED_VALUE) {
        if (pointer < 0) {
          pointer = slot;
        }
        slot = (slot + 1) & modulo;
        continue;
      }
      if (key == keys[slot]) {
        return slot;
      }
      slot = (slot + 1) & modulo;
    }
  }

  /**
   * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value
   * by 1.
   *
   * @param key key
   * @return the new count value after increment
   */
  public int increment(long key) {
    return incrementByAmount(key, 1);
  }

  /**
   * Returns the value of the key. If key does not exist, returns -1.
   *
   * @param key key
   * @return value of the key, or -1 if key does not exist
   */
  public int get(long key) {
    int slot = hash(key) & modulo;

    while (true) {
      final int t = values[slot];
      if (t == EMPTY_VALUE) {
        return -1;
      }
      if (t == DELETED_VALUE) {
        slot = (slot + 1) & modulo;
        continue;
      }
      if (keys[slot] == key) {
        return values[slot];
      }
      slot = (slot + 1) & modulo;
    }
  }

  public int decrement(long key) {
    return incrementByAmount(key, -1);
  }

  public boolean containsKey(long key) {
    return locate(key) >= 0;
  }

  /**
   * Increment the value by "amount". If key does not exist, it inserts it with value "amount".
   *
   * @param key key
   * @param amount amount to increment
   * @return incremented value
   */
  public int incrementByAmount(long key, int amount) {
    int loc = locate(key);
    if (loc >= 0) {
      long newVal = (long) values[loc] + amount;
      if (newVal < 0 || newVal > Integer.MAX_VALUE) {
        throw new IllegalStateException(
            "Value out of bounds after incrementing with " + amount + ": " + newVal);
      }
      values[loc] = (int) newVal;
      return values[loc];
    }
    if (amount < 0) {
      throw new IllegalStateException(
          "Cannot decrement non-existent key: " + key);
    }
    if (keyCount + removeCount >= threshold) {
      expand();
      loc = locate(key);
    }
    loc = -loc - 1;
    if (values[loc] == DELETED_VALUE) {
      removeCount--;
    }
    values[loc] = amount;
    keys[loc] = key;
    keyCount++;
    return values[loc];
  }

  public void remove(long key) {
    int k = locate(key);
    if (k < 0) {
      return;
    }
    values[k] = DELETED_VALUE; // mark deletion
    keyCount--;
    removeCount++;
  }

  private int newCapacity() {
    long size = (long) values.length * 2L;
    if (size > (1 << 30)) {
      throw new IllegalStateException("Map size is too large.");
    }
    return (int) size;
  }

  private void expand() {
    LongUIntMap h = new LongUIntMap(newCapacity());
    for (int i = 0; i < keys.length; i++) {
      if (values[i] != EMPTY_VALUE && values[i] != DELETED_VALUE) {
        h.put(keys[i], values[i]);
      }
    }
    assert (h.keyCount == keyCount);
    this.values = h.values;
    this.keys = h.keys;
    this.keyCount = h.keyCount;
    this.modulo = h.modulo;
    this.threshold = h.threshold;
    this.removeCount = 0;
  }

  public void put(long key, int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Cannot put negative value = " + value);
    }
    int loc = locate(key);
    if (loc >= 0) {
      values[loc] = value;
      return;
    }
    if (keyCount + removeCount >= threshold) {
      expand();
      loc = locate(key);
    }
    loc = -loc - 1;
    if (values[loc] == DELETED_VALUE) {
      removeCount--;
    }
    keys[loc] = key;
    values[loc] = value;
    keyCount++;
  }

  /**
   * @return amount of keys
   */
  public int size() {
    return keyCount;
  }

  public int capacity() {
    return values.length;
  }

  /**
   * @return a copy of active values.
   */
  public int[] copyOfValues() {
    int[] result = new int[keyCount];
    int j = 0;
    for (int i = 0; i < values.length; i++) {
      if (values[i] != EMPTY_VALUE && values[i] != DELETED_VALUE) {
        result[j++] = values[i];
      }
    }
    return result;
  }

  /**
   * @return an array containing all active keys in the map.
   */
  public long[] keyArray() {
    long[] result = new long[keyCount];
    int j = 0;
    for (int i = 0; i < keys.length; i++) {
      if (values[i] != EMPTY_VALUE && values[i] != DELETED_VALUE) {
        result[j++] = keys[i];
      }
    }
    return result;
  }
}
