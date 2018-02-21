package zemberek.morphology.analysis;

import org.junit.Test;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class VerbsAfterNounAdjTest extends AnalyzerTestBase {

  @Test
  public void expectsSingleResult() {
    InterpretingAnalyzer analyzer = getAnalyzer("mavi [P:Adj]");
    shouldPass(analyzer, 1, "maviyim");
    shouldPass(analyzer, 1, "maviydim");
    shouldPass(analyzer, 1, "maviyimdir");
    shouldPass(analyzer, 1, "maviydi");
    shouldPass(analyzer, 1, "mavidir");
  }

  @Test
  public void expectsSingleResult2() {
    InterpretingAnalyzer analyzer = getAnalyzer("elma");
    shouldPass(analyzer, 1, "elmayım");
    shouldPass(analyzer, 1, "elmaydım");
    shouldPass(analyzer, 1, "elmayımdır");
    shouldPass(analyzer, 1, "elmaydı");
    shouldPass(analyzer, 1, "elmadır");
  }


}
