package zemberek.core.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A map like structure that has unsigned integer keys and T values.
 */
public class UIntMap<T> extends UIntKeyHashBase implements Iterable<T> {

  private T[] values;

  public UIntMap() {
    this(INITIAL_SIZE);
  }

  public UIntMap(int size) {
    super(size);
    values = (T[]) new Object[keys.length];
  }

  /**
   * Returns the value for the key. If key does not exist, returns 0.
   *
   * @param key key
   * @return count of the key
   */
  public T get(int key) {
    if (key < 0) {
      throw new IllegalArgumentException("Key cannot be negative: " + key);
    }
    int slot = hash(key) & modulo;
    while (true) {
      final int t = keys[slot];
      if (t == EMPTY) {
        return null;
      }
      if (t == DELETED) {
        slot = (slot + 1) & modulo;
        continue;
      }
      if (t == key) {
        return values[slot];
      }
      slot = (slot + 1) & modulo;
    }
  }

  public boolean containsKey(int key) {
    return locate(key) >= 0;
  }

  private void expand() {
    UIntMap<T> h = new UIntMap<>(newSize());
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
  public void put(int key, T value) {
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
      if (keys[loc] == DELETED) {
        removeCount--;
      }
      keys[loc] = key;
      values[loc] = value;
      keyCount++;
    }
  }

  public List<T> getValues() {
    List<T> result = new ArrayList<>();
    for (int i = 0; i < keys.length; i++) {
      int key = keys[i];
      if (key >= 0) {
        result.add(values[i]);
      }
    }
    return result;
  }

  @Override
  public Iterator<T> iterator() {
    return new ValueIterator();
  }

  /**
   * returns the values sorted ascending.
   */
  public List<T> getValuesSortedByKey() {
    int[] sortedKeys = getKeyArraySorted();
    List<T> result = new ArrayList<>(sortedKeys.length);
    for (int sortedKey : sortedKeys) {
      result.add(get(sortedKey));
    }
    return result;
  }

  private class ValueIterator implements Iterator<T> {

    // Index of the next slot to inspect. hasNext() only skips over empty and tombstoned slots,
    // so it is idempotent and next() is the only method that consumes an element.
    int i = 0;

    @Override
    public boolean hasNext() {
      while (i < keys.length && keys[i] < 0) {
        i++;
      }
      return i < keys.length;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return values[i++];
    }
  }

}
