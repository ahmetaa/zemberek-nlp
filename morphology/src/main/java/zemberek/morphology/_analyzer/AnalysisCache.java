package zemberek.morphology._analyzer;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.concurrent.ConcurrentHashMap;

class AnalysisCache {

  private static final int DEFAULT_INITIAL_STATIC_CACHE_CAPACITY = 1000;
  private static final int DEFAULT_INITIAL_DYNAMIC_CACHE_CAPACITY = 1000;
  private static final int DEFAULT_MAX_DYNAMIC_CACHE_CAPACITY = 10000;

  final Analyzer analyzer;
  ConcurrentHashMap<String, _WordAnalysis> staticCache;
  LoadingCache<String, _WordAnalysis> dynamicCache;

  // TODO(add a builder)
  AnalysisCache(Analyzer analyzer, String mostUsedWordsFile) {
    this.analyzer = analyzer;
    dynamicCache = Caffeine.newBuilder()
        .initialCapacity(DEFAULT_INITIAL_DYNAMIC_CACHE_CAPACITY)
        .maximumSize(DEFAULT_MAX_DYNAMIC_CACHE_CAPACITY)
        .build(analyzer::analyze);
    staticCache = new ConcurrentHashMap<>(DEFAULT_INITIAL_STATIC_CACHE_CAPACITY);
    initializeStaticCache();
  }

  private void initializeStaticCache() {
    new Thread(() -> {
      // Read first n words from file and initiate static cache
    }).start();
  }

  public _WordAnalysis getAnalysis(String input) {
    _WordAnalysis analysis = staticCache.get(input);
    if (analysis != null) {
      return  analysis;
    }
    return dynamicCache.get(input);
  }

}
