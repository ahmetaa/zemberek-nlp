package zemberek.core.hash;

import zemberek.core.collections.LongBitVector;
import zemberek.core.logging.Log;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a Minimum Perfect Hash Function implementation (MPHF). A MPHF generates distinct integers for
 * a unique set of n keys in the range of [0,...,n-1]
 * <p/>
 * MultiLevelMphf can be considered as an improvement over MOS Algorithm II of
 * Fox, Heath, Chen, and Daoud's Practical minimal perfect hash `functions for large databases [CACM 1992]
 * and MPHF structure defined in
 * Belazzougui, Botelho and Dietzfelbinger's Hash, Displace and Compress (2009) papers.
 * <p/>
 * It is different in these aspects:
 * <p/>
 * MOS algorithm implementation requires two integer arrays.
 * First one carries hash seed values for buckets with more than one keys.
 * Second one contains MPHF values for 1 key buckets.
 * <p/>
 * MultiLevelMphf does not follow this. It uses byte arrays instead of int arrays for bucket hash seed values.
 * If a bucket's keys fail to find a hash index value that maps to zero bit indexes after the index value 255,
 * it stops trying and marks it as failed bucket. Keys in the failed buckets are applied to the same mechanism until
 * no failed bucket is left recursively. System carries an extra int index array for next levels failed buckets.
 * <p/>
 * MOS implementation may fail to generate a MPHF. But MultiLevelMphf guarantees generation of MPHF.
 * <p/>
 * For same byte per key values, MultiLevelMPHF generates hash values orders of magnitude faster than MOS.
 * </p>
 * It is also different than Belazzougui et al.'s approach as it does not apply integer array compression.
 * </p>
 * MultiLevelMPHF typically uses around 3.1-3.2 bits memory per key.
 */
public class MultiLevelMphf implements Mphf {

    public static final int HASH_MULTIPLIER = 16777619;
    final HashIndexes[] hashLevelData;

    public static MultiLevelMphf generate(IntHashKeyProvider keyProvider) {
        BucketCalculator bc = new BucketCalculator(keyProvider);
        return new MultiLevelMphf(bc.calculate());
    }

    public static MultiLevelMphf generate(File binaryKeyFile) throws IOException {
        return generate(new ByteGramProvider(binaryKeyFile));
    }

    private MultiLevelMphf(HashIndexes[] hashLevelData) {
        this.hashLevelData = hashLevelData;
    }

    public int size() {
        return hashLevelData[0].keyAmount;
    }

    private static class HashIndexes {
        final int keyAmount;
        final int bucketAmount;
        final byte[] bucketHashSeedValues;
        final int[] failedIndexes;

        HashIndexes(int keyAmount, int bucketAmount, byte[] bucketHashSeedValues, int[] failedIndexes) {
            this.keyAmount = keyAmount;
            this.bucketAmount = bucketAmount;
            this.bucketHashSeedValues = bucketHashSeedValues;
            this.failedIndexes = failedIndexes;
        }

        int getSeed(int fingerPrint) {
            return (bucketHashSeedValues[fingerPrint % bucketAmount]) & 0xff;
        }
    }

    public int getLevelCount() {
        return hashLevelData.length;
    }

    public static final int INITIAL_HASH_SEED = 0x811C9DC5;

    public static int hash(byte[] data, int seed) {
        int d = seed > 0 ? seed : INITIAL_HASH_SEED;
        for (int a : data) {
            d = (d ^ a) * HASH_MULTIPLIER;
        }
        return d & 0x7fffffff;
    }

    public static int hash(int[] data, int seed) {
        int d = seed > 0 ? seed : INITIAL_HASH_SEED;
        for (int a : data) {
            d = (d ^ a) * HASH_MULTIPLIER;
        }
        return d & 0x7fffffff;
    }

    public static int hash(int d0, int d1, int d2, int seed) {
        int d = seed > 0 ? seed : INITIAL_HASH_SEED;
        d = (d ^ d0) * HASH_MULTIPLIER;
        d = (d ^ d1) * HASH_MULTIPLIER;
        d = (d ^ d2) * HASH_MULTIPLIER;
        return d & 0x7fffffff;
    }

