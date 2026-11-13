package zemberek.normalization.deasciifier.latticedeasciifier;

/**
 * Deterministic 64-bit SplitMix64 feature hashing utilities.
 *
 * <p>Operates directly on primitives and {@link CharSequence} slices without allocating
 * intermediate strings.
 */
final class FeatureHash {

  private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

  private FeatureHash() {}

  static long mix64(long z) {
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }

  static long of(int namespace, long a) {
    return mix64(namespace * GOLDEN_GAMMA ^ mix64(a));
  }

  static long of(int namespace, long a, long b) {
    long h = mix64(namespace * GOLDEN_GAMMA ^ a);
    return mix64(h ^ (b * GOLDEN_GAMMA));
  }

  static long of(int namespace, long a, long b, long c) {
    long h = mix64(namespace * GOLDEN_GAMMA ^ a);
    h = mix64(h ^ (b * GOLDEN_GAMMA));
    return mix64(h ^ c);
  }

  static long hashString(CharSequence text) {
    return hashSlice(text, 0, text.length());
  }

  static long hashSlice(CharSequence text, int start, int end) {
    long h = 0xCBF29CE484222325L;
    for (int i = start; i < end; i++) {
      h ^= text.charAt(i);
      h *= 0x100000001B3L;
    }
    return mix64(h);
  }

  static long hashPrefix(CharSequence text, int maxChars) {
    return hashSlice(text, 0, Math.min(text.length(), maxChars));
  }

  static long hashSuffix(CharSequence text, int maxChars) {
    int len = text.length();
    return hashSlice(text, Math.max(0, len - maxChars), len);
  }

  static short fingerprint16(long featureKey) {
    short fp = (short) ((featureKey >>> 48) ^ (featureKey >>> 32) ^ (featureKey >>> 16));
    return fp == 0 ? (short) 1 : fp;
  }
}
