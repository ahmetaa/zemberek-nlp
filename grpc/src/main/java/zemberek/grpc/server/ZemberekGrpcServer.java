package zemberek.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import zemberek.core.logging.Log;

public class ZemberekGrpcServer {
  private static final int PORT = 6789;

  public static void main(String[] args) throws Exception {
    Server server = ServerBuilder.forPort(PORT)
        .addService(new AnalysisServerImpl())
        .build();
    Log.info("Zemberek grpc server started at port: " + PORT);
    server.awaitTermination();
  }
}
