package zemberek.morphology.analysis;

import org.junit.Test;

public class AdjectiveToAdjectiveTest extends AnalyzerTestBase {

  @Test
  public void justlikeAdjTest() {
    AnalysisTester tester = getTester("mavi [P:Adj]");
    tester.expectSingleTrue("mavimsi",
        matchesLexicalTail("Adj + JustLike + Adj"));
    tester = getTester("siyah [P:Adj]");
    tester.expectSingleTrue("siyahsı",
        matchesLexicalTail("Adj + JustLike + Adj"));
    tester.expectSingleTrue("siyahımsı",
        matchesLexicalTail("Adj + JustLike + Adj"));
  }


}
