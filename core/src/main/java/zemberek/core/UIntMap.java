package zemberek.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UIntMap<T> extends UIntKeyHashBase {
    private T[] values;

    public UIntMap() {
        this(INITIAL_SIZE);
    }

    public UIntMap(int size) {
        int k = INITIAL_SIZE;
        while (k < size)
            k <<= 1;
        keys = new int[k];
        Arrays.fill(keys, -1);
        values = (T[]) new Object[k];
        threshold = (int) (k * LOAD_FACTOR);
        modulo = k - 1;
    }

    /**
     * Returns the value for the key. If key does not exist, returns 0.
     *
     * @param key key
     * @return count of the key
     */
    public T get(int key) {
        if (key < 0) {
            throw new IllegalArgumentException("Key cannot be negative: " + key);
        }
        int probeCount = 0;
        int slot = firstProbe(key);
        while (true) {
            final int t = keys[slot];
            if (t == EMPTY) {
                return null;
            }
            if (t == DELETED) {
                slot = nextProbe(slot, ++probeCount);
                continue;
            }
            if (t == key) {
                return values[slot];
            }
            slot = nextProbe(slot, ++probeCount);
        }
    }

    public boolean containsKey(int key) {
        return locate(key) >= 0;
    }

    private void expand() {
        UIntMap<T> h = new UIntMap<>(values.length * 2);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] >= 0) {
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

    /**
     * puts `key` with `value`. if `key` already exists, it overwrites its value with `value`
     */
    public void put(int key, T value) {
        if (key < 0) {
            throw new IllegalArgumentException("Key cannot be negative: " + key);
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

    public List<T> getValues() {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            int key = keys[i];
            if(key>=0) {
                result.add(values[i]);
            }
        }
        return result;
    }

    /**
     * returns the keys sorted ascending.
     */
    public int[] getKeysSorted() {
        int[] sorted = new int[keyCount];
        int c =0;
        for (int key : keys) {
            if (key >= 0)
                sorted[c++]=key;
        }
        Arrays.sort(sorted);
        return sorted;
    }

    /**
     * returns the keys sorted ascending.
     */
    public List<T> getValuesSortedByKey() {
        int[] sortedKeys = getKeysSorted();
        List<T> result = new ArrayList<>(sortedKeys.length);
        for (int sortedKey : sortedKeys) {
            result.add(values[sortedKey]);
        }
        return result;
    }

}
