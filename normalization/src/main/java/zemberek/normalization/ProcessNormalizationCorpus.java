package zemberek.normalization;

import static zemberek.normalization.NormalizationVocabularyGenerator.getTurkishMorphology;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.morphology.TurkishMorphology;

public class ProcessNormalizationCorpus {

  public static final int BLOCK_SIZE = 1_000_000;

  final TurkishSentenceNormalizer normalizer;

  public ProcessNormalizationCorpus(TurkishSentenceNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  public static void main(String[] args) throws Exception {
    TurkishMorphology morphology = getTurkishMorphology();

    Path normalizationDataRoot = Paths.get("/home/aaa/data/normalization/test-large");
    Path lmPath = Paths.get("/home/aaa/data/normalization/lm.slm");

    TurkishSentenceNormalizer normalizationPreprocessor =
        new TurkishSentenceNormalizer(morphology, normalizationDataRoot, lmPath);

    ProcessNormalizationCorpus processor =
        new ProcessNormalizationCorpus(normalizationPreprocessor);

    Path corporaRoot = Paths.get("/home/aaa/data/corpora");
    Path outRoot = Paths.get("/home/aaa/data/normalization/corpus/clean");
    Path rootList = corporaRoot.resolve("clean-list");

    Files.createDirectories(outRoot);

    BlockTextLoader corpusProvider =
        BlockTextLoader.fromDirectoryRoot(corporaRoot, rootList, BLOCK_SIZE);

    int threadCount = Math.clamp(Runtime.getRuntime().availableProcessors() / 2, 1, 10);

    processor.process(corpusProvider, threadCount, outRoot);
    Log.info("Done.");
  }

  void process(
      BlockTextLoader corpusProvider,
      int threadCount,
      Path outRoot) throws Exception {
    ExecutorService service = new BlockingExecutor(threadCount);
    AtomicInteger c = new AtomicInteger(0);
    for (TextChunk chunk : corpusProvider) {
      service.submit(() -> {
        List<String> sentences = TextCleaner.cleanAndExtractSentences(chunk.getData()).stream()
            .map(normalizer::preProcess)
            .toList();
        Path p = outRoot.resolve(String.valueOf(c.getAndIncrement()));
        try {
          Files.write(p, sentences, StandardCharsets.UTF_8);
        } catch (IOException e) {
          Log.error(e.toString());
        }
        Log.info(c.get() * BLOCK_SIZE + " Lines processed.");
      });
    }
    service.shutdown();
    service.awaitTermination(1, TimeUnit.DAYS);
  }
}
