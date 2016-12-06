package zemberek.core.hash;

import com.google.common.io.Files;
import zemberek.core.logging.Log;

import java.io.*;

/**
 * This is a MPHF implementation suitable for very large key sets.
 */
public class LargeNgramMphf implements Mphf {

    private static final int DEFAULT_CHUNK_SIZE_IN_BITS = 22;
    final int maxBitMask;
    final int bucketMask;
    final int pageShift;

    MultiLevelMphf mphfs[];
    int[] offsets;

    public LargeNgramMphf(int maxBitMask, int bucketMask, int pageShift, MultiLevelMphf[] mphfs, int[] offsets) {
        this.maxBitMask = maxBitMask;
        this.bucketMask = bucketMask;
        this.pageShift = pageShift;
        this.mphfs = mphfs;
        this.offsets = offsets;
    }

    /**
     * Same as generate(File file, int chunkBits) but uses DEFAULT_CHUNK_SIZE_IN_BITS for chunk size.
     *
     * @param file binary key file
     * @return generated LargeNgramMphf
     */
    public static LargeNgramMphf generate(File file) throws IOException {
        return generate(file, DEFAULT_CHUNK_SIZE_IN_BITS);
    }

    /**
     * Generates MPHF from a binary integer key file. File needs to be in this structure:
     * int32 order (how many integers each key)
     * int32 amount of keys . Max is 2^31-1
     * int32... key1
     * int32... key2
     * <p/>
     * Keys in the file must be unique. During generation of PHF system does not check for uniqueness.
     * System does the following:
     * <p/>
     * it splits the total amount of keys to large chunks (2^23 ~ 8 million keys by default.)
     * during split operation, all keys are divided to a chunk file according to it's bucket index.
     * this is calculated with PHF algorithms generic hash function g()
     * Key counts in each chunk may be different. But they are generally close values. These count values are important
     * Because they are used during global PHF value. Values are stored in an array (offsets)
     * <p/>
     * After files chunks are generated, a PHF is calculated for each chunk. And they are stored in an array
     *
     * @param file binary key file
     * @return LargeNgramMphf fro the keys in the file
     * @throws IOException If an error occurs during file access.
     */
    public static LargeNgramMphf generate(File file, int chunkBits) throws IOException {
        File tmp = Files.createTempDir();
        Splitter splitter = new Splitter(file, tmp, chunkBits);
        Log.info("Gram count: " + splitter.gramCount);
        Log.info("Segment count: " + splitter.pageCount);
        Log.info("Avrg segment size: " + (1 << splitter.pageBit));
        Log.info("Segmenting File...");
        splitter.split();
        int bucketBits = splitter.pageBit - 2;
        if (bucketBits <= 0)
            bucketBits = 1;
        MultiLevelMphf[] mphfs = new MultiLevelMphf[splitter.pageCount];
        int[] offsets = new int[splitter.pageCount];
        int total = 0;
        for (int i = 0; i < splitter.pageCount; i++) {
            final ByteGramProvider keySegment = splitter.getKeySegment(i);
            Log.debug("Segment key count: " + keySegment.keyAmount());
            Log.debug("Segment bucket ratio: " + ((double) keySegment.keyAmount() / (1 << bucketBits)));
            total += keySegment.keyAmount();
            MultiLevelMphf mphf = MultiLevelMphf.generate(keySegment);
            Log.info("MPHF is generated for segment %d with %d keys. Average bits per key: %.3f",
                    i,
                    mphf.size(),
                    mphf.averageBitsPerKey());
            mphfs[i] = mphf;
            if (i > 0)
                offsets[i] = offsets[i - 1] + mphfs[i - 1].size();
        }
        Log.debug("Total processed keys:" + total);
        int maxMask = (1 << splitter.maxBit) - 1;
        int bucketMask = (1 << bucketBits) - 1;
        return new LargeNgramMphf(maxMask, bucketMask, splitter.pageShift, mphfs, offsets);
    }

    public int get(int[] ngram) {
        final int hash = MultiLevelMphf.hash(ngram, -1);
        final int pageIndex = (hash & maxBitMask) >>> pageShift;
        return mphfs[pageIndex].get(ngram, hash) + offsets[pageIndex];
    }

