package zemberek.core.collections;

/**
 * A fixed size bit vector. Size can be maximum 2^31-1
 */
public class FixedBitVector {
    private int[] words;
    public final int length;

    private static final int[] setMasks = new int[32];
    private static final int[] resetMasks = new int[32];

    static {
        for (int i = 0; i < 32; i++) {
            setMasks[i] = 1 << i;
            resetMasks[i] = ~setMasks[i];
        }
    }

    public FixedBitVector(int length) {
        if (length < 0)
            throw new IllegalArgumentException("Length cannot be negative. But it is:" + length);
        this.length = length;
        int wordCount = ((length + 31) >> 5);
        words = new int[wordCount];
    }

    public boolean get(int n) {
        return (words[n >> 5] & setMasks[n & 31]) != 0;
    }

    public boolean safeGet(int n) {
        check(n);
        return (words[n >> 5] & setMasks[n & 31]) != 0;
    }

    private void check(int n) {
        if (n < 0 || n >= length)
            throw new IllegalArgumentException("Value must be between 0 and " + length + ". But it is " + n);
    }

    public void set(int n) {
        words[n >> 5] |= setMasks[n & 31];
    }

    public void safeSet(int n) {
        check(n);
        words[n >> 5] |= setMasks[n & 31];
    }

    public void clear(int n) {
        words[n >> 5] &= resetMasks[n & 31];
    }

    public void safeClear(int n) {
        check(n);
        words[n >> 5] &= resetMasks[n & 31];
    }

    public int numberOfOnes() {
        int count = 0;
        for (int word : words)
            count += Integer.bitCount(word);
        return count;
    }

    public int numberOfNewOneBitCount(FixedBitVector other) {
        int total = 0;
        for (int i = 0; i < this.length; i++) {
            if (!this.get(i) && other.get(i)) {
                total++;
            }
        }
        return total;
    }

    public int differentBitCount(FixedBitVector other) {
        int total = 0;
        for (int i = 0; i < this.length; i++) {
            if (this.get(i) != other.get(i)) {
                total++;
            }
        }
        return total;
    }

    /**
     * @return number of zeroes
     */
    public int numberOfZeroes() {
        return length - numberOfOnes();
    }

    /**
     * @return an array containing 0 bit indexes.
     */
    public int[] zeroIndexes() {
        int[] zeroIndexes = new int[numberOfZeroes()];
        int j = 0;
        for (int i = 0; i < length; i++) {
            if (!get(i))
                zeroIndexes[j++] = i;
        }
        return zeroIndexes;
    }

    /**
     * Used only for test purposes.
     *
     * @param bits bit string. It can contain space characters
     * @return bit vector equivalent.
     */
    static FixedBitVector fromBinaryString(String bits) {
        bits = bits.replaceAll("\\s+", "");
        FixedBitVector vector = new FixedBitVector(bits.length());

        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1')
                vector.set(i);
            else
                vector.clear(i);
        }
        return vector;
    }
}
