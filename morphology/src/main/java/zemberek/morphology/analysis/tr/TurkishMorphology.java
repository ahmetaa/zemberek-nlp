package zemberek.morphology.analysis.tr;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalyzer;
import zemberek.morphology.generator.SimpleGenerator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.structure.StemAndEnding;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turkish Morphological Parser finds all possible parses for a Turkish word.
 */
public class TurkishMorphology {

    private WordAnalyzer wordAnalyzer;
    private SimpleGenerator generator;
    private RootLexicon lexicon;
    private DynamicLexiconGraph graph;
    private SuffixProvider suffixProvider;
    private UnidentifiedTokenAnalyzer unidentifiedTokenAnalyzer;

    private LoadingCache<String, List<WordAnalysis>> dynamicCache;

    private boolean useCache = true;
    private boolean useUnidentifiedTokenAnalyzer = true;

    private static final int DEFAULT_INITIAL_CACHE_SIZE = 50_000;
    private static final int DEFAULT_MAX_CACHE_SIZE = 100_000;

    public static class Builder {
        WordAnalyzer _analyzer;
        SimpleGenerator _generator;
        SuffixProvider suffixProvider = new TurkishSuffixes();
        RootLexicon lexicon = new RootLexicon();
        private boolean useDynamicCache = true;
        private boolean useUnidentifiedTokenAnalyzer = true;
        private int initialCacheSize = DEFAULT_INITIAL_CACHE_SIZE;
        private int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;

        public Builder addDefaultDictionaries() throws IOException {
            return addTextDictionaryResources(TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.toArray(
                    new String[TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.size()]));
        }

        public Builder addTextDictionaries(File... dictionaryFiles) throws IOException {
            List<String> lines = new ArrayList<>();
            for (File file : dictionaryFiles) {
                lines.addAll(Files.readAllLines(file.toPath()));
            }
            lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
            return this;
        }

        public Builder addTextDictionaries(Path... dictionaryPaths) throws IOException {
            for (Path dictionaryPath : dictionaryPaths) {
                addTextDictionaries(dictionaryPath.toFile());
            }
            return this;
        }

        public Builder addDictionaryLines(String... lines) throws IOException {
            lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
            return this;
        }

        public Builder removeDictionaryFiles(File... dictionaryFiles) throws IOException {
            for (File file : dictionaryFiles) {
                lexicon.removeAll(new TurkishDictionaryLoader(suffixProvider).load(file));
            }
            return this;
        }

        public Builder cacheParameters(int initialSize, int maxSize) {
            Preconditions.checkArgument(initialSize >= 0 && initialSize <= 1_000_000,
                    "Initial cache size must be between 0 and 1 million. But it is %d", initialSize);
            Preconditions.checkArgument(maxSize > initialSize,
                    "Max cache max size must be more than initial size %d. But it is %d", initialSize, maxSize);
            this.initialCacheSize = initialSize;
            this.maxCacheSize = maxSize;
            return this;
        }

        public Builder disableCache() {
            useDynamicCache = false;
            return this;
        }

        public Builder disableUnidentifiedTokenAnalyzer() {
            useUnidentifiedTokenAnalyzer = false;
            return this;
        }

        public Builder addTextDictionaryResources(String... resources) throws IOException {
            Log.info("Dictionaries :%s", String.join(", ", Arrays.asList(resources)));
            List<String> lines = new ArrayList<>();
            for (String resource : resources) {
                lines.addAll(Resources.readLines(Resources.getResource(resource), Charsets.UTF_8));
            }
            lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
            Log.info("Lexicon Generated.");
            return this;
        }

        public Builder removeItems(Iterable<String> dictionaryString) throws IOException {
            lexicon.removeAll(new TurkishDictionaryLoader(suffixProvider).load(dictionaryString));
            return this;
        }

        public Builder removeAllLemmas(Iterable<String> lemmas) throws IOException {
            lexicon.removeAllLemmas(lemmas);
            return this;
        }

        public TurkishMorphology build() throws IOException {
            DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
            graph.addDictionaryItems(lexicon);
            _analyzer = new WordAnalyzer(graph);
            _generator = new SimpleGenerator(graph);
            return new TurkishMorphology(this, graph);
        }
    }

    private void generateCache(int initialSize, int maxSize) {
        if (useCache) {
            this.dynamicCache = CacheBuilder.newBuilder()
                    .maximumSize(maxSize)
                    .concurrencyLevel(4)
                    .recordStats()
                    .initialCapacity(initialSize)
                    .build(new MorphParseCacheLoader());
        }
    }

    private class MorphParseCacheLoader extends CacheLoader<String, List<WordAnalysis>> {
        @Override
        public List<WordAnalysis> load(String word) throws Exception {
            return TurkishMorphology.this.analyzeWithoutCache(word);
        }
    }

    private TurkishMorphology(
            Builder builder, DynamicLexiconGraph graph) {
        this.wordAnalyzer = builder._analyzer;
        this.generator = builder._generator;
        this.lexicon = builder.lexicon;
        if (lexicon.size() == 0) {
            Log.warn("TurkishMorphology object is being instantiated without any dictionary items.");
        } else {
            Log.info("Total number dictionary items = %d", lexicon.size());
        }
        this.graph = graph;
        if (builder.useUnidentifiedTokenAnalyzer) {
            this.unidentifiedTokenAnalyzer = new UnidentifiedTokenAnalyzer(this);
        }
        this.suffixProvider = builder.suffixProvider;
        this.useCache = builder.useDynamicCache;
        this.useUnidentifiedTokenAnalyzer = builder.useUnidentifiedTokenAnalyzer;
        generateCache(builder.initialCacheSize, builder.maxCacheSize);
        Log.info("Initialization complete.");
    }

