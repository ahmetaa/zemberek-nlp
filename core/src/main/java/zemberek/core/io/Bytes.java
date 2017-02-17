package zemberek.core.io;

import com.google.common.base.Preconditions;

import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Byte related low level functions.
 */
public class Bytes {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * converts an unsigned integer array to a byte array. this is useful when defining byte arrays in the code without using
     * (byte) casts. Note that integers sent to this method should be unsigned. integer are sugggested to be written in
     * hex format.
     *
     * @param uints an integer array formed from unsigned ints.
     * @return a byte array.
     * @throws IllegalArgumentException if an array item is smaller than zero or larger than 255 (0xff)
     */
    public static byte[] toByteArray(int... uints) {
        byte[] bytez = new byte[uints.length];
        for (int i = 0; i < uints.length; i++) {
            if (uints[i] > 255 || uints[i] < 0)
                throw new IllegalArgumentException("Cannot convert to byte. Number should be between 0 and (255) 0xff. " +
                        "Number:" + uints[i]);
            bytez[i] = (byte) (uints[i] & 0xff);
        }
        return bytez;
    }

    /**
     * converts a byte array to an integer. byte array may be with the lenght of 1,2,3 or 4.
     *
     * @param pb        byte array
     * @param bigEndian endianness.
     * @return an integer represented byt the byte array
     * @throws IllegalArgumentException if byte array size is larger than 4
     */
    public static int toInt(byte[] pb, boolean bigEndian) {

        switch (pb.length) {
            case 1:
                return pb[0] & 0xff;
            case 2:
                if (bigEndian)
                    return (pb[0] << 8 & 0xff00) | (pb[1] & 0xff);
                else
                    return (pb[1] << 8 & 0xff00) | (pb[0] & 0xff);
            case 3:
                if (bigEndian)
                    return (pb[0] << 16 & 0xff0000) | (pb[1] << 8 & 0xff00) | (pb[2] & 0xff);
                else
                    return (pb[2] << 16 & 0xff0000) | (pb[1] << 8 & 0xff00) | (pb[0] & 0xff);
            case 4:
                if (bigEndian) {
                    return (pb[0] << 24 & 0xff000000) |
                            (pb[1] << 16 & 0xff0000) |
                            (pb[2] << 8 & 0xff00) |
                            (pb[3] & 0xff);
                } else {
                    return (pb[3] << 24 & 0xff000000) |
                            (pb[2] << 16 & 0xff0000) |
                            (pb[1] << 8 & 0xff00) |
                            (pb[0] & 0xff);
                }
            default:
                throw new IllegalArgumentException("1,2,3 or 4 byte arrays allowed. size:" + pb.length);
        }
    }

    public static int normalize(int i, int bitCount) {
        int max = 0xffffffff >>> (32 - bitCount);
        if (i > max)
            throw new IllegalArgumentException("The integer cannot fit to bit boundaries.");
        if (i > (max >>> 1))
            return i - (max + 1);
        else
            return i;
    }

    public static void normalize(int iarr[], int bitCount) {
        for (int i = 0; i < iarr.length; i++) {
            iarr[i] = normalize(iarr[i], bitCount);
        }
    }


    public static byte[] toByteArray(int i, int size, boolean isBigEndian) {
        switch (size) {
            case 1:
                return new byte[]{(byte) i};
            case 2:
                if (isBigEndian)
                    return new byte[]{(byte) (i >>> 8 & 0xff), (byte) (i & 0xff)};
                else
                    return new byte[]{(byte) (i & 0xff), (byte) (i >>> 8 & 0xff)};
            case 3:
                if (isBigEndian)
                    return new byte[]{(byte) (i >>> 16 & 0xff), (byte) (i >>> 8 & 0xff), (byte) (i & 0xff)};
                else
                    return new byte[]{(byte) (i & 0xff), (byte) (i >>> 8 & 0xff), (byte) (i >>> 16 & 0xff)};
            case 4:
                return toByteArray(i, isBigEndian);
            default:
                throw new IllegalArgumentException("1,2,3 or 4 size values are allowed. size:" + size);
        }
    }

