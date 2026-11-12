package zemberek.normalization.deasciifier.latticedeasciifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import zemberek.core.hash.IntHashKeyProvider;
import zemberek.core.hash.MultiLevelMphf;

/**
 * Compact immutable feature-weight table (~3.4 bytes/feature) backed by Zemberek's {@link
 * MultiLevelMphf}, 16-bit collision fingerprints, and an 8-bit Lloyd-Max float codebook.
 */
public final class QuantizedWeightTable implements WeightTable {

  private static final int MAGIC = 0x5A4C5754; // "ZLWT"
  private static final int VERSION = 1;

  private final MultiLevelMphf mphf;
  private final short[] fingerprints;
  private final byte[] codes;
  private final float[] codebook; // 256 centroids; index 0 is reserved for 0.0f

  private QuantizedWeightTable(
      MultiLevelMphf mphf, short[] fingerprints, byte[] codes, float[] codebook) {
    this.mphf = mphf;
    this.fingerprints = fingerprints;
    this.codes = codes;
    this.codebook = codebook;
  }

  public static QuantizedWeightTable empty() {
    return new QuantizedWeightTable(null, new short[0], new byte[0], new float[256]);
  }

  public static QuantizedWeightTable build(long[] keys, float[] values, int maxFeatures) {
    int n = keys.length;
    if (n == 0 || maxFeatures <= 0) {
      return empty();
    }

    int nonZero = 0;
    for (float v : values) {
      if (Math.abs(v) > 1e-6f) {
        nonZero++;
      }
    }
    if (nonZero == 0) {
      return empty();
    }

    float minAbsThreshold = 1e-6f;
    if (nonZero > maxFeatures) {
      float[] absVals = new float[nonZero];
      int p = 0;
      for (float v : values) {
        float a = Math.abs(v);
        if (a > 1e-6f) {
          absVals[p++] = a;
        }
      }
      Arrays.sort(absVals);
      minAbsThreshold = absVals[nonZero - maxFeatures];
    }

    long[] keptKeys = new long[Math.min(nonZero, maxFeatures)];
    float[] keptVals = new float[keptKeys.length];
    int count = 0;
    for (int i = 0; i < n && count < keptKeys.length; i++) {
      if (Math.abs(values[i]) > minAbsThreshold) {
        keptKeys[count] = keys[i];
        keptVals[count] = values[i];
        count++;
      }
    }
    if (count < keptKeys.length && nonZero > maxFeatures) {
      for (int i = 0; i < n && count < keptKeys.length; i++) {
        if (Math.abs(values[i]) == minAbsThreshold) {
          keptKeys[count] = keys[i];
          keptVals[count] = values[i];
          count++;
        }
      }
    }
    if (count == 0) {
      return empty();
    }
    if (count < keptKeys.length) {
      keptKeys = Arrays.copyOf(keptKeys, count);
      keptVals = Arrays.copyOf(keptVals, count);
    }

    final long[] finalKeys = keptKeys;
    IntHashKeyProvider provider =
        new IntHashKeyProvider() {
          @Override
          public int[] getKey(int index) {
            long k = finalKeys[index];
            return new int[] {(int) k, (int) (k >>> 32)};
          }

          @Override
          public int keyAmount() {
            return finalKeys.length;
          }
        };

    MultiLevelMphf mphf = MultiLevelMphf.generate(provider);
    float[] codebook = fitCodebook256(keptVals);
    short[] fingerprints = new short[count];
    byte[] codes = new byte[count];

    for (int i = 0; i < count; i++) {
      long k = finalKeys[i];
      int k0 = (int) k;
      int k1 = (int) (k >>> 32);
      int slot = mphf.get(k0, k1, MultiLevelMphf.hash(k0, k1, -1));
      fingerprints[slot] = FeatureHash.fingerprint16(k);
      codes[slot] = (byte) nearestCentroid(codebook, keptVals[i]);
    }
    return new QuantizedWeightTable(mphf, fingerprints, codes, codebook);
  }

  @Override
  public int size() {
    return codes.length;
  }

