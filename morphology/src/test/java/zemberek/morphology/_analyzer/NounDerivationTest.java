package zemberek.morphology._analyzer;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class NounDerivationTest extends AnalyzerTestBase {

  @Test
  public void withTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectSingle("meyveli",
        matchesTailLex("Pnon + Nom + With + Adj"));
  }

  @Test
  public void withoutTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectSingle("meyvesiz",
        matchesTailLex("Pnon + Nom + Without + Adj"));
    tester.expectSingle("meyvesizdi",
        matchesTailLex("Pnon + Nom + Without + Adj + Zero + Verb + Past + A3sg"));

    tester.expectFail(
        "meyvemsiz",
        "meyvelersiz",
        "meyvedesiz",
        "meyvesizli",
        "meyvelisiz"
    );
  }

  @Test
  public void justlikeTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectSingle("meyvemsi",
        matchesTailLex("Pnon + Nom + JustLike + Adj"));
    tester = getTester("odun");
    tester.expectSingle("odunsu",
        matchesTailLex("Pnon + Nom + JustLike + Adj"));
    tester.expectSingle("odunumsu",
        matchesTailLex("Pnon + Nom + JustLike + Adj"));
    tester = getTester("kitap");
    tester.expectSingle("kitabımsı",
        matchesTailLex("Pnon + Nom + JustLike + Adj"));
  }

  // check for
  // incorrect P1sg analysis for meyvemsi.
  // incorrect JustLike analysis for meyvesi.
  @Test
  public void justLikeFalseTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectFalse("meyvemsi",
        matchesTailLex("P1sg + Nom + JustLike + Adj"));
    tester.expectFalse("meyvesi",
        matchesTailLex("Pnon + Nom + JustLike + Adj"));
  }

  @Test
  public void incorrect1() {
    AnalysisTester tester = getTester("meyve");
    tester.expectFail("meyvelili");
    tester.expectFail("meyvelerli");
    tester.expectFail("meyvemli");
    tester.expectFail("meyveyeli");
    tester.expectFail("meyvelersi");
    tester.expectFail("meyveyemsi");
    tester.expectFail("meyvensi");
    tester = getTester("armut");
    tester.expectFail("armudsu");
    tester.expectFail("armutumsu");
    tester.expectFail("armutlarımsı");
    tester.expectFail("armutlarsı");
  }

  @Test
  public void rel1() {
    AnalysisTester tester = getTester("meyve");
    tester.expectSingle("meyvedeki",
        matchesTailLex("Noun + A3sg + Pnon + Loc + Rel + Adj"));
    tester.expectAny("meyvelerdeki",
        matchesTailLex("Noun + A3pl + Pnon + Loc + Rel + Adj"));
    tester.expectSingle("meyvedekiydi",
        matchesTailLex("Noun + A3sg + Pnon + Loc + Rel + Adj + Zero + Verb + Past + A3sg"));

    tester.expectFail(
        "meyveki",
        "meyveyeki",
        "meyvedekideki",
        "meyvemki"
    );
  }

  @Test
  public void relTime() {
    AnalysisTester tester = getTester("dün [P:Time]");
    tester.expectSingle("dünkü",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Rel + Adj"));
    tester.expectSingle("dünküydü",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Rel + Adj + Zero + Verb + Past + A3sg"));
    tester = getTester("akşam [P:Time]");
    tester.expectSingle("akşamki",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Rel + Adj"));
    tester.expectSingle("akşamkiydi",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Rel + Adj + Zero + Verb + Past + A3sg"));
    tester.expectFail(
        "dünki",
        "akşamkü",
        "akşamkı",
        "akşamdaki"
    );
  }

  @Test
  public void noun2Noun_1() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    String in = "kitapçık";
    List<AnalysisResult> results = analyzer.analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);
    Assert.assertTrue(containsMorpheme(first, "Dim"));
  }


  @Test
  public void noun2NounIncorrect_1() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    expectFail(analyzer,
        "kitaplarcık", "kitapçıklarcık", "kitapçığ", "kitapcık", "kitabımcık",
        "kitaptacık", "kitapçıkçık", "kitabcığ", "kitabçığ", "kitabçık", "kitapçığ"
    );
  }

  @Test
  public void nessTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectAny("elmalık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Ness + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("elmalığı",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Ness + Noun + A3sg + Pnon + Acc"));
    tester.expectAny("elmalığa",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Ness + Noun + A3sg + Pnon + Dat"));
    tester.expectAny("elmasızlık",
        matchesTailLex(
            "Noun + A3sg + Pnon + Nom + Without + Adj + Ness + Noun + A3sg + Pnon + Nom"));

    tester.expectFail(
        "elmalarlık",
        "elmamlık",
        "elmlığ",
        "elmayalık",
        "elmadalık"
    );
  }

  @Test
  public void agtTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectAny("elmacı",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Agt + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("elmacıyı",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Agt + Noun + A3sg + Pnon + Acc"));
    tester.expectAny("elmacıya",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Agt + Noun + A3sg + Pnon + Dat"));
    tester.expectAny("elmacıkçı",
        matchesTailLex(
            "Noun + A3sg + Pnon + Nom + Dim + Noun + A3sg + Pnon + Nom + Agt + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("elmacılık",
        matchesTailLex(
            "Noun + A3sg + Pnon + Nom + Agt + Noun + A3sg + Pnon + Nom + Ness + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("elmacılığı",
        matchesTailLex(
            "Noun + A3sg + Pnon + Nom + Agt + Noun + A3sg + Pnon + Nom + Ness + Noun + A3sg + Pnon + Acc"));

    tester.expectFail(
        "elmalarcı",
        "elmamcı",
        "elmayacı",
        "elmadacı"
    );
  }


  @Test
  public void become() {
    AnalysisTester t = getTester("tahta");

    t.expectAny("tahtalaş",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Imp + A2sg"));
    t.expectAny("tahtalaştık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Past + A1pl"));
    t.expectAny("tahtalaşacak",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Fut + A3sg"));

    t.expectFail(
        "tahtamlaş",
        "tahtalarlaştı",
        "tahtayalaştı"
    );

    t = getTester("kitap");
    t.expectAny("kitaplaştı",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Past + A3sg"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağlaştık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Past + A1pl"));
  }

  @Test
  public void acquire() {
    AnalysisTester t = getTester("tahta");

    t.expectAny("tahtalan",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Imp + A2sg"));
    t.expectAny("tahtalandık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Past + A1pl"));
    t.expectAny("tahtalanacak",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Fut + A3sg"));

    t.expectFail(
        "tahtamlan",
        "tahtalarlandı",
        "tahtayaland"
    );

    t = getTester("kitap");
    t.expectAny("kitaplandı",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Past + A3sg"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağlandık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Past + A1pl"));
  }

  @Test
  public void whileTest() {
    AnalysisTester t = getTester("tahta");

    t.expectAny("tahtayken",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Zero + Verb + While + Adv"));
    t.expectAny("tahtamken",
        matchesTailLex("Noun + A3sg + P1sg + Nom + Zero + Verb + While + Adv"));
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
  public void incorrect2() {
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

    tester.expectSingle("elmaysa", matchesTailLex("Zero + Verb + Cond + A3sg"));
    tester.expectSingle("elmaysak", matchesTailLex("Zero + Verb + Cond + A1pl"));
    tester.expectSingle("elmaymışsa", matchesTailLex("Zero + Verb + Narr + Cond + A3sg"));
    tester.expectSingle("elmaymışsam", matchesTailLex("Zero + Verb + Narr + Cond + A1sg"));

    tester.expectFail(
        "elmaydıysa",
        "elmaysadır",
        "elmaysalardır"
    );
  }

  @Test
  public void A2plTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectSingle("elmasınız", matchesTailLex("Zero + Verb + Pres + A2pl"));
    tester.expectSingle("elmaydınız", matchesTailLex("Zero + Verb + Past + A2pl"));
    tester.expectSingle("elmaymışsınız", matchesTailLex("Zero + Verb + Narr + A2pl"));
    tester.expectSingle("elmaysanız", matchesTailLex("Zero + Verb + Cond + A2pl"));
    tester.expectSingle("elmaymışsanız", matchesTailLex("Zero + Verb + Narr + Cond + A2pl"));
  }

  @Test
  public void A3plAfterZeroVerbDerivationTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectAny("elmalar", matchesTailLex("Zero + Verb + Pres + A3pl"));
    tester.expectAny("elmalardır", matchesTailLex("Zero + Verb + Pres + A3pl + Cop"));
    tester.expectAny("elmadırlar", matchesTailLex("Nom + Zero + Verb + Pres + Cop + A3pl"));
    tester.expectAny("elmayadırlar", matchesTailLex("Dat + Zero + Verb + Pres + Cop + A3pl"));
    tester.expectAny("elmasındalar", matchesTailLex("Loc + Zero + Verb + Pres + A3pl"));
  }

  @Test
  public void afterLocTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectSingle("elmadayım", matchesTailLex("Loc + Zero + Verb + Pres + A1sg"));
    tester.expectSingle("elmadasın", matchesTailLex("Loc + Zero + Verb + Pres + A2sg"));
    tester.expectSingle("elmadaydı", matchesTailLex("Loc + Zero + Verb + Past + A3sg"));
    tester.expectSingle("elmadaymışsınız", matchesTailLex("Loc + Zero + Verb + Narr + A2pl"));
    tester.expectSingle("elmadaysak", matchesTailLex("Loc + Zero + Verb + Cond + A1pl"));
  }

  @Test
  public void copulaBeforeA3plTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectSingle("elmadırlar",
        matchesTailLex("Nom + Zero + Verb + Pres + Cop + A3pl"));
    tester.expectAny("elmadalardır",
        matchesTailLex("Pnon + Loc + Zero + Verb + Pres + A3pl + Cop"));
    tester.expectSingle("elmamdadırlar",
        matchesTailLex("P1sg + Loc + Zero + Verb + Pres + Cop + A3pl"));

    tester.expectFail(
        "elmadalardırlar",
        "elmadadırlardır"
    );
  }

  @Test
  public void related() {
    AnalysisTester tester = getTester("meyve");
    tester.expectSingle("meyvesel",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Related + Adj"));
    tester.expectAny("meyveseldi",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Related + Adj + Zero + Verb + Past + A3sg"));

    tester.expectFail(
        "meyvemsel",
        "meyvedesel",
        "meyveselsel",
        "meyveselki"
    );
  }

  @Test
  public void relPronDerivationTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectAny("meyveninki",
        matchesTailLex("Noun + A3sg + Pnon + Gen + Rel + Pron + A3sg + Pnon + Nom"));
    tester.expectAny("meyveninkine",
        matchesTailLex("Noun + A3sg + Pnon + Gen + Rel + Pron + A3sg + Pnon + Dat"));
    tester.expectAny("meyveminkine",
        matchesTailLex("Noun + A3sg + P1sg + Gen + Rel + Pron + A3sg + Pnon + Dat"));
    tester.expectAny("meyveminkinde",
        matchesTailLex("Noun + A3sg + P1sg + Gen + Rel + Pron + A3sg + Pnon + Loc"));
    tester.expectAny("meyveminkindeymiş",
        matchesTailLex(
            "Noun + A3sg + P1sg + Gen + Rel + Pron + A3sg + Pnon + Loc + Zero + Verb + Narr + A3sg"));
  }


}
