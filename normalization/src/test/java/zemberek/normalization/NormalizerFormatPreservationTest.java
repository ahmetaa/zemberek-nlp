package zemberek.normalization;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.lm.compression.MultiFileUncompressedLm;
import zemberek.lm.compression.UncompressedToSmoothLmConverter;
import zemberek.morphology.TurkishMorphology;

public class NormalizerFormatPreservationTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path lmPath;
  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-normalizer-format-test");
    dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(dataRoot.resolve("lookup-from-graph"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("ascii-map"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(
        dataRoot.resolve("split"),
        Arrays.asList("yollarıda=yolları da", "bişey=bir şey"),
        StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("bigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=21",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.5000 düzenli\t-0.1",
            "-1.5000 bir\t-0.1",
            "-1.5000 şekilde\t-0.1",
            "-1.5000 bugün\t-0.1",
            "-1.5000 hava\t-0.1",
            "-1.5000 çok\t-0.1",
            "-1.5000 güzel\t-0.1",
            "-1.5000 yolları\t-0.1",
            "-1.5000 da\t-0.1",
            "-1.5000 bozuktu\t-0.1",
            "-1.5000 şey\t-0.1",
            "-1.5000 yapmadılar\t-0.1",
            "-1.5000 soğuk\t-0.1",
            "-1.5000 savaş\t-0.1",
            "-1.5000 döneminde\t-0.1",
            "-1.5000 tuz\t-0.1",
            "-1.5000 denizi\t-0.1",
            "-1.5000 kurudu\t-0.1",
            "",
            "\\2-grams:",
            "-0.5000 düzenli bir",
            "-0.5000 bir şey",
            "",
            "\\end\\"),
        StandardCharsets.UTF_8);

    Path lmDir = tempDir.resolve("lm-uncompressed");
    MultiFileUncompressedLm.generate(arpaPath.toFile(), lmDir.toFile(), "utf-8", 4);
    lmPath = tempDir.resolve("lm.2gram.slm");
    UncompressedToSmoothLmConverter converter =
        new UncompressedToSmoothLmConverter(lmPath.toFile(), lmDir.toFile());
    converter.convertSmall(
        lmDir.toFile(),
        new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));

    morphology = TurkishMorphology.createWithDefaults();
  }

  @AfterClass
  public static void tearDownClass() throws IOException {
    if (tempDir != null && Files.exists(tempDir)) {
      Files.walk(tempDir)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  @Test
  public void preservesCleanMixedCaseAndPunctuation() {
    NormalizationFormatProjector projector = new NormalizationFormatProjector(morphology);
    String raw =
        "Guingamp, maça 8. dakikada attığı golle 1-0 önde başlasa da, arka arkaya gelen 'muhacir' gollere engel olamadı.";
    String lower =
        "guingamp , maça 8. dakikada attığı golle 1-0 önde başlasa da , arka arkaya gelen ' muhacir ' gollere engel olamadı .";
    Assert.assertEquals(raw, projector.project(raw, lower));
  }

  @Test
  public void preservesCapitalizationAndAcronymsOnModifiedTokens() {
    NormalizationFormatProjector projector = new NormalizationFormatProjector(morphology);
    Assert.assertEquals(
        "Düzenli bir şekilde. PM gönderin, ABD ve AB üyeleri Ankara'da toplandı.",
        projector.project(
            "Duzenlı bi sekılde. PM gonderin, ABD ve AB uyeleri ankara'da toplandı.",
            "düzenli bir şekilde . pm gönderin , abd ve ab üyeleri ankara'da toplandı ."));
  }

  @Test
  public void restoresUnambiguousProperNounsWithTurkishCapitalizationAndApostrophe() {
    NormalizationFormatProjector projector = new NormalizationFormatProjector(morphology);
    Assert.assertEquals(
        "Isparta'da güzel gün",
        projector.project("ısparta'da guzel gun", "ıspartada güzel gün"));
    Assert.assertEquals(
        "İstanbul'da yağmur yağıyor.",
        projector.project("istanbulda yagmur yagiyor.", "istanbulda yağmur yağıyor ."));
  }

  @Test
  public void doesNotCapitalizeCommonNounProperNameHomographs() {
    NormalizationFormatProjector projector = new NormalizationFormatProjector(morphology);
    String raw = "Soğuk savaş döneminde tuz denizi kurudu.";
    String lower = "soğuk savaş döneminde tuz denizi kurudu .";
    Assert.assertEquals(raw, projector.project(raw, lower));
  }

  @Test
  public void normalizesAllCapsAndTitleSpamWhileKeepingSentenceStartAndProperNouns() {
    NormalizationFormatProjector projector = new NormalizationFormatProjector(morphology);
    Assert.assertEquals(
        "Bugün hava çok güzel!",
        projector.project("BUGÜN HAVA ÇOK GÜZEL!", "bugün hava çok güzel !"));
    Assert.assertEquals(
        "Tebrik ederim canım Ankara'da çok sevindim.",
        projector.project(
            "Tebrik Ederim Canım ankara'da Çok Sevindim.",
            "tebrik ederim canım ankara'da çok sevindim ."));
  }

  @Test
  public void alignsSplitsAndMergesInNormalizerToggleAndStatelessMethod() throws IOException {
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);

    // Default remains lowercase space-separated tokens for backward compatibility.
    Assert.assertFalse(normalizer.isPreserveCaseAndPunctuation());
    String defaultOut = normalizer.normalize("Yollarıda bozuktu, bişey yapmadılar.");
    Assert.assertEquals("yolları da bozuktu , bir şey yapmadılar .", defaultOut);

    // Stateless formatting method works without changing toggle.
    String formatted = normalizer.normalizePreservingFormat("Yollarıda bozuktu, bişey yapmadılar.");
    Assert.assertEquals("Yolları da bozuktu, bir şey yapmadılar.", formatted);

    // Toggle updates normalize() behavior cleanly.
    normalizer.setPreserveCaseAndPunctuation(true);
    Assert.assertTrue(normalizer.isPreserveCaseAndPunctuation());
    Assert.assertEquals(
        "Yolları da bozuktu, bir şey yapmadılar.",
        normalizer.normalize("Yollarıda bozuktu, bişey yapmadılar."));
  }
}
