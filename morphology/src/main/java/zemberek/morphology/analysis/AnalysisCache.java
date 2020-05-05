package zemberek.morphology.analysis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.tokenization.Token;

/**
 * A simple analysis cache. Can be shared between threads.
 */
public class AnalysisCache {

  private static final int STATIC_CACHE_CAPACITY = 5000;
  private static final int DEFAULT_INITIAL_DYNAMIC_CACHE_CAPACITY = 3000;
  private static final int DEFAULT_MAX_DYNAMIC_CACHE_CAPACITY = 30_000;
  private static final int DYNAMIC_CACHE_CAPACITY_LIMIT = 1_000_000;

  private static final String MOST_USED_WORDS_FILE = "/tr/first-10K";
  private ConcurrentHashMap<String, WordAnalysis> staticCache;
  private boolean staticCacheInitialized = false;
  private long staticCacheHits;
  private long staticCacheMiss;
  private Cache<String, WordAnalysis> dynamicCache;
  private boolean staticCacheDisabled;
  private boolean dynamicCacheDisabled;

  AnalysisCache(Builder builder) {

    this.dynamicCacheDisabled = builder._disableDynamicCache;
    this.staticCacheDisabled = builder._disableStaticCache;

    dynamicCache = dynamicCacheDisabled ? null : Caffeine.newBuilder()
        .recordStats()
        .initialCapacity(builder._dynamicCacheInitialSize)
        .maximumSize(builder._dynamicCacheMaxSize)
        .build();
    staticCache = staticCacheDisabled ? null : new ConcurrentHashMap<>(STATIC_CACHE_CAPACITY);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    int _staticCacheSize = STATIC_CACHE_CAPACITY;
    int _dynamicCacheInitialSize = DEFAULT_INITIAL_DYNAMIC_CACHE_CAPACITY;
    int _dynamicCacheMaxSize = DEFAULT_MAX_DYNAMIC_CACHE_CAPACITY;
    boolean _disableStaticCache = false;
    boolean _disableDynamicCache = false;

    public Builder staticCacheSize(int staticCacheSize) {
      Preconditions.checkArgument(staticCacheSize >= 0,
          "Static cache size cannot be negative. But it is %d", staticCacheSize);
      this._staticCacheSize = staticCacheSize;
      return this;
    }

    public Builder dynamicCacheSize(int initial, int max) {
      Preconditions.checkArgument(initial >= 0,
          "Dynamic cache initial size cannot be negative. But it is %d", initial);
      Preconditions.checkArgument(max >= 0,
          "Dynamic cache initial size cannot be negative. But it is %d", max);
      Preconditions.checkArgument(max <= DYNAMIC_CACHE_CAPACITY_LIMIT,
          "Dynamic cache initial size cannot be larger than %d. But it is %d",
          DYNAMIC_CACHE_CAPACITY_LIMIT, max);
      this._dynamicCacheInitialSize = initial;
      this._dynamicCacheMaxSize = max;
      return this;
    }

    public Builder disableStaticCache() {
      this._disableStaticCache = true;
      return this;
    }

    public Builder disableDynamicCache() {
      this._disableDynamicCache = true;
      return this;
    }

    public AnalysisCache build() {
      return new AnalysisCache(this);
    }
  }

  public void invalidateDynamicCache() {
    if (!dynamicCacheDisabled && dynamicCache != null) {
      dynamicCache.invalidateAll();
    }
  }

  public synchronized void initializeStaticCache(Function<String, WordAnalysis> analysisProvider) {
    if (staticCacheDisabled || staticCacheInitialized) {
      return;
    }
    new Thread(() -> {
      try {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<String> words = TextIO.loadLinesFromResource(MOST_USED_WORDS_FILE);
        Log.debug("File read in %d ms.", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        int size = Math.min(STATIC_CACHE_CAPACITY, words.size());
        for (int i = 0; i < size; i++) {
          String word = words.get(i);
          staticCache.put(word, analysisProvider.apply(word));
        }
        Log.debug("Static cache initialized with %d most frequent words", size);
        Log.debug("Initialization time: %d ms.", stopwatch.elapsed(TimeUnit.MILLISECONDS));
      } catch (IOException e) {
        Log.error("Could not read most frequent words list, static cache is disabled.");
        e.printStackTrace();
      }
    }).start();
    staticCacheInitialized = true;
  }

  public WordAnalysis getAnalysis(String input, Function<String, WordAnalysis> analysisProvider) {

    WordAnalysis analysis = staticCacheDisabled ? null : staticCache.get(input);
    if (analysis != null) {
      staticCacheHits++;
      return analysis;
    }
    staticCacheMiss++;
    if (dynamicCacheDisabled) {
      return analysisProvider.apply(input);
    } else {
      return dynamicCache.get(input, analysisProvider);
    }
  }

  public WordAnalysis getAnalysis(Token input, Function<Token, WordAnalysis> analysisProvider) {
    WordAnalysis analysis = staticCacheDisabled ? null : staticCache.get(input.getText());
    if (analysis != null) {
      staticCacheHits++;
      return analysis;
    }
    staticCacheMiss++;
    if (dynamicCacheDisabled) {
      return analysisProvider.apply(input);
    } else {
      WordAnalysis a = dynamicCache.getIfPresent(input.getText());
      if (a == null) {
        a = analysisProvider.apply(input);
        dynamicCache.put(input.getText(), a);
      }
      return a;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    long total = staticCacheHits + staticCacheMiss;
    if (total > 0) {
      sb.append(String.format("Static cache(size: %d) Hit rate: %.3f%n",
          staticCache.size(), 1.0 * (staticCacheHits) / (staticCacheHits + staticCacheMiss)));
    }
    sb.append(String.format("Dynamic cache hit rate: %.3f ", dynamicCache.stats().hitRate()));
    return sb.toString();
  }
}
