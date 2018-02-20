package zemberek.morphology.analysis;

import org.junit.Test;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class AdjectivesTest extends AnalyzerTestBase {

  @Test
  public void expectsSingleResult() {
    InterpretingAnalyzer analyzer = getAnalyzer("mavi [P:Adj]");
    shouldPass(analyzer, 1, "mavi");
    shouldPass(analyzer, 1, "maviye");
    shouldPass(analyzer, 1, "maviler");
    shouldPass(analyzer, 1, "mavilere");
    shouldPass(analyzer, 1, "mavilerime");
    shouldPass(analyzer, 1, "mavicik");
    shouldPass(analyzer, 1, "mavili");
    shouldPass(analyzer, 1, "mavicikli");
    shouldPass(analyzer, 1, "mavicikliye");
  }




}
