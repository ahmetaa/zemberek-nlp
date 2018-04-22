package zemberek.morphology.analysis;

import org.junit.Test;

public class QuestionTest extends AnalyzerTestBase {

  @Test
  public void mıTest() {
    AnalysisTester tester = getTester("mı [P:Ques]");

    tester.expectSingle("mı", matchesTailLex("Ques + Pres + A3sg"));
    tester.expectSingle("mısın", matchesTailLex("Ques + Pres + A2sg"));
    tester.expectSingle("mıydı", matchesTailLex("Ques + Past + A3sg"));
    tester.expectSingle("mıymışsın", matchesTailLex("Ques + Narr + A2sg"));

    tester.expectFail("mıymışsak");
  }

  @Test
  public void mıCopulaTest() {
    AnalysisTester tester = getTester("mı [P:Ques]");

    tester.expectSingle("mıdır", matchesTailLex("Ques + Pres + A3sg + Cop"));
    tester.expectSingle("mısındır", matchesTailLex("Ques + Pres + A2sg + Cop"));
    tester.expectSingle("mıyımdır", matchesTailLex("Ques + Pres + A1sg + Cop"));
    tester.expectSingle("mıyızdır", matchesTailLex("Ques + Pres + A1pl + Cop"));
    tester.expectSingle("mıymışımdır", matchesTailLex("Ques + Narr + A1sg + Cop"));
    tester.expectSingle("mıymışsındır", matchesTailLex("Ques + Narr + A2sg + Cop"));
    tester.expectSingle("mıymıştır", matchesTailLex("Ques + Narr + A3sg + Cop"));
    tester.expectSingle("mıdırlar", matchesTailLex("Ques + Pres + Cop + A3pl"));

    tester.expectFail("mıydıdır");
  }

}
