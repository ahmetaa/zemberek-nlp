package zemberek.lm.compression;

import com.google.common.io.Files;
import zemberek.core.hash.IntHashKeyProvider;
import zemberek.core.hash.LargeNgramMphf;
import zemberek.core.hash.Mphf;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.core.logging.Log;
import zemberek.core.quantization.QuantizerType;

import java.io.*;
import java.util.BitSet;

public class UncompressedToSmoothLmConverter {
    private static final int VERSION = 1;
    File lmFile;
    File tempDir;

    int order;

    public UncompressedToSmoothLmConverter(File lmFile, File tempDir) {
        this.lmFile = lmFile;
        this.tempDir = tempDir;
    }

    public void convertSmall(File binaryUncompressedLmDir, NgramDataBlock block) throws IOException {
        convert(binaryUncompressedLmDir, block, SmoothLm.MphfType.SMALL, null, -1);
    }

    public void convertLarge(File binaryUncompressedLmDir, NgramDataBlock block, int chunkBits) throws IOException {
        convert(binaryUncompressedLmDir, block, SmoothLm.MphfType.LARGE, null, chunkBits);
    }

    public void convertLarge(File binaryUncompressedLmDir, NgramDataBlock block,
                             File[] oneBasedMphfFiles, int chunkBits) throws IOException {
        convert(binaryUncompressedLmDir, block, SmoothLm.MphfType.LARGE, oneBasedMphfFiles, chunkBits);
    }

