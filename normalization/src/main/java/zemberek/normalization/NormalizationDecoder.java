package zemberek.normalization;

import zemberek.core.collections.FloatValueMap;

/**
 * Decode interface for various normalizations.
 *
 * Known direct implementors {@link SingleWordSpellChecker#decode(String)}
 * Known direct implementors {@link CharacterGraphDecoder#decode(String)}
 */
interface NormalizationDecoder<T> {
    FloatValueMap<T> decode(String input);
}
