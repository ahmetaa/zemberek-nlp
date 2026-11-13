package zemberek.normalization.deasciifier.nimbledeasciifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleDeasciifier.CircumflexMode;

public class NimbleDeasciifierTest {

  private static NimbleDeasciifier defaultDeasciifier;

  @BeforeClass
  public static void setUpOnce() {
    defaultDeasciifier = NimbleDeasciifier.createWithDefaults();
  }

  @Test
  public void disambiguatesHomographsCircumflexesAndCaseSensitiveProperNouns() {
    assertEquals(
        "Çocuklar okuldan gelene kadar evi toparlayıp çamaşırları balkondaki ipe asmam gerekiyor.",
        defaultDeasciifier.deasciify(
            "Cocuklar okuldan gelene kadar evi toparlayip camasirlari balkondaki ipe asmam gerekiyor."));
    assertEquals(
        "Bu zorlu engeli aşmam hiç kolay olmayacak.",
        defaultDeasciifier.deasciify("Bu zorlu engeli asmam hic kolay olmayacak."));
    assertEquals(
        "Derin yapay sinir ağları görüntü işlemede çığır açtı.",
        defaultDeasciifier.deasciify("Derin yapay sinir aglari goruntu islemede cigir acti."));
    assertEquals(
        "Modern bilgisayarlarda kullanılan en hızlı bellek türü hangisidir?",
        defaultDeasciifier.deasciify(
            "Modern bilgisayarlarda kullanilan en hizli bellek turu hangisidir?"));
    assertEquals(
        "Bisikletçiler zorlu Karadeniz turu için sabah erkenden pedal çevirdi.",
        defaultDeasciifier.deasciify(
            "Bisikletciler zorlu Karadeniz turu icin sabah erkenden pedal cevirdi."));
    assertEquals(
        "Bu önemli sırrı kimseye sakın söyleme!",
        defaultDeasciifier.deasciify("Bu onemli sirri kimseye sakin soyleme!"));
    assertEquals(
        "Deniz bugün her zamankinden daha sakin görünüyordu.",
        defaultDeasciifier.deasciify("Deniz bugun her zamankinden daha sakin gorunuyordu."));
    assertEquals(
        "Aradığınız kayıt sistemde mevcut mudur?",
        defaultDeasciifier.deasciify("Aradiginiz kayit sistemde mevcut mudur?"));
    assertEquals(
        "Şirketin yeni genel müdür yardımcısı bugün göreve başladı.",
        defaultDeasciifier.deasciify(
            "Sirketin yeni genel mudur yardimcisi bugun goreve basladi."));
    assertEquals(
        "Rüzgar sert esiyor ama hâlâ dışarıda yürüyüş yapan insanlar var.",
        defaultDeasciifier.deasciify(
            "Ruzgar sert esiyor ama hala disarida yuruyus yapan insanlar var."));
    assertEquals(
        "Şirket bu yıl geçen yıla göre çok daha yüksek kâr elde etti.",
        defaultDeasciifier.deasciify(
            "Sirket bu yil gecen yila gore cok daha yuksek kar elde etti."));
    assertEquals(
        "Şirketin yıllık kâr oranı beklentileri aştı.",
        defaultDeasciifier.deasciify("Sirketin yillik kar orani beklentileri asti."));
    assertEquals(
        "Sabahları vadi çok sisli oluyor ama Şişli meydanı kalabalıktı.",
        defaultDeasciifier.deasciify(
            "Sabahlari vadi cok sisli oluyor ama Sisli meydani kalabalikti."));
    assertEquals(
        "Polis insan avı başlatırken kameradaki AVI dosyasını inceledi.",
        defaultDeasciifier.deasciify(
            "Polis insan avi baslatirken kameradaki AVI dosyasini inceledi."));
  }

