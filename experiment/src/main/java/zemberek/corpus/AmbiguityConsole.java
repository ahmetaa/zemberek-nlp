package zemberek.corpus;

import java.io.IOException;
import java.util.Scanner;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class AmbiguityConsole {

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.builder().addDefaultDictionaries().build();
    new AmbiguityConsole().run(morphology);
  }

  public void run(TurkishMorphology morphology) {
    String input;
    System.out.println("Enter sentence:");
    Scanner sc = new Scanner(System.in);
    input = sc.nextLine();
    while (!input.equals("exit") && !input.equals("quit")) {

      if(input.trim().length()==0) {
        System.out.println("Empty line cannot be processed.");
        input = sc.nextLine();
        continue;
      }

      SentenceAnalysis analysis = morphology.analyzeAndResolveAmbiguity(input);

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
