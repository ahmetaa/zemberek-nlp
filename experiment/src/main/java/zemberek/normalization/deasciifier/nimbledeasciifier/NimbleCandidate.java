package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.util.Set;

/**
 * Lightweight, morphology-free candidate representation with precomputed stem-prefix, suffix-case,
 * surface, and vowel-harmony features.
 */
final class NimbleCandidate {

  private static final Set<String> ASCII_STOPWORDS =
      Set.of(
          "ve", "bir", "bu", "da", "de", "ki", "mi", "mu", "ile", "icin", "gibi", "ama",
          "veya", "ne", "o", "su", "daha", "cok", "her", "hic", "tum", "olan", "olarak",
          "gore", "kadar", "sonra", "once", "degil", "var", "yok", "hem", "ya", "ise",
          "yani", "hatta", "fakat", "ancak", "lakin", "oysa", "sanki", "bile", "olanlar");

  static final NimbleCandidate BOUNDARY =
      new NimbleCandidate("<s>", "<s>", 0, 0.0f, 0.0f, false, false, false);

  final String surface;
  final long surfaceHash;
  final long stem3Hash;
  final long stem4Hash;
  final long stem5Hash;
  final long suf2Hash;
  final long suf3Hash;
  final int lastVowelHarmony;
  final int foreignStemLength;
  final float empiricalLogProb;
  final float charRelScore;
  final boolean properCandidate;
  final boolean oovFallback;
  final boolean contentWord;

  private NimbleCandidate(
      String surface,
      String asciiLower,
      int foreignStemLength,
      float empiricalLogProb,
      float charRelScore,
      boolean properCandidate,
      boolean oovFallback,
      boolean contentWord) {
    this.surface = surface;
    this.surfaceHash = NimbleHash.hashString(surface);
    this.stem3Hash = NimbleHash.hashPrefix(surface, 3);
    this.stem4Hash = NimbleHash.hashPrefix(surface, 4);
    this.stem5Hash = NimbleHash.hashPrefix(surface, 5);
    this.suf2Hash = NimbleHash.hashSuffix(surface, 2);
    this.suf3Hash = NimbleHash.hashSuffix(surface, 3);
    this.lastVowelHarmony = computeLastVowelHarmony(surface);
    this.foreignStemLength = foreignStemLength;
    this.empiricalLogProb = empiricalLogProb;
    this.charRelScore = charRelScore;
    this.properCandidate = properCandidate;
    this.oovFallback = oovFallback;
    this.contentWord = contentWord;
  }

  static NimbleCandidate create(
      String surface,
      String asciiLower,
      int foreignStemLength,
      float empiricalLogProb,
      float charRelScore,
      boolean properCandidate,
      boolean oovFallback) {
    boolean content = surface.length() >= 3 && !ASCII_STOPWORDS.contains(asciiLower);
    return new NimbleCandidate(
        surface,
        asciiLower,
        foreignStemLength,
        empiricalLogProb,
        charRelScore,
        properCandidate,
        oovFallback,
        content);
  }

  static boolean isStopword(String asciiLower) {
    return asciiLower.length() <= 2 || ASCII_STOPWORDS.contains(asciiLower);
  }

  static int computeLastVowelHarmony(String s) {
    for (int i = s.length() - 1; i >= 0; i--) {
      char c = TurkishTextCodec.toTurkishLower(s.charAt(i));
      switch (c) {
        case 'a', 'ı', 'â' -> {
          return 1;
        }
        case 'e', 'i', 'î' -> {
          return 2;
        }
        case 'o', 'u' -> {
          return 3;
        }
        case 'ö', 'ü', 'û' -> {
          return 4;
        }
        default -> {}
      }
    }
    return 0;
  }
}
