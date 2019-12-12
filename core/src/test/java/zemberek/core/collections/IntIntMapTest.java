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
public class IntIntMapTest {

  private IntIntMap createMap() {
    return new IntIntMap();
  }

  private IntIntMap createMap(int initialSize) {
    return new IntIntMap(initialSize);
  }


  @Test
  public void initializesCorrectly() {
    // Check first 1K initial sizes.
    for (int i = 1; i < 1000; i++) {
      IntIntMap im = createMap(i);
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
      IntIntMap im = createMap();
      im.put(key, 1);
      Assert.fail("Illegal key should have thrown an exception. Key: " + key);
    } catch (RuntimeException e) {
      // Nothing to do
    }
  }

  @Test public void putGetWorksCorrectly() {
    // Test edge conditions.
    putGetCheck(0, 0);
    putGetCheck(0, -1);
    putGetCheck(0, 1);
    putGetCheck(0, Integer.MAX_VALUE);
    putGetCheck(0, Integer.MIN_VALUE);
    putGetCheck(-1, 0);
    putGetCheck(-1, -1);
    putGetCheck(-1, 1);
    putGetCheck(-1, Integer.MAX_VALUE);
    putGetCheck(-1, Integer.MIN_VALUE);
    putGetCheck(Integer.MAX_VALUE, 0);
    putGetCheck(Integer.MAX_VALUE, -1);
    putGetCheck(Integer.MAX_VALUE, 1);
    putGetCheck(Integer.MAX_VALUE, Integer.MAX_VALUE);
    putGetCheck(Integer.MAX_VALUE, Integer.MIN_VALUE);
    putGetCheck(Integer.MIN_VALUE + 2, 0);
    putGetCheck(Integer.MIN_VALUE + 2, -1);
    putGetCheck(Integer.MIN_VALUE + 2, 1);
    putGetCheck(Integer.MIN_VALUE + 2, Integer.MAX_VALUE);
    putGetCheck(Integer.MIN_VALUE + 2, Integer.MIN_VALUE);
  }

  private void putGetCheck(int key, int value) {
    IntIntMap im = createMap();
    im.put(key, value);
    assertEquals(value, im.get(key));
  }

  @Test
  public void handlesExpansionEdgeCases() {
    // If load factor is 1 this means backing array is filled completely
    // and this causes an infinite loop in case a non existing element is
    // searched in the map. This happens because we expect at least an
    // empty slot in the map to decide the element does not exist in the
    // map.
    IntIntMap im = new IntIntMap(2);
    im.put(1, 3);
    im.put(2, 5);
    // If backing array has no  empty element this call would cause an
    // infinite loop.
    assertEquals(im.get(3), IntIntMap.EMPTY);

    im = new IntIntMap(4);
    im.put(1, 3);
    im.put(2, 5);
    im.put(3, 5);
    im.put(4, 5);
    assertEquals(im.get(5), IntIntMap.EMPTY);

    im = new IntIntMap(1);
    im.put(1, 2);
    assertEquals(im.get(3), IntIntMap.EMPTY);
  }

  @Test
  public void expandsCorrectly() {
    // Create maps with different sizes and add size * 10 elements to each.
    for (int i = 1; i < 100; i++) {
      IntIntMap im = createMap(i);
      // Insert i * 10 elements to each and confirm sizes
      int elements = i * 10;
      for (int j = 0; j < elements; j++) {
        im.put(j, j + 13);
      }
      for (int j = 0; j < elements; j++) {
        Assert.assertEquals(im.get(j), j + 13);
      }
      checkSize(im, elements);
    }
  }

  @Test
  public void putAddsAndUpdatesElementsCorrectly() {
    int span = 100;
    for (int i = 0; i < span; i++) {
      IntIntMap im = createMap();
      checkSpanInsertions(im, -i, i);
    }
    // Do the same, this time overwrite values as well
    IntIntMap im = createMap();
    for (int i = 0; i < span; i++) {
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
    }
  }

  @Test
  public void removeRemovesCorrectly() {
    IntIntMap im = createMap();
    im.put(0, 0);
    assertEquals(im.get(0), 0);
    im.remove(0);
    assertEquals(im.get(0), IntIntMap.NO_RESULT);
    assertEquals(im.size(), 0);
    // remove again works
    im.remove(0);
    assertEquals(im.get(0), IntIntMap.NO_RESULT);
    assertEquals(im.size(), 0);
    im.put(0, 1);
    assertEquals(im.size(), 1);
    assertEquals(im.get(0), 1);
  }

  @Test
  public void removeSpansWorksCorrectly() {
    IntIntMap im = createMap();
    insertSpan(im, 0, 99);
    removeSpan(im, 1, 98);
    assertEquals(im.size(), 2);
    checkSpanRemoved(im, 1, 98);
    insertSpan(im, 0, 99);
    assertEquals(im.size(), 100);
    checkSpan(im, 0, 99);
  }

  @Test
  public void removeSpansWorksCorrectly2() {
    IntIntMap im = createMap();
    int limit = 9999;
    insertSpan(im, 0, limit);
    int[] r = TestUtils.createRandomUintArray(1000, limit);
    for (int i : r) {
      im.remove(i);
    }
    for (int i : r) {
      assertEquals(im.get(i), IntIntMap.NO_RESULT);
    }
    insertSpan(im, 0, limit);
    checkSpan(im, 0, limit);
    removeSpan(im, 0, limit);
    assertEquals(im.size(), 0);
    insertSpan(im, -limit, limit);
    checkSpan(im, -limit, limit);
  }

