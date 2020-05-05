package zemberek.apps.fasttext;

import com.beust.jcommander.Parameter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.classification.FastTextClassifier;
import zemberek.classification.FastTextClassifierTrainer;
import zemberek.classification.FastTextClassifierTrainer.LossType;
import zemberek.core.embeddings.FastText;
import zemberek.core.logging.Log;

public class TrainClassifier extends FastTextAppBase {

  @Parameter(names = {"--input", "-i"},
      required = true,
      description = "Classifier training file. each line should contain a single document and "
          + "one or more class labels. "
          + "Document class label needs to have __label__ prefix attached to it.")
  Path input;

  @Parameter(names = {"--output", "-o"},
      required = true,
      description = "Output model file.")
  Path output;

  @Parameter(names = {"--lossType", "-l"},
      description = "Model type.")
  LossType lossType = LossType.SOFTMAX;

  @Parameter(names = {"--applyQuantization", "-q"},
      description = "If used, applies quantization to model. This way model files will be "
          + " smaller. Underlying algorithm uses 8 bit values for weights instead of 32 bit floats."
          + " Quantized model will be saved same place of output with name [output].q ")
  boolean applyQuantization = false;

  @Parameter(names = {"--cutOff", "-c"},
      description = "Reduces dictionary size with given threshold value. "
          + "Dictionary entries are sorted with l2-norm values and top `cutOff` are selected. "
          + "This greatly reduces model size. This option is only available if"
          + "applyQuantization flag is used.")
  int cutOff = -1;

  @Parameter(names = {"--epochCount", "-ec"},
      description = "Epoch Count.")
  int epochCount = FastTextClassifierTrainer.DEFAULT_EPOCH;

  @Parameter(names = {"--learningRate", "-lr"},
      description = "Learning rate. Should be between 0.01-2.0")
  float learningRate = FastTextClassifierTrainer.DEFAULT_LR;

  @Override
  public String description() {
    return "Generates a text classification model from a training set. Classification algorithm"
        + " is based on Java port of fastText library. It is usually suggested to apply "
        + "tokenization, lower-casing and other specific text operations to the training set"
        + " before training the model. "
        + "Algorithm may be more suitable for sentence and short paragraph"
        + " level texts rather than long documents.\n "
        + "In the training set, each line should contain a single document. Document class "
        + "label needs to have __label__ prefix attached to it. Such as "
        + "[__label__sports Match ended in a draw.]\n"
        + "Each line (document) may contain more than one label.\n"
        + "If there are a lot of labels, LossType can be chosen `HIERARCHICAL_SOFTMAX`. "
        + "This way training and runtime speed will be faster with a small accuracy loss.\n "
        + "For generating compact models, use -applyQuantization and -cutOff [dictionary-cut-off] "
        + "parameters.";
  }

  @Override
  public void run() throws IOException {

    Log.info("Generating classification model from %s", input);

    FastTextClassifierTrainer trainer = FastTextClassifierTrainer.builder()
        .epochCount(epochCount)
        .learningRate(learningRate)
        .lossType(lossType)
        .quantizationCutOff(cutOff)
        .minWordCount(minWordCount)
        .threadCount(threadCount)
        .wordNgramOrder(wordNGrams)
        .dimension(dimension)
        .contextWindowSize(contextWindowSize)
        .build();

    Log.info("Training Started.");
    trainer.getEventBus().register(this);

    FastTextClassifier classifier = trainer.train(input);

    Log.info("Saving classification model to %s", output);
    FastText fastText = classifier.getFastText();
    fastText.saveModel(output);

    if (applyQuantization) {
      Log.info("Applying quantization.");
      if (cutOff > 0) {
        Log.info("Quantization dictionary cut-off value = %d", cutOff);
      }
      Path parent = output.getParent();
      String name = output.toFile().getName() + ".q";
      Path quantizedModel = parent == null ? Paths.get(name) : parent.resolve(name);
      Log.info("Saving quantized classification model to %s", quantizedModel);
      FastText quantized = fastText.quantize(output, fastText.getArgs());
      quantized.saveModel(quantizedModel);
    }
  }

  public static void main(String[] args) {
    new TrainClassifier().execute(args);
  }
}
