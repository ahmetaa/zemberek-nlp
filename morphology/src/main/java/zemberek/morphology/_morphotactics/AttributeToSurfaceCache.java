package zemberek.morphology._morphotactics;

import java.util.Arrays;

/**
 * A cache for set of morphemic attributes to surface forms.
 *
 * For thread safety, writes to map are synchronized, when map needs expanding
 * writer thread creates the expanded version and replaces the map. The map
 * reference is volatile so readers always see a consistent albeit possibly
 * stale version of the map.
 *
 * This is also knows as cheap read-write lock trick (see item #5 in the link)
 * https://www.ibm.com/developerworks/java/library/j-jtp06197/index.html
 */
public class AttributeToSurfaceCache {
  private static final int DEFAULT_INITIAL_CAPACITY = 4;

  // volatile guarantees atomic reference copy.
  private volatile AttributeToSurfaceMap attributeMap;

  public AttributeToSurfaceCache() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  public AttributeToSurfaceCache(int initialCapacity) {
    attributeMap = new AttributeToSurfaceMap(initialCapacity);
  }

  public synchronized void addSurface(int attributes, String surface) {
    while (!attributeMap.put(attributes, surface)) {
      attributeMap = attributeMap.expand();
    }
  }

  public String getSurface(int attributes) {
    // Obtain a reference to current edge map. Even if the edgeMap instance is changed
    // by a writer thread, we can still read a consistent version of the map.
    AttributeToSurfaceMap map = attributeMap;
    return map.get(attributes);
  }

  /**
   * A simple map with int keys.
   * This map is designed to be used by a thread safe caller class with Copy on Write semantics.
   * If put operation fails (not enough space in the map for efficient gets), it just returns false
   * it is callers responsibility to create a new Map with expanded size and replace the old one.
   *
   * TODO: Move this version to collections as well.
   */
  private static final class AttributeToSurfaceMap {

    // Special value to mark empty cells.
    private static final int EMPTY = Integer.MIN_VALUE;

    private static final int CAPACITY_LIMIT = 1 << 29;
    /**
     * Capacity of the map is expanded when size reaches to
     * capacity * LOAD_FACTOR.
     */
    private static final float LOAD_FACTOR = 0.60f;

    /**
     * Map capacity is always a power of 2. With this property,
     * integer modulo operation (key % capacity) can be replaced with
     * (key & (capacity - 1)). We keep (capacity - 1) value in this variable.
     */
    private int modulo;

    // Backing arrays for keys and value references.
    protected int[] keys;
    protected String[] values;

    // Number of keys in the map = size of the map.
    private int keyCount;

    // When size reaches a threshold, backing arrays are expanded.
    private int threshold;

    AttributeToSurfaceMap() {
      this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * @param capacity initial internal array size. It must be a positive number. If value is not a
     *                 power of two, size will be the nearest larger power of two.
     */
    @SuppressWarnings("unchecked")
    AttributeToSurfaceMap(int capacity) {
      capacity = adjustInitialCapacity(capacity);
      keys = new int[capacity];
      values = new String[keys.length];
      Arrays.fill(keys, EMPTY);
      modulo = keys.length - 1;
      threshold = (int) (capacity * LOAD_FACTOR);
    }

    private int adjustInitialCapacity(int initialCapacity) {
      if (initialCapacity < 1) {
        throw new IllegalArgumentException("Capacity must be > 0: " + initialCapacity);
      }
      long k = 2;
      while (k < initialCapacity) {
        k <<= 1;
      }
      if (k > CAPACITY_LIMIT) {
        throw new IllegalArgumentException("Size too large: " + initialCapacity);
      }
      return (int) k;
    }

    public int capacity() {
      return keys.length;
    }

    public int size() {
      return keyCount;
    }

    private void checkKey(int key) {
      if (key == EMPTY) {
        throw new IllegalArgumentException("Illegal key: " + key);
      }
    }

    public boolean put(int key, String value) {
      checkKey(key);
      if (keyCount == threshold) {
        // Caller should create a new version with expanded capacity.
        return false;
      }
      int loc = locate(key);
      if (loc >= 0) {
        values[loc] = value;
      } else {
        loc = -loc - 1;
        keys[loc] = key;
        values[loc] = value;
        keyCount++;
      }
      return true;
    }

    /**
     * @return The value {@code T} that is mapped to given {@code key}. or {@code null} If key does
     * not exist.
     * @throws IllegalArgumentException if key is {@code Integer.MIN_INT}
     */
    public String get(int key) {
      checkKey(key);
      int slot = key & modulo;
      if (key == keys[slot]) {
        return values[slot];
      }
      if (key == EMPTY) {
        return null;
      }
      // Apply linear probing.
      while (true) {
        slot = (slot + 1) & modulo;
        final int t = keys[slot];
        if (t == key) {
          return values[slot];
        }
        if (t == EMPTY) {
          return null;
        }
      }
    }

    public boolean containsKey(int key) {
      return locate(key) >= 0;
    }

    private int locate(int key) {
      int slot = key & modulo;
      while (true) {
        int k = keys[slot];
        // If slot is empty, return its location
        if (k == EMPTY) {
          return -slot - 1;
        }
        if (k == key) {
          return slot;
        }
        slot = (slot + 1) & modulo;
      }
    }

    private int newCapacity() {
      long size = (long) (keys.length * 2);
      if (keys.length > CAPACITY_LIMIT) {
        throw new RuntimeException("Map size is too large.");
      }
      return (int) size;
    }

    /**
     * Returns an new expanded (double capcacity) map with content of this map.
     */
    AttributeToSurfaceMap expand() {
      int capacity = newCapacity();
      AttributeToSurfaceMap newMap = new AttributeToSurfaceMap(capacity);
      for (int i = 0; i < keys.length; i++) {
        if (keys[i] != EMPTY) {
          newMap.put(keys[i], values[i]);
        }
      }
      return newMap;
    }

    /**
     * @return The array of keys in the map. Sorted ascending.
     */
    public int[] getKeys() {
      int[] keyArray = new int[keyCount];
      int c = 0;
      for (int key : keys) {
        if (key != EMPTY) {
          keyArray[c++] = key;
        }
      }
      Arrays.sort(keyArray);
      return keyArray;
    }

  }
}
