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


  @Test
  public void shouldParse_1() {
    List<AnalysisResult> results = getAnalyzer("elma").analyze("elmalar");
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);
    System.out.println("Parse result = " + first);
    Assert.assertEquals("elma_Noun", first.getDictionaryItem().id);
    Assert.assertTrue(containsMorpheme(first, "A3pl"));
  }

  @Test
  public void implicitDative_1() {
    List<AnalysisResult> results = getAnalyzer("içeri [A:ImplicitDative]")
        .analyze("içeri");
    Assert.assertEquals(2, results.size());
    AnalysisResult first = results.get(1);
    System.out.println("Parse result = " + first);
    Assert.assertEquals("içeri_Noun", first.getDictionaryItem().id);
    Assert.assertEquals("içeri", first.root);
    Assert.assertTrue(containsMorpheme(first, "Dat"));
  }


}
