package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class NounAdjectiveDerivationTest extends AnalyzerTestBase {

  @Test
  public void noun2Noun_1() {
    InterpretingAnalyzer analyzer = getAnalyzer("meyve");
    String in = "meyveli";
    List<AnalysisResult> results = analyzer.analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    Assert.assertTrue(lastMorphemeIs(results.get(0), "Adj"));
    Assert.assertTrue(containsMorpheme(results.get(0), "with"));
  }


}
