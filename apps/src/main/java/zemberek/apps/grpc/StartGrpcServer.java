package zemberek.apps.grpc;

import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import zemberek.apps.ConsoleApp;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.grpc.server.ZemberekGrpcConfiguration;
import zemberek.grpc.server.ZemberekGrpcServer;

public class StartGrpcServer extends ConsoleApp {

  @Parameter(names = {"--port", "-p"},
      description = "Service port. If not used, default port (6789) will be used.")
  public int port = ZemberekGrpcServer.DEFAULT_PORT;

  @Parameter(names = {"--dataRoot", "-r"},
      description = "Zemberek external data root path. This may contain models and other necessary"
          + " external data.")
  public Path dataRoot;

  @Override
  public String description() {
    return "Starts Zemberek gRPC Server. By default it uses port 6789";
  }

  @Override
  public void run() throws Exception {
    ZemberekGrpcConfiguration configuration = null;
    if (dataRoot != null) {
      IOUtil.checkDirectoryArgument(dataRoot, "Zemberek External Data Root");
      Log.info("Zemberek external data root is : %s", dataRoot.toFile().getAbsolutePath());
      configuration = ZemberekGrpcConfiguration.fromDataRoot(dataRoot);
    }
    ZemberekGrpcServer server = new ZemberekGrpcServer(port, configuration);
    server.start();
  }

  public static void main(String[] args) {
    new StartGrpcServer().execute(args);
  }
}
