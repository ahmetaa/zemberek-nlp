package zemberek.morphology.generator.morphology;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.Strings;
import zemberek.morphology.generator.SimpleGenerator;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.Suffix;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleGeneratorTest {
    @Test
    public void regenerateTest() throws IOException {
        DynamicLexiconGraph graph = getLexicon();
        WordAnalyzer parser = new WordAnalyzer(graph);
        SimpleGenerator generator = new SimpleGenerator(graph);
        List<String> parseables = SimpleTextReader.trimmingUTF8Reader(new File(Resources.getResource("parseable.txt").getFile())).asStringList();
        for (String parseable : parseables) {
            System.out.println(parseable);
            List<WordAnalysis> parseResults = parser.analyze(parseable);
            for (WordAnalysis parseResult : parseResults) {
                System.out.println(parseResult);
                String[] res = generator.generate(parseResult.dictionaryItem, parseResult.getSuffixes());
                System.out.println(Arrays.toString(res));
                boolean found = false;
                for (String re : res) {
                    if (re.equals(parseable))
                        found = true;
                }
                Assert.assertTrue("Error in:" + parseable + " with parse:" + parseResult, found);
            }
        }
    }

    @Test
    public void regenerateTest2() throws IOException {
        DynamicLexiconGraph graph = getLexicon();
        WordAnalyzer parser = new WordAnalyzer(graph);
        SimpleGenerator generator = new SimpleGenerator(graph);
        String word = "elmada";
        List<WordAnalysis> parseResults = parser.analyze(word);
        for (WordAnalysis parseResult : parseResults) {
            System.out.println(parseResult);
            String[] res = generator.generate(parseResult.dictionaryItem, parseResult.getSuffixes());
            System.out.println(Arrays.toString(res));
        }
    }

    @Test
    public void regenerateTest3() throws IOException {
        DynamicLexiconGraph graph = getLexicon();
        WordAnalyzer parser = new WordAnalyzer(graph);
        SimpleGenerator generator = new SimpleGenerator(graph);
        String word = "elmada";
        List<WordAnalysis> parseResults = parser.analyze(word);
        for (WordAnalysis parseResult : parseResults) {
            List<Suffix> suffixes = parseResult.getSuffixes();
            suffixes.remove(suffixProvider.A3sg);
            suffixes.remove(suffixProvider.Pnon);
            System.out.println(parseResult);
            String[] res = generator.generate(parseResult.dictionaryItem, suffixes);
            System.out.println(Arrays.toString(res));
        }
    }

    @Test
    public void morphemeGenerationTest() throws IOException {
        DynamicLexiconGraph graph = getLexicon();
        WordAnalyzer parser = new WordAnalyzer(graph);
        SimpleGenerator generator = new SimpleGenerator(graph);
        List<String> testLines = SimpleTextReader.trimmingUTF8Reader(new File(Resources.getResource("separate-morphemes.txt").getFile())).asStringList();
        ArrayListMultimap<String, String> results = ArrayListMultimap.create(100, 2);
        for (String testLine : testLines) {
            for (String s : Splitter.on(",").trimResults().split(Strings.subStringAfterFirst(testLine, "=")))
                results.put(Strings.subStringUntilFirst(testLine, "=").trim(), s);
        }
        for (String parseable : results.keySet()) {
            List<WordAnalysis> parseResults = parser.analyze(parseable);
            for (WordAnalysis parseResult : parseResults) {
                String[] res = generator.generateMorphemes(parseResult.dictionaryItem, parseResult.getSuffixes());
                String s = Joiner.on("-").join(res);
                Assert.assertTrue("Error in:" + parseable, results.get(parseable).contains(s));
            }
        }
    }

    @Test
    @Ignore("Performance Test")
    public void speedTest() throws IOException {
        DynamicLexiconGraph graph = getLexicon();
        WordAnalyzer parser = new WordAnalyzer(graph);
        SimpleGenerator generator = new SimpleGenerator(graph);
        List<String> parseables = SimpleTextReader.trimmingUTF8Reader(new File(Resources.getResource("parseable.txt").getFile())).asStringList();
        List<WordAnalysis> parses = new ArrayList<WordAnalysis>();
        for (String word : parseables) {
            parses.addAll(parser.analyze(word));
        }
        long start = System.currentTimeMillis();
        final long iteration = 1000;
        for (int i = 0; i < iteration; i++) {
            for (WordAnalysis parseToken : parses) {
                String[] result = generator.generate(parseToken.dictionaryItem, parseToken.getSuffixes());
                if (i == 0) {
                    System.out.println(parseToken + " = " + Arrays.toString(result));
                }
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Elapsed:" + elapsed + " ms.");
        System.out.println("Speed:" + (iteration * 1000 * parses.size() / elapsed) + " words/second");
    }

    TurkishSuffixes suffixProvider = new TurkishSuffixes();

    private DynamicLexiconGraph getLexicon() throws IOException {
        RootLexicon items = new TurkishDictionaryLoader().load(new File(Resources.getResource("dev-lexicon.txt").getFile()));
        DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
        graph.addDictionaryItems(items);
        return graph;
    }
}
