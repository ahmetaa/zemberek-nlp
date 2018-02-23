package zemberek.morphology.analysis;

import org.junit.Test;

public class AdjectivesTest extends AnalyzerTestBase {

  @Test
  public void expectsSingleResult() {
    AnalysisTester tester = getTester("mavi [P:Adj]");
    tester.expectSuccess(
        1,
        "mavi",
        "maviye",
        "mavilere",
        "mavilerime",
        "mavicik",
        "mavili",
        "mavicikli",
        "mavicikliye"
    );
  }

  @Test
  public void expects2Results() {
    AnalysisTester tester = getTester("mavi [P:Adj]");
    tester.expectSuccess(
        2,
        "maviler"
    );
  }


}
