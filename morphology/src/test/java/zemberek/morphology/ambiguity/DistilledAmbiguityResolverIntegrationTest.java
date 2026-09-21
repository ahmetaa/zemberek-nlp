package zemberek.morphology.ambiguity;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;

import java.util.List;

public class DistilledAmbiguityResolverIntegrationTest {

  @Test
  public void testDefaultResolverIsFastPerceptron() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    AmbiguityResolver resolver = morphology.getAmbiguityResolver();
    Assert.assertNotNull(resolver);
    Assert.assertTrue("Default resolver should be FastPerceptronAmbiguityResolver",
        resolver instanceof FastPerceptronAmbiguityResolver);
  }

  @Test
  public void testBuilderFlagEnablesLegacyResolver() {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .useLegacyAmbiguityResolver()
        .build();
    AmbiguityResolver resolver = morphology.getAmbiguityResolver();
    Assert.assertNotNull(resolver);
    Assert.assertTrue("Builder flag should instantiate PerceptronAmbiguityResolver",
        resolver instanceof PerceptronAmbiguityResolver);
  }

  @Test
  public void testStaticFactoryEnablesLegacyResolver() {
    TurkishMorphology morphology = TurkishMorphology.createWithLegacyAmbiguityResolver();
    AmbiguityResolver resolver = morphology.getAmbiguityResolver();
    Assert.assertNotNull(resolver);
    Assert.assertTrue("createWithLegacyAmbiguityResolver should instantiate PerceptronAmbiguityResolver",
        resolver instanceof PerceptronAmbiguityResolver);
  }

  @Test
  public void testSystemPropertyEnablesLegacyResolver() {
    String prop = "zemberek.morphology.useLegacyAmbiguityResolver";
    String prev = System.getProperty(prop);
    try {
      System.setProperty(prop, "true");
      TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
      AmbiguityResolver resolver = morphology.getAmbiguityResolver();
      Assert.assertNotNull(resolver);
      Assert.assertTrue("System property should switch default to PerceptronAmbiguityResolver",
          resolver instanceof PerceptronAmbiguityResolver);
    } finally {
      if (prev != null) {
        System.setProperty(prop, prev);
      } else {
        System.clearProperty(prop);
      }
    }
  }

  @Test
  public void testFastPerceptronDisambiguationAnalysis() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    String sentence = "Yarın sabah Ankara'ya gideceğim.";
    SentenceAnalysis result = morphology.analyzeAndDisambiguate(sentence);

    Assert.assertNotNull(result);
    List<SentenceWordAnalysis> analyses = result.getWordAnalyses();
    Assert.assertEquals(5, analyses.size()); // Yarın, sabah, Ankara'ya, gideceğim, .

    // Check first word 'Yarın' (Noun)
    SentenceWordAnalysis first = analyses.get(0);
    Assert.assertEquals("yarın", first.getBestAnalysis().getDictionaryItem().lemma);

    // Check last lexical word 'gideceğim' (Verb gitmek)
    SentenceWordAnalysis verb = analyses.get(3);
    Assert.assertEquals("gitmek", verb.getBestAnalysis().getDictionaryItem().lemma);
  }

  @Test
  public void testNounCompoundHeadDisambiguation() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    String sentence = "Sevimli karakterli peluş dolgulu pofuduk çocuk tacı";
    SentenceAnalysis result = morphology.analyzeAndDisambiguate(sentence);

    Assert.assertNotNull(result);
    List<SentenceWordAnalysis> analyses = result.getWordAnalyses();

    // Verify 'tacı' resolves to taç + P3sg (indefinite noun compound head), not Tac (Abbrv) + Acc
    SentenceWordAnalysis taciWord = analyses.get(analyses.size() - 1);
    Assert.assertEquals("tacı", taciWord.getWordAnalysis().getInput());
    Assert.assertEquals("taç", taciWord.getBestAnalysis().getDictionaryItem().lemma);
    Assert.assertTrue("Head of 'çocuk tacı' must carry 3rd person possessive (P3sg)",
        taciWord.getBestAnalysis().formatLong().contains("P3sg"));
  }
}
