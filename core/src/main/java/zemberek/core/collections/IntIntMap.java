package zemberek.core.collections;

import java.util.Arrays;

/**
 * A simple hashmap with integer keys integer values.
 * implements open address linear probing algorithm.
 * <p>
 * Constraints:
 * - Supports int key values in range (Integer.MIN_VALUE..Integer.MAX_VALUE];
 * - Does not implement Map interface
 * - Capacity can be max 1 << 28
 * - Does not implement Iterable.
 * - Class is not thread safe.
 */
public final class IntIntMap {

	private static final int DEFAULT_INITIAL_CAPACITY = 4;
	/**
	 * Capacity of the map is expanded when size reaches to
	 * capacity * LOAD_FACTOR. This value is selected to fit
	 * max 5 elements to 8 and 10 elements to a 16 sized map.
	 */
	private static final float LOAD_FACTOR = 0.65f;

	private static final int MAX_CAPACITY = 1 << 28;

	// Special value to mark empty cells.
	private static final int EMPTY = Integer.MIN_VALUE;
	public static final int NO_RESULT = Integer.MIN_VALUE;

	// Backing array for keys and values.
	private int[] entries;

	// Number of keys in the map = size of the map.
	private int keyCount;

	// When size reaches a threshold, backing arrays are expanded.
	private int threshold;

	/**
	 * Map capacity is always a power of 2. With this property,
	 * integer modulo operation (key % capacity) can be replaced with
	 * (key & (capacity - 1))
	 * We keep (capacity - 1) value in this variable.
	 */
	private int modulo;
	private int modulo2;

	public IntIntMap() {
		this(DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * @param capacity initial internal array size. It must be a positive
	 * number. If value is not a power of two, size will be the nearest
	 * larger power of two.
	 */
	@SuppressWarnings("unchecked")
	public IntIntMap(int capacity) {
		// Backing array has always size = capacity * 2
		capacity = adjustInitialCapacity(capacity) ;
		entries = new int[capacity * 2];
		Arrays.fill(entries, EMPTY);
		modulo = capacity - 1;
		modulo2 = capacity * 2 -1;
		threshold = (int)(capacity * LOAD_FACTOR);
	}

	private int rehash(int hash) {
  	// 0x9E3779B9 is int phi, it has some nice distributing characteristics.
		final int h = hash * 0x9E3779B9;
		return h ^ (h >> 16);
	}

	private int adjustInitialCapacity(int capacity) {
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
		if (key == EMPTY) {
			throw new IllegalArgumentException("Illegal key: " + key);
		}
	}

	public void put(int key, int value) {
		checkKey(key);
		if (keyCount > threshold) {
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

	/**
	 * @return The value {@code T} that is mapped to given {@code key}.
	 * or {@code Integer.MIN_VALUE} If key does not exist,
	 *
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
		}
	}

	public boolean containsKey(int key) {
		return locate(key) >= 0;
	}

	/**
	 * @return The array of keys in the map.
	 */
	public int[] getKeys() {
		int[] keyArray = new int[keyCount];
		int c = 0;
		for (int i=0; i<entries.length ; i+=2) {
			if (entries[i] != EMPTY) {
				keyArray[c++] = entries[i];
			}
		}
		return keyArray;
	}

	/**
	 * @return The array of keys in the map.
	 */
	public int[] getValues() {
		int[] valueArray = new int[keyCount];
		for (int i=0, j=0; i<entries.length ; i+=2) {
			if (entries[i] != EMPTY) {
				valueArray[j++] = entries[i+1];
			}
		}
  	return valueArray;
	}

	private int locate(int key) {
		// key location is the hash
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
			slot = (slot + 2) & modulo2;
		}
	}

  private int newCapacity() {
		int newCapacity = entries.length;
		if (newCapacity > MAX_CAPACITY) {
			throw new RuntimeException("Map size is too large.");
		}
		return newCapacity;
	}

	/**
	 * Expands backing arrays by doubling their capacity.
	 */
  private void expand() {
		int capacity = newCapacity();
		IntIntMap h = new IntIntMap(capacity);
		for (int i = 0; i < entries.length; i+=2) {
			if (entries[i] != EMPTY) {
				h.put(entries[i], entries[i+1]);
			}
		}
		this.entries = h.entries;
		this.threshold = h.threshold;
		this.modulo = h.modulo;
		this.modulo2 = h.modulo2;
	}
}