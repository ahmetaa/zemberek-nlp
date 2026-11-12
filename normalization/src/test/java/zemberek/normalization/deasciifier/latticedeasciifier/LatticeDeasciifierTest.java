package zemberek.normalization.deasciifier.latticedeasciifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.lm.compression.MultiFileUncompressedLm;
import zemberek.lm.compression.UncompressedToSmoothLmConverter;
import zemberek.lm.compression.UncompressedToSmoothLmConverter.NgramDataBlock;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.normalization.TurkishSentenceNormalizer;

public class LatticeDeasciifierTest {

  private static LatticeDeasciifier defaultDeasciifier;
  private static LatticeDeasciifier singleInjectedDeasciifier;
  private static LatticeDeasciifier dualInjectedDeasciifier;
  private static TurkishMorphology sharedStrictMorph;

  @BeforeClass
  public static void setUpOnce() {
    defaultDeasciifier = LatticeDeasciifier.createWithDefaults();
    sharedStrictMorph = TurkishMorphology.createWithDefaults();
    TurkishMorphology sharedInformalAsciiMorph =
        TurkishMorphology.builder()
            .setLexicon(RootLexicon.getDefault())
            .ignoreDiacriticsInAnalysis()
            .useInformalAnalysis()
            .build();
    singleInjectedDeasciifier = LatticeDeasciifier.createWithDefaults(sharedStrictMorph);
    dualInjectedDeasciifier =
        LatticeDeasciifier.createWithDefaults(sharedStrictMorph, sharedInformalAsciiMorph);
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
        "Rüzgâr sert esiyor ama hâlâ dışarıda yürüyüş yapan insanlar var.",
        defaultDeasciifier.deasciify(
            "Ruzgar sert esiyor ama hala disarida yuruyus yapan insanlar var."));
    assertEquals(
        "Şirket bu yıl geçen yıla göre çok daha yüksek kâr elde etti.",
        defaultDeasciifier.deasciify(
            "Sirket bu yil gecen yila gore cok daha yuksek kar elde etti."));
    assertEquals(
        "Şirketin yıllık kâr oranı beklentileri aştı.",
        defaultDeasciifier.deasciify("Sirketin yillik kar orani beklentileri asti."));
    // Case-sensitive separation between mid-sentence ProperNoun ("Şişli") and lowercase ("sisli"):
    assertEquals(
        "Sabahları vadi çok sisli oluyor ama Şişli meydanı kalabalıktı.",
        defaultDeasciifier.deasciify(
            "Sabahlari vadi cok sisli oluyor ama Sisli meydani kalabalikti."));
    // Lowercase Turkish "avı" vs uppercase file-extension acronym "AVI":
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
  public void handlesNumeralsDistributivesElongationsPartialAsciiAndAllCaps() {
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
        "TÜRKİYE CUMHURİYETİ MERKEZ BANKASI FAİZ KARARINI AÇIKLADI.",
        defaultDeasciifier.deasciify(
            "TURKIYE CUMHURIYETI MERKEZ BANKASI FAIZ KARARINI ACIKLADI."));
  }

  @Test
  public void rescuesTurkishLocaleCorruptedDotlessI() {
    assertEquals(
        "istanbul'da hava bugün çok güzel.",
        defaultDeasciifier.deasciify("ıstanbul'da hava bugun cok guzel."));
    assertEquals(
        "iyi günler dilerim, nasılsınız?",
        defaultDeasciifier.deasciify("ıyı gunler dılerım, nasılsınız?"));
  }

  @Test
  public void selfContainedSingleInjectedAndDualInjectedModesMatchExactly() {
    List<String> inputs =
        List.of(
            "Turkiye'nin dort bir yanindan gelen katilimcilar Cukurova'daki toplantida konustu.",
            "TUIK'in acikladigi OTV oranlari hakkinda MIT raporu yayimlandi.",
            "Cayi tazeledim, sana da koyayim mi?",
            "Yarismayi 6'sar kisilik gruplarla tamamlayan sporcu 00.00'dan itibaren dinlendi.");
    for (String in : inputs) {
      String expected = defaultDeasciifier.deasciify(in);
      assertEquals(expected, singleInjectedDeasciifier.deasciify(in));
      assertEquals(expected, dualInjectedDeasciifier.deasciify(in));
    }
  }

  @Test
  public void integratesCleanlyWithTurkishSentenceNormalizer() throws Exception {
    Path tempRoot = Files.createTempDirectory("zemberek_norm_test_");
    try {
      Files.writeString(
          tempRoot.resolve("lookup-from-graph"), "bisi=bir şey\n", StandardCharsets.UTF_8);
      Files.writeString(tempRoot.resolve("ascii-map"), "cok=çok\n", StandardCharsets.UTF_8);
      Files.writeString(tempRoot.resolve("split"), "birsey=bir şey\n", StandardCharsets.UTF_8);

      String tinyArpa =
          """

          \\data\\
          ngram 1= 12
          ngram 2= 8

          \\1-grams:
          -1.0 <s> -0.3
          -1.0 </s> 0.0
          -1.2 istanbul'da -0.2
          -1.2 hava -0.2
          -1.1 çok -0.2
          -1.2 güzel -0.2
          -1.2 yarın -0.2
          -1.2 kütüphaneye -0.2
          -1.2 gideceğim -0.2
          -1.2 gelecek -0.2
          -1.2 misiniz -0.2
          -2.5 <unk> 0.0

          \\2-grams:
          -0.4 <s> istanbul'da
          -0.4 istanbul'da hava
          -0.4 hava çok
          -0.4 çok güzel
          -0.4 güzel yarın
          -0.4 yarın kütüphaneye
          -0.4 kütüphaneye gideceğim
          -0.3 gelecek misiniz

          \\end\\
          """;
      Path arpaPath = tempRoot.resolve("tiny.arpa");
      Path lmWorkDir = tempRoot.resolve("lm_work");
      Files.createDirectories(lmWorkDir);
      Files.writeString(arpaPath, tinyArpa, StandardCharsets.UTF_8);
      Path lmPath = tempRoot.resolve("tiny.slm");
      UncompressedToSmoothLmConverter converter =
          new UncompressedToSmoothLmConverter(lmPath.toFile(), lmWorkDir.toFile());
      converter.convertSmall(
          MultiFileUncompressedLm.generate(arpaPath.toFile(), lmWorkDir.toFile(), "utf-8", 4)
              .getLmDir(),
          new NgramDataBlock(8, 8, 8));

      // LatticeDeasciifier is enabled by default in TurkishSentenceNormalizer:
      TurkishSentenceNormalizer normalizer =
          new TurkishSentenceNormalizer(sharedStrictMorph, tempRoot, lmPath);
      normalizer.setAlwaysApplyDeasciifier(true);

      // Verifies uppercase 'I' -> 'ı' / 'İ' -> 'i' is preserved before normalizer lowercasing,
      // informal future suffix ("gidicem" -> "gideceğim") is normalized, and glued question clitic splits:
      String normalized =
          normalizer.normalize(
              "ISTANBUL'DA hava cok guzel yarin kutuphaneye gidicem gelecekmisiniz");
      assertEquals(
          "istanbul'da hava çok güzel yarın kütüphaneye gideceğim gelecek misiniz", normalized);

      // Callers can switch to legacy pattern-table Deasciifier and back if needed:
      normalizer.useLegacyDeasciifier().useLatticeDeasciifier();
      assertEquals(
          "istanbul'da hava çok güzel yarın kütüphaneye gideceğim gelecek misiniz",
          normalizer.normalize(
              "ISTANBUL'DA hava cok guzel yarin kutuphaneye gidicem gelecekmisiniz"));
    } finally {
      try (var walk = Files.walk(tempRoot)) {
        walk.sorted(Comparator.reverseOrder())
            .forEach(
                p -> {
                  try {
                    Files.deleteIfExists(p);
                  } catch (Exception ignored) {
                  }
                });
      }
    }
  }

  @Test
  public void supportsMiniatureTrainingAndCompressedSerializationRoundTrip() throws Exception {
    List<String> base =
        List.of(
            "Bu önemli sırrı kimseye sakın söyleme!",
            "Deniz bugün her zamankinden daha sakin görünüyordu.",
            "Üniversite öğrencileri bilimsel araştırmalar için kütüphanede buluştu.");
    List<String> miniCorpus = new ArrayList<>(base.size() * 12);
    for (int i = 0; i < 12; i++) {
      miniCorpus.addAll(base);
    }

    MorphologicalCandidateGenerator morphGen = new MorphologicalCandidateGenerator(sharedStrictMorph);
    LatticeDeasciifierTrainer.Config cfg = new LatticeDeasciifierTrainer.Config();
    cfg.charEpochs = 2;
    cfg.sentenceEpochs = 3;

    DeasciifierModel trained = LatticeDeasciifierTrainer.train(miniCorpus, morphGen, cfg);
    Path tempGz = Files.createTempFile("lattice_mini_", ".bin.gz");
    try {
      trained.saveCompressed(tempGz);
      assertTrue(Files.size(tempGz) > 100);
      DeasciifierModel reloaded = DeasciifierModel.load(tempGz);
      LatticeDeasciifier mini = new LatticeDeasciifier(reloaded, sharedStrictMorph);
      assertEquals(
          "Üniversite öğrencileri bilimsel araştırmalar için kütüphanede buluştu.",
          mini.deasciify("Universite ogrencileri bilimsel arastirmalar icin kutuphanede bulustu."));
    } finally {
      Files.deleteIfExists(tempGz);
    }
  }
}
