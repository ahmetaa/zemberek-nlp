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
public abstract class IntMapTestBase {
  
  abstract IntIntMapBase createMap();

  abstract IntIntMapBase createMap(int initialSize);
  
  @Test
  public void initializesCorrectly() {
    // Check first 1K initial sizes.
    for (int i = 1; i < 1000; i++) {
      IntIntMapBase im = createMap(i);
      checkSize(im, 0);
    }
  }

  @Test
  public void failsOnInvalidSizes() {
    checkInvalidSize(0);
    checkInvalidSize(-1);
    checkInvalidSize(Integer.MAX_VALUE);
    checkInvalidSize(1 << 30 + 1);
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
      IntIntMapBase im = createMap();
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
    IntIntMapBase im = createMap();
    im.put(key, value);
    assertEquals(value, im.get(key));
  }

  @Test
  public void expandsCorrectly() {
    // Create maps with different sizes and add size * 10 elements to each.
    for (int i = 1; i < 100; i++) {
      IntIntMapBase im = createMap(i);
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
      IntIntMapBase im = createMap();
      checkSpanInsertions(im, -i, i);
    }
    // Do the same, this time overwrite values as well
    IntIntMapBase im = createMap();
    for (int i = 0; i < span; i++) {
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
    }
  }

  @Test
  public void removeRemovesCorrectly() {
    IntIntMapBase im = createMap();
    im.put(0, 0);
    assertEquals(im.get(0), 0);
    im.remove(0);
    assertEquals(im.get(0), IntIntMapBase.NO_RESULT);
    assertEquals(im.size(), 0);
    // remove again works
    im.remove(0);
    assertEquals(im.get(0), IntIntMapBase.NO_RESULT);
    assertEquals(im.size(), 0);
    im.put(0, 1);
    assertEquals(im.size(), 1);
    assertEquals(im.get(0), 1);
  }

  @Test
  public void removeSpansWorksCorrectly() {
    IntIntMapBase im = createMap();
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
    IntIntMapBase im = createMap();
    int limit = 9999;
    insertSpan(im, 0, limit);
    int[] r = TestUtils.createRandomUintArray(1000, limit);
    for (int i : r) {
      im.remove(i);
    }
    for (int i : r) {
      assertEquals(im.get(i), IntIntMapBase.NO_RESULT);
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
      IntIntMapBase im = createMap();
      for (int i = 0; i < arr.length; i++) {
        im.put(arr[i], arr[i] + 7);
        assertEquals(im.get(arr[i]), arr[i] + 7);
      }
    }

    IntIntMapBase im = createMap();
    for (int[] arr : fuzzLists) {
      for (int i = 0; i < arr.length; i++) {
        im.put(arr[i], arr[i] + 7);
        assertEquals(im.get(arr[i]), arr[i] + 7);
      }
    }
  }

  private void removeSpan(IntIntMapBase im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.remove(i);
    }
  }

  private void checkSpanRemoved(IntIntMapBase im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), IntIntMapBase.NO_RESULT);
    }
  }

  private void checkSpanInsertions(IntIntMapBase im, int start, int end) {
    insertSpan(im, start, end);
    // Expected size.
    int size = Math.abs(start) + Math.abs(end) + 1;
    assertEquals(size, im.size());
    checkSpan(im, start, end);
  }

  @Test
  public void checkLargeValues() {
    IntIntMapBase map = createMap();
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

  private void insertSpan(IntIntMapBase im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.put(i, i);
    }
  }

  private void checkSpan(IntIntMapBase im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), i);
    }
    // Check outside of span values do not exist in the map
    for (int i = spanStart - 1, idx = 0; idx < 100; i--, idx++) {
      Assert.assertEquals(IntIntMapBase.NO_RESULT, im.get(i));
    }
    for (int i = spanEnd + 1, idx = 0; idx < 100; i++, idx++) {
      Assert.assertEquals(IntIntMapBase.NO_RESULT, im.get(i));
    }
  }

  private void checkSize(IntIntMapBase m, int size) {
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
      IntIntMapBase imap = createMap();
      for (int j = 0; j < arr.length; j++) {
        imap.put(arr[j], arr[j] + 1);
      }
    }
    long elapsed = System.currentTimeMillis() - start;
    System.out.println("Creation: " + elapsed);

    IntIntMapBase imap = createMap();
    for (int j = 0; j < arr.length; j++) {
      imap.put(arr[j], arr[j] + 1);
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
    IntIntMapBase map = createMap();
    map.put(1, 2);
    Assert.assertEquals(2, map.get(1));
    Assert.assertEquals(IntIntMapBase.NO_RESULT, map.get(2));
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
    IntIntMapBase map = createMap();
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
      IntIntMapBase countTable = createMap();
      for (int[] keyVal : keyVals) {
        countTable.put(keyVal[0], keyVal[1]);
      }
    }
    System.out.println("IntIntMapBase creation: " + sw.elapsed(TimeUnit.MILLISECONDS));

    IntIntMapBase countTable = createMap();
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
    System.out.println("IntIntMapBase retrieval: " + sw.elapsed(TimeUnit.MILLISECONDS));
    System.out.println("Verification sum: " + val);
  }
}
