package zemberek.apps.grpc;

import com.beust.jcommander.Parameter;
import java.io.IOException;
import java.nio.file.Path;
import zemberek.apps.ConsoleApp;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.grpc.server.ZemberekGrpcConfiguration;
import zemberek.grpc.server.ZemberekGrpcServer;

public class StartGrpcServer extends ConsoleApp {

  @Parameter(names = {"--port", "-p"},
      description = "Service port. If not used, configured port or default port (6789) will be used.")
  public Integer port;

  @Parameter(names = {"--dataRoot", "-r"},
      description = "Zemberek external data root path. This may contain models and other necessary"
          + " external data.")
  public Path dataRoot;

  @Parameter(names = {"--config", "-c"},
      description = "Path to gRPC server configuration file (key=value format).")
  public Path configPath;

  @Override
  public String description() {
    return "Starts Zemberek gRPC Server. By default it uses port 6789";
  }

  ZemberekGrpcServer createServer() throws IOException {
    ZemberekGrpcConfiguration configuration = null;
    if (configPath != null) {
      IOUtil.checkFileArgument(configPath, "Zemberek gRPC Configuration File");
      Log.info("Loading Zemberek gRPC configuration from: %s",
          configPath.toFile().getAbsolutePath());
      configuration = ZemberekGrpcConfiguration.loadFromFile(configPath);
    }
    if (dataRoot != null) {
      IOUtil.checkDirectoryArgument(dataRoot, "Zemberek External Data Root");
      Log.info("Zemberek external data root is : %s", dataRoot.toFile().getAbsolutePath());
      configuration = (configuration == null)
          ? ZemberekGrpcConfiguration.fromDataRoot(dataRoot)
          : configuration.withDataRoot(dataRoot);
    }
    int resolvedPort = (port != null)
        ? port
        : (configuration != null ? configuration.getPort() : ZemberekGrpcServer.DEFAULT_PORT);
    if (configuration != null && configuration.getPort() != resolvedPort) {
      configuration = configuration.withPort(resolvedPort);
    }
    return new ZemberekGrpcServer(resolvedPort, configuration);
  }

  @Override
  public void run() throws Exception {
    createServer().start();
  }

  public static void main(String[] args) {
    new StartGrpcServer().execute(args);
  }
}
