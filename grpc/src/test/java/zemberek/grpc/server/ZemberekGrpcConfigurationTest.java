package zemberek.grpc.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zemberek.morphology.analysis.WordAnalysis;

public class ZemberekGrpcConfigurationTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testLoadFromFileWithNormalizationDirectoryAndCustomDict() throws IOException {
    Path root = tempFolder.getRoot().toPath();
    Path lmFile = root.resolve("lm/lm.2gram.slm");
    Files.createDirectories(lmFile.getParent());
    Files.write(lmFile, new byte[]{1, 2, 3});

    Path normDir = root.resolve("normalization");
    Files.createDirectories(normDir);

    Path extraDict = root.resolve("custom.dict");
    Files.write(extraDict, Arrays.asList("Limanx [P:Noun, Prop]", "zemberekx"),
        StandardCharsets.UTF_8);

    Path confFile = root.resolve("server.conf");
    Files.write(confFile, Arrays.asList(
        "# gRPC Server Config",
        "port = 7788",
        "normalization.lm = lm/lm.2gram.slm",
        "normalization.dataRoot = normalization",
        "morphology.extraDictionaries = custom.dict",
        "morphology.ignoreDiacritics = true"
    ), StandardCharsets.UTF_8);

    ZemberekGrpcConfiguration config = ZemberekGrpcConfiguration.loadFromFile(confFile);
    Assert.assertEquals(7788, config.getPort());
    Assert.assertTrue(config.normalizationPathsAvailable());
    Assert.assertEquals(lmFile.normalize(), config.getNormalizationLmPath().normalize());
    Assert.assertEquals(normDir.normalize(), config.getNormalizationDataRoot().normalize());
    Assert.assertEquals(1, config.getExtraDictionaries().size());
    Assert.assertTrue(config.isIgnoreDiacritics());

    ZemberekContext context = new ZemberekContext(config);
    WordAnalysis analysis = context.getMorphology().analyze("zemberekxler");
    Assert.assertTrue(analysis.isCorrect());
  }

  @Test
  public void testLoadFromFileWithoutNormalizationPaths() throws IOException {
    Path confFile = tempFolder.newFile("minimal.conf").toPath();
    Files.write(confFile, Arrays.asList(
        "port = 8090"
    ), StandardCharsets.UTF_8);

    ZemberekGrpcConfiguration config = ZemberekGrpcConfiguration.loadFromFile(confFile);
    Assert.assertEquals(8090, config.getPort());
    Assert.assertFalse(config.normalizationPathsAvailable());
  }

  @Test(expected = IllegalStateException.class)
  public void testLoadFromFileRejectsHalfConfiguredNormalization() throws IOException {
    Path root = tempFolder.getRoot().toPath();
    Path lmFile = root.resolve("lm.slm");
    Files.write(lmFile, new byte[]{1});

    Path confFile = root.resolve("broken.conf");
    Files.write(confFile, Arrays.asList(
        "normalization.lm = lm.slm"
    ), StandardCharsets.UTF_8);

    ZemberekGrpcConfiguration.loadFromFile(confFile);
  }
}
