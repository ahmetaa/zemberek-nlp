package zemberek.morphology.analysis;

import org.junit.Test;

public class PostpTest extends AnalyzerTestBase {

  @Test
  public void advTest() {
    AnalysisTester tester = getTester("gibi [P:Postp, PCNom]");
    tester.expectSingle("gibi", matchesTailLex("Postp"));
    tester.expectSingle("gibisi", matchesTailLex("Postp + Zero + Noun + A3sg + P3sg"));
    tester.expectAny("gibiler", matchesTailLex("Postp + Zero + Noun + A3pl"));
    tester.expectSingle("gibilere", matchesTailLex("Postp + Zero + Noun + A3pl + Dat"));
    tester.expectSingle("gibisine", matchesTailLex("Postp + Zero + Noun + A3sg + P3sg + Dat"));
    tester.expectSingle("gibisiyle", matchesTailLex("Postp + Zero + Noun + A3sg + P3sg + Ins"));
    tester.expectSingle("gibilerle", matchesTailLex("Postp + Zero + Noun + A3pl + Ins"));
  }

}
