package zemberek.core.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

// Base test class for int-int maps.
public class IntFloatMapTest {

  private IntFloatMap createMap() {
    return new IntFloatMap();
  }

  private IntFloatMap createMap(int initialSize) {
    return new IntFloatMap(initialSize);
  }

  private static void assertEqualsF(float a, float b) {
    assertEquals(Float.floatToIntBits(a), Float.floatToIntBits(b));
  }
  
  @Test
  public void initializesCorrectly() {
    // Check first 1K initial sizes.
    for (int i = 1; i < 1000; i++) {
      IntFloatMap im = createMap(i);
      checkSize(im, 0);
    }
  }

  @Test
  public void failsOnInvalidSizes() {
    checkInvalidSize(0);
    checkInvalidSize(-1);
    checkInvalidSize(Integer.MAX_VALUE);
  }

  private void checkInvalidSize(int size) {
    try {
      createMap(size);
      Assert.fail("Illegal size should have thrown an exception. Size: " + size);
    } catch (RuntimeException e) {
      // Nothing to do
    }
  }

  @Test
  public void failsOnInvalidKeys() {
    checkInvalidKeys(Integer.MIN_VALUE);
    checkInvalidKeys(Integer.MIN_VALUE + 1);
  }

  private void checkInvalidKeys(int key) {
    try {
      IntFloatMap im = createMap();
      im.put(key, 1);
      Assert.fail("Illegal key should have thrown an exception. Key: " + key);
    } catch (RuntimeException e) {
      // Nothing to do
    }
  }

  @Test public void putGetWorksCorrectly() {
    // Test edge conditions.
    putGetCheck(0, 0.0f);
    putGetCheck(0, -1.0f);
    putGetCheck(0, 1.0f);
    putGetCheck(0, Float.MAX_VALUE);
    putGetCheck(0, Float.MIN_VALUE);
    putGetCheck(-1, 0.0f);
    putGetCheck(-1, -1.0f);
    putGetCheck(-1, 1.0f);
    putGetCheck(-1, Float.MAX_VALUE);
    putGetCheck(-1, Float.MIN_VALUE);
    putGetCheck(Integer.MAX_VALUE, 0.0f);
    putGetCheck(Integer.MAX_VALUE, -1.0f);
    putGetCheck(Integer.MAX_VALUE, 1.0f);
    putGetCheck(Integer.MAX_VALUE, Float.MAX_VALUE);
    putGetCheck(Integer.MAX_VALUE, Float.MIN_VALUE);
    putGetCheck(Integer.MIN_VALUE + 2, 0f);
    putGetCheck(Integer.MIN_VALUE + 2, -1f);
    putGetCheck(Integer.MIN_VALUE + 2, 1f);
    putGetCheck(Integer.MIN_VALUE + 2, Float.MAX_VALUE);
    putGetCheck(Integer.MIN_VALUE + 2, Float.MIN_VALUE);
  }

  private void putGetCheck(int key, float value) {
    IntFloatMap im = createMap();
    im.put(key, value);
    assertEqualsF(value, im.get(key));
  }

  @Test
  public void expandsCorrectly() {
    // Create maps with different sizes and add size * 10 elements to each.
    for (int i = 1; i < 100; i++) {
      IntFloatMap im = createMap(i);
      // Insert i * 10 elements to each and confirm sizes
      int elements = i * 10;
      for (int j = 0; j < elements; j++) {
        im.put(j, j + 13);
      }
      for (int j = 0; j < elements; j++) {
        assertEqualsF(im.get(j), j + 13);
      }
      checkSize(im, elements);
    }
  }

  @Test
  public void putAddsAndUpdatesElementsCorrectly() {
    int span = 100;
    for (int i = 0; i < span; i++) {
      IntFloatMap im = createMap();
      checkSpanInsertions(im, -i, i);
    }
    // Do the same, this time overwrite values as well
    IntFloatMap im = createMap();
    for (int i = 0; i < span; i++) {
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
    }
  }

