package zemberek.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A compact set structure for counting objects.
 *
 * @param <T>
 */
public class CountSet<T> implements Iterable<T> {

    static final int INITIAL_SIZE = 8;
    static final double DEFAULT_LOAD_FACTOR = 0.5;

    // This is the size-1 of the key and value array length. Array length is a value power of two
    private int modulo = INITIAL_SIZE - 1;

    // Key array.
    T[] keys;

    // Used for marking slots of deleted keys.
    private final T SENTINEL = (T) new Object();

    // Carries count values.
    int[] counts;

    int keyCount;

    // When structure has this amount of keys, it expands the key and count arrays.
    int threshold = (int) (INITIAL_SIZE * DEFAULT_LOAD_FACTOR);

    // Only used for debugging.
    int collisionCount;

    public CountSet() {
        this(INITIAL_SIZE);
    }

    public CountSet(int size) {
        size += (int) (size * (1 - DEFAULT_LOAD_FACTOR));
        if (size < 2)
            size = 2;
        if ((size & (size - 1)) != 0) { // check for power of two
            int power = (int) (Math.log(size) / Math.log(2));
            size = 1 << (power + 1);
        }
        keys = (T[]) new Object[size];
        counts = new int[size];
        threshold = (int) (size * DEFAULT_LOAD_FACTOR);
        modulo = size - 1;
    }

    private int firstProbe(int hashCode) {
        return hashCode & modulo;
    }

    private int nextProbe(int previousIndex, int probeCount) {
        return (previousIndex + probeCount) & modulo;
    }

    private int hash(T key) {
        return key.hashCode();
    }

    /*
     * locate operation does the following:
     * - finds the slot
     * - if there was a deleted key before (key[slot]==SENTINEL) and pointer is not set yet (pointer==-1) pointer is set to this
     *   slot index and index is incremented.
     *   This is necessary for the following problem.
     *   Suppose we add key "foo" first then key "bar" with key clash. first one is put to slotindex=1 and the other one is
     *   located to slot=2. Then we remove the key "foo". Now if we do not use the SENTINEL, and want to access the value of key "bar".
     *   we would get "2" because slot will be 1 and key does not exist there.
     *   that is why we use a SENTINEL object for marking deleted slots. So when getting a value we pass the deleted slots. And when we insert,
     *   we use the first deleted slot if any.
     *    Key Val  Key Val  Key Val
     *     0   0    0   0    0   0
     *     foo 2    foo 2    SENTINEL  2
     *     0   0    bar 3    bar 3
     *     0   0    0   0    0   0
     * - if there was no deleted key in that slot, check the value. if value is null then we can put our key here. However,
     *   we cannot return the slot value immediately. if pointer value is set, we use it as the vacant index. we do not use
     *   the slot or the pointer value itself. we use negative of it, pointing the key does not exist in this list. Also we
     *   return -slot-1 or -pointer-1 to avoid the 0 index problem.
     *
     */
    private int locate(T key) {
        int probeCount = 0;
        int slot = firstProbe(hash(key));
        int pointer = -1;
        while (true) {
            final T t = keys[slot];
            if (t == null) {
                return pointer < 0 ? (-slot - 1) : (-pointer - 1);
            }
            if (t == SENTINEL) {
                if (pointer < 0) {
                    pointer = slot;
                }
                slot = nextProbe(slot, ++probeCount);
                continue;
            }
            if (t.equals(key))
                return slot;
            slot = nextProbe(slot, ++probeCount);
        }
    }

    /**
     * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value by 1.
     *
     * @param key key
     * @return the new count value after increment
     */
    public int add(T key) {
        return incrementByAmount(key, 1);
    }

    /**
     * Adds all keys in Iterable.
     * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value by 1.
     *
     * @param keys key
     */
    public void addAll(Iterable<T> keys) {
        for (T t : keys) {
            incrementByAmount(t, 1);
        }
    }

