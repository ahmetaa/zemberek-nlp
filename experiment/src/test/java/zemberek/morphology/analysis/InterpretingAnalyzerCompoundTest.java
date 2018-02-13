package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class InterpretingAnalyzerCompoundTest extends InterpretingAnalyzerTestBase {

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
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    String in = "balkabağına";
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
  public void CompoundP3sg_Incorrect2() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    shouldNotPass(analyzer, "balkabak", "balkabağa", "balkabakta", "balkabaktan");
  }

  @Test
  public void CompoundP3sg_Correct() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    shouldPass(analyzer, "zeytinyağı", "zeytinyağına");
  }

  @Test
  public void CompoundP3sg_Correct2() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    shouldPass(analyzer, 1, "balkabağı", "balkabakları");
  }

}
