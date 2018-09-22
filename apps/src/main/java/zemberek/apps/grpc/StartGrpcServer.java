package zemberek.apps.grpc;

import com.beust.jcommander.Parameter;
import zemberek.apps.ConsoleApp;
import zemberek.grpc.server.ZemberekGrpcServer;

public class StartGrpcServer extends ConsoleApp {

  @Parameter(names = {"--port", "-p"},
      description = "Service port. If not used, default port (6789) will be used.")
  public int port = ZemberekGrpcServer.DEFAULT_PORT;

  @Override
  public String description() {
    return "Starts Zemberek Grpc Server.";
  }

  @Override
  public void run() throws Exception {
    ZemberekGrpcServer server = new ZemberekGrpcServer(port);
    server.start();
  }
}
