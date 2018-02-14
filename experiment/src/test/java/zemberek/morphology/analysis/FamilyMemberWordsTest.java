package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class FamilyMemberWordsTest extends AnalyzerTestBase {

  @Test
  public void onlyIncorrect1() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "annemler [A:ImplicitPlural,ImplicitP1sg,FamilyMember]");
    shouldNotPass(analyzer, "annemlerler", "annemlerim");
  }

  @Test
  public void expectsSingleResult() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "annemler [A:ImplicitPlural,ImplicitP1sg,FamilyMember]");
    shouldPass(analyzer, 1, "annemler", "annemlere", "annemleri");
  }

  @Test
  public void expectsAccusativeNotPossesive3sg() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "annemler [A:ImplicitPlural,ImplicitP1sg,FamilyMember]");
    String in = "annemleri";
    List<AnalysisResult> results = analyzer.analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);
    Assert.assertTrue(containsMorpheme(first, "Acc"));
    Assert.assertTrue(!containsMorpheme(first, "P3sg"));
  }

}
