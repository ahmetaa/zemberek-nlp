package zemberek.classification;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;

public class ItemFindExperiment {

  public static void main(String[] args) throws IOException {
    preProcessData();
  }

  static void preProcessData() throws IOException {
    Path p = Paths.get("/home/aaa/data/foo/true.txt");

    List<String> allTrue = TextIO.loadLines(p);
    Log.info("%d true lines", allTrue.size());
    LinkedHashSet<String> unique = new LinkedHashSet<>(allTrue);
    Log.info("Unique lines = %d", unique.size());

    Path none = Paths.get("/home/aaa/data/foo/none-utf8.txt");
    List<String> allNone = TextIO.loadLines(none);
    allNone = allNone.stream().map(s -> s.replaceAll("[(].+?[)]", " ")
        .replaceAll("\\s+", " ").trim())
        .collect(Collectors.toList());
    allNone.forEach(Log::info);


  }


}
