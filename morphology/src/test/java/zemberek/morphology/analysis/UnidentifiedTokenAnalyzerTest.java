package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;

public class UnidentifiedTokenAnalyzerTest {

  @Test
  public void classifyNumeralValidFormats() {
    // Cardinal
    Assert.assertEquals(List.of(SecondaryPos.Cardinal), UnidentifiedTokenAnalyzer.classifyNumeral("0"));
    Assert.assertEquals(List.of(SecondaryPos.Cardinal), UnidentifiedTokenAnalyzer.classifyNumeral("42"));
    Assert.assertEquals(List.of(SecondaryPos.Cardinal), UnidentifiedTokenAnalyzer.classifyNumeral("+10"));
    Assert.assertEquals(List.of(SecondaryPos.Cardinal), UnidentifiedTokenAnalyzer.classifyNumeral("-7"));

    // Ordinal (must not also emit Distribution)
    Assert.assertEquals(List.of(SecondaryPos.Ordinal), UnidentifiedTokenAnalyzer.classifyNumeral("1."));
    Assert.assertEquals(List.of(SecondaryPos.Ordinal), UnidentifiedTokenAnalyzer.classifyNumeral("5."));
    Assert.assertEquals(List.of(SecondaryPos.Ordinal), UnidentifiedTokenAnalyzer.classifyNumeral("-3."));

    // Range & Ratio
    Assert.assertEquals(List.of(SecondaryPos.Range), UnidentifiedTokenAnalyzer.classifyNumeral("1-5"));
    Assert.assertEquals(List.of(SecondaryPos.Range), UnidentifiedTokenAnalyzer.classifyNumeral("-5-10"));
    Assert.assertEquals(List.of(SecondaryPos.Ratio), UnidentifiedTokenAnalyzer.classifyNumeral("1/2"));
    Assert.assertEquals(List.of(SecondaryPos.Ratio), UnidentifiedTokenAnalyzer.classifyNumeral("-3/4"));

    // Real (unambiguous)
    Assert.assertEquals(List.of(SecondaryPos.Real), UnidentifiedTokenAnalyzer.classifyNumeral("1.5"));
    Assert.assertEquals(List.of(SecondaryPos.Real), UnidentifiedTokenAnalyzer.classifyNumeral("12,5"));
    Assert.assertEquals(List.of(SecondaryPos.Real), UnidentifiedTokenAnalyzer.classifyNumeral("-3.14"));
    Assert.assertEquals(List.of(SecondaryPos.Real), UnidentifiedTokenAnalyzer.classifyNumeral("0.30"));
    Assert.assertEquals(List.of(SecondaryPos.Real), UnidentifiedTokenAnalyzer.classifyNumeral("24.00"));
    Assert.assertEquals(List.of(SecondaryPos.Real), UnidentifiedTokenAnalyzer.classifyNumeral("12.60"));

    // Real + Clock ambiguity with dot separator
    Assert.assertEquals(
        List.of(SecondaryPos.Real, SecondaryPos.Clock),
        UnidentifiedTokenAnalyzer.classifyNumeral("12.30"));
    Assert.assertEquals(
        List.of(SecondaryPos.Real, SecondaryPos.Clock),
        UnidentifiedTokenAnalyzer.classifyNumeral("9.30"));
    Assert.assertEquals(
        List.of(SecondaryPos.Real, SecondaryPos.Clock),
        UnidentifiedTokenAnalyzer.classifyNumeral("20.00"));
    Assert.assertEquals(
        List.of(SecondaryPos.Real, SecondaryPos.Clock),
        UnidentifiedTokenAnalyzer.classifyNumeral("00.00"));

    // Clock (colon separator, HH:MM and HH:MM:SS)
    Assert.assertEquals(List.of(SecondaryPos.Clock), UnidentifiedTokenAnalyzer.classifyNumeral("20:30"));
    Assert.assertEquals(List.of(SecondaryPos.Clock), UnidentifiedTokenAnalyzer.classifyNumeral("0:30"));
    Assert.assertEquals(List.of(SecondaryPos.Clock), UnidentifiedTokenAnalyzer.classifyNumeral("00:00"));
    Assert.assertEquals(List.of(SecondaryPos.Clock), UnidentifiedTokenAnalyzer.classifyNumeral("10:24:05"));

    // Date (4-digit and 2-digit year)
    Assert.assertEquals(List.of(SecondaryPos.Date), UnidentifiedTokenAnalyzer.classifyNumeral("1.1.2014"));
    Assert.assertEquals(List.of(SecondaryPos.Date), UnidentifiedTokenAnalyzer.classifyNumeral("31/12/2025"));
    Assert.assertEquals(List.of(SecondaryPos.Date), UnidentifiedTokenAnalyzer.classifyNumeral("12.05.20"));
    Assert.assertEquals(List.of(SecondaryPos.Date), UnidentifiedTokenAnalyzer.classifyNumeral("1/2/99"));

    // Percentage
    Assert.assertEquals(List.of(SecondaryPos.Percentage), UnidentifiedTokenAnalyzer.classifyNumeral("%2"));
    Assert.assertEquals(List.of(SecondaryPos.Percentage), UnidentifiedTokenAnalyzer.classifyNumeral("+%5"));
    Assert.assertEquals(List.of(SecondaryPos.Percentage), UnidentifiedTokenAnalyzer.classifyNumeral("-%12.5"));
    Assert.assertEquals(List.of(SecondaryPos.Percentage), UnidentifiedTokenAnalyzer.classifyNumeral("%2,2"));
  }

  @Test
  public void classifyNumeralRejectsInvalidStems() {
    String[] invalid = {
        "", "+", "-", "%", "%2.", "%2..3", "abc-%5",
        "S3", "3G", "5..", "1.2.", "1-2-3",
        "24:00", "29:59", "10:60", "-10:20", "10:24:60", "10:24:5",
        "0.1.2020", "32.01.2020", "12.13.2020", "12.05/2020", "-1.1.2014", "1.1.201"
    };
    for (String stem : invalid) {
      Assert.assertTrue("Expected empty for " + stem,
          UnidentifiedTokenAnalyzer.classifyNumeral(stem).isEmpty());
    }
  }

  @Test
  public void analyzeOrdinalClockAndShortYearDateEndToEnd() {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.fromLines(
            "beş [P:Num,Card]",
            "beşinci [P:Num,Ord]",
            "yirmi [P:Num,Card]",
            "sıfır [P:Num,Card]"))
        .disableCache()
        .build();

    // Ordinal "5." should only produce Ordinal (no spurious Distribution)
    WordAnalysis ord = morphology.analyze("5.");
    Assert.assertEquals(1, ord.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Ordinal,
        ord.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);

    // HH:MM:SS clock time
    WordAnalysis clockSec = morphology.analyze("10:24:05'te");
    Assert.assertEquals(1, clockSec.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Clock,
        clockSec.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);

    // 2-digit year date
    WordAnalysis shortDate = morphology.analyze("12.05.20'de");
    Assert.assertEquals(1, shortDate.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Date,
        shortDate.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);

    // Ambiguous dot-separated clock/real ("12.20'de" -> both Real and Clock via reused analysis)
    WordAnalysis dotTime = morphology.analyze("12.20'de");
    Assert.assertEquals(2, dotTime.analysisCount());
    Assert.assertEquals(
        List.of(SecondaryPos.Real, SecondaryPos.Clock),
        dotTime.getAnalysisResults().stream()
            .map(a -> a.getDictionaryItem().secondaryPos)
            .toList());
  }
}
