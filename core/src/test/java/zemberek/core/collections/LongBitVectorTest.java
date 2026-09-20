package zemberek.core.collections;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Stopwatch;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


public class LongBitVectorTest {

  @Test(expected = IllegalArgumentException.class)
  public void initializationNegative() {
    new LongBitVector(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void initializationOutOfBOund() {
    new LongBitVector(Integer.MAX_VALUE * 64L + 1L);
  }

  @Test
  public void getSetTest() {
    long[] setBits = {1, 2, 3, 7, 9, 21, 35, 56, 63, 64, 88, 99, 101, 500, 700, 999};
    LongBitVector vector = new LongBitVector(1000);
    for (long setBit : setBits) {
      vector.set(setBit);
    }

    for (int i = 0; i < 1000; i++) {
      if (Arrays.binarySearch(setBits, i) >= 0) {
        Assert.assertTrue(vector.get(i));
      } else {
        Assert.assertFalse(vector.get(i));
      }
    }
  }

  @Test
  public void setTest2() {
    String s = "101100100 01110110 11001010 11111001 01001110 10111011 01111010 11010100";
    LongBitVector vector = LongBitVector.fromBinaryString(s);
    s = s.replaceAll(" ", "");
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '1') {
        Assert.assertTrue(vector.get(i));
      } else {
        Assert.assertFalse(vector.get(i));
      }
    }
  }

  @Test
  public void setGetLastBitIndex() {
    String s = "101100100 01110110 11001010 11111001 01001110 10111011 01111010 11010100";
    LongBitVector vector = LongBitVector.fromBinaryString(s);
    s = s.replaceAll(" ", "");
    assertEquals(s.length() - 3, vector.getLastBitIndex(true));
    assertEquals(s.length() - 1, vector.getLastBitIndex(false));
    vector = LongBitVector.fromBinaryString("000011");
    assertEquals(5, vector.getLastBitIndex(true));
    assertEquals(3, vector.getLastBitIndex(false));
    vector = LongBitVector.fromBinaryString("0000");
    assertEquals(-1, vector.getLastBitIndex(true));
    assertEquals(3, vector.getLastBitIndex(false));
    vector = LongBitVector.fromBinaryString("1111");
    assertEquals(-1, vector.getLastBitIndex(false));
    assertEquals(3, vector.getLastBitIndex(true));
  }


  @Test
  public void getResetTest() {
    long[] resetBits = {1, 2, 3, 7, 9, 21, 35, 56, 63, 64, 88, 99, 101, 500, 700, 999};
    LongBitVector vector = new LongBitVector(1000);
    vector.add(1000, true);
    for (long resetBit : resetBits) {
      vector.clear(resetBit);
    }

    for (int i = 0; i < 1000; i++) {
      if (Arrays.binarySearch(resetBits, i) >= 0) {
        Assert.assertFalse(vector.get(i));
      } else {
        Assert.assertTrue(vector.get(i));
      }
    }
  }

  @Test
  public void appendTestBoolean() {
    LongBitVector vector = new LongBitVector(0);
    vector.add(true);
    vector.add(true);
    vector.add(false);
    vector.add(true);
    assertEquals(4, vector.size());
    for (int i = 0; i < 1000; i++) {
      vector.add(true);
    }
    assertEquals(1004, vector.size());
  }

  @Test
  public void appendTestInteger() {

    LongBitVector vector = new LongBitVector(0);
    vector.add(0x0000ffff, 16);
    assertEquals(16, vector.size());
    vector.add(0x0000ffff, 32);
    assertEquals(48, vector.size());

    LongBitVector vector2 = new LongBitVector(64);
    vector2.add(64, false);
    vector2.add(0xff, 6);
    assertEquals(70, vector2.size());
    Assert.assertFalse(vector2.get(63));
    for (int i = 64; i < 70; i++) {
      Assert.assertTrue(vector2.get(i));
    }
  }

  @Test
  public void fillTest() {
    LongBitVector vector = new LongBitVector(128);
    vector.add(128, false);
    for (int i = 0; i < 128; i++) {
      Assert.assertTrue(!vector.get(i));
    }
    vector.fill(true);
    for (int i = 0; i < 128; i++) {
      Assert.assertTrue(vector.get(i));
    }
    vector.fill(false);
    for (int i = 0; i < 128; i++) {
      Assert.assertTrue(!vector.get(i));
    }

    // check filling with 1 does not effect the overflow smoothnlp.core.bits of the last long.
    vector = new LongBitVector(3);
    vector.add(3, false);
    vector.fill(true);
    assertEquals(vector.getLongArray()[0], 7);

  }

