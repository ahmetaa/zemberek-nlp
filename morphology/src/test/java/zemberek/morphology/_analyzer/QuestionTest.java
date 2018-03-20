package zemberek.morphology._analyzer;

import org.junit.Test;

public class QuestionTest extends AnalyzerTestBase {

  @Test
  public void mıTest() {
    AnalysisTester tester = getTester("mı [P:Ques]");

    tester.expectSingle("mı", matchesTailLex("Ques + Pres + A3sg"));
    tester.expectSingle("mısın", matchesTailLex("Ques + Pres + A2sg"));
    tester.expectSingle("mıydı", matchesTailLex("Ques + Past + A3sg"));
    tester.expectSingle("mıymışsın", matchesTailLex("Ques + Narr + A2sg"));

    // kendine has 2 analyses
    tester.expectFail("mıymışsak");

  }

}
