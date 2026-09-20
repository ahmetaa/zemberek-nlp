package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class UIntSetTest {

  @Test
  public void containsSet() {
    UIntSet set = new UIntSet();
    for (int i = 0; i < 100000; i++) {
      set.add(i);
    }

    for (int i = 0; i < 200000; i++) {
      if (i < 100000) {
        Assert.assertTrue(set.contains(i));
      } else {
        Assert.assertFalse(set.contains(i));
      }
    }
  }

  @Test
  public void stressTest() {
    UIntSet set = new UIntSet();
    int size = 10000;
    for (int i = 0; i < size; i++) {
      set.add(i);
    }
    Random rnd = new Random();
    int[] removed = new int[size];
    for (int i = 0; i < 5000; i++) {
      int key = rnd.nextInt(size);
      removed[key] = 1;
      set.remove(key);
    }

    for (int i = 0; i < size; i++) {
      if (removed[i] == 0) {
        Assert.assertTrue(set.contains(i));
      } else {
        Assert.assertFalse(set.contains(i));
      }
    }

    for (int i = 0; i < 2000; i++) {
      int key = rnd.nextInt(size);
      removed[key] = 0;
      set.add(key);
    }

    for (int i = 0; i < size; i++) {
      if (removed[i] == 0) {
        Assert.assertTrue(set.contains(i));
      } else {
        Assert.assertFalse(set.contains(i));
      }
    }
  }

  @Test
  public void removeTest() {
    UIntSet set = new UIntSet();
    int count = 1000;
    for (int i = 0; i < count; i++) {
      set.add(i);
    }
    Assert.assertEquals(count, set.size());
    int removedCount = 0;
    for (int i = 0; i < count; i += 3) {
      set.remove(i);
      removedCount++;
    }
    Assert.assertEquals(count - removedCount, set.size());

    for (int i = 0; i < count; i += 3) {
      Assert.assertFalse(set.contains(i));
    }

    for (int i = 0; i < count; i++) {
      set.add(i);
    }

    Assert.assertEquals(count, set.size());

    for (int i = 0; i < count; i += 3) {
      Assert.assertTrue(set.contains(i));
    }
  }


  @Test
  @Ignore("Not a unit test")
  public void performance() {
    Random r = new Random();
    int[] keys = new int[1000000];
    final int itCount = 10;
    for (int i = 0; i < keys.length; i++) {
      keys[i] = r.nextInt(500000);
    }
    Stopwatch sw = Stopwatch.createStarted();
    for (int j = 0; j < itCount; j++) {

      Set<Integer> set = new HashSet<>();

      for (int key1 : keys) {
        set.add(key1);
      }

      for (int key : keys) {
        set.contains(key);
      }

      for (int key : keys) {
        if (set.contains(key)) {
          set.remove(key);
        }
      }
    }
    System.out.println("Set Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));

    sw = Stopwatch.createStarted();

    for (int j = 0; j < itCount; j++) {

      UIntSet set = new UIntSet();

      for (int key1 : keys) {
        set.add(key1);
      }
      for (int key : keys) {
        set.contains(key);
      }

      for (int key : keys) {
        if (set.contains(key)) {
          set.remove(key);
        }
      }
    }
    System.out.println("Uint Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
  }

  @Test
  public void addReportsWhetherTheKeyWasNew() {
    UIntSet set = new UIntSet();
    Assert.assertTrue(set.add(5));
    Assert.assertFalse(set.add(5));
    Assert.assertEquals(1, set.size());

    set.remove(5);
    Assert.assertEquals(0, set.size());
    Assert.assertFalse(set.contains(5));
    // The tombstone left behind must not make the key look present again.
    Assert.assertTrue(set.add(5));
    Assert.assertEquals(1, set.size());
  }

  @Test
  public void negativeKeysAreRejected() {
    UIntSet set = new UIntSet();
    try {
      set.add(-1);
      Assert.fail("A negative key should have been rejected.");
    } catch (IllegalArgumentException expected) {
      // expected
    }
    Assert.assertEquals(0, set.size());
  }

  @Test
  public void ofAndAddAllCollapseDuplicates() {
    UIntSet set = UIntSet.of(1, 2, 2, 3);
    Assert.assertEquals(3, set.size());
    set.addAll(3, 4, 5);
    Assert.assertEquals(5, set.size());
    for (int i = 1; i <= 5; i++) {
      Assert.assertTrue("missing " + i, set.contains(i));
    }
  }

  @Test
  public void sortedKeysSkipRemovedOnes() {
    UIntSet set = new UIntSet();
    for (int i = 0; i < 100; i++) {
      set.add(i);
    }
    for (int i = 0; i < 100; i += 2) {
      set.remove(i);
    }
    int[] expected = new int[50];
    for (int i = 0; i < 50; i++) {
      expected[i] = i * 2 + 1;
    }
    Assert.assertEquals(50, set.size());
    Assert.assertArrayEquals(expected, set.getKeyArraySorted());
    Assert.assertEquals(50, set.getKeysSorted().size());
    Assert.assertEquals(Integer.valueOf(1), set.getKeysSorted().get(0));
    Assert.assertEquals(Integer.valueOf(99), set.getKeysSorted().get(49));
  }
}
