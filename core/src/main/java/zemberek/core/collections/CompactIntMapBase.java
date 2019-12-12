package zemberek.core.collections;

import java.util.Arrays;

public abstract class CompactIntMapBase {

  public static int NO_RESULT = Integer.MIN_VALUE;
  static int DEFAULT_INITIAL_CAPACITY = 4;
  // Special values to mark empty and deleted cells.
  static int EMPTY = NO_RESULT;
  static int DELETED = EMPTY + 1;

  static int MAX_CAPACITY = 1 << 30;
  // Backing array for keys and values. Each 64 bit slot is used for storing
  // 32 bit key, value pairs.
  long[] entries;
  // Number of keys in the map = size of the map.
  int keyCount;
  // Number of Removed keys.
  int removedKeyCount;
  int threshold;

  /**
   * @param capacity initial internal array size for capacity amount of key - values. It must be a
   * positive number. If value is not a power of two, size will be the nearest larger power of two.
   */
  CompactIntMapBase(int capacity) {
    capacity = nearestPowerOf2Capacity(capacity, MAX_CAPACITY);
    entries = new long[capacity];
    Arrays.fill(entries, EMPTY);
    threshold = (int) (capacity * calculateLoadFactor(capacity));
  }

  static int nearestPowerOf2Capacity(int capacity, int maxCapacity) {
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
    entries[i] = (entries[i] & 0xFFFF_FFFF_0000_0000L) | key;
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

  // This method is only used during expansion of the map. New capacity is calculated as
  // old capacity * 2
  int newCapacity() {
    int newCapacity = nearestPowerOf2Capacity(capacity(), MAX_CAPACITY) * 2;
    if (newCapacity > MAX_CAPACITY) {
      throw new RuntimeException("Map size is too large.");
    }
    return newCapacity;
  }

  final int locate(int key) {
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
