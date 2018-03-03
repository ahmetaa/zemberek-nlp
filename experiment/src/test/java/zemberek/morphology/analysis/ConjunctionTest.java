package zemberek.morphology.analysis;

import org.junit.Test;

public class ConjunctionTest extends AnalyzerTestBase {

  @Test
  public void advTest() {
    AnalysisTester tester = getTester("ve [P:Conj]");
    tester.expectSingle("ve", matchesTailLex("Conj"));
  }

}
