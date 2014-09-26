package zemberek.core.math;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;

public class FloatArraysTest {

    static float delta = 0.0001f;

    @Test
    public void testSum() {
        float[] da = {1, 2, 0, -1, -3};
        float[] da2 = {0.5f, -2, 30, 1, -30};
        Assert.assertTrue(inDelta(FloatArrays.sum(da), -1));
        Assert.assertTrue(inDelta(FloatArrays.sum(da, da2), new float[]{1.5f, 0, 30, 0, -33}));
        FloatArrays.addToFirst(da, da2);
        Assert.assertTrue(inDelta(da, new float[]{1.5f, 0, 30, 0, -33}));
    }

    @Test
    public void addToFirstScaledTest() {
        float[] da1 = {1, 2, 0, -1, -30};
        float[] da2 = {-0.5f, -1, 0, 0.5f, 15};
        FloatArrays.addToFirstScaled(da1, da2, 2);
        Assert.assertEquals(FloatArrays.max(da1), 0f, 0.0001);
        Assert.assertEquals(FloatArrays.min(da1), 0f, 0.0001);
    }

    @Test
    public void testDotProduct() {
        float[] da = {1, 2, 0, -1, -3};
        float[] da2 = {0.5f, -2, 30, 1, -30};
        Assert.assertTrue(inDelta(FloatArrays.dotProduct(da, da2), 85.5f));
    }

    @Test
    public void testSubstract() {
        float[] da = {1, 2, 0, -1, -3};
        float[] da2 = {0.5f, -2, 30, 1, -30};
        Assert.assertTrue(inDelta(FloatArrays.subtract(da, da2), new float[]{0.5f, 4, -30, -2, 27}));
        FloatArrays.subtractFromFirst(da, da2);
        Assert.assertTrue(inDelta(da, new float[]{0.5f, 4, -30, -2, 27}));
    }

