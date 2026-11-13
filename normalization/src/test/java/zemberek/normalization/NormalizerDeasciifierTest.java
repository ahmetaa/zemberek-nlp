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

public class NormalizerDeasciifierTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path lmPath;
  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-normalizer-deasciifier-test");
    dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(dataRoot.resolve("lookup-from-graph"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("ascii-map"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("split"), Collections.emptyList(), StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("bigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=10",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.4000 aldığından\t-0.1",
            "-1.5000 alındığından\t-0.1",
            "-1.5000 müşteriden\t-0.1",
            "-1.5000 bilgiler\t-0.1",
            "-1.5000 dolayı\t-0.1",
            "-1.5000 ışık\t-0.1",
            "-1.5000 açık\t-0.1",
            "",
            "\\2-grams:",
            "-0.5000 müşteriden bilgiler",
            "-0.5000 ışık açık",
            "",
            "\\end\\"),
        StandardCharsets.UTF_8);

    Path lmBuildDir = tempDir.resolve("lm-build");
    Files.createDirectories(lmBuildDir);
    lmPath = tempDir.resolve("bigram.slm");
    File uncompressedDir =
        MultiFileUncompressedLm.generate(arpaPath.toFile(), lmBuildDir.toFile(), "utf-8", 4)
            .getLmDir();
    UncompressedToSmoothLmConverter converter =
        new UncompressedToSmoothLmConverter(lmPath.toFile(), lmBuildDir.toFile());
    converter.convertSmall(
        uncompressedDir, new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));

    morphology = TurkishMorphology.builder()
        .setLexicon(
            "almak",
            "müşteri",
            "bilgi",
            "dolayı [P:Postp,PCAbl]",
            "ışık [A:Voicing]",
            "açık [P:Adj; A:Voicing]")
        .disableCache()
        .build();
  }

  @AfterClass
  public static void tearDownClass() throws IOException {
    if (tempDir != null && Files.exists(tempDir)) {
      Files.walk(tempDir)
          .sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try {
              Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
          });
    }
  }

  @Test
  public void testDotlessIAndMorphologicalDeasciifierGuard_Issue233() throws IOException {
    Assert.assertFalse(TurkishSentenceNormalizer.probablyRequiresDeasciifier("alndığından"));
    Assert.assertTrue(TurkishSentenceNormalizer.probablyRequiresDeasciifier("Isik"));

    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);
    String single = normalizer.normalize("alndığından");
    Assert.assertNotEquals("alndiğından", single);
    Assert.assertTrue(single.equals("aldığından") || single.equals("alındığından"));

    // Even inside a mostly-ASCII sentence where Deasciifier runs, invalid per-token deasciification
    // ("alndığından" -> "alndiğından") is reverted so spellchecker can still recover the word.
    String inAsciiSentence = normalizer.normalize("musteriden bilgiler alndığından dolayi");
    Assert.assertFalse(inAsciiSentence.contains("alndiğından"));
    Assert.assertTrue(inAsciiSentence.startsWith("müşteriden bilgiler "));
    Assert.assertTrue(inAsciiSentence.endsWith(" dolayı"));

    // ASCII uppercase 'I' at start of ASCII text still deasciifies properly.
    Assert.assertEquals("ışık açık", normalizer.normalize("Isik acik"));
  }

  @Test
  public void testPreservesValidWordsAgainstOverDeasciificationAndDiacriticStripping()
      throws IOException {
    TurkishMorphology richMorphology = TurkishMorphology.builder()
        .setLexicon(
            "on [P:Num]",
            "ön",
            "madde",
            "olmak",
            "ölmek",
            "kanı",
            "eser",
            "eş",
            "konuşmak",
            "toplantı")
        .disableCache()
        .build();
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(richMorphology, dataRoot, lmPath);

    // "Toplantıda" contains Turkish 'ı', so valid "on" must not be over-deasciified into "ön".
    Assert.assertEquals(
        "toplantıda on madde konuşuldu .",
        normalizer.normalize("Toplantıda on madde konuşuldu."));
    // Valid words with explicit diacritics ("ölmüş", "kanıydı", "eseriniz") must not be corrupted.
    Assert.assertEquals("ölmüş", normalizer.normalize("ölmüş"));
    Assert.assertEquals("kanıydı", normalizer.normalize("kanıydı"));
    Assert.assertEquals("eseriniz", normalizer.normalize("eseriniz"));

    // Verify legacy pattern-table deasciifier path also preserves valid words with preserveValidWords.
    normalizer.useLegacyDeasciifier();
    Assert.assertEquals(
        "toplantıda on madde konuşuldu .",
        normalizer.normalize("Toplantıda on madde konuşuldu."));
    Assert.assertEquals("ölmüş", normalizer.normalize("ölmüş"));
    Assert.assertEquals("kanıydı", normalizer.normalize("kanıydı"));
    Assert.assertEquals("eseriniz", normalizer.normalize("eseriniz"));
  }
}
