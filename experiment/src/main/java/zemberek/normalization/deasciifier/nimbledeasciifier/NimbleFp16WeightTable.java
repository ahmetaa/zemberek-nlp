package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import zemberek.core.hash.IntHashKeyProvider;
import zemberek.core.hash.MultiLevelMphf;

/**
 * Compact immutable feature-weight table (~4.38 bytes/feature) backed by Zemberek's {@link
 * MultiLevelMphf}, 16-bit collision fingerprints, and IEEE-754 binary16 (FP16) weights.
 *
 * <p>Avoids 8-bit quantile codebook collapse on heavy-tailed Perceptron weights across {@code
 * +-65,000}.
 */
public final class NimbleFp16WeightTable implements NimbleWeightTable {

  private static final int MAGIC = 0x5A4E4632; // "ZNF2"
  private static final int VERSION = 1;

  private final MultiLevelMphf mphf;
  private final short[] fingerprints;
  private final short[] fp16Weights;

  private NimbleFp16WeightTable(
      MultiLevelMphf mphf, short[] fingerprints, short[] fp16Weights) {
    this.mphf = mphf;
    this.fingerprints = fingerprints;
    this.fp16Weights = fp16Weights;
  }

  public static NimbleFp16WeightTable empty() {
    return new NimbleFp16WeightTable(null, new short[0], new short[0]);
  }

