package zemberek.morphology.ambiguity.fast;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.Weights;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class FastPerceptronResolverTest {

  @Test
  public void testFeatureExtractorAndCache() {
    FastPerceptronAmbiguityResolver.FastFeatureExtractor extractor =
        new FastPerceptronAmbiguityResolver.FastFeatureExtractor(true);

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    SingleAnalysis sa1 = morphology.analyze("Türkiye").getAnalysisResults().get(0);
    SingleAnalysis sa2 = morphology.analyze("büyük").getAnalysisResults().get(0);
    SingleAnalysis sa3 = morphology.analyze("millet").getAnalysisResults().get(0);

    SingleAnalysis[] trigram = {sa1, sa2, sa3};
    IntValueMap<String> features = extractor.extractFromTrigram(trigram);

    Assert.assertTrue(features.contains("10:millet"));

    // Cache hit verification
    IntValueMap<String> cachedFeatures = extractor.extractFromTrigram(trigram);
    Assert.assertEquals(features.size(), cachedFeatures.size());
  }

  @Test
  public void testSurfaceRuleFeatures() {
    FastPerceptronAmbiguityResolver.FastFeatureExtractor extractor =
        new FastPerceptronAmbiguityResolver.FastFeatureExtractor(false);

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    SingleAnalysis saVerb = morphology.analyze("gitti").getAnalysisResults().get(0);
    SingleAnalysis saNoun = morphology.analyze("ali").getAnalysisResults().get(0);

    // Sentence-final verb
    SingleAnalysis[] endTrigram = {
        saNoun,
        saVerb,
        FastPerceptronAmbiguityResolver.sentenceEnd
    };
    IntValueMap<String> endFeatures = extractor.extractFromTrigram(endTrigram);
    Assert.assertTrue("Should contain P:ENDSVERB for sentence-final verb",
        endFeatures.contains("P:ENDSVERB"));
  }

  @Test
  public void testMorphologyIntegration() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    Weights weights = new Weights();
    // Prioritize noun interpretation of 'ev'
    weights.put("10:ev", 1.0f);

    FastPerceptronAmbiguityResolver resolver = new FastPerceptronAmbiguityResolver(weights);

    TurkishMorphology customMorphology = TurkishMorphology.builder()
        .setLexicon(morphology.getLexicon())
        .setAmbiguityResolver(resolver)
        .build();

    SentenceAnalysis result = customMorphology.analyzeAndDisambiguate("Büyük ev.");
    Assert.assertEquals(3, result.size());
    Assert.assertEquals("ev", result.bestAnalysis().get(1).getDictionaryItem().lemma);
  }

  @Test
  public void testFactoredScoringEquivalence() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    FastPerceptronAmbiguityResolver.FastFeatureExtractor extractor =
        new FastPerceptronAmbiguityResolver.FastFeatureExtractor(false);

    Weights weights = new Weights();
    weights.put("10:güzel", 1.5f);
    weights.put("10:bir", -0.5f);
    weights.put("10:gün", 2.0f);
    weights.put("4:gün+Noun+A3sg+Pnon+Nom", 0.7f);
    weights.put("P:ENDSVERB", 2.25f);
    weights.put("P:LOWER_PROPER", -9.61f);

    FastPerceptronAmbiguityResolver.FastDecoder decoder =
        new FastPerceptronAmbiguityResolver.FastDecoder(weights, extractor);

    SingleAnalysis sa1 = morphology.analyze("güzel").getAnalysisResults().get(0);
    SingleAnalysis sa2 = morphology.analyze("bir").getAnalysisResults().get(0);
    SingleAnalysis sa3 = morphology.analyze("gün").getAnalysisResults().get(0);

    // 1. Unfactored score via extractFromTrigram
    SingleAnalysis[] trigram = {sa1, sa2, sa3};
    IntValueMap<String> features = extractor.extractFromTrigram(trigram);
    float expectedScore = 0;
    for (String k : features) {
      expectedScore += weights.get(k) * features.get(k);
    }

    // 2. Factored score
    FastPerceptronAmbiguityResolver.CandidateContext ctx1 =
        new FastPerceptronAmbiguityResolver.CandidateContext(sa1, weights, false, 0);
    FastPerceptronAmbiguityResolver.CandidateContext ctx2 =
        new FastPerceptronAmbiguityResolver.CandidateContext(sa2, weights, false, 0);
    FastPerceptronAmbiguityResolver.CandidateContext ctx3 =
        new FastPerceptronAmbiguityResolver.CandidateContext(sa3, weights, false, 0);

    float factoredScore = ctx3.uniScore
        + decoder.computeBigramScore(ctx2, ctx3)
        + decoder.computeTrigramScore(ctx1, ctx2, ctx3);

    Assert.assertEquals("Factored score must exactly match unfactored score",
        expectedScore, factoredScore, 1e-5f);

    // 3. Hypothesis-based score with precomputed prefixes
    FastPerceptronAmbiguityResolver.FastHypothesis h =
        new FastPerceptronAmbiguityResolver.FastHypothesis(ctx1, ctx2, null, 0f);
    float hypothesisTrigramScore = decoder.computeTrigramScore(h, ctx3);
    Assert.assertEquals("Hypothesis trigram score must match direct trigram score",
        decoder.computeTrigramScore(ctx1, ctx2, ctx3), hypothesisTrigramScore, 1e-5f);
  }

  @Test
  public void testBeamSearchAndGreedyModes() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    Weights weights = new Weights();
    weights.put("10:ev", 1.0f);

    FastPerceptronAmbiguityResolver resolver = new FastPerceptronAmbiguityResolver(weights);

    TurkishMorphology customMorphology = TurkishMorphology.builder()
        .setLexicon(morphology.getLexicon())
        .setAmbiguityResolver(resolver)
        .build();

    // 1. Default (Exact Viterbi)
    Assert.assertEquals(FastPerceptronAmbiguityResolver.DEFAULT_MODE, resolver.getDecodeMode());
    Assert.assertEquals(FastPerceptronAmbiguityResolver.DecodeMode.VITERBI, resolver.getDecodeMode());
    Assert.assertTrue(resolver.isExactViterbi());
    Assert.assertEquals(-1, resolver.getBeamSize());
    Assert.assertFalse(resolver.isGreedy());
    SentenceAnalysis defaultResult = customMorphology.analyzeAndDisambiguate("Büyük beyaz ev.");
    Assert.assertEquals(4, defaultResult.size());
    Assert.assertEquals("ev", defaultResult.bestAnalysis().get(2).getDictionaryItem().lemma);

    // 2. Beam search (custom beam = 4)
    resolver.setBeamSize(4);
    Assert.assertEquals(FastPerceptronAmbiguityResolver.DecodeMode.BEAM, resolver.getDecodeMode());
    Assert.assertEquals(4, resolver.getBeamSize());
    Assert.assertFalse(resolver.isExactViterbi());
    Assert.assertFalse(resolver.isGreedy());
    SentenceAnalysis beamResult = customMorphology.analyzeAndDisambiguate("Büyük beyaz ev.");
    Assert.assertEquals(4, beamResult.size());
    Assert.assertEquals("ev", beamResult.bestAnalysis().get(2).getDictionaryItem().lemma);

    // 3. Switch back to Exact Viterbi mode explicitly
    resolver.setExactViterbi();
    Assert.assertEquals(-1, resolver.getBeamSize());
    Assert.assertEquals(FastPerceptronAmbiguityResolver.DecodeMode.VITERBI, resolver.getDecodeMode());
    Assert.assertTrue(resolver.isExactViterbi());
    Assert.assertFalse(resolver.isGreedy());
    SentenceAnalysis exactResult = customMorphology.analyzeAndDisambiguate("Büyük beyaz ev.");
    Assert.assertEquals(4, exactResult.size());
    Assert.assertEquals("ev", exactResult.bestAnalysis().get(2).getDictionaryItem().lemma);

    // 4. Greedy mode
    resolver.setGreedy(true);
    Assert.assertEquals(FastPerceptronAmbiguityResolver.DecodeMode.GREEDY, resolver.getDecodeMode());
    Assert.assertTrue(resolver.isGreedy());
    Assert.assertFalse(resolver.isExactViterbi());
    SentenceAnalysis greedyResult = customMorphology.analyzeAndDisambiguate("Büyük beyaz ev.");
    Assert.assertEquals(4, greedyResult.size());
    Assert.assertEquals("ev", greedyResult.bestAnalysis().get(2).getDictionaryItem().lemma);

    // 5. DecodeMode enum switching directly
    resolver.setDecodeMode(FastPerceptronAmbiguityResolver.DecodeMode.BEAM);
    Assert.assertEquals(FastPerceptronAmbiguityResolver.DecodeMode.BEAM, resolver.getDecodeMode());
    Assert.assertEquals(FastPerceptronAmbiguityResolver.DEFAULT_BEAM_SIZE, resolver.getBeamSize());
    resolver.setDecodeMode(FastPerceptronAmbiguityResolver.DecodeMode.VITERBI);
    Assert.assertTrue(resolver.isExactViterbi());
    Assert.assertEquals(-1, resolver.getBeamSize());
  }
}
