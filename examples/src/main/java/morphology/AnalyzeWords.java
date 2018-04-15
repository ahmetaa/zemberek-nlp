package morphology;

import java.io.IOException;
import zemberek.morphology._analyzer.AnalysisFormatters;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._TurkishMorphology;
import zemberek.morphology._analyzer._WordAnalysis;

public class AnalyzeWords {

  _TurkishMorphology morphology;

  public AnalyzeWords(_TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    _TurkishMorphology morphology = _TurkishMorphology.createWithDefaults();
    new AnalyzeWords(morphology).analyze("kalemi");
  }

  public void analyze(String word) {
    System.out.println("Word = " + word);
    _WordAnalysis results = morphology.analyze(word);
    for (_SingleAnalysis result : results) {
      System.out.println("Morphemes and Surface : " + result.formatLong());
      System.out.println("Only Morphemes        : " + result.formatLexical());
      System.out.println("Oflazer style         : " +
          AnalysisFormatters.OFLAZER_STYLE.format(result));
      System.out.println();
    }
  }

}
