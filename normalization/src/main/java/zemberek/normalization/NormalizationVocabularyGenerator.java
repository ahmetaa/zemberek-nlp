package zemberek.normalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import zemberek.core.collections.Histogram;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class NormalizationVocabularyGenerator {

  TurkishMorphology morphology;
  ReentrantLock lock = new ReentrantLock();

  public NormalizationVocabularyGenerator(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws Exception {

    AnalysisCache cache = AnalysisCache
        .builder()
        .dynamicCacheSize(100_000, 500_000).build();
    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .addDefaultBinaryDictionary()
        .disableUnidentifiedTokenAnalyzer()
        .setCache(cache)
        .build();

    NormalizationVocabularyGenerator generator = new NormalizationVocabularyGenerator(morphology);

    Path corporaRoot = Paths.get("/home/aaa/data/corpora");
    Path outRoot = Paths.get("/home/aaa/data/normalization/test");
    Path rootList = Paths.get("/home/aaa/data/corpora/vocab-list");
    List<String> rootNames = TextIO.loadLines(rootList, "#");

    List<Path> roots = new ArrayList<>();
    rootNames.forEach(s -> roots.add(corporaRoot.resolve(s)));

    List<Path> corpora = new ArrayList<>();
    for (Path corpusRoot : roots) {
      corpora.addAll(Files.walk(corpusRoot, 1)
          .filter(s -> s.toFile().isFile())
          .collect(Collectors.toList()));
    }

    Log.info("There are %d corpus files.", corpora.size());

    Files.createDirectories(outRoot);

    // create vocabularies
    generator.createVocabulary(
        corpora,
        Runtime.getRuntime().availableProcessors() / 2,
        outRoot);
  }


  private static class Vocabulary {

    Histogram<String> correct = new Histogram<>(100_000);
    Histogram<String> incorrect = new Histogram<>(100_000);
    Histogram<String> ignored = new Histogram<>(10_000);

    public String toString() {
      return String.format("Correct =%d Incorrect=%d Ignored=%d",
          correct.size(),
          incorrect.size(),
          ignored.size());
    }
  }

  void createVocabulary(List<Path> corpora, int threadCount, Path outRoot) throws IOException {
    Log.info("Thread count = %d", threadCount);
    Vocabulary vocabulary = collectVocabularyHistogram(corpora, threadCount);

    Path correct = outRoot.resolve("correct");
    vocabulary.correct.saveSortedByCounts(correct, " ");
    Path incorrect = outRoot.resolve("incorrect");
    vocabulary.incorrect.saveSortedByCounts(incorrect, " ");
    vocabulary.ignored.saveSortedByCounts(outRoot.resolve("ignored"), " ");
  }

  Vocabulary collectVocabularyHistogram(List<Path> corpora, int threadCount) {
    ExecutorService executorService = new BlockingExecutor(threadCount);
    CompletionService<Vocabulary> service =
        new ExecutorCompletionService<>(executorService);
    Vocabulary result = new Vocabulary();

    for (Path path : corpora) {
      Log.info("Processing %s", path);
      service.submit(new WordCollectorTask(path, result));
    }
    executorService.shutdown();
    return result;
  }

  class WordCollectorTask implements Callable<Vocabulary> {

    Path path;
    Vocabulary global;

    WordCollectorTask(Path path, Vocabulary global) {
      this.path = path;
      this.global = global;
    }

    @Override
    public Vocabulary call() throws Exception {
      Vocabulary local = new Vocabulary();
      LinkedHashSet<String> sentences = getSentences(path);
      for (String sentence : sentences) {
        List<String> tokens = TurkishTokenizer.DEFAULT.tokenizeToStrings(sentence);
        for (String token : tokens) {
          if (local.correct.contains(token) || global.correct.contains(token)) {
            local.correct.add(token);
            continue;
          }
          if (local.incorrect.contains(token) || global.incorrect.contains(token)) {
            local.incorrect.add(token);
            continue;
          }
          if (local.ignored.contains(token) ||
              global.ignored.contains(token) ||
              TurkishAlphabet.INSTANCE.containsDigit(token) ||
              TurkishAlphabet.INSTANCE.containsApostrophe(token) ||
              Character.isUpperCase(token.charAt(0))) {
            //local.ignored.add(token);
            continue;
          }
          token = token.toLowerCase(Turkish.LOCALE);
          WordAnalysis results = morphology.analyze(token);
          if (results.analysisCount() == 0) {
            local.incorrect.add(token);
          } else {
            local.correct.add(token);
          }
        }
      }
      Log.info("%s processed. %s", path, local.toString());
      try {
        lock.lock();
        global.correct.add(local.correct);
        global.incorrect.add(local.incorrect);
        //global.ignored.add(local.ignored);
        Log.info("Size of histogram = %d correct %d incorrect",
            global.correct.size(),
            global.incorrect.size());
      } finally {
        lock.unlock();
      }
      return local;
    }
  }

  LinkedHashSet<String> getSentences(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8).stream()
        .filter(s -> !s.startsWith("<"))
        .map(TextUtil::normalizeSpacesAndSoftHyphens)
        .collect(Collectors.toList());
    return new LinkedHashSet<>(TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines));
  }


}
