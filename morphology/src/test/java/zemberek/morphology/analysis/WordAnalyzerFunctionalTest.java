package zemberek.morphology.analysis;

import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.io.SimpleTextReader;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WordAnalyzerFunctionalTest {

    public static final File PARSEABLES_FILE = new File(Resources.getResource("parseable.txt").getFile());
    public static final File DEV_LEXICON_FILE = new File(Resources.getResource("dev-lexicon.txt").getFile());
//    public static final File UNPARSEABLES_FILE = new File(Resources.getResource("unparseable.txt").getFile());
    public static final File MASTER_DICTIONARY_FILE = new File(Resources.getResource("tr/master-dictionary.dict").getFile());
    public static final File SECONDARY_DICTIONARY_FILE = new File(Resources.getResource("tr/secondary-dictionary.dict").getFile());
//  public static final File OFLAZER_MISMATCH_FILE = new File(Resources.getResource("misc/oflazer-mismatch.txt").getFile());
//  public static final File Z2_VOCAB_FILE = new File(Resources.getResource("z2-vocab.tr.7z").getFile());
    public static final File NON_TDK_DICT_FILE = new File(Resources.getResource("tr/non-tdk.dict").getFile());

    public void parseableTest(WordAnalyzer parser) throws IOException {
        List<String> parseables = SimpleTextReader.trimmingUTF8Reader(PARSEABLES_FILE).asStringList();
        for (String parseable : parseables) {
            Assert.assertTrue("Could not parse valid word:" + parseable, parser.analyze(parseable).size() > 0);
        }
    }

    @Test
    public void simpleParse() throws IOException {
        DynamicLexiconGraph graph = getLexiconGraph(DEV_LEXICON_FILE);
        WordAnalyzer wordAnalyzer = new WordAnalyzer(graph);
        List<String> parseables = SimpleTextReader.trimmingUTF8Reader(PARSEABLES_FILE).asStringList();
        for (String parseable : parseables) {
            List<WordAnalysis> results = wordAnalyzer.analyze(parseable);
            if (results.size() > 0) {
                //System.out.print(parseable + " : ");
                for (WordAnalysis parseResult : results) {
                    System.out.println( parseable +" -> " + parseResult.formatOflazer() + "   ");
                }
                //System.out.println();

            } else {
                System.out.println("ERROR:" + parseable);
                wordAnalyzer.dump(parseable);
            }
            //Assert.assertTrue("Could not parses valid word:" + parseable, parser.parses(parseable).size() > 0);
        }
    }

    @Test
    public void simpleParserParseable() throws IOException {
        parseableTest(simpleParser(DEV_LEXICON_FILE));
    }

/*    @Test
    public void unparseableTest() throws IOException {
        WordParser parser = simpleParser(DEV_LEXICON_FILE);
        List<String> unparseables = SimpleTextReader.trimmingUTF8Reader(UNPARSEABLES_FILE).asStringList();
        for (String wrong : unparseables) {
            Assert.assertTrue("Parses invalid word:" + wrong, parser.parse(wrong).size() == 0);
        }
    }
*/

    @Test
    @Ignore("Performance Test")
    public void speedTest() throws IOException {
        WordAnalyzer parser = simpleParser(DEV_LEXICON_FILE);
        List<String> parseables = SimpleTextReader.trimmingUTF8Reader(PARSEABLES_FILE).asStringList();
        long start = System.currentTimeMillis();
        final long iteration = 1000;
        for (int i = 0; i < iteration; i++) {
            for (String s : parseables) {
                List<WordAnalysis> results = parser.analyze(s);
                for (WordAnalysis result : results) {
                    result.formatLong();
                }
                if (i == 0) {
                    for (WordAnalysis result : results) {
                        //System.out.println(s + " = " + result.formatLong());
                    }
                }
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Elapsed:" + elapsed + " ms.");
        System.out.println("Speed:" + (iteration * 1000 * parseables.size() / elapsed) + " words/second");
    }

/*    @Test
    @Ignore("Not a unit Test")
    public void z2Comparison() throws IOException {
        List<String> allWords = SimpleTextReader.trimmingUTF8Reader(
                Z2_VOCAB_FILE).asStringList();
        System.out.println("word list loaded");
        SimpleTextWriter stw = SimpleTextWriter.keepOpenUTF8Writer(new File(Resources.getResource("unknowns.txt").getFile()));
        System.out.println("Initial number of Suffix Form Sets: " + suffixes.getFormCount());
        WordParser parser = simpleParser(MASTER_DICTIONARY_FILE, SECONDARY_DICTIONARY_FILE);
        System.out.println("Total number of Suffix Form Sets After Adding Stems: " + suffixes.getFormCount());
        System.out.println("Total number of Suffix nodes: " + parser.graph.totalSuffixNodeCount());
        System.out.println("Total number of Stem nodes: " + parser.graph.totalStemNodeCount());
        System.out.println("Parsing started");
        long start = System.currentTimeMillis();
        int pass = 0;
        for (String word : allWords) {
            if (parser.parse(word).size() > 0)
                pass++;
            else
                stw.writeLine(word);
        }
        stw.close();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Elapsed:" + elapsed + " ms.");
        System.out.println("Speed:" + ((double) allWords.size() * 1000 / elapsed) + " word/sec.");

        System.out.println("Total words:" + allWords.size());
        System.out.println("Passed words:" + pass);
        System.out.println("Ratio=%" + ((double) pass * 100 / allWords.size()));
    }*/

/*    @Test
    @Ignore("Not a unit Test")
    public void oflazerComparison() throws IOException {
        List<String> allWords = SimpleTextReader.trimmingUTF8Reader(
                new File("/home/kodlab/data/lm/unknowns/oflazer-regular-words.tr.txt")).asStringList();
        System.out.println("word list loaded");
        SimpleTextWriter stw = SimpleTextWriter.keepOpenUTF8Writer(OFLAZER_MISMATCH_FILE);
        System.out.println("Initial number of Suffix Form Sets: " + suffixes.getFormCount());
        WordParser parser = simpleParser(
                MASTER_DICTIONARY_FILE,
                SECONDARY_DICTIONARY_FILE,
                NON_TDK_DICT_FILE);
        System.out.println("Total number of Suffix Form Sets After Adding Stems: " + suffixes.getFormCount());
        System.out.println("Total number of Suffix nodes: " + parser.graph.totalSuffixNodeCount());
        System.out.println("Total number of Stem nodes: " + parser.graph.totalStemNodeCount());
        System.out.println("Parsing started");
        long start = System.currentTimeMillis();
        int pass = 0;
        for (String word : allWords) {
            if (parser.parse(word).size() > 0)
                pass++;
            else {
                word = word.replaceAll("â", "a").replaceAll("î", "i").replaceAll("û", "u");
                if (parser.parse(word).size() > 0)
                    pass++;
                else
                    stw.writeLine(word);
            }
        }
        stw.close();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Elapsed:" + elapsed + " ms.");
        System.out.println("Speed:" + ((double) allWords.size() * 1000 / elapsed) + " word/sec.");

        System.out.println("Total words:" + allWords.size());
        System.out.println("Passed words:" + pass);
        System.out.println("Ratio=%" + ((double) pass * 100 / allWords.size()));
    }*/

/*    @Test
    @Ignore("Not a unit Test")
    public void generateSuffixSurfaceForms() throws IOException {
        Set<String> surfaceForms = new HashSet<String>();
        List<String> allWords = SimpleTextReader.trimmingUTF8Reader(Z2_VOCAB_FILE).asStringList();
        WordParser parser = simpleParser(MASTER_DICTIONARY_FILE, SECONDARY_DICTIONARY_FILE);
        for (String word : allWords) {
            List<WordAnalysis> results = parser.parse(word);
            for (WordAnalysis result : results) {
                for (WordAnalysis.InflectionalGroup node : result.inflectionalGroups) {
                    for (WordAnalysis.SuffixData d : node.suffixList) {
                        surfaceForms.add(d.surface);
                    }
                }
            }
        }
        List<String> lst = new ArrayList<String>(surfaceForms);
        Collections.sort(lst, Collator.getInstance(new Locale("tr")));
        SimpleTextWriter.oneShotUTF8Writer(new File(Resources.getResource("suffix-surface.txt").getFile())).writeLines(lst);
    }*/


    private WordAnalyzer simpleParser(File... dictionary) throws IOException {
        DynamicLexiconGraph graph = getLexiconGraph(dictionary);
        return new WordAnalyzer(graph);
    }

    static TurkishSuffixes suffixes = new TurkishSuffixes();

    private DynamicLexiconGraph getLexiconGraph(File... dictionaries) throws IOException {
        SuffixProvider suffixProvider = suffixes;
        RootLexicon lexicon = new RootLexicon();
        for (File dictionary : dictionaries) {
            new TurkishDictionaryLoader(suffixProvider).loadInto(lexicon, dictionary);
        }
        DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
        graph.addDictionaryItems(lexicon);
        return graph;
    }
}
