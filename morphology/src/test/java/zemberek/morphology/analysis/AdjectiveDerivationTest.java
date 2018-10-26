package zemberek.morphology.analysis;

import org.junit.Test;

public class AdjectiveDerivationTest extends AnalyzerTestBase {

  @Test
  public void become() {
    AnalysisTester t = getTester("beyaz [P:Adj]");

    t.expectSingle("beyazlaş", matchesTailLex("Adj + Become + Verb + Imp + A2sg"));
    t.expectAny("beyazlaştık", matchesTailLex("Adj + Become + Verb + Past + A1pl"));
    t.expectAny("beyazlaşacak", matchesTailLex("Adj + Become + Verb + Fut + A3sg"));

    t.expectFail(
        "beyazımlaş",
        "beyazlarlaştı",
        "beyazyalaştı"
    );
  }

  @Test
  public void ly() {
    AnalysisTester t = getTester("beyaz [P:Adj]");
    t.expectAny("beyazca", matchesTailLex("Adj + Ly + Adv"));
  }

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

  @Test
  public void expectsSingleResult() {
    RuleBasedAnalyzer analyzer = getAnalyzer("mavi [P:Adj]");
    expectSuccess(analyzer, 1, "maviyim");
    expectSuccess(analyzer, 1, "maviydim");
    expectSuccess(analyzer, 1, "maviyimdir");
    expectSuccess(analyzer, 1, "maviydi");
    expectSuccess(analyzer, 1, "mavidir");
    expectSuccess(analyzer, 1, "maviliyimdir");
  }


  @Test
  public void agtTest() {
    AnalysisTester tester = getTester("ucuz [P:Adj]");

    tester.expectAny("ucuzcu",
        matchesTailLex("Adj + Agt + Noun + A3sg"));
    tester.expectAny("ucuzcuyu",
        matchesTailLex("Adj + Agt + Noun + A3sg + Acc"));
    tester.expectAny("ucuzcuya",
        matchesTailLex("Adj + Agt + Noun + A3sg + Dat"));
    tester.expectAny("ucuzculuk",
        matchesTailLex("Adj + Agt + Noun + A3sg + Ness + Noun + A3sg"));
    tester.expectAny("ucuzculuğu",
        matchesTailLex("Adj + Agt + Noun + A3sg + Ness + Noun + A3sg + Acc"));


    tester.expectFail(
        "ucuzcucu",
        "ucuzlucu",
        "ucuzumsucu"
    );
  }

}
