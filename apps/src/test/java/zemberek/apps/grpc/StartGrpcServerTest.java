package zemberek.apps.grpc;

import com.beust.jcommander.JCommander;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zemberek.grpc.server.ZemberekGrpcServer;

public class StartGrpcServerTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testConfigFlagAndCliPortOverride() throws IOException {
    Path root = tempFolder.getRoot().toPath();
    Path lmFile = root.resolve("lm/lm.2gram.slm");
    Files.createDirectories(lmFile.getParent());
    Files.write(lmFile, new byte[]{1, 2});
    Files.createDirectories(root.resolve("normalization"));

    Path confFile = root.resolve("grpc.conf");
    Files.write(confFile, Arrays.asList(
        "port = 7100",
        "dataRoot = " + root.toAbsolutePath()
    ), StandardCharsets.UTF_8);

    StartGrpcServer appFromConf = new StartGrpcServer();
    JCommander.newBuilder().addObject(appFromConf).build()
        .parse("--config", confFile.toAbsolutePath().toString());
    ZemberekGrpcServer server1 = appFromConf.createServer();
    Assert.assertEquals(7100, server1.getPort());
    Assert.assertTrue(server1.getContext().getConfiguration().normalizationPathsAvailable());

    StartGrpcServer appWithPortOverride = new StartGrpcServer();
    JCommander.newBuilder().addObject(appWithPortOverride).build()
        .parse("-c", confFile.toAbsolutePath().toString(), "-p", "7200");
    ZemberekGrpcServer server2 = appWithPortOverride.createServer();
    Assert.assertEquals(7200, server2.getPort());
    Assert.assertEquals(7200, server2.getContext().getConfiguration().getPort());
  }
}
