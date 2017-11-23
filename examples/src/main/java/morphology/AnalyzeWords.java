package morphology;

import java.io.IOException;
import java.util.List;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;

public class AnalyzeWords {

  TurkishMorphology morphology;

  public AnalyzeWords(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    new AnalyzeWords(morphology).analyze("kalemi");
  }

  public void analyze(String word) {
    System.out.println("Word = " + word);
    List<WordAnalysis> results = morphology.analyze(word);
    for (WordAnalysis result : results) {
      System.out.println(result.formatLong());
      System.out.println(result.formatNoEmpty());
      System.out.println(result.formatOflazer());
      System.out.println(result.formatOnlyIgs());
    }
  }

}
