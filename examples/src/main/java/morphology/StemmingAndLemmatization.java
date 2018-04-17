package morphology;

import java.io.IOException;
import zemberek.core.logging.Log;
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
    Log.info("Word = " + word);

    Log.info("Parses: ");
    WordAnalysis results = morphology.analyze(word);
    for (SingleAnalysis result : results) {
      Log.info(result.formatLong());
      Log.info("\tStems = " + result.getStems());
      Log.info("\tLemmas = " + result.getLemmas());
    }
  }
}
