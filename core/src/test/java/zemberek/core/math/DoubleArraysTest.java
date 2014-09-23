package zemberek.core.math;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;

public class DoubleArraysTest {

    static double delta = 0.0001;

    @Test
    public void testSum() {
        double[] da = {1, 2, 0, -1, -3};
        double[] da2 = {0.5, -2, 30, 1, -30};
        Assert.assertTrue(inDelta(DoubleArrays.sum(da), -1));
        Assert.assertTrue(inDelta(DoubleArrays.sum(da, da2), new double[]{1.5, 0, 30, 0, -33}));
        DoubleArrays.addToFirst(da, da2);
        Assert.assertTrue(inDelta(da, new double[]{1.5, 0, 30, 0, -33}));
    }

    @Test
    public void addToFirstScaledTest() {
        double[] da1 = {1, 2, 0, -1, -30};
        double[] da2 = {-0.5, -1, 0, 0.5, 15};
        DoubleArrays.addToFirstScaled(da1, da2, 2);
        Assert.assertEquals(DoubleArrays.max(da1), 0d, 0.0001);
        Assert.assertEquals(DoubleArrays.min(da1), 0d, 0.0001);
    }

    @Test
    public void testDotProduct() {
        double[] da = {1, 2, 0, -1, -3};
        double[] da2 = {0.5, -2, 30, 1, -30};
        Assert.assertTrue(inDelta(DoubleArrays.dotProduct(da, da2), 85.5));
    }

    @Test
    public void testSubstract() {
        double[] da = {1, 2, 0, -1, -3};
        double[] da2 = {0.5, -2, 30, 1, -30};
        Assert.assertTrue(inDelta(DoubleArrays.subtract(da, da2), new double[]{0.5, 4, -30, -2, 27}));
        DoubleArrays.subtractFromFirst(da, da2);
        Assert.assertTrue(inDelta(da, new double[]{0.5, 4, -30, -2, 27}));
    }