  @Test
  public void removeRemovesCorrectly() {
    IntFloatMap im = createMap();
    im.put(0, 0);
    assertEqualsF(im.get(0), 0);
    im.remove(0);
    assertEqualsF(im.get(0), IntIntMap.NO_RESULT);
    assertEqualsF(im.size(), 0);
    // remove again works
    im.remove(0);
    assertEqualsF(im.get(0), IntIntMap.NO_RESULT);
    assertEqualsF(im.size(), 0);
    im.put(0, 1);
    assertEqualsF(im.size(), 1);
    assertEqualsF(im.get(0), 1);
  }

  @Test
  public void removeSpansWorksCorrectly() {
    IntFloatMap im = createMap();
    insertSpan(im, 0, 99);
    removeSpan(im, 1, 98);
    assertEqualsF(im.size(), 2);
    checkSpanRemoved(im, 1, 98);
    insertSpan(im, 0, 99);
    assertEqualsF(im.size(), 100);
    checkSpan(im, 0, 99);
  }

  @Test
  public void removeSpansWorksCorrectly2() {
    IntFloatMap im = createMap();
    int limit = 9999;
    insertSpan(im, 0, limit);
    int[] r = TestUtils.createRandomUintArray(1000, limit);
    for (int i : r) {
      im.remove(i);
    }
    for (int i : r) {
      assertEqualsF(im.get(i), IntIntMap.NO_RESULT);
    }
    insertSpan(im, 0, limit);
    checkSpan(im, 0, limit);
    removeSpan(im, 0, limit);
    assertEqualsF(im.size(), 0);
    insertSpan(im, -limit, limit);
    checkSpan(im, -limit, limit);
  }

  @Test
  public void survivesSimpleFuzzing() {
    List<int[]> fuzzLists = TestUtils.createFuzzingLists();
    for (int[] arr : fuzzLists) {
      IntFloatMap im = createMap();
      for (int i : arr) {
        im.put(i, i + 7);
        assertEqualsF(im.get(i), i + 7);
      }
    }

    IntFloatMap im = createMap();
    for (int[] arr : fuzzLists) {
      for (int i1 : arr) {
        im.put(i1, i1 + 7);
        assertEqualsF(im.get(i1), i1 + 7);
      }
    }
  }

