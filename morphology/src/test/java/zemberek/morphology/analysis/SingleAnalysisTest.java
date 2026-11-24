package zemberek.morphology.analysis;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeGroup;

public class SingleAnalysisTest extends AnalyzerTestBase {

  @Test
  public void stemEndingTest() {
    RuleBasedAnalyzer analyzer = getAnalyzer("kitap");
    List<SingleAnalysis> analyses = analyzer.analyze("kitaplarda");
    Assert.assertEquals(1, analyses.size());
    SingleAnalysis analysis = analyses.get(0);

    Assert.assertEquals(analysis.getDictionaryItem(),
        analyzer.getLexicon().getItemById("kitap_Noun"));
    Assert.assertEquals("larda", analysis.getEnding());
    Assert.assertEquals("kitap", analysis.getStem());
  }

  @Test
  public void getPosTest() {
    RuleBasedAnalyzer analyzer = getAnalyzer("görmek");
    List<SingleAnalysis> analyses = analyzer.analyze("görmek");
    Assert.assertEquals(1, analyses.size());
    SingleAnalysis analysis = analyses.get(0);

    Assert.assertEquals(analysis.getDictionaryItem(),
        analyzer.getLexicon().getItemById("görmek_Verb"));
    Assert.assertEquals(PrimaryPos.Noun, analysis.getPos());
  }

  @Test
  public void morphemeGroupTest() {
    RuleBasedAnalyzer analyzer = getAnalyzer("kitap");

    SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    MorphemeGroup group = analysis.getGroup(0);
    Assert.assertEquals("kitaplarda", group.surfaceForm());

    analysis = analyzer.analyze("kitaplı").get(0);
    group = analysis.getGroup(0);
    Assert.assertEquals("kitap", group.surfaceForm());
    group = analysis.getGroup(1);
    Assert.assertEquals("lı", group.surfaceForm());

    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okutmuyor").get(0);

    Assert.assertEquals(2, analysis.getMorphemeGroupCount());
    MorphemeGroup group0 = analysis.getGroup(0);
    Assert.assertEquals("oku", group0.surfaceForm());
    MorphemeGroup group1 = analysis.getGroup(1);
    Assert.assertEquals("tmuyor", group1.surfaceForm());
  }

  static List<String> toList(String... input) {
    return Arrays.asList(input);
  }

  @Test
  public void getStemsTest() {
    RuleBasedAnalyzer analyzer = getAnalyzer("kitap");
    SingleAnalysis analysis = analyzer.analyze("kitap").get(0);
    Assert.assertEquals(toList("kitap"), analysis.getStems());

    analysis = analyzer.analyze("kitaplı").get(0);
    Assert.assertEquals(toList("kitap", "kitaplı"), analysis.getStems());

    analysis = analyzer.analyze("kitaplarda").get(0);
    Assert.assertEquals(toList("kitap"), analysis.getStems());

    analysis = analyzer.analyze("kitabımmış").get(0);
    Assert.assertEquals(toList("kitab", "kitabım"), analysis.getStems());

    analysis = analyzer.analyze("kitapçığa").get(0);
    Assert.assertEquals(toList("kitap", "kitapçığ"), analysis.getStems());

    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okut").get(0);
    Assert.assertEquals(toList("oku", "okut"), analysis.getStems());
    analysis = analyzer.analyze("okuttur").get(0);
    Assert.assertEquals(toList("oku", "okut", "okuttur"), analysis.getStems());
    analysis = analyzer.analyze("okutturuluyor").get(0);
    Assert.assertEquals(toList("oku", "okut", "okuttur", "okutturul"), analysis.getStems());
    analysis = analyzer.analyze("okutturamıyor").get(0);
    Assert.assertEquals(toList("oku", "okut", "okuttur"), analysis.getStems());
    analysis = analyzer.analyze("okutturabiliyor").get(0);
    Assert.assertEquals(toList("oku", "okut", "okuttur", "okutturabil"), analysis.getStems());
  }

