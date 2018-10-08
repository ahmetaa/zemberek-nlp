package zemberek.core.text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class TextIO {

  public static String loadUtfAsString(Path filePath) throws IOException {
    return String.join("\n", Files.readAllLines(filePath, StandardCharsets.UTF_8));
  }

  /**
   * Loads lines from a UTF-8 encoded text file. Ignores empty lines.
   */
  public static List<String> loadLines(Path path) throws IOException {
    return Files.readAllLines(path, StandardCharsets.UTF_8)
        .stream()
        .filter(s -> s.trim().length() > 0)
        .collect(Collectors.toList());
  }

  public static long lineCount(Path p) throws IOException {
    try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
      int count = 1;
      for (int c = 0; c != -1; c = br.read()) {
        count += c == '\n' ? 1 : 0;
      }
      return count;
    }

  }

  public static List<String> loadLines(Path path, String ignorePrefix) throws IOException {
    return Files.readAllLines(path, StandardCharsets.UTF_8)
        .stream()
        .filter(s -> s.trim().length() > 0 &&
            (ignorePrefix == null || !s.trim().startsWith(ignorePrefix)))
        .collect(Collectors.toList());
  }

  public static List<String> loadLinesFromCompressed(Path path) throws IOException {
    try (InputStream gzipStream = new GZIPInputStream(new FileInputStream(path.toFile()))) {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(gzipStream, StandardCharsets.UTF_8));
      return reader.lines()
          .filter(s -> s.trim().length() > 0)
          .collect(Collectors.toCollection(ArrayList::new));
    }
  }

  public static long charCount(Path path, Charset charset) throws IOException {
    BufferedReader reader = Files.newBufferedReader(path, charset);
    char[] buf = new char[4096];
    long count = 0;
    while (true) {
      int k = reader.read(buf);
      if (k == -1) {
        break;
      }
      count += k;
    }
    return count;
  }

  public static List<String> loadLinesFromResource(String resourcePath) throws IOException {
    return loadLinesFromResource(resourcePath, null);
  }

  public static List<String> loadLinesFromCompressedResource(String resourcePath)
      throws IOException {
    try (InputStream gzipStream =
        new GZIPInputStream(TextIO.class.getResourceAsStream(resourcePath))) {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(gzipStream, StandardCharsets.UTF_8));
      return reader.lines()
          .filter(s -> s.trim().length() > 0)
          .collect(Collectors.toCollection(ArrayList::new));
    }
  }


  public static List<String> loadLinesFromResource(String resourcePath, String ignorePrefix)
      throws IOException {
    if (!resourcePath.startsWith("/")) {
      resourcePath = "/" + resourcePath;
    }
    try (InputStream is = TextIO.class.getResourceAsStream(resourcePath)) {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(is, StandardCharsets.UTF_8));
      return reader.lines()
          .filter(s -> s.trim().length() > 0 &&
              (ignorePrefix == null || !s.trim().startsWith(ignorePrefix)))
          .collect(Collectors.toList());
    }
  }

  public static Path createTempFile(String content) throws IOException {
    return createTempFile(Collections.singletonList(content));
  }

  public static Path createTempFile(String... content) throws IOException {
    return createTempFile(Arrays.asList(content));
  }

  public static Path createTempFile(List<String> content) throws IOException {
    Path temp = Files.createTempFile("tmp", ".tmp");
    temp.toFile().deleteOnExit();
    Files.write(temp, content, StandardCharsets.UTF_8);
    return temp;
  }
}
