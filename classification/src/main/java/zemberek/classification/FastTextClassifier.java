package zemberek.classification;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import zemberek.core.ScoredItem;
import zemberek.core.embeddings.FastText;
import zemberek.core.embeddings.FastText.EvaluationResult;

public class FastTextClassifier {

  private FastText fastText;

  FastTextClassifier(FastText fastText) {
    this.fastText = fastText;
  }

  public FastText getFastText() {
    return fastText;
  }

  public static FastTextClassifier load(Path modelPath) throws IOException {
    Preconditions.checkArgument(modelPath.toFile().exists(),
        "%s does not exist.", modelPath);
    FastText fastText = FastText.load(modelPath);
    return new FastTextClassifier(fastText);
  }

  public EvaluationResult evaluate(Path testPath, int k) throws IOException {
    return fastText.test(testPath, k);
  }

  public EvaluationResult evaluate(Path testPath, int k, float threshold) throws IOException {
    return fastText.test(testPath, k, threshold);
  }

  public List<ScoredItem<String>> predict(String input, int k) {
    return fastText.predict(input, k);
  }

  public List<ScoredItem<String>> predict(String input, int k, float threshold) {
    return fastText.predict(input, k, threshold);
  }


  public List<String> getLabels() {
    return fastText.getLabels();
  }

}
