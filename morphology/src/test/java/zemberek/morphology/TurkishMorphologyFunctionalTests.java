package zemberek.morphology;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analysis.WordAnalysis;

public class TurkishMorphologyFunctionalTests {


  @Test
  public void testWordsWithCircumflex() {
    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .addDictionaryLines("zekâ")
        .disableCache()
        .build();
    WordAnalysis result = morphology.analyze("zekâ");
    Assert.assertEquals(1, result.analysisCount());
  }

  @Test
  public void testWordsWithDot() {
    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .addDictionaryLines("Dr [P:Abbrv]")
        .disableCache()
        .build();
    WordAnalysis result = morphology.analyze("Dr.");

    Assert.assertEquals(1, result.analysisCount());

  }
}
