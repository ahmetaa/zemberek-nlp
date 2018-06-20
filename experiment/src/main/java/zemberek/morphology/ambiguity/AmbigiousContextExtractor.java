package zemberek.morphology.ambiguity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.tokenization.TurkishTokenizer;

public class AmbigiousContextExtractor extends AmbiguityScriptsBase {

  public AmbigiousContextExtractor() throws IOException {
    super();
    acceptWordPredicates.add(maxAnalysisCount(10));
    acceptWordPredicates.add(hasAnalysis());
    ignoreSentencePredicates.add(contains("\""));
    ignoreSentencePredicates.add(contains("…"));
    ignoreSentencePredicates.add(probablyNotTurkish());
    ignoreSentencePredicates.add(tooLongSentence(25));
  }

  public static void main(String[] args) throws IOException {
    //Path p = Paths.get("/media/aaa/Data/corpora/final/www.aljazeera.com.tr");
    //Path p = Paths.get("/home/ahmetaa/data/zemberek/data/corpora/www.aljazeera.com.tr");
    Path p = Paths.get("/home/ahmetaa/data/zemberek/data/corpora/open-subtitles");
    //Path p = Paths.get("/home/ahmetaa/data/zemberek/data/corpora/wowturkey.com");
    //Path p = Paths.get("/media/aaa/Data/corpora/final/open-subtitles");
    //Path p = Paths.get("/media/aaa/Data/corpora/final/wowturkey.com");
    Path outRoot = Paths.get("data/ambiguity");
    Files.createDirectories(outRoot);

    List<String> words = Files.readAllLines(outRoot.resolve("zemberek-ambigious-words.txt"));

    AmbigiousContextExtractor e = new AmbigiousContextExtractor();

    List<Path> files = Files.walk(p, 1).filter(s -> s.toFile().isFile())
        .collect(Collectors.toList());

    Multimap<String, String> result = HashMultimap.create();

    for (Path file : files) {
      Log.info("Processing %s", file);
      LinkedHashSet<String> sentences = e.getSentences(file);
      //for (int i = 0; i < 10; i++) {
        //String s = words.get(i);
        String s = "kazan";
        LinkedHashSet<String> partials = e.extract(s, 2, 200, sentences);
        System.out.println(s);
        for (String partial : partials) {
          System.out.println(partial);
        }
      //}
      //break;
    }

  }

  public LinkedHashSet<String> extract(String word, int contextSize, int sentenceCount,
      LinkedHashSet<String> sentences) {

    LinkedHashSet<String> partials = new LinkedHashSet<>();
    List<List<String>> group = group(new ArrayList<>(sentences), 5000);

    for (List<String> lines : group) {
      LinkedHashSet<String> toProcess = getAccpetableSentences(lines);
      for (String sentence : toProcess) {
        if (sentence.contains(word)) {
          List<String> words = TurkishTokenizer.DEFAULT.tokenizeToStrings(sentence);
          for (int i = 0; i < words.size(); i++) {
            if (words.get(i).equals(word)) {
              int start = i - contextSize < 0 ? 0 : i - contextSize;
              int end = i + contextSize >= words.size() ? words.size() : i + contextSize + 1;
              String s = String.join(" ", words.subList(start, end));
              partials.add(s);
              System.out.println(s);
              if (partials.size() > sentenceCount) {
                return partials;
              }
              break;
            }
          }
        }
      }
    }
    return partials;
  }


}
