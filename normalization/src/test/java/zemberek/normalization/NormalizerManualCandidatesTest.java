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

public class NormalizerManualCandidatesTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path lmPath;
  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-normalizer-manual-test");
    dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);

    // Populate lookup-from-graph with noisy entries from Issue #228 to verify manual override.
    Files.write(
        dataRoot.resolve("lookup-from-graph"),
        Arrays.asList("göster=göstersene", "listele=listelesene"),
        StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("ascii-map"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("split"), Collections.emptyList(), StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("bigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=16",
            "ngram 2=5",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.5000 en\t-0.1",
            "-1.5000 ucuz\t-0.1",
            "-1.5000 arabayı\t-0.1",
            "-1.5000 yolu\t-0.1",
            "-2.0000 göster\t-0.1",
            "-0.5000 göstersene\t-0.1",
            "-2.0000 listele\t-0.1",
            "-0.5000 listelesene\t-0.1",
            "-1.5000 kitabı\t-0.1",
            "-2.0000 oku\t-0.1",
            "-0.5000 okusana\t-0.1",
            "-1.5000 ne\t-0.1",
            "-1.5000 yaparsak\t-0.1",
            "",
            "\\2-grams:",
            "-0.3000 arabayı göstersene",
            "-0.3000 yolu listelesene",
            "-0.3000 kitabı okusana",
            "-0.5000 en ucuz",
            "-0.5000 ne yaparsak",
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
            "en [P:Adv]",
            "ucuz [P:Adj]",
            "araba",
            "yol",
            "göstermek",
            "listelemek",
            "kitap [A:Voicing]",
            "okumak",
            "ne [P:Pron,Ques]",
            "yapmak [A:Aorist_A]")
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
  public void testImperativeVerbsAndMultiWordReplacement_Issue228() throws IOException {
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);
    Assert.assertEquals("en ucuz arabayı göster", normalizer.normalize("en ucuz arabayı göster"));
    Assert.assertEquals("en ucuz arabayı göster", normalizer.normalize("en ucuz arabayi goster"));
    Assert.assertEquals("en ucuz yolu listele", normalizer.normalize("en ucuz yolu listele"));
    Assert.assertEquals("kitabı oku", normalizer.normalize("kitabı oku"));
    Assert.assertEquals("ne yaparsak", normalizer.normalize("naparsak"));
  }
}
