package zemberek.morphology.analysis;

import java.util.HashSet;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;

public class DuplicateAnalysisTest extends AnalyzerTestBase {

  private static TurkishMorphology defaultMorphology;

  @BeforeClass
  public static void setUpClass() {
    defaultMorphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .disableCache()
        .build();
  }

  // Issue #81: No duplicate SingleAnalysis results for dual-voicing / multi-index entries ("yoksa")
  @Test
  public void testNoDuplicateAnalyses_Issue81() {
    String[] testWords = {
        "yoksa", "yok", "yoktan", "yoku", "yoğu",
        "gasp", "gasptan", "stok", "stoktan",
        "kayıt", "kayıttan", "Selçuklu", "sır", "koyun", "hal"
    };
    for (var word : testWords) {
      var analysis = defaultMorphology.analyze(word);
      Assert.assertTrue("Expected non-empty analysis for " + word, analysis.analysisCount() > 0);
      var formatted = analysis.stream()
          .map(SingleAnalysis::formatLong)
          .collect(Collectors.toList());
      var unique = new HashSet<>(formatted);
      Assert.assertEquals(
          "Duplicate analyses found for '" + word + "': " + formatted,
          unique.size(),
          formatted.size());
    }

    // Verify both optional voicing forms ("yoku" and "yoğu") still analyze cleanly.
    Assert.assertTrue(
        defaultMorphology.analyze("yoku").stream()
            .anyMatch(a -> a.formatLong().equals("[yok:Adj] yok:Adj|Zero→Noun+A3sg+u:Acc")));
    Assert.assertTrue(
        defaultMorphology.analyze("yoğu").stream()
            .anyMatch(a -> a.formatLong().equals("[yok:Adj] yoğ:Adj|Zero→Noun+A3sg+u:Acc")));

    // Verify debug mode also reflects deduplicated results and keeps the lower-index DictionaryItem.
    var debugAnalyzer = getAnalyzer(
        "yok [P:Adj; A:NoVoicing; Index:2]",
        "yok [P:Adj; A:Voicing; Index:1]");
    var debugResults = debugAnalyzer.analyze("yoksa");
    var debugFormatted = debugResults.stream()
        .map(SingleAnalysis::formatLong)
        .collect(Collectors.toList());
    Assert.assertEquals(new HashSet<>(debugFormatted).size(), debugFormatted.size());
    Assert.assertEquals(debugResults.size(), debugAnalyzer.getDebugData().results.size());
    Assert.assertTrue(
        debugResults.stream().allMatch(a -> a.getDictionaryItem().index == 1));
  }
}
