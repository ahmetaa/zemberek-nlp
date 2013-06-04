package zemberek.core.io;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static zemberek.core.io.Bytes.toHex;
import static zemberek.core.io.Bytes.toHexWithZeros;

public class TestBytes {

    byte[] ba = {0x7e, (byte) 0xac, (byte) 0x8a, (byte) 0x93};
    int bigEndianInt = 0x7eac8a93;
    int littleEndianInt = 0x938aac7e;


    @Test
    public void testtoByteArray() {
        Assert.assertArrayEquals(Bytes.toByteArray(0x7e, 0xac, 0x8a, 0x93), ba);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testtoByteArrayNegativeException() {
        Assert.assertArrayEquals(Bytes.toByteArray(-1, 0xac, 0x8a, 0x93), ba);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testtoByteArrayLArgeNumberException() {
        Assert.assertArrayEquals(Bytes.toByteArray(256, 0xac, 0x8a, 0x93), ba);
    }


    @Test
    public void testToInt() {
        assertEquals(Bytes.toInt(ba, true), bigEndianInt);
        assertEquals(Bytes.toInt(ba, false), littleEndianInt);
        assertEquals(Bytes.toInt((byte) 0x7e, (byte) 0xac, (byte) 0x8a, (byte) 0x93, true), bigEndianInt);
        assertEquals(Bytes.toInt((byte) 0x7e, (byte) 0xac, (byte) 0x8a, (byte) 0x93, false), littleEndianInt);
        assertEquals(Bytes.toInt(new byte[]{0x7e, (byte) 0xac, (byte) 0x8a}, true), 0x7eac8a);
        assertEquals(Bytes.toInt(new byte[]{0x7e, (byte) 0xac, (byte) 0x8a}, false), 0x8aac7e);
        assertEquals(Bytes.toInt(new byte[]{0x7e, (byte) 0xac}, true), 0x7eac);
        assertEquals(Bytes.toInt(new byte[]{0x7e, (byte) 0xac}, false), 0xac7e);
        assertEquals(Bytes.toInt(new byte[]{0x7e}, true), 0x7e);
        assertEquals(Bytes.toInt(new byte[]{0x7e}, false), 0x7e);
        assertEquals(Bytes.toInt(new byte[]{0x2f, (byte) 0xff}, false), 0xff2f);
    }

    @Test
    public void testNormalize() {
        assertEquals(Bytes.normalize(0xff, 8), -1);
        assertEquals(Bytes.normalize(0x8000, 16), Short.MIN_VALUE);
    }

    @Test
    public void testToByte() {
        byte[] baReverse = {(byte) 0x93, (byte) 0x8a, (byte) 0xac, 0x7e};
        Assert.assertArrayEquals(Bytes.toByteArray(bigEndianInt, true), ba);
        Assert.assertArrayEquals(Bytes.toByteArray(bigEndianInt, false), baReverse);
        Assert.assertArrayEquals(Bytes.toByteArray(littleEndianInt, false), ba);
        Assert.assertArrayEquals(Bytes.toByteArray(littleEndianInt, true), baReverse);
    }

    @Test
    public void testToByteShort() {
        byte[] baShort = {0x43, (byte) 0xac};
        byte[] baShortReverse = {(byte) 0xac, 0x43};
        short bigEndianShort = 0x43ac;
        short littleEndianShort = (short) 0xac43;
        Assert.assertArrayEquals(Bytes.toByteArray(bigEndianShort, true), baShort);
        Assert.assertArrayEquals(Bytes.toByteArray(bigEndianShort, false), baShortReverse);
        Assert.assertArrayEquals(Bytes.toByteArray(littleEndianShort, false), baShort);
        Assert.assertArrayEquals(Bytes.toByteArray(littleEndianShort, true), baShortReverse);
    }

    @Test
    public void testToIntArray() {
        int[] intArrBig = {0x7eac8a93, 0x66AABBCC};
        int[] intArrLittle = {0x938aac7e, 0xCCBBAA66,};
        byte[] barr = {0x7e, (byte) 0xac, (byte) 0x8a, (byte) 0x93, 0x66, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC};
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, true), intArrBig);
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, false), intArrLittle);
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, 4, true), intArrBig);
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, 4, false), intArrLittle);

        barr = new byte[]{0x7e, (byte) 0xac, (byte) 0x8a, (byte) 0x93, 0x66, (byte) 0xAA};
        intArrBig = new int[]{0x7eac8a, 0x9366aa};
        intArrLittle = new int[]{0x8aac7e, 0xaa6693};
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, 3, true), intArrBig);
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, 3, false), intArrLittle);

        barr = new byte[]{0x7e, (byte) 0xac, (byte) 0x8a, (byte) 0x93, 0x66, (byte) 0xAA};
        intArrBig = new int[]{0x7eac, 0x8a93, 0x66aa};
        intArrLittle = new int[]{0xac7e, 0x938a, 0xaa66};
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, 2, true), intArrBig);
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, 2, false), intArrLittle);

        barr = new byte[]{0x7e, (byte) 0xac, (byte) 0x8a};
        intArrBig = new int[]{0x7e, 0xac, 0x8a};
        intArrLittle = new int[]{0x7e, 0xac, 0x8a};
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, 1, true), intArrBig);
        Assert.assertArrayEquals(Bytes.toIntArray(barr, barr.length, 1, false), intArrLittle);
    }

    @Test
    public void testToByteArrayShort() {
        byte[] baBe = {0x7e, (byte) 0xac, (byte) 0x8a, (byte) 0x93};
        byte[] baLe = {(byte) 0xac, 0x7e, (byte) 0x93, (byte) 0x8a};
        short[] sarr = {0x7eac, (short) 0x8a93};

        Assert.assertArrayEquals(Bytes.toByteArray(sarr, sarr.length, true), baBe);
        Assert.assertArrayEquals(Bytes.toByteArray(sarr, sarr.length, false), baLe);
    }

    @Test
    public void testByteArray() {
        Assert.assertArrayEquals(Bytes.toByteArray(0xCA, 0xFE, 0xBA, 0xBE, 0x45),
                new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0x45});
    }

    @Test
    public void testToByteArrayInt() {
        int[] sarr4 = {0xCAFEBABE, 0xDEADBEEF};
        int[] sarr3 = {0xCAFEBA, 0xDEADBE};
        int[] sarr2 = {0xCAFE, 0xDEAD};
        int[] sarr1 = {0xCA, 0xFE, 0xBA, 0xBE};

        Assert.assertArrayEquals(Bytes.toByteArray(sarr4, sarr4.length, 4, true),
                Bytes.toByteArray(0xCA, 0xFE, 0xBA, 0xBE, 0xDE, 0xAD, 0xBE, 0xEF));
        Assert.assertArrayEquals(Bytes.toByteArray(sarr4, sarr4.length, 4, false),
                Bytes.toByteArray(0xBE, 0xBA, 0xFE, 0xCA, 0xEF, 0xBE, 0xAD, 0xDE));
        Assert.assertArrayEquals(Bytes.toByteArray(sarr3, sarr3.length, 3, true),
                Bytes.toByteArray(0xCA, 0xFE, 0xBA, 0xDE, 0xAD, 0xBE));
        Assert.assertArrayEquals(Bytes.toByteArray(sarr3, sarr3.length, 3, false),
                Bytes.toByteArray(0xBA, 0xFE, 0xCA, 0xBE, 0xAD, 0xDE));
        Assert.assertArrayEquals(Bytes.toByteArray(sarr2, sarr2.length, 2, true),
                Bytes.toByteArray(0xCA, 0xFE, 0xDE, 0xAD));
        Assert.assertArrayEquals(Bytes.toByteArray(sarr2, sarr2.length, 2, false),
                Bytes.toByteArray(0xFE, 0xCA, 0xAD, 0xDE));
        Assert.assertArrayEquals(Bytes.toByteArray(sarr1, sarr1.length, 1, true),
                Bytes.toByteArray(0xCA, 0xFE, 0xBA, 0xBE));
        Assert.assertArrayEquals(Bytes.toByteArray(sarr1, sarr1.length, 1, false),
                Bytes.toByteArray(0xCA, 0xFE, 0xBA, 0xBE));
    }

    @Test
    public void testToShort() {
        byte[] barr = {0x7e, (byte) 0xac, (byte) 0x8a, (byte) 0x93};
        short[] sarrBe = {0x7eac, (short) 0x8a93};
        short[] sarrLe = {(short) 0xac7e, (short) 0x938a};
        Assert.assertArrayEquals(Bytes.toShortArray(barr, barr.length, true), sarrBe);
        Assert.assertArrayEquals(Bytes.toShortArray(barr, barr.length, false), sarrLe);
    }

    @Test
    public void toHextTest() {
        assertEquals(toHex((byte) 0), "0");
        assertEquals(toHex((byte) 1), "1");
        assertEquals(toHex((byte) 15), "f");
        assertEquals(toHex((byte) 127), "7f");
        assertEquals(toHex((byte) 0xcc), "cc");
        // arrays
        assertEquals(toHex(new byte[]{(byte) 0x01}), "1");
        assertEquals(toHex(new byte[]{(byte) 0xcc}), "cc");
        assertEquals(toHex(new byte[]{0x00, 0x00}), "0");
        assertEquals(toHex(new byte[]{0x01, 0x1f, (byte) 0xcc}), "11fcc");
        assertEquals(toHex(new byte[]{0x01, 0x1f, 0x00}), "11f00");
        assertEquals(toHex(new byte[]{0x00, 0x01, 0x1f, 0x01, 0x00, 0x00}), "11f010000");
    }

    @Test(expected = NullPointerException.class)
    public void toHexExceptionTest() {
        toHex(null);
    }

    //    ~~~~~~~~~~~ toHexWithZerosWithZeros ~~~~~~~~~~~~~~

    @Test
    public void toHexWithZerostWithZerosTest() {
        assertEquals(toHexWithZeros((byte) 0), "00");
        assertEquals(toHexWithZeros((byte) 1), "01");
        assertEquals(toHexWithZeros((byte) 15), "0f");
        assertEquals(toHexWithZeros((byte) 127), "7f");
        assertEquals(toHexWithZeros((byte) 0xcc), "cc");
        // arrays
        assertEquals(toHexWithZeros(new byte[]{(byte) 0x01}), "01");
        assertEquals(toHexWithZeros(new byte[]{(byte) 0xcc}), "cc");
        assertEquals(toHexWithZeros(new byte[]{0x00, 0x00}), "0000");
        assertEquals(toHexWithZeros(new byte[]{0x01, 0x1f, (byte) 0xcc}), "011fcc");
        assertEquals(toHexWithZeros(new byte[]{0x01, 0x1f, 0x00}), "011f00");
        assertEquals(toHexWithZeros(new byte[]{0x00, 0x01, 0x1f, 0x01, 0x00, 0x00}), "00011f010000");
    }

    @Test(expected = NullPointerException.class)
    public void toHexWithZerosExceptionTest() {
        toHexWithZeros(null);
    }

    @Test
    @Ignore(value = "Not a test")
    public void dump() throws IOException {
        Bytes.hexDump(new byte[]{0x01}, 20);
    }

}
