package zemberek.examples.morphology;

import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class AnalyzeWords {

  public static void main(String[] args) {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    String word = "kalemi";

    Log.info("Word = " + word);
    WordAnalysis results = morphology.analyze(word);
    for (SingleAnalysis result : results) {
      Log.info("Lexical and Surface : " + result.formatLong());
      Log.info("Only Lexical        : " + result.formatLexical());
      Log.info("Oflazer style       : " +
          AnalysisFormatters.OFLAZER_STYLE.format(result));
      Log.info();
    }
  }

}
