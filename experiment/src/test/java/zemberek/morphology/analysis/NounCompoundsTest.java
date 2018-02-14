package zemberek.morphology.analysis;

import org.junit.Test;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class NounCompoundsTest extends AnalyzerTestBase {

  @Test
  public void incorrect1() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    shouldNotPass(analyzer, "zeytinyağ", "zeytinyağıya", "zeytinyağılar", "zeytinyağlar");
  }

  @Test
  public void incorrect2() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    shouldNotPass(analyzer, "balkabak", "balkabağa", "balkabakta", "balkabaktan");
  }

  @Test
  public void expectsResult() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    shouldPass(analyzer, "zeytinyağı", "zeytinyağına", "zeytinyağım", "zeytinyağlarıma");
  }

  @Test
  public void expectsSingleResult() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    shouldPass(analyzer, 1, "balkabağı", "balkabakları");
  }

}
