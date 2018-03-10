package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

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
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    String in = "kitabım";
    List<AnalysisResult> results = analyzer.analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);

    Assert.assertEquals("kitap", first.getDictionaryItem().lemma);
    Assert.assertEquals("kitab", first.root);
    Assert.assertTrue(containsMorpheme(first, "P1sg"));
  }

  @Test
  public void voicingIncorrect_1() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    expectFail(analyzer, "kitapım", "kitab", "kitabcık", "kitapa", "kitablar");
  }

  @Test
  public void suTest() {
    AnalysisTester t = getTester("su");

    t.expectSingle("su", matchesTailLex("Noun + A3sg + Pnon + Nom"));
    t.expectSingle("sulara", matchesTailLex("Noun + A3pl + Pnon + Dat"));
    t.expectSingle("suyuma", matchesTailLex("Noun + A3sg + P1sg + Dat"));
    t.expectSingle("suyun", matchesTailLex("Noun + A3sg + P2sg + Nom"));
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
  public void Ablative() {
    AnalysisTester t = getTester("ev");

    t.expectAny("evden", matchesTailLex("Noun + A3sg + Pnon + Abl"));
    t.expectSingle("evlerden", matchesTailLex("Noun + A3pl + Pnon + Abl"));
    t.expectSingle("evimden", matchesTailLex("Noun + A3sg + P1sg + Abl"));
    t.expectAny("evimizden", matchesTailLex("Noun + A3sg + P1pl + Abl"));

    t = getTester("kitap");

    t.expectAny("kitaptan", matchesTailLex("Noun + A3sg + Pnon + Abl"));
    t.expectAny("kitabımdan", matchesTailLex("Noun + A3pl + P1sg + Abl"));

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

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağında", matchesTailLex("Noun + A3sg + Pnon + Loc"));
    t.expectSingle("zeytinyağımda", matchesTailLex("Noun + A3sg + P1sg + Loc"));
    t.expectSingle("zeytinyağlarımızda", matchesTailLex("Noun + A3pl + P1pl + Loc"));
    t.expectSingle("zeytinyağlarınızda", matchesTailLex("Noun + A3pl + P2pl + Loc"));

  }
  

}
