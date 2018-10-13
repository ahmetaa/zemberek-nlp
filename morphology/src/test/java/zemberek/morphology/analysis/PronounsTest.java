package zemberek.morphology.analysis;

import org.junit.Test;

public class PronounsTest extends AnalyzerTestBase {

  @Test
  public void benSenTest1() {
    AnalysisTester tester = getTester("ben [P:Pron,Pers]");
    tester.expectSingle("ben", matchesTailLex("Pron + A1sg"));
    tester.expectSingle("bana", matchesTailLex("Pron + A1sg + Dat"));
    tester.expectSingle("beni", matchesTailLex("Pron + A1sg + Acc"));
    tester.expectAny("benim", matchesTailLex("Pron + A1sg + Gen"));
    tester.expectAny("benken", matchesTailLex("Pron + A1sg + Zero + Verb + While + Adv"));
    tester.expectSingle("benimle", matchesTailLex("Pron + A1sg + Ins"));

    tester.expectFail(
        "ban",
        "bene",
        "banı",
        "benin",
        "beniz",
        "benler"
    );

    tester = getTester("sen [P:Pron,Pers]");
    tester.expectSingle("sen", matchesTailLex("Pron + A2sg"));
    tester.expectSingle("sana", matchesTailLex("Pron + A2sg + Dat"));
    tester.expectSingle("seni", matchesTailLex("Pron + A2sg + Acc"));
    tester.expectSingle("senin", matchesTailLex("Pron + A2sg + Gen"));
    tester.expectSingle("seninle", matchesTailLex("Pron + A2sg + Ins"));

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
    tester.expectSingle("o", matchesTailLex("Pron + A3sg"));
    tester.expectSingle("ona", matchesTailLex("Pron + A3sg + Dat"));
    tester.expectSingle("onu", matchesTailLex("Pron + A3sg + Acc"));
    tester.expectSingle("onlar", matchesTailLex("Pron + A3pl"));
    tester.expectSingle("onlara", matchesTailLex("Pron + A3pl + Dat"));
    tester.expectSingle("onunla", matchesTailLex("Pron + A3sg + Ins"));

  }

  @Test
  public void falanFalancaTest1() {
    AnalysisTester tester = getTester("falan [P:Pron,Pers]");
    tester.expectSingle("falan", matchesTailLex("Pron + A3sg"));
    tester.expectSingle("falana", matchesTailLex("Pron + A3sg + Dat"));
    tester.expectSingle("falanı", matchesTailLex("Pron + A3sg + Acc"));
    tester.expectSingle("falanlar", matchesTailLex("Pron + A3pl"));
    tester.expectSingle("falanlara", matchesTailLex("Pron + A3pl + Dat"));

    tester = getTester("falanca [P:Pron,Pers]");
    tester.expectSingle("falanca", matchesTailLex("Pron + A3sg"));
    tester.expectSingle("falancaya", matchesTailLex("Pron + A3sg + Dat"));
    tester.expectSingle("falancayı", matchesTailLex("Pron + A3sg + Acc"));
    tester.expectSingle("falancalar", matchesTailLex("Pron + A3pl"));
    tester.expectSingle("falancalara", matchesTailLex("Pron + A3pl + Dat"));
  }

  @Test
  public void bizSizTest() {
    AnalysisTester tester = getTester("biz [P:Pron,Pers]");
    tester.expectSingle("biz", matchesTailLex("Pron + A1pl"));
    tester.expectSingle("bize", matchesTailLex("Pron + A1pl + Dat"));
    tester.expectSingle("bizi", matchesTailLex("Pron + A1pl + Acc"));
    tester.expectSingle("bizim", matchesTailLex("Pron + A1pl + Gen"));
    tester.expectSingle("bizce", matchesTailLex("Pron + A1pl + Equ"));
    tester.expectSingle("bizimle", matchesTailLex("Pron + A1pl + Ins"));


    tester.expectFail(
        "bizin"
    );

    tester = getTester("siz [P:Pron,Pers]");
    tester.expectSingle("siz", matchesTailLex("Pron + A2pl"));
    tester.expectSingle("size", matchesTailLex("Pron + A2pl + Dat"));
    tester.expectSingle("sizi", matchesTailLex("Pron + A2pl + Acc"));
    tester.expectSingle("sizin", matchesTailLex("Pron + A2pl + Gen"));
    tester.expectSingle("sizce", matchesTailLex("Pron + A2pl + Equ"));
    tester.expectSingle("sizle", matchesTailLex("Pron + A2pl + Ins"));
    tester.expectSingle("sizinle", matchesTailLex("Pron + A2pl + Ins"));
  }

