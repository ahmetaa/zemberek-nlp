package zemberek.morphology.analysis;

import org.junit.Test;

public class VerbsTest extends AnalyzerTestBase {

  @Test
  public void Imp() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("oku", matchesTailLex("Verb + Imp + A2sg"));
    tester.expectSingle("okusun", matchesTailLex("Verb + Imp + A3sg"));
    tester.expectSingle("okusunlar", matchesTailLex("Verb + Imp + A3pl"));
  }


  @Test
  public void progressive1() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okuyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    tester.expectSingle("okuyorsun", matchesTailLex("Verb + Prog1 + A2sg"));
    tester.expectSingle("okuyor", matchesTailLex("Verb + Prog1 + A3sg"));
    tester.expectSingle("okuyoruz", matchesTailLex("Verb + Prog1 + A1pl"));
    tester.expectSingle("okuyorlar", matchesTailLex("Verb + Prog1 + A3pl"));
  }

  @Test
  public void progressive1Drop() {
    AnalysisTester tester = getTester("aramak");

    tester.expectSingle("arıyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    tester.expectSingle("arıyor", matchesTailLex("Verb + Prog1 + A3sg"));

    tester = getTester("yürümek");

    tester.expectSingle("yürüyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    tester.expectSingle("yürüyor", matchesTailLex("Verb + Prog1 + A3sg"));


  }

}
