package zemberek.core.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Capacity, iterator and construction contracts of the primitive hash tables: capacity must track
 * the live key count under put/remove churn, iterators must not consume elements in
 * {@code hasNext()}, and a zero capacity table must be usable.
 */
public class CompactMapChurnTest {

  /**
   * A map that is emptied again and again must not keep the capacity it once needed. Deleted
   * slots have to be reused, and the capacity of an empty map has to fall back to the default.
   */
  @Test
  public void intIntMapDoesNotGrowWhenKeysAreRemovedAgain() {
    IntIntMap map = new IntIntMap();
    for (int i = 0; i < 100_000; i++) {
      map.put(i, i);
      map.remove(i);
    }
    Assert.assertEquals(0, map.size());
    Assert.assertEquals(4, map.capacity());
  }

  @Test
  public void intFloatMapDoesNotGrowWhenKeysAreRemovedAgain() {
    IntFloatMap map = new IntFloatMap();
    for (int i = 0; i < 100_000; i++) {
      map.put(i, i);
      map.remove(i);
    }
    Assert.assertEquals(0, map.size());
    Assert.assertEquals(4, map.capacity());
  }

  @Test
  public void longUIntMapDoesNotGrowWhenKeysAreRemovedAgain() {
    LongUIntMap map = new LongUIntMap();
    for (int i = 0; i < 100_000; i++) {
      map.put(i, i);
      map.remove(i);
    }
    Assert.assertEquals(0, map.size());
    Assert.assertEquals(8, map.capacity());
  }

  /**
   * Capacity of a map under churn must track the live key count, not the total number of inserts
   * it has ever seen.
   */
  @Test
  public void capacityTracksLiveKeyCountUnderSteadyStateChurn() {
    int live = 1000;
    IntIntMap intMap = new IntIntMap();
    LongUIntMap longMap = new LongUIntMap();
    for (int i = 0; i < live; i++) {
      intMap.put(i, i);
      longMap.put(i, i);
    }
    for (int i = live; i < 200_000; i++) {
      intMap.put(i, i);
      intMap.remove(i - live);
      longMap.put(i, i);
      longMap.remove(i - live);
    }
    Assert.assertEquals(live, intMap.size());
    Assert.assertEquals(live, longMap.size());
    // A generous bound: the table only has to stay proportional to the live key count.
    Assert.assertTrue("capacity was " + intMap.capacity(), intMap.capacity() <= live * 16);
    Assert.assertTrue("capacity was " + longMap.capacity(), longMap.capacity() <= live * 16);
  }

  /**
   * Removing a key must not corrupt the values of the keys that share its probe path. Removal
   * writes the DELETED marker into the key half of the slot, and negative keys make the halves of
   * the packed entry easy to mix up.
   */
  @Test
  public void removingNegativeKeysKeepsOtherValuesIntact() {
    IntIntMap map = new IntIntMap();
    for (int i = -50; i < 0; i++) {
      map.put(i, i * 100);
    }
    for (int i = -50; i < 0; i += 2) {
      map.remove(i);
    }
    for (int i = -49; i < 0; i += 2) {
      Assert.assertEquals(i * 100, map.get(i));
    }
  }

  @Test
  public void mapsCanBeCreatedWithZeroCapacity() {
    Assert.assertEquals(0, new IntIntMap(0).size());
    Assert.assertEquals(0, new IntFloatMap(0).size());
    Assert.assertEquals(0, new IntMap<String>(0).size());
    Assert.assertEquals(0, new LongUIntMap(0).size());
    Assert.assertEquals(0, new UIntSet(0).size());
    Assert.assertEquals(0, new UIntMap<String>(0).size());
    Assert.assertEquals(0, new UIntValueMap<String>(0).size());
    Assert.assertEquals(0, new IntValueMap<String>(0).size());
    Assert.assertEquals(0, new FloatValueMap<String>(0).size());
    Assert.assertEquals(0, new LookupSet<String>(0).size());
    Assert.assertEquals(0, UIntSet.of().size());
    Assert.assertEquals(0, new Histogram<String>(0).size());

    // They must be usable, not just constructible.
    IntIntMap map = new IntIntMap(0);
    for (int i = 0; i < 100; i++) {
      map.put(i, i);
    }
    Assert.assertEquals(100, map.size());
    Assert.assertEquals(42, map.get(42));
  }