  @Test
  public void survivesSimpleFuzzing() {
    List<int[]> fuzzLists = TestUtils.createFuzzingLists();
    for (int[] arr : fuzzLists) {
      IntIntMap im = createMap();
      for (int i1 : arr) {
        im.put(i1, i1 + 7);
        assertEquals(im.get(i1), i1 + 7);
      }
    }

    IntIntMap im = createMap();
    for (int[] arr : fuzzLists) {
      for (int i1 : arr) {
        im.put(i1, i1 + 7);
        assertEquals(im.get(i1), i1 + 7);
      }
    }
  }

  private void removeSpan(IntIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.remove(i);
    }
  }

  private void checkSpanRemoved(IntIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), IntIntMap.NO_RESULT);
    }
  }

  private void checkSpanInsertions(IntIntMap im, int start, int end) {
    insertSpan(im, start, end);
    // Expected size.
    int size = Math.abs(start) + Math.abs(end) + 1;
    assertEquals(size, im.size());
    checkSpan(im, start, end);
  }

  @Test
  public void checkLargeValues() {
    IntIntMap map = createMap();
    int c = 0;
    for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE-1000; i += 1000) {
      map.put(c, i);
      c++;
    }
    c = 0;
    for (int i = Integer.MIN_VALUE; i <  Integer.MAX_VALUE-1000; i += 1000) {
      int val = map.get(c);
      Assert.assertEquals(i, val);
      c++;
    }
    c = 0;
    for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE-1000; i += 1000) {
      map.increment(c, 1);
      c++;
    }

    c = 0;
    for (int i = Integer.MIN_VALUE; i <  Integer.MAX_VALUE-1000; i += 1000) {
      int val = map.get(c);
      Assert.assertEquals(i+1, val);
      c++;
    }
  }

  private void insertSpan(IntIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.put(i, i);
    }
  }

  private void checkSpan(IntIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), i);
    }
    // Check outside of span values do not exist in the map
    for (int i = spanStart - 1, idx = 0; idx < 100; i--, idx++) {
      Assert.assertEquals(IntIntMap.NO_RESULT, im.get(i));
    }
    for (int i = spanEnd + 1, idx = 0; idx < 100; i++, idx++) {
      Assert.assertEquals(IntIntMap.NO_RESULT, im.get(i));
    }
  }

  private void checkSize(IntIntMap m, int size) {
    assertEquals(size, m.size());
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
      IntIntMap imap = createMap();
      for (int i1 : arr) {
        imap.put(i1, i1 + 1);
      }
    }
    long elapsed = System.currentTimeMillis() - start;
    System.out.println("Creation: " + elapsed);

    IntIntMap imap = createMap();
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
    IntIntMap map = createMap();
    map.put(1, 2);
    Assert.assertEquals(2, map.get(1));
    Assert.assertEquals(IntIntMap.NO_RESULT, map.get(2));
    map.put(1, 3);
    Assert.assertEquals(3, map.get(1));

    map = createMap();
    for (int i = 0; i < 100000; i++) {
      map.put(i, i + 1);
    }
    for (int i = 0; i < 100000; i++) {
      Assert.assertEquals(i + 1, map.get(i));
    }
  }

  @Test
  public void removeTest2() {
    IntIntMap map = createMap();
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
    int[][] keyVals = new int[1_000_000][2];
    final int iterCreation = 10;
    final int iterRetrieval = 50;
    for (int i = 0; i < keyVals.length; i++) {
      // We allow some duplications.
      keyVals[i][0] = r.nextInt(5_000_000);
      keyVals[i][1] = r.nextInt(5000) + 1;
    }
    Stopwatch sw = Stopwatch.createStarted();
    for (int j = 0; j < iterCreation; j++) {
      HashMap<Integer, Integer> map = new HashMap<>();
      for (int[] keyVal : keyVals) {
        map.put(keyVal[0], keyVal[1]);
      }
    }
    System.out.println("Map creation: " + sw.elapsed(TimeUnit.MILLISECONDS));
    HashMap<Integer, Integer> map = new HashMap<>();
    for (int[] keyVal : keyVals) {
      map.put(keyVal[0], keyVal[1]);
    }
    long val = 0;
    sw = Stopwatch.createStarted();
    for (int j = 0; j < iterRetrieval; j++) {
      for (int[] keyVal : keyVals) {
        val += map.get(keyVal[0]);
      }
    }
    System.out.println("Map retrieval: " + sw.elapsed(TimeUnit.MILLISECONDS));
    System.out.println("Verification sum: " + val);

    sw = Stopwatch.createStarted();
    for (int j = 0; j < iterCreation; j++) {
      IntIntMap countTable = createMap();
      for (int[] keyVal : keyVals) {
        countTable.put(keyVal[0], keyVal[1]);
      }
    }
    System.out.println("IntIntMap creation: " + sw.elapsed(TimeUnit.MILLISECONDS));

    IntIntMap countTable = createMap();
    for (int[] keyVal : keyVals) {
      countTable.put(keyVal[0], keyVal[1]);
    }
    val = 0;
    sw = Stopwatch.createStarted();
    for (int j = 0; j < iterRetrieval; j++) {
      for (int[] keyVal : keyVals) {
        val += countTable.get(keyVal[0]);
      }
    }
    System.out.println("IntIntMap retrieval: " + sw.elapsed(TimeUnit.MILLISECONDS));
    System.out.println("Verification sum: " + val);
  }
}
