package zemberek.core.collections;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class LongUIntMapTest {

  @Test
  public void testKeyArray() {
    LongUIntMap map = new LongUIntMap();
    map.put(100L, 1);
    map.put(200L, 2);
    map.put(300L, 3);

    Assert.assertEquals(3, map.size());
    long[] keys = map.keyArray();
    Assert.assertEquals(3, keys.length);
    Arrays.sort(keys);
    Assert.assertArrayEquals(new long[]{100L, 200L, 300L}, keys);
  }

  @Test
  public void testCopyOfValues() {
    LongUIntMap map = new LongUIntMap();
    map.put(100L, 10);
    map.put(200L, 20);
    map.put(300L, 30);

    int[] values = map.copyOfValues();
    Assert.assertEquals(3, values.length);
    Arrays.sort(values);
    Assert.assertArrayEquals(new int[]{10, 20, 30}, values);
  }

  @Test
  public void testPutGetRemove() {
    LongUIntMap map = new LongUIntMap();
    map.put(1L, 10);
    map.put(-1L, 20);
    map.put(Long.MAX_VALUE, 30);
    map.put(Long.MIN_VALUE, 40);

    Assert.assertEquals(4, map.size());
    Assert.assertEquals(10, map.get(1L));
    Assert.assertEquals(20, map.get(-1L));
    Assert.assertEquals(30, map.get(Long.MAX_VALUE));
    Assert.assertEquals(40, map.get(Long.MIN_VALUE));

    Assert.assertTrue(map.containsKey(1L));
    Assert.assertTrue(map.containsKey(-1L));
    Assert.assertFalse(map.containsKey(999L));

    // Update existing
    map.put(1L, 15);
    Assert.assertEquals(4, map.size());
    Assert.assertEquals(15, map.get(1L));

    // Remove
    map.remove(1L);
    Assert.assertEquals(3, map.size());
    Assert.assertFalse(map.containsKey(1L));
    Assert.assertEquals(-1, map.get(1L));

    // Re-insert removed
    map.put(1L, 50);
    Assert.assertEquals(4, map.size());
    Assert.assertEquals(50, map.get(1L));
  }

  @Test
  public void testIncrementAndDecrement() {
    LongUIntMap map = new LongUIntMap();
    Assert.assertEquals(1, map.increment(5L));
    Assert.assertEquals(3, map.incrementByAmount(5L, 2));
    Assert.assertEquals(2, map.decrement(5L));
    Assert.assertEquals(1, map.decrement(5L));
    Assert.assertEquals(0, map.decrement(5L));

    try {
      map.decrement(5L);
      Assert.fail("Should have thrown IllegalStateException on negative value");
    } catch (IllegalStateException e) {
      // Expected
    }

    try {
      map.decrement(999L);
      Assert.fail("Should have thrown IllegalStateException on decrementing non-existent key");
    } catch (IllegalStateException e) {
      // Expected
    }
  }

  @Test
  public void testTombstoneReuse() {
    LongUIntMap map = new LongUIntMap(8);
    for (long i = 0; i < 4; i++) {
      map.put(i, (int) i);
    }
    Assert.assertEquals(4, map.size());

    // Remove 2 elements
    map.remove(1L);
    map.remove(2L);
    Assert.assertEquals(2, map.size());

    // Insert new elements (should reuse tombstones)
    map.put(10L, 100);
    map.put(20L, 200);
    Assert.assertEquals(4, map.size());
    Assert.assertEquals(100, map.get(10L));
    Assert.assertEquals(200, map.get(20L));
    Assert.assertEquals(-1, map.get(1L));
    Assert.assertEquals(-1, map.get(2L));
  }

  @Test
  public void testConstructors() {
    LongUIntMap map = new LongUIntMap(1);
    Assert.assertEquals(0, map.size());
    map.put(1L, 10);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals(10, map.get(1L));

    try {
      new LongUIntMap(0);
      Assert.fail("Should have thrown IllegalArgumentException for capacity 0");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void testExpansion() {
    LongUIntMap map = new LongUIntMap(4);
    for (int i = 0; i < 1000; i++) {
      map.put(i * 10L, i);
    }
    Assert.assertEquals(1000, map.size());
    for (int i = 0; i < 1000; i++) {
      Assert.assertEquals(i, map.get(i * 10L));
    }

    long[] keys = map.keyArray();
    Assert.assertEquals(1000, keys.length);
    Arrays.sort(keys);
    for (int i = 0; i < 1000; i++) {
      Assert.assertEquals(i * 10L, keys[i]);
    }
  }

}
