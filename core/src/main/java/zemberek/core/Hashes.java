package zemberek.core;

public class Hashes {
    /**
     * Murmur hash 2.0.
     * <p/>
     * The murmur hash is a relative fast hash function from
     * http://murmurhash.googlepages.com/ for platforms with efficient
     * multiplication.
     * <p/>
     * This is a re-implementation of the original C code plus some
     * additional features.
     * <p/>
     * Public domain.
     *
     * @param data byte array to hash
     * @param seed initial seed value
     * @return 32 bit hash of the given array
     *         <p/>
     *         author Viliam Holub
     *         version 1.0.2
     *         <p/>
     *         Slightly modified and reduced (aaa)
     *         <p/>
     *         Generates 32 bit hash from byte array of the given length and
     *         seed.
     */
    public static int murmur32V2(final byte[] data, int seed) {
        // 'numberOfKeys' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int length = data.length;
        // Initialize the hash to a random value
        int h = seed ^ length;
        int length4 = length >>> 2;

        for (int i = 0; i < length4; i++) {
            final int i4 = i << 2;
            int k = (data[i4] & 0xff) + ((data[i4 + 1] & 0xff) << 8)
                    + ((data[i4 + 2] & 0xff) << 16) + ((data[i4 + 3] & 0xff) << 24);
            k *= m;
            k ^= k >>> 24;
            k *= m;
            h *= m;
            h ^= k;
        }

        // Handle the last few bytes of the input array
        switch (length & 0x3) {
            case 3:
                h ^= (data[(length & ~3) + 2] & 0xff) << 16;
            case 2:
                h ^= (data[(length & ~3) + 1] & 0xff) << 8;
            case 1:
                h ^= (data[length & ~3] & 0xff);
                h += m;
        }

        h ^= h >>> 13;
        h += m;
        h ^= h >>> 15;

        return h & 0x7fffffff; // we only want positize results to avoid Math.abs(NEG_MIN_VAL) issue
    }

    public static int murmurCustom(int[] data, int seed) {
        int h1 = seed;
        for (int a : data) {
            int k = a * 0xcc9e2d51;
            k = (k << 15 | k >>> 17);
            k *= 0x1b873593;
            h1 ^= k;
            h1 = (h1 << 13 | h1 >>> 19);
            h1 = h1 * 5 + 0xe6546b64;
        }
        return h1 & 0x7fffffff;
    }

    public static int jenkinsCustom(int[] data, int seed) {
        int h1 = seed;
        for (int a : data) {
            h1 += a;
            h1 += (h1 << 10);
            h1 ^= (h1 >> 6);
        }
        h1 += h1 << 3;
        h1 ^= h1 >> 11;
        h1 += h1 << 15;
        return h1 & 0x7fffffff;
    }

    public static int jenkinsCustom(int[] data, int begin, int end, int seed) {
        int h1 = seed;
        for (int i = begin; i < end; i++) {
            h1 += data[i];
            h1 += (h1 << 10);
            h1 ^= (h1 >> 6);
        }
        h1 += h1 << 3;
        h1 ^= h1 >> 11;
        h1 += h1 << 15;
        return h1 & 0x7fffffff;
    }

    public static int murmurCustom(int[] data, int begin, int end, int seed) {
        int h1 = seed;
        for (int i = begin; i < end; i++) {
            int k = data[i] * 0xcc9e2d51;
            k = (k << 15 | k >>> 17);
            k *= 0x1b873593;
            h1 ^= k;
            h1 = (h1 << 13 | h1 >>> 19);
            h1 = h1 * 5 + 0xe6546b64;
        }
        return h1 & 0x7fffffff;
    }

    public static int murmur32V3(byte[] data, int seed) {

        int h1 = seed;
        final int length = data.length;

        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;

        for (int i = 0; i < length >> 2; i++) {
            final int i4 = i << 2;
            int k = (data[i4] & 0xff) + ((data[i4 + 1] & 0xff) << 8)
                    + ((data[i4 + 2] & 0xff) << 16) + ((data[i4 + 3] & 0xff) << 24);
            k *= c1;
            k = (k << 15 | k >>> 17);
            k *= c2;
            h1 ^= k;
            h1 = (h1 << 13 | h1 >>> 19);
            h1 = h1 * 5 + 0xe6546b64;
        }

        int k = 0;
        // Handle the last few bytes of the input array
        switch (data.length & 0x3) {
            case 3:
                k ^= (data[(length & ~3) + 2] & 0xff) << 16;
            case 2:
                k ^= (data[(length & ~3) + 1] & 0xff) << 8;
            case 1:
                k ^= (data[length & ~3] & 0xff);
                k *= c1;
                k = (k << 15 | k >>> 17);
                k *= c2;
                h1 ^= k;
        }

        h1 ^= length * 4;

        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;
        return h1 & 0x7fffffff;
    }

}