    public int get(int[] ngram, int hash) {
        final int pageIndex = (hash & maxBitMask) >>> pageShift;
        return mphfs[pageIndex].get(ngram, hash) + offsets[pageIndex];
    }

    public int get(int g1, int g2, int g3, int hash) {
        final int pageIndex = (hash & maxBitMask) >>> pageShift;
        return mphfs[pageIndex].get(g1,g2,g3, hash) + offsets[pageIndex];
    }

    public int get(int g1, int g2, int hash) {
        final int pageIndex = (hash & maxBitMask) >>> pageShift;
        return mphfs[pageIndex].get(g1,g2,hash) + offsets[pageIndex];
    }

    public int get(String ngram) {
        final int hash = MultiLevelMphf.hash(ngram, -1);
        final int pageIndex = (hash & maxBitMask) >>> pageShift;
        return mphfs[pageIndex].get(ngram, hash) + offsets[pageIndex];
    }

    public int get(String ngram, int hash) {
        final int pageIndex = (hash & maxBitMask) >>> pageShift;
        return mphfs[pageIndex].get(ngram, hash) + offsets[pageIndex];
    }

    public int get(int[] ngram, int begin, int end, int hash) {
        final int pageIndex = (hash & maxBitMask) >>> pageShift;
        return mphfs[pageIndex].get(ngram, begin, end, hash) + offsets[pageIndex];
    }

    private static class Splitter {
        File gramFile;
        File[] files;
        int order;
        int gramCount;
        File tmpDir;

        int maxBit;
        int pageCount;
        int pageBit;

        final int pageShift;
        final int pageMask;

        public Splitter(File gramFile, File tmpdir, int pageBit) throws IOException {
            this.gramFile = gramFile;
            RandomAccessFile raf = new RandomAccessFile(gramFile, "r");
            this.order = raf.readInt();
            this.gramCount = raf.readInt();
            raf.close();
            this.tmpDir = tmpdir;
            this.pageBit = pageBit;

            // maxbit ıs the x value where 2^(x-1)<gramCount<2^x
            maxBit = getBitCountHigher(gramCount);
            if (maxBit == 0)
                maxBit = 1;

            // if the x is smaller than the default page bit, we use the x value for the page bit.
            if (maxBit < this.pageBit) {
                this.pageBit = maxBit;
                pageShift = maxBit;
            } else
                pageShift = this.pageBit;

            int pageLength = 1 << this.pageBit;
            pageMask = pageLength - 1;
            pageCount = 1 << (maxBit - this.pageBit);
            files = new File[pageCount];
        }

        int getBitCountHigher(int num) {
            int i = 1;
            int c = 0;
            while (i < num) {
                i *= 2;
                c++;
            }
            return c;
        }

        public int keyAmount() {
            return gramCount;
        }

        public void split() throws IOException {

            if (pageCount == 1) {
                files[0] = gramFile;
                return;
            }

            FileKeyWriter[] fileKeyWriters = new FileKeyWriter[pageCount];
            int[] counts = new int[pageCount];

            for (int i = 0; i < pageCount; i++) {
                files[i] = new File(tmpDir, order + "-gramidfile" + i + ".batch");
                fileKeyWriters[i] = new FileKeyWriter(files[i], order);
            }

            byte[] buffer = new byte[(1 << pageBit) * 4 * order];
            RandomAccessFile raf = new RandomAccessFile(gramFile, "r");
            raf.skipBytes(8);
            int actual;
            while ((actual = raf.read(buffer)) > 0) {
                if (actual % (order * 4) != 0)
                    throw new IllegalStateException("Cannot read order*4 aligned bytes from:" + gramFile);

                int[] gramIds = new int[order];
                int p = 0;
                int maxBitMask = (1 << maxBit) - 1;
                for (int i = 0; i < actual; i += 4) {
                    gramIds[p++] = (buffer[i] & 0xff) << 24 | (buffer[i + 1] & 0xff) << 16 | (buffer[i + 2] & 0xff) << 8 | (buffer[i + 3] & 0xff);
                    if (p == order) {
                        int hash = MultiLevelMphf.hash(gramIds, -1) & maxBitMask;
                        int segmentId = hash >>> pageShift;
                        fileKeyWriters[segmentId].write(gramIds);
                        counts[segmentId]++;
                        p = 0;
                    }
                }
            }
            raf.close();
            int i = 0;
            for (FileKeyWriter writer : fileKeyWriters) {
                writer.close();
                writer.changeCount(counts[i++]);
            }
        }

