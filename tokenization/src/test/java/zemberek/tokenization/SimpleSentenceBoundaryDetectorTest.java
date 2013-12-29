package zemberek.tokenization;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.tokenizer.SimpleSentenceBoundaryDetector;

import java.io.IOException;
import java.util.List;

public class SimpleSentenceBoundaryDetectorTest {

    private String markBoundaries(String input) {
        SimpleSentenceBoundaryDetector splitter = new SimpleSentenceBoundaryDetector();
        List<String> list = splitter.getSentences(input);
        return Joiner.on("|").join(list);
    }

    @Test
    public void testSimpleSentence() {
        Assert.assertEquals("Ali gel.",
                markBoundaries("Ali gel."));
        Assert.assertEquals("Ali gel.|Okul acildi!",
                markBoundaries("Ali gel. Okul acildi!"));
        Assert.assertEquals("Ali gel.|Okul acildi!",
                markBoundaries("Ali gel. Okul acildi!"));
        Assert.assertEquals("Ali gel...|Okul acildi.",
                markBoundaries("Ali gel... Okul acildi."));
        Assert.assertEquals("Tam 1.000.000 papeli cebe atmislar...",
                markBoundaries("Tam 1.000.000 papeli cebe atmislar..."));
        Assert.assertEquals("Prof. Dr. Veli Zambur %2.5 lik enflasyon oranini begenmemis!",
                markBoundaries("Prof. Dr. Veli Zambur %2.5 lik enflasyon oranini begenmemis!"));
        Assert.assertEquals("16. yüzyılda?|Dr. Av. Blah'a gitmiş.",
                markBoundaries("16. yüzyılda? Dr. Av. Blah'a gitmiş."));
        Assert.assertEquals("Ali gel.|Okul açıldı...|sınavda 2. oldum.",
                markBoundaries("Ali gel. Okul açıldı... sınavda 2. oldum."));
    }

    @Test
    @Ignore
    public void testRealData() throws IOException {
        List<String> lines = Resources.readLines(
                Resources.getResource("tokenizer/sentence-boundary-text.txt"), Charsets.UTF_8);
        String all = Joiner.on(" ").join(lines);
        SimpleSentenceBoundaryDetector detector = new SimpleSentenceBoundaryDetector();
        List<String> result = detector.getSentences(all);
        for (String s : result) {
            System.out.println(s);
        }
    }



}
