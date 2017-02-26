package zemberek.core.collections;

import java.util.Arrays;
import java.util.Random;

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

    public IntVector(int[] values) {
        data = new int[values.length + DEFAULT_INITIAL_CAPACITY];
        System.arraycopy(values, 0, data, 0, values.length);
        size = values.length;
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

    public void addAll(IntVector vec) {
        if (size + vec.size >= data.length) {
            expand(vec.size);
        }
        System.arraycopy(vec.data, 0, data, size, vec.size);
        size += vec.size;
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

    public void shuffle(Random random) {
        for (int i = size - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int d = data[index];
            data[index] = data[i];
            data[i] = d;
        }
    }

    public boolean contains(int i) {
        for (int j = 0; j < size; j++) {
            if (data[j] == i) {
                return true;
            }
        }
        return false;
    }

    public void shuffle() {
        shuffle(new Random());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntVector vector = (IntVector) o;

        if (size != vector.size) return false;
        for (int i = 0; i < size; i++) {
            if (data[i] != vector.data[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0; i < size; i++) {
            result = 31 * result + data[i];
        }
        result = 31 * result + size;
        return result;
    }
}
