package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class InterpretingAnalyzerFunctionalTest {

  @Test
  public void shouldParse_1() {

    RootLexicon loader = new TurkishDictionaryLoader().load("elma");
    InterpretingAnalyzer analyzer = new InterpretingAnalyzer(loader);
    List<AnalysisResult> results = analyzer.analyze("elmalar");
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);
    System.out.println("Parse result = "  + first);
    Assert.assertEquals("elma_Noun", first.getDictionaryItem().id);
    Assert.assertEquals("elma", first.root);
  }

}
