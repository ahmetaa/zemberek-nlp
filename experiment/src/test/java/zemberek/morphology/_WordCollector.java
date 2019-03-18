package zemberek.morphology;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.collections.Histogram;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.morphology._MorphologicalAmbiguityResolverExperiment.SingleAnalysisSentence;
import zemberek.core.turkish.Turkish;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class _WordCollector {

  public static void main(String[] args) throws IOException {
    Path p = Paths.get("/media/aaa/Data/corpora/final");
    Path outRoot = Paths.get("data/ambiguity");
    Files.createDirectories(outRoot);

    //mergeHistorgrams(outRoot);
    saveLists(Paths.get("data/ambiguity/all-counts-sorted-freq.txt"),
        Paths.get("data/zemberek-oflazer/new-analyzer-passed.txt"),
        Paths.get("data/zemberek-oflazer/new-analyzer-failed.txt"),
        outRoot
    );

/*

    List<Path> dirs = Files.walk(p, 1)
        .filter(s -> s.toFile().isDirectory() && s != p)
        .collect(Collectors.toList());

    for (Path dir : dirs) {
      new _WordCollector()
          .extractData(dir, outRoot, -1);
    }
*/
  }

  public Histogram<String> extracData(Path p, Path outRoot, int resultLimit)
      throws IOException {

    Histogram<String> words = new Histogram<>(5_000_000);

    List<Path> files = Files.walk(p, 1).filter(s -> s.toFile().isFile()
        && s.toFile().getName().endsWith(".corpus")).collect(Collectors.toList());
    LinkedHashSet<SingleAnalysisSentence> result = new LinkedHashSet<>();

    for (Path file : files) {
      Log.info("Processing %s", file);

      List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
          .filter(s -> !s.startsWith("<")).collect(Collectors.toList());
      List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines);

      for (String sentence : sentences) {
        sentence = sentence.replaceAll("[\\s/\\-\\u00a0]+", " ");
        sentence = sentence.replaceAll("[\\u00ad]", "");
        List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);

        for (Token token : tokens) {
          String rawWord = token.getText();
          if (!Strings.containsNone(rawWord, "0123456789_")) {
            continue;
          }
          String word = Character.isUpperCase(rawWord.charAt(0)) ?
              Turkish.capitalize(rawWord) : rawWord.toLowerCase(Turkish.LOCALE);
          words.add(word);
        }
      }

      Log.info("Count = %d", words.size());
    }
    String s = p.toFile().getName();

    Log.info("Saving words.");
    // saving failed words.
    words.saveSortedByKeys(outRoot.resolve(s + "-counts-sorted-name.txt"), " ",
        Turkish.STRING_COMPARATOR_ASC);
    // saving failed words by frequency.
    words.saveSortedByCounts(outRoot.resolve(s + "-counts-sorted-freq.txt"), " ");
    Files.write(outRoot.resolve(s + "-words-sorted-freq.txt"), words.getSortedList());
    Files.write(outRoot.resolve(s + "-words-sorted-name.txt"),
        words.getSortedList(Turkish.STRING_COMPARATOR_ASC));

    return words;
  }

  public static void mergeHistorgrams(Path path) throws IOException {
    List<Path> files = Files.walk(path, 1).filter(s -> s.toFile().isFile()
        && s.toFile().getName().endsWith("-counts-sorted-freq.txt")).collect(Collectors.toList());
    Histogram<String> words = new Histogram<>(10_000_000);
    for (Path file : files) {
      Log.info("Laoding histogram for %s", file);
      Histogram<String> h = Histogram.loadFromUtf8File(file, ' ');
      words.add(h);
    }
    saveHistogram(path, "all", words);
  }

  private static void saveHistogram(Path path, String name, Histogram<String> words)
      throws IOException {
    Log.info("Saving words.");
    // saving failed words.
    words.saveSortedByKeys(path.resolve(name + "-counts-sorted-name.txt"), " ",
        Turkish.STRING_COMPARATOR_ASC);
    // saving failed words by frequency.
    words.saveSortedByCounts(path.resolve(name + "-counts-sorted-freq.txt"), " ");
    Files.write(path.resolve(name + "-words-sorted-freq.txt"), words.getSortedList());
    Files.write(path.resolve(name + "-words-sorted-name.txt"),
        words.getSortedList(Turkish.STRING_COMPARATOR_ASC));
  }

  public static void saveLists(Path fullList, Path correctPath, Path falsepath, Path outRoot)
      throws IOException {

    Histogram<String> full = Histogram.loadFromUtf8File(fullList, ' ');
    List<String> correctList = Files.readAllLines(correctPath);
    Histogram<String> correct = new Histogram<>(full.size());
    for (String s : correctList) {
      correct.add(s, full.getCount(s));
    }
    saveHistogram(outRoot, "correct", correct);


    List<String> falseList = Files.readAllLines(falsepath);
    Histogram<String> falseHist = new Histogram<>(full.size());
    for (String s : falseList) {
      falseHist.add(s, full.getCount(s));
    }
    saveHistogram(outRoot, "false", falseHist);
  }

}
