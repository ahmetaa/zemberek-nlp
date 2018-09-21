package zemberek.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import zemberek.core.logging.Log;

public class ZemberekGrpcServer {

  public static final int PORT = 6789;

  public static void main(String[] args) throws Exception {
    ZemberekContext context = new ZemberekContext();
    Server server = ServerBuilder.forPort(PORT)
        .addService(new AnalysisServiceImpl(context))
        .addService(new LanguageIdServiceImpl())
        .addService(new PreprocessingServiceImpl())
        .addService(new NormalizationServiceImpl(context))
        .build()
        .start();
    Log.info("Zemberek grpc server started at port: " + PORT);
    server.awaitTermination();
  }
}
