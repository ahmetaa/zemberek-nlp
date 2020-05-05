package zemberek.apps.fasttext;

import com.beust.jcommander.Parameter;
import java.io.IOException;
import java.nio.file.Path;
import zemberek.core.embeddings.FastText;
import zemberek.core.embeddings.WordVectorsTrainer;
import zemberek.core.embeddings.WordVectorsTrainer.ModelType;
import zemberek.core.logging.Log;

public class GenerateWordVectors extends FastTextAppBase {

  @Parameter(names = {"--input", "-i"},
      required = true,
      description = "Input corpus text file. Assumed to be one sentence per line, "
          + "tokenized and in UTF-8 encoding.")
  Path input;

  @Parameter(names = {"--output", "-o"},
      required = true,
      description = "Output model file.")
  Path output;

  @Parameter(names = {"--type", "-t"},
      description = "Model type.")
  WordVectorsTrainer.ModelType modelType = ModelType.SKIP_GRAM;

  @Parameter(names = {"--learningRate", "-lr"},
      description = "Learning rate. Should be between 0.01-1.0")
  float learningRate = WordVectorsTrainer.DEFAULT_LR;

  @Parameter(names = {"--epochCount", "-ec"},
      description = "Epoch Count.")
  int epochCount = WordVectorsTrainer.DEFAULT_EPOCH;

  @Override
  public String description() {
    return "Generates word vectors using a text corpus. Uses java port of fastText project.";
  }

  @Override
  public void run() throws IOException {

    Log.info("Generating word vectors from %s", input);

    WordVectorsTrainer trainer = WordVectorsTrainer.builder()
        .epochCount(epochCount)
        .learningRate(learningRate)
        .modelType(modelType)
        .minWordCount(minWordCount)
        .threadCount(threadCount)
        .wordNgramOrder(wordNGrams)
        .dimension(dimension)
        .contextWindowSize(contextWindowSize)
        .build();

    Log.info("Training Started.");
    trainer.getEventBus().register(this);

    FastText fastText = trainer.train(input);

    Log.info("Saving vectors in text format to %s", output);
    fastText.saveVectors(output);
  }



  public static void main(String[] args) {
    new GenerateWordVectors().execute(args);
  }

}