  @Test
  public void buTest1() {
    AnalysisTester tester = getTester("bu [P:Pron, Demons]");
    tester.expectSingle("bu", matchesTailLex("Pron + A3sg"));
    tester.expectSingle("buna", matchesTailLex("Pron + A3sg + Dat"));
    tester.expectSingle("bunu", matchesTailLex("Pron + A3sg + Acc"));
    tester.expectSingle("bunlar", matchesTailLex("Pron + A3pl"));
    tester.expectSingle("bunları", matchesTailLex("Pron + A3pl + Acc"));
    tester.expectSingle("bununla", matchesTailLex("Pron + A3sg + Ins"));


    tester.expectSingle("bunlaraymış",
        matchesTailLex("Pron + A3pl + Dat + Zero + Verb + Narr + A3sg"));
  }

  @Test
  public void biriTest1() {
    AnalysisTester tester = getTester("biri [P:Pron,Quant]");
    // both are same
    tester.expectSingle("biri", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("birisi", matchesTailLex("Pron + A3sg + P3sg"));
    // both are same
    tester.expectSingle("birine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("birisine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    // both are same
    tester.expectSingle("birini", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("birisini", matchesTailLex("Pron + A3sg + P3sg + Acc"));

    tester.expectSingle("birimiz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("biriniz", matchesTailLex("Pron + A2pl + P2pl"));
    tester.expectSingle("birileri", matchesTailLex("Pron + A3pl + P3pl"));
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
    tester.expectSingle("herbiri", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("herbirisi", matchesTailLex("Pron + A3sg + P3sg"));
    // both are same
    tester.expectSingle("herbirine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("herbirisine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    // both are same

    tester.expectSingle("herbirimiz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("herbiriniz", matchesTailLex("Pron + A2pl + P2pl"));

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

    tester.expectSingle("herkes", matchesTailLex("Pron + A3pl"));
    tester.expectSingle("herkese", matchesTailLex("Pron + A3pl + Dat"));
    tester.expectSingle("herkesi", matchesTailLex("Pron + A3pl + Acc"));
  }

  @Test
  public void umumTest() {
    AnalysisTester tester = getTester("umum [P:Pron,Quant]");

    tester.expectSingle("umum", matchesTailLex("Pron + A3pl"));
    tester.expectSingle("umuma", matchesTailLex("Pron + A3pl + Dat"));
    tester.expectSingle("umumu", matchesTailLex("Pron + A3pl + Acc"));

    tester.expectFail(
        "umumlar"
    );
  }

  @Test
  public void birbiriTest() {
    AnalysisTester tester = getTester("birbiri [P:Pron,Quant]");

    tester.expectSingle("birbiri", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("birbirine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("birbirimiz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("birbiriniz", matchesTailLex("Pron + A2pl + P2pl"));

    tester.expectSingle("birbirileri", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("birbirleri", matchesTailLex("Pron + A3pl + P3pl"));

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

    tester.expectSingle("hepimiz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("hepimize", matchesTailLex("Pron + A1pl + P1pl + Dat"));
    tester.expectSingle("hepiniz", matchesTailLex("Pron + A2pl + P2pl"));
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

    tester.expectSingle("tümü", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("tümümüz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("tümümüze", matchesTailLex("Pron + A1pl + P1pl + Dat"));
    tester.expectSingle("tümünüz", matchesTailLex("Pron + A2pl + P2pl"));
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

    tester.expectSingle("topu", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("topumuz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("topumuza", matchesTailLex("Pron + A1pl + P1pl + Dat"));
    tester.expectSingle("topunuz", matchesTailLex("Pron + A2pl + P2pl"));
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

    tester.expectSingle("birkaçı", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("birkaçımız", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("birkaçımıza", matchesTailLex("Pron + A1pl + P1pl + Dat"));
    tester.expectSingle("birkaçınız", matchesTailLex("Pron + A2pl + P2pl"));
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

    tester.expectSingle("hepsi", matchesTailLex("Pron + A3pl + P3pl"));
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
    tester.expectSingle("cümlesi", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("cümlesine", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("cümlesini", matchesTailLex("Pron + A3pl + P3pl + Acc"));
  }  
  
  @Test
  public void kimiTest() {
    AnalysisTester tester = getTester("kimi [P:Pron,Quant]");

    tester.expectSingle("kimi", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("kimimiz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("kiminiz", matchesTailLex("Pron + A2pl + P2pl"));
    tester.expectSingle("kimileri", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("kimine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("kimisi", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("kimimize", matchesTailLex("Pron + A1pl + P1pl + Dat"));

    tester.expectFail(
        "kimiler",
        "kimim",
        "kimin"
    );
  }

  @Test
  public void coguTest() {
    AnalysisTester tester = getTester("çoğu [P:Pron,Quant]");
    tester.expectSingle("çoğu", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("çoğumuz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("çoğunuz", matchesTailLex("Pron + A2pl + P2pl"));
    tester.expectSingle("çokları", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("çoğuna", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("çoklarını", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    tester.expectFail(
        "çoğular",
        "çokumuz",
        "çoğum"
    );
  }

  @Test
  public void baziTest() {
    AnalysisTester tester = getTester("bazı [P:Pron,Quant]");

    tester.expectSingle("bazımız", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("bazınız", matchesTailLex("Pron + A2pl + P2pl"));
    tester.expectSingle("bazıları", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("bazısına", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("bazımızdan", matchesTailLex("Pron + A1pl + P1pl + Abl"));
    tester.expectSingle("bazınıza", matchesTailLex("Pron + A2pl + P2pl + Dat"));
    tester.expectSingle("bazılarımızdan", matchesTailLex("Pron + A1pl + P1pl + Abl"));
    tester.expectSingle("bazılarını", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    tester.expectFail(
        "bazı",// oflazer does not solve this for Pron+Quant
        "bazına",
        "bazım",
        "bazın",
        "bazılar"
    );
  }

  @Test
  public void bircoguTest() {
    AnalysisTester tester = getTester("birçoğu [P:Pron,Quant]");
    tester.expectSingle("birçoğu", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("birçoğumuz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("birçoğunuz", matchesTailLex("Pron + A2pl + P2pl"));
    tester.expectSingle("birçokları", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("birçoğuna", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("birçoklarını", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    tester.expectFail(
        "birçoğular",
        "birçokumuz",
        "birçoğum"
    );
  }

  @Test
  public void hicbiriTest() {
    AnalysisTester tester = getTester("hiçbiri [P:Pron,Quant]");
    // both are same
    tester.expectSingle("hiçbiri", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("hiçbirisi", matchesTailLex("Pron + A3sg + P3sg"));
    // both are same
    tester.expectSingle("hiçbirine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("hiçbirisine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    // both are same
    tester.expectSingle("hiçbirini", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("hiçbirisini", matchesTailLex("Pron + A3sg + P3sg + Acc"));

    tester.expectSingle("hiçbirimiz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("hiçbiriniz", matchesTailLex("Pron + A2pl + P2pl"));

    tester.expectFail(
        "hiçbiriler",
        "hiçbirileri",
        "hiçbirilerine",
        "hiçbirilerim"
    );
  }

  @Test
  public void oburuTest() {
    AnalysisTester tester = getTester("öbürü [P:Pron,Quant]");
    tester.expectSingle("öbürü", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("öbürüne", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("öbürünü", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("öbürleri", matchesTailLex("Pron + A3pl + P3pl"));
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
    tester.expectSingle("öbürkü", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("öbürküne", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("öbürkünü", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("öbürküler", matchesTailLex("Pron + A3pl"));
    tester.expectSingle("öbürkülerini", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    // Multiple solutions for öbürküleri
    tester.expectAny("öbürküleri", matchesTailLex("Pron + A3pl + Acc"));
    tester.expectAny("öbürküleri", matchesTailLex("Pron + A3pl + P3pl"));

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
    tester.expectSingle("beriki", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("berikine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectSingle("berikini", matchesTailLex("Pron + A3sg + P3sg + Acc"));
    tester.expectSingle("berikiler", matchesTailLex("Pron + A3pl"));
    tester.expectSingle("berikilerini", matchesTailLex("Pron + A3pl + P3pl + Acc"));

    // Multiple solutions for berikileri
    tester.expectAny("berikileri", matchesTailLex("Pron + A3pl + Acc"));
    tester.expectAny("berikileri", matchesTailLex("Pron + A3pl + P3pl"));

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

    tester.expectSingle("kimse", matchesTailLex("Pron + A3sg"));
    tester.expectSingle("kimsem", matchesTailLex("Pron + A3sg + P1sg"));
    tester.expectSingle("kimseye", matchesTailLex("Pron + A3sg + Dat"));

    // two analysis.
    tester.expectAny("kimseler", matchesTailLex("Pron + A3pl"));
  }

  @Test
  public void kimTest() {
    AnalysisTester tester = getTester("kim [P:Pron,Ques]");

    tester.expectSingle("kim", matchesTailLex("Pron + A3sg"));
    tester.expectSingle("kime", matchesTailLex("Pron + A3sg + Dat"));
    tester.expectSingle("kimlere", matchesTailLex("Pron + A3pl + Dat"));
    tester.expectSingle("kimimiz", matchesTailLex("Pron + A3sg + P1pl"));
    tester.expectSingle("kiminle", matchesTailLex("Pron + A3sg + P2sg + Ins"));
    tester.expectSingle("kimimize", matchesTailLex("Pron + A3sg + P1pl + Dat"));
    tester.expectSingle("kimsiniz",
        matchesTailLex("Pron + A3sg + Zero + Verb + Pres + A2pl"));

    tester.expectAny("kimim", matchesTailLex("Pron + A3sg + P1sg"));
    tester.expectAny("kimler", matchesTailLex("Pron + A3pl"));
    tester.expectAny("kimi", matchesTailLex("Pron + A3sg + Acc"));
  }

  @Test
  public void neTest() {
    AnalysisTester tester = getTester("ne [P:Pron,Ques]");

    tester.expectSingle("neyimiz", matchesTailLex("Pron + A3sg + P1pl"));
    tester.expectSingle("ne", matchesTailLex("Pron + A3sg"));
    tester.expectSingle("neye", matchesTailLex("Pron + A3sg + Dat"));
    tester.expectSingle("nelere", matchesTailLex("Pron + A3pl + Dat"));
    tester.expectSingle("neyimize", matchesTailLex("Pron + A3sg + P1pl + Dat"));

    tester.expectAny("neler", matchesTailLex("Pron + A3pl"));
    tester.expectAny("neyim", matchesTailLex("Pron + A3sg + P1sg"));
    tester.expectAny("neyi", matchesTailLex("Pron + A3sg + Acc"));
  }

  @Test
  public void nereTest() {
    AnalysisTester tester = getTester("nere [P:Pron,Ques]");

    tester.expectSingle("nere", matchesTailLex("Pron + A3sg"));
    tester.expectSingle("nereye", matchesTailLex("Pron + A3sg + Dat"));
    tester.expectSingle("nerelere", matchesTailLex("Pron + A3pl + Dat"));
    tester.expectSingle("nerem", matchesTailLex("Pron + A3sg + P1sg"));
    tester.expectSingle("neremiz", matchesTailLex("Pron + A3sg + P1pl"));
    tester.expectSingle("neremize", matchesTailLex("Pron + A3sg + P1pl + Dat"));

    // TODO: consider below. For now it does not pass. Oflazer accepts.
    //tester.expectSingle("nereyim", matchesTailLex("Pron + A3sg + Zero + Verb + Pres + A1sg"));

    tester.expectAny("nereler", matchesTailLex("Pron + A3pl"));
    tester.expectAny("nereyi", matchesTailLex("Pron + A3sg + Acc"));
    tester.expectAny("nereli", matchesTailLex("Pron + A3sg + With + Adj"));
  }

  @Test
  public void kendiTest() {
    AnalysisTester tester = getTester("kendi [P:Pron,Reflex]");

    tester.expectSingle("kendi", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("kendileri", matchesTailLex("Pron + A3pl + P3pl"));
    tester.expectSingle("kendilerine", matchesTailLex("Pron + A3pl + P3pl + Dat"));
    tester.expectSingle("kendim", matchesTailLex("Pron + A1sg + P1sg"));
    tester.expectSingle("kendin", matchesTailLex("Pron + A2sg + P2sg"));
    tester.expectSingle("kendisi", matchesTailLex("Pron + A3sg + P3sg"));
    tester.expectSingle("kendimiz", matchesTailLex("Pron + A1pl + P1pl"));
    tester.expectSingle("kendiniz", matchesTailLex("Pron + A2pl + P2pl"));
    tester.expectSingle("kendisiyle", matchesTailLex("Pron + A3sg + P3sg + Ins"));
    tester.expectSingle("kendimizle", matchesTailLex("Pron + A1pl + P1pl + Ins"));

    // kendine has 2 analyses
    tester.expectAny("kendine", matchesTailLex("Pron + A3sg + P3sg + Dat"));
    tester.expectAny("kendine", matchesTailLex("Pron + A2sg + P2sg + Dat"));
  }

  /**
   * Test for issues
   * <a href="https://github.com/ahmetaa/zemberek-nlp/issues/171">171</a>
   * <a href="https://github.com/ahmetaa/zemberek-nlp/issues/172">172</a>
   */
  @Test
  public void kendiTest_issues_171_172() {
    AnalysisTester tester = getTester("kendi [P:Pron,Reflex]");

    tester.expectSingle("kendime", matchesTailLex("Pron + A1sg + P1sg + Dat"));
    tester.expectSingle("kendimde", matchesTailLex("Pron + A1sg + P1sg + Loc"));
    tester.expectSingle("kendimden", matchesTailLex("Pron + A1sg + P1sg + Abl"));
    tester.expectSingle("kendimce", matchesTailLex("Pron + A1sg + P1sg + Equ"));
    tester.expectSingle("kendimle", matchesTailLex("Pron + A1sg + P1sg + Ins"));

    // These also have A3sg analyses.
    tester.expectAny("kendine", matchesTailLex("Pron + A2sg + P2sg + Dat"));
    tester.expectAny("kendinde", matchesTailLex("Pron + A2sg + P2sg + Loc"));
    tester.expectAny("kendinden", matchesTailLex("Pron + A2sg + P2sg + Abl"));
    tester.expectAny("kendince", matchesTailLex("Pron + A2sg + P2sg + Equ"));
    tester.expectAny("kendinle", matchesTailLex("Pron + A2sg + P2sg + Ins"));

  }

  /**
   * Test for issue
   * <a href="https://github.com/ahmetaa/zemberek-nlp/issues/178">178</a>
   */
  @Test
  public void herkesteTest_issue_178() {
    AnalysisTester tester = getTester("herkes [P:Pron,Quant]");

    tester.expectSingle("herkese", matchesTailLex("Pron + A3pl + Dat"));
    tester.expectSingle("herkeste", matchesTailLex("Pron + A3pl + Loc"));
    tester.expectSingle("herkesten", matchesTailLex("Pron + A3pl + Abl"));
    tester.expectSingle("herkesçe", matchesTailLex("Pron + A3pl + Equ"));
    tester.expectSingle("herkesle", matchesTailLex("Pron + A3pl + Ins"));
  }

  /**
   * Test for issue
   * <a href="https://github.com/ahmetaa/zemberek-nlp/issues/188">178</a>
   * Cannot analyze sendeki, bendeki etc.
   */
  @Test
  public void sendekiTest_issue_188() {
    AnalysisTester tester = getTester("ben [P:Pron,Pers]",
        "sen [P:Pron,Pers]",
        "o [P:Pron,Pers]",
        "biz [P:Pron,Pers]",
        "siz [P:Pron,Pers]"
    );

    tester.expectSingle("bendeki", matchesTailLex("Pron + A1sg + Loc + Rel + Adj"));
    tester.expectSingle("sendeki", matchesTailLex("Pron + A2sg + Loc + Rel + Adj"));
    tester.expectSingle("ondaki", matchesTailLex("Pron + A3sg + Loc + Rel + Adj"));
    tester.expectSingle("bizdeki", matchesTailLex("Pron + A1pl + Loc + Rel + Adj"));
    tester.expectSingle("sizdeki", matchesTailLex("Pron + A2pl + Loc + Rel + Adj"));
    tester.expectSingle("onlardaki", matchesTailLex("Pron + A3pl + Loc + Rel + Adj"));
  }

}
