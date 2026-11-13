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

public class NormalizerAcronymAndExtElongationTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path lmPath;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-normalizer-acronym-ext-test");
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
            "ngram 1=12",
            "ngram 2=3",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.0000 ya\t-0.1",
            "-1.0000 of\t-0.1",
            "-1.2000 sevda\t-0.1",
            "-1.2000 teknik\t-0.1",
            "-1.2000 danışmanı\t-0.1",
            "-1.2000 savunma\t-0.1",
            "-1.2000 sanayii\t-0.1",
            "-1.2000 güzel\t-0.1",
            "-1.2000 oldu\t-0.1",
            "",
            "\\2-grams:",
            "-0.4000 teknik danışmanı",
            "-0.4000 savunma sanayii",
            "-0.4000 güzel oldu",
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
  public void extInterjectionElongationsNormalizeWhileExtCompoundNounsStayIntact()
      throws IOException {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .setLexicon(
            "ya [P:Interj]",
            "yaa [P:Interj; A:Ext]",
            "yaaa [P:Interj; A:Ext]",
            "of [P:Interj]",
            "oof [P:Interj; A:Ext]",
            "savunma",
            "sanayi",
            "sanayii [A:Ext, CompoundP3sg; Ref:sanayi]",
            "güzel [P:Adj]",
            "olmak")
        .disableCache()
        .build();
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);

    Assert.assertEquals("güzel oldu ya", normalizer.normalize("güzel oldu yaaa"));
    Assert.assertEquals("güzel oldu ya", normalizer.normalize("güzel oldu yaa"));
    Assert.assertEquals("of güzel oldu", normalizer.normalize("ooof güzel oldu"));
    Assert.assertEquals("savunma sanayii", normalizer.normalize("savunma sanayii"));
  }

  @Test
  public void sentenceInitialAllCapsAcronymProtectedInMixedCaseSentence() throws IOException {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .setLexicon("sevda", "teknik [P:Adj]", "danışman")
        .disableCache()
        .build();
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);

    // Sentence-initial ALL-CAPS acronym in a mixed-case sentence must not be edit-distance
    // rewritten to "sevda", while a lowercase typo ("serda") is still spell-checked.
    Assert.assertEquals(
        "serda teknik danışmanı", normalizer.normalize("SERDA teknik danışmanı"));
    Assert.assertEquals(
        "sevda teknik danışmanı", normalizer.normalize("serda teknik danışmanı"));
  }
}
