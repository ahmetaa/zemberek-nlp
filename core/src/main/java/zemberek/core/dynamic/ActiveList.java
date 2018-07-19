package zemberek.core.dynamic;

import java.util.Iterator;

/**
 * This is a special set like data structure that can be used in beam-search like algorithms.
 * It holds objects with a {@link Scoreable} interface.
 *
 * When an object is added to the ActiveList, it checks if an equivalent object exists. If not,
 * object is placed in the data structure using linear probing. If an equivalent object exists
 * object with lower score is replaced with the the other one.
 * @param <T>
 */
public class ActiveList<T extends Scoreable> implements Iterable<T> {

  static float DEFAULT_LOAD_FACTOR = 0.65f;

  static int DEFAULT_INITIAL_CAPACITY = 8;

  private T[] hypotheses;

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
    hypotheses = (T[]) new Scoreable[k];
    expandLimit = (int) (k * DEFAULT_LOAD_FACTOR);
    modulo = k - 1;
  }

  private int firstProbe(int hashCode) {
    return hashCode & modulo;
  }

  private int nextProbe(int index) {
    return index & modulo;
  }

  /**
   * Finds either an empty slot location in Hypotheses array or the location of an equivalent
   * Hypothesis. If an empty slot is found, it returns -(slot index)-1, if an equivalent
   * Hypotheses is found, returns equal hypothesis's slot index.
   */
  private int locate(T hyp) {
    int slot = firstProbe(hyp.hashCode());
    while (true) {
      final Scoreable h = hypotheses[slot];
      if (h == null) {
        return (-slot - 1);
      }
      if (h.equals(hyp)) {
        return slot;
      }
      slot = nextProbe(slot + 1);
    }
  }

  /**
   * Adds a new hypothesis to the list.
   **/
  public void add(T hypothesis) {

    int slot = locate(hypothesis);

    // if not exist, add.
    if (slot < 0) {
      slot = -slot - 1;
      hypotheses[slot] = hypothesis;
      size++;
    } else {
      // If exist, check score and if score is better, replace it.
      if (hypotheses[slot].getScore() < hypothesis.getScore()) {
        hypotheses[slot] = hypothesis;
      }
    }
    if (size == expandLimit) {
      expand();
    }
  }

  private void expand() {
    ActiveList<T> expandedList = new ActiveList<>(hypotheses.length * 2);
    // put hypotheses to new list.
    for (int i = 0; i < hypotheses.length; i++) {
      T hyp = hypotheses[i];
      if (hyp == null) {
        continue;
      }
      int slot = firstProbe(hyp.hashCode());
      while (true) {
        final T h = expandedList.hypotheses[slot];
        if (h == null) {
          expandedList.hypotheses[slot] = hyp;
          break;
        }
        slot = nextProbe(slot + 1);
      }
    }
    this.modulo = expandedList.modulo;
    this.expandLimit = expandedList.expandLimit;
    this.hypotheses = expandedList.hypotheses;
  }

  public T getBest() {
    T best = null;
    for (T hypothesis : hypotheses) {
      if (hypothesis == null) {
        continue;
      }
      if (best == null || hypothesis.getScore() > best.getScore()) {
        best = hypothesis;
      }
    }
    return best;
  }

  @Override
  public Iterator<T> iterator() {
    return new HIterator();
  }

  class HIterator implements Iterator<T> {

    int pointer = 0;
    int count = 0;
    T current;

    @Override
    public boolean hasNext() {
      if (count == size) {
        return false;
      }
      while (hypotheses[pointer] == null) {
        pointer++;
      }
      current = hypotheses[pointer];
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
