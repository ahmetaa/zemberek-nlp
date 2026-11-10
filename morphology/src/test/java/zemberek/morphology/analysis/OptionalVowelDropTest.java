package zemberek.morphology.analysis;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;

public class OptionalVowelDropTest extends AnalyzerTestBase {

  private static TurkishMorphology defaultMorphology;

  @BeforeClass
  public static void setUpClass() {
    defaultMorphology = TurkishMorphology.createWithDefaults();
  }

  @Test
  public void optionalVowelDropWithoutConsonantDuplication() {
    // Both dropped and non-dropped vowel-initial forms of "omuz" are accepted.
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("omzum"), "omuz", "Noun + A3sg + P1sg"));
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("omuzum"), "omuz", "Noun + A3sg + P1sg"));
    Assert.assertTrue(
        hasMatch(defaultMorphology.analyze("omzuna"), "omuz", "Noun + A3sg + P3sg + Dat"));
    Assert.assertTrue(
        hasMatch(defaultMorphology.analyze("omuzuna"), "omuz", "Noun + A3sg + P3sg + Dat"));

    // Bare and consonant-initial forms must not be duplicated by the secondary non-dropped entry.
    Assert.assertEquals(1, countLemma(defaultMorphology.analyze("omuz"), "omuz"));
    Assert.assertEquals(1, countLemma(defaultMorphology.analyze("omuzda"), "omuz"));
    Assert.assertEquals(
        1,
        defaultMorphology.analyze("omuzlar").getAnalysisResults().stream()
            .filter(
                a ->
                    a.getDictionaryItem().lemma.equals("omuz")
                        && formatLexicalSequence(a).endsWith("Noun + A3pl"))
            .count());

    // Existing optional vowel-drop entries ("zehir", "avuç") also avoid consonant/bare duplication.
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("zehri"), "zehir", "Noun + A3sg + P3sg"));
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("zehiri"), "zehir", "Noun + A3sg + P3sg"));
    Assert.assertEquals(1, countLemma(defaultMorphology.analyze("zehirli"), "zehir"));
    Assert.assertEquals(1, countLemma(defaultMorphology.analyze("zehir"), "zehir"));

    Assert.assertTrue(hasMatch(defaultMorphology.analyze("avcu"), "avuç", "Noun + A3sg + P3sg"));
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("avucu"), "avuç", "Noun + A3sg + P3sg"));
    Assert.assertEquals(1, countLemma(defaultMorphology.analyze("avuç"), "avuç"));
    Assert.assertEquals(1, countLemma(defaultMorphology.analyze("avuçta"), "avuç"));
  }

  private boolean hasMatch(WordAnalysis analysis, String lemma, String lexicalTail) {
    return analysis.getAnalysisResults().stream()
        .anyMatch(
            a ->
                a.getDictionaryItem().lemma.equals(lemma)
                    && formatLexicalSequence(a).endsWith(lexicalTail));
  }

  private long countLemma(WordAnalysis analysis, String lemma) {
    return analysis.getAnalysisResults().stream()
        .filter(a -> a.getDictionaryItem().lemma.equals(lemma))
        .count();
  }
}
