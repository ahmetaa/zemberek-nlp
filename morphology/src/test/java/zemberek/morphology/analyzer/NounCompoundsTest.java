package zemberek.morphology.analyzer;

import org.junit.Test;

public class NounCompoundsTest extends AnalyzerTestBase {

  @Test
  public void incorrect1() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    expectFail(analyzer, "zeytinyağ", "zeytinyağıya", "zeytinyağılar", "zeytinyağlar"
        , "zeytinyağya", "zeytinyağna", "zeytinyağda", "zeytinyağdan");
  }

  @Test
  public void incorrect2() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    expectFail(analyzer, "balkabak", "balkabağa", "balkabakta", "balkabaktan");
  }

  @Test
  public void expectsResult() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    expectSuccess(analyzer, "zeytinyağı", "zeytinyağına", "zeytinyağım", "zeytinyağlarıma");
  }

  @Test
  public void expectsResult2() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    expectSuccess(analyzer, "balkabağı", "balkabakları", "balkabağına");
  }

  @Test
  public void mustHaveTwoResults() {
    InterpretingAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    expectSuccess(analyzer, 2, "zeytinyağı");
  }

}
