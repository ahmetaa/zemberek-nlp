package zemberek.morphology.apps;

import org.junit.Test;
import zemberek.core.Histogram;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.lexicon.NullSuffixForm;
import zemberek.morphology.lexicon.SuffixForm;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.tr.TurkishWordParserGenerator;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    public void parseLargeVocabularyZemberek() throws IOException {
        Path wordFreqFile = Paths.get("/media/depo/data/aaa/vocab.all.freq");
        Path outDir = Paths.get("/media/depo/data/aaa/out");
        Files.createDirectories(outDir);

        TurkishWordParserGenerator parser = TurkishWordParserGenerator.createWithDefaults();
        System.out.println("Loading histogram.");
        Histogram<String> histogram = Histogram.loadFromUtf8File(wordFreqFile, ' ');
        int c = 0;
        try (PrintWriter pw = new PrintWriter(
                outDir.resolve("zemberek-parsed-words.txt").toFile(), "utf-8")) {
            for (String s : histogram) {
                List<MorphParse> parses = parser.parse(s);
                if (parses.size() > 0 &&
                        parses.get(0).dictionaryItem.primaryPos != PrimaryPos.Unknown) {
                    pw.println(s);
                }
                if (c > 0 && c % 10000 == 0) {
                    System.out.println("Processed = " + c);
                }
                c++;
            }

        }
    }


}
