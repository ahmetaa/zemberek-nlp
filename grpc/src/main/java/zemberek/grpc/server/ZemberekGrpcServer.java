package zemberek.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import zemberek.core.logging.Log;

public class ZemberekGrpcServer {

  public static final int PORT = 6789;

  public static void main(String[] args) throws Exception {
    Server server = ServerBuilder.forPort(PORT)
        .addService(new AnalysisServiceImpl())
        .addService(new LanguageIdServiceImpl())
        .addService(new PreprocessingServiceImpl())
        .build()
        .start();
    Log.info("Zemberek grpc server started at port: " + PORT);
    server.awaitTermination();
  }
}