  @Test
  public void negativeCapacityIsStillRejected() {
    assertRejects(() -> new IntIntMap(-1));
    assertRejects(() -> new IntMap<String>(-1));
    assertRejects(() -> new LongUIntMap(-1));
    assertRejects(() -> new UIntSet(-1));
    assertRejects(() -> new IntValueMap<String>(-1));
    assertRejects(() -> new IntVector(-1));
  }

  @Test
  public void intVectorWithZeroCapacityCanGrow() {
    IntVector vector = new IntVector(0);
    for (int i = 0; i < 100; i++) {
      vector.add(i);
    }
    Assert.assertEquals(100, vector.size());
    Assert.assertEquals(99, vector.get(99));

    IntVector other = new IntVector(0);
    other.addAll(new int[]{1, 2, 3});
    other.add(4);
    Assert.assertArrayEquals(new int[]{1, 2, 3, 4}, other.copyOf());
  }

  /**
   * {@code hasNext()} must not consume elements: calling it more than once per element must
   * report the same thing and must not skip entries.
   */
  @Test
  public void iteratorHasNextIsIdempotent() {
    LookupSet<String> set = new LookupSet<>();
    for (int i = 0; i < 5; i++) {
      set.add("k" + i);
    }
    Assert.assertEquals(5, countWithRepeatedHasNext(set.iterator()));

    UIntMap<String> uIntMap = new UIntMap<>();
    for (int i = 0; i < 5; i++) {
      uIntMap.put(i, "v" + i);
    }
    Assert.assertEquals(5, countWithRepeatedHasNext(uIntMap.iterator()));
    Assert.assertEquals(5, uIntMap.getValues().size());

    IntValueMap<String> valueMap = new IntValueMap<>();
    for (int i = 0; i < 5; i++) {
      valueMap.put("k" + i, i);
    }
    Assert.assertEquals(5, countWithRepeatedHasNext(valueMap.iterator()));
  }

  @Test
  public void iteratorsSkipRemovedKeys() {
    LookupSet<String> set = new LookupSet<>();
    for (int i = 0; i < 20; i++) {
      set.add("k" + i);
    }
    for (int i = 0; i < 20; i += 2) {
      set.remove("k" + i);
    }
    List<String> seen = new ArrayList<>();
    for (String s : set) {
      seen.add(s);
    }
    Assert.assertEquals(10, seen.size());
    Assert.assertEquals(10, set.size());
  }

  @Test
  public void exhaustedIteratorsThrowNoSuchElement() {
    LookupSet<String> set = new LookupSet<>();
    set.add("a");
    assertExhausts(set.iterator());
    UIntMap<String> uIntMap = new UIntMap<>();
    uIntMap.put(1, "a");
    assertExhausts(uIntMap.iterator());
    IntValueMap<String> valueMap = new IntValueMap<>();
    valueMap.put("a", 1);
    assertExhausts(valueMap.iterableEntries().iterator());
  }

  private static void assertExhausts(Iterator<?> iterator) {
    Assert.assertTrue(iterator.hasNext());
    iterator.next();
    Assert.assertFalse(iterator.hasNext());
    try {
      iterator.next();
      Assert.fail("Expected NoSuchElementException from an exhausted iterator.");
    } catch (NoSuchElementException expected) {
      // expected.
    }
  }

  private static int countWithRepeatedHasNext(Iterator<?> iterator) {
    int count = 0;
    while (true) {
      // Two calls in a row must report the same thing and consume nothing.
      boolean first = iterator.hasNext();
      Assert.assertEquals(first, iterator.hasNext());
      if (!first) {
        return count;
      }
      Assert.assertNotNull(iterator.next());
      count++;
    }
  }

  private static void assertRejects(Runnable constructor) {
    try {
      constructor.run();
      Assert.fail("Expected IllegalArgumentException for a negative capacity.");
    } catch (IllegalArgumentException expected) {
      // expected.
    }
  }
}
