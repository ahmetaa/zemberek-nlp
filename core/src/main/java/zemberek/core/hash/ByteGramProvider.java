package zemberek.core.hash;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ByteGramProvider implements IntHashKeyProvider {

    byte[] data;
    int order;
    int ngramCount;

    public ByteGramProvider(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        this.order = raf.readInt();
        this.ngramCount = raf.readInt();
        final int byteAmount = order * ngramCount * 4;
        data = new byte[byteAmount];
        int actual = raf.read(data);
        if (actual != byteAmount)
            throw new IllegalStateException("File suppose to have " + byteAmount + " bytes for " + ngramCount + " ngrams");
        raf.close();
    }

    public ByteGramProvider(byte[] data, int order, int ngramCount) {
        this.data = data;
        this.order = order;
        this.ngramCount = ngramCount;
    }

    public int[] getKey(int index) {
        int[] res = new int[order];
        int start = index * order * 4;
        int p = 0;
        for (int i = start; i < start + order * 4; i += 4) {
            res[p++] = (data[i] & 0xff) << 24 | (data[i + 1] & 0xff) << 16 | (data[i + 2] & 0xff) << 8 | (data[i + 3] & 0xff);
        }
        return res;
    }

    public void getKey(int index, int[] b) {
        int start = index * order * 4;
        int p = 0;
        for (int i = start; i < start + order * 4; i += 4) {
            b[p++] = (data[i] & 0xff) << 24 | (data[i + 1] & 0xff) << 16 | (data[i + 2] & 0xff) << 8 | (data[i + 3] & 0xff);
        }
    }

    public byte[] getKeyAsBytes(int index) {
        int start = index * order * 4;
        byte[] buf = new byte[order * 4];
        System.arraycopy(data, start, buf, 0, buf.length);
        return buf;
    }

    public int keyAmount() {
        return ngramCount;
    }
}
