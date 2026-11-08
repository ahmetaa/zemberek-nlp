package zemberek.tokenization;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TurkishSentenceExtractorTest {

  static List<String> getSentences(String pipeDelimited) {
    return Splitter.on('|').trimResults().splitToList(pipeDelimited);
  }

  @Test
  public void singletonAccessShouldNotThrowException() throws IOException {
    TurkishSentenceExtractor.DEFAULT.fromParagraph("hello");
  }

  @Test
  public void shouldExtractSentences1() throws IOException {
    String test = "Merhaba Dünya.| Nasılsın?";
    List<String> expected = getSentences(test);

    Assert.assertEquals(expected,
        TurkishSentenceExtractor.DEFAULT.fromParagraph(test.replace("|", "")));
  }

  @Test
  public void shouldExtractSingleSentences() throws IOException {
    String test = "Merhaba Dünya.";
    List<String> expected = getSentences(test);

    Assert.assertEquals(expected,
        TurkishSentenceExtractor.DEFAULT.fromParagraph(test.replace("|", "")));
  }

  @Test
  public void shouldExtractSentencesSecondDoesNotEndWithDot() throws IOException {
    String test = "Merhaba Dünya.| Nasılsın";
    List<String> expected = getSentences(test);

    Assert.assertEquals(expected,
        TurkishSentenceExtractor.DEFAULT.fromParagraph(test.replace("|", "")));
  }

  @Test
  public void shouldReturnDotForDot() throws IOException {
    List<String> expected = getSentences(".");
    Assert.assertEquals(expected, TurkishSentenceExtractor.DEFAULT.fromParagraph("."));
  }

  @Test
  public void shouldReturn0ForEmpty() throws IOException {
    Assert.assertEquals(0, TurkishSentenceExtractor.DEFAULT.fromParagraph("").size());
  }

  @Test
  public void extractFromDocument() throws IOException {
    Assert.assertEquals("Merhaba!|Bugün 2. köprü Fsm.'de trafik vardı.|değil mi?",
        markBoundariesDocument("Merhaba!\n Bugün 2. köprü Fsm.'de trafik vardı.değil mi?\n"));
    Assert.assertEquals("Ali|gel.",
        markBoundariesDocument("Ali\n\n\rgel.\n"));
    Assert.assertEquals("Ali gel.|Merhaba|Ne haber?",
        markBoundariesDocument("\n\nAli gel. Merhaba\n\rNe haber?"));

  }

  private String markBoundariesDocument(String input) {
    List<String> list = TurkishSentenceExtractor.DEFAULT.fromDocument(input);
    return Joiner.on("|").join(list);
  }


  private String markBoundariesParagraph(String input) {
    List<String> list = TurkishSentenceExtractor.DEFAULT.fromParagraph(input);
    return Joiner.on("|").join(list);
  }

  private String markBoundariesParagraph(TurkishSentenceExtractor extractor, String input) {
    List<String> list = extractor.fromParagraph(input);
    return Joiner.on("|").join(list);
  }

  @Test
  public void testSimpleSentence() throws IOException {
    Assert.assertEquals("Merhaba!|Bugün 2. köprü Fsm.'de trafik vardı.|değil mi?",
        markBoundariesParagraph("Merhaba! Bugün 2. köprü Fsm.'de trafik vardı.değil mi?"));
    Assert.assertEquals("Prof. Dr. Veli Zambur %2.5 lik enflasyon oranini begenmemis!",
        markBoundariesParagraph("Prof. Dr. Veli Zambur %2.5 lik enflasyon oranini begenmemis!"));
    Assert.assertEquals("Ali gel.",
        markBoundariesParagraph("Ali gel."));
    Assert.assertEquals("Ali gel.|Okul acildi!",
        markBoundariesParagraph("Ali gel. Okul acildi!"));
    Assert.assertEquals("Ali gel.|Okul acildi!",
        markBoundariesParagraph("Ali gel. Okul acildi!"));
    Assert.assertEquals("Ali gel...|Okul acildi.",
        markBoundariesParagraph("Ali gel... Okul acildi."));
    Assert.assertEquals("Tam 1.000.000 papeli cebe atmislar...",
        markBoundariesParagraph("Tam 1.000.000 papeli cebe atmislar..."));
    Assert.assertEquals("16. yüzyılda?|Dr. Av. Blah'a gitmiş.",
        markBoundariesParagraph("16. yüzyılda? Dr. Av. Blah'a gitmiş."));
    Assert.assertEquals("Ali gel.|Okul açıldı...|sınavda 2. oldum.",
        markBoundariesParagraph("Ali gel. Okul açıldı... sınavda 2. oldum."));
  }

  @Test
  public void shouldReturn0ForEmptyff() {
    List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraph("");
    Assert.assertEquals(0, sentences.size());
  }

  @Test
  public void testDoubleQuotes() throws IOException {

    TurkishSentenceExtractor e = TurkishSentenceExtractor
        .builder()
        .doNotSplitInDoubleQuotes()
        .useDefaultModel().build();

    Assert.assertEquals(
        "\"Merhaba! Bugün hava çok güzel. Ne dersin?\" dedi tavşan.|Havucu kemirirken.",
        markBoundariesParagraph(
            e,
            "\"Merhaba! Bugün hava çok güzel. Ne dersin?\" dedi tavşan. Havucu kemirirken."));

    Assert.assertEquals(
        "\"Buna hakkı yok!\" diye öfkeyle konuşmaya başladı Baba Kurt.",
        markBoundariesParagraph(
            e, "\"Buna hakkı yok!\" diye öfkeyle konuşmaya başladı Baba Kurt."));
  }

  @Test
  public void testAcronymCollisionWordsAndInflectedNumbers() {
    Assert.assertEquals("İstanbul çok güzel bir şehir.|Herkes görmeli.",
        markBoundariesParagraph("İstanbul çok güzel bir şehir. Herkes görmeli."));
    Assert.assertEquals("Bana yardım et.|Çok zor durumdayım.",
        markBoundariesParagraph("Bana yardım et. Çok zor durumdayım."));
    Assert.assertEquals("Cevabı kendin bul.|Sonra konuşalım.",
        markBoundariesParagraph("Cevabı kendin bul. Sonra konuşalım."));
    Assert.assertEquals("Burada biraz kal.|Hemen döneceğim.",
        markBoundariesParagraph("Burada biraz kal. Hemen döneceğim."));
    Assert.assertEquals("Topu bana at.|Hemen koş.",
        markBoundariesParagraph("Topu bana at. Hemen koş."));
    Assert.assertEquals("Bu çok değerli bir taş.|Müzeye konmalı.",
        markBoundariesParagraph("Bu çok değerli bir taş. Müzeye konmalı."));
    Assert.assertEquals(
        "Telpa 06.01.2014'de teslim aldığı ürünü 17.02.2014'de anca yolladı.",
        markBoundariesParagraph(
            "Telpa 06.01.2014'de teslim aldığı ürünü 17.02.2014'de anca yolladı."));
    Assert.assertEquals("Fiyatı oldukça cazip 49.90'dan 19.90'a indirilmiş.",
        markBoundariesParagraph("Fiyatı oldukça cazip 49.90'dan 19.90'a indirilmiş."));
    Assert.assertEquals(
        "Uçaklarımızın zamanında kalkma oranı yüzde 95.63.|Fazla bagaj konusunda taviz vermiyoruz.",
        markBoundariesParagraph(
            "Uçaklarımızın zamanında kalkma oranı yüzde 95.63. Fazla bagaj konusunda taviz vermiyoruz."));
    Assert.assertEquals("10 TL. dedi görevli.",
        markBoundariesParagraph("10 TL. dedi görevli."));
  }

}
