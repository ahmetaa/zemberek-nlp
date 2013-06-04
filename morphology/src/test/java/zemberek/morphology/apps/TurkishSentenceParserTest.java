package zemberek.morphology.apps;

import com.google.common.base.Stopwatch;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import smoothnlp.core.io.SimpleTextReader;
import zemberek.morphology.parser.SentenceMorphParse;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TurkishSentenceParserTest {

    TurkishSentenceParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new TurkishSentenceParser(new File("res"));
    }

    @Test
    @Ignore("To be executed manually, not within continuous build.")
    public void shouldParseSentencesInNTVMSNBCCorpus() throws IOException {
        final File ntvmsnbcCorpus = new File("/home/kodlab/apps/nlp/sak/ntvmsnbc.txt");
        doParseSentencesInCorpus(ntvmsnbcCorpus);
    }

    private void doParseSentencesInCorpus(File ntvmsnbcCorpus) throws IOException {
    /* SentenceMorphParse parse = parser.parse("Turgut Özal'ın ölüm raporu ile ilgili flaş bir gelişme.");
     parse.dump();
     System.out.println("After disambiguation:");
     parser.disambiguate(parse);
     parse.dump();
     for (SentenceMorphParse.Entry entry : parse) {
         System.out.println(entry.input + "=" + entry.parses.get(0));
     }
     for (SentenceMorphParse.Entry entry : parse) {
         System.out.println(entry.input + " kök=" + entry.parses.get(0).stem);
     }*/
        List<String> sentences = SimpleTextReader.trimmingUTF8Reader(ntvmsnbcCorpus).asStringList();
        Stopwatch sw = new Stopwatch().start();
        int wc = 0;
        for (String sentence : sentences) {
            SentenceMorphParse parse = parser.parse(sentence);
            wc += parse.size();
            parser.disambiguate(parse);
            // System.out.println(sentence);
            // parse.dump();
        }
        System.out.println(wc);
        System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));
    }

}
