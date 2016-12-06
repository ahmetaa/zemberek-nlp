package zemberek.lm.compression;


import zemberek.core.hash.ByteGramProvider;
import zemberek.core.hash.IntHashKeyProvider;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Iterator;

public class ChunkingNGramReader implements Iterable<IntHashKeyProvider> {

    File file;
    int chunkGramSize;
    int chunkByteSize;
    int order;

    public ChunkingNGramReader(File file, int order, int chunkGramSize) {
        this.file = file;
        this.order = order;
        this.chunkGramSize = chunkGramSize;
        this.chunkByteSize = chunkGramSize * order * 4;
    }

    public Iterator<IntHashKeyProvider> iterator() {
        return new ChunkIterator();
    }

    private class ChunkIterator implements Iterator<IntHashKeyProvider> {

        RandomAccessFile raf;
        int readByteAmount;
        byte[] data;

        private ChunkIterator() {
            data = new byte[chunkByteSize];
            try {
                raf = new RandomAccessFile(file, "r");
                raf.skipBytes(8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean hasNext() {
            try {
                readByteAmount = raf.read(data);
                if (readByteAmount > 0) {
                    if (readByteAmount < chunkByteSize) {
                        data = Arrays.copyOf(data, readByteAmount);
                    }
                    return true;
                } else {
                    raf.close();
                    return false;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        public IntHashKeyProvider next() {
            return new ByteGramProvider(data, order, readByteAmount / (order * 4));
        }

        public void remove() {
            throw new UnsupportedOperationException("Not valid!");
        }
    }

}
