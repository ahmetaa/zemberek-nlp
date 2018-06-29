package zemberek.examples.morphology;

import java.io.IOException;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.TurkishMorphology;

public class FindPOS {

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    String sentence = "Keşke yarın hava güzel olsa.";
    Log.info("Sentence  = " + sentence);
    SentenceAnalysis analysis = morphology.analyzeAndResolveAmbiguity(sentence);

    for (SentenceWordAnalysis a : analysis) {
      PrimaryPos primaryPos = a.getBestAnalysis().getDictionaryItem().primaryPos;
      SecondaryPos secondaryPos = a.getBestAnalysis().getDictionaryItem().secondaryPos;
      Log.info("%s -> %s : %s ",
          a.getWordAnalysis().getInput(),
          primaryPos,
          secondaryPos == SecondaryPos.None ? "" : secondaryPos);
    }
  }

}
