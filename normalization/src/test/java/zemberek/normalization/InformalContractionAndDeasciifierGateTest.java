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

public class InformalContractionAndDeasciifierGateTest {

  private static Path tempDir;
  private static TurkishSentenceNormalizer normalizer;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-contraction-gate-test");
    Path dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(
        dataRoot.resolve("lookup-from-graph"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("split"), Collections.emptyList(), StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("bigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=14",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.2000 ne\t-0.1",
            "-1.2000 haber\t-0.1",
            "-1.2000 selam\t-0.1",
            "-1.2000 merhaba\t-0.1",
            "-1.2000 değil\t-0.1",
            "-1.2000 yanlış\t-0.1",
            "-1.2000 şarj\t-0.1",
            "-1.2000 burada\t-0.1",
            "-1.2000 orada\t-0.1",
            "-1.2000 nerede\t-0.1",
            "-1.2000 orijinal\t-0.1",
            "",
            "\\2-grams:",
            "-0.5000 ne haber",
            "-0.5000 selam merhaba",
            "",
            "\\end\\"),
        StandardCharsets.UTF_8);

    Path lmBuildDir = tempDir.resolve("lm-build");
    Files.createDirectories(lmBuildDir);
    Path lmPath = tempDir.resolve("bigram.slm");
    File uncompressedDir =
        MultiFileUncompressedLm.generate(arpaPath.toFile(), lmBuildDir.toFile(), "utf-8", 4)
            .getLmDir();
    UncompressedToSmoothLmConverter converter =
        new UncompressedToSmoothLmConverter(lmPath.toFile(), lmBuildDir.toFile());
    converter.convertSmall(
        uncompressedDir, new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));

    TurkishMorphology morphology =
        TurkishMorphology.builder()
            .setLexicon(
                "bura",
                "şura",
                "ora",
                "nere [P:Pron,Ques]",
                "orijinal [P:Adj; A:InverseHarmony]",
                "orda [A:Ext]",
                "şurda [A:Ext]",
                "orjinal [A:Ext]",
                "yapmak",
                "olmak [A:Aorist_I]",
                "napalm",
                "nolu [P:Adj]",
                "ordu",
                "ne",
                "haber",
                "selam",
                "merhaba",
                "değil [P:Verb]",
                "yanlış [P:Adj]",
                "şarj [A:Voicing]")
            .disableCache()
            .build();
    normalizer = new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);
  }

  @AfterClass
  public static void tearDownClass() throws IOException {
    if (tempDir != null && Files.exists(tempDir)) {
      Files.walk(tempDir)
          .sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
              });
    }
  }

  @Test
  public void expandsColloquialVowelDropsAndExtMaskedStemsTest() {
    Assert.assertEquals(
        "burada buradan orada oradan",
        normalizer.normalize("burda burdan orda ordan"));
    Assert.assertEquals(
        "şurada şuradan nerede nereden neredesin",
        normalizer.normalize("şurda şurdan nerde nerden nerdesin"));
    Assert.assertEquals("orijinal orijinallik", normalizer.normalize("orjinal orjinallik"));
  }

  @Test
  public void expandsProductiveNeYapAndNeOlWithoutFalsePositivesTest() {
    Assert.assertEquals(
        "ne yapıyorsun ne yaptın ne oldu ne oluyor",
        normalizer.normalize("napıyon naptın noldu noluyo"));
    Assert.assertEquals("napalm nolu ordu", normalizer.normalize("napalm nolu ordu"));
  }

  @Test
  public void normalizesSmsShorthandAndCommonTyposTest() {
    Assert.assertEquals(
        "selam merhaba ne haber değil yanlış şarj",
        normalizer.normalize("slm mrb nbr deil yalnış şarz"));
  }

  @Test
  public void deasciifierGateUsesLetterRatioNotTotalLengthTest() {
    // Uppercase 'I' does not count as a Turkish diacritic; ASCII 'Isik' must trigger deasciifier.
    Assert.assertTrue(TurkishSentenceNormalizer.probablyRequiresDeasciifier("Isik"));
    // Digits and punctuation padding must not trigger deasciifier on clean Turkish text.
    Assert.assertFalse(
        TurkishSentenceNormalizer.probablyRequiresDeasciifier(
            "2024 yılında 123, 456, 789 sayıları geldi!"));
    // Pure digits/symbols require no deasciification.
    Assert.assertFalse(TurkishSentenceNormalizer.probablyRequiresDeasciifier("12345 !!!"));
    // Punctuated ASCII sentences normalize cleanly in preProcess.
    Assert.assertEquals("şurada değil .", normalizer.preProcess("Surada degil."));
  }
}
