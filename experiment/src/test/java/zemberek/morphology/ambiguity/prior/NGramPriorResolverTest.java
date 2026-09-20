package zemberek.morphology.ambiguity.prior;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zemberek.core.collections.IntValueMap;
import zemberek.core.data.Weights;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class NGramPriorResolverTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testLogBinning() {
    Assert.assertEquals(0, NGramPriorStore.getLogBin(0));
    Assert.assertEquals(1, NGramPriorStore.getLogBin(1));
    Assert.assertEquals(1, NGramPriorStore.getLogBin(2));
    Assert.assertEquals(2, NGramPriorStore.getLogBin(3));
    Assert.assertEquals(2, NGramPriorStore.getLogBin(4));
    Assert.assertEquals(3, NGramPriorStore.getLogBin(7));
    Assert.assertEquals(6, NGramPriorStore.getLogBin(100));
    Assert.assertEquals(9, NGramPriorStore.getLogBin(1000));
    Assert.assertEquals(10, NGramPriorStore.getLogBin(100000));
  }

  @Test
  public void testPriorStoreLoading() throws IOException {
    Path biFile = temp.newFile("bigrams.txt").toPath();
    Path triFile = temp.newFile("trigrams.txt").toPath();
    Path colFile = temp.newFile("collocations.txt").toPath();

    Files.write(biFile, List.of(
        "söz konusu : 3302",
        "merkez bankası : 1071",
        "nadir bir : 1" // below threshold
    ));

    Files.write(triFile, List.of(
        "cumhuriyet halk partisi : 723",
        "kültür ve turizm : 268"
    ));

    Files.write(colFile, List.of(
        "büyük bir : 2951",
        "bir şey : 3666"
    ));

    NGramPriorStore store = NGramPriorStore.builder()
        .bigramsPath(biFile)
        .trigramsPath(triFile)
        .collocationsPath(colFile)
        .minCount(2)
        .build();

    Assert.assertEquals(2, store.bigramSize());
    Assert.assertEquals(2, store.trigramSize());
    Assert.assertEquals(2, store.collocationSize());

    Assert.assertEquals(3302, store.getBigramCount("söz", "konusu"));
    Assert.assertEquals(1071, store.getBigramCount("Merkez", "Bankası")); // case-insensitive
    Assert.assertEquals(0, store.getBigramCount("nadir", "bir")); // filtered by minCount

    Assert.assertTrue(store.getBigramBin("söz", "konusu") >= 10);
    Assert.assertTrue(store.hasCollocation("büyük", "bir"));
    Assert.assertFalse(store.hasCollocation("rastgele", "kelime"));
  }

  @Test
  public void testFeatureExtractorWithPriors() {
    IntValueMap<String> bigrams = new IntValueMap<>();
    bigrams.put("büyük ev", 50);

    NGramPriorStore store = new NGramPriorStore(bigrams, null, null);
    NGramPriorPerceptronResolver.PriorFeatureExtractor extractor =
        new NGramPriorPerceptronResolver.PriorFeatureExtractor(false, store);

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    WordAnalysis w1 = morphology.analyze("büyük");
    WordAnalysis w2 = morphology.analyze("ev");

    SingleAnalysis sa1 = w1.getAnalysisResults().get(0);
    SingleAnalysis sa2 = w2.getAnalysisResults().get(0);

    SingleAnalysis[] trigram = {
        NGramPriorPerceptronResolver.sentenceBegin,
        sa1,
        sa2
    };

    IntValueMap<String> features = extractor.extractFromTrigram(trigram);
    Assert.assertTrue("Should contain bigram bin feature", features.contains("P:BI_BIN:5"));
  }

  @Test
  public void testMorphologyIntegration() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    Weights weights = new Weights();
    // Prioritize noun interpretation of 'ev'
    weights.put("10:ev", 1.0f);

    NGramPriorPerceptronResolver resolver = new NGramPriorPerceptronResolver(
        weights,
        new NGramPriorPerceptronResolver.PriorFeatureExtractor(false, null),
        new NGramPriorStore()
    );

    TurkishMorphology customMorphology = TurkishMorphology.builder()
        .setLexicon(morphology.getLexicon())
        .setAmbiguityResolver(resolver)
        .build();

    SentenceAnalysis result = customMorphology.analyzeAndDisambiguate("Büyük ev.");
    Assert.assertEquals(3, result.size());
    Assert.assertEquals("ev", result.bestAnalysis().get(1).getDictionaryItem().lemma);
  }

  @Test
  public void testTrigramPosFeatureExtraction() {
    IntValueMap<String> trigrams = new IntValueMap<>();
    trigrams.put("türkiye büyük millet", 500);

    NGramPriorStore store = new NGramPriorStore(null, trigrams, null);
    NGramPriorPerceptronResolver.PriorFeatureExtractor extractor =
        new NGramPriorPerceptronResolver.PriorFeatureExtractor(true, store);

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    SingleAnalysis sa1 = morphology.analyze("Türkiye").getAnalysisResults().get(0);
    SingleAnalysis sa2 = morphology.analyze("büyük").getAnalysisResults().get(0);
    SingleAnalysis sa3 = morphology.analyze("millet").getAnalysisResults().get(0);

    SingleAnalysis[] trigram = {sa1, sa2, sa3};
    IntValueMap<String> features = extractor.extractFromTrigram(trigram);

    Assert.assertTrue(features.contains("P:TRI_BIN:8"));
    String expectedPosFeat = "P:TRI_POS:8_" + NGramPriorPerceptronResolver.WordData.fromAnalysis(sa3).lastGroup();
    Assert.assertTrue(features.contains(expectedPosFeat));

    // Cache hit verification
    IntValueMap<String> cachedFeatures = extractor.extractFromTrigram(trigram);
    Assert.assertEquals(features.size(), cachedFeatures.size());
  }

  @Test
  public void testFactoredScoringEquivalence() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    IntValueMap<String> bigrams = new IntValueMap<>();
    bigrams.put("güzel bir", 100);
    IntValueMap<String> trigrams = new IntValueMap<>();
    trigrams.put("güzel bir gün", 50);

    NGramPriorStore store = new NGramPriorStore(bigrams, trigrams, null);
    NGramPriorPerceptronResolver.PriorFeatureExtractor extractor =
        new NGramPriorPerceptronResolver.PriorFeatureExtractor(false, store);

    Weights weights = new Weights();
    weights.put("10:güzel", 1.5f);
    weights.put("10:bir", -0.5f);
    weights.put("10:gün", 2.0f);
    weights.put("P:BI_BIN:6", 0.8f);
    weights.put("P:TRI_BIN:5", 1.2f);
    weights.put("4:gün+Noun+A3sg+Pnon+Nom", 0.7f);

    NGramPriorPerceptronResolver.PriorDecoder decoder =
        new NGramPriorPerceptronResolver.PriorDecoder(weights, extractor);

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
    NGramPriorPerceptronResolver.CandidateContext ctx1 =
        new NGramPriorPerceptronResolver.CandidateContext(sa1, weights, false, 0);
    NGramPriorPerceptronResolver.CandidateContext ctx2 =
        new NGramPriorPerceptronResolver.CandidateContext(sa2, weights, false, 0);
    NGramPriorPerceptronResolver.CandidateContext ctx3 =
        new NGramPriorPerceptronResolver.CandidateContext(sa3, weights, false, 0);

    float factoredScore = ctx3.uniScore
        + decoder.computeBigramScore(ctx2, ctx3)
        + decoder.computeTrigramScore(ctx1, ctx2, ctx3);

    Assert.assertEquals("Factored score must exactly match unfactored score",
        expectedScore, factoredScore, 1e-5f);
  }

  @Test
  public void testBeamSearchAndGreedyModes() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    Weights weights = new Weights();
    weights.put("10:ev", 1.0f);

    NGramPriorPerceptronResolver resolver = new NGramPriorPerceptronResolver(
        weights,
        new NGramPriorPerceptronResolver.PriorFeatureExtractor(false, null),
        new NGramPriorStore()
    );

    TurkishMorphology customMorphology = TurkishMorphology.builder()
        .setLexicon(morphology.getLexicon())
        .setAmbiguityResolver(resolver)
        .build();

    // 1. Default (Beam-8)
    Assert.assertEquals(NGramPriorPerceptronResolver.DEFAULT_BEAM_SIZE, resolver.getBeamSize());
    Assert.assertEquals(8, resolver.getBeamSize());
    Assert.assertFalse(resolver.isGreedy());
    SentenceAnalysis defaultResult = customMorphology.analyzeAndDisambiguate("Büyük beyaz ev.");
    Assert.assertEquals(4, defaultResult.size());
    Assert.assertEquals("ev", defaultResult.bestAnalysis().get(2).getDictionaryItem().lemma);

    // 2. Exact Viterbi mode
    resolver.setExactViterbi();
    Assert.assertEquals(-1, resolver.getBeamSize());
    Assert.assertFalse(resolver.isGreedy());
    SentenceAnalysis exactResult = customMorphology.analyzeAndDisambiguate("Büyük beyaz ev.");
    Assert.assertEquals(4, exactResult.size());
    Assert.assertEquals("ev", exactResult.bestAnalysis().get(2).getDictionaryItem().lemma);

    // 3. Beam search (custom beam = 4)
    resolver.setBeamSize(4);
    Assert.assertEquals(4, resolver.getBeamSize());
    SentenceAnalysis beamResult = customMorphology.analyzeAndDisambiguate("Büyük beyaz ev.");
    Assert.assertEquals(4, beamResult.size());
    Assert.assertEquals("ev", beamResult.bestAnalysis().get(2).getDictionaryItem().lemma);

    // 4. Greedy mode
    resolver.setGreedy(true);
    Assert.assertTrue(resolver.isGreedy());
    SentenceAnalysis greedyResult = customMorphology.analyzeAndDisambiguate("Büyük beyaz ev.");
    Assert.assertEquals(4, greedyResult.size());
    Assert.assertEquals("ev", greedyResult.bestAnalysis().get(2).getDictionaryItem().lemma);
  }
}