  private void removeSpan(IntFloatMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.remove(i);
    }
  }

  private void checkSpanRemoved(IntFloatMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEqualsF(im.get(i), IntIntMap.NO_RESULT);
    }
  }

  private void checkSpanInsertions(IntFloatMap im, int start, int end) {
    insertSpan(im, start, end);
    // Expected size.
    int size = Math.abs(start) + Math.abs(end) + 1;
    assertEqualsF(size, im.size());
    checkSpan(im, start, end);
  }

  private void insertSpan(IntFloatMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.put(i, i);
    }
  }

  private void checkSpan(IntFloatMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEqualsF(im.get(i), i);
    }
    // Check outside of span values do not exist in the map
    for (int i = spanStart - 1, idx = 0; idx < 100; i--, idx++) {
      assertEqualsF(IntIntMap.NO_RESULT, im.get(i));
    }
    for (int i = spanEnd + 1, idx = 0; idx < 100; i++, idx++) {
      assertEqualsF(IntIntMap.NO_RESULT, im.get(i));
    }
  }

  private void checkSize(IntFloatMap m, int size) {
    assertEqualsF(size, m.size());
    assertTrue(m.capacity() > m.size());
    // Check capacity is 2^n
    assertTrue((m.capacity() & (m.capacity() - 1)) == 0);
  }

  @Test
  @Ignore("Not a unit test")
  public void testPerformance() {
    int[] arr = TestUtils.createRandomUintArray(1_000_000, 1 << 29);
    long sum = 0;
    int iter = 100;
    long start = System.currentTimeMillis();
    for (int i = 0; i < iter; i++) {
      IntFloatMap imap = createMap();
      for (int i1 : arr) {
        imap.put(i1, i1 + 1);
      }
    }
    long elapsed = System.currentTimeMillis() - start;
    System.out.println("Creation: " + elapsed);

    IntFloatMap imap = createMap();
    for (int i1 : arr) {
      imap.put(i1, i1 + 1);
    }
    start = System.currentTimeMillis();
    for (int i = 0; i < iter; i++) {
      for (int j = arr.length - 1; j >= 0; j--) {
        sum += imap.get(arr[j]);
      }
    }
    elapsed = System.currentTimeMillis() - start;
    System.out.println("Retrieval: " + elapsed);
    System.out.println("Val: " + sum);
  }

  @Test
  public void getTest2() {
    IntFloatMap map = createMap();
    map.put(1, 2);
    assertEqualsF(2, map.get(1));
    assertEqualsF(IntIntMap.NO_RESULT, map.get(2));
    map.put(1, 3);
    assertEqualsF(3, map.get(1));

    map = createMap();
    for (int i = 0; i < 100000; i++) {
      map.put(i, i + 1);
    }
    for (int i = 0; i < 100000; i++) {
      assertEqualsF(i + 1, map.get(i));
    }
  }

  @Test
  public void removeTest2() {
    IntFloatMap map = createMap();
    for (int i = 0; i < 10000; i++) {
      map.put(i, i + 1);
    }
    for (int i = 0; i < 10000; i += 3) {
      map.remove(i);
    }
    for (int i = 0; i < 10000; i += 3) {
      Assert.assertTrue(!map.containsKey(i));
    }
    for (int i = 0; i < 10000; i++) {
      map.put(i, i + 1);
    }
    for (int i = 0; i < 10000; i += 3) {
      Assert.assertTrue(map.containsKey(i));
    }
  }

  @Test
  @Ignore("Not a unit test")
  public void speedAgainstHashMap() {
    Random r = new Random(0xBEEFCAFE);
    int size = 1_000_000;
    int[] keys = new int[size];
    float[] vals = new float[size];
    final int iterCreation = 10;
    final int iterRetrieval = 50;
    for (int i = 0; i < keys.length; i++) {
      // We allow some duplications.
      keys[i] = r.nextInt(size * 5);
      vals[i] = r.nextFloat() * 5000f;
    }
    Stopwatch sw = Stopwatch.createStarted();
    for (int j = 0; j < iterCreation; j++) {
      HashMap<Integer, Float> map = new HashMap<>();
      for (int i = 0; i < size; i ++) {
        map.put(keys[i], vals[i]);
      }
    }
    System.out.println("Map creation: " + sw.elapsed(TimeUnit.MILLISECONDS));
    HashMap<Integer, Float> map = new HashMap<>();
    for (int i = 0; i < size; i ++) {
      map.put(keys[i], vals[i]);
    }
    double val = 0;
    sw = Stopwatch.createStarted();
    for (int j = 0; j < iterRetrieval; j++) {
      for (int i = 0; i < size; i ++) {
        val += map.get(keys[i]);
      }
    }
    System.out.println("Map retrieval: " + sw.elapsed(TimeUnit.MILLISECONDS));
    System.out.println("Verification sum: " + val);

    sw = Stopwatch.createStarted();
    for (int j = 0; j < iterCreation; j++) {
      IntFloatMap countTable = createMap();
      for (int i = 0; i < size; i ++) {
        countTable.put(keys[i], vals[i]);
      }
    }
    System.out.println("IntIntMap creation: " + sw.elapsed(TimeUnit.MILLISECONDS));

    IntFloatMap countTable = createMap();
    for (int i = 0; i < size; i ++) {
      countTable.put(keys[i], vals[i]);
    }
    val = 0.0d;
    sw = Stopwatch.createStarted();
    for (int j = 0; j < iterRetrieval; j++) {
      for (int i = 0; i < size; i ++) {
        val += countTable.get(keys[i]);
      }
    }
    System.out.println("IntIntMap retrieval: " + sw.elapsed(TimeUnit.MILLISECONDS));
    System.out.println("Verification sum: " + val);
  }
}
