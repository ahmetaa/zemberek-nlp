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
    weights.put("2:güzel+Adj-gün+Noun+A3sg+Pnon+Nom", 3.14f);
    weights.put("3:bir+Det-gün+Noun+A3sg+Pnon+Nom", 1.82f);
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
  public void testFullSentenceScoreParity() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    FastPerceptronAmbiguityResolver.FastFeatureExtractor extractor =
        new FastPerceptronAmbiguityResolver.FastFeatureExtractor(false);

    Weights weights = new Weights();
    weights.put("10:bugün", 1.2f);
    weights.put("10:okul", 0.8f);
    weights.put("10:gitmek", 2.1f);
    weights.put("2:bugün+Noun+A3sg+Pnon+Nom-gitmek+Verb+Past+A1sg", 3.5f);
    weights.put("3:okul+Noun+A3sg+Pnon+Dat-gitmek+Verb+Past+A1sg", 1.9f);
    weights.put("P:ENDSVERB", 2.0f);

    FastPerceptronAmbiguityResolver.FastDecoder decoder =
        new FastPerceptronAmbiguityResolver.FastDecoder(weights, extractor);

    java.util.List<WordAnalysis> waList = morphology.analyzeSentence("Bugün okula gittim.");
    FastPerceptronAmbiguityResolver.FastDecodeResult result = decoder.bestPath(waList);

    IntValueMap<String> counts = extractor.extractFeatureCounts(result.bestParse);
    float dotProduct = 0;
    for (IntValueMap.Entry<String> entry : counts.iterableEntries()) {
      dotProduct += weights.get(entry.key) * entry.count;
    }

    Assert.assertEquals("Full sentence decoder score must equal feature dot product",
        dotProduct, result.score, 1e-4f);
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

  @Test
  public void testPrecomputedTrigramMatrixEquivalence() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    FastPerceptronAmbiguityResolver.FastFeatureExtractor extractor =
        new FastPerceptronAmbiguityResolver.FastFeatureExtractor(false);

    Weights weights = new Weights();
    // Unigrams
    weights.put("10:küçük", 1.0f);
    weights.put("10:çocuk", 1.5f);
    weights.put("10:ev", 2.0f);
    weights.put("10:koşmak", 2.5f);
    // Feature 15 (Trigram morpheme transition features)
    weights.put("15:Noun+A3sg-Noun+A3sg-Noun+A3sg", 3.2f);
    weights.put("15:Adj-Noun+A3sg-Verb+Past", 4.1f);
    weights.put("15:Noun+A3sg-Verb+Past-</s>", 1.8f);
    // Bigrams
    weights.put("3:küçük+Adj-çocuk+Noun+A3sg+Pnon+Nom", 2.1f);
    weights.put("17:Noun+A3sg-Verb+Past", 1.4f);
    weights.put("P:ENDSVERB", 2.0f);

    FastPerceptronAmbiguityResolver.FastDecoder decoder =
        new FastPerceptronAmbiguityResolver.FastDecoder(weights, extractor);

    String[] testSentences = {
        "Küçük çocuk eve koştu.",
        "Bugün hava çok güzel.",
        "O adam yeni bir kitap aldı."
    };

    for (String s : testSentences) {
      java.util.List<WordAnalysis> waList = morphology.analyzeSentence(s);
      FastPerceptronAmbiguityResolver.FastDecodeResult result = decoder.bestPath(waList);

      IntValueMap<String> counts = extractor.extractFeatureCounts(result.bestParse);
      float dotProduct = 0;
      for (IntValueMap.Entry<String> entry : counts.iterableEntries()) {
        dotProduct += weights.get(entry.key) * entry.count;
      }

      Assert.assertEquals(
          "Sentence '" + s + "' decoder score must exactly equal feature dot product",
          dotProduct, result.score, 1e-4f);
    }
  }

  @Test
  public void testTrigramFeature15DirectDisambiguationImpact() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    FastPerceptronAmbiguityResolver.FastFeatureExtractor extractor =
        new FastPerceptronAmbiguityResolver.FastFeatureExtractor(false);

    java.util.List<WordAnalysis> waList = morphology.analyzeSentence("Çocuk eve gitti.");
    SingleAnalysis a1 = waList.get(0).getAnalysisResults().get(0);
    SingleAnalysis a2 = waList.get(1).getAnalysisResults().get(0);
    SingleAnalysis a3 = waList.get(2).getAnalysisResults().get(0);

    SingleAnalysis[] trigram = {a1, a2, a3};
    IntValueMap<String> sampleFeatures = extractor.extractFromTrigram(trigram);
    String targetF15 = null;
    for (String key : sampleFeatures) {
      if (key.startsWith("15:")) {
        targetF15 = key;
        break;
      }
    }
    Assert.assertNotNull("Trigram should generate at least one Feature 15", targetF15);

    // Setup weights where this Feature 15 has high positive weight
    Weights weights = new Weights();
    weights.put(targetF15, 12.5f);
    weights.put("P:ENDSVERB", 2.0f);

    FastPerceptronAmbiguityResolver.FastDecoder decoder =
        new FastPerceptronAmbiguityResolver.FastDecoder(weights, extractor);

    FastPerceptronAmbiguityResolver.FastDecodeResult result = decoder.bestPath(waList);

    IntValueMap<String> counts = extractor.extractFeatureCounts(result.bestParse);
    float dotProduct = 0;
    for (IntValueMap.Entry<String> entry : counts.iterableEntries()) {
      dotProduct += weights.get(entry.key) * entry.count;
    }

    Assert.assertEquals("Decoder score must equal dot product with Feature 15 active",
        dotProduct, result.score, 1e-4f);
    Assert.assertTrue("Should trigger target Feature 15 in winning sequence",
        counts.contains(targetF15));
  }

  @Test
  public void testFastDecoderConstructorsAndValidation() {
    Weights weights = new Weights();
    // 1. Single argument constructor
    FastPerceptronAmbiguityResolver.FastDecoder decoder =
        new FastPerceptronAmbiguityResolver.FastDecoder(weights);
    Assert.assertNotNull(decoder);
    Assert.assertEquals(FastPerceptronAmbiguityResolver.DecodeMode.VITERBI, decoder.getDecodeMode());

    // 2. Null model validation
    try {
      new FastPerceptronAmbiguityResolver.FastDecoder(null);
      Assert.fail("Expected NullPointerException when model is null");
    } catch (NullPointerException expected) {
      Assert.assertTrue(expected.getMessage().contains("model cannot be null"));
    }
  }

  @Test
  public void testDisambiguateNullAndEmptySafety() {
    Weights weights = new Weights();
    FastPerceptronAmbiguityResolver resolver = new FastPerceptronAmbiguityResolver(weights);

    SentenceAnalysis emptyResult = resolver.disambiguate("boş", java.util.Collections.emptyList());
    Assert.assertEquals(0, emptyResult.size());

    SentenceAnalysis nullResult = resolver.disambiguate("boş", null);
    Assert.assertEquals(0, nullResult.size());
  }
}