    @Test
    public void testAppendZeros() {
        float[] da = {1, 2, 0, -1, -3};
        float[] da2 = {1, 2, 0, -1, -3, 0, 0, 0, 0, 0, 0};
        Assert.assertTrue(inDelta(FloatArrays.appendZeros(da, 6), da2));
        Assert.assertTrue(inDelta(FloatArrays.appendZeros(da2, 0), da2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAppendZerosExc() {
        float[] da = {1, 2, 0, -1, -3};
        FloatArrays.appendZeros(da, -10);
    }

    @Test
    public void testMaxMinValue() {
        float[] da = {1, 2, 0, -1, -3};
        Assert.assertTrue(inDelta(FloatArrays.max(da), 2));
        Assert.assertTrue(inDelta(FloatArrays.min(da), -3));
    }

    @Test
    public void testMaxMinIndex() {
        float[] da = {1, 2, 0, -1, -3};
        Assert.assertEquals(FloatArrays.maxIndex(da), 1);
        Assert.assertEquals(FloatArrays.minIndex(da), 4);
        float[] da2 = {2, 2, 0, -1, -3, 2, -3};
        Assert.assertEquals(FloatArrays.maxIndex(da2), 0);
        Assert.assertEquals(FloatArrays.minIndex(da2), 4);
    }

    @Test
    public void testSquare() {
        float[] da = {1, 2, 0, -1, -3};
        Assert.assertTrue(inDelta(FloatArrays.square(da), new float[]{1, 4, 0, 1, 9}));
    }

    @Test
    public void testSquaredSum() {
        float[] da = {1, 2, 0, -1, -3};
        Assert.assertTrue(inDelta(FloatArrays.squaredSum(da), 15));
        float[] da2 = {0, 1, 1, -2, 1};
        Assert.assertTrue(inDelta(FloatArrays.squaredSumOfDifferences(da, da2), 20));
        Assert.assertTrue(inDelta(FloatArrays.absoluteDifference(da, da2), new float[]{1, 1, 1, 1, 4}));
    }

    @Test
    public void testMean() {
        float[] da = {1, 2, 0, -1, -3};
        Assert.assertTrue(inDelta(FloatArrays.mean(da), -1f / 5f));
    }

    @Test
    public void testInRange() {
        float d = 3;
        float d2 = 2.5f;
        float d3 = 7;
        Assert.assertTrue(FloatArrays.inRange(d, d2, 1));
        Assert.assertFalse(FloatArrays.inRange(d2, d3, 4));
        Assert.assertTrue(FloatArrays.inRange(d, d2, 0.5f));
    }

    @Test
    public void testReverse() {
        float[] da = {1, 3, 7, 2.5f, 0};
        float[] da2 = FloatArrays.reverse(FloatArrays.reverse(da));
        Assert.assertNotNull(da2);
        Assert.assertEquals(da.length, da2.length);
        Assert.assertTrue(inDelta(da, da2));
    }

    @Test
    public void testConvertInt() {
        int[] ia = {1, 3, -1, 0, 9, 12};
        float[] da2 = {1, 3, -1, 0, 9.0f, 12.0f};
        float[] da = FloatArrays.convert(ia);
        Assert.assertNotNull(da);
        Assert.assertTrue(inDelta(da, da2));
    }

    @Test
    public void testConvert2f() {
        int[][] ia = {{1, 2}, {4, 3}, {-1, 5}};
        float[][] da = {{1, 2}, {4, 3}, {-1, 5}};
        float[][] da2 = FloatArrays.convert(ia);
        Assert.assertEquals(ia.length, da2.length);
        Assert.assertNotNull(da2);
        int k = 0;
        for (float[] i : da) {
            int j = 0;
            for (float ii : i) {
                Assert.assertEquals(da2[k][j++], ii, 0.0001);
            }
            k++;
        }
    }

    @Test
    public void testArrayEqualsInRange() {
        float[] da = {0, 3.5f, 7, -2, 19};
        float[] da2 = {0, 4.5f, 4, 2, -18};
        Assert.assertTrue(FloatArrays.arrayEqualsInRange(da, da2, 37));
        Assert.assertFalse(FloatArrays.arrayEqualsInRange(da, da2, 15));
        Assert.assertTrue(FloatArrays.arrayEqualsInRange(da, da2, 45));
    }

    @Test
    public void testArrayEquals() {
        float[] da = {-1, 0, 3.4f, 7};
        float[] da2 = {-1, 0.0f, 3.4f, 7};
        float[] da3 = {7, 9.0f, 12.3f, -5.6f};
        Assert.assertTrue(FloatArrays.arrayEquals(da, da2));
        Assert.assertFalse(FloatArrays.arrayEquals(da2, da3));
    }

    @Test
    public void testMultiply() {
        float[] da = {-1, 1.3f, 8.2f, 10, 90};
        float[] da2 = {-1, 3, 2, 2, -1};
        Assert.assertTrue(inDelta(FloatArrays.multiply(da, da2), new float[]{1, 3.9f, 16.4f, 20, -90}));
        FloatArrays.multiplyToFirst(da, da2);
        Assert.assertTrue(inDelta(da, new float[]{1, 3.9f, 16.4f, 20, -90}));
    }

    @Test
    public void testScale() {
        float[] da = {1, -1, 3.1f, 0.0f, 0};
        Assert.assertTrue(inDelta(FloatArrays.scale(da, 0), new float[]{0, 0, 0, 0, 0}));
        Assert.assertTrue(inDelta(FloatArrays.scale(da, -1), new float[]{-1, 1, -3.1f, 0, 0.0f}));
        Assert.assertTrue(inDelta(FloatArrays.scale(da, 4), new float[]{4, -4, 12.4f, 0.0f, 0.0f}));

        FloatArrays.scaleInPlace(da, 4);
        Assert.assertTrue(inDelta(da, new float[]{4, -4, 12.4f, 0.0f, 0.0f}));
        FloatArrays.scaleInPlace(da, 0);
        Assert.assertTrue(inDelta(da, new float[]{0, 0, 0, 0, 0}));
    }

    @Test
    public void testAbsoluteDifference() {
        float[] da = {1, 2, 4.5f, -2, 4};
        float[] da2 = {1, 1, 2.5f, 5, -15.2f};
        Assert.assertTrue(inDelta(FloatArrays.absoluteDifference(da, da2), new float[]{0, 1, 2, 7, 19.2f}));
        Assert.assertEquals(FloatArrays.absoluteSumOfDifferences(da, da2), 29.2f, 0.0001);
    }

    @Test
    public void testVariance() {
        float[] da = {0, 2, 4};
        Assert.assertEquals(FloatArrays.variance(da), 4f, 0.0001);
        Assert.assertEquals(FloatArrays.standardDeviation(da), 2f, 0.0001);
    }

    @Test
    public void testContainsNaN() {
        float[] da = {-1, 1, 3, 5, 7, 9, Float.NaN};
        Assert.assertTrue(FloatArrays.containsNaN(da));

        float[] da2 = {-1, 1, 3, 5, 7, 9};
        Assert.assertFalse(FloatArrays.containsNaN(da2));
    }

    @Test
    public void testFloorInPlace() {
        float[] da = {0, -7, 2, 1.1123f, -10, -22, 56};
        FloatArrays.floorInPlace(da, -7);
        Assert.assertTrue(inDelta(da, new float[]{0, -7, 2, 1.1123f, -7, -7, 56}));

    }

    @Test
    public void testNonZeroFloorInPlace() {
        float[] da = {0, -7, 2, 1.1123f, -10, -22, 56};
        FloatArrays.nonZeroFloorInPlace(da, 3);
        Assert.assertTrue(inDelta(da, new float[]{0, 3, 3, 3, 3, 3, 56}));

    }

    @Test
    public void testNormalizeInPlace() {
        float[] da = {1, 5.6f, 2.4f, -1, -3.0f, 5};
        FloatArrays.normalizeInPlace(da);
        Assert.assertTrue(inDelta(da, new float[]{0.1f, 0.56f, 0.24f, -0.1f, -0.3f, 0.5f}));
    }

    @Test
    public void testAddToAll() {
        float[] da = {1, 2, 0, -1, -3};
        float[] expected = {2, 3, 1, 0, -2};
        FloatArrays.addToAll(da, 1);
        Assert.assertTrue(Arrays.equals(expected, da));
    }

    @Test
    public void testValidateArray() {
        float[] da = null;
        try {
            FloatArrays.validateArray(da);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        da = new float[0];
        try {
            FloatArrays.validateArray(da);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testValidateArrays() {
        float[] da1 = {1, 2, 3};
        float[] da2 = null;
        try {
            FloatArrays.validateArrays(da1, da2);
        } catch (NullPointerException e) {
            Assert.assertTrue(true);
        }

        float[] da3 = {5, 6, 2.33f, 2};
        try {
            FloatArrays.validateArrays(da1, da3);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNormalize16bitLittleEndian() {
        byte[] ba = {0x10, 0x71, 0x18, 0x54};
        float[] da = FloatArrays.normalize16bitLittleEndian(ba);
        Assert.assertEquals(da[0] * Short.MAX_VALUE, 28944f, 0.0001);
        Assert.assertEquals(da[1] * Short.MAX_VALUE, 21528f, 0.0001);

        byte[] ba2 = FloatArrays.denormalize16BitLittleEndian(da);
        Assert.assertEquals(ba2[0], 0x10);
        Assert.assertEquals(ba2[3], 0x54);

        byte[] ba3 = FloatArrays.denormalizeLittleEndian(da, 16);
        Assert.assertEquals(ba3[0], 0x10);
        Assert.assertEquals(ba3[3], 0x54);

        byte[] ba4 = {(byte) 0xCC, (byte) 0xAB};
        da = FloatArrays.normalize16bitLittleEndian(ba4);
        Assert.assertEquals(da[0] * Short.MIN_VALUE, 21556f, 0.0001);
    }

    @Test
    public void testToUnsignedInteger() {
        float[] da = {0.2f, -0.4f, 0.6f};
        int[] ia = FloatArrays.toUnsignedInteger(da, 6);
        Assert.assertEquals((int) (0.2f * 3.0f), ia[0]);
        Assert.assertEquals((int) (-0.4f * 3.0f), ia[1]);
    }

    @Test
    public void testFormat() {
        float[] da = {0.2f, -0.45f, 0.6f};
        Assert.assertEquals("0.2 -0.4 0.6", FloatArrays.format(1, " ", da));
        Assert.assertEquals("0.20 -0.45 0.60", FloatArrays.format(2, " ", da));
        Assert.assertEquals("0.20, -0.45, 0.60", FloatArrays.format(2, ", ", da));
        Assert.assertEquals("0.20-0.450.60", FloatArrays.format(2, "", da));
        Assert.assertEquals("0.2   -0.4  0.6", FloatArrays.format(5, 1, " ", da));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateArrayExc() {
        float[] da1 = new float[0];
        FloatArrays.validateArray(da1);
    }

    @Test(expected = NullPointerException.class)
    public void testValidateArraysNullExc() {
        float[] da1 = null;
        float[] da2 = null;
        FloatArrays.validateArrays(da1, da2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateArraysArgExc() {
        float[] da1 = new float[2];
        float[] da2 = new float[3];
        FloatArrays.validateArrays(da1, da2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalize16bitExc() {
        byte[] ba = {-10, 45, 120};
        FloatArrays.normalize16bitLittleEndian(ba);
    }

    public static boolean inDelta(float result, float actual) {
        return Math.abs(result - actual) < delta;
    }

    public static boolean inDelta(float[] result, float[] actual) {
        for (int i = 0; i < result.length; i++) {
            if (Math.abs(result[i] - actual[i]) > delta) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testSerialization1D() throws IOException {
        serialization1D(new float[]{0.2f, -0.4f, 0.6f});
        serialization1D(new float[]{0.256f});
        serialization1D(new float[]{});
    }

    @Test
    public void testSerialization2D() throws IOException {
        serialization2D(new float[][]{{0.2f}, {-0.4f}, {0.6f}});
        serialization2D(new float[][]{{0.2f, 1f}, {-0.4f, 2}, {0.6f, 3}});
        serialization2D(new float[][]{{0.2f, 1f}, {-0.4f}, {0.6f, 3, 6}});
        serialization2D(new float[][]{{}, {}, {0.6f, 3, 6}});
        serialization2D(new float[][]{{}, {}, {}});
    }

    private void serialization1D(float[] da) throws IOException {
        File f = File.createTempFile("blah", "foo");
        f.deleteOnExit();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
            FloatArrays.serialize(dos, da);
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
            float[] read = FloatArrays.deserialize(dis);
            Assert.assertTrue(FloatArrays.arrayEquals(da, read));
        }
    }

    private void serialization2D(float[][] da) throws IOException {
        File f = File.createTempFile("blah", "foo");
        f.deleteOnExit();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
            FloatArrays.serialize(dos, da);
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
            float[][] read = FloatArrays.deserialize2d(dis);
            for (int i = 0; i < read.length; i++) {
                Assert.assertTrue(FloatArrays.arrayEquals(da[i], read[i]));
            }
        }
    }

}
