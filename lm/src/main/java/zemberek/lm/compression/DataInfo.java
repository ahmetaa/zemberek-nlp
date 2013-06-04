package zemberek.lm.compression;

public class DataInfo {
    public final int fpBits;
    public final int probBits;
    public final int backoffBits;
    public final int byteCount;
    public final int fpByteCount;
    public final int fpMask;
    public final int probByteCount;
    public final int probMask;
    public final int backOffByteCount;
    public final int backOffMask;

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

        fpByteCount = byteCount(fpBits);
        fpMask = 1 << fpBits;

        probByteCount = byteCount(probBits);
        probMask = 1 << probBits;

        backOffByteCount = byteCount(backoffBits);
        backOffMask = 1 << backoffBits;
    }

    static int byteCount(int bitCout) {
        return (bitCout & 7) == 0 ? (bitCout / 8) : (bitCout / 8) + 1;
    }

    static DataInfo fromCounts(int minfingerPrintBits, int probCount, int backoffCount) {
        int probBits = probCount == 0 ? 0 : minBitCount(probCount);
        int backoffBits = backoffCount == 0 ? 0 : minBitCount(backoffCount);
        return new DataInfo(minfingerPrintBits, probBits, backoffBits);
    }

    static DataInfo fromCountsAndExpectedBits(int minfingerPrintBits, int probCount, int probBitsDesired, int backoffCount, int backoffBitsDesired) {
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

    int getFp(byte[] data) {
        int f = 0;
        for (int i = 0; i < fpByteCount; i++) {
            f = f | data[i];
            f = f << 8;
        }
        return f & fpMask;
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
