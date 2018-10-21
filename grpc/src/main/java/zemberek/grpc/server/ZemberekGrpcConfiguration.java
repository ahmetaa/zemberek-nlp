package zemberek.grpc.server;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import zemberek.core.io.IOUtil;
import zemberek.core.io.KeyValueReader;

//TODO: This class will change in next release.
public class ZemberekGrpcConfiguration {

  Path normalizationLmPath;
  Path normalizationDataRoot;

  ZemberekGrpcConfiguration(Path normalizationLmPath, Path normalizationDataRoot) {
    this.normalizationLmPath = normalizationLmPath;
    this.normalizationDataRoot = normalizationDataRoot;
  }

  public Path getNormalizationLmPath() {
    return normalizationLmPath;
  }

  public Path getNormalizationDataRoot() {
    return normalizationDataRoot;
  }

  public boolean normalizationPathsAvailable() {
    return normalizationLmPath!=null && normalizationDataRoot!=null;
  }

  public static ZemberekGrpcConfiguration loadFromFile(Path path) throws IOException {
    KeyValueReader reader = new KeyValueReader("=", "#");
    Map<String, String> keyValues = reader.loadFromFile(path.toFile());
    String lmVal = keyValues.get("normalization.lm");
    if (lmVal == null) {
      throw new IllegalStateException(path + " file does not contain normalization.lm key");
    }
    String normalizationRootVal = keyValues.get("normalization.dataRoot");
    if (normalizationRootVal == null) {
      throw new IllegalStateException(path + " file does not contain normalization.dataRoot key");
    }
    Path lmPath = Paths.get(lmVal);
    IOUtil.checkFileArgument(lmPath, "Language model path");
    Path normalizationPath = Paths.get(normalizationRootVal);
    IOUtil.checkFileArgument(normalizationPath, "Normalization root path");
    return new ZemberekGrpcConfiguration(lmPath, normalizationPath);
  }

  public static ZemberekGrpcConfiguration fromDataRoot(Path root) {
    Path lmPath = root.resolve("lm/lm.2gram.slm");
    IOUtil.checkFileArgument(lmPath, "Language model path");
    Path normalizationRoot = root.resolve("normalization");
    IOUtil.checkDirectoryArgument(normalizationRoot, "Normalization root path");
    return new ZemberekGrpcConfiguration(lmPath, normalizationRoot);
  }

}
