package zemberek.apps.demo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.apps.demo.DeasciifierInspector.DeasciifyResponse;
import zemberek.apps.demo.NormalizationInspector.NormalizeResponse;
import zemberek.lm.compression.MultiFileUncompressedLm;
import zemberek.lm.compression.SmoothLm;
import zemberek.lm.compression.UncompressedToSmoothLmConverter;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.deasciifier.latticedeasciifier.QuantizedWeightTable;
import zemberek.normalization.v2.CompactStemEndingSpellChecker;
import zemberek.normalization.v2.NormalizationKnowledge;
import zemberek.normalization.v2.SpanCandidateGenerator;
import zemberek.normalization.v2.SpanLatticePerceptronNormalizer;

public class ZemberekDemoServerTest {

  private static Path tempDir;
  private static TurkishMorphology morphology;
  private static SpanLatticePerceptronNormalizer tinyPerceptron;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-webdemo-test");
    morphology = TurkishMorphology.createWithDefaults();

    Path arpaPath = tempDir.resolve("tiny.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=8",
            "ngram 2=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.5000 selam\t-0.1",
            "-1.5000 bugün\t-0.1",
            "-1.5000 geliyor\t-0.1",
            "-1.5000 musun\t-0.1",
            "-1.6000 kar\t-0.1",
            "",
            "\\2-grams:",
            "-0.6000 selam bugün",
            "-0.5000 geliyor musun",
            "",
            "\\end\\"),
        StandardCharsets.UTF_8);

    Path lmBuildDir = tempDir.resolve("lm-build");
    Files.createDirectories(lmBuildDir);
    Path slmPath = tempDir.resolve("tiny.slm");
    File uncompressedDir =
        MultiFileUncompressedLm.generate(arpaPath.toFile(), lmBuildDir.toFile(), "utf-8", 4)
            .getLmDir();
    UncompressedToSmoothLmConverter converter =
        new UncompressedToSmoothLmConverter(slmPath.toFile(), lmBuildDir.toFile());
    converter.convertSmall(
        uncompressedDir, new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));
    SmoothLm tinyLm = SmoothLm.builder(slmPath).logBase(Math.E).build();

    SpanCandidateGenerator generator =
        new SpanCandidateGenerator(
            morphology,
            new CompactStemEndingSpellChecker(morphology, true),
            tinyLm,
            NormalizationKnowledge.empty(),
            SpanCandidateGenerator.Config.full());

    float[] dense = new float[SpanLatticePerceptronNormalizer.DENSE_DIM];
    dense[0] = 0.5f;
    dense[3] = 1.2f;
    dense[9] = 1.5f;
    dense[10] = -0.8f;
    dense[19] = 2.0f;
    QuantizedWeightTable sparse =
        QuantizedWeightTable.build(new long[] {1L, 2L}, new float[] {0.1f, -0.1f}, 16);
    tinyPerceptron =
        new SpanLatticePerceptronNormalizer("test-demo-v2.4", generator, dense, sparse);
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
  public void normalizationAndDeasciifierInspectorsProduceStructuredTraces() {
    NormalizationInspector normInspector = new NormalizationInspector(tinyPerceptron, null);
    NormalizeResponse normResp =
        normInspector.normalizeAndInspect("Slm bugun geliyomusun?", "perceptron", true, true);

    Assert.assertEquals("Selam bugün geliyor musun?", normResp.normalizedText());
    Assert.assertEquals(1, normResp.lines().size());
    Assert.assertFalse(normResp.lines().get(0).decodedSpans().isEmpty());
    Assert.assertFalse(normResp.lines().get(0).slots().isEmpty());

    DeasciifierInspector deascInspector = new DeasciifierInspector(morphology);
    DeasciifyResponse deascResp =
        deascInspector.deasciifyAndInspect(
            "Turkiye'de yasayan ogrenciler kar orani acikladi.", "lattice_disambig", true);

    Assert.assertTrue(deascResp.deasciifiedText().startsWith("Türkiye'de yaşayan öğrenciler"));
    Assert.assertEquals(5, deascResp.comparisons().size());
    Assert.assertFalse(deascResp.tokens().isEmpty());
  }
}
