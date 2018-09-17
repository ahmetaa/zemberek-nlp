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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
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
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class NormalizationVocabularyGenerator {

  TurkishMorphology morphology;
  ReentrantLock lock = new ReentrantLock();

  public NormalizationVocabularyGenerator(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws Exception {

    AnalysisCache cache = AnalysisCache
        .builder()
        .dynamicCacheSize(300_000, 700_000).build();

    RootLexicon lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");

    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .useLexicon(lexicon)
        .disableUnidentifiedTokenAnalyzer()
        .morphotactics(new InformalTurkishMorphotactics(lexicon))
        .setCache(cache)
        .build();

    NormalizationVocabularyGenerator generator = new NormalizationVocabularyGenerator(morphology);

    Path corporaRoot = Paths.get("/media/aaa/Data/corpora/reduced");
    Path outRoot = Paths.get("/home/aaa/data/normalization/test");
    Path rootList = Paths.get("/media/aaa/Data/corpora/reduced/corpora.list");
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
    int threadCount = Runtime.getRuntime().availableProcessors() / 2;
    if (threadCount > 20) {
      threadCount = 20;
    }
    generator.createVocabulary(
        corpora,
        threadCount,
        outRoot);
  }

  static class Vocabulary {

    Histogram<String> correct = new Histogram<>(100_000);
    Histogram<String> incorrect = new Histogram<>(100_000);
    Histogram<String> ignored = new Histogram<>(10_000);

    public Vocabulary() {
    }

    public Vocabulary(Histogram<String> correct,
        Histogram<String> incorrect, Histogram<String> ignored) {
      this.correct = correct;
      this.incorrect = incorrect;
      this.ignored = ignored;
    }

    public String toString() {
      return String.format("Correct =%d Incorrect=%d Ignored=%d",
          correct.size(),
          incorrect.size(),
          ignored.size());
    }

  }

  void createVocabulary(List<Path> corpora, int threadCount, Path outRoot) throws Exception {
    Log.info("Thread count = %d", threadCount);
    Vocabulary vocabulary = collectVocabularyHistogram(corpora, threadCount);

    Path correct = outRoot.resolve("correct");
    vocabulary.correct.saveSortedByCounts(correct, " ");
    Path incorrect = outRoot.resolve("incorrect");
    vocabulary.incorrect.saveSortedByCounts(incorrect, " ");
    vocabulary.ignored.saveSortedByCounts(outRoot.resolve("ignored"), " ");
  }

  Vocabulary collectVocabularyHistogram(List<Path> corpora, int threadCount) throws Exception {
    ExecutorService executorService = new BlockingExecutor(threadCount);
    CompletionService<Vocabulary> service =
        new ExecutorCompletionService<>(executorService);
    Vocabulary result = new Vocabulary();

    for (Path path : corpora) {
      Log.info("Processing %s", path);
      service.submit(new WordCollectorTask(path, result));
    }
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.DAYS);
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
        List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
        for (Token token : tokens) {
          String s = token.getText();
          if (local.correct.contains(s) || global.correct.contains(s)) {
            local.correct.add(s);
            continue;
          }
          if (local.incorrect.contains(s) || global.incorrect.contains(s)) {
            local.incorrect.add(s);
            continue;
          }
          // TODO: fix below.
          if (token.getType() == TurkishLexer.URL ||
              token.getType() == TurkishLexer.Punctuation ||
              token.getType() == TurkishLexer.Email ||
              token.getType() == TurkishLexer.HashTag ||
              token.getType() == TurkishLexer.Mention ||
              token.getType() == TurkishLexer.Emoticon ||
              local.ignored.contains(s) ||
              global.ignored.contains(s) ||
              TurkishAlphabet.INSTANCE.containsDigit(s) /*||
              TurkishAlphabet.INSTANCE.containsApostrophe(s) ||
              Character.isUpperCase(s.charAt(0))*/) {
            //local.ignored.add(s);
            continue;
          }
          s = s.toLowerCase(Turkish.LOCALE);
          WordAnalysis results = morphology.analyze(s);
          if (results.analysisCount() == 0) {
            local.incorrect.add(s);
          } else {
            local.correct.add(s);
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
