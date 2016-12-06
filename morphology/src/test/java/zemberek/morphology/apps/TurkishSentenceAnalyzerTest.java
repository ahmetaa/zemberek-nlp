package zemberek.morphology.apps;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.collections.Histogram;
import zemberek.core.io.SimpleTextReader;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.tr.TurkishSentenceAnalyzer;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TurkishSentenceAnalyzerTest {

    TurkishSentenceAnalyzer parser;

    @Before
    public void setUp() throws Exception {
        TurkishMorphology morphParser = TurkishMorphology.createWithDefaults();
        parser = new TurkishSentenceAnalyzer(morphParser, new Z3MarkovModelDisambiguator());
    }

    @Test
    public void tokenCountTest() {
        Assert.assertEquals(6, parser.bestParse("15. yüzyılda, Türkiye'de yaşadı.").size());
    }

    @Test
    @Ignore("To be executed manually, not within continuous build.")
    public void shouldParseSentencesInNTVMSNBCCorpus() throws IOException {
        final File corpus = new File("/home/kodlab/data/2014-mt-txt/dunya");
        doParseSentencesInCorpus(corpus);
    }

    private void doParseSentencesInCorpus(File ntvmsnbcCorpus) throws IOException {
        List<String> sentences = SimpleTextReader.trimmingUTF8Reader(ntvmsnbcCorpus).asStringList();
        Stopwatch sw = Stopwatch.createStarted();
        long wc = 0;
        int s = 0;
        Histogram<String> unknownStuff = new Histogram<>();
        for (String sentence : sentences) {
            SentenceAnalysis parse = parser.analyze(sentence);
            for (SentenceAnalysis.Entry entry : parse) {
                List<WordAnalysis> parses = entry.parses;
                for (WordAnalysis wordAnalysis : parses) {
                    if (wordAnalysis.dictionaryItem == DictionaryItem.UNKNOWN) {
                        unknownStuff.add(wordAnalysis.getSurfaceForm());
                    }
                }
            }
            wc += parse.size();
            //parser.disambiguate(parse);
            s++;
            if (s % 10000 == 0) {
                System.out.println(s);
                System.out.println(sw.elapsed(TimeUnit.MILLISECONDS) / 1000d);
            }
        }
        try (PrintWriter pw = new PrintWriter("unknown.txt", "utf-8")) {
            for (String s1 : unknownStuff.getSortedList()) {
                pw.println(s1+ " " + unknownStuff.getCount(s1));
            }
        }
        System.out.println("Word count = " + wc);
        System.out.println("Elapsed Time =" + sw.elapsed(TimeUnit.MILLISECONDS));
        System.out.println("Parse and disambiguate per second = " + (wc * 1000d) / (sw.elapsed(TimeUnit.MILLISECONDS)));
    }

}
