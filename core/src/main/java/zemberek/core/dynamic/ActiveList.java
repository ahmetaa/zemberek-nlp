package zemberek.core.dynamic;

import java.util.Iterator;

/**
 * This is a special set like data structure that can be used in beam-search like algorithms. It
 * holds objects with a {@link Scorable} interface.
 *
 * When an object is added to the ActiveList, it checks if an equivalent object exists. If not,
 * object is placed in the data structure using linear probing. If an equivalent object exists
 * object with lower score is replaced with the the other one.
 */
public class ActiveList<T extends Scorable> implements Iterable<T> {

  static float DEFAULT_LOAD_FACTOR = 0.55f;

  static int DEFAULT_INITIAL_CAPACITY = 8;

  private T[] items;

  private int modulo;
  private int size;
  private int expandLimit;

  public ActiveList() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  public ActiveList(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Size must be a positive value. But it is " + size);
    }
    int k = 1;
    while (k < size) {
      k <<= 1;
    }
    items = (T[]) new Scorable[k];
    expandLimit = (int) (k * DEFAULT_LOAD_FACTOR);
    modulo = k - 1;
  }

  private int rehash(int key) {
    final int h = key * 0x9E3779B9;
    return (h ^ (h >> 16)) & 0x7fff_ffff;
  }

  /**
   * Finds either an empty slot location in Hypotheses array or the location of an equivalent item.
   * If an empty slot is found, it returns -(slot index)-1, if an equivalent item is found, returns
   * equal item's slot index.
   */
  private int locate(T t) {
    int slot = rehash(t.hashCode()) & modulo;
    while (true) {
      final Scorable h = items[slot];
      if (h == null) {
        return (-slot - 1);
      }
      if (h.equals(t)) {
        return slot;
      }
      slot = (slot + 1) & modulo;
    }
  }

  /**
   * Adds a new scorable to the list.
   **/
  public void add(T t) {

    int slot = locate(t);

    // if not exist, add.
    if (slot < 0) {
      slot = -slot - 1;
      items[slot] = t;
      size++;
    } else {
      // If exist, check score and if score is better, replace it.
      if (items[slot].getScore() < t.getScore()) {
        items[slot] = t;
      }
    }
    if (size == expandLimit) {
      expand();
    }
  }

  private void expand() {
    ActiveList<T> expandedList = new ActiveList<>(items.length * 2);
    // put items to new list.
    for (int i = 0; i < items.length; i++) {
      T t = items[i];
      if (t == null) {
        continue;
      }
      int slot = rehash(t.hashCode()) & modulo;
      while (true) {
        final T h = expandedList.items[slot];
        if (h == null) {
          expandedList.items[slot] = t;
          break;
        }
        slot = (slot + 1) & modulo;
      }
    }
    this.modulo = expandedList.modulo;
    this.expandLimit = expandedList.expandLimit;
    this.items = expandedList.items;
  }

  public T getBest() {
    T best = null;
    for (T t : items) {
      if (t == null) {
        continue;
      }
      if (best == null || t.getScore() > best.getScore()) {
        best = t;
      }
    }
    return best;
  }

  @Override
  public Iterator<T> iterator() {
    return new TIterator();
  }

  class TIterator implements Iterator<T> {

    int pointer = 0;
    int count = 0;
    T current;

    @Override
    public boolean hasNext() {
      if (count == size) {
        return false;
      }
      while (items[pointer] == null) {
        pointer++;
      }
      current = items[pointer];
      count++;
      pointer++;
      return true;
    }

    @Override
    public T next() {
      return current;
    }
  }
}
