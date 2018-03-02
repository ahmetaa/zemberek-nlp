package zemberek.morphology.analysis;

import org.junit.Test;

public class PronounsTest extends AnalyzerTestBase {

  @Test
  public void benSenTest1() {
    AnalysisTester tester = getTester("ben [P:Pron,Pers ;A:Special]");
    tester.expectSingle("ben", matchesTailLex("Pron + A1sg + Pnon + Nom"));
    tester.expectSingle("bana", matchesTailLex("Pron + A1sg + Pnon + Dat"));
    tester.expectSingle("beni", matchesTailLex("Pron + A1sg + Pnon + Acc"));

    tester.expectFail(
        "ban",
        "bene",
        "banı",
        "benler"
    );

    tester = getTester("sen [P:Pron,Pers ;A:Special]");
    tester.expectSingle("sen", matchesTailLex("Pron + A2sg + Pnon + Nom"));
    tester.expectSingle("sana", matchesTailLex("Pron + A2sg + Pnon + Dat"));
    tester.expectSingle("seni", matchesTailLex("Pron + A2sg + Pnon + Acc"));

    tester.expectFail(
        "san",
        "sene",
        "sanı",
        "senler"
    );
  }

  @Test
  public void oTest1() {
    AnalysisTester tester = getTester("o [P:Pron,Pers]");
    tester.expectSingle("o", matchesTailLex("Pron + A3sg + Pnon + Nom"));
    tester.expectSingle("ona", matchesTailLex("Pron + A3sg + Pnon + Dat"));
    tester.expectSingle("onu", matchesTailLex("Pron + A3sg + Pnon + Acc"));
    tester.expectSingle("onlar", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectSingle("onlara", matchesTailLex("Pron + A3pl + Pnon + Dat"));
  }

  @Test
  public void bizSizTest() {
    AnalysisTester tester = getTester("biz [P:Pron,Pers]");
    tester.expectSingle("biz", matchesTailLex("Pron + A1pl + Pnon + Nom"));
    tester.expectSingle("bize", matchesTailLex("Pron + A1pl + Pnon + Dat"));
    tester.expectSingle("bizi", matchesTailLex("Pron + A1pl + Pnon + Acc"));

    tester = getTester("siz [P:Pron,Pers]");
    tester.expectSingle("siz", matchesTailLex("Pron + A2pl + Pnon + Nom"));
    tester.expectSingle("size", matchesTailLex("Pron + A2pl + Pnon + Dat"));
    tester.expectSingle("sizi", matchesTailLex("Pron + A2pl + Pnon + Acc"));
  }


  @Test
  public void buTest1() {
    AnalysisTester tester = getTester("bu [P:Pron,Demons]");
    tester.expectSingle("bu", matchesTailLex("Pron + A3sg + Pnon + Nom"));
    tester.expectSingle("buna", matchesTailLex("Pron + A3sg + Pnon + Dat"));
    tester.expectSingle("bunu", matchesTailLex("Pron + A3sg + Pnon + Acc"));
    tester.expectSingle("bunlar", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectSingle("bunları", matchesTailLex("Pron + A3pl + Pnon + Acc"));
  }

  @Test
  public void biriTest1() {
    AnalysisTester tester = getTester("biri [P:Pron,Quant]");
    // both are same
    tester.expectSingle("biri", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("birisi", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    // both are same
    tester.expectSingle("birine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("birisine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    // both are same
    tester.expectSingle("birini", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("birisini", matchesTailLex("Pron + A3sg + P3sg + Acc"));

    tester.expectSingle("birimiz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("biriniz", matchesTailLex("Pron + A2pl + P2pl + Nom"));
    tester.expectSingle("birileri", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("birilerine", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("birilerini", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    tester.expectFail(
        "biriler",
        "birilerim"
    );
  }

  @Test
  public void herbiriTest1() {
    AnalysisTester tester = getTester("herbiri [P:Pron,Quant]");
    // both are same
    tester.expectSingle("herbiri", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("herbirisi", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    // both are same
    tester.expectSingle("herbirine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("herbirisine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    // both are same

    tester.expectSingle("herbirimiz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("herbiriniz", matchesTailLex("Pron + A2pl + P2pl + Nom"));

    tester.expectSingle("herbirini", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("herbirisini", matchesTailLex("Pron + A3sg + P3sg + Acc"));

    tester.expectFail(
        "herbiriler",
        "herbirileri",
        "herbirilerine",
        "herbirilerim",
        "herbirilerin"
    );
  }

  @Test
  public void herkesTest() {
    AnalysisTester tester = getTester("herkes [P:Pron,Quant]");

    tester.expectSingle("herkes", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectSingle("herkese", matchesTailLex("Pron + A3pl + Pnon + Dat"));
    tester.expectSingle("herkesi", matchesTailLex("Pron + A3pl + Pnon + Acc"));

    tester.expectFail(
        "herkesim" // no Pron analysis. Oflzer offers noun analysis
    );
  }

  @Test
  public void birbiriTest() {
    AnalysisTester tester = getTester("birbiri [P:Pron,Quant; A:Special]");

    tester.expectSingle("birbiri", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("birbirine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("birbirimiz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("birbiriniz", matchesTailLex("Pron + A2pl + P2pl + Nom"));

    tester.expectSingle("birbirileri", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("birbirleri", matchesTailLex("Pron + A3pl + P3pl + Nom"));

    tester.expectFail(
        "birbir",
        "birbire",
        "birbirler",
        "birbiriler",
        "birbirlere"
    );
  }

  @Test
  public void hepTest() {
    AnalysisTester tester = getTester("hep [P:Pron,Quant]");

    tester.expectSingle("hepimiz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("hepimize", matchesTailLex("Pron + A1pl + P1pl + Dat"));
    tester.expectSingle("hepiniz", matchesTailLex("Pron + A2pl + P2pl + Nom"));
    tester.expectSingle("hepinizi", matchesTailLex("Pron + A2pl + P2pl + Acc"));

    tester.expectFail(
        "hep", // only [hep+Adv] is allowed.
        "hepler",
        "hepleri",
        "hepe",
        "hepim"
    );
  }

  @Test
  public void tumuTest() {
    AnalysisTester tester = getTester("tümü [P:Pron,Quant]");

    tester.expectSingle("tümü", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("tümümüz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("tümümüze", matchesTailLex("Pron + A1pl + P1pl + Dat"));
    tester.expectSingle("tümünüz", matchesTailLex("Pron + A2pl + P2pl + Nom"));
    tester.expectSingle("tümünüzü", matchesTailLex("Pron + A2pl + P2pl + Acc"));

    tester.expectFail(
        "tümler",
        "tümleri",
        "tümüm"
    );
  }

  @Test
  public void hepsiTest() {
    AnalysisTester tester = getTester("hepsi [P:Pron,Quant]");

    tester.expectSingle("hepsi", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("hepsine", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("hepsini", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    tester.expectFail(
        "hepsiler",
        "hepsim",
        "hepsin",
        "hepsisi",
        "hepsimiz",
        "hepsiniz",
        "hepsileri"
    );
  }

  @Test
  public void kimiTest() {
    AnalysisTester tester = getTester("kimi [P:Pron,Quant]");

    tester.expectSingle("kimi", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("kimimiz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("kiminiz", matchesTailLex("Pron + A2pl + P2pl + Nom"));
    tester.expectSingle("kimileri", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("kimine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("kimimize", matchesTailLex("Pron + A1pl + P1pl + Dat"));

    tester.expectFail(
        "kimiler",
        "kimim",
        "kimin"
    );
  }

  @Test
  public void coguTest() {
    AnalysisTester tester = getTester("çoğu [P:Pron,Quant; A:Special]");
    tester.expectSingle("çoğu", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("çoğumuz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("çoğunuz", matchesTailLex("Pron + A2pl + P2pl + Nom"));
    tester.expectSingle("çokları", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("çoğuna", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("çoklarını", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    tester.expectFail(
        "çoğular",
        "çokumuz",
        "çoğum"
    );
  }

  @Test
  public void bircoguTest() {
    AnalysisTester tester = getTester("birçoğu [P:Pron,Quant; A:Special]");
    tester.expectSingle("birçoğu", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("birçoğumuz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("birçoğunuz", matchesTailLex("Pron + A2pl + P2pl + Nom"));
    tester.expectSingle("birçokları", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("birçoğuna", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("birçoklarını", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    tester.expectFail(
        "birçoğular",
        "birçokumuz",
        "birçoğum"
    );
  }

  @Test
  public void hiçbiriTest() {
    AnalysisTester tester = getTester("hiçbiri [P:Pron,Quant]");
    // both are same
    tester.expectSingle("hiçbiri", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("hiçbirisi", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    // both are same
    tester.expectSingle("hiçbirine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("hiçbirisine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    // both are same
    tester.expectSingle("hiçbirini", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("hiçbirisini", matchesTailLex("Pron + A3sg + P3sg + Acc"));

    tester.expectSingle("hiçbirimiz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("hiçbiriniz", matchesTailLex("Pron + A2pl + P2pl + Nom"));

    tester.expectFail(
        "hiçbiriler",
        "hiçbirileri",
        "hiçbirilerine",
        "hiçbirilerim"
    );
  }  
  
  

}
