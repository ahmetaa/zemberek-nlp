package zemberek.morphology.analysis;

import org.junit.Test;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class AdjectivesTest extends AnalyzerTestBase {

  @Test
  public void expectsSingleResult() {
    InterpretingAnalyzer analyzer = getAnalyzer("mavi [P:Adj]");

    shouldPass(analyzer, 1, "mavi");
  }

  @Test
  public void incorrect1() {
    InterpretingAnalyzer analyzer = getAnalyzer("meyve");
    shouldNotPass(analyzer, "meyvelili");
  }

}
