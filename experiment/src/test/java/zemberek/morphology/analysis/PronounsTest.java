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
  public void falanFalancaTest1() {
    AnalysisTester tester = getTester("falan [P:Pron,Pers]");
    tester.expectSingle("falan", matchesTailLex("Pron + A3sg + Pnon + Nom"));
    tester.expectSingle("falana", matchesTailLex("Pron + A3sg + Pnon + Dat"));
    tester.expectSingle("falanı", matchesTailLex("Pron + A3sg + Pnon + Acc"));
    tester.expectSingle("falanlar", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectSingle("falanlara", matchesTailLex("Pron + A3pl + Pnon + Dat"));

    tester = getTester("falanca [P:Pron,Pers]");
    tester.expectSingle("falanca", matchesTailLex("Pron + A3sg + Pnon + Nom"));
    tester.expectSingle("falancaya", matchesTailLex("Pron + A3sg + Pnon + Dat"));
    tester.expectSingle("falancayı", matchesTailLex("Pron + A3sg + Pnon + Acc"));
    tester.expectSingle("falancalar", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectSingle("falancalara", matchesTailLex("Pron + A3pl + Pnon + Dat"));
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


    tester.expectSingle("bunlaraymış",
        matchesTailLex("Pron + A3pl + Pnon + Dat + Zero + Verb + Narr + A3sg"));
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
  }

  @Test
  public void umumTest() {
    AnalysisTester tester = getTester("umum [P:Pron,Quant]");

    tester.expectSingle("umum", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectSingle("umuma", matchesTailLex("Pron + A3pl + Pnon + Dat"));
    tester.expectSingle("umumu", matchesTailLex("Pron + A3pl + Pnon + Acc"));

    tester.expectFail(
        "umumlar"
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
        "tümüler",
        "tümleri",
        "tümüleri",
        "tümüm"
    );
  }

  @Test
  public void topuTest() {
    AnalysisTester tester = getTester("topu [P:Pron,Quant]");

    tester.expectSingle("topu", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("topumuz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("topumuza", matchesTailLex("Pron + A1pl + P1pl + Dat"));
    tester.expectSingle("topunuz", matchesTailLex("Pron + A2pl + P2pl + Nom"));
    tester.expectSingle("topunuzu", matchesTailLex("Pron + A2pl + P2pl + Acc"));

    tester.expectFail(
        "topular",
        "topuları",
        "topum" // no Pron analysis.
    );
  }

  @Test
  public void birkaciTest() {
    AnalysisTester tester = getTester("birkaçı [P:Pron,Quant]");

    tester.expectSingle("birkaçı", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("birkaçımız", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("birkaçımıza", matchesTailLex("Pron + A1pl + P1pl + Dat"));
    tester.expectSingle("birkaçınız", matchesTailLex("Pron + A2pl + P2pl + Nom"));
    tester.expectSingle("birkaçınızı", matchesTailLex("Pron + A2pl + P2pl + Acc"));

    tester.expectFail(
        "birkaçılar",
        "birkaçlar",
        "birkaçım"
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
  public void cumlesiTest() {
    AnalysisTester tester = getTester("cümlesi [P:Pron,Quant]");
    tester.expectSingle("cümlesi", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("cümlesine", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("cümlesini", matchesTailLex("Pron + A3pl + P3pl + Acc"));
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

  @Test
  public void oburuTest() {
    AnalysisTester tester = getTester("öbürü [P:Pron,Quant; A:Special]");
    tester.expectSingle("öbürü", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("öbürüne", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("öbürünü", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("öbürleri", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("öbürlerini", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    tester.expectFail(
        "öbürüler",
        "öbürümüz",
        "öbürünüz",
        "öbürün",
        "öbürüm",
        "öbürüleri",
        "öbürülerine",
        "öbürülerim"
    );
  }

  @Test
  public void oburkuTest() {
    AnalysisTester tester = getTester("öbürkü [P:Pron,Quant]");
    tester.expectSingle("öbürkü", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("öbürküne", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("öbürkünü", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("öbürküler", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectSingle("öbürkülerini", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    // Multiple solutions for öbürküleri
    tester.expectAny("öbürküleri", matchesTailLex("Pron + A3pl + Pnon + Acc"));
    tester.expectAny("öbürküleri", matchesTailLex("Pron + A3pl + P3pl + Nom"));

    tester.expectFail(
        "öbürkümüz",
        "öbürkünüz",
        "öbürkün",
        "öbürküm"
    );
  }

  @Test
  public void berikiTest() {
    AnalysisTester tester = getTester("beriki [P:Pron,Quant]");
    tester.expectSingle("beriki", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("berikine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("berikini", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("berikiler", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectSingle("berikilerini", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    // Multiple solutions for berikileri
    tester.expectAny("berikileri", matchesTailLex("Pron + A3pl + Pnon + Acc"));
    tester.expectAny("berikileri", matchesTailLex("Pron + A3pl + P3pl + Nom"));

    tester.expectFail(
        "berikimiz",
        "berikiniz",
        "berikin",
        "berikim"
    );
  }

  @Test
  public void kimseQuantTest() {
    AnalysisTester tester = getTester("kimse [P:Pron,Quant]");

    tester.expectSingle("kimse", matchesTailLex("Pron + A3sg + Pnon + Nom"));
    tester.expectSingle("kimsem", matchesTailLex("Pron + A3sg + P1sg + Nom"));
    tester.expectSingle("kimseye", matchesTailLex("Pron + A3sg + Pnon + Dat"));

    // two analysis.
    tester.expectAny("kimseler", matchesTailLex("Pron + A3pl + Pnon + Nom"));
  }

  @Test
  public void kimTest() {
    AnalysisTester tester = getTester("kim [P:Pron,Ques]");

    tester.expectSingle("kim", matchesTailLex("Pron + A3sg + Pnon + Nom"));
    tester.expectSingle("kime", matchesTailLex("Pron + A3sg + Pnon + Dat"));
    tester.expectSingle("kimlere", matchesTailLex("Pron + A3pl + Pnon + Dat"));
    tester.expectSingle("kimimiz", matchesTailLex("Pron + A3sg + P1pl + Nom"));
    tester.expectSingle("kimimize", matchesTailLex("Pron + A3sg + P1pl + Dat"));

    tester.expectAny("kimim", matchesTailLex("Pron + A3sg + P1sg + Nom"));
    tester.expectAny("kimler", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectAny("kimi", matchesTailLex("Pron + A3sg + Pnon + Acc"));
  }

  @Test
  public void neTest() {
    AnalysisTester tester = getTester("ne [P:Pron,Ques]");

    tester.expectSingle("neyimiz", matchesTailLex("Pron + A3sg + P1pl + Nom"));
    tester.expectSingle("ne", matchesTailLex("Pron + A3sg + Pnon + Nom"));
    tester.expectSingle("neye", matchesTailLex("Pron + A3sg + Pnon + Dat"));
    tester.expectSingle("nelere", matchesTailLex("Pron + A3pl + Pnon + Dat"));
    tester.expectSingle("neyimize", matchesTailLex("Pron + A3sg + P1pl + Dat"));

    tester.expectAny("neler", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectAny("neyim", matchesTailLex("Pron + A3sg + P1sg + Nom"));
    tester.expectAny("neyi", matchesTailLex("Pron + A3sg + Pnon + Acc"));
  }

  @Test
  public void nereTest() {
    AnalysisTester tester = getTester("nere [P:Pron,Ques]");

    tester.expectSingle("nere", matchesTailLex("Pron + A3sg + Pnon + Nom"));
    tester.expectSingle("nereye", matchesTailLex("Pron + A3sg + Pnon + Dat"));
    tester.expectSingle("nerelere", matchesTailLex("Pron + A3pl + Pnon + Dat"));
    tester.expectSingle("nerem", matchesTailLex("Pron + A3sg + P1sg + Nom"));
    tester.expectSingle("neremiz", matchesTailLex("Pron + A3sg + P1pl + Nom"));
    tester.expectSingle("neremize", matchesTailLex("Pron + A3sg + P1pl + Dat"));

    // TODO: consider below. For now it does not pass. Oflazer accepts.
    //tester.expectSingle("nereyim", matchesTailLex("Pron + A3sg + Pnon + Nom + Zero + Verb + Pres + A1sg"));

    tester.expectAny("nereler", matchesTailLex("Pron + A3pl + Pnon + Nom"));
    tester.expectAny("nereyi", matchesTailLex("Pron + A3sg + Pnon + Acc"));
  }

  @Test
  public void kendiTest() {
    AnalysisTester tester = getTester("kendi [P:Pron,Reflex]");

    tester.expectSingle("kendi", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("kendileri", matchesTailLex("Pron + A3pl + P3pl + Nom"));
    tester.expectSingle("kendilerine", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("kendim", matchesTailLex("Pron + A1sg + P1sg + Nom"));
    tester.expectSingle("kendin", matchesTailLex("Pron + A2sg + P2sg + Nom"));
    tester.expectSingle("kendisi", matchesTailLex("Pron + A3sg + P3sg + Nom"));
    tester.expectSingle("kendimiz", matchesTailLex("Pron + A1pl + P1pl + Nom"));
    tester.expectSingle("kendiniz", matchesTailLex("Pron + A2pl + P2pl + Nom"));

    // kendine has 2 analyses
    tester.expectAny("kendine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectAny("kendine", matchesTailLex("Pron + A2sg + P2sg + Dat"));
  }

}
