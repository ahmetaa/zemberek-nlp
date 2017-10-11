package zemberek.core.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class IntIntMapTest {

  @Test
  public void initializesCorrectly() {
    // Check first 1K initial sizes.
    for (int i=1; i < 1000; i++ ) {
      IntIntMap im = new IntIntMap(i);
      checkSize(im, 0);
    }
  }

  @Test
  public void failsOnInvalidSizes() {
    try {
      IntIntMap im;
      im = new IntIntMap(0);
      im = new IntIntMap(-1);
      im = new IntIntMap(Integer.MAX_VALUE);
      im = new IntIntMap(Integer.MIN_VALUE);
      im = new IntIntMap(1 << 29 + 1);
      Assert.fail("Illegal size should have thrown an exception.");
    } catch (RuntimeException e) {
      // Nothing to do
    }
  }

  @Test
  public void expandsCorrectly() {
    // Create maps with different sizes and add size * 10 elements to each.
    for (int i=1; i < 100; i++ ) {
      IntIntMap im = new IntIntMap(i);
      // Insert i * 10 elements to each and confirm sizes
      int elements = i * 10;
      for (int j=0; j< elements; j++) {
        im.put(j, j+13);
      }
      for (int j=0; j< elements; j++) {
        Assert.assertEquals(im.get(j), j+13);
      }
      checkSize(im, elements);
    }
  }

  @Test
  public void putAddsAndUpdatesElementsCorrectly() {
    int span = 100;
    for (int i=0; i<span; i++) {
      IntIntMap im = new IntIntMap();
      checkSpanInsertions(im, -i, i);
    }
    // Do the same, this time overwrite values as well
    IntIntMap im = new IntIntMap();
    for (int i=0; i<span; i++) {
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
    }
  }

  @Test
  public void survivesSimpleFuzzing() {
    List<int[]> fuzzLists = TestUtils.createFuzzingLists();
    for (int[] arr : fuzzLists) {
      IntIntMap im = new IntIntMap();
      for (int i=0; i<arr.length; i++) {
        im.put(arr[i], arr[i] + 7);
        assertEquals(im.get(arr[i]), arr[i] + 7);
      }
    }

    IntIntMap im = new IntIntMap();
    for (int[] arr : fuzzLists) {
      for (int i=0; i<arr.length; i++) {
        im.put(arr[i], arr[i] + 7);
        assertEquals(im.get(arr[i]), arr[i] + 7);
      }
    }
  }

  private void checkSpanInsertions(IntIntMap im, int start, int end) {
    insertSpan(im, start, end);
    // Expected size.
    int size = Math.abs(start) + Math.abs(end) + 1;
    assertEquals(size, im.size());
    checkSpan(im, start, end);
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
    for (int i = spanStart - 1, idx=0; idx < 100; i--, idx++) {
      Assert.assertEquals(IntIntMap.NO_RESULT, im.get(i));
    }
    for (int i = spanEnd + 1, idx=0; idx < 100; i++, idx++) {
      Assert.assertEquals(IntIntMap.NO_RESULT, im.get(i));
    }
  }

  private void checkSize(IntIntMap m, int size) {
    assertEquals(size, m.size());
    assertTrue(m.capacity() > m.size());
    // Check capacity is 2^n
    assertTrue((m.capacity() & (m.capacity() - 1)) == 0);
  }



}
