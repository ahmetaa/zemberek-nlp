package zemberek.core.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base class for various specialized hash table like data structures that uses unsigned integer keys.
 */
public abstract class UIntKeyHashBase {

    protected static final int INITIAL_SIZE = 4;
    private static final double LOAD_FACTOR = 0.7;
    static final int EMPTY = -1;
    static final int DELETED = -2;

    // Array length is a value power of two, so we can use x & modulo instead of
    // x % size to calculate the slot
    protected int modulo = INITIAL_SIZE - 1;
    protected int[] keys;

    protected int keyCount;
    protected int removeCount;

    // When structure has this amount of keys, it expands the key and count arrays.
    protected int threshold = (int) (INITIAL_SIZE * LOAD_FACTOR);

    protected int firstProbe(int hashCode) {
        return hashCode & modulo;
    }

    protected int nextProbe(int index) {
        return index & modulo;
    }

    UIntKeyHashBase(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Size must be a positive value. But it is " + size);
        }
        int k = 1;
        while (k < size)
            k <<= 1;
        keys = new int[k];
        Arrays.fill(keys, -1);
        threshold = (int) (k * LOAD_FACTOR);
        modulo = k - 1;
    }

    protected int locate(int key) {

        int slot = firstProbe(key);
        int pointer = -1;
        while (true) {
            final int k = keys[slot];
            if (k == EMPTY) {
                return pointer < 0 ? (-slot - 1) : (-pointer - 1);
            }
            if (k == DELETED) {
                if (pointer < 0) {
                    pointer = slot;
                }
                slot = nextProbe(slot+1);
                continue;
            }
            if (k == key) {
                return slot;
            }
            slot = nextProbe(slot+1);
        }
    }

    public boolean containsKey(int key) {
        return locate(key) >= 0;
    }

    /**
     * removes the key.
     */
    public void remove(int key) {
        int k = locate(key);
        if (k < 0)
            return;
        keys[k] = DELETED;
        keyCount--;
        removeCount++;
    }

    void copyParameters(UIntKeyHashBase h) {
        assert (h.keyCount == keyCount);
        this.keys = h.keys;
        this.modulo = h.modulo;
        this.threshold = h.threshold;
        this.removeCount = 0;
    }

    int newSize() {
        long size = keys.length * 2L;
        if(size > Integer.MAX_VALUE) {
            throw new IllegalStateException("Too many items in collection " + this.getClass());
        }
        return (int) size;
    }

    public int size() {
        return keyCount;
    }

    /**
     * returns the keys sorted ascending.
     */
    public List<Integer> getKeysSorted() {
        List<Integer> keyList = new ArrayList<>();
        for (int key : keys) {
            if (key >= 0)
                keyList.add(key);
        }
        Collections.sort(keyList);
        return keyList;
    }

    /**
     * returns the keys sorted ascending.
     */
    public int[] getKeyArraySorted() {
        int[] sorted = getKeyArray();
        Arrays.sort(sorted);
        return sorted;
    }


    /**
     * returns the keys sorted ascending.
     */
    public int[] getKeyArray() {
        int[] keyArray = new int[keyCount];
        int c = 0;
        for (int key : keys) {
            if (key >= 0)
                keyArray[c++] = key;
        }
        return keyArray;
    }

}
