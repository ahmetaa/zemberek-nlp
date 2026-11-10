package zemberek.grpc.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import zemberek.core.io.IOUtil;
import zemberek.core.io.KeyValueReader;

public class ZemberekGrpcConfiguration {

  int port = ZemberekGrpcServer.DEFAULT_PORT;
  Path normalizationLmPath;
  Path normalizationDataRoot;
  List<Path> extraDictionaries = Collections.emptyList();
  boolean ignoreDiacritics = false;

  ZemberekGrpcConfiguration(Path normalizationLmPath, Path normalizationDataRoot) {
    this(
        ZemberekGrpcServer.DEFAULT_PORT,
        normalizationLmPath,
        normalizationDataRoot,
        Collections.emptyList(),
        false);
  }

  ZemberekGrpcConfiguration(
      int port,
      Path normalizationLmPath,
      Path normalizationDataRoot,
      List<Path> extraDictionaries,
      boolean ignoreDiacritics) {
    validatePort(port);
    this.port = port;
    this.normalizationLmPath = normalizationLmPath;
    this.normalizationDataRoot = normalizationDataRoot;
    this.extraDictionaries = extraDictionaries == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(new ArrayList<>(extraDictionaries));
    this.ignoreDiacritics = ignoreDiacritics;
  }

  public int getPort() {
    return port;
  }

  public Path getNormalizationLmPath() {
    return normalizationLmPath;
  }

  public Path getNormalizationDataRoot() {
    return normalizationDataRoot;
  }

  public List<Path> getExtraDictionaries() {
    return extraDictionaries;
  }

  public boolean isIgnoreDiacritics() {
    return ignoreDiacritics;
  }

  public boolean normalizationPathsAvailable() {
    return normalizationLmPath != null && normalizationDataRoot != null;
  }

  public ZemberekGrpcConfiguration withPort(int overridePort) {
    return new ZemberekGrpcConfiguration(
        overridePort,
        normalizationLmPath,
        normalizationDataRoot,
        extraDictionaries,
        ignoreDiacritics);
  }

  public ZemberekGrpcConfiguration withDataRoot(Path root) {
    IOUtil.checkDirectoryArgument(root, "Zemberek External Data Root");
    Path lmPath = root.resolve("lm/lm.2gram.slm");
    IOUtil.checkFileArgument(lmPath, "Language model path");
    Path normRoot = root.resolve("normalization");
    IOUtil.checkDirectoryArgument(normRoot, "Normalization root path");
    return new ZemberekGrpcConfiguration(
        port,
        lmPath,
        normRoot,
        extraDictionaries,
        ignoreDiacritics);
  }

  public static ZemberekGrpcConfiguration loadFromFile(Path path) throws IOException {
    IOUtil.checkFileArgument(path, "gRPC configuration file");
    Path baseDir = path.toAbsolutePath().getParent();
    KeyValueReader reader = new KeyValueReader("=", "#");
    Map<String, String> keyValues = reader.loadFromFile(path.toFile());

    int port = ZemberekGrpcServer.DEFAULT_PORT;
    String portVal = keyValues.get("port");
    if (portVal != null && !portVal.isEmpty()) {
      try {
        port = Integer.parseInt(portVal);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid port value in " + path + ": " + portVal, e);
      }
      validatePort(port);
    }

    Path lmPath = null;
    Path normalizationPath = null;

    String dataRootVal = keyValues.get("dataRoot");
    if (dataRootVal != null && !dataRootVal.isEmpty()) {
      Path root = resolvePath(baseDir, dataRootVal);
      IOUtil.checkDirectoryArgument(root, "Zemberek External Data Root");
      lmPath = root.resolve("lm/lm.2gram.slm");
      normalizationPath = root.resolve("normalization");
    }

    String lmVal = keyValues.get("normalization.lm");
    if (lmVal != null && !lmVal.isEmpty()) {
      lmPath = resolvePath(baseDir, lmVal);
    }
    String normalizationRootVal = keyValues.get("normalization.dataRoot");
    if (normalizationRootVal != null && !normalizationRootVal.isEmpty()) {
      normalizationPath = resolvePath(baseDir, normalizationRootVal);
    }

    if ((lmPath != null) != (normalizationPath != null)) {
      String missingKey = (lmPath == null) ? "normalization.lm" : "normalization.dataRoot";
      throw new IllegalStateException(path + " file does not contain " + missingKey + " key");
    }
    if (lmPath != null) {
      IOUtil.checkFileArgument(lmPath, "Language model path");
      IOUtil.checkDirectoryArgument(normalizationPath, "Normalization root path");
    }

    List<Path> extraDicts = new ArrayList<>();
    String extraDictsVal = keyValues.get("morphology.extraDictionaries");
    if (extraDictsVal != null && !extraDictsVal.isEmpty()) {
      for (String rawPart : extraDictsVal.split(",")) {
        String trimmed = rawPart.trim();
        if (!trimmed.isEmpty()) {
          Path dictPath = resolvePath(baseDir, trimmed);
          IOUtil.checkFileArgument(dictPath, "Extra dictionary path");
          extraDicts.add(dictPath);
        }
      }
    }

    boolean ignoreDiacritics = false;
    String ignoreDiacriticsVal = keyValues.get("morphology.ignoreDiacritics");
    if (ignoreDiacriticsVal != null && !ignoreDiacriticsVal.isEmpty()) {
      ignoreDiacritics = Boolean.parseBoolean(ignoreDiacriticsVal);
    }

    return new ZemberekGrpcConfiguration(
        port, lmPath, normalizationPath, extraDicts, ignoreDiacritics);
  }

  public static ZemberekGrpcConfiguration fromDataRoot(Path root) {
    Path lmPath = root.resolve("lm/lm.2gram.slm");
    IOUtil.checkFileArgument(lmPath, "Language model path");
    Path normalizationRoot = root.resolve("normalization");
    IOUtil.checkDirectoryArgument(normalizationRoot, "Normalization root path");
    return new ZemberekGrpcConfiguration(lmPath, normalizationRoot);
  }

  private static Path resolvePath(Path baseDir, String rawPath) {
    Path candidate = Paths.get(rawPath);
    if (!candidate.isAbsolute() && !Files.exists(candidate) && baseDir != null) {
      Path relativeToConfig = baseDir.resolve(candidate).normalize();
      if (Files.exists(relativeToConfig)) {
        return relativeToConfig;
      }
    }
    return candidate;
  }

  private static void validatePort(int port) {
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535, but was: " + port);
    }
  }
}