    private void convert(File binaryUncompressedLmDir,
                         NgramDataBlock block,
                         SmoothLm.MphfType type,
                         File[] oneBasedMphfFiles,
                         int chunkBits) throws IOException {

        Log.info("Generating compressed language model.");

        MultiFileUncompressedLm lm = new MultiFileUncompressedLm(binaryUncompressedLmDir);

        lm.generateRankFiles(block.probabilitySize * 8, QuantizerType.BINNING);

        order = lm.order;

        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(lmFile)));

        // generate Minimal Perfect Hash functions for 2,3...n grams and save them as separate files.

        File[] phfFiles = new File[order + 1];
        if (oneBasedMphfFiles != null) {
            phfFiles = oneBasedMphfFiles;
        } else {
            for (int i = 2; i <= order; i++) {
                Mphf mphf;
                if (type == SmoothLm.MphfType.LARGE)
                    mphf = LargeNgramMphf.generate(lm.getGramFile(i), chunkBits);
                else
                    mphf = MultiLevelMphf.generate(lm.getGramFile(i));
                Log.info("MPHF is generated for order %d with %d keys. Average bits per key: %.3f",
                        i,
                        mphf.size(),
                        mphf.averageBitsPerKey());
                File mphfFile = new File(tempDir, lmFile.getName() + String.valueOf(i) + "gram.mphf");
                phfFiles[i] = mphfFile;
                mphf.serialize(mphfFile);
            }
        }
        // generate header.
        Log.info("Writing header");
        // write version and type info
        dos.writeInt(VERSION);

        // write Mphf type
        if (type == SmoothLm.MphfType.SMALL)
            dos.writeInt(0);
        else
            dos.writeInt(1);

        // write log-base
        dos.writeDouble(10d);
        // write n value for grams (3 for trigram model)
        dos.writeInt(order);
        // write counts, generate gramdata

        for (int i = 1; i <= order; i++) {
            dos.writeInt(lm.getCount(i));
        }

        // write rank lookup data (contains size+doubles)
        for (int i = 1; i <= order; i++) {
            Files.copy(lm.getProbabilityLookupFile(i), dos);
        }
        for (int i = 1; i <= order; i++) {
            if (i < order) {
                Files.copy(lm.getBackoffLookupFile(i), dos);
            }
        }

        Log.info("Reordering probability data and saving it together with n-gram fingerprints");
        for (int i = 1; i <= order; i++) {
            InMemoryBigByteArray probData = new InMemoryBigByteArray(lm.getProbRankFile(i));
            InMemoryBigByteArray backoffData = null;

            if (i < order) {
                backoffData = new InMemoryBigByteArray(lm.getBackoffRankFile(i));
            }

            ReorderData reorderData;
            final int gramCount = probData.count;
            if (i == 1) {
                int reorderedIndexes[] = new int[gramCount];
                for (int j = 0; j < gramCount; j++) {
                    reorderedIndexes[j] = j;
                }
                reorderData = new ReorderData(reorderedIndexes, new int[0]);
            } else {
                if (type == SmoothLm.MphfType.LARGE) {
                    reorderData = reorderIndexes(block, lm, i, LargeNgramMphf.deserialize(phfFiles[i]));
                } else {
                    reorderData = reorderIndexes(block, lm, i, MultiLevelMphf.deserialize(phfFiles[i]));
                }
            }
            Log.info("Validating reordered index array for order: %d", i);

            validateIndexArray(reorderData.reorderedKeyIndexes);

            int fingerPrintSize = block.fingerPrintSize;
            if (i == 1) {
                fingerPrintSize = 0;
            }
            int backOffSize = block.backoffSize;
            if (i == order) {
                backOffSize = 0;
            }

            dos.writeInt(gramCount);
            dos.writeInt(fingerPrintSize);
            dos.writeInt(block.probabilitySize);
            dos.writeInt(backOffSize);

            byte[] probBuff = new byte[block.probabilitySize];
            byte[] fpBuff = new byte[fingerPrintSize];
            byte[] backoffBuff = new byte[backOffSize];

            for (int k = 0; k < gramCount; k++) {
                // save fingerprint values for 2,3,.. grams.
                if (i > 1) {
                    block.fingerprintAsBytes(reorderData.fingerprints[k], fpBuff);
                    dos.write(fpBuff);
                }
                probData.get(reorderData.reorderedKeyIndexes[k], probBuff);
                dos.write(probBuff);
                // write backoff value if exists.
                if (backoffData != null) {
                    backoffData.get(reorderData.reorderedKeyIndexes[k], backoffBuff);
                    dos.write(backoffBuff);
                }
            }
        }

        // append size of the Perfect hash and its content.
        if (phfFiles.length > 0)
            Log.info("Saving MPHF values.");

        for (int i = 2; i <= order; i++) {
            Files.copy(phfFiles[i], dos);
        }

        // save vocabulary
        Log.info("Saving vocabulary.");
        Files.copy(lm.getVocabularyFile(), dos);

        dos.close();

    }

    private class ReorderData {
        int[] reorderedKeyIndexes;
        int[] fingerprints;

        private ReorderData(int[] reorderedKeyIndexes, int[] fingerprints) {
            this.reorderedKeyIndexes = reorderedKeyIndexes;
            this.fingerprints = fingerprints;
        }
    }

    private void validateIndexArray(int[] arr) {
        BitSet set = new BitSet();
        for (int i : arr) {
            if (i >= arr.length || i < 0)
                throw new IllegalStateException("array contains a value=" + i + " larger than the array size=" + arr.length);
            set.set(i);
        }
        for (int i = 0; i < arr.length; i++) {
            if (!set.get(i)) {
                throw new IllegalStateException("Not validated.");
            }
        }
    }

    /**
     * This class does the following:
     * suppose we have the keys as: [k0, k1, k2, k3, k4, k5] -> [0,1,2,3,4,5]
     * their mphf values are however: k0=2, k1=5, k2=0, k3=4, k4=1, k5=3
     * So what we want is to have key indexes in their minimal perfect has index order.
     * reordered keys indexes: [k2, k4, k0, k5, k3, k1] -> [2,4,0,5,3,1]
     *
     * @param lm     multifile language language model.
     * @param _order current order of language model
     * @param mphf   MPH function
     * @return reordered key indexes and those keys fingerprint values.
     * @throws IOException
     */
    private ReorderData reorderIndexes(NgramDataBlock block, MultiFileUncompressedLm lm, int _order, Mphf mphf) throws IOException {
        ChunkingNGramReader reader = new ChunkingNGramReader(lm.getGramFile(_order), _order, 1000000);
        int[] reorderedIndexes = new int[lm.getCount(_order)];
        int[] fingerPrints = new int[lm.getCount(_order)];
        int counter = 0;
        for (IntHashKeyProvider provider : reader) {
            for (int k = 0; k < provider.keyAmount(); k++) {
                final int[] key = provider.getKey(k);
                final int hashVal = mphf.get(key);
                reorderedIndexes[hashVal] = counter;
                fingerPrints[hashVal] = block.fingerprint(key);
                counter++;
            }
        }
        return new ReorderData(reorderedIndexes, fingerPrints);
    }

    public static class NgramDataBlock {
        int fingerPrintSize;
        int probabilitySize;
        int backoffSize;
        int fingerprintMask;

        public NgramDataBlock(int fingerPrintBits, int probabilityBits, int backoffBits) {
            if (fingerPrintBits % 8 != 0) {
                throw new IllegalArgumentException("FingerPrint bit size must be an order of 8");
            }
            if (probabilityBits % 8 != 0) {
                throw new IllegalArgumentException("Probability bit size must be an order of 8");
            }
            if (backoffBits % 8 != 0) {
                throw new IllegalArgumentException("Backoff bit size must be an order of 8");
            }
            this.fingerPrintSize = fingerPrintBits / 8;
            this.probabilitySize = probabilityBits / 8;
            this.backoffSize = backoffBits / 8;
            if (fingerPrintBits == 4) {
                this.fingerprintMask = 0xffffffff;
            } else
                this.fingerprintMask = (1 << fingerPrintBits) - 1;
        }

        int fingerprint(int[] key) {
            return MultiLevelMphf.hash(key, -1) & fingerprintMask;
        }

        void fingerprintAsBytes(int fingerprint, byte[] res) {
            int k = 0;
            switch (fingerPrintSize) {
                case 1:
                    res[k] = (byte) (fingerprint & 0xff);
                    break;
                case 2:
                    res[k] = (byte) ((fingerprint >>> 8) & 0xff);
                    res[k + 1] = (byte) (fingerprint & 0xff);
                    break;
                case 3:
                    res[k] = (byte) ((fingerprint >>> 16) & 0xff);
                    res[k + 1] = (byte) ((fingerprint >>> 8) & 0xff);
                    res[k + 2] = (byte) (fingerprint & 0xff);
                    break;
                case 4:
                    res[k] = (byte) ((fingerprint >>> 24) & 0xff);
                    res[k + 1] = (byte) ((fingerprint >>> 16) & 0xff);
                    res[k + 2] = (byte) ((fingerprint >>> 8) & 0xff);
                    res[k + 3] = (byte) (fingerprint & 0xff);
                    break;
            }
        }

        void fingerprintAsBytes(int[] key, byte[] res) {
            fingerprintAsBytes(MultiLevelMphf.hash(key, -1) & fingerprintMask, res);
        }
    }
}