  @Test
  public void preservesForeignTermsUsernamesUrlsAndRestoresTurkishAcronyms() {
    assertEquals(
        "Bilgisayar açılırken BIOS ayarlarına girip RAM hızını kontrol ettim.",
        defaultDeasciifier.deasciify(
            "Bilgisayar acilirken BIOS ayarlarina girip RAM hizini kontrol ettim."));
    assertEquals(
        "TÜİK ve TÜBİTAK verileri ÖSYM ile İSKİ tarafından incelendi.",
        defaultDeasciifier.deasciify(
            "TUIK ve TUBITAK verileri OSYM ile ISKI tarafindan incelendi."));
    assertEquals(
        "II. Dünya Savaşı hakkında https://ornek.gov.tr/basvuru/urunler adresinden @ornek_hesap ve cimcime50 yazdı.",
        defaultDeasciifier.deasciify(
            "II. Dunya Savasi hakkinda https://ornek.gov.tr/basvuru/urunler adresinden @ornek_hesap ve cimcime50 yazdi."));
    assertEquals(
        "Detayları WhatsApp'tan ve GitHub üzerinden paylaştık.",
        defaultDeasciifier.deasciify("Detaylari WhatsApp'tan ve GitHub uzerinden paylastik."));
    assertEquals(
        "ABD'nin kararı sonrasında ÖTV'nin oranı AİHM'e ve TSK'nın arşivine yansıdı.",
        defaultDeasciifier.deasciify(
            "ABD'nin karari sonrasinda OTV'nin orani AIHM'e ve TSK'nin arsivine yansidi."));
    assertEquals(
        "Eski MP3'ün sesini açtık.",
        defaultDeasciifier.deasciify("Eski MP3'un sesini actik."));
  }

  @Test
  public void handlesNumeralsDistributivesElongationsPartialAsciiCliticsAndAllCaps() {
    assertEquals(
        "Yarışmayı 3'üncü sırada bitirip gruplara 6'şar kişi dağıtarak 1923'ün 100'üncü yılını kutladık.",
        defaultDeasciifier.deasciify(
            "Yarismayi 3'uncu sirada bitirip gruplara 6'sar kisi dagitarak 1923'un 100'uncu yilini kutladik."));
    assertEquals(
        "Türkiye'nin Çukurova'daki çoooook güzel bahçeleri.",
        defaultDeasciifier.deasciify("Turkiye'nin Cukurova'daki coooook guzel bahceleri."));
    assertEquals(
        "Öğrenciler kütüphanede çalışıyor.",
        defaultDeasciifier.deasciify("Öğrenciler kutuphanede çalışıyor."));
    assertEquals(
        "Çayı tazeledim, sana da koyayım mı?",
        defaultDeasciifier.deasciify("Cayi tazeledim, sana da koyayim mi?"));
    assertEquals(
        "Kuantum bilgisayarlardaki kübitlerinin durumu incelendi.",
        defaultDeasciifier.deasciify(
            "Kuantum bilgisayarlardaki kubitlerinin durumu incelendi."));
    assertEquals(
        "TÜRKİYE CUMHURİYETİ MERKEZ BANKASI FAİZ KARARINI AÇIKLADI.",
        defaultDeasciifier.deasciify(
            "TURKIYE CUMHURIYETI MERKEZ BANKASI FAIZ KARARINI ACIKLADI."));
  }

  @Test
  public void rescuesTurkishLocaleCorruptedDotlessIWhilePreservingGenuineDotlessI() {
    assertEquals(
        "istanbul'da hava bugün çok güzel.",
        defaultDeasciifier.deasciify("ıstanbul'da hava bugun cok guzel."));
    assertEquals(
        "iyi günler dilerim, nasılsınız?",
        defaultDeasciifier.deasciify("ıyı gunler dılerım, nasılsınız?"));
    assertEquals(
        "ılık su akıyor.",
        defaultDeasciifier.deasciify("ılık su akıyor."));
  }

