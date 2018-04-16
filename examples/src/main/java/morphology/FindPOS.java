package morphology;

import java.io.IOException;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._SentenceWordAnalysis;
import zemberek.morphology._analyzer._TurkishMorphology;

public class FindPOS {

  _TurkishMorphology morphology;

  public FindPOS(_TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    _TurkishMorphology morphology = _TurkishMorphology.createWithDefaults();
    new FindPOS(morphology)
        .test("Keşke yarın hava güzel olsa.");
  }

  private void test(String s) {
    System.out.println("Sentence  = " + s);
    _SentenceAnalysis analysis = morphology.analyzeAndResolveAmbiguity(s);

    for (_SentenceWordAnalysis a : analysis) {
      PrimaryPos primaryPos = a.getAnalysis().getDictionaryItem().primaryPos;
      SecondaryPos secondaryPos = a.getAnalysis().getDictionaryItem().secondaryPos;
      Log.info("%s -> %s : %s ",
          a.getWordAnalysis().getInput(),
          primaryPos,
          secondaryPos == SecondaryPos.None ? "" : secondaryPos);
    }
  }
}
