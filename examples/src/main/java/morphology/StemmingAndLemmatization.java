package morphology;

import java.io.IOException;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._TurkishMorphology;
import zemberek.morphology._analyzer._WordAnalysis;

public class StemmingAndLemmatization {

  _TurkishMorphology morphology;

  public StemmingAndLemmatization(_TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    _TurkishMorphology morphology = _TurkishMorphology.createWithDefaults();
    new StemmingAndLemmatization(morphology).analyze("kitabımızsa");
  }

  public void analyze(String word) {
    System.out.println("Word = " + word);

    System.out.println("Parses: ");
    _WordAnalysis results = morphology.analyze(word);
    for (_SingleAnalysis result : results) {
      System.out.println(result.formatLong());
      System.out.println("\tStems = " + result.getStems());
      System.out.println("\tLemmas = " + result.getLemmas());
    }
  }
}
