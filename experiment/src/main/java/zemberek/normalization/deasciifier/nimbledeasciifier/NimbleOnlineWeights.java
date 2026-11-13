package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.util.Arrays;

/**
 * Open-addressed online Perceptron weight table with lazy cumulative parameter averaging and
 * update-count tracking for frequency-aware feature pruning.
 */
final class NimbleOnlineWeights implements NimbleWeightTable {

  private long[] keys;
  private float[] weights;
  private double[] cumulativeSums;
  private int[] lastUpdatedStep;
  private short[] updateCounts;
  private int size;
  private int mask;
  private int currentStep = 1;

  NimbleOnlineWeights(int initialCapacityPowerOfTwo) {
    int cap = Integer.highestOneBit(Math.max(1024, initialCapacityPowerOfTwo));
    if (cap < initialCapacityPowerOfTwo) {
      cap <<= 1;
    }
    this.keys = new long[cap];
    this.weights = new float[cap];
    this.cumulativeSums = new double[cap];
    this.lastUpdatedStep = new int[cap];
    this.updateCounts = new short[cap];
    this.mask = cap - 1;
  }

  void advanceStep() {
    currentStep++;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public float get(long featureKey) {
    long k = featureKey == 0L ? 1L : featureKey;
    int slot = ((int) k) & mask;
    while (true) {
      long existing = keys[slot];
      if (existing == k) {
        return weights[slot];
      }
      if (existing == 0L) {
        return 0.0f;
      }
      slot = (slot + 1) & mask;
    }
  }

  void addBatch(long[] featureKeys, int count, float delta) {
    for (int i = 0; i < count; i++) {
      add(featureKeys[i], delta);
    }
  }

  void add(long featureKey, float delta) {
    if (delta == 0.0f) {
      return;
    }
    if (size * 10 >= keys.length * 7) {
      rehash(keys.length * 2);
    }
    long k = featureKey == 0L ? 1L : featureKey;
    int slot = ((int) k) & mask;
    while (true) {
      long existing = keys[slot];
      if (existing == 0L) {
        keys[slot] = k;
        weights[slot] = delta;
        lastUpdatedStep[slot] = currentStep;
        updateCounts[slot] = 1;
        size++;
        return;
      }
      if (existing == k) {
        int elapsed = currentStep - lastUpdatedStep[slot];
        if (elapsed > 0) {
          cumulativeSums[slot] += (double) weights[slot] * elapsed;
          lastUpdatedStep[slot] = currentStep;
          if (updateCounts[slot] < Short.MAX_VALUE) {
            updateCounts[slot]++;
          }
        }
        weights[slot] += delta;
        return;
      }
      slot = (slot + 1) & mask;
    }
  }

  NimbleFp16WeightTable buildFp16Averaged(int maxFeatures, int minUpdates) {
    if (size == 0) {
      return NimbleFp16WeightTable.empty();
    }
    int finalStep = currentStep + 1;
    long[] activeKeys = new long[size];
    float[] averagedValues = new float[size];
    short[] activeCounts = new short[size];
    int p = 0;
    for (int i = 0; i < keys.length; i++) {
      if (keys[i] != 0L) {
        int elapsed = finalStep - lastUpdatedStep[i];
        double total = cumulativeSums[i] + (double) weights[i] * elapsed;
        activeKeys[p] = keys[i];
        averagedValues[p] = (float) (total / finalStep);
        activeCounts[p] = updateCounts[i];
        p++;
      }
    }
    if (p < activeKeys.length) {
      activeKeys = Arrays.copyOf(activeKeys, p);
      averagedValues = Arrays.copyOf(averagedValues, p);
      activeCounts = Arrays.copyOf(activeCounts, p);
    }
    return NimbleFp16WeightTable.build(
        activeKeys, averagedValues, activeCounts, maxFeatures, minUpdates);
  }

  private void rehash(int newCapacity) {
    long[] oldKeys = this.keys;
    float[] oldWeights = this.weights;
    double[] oldSums = this.cumulativeSums;
    int[] oldSteps = this.lastUpdatedStep;
    short[] oldCounts = this.updateCounts;

    this.keys = new long[newCapacity];
    this.weights = new float[newCapacity];
    this.cumulativeSums = new double[newCapacity];
    this.lastUpdatedStep = new int[newCapacity];
    this.updateCounts = new short[newCapacity];
    this.mask = newCapacity - 1;

    for (int i = 0; i < oldKeys.length; i++) {
      long k = oldKeys[i];
      if (k != 0L) {
        int slot = ((int) k) & mask;
        while (keys[slot] != 0L) {
          slot = (slot + 1) & mask;
        }
        keys[slot] = k;
        weights[slot] = oldWeights[i];
        cumulativeSums[slot] = oldSums[i];
        lastUpdatedStep[slot] = oldSteps[i];
        updateCounts[slot] = oldCounts[i];
      }
    }
  }
}
