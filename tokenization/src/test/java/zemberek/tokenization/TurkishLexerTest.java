package zemberek.tokenization;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.tokenizer.antlr.TurkishLexer;

import java.util.List;

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
        matchSentences("F-16'yı, (H1N1) H1N1'den.", "F-16'yı , ( H1N1 ) H1N1'den .");
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
    public void testUnknown() {
        matchToken("~", TurkishLexer.Unknown, "~");
        //matchToken("AaAa", TurkishLexer.Unknown, "AaAa");
    }

}
