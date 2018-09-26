package zemberek.morphology.analysis;

import org.junit.Test;

public class PostpTest extends AnalyzerTestBase {

  @Test
  public void gibiTest() {
    AnalysisTester tester = getTester("gibi [P:Postp, PCNom]");
    tester.expectSingle("gibi", matchesTailLex("Postp"));
    tester.expectSingle("gibisi", matchesTailLex("Postp + Zero + Noun + A3sg + P3sg"));
    tester.expectAny("gibiler", matchesTailLex("Postp + Zero + Noun + A3pl"));
    tester.expectSingle("gibilere", matchesTailLex("Postp + Zero + Noun + A3pl + Dat"));
    tester.expectSingle("gibisine", matchesTailLex("Postp + Zero + Noun + A3sg + P3sg + Dat"));
    tester.expectSingle("gibisiyle", matchesTailLex("Postp + Zero + Noun + A3sg + P3sg + Ins"));
    tester.expectSingle("gibilerle", matchesTailLex("Postp + Zero + Noun + A3pl + Ins"));
  }

  /**
   * Test for issue
   * <a href="https://github.com/ahmetaa/zemberek-nlp/issues/178">178</a>
   */
  @Test
  public void gibimeTest_issue_178() {
    AnalysisTester tester = getTester("gibi [P:Postp, PCNom]");
    tester.expectSingle("gibime", matchesTailLex("Postp + Zero + Noun + A3sg + P1sg + Dat"));
    tester.expectSingle("gibine", matchesTailLex("Postp + Zero + Noun + A3sg + P2sg + Dat"));
    tester.expectSingle("gibinize", matchesTailLex("Postp + Zero + Noun + A3sg + P2pl + Dat"));
    tester.expectSingle("gibimize", matchesTailLex("Postp + Zero + Noun + A3sg + P1pl + Dat"));
  }

}
