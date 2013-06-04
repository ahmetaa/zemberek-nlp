package zemberek.langid;

import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;

public class LanguageIndentifierTest {
    @Test
    public void functionalTest() throws IOException {
        String[] langs = {"en","tr"};
        LanguageIdentifier lid = LanguageIdentifier.generateFromCounts(langs);
        Assert.assertEquals("tr",lid.identifyFull("merhaba dünya ve tüm gezegenler"));
        Assert.assertEquals("en",lid.identifyFull("hello world and all the planets"));
    }
}
