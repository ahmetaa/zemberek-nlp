package zemberek.core.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * Randomized differential tests. Every collection in this package is driven with a random
 * operation sequence and compared against the equivalent java.util collection after every single
 * operation, then compared once more in full when the sequence ends.
 *
 * <p>Key spaces are deliberately small so that hash collisions, tombstone reuse, probe sequences
 * that run over deleted slots and table expansion all happen often. Seeds are fixed so that a
 * failure is reproducible, and every failure message carries the seed and the operation index
 * that produced it.
 */
public class CollectionsFuzzTest {

  private static final int[] SEEDS = {1, 13, 97, 2026, 31337};
  private static final int OPERATIONS = 20_000;
  // Small enough that the same key is hit again and again.
  private static final int KEY_SPACE = 400;

  @Test
  public void intIntMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      IntIntMap map = new IntIntMap();
      Map<Integer, Integer> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        // Negative keys are legal here, only EMPTY and DELETED are not.
        int key = random.nextInt(KEY_SPACE) - KEY_SPACE / 2;
        switch (random.nextInt(5)) {
          case 0:
            int value = smallValue(random);
            map.put(key, value);
            reference.put(key, value);
            break;
          case 1:
            int amount = random.nextInt(21) - 10;
            map.increment(key, amount);
            reference.merge(key, amount, Integer::sum);
            break;
          case 2:
            map.remove(key);
            reference.remove(key);
            break;
          case 3:
            check(reference.containsKey(key) == map.containsKey(key), seed, op,
                "containsKey(" + key + ")");
            break;
          default:
            Integer expected = reference.get(key);
            checkEquals(expected == null ? IntIntMap.NO_RESULT : expected, map.get(key),
                seed, op, "get(" + key + ")");
        }
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      assertSameKeys(seed, reference.keySet(), map.getKeys());
      assertSameInts(seed, reference.values(), map.getValues());
      for (Map.Entry<Integer, Integer> e : reference.entrySet()) {
        checkEquals(e.getValue(), map.get(e.getKey()), seed, -1, "final get(" + e.getKey() + ")");
      }
      // Pairs must carry the same mapping the map reports one key at a time.
      checkEquals(reference.size(), map.getAsPairs().length, seed, -1, "pair count");
      for (zemberek.core.IntPair pair : map.getAsPairs()) {
        checkEquals(reference.get(pair.first), pair.second, seed, -1, "pair " + pair.first);
      }
    }
  }

  @Test
  public void intFloatMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      IntFloatMap map = new IntFloatMap();
      Map<Integer, Float> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        int key = random.nextInt(KEY_SPACE) - KEY_SPACE / 2;
        switch (random.nextInt(5)) {
          case 0:
            // Whole numbers keep float addition exact, so values can be compared without a delta.
            float value = random.nextInt(1000);
            map.put(key, value);
            reference.put(key, value);
            break;
          case 1:
            float amount = random.nextInt(21) - 10;
            map.increment(key, amount);
            reference.merge(key, amount, Float::sum);
            break;
          case 2:
            map.remove(key);
            reference.remove(key);
            break;
          case 3:
            check(reference.containsKey(key) == map.containsKey(key), seed, op,
                "containsKey(" + key + ")");
            break;
          default:
            Float expected = reference.get(key);
            Assert.assertEquals(where(seed, op, "get(" + key + ")"),
                expected == null ? IntFloatMap.NO_RESULT : expected, map.get(key), 0f);
        }
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      assertSameKeys(seed, reference.keySet(), map.getKeys());
      float[] values = map.getValues();
      Arrays.sort(values);
      float[] expectedValues = new float[reference.size()];
      int i = 0;
      for (float v : reference.values()) {
        expectedValues[i++] = v;
      }
      Arrays.sort(expectedValues);
      Assert.assertArrayEquals(where(seed, -1, "values"), expectedValues, values, 0f);
    }
  }

  @Test
  public void intMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      IntMap<String> map = new IntMap<>();
      Map<Integer, String> reference = new HashMap<>();

      // IntMap has no remove, so it only ever grows.
      for (int op = 0; op < OPERATIONS; op++) {
        int key = random.nextInt(KEY_SPACE) - KEY_SPACE / 2;
        switch (random.nextInt(3)) {
          case 0:
            String value = "v" + random.nextInt(1000);
            check(map.put(key, value), seed, op, "put(" + key + ") was refused");
            reference.put(key, value);
            break;
          case 1:
            check(reference.containsKey(key) == map.containsKey(key), seed, op,
                "containsKey(" + key + ")");
            break;
          default:
            Assert.assertEquals(where(seed, op, "get(" + key + ")"),
                reference.get(key), map.get(key));
        }
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      assertSameKeys(seed, reference.keySet(), map.getKeys());
      Assert.assertEquals(where(seed, -1, "values"),
          new HashSet<>(reference.values()), new HashSet<>(map.getValues()));
    }
  }

  /**
   * An externally managed {@link IntMap} refuses to expand on its own: put returns false when the
   * table is full and the caller has to swap in the expanded copy.
   */
  @Test
  public void managedIntMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      IntMap<String> map = IntMap.createManaged();
      Map<Integer, String> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        int key = random.nextInt(KEY_SPACE) - KEY_SPACE / 2;
        String value = "v" + random.nextInt(1000);
        if (!map.put(key, value)) {
          map = map.expand();
          check(map.put(key, value), seed, op, "put after expand was refused");
        }
        reference.put(key, value);
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      for (Map.Entry<Integer, String> e : reference.entrySet()) {
        Assert.assertEquals(where(seed, -1, "get(" + e.getKey() + ")"),
            e.getValue(), map.get(e.getKey()));
      }
    }
  }

  @Test
  public void longUIntMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      LongUIntMap map = new LongUIntMap();
      Map<Long, Integer> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        long key = randomLongKey(random);
        switch (random.nextInt(6)) {
          case 0:
            int value = random.nextInt(1000);
            map.put(key, value);
            reference.put(key, value);
            break;
          case 1:
            int amount = random.nextInt(10);
            checkEquals(reference.merge(key, amount, Integer::sum),
                map.incrementByAmount(key, amount), seed, op, "incrementByAmount(" + key + ")");
            break;
          case 2:
            // decrement throws for a missing key and for a value that would go negative.
            Integer current = reference.get(key);
            if (current != null && current > 0) {
              reference.put(key, current - 1);
              checkEquals(current - 1, map.decrement(key), seed, op, "decrement(" + key + ")");
            }
            break;
          case 3:
            map.remove(key);
            reference.remove(key);
            break;
          case 4:
            check(reference.containsKey(key) == map.containsKey(key), seed, op,
                "containsKey(" + key + ")");
            break;
          default:
            Integer expected = reference.get(key);
            checkEquals(expected == null ? -1 : expected, map.get(key), seed, op,
                "get(" + key + ")");
        }
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      long[] keys = map.keyArray();
      checkEquals(reference.size(), keys.length, seed, -1, "key array length");
      Set<Long> keySet = new HashSet<>();
      for (long key : keys) {
        check(keySet.add(key), seed, -1, "duplicate key " + key);
      }
      Assert.assertEquals(where(seed, -1, "keys"), reference.keySet(), keySet);
      assertSameInts(seed, reference.values(), map.copyOfValues());
    }
  }

  @Test
  public void uIntSetFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      UIntSet set = new UIntSet();
      Set<Integer> reference = new HashSet<>();

      for (int op = 0; op < OPERATIONS; op++) {
        int key = random.nextInt(KEY_SPACE);
        switch (random.nextInt(3)) {
          case 0:
            checkEquals(reference.add(key) ? 1 : 0, set.add(key) ? 1 : 0, seed, op,
                "add(" + key + ")");
            break;
          case 1:
            set.remove(key);
            reference.remove(key);
            break;
          default:
            check(reference.contains(key) == set.contains(key), seed, op, "contains(" + key + ")");
        }
        checkEquals(reference.size(), set.size(), seed, op, "size");
      }

      assertSameKeys(seed, reference, set.getKeys());
      Assert.assertEquals(where(seed, -1, "sorted keys"),
          new ArrayList<>(new TreeSet<>(reference)), set.getKeysSorted());
    }
  }

  @Test
  public void uIntMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      UIntMap<String> map = new UIntMap<>();
      Map<Integer, String> reference = new TreeMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        int key = random.nextInt(KEY_SPACE);
        switch (random.nextInt(4)) {
          case 0:
            String value = "v" + random.nextInt(1000);
            map.put(key, value);
            reference.put(key, value);
            break;
          case 1:
            map.remove(key);
            reference.remove(key);
            break;
          case 2:
            check(reference.containsKey(key) == map.containsKey(key), seed, op,
                "containsKey(" + key + ")");
            break;
          default:
            Assert.assertEquals(where(seed, op, "get(" + key + ")"),
                reference.get(key), map.get(key));
        }
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      assertSameKeys(seed, reference.keySet(), map.getKeys());
      Assert.assertEquals(where(seed, -1, "values sorted by key"),
          new ArrayList<>(reference.values()), map.getValuesSortedByKey());
      List<String> iterated = new ArrayList<>();
      for (String value : map) {
        iterated.add(value);
      }
      assertSameStrings(seed, reference.values(), iterated);
      assertSameStrings(seed, reference.values(), map.getValues());
    }
  }

  @Test
  public void intValueMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      IntValueMap<String> map = new IntValueMap<>();
      Map<String, Integer> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        String key = "k" + random.nextInt(KEY_SPACE);
        switch (random.nextInt(6)) {
          case 0:
            int value = smallValue(random);
            map.put(key, value);
            reference.put(key, value);
            break;
          case 1:
            checkEquals(reference.merge(key, 1, Integer::sum), map.addOrIncrement(key), seed, op,
                "addOrIncrement(" + key + ")");
            break;
          case 2:
            checkEquals(reference.merge(key, -1, Integer::sum), map.decrement(key), seed, op,
                "decrement(" + key + ")");
            break;
          case 3:
            map.remove(key);
            reference.remove(key);
            break;
          case 4:
            check(reference.containsKey(key) == map.contains(key), seed, op,
                "contains(" + key + ")");
            break;
          default:
            // A missing key reads back as 0, which is also a legal stored value.
            checkEquals(reference.getOrDefault(key, 0), map.get(key), seed, op, "get(" + key + ")");
        }
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      Assert.assertEquals(where(seed, -1, "key set"), reference.keySet(), map.getKeySet());
      Assert.assertEquals(where(seed, -1, "key list"),
          reference.keySet(), new HashSet<>(map.getKeyList()));
      assertSameInts(seed, reference.values(), map.copyOfValues());
      Map<String, Integer> fromEntries = new HashMap<>();
      for (IntValueMap.Entry<String> entry : map.iterableEntries()) {
        check(fromEntries.put(entry.key, entry.count) == null, seed, -1,
            "duplicate entry " + entry.key);
      }
      Assert.assertEquals(where(seed, -1, "entries"), reference, fromEntries);
    }
  }

  @Test
  public void floatValueMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      FloatValueMap<String> map = new FloatValueMap<>();
      Map<String, Float> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        String key = "k" + random.nextInt(KEY_SPACE);
        switch (random.nextInt(5)) {
          case 0:
            float value = random.nextInt(1000);
            map.set(key, value);
            reference.put(key, value);
            break;
          case 1:
            float amount = random.nextInt(21) - 10;
            Assert.assertEquals(where(seed, op, "incrementByAmount(" + key + ")"),
                reference.merge(key, amount, Float::sum), map.incrementByAmount(key, amount), 0f);
            break;
          case 2:
            map.remove(key);
            reference.remove(key);
            break;
          case 3:
            check(reference.containsKey(key) == map.contains(key), seed, op,
                "contains(" + key + ")");
            break;
          default:
            Assert.assertEquals(where(seed, op, "get(" + key + ")"),
                reference.getOrDefault(key, 0f), map.get(key), 0f);
        }
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      Assert.assertEquals(where(seed, -1, "key set"), reference.keySet(), map.getKeySet());
      Map<String, Float> fromEntries = new HashMap<>();
      for (FloatValueMap.Entry<String> entry : map.iterableEntries()) {
        check(fromEntries.put(entry.key, entry.value) == null, seed, -1,
            "duplicate entry " + entry.key);
      }
      Assert.assertEquals(where(seed, -1, "entries"), reference, fromEntries);
      // A copy must be an independent snapshot.
      FloatValueMap<String> copy = map.copy();
      Assert.assertEquals(where(seed, -1, "copy key set"), reference.keySet(), copy.getKeySet());
      copy.set("k0", 12345f);
      Assert.assertEquals(where(seed, -1, "copy is independent"),
          reference.getOrDefault("k0", 0f), map.get("k0"), 0f);
    }
  }

  @Test
  public void uIntValueMapFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      UIntValueMap<String> map = new UIntValueMap<>();
      Map<String, Integer> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        String key = "k" + random.nextInt(KEY_SPACE);
        switch (random.nextInt(5)) {
          case 0:
            // Values of this map can not be negative.
            int value = random.nextInt(1000);
            map.put(key, value);
            reference.put(key, value);
            break;
          case 1:
            int amount = random.nextInt(10);
            checkEquals(reference.merge(key, amount, Integer::sum), map.incrementByAmount(key, amount),
                seed, op, "incrementByAmount(" + key + ")");
            break;
          case 2:
            map.remove(key);
            reference.remove(key);
            break;
          case 3:
            check(reference.containsKey(key) == map.contains(key), seed, op,
                "contains(" + key + ")");
            break;
          default:
            // A missing key reads back as -1.
            checkEquals(reference.getOrDefault(key, -1), map.get(key), seed, op,
                "get(" + key + ")");
        }
        checkEquals(reference.size(), map.size(), seed, op, "size");
      }

      Assert.assertEquals(where(seed, -1, "key set"), reference.keySet(), map.getKeySet());
      assertSameInts(seed, reference.values(), map.valueArray());

      long sum = 0;
      int max = 0;
      int min = Integer.MAX_VALUE;
      int largerThan500 = 0;
      int smallerThan500 = 0;
      for (int value : reference.values()) {
        sum += value;
        max = Math.max(max, value);
        min = Math.min(min, value);
        largerThan500 += value > 500 ? 1 : 0;
        smallerThan500 += value < 500 ? 1 : 0;
      }
      checkEquals(sum, map.sumOfValues(), seed, -1, "sumOfValues");
      checkEquals(max, map.maxValue(), seed, -1, "maxValue");
      checkEquals(min, map.minValue(), seed, -1, "minValue");
      checkEquals(largerThan500, map.sizeLarger(500), seed, -1, "sizeLarger");
      checkEquals(smallerThan500, map.sizeSmaller(500), seed, -1, "sizeSmaller");
    }
  }

  @Test
  public void lookupSetFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      LookupSet<String> set = new LookupSet<>();
      // The reference maps each key to the instance the set is expected to be holding.
      Map<String, String> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        // A fresh instance every time, so that lookup() identity can be checked.
        String key = new String("k" + random.nextInt(KEY_SPACE));
        switch (random.nextInt(5)) {
          case 0:
            boolean added = set.add(key);
            check(added == !reference.containsKey(key), seed, op, "add(" + key + ")");
            reference.putIfAbsent(key, key);
            break;
          case 1:
            // set() overrides the stored instance and hands back the old one.
            String previous = set.set(key);
            Assert.assertSame(where(seed, op, "set(" + key + ")"), reference.get(key), previous);
            reference.put(key, key);
            break;
          case 2:
            Assert.assertSame(where(seed, op, "remove(" + key + ")"),
                reference.remove(key), set.remove(key));
            break;
          case 3:
            check(reference.containsKey(key) == set.contains(key), seed, op,
                "contains(" + key + ")");
            break;
          default:
            // lookup must return the stored instance, not the equal instance passed in.
            Assert.assertSame(where(seed, op, "lookup(" + key + ")"),
                reference.get(key), set.lookup(key));
        }
        checkEquals(reference.size(), set.size(), seed, op, "size");
      }

      Assert.assertEquals(where(seed, -1, "key set"), reference.keySet(), set.getKeySet());
      List<String> iterated = new ArrayList<>();
      for (String key : set) {
        iterated.add(key);
      }
      assertSameStrings(seed, reference.keySet(), iterated);
    }
  }

  @Test
  public void histogramFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      Histogram<String> histogram = new Histogram<>();
      Map<String, Integer> reference = new HashMap<>();

      for (int op = 0; op < OPERATIONS; op++) {
        String key = "k" + random.nextInt(KEY_SPACE);
        switch (random.nextInt(6)) {
          case 0:
            checkEquals(reference.merge(key, 1, Integer::sum), histogram.add(key), seed, op,
                "add(" + key + ")");
            break;
          case 1:
            int count = random.nextInt(10);
            checkEquals(reference.merge(key, count, Integer::sum), histogram.add(key, count),
                seed, op, "add(" + key + ", " + count + ")");
            break;
          case 2:
            int value = random.nextInt(100);
            histogram.set(key, value);
            reference.put(key, value);
            break;
          case 3:
            Integer current = reference.get(key);
            if (current != null && current > 0) {
              reference.put(key, current - 1);
              checkEquals(current - 1, histogram.decrementIfPositive(key), seed, op,
                  "decrementIfPositive(" + key + ")");
            } else {
              checkEquals(0, histogram.decrementIfPositive(key), seed, op,
                  "decrementIfPositive(" + key + ") on a non positive count");
            }
            break;
          case 4:
            histogram.remove(key);
            reference.remove(key);
            break;
          default:
            checkEquals(reference.getOrDefault(key, 0), histogram.getCount(key), seed, op,
                "getCount(" + key + ")");
        }
        checkEquals(reference.size(), histogram.size(), seed, op, "size");
      }

      Assert.assertEquals(where(seed, -1, "key set"), reference.keySet(), histogram.getKeySet());

      long total = 0;
      for (int count : reference.values()) {
        total += count;
      }
      checkEquals(total, histogram.totalCount(), seed, -1, "totalCount");

      // getTop must hand back the highest counts, in descending order.
      List<String> top = histogram.getTop(10);
      checkEquals(Math.min(10, reference.size()), top.size(), seed, -1, "top size");
      List<Integer> sortedDescending = new ArrayList<>(reference.values());
      sortedDescending.sort((a, b) -> Integer.compare(b, a));
      for (int i = 0; i < top.size(); i++) {
        checkEquals(sortedDescending.get(i), histogram.getCount(top.get(i)), seed, -1,
            "top[" + i + "] count");
      }

      // removeSmaller must drop exactly the entries below the limit.
      int expectedRemoved = 0;
      for (int count : reference.values()) {
        expectedRemoved += count < 5 ? 1 : 0;
      }
      checkEquals(expectedRemoved, histogram.removeSmaller(5), seed, -1, "removeSmaller");
      reference.values().removeIf(count -> count < 5);
      Assert.assertEquals(where(seed, -1, "key set after removeSmaller"),
          reference.keySet(), histogram.getKeySet());
    }
  }

  @Test
  public void intVectorFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      IntVector vector = new IntVector(random.nextInt(8));
      List<Integer> reference = new ArrayList<>();

      for (int op = 0; op < OPERATIONS; op++) {
        switch (random.nextInt(6)) {
          case 0:
            int value = random.nextInt();
            vector.add(value);
            reference.add(value);
            break;
          case 1:
            int[] values = new int[random.nextInt(5)];
            for (int i = 0; i < values.length; i++) {
              values[i] = random.nextInt();
              reference.add(values[i]);
            }
            vector.addAll(values);
            break;
          case 2:
            if (!reference.isEmpty()) {
              int index = random.nextInt(reference.size());
              int newValue = random.nextInt();
              vector.set(index, newValue);
              reference.set(index, newValue);
            }
            break;
          case 3:
            if (!reference.isEmpty()) {
              int index = random.nextInt(reference.size());
              checkEquals(reference.get(index), vector.get(index), seed, op, "get(" + index + ")");
            }
            break;
          case 4:
            check(reference.isEmpty() == vector.isempty(), seed, op, "isempty");
            break;
          default:
            int probe = random.nextInt(100);
            check(reference.contains(probe) == vector.contains(probe), seed, op,
                "contains(" + probe + ")");
        }
        checkEquals(reference.size(), vector.size(), seed, op, "size");
        check(vector.capacity() >= vector.size(), seed, op,
            "capacity " + vector.capacity() + " below size " + vector.size());
      }

      Assert.assertArrayEquals(where(seed, -1, "contents"), toIntArray(reference), vector.copyOf());

      // trimToSize keeps the contents, sort orders them, and equals sees the two as the same.
      vector.trimToSize();
      checkEquals(reference.size(), vector.capacity(), seed, -1, "capacity after trimToSize");
      Assert.assertArrayEquals(where(seed, -1, "contents after trim"),
          toIntArray(reference), vector.copyOf());

      vector.sort();
      reference.sort(Integer::compare);
      Assert.assertArrayEquals(where(seed, -1, "contents after sort"),
          toIntArray(reference), vector.copyOf());

      IntVector same = new IntVector(vector.copyOf());
      Assert.assertEquals(where(seed, -1, "equals"), vector, same);
      Assert.assertEquals(where(seed, -1, "hashCode"), vector.hashCode(), same.hashCode());
    }
  }

  @Test
  public void fixedBitVectorFuzz() {
    for (int seed : SEEDS) {
      Random random = new Random(seed);
      int length = 1 + random.nextInt(500);
      FixedBitVector vector = new FixedBitVector(length);
      BitSet reference = new BitSet(length);

      for (int op = 0; op < OPERATIONS; op++) {
        int index = random.nextInt(length);
        switch (random.nextInt(3)) {
          case 0:
            vector.set(index);
            reference.set(index);
            break;
          case 1:
            vector.clear(index);
            reference.clear(index);
            break;
          default:
            check(reference.get(index) == vector.safeGet(index), seed, op, "get(" + index + ")");
        }
      }

      for (int i = 0; i < length; i++) {
        check(reference.get(i) == vector.get(i), seed, -1, "final get(" + i + ")");
      }
      checkEquals(reference.cardinality(), vector.numberOfOnes(), seed, -1, "numberOfOnes");
      checkEquals(length - reference.cardinality(), vector.numberOfZeroes(), seed, -1,
          "numberOfZeroes");
      int[] zeroIndexes = vector.zeroIndexes();
      checkEquals(length - reference.cardinality(), zeroIndexes.length, seed, -1,
          "zeroIndexes length");
      for (int zeroIndex : zeroIndexes) {
        check(!reference.get(zeroIndex), seed, -1, "zeroIndexes contains a set bit " + zeroIndex);
      }
    }
  }

  /**
   * The value that {@code get} returns for a missing key is a legal value to store. Storing it
   * must not make the key disappear from anything but {@code get}.
   */
  @Test
  public void sentinelValuesCanBeStored() {
    IntIntMap intIntMap = new IntIntMap();
    intIntMap.put(7, IntIntMap.NO_RESULT);
    Assert.assertTrue(intIntMap.containsKey(7));
    Assert.assertEquals(1, intIntMap.size());
    Assert.assertArrayEquals(new int[]{7}, intIntMap.getKeys());
    intIntMap.remove(7);
    Assert.assertFalse(intIntMap.containsKey(7));
    Assert.assertEquals(0, intIntMap.size());

    IntValueMap<String> intValueMap = new IntValueMap<>();
    intValueMap.put("a", 0);
    Assert.assertTrue(intValueMap.contains("a"));
    Assert.assertEquals(1, intValueMap.size());
    Assert.assertEquals(0, intValueMap.get("a"));

    FloatValueMap<String> floatValueMap = new FloatValueMap<>();
    floatValueMap.set("a", 0f);
    Assert.assertTrue(floatValueMap.contains("a"));
    Assert.assertEquals(1, floatValueMap.size());

    UIntValueMap<String> uIntValueMap = new UIntValueMap<>();
    uIntValueMap.put("a", 0);
    Assert.assertTrue(uIntValueMap.contains("a"));
    Assert.assertEquals(0, uIntValueMap.get("a"));

    LongUIntMap longUIntMap = new LongUIntMap();
    longUIntMap.put(3L, 0);
    Assert.assertTrue(longUIntMap.containsKey(3L));
    Assert.assertEquals(0, longUIntMap.get(3L));
  }

  private static int smallValue(Random random) {
    // Avoids the NO_RESULT sentinel, which get() can not tell apart from a missing key.
    return random.nextInt(2_000_000) - 1_000_000;
  }

  private static long randomLongKey(Random random) {
    long key = random.nextInt(KEY_SPACE) - KEY_SPACE / 2;
    // Half of the keys are spread over the whole long range, so that the hash has to mix the
    // high word as well.
    return random.nextBoolean() ? key : key * 0x1_0000_0001L;
  }

  private static int[] toIntArray(List<Integer> list) {
    int[] result = new int[list.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = list.get(i);
    }
    return result;
  }

  private static void assertSameKeys(int seed, Set<Integer> expected, int[] actual) {
    checkEquals(expected.size(), actual.length, seed, -1, "key count");
    Set<Integer> actualSet = new HashSet<>();
    for (int key : actual) {
      check(actualSet.add(key), seed, -1, "duplicate key " + key);
    }
    Assert.assertEquals(where(seed, -1, "keys"), expected, actualSet);
  }

  private static void assertSameInts(int seed, Iterable<Integer> expected, int[] actual) {
    List<Integer> expectedList = new ArrayList<>();
    for (int value : expected) {
      expectedList.add(value);
    }
    expectedList.sort(Integer::compare);
    int[] sorted = actual.clone();
    Arrays.sort(sorted);
    Assert.assertArrayEquals(where(seed, -1, "values"), toIntArray(expectedList), sorted);
  }

  private static void assertSameStrings(int seed, Iterable<String> expected, List<String> actual) {
    List<String> expectedList = new ArrayList<>();
    for (String value : expected) {
      expectedList.add(value);
    }
    expectedList.sort(String::compareTo);
    List<String> actualList = new ArrayList<>(actual);
    actualList.sort(String::compareTo);
    Assert.assertEquals(where(seed, -1, "values"), expectedList, actualList);
  }

  private static void check(boolean condition, int seed, int op, String what) {
    if (!condition) {
      Assert.fail(where(seed, op, what));
    }
  }

  private static void checkEquals(long expected, long actual, int seed, int op, String what) {
    if (expected != actual) {
      Assert.assertEquals(where(seed, op, what), expected, actual);
    }
  }

  private static String where(int seed, int op, String what) {
    return "seed " + seed + (op < 0 ? " final check" : " operation " + op) + ": " + what;
  }
}
