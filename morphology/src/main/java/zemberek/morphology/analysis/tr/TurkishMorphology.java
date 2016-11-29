package zemberek.morphology.analysis.tr;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.morphology.generator.SimpleGenerator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalyzer;
import zemberek.morphology.structure.StemAndEnding;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private StaticMorphCache staticCache;

    public static class TurkishMorphParserBuilder {
        WordAnalyzer _parser;
        SimpleGenerator _generator;
        SuffixProvider suffixProvider = new TurkishSuffixes();
        RootLexicon lexicon = new RootLexicon();

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
            _parser = new WordAnalyzer(graph);
            _generator = new SimpleGenerator(graph);
            Log.info("Parser ready: " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms.");
            return new TurkishMorphology(_parser, _generator, lexicon, graph, suffixProvider);
        }
    }

    private void generateCaches() {
        this.dynamicCache = CacheBuilder.newBuilder()
                .maximumSize(50000)
                .concurrencyLevel(1)
                .initialCapacity(20000)
                .build(new MorphParseCacheLoader());
        try {
            List<String> words = Resources.readLines(Resources.getResource("tr/top-20K-words.txt"), Charsets.UTF_8);
            staticCache = new StaticMorphCache(wordAnalyzer, words);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MorphParseCacheLoader extends CacheLoader<String, List<WordAnalysis>> {
        @Override
        public List<WordAnalysis> load(String word) throws Exception {
            String s = normalize(word); // TODO: this may cause problem for some foreign words.
            if (s.length() == 0)
                return Collections.emptyList();
            List<WordAnalysis> res = wordAnalyzer.analyze(s);
            if (res.size() == 0) {
                res.addAll(quoteParseCheck(s));
            }
            if (res.size() == 0) {
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

        public List<WordAnalysis> quoteParseCheck(String word) {
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
    }

    private TurkishMorphology(
            WordAnalyzer parser,
            SimpleGenerator generator,
            RootLexicon lexicon,
            DynamicLexiconGraph graph,
            SuffixProvider suffixProvider) {
        this.wordAnalyzer = parser;
        this.generator = generator;
        this.lexicon = lexicon;
        this.graph = graph;
        this.unidentifiedTokenAnalyzer = new UnidentifiedTokenAnalyzer(this);
        this.suffixProvider = suffixProvider;
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
        List<WordAnalysis> res = staticCache.get(word);
        if (res == null) {
            res = dynamicCache.getUnchecked(word);
        }
        return res;
    }

    public void invalidateAllCache() {
        dynamicCache.invalidateAll();
        staticCache.removeAll();
    }

    public void invalidateCache(String input) {
        dynamicCache.invalidate(input);
        staticCache.remove(input);
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
    public void addDictionaryItems(DictionaryItem... item) {
        this.graph.addDictionaryItems(item);
        invalidateAllCache();
    }


    public SuffixProvider getSuffixProvider() {
        return suffixProvider;
    }

    static class StaticMorphCache {
        private final HashMap<String, List<WordAnalysis>> cache;
        private long hit = 0;
        private long miss = 0;

        public StaticMorphCache(WordAnalyzer parser, List<String> wordList) throws IOException {
            cache = Maps.newHashMapWithExpectedSize(5000);
            for (String s : wordList) {
                cache.put(s, parser.analyze(s));
            }
        }

        public void put(String key, List<WordAnalysis> parses) {
            this.cache.put(key, parses);
        }

        public void remove(String key) {
            this.cache.remove(key);
        }

        public void removeAll() {
            cache.clear();
        }

        public List<WordAnalysis> get(String s) {
            List<WordAnalysis> result = cache.get(s);
            if (result != null) {
                hit++;
            } else {
                miss++;
            }
            return result;
        }

        @Override
        public String toString() {
            return "Hits: " + hit + " Miss: " + miss + " Hit ratio: %" + (hit / (double) (hit + miss) * 100);
        }
    }
}
