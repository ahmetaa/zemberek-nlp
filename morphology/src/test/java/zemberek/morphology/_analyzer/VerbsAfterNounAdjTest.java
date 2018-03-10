package zemberek.morphology._analyzer;

import org.junit.Test;

public class VerbsAfterNounAdjTest extends AnalyzerTestBase {

  @Test
  public void expectsSingleResult() {
    InterpretingAnalyzer analyzer = getAnalyzer("mavi [P:Adj]");
    expectSuccess(analyzer, 1, "maviyim");
    expectSuccess(analyzer, 1, "maviydim");
    expectSuccess(analyzer, 1, "maviyimdir");
    expectSuccess(analyzer, 1, "maviydi");
    expectSuccess(analyzer, 1, "mavidir");
    expectSuccess(analyzer, 1, "maviliyimdir");
  }

  @Test
  public void expectsSingleResult2() {
    InterpretingAnalyzer analyzer = getAnalyzer("elma");
    expectSuccess(analyzer, 1, "elmayım");
    expectSuccess(analyzer, 1, "elmaydım");
    expectSuccess(analyzer, 1, "elmayımdır");
    expectSuccess(analyzer, 1, "elmaydı");
    expectSuccess(analyzer, 1, "elmadır");
    expectSuccess(analyzer, 1, "elmayadır");

    // this has two analyses.
    expectSuccess(analyzer, 2, "elmalar");
  }

  @Test
  public void incorrect1() {
    InterpretingAnalyzer analyzer = getAnalyzer("elma");
    expectFail(analyzer,
        "elmaydıdır",
        "elmayıdır",
        "elmamdırım",
        "elmamdımdır",
        "elmalarlar", // Oflazer accepts this.
        "elmamım", // Oflazer accepts this.
        "elmamımdır", // Oflazer accepts this.
        "elmaydılardır"
    );
  }

  @Test
  public void degilTest() {
    AnalysisTester tester = getTester("değil [P:Verb]");
    tester.expectSingle("değil", matchesTailLex("Neg + Pres + A3sg"));
    tester.expectSingle("değildi", matchesTailLex("Neg + Past + A3sg"));
    tester.expectSingle("değilim", matchesTailLex("Neg + Pres + A1sg"));
  }

  @Test
  public void nounVerbZeroTest() {
    AnalysisTester tester = getTester("elma");
    tester.expectSingle("elmayım", matchesTailLex("Zero + Verb + Pres + A1sg"));
    tester.expectSingle("elmanım", matchesTailLex("Zero + Verb + Pres + A1sg"));
  }

  @Test
  public void narrTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectSingle("elmaymış", matchesTailLex("Zero + Verb + Narr + A3sg"));
    tester.expectSingle("elmaymışız", matchesTailLex("Zero + Verb + Narr + A1pl"));
    tester.expectSingle("elmaymışım", matchesTailLex("Zero + Verb + Narr + A1sg"));
    tester.expectSingle("elmaymışımdır", matchesTailLex("Zero + Verb + Narr + A1sg + Cop"));
    tester.expectSingle("elmaymışsam", matchesTailLex("Zero + Verb + Narr + Cond + A1sg"));

    tester.expectFail(
        "elmaymışmış"
    );
  }

  @Test
  public void pastTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectSingle("elmaydı", matchesTailLex("Zero + Verb + Past + A3sg"));
    tester.expectSingle("elmaydık", matchesTailLex("Zero + Verb + Past + A1pl"));
    tester.expectSingle("elmaydım", matchesTailLex("Zero + Verb + Past + A1sg"));
    tester.expectSingle("elmaydılar", matchesTailLex("Zero + Verb + Past + A3pl"));

    tester.expectFail(
        "elmaydıysa",
        "elmaydıyız",
        "elmaydılardır"
    );
  }

  @Test
  public void condTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectSingle("elmaysa", matchesTailLex("Zero + Verb + Pres + Cond + A3sg"));
    tester.expectSingle("elmaysak", matchesTailLex("Zero + Verb + Pres + Cond + A1pl"));
    tester.expectSingle("elmaymışsa", matchesTailLex("Zero + Verb + Narr + Cond + A3sg"));
    tester.expectSingle("elmaymışsam", matchesTailLex("Zero + Verb + Narr + Cond + A1sg"));

    tester.expectFail(
        "elmaydıysa",
        "elmaysadır",
        "elmaysalardır"
    );
  }

}
