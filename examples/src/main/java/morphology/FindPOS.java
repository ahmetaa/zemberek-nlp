package morphology;

import java.io.IOException;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.TurkishMorphology;

public class FindPOS {

  TurkishMorphology morphology;

  public FindPOS(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    new FindPOS(morphology)
        .test("Keşke yarın hava güzel olsa.");
  }

  private void test(String s) {
    System.out.println("Sentence  = " + s);
    SentenceAnalysis analysis = morphology.analyzeAndResolveAmbiguity(s);

    for (SentenceWordAnalysis a : analysis) {
      PrimaryPos primaryPos = a.getAnalysis().getDictionaryItem().primaryPos;
      SecondaryPos secondaryPos = a.getAnalysis().getDictionaryItem().secondaryPos;
      Log.info("%s -> %s : %s ",
          a.getWordAnalysis().getInput(),
          primaryPos,
          secondaryPos == SecondaryPos.None ? "" : secondaryPos);
    }
  }
}
