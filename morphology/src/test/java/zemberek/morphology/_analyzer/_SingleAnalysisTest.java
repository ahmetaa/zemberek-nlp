package zemberek.morphology._analyzer;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology._analyzer._SingleAnalysis.MorphemeGroup;

public class _SingleAnalysisTest extends AnalyzerTestBase {

  @Test
  public void stemEndingTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    List<_SingleAnalysis> analyses = analyzer.analyze("kitaplarda");
    Assert.assertEquals(1, analyses.size());
    _SingleAnalysis analysis = analyses.get(0);

    Assert.assertEquals(analysis.getItem(), analyzer.getLexicon().getItemById("kitap_Noun"));
    Assert.assertEquals("larda", analysis.getEnding());
    Assert.assertEquals("kitap", analysis.getStem());
  }


  @Test
  public void morphemeGroupTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    _SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    MorphemeGroup group = analysis.getGroup(0);
    Assert.assertEquals("kitaplarda", group.surface());

    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okutmuyor").get(0);

    Assert.assertEquals(2, analysis.getMorphemeGroupCount());
    MorphemeGroup group0 = analysis.getGroup(0);
    Assert.assertEquals("oku", group0.surface());
    MorphemeGroup group1 = analysis.getGroup(1);
    Assert.assertEquals("tmuyor", group1.surface());
  }


}
