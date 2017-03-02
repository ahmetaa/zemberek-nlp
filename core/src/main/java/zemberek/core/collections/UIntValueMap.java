package zemberek.core.collections;

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
        int probeCount = 0;
        int slot = firstProbe(hash(key));
        while (true) {
            final T t = keys[slot];
            if (t == null) {
                return -1;
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

    private void expand() {
        UIntValueMap<T> h = new UIntValueMap<>(newSize());
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != TOMB_STONE)
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

    public int[] values() {
        int[] result = new int[size()];
        int j = 0;
        for (int i = 0; i < keys.length; i++) {
            T key = keys[i];
            if (key != null && key != TOMB_STONE) {
                result[j++] = values[i];
            }
        }
        return result;
    }

}