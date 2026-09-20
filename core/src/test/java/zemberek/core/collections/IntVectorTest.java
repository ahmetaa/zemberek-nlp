package zemberek.core.collections;

import java.util.Arrays;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class IntVectorTest {

  @Test
  public void testConstructor() {
    IntVector darray = new IntVector();
    Assert.assertEquals(0, darray.size());
    Assert.assertEquals(7, darray.capacity());
  }

  @Test
  public void testConstructor2() {
    IntVector darray = new IntVector(1);
    Assert.assertEquals(0, darray.size());
    Assert.assertEquals(1, darray.capacity());
  }

  @Test
  public void testAdd() {
    IntVector darray = new IntVector();
    for (int i = 0; i < 10000; i++) {
      darray.add(i);
    }
    Assert.assertEquals(10000, darray.size());
    for (int i = 0; i < 10000; i++) {
      Assert.assertEquals(i, darray.get(i));
    }
  }

  @Test
  public void testAddAll() {
    int[] d1 = {2, 4, 5, 17, -1, -2, 5, -123};
    IntVector darray = new IntVector();
    IntVector i = new IntVector(d1);
    darray.addAll(i);
    Assert.assertEquals(i, darray);
  }

  @Test
  public void testAddAllVector() {
    int[] d1 = {2, 4, 5, 17, -1, -2, 5, -123};
    IntVector darray = new IntVector();
    darray.addAll(d1);
    Assert.assertEquals(8, darray.size());
    Assert.assertArrayEquals(d1, darray.copyOf());
    darray.addAll(d1);
    Assert.assertEquals(16, darray.size());
    Assert.assertEquals(2, darray.get(0));
    Assert.assertEquals(-123, darray.get(15));
    Assert.assertArrayEquals(d1, Arrays.copyOfRange(darray.copyOf(), 8, 16));
  }


  @Test
  public void testTrimToSize() {
    IntVector darray = new IntVector();
    for (int i = 0; i < 10000; i++) {
      darray.add(i);
    }
    Assert.assertEquals(10000, darray.size());
    Assert.assertNotEquals(darray.size(), darray.capacity());
    darray.trimToSize();
    Assert.assertEquals(10000, darray.size());
    Assert.assertEquals(10000, darray.copyOf().length);
    Assert.assertEquals(darray.size(), darray.capacity());
  }

  @Test
  public void safeSetChecksTheIndex() {
    IntVector vector = new IntVector(new int[]{1, 2, 3});
    vector.safeSet(1, 20);
    Assert.assertEquals(20, vector.get(1));
    assertRejectsIndex(vector, -1);
    assertRejectsIndex(vector, 3);
    // Spare capacity is not part of the vector.
    Assert.assertTrue(vector.capacity() > vector.size());
    assertRejectsIndex(vector, vector.capacity() - 1);
  }

  private void assertRejectsIndex(IntVector vector, int index) {
    try {
      vector.safeSet(index, 0);
      Assert.fail("Index " + index + " should have been rejected.");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // expected
    }
  }

  @Test
  public void sortOnlyTouchesTheUsedPart() {
    IntVector vector = new IntVector(2);
    vector.add(5);
    vector.add(-3);
    vector.add(9);
    vector.add(0);
    vector.sort();
    Assert.assertArrayEquals(new int[]{-3, 0, 5, 9}, vector.copyOf());
    Assert.assertEquals(4, vector.size());
  }

  @Test
  public void containsAndIsEmpty() {
    IntVector vector = new IntVector();
    Assert.assertTrue(vector.isempty());
    Assert.assertFalse(vector.contains(0));
    vector.add(7);
    Assert.assertFalse(vector.isempty());
    Assert.assertTrue(vector.contains(7));
    Assert.assertFalse(vector.contains(8));
  }

  @Test
  public void copyOfIsASnapshot() {
    IntVector vector = new IntVector(new int[]{1, 2, 3});
    int[] copy = vector.copyOf();
    Assert.assertArrayEquals(new int[]{1, 2, 3}, copy);
    copy[0] = 100;
    vector.set(1, 200);
    Assert.assertArrayEquals(new int[]{1, 200, 3}, vector.copyOf());
  }

  /**
   * Two vectors with the same content must be equal even when their backing arrays differ in
   * length, and shuffling must keep the content, only its order changes.
   */
  @Test
  public void equalsIgnoresSpareCapacity() {
    IntVector fromArray = new IntVector(new int[]{1, 2, 3});
    IntVector built = new IntVector(1);
    built.add(1);
    built.add(2);
    built.add(3);

    Assert.assertNotEquals(fromArray.capacity(), built.capacity());
    Assert.assertEquals(fromArray, built);
    Assert.assertEquals(fromArray.hashCode(), built.hashCode());

    built.add(4);
    Assert.assertNotEquals(fromArray, built);
  }

  @Test
  public void shuffleKeepsTheContent() {
    IntVector vector = new IntVector();
    for (int i = 0; i < 100; i++) {
      vector.add(i);
    }
    vector.shuffle(new Random(1));
    Assert.assertEquals(100, vector.size());
    int[] values = vector.copyOf();
    Arrays.sort(values);
    for (int i = 0; i < 100; i++) {
      Assert.assertEquals(i, values[i]);
    }
  }

  @Test
  public void negativeCapacityIsRejected() {
    try {
      new IntVector(-1);
      Assert.fail("A negative capacity should have been rejected.");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }
}
