package zemberek.normalization;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.lm.NgramLanguageModel;
import zemberek.lm.compression.MultiFileUncompressedLm;
import zemberek.lm.compression.SmoothLm;
import zemberek.lm.compression.UncompressedToSmoothLmConverter;
import zemberek.morphology.TurkishMorphology;

public class NoisyChannelScoringTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path lmPath;
  private static NgramLanguageModel lm;
  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-noisy-channel-test");
    dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(
        dataRoot.resolve("lookup-from-graph"),
        Arrays.asList("bloğa=bloga", "eseriniz=eşeriniz"),
        StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("split"), Collections.emptyList(), StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("bigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=17",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.8000 aldım\t-0.1",
            "-1.2000 aldı\t-0.1",
            "-1.2000 oldum\t-0.1",
            "-1.8000 bloğa\t-0.1",
            "-1.2000 bloga\t-0.1",
            "-1.8000 eseriniz\t-0.1",
            "-1.2000 eşeriniz\t-0.1",
            "-1.5000 ışık\t-0.1",
            "-1.5000 açık\t-0.1",
            "-1.4000 aldığından\t-0.1",
            "-1.5000 kaldım\t-0.1",
            "-1.5000 güzel\t-0.1",
            "-2.0000 Sun'un\t-0.1",
            "-1.2000 sunun\t-0.1",
            "",
            "\\2-grams:",
            "-0.5000 ışık açık",
            "-0.5000 eseriniz güzel",
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

    lm = SmoothLm.builder(lmPath).logBase(Math.E).build();
    morphology =
        TurkishMorphology.builder()
            .setLexicon(
                "almak",
                "olmak",
                "blog [A:Voicing]",
                "eser",
                "eşer",
                "ışık [A:Voicing]",
                "açık [P:Adj; A:Voicing]",
                "kalmak",
                "güzel [P:Adj]",
                "Sun")
            .disableCache()
            .build();
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
  public void spellCheckerPrefersCloserDiacriticMatchOverHigherFreqNeighborTest()
      throws IOException {
    TurkishSpellChecker exact = new TurkishSpellChecker(morphology);
    List<String> exactRanked = exact.suggestForWord("aldim", lm);
    Assert.assertFalse(exactRanked.isEmpty());
    Assert.assertEquals("aldım", exactRanked.get(0));

    TurkishSpellChecker diacIgnoring =
        new TurkishSpellChecker(
            morphology, exact.getDecoder(), CharacterGraphDecoder.DIACRITICS_IGNORING_MATCHER);
    List<String> ranked = diacIgnoring.suggestForWord("aldim", lm);
    Assert.assertFalse(ranked.isEmpty());
    Assert.assertEquals("aldım", ranked.get(0));
  }

  @Test
  public void cleanFormallyValidWordsAndApostropheProperNounsArePreservedTest()
      throws IOException {
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);

    // Valid diacritic words must not be overwritten by noisier neighbors in clean Turkish text.
    String out = normalizer.normalize("eseriniz bloğa güzel");
    Assert.assertEquals("eseriniz bloğa güzel", out);

    // Valid apostrophized proper noun must retain its apostrophe (ASCII or curly).
    String properOut = normalizer.normalize("Sun'un eseriniz güzel");
    Assert.assertTrue(properOut, properOut.startsWith("sun'un "));
    String curlyProperOut = normalizer.normalize("Sun’un eseriniz güzel");
    Assert.assertTrue(curlyProperOut, curlyProperOut.startsWith("sun’un "));

    // ASCII input still normalizes missing diacritics, and real 1-edit typos still recover.
    Assert.assertEquals("ışık açık", normalizer.normalize("Isik acik"));
    Assert.assertEquals("aldığından", normalizer.normalize("alndığından"));
  }

  @Test
  public void countDiacriticDifferencesIgnoresNonDiacriticEditsTest() {
    Assert.assertEquals(1, TurkishSpellChecker.countDiacriticDifferences("aldim", "aldım"));
    Assert.assertEquals(0, TurkishSpellChecker.countDiacriticDifferences("aldım", "oldum"));
    Assert.assertEquals(1, TurkishSpellChecker.countDiacriticDifferences("aldim", "kaldım"));
    Assert.assertEquals(3, TurkishSpellChecker.countDiacriticDifferences("alndigindan", "aldığından"));
  }
}
