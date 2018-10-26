package zemberek.apps.morphology;

import com.beust.jcommander.Parameter;
import java.io.IOException;
import java.util.Scanner;
import zemberek.apps.ConsoleApp;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.TurkishMorphology.Builder;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;

public class MorphologyConsole extends ConsoleApp {

  @Parameter(names = {"--disableUnknownAnalysis", "-no_unk"},
      description = "If used, unknown words will not be analysed. Such as out of dictionary "
          + "Proper nouns or numerals.")
  public boolean disableUnknownAnalysis;

  @Parameter(names = {"--enableInformalWordAnalysis", "-informal"},
      description = "If used, informal word analysis results will be included.")
  public boolean enableInformalWordAnalysis;

  public static void main(String[] args) {
    new MorphologyConsole().execute(args);
  }

  @Override
  public String description() {
    return "Applies morphological analysis and disambiguation to user entries.";
  }

  @Override
  public void run() throws IOException {
    Builder b = TurkishMorphology.builder();
    if (disableUnknownAnalysis) {
      b.disableUnidentifiedTokenAnalyzer();
    }
    RootLexicon lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");
    if (enableInformalWordAnalysis) {
      b.useAnalyzer(RuleBasedAnalyzer.instance(new InformalTurkishMorphotactics(lexicon)));
    }
    b.useLexicon(lexicon);
    TurkishMorphology morphology = b.build();
    String input;
    System.out.println("Enter word or sentence. Type `quit` or `Ctrl+C` to exit.:");
    Scanner sc = new Scanner(System.in);
    input = sc.nextLine();
    while (!input.equals("quit")) {

      if (input.trim().length() == 0) {
        System.out.println("Empty line cannot be processed.");
        input = sc.nextLine();
        continue;
      }

      SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(input);

      System.out.format("%nS:%s%n", input);
      for (SentenceWordAnalysis sw : analysis) {
        WordAnalysis wa = sw.getWordAnalysis();
        System.out.println(wa.getInput());

        SingleAnalysis best = sw.getBestAnalysis();
        for (SingleAnalysis singleAnalysis : wa) {
          boolean isBest = singleAnalysis.equals(best);
          if (wa.analysisCount() == 1) {
            System.out.println(singleAnalysis.formatLong());
          } else {
            System.out.format("%s%s%n", singleAnalysis.formatLong(), isBest ? "*" : "");
          }
        }
      }
      System.out.println();

      input = sc.nextLine();
    }
  }
}
