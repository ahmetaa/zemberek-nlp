package zemberek.morphology.analysis;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;

public class CircumflexAnalysisTest extends AnalyzerTestBase {

  private static TurkishMorphology defaultMorphology;

  @BeforeClass
  public static void setUpClass() {
    defaultMorphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .disableCache()
        .build();
  }

  // Issue #111: "hâlâ" should not have analyses "hal+a:Dat" or "[hala:Noun]"
  @Test
  public void testCircumflexWordHala_Issue111() {
    WordAnalysis analysis = defaultMorphology.analyze("hâlâ");
    List<String> formatted = analysis.stream()
        .map(SingleAnalysis::formatLong)
        .collect(Collectors.toList());

    Assert.assertTrue(formatted.contains("[hâlâ:Adv] hala:Adv"));
    Assert.assertFalse(
        "Should not match non-circumflex noun hala: " + formatted,
        analysis.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("hala")));
    Assert.assertFalse(
        "Should not match dative suffix -a on hal/hâl: " + formatted,
        analysis.stream().anyMatch(
            a -> a.getDictionaryItem().lemma.equals("hal")
                || a.getDictionaryItem().lemma.equals("hâl")));

    // Non-circumflex input "hala" still allows both [hâlâ:Adv] and [hala:Noun] and [hal:Noun]+a.
    WordAnalysis plainHala = defaultMorphology.analyze("hala");
    Assert.assertTrue(
        plainHala.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("hâlâ")));
    Assert.assertTrue(
        plainHala.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("hala")));
    Assert.assertTrue(
        plainHala.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("hal")));
  }

  @Test
  public void testCircumflexInflectionApostropheAndAsciiTolerant_Issue111() {
    // "kâra" -> dative of "kâr", not "kar+a" or "kara"
    WordAnalysis kara = defaultMorphology.analyze("kâra");
    Assert.assertTrue(
        kara.stream().anyMatch(
            a -> a.getDictionaryItem().lemma.equals("kâr")
                && a.formatLong().equals("[kâr:Noun] kar:Noun+A3sg+a:Dat")));
    Assert.assertFalse(
        kara.stream().anyMatch(
            a -> a.getDictionaryItem().lemma.equals("kar")
                || a.getDictionaryItem().lemma.equals("kara")));

    // Suffix carrying circumflex ("karâ") must be rejected.
    Assert.assertEquals(0, defaultMorphology.analyze("karâ").analysisCount());

    // Apostrophe-inflected circumflex proper nouns ("Kâzım'ın") and OOV proper nouns ("Bâbıâli'de").
    WordAnalysis kazim = defaultMorphology.analyze("Kâzım'ın");
    Assert.assertTrue(
        kazim.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("Kâzım")));
    Assert.assertFalse(
        kazim.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("Kazım")));

    TurkishMorphology emptyMorphology = TurkishMorphology.builder().disableCache().build();
    WordAnalysis oovProper = emptyMorphology.analyze("Bâbıâli'de");
    Assert.assertEquals(1, oovProper.analysisCount());
    Assert.assertEquals("Bâbıâli", oovProper.getAnalysisResults().get(0).getDictionaryItem().lemma);
    Assert.assertEquals(0, emptyMorphology.analyze("Bâbıâli'dâ").analysisCount());

    // CompoundP3sg word with circumflex ("denizlâleleri")
    WordAnalysis compound = defaultMorphology.analyze("denizlâleleri");
    Assert.assertTrue(
        compound.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("denizlâlesi")));

    // Direct RuleBasedAnalyzer.analyze("hâlâ") also works.
    RuleBasedAnalyzer directAnalyzer = getAnalyzer(
        "hâlâ [P:Adv]", "hala", "hal [A:InverseHarmony]", "hâl [A:InverseHarmony]");
    List<SingleAnalysis> direct = directAnalyzer.analyze("hâlâ");
    Assert.assertEquals(1, direct.size());
    Assert.assertEquals("hâlâ", direct.get(0).getDictionaryItem().lemma);

    // When ignoreDiacriticsInAnalysis is enabled, circumflexes are ignored.
    TurkishMorphology asciiTolerant = TurkishMorphology.builder()
        .setLexicon("hâlâ [P:Adv]", "hala", "hal")
        .ignoreDiacriticsInAnalysis()
        .disableCache()
        .build();
    WordAnalysis tolerantResult = asciiTolerant.analyze("hâlâ");
    Assert.assertTrue(
        tolerantResult.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("hala")));
    Assert.assertTrue(
        tolerantResult.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("hal")));
  }
}
