package zemberek.lm.compression;

import java.io.DataInputStream;
import java.io.IOException;

public class GramDataArray {
    int count; // gram count

    int blockSize; // defines the size of the key data. Such as if 3 bytes FP, 2 bytes Prob , 2 Bytes Backoff blockSize = 7
    final int pageShift; // for getting the page index value this amount of left shift is used. page index value resides on higher bits.
    final int indexMask; // used for obtaining the actual index of the key block.
    byte[][] data; // holds the actual data. [page count][page length * block size ] bytes
    private static final int MAX_BUF = 0x3fffffff;
    final int fpSize; // length of fingerprint in bytes
    final int fpMask; // to access fingerprint data length in bytes.
    final int probSize; // size of probability data length in bytes
    final int backoffSize; // size of backoff length in bytes

    int getPowerOf2(int k, int limit) {
        if (k <= 2)
            return 1;
        int i = 1;
        while (i < k) {
            i *= 2;
        }
        if (i >= limit)
            return i / 2;
        else return i;
    }

    public GramDataArray(DataInputStream dis) throws IOException {
        count = dis.readInt();
        this.fpSize = dis.readInt();
        this.probSize = dis.readInt();
        this.backoffSize = dis.readInt();

        if (fpSize == 4)
            fpMask = 0xffffffff;
        else
            fpMask = (1 << (fpSize * 8)) - 1;

        blockSize = fpSize + probSize + backoffSize;
        int pageLength = getPowerOf2(MAX_BUF / blockSize, MAX_BUF / blockSize);
        pageShift = 32 - Integer.numberOfLeadingZeros(pageLength - 1);
        indexMask = (1 << pageShift - 1) - 1;
        long l = 0;
        int pageCounter = 0;
        while (l < count * blockSize) {
            pageCounter++;
            l += (pageLength * blockSize);
        }
        data = new byte[pageCounter][];
        int total = 0;
        for (int i = 0; i < pageCounter; i++) {
            if (i < pageCounter - 1) {
                data[i] = new byte[pageLength * blockSize];
                total += pageLength * blockSize;
            } else
                data[i] = new byte[count * blockSize - total];
            dis.readFully(data[i]);
        }
    }

    public int getFingerPrint(int index) {
        final int pageIndex = (index & indexMask) * blockSize;
        byte[] d = data[index >>> pageShift];
        switch (fpSize) {
            case 1:
                return d[pageIndex] & 0xff;
            case 2:
                return ((d[pageIndex] & 0xff) << 8) |
                        (d[pageIndex + 1] & 0xff);
            case 3:
                return ((d[pageIndex] & 0xff) << 16) |
                        ((d[pageIndex + 1] & 0xff) << 8) |
                        (d[pageIndex + 2] & 0xff);
            case 4:
                return ((d[pageIndex] & 0xff) << 24) |
                        ((d[pageIndex + 1] & 0xff) << 16) |
                        ((d[pageIndex + 2] & 0xff) << 8) |
                        (d[pageIndex + 3] & 0xff);
        }
        return -1;
    }

    public boolean checkFingerPrint(int fpToCheck_, int globalIndex) {
        final int fpToCheck = fpToCheck_ & fpMask;
        final int pageIndex = (globalIndex & indexMask) * blockSize;
        byte[] d = data[globalIndex >>> pageShift];
        switch (fpSize) {
            case 1:
                return fpToCheck == (d[pageIndex] & 0xff);
            case 2:
                return (fpToCheck >>> 8 == (d[pageIndex] & 0xff)) && ((fpToCheck & 0xff) == (d[pageIndex + 1] & 0xff));
            case 3:
                return (fpToCheck >>> 16 == (d[pageIndex] & 0xff)) &&
                        ((fpToCheck >>> 8 & 0xff) == (d[pageIndex + 1] & 0xff)) &&
                        ((fpToCheck & 0xff) == (d[pageIndex + 2] & 0xff));
            case 4:
                return (fpToCheck >>> 24 == (d[pageIndex] & 0xff)) &&
                        ((fpToCheck >>> 16 & 0xff) == (d[pageIndex + 1] & 0xff)) &&
                        ((fpToCheck >>> 8 & 0xff) == (d[pageIndex + 2] & 0xff)) &&
                        ((fpToCheck & 0xff) == (d[pageIndex + 3] & 0xff));
            default:
                throw new IllegalStateException("fpSize must be between 1 and 4");
        }
    }

    public int getProbabilityRank(int index) {
        final int pageId = index >>> pageShift;
        final int pageIndex = (index & indexMask) * blockSize + fpSize;
        byte[] d = data[pageId];
        switch (probSize) {
            case 1:
                return d[pageIndex] & 0xff;
            case 2:
                return ((d[pageIndex] & 0xff) << 8) |
                        (d[pageIndex + 1] & 0xff);
            case 3:
                return ((d[pageIndex] & 0xff) << 16) |
                        ((d[pageIndex + 1] & 0xff) << 8) | (d[pageIndex + 2] & 0xff);
        }
        return -1;
    }

    /**
     * loads fingerprint, probability and backoff values to a single integer.
     * this is only applicaple when 16 bit fingerprint and 8 bits quantized prob-backoff values are used.
     *
     * @param index index value
     * @return integer carrying all fingerprint, probability and backoff value. structure is:
     *         [fingerprint|probability rank|backoff rank]
     */
    public int getCompact(int index) {
        final int pageIndex = (index & indexMask) * blockSize;
        final byte[] d = data[index >>> pageShift];
        return ((d[pageIndex] & 0xff) << 24) |
                ((d[pageIndex + 1] & 0xff) << 16) |
                ((d[pageIndex + 2] & 0xff) << 8) |
                (d[pageIndex + 3] & 0xff);
    }

    public int getBackoffRank(int index) {
        final int pageId = index >>> pageShift;
        final int pageIndex = (index & indexMask) * blockSize + fpSize + probSize;
        byte[] d = data[pageId];
        switch (backoffSize) {
            case 1:
                return d[pageIndex] & 0xff;
            case 2:
                return ((d[pageIndex] & 0xff) << 8) | (d[pageIndex + 1] & 0xff);
            case 3:
                return ((d[pageIndex] & 0xff) << 16) | ((d[pageIndex + 1] & 0xff) << 8) | (d[pageIndex + 2] & 0xff);
        }
        return -1;
    }

    void load(int index, byte[] buff) {
        System.arraycopy(data[index >>> pageShift], (index & indexMask) * blockSize, buff, 0, blockSize);
    }
}
