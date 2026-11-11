package zemberek.morphology.analysis;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;

public class ApostropheProperNounTest extends AnalyzerTestBase {

  private static TurkishMorphology defaultMorphology;

  @BeforeClass
  public static void setUpClass() {
    defaultMorphology = TurkishMorphology.createWithDefaults();
  }

  @Test
  public void capitalizedCommonNounWithApostropheGetsProperNoun() {
    Assert.assertTrue(hasProperNoun(defaultMorphology.analyze("Kaya'nın"), "Kaya"));
    Assert.assertTrue(hasProperNoun(defaultMorphology.analyze("Deniz'in"), "Deniz"));
    Assert.assertTrue(hasProperNoun(defaultMorphology.analyze("Savaş'a"), "Savaş"));
    Assert.assertTrue(hasProperNoun(defaultMorphology.analyze("Barış'ın"), "Barış"));
    Assert.assertTrue(hasProperNoun(defaultMorphology.analyze("Çiçek'ten"), "Çiçek"));

    // Common noun analyses remain present alongside ProperNoun.
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("Kaya'nın"), "kaya", "Noun + A3sg + Gen"));
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("Savaş'a"), "savaş", "Noun + A3sg + Dat"));

    // Lowercase words with apostrophes do not synthesize ProperNoun.
    Assert.assertFalse(hasProperNoun(defaultMorphology.analyze("kaya'nın"), "Kaya"));

    // Inflected P3sg compound heads do not synthesize bogus ProperNoun lemmas.
    Assert.assertFalse(hasProperNoun(defaultMorphology.analyze("Hastanesi'ne"), "Hastanesi"));
    Assert.assertFalse(hasProperNoun(defaultMorphology.analyze("Müdürlüğü'ne"), "Müdürlüğü"));

    // Known ProperNouns in proper.dict are not duplicated.
    List<SingleAnalysis> ankara =
        defaultMorphology.analyze("Ankara'ya").getAnalysisResults().stream()
            .filter(
                a ->
                    a.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun
                        && formatLexicalSequence(a).endsWith("Noun + A3sg + Dat"))
            .collect(Collectors.toList());
    Assert.assertEquals(1, ankara.size());
  }

  private boolean hasMatch(WordAnalysis analysis, String lemma, String lexicalTail) {
    return analysis.getAnalysisResults().stream()
        .anyMatch(
            a ->
                a.getDictionaryItem().lemma.equals(lemma)
                    && formatLexicalSequence(a).endsWith(lexicalTail));
  }

  private boolean hasProperNoun(WordAnalysis analysis, String lemma) {
    return analysis.getAnalysisResults().stream()
        .anyMatch(
            a ->
                a.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun
                    && a.getDictionaryItem().lemma.equals(lemma));
  }
}
