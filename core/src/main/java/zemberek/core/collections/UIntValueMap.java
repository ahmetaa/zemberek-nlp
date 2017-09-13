package zemberek.core.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class UIntValueMap<T> extends HashBase<T> implements Iterable<T> {

    // Carries unsigned integer values.
    private int[] values;

    public UIntValueMap() {
        this(INITIAL_SIZE);
    }

    public UIntValueMap(int size) {
        super(size);
        values = new int[keys.length];
    }

    /**
     * Returns the count of the key. If key does not exist, returns -1.
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
                return -1;
            }
            if (t == TOMB_STONE) {
                slot = nextProbe(slot + 1);
                continue;
            }
            if (t.equals(key)) {
                return values[slot];
            }
            slot = nextProbe(slot + 1);
        }
    }

    private void expand() {
        UIntValueMap<T> h = new UIntValueMap<>(newSize());
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i))
                h.put(keys[i], values[i]);
        }
        expandCopyParameters(h);
        this.values = h.values;
    }

    /**
     * Sets the key with the value. If there is a matching key, it overwrites it (key and the value).
     *
     * @param key   key
     * @param value value
     */
    public void put(T key, int value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null.");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Value cannot be negative : " + value);
        }
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

    /**
     * If key does not exist, it adds it with count value 1. Otherwise, it increments the count value by 1.
     *
     * @param key key
     * @return the new value after addOrIncrement
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
            if (values[l] < 0) {
                throw new IllegalStateException("Value reached to negative.");
            }
            return values[l];
        } else {
            values[l] += amount;
            if (values[l] < 0) {
                throw new IllegalStateException("Value reached to negative.");
            }
            return values[l];
        }
    }

    public List<IntValueMap.Entry<T>> getAsEntryList() {
        List<IntValueMap.Entry<T>> res = new ArrayList<>(keyCount);
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i))
                res.add(new IntValueMap.Entry<>(keys[i], values[i]));
        }
        return res;
    }

    public Iterator<IntValueMap.Entry<T>> entryIterator() {
        return new EntryIterator();
    }

    public Iterable<IntValueMap.Entry<T>> iterableEntries() {
        return EntryIterator::new;
    }

    private class EntryIterator implements Iterator<IntValueMap.Entry<T>> {

        int i;
        int k;

        @Override
        public boolean hasNext() {
            return k < keyCount;
        }

        @Override
        public IntValueMap.Entry<T> next() {
            while (!hasValidKey(i)) {
                i++;
            }
            IntValueMap.Entry<T> te = new IntValueMap.Entry<>(keys[i], values[i]);
            i++;
            k++;
            return te;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class Entry<T> implements Comparable<IntValueMap.Entry<T>> {
        public final T key;
        public final int count;

        public Entry(T key, int count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public int compareTo(IntValueMap.Entry<T> o) {
            return Integer.compare(o.count, count);
        }

        public String toString() {
            return key.toString() + ":" + count;
        }
    }


    /**
     * counts the items those values are smaller than amount
     *
     * @param amount amount to check size
     * @return count.
     */
    public int sizeLarger(int amount) {
        int count = 0;
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i) && values[i] > amount) {
                count++;
            }
        }
        return count;
    }

    /**
     * counts the items those values are smaller than amount
     *
     * @param amount amount to check size
     * @return count.
     */
    public int sizeSmaller(int amount) {
        int count = 0;
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i) && values[i] < amount) {
                count++;
            }
        }
        return count;
    }

    /**
     * returns the max value.
     *
     * @return the max value in the map. If map is empty, returns 0.
     */
    public int maxValue() {
        int max = 0;
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i) && values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    /**
     * returns the max value.
     *
     * @return the max value in the map. If map is empty, returns 0.
     */
    public int minValue() {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i) && values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    public long sumOfValues(int minValue, int maxValue) {
        long sum = 0;
        for (int i = 0; i < keys.length; i++) {
            int value = values[i];
            if (hasValidKey(i) && value >= minValue && value <= maxValue) {
                sum += value;
            }
        }
        return sum;
    }

    public int[] valueArray() {
        int[] result = new int[size()];
        int j = 0;
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i)) {
                result[j++] = values[i];
            }
        }
        return result;
    }

    public long sumOfValues() {
        long sum = 0;
        for (int i = 0; i < keys.length; i++) {
            if (hasValidKey(i)) {
                sum += values[i];
            }
        }
        return sum;
    }

}