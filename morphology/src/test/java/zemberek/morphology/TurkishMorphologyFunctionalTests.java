package zemberek.morphology;

import java.io.IOException;
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
  public void test2() throws IOException {
    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .addDefaultDictionaries()
        .disableCache()
        .build();
    morphology.analyze("airpods");
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
