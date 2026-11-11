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

public class NormalizerCapitalizedWordsTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path lmPath;
  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-normalizer-cap-test");
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
            "ngram 1=19",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.5000 yüküm\t-0.1",
            "-1.5000 limanından\t-0.1",
            "-1.5000 limanına\t-0.1",
            "-1.5000 ve\t-0.1",
            "-1.5000 üzerinden\t-0.1",
            "-1.5000 gidecek\t-0.1",
            "-1.5000 iki\t-0.1",
            "-1.5000 palet\t-0.1",
            "-1.5000 hazır\t-0.1",
            "-1.5000 izmir\t-0.1",
            "-1.5000 kitap\t-0.1",
            "-1.5000 burada\t-0.1",
            "-1.5000 tenis\t-0.1",
            "-1.5000 meddah\t-0.1",
            "-1.5000 lades\t-0.1",
            "-1.5000 godoş\t-0.1",
            "",
            "\\2-grams:",
            "-0.5000 iki palet",
            "-0.5000 kitap burada",
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
            "yük",
            "liman",
            "ve [P:Conj]",
            "üzer",
            "gitmek [A:Voicing, Aorist_A]",
            "iki [P:Num,Card]",
            "palet",
            "hazır [P:Adj]",
            "İzmir [P:Noun,Prop]",
            "kitap [A:Voicing]",
            "bura",
            "tenis",
            "meddah",
            "lades",
            "godoş")
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
  public void testCapitalizedForeignAndPortWordsNotCorrupted_Issue309() throws IOException {
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);

    // Mid-sentence capitalized OOV words in mixed-case prose are preserved by default.
    Assert.assertEquals(
        "yüküm tunis limanından jeddah ve rades üzerinden gidecek",
        normalizer.normalize("Yüküm Tunis limanından Jeddah ve Rades üzerinden gidecek"));
    Assert.assertEquals(
        "iki palet goods hazır",
        normalizer.normalize("iki palet Goods hazır"));

    // Known Turkish proper nouns written in ASCII still normalize via ascii-tolerant morphology.
    Assert.assertEquals(
        "yüküm izmir limanından gidecek",
        normalizer.normalize("Yüküm Izmir limanından gidecek"));

    // Sentence-initial capitalized typos are still corrected by default.
    Assert.assertEquals("kitap burada", normalizer.normalize("Kitp burada"));

    // Explicit opt-out protects sentence-initial capitalized OOV words as well.
    normalizer.setSpellCheckCapitalizedWords(false);
    Assert.assertEquals(
        "tunis limanından jeddah limanına",
        normalizer.normalize("Tunis limanından Jeddah limanına"));

    // Explicit opt-in spellchecks capitalized OOV words everywhere.
    normalizer.setSpellCheckCapitalizedWords(true);
    Assert.assertEquals(
        "tenis limanından meddah limanına",
        normalizer.normalize("Tunis limanından Jeddah limanına"));
  }
}
