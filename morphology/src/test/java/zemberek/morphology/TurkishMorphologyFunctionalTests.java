package zemberek.morphology;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.SecondaryPos;
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
  public void test2() {
    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .addDictionaryLines("Air")
        .disableCache()
        .build();
    Assert.assertEquals(0, morphology.analyze("Air'rrr").analysisCount());
    Assert.assertEquals(1, morphology.analyze("Air").analysisCount());
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

  @Test
  public void testRomanNumeral() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getEmptyTurkishMorphology();
    WordAnalysis result = morphology.analyze("IV");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.RomanNumeral,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  private TurkishMorphology getEmptyTurkishMorphology() {
    return TurkishMorphology
          .builder()
          .disableCache()
          .build();
  }

  @Test
  public void testEmoticon() {
    TurkishMorphology morphology = getEmptyTurkishMorphology();
    WordAnalysis result = morphology.analyze(":)");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Emoticon,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testWordsWithDash() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getEmptyTurkishMorphology();
    WordAnalysis result = morphology.analyze("Blah-Foo'ya");
    Assert.assertEquals(1, result.analysisCount());
  }


}
