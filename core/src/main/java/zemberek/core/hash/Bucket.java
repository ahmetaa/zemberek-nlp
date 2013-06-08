package zemberek.core.hash;

import java.util.Arrays;

/**
 * A bucket that holds keys. It contains a small array for keys.
 */
class Bucket implements Comparable<Bucket> {
    static final int[] EMPTY = new int[0];
    public final int id;
    public int[] keyIndexes = EMPTY;

    public Bucket(int id) {
        this.id = id;
    }

    void add(int i) {
        keyIndexes = Arrays.copyOf(keyIndexes, keyIndexes.length + 1);
        keyIndexes[keyIndexes.length-1] = i;
    }

    public int compareTo(Bucket o) {
        return keyIndexes.length < o.keyIndexes.length ? 1 : (keyIndexes.length > o.keyIndexes.length ? -1 : 0);
    }
}
