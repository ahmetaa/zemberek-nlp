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
}
