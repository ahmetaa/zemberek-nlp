package zemberek.core.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tombstone accounting and expansion sizing tests shared by the linear probing hash
 * implementations. Removing a key leaves a tombstone behind, and reusing that slot for a new key
 * must give the tombstone budget back. Expansion must also size the new table from the live key
 * count, otherwise a table that is mostly tombstones keeps doubling instead of being rehashed at
 * the same size.
 */
public class HashTombstoneTest {

  private static final int CHURN = 100_000;

  @SuppressWarnings("rawtypes")
  private static int capacity(HashBase h) {
    return h.keys.length;
  }

  private static void assertCapacity(String name, int capacity, int limit) {
    Assert.assertTrue(
        name + " capacity grew to " + capacity + ", expected at most " + limit,
        capacity <= limit);
  }

  @Test
  public void transientKeysDoNotGrowTheTable() {
    UIntSet a = new UIntSet();
    for (int i = 0; i < CHURN; i++) {
      a.add(i);
      a.remove(i);
    }
    assertCapacity("UIntSet", a.keys.length, 16);

    UIntMap<String> b = new UIntMap<>();
    for (int i = 0; i < CHURN; i++) {
      b.put(i, "v");
      b.remove(i);
    }
    assertCapacity("UIntMap", b.keys.length, 16);

    UIntValueMap<String> c = new UIntValueMap<>();
    for (int i = 0; i < CHURN; i++) {
      c.put("k" + i, 1);
      c.remove("k" + i);
    }
    assertCapacity("UIntValueMap", capacity(c), 16);

    IntValueMap<String> d = new IntValueMap<>();
    for (int i = 0; i < CHURN; i++) {
      d.put("k" + i, 1);
      d.remove("k" + i);
    }
    assertCapacity("IntValueMap", capacity(d), 16);

    IntValueMap<String> e = new IntValueMap<>();
    for (int i = 0; i < CHURN; i++) {
      e.incrementByAmount("k" + i, 1);
      e.remove("k" + i);
    }
    assertCapacity("IntValueMap.incrementByAmount", capacity(e), 16);

    FloatValueMap<String> f = new FloatValueMap<>();
    for (int i = 0; i < CHURN; i++) {
      f.set("k" + i, 1f);
      f.remove("k" + i);
    }
    assertCapacity("FloatValueMap", capacity(f), 16);

    LookupSet<String> g = new LookupSet<>();
    for (int i = 0; i < CHURN; i++) {
      g.add("k" + i);
      g.remove("k" + i);
    }
    assertCapacity("LookupSet", capacity(g), 16);
  }

  @Test
  public void stableWorkingSetDoesNotGrowTheTable() {
    int live = 1000;
    int limit = 8192;

    UIntSet a = new UIntSet();
    for (int i = 0; i < live; i++) {
      a.add(i);
    }
    for (int i = 0; i < CHURN; i++) {
      a.add(live + i);
      a.remove(live + i);
    }
    Assert.assertEquals(live, a.size());
    assertCapacity("UIntSet", a.keys.length, limit);

    UIntMap<String> b = new UIntMap<>();
    for (int i = 0; i < live; i++) {
      b.put(i, "v");
    }
    for (int i = 0; i < CHURN; i++) {
      b.put(live + i, "v");
      b.remove(live + i);
    }
    Assert.assertEquals(live, b.size());
    assertCapacity("UIntMap", b.keys.length, limit);

    IntValueMap<String> c = new IntValueMap<>();
    for (int i = 0; i < live; i++) {
      c.put("k" + i, 1);
    }
    for (int i = 0; i < CHURN; i++) {
      c.put("x" + i, 1);
      c.remove("x" + i);
    }
    Assert.assertEquals(live, c.size());
    assertCapacity("IntValueMap", capacity(c), limit);

    LookupSet<String> d = new LookupSet<>();
    for (int i = 0; i < live; i++) {
      d.add("k" + i);
    }
    for (int i = 0; i < CHURN; i++) {
      d.add("x" + i);
      d.remove("x" + i);
    }
    Assert.assertEquals(live, d.size());
    assertCapacity("LookupSet", capacity(d), limit);
  }

  @Test
  public void reusingATombstoneGivesTheRemoveBudgetBack() {
    IntValueMap<String> map = new IntValueMap<>();
    LookupSet<String> set = new LookupSet<>();
    UIntSet uset = new UIntSet();
    for (int i = 0; i < 100; i++) {
      map.put("key", i);
      map.remove("key");
      map.put("key", i);
      Assert.assertEquals("IntValueMap leaked a tombstone", 0, map.removeCount);

      set.add("key");
      set.remove("key");
      set.add("key");
      Assert.assertEquals("LookupSet leaked a tombstone", 0, set.removeCount);

      uset.add(7);
      uset.remove(7);
      uset.add(7);
      Assert.assertEquals("UIntSet leaked a tombstone", 0, uset.removeCount);
    }
  }

  @Test
  public void heavyChurnKeepsContentsCorrect() {
    Random random = new Random(1);
    IntValueMap<String> map = new IntValueMap<>();
    Map<String, Integer> reference = new HashMap<>();

    for (int i = 0; i < 200_000; i++) {
      String key = "k" + random.nextInt(2000);
      if (random.nextBoolean()) {
        int value = random.nextInt(1000);
        map.put(key, value);
        reference.put(key, value);
      } else {
        map.remove(key);
        reference.remove(key);
      }
      Assert.assertEquals(reference.size(), map.size());
    }
    for (Map.Entry<String, Integer> e : reference.entrySet()) {
      Assert.assertEquals((int) e.getValue(), map.get(e.getKey()));
    }
    assertCapacity("IntValueMap", capacity(map), 8192);
  }

  @Test
  public void constructorRejectsSizeAboveMaxCapacity() {
    int tooBig = HashBase.MAX_CAPACITY + 1;
    assertRejects(() -> new UIntSet(tooBig));
    assertRejects(() -> new UIntMap<String>(tooBig));
    assertRejects(() -> new UIntValueMap<String>(tooBig));
    assertRejects(() -> new IntValueMap<String>(tooBig));
    assertRejects(() -> new FloatValueMap<String>(tooBig));
    assertRejects(() -> new LookupSet<String>(tooBig));
    assertRejects(() -> new UIntSet(Integer.MAX_VALUE));
    assertRejects(() -> new IntValueMap<String>(Integer.MAX_VALUE));
  }

  private static void assertRejects(Runnable constructor) {
    try {
      constructor.run();
      Assert.fail("Expected IllegalArgumentException for an oversize table.");
    } catch (IllegalArgumentException expected) {
      // expected.
    }
  }
}
