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

public class NormalizerElongationAndNumeralTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path lmPath;
  private static TurkishMorphology morphology;
  private static TurkishSentenceNormalizer normalizer;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-normalizer-elong-num-test");
    dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(
        dataRoot.resolve("lookup-from-graph"), Collections.emptyList(), StandardCharsets.UTF_8);
    Files.write(dataRoot.resolve("split"), Collections.emptyList(), StandardCharsets.UTF_8);

    Path arpaPath = tempDir.resolve("bigram.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=21",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-0.8000 çok\t-0.1",
            "-0.9000 güzel\t-0.1",
            "-0.8000 saat\t-0.1",
            "-3.5000 sat\t-0.1",
            "-1.0000 affetti\t-0.1",
            "-0.8000 ya\t-0.1",
            "-3.5000 yaa\t-0.1",
            "-1.0000 evet\t-0.1",
            "-1.2000 3'te\t-0.1",
            "-1.2000 2007'den\t-0.1",
            "-1.2000 4'ten\t-0.1",
            "-1.2000 5'inci\t-0.1",
            "-1.2000 2'nci\t-0.1",
            "-1.2000 ankara'ya\t-0.1",
            "-1.2000 gel\t-0.1",
            "-1.2000 ile\t-0.1",
            "-1.2000 ve\t-0.1",
            "-1.2000 de\t-0.1",
            "",
            "\\2-grams:",
            "-0.5000 çok güzel",
            "-0.5000 saat 3'te",
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
            "iki [P:Num]",
            "ikinci [P:Num,Ord]",
            "üç [P:Num]",
            "dört [P:Num; A:Voicing]",
            "dördüncü [P:Num,Ord]",
            "beş [P:Num]",
            "beşinci [P:Num,Ord]",
            "yedi [P:Num]",
            "bin [P:Num]",
            "elma",
            "çok",
            "güzel [P:Adj]",
            "saat [A:NoVoicing]",
            "satmak",
            "affetmek [A:Voicing]",
            "ya [P:Conj]",
            "yaa [P:Interj]",
            "evet [P:Interj]",
            "ciddi [P:Adj]",
            "dil",
            "mi [P:Ques]",
            "gelmek",
            "ile [P:Postp]",
            "ve [P:Conj]",
            "de [P:Conj]",
            "da [P:Conj]",
            "ankara [P:Prop]")
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
  public void normalizesElongationsWhilePreservingGeminatesAndRomanNumerals() {
    Assert.assertEquals("çok güzel", normalizer.preProcess("çooook güzellll"));
    Assert.assertEquals("saat", normalizer.preProcess("saatttt"));
    Assert.assertEquals("affetti", normalizer.preProcess("affetttti"));
    Assert.assertEquals("ya", normalizer.preProcess("yaaaa"));
    Assert.assertEquals("evet", normalizer.preProcess("evett"));
    // Words made of {c, d, i, l, m, v} must not be misclassified as Roman numerals.
    Assert.assertEquals("ciddi", normalizer.preProcess("ciddiiii"));
    Assert.assertEquals("dil", normalizer.preProcess("diiiiil"));
    Assert.assertEquals("mi", normalizer.preProcess("miiii"));
    // Genuine Roman numerals remain intact.
    Assert.assertEquals("iii", normalizer.preProcess("iii"));
    Assert.assertEquals("xxiii", normalizer.preProcess("xxiii"));
  }

  @Test
  public void attachesAndRepairsNumeralSuffixesSafely() {
    Assert.assertEquals("saat 3'te", normalizer.preProcess("saat 3 de"));
    Assert.assertEquals("2007'den", normalizer.preProcess("2007 den"));
    Assert.assertEquals("4'ten", normalizer.preProcess("4 den"));
    Assert.assertEquals("5'inci", normalizer.preProcess("5 inci"));
    Assert.assertEquals("2'nci", normalizer.preProcess("2 inci"));
    Assert.assertEquals("4'üncü", normalizer.preProcess("4 ncü"));
    Assert.assertEquals("4'üncü", normalizer.preProcess("4'ncü"));
    Assert.assertEquals("2'nci", normalizer.preProcess("2'inci"));
    Assert.assertEquals("4'te", normalizer.preProcess("4'de"));
  }

  @Test
  public void doesNotAttachStandaloneWordsOrDuplicateCombinedTokensBeforePunctuation() {
    Assert.assertEquals("3 ile 5", normalizer.preProcess("3 ile 5"));
    Assert.assertEquals("ve 2 de", normalizer.preProcess("ve 2 de"));
    Assert.assertEquals("2 ya da 3", normalizer.preProcess("2 ya da 3"));
    Assert.assertEquals("elmalar .", normalizer.preProcess("elma lar."));
    Assert.assertEquals("saat 3'te .", normalizer.preProcess("saat 3 de."));
  }
}
