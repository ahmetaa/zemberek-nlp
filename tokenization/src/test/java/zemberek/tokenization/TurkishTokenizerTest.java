package zemberek.tokenization;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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


  @Test
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
  public void testTypeAcceptanceAndBuilder() {
    TurkishTokenizer def = TurkishTokenizer.DEFAULT;
    Assert.assertTrue(def.isTypeAccepted(Type.Word));
    Assert.assertFalse(def.isTypeIgnored(Type.Word));
    Assert.assertFalse(def.isTypeAccepted(Type.SpaceTab));
    Assert.assertTrue(def.isTypeIgnored(Type.SpaceTab));
    Assert.assertFalse(def.isTypeAccepted(Type.NewLine));
    Assert.assertTrue(def.isTypeIgnored(Type.NewLine));

    TurkishTokenizer all = TurkishTokenizer.ALL;
    for (Type type : Type.values()) {
      Assert.assertTrue(all.isTypeAccepted(type));
      Assert.assertFalse(all.isTypeIgnored(type));
    }

    TurkishTokenizer custom = TurkishTokenizer.builder()
        .ignoreAll()
        .acceptTypes(Type.Word, Type.Number)
        .ignoreTypes(Type.Number)
        .build();
    Assert.assertTrue(custom.isTypeAccepted(Type.Word));
    Assert.assertFalse(custom.isTypeAccepted(Type.Number));
    Assert.assertTrue(custom.isTypeIgnored(Type.Number));
  }

  @Test
  public void testTokenIteratorContract() throws IOException {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    String input = "Merhaba dünya, bugün 200 TL.";

    // Calling hasNext() repeatedly must not skip tokens.
    Iterator<Token> it1 = t.getTokenIterator(input);
    List<Token> fromRepeatedHasNext = new ArrayList<>();
    while (it1.hasNext()) {
      Assert.assertTrue(it1.hasNext());
      Assert.assertTrue(it1.hasNext());
      fromRepeatedHasNext.add(it1.next());
    }
    Assert.assertFalse(it1.hasNext());
    Assert.assertEquals(t.tokenize(input).toString(), fromRepeatedHasNext.toString());

    // Calling next() after exhaustion must throw NoSuchElementException.
    try {
      it1.next();
      Assert.fail("Expected NoSuchElementException");
    } catch (NoSuchElementException expected) {
      // expected
    }

    // Calling next() directly without calling hasNext() first must still work.
    Iterator<Token> it2 = t.getTokenIterator(new StringReader(input));
    List<Token> fromDirectNext = new ArrayList<>();
    for (int i = 0; i < fromRepeatedHasNext.size(); i++) {
      fromDirectNext.add(it2.next());
    }
    Assert.assertFalse(it2.hasNext());
    Assert.assertEquals(fromRepeatedHasNext.toString(), fromDirectNext.toString());
  }

  @Test
  public void testSingleLetterStemsAndHyphenatedWords() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchToken(t, "O'na", Type.WordWithSymbol, "O'na");
    matchToken(t, "O'nu", Type.WordWithSymbol, "O'nu");
    matchToken(t, "A'dan", Type.WordWithSymbol, "A'dan");
    matchToken(t, "Z'ye", Type.WordWithSymbol, "Z'ye");
    matchToken(t, "Kur'an'da", Type.WordWithSymbol, "Kur'an'da");
    matchToken(t, "check-up'ı", Type.WordWithSymbol, "check-up'ı");
    matchToken(t, "doğu-güney-batı", Type.WordWithSymbol, "doğu-güney-batı");
    matchToken(t, "kuzey-doğu'dan", Type.WordWithSymbol, "kuzey-doğu'dan");
    matchSentences(t, "O'na A'dan Z'ye her şeyi anlattım.", "O'na A'dan Z'ye her şeyi anlattım .");
  }

  @Test
  public void testLowercaseWordsCollidingWithAbbreviations() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchSentences(t, "Onu hemen bul.", "Onu hemen bul .");
    matchSentences(t, "Bunu kendin gör.", "Bunu kendin gör .");
    matchSentences(t, "Sofrayı kur.", "Sofrayı kur .");
    matchSentences(t, "Ekmeği ikiye böl.", "Ekmeği ikiye böl .");
    matchSentences(t, "Kapıyı it.", "Kapıyı it .");
    matchSentences(t, "Ceketini as.", "Ceketini as .");
    matchSentences(t, "Ellerini cebine sok.", "Ellerini cebine sok .");
    matchSentences(t, "Elindeki sadece bir top.", "Elindeki sadece bir top .");
    matchSentences(t, "Elindeki sadece bir yay.", "Elindeki sadece bir yay .");

    // Capitalized abbreviations and genuine lowercase abbreviations still remain single tokens:
    matchSentences(t, "Atatürk Bul. ve Gül Sok. kesişimi.", "Atatürk Bul. ve Gül Sok. kesişimi .");
    matchSentences(t, "Arş. Gör. Ahmet Bey geldi.", "Arş. Gör. Ahmet Bey geldi .");
    matchSentences(t, "Elma, armut vb. meyveler.", "Elma , armut vb. meyveler .");
    matchSentences(t, "19. yy. sonlarında.", "19. yy. sonlarında .");
    matchSentences(t, "Ücreti 200 TL. olarak belirlendi.", "Ücreti 200 TL. olarak belirlendi .");
  }

  @Test
  public void testRomanNumeralsVsAcronymsAndInitials() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;

    // Valid Roman numerals:
    matchToken(t, "I", Type.RomanNumeral, "I");
    matchToken(t, "IV", Type.RomanNumeral, "IV");
    matchToken(t, "XVI.", Type.RomanNumeral, "XVI.");
    matchToken(t, "XXI.", Type.RomanNumeral, "XXI.");
    matchToken(t, "XXIV.", Type.RomanNumeral, "XXIV.");
    matchToken(t, "XXIV'ten", Type.RomanNumeral, "XXIV'ten");

    // Single-letter initials that are not Roman numerals I, V, X:
    matchToken(t, "M.", Type.AbbreviationWithDots, "M.");
    matchToken(t, "C.", Type.AbbreviationWithDots, "C.");
    matchToken(t, "D.", Type.AbbreviationWithDots, "D.");
    matchToken(t, "L.", Type.AbbreviationWithDots, "L.");
    matchSentences(t, "M. Kemal Atatürk", "M. Kemal Atatürk");

    // Common acronyms composed of [ILVCDMX] letters:
    matchToken(t, "CD", Type.Word, "CD");
    matchToken(t, "CD'ye", Type.WordWithSymbol, "CD'ye");
    matchToken(t, "DVD", Type.Word, "DVD");
    matchToken(t, "DVD'de", Type.WordWithSymbol, "DVD'de");
    matchToken(t, "LCD", Type.Word, "LCD");
    matchToken(t, "XML", Type.Word, "XML");
    matchToken(t, "CV", Type.Word, "CV");
    matchToken(t, "DM", Type.Word, "DM");
    matchToken(t, "ML", Type.Word, "ML");
  }

  @Test
  public void testTimeVsDecimalNumbers() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;

    // Valid 24-hour times:
    matchToken(t, "00:00", Type.Time, "00:00");
    matchToken(t, "23:59", Type.Time, "23:59");
    matchToken(t, "10.20'de", Type.Time, "10.20'de");
    matchToken(t, "10:20:53", Type.Time, "10:20:53");
    matchToken(t, "10.20.00'da", Type.Time, "10.20.00'da");

    // Numbers outside 24-hour/60-minute bounds must be Number, not Time:
    matchToken(t, "24.30", Type.Number, "24.30");
    matchToken(t, "29.59", Type.Number, "29.59");
    matchToken(t, "12.75", Type.Number, "12.75");
    matchToken(t, "95.63'ten", Type.Number, "95.63'ten");
  }

  @Test
  public void testExtendedPunctuationAndCurrencySymbols() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchSentences(t, "İstanbul–Ankara yolu", "İstanbul – Ankara yolu");
    matchSentences(t, "geldi—ama gitmedi", "geldi — ama gitmedi");
    matchSentences(t, "Fiyatı 100₺, 50€ veya 40£ oldu.", "Fiyatı 100 ₺ , 50 € veya 40 £ oldu .");
    matchSentences(t, "x < y ve y > z", "x < y ve y > z");
    matchSentences(t, "Hava 25°C civarında.", "Hava 25 ° C civarında .");
    matchSentences(t, "Ali|Veli~Test", "Ali | Veli ~ Test");
    matchSentences(t, "Merhaba\u00a0dünya", "Merhaba dünya");
  }

  @Test
  public void testEmailAndUrlEdgeCases() {
    TurkishTokenizer t = TurkishTokenizer.DEFAULT;
    matchToken(t, "a@b.com", Type.Email, "a@b.com");
    matchToken(t, "first.middle.last@sub.example.co.uk", Type.Email,
        "first.middle.last@sub.example.co.uk");
    matchToken(t, "user-name+tag@my-domain.org'dan", Type.Email,
        "user-name+tag@my-domain.org'dan");
    matchSentences(t, "Bana a@b.com.", "Bana a@b.com .");

    // URLs at sentence end must not swallow the sentence-ending period:
    matchSentences(t, "Siteye https://www.google.com.tr. adresinden",
        "Siteye https://www.google.com.tr . adresinden");
    matchSentences(t, "Adresi www.fo.bar.", "Adresi www.fo.bar .");
    matchToken(t, "http://localhost:8080/api/v1?q=a%20b#top", Type.URL,
        "http://localhost:8080/api/v1?q=a%20b#top");
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
