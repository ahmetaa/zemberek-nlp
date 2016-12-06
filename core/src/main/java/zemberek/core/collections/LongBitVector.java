package zemberek.core.collections;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * A bit vector backed by a long array
 */
public class LongBitVector {
    private long[] words;
    private long size;
    private final int capacityInterval;

    // maximum size for the backing long array.
    public static final long MAX_ARRAY_SIZE = (Integer.MAX_VALUE - 1) * 64L;


    // for fast modulo 64 calculation of longs, n mod 64 = n and 0011 1111
    private static final long mod64Mask = 0x3FL;

    private static final long[] longSetMasks = new long[64];
    private static final long[] longResetMasks = new long[64];
    private static final int[] intSetMasks = new int[32];
    private static final long[] cutMasks = new long[64];

    static {
        for (int i = 0; i < 64; i++) {
            longSetMasks[i] = 0x1L << i;
            longResetMasks[i] = ~longSetMasks[i];
            if (i < 32) {
                intSetMasks[i] = 0x1 << i;
            }
            cutMasks[i] = ~(0xfffffffffffffffeL << i);
        }
    }

    /**
     * creates an empty bit vector.
     */
    public LongBitVector() {
        this(128);
    }

    /**
     * Creates an empty bit vector with initial bit capacity of initialCapcity.
     *
     * @param initialCapcity initial
     */
    public LongBitVector(long initialCapcity) {
        this(initialCapcity, 7);
    }

    /**
     * Creates a bit vector with the bit values from words with size of size.
     *
     * @param words long values carrying bits.
     * @param size  vector size.
     */
    public LongBitVector(long[] words, long size) {
        if (size < 0 || size > words.length * 64)
            throw new IllegalArgumentException("Cannot create vector with size:" + size);
        this.size = size;
        this.words = words.clone();
        this.capacityInterval = 7;
    }

    /**
     * creates an empty bit vector with determined initial capacity and capacity interval.
     *
     * @param initialCapcity   initial bit capacity.
     * @param capacityInterval amount of long values to add when capacity is not enough.
     */
    public LongBitVector(long initialCapcity, int capacityInterval) {
        if (capacityInterval < 0)
            throw new IllegalArgumentException("Cannot create vector with capacityInterval:" + capacityInterval);
        this.capacityInterval = capacityInterval;
        ensureSize(initialCapcity);
        words = new long[(int) (initialCapcity >>> 6) + capacityInterval];
        this.size = 0;
    }

