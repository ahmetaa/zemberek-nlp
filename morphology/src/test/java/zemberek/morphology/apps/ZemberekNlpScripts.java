package zemberek.morphology.apps;

import com.google.common.base.Stopwatch;
import org.antlr.v4.runtime.Token;
import org.junit.Test;
import zemberek.core.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.analysis.tr.TurkishSentenceAnalyzer;
import zemberek.morphology.external.OflazerAnalyzerRunner;
import zemberek.morphology.lexicon.NullSuffixForm;
import zemberek.morphology.lexicon.SuffixForm;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.tokenizer.ZemberekLexer;
import zemberek.tokenizer.antlr.TurkishLexer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ZemberekNlpScripts {

    @Test
    public void generateSuffixNames() throws IOException {
        TurkishSuffixes suffixes = new TurkishSuffixes();
        List<SuffixForm> forms = new ArrayList<>();
        for (SuffixForm form : suffixes.getAllForms()) {
            if (form instanceof NullSuffixForm) {
                continue;
            }
            forms.add(form);
        }
        forms.sort((a, b) -> a.getId().compareTo(b.getId()));
        List<String> result = forms.stream().map(s -> s.id).collect(Collectors.toList());
        Files.write(Paths.get("suffix-list"), result);
    }

    static Path DATA_PATH = Paths.get("/home/ahmetaa/data/nlp");

    @Test
    public void parseLargeVocabularyZemberek() throws IOException {
        Path wordFreqFile = DATA_PATH.resolve("vocab.all.freq");
        Path outDir = DATA_PATH.resolve("out");
        Files.createDirectories(outDir);

        TurkishMorphology parser = TurkishMorphology.createWithDefaults();
        System.out.println("Loading histogram.");
        Histogram<String> histogram = Histogram.loadFromUtf8File(wordFreqFile, ' ');
        List<String> accepted = new ArrayList<>(histogram.size() / 3);

        int c = 0;
        for (String s : histogram) {
            List<WordAnalysis> parses = parser.analyze(s);
            if (parses.size() > 0 &&
                    parses.get(0).dictionaryItem.primaryPos != PrimaryPos.Unknown) {
                accepted.add(s);
            }
            if (c > 0 && c % 10000 == 0) {
                System.out.println("Processed = " + c);
            }
            c++;
        }

        sortAndSave(outDir.resolve("zemberek-parsed-words.txt"), accepted);
    }

    static Collator collTr = Collator.getInstance(new Locale("tr"));


    private void sortAndSave(Path outPath, List<String> accepted) throws IOException {
        Log.info("Sorting %d words.", accepted.size());
        accepted.sort(collTr::compare);
        Log.info("Writing.");
        try (PrintWriter pw = new PrintWriter(outPath.toFile(), "utf-8")) {
            accepted.forEach(pw::println);
        }
    }

    static Path NLP_TOOLS_PATH = Paths.get("/home/ahmetaa/apps/nlp/tools");
    static Path OFLAZER_ANALYZER_PATH = NLP_TOOLS_PATH.resolve("Morphological-Analyzer/Turkish-Oflazer-Linux64");

    @Test
    public void parseLargeVocabularyOflazer() throws IOException {

        OflazerAnalyzerRunner runner = new OflazerAnalyzerRunner(
                OFLAZER_ANALYZER_PATH.toFile(), OFLAZER_ANALYZER_PATH.resolve("tfeaturesulx.fst").toFile());

        Path wordFile = DATA_PATH.resolve("vocab.all");
        Path outDir = DATA_PATH.resolve("out");
        Files.createDirectories(outDir);

        Path outPath = outDir.resolve("oflazer-parses.txt");
        runner.parseSentences(wordFile.toFile(), outPath.toFile());
    }

    @Test
    public void extractFromOflazerAnalysisResult() throws IOException {
        Path inPath = DATA_PATH.resolve("out").resolve("oflazer-parses.txt");
        List<String> lines = Files.readAllLines(inPath, StandardCharsets.UTF_8);
        Log.info("Loaded.");
        LinkedHashSet<String> accepted = new LinkedHashSet<>(lines.size() / 5);
        for (String line : lines) {
            if (line.trim().length() == 0 || line.endsWith("+?")) {
                continue;
            }
            accepted.add(line.substring(0, line.indexOf('\t')));
        }
        sortAndSave(DATA_PATH.resolve("out").resolve("oflazer-parsed-words.txt"), new ArrayList<>(accepted));
    }

    @Test
    public void extractPostpDataFromOflazerAnalysisResult() throws IOException {
        Path inPath = DATA_PATH.resolve("out").resolve("oflazer-parses.txt");
        List<String> lines = Files.readAllLines(inPath, StandardCharsets.UTF_8);
        Log.info("Loaded.");
        LinkedHashSet<String> accepted = new LinkedHashSet<>(lines.size() / 50);
        for (String line : lines) {
            if (line.trim().length() == 0 || line.endsWith("+?") || !line.contains("Postp")) {
                continue;
            }
            accepted.add(line);
        }
        sortAndSave(DATA_PATH.resolve("out").resolve("oflazer-potp-words.txt"), new ArrayList<>(accepted));
    }

    @Test
    public void generateOnlyOflazer() throws IOException {
        Path inPath = DATA_PATH.resolve("out");
        List<String> zemberekAll =
                Files.readAllLines(inPath.resolve("zemberek-parsed-words.txt"));
        Log.info("Zemberek Loaded.");
        LinkedHashSet<String> onlyOflazer =
                new LinkedHashSet<>(Files.readAllLines(inPath.resolve("oflazer-parsed-words.txt")));
        Log.info("Oflazer Loaded.");
        zemberekAll.forEach(onlyOflazer::remove);
        Log.info("Writing.");
        Files.write(inPath.resolve("only-oflazer.txt"), onlyOflazer);
        Log.info("Oflazer-only saved.");
    }

    @Test
    public void generateOnlyZemberek() throws IOException {
        Path dir = DATA_PATH.resolve("out");
        List<String> oflazerAll =
                Files.readAllLines(dir.resolve("oflazer-parsed-words.txt"));
        Log.info("Oflazer Loaded.");

        LinkedHashSet<String> onlyZemberek =
                new LinkedHashSet<>(Files.readAllLines(dir.resolve("zemberek-parsed-words.txt")));
        Log.info("Zemberek Loaded.");

        oflazerAll.forEach(onlyZemberek::remove);
        Log.info("Writing.");

        Files.write(dir.resolve("only-zemberek.txt"), onlyZemberek);
        Log.info("Zemberek-only saved.");

    }

    @Test
    public void frequentUnknown() throws IOException {

        Path wordFreqFile = DATA_PATH.resolve("vocab.all.freq");

        System.out.println("Loading histogram.");
        Histogram<String> histogram = Histogram.loadFromUtf8File(wordFreqFile, ' ');

        Path dir = DATA_PATH.resolve("out");
        List<String> oflazerAll =
                Files.readAllLines(dir.resolve("oflazer-parsed-words.txt"));
        List<String> zemberekAll =
                Files.readAllLines(dir.resolve("zemberek-parsed-words.txt"));

        histogram.removeAll(oflazerAll);
        histogram.removeAll(zemberekAll);

        histogram.removeSmaller(10);

        Files.write(dir.resolve("no-parse-freq.txt"), histogram.getSortedList());
        Files.write(dir.resolve("no-parse-tr.txt"), histogram.getSortedList((a, b) -> collTr.compare(a, b)));
    }

    @Test
    public void generatorTest() throws IOException {

        TurkishMorphology parser = TurkishMorphology.createWithDefaults();
        List<WordAnalysis> result = parser.analyze("besiciliği");
        WordAnalysis first = result.get(0);
        System.out.println(first.inflectionalGroups);
    }

    @Test
    public void performance() throws IOException {
        List<String> lines = Files.readAllLines(
                Paths.get("/media/depo/data/aaa/corpora/dunya.100k"));
        TurkishMorphology parser = TurkishMorphology.createWithDefaults();
        TurkishSentenceAnalyzer sentenceParser = new TurkishSentenceAnalyzer(parser, new Z3MarkovModelDisambiguator());
        System.out.println(lines.size() + " lines will be processed.");
        System.out.println("Dictionary has " + parser.getLexicon().size() + " items.");

        long tokenCount = 0;
        long tokenCountNoPunct = 0;
        Stopwatch clock = Stopwatch.createStarted();
        ZemberekLexer lexer = new ZemberekLexer();
        for (String line : lines) {
            List<Token> tokens = lexer.tokenizeAll(line);
            tokenCount += tokens.stream().filter(s ->
                    (s.getType() != TurkishLexer.SpaceTab)).count();
            tokenCountNoPunct += tokens.stream().filter(s ->
                    (s.getType() != TurkishLexer.Punctuation && s.getType() != TurkishLexer.SpaceTab)).count();
        }
        long elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Elapsed Time = " + elapsed);
        System.out.println("Token Count = " + tokenCount);
        System.out.println("Token Count (No Punctuation) = " + tokenCountNoPunct);
        System.out.println("Tokenization Speed = "
                + tokenCount * 1000d / elapsed + " tokens/sec");
        System.out.println("Tokenization Speed (No Punctuation) = "
                + tokenCountNoPunct * 1000d / elapsed + " tokens/sec");
        System.out.println();
        System.out.println("Sentence word analysis test:");

        clock.reset().start();
        for (String line : lines) {
            try {
                sentenceParser.analyze(line);
            } catch (Exception e) {
                System.out.println(line);
                e.printStackTrace();
            }
        }
        elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Elapsed Time = " + elapsed);
        System.out.println("Tokenization + Analysis speed = " + tokenCount * 1000d / elapsed + " tokens/sec");
        System.out.println("Tokenization + Analysis speed (no punctuation) = " + tokenCountNoPunct * 1000d / elapsed + " tokens/sec");
        System.out.println();
        System.out.println("Disambiguation Test:");
        parser.invalidateAllCache();
        clock.reset().start();
        for (String line : lines) {
            try {
                sentenceParser.bestParse(line);
            } catch (Exception e) {
                System.out.println(line);
                e.printStackTrace();
            }
        }
        elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Elapsed Time = " + elapsed);
        System.out.println("Tokenization + Analysis + Disambiguation speed = " + tokenCount * 1000d / elapsed + " tokens/sec");
        System.out.println("Tokenization + Analysis + Disambiguation speed (no punctuation) = "
                + tokenCountNoPunct * 1000d / elapsed + " tokens/sec");
    }

    @Test
    public void testWordAnalysis() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        List<WordAnalysis> results = morphology.analyze("besiciliğe");
        for (WordAnalysis result : results) {
            System.out.println(result.formatLong());
            System.out.println("\tStems = " + result.getStems());
            System.out.println("\tLemmas = " + result.getLemmas());
        }
    }

    @Test
    public void testSentenceAnalysis() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        Z3MarkovModelDisambiguator disambiguator = new Z3MarkovModelDisambiguator();
        TurkishSentenceAnalyzer analyzer = new TurkishSentenceAnalyzer(morphology, disambiguator);

        String sentence = "Kırmızı kalemi al.";
        System.out.println("Sentence  = " + sentence);
        SentenceAnalysis analysis = analyzer.analyze(sentence);

        System.out.println("Before disambiguation.");
        writeParseResult(analysis);

        System.out.println("\nAfter disambiguation.");
        analyzer.disambiguate(analysis);
        writeParseResult(analysis);
    }

    private void writeParseResult(SentenceAnalysis analysis) {
        for (SentenceAnalysis.Entry entry : analysis) {
            System.out.println("Word = " + entry.input);
            for (WordAnalysis w : entry.parses) {
                System.out.println(w.formatLong());
            }
        }
    }

}
