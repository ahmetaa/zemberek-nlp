package zemberek.morphology.analysis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import java.util.function.Function;
import zemberek.tokenization.Token;

/**
 * A thread-safe morphological analysis cache backed by Caffeine (W-TinyLFU).
 */
public class AnalysisCache {

  private static final int DEFAULT_INITIAL_CACHE_CAPACITY = 1024;
  private static final int DEFAULT_MAX_CACHE_CAPACITY = 32_768;
  private static final int CACHE_CAPACITY_LIMIT = 1_000_000;

  private final Cache<String, WordAnalysis> cache;
  private final boolean cacheDisabled;

  AnalysisCache(Builder builder) {
    this.cacheDisabled = builder._disableCache;
    this.cache = cacheDisabled ? null : Caffeine.newBuilder()
        .recordStats()
        .initialCapacity(builder._initialSize)
        .maximumSize(builder._maxSize)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    int _initialSize = DEFAULT_INITIAL_CACHE_CAPACITY;
    int _maxSize = DEFAULT_MAX_CACHE_CAPACITY;
    boolean _disableCache = false;

    /**
     * @deprecated Static cache has been unified into the single Caffeine cache.
     */
    @Deprecated
    public Builder staticCacheSize(int staticCacheSize) {
      Preconditions.checkArgument(staticCacheSize >= 0,
          "Static cache size cannot be negative. But it is %d", staticCacheSize);
      return this;
    }

    public Builder dynamicCacheSize(int initial, int max) {
      Preconditions.checkArgument(initial >= 0,
          "Cache initial size cannot be negative. But it is %d", initial);
      Preconditions.checkArgument(max >= 0,
          "Cache max size cannot be negative. But it is %d", max);
      Preconditions.checkArgument(max <= CACHE_CAPACITY_LIMIT,
          "Cache max size cannot be larger than %d. But it is %d",
          CACHE_CAPACITY_LIMIT, max);
      this._initialSize = initial;
      this._maxSize = max;
      return this;
    }

    /**
     * @deprecated Static cache has been unified into the single Caffeine cache.
     */
    @Deprecated
    public Builder disableStaticCache() {
      return this;
    }

    public Builder disableDynamicCache() {
      this._disableCache = true;
      return this;
    }

    public AnalysisCache build() {
      return new AnalysisCache(this);
    }
  }

  public void invalidateDynamicCache() {
    if (!cacheDisabled && cache != null) {
      cache.invalidateAll();
    }
  }

  /**
   * @deprecated Static cache has been unified into the single Caffeine cache.
   */
  @Deprecated
  public void initializeStaticCache(Function<String, WordAnalysis> analysisProvider) {
  }

  public WordAnalysis getAnalysis(String input, Function<String, WordAnalysis> analysisProvider) {
    if (cacheDisabled) {
      return analysisProvider.apply(input);
    }
    return cache.get(input, analysisProvider);
  }

  public WordAnalysis getAnalysis(Token input, Function<Token, WordAnalysis> analysisProvider) {
    if (cacheDisabled) {
      return analysisProvider.apply(input);
    }
    return cache.get(input.getText(), k -> analysisProvider.apply(input));
  }

  @Override
  public String toString() {
    if (cacheDisabled || cache == null) {
      return "Cache is disabled.";
    }
    return String.format(
        "Cache(size: %d) Hit rate: %.3f",
        cache.estimatedSize(), cache.stats().hitRate());
  }
}
