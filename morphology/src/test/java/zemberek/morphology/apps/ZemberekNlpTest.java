package zemberek.morphology.apps;

import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.tr.TurkishSentenceParser;
import zemberek.morphology.parser.tr.TurkishWordParserGenerator;

import java.io.IOException;
import java.util.List;

public class ZemberekNlpTest {
    public static void main(String[] args) throws IOException {
        TurkishWordParserGenerator parser = TurkishWordParserGenerator.createWithDefaults();
        List<MorphParse> parses = parser.getParser().parse("öğrencilerden");
        for (MorphParse parse : parses) {
            System.out.println(parse.formatLong());
        }

        TurkishSentenceParser sentenceParser = new TurkishSentenceParser(parser, new Z3MarkovModelDisambiguator());
        List<MorphParse> parseRes = sentenceParser.bestParse("5. gün yüz metre yüzdüm.");
        for (MorphParse p : parseRes) {
            System.out.println(p.formatLong());
        }
    }
}
