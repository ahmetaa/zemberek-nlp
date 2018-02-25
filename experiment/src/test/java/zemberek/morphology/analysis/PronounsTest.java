package zemberek.morphology.analysis;

import org.junit.Test;

public class PronounsTest extends AnalyzerTestBase {

  @Test
  public void benTest1() {
    AnalysisTester tester = getTester("ben [P:Pron,Pers ;A:Special]");
    tester.expectSingleTrue("ben", matchesLexicalTail("Pron + A1sg + Pnon + Nom"));
    tester.expectSingleTrue("bana", matchesLexicalTail("Pron + A1sg + Pnon + Dat"));
  }


  @Test
  public void benFailTest1() {
    AnalysisTester tester = getTester("ben [P:Pron,Pers ;A:Special]");
    tester.expectFail(
        "ban",
        "bene"
    );
  }

}
