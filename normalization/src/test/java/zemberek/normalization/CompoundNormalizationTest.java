package zemberek.normalization;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zemberek.lm.compression.MultiFileUncompressedLm;
import zemberek.lm.compression.UncompressedToSmoothLmConverter;
import zemberek.morphology.TurkishMorphology;

public class CompoundNormalizationTest {

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  private static TurkishSentenceNormalizer normalizer;

  private static final String TINY_ARPA = String.join("\n",
      "\\data\\",
      "ngram 1=19",
      "ngram 2=8",
      "",
      "\\1-grams:",
      "-5.0\t<unk>\t0.0",
      "-99.0\t<s>\t-0.5",
      "-1.0\t</s>\t0.0",
      "-1.5\tuluslararası\t-0.2",
      "-1.5\tmilletlerarası\t-0.2",
      "-1.5\tilişkiler\t-0.2",
      "-1.5\tanlaşma\t-0.2",
      "-1.5\tbirkaç\t-0.2",
      "-1.5\tkişi\t-0.2",
      "-1.5\thiçbir\t-0.2",
      "-1.5\thiçbiri\t-0.2",
      "-1.5\tşey\t-0.2",
      "-1.5\therhangi\t-0.2",
      "-1.5\tbir\t-0.2",
      "-1.5\tsorun\t-0.2",
      "-1.5\tvazgeçti\t-0.2",
      "-1.5\tdevletler\t-0.2",
      "-1.5\tarası\t-0.2",
      "-1.5\thafta\t-0.2",
      "",
      "\\2-grams:",
      "-0.5\tuluslararası ilişkiler",
      "-0.5\tmilletlerarası anlaşma",
      "-0.5\tbirkaç kişi",
      "-0.5\thiçbir şey",
      "-0.5\therhangi bir",
      "-0.5\tbir sorun",
      "-0.5\tbir şey",
      "-0.5\tdevletler arası",
      "",
      "\\end\\",
      ""
  );

  @BeforeClass
  public static void setUpClass() throws IOException {
    Path root = tempFolder.getRoot().toPath();
    Path arpaPath = root.resolve("tiny.arpa");
    Files.write(arpaPath, TINY_ARPA.getBytes(StandardCharsets.UTF_8));

    Path uncompDir = root.resolve("uncomp");
    Files.createDirectories(uncompDir);
    File lmDir = MultiFileUncompressedLm.generate(arpaPath.toFile(), uncompDir.toFile(), "utf-8", 4)
        .getLmDir();

    Path lmPath = root.resolve("lm.2gram.slm");
    UncompressedToSmoothLmConverter converter =
        new UncompressedToSmoothLmConverter(lmPath.toFile(), uncompDir.toFile());
    converter.convertSmall(
        lmDir, new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));

    Path normDataRoot = root.resolve("norm-data");
    Files.createDirectories(normDataRoot);
    Files.write(normDataRoot.resolve("lookup-from-graph"), Collections.emptyList(),
        StandardCharsets.UTF_8);
    Files.write(normDataRoot.resolve("ascii-map"), Collections.emptyList(),
        StandardCharsets.UTF_8);
    Files.write(normDataRoot.resolve("split"), Arrays.asList("birşey=bir şey"),
        StandardCharsets.UTF_8);

    TurkishMorphology morphology = TurkishMorphology.builder()
        .setLexicon(
            "ulus",
            "millet",
            "devlet",
            "ara",
            "uluslararası [P:Adj]",
            "milletlerarası [P:Adj]",
            "ilişki",
            "anlaşma",
            "bir [P:Det]",
            "kaç [P:Adj]",
            "birkaç [P:Det]",
            "kişi",
            "hiç [P:Adv]",
            "hiçbir [P:Det]",
            "hiçbiri [P:Pron, Quant]",
            "şey",
            "her [P:Det]",
            "hangi [P:Adj]",
            "herhangi [P:Adj]",
            "sorun",
            "geçmek",
            "vazgeçmek",
            "hafta",
            "son",
            "deniz",
            "alt",
            "denizaltı",
            "metre")
        .disableCache()
        .build();

    normalizer = new TurkishSentenceNormalizer(morphology, normDataRoot, lmPath);
  }

  @Test
  public void testIssue245SolidCompoundsAreCombined() {
    Assert.assertEquals("uluslararası ilişkiler",
        normalizer.normalize("Uluslar arası ilişkiler"));
    Assert.assertEquals("milletlerarası anlaşma",
        normalizer.normalize("Milletler arası anlaşma"));
    Assert.assertEquals("birkaç kişi",
        normalizer.normalize("bir kaç kişi"));
    Assert.assertEquals("birkaç kişi",
        normalizer.normalize("bikaç kişi"));
    Assert.assertEquals("hiçbir şey",
        normalizer.normalize("hiç bir şey"));
    Assert.assertEquals("hiçbiri",
        normalizer.normalize("hiç biri"));
    Assert.assertEquals("herhangi bir sorun",
        normalizer.normalize("her hangi bir sorun"));
    Assert.assertEquals("vazgeçti",
        normalizer.normalize("vaz geçti"));
  }

  @Test
  public void testSeparateTdkExpressionsAreNotFalselyCombined() {
    Assert.assertEquals("bir şey", normalizer.normalize("bir şey"));
    Assert.assertEquals("hafta sonu", normalizer.normalize("hafta sonu"));
    Assert.assertEquals("devletler arası", normalizer.normalize("devletler arası"));
    Assert.assertEquals("deniz altı metre", normalizer.normalize("deniz altı metre"));
  }
}