    /**
     * converts 4 bytes to an integer
     *
     * @param b0        first byte
     * @param b1        second byte
     * @param b2        third byte
     * @param b3        forth byte
     * @param bigEndian , if we want it in big endian format
     * @return integer formed from bytes.
     */
    public static int toInt(byte b0, byte b1, byte b2, byte b3, boolean bigEndian) {
        if (bigEndian) {
            return (b0 << 24 & 0xff000000) |
                    (b1 << 16 & 0xff0000) |
                    (b2 << 8 & 0xff00) |
                    (b3 & 0xff);
        } else {
            return (b3 << 24 & 0xff000000) |
                    (b2 << 16 & 0xff0000) |
                    (b1 << 8 & 0xff00) |
                    (b0 & 0xff);
        }
    }

    /**
     * converts an integer to 4 byte array.
     *
     * @param i         the number.
     * @param bigEndian endianness.
     * @return byte array generated from the integer.
     */
    public static byte[] toByteArray(int i, boolean bigEndian) {
        byte[] ba = new byte[4];
        if (bigEndian) {
            ba[0] = (byte) (i >>> 24);
            ba[1] = (byte) (i >>> 16 & 0xff);
            ba[2] = (byte) (i >>> 8 & 0xff);
            ba[3] = (byte) (i & 0xff);
        } else {
            ba[0] = (byte) (i & 0xff);
            ba[1] = (byte) (i >>> 8 & 0xff);
            ba[2] = (byte) (i >>> 16 & 0xff);
            ba[3] = (byte) (i >>> 24);
        }
        return ba;
    }

    /**
     * converts a short to 2 byte array.
     *
     * @param i         the number.
     * @param bigEndian endianness.
     * @return byte array generated from the short.
     */
    public static byte[] toByteArray(short i, boolean bigEndian) {
        byte[] ba = new byte[2];
        if (bigEndian) {
            ba[0] = (byte) (i >>> 8);
            ba[1] = (byte) (i & 0xff);
        } else {
            ba[0] = (byte) (i & 0xff);
            ba[1] = (byte) (i >>> 8 & 0xff);
        }
        return ba;
    }

    /**
     * Converts a byte array to an integer array. byte array length must be an order of 4.
     *
     * @param ba        byte array
     * @param amount    amount of bytes to convert to int.
     * @param bigEndian true if big endian.
     * @return an integer array formed form byte array.
     * @throws IllegalArgumentException if amount is smaller than 4, larger than byte array, or not an order of 4.
     */
    public static int[] toIntArray(byte[] ba, int amount, boolean bigEndian) {
        final int size = determineSize(amount, ba.length, 4);
        int[] result = new int[size / 4];
        int i = 0;
        for (int j = 0; j < size; j += 4) {
            if (bigEndian) {
                result[i++] = toInt(ba[j], ba[j + 1], ba[j + 2], ba[j + 3], true);
            } else {
                result[i++] = toInt(ba[j + 3], ba[j + 2], ba[j + 1], ba[j], true);
            }
        }
        return result;
    }


    /**
     * Converts a byte array to an integer array. byte array length must be an order of 4.
     *
     * @param ba             byte array
     * @param amount         amount of bytes to convert to int.
     * @param bytePerInteger byte count per integer.
     * @param bigEndian      true if big endian.
     * @return an integer array formed form byte array.
     * @throws IllegalArgumentException if amount is smaller than 4, larger than byte array, or not an order of 4.
     */
    public static int[] toIntArray(byte[] ba, final int amount, final int bytePerInteger, boolean bigEndian) {
        final int size = determineSize(amount, ba.length, bytePerInteger);
        int[] result = new int[size / bytePerInteger];
        int i = 0;
        byte[] bytez = new byte[bytePerInteger];
        for (int j = 0; j < size; j += bytePerInteger) {
            System.arraycopy(ba, j, bytez, 0, bytePerInteger);
            if (bigEndian) {
                result[i++] = toInt(bytez, true);
            } else {
                result[i++] = toInt(bytez, false);
            }
        }
        return result;
    }

