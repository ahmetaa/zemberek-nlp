package zemberek.normalization;

import zemberek.core.collections.FloatValueMap;

/**
 * Created by mayata on 13/08/17.
 * Decode interface for various normalizations.
 */
interface NormalizationDecoder<T> {
    FloatValueMap<T> decode(String input);
}
