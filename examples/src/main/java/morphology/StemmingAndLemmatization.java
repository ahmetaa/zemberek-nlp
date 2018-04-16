package morphology;

import java.io.IOException;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;

public class StemmingAndLemmatization {

  TurkishMorphology morphology;

  public StemmingAndLemmatization(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    //new StemmingAndLemmatization(morphology).analyze("kitabımızsa");
    new StemmingAndLemmatization(morphology).analyze("geleceğe");
  }

  public void analyze(String word) {
    System.out.println("Word = " + word);

    System.out.println("Parses: ");
    WordAnalysis results = morphology.analyze(word);
    for (SingleAnalysis result : results) {
      System.out.println(result.formatLong());
      System.out.println("\tStems = " + result.getStems());
      System.out.println("\tLemmas = " + result.getLemmas());
    }
  }
}
