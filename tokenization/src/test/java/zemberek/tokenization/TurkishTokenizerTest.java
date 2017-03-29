package zemberek.tokenization;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import org.antlr.v4.runtime.Token;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.tokenization.antlr.TurkishLexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TurkishTokenizerTest {

    private void matchToken(TurkishTokenizer tokenizer, String input, int tokenType, String... expectedTokens) {
        List<Token> tokens = tokenizer.tokenize(input);
        Assert.assertNotNull("Token list is null.", tokens);
        Assert.assertTrue(tokens.size() > 0);
        Assert.assertEquals("Token count is not equal to expected Token count for input " + input,
                expectedTokens.length, tokens.size());
        int i = 0;
        for (String expectedToken : expectedTokens) {
            Token token = tokens.get(i);
            Assert.assertEquals(expectedToken + " is not equal to " + token.getText(),
                    expectedToken, token.getText());
            if (tokenType != -1) {
                Assert.assertEquals(tokenType, token.getType());
            }
            i++;
        }
    }

    private String getTokensAsString(TurkishTokenizer tokenizer, String input) {
        List<String> elements = tokenizer.tokenizeToStrings(input);
        return Joiner.on(" ").join(elements);
    }

    private void matchToken(TurkishTokenizer tokenizer, String input, String... expectedTokens) {
        matchToken(tokenizer, input, -1, expectedTokens);
    }

    private void matchSentences(TurkishTokenizer tokenizer, String input, String expectedJoinedTokens) {
        String actual = getTokensAsString(tokenizer, input);
        Assert.assertEquals(expectedJoinedTokens, actual);
    }

    @Test
    public void testInstances() {
        // default ignores white spaces and new lines.
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchToken(t, "a b \t c   \n \r", "a", "b", "c");
        // ALL tokenizer catches all tokens.
        t = TurkishTokenizer.ALL;
        matchToken(t, " a b\t\n\rc", " ", "a", " ", "b", "\t", "\n", "\r", "c");
        // A tokenizer only catches Number type (not date or times).
        t = TurkishTokenizer.builder().ignoreAll().acceptTypes(TurkishLexer.Number).build();
        matchToken(t, "www.foo.bar 12,4'ü a@a.v ; ^% 2 adf 12 \r \n ", "12,4'ü", "2", "12");
    }

    @Test
    public void testNumbers() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchToken(t, "1", TurkishLexer.Number, "1");
        matchToken(t, "12", TurkishLexer.Number, "12");
        matchToken(t, "3.14", TurkishLexer.Number, "3.14");
        matchToken(t, "-1", TurkishLexer.Number, "-1");
        matchToken(t, "-1.34", TurkishLexer.Number, "-1.34");
        matchToken(t, "-3,14", TurkishLexer.Number, "-3,14");
        matchToken(t, "100'e", TurkishLexer.Number, "100'e");
        matchToken(t, "3.14'ten", TurkishLexer.Number, "3.14'ten");
        matchToken(t, "%2.5'ten", TurkishLexer.PercentNumeral, "%2.5'ten");
    }

    @Test
    public void testWords() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchToken(t, "kedi", TurkishLexer.Word, "kedi");
        matchToken(t, "Kedi", "Kedi");
        matchToken(t, "Ahmet'e", "Ahmet'e");
    }

    @Test
    public void testAlphaNumerical() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchSentences(t,
                "F-16'yı, (H1N1) H1N1'den.",
                "F-16'yı , ( H1N1 ) H1N1'den .");
    }


    @Test
    public void testCapitalWords() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchToken(t, "TCDD", "TCDD");
        matchToken(t, "I.B.M.", "I.B.M.");
        matchToken(t, "TCDD'ye", "TCDD'ye");
        matchToken(t, "I.B.M.'nin", "I.B.M.'nin");
        matchToken(t, "I.B.M'nin", "I.B.M'nin");
        matchSentences(t, "İ.Ö,Ğ.Ş", "İ.Ö , Ğ.Ş");
        matchSentences(t, "İ.Ö,", "İ.Ö ,");
        matchSentences(t, "İ.Ö.,Ğ.Ş.", "İ.Ö. , Ğ.Ş.");
    }

    @Test
    public void testAbbreviations() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchToken(t, "Prof.", "Prof.");
        matchToken(t, "yy.", "yy.");
        matchSentences(t, "kedi.", "kedi .");
    }

    @Test
    public void testApostrophes() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchToken(t, "foo'f", "foo'f");
        matchToken(t, "foo’f", "foo’f");
        matchSentences(t, "’foo", "’ foo");
        matchSentences(t, "’’foo’", "’ ’ foo ’");
        matchSentences(t, "'foo'", "' foo '");
        matchSentences(t, "'foo'fo", "' foo'fo");
        matchSentences(t, "‘foo'fo’", "‘ foo'fo ’");
    }

    @Test
    public void testTokenBoundaries() {
        TurkishTokenizer t = TurkishTokenizer.ALL;
        List<Token> tokens = t.tokenize("bir av. geldi.");
        Token t0 = tokens.get(0);
        Assert.assertEquals("bir", t0.getText());
        Assert.assertEquals(0, t0.getStartIndex());
        Assert.assertEquals(2, t0.getStopIndex());
        Assert.assertEquals(0, t0.getCharPositionInLine());

        Token t1 = tokens.get(1);
        Assert.assertEquals(" ", t1.getText());
        Assert.assertEquals(3, t1.getStartIndex());
        Assert.assertEquals(3, t1.getStopIndex());
        Assert.assertEquals(3, t1.getCharPositionInLine());

        Token t2 = tokens.get(2);
        Assert.assertEquals("av.", t2.getText());
        Assert.assertEquals(4, t2.getStartIndex());
        Assert.assertEquals(6, t2.getStopIndex());
        Assert.assertEquals(4, t2.getCharPositionInLine());

        Token t3 = tokens.get(3);
        Assert.assertEquals(" ", t3.getText());
        Assert.assertEquals(7, t3.getStartIndex());
        Assert.assertEquals(7, t3.getStopIndex());
        Assert.assertEquals(7, t3.getCharPositionInLine());

        Token t4 = tokens.get(4);
        Assert.assertEquals("geldi", t4.getText());
        Assert.assertEquals(8, t4.getStartIndex());
        Assert.assertEquals(12, t4.getStopIndex());
        Assert.assertEquals(8, t4.getCharPositionInLine());

        Token t5 = tokens.get(5);
        Assert.assertEquals(".", t5.getText());
        Assert.assertEquals(13, t5.getStartIndex());
        Assert.assertEquals(13, t5.getStopIndex());
        Assert.assertEquals(13, t5.getCharPositionInLine());
    }

    @Test
    public void testAbbreviations2() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;

        matchSentences(t,
                "Prof. Dr. Ahmet'e git! dedi Av. Mehmet.",
                "Prof. Dr. Ahmet'e git ! dedi Av. Mehmet .");
    }

    @Test
    public void testCapitalLettersAfterQuotesIssue64() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;

        matchSentences(t, "Ankaraya.", "Ankaraya .");
        matchSentences(t, "Ankara'ya.", "Ankara'ya .");
        matchSentences(t, "ANKARA'ya.", "ANKARA'ya .");
        matchSentences(t, "ANKARA'YA.", "ANKARA'YA .");
        matchSentences(t, "Ankara'YA.", "Ankara'YA .");
        matchSentences(t, "Ankara'Ya.", "Ankara'Ya .");
    }

    @Test
    public void testUnknownWord1() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchSentences(t, "زنبورك", "زنبورك");
    }

    @Test
    public void testDotInMiddle() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchSentences(t, "Ali.gel.", "Ali . gel .");
    }

    @Test
    public void testPunctuation() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchSentences(t, ".,!:;$%\"\'()[]{}&@", ". , ! : ; $ % \" \' ( ) [ ] { } & @");
        matchToken(t, "...", "...");
        matchToken(t, "(!)", "(!)");
    }

    @Test
    public void testTokenizeSentence() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchSentences(t, "Ali gel.", "Ali gel .");
        matchSentences(t, "(Ali gel.)", "( Ali gel . )");
        matchSentences(t, "Ali'ye, gel...", "Ali'ye , gel ...");
        matchSentences(t, "\"Ali'ye\", gel!...", "\" Ali'ye \" , gel ! ...");
        matchSentences(t, "[Ali]{gel}", "[ Ali ] { gel }");
    }

    @Test
    public void testTokenizeDoubleQuote() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;

        matchSentences(t, "\"Soner\"'e boyle dedi", "\" Soner \" ' e boyle dedi");
        matchSentences(t, "Hey \"Ali\" gel.", "Hey \" Ali \" gel .");
        matchSentences(t, "\"Soner boyle dedi\"", "\" Soner boyle dedi \"");
    }

    @Test
    public void testNewline() {
        TurkishTokenizer tokenizer = TurkishTokenizer.ALL;
        matchToken(tokenizer, "Hey \nAli naber\n", "Hey", " ", "\n", "Ali", " ", "naber", "\n");
        matchToken(tokenizer, "Hey\n\r \n\rAli\n \n\n \n naber\n",
                "Hey", "\n", "\r", " ", "\n", "\r", "Ali", "\n", " ", "\n", "\n", " ", "\n", " ", "naber", "\n");
    }


    //TODO: failing.
    @Test
    @Ignore
    public void testUnknownWord() {
        TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
        matchSentences(tokenizer, "L'Oréal", "L'Oréal");
    }

    @Test
    public void testTimeToken() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchSentences(t,
                "Saat, 10:20 ile 00:59 arasinda.",
                "Saat , 10:20 ile 00:59 arasinda .");
        matchToken(t, "10:20", TurkishLexer.Time, "10:20");
    }

    @Test
    public void testTimeTokenSeconds() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;
        matchToken(t, "10:20:53", TurkishLexer.Time, "10:20:53");
        matchToken(t, "10.20.00'da", TurkishLexer.Time, "10.20.00'da");
    }

    @Test
    public void testDateToken() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;

        matchSentences(t,
                "1/1/2011 02.12.1998'de.",
                "1/1/2011 02.12.1998'de .");
        matchToken(t, "1/1/2011", TurkishLexer.Date, "1/1/2011");
        matchToken(t, "02.12.1998'de", TurkishLexer.Date, "02.12.1998'de");
    }

    @Test
    public void testEmoticonToken() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;

        String[] emoticons = {
                ":)", ":-)", ":-]", ":D", ":-D", "8-)", ";)", ";‑)", ":(", ":-(",
                ":'(", ":‑/", ":/", ":^)", "¯\\_(ツ)_/¯", "O_o", "o_O", "O_O", "\\o/"
        };
        for (String s : emoticons) {
            matchToken(t, s, TurkishLexer.Emoticon, s);
        }
    }

    @Test
    public void testUrl() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;

        String[] urls = {
                "http://www.fo.bar",
                "https://www.fo.baz.zip",
                "www.fo.tar.kar",
                "www.fo.bar",
                "http://www.foo.net/showthread.php?134628-ucreti",
        };
        for (String s : urls) {
            matchToken(t, s, TurkishLexer.URL, s);
        }
    }

    @Test
    public void testUrl2() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;

        String[] urls = {
                "https://www.google.com.tr/search?q=bla+foo&oq=blah+net&aqs=chrome.0.0l6",
                "https://www.google.com.tr/search?q=bla+foo&oq=blah+net&aqs=chrome.0.0l6.5486j0j4&sourceid=chrome&ie=UTF-8"
        };
        for (String s : urls) {
            matchToken(t, s, TurkishLexer.URL, s);
        }
    }

    @Test
    public void testEmail() {
        TurkishTokenizer t = TurkishTokenizer.DEFAULT;

        String[] urls = {
                "fo@bar.baz",
                "fo.bar@bar.baz"
        };
        for (String s : urls) {
            matchToken(t, s, TurkishLexer.Email, s);
        }
    }

    @Test
    @Ignore("Not an actual test. Requires external data.")
    public void performance() throws IOException {
        TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

        // load a hundred thousand lines.
        List<String> lines = Files.readAllLines(
                Paths.get("/home/ahmetaa/data/nlp/corpora/dunya.100k"));
        for (int it = 0; it < 5; it++) {
            Stopwatch clock = Stopwatch.createStarted();
            long tokenCount = 0;
            for (String line : lines) {
                List<Token> tokens = tokenizer.tokenize(line);
                tokenCount += tokens.size();
            }
            long elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
            Log.info("Token count = %d ", tokenCount);
            Log.info("Speed (tps) = %.1f", tokenCount * 1000d / elapsed);
        }
    }


}
