package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;
import zemberek.morphology.analyzer.MorphemeSurfaceForm;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class InterpretingAnalyzerFunctionalTest {

  public static InterpretingAnalyzer getAnalyzer(String... dictionaryLines) {
    RootLexicon loader = new TurkishDictionaryLoader().load(dictionaryLines);
    return new InterpretingAnalyzer(loader);
  }

  public boolean containsMorpheme(AnalysisResult result, String morphemeName) {
    for (MorphemeSurfaceForm forms : result.getMorphemes()) {
      if (forms.lexicalTransition.to.morpheme.id.equalsIgnoreCase(morphemeName)) {
        return true;
      }
    }
    return false;
  }

  void printResults(List<AnalysisResult> results) {
    for (AnalysisResult result : results) {
      System.out.println("Parse result = " + result);
    }
  }

  @Test
  public void shouldParse_1() {
    List<AnalysisResult> results = getAnalyzer("elma").analyze("elmalar");
    printResults(results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);

    Assert.assertEquals("elma_Noun", first.getDictionaryItem().id);
    Assert.assertEquals("elma", first.root);
    Assert.assertTrue(containsMorpheme(first, "A3pl"));
  }

  @Test
  public void implicitDative_1() {
    List<AnalysisResult> results = getAnalyzer("içeri [A:ImplicitDative]")
        .analyze("içeri");
    printResults(results);
    Assert.assertEquals(2, results.size());
    AnalysisResult first = results.get(1);

    Assert.assertEquals("içeri_Noun", first.getDictionaryItem().id);
    Assert.assertEquals("içeri", first.root);
    Assert.assertTrue(containsMorpheme(first, "Dat"));
  }


}
