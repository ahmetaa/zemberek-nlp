package zemberek.morphology.analysis;

import org.junit.Test;

public class NounDerivationTest extends AnalyzerTestBase {

  @Test
  public void withTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectSingle("meyveli",
        matchesTailLex("A3sg + With + Adj"));

    tester = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    tester.expectAny("zeytinyağlı",
        matchesTailLex("Noun + A3sg + With + Adj"));

  }

  @Test
  public void withoutTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectSingle("meyvesiz",
        matchesTailLex("A3sg + Without + Adj"));
    tester.expectSingle("meyvesizdi",
        matchesTailLex("A3sg + Without + Adj + Zero + Verb + Past + A3sg"));

    tester.expectFail(
        "meyvemsiz",
        "meyvelersiz",
        "meyvedesiz",
        "meyvesizli",
        "meyvelisiz"
    );

    tester = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    tester.expectAny("zeytinyağsız",
        matchesTailLex("Noun + A3sg + Without + Adj"));
  }

  @Test
  public void justlikeTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectSingle("meyvemsi",
        matchesTailLex("A3sg + JustLike + Adj"));
    tester = getTester("odun");
    tester.expectSingle("odunsu",
        matchesTailLex("A3sg + JustLike + Adj"));
    tester.expectSingle("odunumsu",
        matchesTailLex("A3sg + JustLike + Adj"));
    tester = getTester("kitap");
    tester.expectSingle("kitabımsı",
        matchesTailLex("A3sg + JustLike + Adj"));

    tester = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    tester.expectAny("zeytinyağımsı",
        matchesTailLex("Noun + A3sg + JustLike + Adj"));
    tester.expectAny("zeytinyağsı",
        matchesTailLex("Noun + A3sg + JustLike + Adj"));
  }

  // check for
  // incorrect P1sg analysis for meyvemsi.
  // incorrect JustLike analysis for meyvesi.
  @Test
  public void justLikeFalseTest() {
    AnalysisTester tester = getTester("meyve");
    tester.expectFalse("meyvemsi",
        matchesTailLex("P1sg + JustLike + Adj"));
    tester.expectFalse("meyvesi",
        matchesTailLex("A3sg + JustLike + Adj"));
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
        matchesTailLex("Noun + A3sg + Loc + Rel + Adj"));
    tester.expectAny("meyvendeki",
        matchesTailLex("Noun + A3sg + P2sg + Loc + Rel + Adj"));
    tester.expectAny("meyvelerdeki",
        matchesTailLex("Noun + A3pl + Loc + Rel + Adj"));
    tester.expectSingle("meyvedekiydi",
        matchesTailLex("Noun + A3sg + Loc + Rel + Adj + Zero + Verb + Past + A3sg"));

    tester.expectFail(
        "meyveki",
        "meyveyeki",
        "meyvedekideki",
        "meyvemki"
    );

    tester = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    tester.expectAny("zeytinyağındaki",
        matchesTailLex("Noun + A3sg + Loc + Rel + Adj"));
  }

  @Test
  public void relTime() {
    AnalysisTester tester = getTester("dün [P:Time]");
    tester.expectSingle("dünkü",
        matchesTailLex("Noun + A3sg + Rel + Adj"));
    tester.expectSingle("dünküydü",
        matchesTailLex("Noun + A3sg + Rel + Adj + Zero + Verb + Past + A3sg"));
    tester = getTester("akşam [P:Time]");
    tester.expectSingle("akşamki",
        matchesTailLex("Noun + A3sg + Rel + Adj"));
    tester.expectSingle("akşamkiydi",
        matchesTailLex("Noun + A3sg + Rel + Adj + Zero + Verb + Past + A3sg"));

    // Unlike Oflazer, we allow thıs:
    tester.expectSingle("akşamdaki",
        matchesTailLex("Noun + A3sg + Loc + Rel + Adj"));
    tester.expectAny("akşamındaki",
        matchesTailLex("Noun + A3sg + P2sg + Loc + Rel + Adj"));

    tester = getTester("ileri");
    tester.expectSingle("ileriki",
        matchesTailLex("Noun + A3sg + Rel + Adj"));
    tester.expectSingle("ilerikiydi",
        matchesTailLex("Noun + A3sg + Rel + Adj + Zero + Verb + Past + A3sg"));

    tester.expectFail(
        "dünki",
        "akşamkü",
        "akşamkı"
    );
  }

  @Test
  public void dim1() {

    AnalysisTester tester = getTester("kitap");
    tester.expectSingle("kitapçık",
        matchesTailLex("Noun + A3sg + Dim + Noun + A3sg"));
    tester.expectSingle("kitapçıkta",
        matchesTailLex("Noun + A3sg + Dim + Noun + A3sg + Loc"));
    tester.expectSingle("kitapçığa",
        matchesTailLex("Noun + A3sg + Dim + Noun + A3sg + Dat"));

    tester.expectFail(
        "kitaplarcık", "kitapçıklarcık", "kitapçığ", "kitapcık", "kitabımcık",
        "kitaptacık", "kitapçıkçık", "kitabcığ", "kitabçığ", "kitabçık", "kitapçığ"
    );

    tester = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    tester.expectAny("zeytinyağcık",
        matchesTailLex("Noun + A3sg + Dim + Noun + A3sg"));
  }


  @Test
  public void noun2NounIncorrect_1() {
    RuleBasedAnalyzer analyzer = getAnalyzer("kitap");
    expectFail(analyzer,
        "kitaplarcık", "kitapçıklarcık", "kitapçığ", "kitapcık", "kitabımcık",
        "kitaptacık", "kitapçıkçık", "kitabcığ", "kitabçığ", "kitabçık", "kitapçığ"
    );
  }

  @Test
  public void nessTest() {
    AnalysisTester tester = getTester("elma");

    tester.expectAny("elmalık",
        matchesTailLex("Noun + A3sg + Ness + Noun + A3sg"));
    tester.expectAny("elmalığı",
        matchesTailLex("Noun + A3sg + Ness + Noun + A3sg + Acc"));
    tester.expectAny("elmalığa",
        matchesTailLex("Noun + A3sg + Ness + Noun + A3sg + Dat"));
    tester.expectAny("elmasızlık",
        matchesTailLex(
            "Noun + A3sg + Without + Adj + Ness + Noun + A3sg"));

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
        matchesTailLex("Noun + A3sg + Agt + Noun + A3sg"));
    tester.expectAny("elmacıyı",
        matchesTailLex("Noun + A3sg + Agt + Noun + A3sg + Acc"));
    tester.expectAny("elmacıya",
        matchesTailLex("Noun + A3sg + Agt + Noun + A3sg + Dat"));
    tester.expectAny("elmacıkçı",
        matchesTailLex(
            "Noun + A3sg + Dim + Noun + A3sg + Agt + Noun + A3sg"));
    tester.expectAny("elmacılık",
        matchesTailLex(
            "Noun + A3sg + Agt + Noun + A3sg + Ness + Noun + A3sg"));
    tester.expectAny("elmacılığı",
        matchesTailLex(
            "Noun + A3sg + Agt + Noun + A3sg + Ness + Noun + A3sg + Acc"));

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
        matchesTailLex("Noun + A3sg + Become + Verb + Imp + A2sg"));
    t.expectAny("tahtalaştık",
        matchesTailLex("Noun + A3sg + Become + Verb + Past + A1pl"));
    t.expectAny("tahtalaşacak",
        matchesTailLex("Noun + A3sg + Become + Verb + Fut + A3sg"));

    t.expectFail(
        "tahtamlaş",
        "tahtalarlaştı",
        "tahtayalaştı"
    );

    t = getTester("kitap");
    t.expectAny("kitaplaştı",
        matchesTailLex("Noun + A3sg + Become + Verb + Past + A3sg"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağlaştık",
        matchesTailLex("Noun + A3sg + Become + Verb + Past + A1pl"));
  }

  @Test
  public void acquire() {
    AnalysisTester t = getTester("tahta");

    t.expectAny("tahtalan",
        matchesTailLex("Noun + A3sg + Acquire + Verb + Imp + A2sg"));
    t.expectAny("tahtalandık",
        matchesTailLex("Noun + A3sg + Acquire + Verb + Past + A1pl"));
    t.expectAny("tahtalanacak",
        matchesTailLex("Noun + A3sg + Acquire + Verb + Fut + A3sg"));

    t.expectFail(
        "tahtamlan",
        "tahtalarlandı",
        "tahtayaland"
    );

    t = getTester("kitap");
    t.expectAny("kitaplandı",
        matchesTailLex("Noun + A3sg + Acquire + Verb + Past + A3sg"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağlandık",
        matchesTailLex("Noun + A3sg + Acquire + Verb + Past + A1pl"));
  }

  @Test
  public void whileTest() {
    AnalysisTester t = getTester("tahta");

    t.expectAny("tahtayken",
        matchesTailLex("Noun + A3sg + Zero + Verb + While + Adv"));
    t.expectAny("tahtamken",
        matchesTailLex("Noun + A3sg + P1sg + Zero + Verb + While + Adv"));
  }

  @Test
  public void expectsSingleResult2() {
    RuleBasedAnalyzer analyzer = getAnalyzer("elma");
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
    RuleBasedAnalyzer analyzer = getAnalyzer("elma");
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
    tester.expectSingle("değildir", matchesTailLex("Neg + Pres + A3sg + Cop"));
    tester.expectSingle("değilimdir", matchesTailLex("Neg + Pres + A1sg + Cop"));
    tester.expectSingle("değilsindir", matchesTailLex("Neg + Pres + A2sg + Cop"));
    tester.expectSingle("değilsinizdir", matchesTailLex("Neg + Pres + A2pl + Cop"));
    tester.expectSingle("değilmişsinizdir", matchesTailLex("Neg + Narr + A2pl + Cop"));

    tester.expectFail(
        "değildinizdir"
    );
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
    tester.expectAny("elmadırlar", matchesTailLex("A3sg + Zero + Verb + Pres + Cop + A3pl"));
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
        matchesTailLex("Zero + Verb + Pres + Cop + A3pl"));
    tester.expectAny("elmadalardır",
        matchesTailLex("A3sg + Loc + Zero + Verb + Pres + A3pl + Cop"));
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
        matchesTailLex("Noun + A3sg + Related + Adj"));
    tester.expectAny("meyveseldi",
        matchesTailLex("Noun + A3sg + Related + Adj + Zero + Verb + Past + A3sg"));

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
        matchesTailLex("Noun + A3sg + Gen + Rel + Pron + A3sg"));
    tester.expectAny("meyveninkine",
        matchesTailLex("Noun + A3sg + Gen + Rel + Pron + A3sg + Dat"));
    tester.expectAny("meyveminkine",
        matchesTailLex("Noun + A3sg + P1sg + Gen + Rel + Pron + A3sg + Dat"));
    tester.expectAny("meyveminkinde",
        matchesTailLex("Noun + A3sg + P1sg + Gen + Rel + Pron + A3sg + Loc"));
    tester.expectAny("meyveminkindeymiş",
        matchesTailLex(
            "Noun + A3sg + P1sg + Gen + Rel + Pron + A3sg + Loc + Zero + Verb + Narr + A3sg"));
  }


  @Test
  public void noun2VerbAsIfTest() {
    AnalysisTester tester = getTester("dost");
    tester.expectSingle("dostmuşçasına",
        matchesTailLex("Zero + Verb + Narr + A3sg + AsIf + Adv"));
    tester.expectSingle("dostmuşlarcasına",
        matchesTailLex("Zero + Verb + Narr + A3pl + AsIf + Adv"));
  }

  /**
   * Test for Issue 170. After justlike derivation, P2sg should not be allowed. Such as: "güzelsin"
   */
  @Test
  public void justlikeTest_Issue_170() {
    AnalysisTester tester = getTester("güzel [P:Adj]");
    // no Justlike+Noun+A3sg+P2sg allowed
    tester.expectSingle("güzelsin", matchesTailLex("Zero + Verb + Pres + A2sg"));
    tester = getTester("odun");
    // no Justlike+Adj+Zero+A3sg+P2sg allowed
    tester.expectSingle("odunsun", matchesTailLex("Noun + A3sg + Zero + Verb + Pres + A2sg"));
  }

  /**
   * Test for Issue 167. For adjective to noun derivation like `mor-luk` two analysis was produced.
   * One was redundant.
   */
  @Test
  public void nessTest_Issue_167() {
    AnalysisTester tester = getTester("mor [P:Adj]");
    // no Adj|Zero→Noun+A3sg|luk:Ness→Noun+A3sg
    tester.expectSingle("morluk", matchesTailLex("Adj + Ness + Noun + A3sg"));
  }

  /**
   * Test for Issue 184 : Cannot analyze `abimsin` or any Noun+..+P1sg+..+Verb+..+A2sg
   */
  @Test
  public void A2sgVerbAfterP1sgNounTest_Issue_184() {
    AnalysisTester tester = getTester("abi");
    tester.expectSingle(
        "abimsin", matchesTailLex("Noun + A3sg + P1sg + Zero + Verb + Pres + A2sg"));
    tester.expectSingle(
        "abimsiniz", matchesTailLex("Noun + A3sg + P1sg + Zero + Verb + Pres + A2pl"));
    tester.expectSingle(
        "abinim", matchesTailLex("Noun + A3sg + P2sg + Zero + Verb + Pres + A1sg"));
  }

}
