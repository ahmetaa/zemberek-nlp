package zemberek.normalization;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.lm.compression.MultiFileUncompressedLm;
import zemberek.lm.compression.UncompressedToSmoothLmConverter;
import zemberek.morphology.TurkishMorphology;

public class NormalizerPipelinePerformanceTest {

  private static Path tempDir;
  private static Path dataRoot;
  private static Path bigramLmPath;
  private static Path trigramLmPath;
  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("zemberek-pipeline-perf-test");
    dataRoot = tempDir.resolve("data");
    Files.createDirectories(dataRoot);
    Files.write(
        dataRoot.resolve("lookup-from-graph"),
        Arrays.asList("gelicem=geleceğim", "gittik=gittik"),
        StandardCharsets.UTF_8);
    Files.write(
        dataRoot.resolve("ascii-map"),
        Arrays.asList("guldum=güldüm", "cok=çok", "guzel=güzel"),
        StandardCharsets.UTF_8);
    Files.write(
        dataRoot.resolve("split"),
        Arrays.asList("geliyormusun=geliyor musun"),
        StandardCharsets.UTF_8);

    Path biArpa = tempDir.resolve("bigram.arpa");
    Files.write(
        biArpa,
        Arrays.asList(
            "\\data\\",
            "ngram 1=14",
            "ngram 2=4",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.2000 ne\t-0.1",
            "-1.2000 haber\t-0.1",
            "-1.2000 çok\t-0.1",
            "-1.2000 güzel\t-0.1",
            "-1.2000 geliyor\t-0.1",
            "-1.2000 musun\t-0.1",
            "-1.2000 Ankara'ya\t-0.1",
            "-1.2000 ankara'ya\t-0.1",
            "-1.2000 gittik\t-0.1",
            "-1.2000 bugun\t-0.1",
            "-1.1000 bugün\t-0.1",
            "",
            "\\2-grams:",
            "-0.4000 ne haber",
            "-0.4000 çok güzel",
            "-0.4000 geliyor musun",
            "-0.4000 ankara'ya gittik",
            "",
            "\\end\\"),
        StandardCharsets.UTF_8);
    bigramLmPath = buildSmoothLm(biArpa, tempDir.resolve("bi-build"), tempDir.resolve("bi.slm"));

    Path triArpa = tempDir.resolve("trigram.arpa");
    Files.write(
        triArpa,
        Arrays.asList(
            "\\data\\",
            "ngram 1=12",
            "ngram 2=4",
            "ngram 3=2",
            "",
            "\\1-grams:",
            "-5.0000 <unk>\t0.0",
            "-1.0000 </s>\t0.0",
            "-1.0000 <s>\t-0.1",
            "-1.2000 ben\t-0.1",
            "-1.2000 ne\t-0.1",
            "-1.2000 haber\t-0.1",
            "-1.2000 çok\t-0.1",
            "-1.2000 güzel\t-0.1",
            "-1.2000 bugün\t-0.1",
            "-1.2000 ankara\t-0.1",
            "-1.2000 gittik\t-0.1",
            "-1.2000 ,\t-0.1",
            "",
            "\\2-grams:",
            "-0.4000 ne haber\t-0.1",
            "-0.4000 çok güzel\t-0.1",
            "-0.4000 bugün çok\t-0.1",
            "-0.4000 <s> bugün\t-0.1",
            "",
            "\\3-grams:",
            "-0.2000 <s> bugün çok",
            "-0.2000 bugün çok güzel",
            "",
            "\\end\\"),
        StandardCharsets.UTF_8);
    trigramLmPath =
        buildSmoothLm(triArpa, tempDir.resolve("tri-build"), tempDir.resolve("tri.slm"));

    morphology =
        TurkishMorphology.builder()
            .setLexicon(
                "ben",
                "ne",
                "haber",
                "çok",
                "güzel [P:Adj]",
                "elma",
                "gelmek",
                "bugün",
                "gitmek",
                "Ankara [P:Noun,Prop]")
            .disableCache()
            .build();
  }

  private static Path buildSmoothLm(Path arpaPath, Path buildDir, Path outPath)
      throws IOException {
    Files.createDirectories(buildDir);
    File uncompressedDir =
        MultiFileUncompressedLm.generate(arpaPath.toFile(), buildDir.toFile(), "utf-8", 4)
            .getLmDir();
    UncompressedToSmoothLmConverter converter =
        new UncompressedToSmoothLmConverter(outPath.toFile(), buildDir.toFile());
    converter.convertSmall(
        uncompressedDir, new UncompressedToSmoothLmConverter.NgramDataBlock(16, 16, 16));
    return outPath;
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
  public void singlePassPreProcessAndTrellisDecodeWorkTest() throws IOException {
    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, bigramLmPath);

    Assert.assertEquals("elmalar gitti !", normalizer.preProcess("elma lar gitti!"));
    Assert.assertEquals("ankara ' ya gittik !", normalizer.preProcess("Ankara 'ya gittik!"));
    Assert.assertEquals("ne yapacağız ?", normalizer.preProcess("napcaz?"));
    Assert.assertEquals("çok güzel , değil mi !", normalizer.normalize("cok guzel, dimi!"));
    Assert.assertEquals("geliyor musun ?", normalizer.normalize("geliyormusun?"));
  }

  @Test
  public void trigramBeamTrellisAndConcurrentNormalizationTest() throws Exception {
    TurkishSentenceNormalizer triNormalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, trigramLmPath);
    Assert.assertEquals("bugün çok güzel !", triNormalizer.normalize("bugun cok guzel!"));

    TurkishSentenceNormalizer biNormalizer =
        new TurkishSentenceNormalizer(morphology, dataRoot, bigramLmPath);
    List<String> inputs =
        Arrays.asList(
            "cok guzel, dimi!",
            "Ankara 'ya gittik.",
            "geliyormusun?",
            "bugun cok guzel!");
    List<String> expected = new ArrayList<>();
    for (String in : inputs) {
      expected.add(biNormalizer.normalize(in));
    }

    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      List<Callable<String>> tasks = new ArrayList<>();
      for (int i = 0; i < 64; i++) {
        final int idx = i % inputs.size();
        tasks.add(() -> biNormalizer.normalize(inputs.get(idx)));
      }
      List<Future<String>> futures = pool.invokeAll(tasks);
      for (int i = 0; i < futures.size(); i++) {
        Assert.assertEquals(expected.get(i % inputs.size()), futures.get(i).get());
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
