package zemberek.normalization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import zemberek.core.collections.Histogram;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;

public class NormalizationVocabularyGenerator {

  private TurkishMorphology morphology;
  private ReentrantLock lock = new ReentrantLock();
  boolean normalize;

  public NormalizationVocabularyGenerator(TurkishMorphology morphology) {
    this.morphology = morphology;
    this.normalize = true;
  }

  public NormalizationVocabularyGenerator(TurkishMorphology morphology, boolean normalize) {
    this.morphology = morphology;
    this.normalize = normalize;
  }

  public static void main(String[] args) throws Exception {

    TurkishMorphology morphology = getTurkishMorphology();

    NormalizationVocabularyGenerator generator = new NormalizationVocabularyGenerator(morphology);

    Path corporaRoot = Paths.get("/home/aaa/data/normalization/corpus");
    Path outRoot = Paths.get("/home/aaa/data/normalization/vocab-clean");
    Path rootList = corporaRoot.resolve("clean-list");

    BlockTextLoader corpusProvider = BlockTextLoader
        .fromDirectoryRoot(corporaRoot, rootList, 30_000);

    Files.createDirectories(outRoot);

    // create vocabularies
    int threadCount = Runtime.getRuntime().availableProcessors() / 2;
    if (threadCount > 22) {
      threadCount = 22;
    }

    generator.createVocabulary(
        corpusProvider,
        threadCount,
        outRoot);
  }

  static TurkishMorphology getTurkishMorphology() throws IOException {
    return getTurkishMorphology(false);
  }

  static TurkishMorphology getTurkishMorphology(boolean asciiTolerant) throws IOException {
    AnalysisCache cache = AnalysisCache
        .builder()
        .dynamicCacheSize(200_000, 400_000).build();

    RootLexicon lexicon = TurkishDictionaryLoader.loadFromResources(
        "tr/master-dictionary.dict",
        "tr/non-tdk.dict",
        "tr/proper.dict",
        "tr/proper-from-corpus.dict",
        "tr/abbreviations.dict",
        "tr/person-names.dict"
    );

    TurkishMorphology.Builder builder = TurkishMorphology
        .builder()
        .setLexicon(lexicon)
        .disableUnidentifiedTokenAnalyzer()
        .setCache(cache);
    if (asciiTolerant) {
      builder.ignoreDiacriticsInAnalysis();
    }
    return builder.build();
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

    void checkConsistency() {
      Set<String> intersectionOfKeys = incorrect.getIntersectionOfKeys(correct);
      int sharedKeyCount = intersectionOfKeys.size();
      if (sharedKeyCount > 0) {
        Log.warn("Incorrect and correct sets share %d keys", sharedKeyCount);
      }
      sharedKeyCount = incorrect.getIntersectionOfKeys(ignored).size();
      if (sharedKeyCount > 0) {
        Log.warn("Incorrect and ignored sets share %d keys", sharedKeyCount);
      }
      sharedKeyCount = correct.getIntersectionOfKeys(ignored).size();
      if (sharedKeyCount > 0) {
        Log.warn("Correct and ignored sets share %d keys", sharedKeyCount);
      }
    }

  }

  void createVocabulary(
      BlockTextLoader corpora,
      int threadCount,
      Path outRoot) throws Exception {

    Log.info("Thread count = %d", threadCount);
    Vocabulary vocabulary = collectVocabularyHistogram(corpora, threadCount);

    Log.info("Checking consistency.");
    vocabulary.checkConsistency();

    Log.info("Saving vocabularies.");

    vocabulary.correct.saveSortedByCounts(outRoot.resolve("correct"), " ");
    vocabulary.correct.saveSortedByKeys(
        outRoot.resolve("correct.abc"),
        " ", String::compareTo);

    vocabulary.incorrect.saveSortedByCounts(outRoot.resolve("incorrect"), " ");
    vocabulary.incorrect.saveSortedByKeys(
        outRoot.resolve("incorrect.abc"),
        " ",
        String::compareTo);

    vocabulary.ignored.saveSortedByCounts(outRoot.resolve("ignored"), " ");
    vocabulary.ignored.saveSortedByKeys(
        outRoot.resolve("ignored.abc"),
        " ",
        String::compareTo);
  }

  Vocabulary collectVocabularyHistogram(BlockTextLoader corpora, int threadCount)
      throws Exception {

    ExecutorService executorService = new BlockingExecutor(threadCount);
    Vocabulary result = new Vocabulary();

    for (TextChunk chunk : corpora) {
      Log.info("Processing %s", chunk);
      executorService.submit(new WordCollectorTask(chunk, result));
    }
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.DAYS);
    return result;
  }

  class WordCollectorTask implements Callable<Vocabulary> {

    TextChunk chunk;
    Vocabulary globalVocabulary;

    WordCollectorTask(TextChunk chunk, Vocabulary globalVocabulary) {
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
          if (token.getType() == Type.URL ||
              token.getType() == Type.Punctuation ||
              token.getType() == Type.Email ||
              token.getType() == Type.HashTag ||
              token.getType() == Type.Mention ||
              token.getType() == Type.Emoticon ||
              token.getType() == Type.Unknown ||
              local.ignored.contains(s) ||
              globalVocabulary.ignored.contains(s) ||
              TurkishAlphabet.INSTANCE.containsDigit(s) /*||
              TurkishAlphabet.INSTANCE.containsApostrophe(s) ||
              Character.isUpperCase(s.charAt(0))*/) {
            local.ignored.add(s);
            continue;
          }
          if (normalize) {
            s = s.toLowerCase(Turkish.LOCALE);
            s = s.replaceAll("'", "");
          }
          WordAnalysis results = morphology.analyze(s);
          if (results.analysisCount() == 0) {
            local.incorrect.add(s);
          } else {
            local.correct.add(s);
          }
        }
      }
      Log.info("%s processed. %s", chunk.toString(), local.toString());
      try {
        lock.lock();
        globalVocabulary.correct.add(local.correct);
        globalVocabulary.incorrect.add(local.incorrect);
        globalVocabulary.ignored.add(local.ignored);
        Log.info("Correct = %d, Incorrect = %d, Ignored = %d",
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
