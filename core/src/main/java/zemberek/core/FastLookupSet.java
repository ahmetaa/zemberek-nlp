package zemberek.core;

public class FastLookupSet<T> extends HashBase<T> implements Iterable<T> {

    public FastLookupSet() {
        this(INITIAL_SIZE);
    }

    public FastLookupSet(int size) {
        int k = INITIAL_SIZE;
        while (k < size)
            k <<= 1;
        keys = (T[]) new Object[k];
        threshold = (int) (k * DEFAULT_LOAD_FACTOR);
        modulo = k - 1;
    }

    private void expand() {
        FastLookupSet<T> h = new FastLookupSet<>(keys.length * 2);
        for (T key : keys) {
            if (key != null && key != TOMB_STONE) {
                h.set(key);
            }
        }
        assert (h.keyCount == keyCount);
        this.keys = h.keys;
        this.keyCount = h.keyCount;
        this.modulo = h.modulo;
        this.threshold = h.threshold;
        this.removeCount = 0;
    }

    /**
     * Adds this key to Set. If there is an equivalent exist, it overrides it.
     * if there was an equivalent key, it returns it. Otherwise returns null.
     *
     * @param key key
     */
    public T set(T key) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
        if (keyCount + removeCount == threshold) {
            expand();
        }
        int loc = locate(key);
        if (loc >= 0) {
            T old = keys[loc];
            keys[loc] = key;
            return old;
        } else {
            loc = -loc - 1;
            keys[loc] = key;
            keyCount++;
            return null;
        }
    }

    @SafeVarargs
    public final void addAll(T... t) {
        for (T t1 : t) {
            add(t1);
        }
    }

    public void addAll(Iterable<T> it) {
        for (T t1 : it) {
            add(t1);
        }
    }

    public boolean add(T key) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
        if (keyCount + removeCount == threshold) {
            expand();
        }
        int loc = locate(key);
        if (loc >= 0) {
            return false;
        } else {
            loc = -loc - 1;
            keys[loc] = key;
            keyCount++;
            return true;
        }
    }

    public T getOrAdd(T key) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
        if (keyCount + removeCount == threshold) {
            expand();
        }
        int loc = locate(key);
        if (loc >= 0) {
            return keys[loc];
        } else {
            loc = -loc - 1;
            keys[loc] = key;
            keyCount++;
            return key;
        }
    }

}