    public static TurkishMorphology createWithDefaults() throws IOException {
        Log.info("Started.");
        return new Builder().addDefaultDictionaries().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public WordAnalyzer getWordAnalyzer() {
        return wordAnalyzer;
    }

    /**
     * Normalizes the input word and analyses it. If word cannot be parsed following occurs:
     * - if input is a number, system tries to parse it by creating a number DictionaryEntry.
     * - if input starts with a capital letter, or contains ['] adds a Dictionary entry as a proper noun.
     * - if above options does not generate a result, it generates an UNKNOWN dictionary entry and returns a parse with it.
     *
     * @param word input word.
     * @return WordAnalysis list.
     */
    public List<WordAnalysis> analyze(String word) {
        stats.inputCount++;
        if (useCache) {
            return dynamicCache.getUnchecked(word);
        } else {
            return analyzeWithoutCache(word);
        }
    }

    /**
     * Normalizes the input word and analyses it. If word cannot be parsed following occurs:
     * - if input is a number, system tries to parse it by creating a number DictionaryEntry.
     * - if input starts with a capital letter, or contains ['] adds a Dictionary entry as a proper noun.
     * - if above options does not generate a result, it generates an UNKNOWN dictionary entry and returns a parse with it.
     *
     * @param word input word.
     * @return WordAnalysis list.
     */
    private List<WordAnalysis> analyzeWithoutCache(String word) {
        String s = TurkishAlphabet.INSTANCE.normalize(word); // TODO: this may cause problem for some foreign words.
        if (s.length() == 0) {
            return Collections.emptyList();
        }
        List<WordAnalysis> res = wordAnalyzer.analyze(s);
        stats.wordAnalyzerCallCount++;

        if (res.size() == 0) {
            res = analyzeWordsWithApostrophe(s);
            stats.analyzeWordsWithApostropheCallCount++;
        }

        if (res.size() == 0 && useUnidentifiedTokenAnalyzer) {
            res = unidentifiedTokenAnalyzer.analyze(s);
            stats.unidentifiedTokenAnalyzerCallCount++;
        }

        if (res.size() == 0) {
            stats.unknownTokenGenerationCount++;
            res = Collections.singletonList(new WordAnalysis(
                    DictionaryItem.UNKNOWN,
                    s,
                    Lists.newArrayList(WordAnalysis.InflectionalGroup.UNKNOWN)));
        }
        return res;
    }

    private List<WordAnalysis> analyzeWordsWithApostrophe(String word) {

        int index = word.indexOf('\'');

        if (index >= 0) {

            if (index == 0 || index == word.length() - 1) {
                return Collections.emptyList();
            }

            StemAndEnding se = new StemAndEnding(word.substring(0, index), word.substring(index + 1));
            String stem = TurkishAlphabet.INSTANCE.normalize(se.stem);

            String withoutQuote = word.replaceAll("'", "");

            List<WordAnalysis> noQuotesParses = wordAnalyzer.analyze(withoutQuote);
            if (noQuotesParses.size() == 0) {
                return Collections.emptyList();
            }

            return noQuotesParses.stream()
                    .filter(noQuotesParse -> noQuotesParse.getStems().contains(stem))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }

    }

    public void invalidateAllCache() {
        if (useCache) {
            dynamicCache.invalidateAll();
        }
    }

    public void invalidateCacheForWord(String input) {
        if (useCache) {
            dynamicCache.invalidate(input);
        }
    }

    public SimpleGenerator getGenerator() {
        return generator;
    }

    public RootLexicon getLexicon() {
        return lexicon;
    }

    public DynamicLexiconGraph getGraph() {
        return graph;
    }

    /**
     * Adds one or more dictionary items. Adding new dictionary items invalidates all caches.
     */
    public synchronized void addDictionaryItems(DictionaryItem... item) {
        this.graph.addDictionaryItems(item);
        invalidateAllCache();
    }

    public SuffixProvider getSuffixProvider() {
        return suffixProvider;
    }


    private Stats stats = new Stats();

    private static class Stats {
        long inputCount;
        long wordAnalyzerCallCount;
        long analyzeWordsWithApostropheCallCount;
        long unidentifiedTokenAnalyzerCallCount;
        long unknownTokenGenerationCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (useCache) {
            sb.append("Cache stats:\n");
            CacheStats stats = dynamicCache.stats();
            sb.append("Hit rate: " + stats.hitRate());
            sb.append(" Average load penalty: " + stats.averageLoadPenalty());
            sb.append(" Eviction count: " + stats.evictionCount());
        }

        sb.append("\nCount stats:\n");
        sb.append("Input count: " + stats.inputCount);
        sb.append(" Word Analyzer calls: " + stats.wordAnalyzerCallCount);
        sb.append(" Word With Apost. calls: " + stats.analyzeWordsWithApostropheCallCount);
        sb.append(" Unidentified Token calls: " + stats.unidentifiedTokenAnalyzerCallCount);
        sb.append(" Unknown token generation: " + stats.unknownTokenGenerationCount);
        return sb.toString();
    }

}