  @Test
  public void supportsCircumflexModesAndPreservesPinnedCircumflexes() {
    assertEquals(CircumflexMode.DISAMBIGUATION_ONLY, defaultDeasciifier.circumflexMode());

    assertEquals(
        "Çocuklara güzel bir hikaye anlattı.",
        defaultDeasciifier.deasciify("Cocuklara guzel bir hikaye anlatti."));
    assertEquals(
        "Çocuklara güzel bir hikâye anlattı.",
        defaultDeasciifier.deasciify("Cocuklara guzel bir hikaye anlatti.", CircumflexMode.FULL));
    assertEquals(
        "Eski dükkanın önünde kağıt topluyordu.",
        defaultDeasciifier.deasciify("Eski dukkanin onunde kagit topluyordu."));
    assertEquals(
        "Eski dükkânın önünde kâğıt topluyordu.",
        defaultDeasciifier.deasciify("Eski dukkanin onunde kagit topluyordu.", CircumflexMode.FULL));
    assertEquals(
        "Bu yatırım şirket için oldukça kârlı görünüyor.",
        defaultDeasciifier.deasciify("Bu yatirim sirket icin oldukca karli gorunuyor."));

    NimbleDeasciifier noneMode = defaultDeasciifier.withCircumflexMode(CircumflexMode.NONE);
    assertEquals(
        "Rüzgar sert esiyor ama hala dışarıda yürüyüş yapan insanlar var.",
        defaultDeasciifier.deasciify(
            "Ruzgar sert esiyor ama hala disarida yuruyus yapan insanlar var.", CircumflexMode.NONE));
    assertEquals(
        "Şirketin yıllık kar oranı beklentileri aştı.",
        noneMode.deasciify("Sirketin yillik kar orani beklentileri asti."));
    assertEquals(
        "Rüzgâr sert esiyor ama hâlâ dışarıda yürüyüş yapan insanlar var.",
        defaultDeasciifier.deasciify(
            "Ruzgar sert esiyor ama hala disarida yuruyus yapan insanlar var.", CircumflexMode.FULL));

    for (CircumflexMode mode : CircumflexMode.values()) {
      assertEquals(
          "Rüzgârda kâğıt uçuşuyor.",
          defaultDeasciifier.deasciify("Rüzgârda kâgit ucusuyor.", mode));
    }
  }

  @Test
  public void supportsStandaloneDictionaryFreeTrainingAndCompactModelRoundTrip()
      throws Exception {
    Path bundled = Paths.get("src/main/resources/deasciifier/nimble_deasciifier.bin");
    if (!Files.exists(bundled)) {
      bundled = Paths.get("experiment/src/main/resources/deasciifier/nimble_deasciifier.bin");
    }
    assertTrue("Bundled model must exist: " + bundled, Files.exists(bundled));
    long size = Files.size(bundled);
    assertTrue("Bundled nimble_deasciifier.bin must be < 900 KB, was " + size, size < 900_000L);

    List<String> base =
        List.of(
            "Bu önemli sırrı kimseye sakın söyleme!",
            "Deniz bugün her zamankinden daha sakin görünüyordu.",
            "Üniversite öğrencileri bilimsel araştırmalar için kütüphanede buluştu.");
    List<String> miniCorpus = new ArrayList<>(base.size() * 12);
    for (int i = 0; i < 12; i++) {
      miniCorpus.addAll(base);
    }

    NimbleDeasciifierTrainer.Config cfg = new NimbleDeasciifierTrainer.Config();
    cfg.charEpochs = 2;
    cfg.sentenceEpochs = 3;
    cfg.charMaxFeatures = 4_096;
    cfg.sentenceMaxFeatures = 4_096;

    NimbleModel trained = NimbleDeasciifierTrainer.train(miniCorpus, null, cfg);
    Path tempBin = Files.createTempFile("nimble_mini_", ".bin");
    try {
      trained.save(tempBin);
      assertTrue(Files.size(tempBin) > 100);
      NimbleModel reloaded = NimbleModel.load(tempBin);
      NimbleDeasciifier mini = new NimbleDeasciifier(reloaded);
      assertEquals(
          "Üniversite öğrencileri bilimsel araştırmalar için kütüphanede buluştu.",
          mini.deasciify("Universite ogrencileri bilimsel arastirmalar icin kutuphanede bulustu."));
    } finally {
      Files.deleteIfExists(tempBin);
    }
  }

  @Test
  public void hasZeroMorphologyOrLmDependencies() throws Exception {
    Path srcDir = Paths.get("src/main/java/zemberek/normalization/deasciifier/nimbledeasciifier");
    if (!Files.exists(srcDir)) {
      srcDir =
          Paths.get(
              "experiment/src/main/java/zemberek/normalization/deasciifier/nimbledeasciifier");
    }
    assertTrue(Files.isDirectory(srcDir));
    try (Stream<Path> files = Files.list(srcDir)) {
      for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        String content = Files.readString(p, StandardCharsets.UTF_8);
        assertFalse(
            "Unexpected zemberek.morphology import in " + p,
            content.contains("zemberek.morphology"));
        assertFalse("Unexpected zemberek.lm import in " + p, content.contains("zemberek.lm"));
        assertFalse(
            "Unexpected latticedeasciifier dependency in " + p,
            content.contains("latticedeasciifier"));
      }
    }
  }
}