    /**
     * Returns the count of the key. If key does not exist, returns 0.
     *
     * @param key key
     * @return count of the key
     */
    public int get(T key) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null. But it is:" + key);
        int probeCount = 0;
        int slot = firstProbe(hash(key));
        while (true) {
            final T t = keys[slot];
            if (t == null) {
                return 0;
            }
            if (t == SENTINEL) {
                slot = nextProbe(slot, ++probeCount);
                continue;
            }
            if (t.equals(key))
                return counts[slot];
            slot = nextProbe(slot, ++probeCount);
        }
    }

    public int decrement(T key) {
        return incrementByAmount(key, -1);
    }

    public boolean contains(T key) {
        return locate(key) >= 0;
    }

    /**
     * increment the value by "amount". If value does not exist, it a applies set() operation.
     *
     * @param key    key
     * @param amount amount to increment
     * @return incremented value
     */
    public int incrementByAmount(T key, int amount) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null. But it is:" + key);
        if (keyCount == threshold) {
            expand();
        }
        int l = locate(key);
        if (l < 0) {
            l = -l - 1;
            counts[l] = amount;
            keys[l] = key;
            keyCount++;
            return counts[l];
        } else {
            counts[l] += amount;
            return counts[l];
        }
    }

    public void remove(T key) {
        int k = locate(key);
        if (k < 0)
            return;
        keys[k] = SENTINEL; // mark deletion
        keyCount--;
    }

    private void expand() {
        CountSet<T> h = new CountSet<>(counts.length * 2);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != SENTINEL)
                h.set(keys[i], counts[i]);
        }
        assert (h.keyCount == keyCount);
        this.counts = h.counts;
        this.keys = h.keys;
        this.keyCount = h.keyCount;
        this.modulo = h.modulo;
        this.threshold = h.threshold;
    }

    public void set(T key, int value) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null. But it is:" + key);
        if (keyCount == threshold) {
            expand();
        }
        int loc = locate(key);
        if (loc >= 0) {
            counts[loc] = value;
        } else {
            loc = -loc - 1;
            keys[loc] = key;
            counts[loc] = value;
            keyCount++;
        }
    }

    /**
     * @param input input object.
     * @return the original key equal to the input, if exists. null otherwise.
     */
    public T getKey(T input) {
        int k = locate(input);
        if (k < 0)
            return null;
        return keys[k];
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
        return counts.clone();
    }

    public List<Entry<T>> getAsEntryList() {
        List<Entry<T>> res = new ArrayList<>(keyCount);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != SENTINEL)
                res.add(new Entry<>(keys[i], counts[i]));
        }
        return res;
    }

    public Iterator<Entry<T>> entryIterator() {
        return new EntryIterator();
    }

    public Iterable<Entry<T>> iterableEntries() {
        return new Iterable<Entry<T>>() {
            @Override
            public Iterator<Entry<T>> iterator() {
                return new EntryIterator();
            }
        };
    }

    private class EntryIterator implements Iterator<Entry<T>> {

        int i;
        int k;

        @Override
        public boolean hasNext() {
            return k < keyCount;
        }

        @Override
        public Entry<T> next() {
            while (keys[i] == null || keys[i] == SENTINEL) {
                i++;
            }
            Entry<T> te = new Entry<>(keys[i], counts[i]);
            i++;
            k++;
            return te;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public Iterator<T> iterator() {
        return new KeyIterator();
    }

    private class KeyIterator implements Iterator<T> {

        int i;
        int k;

        @Override
        public boolean hasNext() {
            return k < keyCount;
        }

        @Override
        public T next() {
            while (keys[i] == null || keys[i] == SENTINEL) {
                i++;
            }
            T key = keys[i];
            i++;
            k++;
            return key;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class Entry<T> implements Comparable<Entry<T>> {
        public final T key;
        public final int value;

        public Entry(T key, int value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int compareTo(Entry<T> o) {
            return Integer.compare(value, o.value);
        }
    }
}