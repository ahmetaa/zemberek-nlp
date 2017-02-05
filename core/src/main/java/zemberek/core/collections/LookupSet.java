package zemberek.core.collections;

/**
 * A Set-like data structure that allows looking up if an equivalent instance exists in it.
 */
public class LookupSet<T> extends HashBase<T> implements Iterable<T> {

    public LookupSet() {
        this(INITIAL_SIZE);
    }

    public LookupSet(int size) {
        super(size);
    }

    private void expand() {
        LookupSet<T> h = new LookupSet<>(newSize());
        for (T key : keys) {
            if (key != null && key != TOMB_STONE) {
                h.set(key);
            }
        }
        expandCopyParameters(h);
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

    /**
     * If there is an equivalent object, it does nothing and returns false.
     * Otherwise it adds the item to set and returns true.
     *
     * @param key input
     */
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

    /**
     * If there is an equivalent object, returns it. Otherwise adds it and returns the input.
     *
     * @param key input.
     */
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
