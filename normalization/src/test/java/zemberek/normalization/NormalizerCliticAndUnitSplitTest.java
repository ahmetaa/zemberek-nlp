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

public class NormalizerCliticAndUnitSplitTest {

  private static Path tempDir;
  private static TurkishSentenceNormalizer normalizer;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-clitic-unit-split-test");
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
            "ngram 1=31",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.0000 5\t-0.1",
            "-1.0000 aydan\t-0.1",
            "-1.0000 haftadan\t-0.1",
            "-1.0000 saatte\t-0.1",
            "-1.0000 yaşında\t-0.1",
            "-1.0000 tl\t-0.1",
            "-1.0000 gb\t-0.1",
            "-1.0000 kod\t-0.1",
            "-1.0000 zengin\t-0.1",
            "-1.0000 büyümüşler\t-0.1",
            "-1.0000 görüyor\t-0.1",
            "-1.0000 musun\t-0.1",
            "-1.0000 mi\t-0.1",
            "-1.0000 mu\t-0.1",
            "-1.0000 mı\t-0.1",
            "-1.0000 baharat\t-0.1",
            "-1.0000 karışımı\t-0.1",
            "-1.2000 karısı\t-0.1",
            "-1.0000 fikrimizi\t-0.1",
            "-1.0000 serilerini\t-0.1",
            "-1.0000 kucağına\t-0.1",
            "-1.0000 fotoğrafları\t-0.1",
            "-1.0000 yaptınız\t-0.1",
            "-1.0000 var\t-0.1",
            "-1.0000 ki\t-0.1",
            "-1.0000 de\t-0.1",
            "-1.0000 da\t-0.1",
            "-1.0000 yanında\t-0.1",
            "",
            "\\2-grams:",
            "-0.5000 karısı mı",
            "-0.5000 baharat karışımı",
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
                "hafta",
                "saat [A:NoVoicing, InverseHarmony]",
                "yaş",
                "tl [P:Noun,Abbrv]",
                "gb [P:Noun,Abbrv]",
                "kod",
                "zengin [P:Adj]",
                "büyümek",
                "görmek [A:Aorist_I]",
                "baharat",
                "karışım",
                "karı",
                "fikir [A:LastVowelDrop]",
                "seri",
                "kucak [A:Voicing]",
                "fotoğraf",
                "okumak",
                "yapmak",
                "demek",
                "var [P:Adj]",
                "ki [P:Conj]",
                "de [P:Conj]",
                "da [P:Conj]",
                "mı [P:Ques]",
                "mi [P:Ques]",
                "mu [P:Ques]",
                "mü [P:Ques]",
                "yan",
                "trafik [A:Voicing]",
                "bugün [P:Noun,Time]")
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
  public void splitsGluedNumberAndUnitTokensWithoutBreakingCodes() {
    Assert.assertEquals("5 aydan", normalizer.normalize("5aydan"));
    Assert.assertEquals("1 haftadan", normalizer.normalize("1haftadan"));
    Assert.assertEquals("20 tl", normalizer.normalize("20tl"));
    Assert.assertEquals("50 gb", normalizer.normalize("50gb"));
    Assert.assertEquals("2 saatte", normalizer.normalize("2saatte"));
    Assert.assertEquals("15 yaşında", normalizer.normalize("15yasinda"));
    Assert.assertEquals("3d", normalizer.preProcess("3d"));
    Assert.assertEquals("1080p", normalizer.preProcess("1080p"));
    Assert.assertEquals("5inci", normalizer.preProcess("5inci"));
  }

  @Test
  public void splitsOovQuestionCliticsWhilePreservingAsciiSingleWords() {
    Assert.assertEquals("kod mu", normalizer.normalize("kodmu"));
    Assert.assertEquals("zengin mi", normalizer.normalize("zengınmı"));
    Assert.assertEquals("büyümüşler mi", normalizer.normalize("büyümüşlermı"));
    Assert.assertEquals("görüyor musun", normalizer.normalize("goruyormusun"));
    Assert.assertEquals("baharat karışımı", normalizer.normalize("baharat karısımı"));
  }

  @Test
  public void splitsUnambiguousOovDeDaAndKiWithoutSplittingLocativeTypos() {
    Assert.assertEquals("fikrimizi de", normalizer.normalize("fıkrımızıde"));
    Assert.assertEquals("serilerini de", normalizer.normalize("serilerinide"));
    Assert.assertEquals("kucağına da", normalizer.normalize("kucaginada"));
    Assert.assertEquals("fotoğrafları da", normalizer.normalize("fotograflarıda"));
    Assert.assertEquals("okudum da", normalizer.normalize("okudumda"));
    Assert.assertEquals("yaptınız da", normalizer.normalize("yaptınızda"));
    Assert.assertEquals("var ki", normalizer.normalize("varkı"));
    Assert.assertEquals("demek ki", normalizer.normalize("demekki"));
    Assert.assertEquals("yanıda", normalizer.preProcess("yanıda"));
    Assert.assertEquals("trafikde", normalizer.preProcess("trafikde"));
    Assert.assertEquals("bugünki", normalizer.preProcess("bugünki"));
  }
}
