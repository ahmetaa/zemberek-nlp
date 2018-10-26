package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.RootAttribute;

public class NounCompoundsTest extends AnalyzerTestBase {

  @Test
  public void incorrect1() {
    RuleBasedAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    expectFail(analyzer, "zeytinyağ", "zeytinyağıya", "zeytinyağılar", "zeytinyağlar"
        , "zeytinyağya", "zeytinyağna", "zeytinyağda", "zeytinyağdan");
  }

  @Test
  public void incorrect2() {
    RuleBasedAnalyzer analyzer = getAnalyzer(
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    expectFail(analyzer, "balkabak", "balkabağa", "balkabakta", "balkabaktan");
  }

  @Test
  public void expectsResult() {
    RuleBasedAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    expectSuccess(analyzer, "zeytinyağı", "zeytinyağına", "zeytinyağım", "zeytinyağlarıma");
  }

  @Test
  public void expectsResult2() {
    RuleBasedAnalyzer analyzer = getAnalyzer(
        "bal",
        "kabak",
        "balkabağı [A:CompoundP3sg; Roots:bal-kabak]");
    expectSuccess(analyzer, "balkabağı", "balkabakları", "balkabağına");
  }

  @Test
  public void mustHaveTwoResults() {
    RuleBasedAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    expectSuccess(analyzer, 2, "zeytinyağı");
  }

  @Test
  public void resultDictionaryItemCannotBeDummy() {
    RuleBasedAnalyzer analyzer = getAnalyzer(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
    List<SingleAnalysis> analyses = analyzer.analyze("zeytinyağlı");
    Assert.assertEquals(1, analyses.size());
    SingleAnalysis a = analyses.get(0);
    Assert.assertTrue(!a.isUnknown());
    Assert.assertEquals("zeytinyağı", a.getDictionaryItem().lemma);
    Assert.assertFalse(a.getDictionaryItem().hasAttribute(RootAttribute.Dummy));
  }

}
