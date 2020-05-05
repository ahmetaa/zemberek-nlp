package zemberek.corpus;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.normalization.TextCleaner;
import zemberek.tokenization.TurkishTokenizer;

public class StemDisambiguationExperiment {

  private static TurkishMorphology morphology;

  Path input = Paths.get("");
  Path dirList = Paths.get("");
  Path output = Paths.get("");
  boolean recurse = true;
  boolean toLowercase = false;
  int threadCount = Runtime.getRuntime().availableProcessors() / 2;

  public static void main(String[] args) throws IOException {
    StemDisambiguationExperiment experiment = new StemDisambiguationExperiment();
    Path root = Paths.get("/media/ahmetaa/depo/corpora");
    experiment.input = root;
    experiment.dirList = root.resolve("dis-list");
    experiment.output = Paths.get("/media/ahmetaa/depo/out/foo");
    AnalysisCache cache = AnalysisCache.builder()
        .staticCacheSize(50_000)
        .dynamicCacheSize(50_000, 200_000)
        .build();
    morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setCache(cache)
        .build();
    experiment.doit();
  }

  private void doit() throws IOException {
    System.setProperty("org.jline.terminal.dumb", "true");
    List<Path> paths = new ArrayList<>();
    if (input.toFile().isFile()) {
      paths.add(input);
    } else {
      Set<String> dirNamesToProcess = new HashSet<>();
      if (dirList != null) {
        List<String> dirNames = TextIO.loadLines(dirList, "#");
        Log.info("Directory names to process:");
        for (String dirName : dirNames) {
          Log.info(dirName);
        }
        dirNamesToProcess.addAll(dirNames);
      }

      List<Path> directories =
          Files.walk(input, recurse ? Integer.MAX_VALUE : 1)
              .filter(s -> s.toFile().isDirectory() && !s.equals(input))
              .collect(Collectors.toList());

      for (Path directory : directories) {
        if (dirList != null && !dirNamesToProcess.contains(directory.toFile().getName())) {
          continue;
        }
        paths.addAll(Files.walk(directory, 1)
            .filter(s -> s.toFile().isFile())
            .collect(Collectors.toList()));
      }
    }
    Log.info("There are %d files to process.", paths.size());
    long totalLines = 0;
    for (Path path : paths) {
      totalLines += TextIO.lineCount(path);
    }

    if (paths.size() == 0) {
      Log.info("No corpus files found for input : %s", input);
      System.exit(0);
    }

    AtomicLong sentenceCount = new AtomicLong(0);

    try (
        PrintWriter pw = new PrintWriter(output.toFile(), "UTF-8")) {

      BlockTextLoader loader = BlockTextLoader.fromPaths(paths, 30_000);
      BlockingExecutor executor =
          new BlockingExecutor(threadCount);

      for (TextChunk chunk : loader) {
        executor.submit(() -> {
          List<String> data = new ArrayList<>(new LinkedHashSet<>(chunk.getData()));
          List<String> sentences = TextCleaner.cleanAndExtractSentences(data);
          sentences = sentences.stream()
              .filter(this::unambiguous)
              .map(s -> toLowercase ? s.toLowerCase(Turkish.LOCALE) : s)
              .collect(Collectors.toList());

          synchronized (this) {
            sentences.forEach(pw::println);
            sentenceCount.addAndGet(sentences.size());
            System.out.println(chunk.size());
          }
        });
      }
      executor.shutdown();
    }

    Log.info("%d sentences are written in %s", sentenceCount.get(), output);
  }

  private boolean unambiguous(String sentence) {
    for (String token : TurkishTokenizer.DEFAULT.tokenizeToStrings(sentence)) {
      WordAnalysis analyses = morphology.analyze(token);
      Set<String> lemmas = new HashSet<>();
      for (SingleAnalysis analysis : analyses) {
        lemmas.add(analysis.getDictionaryItem().normalizedLemma());
      }
      if (lemmas.size() > 1) {
        return false;
      }
    }
    return true;
  }

}
