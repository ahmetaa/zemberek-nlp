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

public class NormalizerCrashTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path trigramLmPath;
  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-normalizer-crash-test");
    dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(dataRoot.resolve("lookup-from-graph"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("ascii-map"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("split"), Collections.emptyList(), StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("trigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=5",
            "ngram 2=2",
            "ngram 3=1",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 <s>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.2000 merhaba\t0.0",
            "-1.2000 dünya\t0.0",
            "",
            "\\2-grams:",
            "-0.5000 <s> merhaba\t0.0",
            "-0.5000 merhaba dünya\t0.0",
            "",
            "\\3-grams:",
            "-0.3000 <s> merhaba dünya",
            "",
            "\\end\\"),
        StandardCharsets.UTF_8);

    Path lmBuildDir = tempDir.resolve("lm-build");
    Files.createDirectories(lmBuildDir);
    trigramLmPath = tempDir.resolve("trigram.slm");
    File uncompressedDir =
        MultiFileUncompressedLm.generate(arpaPath.toFile(), lmBuildDir.toFile(), "utf-8", 4)
            .getLmDir();
    UncompressedToSmoothLmConverter converter =
        new UncompressedToSmoothLmConverter(trigramLmPath.toFile(), lmBuildDir.toFile());
    converter.convertSmall(
        uncompressedDir, new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));

    morphology = TurkishMorphology.builder()
        .setLexicon("merhaba [P:Interj]", "dünya")
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
  public void testEmptyAndNonTokenizableInputAndTrigramDecode_Issue237() throws IOException {
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, trigramLmPath);

    Assert.assertEquals("", normalizer.combineNecessaryWords(Collections.emptyList()));
    Assert.assertEquals("", normalizer.normalize(""));
    Assert.assertEquals("   ", normalizer.normalize("   "));
    // Non-breaking space (\u00A0) passes sentence.trim().length() == 0 yet produces 0 tokens.
    Assert.assertEquals("", normalizer.normalize("\u00A0"));
    Assert.assertEquals("", normalizer.preProcess(""));

    // 3-gram SmoothLm previously threw ArrayIndexOutOfBoundsException in decode().
    Assert.assertEquals("merhaba dünya", normalizer.normalize("merhaba dünya"));
  }
}
