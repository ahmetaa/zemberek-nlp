package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class InterpretingAnalyzerCompoundTest extends InterpretingAnalyzerTestBase{

  @Test
  public void CompoundP3sg_1() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    String in = "zeytinyağı";
    List<AnalysisResult> results = analyzer.analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);
    Assert.assertTrue(containsMorpheme(first, "Nom"));
    Assert.assertTrue(containsMorpheme(first, "Pnon"));
  }

  @Test
  public void CompoundP3sg_2() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "at",
        "kuyruk",
        "atkuyruğu [A:CompoundP3sg; Roots:at-kuyruk]");
    String in = "atkuyruğuna";
    List<AnalysisResult> results = analyzer.analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);
    Assert.assertTrue(containsMorpheme(first, "Pnon"));
    Assert.assertTrue(containsMorpheme(first, "Dat"));
  }

  @Test
  public void CompoundP3sg_Incorrect() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    shouldNotPass(analyzer, "zeytinyağ", "zeytinyağıya", "zeytinyağılar", "zeytinyağlar");
  }

  @Test
  public void CompoundP3sg_Correct() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    shouldPass(analyzer, "zeytinyağı", "zeytinyağına");
  }

}