  @Test
  @Ignore("Not a test.")
  public void performanceTest() {
    int itCount = 5;
    Random rnd = new Random(0xbeefcafe);
    final int size = 20000000;
    int[] oneIndexes = new int[size];
    int k = 0;
    for (int i = 0; i < oneIndexes.length; i++) {
      if (rnd.nextDouble() > 0.33) {
        oneIndexes[k] = i;
        k++;
      }
    }
    Arrays.copyOf(oneIndexes, k);
    LongBitVector vector = new LongBitVector(size);

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

  @Test
  public void getLongPiece() {
    LongBitVector vector = new LongBitVector(128);
    vector.add(128, false);
    vector.set(new long[]{0, 2, 4, 7});
    Long result = vector.getLong(0, 5);
    assertEquals("10101", Long.toBinaryString(result));
    vector.set(new long[]{62, 64, 65});
    result = vector.getLong(62, 5);
    assertEquals("1101", Long.toBinaryString(result));
    vector.fill(true);
    vector.clear(new long[]{63, 68});
    assertEquals("101111011", Long.toBinaryString(vector.getLong(61, 9)));
    assertEquals("1", Long.toBinaryString(vector.getLong(61, 1)));
    assertEquals("0", Long.toBinaryString(vector.getLong(63, 1)));
    assertEquals(Long.toBinaryString(0xbfffffffffffffffL),
        Long.toBinaryString(vector.getLong(1, 64)));
  }

  @Test
  public void addLargeAmountBoolean() {
    LongBitVector vector = new LongBitVector(64);
    vector.add(200, true);
    assertEquals(200, vector.size());
    for (int i = 0; i < 200; i++) {
      Assert.assertTrue(vector.get(i));
    }
    vector.add(500, false);
    assertEquals(700, vector.size());
    for (int i = 0; i < 200; i++) {
      Assert.assertTrue(vector.get(i));
    }
    for (int i = 200; i < 700; i++) {
      Assert.assertFalse(vector.get(i));
    }
  }

  @Test
  public void fillMultipleOf64DoesNotOverflow() {
    LongBitVector vector = new LongBitVector(64, 0);
    vector.add(64, false);
    vector.fill(true);
    assertEquals(64, vector.numberOfOnes());
    assertEquals(0, vector.numberOfZeros());
    assertEquals(0, vector.zeroIndexes().length);
    for (int i = 0; i < 64; i++) {
      Assert.assertTrue(vector.get(i));
    }
  }

  @Test
  public void fillDoesNotCorruptUnusedCapacityWords() {
    LongBitVector vector = new LongBitVector(10, 10);
    vector.add(10, false);
    vector.fill(true);
    assertEquals(10, vector.numberOfOnes());
    assertEquals(0, vector.numberOfZeros());
    assertEquals(0, vector.zeroIndexes().length);
  }

  @Test
  public void fillZeroSizeVector() {
    LongBitVector vector = new LongBitVector(0, 0);
    vector.fill(true);
    assertEquals(0, vector.numberOfOnes());
    assertEquals(0, vector.numberOfZeros());
    vector.fill(false);
    assertEquals(0, vector.numberOfOnes());
    assertEquals(0, vector.numberOfZeros());
  }

  @Test
  public void getLongFullWordAtZero() {
    LongBitVector vector = new LongBitVector(64, 0);
    vector.add(64, false);
    vector.set(0);
    vector.set(63);
    long val = vector.getLong(0, 64);
    assertEquals(0x8000000000000001L, val);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getLongInvalidZeroBitAmount() {
    LongBitVector vector = new LongBitVector(64);
    vector.getLong(0, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getLongInvalidTooLargeBitAmount() {
    LongBitVector vector = new LongBitVector(64);
    vector.getLong(0, 65);
  }

  @Test
  public void compressHandlesMultiplesOf64AndEmpty() {
    LongBitVector empty = new LongBitVector(0, 5);
    empty.compress();
    assertEquals(0, empty.size());

    LongBitVector vector = new LongBitVector(64, 10);
    vector.add(64, true);
    vector.compress();
    assertEquals(64, vector.size());
    assertEquals(1, vector.getLongArray().length);
  }

  @Test
  public void ensureSizeErrorMessageHasActualSize() {
    try {
      new LongBitVector(-5);
      Assert.fail("Should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("-5"));
    }
  }

  @Test
  @Ignore("Allocates ~256MB. Run manually.")
  public void zeroIntIndexesRejectsSizesAboveIntRange() {
    LongBitVector vector;
    try {
      long size = (1L << 31) + 5;
      long[] words = new long[(int) ((size + 63) >>> 6)];
      java.util.Arrays.fill(words, -1L);
      vector = new LongBitVector(words, size);
      vector.clear(size - 1);
      vector.clear(size - 2);
    } catch (OutOfMemoryError e) {
      // Needs ~256MB of backing array. Skip rather than fail on a small heap.
      return;
    }
    // The long-indexed variant stays exact.
    Assert.assertArrayEquals(
        new long[]{(1L << 31) + 3, (1L << 31) + 4}, vector.zeroIndexes());
    // The int-indexed variant cannot represent those indexes and must say so.
    try {
      vector.zeroIntIndexes();
      Assert.fail("Should throw IllegalStateException");
    } catch (IllegalStateException expected) {
      // expected
    }
  }

  @Test
  public void zeroIntIndexesWorksAtIntBoundarySize() {
    LongBitVector vector = new LongBitVector(128, 0);
    vector.add(128, true);
    vector.clear(7);
    vector.clear(64);
    Assert.assertArrayEquals(new int[]{7, 64}, vector.zeroIntIndexes());
  }

  @Test
  public void addGrowsWithZeroCapacityInterval() {
    LongBitVector vector = new LongBitVector(0, 0);
    for (int i = 0; i < 1000; i++) {
      vector.add(i % 3 == 0);
    }
    assertEquals(1000, vector.size());
    for (int i = 0; i < 1000; i++) {
      assertEquals(i % 3 == 0, vector.get(i));
    }
  }
}
