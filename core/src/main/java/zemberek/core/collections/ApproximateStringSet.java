package zemberek.core.collections;

import zemberek.core.hash.IntHashKeyProvider;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.core.hash.StringHashKeyProvider;
import zemberek.core.io.IOUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * An approximate read-only String Set. This data structure does not guarantee if a String exists in it. But
 * it is very unlikely that an item seems to exist but it actually does not.
 * Probability of such a collision is related with a 31 bit hash function and size of the set N.
 * Lower bound of this value is 1/((2^31-1)
 * This structure may be suitable for statistical algorithms.
 * It uses around 36 bits per key regardless of the key size.
 */
public class ApproximateStringSet {

    private MultiLevelMphf mphf;
    private int[] fingerPrints;

    private ApproximateStringSet(MultiLevelMphf mphf, int[] fingerPrints) {
        this.mphf = mphf;
        this.fingerPrints = fingerPrints;
    }

    public boolean contains(String s) {
        int fingerPrint = MultiLevelMphf.hash(s, MultiLevelMphf.INITIAL_HASH_SEED);
        int slot = mphf.get(s, fingerPrint);
        return fingerPrints[slot] == fingerPrint;
    }

    public static ApproximateStringSet generate(Set<String> set) {
        IntHashKeyProvider keyProvider = new StringHashKeyProvider(set);
        MultiLevelMphf mphf = MultiLevelMphf.generate(keyProvider);
        int[] fingerPrints = new int[mphf.size()];
        for (String s : set) {
            int slot = mphf.get(s);
            int fingerPrint = MultiLevelMphf.hash(s, MultiLevelMphf.INITIAL_HASH_SEED);
            fingerPrints[slot] = fingerPrint;
        }
        return new ApproximateStringSet(mphf, fingerPrints);
    }

    public void serialize(Path path) throws IOException {
        try (DataOutputStream dos = IOUtil.getDataOutputStream(path)) {
            mphf.serialize(dos);
            for (int fingerPrint : fingerPrints) {
                dos.writeInt(fingerPrint);
            }
        }
    }

    public static ApproximateStringSet deserialize(Path path) throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
            MultiLevelMphf mphf = MultiLevelMphf.deserialize(dis);
            int[] fingerPrints = new int[mphf.size()];
            for (int i = 0; i < fingerPrints.length; i++) {
                fingerPrints[i] = dis.readInt();
            }
            return new ApproximateStringSet(mphf, fingerPrints);
        }
    }
}
