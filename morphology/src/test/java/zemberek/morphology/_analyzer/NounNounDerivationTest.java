package zemberek.morphology._analyzer;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class NounNounDerivationTest extends AnalyzerTestBase {

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
    tester.expectAny("elmalığa",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Ness + Noun + A3sg + Pnon + Dat"));
    tester.expectAny("elmasızlık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Without + Adj + Ness + Noun + A3sg + Pnon + Nom"));

    tester.expectFail(
        "elmalarlık",
        "elmamlık",
        "elmlığ",
        "elmayalık",
        "elmadalık"
    );
  }

}
