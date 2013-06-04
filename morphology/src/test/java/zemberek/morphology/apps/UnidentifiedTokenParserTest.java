package zemberek.morphology.apps;

import org.junit.Test;
import zemberek.morphology.parser.MorphParse;

import java.io.IOException;
import java.util.List;

public class UnidentifiedTokenParserTest {

    @Test
    public void shouldCreateUnidentifiedTokenParserSuccessfully() throws IOException {
//        TurkishMorphParser parser = TurkishMorphParser.fromDefaultDictionaries();
        TurkishMorphParser parser = null;
        UnidentifiedTokenParser uiParser = new UnidentifiedTokenParser(parser);
        List<MorphParse> results = uiParser.parse("Gazi");
        for (MorphParse result : results) {
            System.out.println(result);
        }
    }

}
