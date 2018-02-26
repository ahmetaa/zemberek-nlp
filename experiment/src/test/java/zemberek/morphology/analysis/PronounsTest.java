package zemberek.morphology.analysis;

import org.junit.Test;

public class PronounsTest extends AnalyzerTestBase {

  @Test
  public void benSenTest1() {
    AnalysisTester tester = getTester("ben [P:Pron,Pers ;A:Special]");
    tester.expectSingleTrue("ben", matchesLexicalTail("Pron + A1sg + Pnon + Nom"));
    tester.expectSingleTrue("bana", matchesLexicalTail("Pron + A1sg + Pnon + Dat"));
    tester.expectSingleTrue("beni", matchesLexicalTail("Pron + A1sg + Pnon + Acc"));

    tester = getTester("sen [P:Pron,Pers ;A:Special]");
    tester.expectSingleTrue("sen", matchesLexicalTail("Pron + A2sg + Pnon + Nom"));
    tester.expectSingleTrue("sana", matchesLexicalTail("Pron + A2sg + Pnon + Dat"));
    tester.expectSingleTrue("seni", matchesLexicalTail("Pron + A2sg + Pnon + Acc"));
  }

  @Test
  public void oTest1() {
    AnalysisTester tester = getTester("o [P:Pron,Pers]");
    tester.expectSingleTrue("o", matchesLexicalTail("Pron + A3sg + Pnon + Nom"));
    tester.expectSingleTrue("ona", matchesLexicalTail("Pron + A3sg + Pnon + Dat"));
    tester.expectSingleTrue("onu", matchesLexicalTail("Pron + A3sg + Pnon + Acc"));
    tester.expectSingleTrue("onlar", matchesLexicalTail("Pron + A3pl + Pnon + Nom"));
    tester.expectSingleTrue("onlara", matchesLexicalTail("Pron + A3pl + Pnon + Dat"));
  }

  @Test
  public void bizSizTest() {
    AnalysisTester tester = getTester("biz [P:Pron,Pers]");
    tester.expectSingleTrue("biz", matchesLexicalTail("Pron + A1pl + Pnon + Nom"));
    tester.expectSingleTrue("bize", matchesLexicalTail("Pron + A1pl + Pnon + Dat"));
    tester.expectSingleTrue("bizi", matchesLexicalTail("Pron + A1pl + Pnon + Acc"));

    tester = getTester("siz [P:Pron,Pers]");
    tester.expectSingleTrue("siz", matchesLexicalTail("Pron + A2pl + Pnon + Nom"));
    tester.expectSingleTrue("size", matchesLexicalTail("Pron + A2pl + Pnon + Dat"));
    tester.expectSingleTrue("sizi", matchesLexicalTail("Pron + A2pl + Pnon + Acc"));
  }

  @Test
  public void benFailTest1() {
    AnalysisTester tester = getTester("ben [P:Pron,Pers ;A:Special]");
    tester.expectFail(
        "ban",
        "bene",
        "banı"
    );
  }

  @Test
  public void buTest1() {
    AnalysisTester tester = getTester("bu [P:Pron,Demons]");
    tester.expectSingleTrue("bu", matchesLexicalTail("Pron + A3sg + Pnon + Nom"));
    tester.expectSingleTrue("buna", matchesLexicalTail("Pron + A3sg + Pnon + Dat"));
    tester.expectSingleTrue("bunu", matchesLexicalTail("Pron + A3sg + Pnon + Acc"));
    tester.expectSingleTrue("bunlar", matchesLexicalTail("Pron + A3pl + Pnon + Nom"));
    tester.expectSingleTrue("bunları", matchesLexicalTail("Pron + A3pl + Pnon + Acc"));
  }


}
