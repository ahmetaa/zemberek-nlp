package zemberek.apps.ner;

import com.beust.jcommander.Parameter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.apps.ConsoleApp;
import zemberek.core.io.IOUtil;
import zemberek.ner.NerDataSet;

abstract class NerAppBase extends ConsoleApp {

  @Parameter(
      names = {"--outDir", "-o"},
      description = "Output directory where all output files will be written. If not provided, "
          + "working directory will be used.")
  public Path outDir;

  @Parameter(
      names = {"--annotationStyle", "-s"},
      description = "Annotation style. Styles:ENAMEX|BRACKET|OPEN_NLP. ENAMEX style example: " +
          "'<b_enamex TYPE=\"LOC\"> İstanbul <e_enamex> güzel.'. BRACKET style example: " +
          "'[LOC İstanbul] güzel.'. OPEN_NLP style example: " +
          "'<START:LOC> İstanbul <END> güzel.'")
  public NerDataSet.AnnotationStyle annotationStyle;

  protected void initializeOutputDir() throws IOException {
    if (outDir == null) {
      outDir = Paths.get(System.getProperty("user.dir"));
    } else {
      if (outDir.toFile().exists()) {
        IOUtil.checkDirectoryArgument(outDir, "Output Directory");
      }
      Files.createDirectories(outDir);
    }
  }

}