  public static NimbleFp16WeightTable build(
      long[] keys, float[] values, short[] updateCounts, int maxFeatures, int minUpdates) {
    int n = keys.length;
    if (n == 0 || maxFeatures <= 0) {
      return empty();
    }

    int nonZero = 0;
    for (int i = 0; i < n; i++) {
      float v = values[i];
      if (Math.abs(v) > 1e-5f && Float.isFinite(v)) {
        nonZero++;
      }
    }
    if (nonZero == 0) {
      return empty();
    }

    int targetCount = Math.min(nonZero, maxFeatures);
    long[] keptKeys = new long[targetCount];
    float[] keptVals = new float[targetCount];
    int count = 0;

    if (nonZero <= maxFeatures) {
      for (int i = 0; i < n; i++) {
        float v = values[i];
        if (Math.abs(v) > 1e-5f && Float.isFinite(v)) {
          keptKeys[count] = keys[i];
          keptVals[count] = v;
          count++;
        }
      }
    } else {
      int preferredCount = 0;
      for (int i = 0; i < n; i++) {
        float v = values[i];
        if (Math.abs(v) > 1e-5f
            && Float.isFinite(v)
            && (updateCounts == null || updateCounts[i] >= minUpdates)) {
          preferredCount++;
        }
      }

      if (preferredCount >= maxFeatures) {
        float[] saliences = new float[preferredCount];
        int p = 0;
        for (int i = 0; i < n; i++) {
          float v = values[i];
          if (Math.abs(v) > 1e-5f
              && Float.isFinite(v)
              && (updateCounts == null || updateCounts[i] >= minUpdates)) {
            int uc = updateCounts != null ? Math.max(1, updateCounts[i]) : 1;
            saliences[p++] = Math.abs(v) * Math.min(4.0f, (float) Math.sqrt(uc));
          }
        }
        Arrays.sort(saliences);
        float threshold = saliences[preferredCount - maxFeatures];

        for (int i = 0; i < n && count < targetCount; i++) {
          float v = values[i];
          if (Math.abs(v) > 1e-5f
              && Float.isFinite(v)
              && (updateCounts == null || updateCounts[i] >= minUpdates)) {
            int uc = updateCounts != null ? Math.max(1, updateCounts[i]) : 1;
            float sal = Math.abs(v) * Math.min(4.0f, (float) Math.sqrt(uc));
            if (sal > threshold) {
              keptKeys[count] = keys[i];
              keptVals[count] = v;
              count++;
            }
          }
        }
        for (int i = 0; i < n && count < targetCount; i++) {
          float v = values[i];
          if (Math.abs(v) > 1e-5f
              && Float.isFinite(v)
              && (updateCounts == null || updateCounts[i] >= minUpdates)) {
            int uc = updateCounts != null ? Math.max(1, updateCounts[i]) : 1;
            float sal = Math.abs(v) * Math.min(4.0f, (float) Math.sqrt(uc));
            if (sal == threshold) {
              keptKeys[count] = keys[i];
              keptVals[count] = v;
              count++;
            }
          }
        }
      } else {
        int remBudget = maxFeatures - preferredCount;
        int otherCount = nonZero - preferredCount;
        float[] otherAbs = new float[otherCount];
        int op = 0;
        for (int i = 0; i < n; i++) {
          float v = values[i];
          if (Math.abs(v) > 1e-5f && Float.isFinite(v)) {
            if (updateCounts == null || updateCounts[i] >= minUpdates) {
              keptKeys[count] = keys[i];
              keptVals[count] = v;
              count++;
            } else {
              otherAbs[op++] = Math.abs(v);
            }
          }
        }
        if (remBudget > 0 && otherCount > 0) {
          float otherThresh = 0.0f;
          if (otherCount > remBudget) {
            Arrays.sort(otherAbs);
            otherThresh = otherAbs[otherCount - remBudget];
          }
          for (int i = 0; i < n && count < targetCount; i++) {
            float v = values[i];
            if (Math.abs(v) > 1e-5f
                && Float.isFinite(v)
                && updateCounts != null
                && updateCounts[i] < minUpdates
                && Math.abs(v) > otherThresh) {
              keptKeys[count] = keys[i];
              keptVals[count] = v;
              count++;
            }
          }
          for (int i = 0; i < n && count < targetCount; i++) {
            float v = values[i];
            if (Math.abs(v) > 1e-5f
                && Float.isFinite(v)
                && updateCounts != null
                && updateCounts[i] < minUpdates
                && Math.abs(v) == otherThresh) {
              keptKeys[count] = keys[i];
              keptVals[count] = v;
              count++;
            }
          }
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
    short[] fingerprints = new short[count];
    short[] fp16Weights = new short[count];

    for (int i = 0; i < count; i++) {
      long k = finalKeys[i];
      int slot = slotOf(mphf, k);
      fingerprints[slot] = fingerprint16(k);
      fp16Weights[slot] = Float.floatToFloat16(Math.clamp(keptVals[i], -65000.0f, 65000.0f));
    }
    return new NimbleFp16WeightTable(mphf, fingerprints, fp16Weights);
  }

  private static int slotOf(MultiLevelMphf mphf, long featureKey) {
    int k0 = (int) featureKey;
    int k1 = (int) (featureKey >>> 32);
    return mphf.get(k0, k1, MultiLevelMphf.hash(k0, k1, -1));
  }

  private static short fingerprint16(long featureKey) {
    short fp = (short) ((featureKey >>> 48) ^ (featureKey >>> 32) ^ (featureKey >>> 16));
    return fp == 0 ? (short) 1 : fp;
  }

  @Override
  public int size() {
    return fp16Weights.length;
  }

  public long ramBytesUsed() {
    long mphfBytes = mphf == null ? 0L : mphf.totalBytesUsed();
    return mphfBytes + fingerprints.length * 4L + 48L;
  }

  @Override
  public float get(long featureKey) {
    if (mphf == null || fp16Weights.length == 0) {
      return 0.0f;
    }
    int idx = slotOf(mphf, featureKey);
    return fingerprints[idx] == fingerprint16(featureKey)
        ? Float.float16ToFloat(fp16Weights[idx])
        : 0.0f;
  }

  @Override
  public float sum(long[] keys, int count) {
    if (mphf == null || fp16Weights.length == 0) {
      return 0.0f;
    }
    float total = 0.0f;
    for (int i = 0; i < count; i++) {
      long fk = keys[i];
      int idx = slotOf(mphf, fk);
      if (fingerprints[idx] == fingerprint16(fk)) {
        total += Float.float16ToFloat(fp16Weights[idx]);
      }
    }
    return total;
  }

  public void serialize(DataOutputStream out) throws IOException {
    out.writeInt(MAGIC);
    out.writeInt(VERSION);
    out.writeInt(fp16Weights.length);
    if (fp16Weights.length == 0) {
      return;
    }
    mphf.serialize(out);
    for (short fp : fingerprints) {
      out.writeShort(fp);
    }
    for (short w : fp16Weights) {
      out.writeShort(w);
    }
  }

  public static NimbleFp16WeightTable deserialize(DataInputStream in) throws IOException {
    int magic = in.readInt();
    if (magic != MAGIC) {
      throw new IOException("Invalid NimbleFp16WeightTable magic: " + Integer.toHexString(magic));
    }
    int version = in.readInt();
    if (version != VERSION) {
      throw new IOException("Unsupported NimbleFp16WeightTable version: " + version);
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
    short[] fp16Weights = new short[count];
    for (int i = 0; i < count; i++) {
      fp16Weights[i] = in.readShort();
    }
    return new NimbleFp16WeightTable(mphf, fingerprints, fp16Weights);
  }
}