  @Test
  public void getLemmasTest() {
    RuleBasedAnalyzer analyzer = getAnalyzer("kitap");
    SingleAnalysis analysis = analyzer.analyze("kitap").get(0);
    Assert.assertEquals(toList("kitap"), analysis.getLemmas());

    analysis = analyzer.analyze("kitaplı").get(0);
    Assert.assertEquals(toList("kitap", "kitaplı"), analysis.getLemmas());

    analysis = analyzer.analyze("kitaplarda").get(0);
    Assert.assertEquals(toList("kitap"), analysis.getLemmas());

    analysis = analyzer.analyze("kitabımmış").get(0);
    Assert.assertEquals(toList("kitap", "kitabım"), analysis.getLemmas());

    analysis = analyzer.analyze("kitapçığa").get(0);
    Assert.assertEquals(toList("kitap", "kitapçık"), analysis.getLemmas());

    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okut").get(0);
    Assert.assertEquals(toList("oku", "okut"), analysis.getLemmas());
    analysis = analyzer.analyze("okuttur").get(0);
    Assert.assertEquals(toList("oku", "okut", "okuttur"), analysis.getLemmas());
    analysis = analyzer.analyze("okutturuluyor").get(0);
    Assert.assertEquals(toList("oku", "okut", "okuttur", "okutturul"), analysis.getLemmas());
    analysis = analyzer.analyze("okutturamıyor").get(0);
    Assert.assertEquals(toList("oku", "okut", "okuttur"), analysis.getLemmas());
    analysis = analyzer.analyze("okutturabiliyor").get(0);
    Assert.assertEquals(toList("oku", "okut", "okuttur", "okutturabil"), analysis.getLemmas());

  }

  @Test
  public void getLemmasAfterZeroMorphemeTest_Issue_175() {
    RuleBasedAnalyzer analyzer = getAnalyzer("gün");
    List<SingleAnalysis> analyses = analyzer.analyze("günlüğüm");
    boolean found = false;
    for (SingleAnalysis analysis : analyses) {
      if (analysis.formatLong().contains("Ness→Noun+A3sg|Zero→Verb")) {
        found = true;
        Assert.assertEquals(toList("gün", "günlük"), analysis.getLemmas());
      }
    }
    if (!found) {
      Assert.fail("Counld not found an analysis with `Ness→Noun+A3sg|Zero→Verb` in it");
    }
  }

  @Test
  public void regularWordAndEndingPredicatesTest() {
    RuleBasedAnalyzer analyzer = getAnalyzer(
        "kitap",
        "Ankara [P:Noun, Prop]",
        "vb [P:Noun, Abbrv]",
        "falan [A:Ext]");

    SingleAnalysis bareNoun = analyzer.analyze("kitap").get(0);
    Assert.assertTrue(bareNoun.isKnownLexical());
    Assert.assertTrue(bareNoun.isRegularWord());
    Assert.assertFalse(bareNoun.hasEnding());

    SingleAnalysis inflectedNoun = analyzer.analyze("kitaplarda").get(0);
    Assert.assertTrue(inflectedNoun.isRegularWord());
    Assert.assertTrue(inflectedNoun.hasEnding());

    SingleAnalysis proper = analyzer.analyze("ankara").get(0);
    Assert.assertTrue(proper.getDictionaryItem().isProperNoun());
    Assert.assertFalse(proper.isRegularWord());

    SingleAnalysis abbrv = analyzer.analyze("vb").get(0);
    Assert.assertTrue(abbrv.getDictionaryItem().isAbbreviation());
    Assert.assertFalse(abbrv.isRegularWord());

    SingleAnalysis ext = analyzer.analyze("falan").get(0);
    Assert.assertTrue(ext.getDictionaryItem().isExtended());
    Assert.assertTrue(ext.isRegularWord());
  }

  @Test
  public void endingSurfaceAndHashCodeTest() {
    RuleBasedAnalyzer analyzer = getAnalyzer("kitap");
    SingleAnalysis bare = analyzer.analyze("kitap").get(0);
    Assert.assertEquals("", bare.getEnding());
    Assert.assertEquals("kitap", bare.surfaceForm());
    Assert.assertFalse(bare.containsInformalMorpheme());

    SingleAnalysis oneSuffix = analyzer.analyze("kitaplar").get(0);
    Assert.assertEquals("lar", oneSuffix.getEnding());
    Assert.assertEquals("kitaplar", oneSuffix.surfaceForm());

    SingleAnalysis multiSuffix = analyzer.analyze("kitaplarda").get(0);
    Assert.assertEquals("larda", multiSuffix.getEnding());
    Assert.assertEquals("kitaplarda", multiSuffix.surfaceForm());
    Assert.assertEquals(multiSuffix.getMorphemeDataList().size(), multiSuffix.getMorphemes().size());

    SingleAnalysis copy = analyzer.analyze("kitaplarda").get(0);
    Assert.assertEquals(multiSuffix, copy);
    Assert.assertEquals(multiSuffix.hashCode(), copy.hashCode());
    Assert.assertNotEquals(oneSuffix, multiSuffix);
  }

}
