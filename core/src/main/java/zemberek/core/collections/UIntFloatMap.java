package zemberek.core.collections;


public class UIntFloatMap extends UIntKeyHashBase {

    private float[] values;
    private float noKeyValue = 0;

    public UIntFloatMap() {
        this(INITIAL_SIZE);
    }

    public UIntFloatMap(int size) {
        super(size);
        values = new float[keys.length];
    }

    public UIntFloatMap(int size, float noKeyValue) {
        super(size);
        values = new float[keys.length];
        this.noKeyValue = noKeyValue;
    }

    /**
     * Returns the value for the key. If key does not exist, returns [noKeyValue].
     * If value is not defined during instantiation, it is 0
     *
     * @param key key
     * @return float value associated with the unsigned integer key.
     */
    public float get(int key) {
        if (key < 0) {
            throw new IllegalArgumentException("Key cannot be negative: " + key);
        }

        int slot = firstProbe(key);
        while (true) {
            final int t = keys[slot];
            if (t == EMPTY) {
                return noKeyValue;
            }
            if (t == DELETED) {
                slot = nextProbe(slot + 1);
                continue;
            }
            if (t == key) {
                return values[slot];
            }
            slot = nextProbe(slot + 1);
        }
    }

    public boolean containsKey(int key) {
        return locate(key) >= 0;
    }

    private void expand() {
        UIntFloatMap h = new UIntFloatMap(newSize());
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
    public void put(int key, float value) {
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

}