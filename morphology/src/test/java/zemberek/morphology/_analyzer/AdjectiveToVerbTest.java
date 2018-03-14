package zemberek.morphology._analyzer;

import org.junit.Test;

public class AdjectiveToVerbTest extends AnalyzerTestBase {

  @Test
  public void become() {
    AnalysisTester t = getTester("beyaz [P:Adj]");

    t.expectSingle("beyazlaş", matchesTailLex("Adj + Become + Verb + Imp + A2sg"));
    t.expectAny("beyazlaştık", matchesTailLex("Adj + Become + Verb + Past + A1pl"));
    t.expectSingle("beyazlaşacak", matchesTailLex("Adj + Become + Verb + Fut + A3sg"));

    t.expectFail(
        "beyazımlaş",
        "beyazlarlaştı",
        "beyazyalaştı"
    );
  }

}
