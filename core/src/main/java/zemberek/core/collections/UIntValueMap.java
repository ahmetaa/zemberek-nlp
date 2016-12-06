package zemberek.core.collections;

public class UIntValueMap<T> extends HashBase<T> implements Iterable<T> {

    // Carries count values.
    private int[] values;

    public UIntValueMap() {
        this(INITIAL_SIZE);
    }

    public UIntValueMap(int size) {
        int k = INITIAL_SIZE;
        while (k < size)
            k <<= 1;
        keys = (T[]) new Object[k];
        values = new int[k];
        threshold = (int) (k * DEFAULT_LOAD_FACTOR);
        modulo = k - 1;
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
        UIntValueMap<T> h = new UIntValueMap<>(values.length * 2);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != TOMB_STONE)
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

    /**
     * Sets the key with the value. If there is a matching key, it overwrites it (key and the value).
     * @param key key
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
            if(key!=null && key!=TOMB_STONE) {
                result[j++] = values[i];
            }
        }
        return result;
    }

}