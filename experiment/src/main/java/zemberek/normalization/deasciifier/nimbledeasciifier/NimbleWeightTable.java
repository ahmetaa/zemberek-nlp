package zemberek.normalization.deasciifier.nimbledeasciifier;

/**
 * Common read-only feature-weight lookup shared by online Perceptron training ({@link
 * NimbleOnlineWeights}) and compact FP16 MPHF inference ({@link NimbleFp16WeightTable}).
 */
sealed interface NimbleWeightTable permits NimbleOnlineWeights, NimbleFp16WeightTable {

  float get(long featureKey);

  int size();

  default float sum(long[] keys, int count) {
    float total = 0.0f;
    for (int i = 0; i < count; i++) {
      total += get(keys[i]);
    }
    return total;
  }
}
