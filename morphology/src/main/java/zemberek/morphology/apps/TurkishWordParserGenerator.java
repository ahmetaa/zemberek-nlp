package zemberek.morphology.apps;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import zemberek.core.logging.Log;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.generator.SimpleGenerator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.MorphParser;
import zemberek.morphology.parser.SimpleParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Turkish Morphological Parser finds all possible parses for a Turkish word.
 */
public class TurkishWordParserGenerator extends BaseParser {

    private MorphParser parser;
    private SimpleGenerator generator;
    private RootLexicon lexicon;
    private DynamicLexiconGraph graph;
    private UnidentifiedTokenParser unidentifiedTokenParser;

    private LoadingCache<String, List<MorphParse>> cache;
    private SimpleMorphCache morphCache;

    public static class TurkishMorphParserBuilder {
        MorphParser _parser;
        SimpleGenerator _generator;
        SuffixProvider suffixProvider = new TurkishSuffixes();
        RootLexicon lexicon = new RootLexicon();

        public TurkishMorphParserBuilder addDefaultDictionaries() throws IOException {
            return addTextDictResources(TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.toArray(
                    new String[TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.size()]));
        }

        public TurkishMorphParserBuilder addTextDictFiles(File... dictionaryFiles) throws IOException {
            List<String> lines = new ArrayList<>();
            for (File file : dictionaryFiles) {
                lines.addAll(Files.readAllLines(file.toPath()));
            }
            lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
            return this;
        }

        public TurkishMorphParserBuilder removeDictFiles(File... dictionaryFiles) throws IOException {
            for (File file : dictionaryFiles) {
                lexicon.removeAll(new TurkishDictionaryLoader(suffixProvider).load(file));
            }
            return this;
        }

        public TurkishMorphParserBuilder addTextDictResources(String... resources) throws IOException {
            List<String> lines = new ArrayList<>();
            for (String resource : resources) {
                lines.addAll(Resources.readLines(Resources.getResource(resource), Charsets.UTF_8));
            }
            lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
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

        public TurkishWordParserGenerator build() throws IOException {
            Stopwatch sw = Stopwatch.createStarted();
            DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
            graph.addDictionaryItems(lexicon);
            _parser = new SimpleParser(graph);
            _generator = new SimpleGenerator(graph);
            Log.info("Parser ready: " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms.");
            return new TurkishWordParserGenerator(_parser, _generator, lexicon, graph);
        }
    }

    private void generateCaches() {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(50000)
                .concurrencyLevel(1)
                .initialCapacity(20000)
                .build(new CacheLoader<String, List<MorphParse>>() {
                    @Override
                    public List<MorphParse> load(String s) {
                        if (s.length() == 0)
                            return Collections.emptyList();
                        List<MorphParse> res = parser.parse(s);
                        if (res.size() == 0 || (Character.isUpperCase(s.charAt(0)) && !containsProperNounParse(res))) {
                            res.addAll(unidentifiedTokenParser.parse(s));
                        }
                        if (res.size() == 0) {
                            res.add(new MorphParse(DictionaryItem.UNKNOWN, s, Lists.newArrayList(MorphParse.InflectionalGroup.UNKNOWN)));
                        }
                        return res;
                    }
                });
        try {
            List<String> words = Resources.readLines(Resources.getResource("tr/top-20K-words.txt"), Charsets.UTF_8);
            morphCache = new SimpleMorphCache(parser, words);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean containsProperNounParse(List<MorphParse> results) {
        for (MorphParse res : results) {
            if (res.dictionaryItem.secondaryPos == SecondaryPos.ProperNoun)
                return true;
        }
        return false;
    }

    private TurkishWordParserGenerator(
            MorphParser parser,
            SimpleGenerator generator,
            RootLexicon lexicon,
            DynamicLexiconGraph graph) {
        this.parser = parser;
        this.generator = generator;
        this.lexicon = lexicon;
        this.graph = graph;
        this.unidentifiedTokenParser = new UnidentifiedTokenParser(this);
        generateCaches();
    }

    public static TurkishWordParserGenerator createWithDefaults() throws IOException {
        return new TurkishMorphParserBuilder().addDefaultDictionaries().build();
    }

    public static TurkishMorphParserBuilder builder() {
        return new TurkishMorphParserBuilder();
    }

    public MorphParser getParser() {
        return parser;
    }

    /**
     * Normalizes the input word and parses it. If word cannot be parsed following occurs:
     * - if input is a number, system tries to parse it by creating a number DictionaryEntry.
     * - if input starts with a capital letter, or contains ['] adds a Dictionary entry as a proper noun.
     * - if above options does not generate a result, it generates an UNKNOWN dictionary entry and returns a parse with it.
     *
     * @param word input word.
     * @return MorphParse list.
     */
    public List<MorphParse> parse(String word) {
        String s = normalize(word); // TODO: may cause problem for some foreign words.
        List<MorphParse> res = morphCache.parse(s);
        if (res == null) {
            res = cache.getUnchecked(s);
        }
        return res;
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
}