    /**
     * Converts a byte array to an integer array. byte array length must be an order of 4.
     *
     * @param ba             byte array
     * @param amount         amount of bytes to convert to int.
     * @param bytePerInteger byte count per integer.
     * @param bitAmount      bit count where the value will be mapped.
     * @param bigEndian      true if big endian.
     * @return an integer array formed form byte array.
     * @throws IllegalArgumentException if amount is smaller than 4, larger than byte array, or not an order of 4.
     */
    public static int[] toReducedBitIntArray(
            byte[] ba,
            final int amount,
            int bytePerInteger,
            int bitAmount,
            boolean bigEndian) {
        final int size = determineSize(amount, ba.length, bytePerInteger);
        int[] result = new int[size / bytePerInteger];
        int i = 0;
        byte[] bytez = new byte[bytePerInteger];
        for (int j = 0; j < size; j += bytePerInteger) {
            System.arraycopy(ba, j, bytez, 0, bytePerInteger);
            if (bigEndian) {
                result[i++] = normalize(toInt(bytez, true), bitAmount);
            } else {
                result[i++] = normalize(toInt(bytez, false), bitAmount);
            }
        }
        return result;
    }


    private static int determineSize(int amount, int arrayLength, int order) {
        if (amount < order || amount > arrayLength)
            throw new IllegalArgumentException(
                    "amount of bytes to read cannot be smaller than " + order +
                            " or larger than array length. Amount is:" + amount);

        final int size = amount < arrayLength ? amount : arrayLength;
        if (size % order != 0)
            throw new IllegalArgumentException("array size must be an order of " + order + ". The size is:" + arrayLength);

        return size;
    }

    /**
     * Converts a byte array to a short array. byte array length must be an order of 2.
     *
     * @param ba        byte array
     * @param amount    amount of bytes to convert to short.
     * @param bigEndian true if big endian.
     * @return a short array formed from byte array.
     * @throws IllegalArgumentException if amount is smaller than 2, larger than byte array, or not an order of 2.
     */
    public static short[] toShortArray(byte[] ba, int amount, boolean bigEndian) {
        final int size = determineSize(amount, ba.length, 2);
        short[] result = new short[size / 2];
        int i = 0;
        for (int j = 0; j < size; j += 2) {
            if (bigEndian) {
                result[i++] = (short) (ba[j] << 8 & 0xff00 | ba[j + 1] & 0xff);
            } else {
                result[i++] = (short) (ba[j + 1] << 8 & 0xff00 | ba[j] & 0xff);
            }
        }
        return result;
    }

    /**
     * Converts a given array of shorts to a byte array.
     *
     * @param sa        short array
     * @param amount    amount of data to convert from input array
     * @param bigEndian if it is big endian
     * @return an array of bytes converted from the input array of shorts.
     *         0xBABE becomes 0xBA, 0xBE (Big Endian) or 0xBE, 0xBA (Little Endian)
     */
    public static byte[] toByteArray(short[] sa, int amount, boolean bigEndian) {
        final int size = amount < sa.length ? amount : sa.length;
        byte[] result = new byte[size * 2];
        for (int j = 0; j < size; j++) {
            final byte bh = (byte) (sa[j] >>> 8);
            final byte bl = (byte) (sa[j] & 0xff);
            if (bigEndian) {
                result[j * 2] = bh;
                result[j * 2 + 1] = bl;
            } else {
                result[j * 2] = bl;
                result[j * 2 + 1] = bh;
            }
        }
        return result;
    }

    /**
     * Converts a given array of ints to a byte array.
     *
     * @param ia             <code>int</code> array
     * @param amount         Amount of data to be converted from input array
     * @param bytePerInteger Byte count per integer.
     * @param bigEndian      If it is big endian
     * @return an array of bytes converted from the input array of shorts.
     *         when bytePerInteger = 2,  ia = {0x0000CAFE, 0x0000BABE}
     *         returns {0xCA, 0xFE, 0xBA, 0xBE} (Big Endian)
     *         returns {0xFE, 0xCA, 0xBE, 0xBA } (Little Endian)
     *         when bytePerInteger=4, ia = {0xCAFEBABE}
     *         return  {0xCA, 0xFE, 0xBA, 0xBE} (Big Endian)
     *         returns { 0xBE, 0xBA, 0xFE, 0xCA} (Little Endian)
     */
    public static byte[] toByteArray(int[] ia, int amount, int bytePerInteger, boolean bigEndian) {

        if (bytePerInteger < 1 || bytePerInteger > 4)
            throw new IllegalArgumentException("bytePerInteger parameter can only be 1,2,3 or 4. But it is:" + bytePerInteger);
        if (amount > ia.length || amount < 0)
            throw new IllegalArgumentException("Amount cannot be negative or more than input array length. Amount:" + amount);

        final int size = amount < ia.length ? amount : ia.length;
        byte[] result = new byte[size * bytePerInteger];
        for (int j = 0; j < size; j++) {
            final byte[] piece = toByteArray(ia[j], bytePerInteger, bigEndian);
            System.arraycopy(piece, 0, result, j * bytePerInteger, bytePerInteger);
        }
        return result;
    }

