package zemberek.apps.fasttext;

import com.beust.jcommander.Parameter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import zemberek.apps.ConsoleApp;
import zemberek.classification.FastTextClassifier;
import zemberek.core.ScoredItem;
import zemberek.core.embeddings.FastText.EvaluationResult;

public class EvaluateClassifier extends ConsoleApp {

  @Parameter(names = {"--input", "-i"},
      required = true,
      description = "Input test set with correct labels.")
  Path input;

  @Parameter(names = {"--model", "-m"},
      required = true,
      description = "Model file.")
  Path model;

  @Parameter(names = {"--output", "-o"},
      description = "Output file where prediction results will be written."
          + " If not provided, [input].predictions will be generated.")
  Path predictions;

  @Parameter(names = {"--maxPrediction", "-k"},
      description = "Amount of top predictions. "
          + "Predictions with lower than --threshold values will not be included.")
  int maxPrediction = 1;

  @Parameter(names = {"--threshold", "-th"},
      description = "Minimum score threshold. Lower values will not be included in predictions.")
  float threshold = -100f;

  @Override
  public String description() {
    return "Evaluates classifier with a test set.";
  }

  @Override
  public void run() throws Exception {

    System.out.println("Loading classification model...");

    FastTextClassifier classifier = FastTextClassifier.load(model);
    EvaluationResult result = classifier.evaluate(input, maxPrediction, threshold);

    System.out.println("Result = " + result.toString());

    if (predictions == null) {
      String name = input.toFile().getName();
      predictions = Paths.get("").resolve(name + ".predictions");
    }

    List<String> testLines = Files.readAllLines(input, StandardCharsets.UTF_8);
    try (PrintWriter pw = new PrintWriter(predictions.toFile(), "utf-8")) {
      for (String testLine : testLines) {
        List<ScoredItem<String>> res = classifier.predict(testLine, maxPrediction);

        res = res.stream().filter(s->s.score >= threshold).collect(Collectors.toList());

        List<String> predictedCategories = new ArrayList<>();
        for (ScoredItem<String> re : res) {
          predictedCategories.add(String.format(Locale.ENGLISH, "%s (%.6f)",
              re.item.replaceAll("__label__", ""),
              Math.exp(re.score)));
        }
        pw.println(testLine);
        pw.println("Predictions   = " + String.join(", ", predictedCategories));
        pw.println();
      }
    }

    System.out.println("Predictions are written to " + predictions);
  }
}
