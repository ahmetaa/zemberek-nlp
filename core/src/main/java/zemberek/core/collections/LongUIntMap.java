package zemberek.core.collections;

import java.util.Arrays;

public class LongUIntMap {

    static final int INITIAL_SIZE = 8;
    static final double DEFAULT_LOAD_FACTOR = 0.5;

    // This is the size-1 of the key and value array length. Array length is a value power of two
    private int modulo = INITIAL_SIZE - 1;

    public static final int EMPTY_VALUE = -1;
    public static final int DELETED_VALUE = -2;
    // Key array.
    long[] keys;

    // Carries unsigned int values.
    int[] values;

    int keyCount;
    int removeCount;

    // When structure has this amount of keys, it expands the key and count arrays.
    int threshold = (int) (INITIAL_SIZE * DEFAULT_LOAD_FACTOR);

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
        Arrays.fill(values, -1);
        threshold = (int) (k * DEFAULT_LOAD_FACTOR);
        modulo = k - 1;
    }

    private int firstProbe(int hashCode) {
        return hashCode & modulo;
    }

    private int nextProbe(int previousIndex, int probeCount) {
        return (previousIndex + probeCount) & modulo;
    }

    private int hash(long key) {
        return Long.hashCode(key);
    }

    private int locate(long key) {
        int probeCount = 0;
        int firstProbe = firstProbe(hash(key));
        int slot = firstProbe;
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
                slot = nextProbe(firstProbe, ++probeCount);
                continue;
            }
            if (key == keys[slot]) {
                return slot;
            }
            slot = nextProbe(firstProbe, ++probeCount);
        }
    }

    /**
     * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value by 1.
     *
     * @param key key
     * @return the new count value after addOrIncrement
     */
    public int increment(long key) {
        return incrementByAmount(key, 1);
    }

    /**
     * Returns the count of the key. If key does not exist, returns 0.
     *
     * @param key key
     * @return count of the key
     */
    public int get(long key) {
        int probeCount = 0;
        int firstProbe = firstProbe(hash(key));
        int slot = firstProbe;
        while (true) {
            final int t = values[slot];
            if (t == EMPTY_VALUE) {
                return -1;
            }
            if (t == DELETED_VALUE) {
                slot = nextProbe(firstProbe, ++probeCount);
                continue;
            }
            if (keys[slot] == key)
                return values[slot];
            slot = nextProbe(firstProbe, ++probeCount);
        }
    }

    public int decrement(long key) {
        return incrementByAmount(key, -1);
    }

    public boolean containsKey(long key) {
        return locate(key) >= 0;
    }

    /**
     * addOrIncrement the value by "amount". If value does not exist, it a applies set() operation.
     *
     * @param key    key
     * @param amount amount to addOrIncrement
     * @return incremented value
     */
    public int incrementByAmount(long key, int amount) {
        if (keyCount + removeCount == threshold) {
            expand();
        }
        int l = locate(key);
        if (l < 0) {
            l = -l - 1;
            values[l] = amount;
            keys[l] = key;
            keyCount++;
            return values[l];
        } else {
            values[l] += amount;
            if (values[l] < 0)
                throw new IllegalStateException("Negative Value calculated after incrementing with " + amount);
            return values[l];
        }
    }

    public void remove(long key) {
        int k = locate(key);
        if (k < 0)
            return;
        values[k] = DELETED_VALUE; // mark deletion
        keyCount--;
        removeCount++;
    }

    private void expand() {
        LongUIntMap h = new LongUIntMap(values.length * 2);
        for (int i = 0; i < keys.length; i++) {
            if (values[i] != EMPTY_VALUE && values[i] != DELETED_VALUE)
                h.put(keys[i], values[i]);
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
        if (keyCount + removeCount == threshold) {
            expand();
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
    }

    /**
     * @return amount of keys
     */
    public int size() {
        return keyCount;
    }

    /**
     * @return a clone of value array.
     */
    public int[] copyOfValues() {
        return values.clone();
    }

    public long[] keyArray() {
        long[] keys = new long[size()];
        int j = 0;
        for (int i = 0; i < keys.length; i++) {
            if (values[i] != EMPTY_VALUE || values[i] != DELETED_VALUE) {
                keys[j++] = keys[i];
            }
        }
        return keys;
    }
}
