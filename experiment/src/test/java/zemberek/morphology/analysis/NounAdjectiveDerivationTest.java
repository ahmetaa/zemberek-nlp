package zemberek.morphology.analysis;

import org.junit.Test;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class NounAdjectiveDerivationTest extends AnalyzerTestBase {

  @Test
  public void withTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectTrue("meyveli",
        matchesLexicalTail("Pnon + Nom + With + Adj"));

  }

  @Test
  public void justlikeTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectTrue("meyvemsi",
        matchesLexicalTail("Pnon + Nom + JustLike + Adj"));
  }

  // check for
  // incorrect P1sg analysis for meyvemsi.
  // incorrect JustLike analysis for meyvesi.
  @Test
  public void justLikeFalseTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectFalse("meyvemsi",
        matchesLexicalTail("P1sg + Nom + JustLike + Adj"));
    tester.expectFalse("meyvesi",
        matchesLexicalTail("Pnon + Nom + JustLike + Adj"));

  }

  @Test
  public void incorrect1() {
    AnalysisTester tester = getTester("meyve");
    tester.expectFail("meyvelili");
    tester.expectFail("meyvelerli");
    tester.expectFail("meyvemli");
    tester.expectFail("meyveyeli");
    tester.expectFail("meyvelersi");
    tester.expectFail("meyveyemsi");
    tester.expectFail("meyvensi");
  }

}
