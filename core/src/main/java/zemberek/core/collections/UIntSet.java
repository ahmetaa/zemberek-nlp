package zemberek.core.collections;

public class UIntSet extends UIntKeyHashBase {

    public UIntSet() {
        this(INITIAL_SIZE);
    }

    public UIntSet(int size) {
        super(size);
    }

    public boolean contains(int key) {
        return locate(key) >= 0;
    }

    private void expand() {
        UIntSet h = new UIntSet(newSize());
        for (int key : keys) {
            if (key >= 0) {
                h.add(key);
            }
        }
        copyParameters(h);
    }

    /**
     * puts `key` with `value`. if `key` already exists, it overwrites its value with `value`
     */
    public boolean add(int key) {
        if (key < 0) {
            throw new IllegalArgumentException("Key cannot be negative: " + key);
        }
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

    public void addAll(int... keys) {
        for (int key : keys) {
            add(key);
        }
    }

    public int size() {
        return keyCount;
    }

}
