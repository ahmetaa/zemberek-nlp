package zemberek.normalization.deasciifier.latticedeasciifier;

/**
 * Common read-only feature-weight lookup shared by online training ({@link
 * AveragedPerceptronWeights}) and quantized runtime inference ({@link QuantizedWeightTable}).
 */
sealed interface WeightTable permits AveragedPerceptronWeights, QuantizedWeightTable {

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
