package zemberek.core.collections;

public interface IntIntMapBase {

  int NO_RESULT = Integer.MIN_VALUE;
  int DEFAULT_INITIAL_CAPACITY = 4;
  // Capacity of the map is expanded when size reaches to capacity * LOAD_FACTOR.
  float LOAD_FACTOR = 0.6f;
  // Special value to mark empty cells.
  int EMPTY = NO_RESULT;
  int DELETED = EMPTY + 1;

  default int rehash(int hash) {
    // 0x9E3779B9 is int phi, it has some nice distributing characteristics.
    final int h = hash * 0x9E3779B9;
    return h ^ (h >> 16);
  }

  default int nearestPowerOf2Capacity(int capacity, int maxCapacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("Capacity must be > 0: " + capacity);
    }
    long k = 1;
    while (k <= capacity) {
      k <<= 1;
    }
    if (k > maxCapacity) {
      throw new IllegalArgumentException("Map too large: " + capacity);
    }
    return (int) k;
  }

  default void checkKey(int key) {
    if (key <= DELETED) {
      throw new IllegalArgumentException("Illegal key: " + key);
    }
  }

  void put(int key, int value);

  int capacity();

  int size();

  void remove(int key);

  void increment(int key, int value);

  int get(int key);

  int[] getKeys();

  int[] getValues();
}
