package zemberek.grpc.server;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.nio.file.Paths;
import zemberek.core.logging.Log;

public class ZemberekGrpcServer {

  public static final int DEFAULT_PORT = 6789;

  private final int port;
  private ZemberekContext context;

  public ZemberekGrpcServer(ZemberekGrpcConfiguration configuration) {
    this(configuration != null ? configuration.getPort() : DEFAULT_PORT, configuration);
  }

  public ZemberekGrpcServer(int port, ZemberekGrpcConfiguration configuration) {
    this.port = port;
    context = new ZemberekContext(configuration);
  }

  public int getPort() {
    return port;
  }

  public ZemberekContext getContext() {
    return context;
  }

  public void start() throws Exception {
    Server server = NettyServerBuilder.forPort(port)
        .addService(new LanguageIdServiceImpl())
        .addService(new PreprocessingServiceImpl())
        .addService(new NormalizationServiceImpl(context))
        .addService(new MorphologyServiceImpl(context))
        .build()
        .start();
    Log.info("Zemberek grpc server started at port: " + port);
    server.awaitTermination();
  }

  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      ZemberekGrpcConfiguration config = ZemberekGrpcConfiguration.loadFromFile(Paths.get(args[0]));
      new ZemberekGrpcServer(config).start();
    } else {
      new ZemberekGrpcServer(DEFAULT_PORT, null).start();
    }
  }

}
