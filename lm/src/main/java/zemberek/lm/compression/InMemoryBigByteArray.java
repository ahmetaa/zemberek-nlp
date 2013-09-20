package zemberek.lm.compression;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class InMemoryBigByteArray {
    int count;
    int blockSize;
    final int pageShift;
    final int indexMask;
    byte[][] data;
    private static final int MAX_BUF = 0x3fffffff;

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

    public int getCount() {
        return count;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public InMemoryBigByteArray(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        count = raf.readInt();
        blockSize = raf.readInt();
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
            raf.readFully(data[i]);
        }
        raf.close();
    }

    public void get(int index, byte[] buff) {
        final int pageId = index >>> pageShift;
        final int pageIndex = (index & indexMask) * blockSize;
        byte[] d = data[pageId];
        System.arraycopy(d, pageIndex, buff, 0, blockSize);
    }

    public int getInt(int index) {
        final int pageId = index >>> pageShift;
        final int pageIndex = (index & indexMask) * blockSize;
        byte[] d = data[pageId];
        switch (blockSize) {
            case 1:
                return d[pageIndex] & 0xff;
            case 2:
                return ((d[pageIndex] & 0xff) << 8) | (d[pageIndex + 1] & 0xff);
            case 3:
                return ((d[pageIndex] & 0xff) << 16) | ((d[pageIndex + 1] & 0xff) << 8) | (d[pageIndex + 2] & 0xff);
        }
        return -1;
    }

    public float getFloat(int index) {
        final int pageId = index >>> pageShift;
        final int pageIndex = (index & indexMask) * blockSize;
        byte[] d = data[pageId];
        return Float.intBitsToFloat(
                ((d[pageIndex] & 0xff) << 24) |
                        ((d[pageIndex + 1] & 0xff) << 16) |
                        ((d[pageIndex + 2] & 0xff) << 8) |
                        (d[pageIndex + 3] & 0xff));
    }

}
