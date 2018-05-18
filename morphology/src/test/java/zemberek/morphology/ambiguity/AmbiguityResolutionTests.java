package zemberek.morphology.ambiguity;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.TurkishTokenizer;

public class AmbiguityResolutionTests {

  @Test
  public void issue157ShouldNotThrowNPE() {
    String input = "Dünya Yıldız Kızlar Voleybol Şampiyonası'nda Yıldız Milli Takım, "
        + "final maçında Çin'i 3-0 yenerek şampiyon oldu.";
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    SentenceAnalysis analysis = morphology.analyzeAndResolveAmbiguity(input);
    Assert.assertEquals(TurkishTokenizer.DEFAULT.tokenize(input).size(), analysis.size());
    for (SentenceWordAnalysis sentenceWordAnalysis : analysis) {
      String token = sentenceWordAnalysis.getWordAnalysis().getInput();
      SingleAnalysis an = sentenceWordAnalysis.getAnalysis();
      System.out.println(token + " = " + an.formatLong());
    }
  }

}
