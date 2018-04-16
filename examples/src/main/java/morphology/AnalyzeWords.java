package morphology;

import java.io.IOException;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;

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
    WordAnalysis results = morphology.analyze(word);
    for (SingleAnalysis result : results) {
      System.out.println("Morphemes and Surface : " + result.formatLong());
      System.out.println("Only Morphemes        : " + result.formatLexical());
      System.out.println("Oflazer style         : " +
          AnalysisFormatters.OFLAZER_STYLE.format(result));
      System.out.println();
    }
  }

}