        public ByteGramProvider getKeySegment(int segment) throws IOException {
            return new ByteGramProvider(files[segment]);
        }
    }

    private static class FileKeyWriter implements Closeable {

        DataOutputStream dos;
        File file;
        int order;

        private FileKeyWriter(File file, int order) throws IOException {
            this.file = file;
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), 1000000));
            dos.writeInt(order);
            dos.writeInt(0);
            this.order = order;
        }

        void write(int[] key) throws IOException {
            for (int i : key) {
                dos.writeInt(i);
            }
        }

        void changeCount(int count) throws IOException {
            try (RandomAccessFile rafw = new RandomAccessFile(file, "rw")) {
                rafw.writeInt(order);
                rafw.writeInt(count);
            }
        }

        public void close() throws IOException {
            dos.close();
        }
    }

    /**
     * A custom serializer.
     *
     * @param file file to serialize data.
     * @throws IOException if an error occurs during file access.
     */
    public void serialize(File file) throws IOException {
        serialize(new BufferedOutputStream(new FileOutputStream(file), 1000000));
    }

    public void serialize(OutputStream os) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(os)) {
            dos.writeInt(this.maxBitMask);
            dos.writeInt(this.bucketMask);
            dos.writeInt(this.pageShift);
            dos.writeInt(this.mphfs.length);
            for (int offset : offsets) {
                dos.writeInt(offset);
            }
            for (MultiLevelMphf mphf : mphfs) {
                mphf.serialize(dos);
            }
        }
    }

    /**
     * A custom deserializer.
     *
     * @param file file that contains serialized data.
     * @param skip amoutn to skip
     * @return a new FastMinimalPerfectHash object.
     * @throws IOException if an error occurs during file access.
     */
    public static LargeNgramMphf deserialize(File file, long skip) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 1000000))) {
            long actualSkip = dis.skip(skip);
            if (actualSkip != skip)
                throw new IOException("Cannot skip necessary amount of bytes from stream:" + skip);
            return deserialize(dis);
        }
    }

    /**
     * A custom deserializer.
     *
     * @param file file that contains serialized data.
     * @return a new FastMinimalPerfectHash object.
     * @throws IOException if an error occurs during file access.
     */
    public static LargeNgramMphf deserialize(File file) throws IOException {
        return deserialize(file, 0);
    }

    /**
     * A custom deserializer. Look serialization method document for the format.
     *
     * @param dis DataInputStream that contains serialized data.
     * @return a new ChdPerfectHash object.
     * @throws IOException if an error occurs during stream access.
     */
    public static LargeNgramMphf deserialize(DataInputStream dis) throws IOException {
        int maxBitMask = dis.readInt();
        int bucketMask = dis.readInt();
        int pageShift = dis.readInt();
        int phfCount = dis.readInt();

        int[] offsets = new int[phfCount];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = dis.readInt();
        }
        MultiLevelMphf[] hashes = new MultiLevelMphf[phfCount];
        for (int i = 0; i < offsets.length; i++) {
            hashes[i] = MultiLevelMphf.deserialize(dis);
        }
        return new LargeNgramMphf(maxBitMask, bucketMask, pageShift, hashes, offsets);
    }

    @Override
    public double averageBitsPerKey() {
        double total = 0;
        for (MultiLevelMphf mphf : mphfs) {
            total = total + mphf.averageBitsPerKey();
        }
        return mphfs.length > 0 ? total / mphfs.length : 0;
    }

    @Override
    public int size() {
        int size = 0;
        for (MultiLevelMphf mphf : mphfs) {
            size += mphf.size();
        }
        return size;
    }
}