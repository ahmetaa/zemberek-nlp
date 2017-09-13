package zemberek.core.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A memory efficient and fast set like data structure with Integer values.
 * Values can be between Integer.MIN_VALUE and Integer.MAX_VALUE.
 * Methods do not check for overflow or underflow.
 * Class is not thread safe.
 */
public class IntValueMap<T> extends HashBase<T> implements Iterable<T> {

    // Carries count values.
    private int[] values;

    public IntValueMap() {
        this(INITIAL_SIZE);
    }

    public IntValueMap(int size) {
        super(size);
        values = new int[keys.length];
    }

    /**
     * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value by 1.
     *
     * @param key key
     * @return the new value after addOrIncrement
     */
    public int addOrIncrement(T key) {
        return incrementByAmount(key, 1);
    }

    /**
     * Adds all keys in Iterable.
     * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value by 1.
     *
     * @param keys key
     */
    public void addOrIncrementAll(Iterable<T> keys) {
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
            throw new IllegalArgumentException("Key cannot be null.");
        int slot = firstProbe(hash(key));
        while (true) {
            final T t = keys[slot];
            if (t == null) {
                return 0;
            }
            if (t == TOMB_STONE) {
                slot = nextProbe(slot+1);
                continue;
            }
            if (t.equals(key)) {
                return values[slot];
            }
            slot = nextProbe(slot+1);
        }
    }

    /**
     * Decrements the objects count.
     *
     * @param key key
     * @return value after decrement.
     */
    public int decrement(T key) {
        return incrementByAmount(key, -1);
    }

    /**
     * addOrIncrement the value by "amount". If value does not exist, it a applies set() operation.
     *
     * @param key    key
     * @param amount amount to addOrIncrement
     * @return incremented value
     */
    public int incrementByAmount(T key, int amount) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
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
            return values[l];
        }
    }

    private void expand() {
        IntValueMap<T> h = new IntValueMap<>(newSize());
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i)) {
                h.put(keys[i], values[i]);
            }
        }
        expandCopyParameters(h);
        this.values = h.values;
    }

    public void put(T key, int value) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
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
     * @return a clone of value array.
     */
    public int[] copyOfValues() {
        int[] result = new int[keyCount];
        int k = 0;
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i)) {
                result[k] = values[i];
                k++;
            }
        }
        return result;
    }

    public List<Entry<T>> getAsEntryList() {
        List<Entry<T>> res = new ArrayList<>(keyCount);
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i))
                res.add(new Entry<>(keys[i], values[i]));
        }
        return res;
    }

    public Iterator<Entry<T>> entryIterator() {
        return new EntryIterator();
    }

    public Iterable<Entry<T>> iterableEntries() {
        return EntryIterator::new;
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
            while (!hasValidKey(i)) {
                i++;
            }
            Entry<T> te = new Entry<>(keys[i], values[i]);
            i++;
            k++;
            return te;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class Entry<T> implements Comparable<Entry<T>> {
        public final T key;
        public final int count;

        public Entry(T key, int count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public int compareTo(Entry<T> o) {
            return Integer.compare(o.count, count);
        }

        public String toString() {
            return key.toString() + ":" + count;
        }
    }
}