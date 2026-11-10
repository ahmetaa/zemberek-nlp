package zemberek.tokenization;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.tokenization.Token.Type;

public class TurkishDetokenizerTest {

  private void assertRoundTrip(String expected) {
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    List<String> tokenStrings = tokenizer.tokenizeToStrings(expected);
    Assert.assertEquals(expected, tokenizer.detokenize(tokenStrings));

    List<Token> tokens = tokenizer.tokenize(expected);
    Assert.assertEquals(expected, tokenizer.detokenizeTokens(tokens));
  }

  @Test
  public void testBasicPunctuationAndIssue181Example() {
    assertRoundTrip("Merhaba, nasılsın?");
    assertRoundTrip("Prof. Dr. Ahmet'e git! dedi Av. Mehmet.");
    assertRoundTrip("Saat, 10:20 ile 00:59 arasında.");
    assertRoundTrip("Elma, armut vb. meyveler; hepsi taze...");
    assertRoundTrip("Merhaba, Kaya Ivır ve Tunç Zıvır…");
  }

  @Test
  public void testQuotesBracketsAndApostropheSuffixes() {
    assertRoundTrip("(Ali gel.)");
    assertRoundTrip("F-16'yı, (H1N1) H1N1'den.");
    assertRoundTrip("\"Ali'ye\", gel!...");
    assertRoundTrip("Hey \"Ali\" gel.");
    assertRoundTrip("\"Soner boyle dedi\"");
    assertRoundTrip("\"Soner\"'e boyle dedi");
    assertRoundTrip("(2020)'de ve [TÜBİTAK]'ın projesi.");
    assertRoundTrip("«Çalıkuşu»'nu ve “Yaban”’ı okudum.");
    assertRoundTrip("‘foo'fo’");
    assertRoundTrip("'foo'");
    assertRoundTrip("Ali \"Veli 'gel' dedi\" diye anlattı.");
    assertRoundTrip("(\"Merhaba\")");
  }

  @Test
  public void testCurrenciesUnitsSlashesAndIronyMarkers() {
    assertRoundTrip("Fiyatı 100₺, 50€ veya $40 oldu.");
    assertRoundTrip("Hava 25°C civarında.");
    assertRoundTrip("Hava 25°C'de (yaklaşık %60 nem) ölçüldü.");
    assertRoundTrip("Sıcaklık 25°C.");
    assertRoundTrip("Açı 90° oldu.");
    assertRoundTrip("Hız sınırı 120 km/sa olarak belirlendi.");
    assertRoundTrip("Çok zeki (!) birisi, değil mi (?) acaba?");
    assertRoundTrip("Zemberek™ ve Google® ürünleri.");
    Assert.assertEquals("%50 indirim", TurkishDetokenizer.DEFAULT.detokenize(
        Arrays.asList("%", "50", "indirim")));
  }

  @Test
  public void testAllTokenizerAndLineBreaks() {
    String raw = "Hey  \nAli\tnaber\n";
    List<Token> allTokens = TurkishTokenizer.ALL.tokenize(raw);
    Assert.assertEquals(raw, TurkishTokenizer.ALL.detokenizeTokens(allTokens));

    TurkishTokenizer keepNewlines = TurkishTokenizer.builder()
        .acceptAll()
        .ignoreTypes(Type.SpaceTab)
        .build();
    List<Token> tokensWithNewlines = keepNewlines.tokenize("Merhaba,\nnasılsın?");
    Assert.assertEquals("Merhaba,\nnasılsın?", keepNewlines.detokenizeTokens(tokensWithNewlines));
  }

  @Test
  public void testEmptyAndNullInputs() {
    Assert.assertEquals("", TurkishDetokenizer.DEFAULT.detokenize(null));
    Assert.assertEquals("", TurkishDetokenizer.DEFAULT.detokenize(Collections.emptyList()));
    Assert.assertEquals("Merhaba dünya.",
        TurkishDetokenizer.DEFAULT.detokenize(Arrays.asList("", null, "Merhaba", "dünya", ".", "")));
    Assert.assertEquals("", TurkishDetokenizer.DEFAULT.detokenizeTokens(null));
    Assert.assertEquals("", TurkishDetokenizer.DEFAULT.detokenizeTokens(Collections.emptyList()));
  }
}
