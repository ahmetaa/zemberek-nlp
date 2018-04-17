package morphology;

import java.io.IOException;
import zemberek.core.logging.Log;
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
    Log.info("Word = " + word);
    WordAnalysis results = morphology.analyze(word);
    for (SingleAnalysis result : results) {
      Log.info("Morphemes and Surface : " + result.formatLong());
      Log.info("Only Morphemes        : " + result.formatLexical());
      Log.info("Oflazer style         : " +
          AnalysisFormatters.OFLAZER_STYLE.format(result));
      Log.info();
    }
  }

}
