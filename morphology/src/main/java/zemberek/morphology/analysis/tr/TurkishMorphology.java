package zemberek.morphology.analysis.tr;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Turkish Morphological Parser finds all possible parses for a Turkish word.
 */
public class TurkishMorphology extends BaseParser {

    private WordAnalyzer wordAnalyzer;
    private SimpleGenerator generator;
    private RootLexicon lexicon;
    private DynamicLexiconGraph graph;
    private SuffixProvider suffixProvider;
    private UnidentifiedTokenAnalyzer unidentifiedTokenAnalyzer;

    private LoadingCache<String, List<WordAnalysis>> dynamicCache;

    private boolean useCache = true;
    private boolean useUnidentifiedTokenAnalyzer = true;

    public static class TurkishMorphParserBuilder {
        WordAnalyzer _analyzer;
        SimpleGenerator _generator;
        SuffixProvider suffixProvider = new TurkishSuffixes();
        RootLexicon lexicon = new RootLexicon();
        private boolean useCache = true;
        private boolean useUnidentifiedTokenAnalyzer = true;

        public TurkishMorphParserBuilder addDefaultDictionaries() throws IOException {
            return addTextDictionaryResources(TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.toArray(
                    new String[TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.size()]));
        }

        public TurkishMorphParserBuilder addTextDictionaries(File... dictionaryFiles) throws IOException {
            List<String> lines = new ArrayList<>();
            for (File file : dictionaryFiles) {
                lines.addAll(Files.readAllLines(file.toPath()));
            }
            lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
            return this;
        }

        public TurkishMorphParserBuilder addDictionaryLines(String... lines) throws IOException {
            lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
            return this;
        }

        public TurkishMorphParserBuilder removeDictFiles(File... dictionaryFiles) throws IOException {
            for (File file : dictionaryFiles) {
                lexicon.removeAll(new TurkishDictionaryLoader(suffixProvider).load(file));
            }
            return this;
        }

        public TurkishMorphParserBuilder doNotUseCache() {
            useCache = false;
            return this;
        }

        public TurkishMorphParserBuilder doNotUseUnidentifiedTokenAnalyzer() {
            useUnidentifiedTokenAnalyzer = false;
            return this;
        }

        public TurkishMorphParserBuilder addTextDictionaryResources(String... resources) throws IOException {
            Log.info("Loading dictionaries.");
            List<String> lines = new ArrayList<>();
            for (String resource : resources) {
                lines.addAll(Resources.readLines(Resources.getResource(resource), Charsets.UTF_8));
            }
            lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
            Log.info("Lexicon Generated.");
            return this;
        }

        public TurkishMorphParserBuilder removeItems(Iterable<String> dictionaryString) throws IOException {
            lexicon.removeAll(new TurkishDictionaryLoader(suffixProvider).load(dictionaryString));
            return this;
        }

        public TurkishMorphParserBuilder removeAllLemmas(Iterable<String> lemmas) throws IOException {
            lexicon.removeAllLemmas(lemmas);
            return this;
        }

        public TurkishMorphology build() throws IOException {
            Stopwatch sw = Stopwatch.createStarted();
            DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
            graph.addDictionaryItems(lexicon);
            _analyzer = new WordAnalyzer(graph);
            _generator = new SimpleGenerator(graph);
            Log.info("Parser ready: " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms.");
            return new TurkishMorphology(this, graph);
        }
    }

    private void generateCaches() {
        if (useCache) {
            this.dynamicCache = CacheBuilder.newBuilder()
                    .maximumSize(60000)
                    .concurrencyLevel(1)
                    .initialCapacity(30000)
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
            TurkishMorphParserBuilder builder, DynamicLexiconGraph graph) {
        this.wordAnalyzer = builder._analyzer;
        this.generator = builder._generator;
        this.lexicon = builder.lexicon;
        this.graph = graph;
        if (builder.useUnidentifiedTokenAnalyzer) {
            this.unidentifiedTokenAnalyzer = new UnidentifiedTokenAnalyzer(this);
        }
        this.suffixProvider = builder.suffixProvider;
        this.useCache = builder.useCache;
        this.useUnidentifiedTokenAnalyzer = builder.useUnidentifiedTokenAnalyzer;
        generateCaches();
    }

    public static TurkishMorphology createWithDefaults() throws IOException {
        return new TurkishMorphParserBuilder().addDefaultDictionaries().build();
    }

    public static TurkishMorphParserBuilder builder() {
        return new TurkishMorphParserBuilder();
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
    public List<WordAnalysis> analyzeWithoutCache(String word) {
        String s = normalize(word); // TODO: this may cause problem for some foreign words.
        if (s.length() == 0) {
            return Collections.emptyList();
        }
        List<WordAnalysis> res = wordAnalyzer.analyze(s);
        if (res.size() == 0) {
            res.addAll(analyzeWordsWithSingleQuote(s));
        }
        if (res.size() == 0 && useUnidentifiedTokenAnalyzer) {
            invalidateCache(s);
            res.addAll(unidentifiedTokenAnalyzer.parse(s));
        }
        if (res.size() == 0) {
            res.add(new WordAnalysis(
                    DictionaryItem.UNKNOWN,
                    s,
                    Lists.newArrayList(WordAnalysis.InflectionalGroup.UNKNOWN)));
        }
        return res;
    }

    private List<WordAnalysis> analyzeWordsWithSingleQuote(String word) {
        List<WordAnalysis> results = new ArrayList<>(2);

        if (word.contains("'")) {

            StemAndEnding se = new StemAndEnding(
                    Strings.subStringUntilFirst(word, "'"),
                    Strings.subStringAfterFirst(word, "'"));
            String stem = normalize(se.stem);

            String withoutQuote = word.replaceAll("'", "");

            List<WordAnalysis> noQuotesParses = wordAnalyzer.analyze(withoutQuote);
            results.addAll(noQuotesParses.stream()
                    .filter(noQuotesParse -> noQuotesParse.getStems().contains(stem))
                    .collect(Collectors.toList()));
        }
        return results;
    }


    public void invalidateAllCache() {
        if (useCache) {
            dynamicCache.invalidateAll();
        }
    }

    public void invalidateCache(String input) {
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
}
