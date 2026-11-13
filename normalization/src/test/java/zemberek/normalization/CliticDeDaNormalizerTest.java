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

public class CliticDeDaNormalizerTest {

  private static Path tempDir;
  private static TurkishSentenceNormalizer normalizer;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-clitic-deda-test");
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
            "ngram 1=20",
            "ngram 2=9",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.2000 başlasa\t-0.1",
            "-1.1000 da\t-0.1",
            "-1.1000 de\t-0.1",
            "-1.2000 ya\t-0.1",
            "-4.0000 yada\t-0.1",
            "-1.2000 ben\t-0.1",
            "-1.2000 geldim\t-0.1",
            "-1.2000 evde\t-0.1",
            "-1.2000 kaldım\t-0.1",
            "-1.1000 birlikte\t-0.1",
            "-1.2000 hayatta\t-0.1",
            "-1.2000 arasında\t-0.1",
            "-3.8000 arasın\t-0.1",
            "-1.4000 saat\t-0.1",
            "-1.4000 ahmet\t-0.1",
            "-1.4000 davutoğlu\t-0.1",
            "-1.2000 Manisa'da\t-0.1",
            "",
            "\\2-grams:",
            "-0.4000 <s> ya",
            "-0.4000 ya da",
            "-0.5000 ben de",
            "-0.5000 de geldim",
            "-0.5000 evde kaldım",
            "-0.5000 <s> arasında",
            "-0.5000 arasında </s>",
            "-0.5000 ahmet davutoğlu",
            "-0.5000 Manisa'da da",
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
                "başlamak",
                "iktidar",
                "olmak [A:Aorist_I]",
                "amaç [A:Voicing]",
                "ya [P:Conj]",
                "da [P:Conj]",
                "de [P:Conj]",
                "birlik",
                "hayat [A:NoVoicing]",
                "ara",
                "aramak",
                "saat [A:InverseHarmony, NoVoicing]",
                "ben",
                "gelmek",
                "ev",
                "kalmak",
                "Manisa [P:Noun,Prop]",
                "Ahmet [P:Noun,Prop]",
                "Davutoğlu [P:Noun,Prop]",
                "Ahmetdavutoğlu [P:Noun,Prop]")
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
  public void splitsMorphotacticallyInvalidAttachedCliticsTest() {
    Assert.assertEquals("başlasa da", normalizer.normalize("başlasada"));
    Assert.assertEquals("iktidarı da", normalizer.normalize("iktidarıda"));
    Assert.assertEquals("manisa'da da", normalizer.preProcess("Manisa'dada"));
    Assert.assertEquals("manisa'da da", normalizer.normalize("Manisa'dada"));
    Assert.assertEquals("saat de", normalizer.normalize("saatde"));
  }

  @Test
  public void splitsAmbiguousYaDaWhenLmPrefersSplitTest() {
    Assert.assertEquals("ya da", normalizer.normalize("yada"));
  }

  @Test
  public void mergesDetachedLocativesTest() {
    Assert.assertEquals("birlikte", normalizer.normalize("birlik te"));
    Assert.assertEquals("hayatta", normalizer.normalize("hayat ta"));
    Assert.assertEquals("arasında", normalizer.normalize("arasın da"));
  }

  @Test
  public void preservesValidCliticsLocativesAndAdjacentProperNounsTest() {
    Assert.assertEquals("ben de geldim", normalizer.normalize("ben de geldim"));
    Assert.assertEquals("evde kaldım", normalizer.normalize("evde kaldım"));
    Assert.assertEquals("ahmet davutoğlu", normalizer.normalize("Ahmet Davutoğlu"));
  }
}
