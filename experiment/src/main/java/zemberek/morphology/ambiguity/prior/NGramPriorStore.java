package zemberek.morphology.ambiguity.prior;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import zemberek.core.collections.IntValueMap;
import zemberek.core.logging.Log;

/**
 * In-memory index of large-corpus N-gram statistics and unambiguous collocations.
 * Uses log-frequency binning to provide robust, regularized feature priors for the
 * Perceptron ambiguity resolver without majority-sense collapse.
 */
public class NGramPriorStore {

  private static final Locale TR = Locale.forLanguageTag("tr");

  private final IntValueMap<String> bigrams;
  private final IntValueMap<String> trigrams;
  private final IntValueMap<String> collocations;

  public NGramPriorStore() {
    this.bigrams = new IntValueMap<>(100);
    this.trigrams = new IntValueMap<>(100);
    this.collocations = new IntValueMap<>(100);
  }

  public NGramPriorStore(
      IntValueMap<String> bigrams,
      IntValueMap<String> trigrams,
      IntValueMap<String> collocations) {
    this.bigrams = bigrams != null ? bigrams : new IntValueMap<>(16);
    this.trigrams = trigrams != null ? trigrams : new IntValueMap<>(16);
    this.collocations = collocations != null ? collocations : new IntValueMap<>(16);
  }

  /**
   * Converts a raw frequency count into a discrete log2 bin (1 to 10).
   * Uses floor(log2(1 + count)) clamped between 1 and 10.
   */
  public static int getLogBin(int count) {
    if (count <= 0) {
      return 0;
    }
    int bin = 31 - Integer.numberOfLeadingZeros(count + 1);
    return Math.min(10, Math.max(1, bin));
  }

  public int getBigramCount(String w1, String w2) {
    if (w1 == null || w2 == null) {
      return 0;
    }
    String key = (w1.toLowerCase(TR) + " " + w2.toLowerCase(TR)).trim();
    return bigrams.get(key);
  }

  public int getTrigramCount(String w1, String w2, String w3) {
    if (w1 == null || w2 == null || w3 == null) {
      return 0;
    }
    String key = (w1.toLowerCase(TR) + " " + w2.toLowerCase(TR) + " " + w3.toLowerCase(TR)).trim();
    return trigrams.get(key);
  }

  public int getBigramBin(String w1, String w2) {
    return getLogBin(getBigramCount(w1, w2));
  }

  public int getTrigramBin(String w1, String w2, String w3) {
    return getLogBin(getTrigramCount(w1, w2, w3));
  }

  public boolean hasCollocation(String w1, String w2) {
    if (w1 == null || w2 == null) {
      return false;
    }
    String key = (w1.toLowerCase(TR) + " " + w2.toLowerCase(TR)).trim();
    return collocations.contains(key);
  }

  public int bigramSize() {
    return bigrams.size();
  }

  public int trigramSize() {
    return trigrams.size();
  }

  public int collocationSize() {
    return collocations.size();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Path bigramsPath;
    private Path trigramsPath;
    private Path collocationsPath;
    private int minCount = 3;
    private int maxBigrams = 500000;
    private int maxTrigrams = 300000;

    public Builder bigramsPath(Path path) {
      this.bigramsPath = path;
      return this;
    }

    public Builder trigramsPath(Path path) {
      this.trigramsPath = path;
      return this;
    }

    public Builder collocationsPath(Path path) {
      this.collocationsPath = path;
      return this;
    }

    public Builder minCount(int minCount) {
      this.minCount = minCount;
      return this;
    }

    public Builder maxBigrams(int max) {
      this.maxBigrams = max;
      return this;
    }

    public Builder maxTrigrams(int max) {
      this.maxTrigrams = max;
      return this;
    }

    public NGramPriorStore build() throws IOException {
      IntValueMap<String> bigrams = new IntValueMap<>(Math.max(1000, maxBigrams));
      IntValueMap<String> trigrams = new IntValueMap<>(Math.max(1000, maxTrigrams));
      IntValueMap<String> collocations = new IntValueMap<>(100000);

      if (bigramsPath != null && Files.exists(bigramsPath)) {
        Log.info("Loading unambiguous bigrams from: %s", bigramsPath);
        loadNGramFile(bigramsPath, bigrams, minCount, maxBigrams);
        Log.info("Loaded %d bigrams.", bigrams.size());
      }

      if (trigramsPath != null && Files.exists(trigramsPath)) {
        Log.info("Loading unambiguous trigrams from: %s", trigramsPath);
        loadNGramFile(trigramsPath, trigrams, minCount, maxTrigrams);
        Log.info("Loaded %d trigrams.", trigrams.size());
      }

      if (collocationsPath != null && Files.exists(collocationsPath)) {
        Log.info("Loading significant collocations from: %s", collocationsPath);
        loadCollocationsFile(collocationsPath, collocations, minCount);
        Log.info("Loaded %d significant collocations.", collocations.size());
      }

      return new NGramPriorStore(bigrams, trigrams, collocations);
    }

    private void loadNGramFile(Path path, IntValueMap<String> map, int minCountThreshold, int maxEntries) throws IOException {
      try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null && count < maxEntries) {
          line = line.trim();
          if (line.isEmpty() || !line.contains(":")) {
            continue;
          }
          int colonIdx = line.lastIndexOf(':');
          String phrase = line.substring(0, colonIdx).trim().toLowerCase(TR);
          String countStr = line.substring(colonIdx + 1).trim();
          try {
            int freq = Integer.parseInt(countStr);
            if (freq >= minCountThreshold && !phrase.isEmpty()) {
              map.put(phrase, freq);
              count++;
            }
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }

    private void loadCollocationsFile(Path path, IntValueMap<String> map, int minCountThreshold) throws IOException {
      try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty() || !line.contains(":")) {
            continue;
          }
          int colonIdx = line.lastIndexOf(':');
          String phrase = line.substring(0, colonIdx).trim().toLowerCase(TR);
          String countStr = line.substring(colonIdx + 1).trim();
          // Filter out header lines containing parentheses
          if (phrase.startsWith("(") || phrase.startsWith("[") || phrase.contains("total")) {
            continue;
          }
          try {
            int freq = Integer.parseInt(countStr);
            if (freq >= minCountThreshold && phrase.contains(" ")) {
              map.put(phrase, freq);
            }
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }
  }
}
