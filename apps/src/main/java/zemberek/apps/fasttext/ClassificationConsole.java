package zemberek.apps.fasttext;

import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import zemberek.apps.ConsoleApp;
import zemberek.classification.FastTextClassifier;
import zemberek.core.ScoredItem;
import zemberek.core.logging.Log;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;

public class ClassificationConsole extends ConsoleApp {

  @Parameter(names = {"--model", "-m"},
      required = true,
      description = "Model file.")
  Path model;

  @Parameter(names = {"--predictionCount", "-k"},
      description = "Amount of top predictions.")
  int predictionCount = 3;

  @Parameter(names = {"--preprocess", "-p"},
      description = "Applies preprocessing to the input.")
  Preprocessor preprocessor = Preprocessor.TOKENIZED;

  enum Preprocessor {
    TOKENIZED, LEMMA
  }

  @Override
  public String description() {
    return "Generates a FasttextTextClassifier from the given model and makes predictions "
        + "for the input sentences provided by the user. By default application applies "
        + "tokenization and lowercasing to the input. If model is generated with Lemmatization "
        + "use [--preprocess LEMMA] parameters. ";
  }

  TurkishMorphology morphology;

  @Override
  public void run() throws Exception {

    Log.info("Loading classification model...");
    FastTextClassifier classifier = FastTextClassifier.load(model);

    if (preprocessor == Preprocessor.LEMMA) {
      morphology = TurkishMorphology.createWithDefaults();
    }

    String input;
    System.out.println("Preprocessing type = " + preprocessor.name());
    System.out.println("Enter sentence:");
    Scanner sc = new Scanner(System.in);
    input = sc.nextLine();
    while (!input.equals("exit") && !input.equals("quit")) {

      if (input.trim().length() == 0) {
        System.out.println("Empty line cannot be processed.");
        input = sc.nextLine();
        continue;
      }

      String processed;
      if (preprocessor == Preprocessor.TOKENIZED) {
        processed = String.join(" ", TurkishTokenizer.DEFAULT.tokenizeToStrings(input));
      } else {
        processed = replaceWordsWithLemma(input);
      }
      processed = removeNonWords(processed).toLowerCase(Turkish.LOCALE);
      System.out.println("Processed Input = " + processed);

      if (processed.trim().length() == 0) {
        System.out.println("Processing result is empty. Enter new sentence.");
        input = sc.nextLine();
        continue;
      }

      List<ScoredItem<String>> res = classifier.predict(processed, predictionCount);
      List<String> predictedCategories = new ArrayList<>();
      for (ScoredItem<String> re : res) {
        predictedCategories.add(String.format(Locale.ENGLISH,"%s (%.6f)",
            re.item.replaceAll("__label__", ""), re.score));
      }
      System.out.println("Predictions   = " + String.join(", ", predictedCategories));
      System.out.println();

      input = sc.nextLine();
    }
  }

  private String replaceWordsWithLemma(String sentence) {

    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);
    List<String> res = new ArrayList<>();

    for (SentenceWordAnalysis e : analysis) {
      SingleAnalysis best = e.getBestAnalysis();
      if (best.isUnknown()) {
        res.add(e.getWordAnalysis().getInput());
        continue;
      }
      List<String> lemmas = best.getLemmas();
      res.add(lemmas.get(lemmas.size() - 1));
    }
    return String.join(" ", res);
  }

  private String removeNonWords(String sentence) {
    List<Token> docTokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    List<String> reduced = new ArrayList<>(docTokens.size());
    for (Token token : docTokens) {
      if (
          token.getType() == Type.PercentNumeral ||
              token.getType() == Type.Number ||
              token.getType() == Type.Punctuation ||
              token.getType() == Type.RomanNumeral ||
              token.getType() == Type.Time ||
              token.getType() == Type.UnknownWord ||
              token.getType() == Type.Unknown) {
        if (!token.getText().contains("__")) {
          continue;
        }
      }
      String tokenStr = token.getText();
      reduced.add(tokenStr);
    }
    return String.join(" ", reduced);
  }

  public static void main(String[] args) {
    new ClassificationConsole().execute(args);
  }
}
