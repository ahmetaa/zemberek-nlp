package zemberek.morphology.apps;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.Histogram;
import zemberek.core.io.SimpleTextReader;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.SentenceMorphParse;
import zemberek.morphology.parser.tr.TurkishSentenceParser;
import zemberek.morphology.parser.tr.TurkishWordParserGenerator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TurkishSentenceParserTest {

    TurkishSentenceParser parser;

    @Before
    public void setUp() throws Exception {
        TurkishWordParserGenerator morphParser = TurkishWordParserGenerator.createWithDefaults();
        parser = new TurkishSentenceParser(morphParser, new Z3MarkovModelDisambiguator());
    }

    @Test
    public void tokenCountTest() {
        Assert.assertEquals(6, parser.bestParse("15. yüzyılda, Türkiye'de yaşadı.").size());
    }

    @Test
    @Ignore("To be executed manually, not within continuous build.")
    public void shouldParseSentencesInNTVMSNBCCorpus() throws IOException {
        final File corpus = new File("/home/kodlab/data/2014-mt-txt/dunya100k");
        doParseSentencesInCorpus(corpus);
    }

    private void doParseSentencesInCorpus(File ntvmsnbcCorpus) throws IOException {
        List<String> sentences = SimpleTextReader.trimmingUTF8Reader(ntvmsnbcCorpus).asStringList();
        Stopwatch sw = Stopwatch.createStarted();
        long wc = 0;
        int s = 0;
        Histogram<String> unknownStuff = new Histogram<>();
        for (String sentence : sentences) {
            if(sentence.contains("ağaçlandırılacağından")||sentence.contains("Ağaçlandırılacağından"))
                System.out.println();
            SentenceMorphParse parse = parser.parse(sentence);
            for (SentenceMorphParse.Entry entry : parse) {
                List<MorphParse> parses = entry.parses;
                for (MorphParse morphParse : parses) {
                    if(morphParse.getSurfaceForm().equals("ağaçlandırılacağından"))
                        System.out.println();
                    if (morphParse.dictionaryItem == DictionaryItem.UNKNOWN) {
/*
                        if(parses.size()>1)
                            System.out.println("huh");
*/
                        unknownStuff.add(morphParse.getSurfaceForm());
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
