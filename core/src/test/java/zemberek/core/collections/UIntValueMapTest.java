package zemberek.core.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.logging.Log;

public class UIntValueMapTest {

  @Test
  public void stressTest() {

    List<String> stringSet = randomNumberStrings(100_000);
    UIntValueMap<String> a = new UIntValueMap<>();
    UIntValueMap<String> b = new UIntValueMap<>();

    for (int i = 0; i < 20; i++) {
      for (String s : stringSet) {
        char c = s.charAt(s.length() - 1);
        if (a.contains(s)) {
          a.incrementByAmount(s, 1);
          continue;
        }
        if (b.contains(s)) {
          b.incrementByAmount(s, 1);
          continue;
        }
        if (c % 2 == 0) {
          a.incrementByAmount(s, 1);
        } else {
          b.incrementByAmount(s, 1);
        }
      }
    }
    Log.info("[a] key count = %d ", a.size());
    Log.info("[b] key count = %d ", b.size());

    // a and b cannot have shared keys.
    for (String k : a) {
      Assert.assertFalse(b.contains(k));
    }

  }

  private List<String> randomNumberStrings(int k) {

    List<String> intSet = new ArrayList<>();
    Random rnd = new Random(1);
    while (intSet.size() < k) {
      int r = rnd.nextInt(100_000);
      intSet.add(String.valueOf(r + 1));
    }
    return intSet;
  }

  @Test
  public void missingKeyReadsAsMinusOne() {
    UIntValueMap<String> map = new UIntValueMap<>();
    Assert.assertEquals(-1, map.get("a"));
    Assert.assertFalse(map.contains("a"));
    // Zero is a legal value and must not read back as a missing key.
    map.put("a", 0);
    Assert.assertTrue(map.contains("a"));
    Assert.assertEquals(0, map.get("a"));
    Assert.assertEquals(1, map.size());
  }

  @Test
  public void negativeValuesAreRejected() {
    UIntValueMap<String> map = new UIntValueMap<>();
    try {
      map.put("a", -1);
      Assert.fail("A negative value should have been rejected.");
    } catch (IllegalArgumentException expected) {
      // expected
    }
    Assert.assertEquals(0, map.size());
    Assert.assertFalse(map.contains("a"));
  }

  /**
   * An increment that would take a value below zero must throw and leave the map exactly as it
   * was, both for an existing key and for a key that is not in the map yet.
   */
  @Test
  public void incrementThatGoesNegativeLeavesTheMapUnchanged() {
    UIntValueMap<String> map = new UIntValueMap<>();
    map.put("a", 1);

    try {
      map.incrementByAmount("a", -2);
      Assert.fail("Decrementing below zero should have thrown.");
    } catch (IllegalStateException expected) {
      // expected
    }
    Assert.assertEquals(1, map.get("a"));
    Assert.assertEquals(1, map.size());

    try {
      map.decrement("b");
      Assert.fail("Decrementing a missing key should have thrown.");
    } catch (IllegalStateException expected) {
      // expected
    }
    Assert.assertFalse(map.contains("b"));
    Assert.assertEquals(-1, map.get("b"));
    Assert.assertEquals(1, map.size());
  }

  @Test
  public void incrementAndDecrementWalkTheValue() {
    UIntValueMap<String> map = new UIntValueMap<>();
    Assert.assertEquals(1, map.increment("a"));
    Assert.assertEquals(2, map.increment("a"));
    Assert.assertEquals(5, map.incrementByAmount("a", 3));
    Assert.assertEquals(4, map.decrement("a"));
    Assert.assertEquals(0, map.incrementByAmount("a", -4));
    Assert.assertTrue(map.contains("a"));

    map.incrementAll(Arrays.asList("b", "c", "b"));
    Assert.assertEquals(2, map.get("b"));
    Assert.assertEquals(1, map.get("c"));
    Assert.assertEquals(3, map.size());
  }

  @Test
  public void removeDropsTheKeyAndItsValue() {
    UIntValueMap<String> map = new UIntValueMap<>();
    map.put("a", 1);
    map.put("b", 2);
    Assert.assertEquals("a", map.remove("a"));
    Assert.assertNull(map.remove("a"));
    Assert.assertEquals(-1, map.get("a"));
    Assert.assertEquals(1, map.size());
    map.put("a", 9);
    Assert.assertEquals(9, map.get("a"));
    Assert.assertEquals(2, map.size());
  }

  /**
   * Removals leave tombstones on the probe paths. Every key must stay reachable across them and
   * across the expansion they trigger.
   */
  @Test
  public void keysSurviveTombstonesAndExpansion() {
    UIntValueMap<String> map = new UIntValueMap<>();
    for (int i = 0; i < 1000; i++) {
      map.put("k" + i, i);
    }
    for (int i = 0; i < 1000; i += 2) {
      map.remove("k" + i);
    }
    for (int i = 1000; i < 2000; i++) {
      map.put("k" + i, i);
    }
    Assert.assertEquals(1500, map.size());
    for (int i = 1; i < 2000; i += 2) {
      Assert.assertEquals("k" + i, i, map.get("k" + i));
    }
    for (int i = 0; i < 1000; i += 2) {
      Assert.assertFalse("k" + i, map.contains("k" + i));
    }
    Assert.assertEquals(1500, map.valueArray().length);
    Assert.assertEquals(1500, map.getKeySet().size());
  }

  @Test
  public void valueStatisticsIgnoreRemovedKeys() {
    UIntValueMap<String> map = new UIntValueMap<>();
    Assert.assertEquals(0, map.sumOfValues());
    Assert.assertEquals(0, map.maxValue());
    Assert.assertEquals(Integer.MAX_VALUE, map.minValue());

    map.put("a", 1);
    map.put("b", 5);
    map.put("c", 9);
    map.put("d", 100);
    map.remove("d");

    Assert.assertEquals(15, map.sumOfValues());
    Assert.assertEquals(9, map.maxValue());
    Assert.assertEquals(1, map.minValue());
    Assert.assertEquals(14, map.sumOfValues(2, 10));
    Assert.assertEquals(2, map.sizeLarger(1));
    Assert.assertEquals(2, map.sizeSmaller(9));

    int[] values = map.valueArray();
    Arrays.sort(values);
    Assert.assertArrayEquals(new int[]{1, 5, 9}, values);
  }

  @Test
  public void entriesMatchTheKeysAndValues() {
    UIntValueMap<String> map = new UIntValueMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);
    map.remove("b");

    Set<String> keysFromEntries = new HashSet<>();
    int sum = 0;
    for (IntValueMap.Entry<String> entry : map.iterableEntries()) {
      keysFromEntries.add(entry.key);
      sum += entry.count;
      Assert.assertEquals(entry.count, map.get(entry.key));
    }
    Assert.assertEquals(new HashSet<>(Arrays.asList("a", "c")), keysFromEntries);
    Assert.assertEquals(4, sum);
    Assert.assertEquals(2, map.getAsEntryList().size());
  }

  @Test
  public void entryIteratorStopsAfterTheLastEntry() {
    UIntValueMap<String> map = new UIntValueMap<>();
    map.put("a", 1);
    Iterator<IntValueMap.Entry<String>> iterator = map.entryIterator();
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
    UIntValueMap<String> map = new UIntValueMap<>();
    assertRejectsNullKey(() -> map.get(null));
    assertRejectsNullKey(() -> map.put(null, 1));
    assertRejectsNullKey(() -> map.incrementByAmount(null, 1));
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
