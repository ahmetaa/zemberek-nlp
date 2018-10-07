package zemberek.normalization;

import static zemberek.normalization.NormalizationVocabularyGenerator.getTurkishMorphology;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.logging.Log;
import zemberek.core.text.MultiPathBlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.morphology.TurkishMorphology;

public class ProcessNormalizationCorpus {

  public static final int BLOCK_SIZE = 30_000;

  NormalizationPreprocessor preprocessor;

  public ProcessNormalizationCorpus(NormalizationPreprocessor preprocessor) {
    this.preprocessor = preprocessor;
  }

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = getTurkishMorphology();

    NormalizationPreprocessor preprocessor = new NormalizationPreprocessor(
        morphology, null, null);

    ProcessNormalizationCorpus processor = new ProcessNormalizationCorpus(preprocessor);

    Path corporaRoot = Paths.get("/home/aaa/data/corpora");
    Path outRoot = Paths.get("/home/aaa/data/normalization/corpus");
    Path rootList = corporaRoot.resolve("noisy-list-small");

    MultiPathBlockTextLoader corpusProvider = MultiPathBlockTextLoader
        .fromDirectoryRoot(corporaRoot, rootList, BLOCK_SIZE);

    Files.createDirectories(outRoot);

    // create vocabularies
    int threadCount = Runtime.getRuntime().availableProcessors() / 2;
    if (threadCount > 20) {
      threadCount = 20;
    }

    processor.process(corpusProvider, threadCount, outRoot);

  }

  void process(
      MultiPathBlockTextLoader corpusProvider,
      int threadCount,
      Path outRoot) throws IOException {

    try (PrintWriter pw = new PrintWriter(outRoot.resolve("normalization-corpus").toFile(),
        "utf-8")) {

      ExecutorService service = new BlockingExecutor(threadCount);
      int c = 0;
      for (TextChunk chunk : corpusProvider) {
        service.submit(() -> {
          List<String> sentences = TextCleaner.cleanAndExtractSentences(chunk.getData());
          sentences = sentences.stream()
              .map(s -> preprocessor.preProcess(s))
              .collect(Collectors.toList());
          sentences.forEach(pw::println);
        });
        c++;
        if (c % 10 == 0) {
          Log.info(c * BLOCK_SIZE + " Lines processed.");
        }
      }
    }
  }

}
