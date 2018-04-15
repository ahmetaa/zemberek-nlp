package zemberek.morphology.external;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.kohsuke.args4j.Option;
import zemberek.core.ConsoleApplication;

public class YuretTureDisambiguatorRunner extends ConsoleApplication {

  @Option(name = "-root",
      required = true,
      usage = "Root of the Disambiguation script developed by Yuret et al.")
  public File root;

  @Option(name = "-in",
      required = true,
      usage = "Input Text File.")
  public File inputFile;

  @Option(name = "-out",
      required = true,
      usage = "Output Text File.")
  public File outputFile;

  public static void main(String[] args) {
    new YuretTureDisambiguatorRunner().execute(args);
  }

  @Override
  protected String getDescription() {
    return "Disambiguates morphological analysis results using Greedy prepent al";
  }

  @Override
  protected void run() throws Exception {
    disambiguate(inputFile, outputFile);
  }

  private void disambiguate(File ambigiousFile, File out) throws IOException {
    ProcessRunner processRunner = new ProcessRunner(root);
    try {
      processRunner.pipe(null, new FileOutputStream(out),
          new ProcessBuilder("cat", ambigiousFile.getAbsolutePath()),
          new ProcessBuilder("perl", "singleline.pl"),
          new ProcessBuilder("perl", "features.pl"),
          new ProcessBuilder("perl", "ft-model-eval.pl")
      );
    } catch (InterruptedException e) {
      System.err.append("Operation interrupted unexpectedly.\n");
      e.printStackTrace();
    }
  }
}
