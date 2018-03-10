package zemberek.morphology._analyzer;

import org.junit.Test;

public class AdverbsTest extends AnalyzerTestBase {

  @Test
  public void advTest() {
    AnalysisTester tester = getTester("işte [P:Adv]");
    tester.expectSingle("işte", matchesTailLex("Adv"));
  }

}
