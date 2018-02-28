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
        "banı",
        "benler"
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

  @Test
  public void biriTest1() {
    AnalysisTester tester = getTester("biri [P:Pron,Quant]");
    // both are same
    tester.expectSingleTrue("biri", matchesLexicalTail("Pron + A3sg + P3sg + Nom"));
    tester.expectSingleTrue("birisi", matchesLexicalTail("Pron + A3sg + P3sg + Nom"));
    // both are same
    tester.expectSingleTrue("birine", matchesLexicalTail("Pron + A3sg + P3sg + Dat"));
    tester.expectSingleTrue("birisine", matchesLexicalTail("Pron + A3sg + P3sg + Dat"));
    // both are same
    tester.expectSingleTrue("birini", matchesLexicalTail("Pron + A3sg + P3sg + Acc"));
    tester.expectSingleTrue("birisini", matchesLexicalTail("Pron + A3sg + P3sg + Acc"));

    tester.expectSingleTrue("birileri", matchesLexicalTail("Pron + A3pl + P3pl + Nom"));
    tester.expectSingleTrue("birilerine", matchesLexicalTail("Pron + A3pl + P3pl + Dat"));
    tester.expectSingleTrue("birilerini", matchesLexicalTail("Pron + A3pl + P3pl + Acc"));
  }

  @Test
  public void biriFailTest1() {
    AnalysisTester tester = getTester("biri [P:Pron,Quant]");
    tester.expectFail(
        "biriler",
        "birilerim"
    );
  }

  @Test
  public void herkesTest() {
    AnalysisTester tester = getTester("herkes [P:Pron,Quant]");

    tester.expectSingleTrue("herkes", matchesLexicalTail("Pron + A3pl + Pnon + Nom"));
    tester.expectSingleTrue("herkese", matchesLexicalTail("Pron + A3pl + Pnon + Dat"));
    tester.expectSingleTrue("herkesi", matchesLexicalTail("Pron + A3pl + Pnon + Acc"));
  }

  @Test
  public void herkesFailTest1() {
    AnalysisTester tester = getTester("herkes [P:Pron,Quant]");
    tester.expectFail(
        "herkesim"
    );
  }

  @Test
  public void birbiriTest() {
    AnalysisTester tester = getTester("birbiri [P:Pron,Quant; A:Special]");

    tester.expectSingleTrue("birbiri", matchesLexicalTail("Pron + A3sg + P3sg + Nom"));
    tester.expectSingleTrue("birbirine", matchesLexicalTail("Pron + A3sg + P3sg + Dat"));
    tester.expectSingleTrue("birbirimiz", matchesLexicalTail("Pron + A1pl + P1pl + Nom"));
    tester.expectSingleTrue("birbiriniz", matchesLexicalTail("Pron + A1pl + P1pl + Nom"));

    tester.expectSingleTrue("birbirileri", matchesLexicalTail("Pron + A3pl + P3pl + Nom"));
    tester.expectSingleTrue("birbirleri", matchesLexicalTail("Pron + A3pl + P3pl + Nom"));
  }


}
