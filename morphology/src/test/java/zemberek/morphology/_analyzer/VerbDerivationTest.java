package zemberek.morphology._analyzer;

import org.junit.Test;

public class VerbDerivationTest extends AnalyzerTestBase {

  @Test
  public void causativeTest() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okut",
        matchesTailLex("Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("okuttur",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("okutturt",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Imp + A2sg"));

    tester.expectFail(
        "okutt",
        "okuturtur"
    );

    // "okutur" should not have a causative analysis
    tester.expectFalse("okutur", matchesTailLex("Verb + Caus + Verb + Imp + A2sg"));

    tester = getTester("semirmek");
    tester.expectSingle("semirt",
        matchesTailLex("Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("semirttir",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("semirttirt",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Imp + A2sg"));

    tester.expectFail(
        "semirtt",
        "semirtirtir"
    );
  }



  @Test
  public void infinitive1() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okumak",
        matchesTailLex("Verb + Inf1 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okumamak",
        matchesTailLex("Verb + Neg + Inf1 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okutmak",
        matchesTailLex("Verb + Caus + Verb + Inf1 + Noun + A3sg + Pnon + Nom"));

    tester.expectFail(
        "okumaka",
        "okumaklar",
        "okumağ",
        "okumağı"
    );
  }


}
