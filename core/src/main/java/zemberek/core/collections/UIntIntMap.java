package zemberek.core.collections;

public class UIntIntMap extends UIntKeyHashBase {

    protected int[] values;

    public UIntIntMap() {
        this(INITIAL_SIZE);
    }

    public UIntIntMap(int size) {
        super(size);
        values = new int[keys.length];
    }

    /**
     * Returns the value for the key. If key does not exist, returns 0.
     *
     * @param key key
     * @return count of the key
     */
    public int get(int key) {
        if (key < 0) {
            throw new IllegalArgumentException("Key cannot be negative: " + key);
        }
        int slot = firstProbe(key);
        while (true) {
            final int t = keys[slot];
            if (t == EMPTY) {
                return 0;
            }
            if (t == DELETED) {
                slot = nextProbe(slot+1);
                continue;
            }
            if (t == key) {
                return values[slot];
            }
            slot = nextProbe(slot+1);
        }
    }

    private void expand() {
        UIntIntMap h = new UIntIntMap(newSize());
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] >= 0) {
                h.put(keys[i], values[i]);
            }
        }
        copyParameters(h);
        this.values = h.values;
    }

    /**
     * puts `key` with `value`. if `key` already exists, it overwrites its value with `value`
     */
    public void put(int key, int value) {
        if (key < 0) {
            throw new IllegalArgumentException("Key cannot be negative: " + key);
        }
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
     * if `key` exists, increments it's value with `amount`. if `key` does not exist,
     * it creates it with the value `amount`.
     * returns the `key`'s value after the addOrIncrement operation.
     */
    public int increment(int key, int amount) {
        if (key < 0) {
            throw new IllegalArgumentException("Key cannot be negative: " + key);
        }
        if (keyCount == threshold) {
            expand();
        }
        int loc = locate(key);
        if (loc >= 0) {
            values[loc] += amount;
            return values[loc];
        } else {
            loc = -loc - 1;
            keys[loc] = key;
            values[loc] = amount;
            keyCount++;
            return amount;
        }
    }
}