package zemberek.core.collections;

import com.google.common.base.Joiner;

import java.util.*;

/**
 * Linear probing Hash base class.
 *
 * @param <T>
 */
public abstract class HashBase<T> {

    static final int INITIAL_SIZE = 4;
    private static final double DEFAULT_LOAD_FACTOR = 0.7;

    // This is the size-1 of the key and value array length. Array length is a value power of two
    private int modulo = INITIAL_SIZE - 1;

    // Key array.
    protected T[] keys;

    // Used for marking slots of deleted keys.
    static final Object TOMB_STONE = new Object();

    int keyCount;
    int removeCount;

    // When structure has this amount of keys, it expands the key and count arrays.
    int threshold = (int) (INITIAL_SIZE * DEFAULT_LOAD_FACTOR);

    HashBase(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Size must be a positive value. But it is " + size);
        }
        int k = 1;
        while (k < size)
            k <<= 1;
        keys = (T[]) new Object[k];
        threshold = (int) (k * DEFAULT_LOAD_FACTOR);
        modulo = k - 1;
    }

    protected HashBase(HashBase<T> other, T[] keys) {
        this.keys = keys;
        this.threshold = other.threshold;
        this.modulo = other.modulo;
        this.keyCount = other.keyCount;
        this.removeCount = other.removeCount;
    }

    final boolean hasValidKey(int i) {
        final T key = keys[i];
        return key != null && key != TOMB_STONE;
    }

    void expandCopyParameters(HashBase<T> h) {
        assert (h.keyCount == keyCount);
        this.keys = h.keys;
        this.modulo = h.modulo;
        this.threshold = h.threshold;
        this.removeCount = 0;
    }

    int newSize() {
        long size = keys.length * 2L;
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("Too many items in collection " + this.getClass());
        }
        return (int) size;
    }

    protected int firstProbe(int hashCode) {
        return hashCode & modulo;
    }

    protected int nextProbe(int index) {
        return index & modulo;
    }

    protected int hash(T key) {
        return key.hashCode();
    }

    /**
     * locate operation does the following:
     * - finds the slot
     * - if there was a deleted key before (key[slot]==TOMB_STONE) and pointer is not set yet (pointer==-1)
     * pointer is set to this slot index and index is incremented.
     * This is necessary for the following problem.
     * Suppose we add key "foo" first then key "bar" with key collision. first one is put to slotindex=1 and the other
     * one is located to slot=2. Then we remove the key "foo". Now if we do not use the TOMB_STONE, and want to access
     * the value of key "bar". We would get "2" because slot will be 1 and key does not exist there.
     * that is why we use a TOMB_STONE object for marking deleted slots. So when getting a value we pass the deleted slots.
     * And when we insert, we use the first deleted slot if any.
     * <pre>
     *    Key Val  Key Val  Key Val
     *     0   0    0   0    0   0
     *     foo 2    foo 2    TOMB_STONE  2
     *     0   0    bar 3    bar 3
     *     0   0    0   0    0   0
     * </pre>
     * - if there was no deleted key in that slot, check the value. if value is null then we can put our key here. However,
     * we cannot return the slot value immediately. if pointer value is set, we use it as the vacant index. we do not use
     * the slot or the pointer value itself. we use negative of it, pointing the key does not exist in this list. Also we
     * return -slot-1 or -pointer-1 to avoid the 0 index problem.
     */
    protected int locate(T key) {
        int slot = firstProbe(hash(key));
        int pointer = -1;
        while (true) {
            final T t = keys[slot];
            if (t == null) {
                return pointer < 0 ? (-slot - 1) : (-pointer - 1);
            }
            if (t == TOMB_STONE) {
                if (pointer < 0) {
                    pointer = slot; // marking the first deleted slot.
                }
                slot = nextProbe(slot+1);
                continue;
            }
            if (t.equals(key)) {
                return slot;
            }
            slot = nextProbe(slot+1);
        }
    }

    public boolean contains(T key) {
        return locate(key) >= 0;
    }

    public T remove(T key) {
        int k = locate(key);
        if (k < 0)
            return null;
        T removed = keys[k];
        keys[k] = (T) TOMB_STONE; // mark deletion
        keyCount--;
        removeCount++;
        return removed;
    }

    /**
     * @param key key object.
     * @return the original key equal to the key, if exists. null otherwise.
     */
    public T lookup(T key) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
        int k = locate(key);
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
     * @return amount of the hash slots.
     */
    int capacity() {
        return keys.length;
    }

    /**
     * an iterator for keys.
     *
     * @return key iterator
     */
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
            while (!hasValidKey(i)) {
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

    /**
     * keys in a list
     *
     * @return list of keys
     */
    public List<T> getKeyList() {
        List<T> res = new ArrayList<>(keyCount);
        for (T key : keys) {
            if (key != null && key != TOMB_STONE)
                res.add(key);
        }
        return res;
    }

    /**
     * get keys in a set.
     *
     * @return keys
     */
    public Set<T> getKeySet() {
        Set<T> res = new HashSet<>(keyCount);
        for (T key : keys) {
            if (key != null && key != TOMB_STONE) {
                res.add(key);
            }
        }
        return res;
    }

    public String toString() {
        return "[ Size = " + size() + " Keys = " + Joiner.on(", ").join(this.iterator()) + "]";
    }
}
