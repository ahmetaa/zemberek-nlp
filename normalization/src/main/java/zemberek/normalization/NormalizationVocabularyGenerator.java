package zemberek.normalization;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.antlr.v4.runtime.Token;
import zemberek.core.collections.Histogram;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.logging.Log;
import zemberek.core.text.MultiPathBlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class NormalizationVocabularyGenerator {

  private TurkishMorphology morphology;
  private ReentrantLock lock = new ReentrantLock();

  public NormalizationVocabularyGenerator(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws Exception {

    AnalysisCache cache = AnalysisCache
        .builder()
        .dynamicCacheSize(400_000, 800_000).build();

    RootLexicon lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");

    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .useLexicon(lexicon)
        .disableUnidentifiedTokenAnalyzer()
        .morphotactics(new InformalTurkishMorphotactics(lexicon))
        .setCache(cache)
        .build();

    NormalizationVocabularyGenerator generator = new NormalizationVocabularyGenerator(morphology);

    Path corporaRoot = Paths.get("/home/aaa/data/corpora");
    Path outRoot = Paths.get("/home/aaa/data/normalization/vocab-noisy");
    Path rootList = corporaRoot.resolve("noisy-list");

    MultiPathBlockTextLoader corpusProvider = MultiPathBlockTextLoader
        .fromDirectoryRoot(corporaRoot, rootList, 30_000);

    Files.createDirectories(outRoot);

    // create vocabularies
    int threadCount = Runtime.getRuntime().availableProcessors() / 2;
    if (threadCount > 20) {
      threadCount = 20;
    }

    generator.createVocabulary(
        corpusProvider,
        threadCount,
        outRoot);
  }

  static class Vocabulary {

    Histogram<String> correct = new Histogram<>(100_000);
    Histogram<String> incorrect = new Histogram<>(100_000);
    Histogram<String> ignored = new Histogram<>(100_000);

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

  void createVocabulary(
      MultiPathBlockTextLoader corpora,
      int threadCount,
      Path outRoot) throws Exception {

    Log.info("Thread count = %d", threadCount);
    Vocabulary vocabulary = collectVocabularyHistogram(corpora, threadCount);

    Path correct = outRoot.resolve("correct");
    vocabulary.correct.saveSortedByCounts(correct, " ");
    Path incorrect = outRoot.resolve("incorrect");
    vocabulary.incorrect.saveSortedByCounts(incorrect, " ");
    vocabulary.ignored.saveSortedByCounts(outRoot.resolve("ignored"), " ");
  }

  Vocabulary collectVocabularyHistogram(MultiPathBlockTextLoader corpora, int threadCount)
      throws Exception {

    ExecutorService executorService = new BlockingExecutor(threadCount);
    Vocabulary result = new Vocabulary();

    for (TextChunk chunk : corpora) {
      Log.info("Processing %s", chunk.id);
      executorService.submit(new WordCollectorTask(chunk, result));
    }
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.DAYS);
    return result;
  }

  class WordCollectorTask implements Callable<Vocabulary> {

    TextChunk chunk;
    Vocabulary globalVocabulary;

    public WordCollectorTask(TextChunk chunk, Vocabulary globalVocabulary) {
      this.chunk = chunk;
      this.globalVocabulary = globalVocabulary;
    }

    @Override
    public Vocabulary call() {
      Vocabulary local = new Vocabulary();
      List<String> sentences = TextCleaner.cleanAndExtractSentences(chunk.getData());
      for (String sentence : sentences) {
        List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
        for (Token token : tokens) {
          String s = token.getText();
          if (local.correct.contains(s) || globalVocabulary.correct.contains(s)) {
            local.correct.add(s);
            continue;
          }
          if (local.incorrect.contains(s) || globalVocabulary.incorrect.contains(s)) {
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
              token.getType() == TurkishLexer.Unknown ||
              local.ignored.contains(s) ||
              globalVocabulary.ignored.contains(s) ||
              TurkishAlphabet.INSTANCE.containsDigit(s) /*||
              TurkishAlphabet.INSTANCE.containsApostrophe(s) ||
              Character.isUpperCase(s.charAt(0))*/) {
            local.ignored.add(s);
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
      Log.info("%s processed. %s", chunk.id, local.toString());
      try {
        lock.lock();
        globalVocabulary.correct.add(local.correct);
        globalVocabulary.incorrect.add(local.incorrect);
        globalVocabulary.ignored.add(local.ignored);
        Log.info("Size of histogram = %d correct %d incorrect %d ignored",
            globalVocabulary.correct.size(),
            globalVocabulary.incorrect.size(),
            globalVocabulary.ignored.size()
        );
      } finally {
        lock.unlock();
      }
      return local;
    }
  }

}
