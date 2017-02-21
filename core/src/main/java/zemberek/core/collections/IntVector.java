package zemberek.core.collections;

import java.util.Arrays;

/**
 * A simple integer array backed list like structure
 */
public class IntVector {
    private static final int DEFAULT_INITIAL_CAPACITY = 7;
    private int[] data;
    private int size = 0;

    public IntVector() {
        data = new int[DEFAULT_INITIAL_CAPACITY];
    }

    public IntVector(int initialCapacity) {
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

    public void addAll(int[] arr) {
        if (size + arr.length >= data.length) {
            expand(arr.length);
        }
        System.arraycopy(arr, 0, data, size, arr.length);
        size += arr.length;
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
        expand(0);
    }

    public boolean isempty() {
        return size == 0;
    }

    private void expand(int offset) {
        if (size + offset >= Integer.MAX_VALUE) {
            throw new IllegalStateException("List size exceeded positive integer limit.");
        }
        long newSize = size * 2L + offset;
        if (newSize > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        data = Arrays.copyOf(data, (int) newSize);
    }
}
