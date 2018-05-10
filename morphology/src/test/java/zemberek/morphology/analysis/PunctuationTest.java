package zemberek.morphology.analysis;

import org.junit.Test;

public class PunctuationTest extends AnalyzerTestBase {

  @Test
  public void test1() {
    AnalysisTester tester = getTester("… [P:Punc]");
    tester.expectSingle("…", matchesTailLex("Punc"));
  }

}
