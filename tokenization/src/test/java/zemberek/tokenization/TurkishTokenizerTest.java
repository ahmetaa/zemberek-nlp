package zemberek.tokenization;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.tokenization.Token.Type;

public class TurkishTokenizerTest {

  private void matchToken(
      TurkishTokenizer tokenizer,
      String input,
      Token.Type tokenType,
      String... expectedTokens) {
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
      if (tokenType != null) {
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
    matchToken(tokenizer, input, null, expectedTokens);
  }

  private void matchSentences(
      TurkishTokenizer tokenizer,
      String input,
      String expectedJoinedTokens) {
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
    t = TurkishTokenizer.builder().ignoreAll().acceptTypes(Type.Number).build();
    matchToken(t, "www.foo.bar 12,4'ü a@a.v ; ^% 2 adf 12 \r \n ", "12,4'ü", "2", "12");
  }

  @Test
  public void testNumbers() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchToken(t, "1", Type.Number, "1");
    matchToken(t, "12", Type.Number, "12");
    matchToken(t, "3.14", Type.Number, "3.14");
    matchToken(t, "-1", Type.Number, "-1");
    matchToken(t, "-1.34", Type.Number, "-1.34");
    matchToken(t, "-3,14", Type.Number, "-3,14");
    matchToken(t, "100'e", Type.Number, "100'e");
    matchToken(t, "3.14'ten", Type.Number, "3.14'ten");
    matchToken(t, "%2.5'ten", Type.PercentNumeral, "%2.5'ten");
    matchToken(t, "%2", Type.PercentNumeral, "%2");
    matchToken(t, "2.5'a", Type.Number, "2.5'a");
    matchToken(t, "2.5’a", Type.Number, "2.5’a");
  }

  @Test
  public void testWords() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchToken(t, "kedi", Type.Word, "kedi");
    matchToken(t, "Kedi", "Kedi");
    matchToken(t, "Ahmet'e", "Ahmet'e");
  }

  @Test
  public void testTags() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchToken(t, "<kedi>", Type.MetaTag, "<kedi>");
    matchToken(
        t,
        "<kedi><eti><7>",
        Type.MetaTag,
        "<kedi>", "<eti>", "<7>");
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
    Assert.assertEquals(0, t0.getStart());
    Assert.assertEquals(2, t0.getEnd());
    

    Token t1 = tokens.get(1);
    Assert.assertEquals(" ", t1.getText());
    Assert.assertEquals(3, t1.getStart());
    Assert.assertEquals(3, t1.getEnd());
    

    Token t2 = tokens.get(2);
    Assert.assertEquals("av.", t2.getText());
    Assert.assertEquals(4, t2.getStart());
    Assert.assertEquals(6, t2.getEnd());

    Token t3 = tokens.get(3);
    Assert.assertEquals(" ", t3.getText());
    Assert.assertEquals(7, t3.getStart());
    Assert.assertEquals(7, t3.getEnd());
    

    Token t4 = tokens.get(4);
    Assert.assertEquals("geldi", t4.getText());
    Assert.assertEquals(8, t4.getStart());
    Assert.assertEquals(12, t4.getEnd());
    

    Token t5 = tokens.get(5);
    Assert.assertEquals(".", t5.getText());
    Assert.assertEquals(13, t5.getStart());
    Assert.assertEquals(13, t5.getEnd());
    
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
  public void testUnderscoreWords() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchSentences(t, "__he_llo__", "__he_llo__");
  }

  @Test
  public void testDotInMiddle() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchSentences(t, "Ali.gel.", "Ali . gel .");
  }

  @Test
  public void testPunctuation() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchSentences(t,
        ".,!:;$%\"\'()[]{}&@®™©℠",
        ". , ! : ; $ % \" \' ( ) [ ] { } & @ ® ™ © ℠");
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
        "Hey", "\n", "\r", " ", "\n", "\r", "Ali", "\n", " ", "\n", "\n", " ", "\n", " ", "naber",
        "\n");
  }


  //TODO: failing.
  @Test
  @Ignore
  public void testUnknownWord() {
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    matchSentences(tokenizer, "L'Oréal", "L'Oréal");
  }

  @Test
  public void testUnknownWord2() {
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    matchSentences(tokenizer, "Bjørn", "Bjørn");
  }

  @Test
  public void testTimeToken() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchSentences(t,
        "Saat, 10:20 ile 00:59 arasinda.",
        "Saat , 10:20 ile 00:59 arasinda .");
    matchToken(t, "10:20", Type.Time, "10:20");
  }

  @Test
  public void testTimeTokenSeconds() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchToken(t, "10:20:53", Type.Time, "10:20:53");
    matchToken(t, "10.20.00'da", Type.Time, "10.20.00'da");
  }

  @Test
  public void testDateToken() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;

    matchSentences(t,
        "1/1/2011 02.12.1998'de.",
        "1/1/2011 02.12.1998'de .");
    matchToken(t, "1/1/2011", Type.Date, "1/1/2011");
    matchToken(t, "02.12.1998'de", Type.Date, "02.12.1998'de");
  }

  @Test
  public void testEmoticonToken() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;

    String[] emoticons = {
        ":)", ":-)", ":-]", ":D", ":-D", "8-)", ";)", ";‑)", ":(", ":-(",
        ":'(", ":‑/", ":/", ":^)", "¯\\_(ツ)_/¯", "O_o", "o_O", "O_O", "\\o/"
    };
    for (String s : emoticons) {
      matchToken(t, s, Type.Emoticon, s);
    }
  }

  @Test
  public void testUrl() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;

    String[] urls = {
        "http://t.co/gn32szS9",
        "http://foo.im/lrıvn",
        "http://www.fo.bar",
        "http://www.fo.bar'da",
        "https://www.fo.baz.zip",
        "www.fo.tar.kar",
        "www.fo.bar",
        "fo.com",
        "fo.com.tr",
        "fo.com.tr/index.html",
        "fo.com.tr/index.html?",
        "foo.net",
        "foo.net'e",
        "www.foo.net'te",
        "http://www.foo.net/showthread.php?134628-ucreti",
        "http://www.foo.net/showthread.php?1-34--628-ucreti+",
        "https://www.hepsiburada.com'dan",
    };
    for (String s : urls) {
      matchToken(t, s, Type.URL, s);
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
      matchToken(t, s, Type.URL, s);
    }
  }

  @Test
  public void testEmail() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;

    String[] emails = {
        "fo@bar.baz",
        "fo.bar@bar.baz",
        "fo_.bar@bar.baz",
        "ali@gmail.com'u"
    };
    for (String s : emails) {
      matchToken(t, s, Type.Email, s);
    }
  }

  @Test
  public void mentionTest() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;

    String[] ss = {
        "@bar",
        "@foo_bar",
        "@kemal'in"
    };
    for (String s : ss) {
      matchToken(t, s, Type.Mention, s);
    }
  }

  @Test
  public void hashTagTest() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;

    String[] ss = {
        "#foo",
        "#foo_bar",
        "#foo_bar'a"
    };
    for (String s : ss) {
      matchToken(t, s, Type.HashTag, s);
    }
  }

  @Test
  public void testEllipsis() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchSentences(t, "Merhaba, Kaya Ivır ve Tunç Zıvır…", "Merhaba , Kaya Ivır ve Tunç Zıvır …");
  }

  @Test
  @Ignore("Not an actual test. Requires external data.")
  public void performance() throws IOException {
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

    // load a hundred thousand lines.
    for (int it = 0; it < 5; it++) {
      List<String> lines = Files.readAllLines(
          Paths.get("/media/aaa/Data/aaa/corpora/dunya.100k"));
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
