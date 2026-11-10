package zemberek.morphology.analysis;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;

public class NonTdkNounAttributesTest extends AnalyzerTestBase {

  private static TurkishMorphology defaultMorphology;

  @BeforeClass
  public static void setUpClass() {
    defaultMorphology = TurkishMorphology.createWithDefaults();
  }

  @Test
  public void camiiSanayiiBayiiCompoundP3sg() {
    WordAnalysis camiine = defaultMorphology.analyze("Camii'ne");
    Assert.assertTrue(
        camiine.getAnalysisResults().stream()
            .anyMatch(a -> formatLexicalSequence(a).endsWith("Noun + A3sg + P3sg + Dat")));
    Assert.assertFalse(hasProperNoun(camiine, "Camii"));

    WordAnalysis camiinde = defaultMorphology.analyze("Camii'nde");
    Assert.assertTrue(
        camiinde.getAnalysisResults().stream()
            .anyMatch(a -> formatLexicalSequence(a).endsWith("Noun + A3sg + P3sg + Loc")));

    WordAnalysis sanayiine = defaultMorphology.analyze("Sanayii'ne");
    Assert.assertTrue(
        sanayiine.getAnalysisResults().stream()
            .anyMatch(a -> formatLexicalSequence(a).endsWith("Noun + A3sg + P3sg + Dat")));

    WordAnalysis sanayiinde = defaultMorphology.analyze("Sanayii'nde");
    Assert.assertTrue(
        sanayiinde.getAnalysisResults().stream()
            .anyMatch(a -> formatLexicalSequence(a).endsWith("Noun + A3sg + P3sg + Loc")));

    WordAnalysis bayiine = defaultMorphology.analyze("Bayii'ne");
    Assert.assertTrue(
        bayiine.getAnalysisResults().stream()
            .anyMatch(a -> formatLexicalSequence(a).endsWith("Noun + A3sg + P3sg + Dat")));

    WordAnalysis bayiinde = defaultMorphology.analyze("Bayii'nde");
    Assert.assertTrue(
        bayiinde.getAnalysisResults().stream()
            .anyMatch(a -> formatLexicalSequence(a).endsWith("Noun + A3sg + P3sg + Loc")));
  }

  @Test
  public void familyMemberImplicitPluralsInDefaultLexicon() {
    List<SingleAnalysis> annemler =
        defaultMorphology.analyze("annemler").getAnalysisResults().stream()
            .filter(a -> a.getDictionaryItem().lemma.equals("annemler"))
            .collect(Collectors.toList());
    Assert.assertEquals(1, annemler.size());
    Assert.assertTrue(formatLexicalSequence(annemler.get(0)).endsWith("Noun + A3pl + P1sg"));

    List<SingleAnalysis> annenler =
        defaultMorphology.analyze("annenler").getAnalysisResults().stream()
            .filter(a -> a.getDictionaryItem().lemma.equals("annenler"))
            .collect(Collectors.toList());
    Assert.assertEquals(1, annenler.size());
    Assert.assertTrue(formatLexicalSequence(annenler.get(0)).endsWith("Noun + A3pl + P2sg"));

    Assert.assertTrue(
        hasMatch(defaultMorphology.analyze("teyzemlere"), "teyzemler", "Noun + A3pl + P1sg + Dat"));
    Assert.assertEquals(0, defaultMorphology.analyze("annemlerim").analysisCount());
    Assert.assertEquals(0, defaultMorphology.analyze("annenlerim").analysisCount());
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
