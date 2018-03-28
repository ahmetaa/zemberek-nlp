package zemberek.morphology._analyzer;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class _SingleAnalysisTest extends AnalyzerTestBase {

  @Test
  public void morphemeGroupTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    List<_SingleAnalysis> analyses = analyzer.analyze("kitaplarda");
    Assert.assertEquals(1, analyses.size());
    _SingleAnalysis analysis = analyses.get(0);

    Assert.assertEquals(analysis.getItem(), analyzer.getLexicon().getItemById("kitap_Noun"));
    Assert.assertEquals("larda", analysis.getEnding());
  }

}