    /**
     * Used only for test purposes.
     *
     * @param bits bit string. It can contain space characters
     * @return bit vector equivalent.
     */
    static LongBitVector fromBinaryString(String bits) {
        bits = bits.replaceAll(" ", "");
        LongBitVector vector = new LongBitVector(bits.length());

        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1')
                vector.addFast(true);
            else
                vector.addFast(false);
        }
        return vector;
    }

    /**
     * retrieves the index of the last bit in the vector with the value of bitValue.
     *
     * @param bitValue value of the bit to search.
     * @return the index of the last bit with the specified value. -1 if there is not such bit.
     */
    public long getLastBitIndex(boolean bitValue) {
        for (long i = size - 1; i >= 0; i--) {
            if (get(i) == bitValue)
                return i;
        }
        return -1;
    }

    /**
     * Fills all bits with the value.
     *
     * @param bitValue bit value to fill.
     */
    public void fill(boolean bitValue) {
        if (bitValue) {
            Arrays.fill(words, 0xffffffffffffffffL);
            int last = (int) (size / 64);
            words[last] &= cutMasks[(int) (size & mod64Mask)] >>> 1;
        } else
            Arrays.fill(words, 0);
    }

    /**
     * retrieves the long array carrying the bit values.
     *
     * @return word array.
     */
    public long[] getLongArray() {
        return words.clone();
    }

    // TODO: can be optimized for almost filled with 1 vectors.

    /**
     * creates a long array containing 0 bit indexes.
     *
     * @return zero bit indexes.
     */
    public long[] zeroIndexes() {
        long[] zeroIndexes = new long[(int) numberOfZeros()];

        int j = 0;
        for (long i = 0; i < size; i++) {
            if (!get(i))
                zeroIndexes[j++] = i;
        }
        return zeroIndexes;
    }

    /**
     * creates a long array containing 0 bit indexes.
     *
     * @return zero bit indexes.
     */
    public int[] zeroIntIndexes() {
        int[] zeroIndexes = new int[(int) numberOfZeros()];

        int j = 0;
        for (int i = 0; i < size; i++) {
            if (!get(i))
                zeroIndexes[j++] = i;
        }
        return zeroIndexes;
    }

    private void ensureSize(long _size) {
        if (_size < 0 || _size > MAX_ARRAY_SIZE)
            throw new IllegalArgumentException("Cannot create vector with size:" + size);
    }

    /**
     * vector size.
     *
     * @return size
     */
    public long size() {
        return size;
    }

    /**
     * appends a bit to the vector. it expads the vector capacity if there is no space left.
     *
     * @param b bit value
     */
    public void add(boolean b) {
        if (words.length << 6 == size + 1) {
            ensureCapacity(capacityInterval);
        }
        if (b)
            set(size);
        size++;
    }

    /**
     * appends a bit to the vector. it does not expad the vector capacity if there is no space left.
     * So user must be sure there is space left in the vector before calling this method.
     *
     * @param b bit value.
     */
    public void addFast(boolean b) {
        if (b)
            set(size);
        size++;
    }

    public long getLong(long start, int bitAmount) {
        final int startInd = (int) (start >>> 6);
        final int endInd = (int) ((start + bitAmount) >>> 6);
        final long startMod = start & mod64Mask;
        if (startInd == endInd) {
            return (words[startInd] >>> startMod) & cutMasks[bitAmount - 1];
        } else {
            long first = words[startInd] >>> startMod;
            long second = words[endInd] << (64 - startMod);
            return (first | second) & cutMasks[bitAmount - 1];
        }
    }

    public void add(int a, int bitLength) {
        if (bitLength < 0 || bitLength > 32)
            throw new IllegalArgumentException("Bit length cannot be negative or larger than 32:" + bitLength);
        if (words.length << 6 < size + 1) {
            ensureCapacity(7);
        }
        for (int i = 0; i < bitLength; i++) {
            if ((a & intSetMasks[i]) != 0)
                set(size);
            size++;
        }
    }

    public void add(int amount, boolean bit) {
        if (amount < 0)
            throw new IllegalArgumentException("Amount cannot be negative:" + amount);
        if (words.length << 6 < size + 1 + amount >> 6) {
            ensureCapacity(capacityInterval + amount >> 6);
        }
        for (int i = 0; i < amount; i++) {
            if (bit)
                set(size);
            size++;
        }
    }

    public void add(long a, int bitLength) {
        if (bitLength < 0 || bitLength > 64)
            throw new IllegalArgumentException("Bit length cannot be negative or lareger than 64:" + bitLength);
        if (words.length << 6 < size + 1) {
            ensureCapacity(capacityInterval);
        }
        for (int i = 0; i < bitLength; i++) {
            if ((a & longSetMasks[i]) != 0)
                set(size);
            size++;
        }
    }

    public long numberOfOnes() {
        long count = 0;
        for (long word : words)
            count += Long.bitCount(word);
        return count;
    }

    public long numberOfZeros() {
        return size - numberOfOnes();
    }

    private void ensureCapacity(int longsToExpand) {
        long[] newData = new long[words.length + longsToExpand];
        System.arraycopy(words, 0, newData, 0, words.length);
        words = newData;
    }

    /**
     * checks if there is enough free space for bitAmount of space in the vector. if not, it extends capacity.
     *
     * @param bitAmount keyAmount of bits to check.
     */
    public void checkAndEnsureCapacity(int bitAmount) {
        if (words.length << 6 < size + bitAmount + 1) {
            ensureCapacity(capacityInterval);
        }
    }

    /**
     * returns the n.th bit value.This is an unsafe method. it does not check argument limits so it can throw an
     * ArrayIndexOutOfBound exception if n is equal or larger than size value or smaller than zero.
     *
     * @param n bit index.
     * @return bit value.
     */
    public boolean get(long n) {
        return (words[(int) (n >>> 6)] & longSetMasks[(int) (n & mod64Mask)]) != 0L;
    }

    /**
     * eliminates the long values that do not carry actual bits.
     */
    void compress() {
        long[] newData = new long[(int) (size >> 6) + 1];
        System.arraycopy(words, 0, newData, 0, (int) (size >> 6) + 1);
        words = newData;
    }

    /**
     * sets the n.th bit. This is an unsafe method. it does not check argument limits so it can throw an
     * ArrayIndexOutOfBound exception if n is equal or larger than size value or smaller than zero.
     *
     * @param n bit index
     */
    public void set(long n) {
        words[(int) (n >>> 6)] |= longSetMasks[(int) (n & mod64Mask)];
    }

    /**
     * sets the bit indexes from the an array .This is an unsafe method. it does not check argument limits so it can throw an
     * ArrayIndexOutOfBound exception if one of the value is equal or larger than size value or smaller than zero.
     *
     * @param n bit index array
     */
    public void set(long[] n) {
        for (long l : n) {
            words[(int) (l >>> 6)] |= longSetMasks[(int) (l & mod64Mask)];
        }
    }

    /**
     * resets the n.th bit. This is an unsafe method. it does not check argument limits so it can throw an
     * ArrayIndexOutOfBound exception if n is equal or larger than size value or smaller than zero.
     *
     * @param n bit index
     */
    public void clear(long n) {
        words[(int) (n >>> 6)] &= longResetMasks[(int) (n & mod64Mask)];
    }

    /**
     * resets the bit indexes from the an array .This is an unsafe method. it does not check argument limits so it can throw an
     * ArrayIndexOutOfBound exception if one of the value is equal or larger than size value or smaller than zero.
     *
     * @param n bit index array
     */
    public void clear(long[] n) {
        for (long l : n) {
            words[(int) (l >>> 6)] &= longResetMasks[(int) (l & mod64Mask)];
        }
    }

    /**
     * Custom serializer. Method does not closes the output stream.
     * @param dos data output stream to write.
     * @throws java.io.IOException
     */
    public void serialize(DataOutputStream dos) throws IOException {
        dos.writeInt(words.length);
        dos.writeLong(size);
        for (long word : words) {
            dos.writeLong(word);
        }
    }

    /**
     * Custom deserializer. It generates a LongBitVector from the stream. Method does not closes the input Stream.
     * @param dis input stream
     * @return a new LongbotVector loaded from the data input stream.
     * @throws java.io.IOException
     */
    public static LongBitVector deserialize(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        long size = dis.readLong();
        long[] words = new long[length];
        for (int i = 0; i < words.length; i++) {
            words[i] = dis.readLong();
        }
        return new LongBitVector(words, size);
    }
}
