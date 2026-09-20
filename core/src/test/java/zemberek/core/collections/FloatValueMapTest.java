package zemberek.core.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class FloatValueMapTest {

  @Test
  public void testValues() {
    FloatValueMap<String> set = new FloatValueMap<>();
    set.set("a", 7);
    set.set("b", 2);
    set.set("c", 3);
    set.set("d", 4);
    set.set("d", 5); // overwrite

    Assert.assertEquals(4, set.size());
    float[] values = set.values();
    Arrays.sort(values);
    Assert.assertTrue(Arrays.equals(new float[]{2f, 3f, 5f, 7f}, values));

  }

  @Test
  public void missingKeyReadsAsZero() {
    FloatValueMap<String> map = new FloatValueMap<>();
    Assert.assertEquals(0f, map.get("a"), 0f);
    Assert.assertFalse(map.contains("a"));
    // Zero is also a legal value, so get() alone can not tell the two apart.
    map.set("a", 0f);
    Assert.assertTrue(map.contains("a"));
    Assert.assertEquals(1, map.size());
    Assert.assertEquals(0f, map.get("a"), 0f);
  }

  @Test
  public void incrementByAmountInsertsAndAccumulates() {
    FloatValueMap<String> map = new FloatValueMap<>();
    Assert.assertEquals(3f, map.incrementByAmount("a", 3f), 0f);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals(5f, map.incrementByAmount("a", 2f), 0f);
    Assert.assertEquals(1f, map.incrementByAmount("a", -4f), 0f);
    Assert.assertEquals(1f, map.get("a"), 0f);
    // Negative values are allowed in this map.
    Assert.assertEquals(-2f, map.incrementByAmount("a", -3f), 0f);
  }

  @Test
  public void removeDropsTheKeyAndTheValue() {
    FloatValueMap<String> map = new FloatValueMap<>();
    map.set("a", 1f);
    map.set("b", 2f);
    Assert.assertEquals("a", map.remove("a"));
    Assert.assertNull(map.remove("a"));
    Assert.assertFalse(map.contains("a"));
    Assert.assertEquals(0f, map.get("a"), 0f);
    Assert.assertEquals(1, map.size());
    // Re-inserting must reuse the tombstone, not resurrect the old value.
    map.set("a", 9f);
    Assert.assertEquals(9f, map.get("a"), 0f);
    Assert.assertEquals(2, map.size());
  }

  /**
   * Removals leave tombstones on the probe paths. Every key must stay reachable across them and
   * across the expansion they trigger.
   */
  @Test
  public void keysSurviveTombstonesAndExpansion() {
    FloatValueMap<String> map = new FloatValueMap<>();
    for (int i = 0; i < 1000; i++) {
      map.set("k" + i, i);
    }
    for (int i = 0; i < 1000; i += 2) {
      map.remove("k" + i);
    }
    for (int i = 1000; i < 2000; i++) {
      map.set("k" + i, i);
    }
    Assert.assertEquals(1500, map.size());
    for (int i = 1; i < 2000; i += 2) {
      Assert.assertEquals("k" + i, i, map.get("k" + i), 0f);
    }
    for (int i = 0; i < 1000; i += 2) {
      Assert.assertFalse("k" + i, map.contains("k" + i));
    }
    Assert.assertEquals(1500, map.values().length);
    Assert.assertEquals(1500, map.getKeySet().size());
  }

  @Test
  public void copyIsAnIndependentSnapshot() {
    FloatValueMap<String> map = new FloatValueMap<>();
    map.set("a", 1f);
    map.set("b", 2f);
    FloatValueMap<String> copy = map.copy();

    Assert.assertEquals(map.getKeySet(), copy.getKeySet());
    Assert.assertEquals(1f, copy.get("a"), 0f);

    copy.set("a", 100f);
    copy.set("c", 3f);
    map.remove("b");

    Assert.assertEquals(1f, map.get("a"), 0f);
    Assert.assertFalse(map.contains("c"));
    Assert.assertEquals(2f, copy.get("b"), 0f);
    Assert.assertEquals(3, copy.size());
    Assert.assertEquals(1, map.size());
  }

  @Test
  public void entriesMatchTheKeysAndValues() {
    FloatValueMap<String> map = new FloatValueMap<>();
    map.set("a", 1f);
    map.set("b", 2f);
    map.set("c", 3f);
    map.remove("b");

    Set<String> keysFromEntries = new HashSet<>();
    float sum = 0;
    for (FloatValueMap.Entry<String> entry : map.iterableEntries()) {
      keysFromEntries.add(entry.key);
      sum += entry.value;
      Assert.assertEquals(entry.value, map.get(entry.key), 0f);
    }
    Assert.assertEquals(new HashSet<>(Arrays.asList("a", "c")), keysFromEntries);
    Assert.assertEquals(4f, sum, 0f);
    Assert.assertEquals(2, map.getAsEntryList().size());

    List<String> keys = new ArrayList<>(map.getKeyList());
    keys.sort(String::compareTo);
    Assert.assertEquals(Arrays.asList("a", "c"), keys);
  }

  @Test
  public void entryIteratorStopsAfterTheLastEntry() {
    FloatValueMap<String> map = new FloatValueMap<>();
    map.set("a", 1f);
    Iterator<FloatValueMap.Entry<String>> iterator = map.entryIterator();
    // hasNext must not consume anything.
    Assert.assertTrue(iterator.hasNext());
    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals("a", iterator.next().key);
    Assert.assertFalse(iterator.hasNext());
    try {
      iterator.next();
      Assert.fail("Exhausted iterator should have thrown.");
    } catch (NoSuchElementException expected) {
      // expected
    }
  }

  @Test
  public void nullKeysAreRejected() {
    FloatValueMap<String> map = new FloatValueMap<>();
    assertRejectsNullKey(() -> map.get(null));
    assertRejectsNullKey(() -> map.set(null, 1f));
    assertRejectsNullKey(() -> map.incrementByAmount(null, 1f));
  }

  private void assertRejectsNullKey(Runnable runnable) {
    try {
      runnable.run();
      Assert.fail("A null key should have been rejected.");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }
}
