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

public class NormalizerSpellCheckGuardsTest {

  private static Path tempDir;
  private static TurkishSentenceNormalizer normalizer;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-spellcheck-guards-test");
    Path dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(
        dataRoot.resolve("lookup-from-graph"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("ascii-map"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("split"), Collections.emptyList(), StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("bigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=19",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.0000 aydan\t-0.1",
            "-1.0000 side\t-0.1",
            "-1.0000 sonra\t-0.1",
            "-1.2000 mesajı\t-0.1",
            "-1.2000 ankara\t-0.1",
            "-1.2000 Ankara'da\t-0.1",
            "-1.2000 Gine\t-0.1",
            "-1.2000 Gine'de\t-0.1",
            "-0.6000 dokuyorsunuz\t-0.1",
            "-1.8000 döküyorsunuz\t-0.1",
            "-0.6000 sokuyor\t-0.1",
            "-1.8000 söküyor\t-0.1",
            "-1.2000 oku\t-0.1",
            "-1.2000 kitabı\t-0.1",
            "-1.2000 canım\t-0.1",
            "-1.2000 geldi\t-0.1",
            "",
            "\\2-grams:",
            "-0.5000 aydan sonra",
            "-0.5000 kitabı oku",
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
                "ay",
                "side",
                "sonra [P:Adv]",
                "mesaj",
                "can",
                "gelmek",
                "kitap [A:Voicing]",
                "okumak",
                "dokumak",
                "dökmek",
                "sokmak",
                "sökmek",
                "Ankara [P:Prop]",
                "Gine [P:Prop]",
                "Caniş [P:Prop]",
                "Muji [P:Prop]")
            .disableCache()
            .build();

    normalizer = new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);
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
  public void doesNotDeleteDigitsInGluedAlphanumericTokens() {
    Assert.assertEquals("5aydan sonra", normalizer.normalize("5aydan sonra"));
    Assert.assertEquals("2sıde geldi", normalizer.normalize("2sıde geldi"));
  }

  @Test
  public void blocksProperNounSpellCheckHallucinationsAndLowercasesApostrophizedMatches() {
    Assert.assertEquals("mesajı geldi", normalizer.normalize("msajı geldi"));
    Assert.assertNotEquals("muji", normalizer.normalize("msjı").toLowerCase());
    Assert.assertNotEquals("caniş'imi", normalizer.normalize("canısımm").toLowerCase());
    Assert.assertNotEquals("gine'de", normalizer.normalize("yınede").toLowerCase());
    Assert.assertEquals("ankara geldi", normalizer.normalize("ankra geldi"));
    Assert.assertEquals("ankara'da geldi", normalizer.normalize("ankara'dx geldi"));
  }

  @Test
  public void preservesExplicitTurkishDiacriticsOnMixedAsciiWordsWithFallback() {
    Assert.assertEquals("döküyorsunuz", normalizer.normalize("dökuyorsunuz"));
    Assert.assertEquals("söküyor", normalizer.normalize("söküooo"));
    Assert.assertEquals("kitabı oku", normalizer.normalize("kitabı öku"));
  }
}
