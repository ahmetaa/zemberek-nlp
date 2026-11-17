package zemberek.apps.demo;

import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import zemberek.apps.ConsoleApp;

/** CLI entry point for launching the Zemberek NLP Web Demo server via {@code ApplicationRunner}. */
public class StartWebDemo extends ConsoleApp {

  @Parameter(
      names = {"--port", "-p"},
      description = "HTTP server port (default: 8080).")
  public int port = ZemberekDemoServer.DEFAULT_PORT;

  @Parameter(
      names = {"--host"},
      description = "Bind address (default: 127.0.0.1).")
  public String host = ZemberekDemoServer.DEFAULT_HOST;

  @Parameter(
      names = {"--modelDir", "--model-dir", "-m"},
      description = "Directory containing knowledge.bin and perceptron.bin.")
  public Path modelDir;

  @Parameter(
      names = {"--normRoot", "--norm-root", "-n"},
      description = "Zemberek normalization data root containing lm.2gram.slm and normalization/.")
  public Path normRoot;

  @Override
  public String description() {
    return "Starts the interactive Zemberek NLP Web Demo Studio (default http://127.0.0.1:8080).";
  }

  @Override
  public void run() throws Exception {
    ZemberekDemoServer server = ZemberekDemoServer.create(modelDir, normRoot);
    server.start(host, port);
    Thread.currentThread().join();
  }

  public static void main(String[] args) {
    new StartWebDemo().execute(args);
  }
}
