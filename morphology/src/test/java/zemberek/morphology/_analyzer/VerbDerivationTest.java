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
    tester.expectSingle("okutmaktan",
        matchesTailLex("Verb + Caus + Verb + Inf1 + Noun + A3sg + Pnon + Abl"));
    

    tester.expectFail(
        "okumaka",
        "okumaklar",
        "okumağ",
        "okumağı"
    );
  }

  @Test
  public void infinitive2() {
    AnalysisTester tester = getTester("okumak");

    tester.expectAny("okuma",
        matchesTailLex("Verb + Inf2 + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okumama",
        matchesTailLex("Verb + Neg + Inf2 + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okumama",
        matchesTailLex("Verb + Inf2 + Noun + A3sg + P1sg + Dat"));
    tester.expectAny("okutma",
        matchesTailLex("Verb + Caus + Verb + Inf2 + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okutma",
        matchesTailLex("Verb + Caus + Verb + Neg + Imp + A2sg"));
    tester.expectAny("okutmama",
        matchesTailLex("Verb + Caus + Verb + Inf2 + Noun + A3sg + P1sg + Dat"));
    tester.expectAny("okutmamadan",
        matchesTailLex("Verb + Caus + Verb + Neg + Inf2 + Noun + A3sg + Pnon + Abl"));
  }


  @Test
  public void infinitive3() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okuyuş",
        matchesTailLex("Verb + Inf3 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okumayış",
        matchesTailLex("Verb + Neg + Inf3 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okutmayış",
        matchesTailLex("Verb + Caus + Verb + Neg + Inf3 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okutmayıştan",
        matchesTailLex("Verb + Caus + Verb + Neg + Inf3 + Noun + A3sg + Pnon + Abl"));
  }

  @Test
  public void pastPartTest() {
    AnalysisTester tester = getTester("okumak");

    tester.expectAny("okuduk",
        matchesTailLex("Verb + PastPart + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okuduk",
        matchesTailLex("Verb + PastPart + Adj + Pnon"));
    tester.expectAny("okumadık",
        matchesTailLex("Verb + Neg + PastPart + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okumadık",
        matchesTailLex("Verb + Neg + PastPart + Adj + Pnon"));
    tester.expectAny("okumadığım",
        matchesTailLex("Verb + Neg + PastPart + Noun + A3sg + P1sg + Nom"));
    tester.expectAny("okumadığım",
        matchesTailLex("Verb + Neg + PastPart + Adj + P1sg"));
    tester.expectAny("okuttuğumuz",
        matchesTailLex("Verb + Caus + Verb + PastPart + Noun + A3sg + P1pl + Nom"));
    tester.expectAny("okuttuğumuzdu",
        matchesTailLex("Verb + Caus + Verb + PastPart + Noun + A3sg + P1pl + Nom + Zero + Verb + Past + A3sg"));

    // false positive test
    tester.expectFalse("okuduğum",
        matchesTailLex("Verb + PastPart + Noun + A3sg + Pnon + Nom + Zero + Verb + Pres + A1sg"));


  }

}
