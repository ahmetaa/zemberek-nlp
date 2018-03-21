package zemberek.morphology._analyzer;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class NounsTest extends AnalyzerTestBase {


  @Test
  public void shouldParse_1() {
    String in = "elmalar";
    List<AnalysisResult> results = getAnalyzer("elma").analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(2, results.size());
  }

  @Test
  public void implicitDative_1() {
    String in = "içeri";
    List<AnalysisResult> results = getAnalyzer("içeri [A:ImplicitDative]")
        .analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(2, results.size());
    AnalysisResult first = results.get(0);

    Assert.assertEquals("içeri_Noun", first.getDictionaryItem().id);
    Assert.assertEquals("içeri", first.root);
    Assert.assertTrue(containsMorpheme(first, "Dat"));
  }

  @Test
  public void implicitPLural_1() {
    String in = "hayvanat";
    List<AnalysisResult> results = getAnalyzer(
        "hayvanat [A:ImplicitPlural]")
        .analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);

    Assert.assertTrue(containsMorpheme(first, "A3pl"));
  }


  @Test
  public void voicing_1() {
    AnalysisTester t = getTester("kitap");
    t.expectSingle("kitap", matchesTailLex("Noun + A3sg + Pnon + Nom"));
    t.expectAny("kitaplar", matchesTailLex("Noun + A3pl + Pnon + Nom"));
    t.expectAny("kitabım", matchesTailLex("Noun + A3sg + P1sg + Nom"));
    t.expectAny("kitaba", matchesTailLex("Noun + A3sg + Pnon + Dat"));
    t.expectAny("kitapta", matchesTailLex("Noun + A3sg + Pnon + Loc"));
    t.expectAny("kitapçık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Dim + Noun + A3sg + Pnon + Nom"));

    t.expectFail( "kitapım", "kitab", "kitabcık", "kitapa", "kitablar");
  }

  @Test
  public void lastVowelDropExceptionTest() {
    AnalysisTester t = getTester("içeri [A:ImplicitDative, Special]");

    t.expectAny("içeri", matchesTailLex("Noun + A3sg + Pnon + Dat"));
    t.expectAny("içeride", matchesTailLex("Noun + A3sg + Pnon + Loc"));
    t.expectAny("içerilerde", matchesTailLex("Noun + A3pl + Pnon + Loc"));
    t.expectAny("içerde", matchesTailLex("Noun + A3sg + Pnon + Loc"));
    t.expectAny("içerlerde", matchesTailLex("Noun + A3pl + Pnon + Loc"));

    t.expectFail( "içer");
  }


  @Test
  public void suTest() {
    AnalysisTester t = getTester("su");

    t.expectSingle("su", matchesTailLex("Noun + A3sg + Pnon + Nom"));
    t.expectSingle("sulara", matchesTailLex("Noun + A3pl + Pnon + Dat"));
    t.expectSingle("suyuma", matchesTailLex("Noun + A3sg + P1sg + Dat"));
    t.expectAny("suyun", matchesTailLex("Noun + A3sg + P2sg + Nom"));
    t.expectAny("suyun", matchesTailLex("Noun + A3sg + Pnon + Gen"));
    t.expectSingle("suyumuz", matchesTailLex("Noun + A3sg + P1pl + Nom"));

    t.expectFail(
        "sunun",
        "susu",
        "sum",
        "sun"
    );
  }

  @Test
  public void P2pl() {
    AnalysisTester t = getTester("ev");

    t.expectAny("eviniz", matchesTailLex("Noun + A3sg + P2pl + Nom"));
    t.expectSingle("evinize", matchesTailLex("Noun + A3sg + P2pl + Dat"));
    t.expectSingle("evinizi", matchesTailLex("Noun + A3sg + P2pl + Acc"));
    t.expectAny("evleriniz", matchesTailLex("Noun + A3pl + P2pl + Nom"));
    t.expectSingle("evlerinize", matchesTailLex("Noun + A3pl + P2pl + Dat"));
    t.expectSingle("evlerinizi", matchesTailLex("Noun + A3pl + P2pl + Acc"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağınız", matchesTailLex("Noun + A3sg + P2pl + Nom"));
    t.expectSingle("zeytinyağınıza", matchesTailLex("Noun + A3sg + P2pl + Dat"));
    t.expectAny("zeytinyağlarınız", matchesTailLex("Noun + A3pl + P2pl + Nom"));
    t.expectSingle("zeytinyağlarınıza", matchesTailLex("Noun + A3pl + P2pl + Dat"));
  }

  @Test
  public void dative() {
    AnalysisTester t = getTester("ev");

    t.expectSingle("eve", matchesTailLex("Noun + A3sg + Pnon + Dat"));
    t.expectSingle("evlere", matchesTailLex("Noun + A3pl + Pnon + Dat"));
    t.expectSingle("evime", matchesTailLex("Noun + A3sg + P1sg + Dat"));
    t.expectSingle("evimize", matchesTailLex("Noun + A3sg + P1pl + Dat"));

    t = getTester("kitap");

    t.expectSingle("kitaba", matchesTailLex("Noun + A3sg + Pnon + Dat"));
    t.expectSingle("kitabıma", matchesTailLex("Noun + A3sg + P1sg + Dat"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağına", matchesTailLex("Noun + A3sg + Pnon + Dat"));
    t.expectSingle("zeytinyağıma", matchesTailLex("Noun + A3sg + P1sg + Dat"));
    t.expectSingle("zeytinyağlarımıza", matchesTailLex("Noun + A3pl + P1pl + Dat"));
    t.expectSingle("zeytinyağlarınıza", matchesTailLex("Noun + A3pl + P2pl + Dat"));
  }

  @Test
  public void Ablative() {
    AnalysisTester t = getTester("ev");

    t.expectAny("evden", matchesTailLex("Noun + A3sg + Pnon + Abl"));
    t.expectSingle("evlerden", matchesTailLex("Noun + A3pl + Pnon + Abl"));
    t.expectSingle("evimden", matchesTailLex("Noun + A3sg + P1sg + Abl"));
    t.expectAny("evimizden", matchesTailLex("Noun + A3sg + P1pl + Abl"));

    t = getTester("kitap");

    t.expectAny("kitaptan", matchesTailLex("Noun + A3sg + Pnon + Abl"));
    t.expectAny("kitabımdan", matchesTailLex("Noun + A3sg + P1sg + Abl"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağından", matchesTailLex("Noun + A3sg + Pnon + Abl"));
    t.expectSingle("zeytinyağımdan", matchesTailLex("Noun + A3sg + P1sg + Abl"));
    t.expectSingle("zeytinyağlarımızdan", matchesTailLex("Noun + A3pl + P1pl + Abl"));
    t.expectSingle("zeytinyağlarınızdan", matchesTailLex("Noun + A3pl + P2pl + Abl"));
  }

  @Test
  public void Locative() {
    AnalysisTester t = getTester("ev");

    t.expectAny("evde", matchesTailLex("Noun + A3sg + Pnon + Loc"));
    t.expectSingle("evlerde", matchesTailLex("Noun + A3pl + Pnon + Loc"));
    t.expectSingle("evimde", matchesTailLex("Noun + A3sg + P1sg + Loc"));
    t.expectAny("evimizde", matchesTailLex("Noun + A3sg + P1pl + Loc"));

    t = getTester("kitap");

    t.expectAny("kitapta", matchesTailLex("Noun + A3sg + Pnon + Loc"));
    t.expectAny("kitabımda", matchesTailLex("Noun + A3sg + P1sg + Loc"));

    t = getTester("elma");

    t.expectAny("elmada", matchesTailLex("Noun + A3sg + Pnon + Loc"));
    t.expectAny("elmanda", matchesTailLex("Noun + A3sg + P2sg + Loc"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağında", matchesTailLex("Noun + A3sg + Pnon + Loc"));
    t.expectSingle("zeytinyağımda", matchesTailLex("Noun + A3sg + P1sg + Loc"));
    t.expectSingle("zeytinyağlarımızda", matchesTailLex("Noun + A3pl + P1pl + Loc"));
    t.expectSingle("zeytinyağlarınızda", matchesTailLex("Noun + A3pl + P2pl + Loc"));
  }

  @Test
  public void Locative2() {
    AnalysisTester t = getTester("ev");

    t.expectAny("evdeyim", matchesTailLex("Noun + A3sg + Pnon + Loc + Zero + Verb + Pres + A1sg"));
  }

  @Test
  public void Instrumental() {
    AnalysisTester t = getTester("ev");

    t.expectAny("evle", matchesTailLex("Noun + A3sg + Pnon + Ins"));
    t.expectSingle("evlerle", matchesTailLex("Noun + A3pl + Pnon + Ins"));
    t.expectSingle("evimle", matchesTailLex("Noun + A3sg + P1sg + Ins"));
    t.expectAny("evimizle", matchesTailLex("Noun + A3sg + P1pl + Ins"));

    t = getTester("kitap");

    t.expectAny("kitapla", matchesTailLex("Noun + A3sg + Pnon + Ins"));
    t.expectAny("kitabımla", matchesTailLex("Noun + A3sg + P1sg + Ins"));

    t = getTester("elma");

    t.expectAny("elmayla", matchesTailLex("Noun + A3sg + Pnon + Ins"));
    t.expectAny("elmanla", matchesTailLex("Noun + A3sg + P2sg + Ins"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağıyla", matchesTailLex("Noun + A3sg + Pnon + Ins"));
    t.expectSingle("zeytinyağımla", matchesTailLex("Noun + A3sg + P1sg + Ins"));
    t.expectSingle("zeytinyağlarımızla", matchesTailLex("Noun + A3pl + P1pl + Ins"));
    t.expectSingle("zeytinyağlarınızla", matchesTailLex("Noun + A3pl + P2pl + Ins"));
  }

  @Test
  public void genitive() {
    AnalysisTester t = getTester("ev");

    t.expectAny("evin", matchesTailLex("Noun + A3sg + Pnon + Gen"));
    t.expectAny("evlerin", matchesTailLex("Noun + A3pl + Pnon + Gen"));
    t.expectSingle("evimin", matchesTailLex("Noun + A3sg + P1sg + Gen"));
    t.expectSingle("evimizin", matchesTailLex("Noun + A3sg + P1pl + Gen"));

    t = getTester("kitap");

    t.expectAny("kitabın", matchesTailLex("Noun + A3sg + Pnon + Gen"));
    t.expectSingle("kitabımın", matchesTailLex("Noun + A3sg + P1sg + Gen"));

    t = getTester("elma");

    t.expectSingle("elmamın", matchesTailLex("Noun + A3sg + P1sg + Gen"));
    t.expectAny("elmanın", matchesTailLex("Noun + A3sg + P2sg + Gen"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağının", matchesTailLex("Noun + A3sg + Pnon + Gen"));
    t.expectSingle("zeytinyağımın", matchesTailLex("Noun + A3sg + P1sg + Gen"));
    t.expectSingle("zeytinyağlarımızın", matchesTailLex("Noun + A3pl + P1pl + Gen"));
    t.expectSingle("zeytinyağlarınızın", matchesTailLex("Noun + A3pl + P2pl + Gen"));
  }


  @Test
  public void equ() {
    AnalysisTester t = getTester("ev");

    t.expectAny("evce", matchesTailLex("Noun + A3sg + Pnon + Equ"));
    t.expectAny("evlerce", matchesTailLex("Noun + A3pl + Pnon + Equ"));
    t.expectSingle("evimce", matchesTailLex("Noun + A3sg + P1sg + Equ"));
    t.expectSingle("evimizce", matchesTailLex("Noun + A3sg + P1pl + Equ"));
    t.expectAny("evlerince", matchesTailLex("Noun + A3pl + P3sg + Equ"));

    t = getTester("kitap");

    t.expectAny("kitapça", matchesTailLex("Noun + A3sg + Pnon + Equ"));
    t.expectAny("kitaplarınca", matchesTailLex("Noun + A3pl + P3pl + Equ"));
    t.expectSingle("kitabımca", matchesTailLex("Noun + A3sg + P1sg + Equ"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağınca", matchesTailLex("Noun + A3sg + Pnon + Equ"));
  }

  @Test
  public void P3pl() {
    AnalysisTester t = getTester("ev");

    // P3pl typically generates 4 analysis
    t.expectAny("evleri", matchesTailLex("Noun + A3pl + Pnon + Acc"));
    t.expectAny("evleri", matchesTailLex("Noun + A3pl + P3sg + Nom"));
    t.expectAny("evleri", matchesTailLex("Noun + A3sg + P3pl + Nom"));
    t.expectAny("evleri", matchesTailLex("Noun + A3pl + P3pl + Nom"));

    t.expectAny("evlerine", matchesTailLex("Noun + A3sg + P3pl + Dat"));
    t.expectAny("evlerinde", matchesTailLex("Noun + A3sg + P3pl + Loc"));
    t.expectAny("evlerinden", matchesTailLex("Noun + A3sg + P3pl + Abl"));
    t.expectAny("evleriyle", matchesTailLex("Noun + A3sg + P3pl + Ins"));
    t.expectAny("evlerini", matchesTailLex("Noun + A3sg + P3pl + Acc"));

    t = getTester("kitap");
    t.expectAny("kitapları", matchesTailLex("Noun + A3pl + P3sg + Nom"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağları", matchesTailLex("Noun + A3pl + Pnon + Nom"));
    t.expectAny("zeytinyağları", matchesTailLex("Noun + A3pl + P3pl + Nom"));
    t.expectAny("zeytinyağları", matchesTailLex("Noun + A3pl + P3sg + Nom"));
    t.expectAny("zeytinyağları", matchesTailLex("Noun + A3sg + P3pl + Nom"));
    t.expectAny("zeytinyağlarına", matchesTailLex("Noun + A3pl + P3sg + Dat"));
  }

  @Test
  public void family1() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "annemler [A:ImplicitPlural,ImplicitP1sg,FamilyMember]");
    expectFail(analyzer, "annemlerler", "annemlerim");
  }

  @Test
  public void family2() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "annemler [A:ImplicitPlural,ImplicitP1sg,FamilyMember]");
    expectSuccess(analyzer, 1, "annemler", "annemlere", "annemleri");
  }

  @Test
  public void family3() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "annemler [A:ImplicitPlural,ImplicitP1sg,FamilyMember]");
    String in = "annemleri";
    List<AnalysisResult> results = analyzer.analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);
    Assert.assertTrue(containsMorpheme(first, "Acc"));
    Assert.assertTrue(!containsMorpheme(first, "P3sg"));
  }


}
