package zemberek.tokenization;

import com.google.common.base.Stopwatch;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.tokenizer.antlr.TurkishLexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TurkishLexerTest {

    private void dumpTokens(List<Token> tokens) {
        for (Token token : tokens) {
            Log.debug(token.getText());
        }
    }

    private void matchToken(String input, int tokenType, String... expectedTokens) {
        List<Token> tokens = getTokens(input);
        Assert.assertNotNull("Token list is null.", tokens);
        Assert.assertTrue(tokens.size() > 0);
        // Remove the <EOF> token.
        tokens.remove(tokens.size() - 1);
        dumpTokens(tokens);
        Assert.assertEquals("Token count is not equal to expected Token count. ",
                expectedTokens.length, tokens.size());
        int i = 0;
        for (String expectedToken : expectedTokens) {
            Token token = tokens.get(i);
            Assert.assertEquals(expectedToken, token.getText());
            if (tokenType != -1) {
                Assert.assertEquals(tokenType, token.getType());
            }
            i++;
        }
    }

    private List<Token> getTokens(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        TurkishLexer lexer = new TurkishLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        tokenStream.fill();
        return tokenStream.getTokens();
    }

    private String getTokensAsString(String input) {
        List<Token> tokens = getTokens(input);
        Assert.assertNotNull("Token list is null.", tokens);
        Assert.assertTrue(tokens.size() > 0);
        tokens.remove(tokens.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            if (token.getType() != TurkishLexer.SpaceTab)
                sb.append(token.getText()).append(" ");
        }
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    private void matchToken(String input, String... expectedTokens) {
        matchToken(input, -1, expectedTokens);
    }

    private void matchSentences(String input, String expectedJoinedTokens) {
        Assert.assertEquals(expectedJoinedTokens, getTokensAsString(input));
    }

    @Test
    public void testNumbers() {
        matchToken("1", TurkishLexer.Number, "1");
        matchToken("12", TurkishLexer.Number, "12");
        matchToken("3.14", TurkishLexer.Number, "3.14");
        matchToken("-1", TurkishLexer.Number, "-1");
        matchToken("-1.34", TurkishLexer.Number, "-1.34");
        matchToken("-3,14", TurkishLexer.Number, "-3,14");
        matchToken("100'e", TurkishLexer.Number, "100'e");
        matchToken("3.14'ten", TurkishLexer.Number, "3.14'ten");
        matchToken("%2.5'ten", TurkishLexer.PercentNumeral, "%2.5'ten");
    }

    @Test
    public void testWords() {
        matchToken("kedi", TurkishLexer.TurkishWord, "kedi");
        matchToken("Kedi", "Kedi");
        matchToken("Ahmet'e", "Ahmet'e");
    }

    @Test
    public void testAlphaNumerical() {
        matchSentences(
                "F-16'yı, (H1N1) H1N1'den.",
                "F-16'yı , ( H1N1 ) H1N1'den .");
    }


    @Test
    public void testCapitalWords() {
        matchToken("TCDD", "TCDD");
        matchToken("I.B.M.", "I.B.M.");
        matchToken("TCDD'ye", "TCDD'ye");
        matchToken("I.B.M.'nin", "I.B.M.'nin");
        matchToken("I.B.M'nin", "I.B.M'nin");
        matchSentences("İ.Ö,Ğ.Ş", "İ.Ö , Ğ.Ş");
        matchSentences("İ.Ö,", "İ.Ö ,");
        matchSentences("İ.Ö.,Ğ.Ş.", "İ.Ö. , Ğ.Ş.");
    }

    @Test
    public void testAbbreviations() {
        matchToken("Prof.", "Prof.");
        matchToken("yy.", "yy.");
        matchSentences("kedi.", "kedi .");
    }

    @Test
    public void testTokenBoundaries() {
        List<Token> tokens = getTokens("bir av. geldi.");
        Token t0 = tokens.get(0);
        Assert.assertEquals("bir", t0.getText());
        Assert.assertEquals(0, t0.getStartIndex());
        Assert.assertEquals(2, t0.getStopIndex());
        Assert.assertEquals(0, t0.getTokenIndex());
        Assert.assertEquals(0, t0.getCharPositionInLine());

        Token t1 = tokens.get(1);
        Assert.assertEquals(" ", t1.getText());
        Assert.assertEquals(3, t1.getStartIndex());
        Assert.assertEquals(3, t1.getStopIndex());
        Assert.assertEquals(1, t1.getTokenIndex());
        Assert.assertEquals(3, t1.getCharPositionInLine());

        Token t2 = tokens.get(2);
        Assert.assertEquals("av.", t2.getText());
        Assert.assertEquals(4, t2.getStartIndex());
        Assert.assertEquals(6, t2.getStopIndex());
        Assert.assertEquals(2, t2.getTokenIndex());
        Assert.assertEquals(4, t2.getCharPositionInLine());

        Token t3 = tokens.get(3);
        Assert.assertEquals(" ", t3.getText());
        Assert.assertEquals(7, t3.getStartIndex());
        Assert.assertEquals(7, t3.getStopIndex());
        Assert.assertEquals(3, t3.getTokenIndex());
        Assert.assertEquals(7, t3.getCharPositionInLine());

        Token t4 = tokens.get(4);
        Assert.assertEquals("geldi", t4.getText());
        Assert.assertEquals(8, t4.getStartIndex());
        Assert.assertEquals(12, t4.getStopIndex());
        Assert.assertEquals(4, t4.getTokenIndex());
        Assert.assertEquals(8, t4.getCharPositionInLine());

        Token t5 = tokens.get(5);
        Assert.assertEquals(".", t5.getText());
        Assert.assertEquals(13, t5.getStartIndex());
        Assert.assertEquals(13, t5.getStopIndex());
        Assert.assertEquals(5, t5.getTokenIndex());
        Assert.assertEquals(13, t5.getCharPositionInLine());
    }

    @Test
    public void testAbbreviations2() {
        matchSentences(
                "Prof. Dr. Ahmet'e git! dedi Av. Mehmet.",
                "Prof. Dr. Ahmet'e git ! dedi Av. Mehmet .");
    }

    @Test
    public void testCapitalLettersAfterQuotesIssue64() {
        matchSentences("Ankaraya.", "Ankaraya .");
        matchSentences("Ankara'ya.", "Ankara'ya .");
        matchSentences("ANKARA'ya.", "ANKARA'ya .");
        matchSentences("ANKARA'YA.", "ANKARA'YA .");
        matchSentences("Ankara'YA.", "Ankara'YA .");
        matchSentences("Ankara'Ya.", "Ankara'Ya .");
    }

    @Test
    public void testUnknownWord1() {
        matchSentences("زنبورك", "زنبورك");
    }

    @Test
    public void testDotInMiddle() {
        matchSentences("Ali.gel.", "Ali . gel .");
    }

    @Test
    public void testPunctuation() {
        matchSentences(".,!:;$%\"\'()[]{}&@", ". , ! : ; $ % \" \' ( ) [ ] { } & @");
        matchToken("...", "...");
        matchToken("(!)", "(!)");
    }

    @Test
    public void testTokenizeSentence() {
        matchSentences("Ali gel.", "Ali gel .");
        matchSentences("(Ali gel.)", "( Ali gel . )");
        matchSentences("Ali'ye, gel...", "Ali'ye , gel ...");
        matchSentences("\"Ali'ye\", gel!...", "\" Ali'ye \" , gel ! ...");
        matchSentences("[Ali]{gel}", "[ Ali ] { gel }");
    }

    @Test
    public void testTokenizeDoubleQuote() {
        matchSentences("\"Soner\"'e boyle dedi", "\" Soner \" ' e boyle dedi");
        matchSentences("Hey \"Ali\" gel.", "Hey \" Ali \" gel .");
        matchSentences("\"Soner boyle dedi\"", "\" Soner boyle dedi \"");
    }

    @Test
    public void testNewline() {
        matchSentences("Hey \nAli naber\n", "Hey \n Ali naber \n");
        matchSentences("Hey\n\r \n\rAli\n \n\n \n naber\n", "Hey \n \r \n \r Ali \n \n \n \n naber \n");
    }


    //TODO: failing.
    @Test
    @Ignore
    public void testUnknownWord() {
        matchSentences("L'Oréal", "L'Oréal");
    }

    @Test
    public void testTimeToken() {
        matchSentences(
                "Saat, 10:20 ile 00:59 arasinda.",
                "Saat , 10:20 ile 00:59 arasinda .");
        matchToken("10:20", TurkishLexer.TimeHours, "10:20");
    }

    @Test
    @Ignore ("Not an actual test. Requires external data.")
    public void performance() throws IOException {
        // load a hundred thousand lines.
        List<String> lines = Files.readAllLines(
                Paths.get("/media/depo/data/aaa/corpora/dunya.100k"));
        Stopwatch clock = Stopwatch.createStarted();
        long tokenCount = 0;
        for (String line : lines) {
            List<Token> tokens = getTokens(line);
            tokenCount +=  tokens.stream().filter(s ->
                    (s.getType() != TurkishLexer.SpaceTab)).count();
        }
        long elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
        System.out.println(elapsed);
        System.out.println("Token count = " + tokenCount);
        System.out.println("Speed (tps) = " + tokenCount * 1000d / elapsed);
    }


}
