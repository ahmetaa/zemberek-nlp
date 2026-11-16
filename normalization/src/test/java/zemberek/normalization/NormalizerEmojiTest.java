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

public class NormalizerEmojiTest {

  private static Path tempDir;
  private static TurkishSentenceNormalizer normalizer;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-emoji-normalizer-test");
    Path dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(
        dataRoot.resolve("lookup-from-graph"),
        Collections.singletonList("etdim=ettim"),
        StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("ascii-map"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("split"), Collections.emptyList(), StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("bigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=14",
            "ngram 2=3",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-0.8000 hediye\t-0.1",
            "-0.8000 ettim\t-0.1",
            "-0.8000 iyi\t-0.1",
            "-0.8000 kötü\t-0.1",
            "-0.8000 ama\t-0.1",
            "-0.8000 ucuz\t-0.1",
            "-0.8000 çok\t-0.1",
            "-0.8000 güzel\t-0.1",
            "-0.8000 olmuş\t-0.1",
            "-0.8000 teşekkürler\t-0.1",
            "-0.8000 canım\t-0.1",
            "",
            "\\2-grams:",
            "-0.4000 hediye ettim",
            "-0.4000 çok güzel",
            "-0.4000 güzel olmuş",
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
                "hediye",
                "etmek [A:Voicing]",
                "iyi [P:Adj]",
                "kötü [P:Adj]",
                "ama [P:Conj]",
                "ucuz [P:Adj]",
                "çok [P:Adv]",
                "güzel [P:Adj]",
                "olmak",
                "teşekkür",
                "can")
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
  public void normalizesGluedWordsAndPreservesTrailingEmojis() {
    Assert.assertEquals("hediye ettim 🥰", normalizer.normalize("hediyye ettimm🥰"));
    Assert.assertEquals("hediye ettim 🥰", normalizer.normalize("hediyye etdim🥰"));
    Assert.assertEquals("güzel 👍 teşekkürler 🙏", normalizer.normalize("güzel👍teşekkürler🙏"));
    Assert.assertEquals("kötü 😡 ama ucuz", normalizer.normalize("kötü😡ama ucuz"));
  }

  @Test
  public void doesNotDeleteBmpHeartOrOtherGluedSymbolsInSpellChecker() {
    Assert.assertEquals("iyi ❤", normalizer.normalize("iyi❤"));
    Assert.assertEquals("iyi ❤️", normalizer.normalize("iyi❤️"));
    Assert.assertEquals("iyi ★", normalizer.normalize("iyi★"));
    // Even unmapped non-emoji/currency symbols outside EmojiBase must not be deleted by spellcheck.
    Assert.assertEquals("iyi¬", normalizer.normalize("iyi¬"));
    Assert.assertEquals("iyi₹", normalizer.normalize("iyi₹"));
  }

  @Test
  public void preservesEmojiRunsFlagsAndZwjSequencesWhileDeasciifyingWords() {
    Assert.assertEquals(
        "😂 😂 😂 çok güzel olmuş", normalizer.normalize("😂😂😂 cok guzel olmus"));
    Assert.assertEquals(
        "🇹🇷 çok güzel 👨‍👩‍👧‍👦", normalizer.normalize("🇹🇷 cok guzel 👨‍👩‍👧‍👦"));
  }

  @Test
  public void legacyDeasciifyValidWordsHandlesLeadingSupplementaryEmojiOffsets() {
    Assert.assertEquals(
        "🥰 çok güzel olmuş", normalizer.deasciifyValidWords("🥰 cok guzel olmus", false));
  }
}
