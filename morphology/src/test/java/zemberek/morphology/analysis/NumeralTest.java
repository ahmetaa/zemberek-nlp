package zemberek.morphology.analysis;

import org.junit.Test;

public class NumeralTest extends AnalyzerTestBase {

  @Test
  public void ordinalTest() {
    AnalysisTester t = getTester("bir [P:Num,Ord]");
    t.expectAny("bir", matchesTailLex("Num"));
    t.expectAny("bire", matchesTailLex("Num + Zero + Noun + A3sg + Dat"));
    t.expectAny("birmiş", matchesTailLex("Num + Zero + Verb + Narr + A3sg"));
  }
}
