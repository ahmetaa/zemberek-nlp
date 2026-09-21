package zemberek.core.collections;

import java.util.Arrays;

public abstract class CompactIntMapBase {

  public static final int NO_RESULT = Integer.MIN_VALUE;
  static final int DEFAULT_INITIAL_CAPACITY = 4;
  // Special values to mark empty and deleted cells.
  static final int EMPTY = NO_RESULT;
  static final int DELETED = EMPTY + 1;

  static final int MAX_CAPACITY = 1 << 30;
  // Backing array for keys and values. Each 64 bit slot is used for storing
  // 32 bit key, value pairs.
  long[] entries;
  // Number of keys in the map = size of the map.
  int keyCount;
  // Number of Removed keys.
  int removedKeyCount;
  int threshold;

  /**
   * @param capacity initial internal array size for capacity amount of key - values. It can not
   * be negative, but it can be zero. If value is not a power of two, size will be the nearest
   * larger power of two.
   */
  CompactIntMapBase(int capacity) {
    capacity = nearestPowerOf2Capacity(capacity, MAX_CAPACITY);
    entries = new long[capacity];
    Arrays.fill(entries, EMPTY);
    threshold = (int) (capacity * calculateLoadFactor(capacity));
  }

  static int nearestPowerOf2Capacity(int capacity, int maxCapacity) {
    if (capacity < 0) {
      throw new IllegalArgumentException("Capacity can not be negative: " + capacity);
    }
    // Tables smaller than the default are degenerate: their threshold rounds down to 0.
    long k = DEFAULT_INITIAL_CAPACITY;
    while (k < capacity) {
      k <<= 1;
    }
    if (k > maxCapacity) {
      throw new IllegalArgumentException("Map too large: " + capacity);
    }
    return (int) k;
  }

  static float calculateLoadFactor(int capacity) {
    // Note: Never return 1.0 as load factor. Backing array should have
    // at least one empty slot.
    if (capacity <= 4) {
      return 0.9f;
    } else if (capacity <= 16) {
      return 0.75f;
    } else if (capacity <= 128) {
      return 0.70f;
    } else if (capacity <= 512) {
      return 0.65f;
    } else if (capacity <= 2048) {
      return 0.60f;
    } else {
      return 0.5f;
    }
  }

  public int capacity() {
    return entries.length;
  }

  public int size() {
    return keyCount;
  }

  /**
   * Map capacity is always a power of 2. With this property, integer modulo operation (key %
   * capacity) can be replaced with (key & (capacity - 1)).
   */
  int firstProbe(int key) {
    return rehash(key) & (entries.length - 1);
  }

  int probe(int slot) {
    return (slot + 1) & (entries.length - 1);
  }

  int rehash(int hash) {
    // 0x9E3779B9 is int phi, it has some nice distributing characteristics.
    final int h = hash * 0x9E3779B9;
    return h ^ (h >> 16);
  }

  final void checkKey(int key) {
    if (key <= DELETED) {
      throw new IllegalArgumentException("Illegal key: " + key);
    }
  }

  final int getKey(int i) {
    return (int) (entries[i] & 0xFFFF_FFFFL);
  }

  final void setKey(int i, int key) {
    // key must be masked, otherwise a negative key sign extends over the value half.
    entries[i] = (entries[i] & 0xFFFF_FFFF_0000_0000L) | (key & 0xFFFF_FFFFL);
  }

  public boolean containsKey(int key) {
    return locate(key) >= 0;
  }

  public boolean hasKey(int i) {
    return getKey(i) > DELETED;
  }

  void expandIfNecessary() {
    if (keyCount + removedKeyCount >= threshold) {
      expand();
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

  /**
   * Capacity is derived from the number of live keys rather than from the current capacity, so a
   * table whose slots are mostly tombstones is rehashed at the same size or shrunk.
   *
   * @return the smallest power of two capacity whose threshold leaves room for the live keys, the
   * key that triggered the expansion, and at least one empty slot.
   */
  int newCapacity() {
    // Room for the live keys, the key that triggered the expansion, and an always empty slot.
    final long needed = keyCount + 1L;
    // When the expansion was triggered by tombstones rather than by growth, ask for twice that
    // much so a map under put/remove churn does not rehash on almost every removal.
    final long preferred = removedKeyCount > 0 ? keyCount * 2L + 1 : needed;
    long smallestFit = -1;
    long capacity = DEFAULT_INITIAL_CAPACITY;
    while (capacity <= MAX_CAPACITY) {
      long slack = (int) (capacity * calculateLoadFactor((int) capacity));
      if (smallestFit < 0 && needed < slack) {
        smallestFit = capacity;
      }
      if (preferred < slack) {
        return (int) capacity;
      }
      capacity <<= 1;
    }
    // The headroom does not fit but the keys themselves still may.
    if (smallestFit > 0) {
      return (int) smallestFit;
    }
    throw new IllegalStateException("Map size is too large.");
  }

  /**
   * Marks the slot returned by {@link #locate(int)} for a key that is about to be inserted and
   * updates the key counters. If the slot held a tombstone, the removed key count is given back.
   *
   * @param locateResult the negative value returned by {@link #locate(int)}
   * @return the slot index the caller must write the new key and value to.
   */
  final int claimSlot(int locateResult) {
    int slot = -locateResult - 1;
    if (getKey(slot) == DELETED) {
      removedKeyCount--;
    }
    keyCount++;
    return slot;
  }

  final int locate(int key) {
    int slot = firstProbe(key);
    // Index of the first tombstone seen on the probe path, if any. Reusing it keeps deleted
    // slots from accumulating until the next expansion.
    int firstDeleted = -1;
    while (true) {
      final int k = getKey(slot);
      // If slot is empty, return the insertion point: the first tombstone on the path, or this
      // slot. Return -slot -1 to tell the slot is free, -1 is for slot = 0.
      if (k == EMPTY) {
        return firstDeleted < 0 ? -slot - 1 : -firstDeleted - 1;
      }
      if (k == key) {
        return slot;
      }
      if (k == DELETED && firstDeleted < 0) {
        firstDeleted = slot;
      }
      slot = probe(slot);
    }
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

  abstract void expand();
}
