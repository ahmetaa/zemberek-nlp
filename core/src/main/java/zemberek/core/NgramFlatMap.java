package zemberek.core;

import java.util.Arrays;

/**
 * A hash map for Ngram ids
 */
public class NgramFlatMap {

    private static final int INITIAL_SIZE = 8;
    private static final double LOAD_FACTOR = 0.6;
    private static final int EMPTY = -1;
    private static final int DELETED = -2;

    // Array length is a value power of two, so we can use x & modulo instead of
    // x % size to calculate the slot
    private int modulo = INITIAL_SIZE - 1;

    private int[] ids;
    private int[] counts;
    private final int order;

    int keyCount;

    // When structure has this amount of keys, it expands the key and count arrays.
    int threshold = (int) (INITIAL_SIZE * LOAD_FACTOR);

    public NgramFlatMap(int order) {
        this(order, INITIAL_SIZE);
    }

    public NgramFlatMap(int order, int size) {
        this.order = order;
        int k = INITIAL_SIZE;
        while (k < size)
            k <<= 1;
        ids = new int[k * order];
        Arrays.fill(ids, -1);
        counts = new int[k];
        threshold = (int) (k * LOAD_FACTOR);
        modulo = k - 1;
    }

    private int firstProbe(int[] keys) {
        int d = INITIAL_HASH_SEED;
        for (int a : keys) {
            d = (d ^ a) * 16777619;
        }
        return (d & 0x7fffffff) & modulo;
    }

    public int size() {
        return keyCount;
    }

    private int nextProbe(int previousIndex, int probeCount) {
        return (previousIndex + probeCount) & modulo;
    }

    public static final int INITIAL_HASH_SEED = 0x811C9DC5;

    private int locate(int[] key) {
        int probeCount = 0;
        int slot = firstProbe(key);
        int pointer = -1;
        while (true) {
            final int k = ids[slot * order];
            if (k == EMPTY) {
                return pointer < 0 ? (-slot - 1) : (-pointer - 1);
            }
            if (k == DELETED) {
                if (pointer < 0) {
                    pointer = slot;
                }
                slot = nextProbe(slot, ++probeCount);
                continue;
            }
            for (int j = 0; j < order; j++) {
                if (ids[slot * order + j] != key[j])
                    break;
                if (j == order - 1)
                    return slot;
            }
            slot = nextProbe(slot, ++probeCount);
        }
    }

    public boolean contains(int[] ids) {
        return locate(ids) >= 0;
    }

    /**
     * Returns the value for the key. If key does not exist, returns 0.
     *
     * @param key key
     * @return count of the key
     */
    public int getCount(int[] key) {
        int probeCount = 0;
        int slot = firstProbe(key);
        while (true) {
            final int t = ids[slot * order];
            if (t == EMPTY) {
                return 0;
            }
            if (t == DELETED) {
                slot = nextProbe(slot, ++probeCount);
                continue;
            }
            for (int j = 0; j < order; j++) {
                if (ids[slot * order + j] != key[j])
                    break;
                if (j == order - 1)
                    return counts[slot];
            }
            slot = nextProbe(slot, ++probeCount);
        }
    }

    /**
     * increment the value by "amount". If value does not exist, it a applies set() operation.
     *
     * @param key key
     * @return incremented value
     */
    public int increment(int[] key) {
        return incrementByAmount(key, 1);
    }

    /**
     * increment the value by "amount". If value does not exist, it a applies set() operation.
     *
     * @param key    key
     * @param amount amount to increment
     * @return incremented value
     */
    public int incrementByAmount(int[] key, int amount) {
        if (keyCount == threshold) {
            expand();
        }
        int l = locate(key);
        if (l < 0) {
            l = -l - 1;
            counts[l] = amount;
            System.arraycopy(key, 0, ids, l * order, key.length);
            keyCount++;
            return counts[l];
        } else {
            counts[l] += amount;
            return counts[l];
        }
    }

    private void expand() {
        NgramFlatMap h = new NgramFlatMap(order, counts.length * 2);
        for (int i = 0; i < counts.length; i++) {
            if (ids[i * order] >= 0) {
                int k[] = new int[order];
                System.arraycopy(ids, i * order, k, 0, order);
                h.put(k, counts[i]);
            }
        }
        assert (h.keyCount == keyCount);
        this.counts = h.counts;
        this.ids = h.ids;
        this.keyCount = h.keyCount;
        this.modulo = h.modulo;
        this.threshold = h.threshold;
    }

    public void put(int[] key, int value) {
        if (keyCount == threshold) {
            expand();
        }
        int loc = locate(key);
        if (loc >= 0) {
            counts[loc] = value;
        } else {
            loc = -loc - 1;
            System.arraycopy(key, 0, ids, loc * order, key.length);
            counts[loc] = value;
            keyCount++;
        }
    }

    public static class NgramCount implements Comparable<NgramCount> {
        int[] ids;
        int count;

        public NgramCount(int[] ids, int count) {
            this.ids = ids;
            this.count = count;
        }

        @Override
        public int compareTo(NgramCount o) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] > o.ids[i])
                    return 1;
                else if (ids[i] == o.ids[i])
                    continue;
                return -1;
            }
            return 0;
        }
    }

    public NgramCount[] getAllSorted() {
        NgramCount[] grams = getAll();
        Arrays.sort(grams);
        return grams;
    }

    public NgramCount[] getAll() {
        NgramCount[] grams = new NgramCount[keyCount];
        int c = 0;
        for (int i = 0; i < counts.length; i++) {
            if (ids[i * order] >= 0) {
                int k[] = new int[order];
                System.arraycopy(ids, i * order, k, 0, order);
                grams[c++] = new NgramCount(k, counts[i]);
            }
        }
        return grams;
    }
}
