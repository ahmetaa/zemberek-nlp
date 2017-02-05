package zemberek.core.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A compact set structure for counting objects.
 *
 * @param <T>
 */
public class CountSet<T> extends HashBase<T> implements Iterable<T> {

    // Carries count values.
    private int[] counts;

    public CountSet() {
        this(INITIAL_SIZE);
    }

    public CountSet(int size) {
        super(size);
        counts = new int[keys.length];
    }

    /**
     * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value by 1.
     *
     * @param key key
     * @return the new count value after increment
     */
    public int increment(T key) {
        return incrementByAmount(key, 1);
    }

    /**
     * Adds all keys in Iterable.
     * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value by 1.
     *
     * @param keys key
     */
    public void incrementAll(Iterable<T> keys) {
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
        int probeCount = 0;
        int slot = firstProbe(hash(key));
        while (true) {
            final T t = keys[slot];
            if (t == null) {
                return 0;
            }
            if (t == TOMB_STONE) {
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

    /**
     * increment the value by "amount". If value does not exist, it a applies set() operation.
     *
     * @param key    key
     * @param amount amount to increment
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
            counts[l] = amount;
            keys[l] = key;
            keyCount++;
            return counts[l];
        } else {
            counts[l] += amount;
            return counts[l];
        }
    }

    private void expand() {
        CountSet<T> h = new CountSet<>(newSize());
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != TOMB_STONE)
                h.set(keys[i], counts[i]);
        }
        expandCopyParameters(h);
        this.counts = h.counts;
    }

    public void set(T key, int value) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
        if (keyCount + removeCount == threshold) {
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
     * @return a clone of value array.
     */
    public int[] copyOfValues() {
        return counts.clone();
    }

    public List<Entry<T>> getAsEntryList() {
        List<Entry<T>> res = new ArrayList<>(keyCount);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != TOMB_STONE)
                res.add(new Entry<>(keys[i], counts[i]));
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
            while (keys[i] == null || keys[i] == TOMB_STONE) {
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