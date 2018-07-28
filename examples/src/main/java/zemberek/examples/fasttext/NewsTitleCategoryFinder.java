package zemberek.examples.fasttext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import zemberek.core.collections.Histogram;

public class NewsTitleCategoryFinder {

  public static void main(String[] args) throws IOException {
    NewsTitleCategoryFinder experiment = new NewsTitleCategoryFinder();
    Path dataPath = Paths.get("/home/ahmetaa/data/fasttext/cat-example/news-title-category-set");
    experiment.preprocess(dataPath);
  }

  public void preprocess(Path dataPath) throws IOException {

    Path root = dataPath.getParent();
    List<String> lines = Files.readAllLines(dataPath, StandardCharsets.UTF_8);
    Collections.shuffle(lines, new Random(1));

    Files.write(dataPath, lines);

    Path tokenizedPath = root.resolve("news-title.tokenized");

    Path lemmaPath = root.resolve("news-title.lemmas");
  }

  void dataInfo(List<String> lines) {
    System.out.println("Total lines = " + lines.size());
    Histogram<String> hist = new Histogram<>();
    lines.stream()
        .map(s -> s.substring(0, s.indexOf(' ')))
        .forEach(hist::add);
    System.out.println("Categories :");
    for (String s : hist.getSortedList()) {
      System.out.println(s + " " + hist.getCount(s));
    }
  }


}
