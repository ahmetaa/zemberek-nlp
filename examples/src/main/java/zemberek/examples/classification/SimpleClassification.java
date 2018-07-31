package zemberek.examples.classification;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import zemberek.classification.FastTextClassifier;
import zemberek.core.ScoredItem;
import zemberek.core.turkish.Turkish;
import zemberek.tokenization.TurkishTokenizer;

public class SimpleClassification {

  public static void main(String[] args) throws IOException {
    // assumes models are generated with NewsTitleCategoryFinder
    Path path = Paths.get("/media/ahmetaa/depo/zemberek/data/classification/news-title-category-set.tokenized.model");
    FastTextClassifier classifier = FastTextClassifier.load(path);

    String s = "Beşiktaş berabere kaldı.";

    // process the input exactly the way trainin set is processed
    String processed = String.join(" ", TurkishTokenizer.DEFAULT.tokenizeToStrings(s));
    processed = processed.toLowerCase(Turkish.LOCALE);

    // results, only top three.
    List<ScoredItem<String>> res = classifier.predict(processed, 3);

    for (ScoredItem<String> re : res) {
      System.out.println(re);
    }
  }

}