    @Test
    public void testAppendZeros() {
        double[] da = {1, 2, 0, -1, -3};
        double[] da2 = {1, 2, 0, -1, -3, 0, 0, 0, 0, 0, 0};
        Assert.assertTrue(inDelta(DoubleArrays.appendZeros(da, 6), da2));
        Assert.assertTrue(inDelta(DoubleArrays.appendZeros(da2, 0), da2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAppendZerosExc() {
        double[] da = {1, 2, 0, -1, -3};
        DoubleArrays.appendZeros(da, -10);
    }

    @Test
    public void testMaxMinValue() {
        double[] da = {1, 2, 0, -1, -3};
        Assert.assertTrue(inDelta(DoubleArrays.max(da), 2));
        Assert.assertTrue(inDelta(DoubleArrays.min(da), -3));
    }

    @Test
    public void testMaxMinIndex() {
        double[] da = {1, 2, 0, -1, -3};
        Assert.assertEquals(DoubleArrays.maxIndex(da), 1);
        Assert.assertEquals(DoubleArrays.minIndex(da), 4);
        double[] da2 = {2, 2, 0, -1, -3, 2, -3};
        Assert.assertEquals(DoubleArrays.maxIndex(da2), 0);
        Assert.assertEquals(DoubleArrays.minIndex(da2), 4);
    }

    @Test
    public void testSquare() {
        double[] da = {1, 2, 0, -1, -3};
        Assert.assertTrue(inDelta(DoubleArrays.square(da), new double[]{1, 4, 0, 1, 9}));
    }

    @Test
    public void testSquaredSum() {
        double[] da = {1, 2, 0, -1, -3};
        Assert.assertTrue(inDelta(DoubleArrays.squaredSum(da), 15));
        double[] da2 = {0, 1, 1, -2, 1};
        Assert.assertTrue(inDelta(DoubleArrays.squaredSumOfDifferences(da, da2), 20));
        Assert.assertTrue(inDelta(DoubleArrays.absoluteDifference(da, da2), new double[]{1, 1, 1, 1, 4}));
    }

    @Test
    public void testAddToAll() {
        double[] da = {1, 2, 0, -1, -3};
        double[] expected = {2, 3, 1, 0, -2};
        DoubleArrays.addToAll(da, 1);
        Assert.assertTrue(Arrays.equals(expected, da));
    }

    @Test
    public void testMean() {
        double[] da = {1, 2, 0, -1, -3};
        Assert.assertTrue(inDelta(DoubleArrays.mean(da), -1d / 5d));
    }

    @Test
    public void testInRange() {
        double d = 3;
        double d2 = 2.5;
        double d3 = 7;
        Assert.assertTrue(DoubleArrays.inRange(d, d2, 1));
        Assert.assertFalse(DoubleArrays.inRange(d2, d3, 4));
        Assert.assertTrue(DoubleArrays.inRange(d, d2, 0.5));
    }

    @Test
    public void testReverse() {
        double[] da = {1, 3, 7, 2.5, 0};
        double[] da2 = DoubleArrays.reverse(DoubleArrays.reverse(da));
        Assert.assertNotNull(da2);
        Assert.assertEquals(da.length, da2.length);
        Assert.assertTrue(inDelta(da, da2));
    }

    @Test
    public void testConvertInt() {
        int[] ia = {1, 3, -1, 0, 9, 12};
        double[] da2 = {1, 3, -1, 0, 9.0, 12.0};
        double[] da = DoubleArrays.convert(ia);
        Assert.assertNotNull(da);
        Assert.assertTrue(inDelta(da, da2));
    }

    @Test
    public void testConvert2d() {
        int[][] ia = {{1, 2}, {4, 3}, {-1, 5}};
        double[][] da = {{1, 2}, {4, 3}, {-1, 5}};
        double[][] da2 = DoubleArrays.convert(ia);
        Assert.assertEquals(ia.length, da2.length);
        Assert.assertNotNull(da2);
        int k = 0;
        for (double[] i : da) {
            int j = 0;
            for (double ii : i) {
                Assert.assertEquals(da2[k][j++], ii, 0.0001);
            }
            k++;
        }
    }

    @Test
    public void testConvertFloat() {
        float[] fa = {1, 3, -1, 0, 9, 12};
        double[] da2 = {1, 3, -1, 0, 9.0, 12.0};
        double[] da = DoubleArrays.convert(fa);
        Assert.assertNotNull(da);
        Assert.assertTrue(inDelta(da, da2));
    }

    @Test
    public void testArrayEqualsInRange() {
        double[] da = {0, 3.5, 7, -2, 19};
        double[] da2 = {0, 4.5, 4, 2, -18};
        Assert.assertTrue(DoubleArrays.arrayEqualsInRange(da, da2, 37));
        Assert.assertFalse(DoubleArrays.arrayEqualsInRange(da, da2, 15));
        Assert.assertTrue(DoubleArrays.arrayEqualsInRange(da, da2, 45));
    }

    @Test
    public void testArrayEquals() {
        double[] da = {-1, 0, 3.4, 7};
        double[] da2 = {-1, 0.0, 3.4, 7};
        double[] da3 = {7, 9.0, 12.3, -5.6};
        Assert.assertTrue(DoubleArrays.arrayEquals(da, da2));
        Assert.assertFalse(DoubleArrays.arrayEquals(da2, da3));
    }

    @Test
    public void testMultiply() {
        double[] da = {-1, 1.3, 8.2, 10, 90};
        double[] da2 = {-1, 3, 2, 2, -1};
        Assert.assertTrue(inDelta(DoubleArrays.multiply(da, da2), new double[]{1, 3.9, 16.4, 20, -90}));
        DoubleArrays.multiplyToFirst(da, da2);
        Assert.assertTrue(inDelta(da, new double[]{1, 3.9, 16.4, 20, -90}));
    }

    @Test
    public void testScale() {
        double[] da = {1, -1, 3.1, 0.0, 0};
        Assert.assertTrue(inDelta(DoubleArrays.scale(da, 0), new double[]{0, 0, 0, 0, 0}));
        Assert.assertTrue(inDelta(DoubleArrays.scale(da, -1), new double[]{-1, 1, -3.1, 0, 0.0}));
        Assert.assertTrue(inDelta(DoubleArrays.scale(da, 4), new double[]{4, -4, 12.4, 0.0, 0.0}));

        DoubleArrays.scaleInPlace(da, 4);
        Assert.assertTrue(inDelta(da, new double[]{4, -4, 12.4, 0.0, 0.0}));
        DoubleArrays.scaleInPlace(da, 0);
        Assert.assertTrue(inDelta(da, new double[]{0, 0, 0, 0, 0}));
    }

    @Test
    public void testAbsoluteDifference() {
        double[] da = {1, 2, 4.5, -2, 4};
        double[] da2 = {1, 1, 2.5, 5, -15.2};
        Assert.assertTrue(inDelta(DoubleArrays.absoluteDifference(da, da2), new double[]{0, 1, 2, 7, 19.2}));
        Assert.assertEquals(DoubleArrays.absoluteSumOfDifferences(da, da2), 29.2, 0.0001);
    }

    @Test
    public void testVariance() {
        double[] da = {0, 2, 4};
        Assert.assertEquals(DoubleArrays.variance(da), 4d, 0.0001);
        Assert.assertEquals(DoubleArrays.standardDeviation(da), 2d, 0.0001);
    }

    @Test
    public void testContainsNaN() {
        double[] da = {-1, 1, 3, 5, 7, 9, Double.NaN};
        Assert.assertTrue(DoubleArrays.containsNaN(da));

        double[] da2 = {-1, 1, 3, 5, 7, 9};
        Assert.assertFalse(DoubleArrays.containsNaN(da2));
    }

    @Test
    public void testFloorInPlace() {
        double[] da = {0, -7, 2, 1.1123, -10, -22, 56};
        DoubleArrays.floorInPlace(da, -7);
        Assert.assertTrue(inDelta(da, new double[]{0, -7, 2, 1.1123, -7, -7, 56}));

    }

    @Test
    public void testNonZeroFloorInPlace() {
        double[] da = {0, -7, 2, 1.1123, -10, -22, 56};
        DoubleArrays.nonZeroFloorInPlace(da, 3);
        Assert.assertTrue(inDelta(da, new double[]{0, 3, 3, 3, 3, 3, 56}));

    }

    @Test
    public void testNormalizeInPlace() {
        double[] da = {1, 5.6, 2.4, -1, -3.0, 5};
        DoubleArrays.normalizeInPlace(da);
        Assert.assertTrue(inDelta(da, new double[]{0.1, 0.56, 0.24, -0.1, -0.3, 0.5}));
    }

    @Test
    public void testValidateArray() {
        double[] da = null;
        try {
            DoubleArrays.validateArray(da);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        da = new double[0];
        try {
            DoubleArrays.validateArray(da);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testValidateArrays() {
        double[] da1 = {1, 2, 3};
        double[] da2 = null;
        try {
            DoubleArrays.validateArrays(da1, da2);
        } catch (NullPointerException e) {
            Assert.assertTrue(true);
        }

        double[] da3 = {5, 6, 2.33, 2};
        try {
            DoubleArrays.validateArrays(da1, da3);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNormalize16bitLittleEndian() {
        byte[] ba = {0x10, 0x71, 0x18, 0x54};
        double[] da = DoubleArrays.normalize16bitLittleEndian(ba);
        Assert.assertEquals(da[0] * Short.MAX_VALUE, 28944d, 0.0001);
        Assert.assertEquals(da[1] * Short.MAX_VALUE, 21528d, 0.0001);

        byte[] ba2 = DoubleArrays.denormalize16BitLittleEndian(da);
        Assert.assertEquals(ba2[0], 0x10);
        Assert.assertEquals(ba2[3], 0x54);

        byte[] ba3 = DoubleArrays.denormalizeLittleEndian(da, 16);
        Assert.assertEquals(ba3[0], 0x10);
        Assert.assertEquals(ba3[3], 0x54);

        byte[] ba4 = {(byte) 0xCC, (byte) 0xAB};
        da = DoubleArrays.normalize16bitLittleEndian(ba4);
        Assert.assertEquals(da[0] * Short.MIN_VALUE, 21556d, 0.0001);
    }

    @Test
    public void testToUnsignedInteger() {
        double[] da = {0.2, -0.4, 0.6};
        int[] ia = DoubleArrays.toUnsignedInteger(da, 6);
        Assert.assertEquals((int) (0.2 * 3.0), ia[0]);
        Assert.assertEquals((int) (-0.4 * 3.0), ia[1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateArrayExc() {
        double[] da1 = new double[0];
        DoubleArrays.validateArray(da1);
    }

    @Test(expected = NullPointerException.class)
    public void testValidateArraysNullExc() {
        double[] da1 = null;
        double[] da2 = null;
        DoubleArrays.validateArrays(da1, da2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateArraysArgExc() {
        double[] da1 = new double[2];
        double[] da2 = new double[3];
        DoubleArrays.validateArrays(da1, da2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalize16bitExc() {
        byte[] ba = {-10, 45, 120};
        DoubleArrays.normalize16bitLittleEndian(ba);
    }

    public static boolean inDelta(double result, double actual) {
        return Math.abs(result - actual) < delta;
    }

    public static boolean inDelta(double[] result, double[] actual) {
        for (int i = 0; i < result.length; i++) {
            if (Math.abs(result[i] - actual[i]) > delta) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testSerialization1D() throws IOException {
        serialization1D(new double[]{0.2, -0.4, 0.6});
        serialization1D(new double[]{0.256});
        serialization1D(new double[]{});
    }

    @Test
    public void testSerialization2D() throws IOException {
        serialization2D(new double[][]{{0.2}, {-0.4}, {0.6}});
        serialization2D(new double[][]{{0.2, 1}, {-0.4, 2}, {0.6, 3}});
        serialization2D(new double[][]{{0.2, 1}, {-0.4}, {0.6, 3, 6}});
        serialization2D(new double[][]{{}, {}, {0.6, 3, 6}});
        serialization2D(new double[][]{{}, {}, {}});
    }

    private void serialization1D(double[] da) throws IOException {
        File f = File.createTempFile("blah", "foo");
        f.deleteOnExit();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
            DoubleArrays.serialize(dos, da);
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
            double[] read = DoubleArrays.deserialize(dis);
            Assert.assertTrue(DoubleArrays.arrayEquals(da, read));
        }
    }

    private void serialization2D(double[][] da) throws IOException {
        File f = File.createTempFile("blah", "foo");
        f.deleteOnExit();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
            DoubleArrays.serialize(dos, da);
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
            double[][] read = DoubleArrays.deserialize2d(dis);
            for (int i = 0; i < read.length; i++) {
                Assert.assertTrue(DoubleArrays.arrayEquals(da[i], read[i]));
            }
        }
    }
}
