package zemberek.morphology.analysis;

import org.junit.Test;

public class AdverbsTest extends AnalyzerTestBase {

  @Test
  public void advTest() {
    AnalysisTester tester = getTester("işte [P:Adv]");
    tester.expectSingle("işte", matchesTailLex("Adv"));
  }

  @Test
  public void advTest2() {
    AnalysisTester tester = getTester("olmak");
    tester.expectSingle("olunca",
        matchesTailLex("Verb + When + Adv"));
    tester.expectSingle("oluncaya",
        matchesTailLex("Verb + When + Adv + Zero + Noun + A3sg + Dat"));
  }

}
