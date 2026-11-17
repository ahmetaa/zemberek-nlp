package zemberek.normalization.v2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.lm.compression.MultiFileUncompressedLm;
import zemberek.lm.compression.SmoothLm;
import zemberek.lm.compression.UncompressedToSmoothLmConverter;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.deasciifier.latticedeasciifier.QuantizedWeightTable;
import zemberek.normalization.v2.NormV2Types.CasingShape;
import zemberek.normalization.v2.NormV2Types.SentenceLattice;
import zemberek.normalization.v2.NormV2Types.SpanArc;

public class SpanLatticePerceptronNormalizerTest {

  private static Path tempDir;
  private static TurkishMorphology morphology;
  private static SmoothLm tinyLm;
  private static SpanCandidateGenerator standaloneGenerator;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-span-lattice-v2-test");
    morphology = TurkishMorphology.createWithDefaults();

    Path arpaPath = tempDir.resolve("tiny.arpa");
    Files.write(
        arpaPath,
        Arrays.asList(
            "\\data\\",
            "ngram 1=12",
            "ngram 2=4",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.5000 selam\t-0.1",
            "-1.5000 bugün\t-0.1",
            "-1.5000 geliyor\t-0.1",
            "-1.5000 musun\t-0.1",
            "-1.6000 alınır\t-0.1",
            "-1.6000 mı\t-0.1",
            "-2.2000 mi\t-0.1",
            "-1.8000 normal\t-0.1",
            "-1.8000 sitedeki\t-0.1",
            "",
            "\\2-grams:",
            "-0.6000 selam bugün",
            "-0.5000 geliyor musun",
            "-0.4000 alınır mı",
            "-0.5000 normal mi",
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
    tinyLm = SmoothLm.builder(slmPath).logBase(Math.E).build();

    CompactStemEndingSpellChecker spellChecker = new CompactStemEndingSpellChecker(morphology, true);
    standaloneGenerator =
        new SpanCandidateGenerator(
            morphology,
            spellChecker,
            tinyLm,
            NormalizationKnowledge.empty(),
            SpanCandidateGenerator.Config.full());
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
  public void standaloneRuntimeNormalizerRunsWithoutExternalData() {
    float[] dense = new float[SpanLatticePerceptronNormalizer.DENSE_DIM];
    dense[0] = 0.5f; // F_SMOOTH_FWD2
    dense[3] = 1.2f; // F_SMOOTH_SPAN_LLR
    dense[9] = 1.5f; // F_SUFFIX_PRECISION
    dense[10] = -0.8f; // F_EDIT_COST
    dense[19] = 2.0f; // F_QUESTION_VOWEL_HARMONY
    QuantizedWeightTable sparse =
        QuantizedWeightTable.build(new long[] {1L, 2L}, new float[] {0.1f, -0.1f}, 16);

    SpanLatticePerceptronNormalizer normalizer =
        new SpanLatticePerceptronNormalizer("test-v2.4", standaloneGenerator, dense, sparse);

    Assert.assertEquals("Selam bugün geliyor musun?", normalizer.normalize("Slm bugun geliyomusun?"));
    Assert.assertEquals("Bu sistem alınır mı?", normalizer.normalize("Bu sistem alınır mi?"));
    Assert.assertEquals("normal mi", normalizer.normalize("normal mi"));
  }

  @Test
  public void contextualTruecasingAndLatticeCandidatesWork() {
    NormalizationKnowledge k = NormalizationKnowledge.empty();
    Assert.assertEquals(
        "Dr.", k.truecase("dr.", CasingShape.UPPER, "DR.", false, false, morphology));
    Assert.assertEquals(
        "vb.", k.truecase("vb.", CasingShape.UPPER, "VB.", false, false, morphology));
    Assert.assertEquals(
        "4K", k.truecase("4k", CasingShape.LOWER, "4k", false, false, morphology));
    Assert.assertEquals(
        "Nisan",
        k.truecase(
            "nisan",
            CasingShape.LOWER,
            "nisan",
            false,
            false,
            morphology,
            NormalizationKnowledge.CTX_ADJ_DATE_NUM));
    Assert.assertEquals(
        "nisan", k.truecase("nisan", CasingShape.LOWER, "nisan", false, false, morphology, 0));

    SentenceLattice lat = standaloneGenerator.buildLattice("Çok güzel :D Sitedeki ürünler 16-18 mayıs");
    boolean sawSitedekiCap = false;
    boolean sawMayisCap = false;
    for (List<SpanArc> slot : lat.outgoing()) {
      for (SpanArc a : slot) {
        if ("Sitedeki".equals(a.normSurface)) {
          sawSitedekiCap = true;
        }
        if ("Mayıs".equals(a.normSurface)) {
          sawMayisCap = true;
        }
      }
    }
    Assert.assertTrue("Post-emoticon uppercase word should stay capitalized", sawSitedekiCap);
    Assert.assertTrue("Month after numeric date range should capitalize", sawMayisCap);
  }

  @Test
  public void snapshotDirectoryLoadMatchesProductionBitForBitWhenAvailable() throws IOException {
    Path userHome = Paths.get(System.getProperty("user.home", "."));
    List<Path> candidateModelDirs =
        Arrays.asList(
            userHome.resolve("data/turkish/normalization/norm_v2_models"),
            Paths.get("../experiment/target/demo_models_v23"),
            Paths.get("experiment/target/demo_models_v23"));
    Path modelDir = null;
    for (Path cand : candidateModelDirs) {
      if (Files.isDirectory(cand)
          && (Files.exists(cand.resolve("perceptron.bin"))
              || Files.exists(cand.resolve("perceptron_v23.bin")))) {
        modelDir = cand;
        break;
      }
    }
    Path lm2Gram = userHome.resolve("data/turkish/normalization/zemberek_norm_data/lm.2gram.slm");
    Assume.assumeTrue(modelDir != null && Files.exists(lm2Gram));

    SpanLatticePerceptronNormalizer loaded =
        SpanLatticePerceptronNormalizer.fromDirectory(morphology, modelDir, lm2Gram);

    Assert.assertTrue(loaded.activeSparseFeatureCount() > 100_000);
    Assert.assertEquals("Selam bugün geliyor musun :)", loaded.normalize("Slm bugun geliyomusun :)"));
    Assert.assertEquals("Bu sistem alınır mı?", loaded.normalize("Bu sistem alınır mi?"));
  }
}
