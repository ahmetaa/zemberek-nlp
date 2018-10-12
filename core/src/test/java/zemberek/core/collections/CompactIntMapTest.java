package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompactIntMapTest {

  @Test
  public void initializesCorrectly() {
    // Check first 1K initial sizes.
    for (int i = 1; i < 1000; i++) {
      CompactIntMap im = new CompactIntMap(i);
      checkSize(im, 0);
    }
  }

  @Test
  public void failsOnInvalidSizes() {
    try {
      CompactIntMap im;
      im = new CompactIntMap(0);
      im = new CompactIntMap(-1);
      im = new CompactIntMap(Integer.MAX_VALUE);
      im = new CompactIntMap(Integer.MIN_VALUE);
      im = new CompactIntMap(Integer.MIN_VALUE + 1);
      im = new CompactIntMap(1 << 29 + 1);
      Assert.fail("Illegal size should have thrown an exception.");
    } catch (RuntimeException e) {
      // Nothing to do
    }
  }

  @Test
  public void expandsCorrectly() {
    // Create maps with different sizes and add size * 10 elements to each.
    for (int i = 1; i < 100; i++) {
      CompactIntMap im = new CompactIntMap(i);
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
      CompactIntMap im = new CompactIntMap();
      checkSpanInsertions(im, -i, i);
    }
    // Do the same, this time overwrite values as well
    CompactIntMap im = new CompactIntMap();
    for (int i = 0; i < span; i++) {
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
    }
  }

  @Test
  public void removeRemovesCorrectly() {
    CompactIntMap im = new CompactIntMap();
    im.put(0,0);
    assertEquals(im.get(0), 0);
    im.remove(0);
    assertEquals(im.get(0), CompactIntMap.NO_RESULT);
    assertEquals(im.size(), 0);
    // remove again works
    im.remove(0);
    assertEquals(im.get(0), CompactIntMap.NO_RESULT);
    assertEquals(im.size(), 0);
    im.put(0, 1);
    assertEquals(im.size(), 1);
    assertEquals(im.get(0), 1);
  }

  @Test
  public void removeSpansWorksCorrectly() {
    CompactIntMap im = new CompactIntMap();
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
    CompactIntMap im = new CompactIntMap();
    int limit = 9999;
    insertSpan(im, 0, limit);
    int[] r = TestUtils.createRandomUintArray(1000, limit);
    for (int i : r) {
      im.remove(i);
    }
    for (int i : r) {
      assertEquals(im.get(i), CompactIntMap.NO_RESULT);
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
      CompactIntMap im = new CompactIntMap();
      for (int i = 0; i < arr.length; i++) {
        im.put(arr[i], arr[i] + 7);
        assertEquals(im.get(arr[i]), arr[i] + 7);
      }
    }

    CompactIntMap im = new CompactIntMap();
    for (int[] arr : fuzzLists) {
      for (int i = 0; i < arr.length; i++) {
        im.put(arr[i], arr[i] + 7);
        assertEquals(im.get(arr[i]), arr[i] + 7);
      }
    }
  }

  private void removeSpan(CompactIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.remove(i);
    }
  }

  private void checkSpanRemoved(CompactIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), CompactIntMap.NO_RESULT);
    }
  }

  private void checkSpanInsertions(CompactIntMap im, int start, int end) {
    insertSpan(im, start, end);
    // Expected size.
    int size = Math.abs(start) + Math.abs(end) + 1;
    assertEquals(size, im.size());
    checkSpan(im, start, end);
  }

  private void insertSpan(CompactIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.put(i, i);
    }
  }

  private void checkSpan(CompactIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), i);
    }
    // Check outside of span values do not exist in the map
    for (int i = spanStart - 1, idx = 0; idx < 100; i--, idx++) {
      Assert.assertEquals(CompactIntMap.NO_RESULT, im.get(i));
    }
    for (int i = spanEnd + 1, idx = 0; idx < 100; i++, idx++) {
      Assert.assertEquals(CompactIntMap.NO_RESULT, im.get(i));
    }
  }

  private void checkSize(CompactIntMap m, int size) {
    assertEquals(size, m.size());
    assertTrue(m.capacity() > m.size());
    // Check capacity is 2^n
    assertTrue((m.capacity() & (m.capacity() - 1)) == 0);
  }

  @Test
  @Ignore("Not a unit test")
  public void testPerformance() {
    int[] arr = TestUtils.createRandomUintArray(1_000_000, 1<<29);
    long sum =0;
    int iter = 100;
    long start = System.currentTimeMillis();
    for (int i=0; i<iter; i++) {
      CompactIntMap imap = new CompactIntMap();
      for (int j=0; j<arr.length; j++) {
        imap.put(arr[j], arr[j] + 1);
      }
    }
    long elapsed = System.currentTimeMillis() - start;
    System.out.println("Creation: " + elapsed);

    CompactIntMap imap = new CompactIntMap();
    for (int j=0; j<arr.length; j++) {
      imap.put(arr[j], arr[j] + 1);
    }
    start = System.currentTimeMillis();
    for (int i=0; i<iter; i++) {
      for (int j=arr.length-1; j >=0; j--) {
        sum += imap.get(arr[j]);
      }
    }
    elapsed = System.currentTimeMillis() - start;
    System.out.println("Retrieval: " + elapsed);
    System.out.println("Val: " + sum);
  }

  @Test
  public void getTest2() {
    CompactIntMap map = new CompactIntMap();
    map.put(1, 2);
    Assert.assertEquals(2, map.get(1));
    Assert.assertEquals(CompactIntMap.NO_RESULT, map.get(2));
    map.put(1, 3);
    Assert.assertEquals(3, map.get(1));

    map = new CompactIntMap();
    for (int i = 0; i < 100000; i++) {
      map.put(i, i + 1);
    }
    for (int i = 0; i < 100000; i++) {
      Assert.assertEquals(i + 1, map.get(i));
    }
  }

  @Test
  public void removeTest2() {
    CompactIntMap map = new CompactIntMap();
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
      CompactIntMap countTable = new CompactIntMap();
      for (int[] keyVal : keyVals) {
        countTable.put(keyVal[0], keyVal[1]);
      }
    }
    System.out.println("CompactIntMap creation: " + sw.elapsed(TimeUnit.MILLISECONDS));

    CompactIntMap countTable = new CompactIntMap();
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
    System.out.println("CompactIntMap retrieval: " + sw.elapsed(TimeUnit.MILLISECONDS));
    System.out.println("Verification sum: " + val);
  }

}
