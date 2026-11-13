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

public class NormalizerSplitAndFalseFriendsTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path lmPath;
  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-normalizer-split-ff-test");
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
            "ngram 1=16",
            "ngram 2=4",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.2000 yapıyor\t-0.1",
            "-1.2000 musun\t-0.1",
            "-1.2000 sevdi\t-0.1",
            "-1.2000 gelmiş\t-0.1",
            "-1.2000 mi\t-0.1",
            "-1.2000 birçok\t-0.1",
            "-1.2000 koyduk\t-0.1",
            "-1.2000 komşu\t-0.1",
            "-1.2000 tuşlarla\t-0.1",
            "-1.2000 resmi\t-0.1",
            "-1.2000 milli\t-0.1",
            "-1.2000 ankara\t-0.1",
            "-1.2000 iyi\t-0.1",
            "",
            "\\2-grams:",
            "-0.2000 yapıyor musun",
            "-0.2000 sevdi mi",
            "-0.2000 gelmiş mi",
            "-0.2000 birçok şey",
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
            "mı [P:Ques]",
            "mi [P:Ques]",
            "mu [P:Ques]",
            "mü [P:Ques]",
            "yapmak [A:Aorist_A]",
            "sevmek",
            "gelmek [A:Aorist_I]",
            "almak",
            "birçok [P:Det]",
            "şey",
            "koymak",
            "komşu",
            "tuş",
            "resmi [P:Adj]",
            "milli [P:Adj]",
            "iyi [P:Adj]",
            "go [P:Abbrv]",
            "Kom [P:Prop]",
            "Tus [P:Prop]",
            "Ankara [P:Prop]")
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
  public void testQuestionAndCompoundSplits() throws IOException {
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);

    // Informal verb + connected question particle
    Assert.assertEquals("yapıyor musun", normalizer.normalize("yapıyomusun"));
    // Unseen 2-gram finite verb + harmonious question particle
    Assert.assertEquals("sevdi mi", normalizer.normalize("sevdimi"));
    Assert.assertEquals("gelmiş mi", normalizer.normalize("gelmişmi"));
    // Solid compound "bir çok" -> "birçok"
    Assert.assertEquals("birçok şey", normalizer.normalize("bir çok şey"));

    // Valid words and bare 2sg imperatives must NOT be over-split
    Assert.assertEquals("resmi", normalizer.normalize("resmi"));
    Assert.assertEquals("milli", normalizer.normalize("milli"));
    Assert.assertEquals("almı", normalizer.separateCommon("almı", true));
  }

  @Test
  public void testAbbreviationAndShortProperFalseFriends() throws IOException {
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, lmPath);

    Assert.assertEquals("koyduk", normalizer.normalize("goyduk"));
    Assert.assertEquals("komşu", normalizer.normalize("komsu"));
    Assert.assertEquals("tuşlarla", normalizer.normalize("tuslarla"));
    // Long proper nouns remain intact
    Assert.assertEquals("ankara iyi", normalizer.normalize("Ankara iyi"));
  }
}
