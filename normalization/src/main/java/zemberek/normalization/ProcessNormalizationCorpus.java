package zemberek.normalization;

import static zemberek.normalization.NormalizationVocabularyGenerator.getTurkishMorphology;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.logging.Log;
import zemberek.core.text.MultiPathBlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;

public class ProcessNormalizationCorpus {

  public static final int BLOCK_SIZE = 500_000;

  NormalizationPreprocessor preprocessor;

  public ProcessNormalizationCorpus(NormalizationPreprocessor preprocessor) {
    this.preprocessor = preprocessor;
  }

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = getTurkishMorphology();

    Path normalizationDataRoot =
        Paths.get("/home/aaa/data/normalization/test-large");
    Path lmPath = Paths.get("/home/aaa/data/normalization/lm.slm");
    SmoothLm lm = SmoothLm.builder(lmPath).logBase(Math.E).build();
    Log.info("Language model = %s", lm.info());

    NormalizationPreprocessor preprocessor = new NormalizationPreprocessor(
        morphology, normalizationDataRoot, lm);

    ProcessNormalizationCorpus processor = new ProcessNormalizationCorpus(preprocessor);

    Path corporaRoot = Paths.get("/home/aaa/data/corpora");
    Path outRoot = Paths.get("/home/aaa/data/normalization/corpus/noisy");
    Path rootList = corporaRoot.resolve("noisy-list-small");

    Files.createDirectories(outRoot);

    MultiPathBlockTextLoader corpusProvider = MultiPathBlockTextLoader
        .fromDirectoryRoot(corporaRoot, rootList, BLOCK_SIZE);

    // create vocabularies
    int threadCount = Runtime.getRuntime().availableProcessors() / 2;
    if (threadCount > 10) {
      threadCount = 10;
    }

    processor.process(corpusProvider, threadCount, outRoot);

  }

  void process(
      MultiPathBlockTextLoader corpusProvider,
      int threadCount,
      Path outRoot) throws IOException {

    ExecutorService service = new BlockingExecutor(threadCount);
    AtomicInteger c = new AtomicInteger(0);
    for (TextChunk chunk : corpusProvider) {
      service.submit(() -> {
        List<String> sentences = TextCleaner.cleanAndExtractSentences(chunk.getData());
        sentences = sentences.stream()
            .map(s -> preprocessor.preProcess(s))
            .collect(Collectors.toList());
        Path p = outRoot.resolve(String.valueOf(c.getAndIncrement()));
        try {
          Files.write(p, sentences, StandardCharsets.UTF_8);
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (c.get() % 10 == 0) {
          Log.info(c.get() * BLOCK_SIZE + " Lines processed.");
        }
      });


    }
  }

}
