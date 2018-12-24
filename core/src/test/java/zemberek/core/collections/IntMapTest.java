package zemberek.core.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class IntMapTest {

  @Test
  public void initializesCorrectly() {
    // Check first 1K initial sizes.
    for (int i = 1; i < 1000; i++) {
      IntMap<String> im = new IntMap<>(i);
      checkSize(im, 0);
    }
  }

  @Test
  public void failsOnInvalidSizes() {
    try {
      IntMap<String> im;
      im = new IntMap<>(0);
      im = new IntMap<>(-1);
      im = new IntMap<>(Integer.MAX_VALUE);
      im = new IntMap<>(Integer.MIN_VALUE);
      im = new IntMap<>(1 << 29 + 1);
      Assert.fail("Illegal size should have thrown an exception.");
    } catch (RuntimeException e) {
      // Nothing to do
    }
  }

  @Test
  public void expandsCorrectly() {
    // Create maps with different sizes and add size * 10 elements to each.
    for (int i = 1; i < 100; i++) {
      IntMap<String> im = new IntMap<>(i);
      // Insert i * 10 elements to each and confirm sizes
      int elements = i * 10;
      for (int j = 0; j < elements; j++) {
        im.put(j, "" + j);
      }
      for (int j = 0; j < elements; j++) {
        Assert.assertEquals(im.get(j), "" + j);
      }
      checkSize(im, elements);
    }
  }

  @Test
  public void putAddsAndUpdatesElementsCorrectly() {
    int span = 100;
    for (int i = 0; i < span; i++) {
      IntMap<String> im = new IntMap<>();
      checkSpanInsertions(im, -i, i);
    }
    // Do the same, this time overwrite values as well
    IntMap<String> im = new IntMap<>();
    for (int i = 0; i < span; i++) {
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
    }
  }

  @Test
  public void survivesSimpleFuzzing() {
    List<int[]> fuzzLists = TestUtils.createFuzzingLists();
    for (int[] arr : fuzzLists) {
      IntMap<String> im = new IntMap<>();
      for (int i1 : arr) {
        im.put(i1, "" + i1);
        assertEquals(im.get(i1), "" + i1);
      }
    }

    IntMap<String> im = new IntMap<>();
    for (int[] arr : fuzzLists) {
      for (int i1 : arr) {
        im.put(i1, "" + i1);
        assertEquals(im.get(i1), "" + i1);
      }
    }
  }

  private void checkSpanInsertions(IntMap<String> im, int start, int end) {
    insertSpan(im, start, end);
    // Expected size.
    int size = Math.abs(start) + Math.abs(end) + 1;
    assertEquals(size, im.size());
    checkSpan(im, start, end);
  }

  private void insertSpan(IntMap<String> im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.put(i, "" + i);
    }
  }

  private void checkSpan(IntMap<String> im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), "" + i);
    }
    // Check outside of span values do not exist in the map
    for (int i = spanStart - 1, idx = 0; idx < 100; i--, idx++) {
      Assert.assertNull(im.get(i));
    }
    for (int i = spanEnd + 1, idx = 0; idx < 100; i++, idx++) {
      Assert.assertNull(im.get(i));
    }
  }

  private void checkSize(IntMap<String> m, int size) {
    assertEquals(size, m.size());
    assertTrue(m.capacity() > m.size());
    // Check capacity is 2^n
    assertTrue((m.capacity() & (m.capacity() - 1)) == 0);
  }

  @Test
  @Ignore(value = "Not a test. Only a performance comparison.")
  public void addingElementsFromAnotherIntMapMustBeFast() {
    // create a map with 5 million keys.
    int k = 5_000_000;
    IntMap<String> map1 = initializeMapWithRandomNumbers(k);
    System.out.println("Map initialized.");

    // using keys with order in the map, copy key-values to another IntMap.
    int[] keys = map1.getKeys();
    int[] keysShuffled = keys.clone();
    TestUtils.shuffle(keysShuffled);
    IntMap<String> map2 = new IntMap<>();

    Stopwatch sw = Stopwatch.createStarted();
    Stopwatch sw2 = Stopwatch.createStarted();

    for (int i : keysShuffled) {
      map2.put(i, map1.get(i));
      if (map2.size() % 10000 == 0) {
        //Assert.assertTrue(sw.elapsed(TimeUnit.MILLISECONDS) < 200);
        sw.reset().start();
      }
    }

    System.out.println("Shuffled Keys Elapsed = " + sw2.elapsed(TimeUnit.MILLISECONDS));

    sw = Stopwatch.createStarted();
    sw2 = Stopwatch.createStarted();
    map2 = new IntMap<>();

    for (int i : keys) {
      map2.put(i, map1.get(i));
      if (map2.size() % 10000 == 0) {
        //Assert.assertTrue(sw.elapsed(TimeUnit.MILLISECONDS) < 200);
        sw.reset().start();
      }
    }
    System.out.println("Keys Elapsed = " + sw2.elapsed(TimeUnit.MILLISECONDS));

  }

  private IntMap<String> initializeMapWithRandomNumbers(int k) {
    IntMap<String> map = new IntMap<>(k);
    Set<Integer> intSet = new HashSet<>();
    Random rnd = new Random(1);
    while (intSet.size() < k) {
      int r = rnd.nextInt(Integer.MAX_VALUE - 10);
      intSet.add(r + 1);
    }
    for (Integer i : intSet) {
      map.put(i, String.valueOf(i));
    }
    return map;
  }
}
