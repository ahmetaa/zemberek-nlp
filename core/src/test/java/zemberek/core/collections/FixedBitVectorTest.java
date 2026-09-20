package zemberek.core.collections;


import com.google.common.base.Stopwatch;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FixedBitVectorTest {

  @Test
  public void setGetLastBitIndex() {
    String s = "101100100 01110110 11001010 11110000 11010100";
    int len = s.replaceAll(" ", "").length();
    FixedBitVector vector = FixedBitVector.fromBinaryString(s);

    Assert.assertEquals(len, vector.length);
    Assert.assertTrue(vector.get(0));
    Assert.assertTrue(vector.get(2));
    Assert.assertTrue(vector.get(3));
    Assert.assertTrue(vector.get(len - 3));
    Assert.assertTrue(vector.get(len - 5));

    Assert.assertFalse(vector.get(1));
    Assert.assertFalse(vector.get(4));
    Assert.assertFalse(vector.get(5));
    Assert.assertFalse(vector.get(len - 1));
    Assert.assertFalse(vector.get(len - 2));

    Assert.assertEquals(21, vector.numberOfOnes());
    Assert.assertEquals(len - 21, vector.numberOfZeroes());
  }

  @Test
  public void getSetClear() {
    for (int j = 1; j < 10_000_000; j = j * 2) {
      FixedBitVector vector = new FixedBitVector(j);
      for (int i = 0; i < vector.length; i++) {
        Assert.assertFalse(vector.get(i));
      }
      for (int i = 0; i < vector.length; i++) {
        vector.set(i);
      }
      for (int i = 0; i < vector.length; i++) {
        Assert.assertTrue(vector.get(i));
      }
      for (int i = 0; i < vector.length; i++) {
        vector.clear(i);
      }
      for (int i = 0; i < vector.length; i++) {
        Assert.assertFalse(vector.get(i));
      }
    }
  }

  @Test
  public void safeGetSetClear() {
    for (int j = 1; j < 10_000_000; j = j * 2) {
      FixedBitVector vector = new FixedBitVector(j);
      for (int i = 0; i < vector.length; i++) {
        Assert.assertFalse(vector.safeGet(i));
      }
      for (int i = 0; i < vector.length; i++) {
        vector.safeSet(i);
      }
      for (int i = 0; i < vector.length; i++) {
        Assert.assertTrue(vector.safeGet(i));
      }
      for (int i = 0; i < vector.length; i++) {
        vector.safeClear(i);
      }
      for (int i = 0; i < vector.length; i++) {
        Assert.assertFalse(vector.safeGet(i));
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void safeGet() {
    FixedBitVector vector = new FixedBitVector(10);
    vector.safeGet(10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void safeSet() {
    FixedBitVector vector = new FixedBitVector(10);
    vector.safeSet(10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void safeClear() {
    FixedBitVector vector = new FixedBitVector(10);
    vector.safeClear(10);
  }

  @Test
  public void largeLengthConstructorDoesNotOverflow() {
    try {
      FixedBitVector vector = new FixedBitVector(Integer.MAX_VALUE - 30);
      Assert.assertEquals(Integer.MAX_VALUE - 30, vector.length);
    } catch (OutOfMemoryError e) {
      // In constrained memory environments, OOM is acceptable,
      // but it must never throw NegativeArraySizeException.
    }
  }

  @Test
  public void zeroLengthVector() {
    FixedBitVector vector = new FixedBitVector(0);
    Assert.assertEquals(0, vector.length);
    Assert.assertEquals(0, vector.numberOfOnes());
    Assert.assertEquals(0, vector.numberOfZeroes());
    Assert.assertEquals(0, vector.zeroIndexes().length);
  }

  @Test
  public void numberOfOnesIgnoresPaddingBits() {
    FixedBitVector vector = new FixedBitVector(10);
    // set() is unchecked, so this lands on a padding bit of the single backing word.
    vector.set(20);
    Assert.assertEquals(0, vector.numberOfOnes());
    Assert.assertEquals(10, vector.numberOfZeroes());
    Assert.assertEquals(10, vector.zeroIndexes().length);
    vector.set(3);
    Assert.assertEquals(1, vector.numberOfOnes());
    Assert.assertEquals(9, vector.zeroIndexes().length);
  }

  @Test
  public void numberOfOnesAtWordBoundary() {
    FixedBitVector vector = new FixedBitVector(32);
    for (int i = 0; i < 32; i++) {
      vector.set(i);
    }
    Assert.assertEquals(32, vector.numberOfOnes());
    Assert.assertEquals(0, vector.numberOfZeroes());
  }

  @Test(expected = IllegalArgumentException.class)
  public void differentBitCountRejectsLengthMismatch() {
    new FixedBitVector(100).differentBitCount(new FixedBitVector(5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void numberOfNewOneBitCountRejectsLengthMismatch() {
    new FixedBitVector(100).numberOfNewOneBitCount(new FixedBitVector(5));
  }

  @Test
  public void comparisonsAcceptEqualLengths() {
    FixedBitVector a = FixedBitVector.fromBinaryString("1100");
    FixedBitVector b = FixedBitVector.fromBinaryString("1010");
    Assert.assertEquals(2, a.differentBitCount(b));
    Assert.assertEquals(1, a.numberOfNewOneBitCount(b));
  }

  @Test
  @Ignore("Not a test.")
  public void performanceTest() {
    int itCount = 5;
    Random rnd = new Random(0xbeefcafe);
    final int size = 20_000_000;
    int[] oneIndexes = new int[size];
    int k = 0;
    for (int i = 0; i < oneIndexes.length; i++) {
      if (rnd.nextDouble() > 0.33) {
        oneIndexes[k] = i;
        k++;
      }
    }
    FixedBitVector vector = new FixedBitVector(size);
    Arrays.copyOf(oneIndexes, k);
    for (int i = 0; i < itCount; i++) {
      Stopwatch sw = Stopwatch.createStarted();
      for (int oneIndex : oneIndexes) {
        vector.set(oneIndex);
      }
      for (int oneIndex : oneIndexes) {
        vector.clear(oneIndex);
      }
      System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));
    }
  }


}
