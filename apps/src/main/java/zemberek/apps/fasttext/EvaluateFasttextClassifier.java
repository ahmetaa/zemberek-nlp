package zemberek.apps.fasttext;

import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import zemberek.apps.ConsoleApp;
import zemberek.core.embeddings.FastText;
import zemberek.core.embeddings.FastText.EvaluationResult;

public class EvaluateFasttextClassifier extends ConsoleApp {

  @Parameter(names = {"--input", "-i"},
      required = true,
      description = "Input test set with correct labels.")
  Path input;

  @Parameter(names = {"--model", "-m"},
      required = true,
      description = "Model file.")
  Path model;

  @Override
  public String description() {
    return "Evaluates classifier with a test set.";
  }

  @Override
  public void run() throws Exception {
    FastText fastText = FastText.load(model);
    EvaluationResult result = fastText.test(input, 1);
    System.out.println(result.toString());
  }
}
