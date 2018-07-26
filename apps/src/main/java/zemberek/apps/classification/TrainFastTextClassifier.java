package zemberek.apps.classification;

import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import zemberek.apps.embeddings.FastTextAppBase;
import zemberek.core.embeddings.FasttextClassifierTrainer.LossType;
import zemberek.core.embeddings.WordVectorsTrainer;

public class TrainFastTextClassifier extends FastTextAppBase {

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
          + " smaller.")
  boolean applyQuantization = false;

  @Parameter(names = {"--epochCount", "-ec"},
      description = "Epoch Count.")
  int epochCount = WordVectorsTrainer.DEFAULT_EPOCH;

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
        + "This way training and runtime speed will be faster with a small accuracy loss. ";

  }

  @Override
  public void run() throws Exception {

  }

  public static void main(String[] args) {
    new TrainFastTextClassifier().execute(args);
  }
}
