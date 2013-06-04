package zemberek.core.bits;


import org.junit.Assert;
import org.junit.Test;

public class SmallFixedBitVectorTest {
    @Test
    public void setGetLastBitIndex() {
        String s = "101100100 01110110 11001010 11110000 11010100";
        int len = s.replaceAll(" ", "").length();
        SmallFixedBitVector vector = SmallFixedBitVector.fromBinaryString(s);

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
            SmallFixedBitVector vector = new SmallFixedBitVector(j);
            for (int i = 0; i < vector.length; i++) {
                Assert.assertEquals(false, vector.get(i));
            }
            for (int i = 0; i < vector.length; i++) {
                vector.set(i);
            }
            for (int i = 0; i < vector.length; i++) {
                Assert.assertEquals(true, vector.get(i));
            }
            for (int i = 0; i < vector.length; i++) {
                vector.clear(i);
            }
            for (int i = 0; i < vector.length; i++) {
                Assert.assertEquals(false, vector.get(i));
            }
        }
    }

    @Test
    public void safeGetSetClear() {
        for (int j = 1; j < 10_000_000; j = j * 2) {
            SmallFixedBitVector vector = new SmallFixedBitVector(j);
            for (int i = 0; i < vector.length; i++) {
                Assert.assertEquals(false, vector.safeGet(i));
            }
            for (int i = 0; i < vector.length; i++) {
                vector.safeSet(i);
            }
            for (int i = 0; i < vector.length; i++) {
                Assert.assertEquals(true, vector.safeGet(i));
            }
            for (int i = 0; i < vector.length; i++) {
                vector.safeClear(i);
            }
            for (int i = 0; i < vector.length; i++) {
                Assert.assertEquals(false, vector.safeGet(i));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeGet() {
        SmallFixedBitVector vector = new SmallFixedBitVector(10);
        vector.safeGet(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeSet() {
        SmallFixedBitVector vector = new SmallFixedBitVector(10);
        vector.safeSet(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeClear() {
        SmallFixedBitVector vector = new SmallFixedBitVector(10);
        vector.safeClear(10);
    }


}
