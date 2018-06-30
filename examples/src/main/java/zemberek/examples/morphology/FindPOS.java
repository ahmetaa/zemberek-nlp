package zemberek.examples.morphology;

import java.io.IOException;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;

public class FindPOS {

  public static void main(String[] args) {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    String sentence = "Keşke yarın hava güzel olsa.";
    Log.info("Sentence  = " + sentence);
    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);

    for (SentenceWordAnalysis a : analysis) {
      PrimaryPos primaryPos = a.getBestAnalysis().getPos();
      Log.info("%s : %s ",
          a.getWordAnalysis().getInput(),
          primaryPos);

    }
  }

}
