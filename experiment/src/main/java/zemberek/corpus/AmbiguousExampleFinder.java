package zemberek.corpus;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class AmbiguousExampleFinder {

  CorpusSearcher searcher;

  public AmbiguousExampleFinder(CorpusSearcher searcher) {
    this.searcher = searcher;
  }

  public List<String> getSentences(
      String word,
      int sentenceCount,
      int minTokenCount,
      int maxTokenCount) throws Exception {

    // load and eliminate duplicates.
    LinkedHashSet<String> sentences = new LinkedHashSet<>(searcher.search(word, 1000));

    // filter and sort from smaller to larger sentences.
    List<String> filtered = sentences.stream()
        .filter(s -> {
          int k = s.split(" ").length;
          return k >= minTokenCount && k <= maxTokenCount;
        })
        .filter(s -> !s.contains("\""))
        .filter(s -> !s.contains(")"))
        .filter(s -> !s.contains("-"))
        //.sorted(Comparator.comparingInt(a -> a.split(" ").length))
        .collect(Collectors.toList());

    Collections.shuffle(filtered);

    int max = filtered.size() < sentenceCount ? filtered.size() : sentenceCount;
    return new ArrayList<>(filtered.subList(0, max));
  }

  public static void main(String[] args) throws Exception {
    TurkishMorphology morphology =  TurkishMorphology.createWithDefaults();
    Path indexRoot = Paths.get("/home/aaa/data/zemberek/corpus-index");
    CorpusSearcher searcher = new CorpusSearcher(indexRoot);
    AmbiguousExampleFinder finder = new AmbiguousExampleFinder(searcher);
    extractSentences(morphology, finder);
  }

  private static void extractSentences(TurkishMorphology morphology, AmbiguousExampleFinder finder)
      throws Exception {
    List<String> ambiguousWords = Files.readAllLines(
        Paths.get("data/ambiguity/zemberek-ambigious-words.txt"),
        StandardCharsets.UTF_8)
        .subList(0, 100);

    Path out = Paths.get("data/ambiguity/sentences.txt");
    Path morph = Paths.get("data/ambiguity/sentences.morph.txt");
    try (
        PrintWriter pw = new PrintWriter(out.toFile(), "utf-8");
        PrintWriter pwMorph = new PrintWriter(morph.toFile(), "utf-8")) {
      for (String word : ambiguousWords) {
        Log.info(word);
        List<String> sentences = finder.getSentences(word, 3, 5, 10);
        pw.println(word);
        sentences.forEach(pw::println);
        pw.println();

        for (String sentence : sentences) {

          SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);
          if (containsUnkown(analysis)) {
            continue;
          }

          pwMorph.format("S:%s%n", sentence);
          for (SentenceWordAnalysis sw : analysis) {
            WordAnalysis wa = sw.getWordAnalysis();
            pwMorph.println(wa.getInput());

            SingleAnalysis best = sw.getBestAnalysis();
            for (SingleAnalysis singleAnalysis : wa) {
              boolean isBest = singleAnalysis.equals(best);
              if (wa.analysisCount() == 1) {
                pwMorph.println(singleAnalysis.formatLong());
              } else {
                pwMorph.format("%s%s%n", singleAnalysis.formatLong(), isBest ? "*" : "");
              }
            }
          }
          pwMorph.println();
        }
      }
    }
  }

  private static boolean containsUnkown(SentenceAnalysis analysis) {
    for (SingleAnalysis s : analysis.bestAnalysis()) {
      if (s == null || s.isUnknown()) {
        return true;
      }
    }
    return false;
  }


}
