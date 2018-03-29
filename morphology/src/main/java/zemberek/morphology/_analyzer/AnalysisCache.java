package zemberek.morphology._analyzer;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import zemberek.core.logging.Log;

/** A simple analysis cache. Can be shared between threads. */
class AnalysisCache {

  private static final int STATIC_CACHE_CAPACITY = 5000;
  private static final int DEFAULT_INITIAL_DYNAMIC_CACHE_CAPACITY = 1000;
  private static final int DEFAULT_MAX_DYNAMIC_CACHE_CAPACITY = 10000;

  static final String MOST_USED_WORDS_FILE = "src/main/resources/first-10k";

  final Analyzer analyzer;
  ConcurrentHashMap<String, _WordAnalysis> staticCache;
  long staticCacheHits;
  long staticCacheMiss;
  LoadingCache<String, _WordAnalysis> dynamicCache;

  // TODO(add a builder)
  public AnalysisCache(Analyzer analyzer) {
    this.analyzer = analyzer;
    dynamicCache = Caffeine.newBuilder()
        .recordStats()
        .initialCapacity(DEFAULT_INITIAL_DYNAMIC_CACHE_CAPACITY)
        .maximumSize(DEFAULT_MAX_DYNAMIC_CACHE_CAPACITY)
        .build(analyzer::analyze);
    staticCache = new ConcurrentHashMap<>(STATIC_CACHE_CAPACITY);
    initializeStaticCache();
  }

  private void initializeStaticCache() {
    new Thread(() -> {
      try {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<String> words = Files.readAllLines(Paths.get(MOST_USED_WORDS_FILE));
        // TODO(make configurable)
        int size = Math.min(STATIC_CACHE_CAPACITY, words.size());
        for (int i = 0; i < size; i++) {
          String word = words.get(i);
          staticCache.put(word, analyzer.analyze(word));
        }
        Log.info("Static cache initialized with %d most frequent words", size);
        Log.info("Initialization time: %d ms.", stopwatch.elapsed(TimeUnit.MILLISECONDS));
      } catch (IOException e) {
        Log.error("Could not read most frequent words list, static cache is disabled.");
        e.printStackTrace();
      }
    }).start();
  }

  public _WordAnalysis getAnalysis(String input) {
    _WordAnalysis analysis = staticCache.get(input);
    if (analysis != null) {
      staticCacheHits++;
      return  analysis;
    }
    staticCacheMiss++;
    return dynamicCache.get(input);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    long total = staticCacheHits + staticCacheMiss;
    if (total > 0) {
        sb.append(String.format("Static cache(size: %d) Hit rate: %f",
            staticCache.size(), 1.0 * (staticCacheHits)/(staticCacheHits + staticCacheMiss)));
    }
    sb.append("Dynamic cache hit rate: " + dynamicCache.stats().hitRate());
    return sb.toString();
  }
}
