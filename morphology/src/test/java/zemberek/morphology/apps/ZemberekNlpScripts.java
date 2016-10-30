package zemberek.morphology.apps;

import org.junit.Test;
import zemberek.core.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.external.OflazerAnalyzerRunner;
import zemberek.morphology.lexicon.NullSuffixForm;
import zemberek.morphology.lexicon.SuffixForm;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.tr.TurkishWordParserGenerator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.*;
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

    static Path DATA_PATH = Paths.get("/home/afsina/data/nlp");

    @Test
    public void parseLargeVocabularyZemberek() throws IOException {
        Path wordFreqFile = DATA_PATH.resolve("vocab.all.freq");
        Path outDir = DATA_PATH.resolve("out");
        Files.createDirectories(outDir);

        TurkishWordParserGenerator parser = TurkishWordParserGenerator.createWithDefaults();
        System.out.println("Loading histogram.");
        Histogram<String> histogram = Histogram.loadFromUtf8File(wordFreqFile, ' ');
        List<String> accepted = new ArrayList<>(histogram.size() / 3);

        int c = 0;
        for (String s : histogram) {
            List<MorphParse> parses = parser.parse(s);
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

    static Path NLP_TOOLS_PATH = Paths.get("/home/afsina/apps/nlp/nlp-tools");
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
        List<String> accepted = new ArrayList<>(lines.size() / 5);
        for (String line : lines) {
            if (line.trim().length()==0 || line.endsWith("+?")) {
                continue;
            }
            accepted.add(line.substring(0, line.indexOf('\t')));
        }
        sortAndSave(DATA_PATH.resolve("out").resolve("oflazer-parsed-words.txt"), accepted);
    }

    @Test
    public void generateOnlyOflazer() throws IOException {
        Path inPath = DATA_PATH.resolve("out");
        List<String> zemberekAll =
                Files.readAllLines(inPath.resolve("zemberek-parsed-words.txt"));
        Log.info("Zemberek Loaded.");
        LinkedHashSet<String> onlyOflazer = new LinkedHashSet<>(Files.readAllLines(inPath.resolve("oflazer-parsed-words.txt")));
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

        LinkedHashSet<String> onlyZemberek = new LinkedHashSet<>(Files.readAllLines(dir.resolve("zemberek-parsed-words.txt")));
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
        Files.write(dir.resolve("no-parse-tr.txt"), histogram.getSortedList((a,b)->collTr.compare(a,b)));
    }

}