  public long ramBytesUsed() {
    long mphfBytes = mphf == null ? 0L : mphf.totalBytesUsed();
    return mphfBytes + fingerprints.length * 2L + codes.length + 256L * 4L + 64L;
  }

  @Override
  public float get(long featureKey) {
    if (mphf == null || codes.length == 0) {
      return 0.0f;
    }
    int k0 = (int) featureKey;
    int k1 = (int) (featureKey >>> 32);
    int idx = mphf.get(k0, k1, MultiLevelMphf.hash(k0, k1, -1));
    return fingerprints[idx] == FeatureHash.fingerprint16(featureKey)
        ? codebook[codes[idx] & 0xFF]
        : 0.0f;
  }

  @Override
  public float sum(long[] keys, int count) {
    if (mphf == null || codes.length == 0 || count <= 0) {
      return 0.0f;
    }
    float total = 0.0f;
    for (int i = 0; i < count; i++) {
      long key = keys[i];
      int k0 = (int) key;
      int k1 = (int) (key >>> 32);
      int idx = mphf.get(k0, k1, MultiLevelMphf.hash(k0, k1, -1));
      if (fingerprints[idx] == FeatureHash.fingerprint16(key)) {
        total += codebook[codes[idx] & 0xFF];
      }
    }
    return total;
  }

  public void serialize(DataOutputStream out) throws IOException {
    out.writeInt(MAGIC);
    out.writeInt(VERSION);
    out.writeInt(codes.length);
    if (codes.length == 0) {
      return;
    }
    mphf.serialize(out);
    for (short fp : fingerprints) {
      out.writeShort(fp);
    }
    out.write(codes);
    for (int i = 0; i < 256; i++) {
      out.writeFloat(codebook[i]);
    }
  }

  public static QuantizedWeightTable deserialize(DataInputStream in) throws IOException {
    int magic = in.readInt();
    if (magic != MAGIC) {
      throw new IOException("Invalid QuantizedWeightTable magic: " + Integer.toHexString(magic));
    }
    int version = in.readInt();
    if (version != VERSION) {
      throw new IOException("Unsupported QuantizedWeightTable version: " + version);
    }
    int count = in.readInt();
    if (count == 0) {
      return empty();
    }
    MultiLevelMphf mphf = MultiLevelMphf.deserialize(in);
    short[] fingerprints = new short[count];
    for (int i = 0; i < count; i++) {
      fingerprints[i] = in.readShort();
    }
    byte[] codes = new byte[count];
    in.readFully(codes);
    float[] codebook = new float[256];
    for (int i = 0; i < 256; i++) {
      codebook[i] = in.readFloat();
    }
    return new QuantizedWeightTable(mphf, fingerprints, codes, codebook);
  }

  private static float[] fitCodebook256(float[] values) {
    float[] sorted = values.clone();
    Arrays.sort(sorted);
    float[] codebook = new float[256];
    codebook[0] = 0.0f;

    int n = sorted.length;
    for (int c = 1; c < 256; c++) {
      int pos = (int) (((c - 0.5) / 255.0) * (n - 1));
      codebook[c] = sorted[pos];
    }

    int stride = Math.max(1, n / 150_000);
    double[] sums = new double[256];
    int[] counts = new int[256];
    for (int iter = 0; iter < 12; iter++) {
      Arrays.fill(sums, 0.0);
      Arrays.fill(counts, 0);
      for (int i = 0; i < n; i += stride) {
        int c = nearestCentroid(codebook, sorted[i]);
        sums[c] += sorted[i];
        counts[c]++;
      }
      for (int c = 1; c < 256; c++) {
        if (counts[c] > 0) {
          codebook[c] = (float) (sums[c] / counts[c]);
        }
      }
      Arrays.sort(codebook, 1, 256);
    }
    return codebook;
  }

  private static int nearestCentroid(float[] codebook, float value) {
    int lo = 1;
    int hi = 255;
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (codebook[mid] < value) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    return (lo > 1 && Math.abs(value - codebook[lo - 1]) < Math.abs(value - codebook[lo]))
        ? lo - 1
        : lo;
  }
}