    public static int hash(int d0, int d1, int seed) {
        int d = seed > 0 ? seed : INITIAL_HASH_SEED;
        d = (d ^ d0) * HASH_MULTIPLIER;
        d = (d ^ d1) * HASH_MULTIPLIER;
        return d & 0x7fffffff;
    }

    public static int hash(String data, int seed) {
        int d = seed > 0 ? seed : INITIAL_HASH_SEED;
        for (int i = 0; i < data.length(); i++) {
            d = (d ^ data.charAt(i)) * HASH_MULTIPLIER;
        }
        return d & 0x7fffffff;
    }

    public static final int BIT_MASK_21 = (1 << 21) - 1;

    /**
     * This hash assumes that a trigram or bigram value is embedded into a 64 bit long value.
     * Structure for order=3: [1bit empty][gram-3][gram-2][gram-1]
     * Structure for order=2: [22bit empty][gram-2][gram-1]
     *
     * @param gramData gram data
     * @param order    order of grams. max-3
     * @param seed     hash seed
     * @return hash value.
     */
    public static int hash(long gramData, int order, int seed) {
        int d = seed > 0 ? seed : INITIAL_HASH_SEED;
        for (int i = 0; i < order; i++) {
            int h = (int) (gramData & BIT_MASK_21);
            gramData = gramData >>> 21;
            d = (d ^ h) * HASH_MULTIPLIER;
        }
        return d & 0x7fffffff;
    }

    public static int hash(int[] data, int begin, int end, int seed) {
        int d = seed > 0 ? seed : INITIAL_HASH_SEED;
        for (int i = begin; i < end; i++) {
            d = (d ^ data[i]) * HASH_MULTIPLIER;
        }
        return d & 0x7fffffff;
    }

    private static class BucketCalculator {

        IntHashKeyProvider keyProvider;
        int keyAmount;
        double averageKeysPerBucket = 3.0;
        private static final int HASH_SEED_LIMIT = 255;


        private BucketCalculator(IntHashKeyProvider keyProvider) {
            this.keyProvider = keyProvider;
        }

        public HashIndexes[] calculate() {
            keyAmount = keyProvider.keyAmount();

            int bucketAmount = (int) (keyAmount / averageKeysPerBucket) + 1;

            Bucket[] buckets = generateInitialBuckets(bucketAmount);

            // sort buckets larger to smaller.
            Arrays.sort(buckets);

            List<HashIndexes> result = new ArrayList<>();

            calculateIndexes(buckets, keyAmount, result);

            return result.toArray(new HashIndexes[result.size()]);
        }

        private Bucket[] generateInitialBuckets(int bucketAmount) {


            // Generating buckets
            Bucket[] buckets = new Bucket[bucketAmount];
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = new Bucket(i);
            }

            for (int i = 0; i < keyAmount; i++) {
                int[] key = keyProvider.getKey(i);
                int bucketIndex = hash(key, -1) % bucketAmount;
                buckets[bucketIndex].add(i);
            }
            return buckets;
        }

