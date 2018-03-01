package zemberek.morphology.analysis;

import org.junit.Test;

public class AdjectiveToAdjectiveTest extends AnalyzerTestBase {

  @Test
  public void justlikeAdjTest() {
    AnalysisTester tester = getTester("mavi [P:Adj]");
    tester.expectSingle("mavimsi",
        matchesTailLex("Adj + JustLike + Adj"));
    tester = getTester("siyah [P:Adj]");
    tester.expectSingle("siyahsı",
        matchesTailLex("Adj + JustLike + Adj"));
    tester.expectSingle("siyahımsı",
        matchesTailLex("Adj + JustLike + Adj"));
  }


}
