package zemberek.core.collections;

import java.util.Arrays;
import zemberek.core.IntPair;

/**
 * A simple hashmap with integer keys and integer values. Implements open address linear probing
 * algorithm. Constraints: <pre>
 * - Supports int key values in range (Integer.MIN_VALUE+1..Integer.MAX_VALUE];
 * - Does not implement Map interface
 * - Capacity can be max 1 << 29
 * - Max size is capacity * LOAD_FACTOR (~322M elements for 0.6 load factor)
 * - Does not implement Iterable.
 * - Class is not thread safe.
 * </pre>
 */
public final class IntIntMap {

  public static final int NO_RESULT = Integer.MIN_VALUE;
  private static final int DEFAULT_INITIAL_CAPACITY = 4;
  // Capacity of the map is expanded when size reaches to capacity * LOAD_FACTOR.
  private static final float LOAD_FACTOR = 0.6f;
  private static final int MAX_CAPACITY = 1 << 29;
  // Special value to mark empty cells.
  private static final int EMPTY = NO_RESULT;
  private static final int DELETED = EMPTY + 1;
  // Backing array for keys and values. Key and value pairs are put next to each other instead of 2
  // separate arrays for better caching behavior.
  private int[] entries;
  // Number of keys in the map = size of the map.
  private int keyCount;
  // Number of Removed keys.
  private int removedKeyCount;
  // When size reaches a threshold, backing arrays are expanded.
  private int threshold;

  /**
   * Map capacity is always a power of 2. With this property, integer modulo operation (key %
   * capacity) can be replaced with (key & (capacity - 1)). We keep (capacity - 1) value in this
   * variable. Because keys and values are always put next to each other, we keep
   * the (capacity * 2 - 1) in a separate variable. First modulo is used to track logical array
   * of key-value pairs second one operates on the whole array.
   * <p>First modulo is used to find the index of the key-value pair given a hash value with:
   * <p>
   * slot = (hash(key) & modulo) << 1;  equivalent to  slot = (hash(key) % capacity) / 2
   * <p>
   * second one is used for linearly walking on the array when moving to next pair:
   * <p>
   * next_slot = (slot + 2) & modulo2;  equivalent to next_slot = (slot + 2) % size
   * <p>
   */
  private int modulo;
  private int modulo2;

  public IntIntMap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * @param capacity initial internal array size for capacity amount of key - values.
   * It must be a positive number. If value is not a power of two, size will be the nearest
   * larger power of two.
   */
  @SuppressWarnings("unchecked")
  public IntIntMap(int capacity) {
    // Backing array has always size = capacity * 2
    capacity = nearestPowerOf2Capacity(capacity);
    // key value pairs are kept next to each other
    entries = new int[capacity * 2];
    Arrays.fill(entries, EMPTY);
    modulo = capacity - 1;
    modulo2 = capacity * 2 - 1;
    threshold = (int) (capacity * LOAD_FACTOR);
  }

  private int rehash(int hash) {
    // 0x9E3779B9 is int phi, it has some nice distributing characteristics.
    final int h = hash * 0x9E3779B9;
    return h ^ (h >> 16);
  }

  private int nearestPowerOf2Capacity(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("Capacity must be > 0: " + capacity);
    }
    long k = 1;
    while (k <= capacity) {
      k <<= 1;
    }
    if (k > MAX_CAPACITY) {
      throw new IllegalArgumentException("Map too large: " + capacity);
    }
    return (int) k;
  }

  public int capacity() {
    return entries.length >> 1;
  }

  public int size() {
    return keyCount;
  }

  private void checkKey(int key) {
    if (key <= DELETED) {
      throw new IllegalArgumentException("Illegal key: " + key);
    }
  }

  public void put(int key, int value) {
    checkKey(key);
    if (keyCount + removedKeyCount > threshold) {
      expand();
    }
    int loc = locate(key);
    if (loc >= 0) {
      entries[loc + 1] = value;
    } else {
      loc = -loc - 1;
      entries[loc] = key;
      entries[loc + 1] = value;
      keyCount++;
    }
  }

  // Only marks the slot as DELETED. In get and locate methods, deleted slots are skipped.
  // TODO: Maybe add compaction method for maps with a lot of deleted keys.
  public void remove(int key) {
    checkKey(key);
    int loc = locate(key);
    if (loc >= 0) {
      entries[loc] = DELETED;
      removedKeyCount++;
      keyCount--;
    }
  }

  public void increment(int key, int value) {
    checkKey(key);
    if (keyCount > threshold) {
      expand();
    }
    int loc = locate(key);
    if (loc >= 0) {
      entries[loc + 1] += value;
    } else {
      loc = -loc - 1;
      entries[loc] = key;
      entries[loc + 1] = value;
      keyCount++;
    }
  }

  /**
   * @return The value {@code T} that is mapped to given {@code key}. or {@code NO_RESULT}
   * If key does not exist,
   * @throws IllegalArgumentException if key is {@code Integer.MIN_VALUE}
   */
  public int get(int key) {
    checkKey(key);
    int slot = (rehash(key) & modulo) << 1;
    // Test the lucky first shot.
    if (key == entries[slot]) {
      return entries[slot + 1];
    }
    // Continue linear probing otherwise
    while (true) {
      slot = (slot + 2) & modulo2;
      final int t = entries[slot];
      if (t == key) {
        return entries[slot + 1];
      }
      if (t == EMPTY) {
        return NO_RESULT;
      }
      // DELETED slots are skipped.
    }
  }

  public boolean containsKey(int key) {
    return locate(key) >= 0;
  }

  private boolean hasKey(int i) {
    return entries[i] > DELETED;
  }

  /**
   * @return The array of keys in the map. Not ordered.
   */
  public int[] getKeys() {
    int[] keyArray = new int[keyCount];
    int c = 0;
    for (int i = 0; i < entries.length; i += 2) {
      if (hasKey(i)) {
        keyArray[c++] = entries[i];
      }
    }
    return keyArray;
  }

  /**
   * @return The array of values in the map. Not ordered.
   */
  public int[] getValues() {
    int[] valueArray = new int[keyCount];
    for (int i = 0, j = 0; i < entries.length; i += 2) {
      if (entries[i] != EMPTY) {
        valueArray[j++] = entries[i + 1];
      }
    }
    return valueArray;
  }

  public IntPair[] getAsPairs() {
    IntPair[] pairs = new IntPair[keyCount];
    int c = 0;
    for (int i = 0; i < entries.length; i += 2) {
      if (hasKey(i)) {
        pairs[c++] = new IntPair(entries[i], entries[i + 1]);
      }
    }
    return pairs;
  }

  private int locate(int key) {
    int slot = (rehash(key) & modulo) << 1;
    while (true) {
      final int k = entries[slot];
      // If slot is empty, return its location
      // return -slot -1 to tell that slot is empty, -1 is for slot = 0.
      if (k == EMPTY) {
        return -slot - 1;
      }
      if (k == key) {
        return slot;
      }
      // DELETED slots are ignored.
      slot = (slot + 2) & modulo2;
    }
  }

  private int newCapacity() {
    int newCapacity = nearestPowerOf2Capacity(keyCount) * 2;
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
    IntIntMap h = new IntIntMap(capacity);
    for (int i = 0; i < entries.length; i += 2) {
      if (hasKey(i)) {
        h.put(entries[i], entries[i + 1]);
      }
    }
    this.entries = h.entries;
    this.threshold = h.threshold;
    this.modulo = h.modulo;
    this.modulo2 = h.modulo2;
  }
}