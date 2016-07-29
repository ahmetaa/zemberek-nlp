package zemberek.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class FloatValueMap<T> extends HashBase<T> implements Iterable<T> {

    // Carries count values.
    private float[] values;

    public FloatValueMap() {
        this(INITIAL_SIZE);
    }

    public FloatValueMap(int size) {
        int k = INITIAL_SIZE;
        while (k < size)
            k <<= 1;
        keys = (T[]) new Object[k];
        values = new float[k];
        threshold = (int) (k * DEFAULT_LOAD_FACTOR);
        modulo = k - 1;
    }

    /**
     * Returns the count of the key. If key does not exist, returns 0.
     *
     * @param key key
     * @return count of the key
     */
    public float get(T key) {
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
                return values[slot];
            slot = nextProbe(slot, ++probeCount);
        }
    }

    /**
     * increment the value by "amount". If value does not exist, it a applies set() operation.
     *
     * @param key    key
     * @param amount amount to increment
     * @return incremented value
     */
    public float incrementByAmount(T key, float amount) {
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
        FloatValueMap<T> h = new FloatValueMap<>(values.length * 2);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != TOMB_STONE)
                h.set(keys[i], values[i]);
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
     * Sets the key with the value. If there is a matching key, it overwrites it (key and the value).
     * @param key key
     * @param value value
     */
    public void set(T key, float value) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
        if (keyCount + removeCount == threshold) {
            expand();
        }
        int loc = locate(key);
        if (loc >= 0) {
            keys[loc] = key;
            values[loc] = value;
        } else {
            loc = -loc - 1;
            keys[loc] = key;
            values[loc] = value;
            keyCount++;
        }
    }

    public float[] values() {
        float[] result = new float[size()];
        int j = 0;
        for (int i = 0; i < keys.length; i++) {
            T key = keys[i];
            if(key!=null && key!=TOMB_STONE) {
                result[j++] = values[i];
            }
        }
        return result;
    }

    public List<Entry<T>> getAsEntryList() {
        List<Entry<T>> res = new ArrayList<>(keyCount);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != TOMB_STONE)
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
            while (keys[i] == null || keys[i] == TOMB_STONE) {
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
        public final float value;

        public Entry(T key, float value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int compareTo(Entry<T> o) {
            return Double.compare(value, o.value);
        }
    }
}