        private void calculateIndexes(Bucket[] buckets, int keyAmount, List<HashIndexes> indexes) {

            // generate a long bit vector with size of hash target size.
            LongBitVector bitVector = new LongBitVector(keyAmount, 100);
            bitVector.add(keyAmount, false);

            byte[] hashSeedArray = new byte[buckets.length];
            Arrays.fill(hashSeedArray, (byte) 0x01);


            // we need to collect failed buckets (A failed bucket such that we cannot find empty slots for all bucket keys
            // after 255 trials. )
            List<Bucket> failedBuckets = new ArrayList<>(buckets.length / 20);

            // for each bucket, find a hash function that will map each key in it to an empty slot in bitVector.
            int bucketIndex = 0;

            for (Bucket bucket : buckets) {
                if (bucket.keyIndexes.length == 0) // because buckets are sorted, we can finish here.
                    break;
                int l = 1;
                boolean loop = true;
                while (loop) {
                    int j = 0;
                    int[] slots = new int[bucket.keyIndexes.length];
                    for (int keyIndex : bucket.keyIndexes) {
                        int[] key = keyProvider.getKey(keyIndex);
                        slots[j] = hash(key, l) % keyAmount;
                        if (bitVector.get(slots[j]))
                            break;
                        else {
                            bitVector.set(slots[j]);
                            j++;
                        }
                    }
                    // if we fail to place all items in the bucket to the bitvector"s empty slots
                    if (j < bucket.keyIndexes.length) {
                        // we reset the occupied slots from bitvector.
                        for (int k = 0; k < j; k++) {
                            bitVector.clear(slots[k]);
                        }
                        // We reached the HASH_SEED_LIMIT.
                        // We place a 0 for its hash index value to know later that bucket is left to secondary lookup.
                        if (l == HASH_SEED_LIMIT) {
                            failedBuckets.add(bucket);
                            hashSeedArray[buckets[bucketIndex].id] = 0;
                            loop = false;
                        }

                    } else { // sweet. We have found empty slots in bit vector for all keys of the bucket.
                        hashSeedArray[buckets[bucketIndex].id] = (byte) (l & 0xff);
                        loop = false;
                    }
                    l++;
                }
                bucketIndex++;
            }

            if (failedBuckets.size() == 0) {
                // we are done.
                indexes.add(new HashIndexes(keyAmount, buckets.length, hashSeedArray, new int[0]));
                return;
            }

            // we assign lower average per key per bucket after each iteration to avoid generation failure.
            if (averageKeysPerBucket > 1)
                averageKeysPerBucket--;

            // start calculation for failed buckets.
            int failedKeyCount = 0;
            for (Bucket failedBucket : failedBuckets) {
                failedKeyCount += failedBucket.keyIndexes.length;
            }

            int failedBucketAmount = (int) (failedKeyCount / averageKeysPerBucket);
            if (Log.isDebug()) {
                Log.debug("Failed key Count:%d " + failedKeyCount);
            }

            // this is a worst case scenario. No empty slot find for any buckets and we are already using buckets where bucket Amount>=keyAmount
            // In this case we double the bucket size with the hope that it will have better bucket-key distribution.
            if (failedKeyCount == keyAmount && averageKeysPerBucket <= 1d) {
                averageKeysPerBucket = averageKeysPerBucket / 2;
                failedBucketAmount *= 2;
            }

            if (failedBucketAmount == 0)
                failedBucketAmount++;

            // this time we generate item keyAmount of Buckets
            Bucket[] nextLevelBuckets = new Bucket[failedBucketAmount];
            for (int i = 0; i < failedBucketAmount; i++) {
                nextLevelBuckets[i] = new Bucket(i);
            }

            // generate secondary buckets with item indexes.
            for (Bucket largeHashIndexBucket : failedBuckets) {
                for (int itemIndex : largeHashIndexBucket.keyIndexes) {
                    int[] key = keyProvider.getKey(itemIndex);
                    int secondaryBucketIndex = hash(key, -1) % failedBucketAmount;
                    nextLevelBuckets[secondaryBucketIndex].add(itemIndex);
                }
            }
            // sort buckets larger to smaller.
            Arrays.sort(nextLevelBuckets);

            int[] failedHashValues;
            int currentLevel = indexes.size();
            if (currentLevel == 0) {
                // if we are in the first level  generate a bit vector with the size of zero indexes of the primary bit vector.
                failedHashValues = bitVector.zeroIntIndexes();
            } else {
                failedHashValues = new int[failedKeyCount];
                int k = 0;
                for (int i = 0; i < bitVector.size(); i++) {
                    if (!bitVector.get(i))
                        failedHashValues[k++] = indexes.get(currentLevel - 1).failedIndexes[i];
                }
            }
            indexes.add(new HashIndexes(keyAmount, buckets.length, hashSeedArray, failedHashValues));

            // recurse for failed buckets.
            calculateIndexes(nextLevelBuckets, failedKeyCount, indexes);
        }
    }

    /**
     * @param key int array representation of the key.
     * @return minimal perfect hash value for the given input. returning number is between [0-keycount] keycount excluded.
     */
    public int get(int[] key) {
        return get(key, hash(key, -1));
    }

    /**
     * @param key         int array representation of the key.
     * @param initialHash sometimes initial hash value for MPHF calculation is already calculated.
     *                    So this value is used instead of re-calculation.
     * @return minimal perfect hash value for the given input. returning number is between [0-keycount] keycount excluded.
     */
    public int get(int[] key, int initialHash) {
        for (int i = 0; i < hashLevelData.length; i++) {
            final int seed = hashLevelData[i].getSeed(initialHash);
            if (seed != 0) {
                if (i == 0) {
                    return hash(key, seed) % hashLevelData[0].keyAmount;
                } else {
                    return hashLevelData[i - 1].failedIndexes[hash(key, seed) % hashLevelData[i].keyAmount];
                }
            }
        }
        throw new IllegalStateException("Cannot be here.");
    }

    public int get(int k0, int k1, int k2, int initialHash) {
        for (int i = 0; i < hashLevelData.length; i++) {
            final int seed = hashLevelData[i].getSeed(initialHash);
            if (seed != 0) {
                if (i == 0) {
                    return hash(k0, k1, k2, seed) % hashLevelData[0].keyAmount;
                } else {
                    return hashLevelData[i - 1].failedIndexes[hash(k0, k1, k2, seed) % hashLevelData[i].keyAmount];
                }
            }
        }
        throw new IllegalStateException("Cannot be here.");
    }

    public int get(int k0, int k1, int initialHash) {
        for (int i = 0; i < hashLevelData.length; i++) {
            final int seed = hashLevelData[i].getSeed(initialHash);
            if (seed != 0) {
                if (i == 0) {
                    return hash(k0, k1, seed) % hashLevelData[0].keyAmount;
                } else {
                    return hashLevelData[i - 1].failedIndexes[hash(k0, k1, seed) % hashLevelData[i].keyAmount];
                }
            }
        }
        throw new IllegalStateException("Cannot be here.");
    }

    /**
     * @param key byte array representation of the key.
     * @return minimal perfect hash value for the given input. returning number is between [0-keycount] keycount excluded.
     */
    public int get(byte[] key) {
        return get(key, hash(key, -1));
    }

    /**
     * @param key         byte array representation of the key.
     * @param initialHash sometimes initial hash value for MPHF calculation is already calculated.
     *                    So this value is used instead of re-calculation.
     * @return minimal perfect hash value for the given input. returning number is between [0-keycount] keycount excluded.
     */
    public int get(byte[] key, int initialHash) {
        for (int i = 0; i < hashLevelData.length; i++) {
            final int seed = hashLevelData[i].getSeed(initialHash);
            if (seed != 0) {
                if (i == 0) {
                    return hash(key, seed) % hashLevelData[0].keyAmount;
                } else {
                    return hashLevelData[i - 1].failedIndexes[hash(key, seed) % hashLevelData[i].keyAmount];
                }
            }
        }
        throw new IllegalStateException("Cannot be here.");
    }

    /**
     * @param key the key.
     * @return minimal perfect hash value for the given input. returning number is between [0-keycount) keycount excluded.
     */
    public int get(String key) {
        return get(key, hash(key, -1));
    }

    /**
     * @param key         the key.
     * @param initialHash sometimes initial hash value for MPHF calculation is already calculated. So this value is used instead of re-calculation.
     *                    This provides a small performance enhancement.
     * @return minimal perfect hash value for the given input. returning number is between [0-keycount] keycount excluded.
     */
    public int get(String key, int initialHash) {
        for (int i = 0; i < hashLevelData.length; i++) {
            final int seed = hashLevelData[i].getSeed(initialHash);
            if (seed != 0) {
                if (i == 0) {
                    return hash(key, seed) % hashLevelData[0].keyAmount;
                } else {
                    return hashLevelData[i - 1].failedIndexes[hash(key, seed) % hashLevelData[i].keyAmount];
                }
            }
        }
        throw new IllegalStateException("Cannot be here.");
    }

    /**
     * @param key         int array representation of the key.
     * @param initialHash sometimes initial hash value for MPHF calculation is already calculated. So this value is used instead of re-calculation.
     *                    This provides a small performance enhancement.
     * @return minimal perfect hash value for the given input. returning number is between [0-keycount] keycount excluded.
     */
    public int get(int[] key, int begin, int end, int initialHash) {
        for (int i = 0; i < hashLevelData.length; i++) {
            final int seed = hashLevelData[i].getSeed(initialHash);
            if (seed != 0) {
                if (i == 0) {
                    return hash(key, begin, end, seed) % hashLevelData[0].keyAmount;
                } else {
                    return hashLevelData[i - 1].failedIndexes[hash(key, begin, end, seed) % hashLevelData[i].keyAmount];
                }
            }
        }
        throw new IllegalStateException("Cannot be here.");
    }

    /**
     * @return total bytes used for this structure.
     * This is an average number and it adds 12 bytes per array as overhead
     */
    public long totalBytesUsed() {
        long result = 12; // array overhead
        for (HashIndexes data : hashLevelData) {
            result += 12; // array overhead for failed buckets
            result += data.bucketHashSeedValues.length;
            result += data.failedIndexes.length * 4;
        }
        return result;
    }

    public double averageBitsPerKey() {
        return ((double) totalBytesUsed() * 8) / hashLevelData[0].keyAmount;
    }

    /**
     * A custom serializer.
     *
     * @param file file to serialize data.
     * @throws IOException if an error occurs during file access.
     */
    public void serialize(File file) throws IOException {
        serialize(file.toPath());
    }

    public void serialize(Path path) throws IOException {
        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(path.toFile()), 1000000)) {
            serialize(os);
        }
    }

    /**
     * A custom serializer.
     * <p/>
     * <p/>int level count
     * <p/> ---- level 0
     * <p/>int key count
     * <p/>int bucket amount
     * <p/>byte[] seed values. Length = bucket count
     * <p/>int failed indexes length
     * <p/>int[] failed indexes
     * <p/> ---- level 1
     * <p/>int key count
     * <p/>int bucket amount
     * <p/>byte[] seed values. Length = bucket count
     * <p/>int failed indexes length
     * <p/>int[] failed indexes
     * <p/> ....
     * <p/> ---- level n
     * <p/>int key count
     * <p/>int bucket amount
     * <p/>byte[] seed values. Length = bucket count
     * <p/>int 0
     *
     * @param os stream to serialize data.
     * @throws IOException if an error occurs during file access.
     */
    public void serialize(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(hashLevelData.length);
        for (HashIndexes index : hashLevelData) {
            dos.writeInt(index.keyAmount);
            dos.writeInt(index.bucketAmount);
            dos.write(index.bucketHashSeedValues);
            dos.writeInt(index.failedIndexes.length);
            for (int i : index.failedIndexes) {
                dos.writeInt(i);
            }
        }
    }

    /**
     * A custom deserializer.
     *
     * @param file file that contains serialized data.
     * @param skip amount to skip
     * @return a new FastMinimalPerfectHash object.
     * @throws IOException if an error occurs during file access.
     */
    public static MultiLevelMphf deserialize(File file, long skip) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 100000))) {
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
    public static MultiLevelMphf deserialize(File file) throws IOException {
        return deserialize(file, 0);
    }

    /**
     * A custom deserializer. Look serialization method document for the format.
     *
     * @param dis DataInputStream that contains serialized data.
     * @return a new ChdPerfectHash object.
     * @throws IOException if an error occurs during stream access.
     */
    public static MultiLevelMphf deserialize(DataInputStream dis) throws IOException {
        int levelCount = dis.readInt();
        HashIndexes[] indexes = new HashIndexes[levelCount];
        for (int i = 0; i < levelCount; i++) {
            int keycount = dis.readInt();
            int bucketAmount = dis.readInt();
            byte[] hashSeedValues = new byte[bucketAmount];
            dis.readFully(hashSeedValues);
            int failedIndexesCount = dis.readInt();
            int[] failedIndexes = new int[failedIndexesCount];
            for (int j = 0; j < failedIndexesCount; j++) {
                failedIndexes[j] = dis.readInt();
            }
            indexes[i] = new HashIndexes(keycount, bucketAmount, hashSeedValues, failedIndexes);
        }
        return new MultiLevelMphf(indexes);
    }
}
