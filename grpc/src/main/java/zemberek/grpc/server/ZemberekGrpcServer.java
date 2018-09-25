package zemberek.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import zemberek.core.logging.Log;

public class ZemberekGrpcServer {

  public static final int DEFAULT_PORT = 6789;

  private final int port;
  private ZemberekContext context;

  public ZemberekGrpcServer() {
    this(DEFAULT_PORT);
  }

  public ZemberekGrpcServer(int port) {
    this.port = port;
    context = new ZemberekContext();
  }

  public int getPort() {
    return port;
  }

  public ZemberekContext getContext() {
    return context;
  }

  public void start() throws Exception {
    Server server = ServerBuilder.forPort(port)
        .addService(new AnalysisServiceImpl(context))
        .addService(new LanguageIdServiceImpl())
        .addService(new PreprocessingServiceImpl())
        .addService(new NormalizationServiceImpl(context))
        .addService(new SimpleAnalysisServiceImpl(context))
        .build()
        .start();
    Log.info("Zemberek grpc server started at port: " + port);
    server.awaitTermination();
  }

  public static void main(String[] args) throws Exception {
    new ZemberekGrpcServer().start();
  }


}
