package zemberek.normalization;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;

public class TurkishSentenceNormalizerTest {

  private static TurkishSentenceNormalizer normalizer;

  @BeforeClass
  public static void setUpClass() throws Exception {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    normalizer = TurkishSentenceNormalizer.createWithDefaults(morphology);
  }

  @Test
  public void testEmptyAndWhitespaceInputs() {
    Assert.assertEquals("", normalizer.normalize(""));
    Assert.assertEquals("   ", normalizer.normalize("   "));
    Assert.assertEquals("\u00A0", normalizer.normalize("\u00A0"));
    Assert.assertEquals("merhaba dünya", normalizer.normalize("merhaba dünya"));
  }

  @Test
  public void testDelegatesToSpanLatticePerceptronNormalizer() {
    Assert.assertNotNull(normalizer.getPerceptronNormalizer());
    String input = "Slm bugun geliyomusun?";
    Assert.assertEquals(
        normalizer.getPerceptronNormalizer().normalize(input),
        normalizer.normalize(input));
    Assert.assertEquals("Selam bugün geliyor musun?", normalizer.normalize(input));
  }

  @Test
  public void testEmojisAndEmoticonsPreserved() {
    Assert.assertEquals("hediye ettim 🥰", normalizer.normalize("hediyye ettimm🥰"));
    Assert.assertEquals("hediye ettim 🥰", normalizer.normalize("hediyye etdim🥰"));
    Assert.assertEquals("güzel 👍 teşekkürler 🙏", normalizer.normalize("güzel👍teşekkürler🙏"));
    Assert.assertEquals("kötü 😡 ama ucuz", normalizer.normalize("kötü😡ama ucuz"));
    Assert.assertEquals("iyi ❤", normalizer.normalize("iyi❤"));
    Assert.assertEquals("iyi ❤️", normalizer.normalize("iyi❤️"));
    Assert.assertEquals("😂😂😂 çok güzel olmuş", normalizer.normalize("😂😂😂 cok guzel olmus"));
    Assert.assertEquals(
        "Tamam, yarın havuza gireceğim ve akşama kadar yatacağım :)",
        normalizer.normalize("Tmm, yarin havuza giricem ve aksama kadar yaticam :)"));
  }

  @Test
  public void testElongationsNumeralsAndUnitSplits() {
    Assert.assertEquals("evet", normalizer.normalize("evett"));
    Assert.assertEquals("saat 3'te", normalizer.normalize("saat 3 de"));
    Assert.assertEquals("saat 3'te.", normalizer.normalize("saat 3 de."));
    Assert.assertEquals("2007'den", normalizer.normalize("2007 den"));
    Assert.assertEquals("4'ten", normalizer.normalize("4 den"));
    Assert.assertEquals("5'inci", normalizer.normalize("5 inci"));
    Assert.assertEquals("2'nci", normalizer.normalize("2 inci"));
    Assert.assertEquals("3 ile 5", normalizer.normalize("3 ile 5"));
    Assert.assertEquals("2 ya da 3", normalizer.normalize("2 ya da 3"));
    Assert.assertEquals("5 kg elma aldım", normalizer.normalize("5kg elma aldım"));
    Assert.assertEquals("10 km yol", normalizer.normalize("10km yol"));
  }

  @Test
  public void testCapitalizedForeignAndProperNounProtection() {
    Assert.assertEquals(
        "Yüküm Tunis limanından Jeddah ve Rades üzerinden gidecek",
        normalizer.normalize("Yüküm Tunis limanından Jeddah ve Rades üzerinden gidecek"));
    Assert.assertEquals(
        "iki palet Goods hazır",
        normalizer.normalize("iki palet Goods hazır"));
    Assert.assertEquals(
        "Yüküm İzmir limanından gidecek",
        normalizer.normalize("Yüküm Izmir limanından gidecek"));
  }

  @Test
  public void testInformalCliticsDeasciificationAndTypos() {
    Assert.assertEquals("mesajı geldi", normalizer.normalize("msajı geldi"));
    Assert.assertEquals("yine de", normalizer.normalize("yınede"));
    Assert.assertEquals("Ankara geldi", normalizer.normalize("ankra geldi"));
    Assert.assertEquals("döküyorsunuz", normalizer.normalize("dökuyorsunuz"));
    Assert.assertEquals("söküyor", normalizer.normalize("söküooo"));
    Assert.assertEquals(
        "gerçek mi bu? Yuh! Artık unutulması bile beklenmiyor",
        normalizer.normalize("gercek mı bu? Yuh! Artık unutulması bile beklenmiyo"));
    Assert.assertEquals(
        "yok hocam kesinlikle öyle bir şey yok",
        normalizer.normalize("yok hocam kesınlıkle oyle birşey yok"));
  }
}
