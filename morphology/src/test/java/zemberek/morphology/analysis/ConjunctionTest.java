package zemberek.morphology.analysis;

import org.junit.Test;

public class ConjunctionTest extends AnalyzerTestBase {

  @Test
  public void conjTest() {
    AnalysisTester tester = getTester("ve [P:Conj]");
    tester.expectSingle("ve", matchesTailLex("Conj"));
  }

}
