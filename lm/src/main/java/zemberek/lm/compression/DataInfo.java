package zemberek.lm.compression;

// TODO: experimental work.
public class DataInfo {

    // total amount ot finger print bits. fingerprint data starts from index 0.
    public final int fpBits;

    // This represents the last byte right shift count to retrieve fp data from byte array.
    // For example if fpBits is 13 int (int representation: MSB|---abcdefghijklmn|LSB )
    // Then  it will be put to the byte array as : B0=|abcdefgh| B1=|ijklmn---| So the fpLastByteRightShiftCount = 3
    public final int fpLastByteRightShiftCount;

    //The index of last byte that contains fingerprint data.
    public final int fpEndByte;

    public final int probBits;

    //The index of first byte that contains probability data.
    public final int probStartByte;

    // this defines amount of higher bits to truncate from the first byte of probability.
    // Suppose first byte of the probability data is : |---abcde| then mask needs to truncate most significant 3 bits.
    public final int probFirstByteMask;

    // This represents the amount of left shift required for getting the probability data from the last byte.
    // public final int probLastByteRightShiftCount;

    //The index of last byte that contains probability data.
    public final int probEndByte;

    public final int backoffBits;

    public final int byteCount;

    DataInfo(int minfingerPrintBits, int probBits, int backoffBits) {

        this.probBits = probBits;
        this.backoffBits = backoffBits;

        int total = minfingerPrintBits + probBits + backoffBits;
        if (total % 8 != 0) {
            total = ((total / 8) + 1) * 8;
            minfingerPrintBits = total - (probBits + backoffBits);
        }
        this.fpBits = minfingerPrintBits;
        byteCount = (fpBits + probBits + backoffBits) / 8;
        this.fpLastByteRightShiftCount = fpBits % 8;

        this.fpEndByte = fpBits / 8;

        if (fpLastByteRightShiftCount != 0) {
            probStartByte = fpEndByte;
            probFirstByteMask = 1 << (7 - fpLastByteRightShiftCount);
        } else {
            probStartByte = fpEndByte + 1;
            probFirstByteMask = 0xff;
        }

        int probLastByteEndBitIndex = (fpBits + probBits)%8;


        probEndByte = (fpBits + probBits) / 8;



    }

    static DataInfo fromCounts(int minfingerPrintBits, int probCount, int backoffCount) {
        int probBits = probCount == 0 ? 0 : minBitCount(probCount);
        int backoffBits = backoffCount == 0 ? 0 : minBitCount(backoffCount);
        return new DataInfo(minfingerPrintBits, probBits, backoffBits);
    }

    static DataInfo fromCountsAndExpectedBits(
            int minfingerPrintBits,
            int probCount,
            int probBitsDesired,
            int backoffCount,
            int backoffBitsDesired) {
        DataInfo initial = fromCounts(minfingerPrintBits, probCount, backoffCount);
        int bb = backoffBitsDesired;
        if (initial.backoffBits < bb)
            bb = initial.backoffBits;
        int pb = probBitsDesired;
        if (initial.probBits < pb)
            pb = initial.probBits;
        return new DataInfo(minfingerPrintBits, pb, bb);
    }

    byte[] encode(int fp, int probIndex, int backoffIndex) {
        long k = backoffIndex;
        k = k << probBits;
        k |= probIndex;
        k = k << fpBits;
        k |= fp;
        byte[] result = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            result[i] = (byte) (k & 0xff);
            k = k >>> 8;
        }
        return result;
    }

    byte[] encode2(int fp, int prob, int backoff) {

        byte[] arr = new byte[byteCount];

        return arr;


    }

    @Override
    public String toString() {
        return "DataInfo{" +
                "fpBits=" + fpBits +
                ", probBits=" + probBits +
                ", backoffBits=" + backoffBits +
                ", byteCount=" + byteCount +
                '}';
    }

    public static final double LOG_2 = Math.log(2);

    /**
     * Calculates 2 base logarithm
     *
     * @param input value to calculate log
     * @return 2 base logarithm of the input
     */
    public static double log2(double input) {
        return Math.log(input) / LOG_2;
    }

    public static boolean powerOfTwo(int k) {
        if (k < 0)
            throw new IllegalArgumentException("Cannot calculate negative numbers:" + k);
        return (k & (k - 1)) == 0;
    }

    public static int minBitCount(int a) {
        int probBits = (int) log2(a);
        if (!powerOfTwo(a))
            probBits++;
        return probBits;
    }

    public static void main(String[] args) {
        System.out.println(fromCounts(10, 100000, 0));
        System.out.println(fromCountsAndExpectedBits(10, 200, 4, 100, 10));
    }
}
