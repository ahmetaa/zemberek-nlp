package zemberek.core.collections;

import java.util.Arrays;

/**
 * A simple integer array backed list like structure
 */
public class DynamicIntArray {
    private static final int DEFAULT_INITIAL_CAPACITY = 7;
    private int[] data;
    private int size = 0;

    public DynamicIntArray() {
        data = new int[DEFAULT_INITIAL_CAPACITY];
    }

    public DynamicIntArray(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity must be positive. But it is " + initialCapacity);
        }
        data = new int[initialCapacity];
    }

    public void add(int i) {
        if (size == data.length) {
            expand();
        }
        data[size] = i;
        size++;
    }

    public int get(int index) {
        return data[index];
    }

    public void set(int index, int value) {
        data[index] = value;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    public void sort() {
        Arrays.sort(data, 0, size);
    }

    public int[] copyOf() {
        return Arrays.copyOf(data, size);
    }

    public void trimToSize() {
        data = Arrays.copyOf(data, size);
    }

    private void expand() {
        if (size == Integer.MAX_VALUE) {
            throw new IllegalStateException("List size exceeded positive integer limit.");
        }
        long newSize = size * 2L;
        if (newSize > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        data = Arrays.copyOf(data, size * 2);
    }
}