    /**
     * converts a byte to a hexadecimal string with special xx formatting. it always return two characters
     * <pre>
     * <p>for 0 , returns "00"
     * <p>for 1..15 returns "00".."0f"
     * </pre>
     *
     * @param b byte
     * @return hex string.
     */
    public static String toHexWithZeros(byte b) {
        if (b == 0)
            return "00";
        String s = toHex(b);
        if (s.length() == 1)
            return "0" + s;
        else
            return s;
    }

    /**
     * converts a byte to a hexadecimal string. it eliminates left zeros.
     * <pre>
     * <p>for 0 , returns "0"
     * <p>for 1 to 15 returns "0".."f"
     * </pre>
     *
     * @param b byte
     * @return hex string.
     */
    public static String toHex(byte b) {
        return String.format("%x", b);
    }

    /**
     * converts byte array to a hexadecimal string. it ignores the zeros on the left side.
     * <pre>
     * <p>{0x00, 0x0c, 0x11, 0x01, 0x00} -> "c110100"
     * </pre>
     *
     * @param bytes byte array, should be non-null, and not empty.
     * @return a String representation of the number represented by the byte array.
     *         empty string is byte array is empty.
     * @throws NullPointerException if byte array is null
     */
    public static String toHex(byte[] bytes) {
        Preconditions.checkNotNull(bytes, "byte array cannot be null.");
        if (bytes.length == 0) return Strings.EMPTY_STRING;
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        boolean nonZeroFound = false;
        for (byte b : bytes) {
            if (!nonZeroFound) {
                if (b != 0) {
                    builder.append(toHex(b));
                    nonZeroFound = true;
                }
                continue;
            }
            builder.append(toHexWithZeros(b));
        }
        //if all bytes are zero, loop above produces nothing. so we return "0"
        if (builder.length() == 0 && bytes.length > 0)
            return "0";
        else
            return builder.toString();
    }

    /**
     * converts a byte array to a hexadecimal string with special xx formatting. it does not ignore the left zeros.
     * <pre>
     * <p>{0x00, 0x0c, 0x11, 0x00} -> "000c1100"
     * <pre>
     *
     * @param bytes byte array
     * @return hex string.  empty string is byte array is empty.
     * @throws NullPointerException if byte array is null
     */
    public static String toHexWithZeros(byte[] bytes) {
        Preconditions.checkNotNull(bytes, "byte array cannot be null.");
        if (bytes.length == 0) return Strings.EMPTY_STRING;
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(toHexWithZeros(b));
        }
        return builder.toString();
    }

    /**
     * dumps the bytes to an Outputstream.
     *
     * @param os      outputstream.
     * @param bytes   bytes to dump
     * @param columns column number
     * @throws java.io.IOException if an error occurs while writing.
     */
    public static void hexDump(OutputStream os, byte[] bytes, int columns) throws IOException {
        Dumper.hexDump(new ByteArrayInputStream(bytes, 0, bytes.length), os, columns, bytes.length);
    }

    /**
     * dumps the bytes to Console.
     *
     * @param bytes   bytes to dump
     * @param columns column number
     * @throws java.io.IOException if an error occurs while writing.
     */
    public static void hexDump(byte[] bytes, int columns) throws IOException {
        Dumper.hexDump(new ByteArrayInputStream(bytes, 0, bytes.length), System.out, columns, bytes.length);
    }

}
