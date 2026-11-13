package zemberek.normalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import zemberek.core.collections.IntIntMap;
import zemberek.core.collections.UIntMap;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextIO;
import zemberek.normalization.NoisyWordsLexiconGenerator.ContextualSimilarityGraph;
import zemberek.normalization.NoisyWordsLexiconGenerator.NormalizationVocabulary;
import zemberek.normalization.NoisyWordsLexiconGenerator.RandomWalkNode;
import zemberek.normalization.NoisyWordsLexiconGenerator.RandomWalker;
import zemberek.normalization.NoisyWordsLexiconGenerator.WalkResult;
import zemberek.normalization.NoisyWordsLexiconGenerator.WalkScore;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;

@FixMethodOrder(MethodSorters.JVM)
public class RandomWalkFunctionalTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  BlockTextLoader corpora;

  @Before
  public void setUp() throws Exception {
    Path tmp = Files.createTempFile("foo", "bar");
    List<String> lines = TextIO.loadLinesFromResource("normalization/mini-noisy-corpus.txt", "#");
    Files.write(tmp, lines);
    corpora = BlockTextLoader.fromPaths(Collections.singletonList(tmp));
  }

  @After
  public void tearDown() throws Exception {
    if (corpora != null) {
      for (Path path : corpora.getCorpusPaths()) {
        Files.delete(path);
      }
    }
  }

  TurkishMorphology getDefaultMorphology() {
    return TurkishMorphology
        .builder()
        .setLexicon(RootLexicon.getDefault())
        .disableCache()
        .build();
  }

  TurkishMorphology getInformalMorphology() throws IOException {
    RootLexicon lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");
    return TurkishMorphology
        .builder()
        .setLexicon(lexicon)
        .disableUnidentifiedTokenAnalyzer()
        .useInformalAnalysis()
        .disableCache()
        .build();
  }


  @Test
  @Ignore(value = "Not working. Work in progress.")
  public void VocabularyGenerationTest() throws Exception {

/*
    NormalizationVocabularyGenerator vocabularyGenerator =
        new NormalizationVocabularyGenerator(getDefaultMorphology());

    Vocabulary v = vocabularyGenerator.collectVocabularyHistogram(corpora, 1);
    Assert.assertEquals(5, v.incorrect.size());
    Assert.assertTrue(v.incorrect.contains("acıba"));
    Assert.assertTrue(v.incorrect.contains("ağşam"));

    NoisyWordsLexiconGenerator lexiconGenerator = new NoisyWordsLexiconGenerator();
    NormalizationVocabulary vocabulary = new NormalizationVocabulary(v, 1, 1);
    Assert.assertTrue(vocabulary.isCorrect("akşam"));
    Assert.assertTrue(vocabulary.isCorrect("kişi"));

    Assert.assertFalse(vocabulary.isCorrect("ağşam"));

    ContextualSimilarityGraph graph = lexiconGenerator.buildGraph(corpora, vocabulary, 1, 1);

    // check if contexts are correct.
    int c1Hash = NoisyWordsLexiconGenerator.hash("bu", "eve");
    IntIntMap m1 = graph.contextHashToWordCounts.get(c1Hash);
    Assert.assertNotNull(m1);
    Assert.assertEquals(2, m1.size());
    int idCorrect = vocabulary.getIndex("akşam");
    int idIncorrect = vocabulary.getIndex("ağşam");
    Assert.assertEquals(2, m1.get(idCorrect));
    Assert.assertEquals(1, m1.get(idIncorrect));

    Path tmp = Files.createTempFile("rnd", "foo");

    graph.serializeForRandomWalk(tmp);

    RandomWalker randomWalker = RandomWalker.fromGraphFile(vocabulary, tmp);
    Assert.assertEquals(
        randomWalker.contextHashesToWords.size(),
        graph.contextHashToWordCounts.size());

    Assert.assertEquals(
        randomWalker.wordsToContextHashes.size(),
        graph.wordToContexts.size());

    WalkResult result = randomWalker.walk(10, 3, 1);
    Assert.assertTrue(result.allCandidates.get("ağşam").contains("akşam"));
*/

  }

  private NormalizationVocabulary createMiniVocabulary(
      List<String> correctEntries,
      List<String> incorrectEntries) throws IOException {
    Path correct = tempFolder.newFile("correct.txt").toPath();
    Path incorrect = tempFolder.newFile("incorrect.txt").toPath();
    Files.write(correct, correctEntries, StandardCharsets.UTF_8);
    Files.write(incorrect, incorrectEntries, StandardCharsets.UTF_8);
    return new NormalizationVocabulary(correct, incorrect, null, 1, 1, 1);
  }

  @Test
  public void contextCountsAccumulatePastThirdOccurrenceTest() throws Exception {
    NormalizationVocabulary vocab = createMiniVocabulary(
        Arrays.asList("akşam 10", "sabah 10"),
        Collections.singletonList("ağşam 5"));

    Path corpusFile = tempFolder.newFile("corpus.txt").toPath();
    Files.write(
        corpusFile,
        Arrays.asList(
            "bu akşam eve",
            "bu akşam eve",
            "bu akşam eve",
            "bu sabah eve",
            "bu ağşam eve",
            "bu ağşam eve"),
        StandardCharsets.UTF_8);

    BlockTextLoader loader = BlockTextLoader.fromPaths(Collections.singletonList(corpusFile));
    NoisyWordsLexiconGenerator generator = new NoisyWordsLexiconGenerator(vocab, 1);
    ContextualSimilarityGraph graph = generator.buildGraph(loader, 1);

    int ctxHash = NoisyWordsLexiconGenerator.hash("bu", "eve");
    IntIntMap counts = graph.contextHashToWordCounts.get(ctxHash);
    Assert.assertNotNull(counts);
    Assert.assertEquals(3, counts.get(vocab.getIndex("akşam")));
    Assert.assertEquals(1, counts.get(vocab.getIndex("sabah")));
    Assert.assertEquals(2, counts.get(vocab.getIndex("ağşam")));
  }

  @Test
  public void singleCandidateWalkProducesFiniteUnitSimilarityTest() throws Exception {
    NormalizationVocabulary vocab = createMiniVocabulary(
        Collections.singletonList("akşam 10"),
        Collections.singletonList("ağşam 5"));

    int idAksam = vocab.getIndex("akşam");
    int idAgsam = vocab.getIndex("ağşam");
    int ctxHash = 1001;

    UIntMap<RandomWalkNode> ctxToWord = new UIntMap<>();
    ctxToWord.put(ctxHash, new RandomWalkNode(new int[]{idAksam, 1}, 1));
    UIntMap<RandomWalkNode> wordToCtx = new UIntMap<>();
    wordToCtx.put(idAgsam, new RandomWalkNode(new int[]{ctxHash, 1}, 1));

    RandomWalker walker = new RandomWalker(vocab, ctxToWord, wordToCtx);
    WalkResult res = walker.walk(20, 4, 1);
    List<WalkScore> scores = new java.util.ArrayList<>(res.allCandidates.get("ağşam"));
    Assert.assertEquals(1, scores.size());
    Assert.assertFalse(Float.isNaN(scores.get(0).contextualSimilarity));
    Assert.assertFalse(Float.isInfinite(scores.get(0).contextualSimilarity));
    Assert.assertEquals(1.0f, scores.get(0).contextualSimilarity, 1e-4f);
  }

  @Test
  public void inverseHittingTimeFavorsCloserCandidateTest() throws Exception {
    NormalizationVocabulary vocab = createMiniVocabulary(
        Arrays.asList("yakın 10", "uzak 10"),
        Arrays.asList("yakin 5", "kopru 5"));

    int idYakinCorrect = vocab.getIndex("yakın");
    int idUzakCorrect = vocab.getIndex("uzak");
    int idYakinNoisy = vocab.getIndex("yakin");
    int idKopruNoisy = vocab.getIndex("kopru");

    int ctxDirect = 101;
    int ctxFar = 102;

    // Hub 1 (ctxDirect) reaches "yakın" directly in 2 hops with probability 1/2, or reaches
    // bridge noisy node "kopru" with probability 1/2, which only connects to "uzak" (4 hops).
    UIntMap<RandomWalkNode> ctxToWord = new UIntMap<>();
    ctxToWord.put(ctxDirect, new RandomWalkNode(new int[]{idYakinCorrect, 1, idKopruNoisy, 1}, 2));
    ctxToWord.put(ctxFar, new RandomWalkNode(new int[]{idUzakCorrect, 1}, 1));

    UIntMap<RandomWalkNode> wordToCtx = new UIntMap<>();
    wordToCtx.put(idYakinNoisy, new RandomWalkNode(new int[]{ctxDirect, 1}, 1));
    wordToCtx.put(idKopruNoisy, new RandomWalkNode(new int[]{ctxFar, 1}, 1));

    RandomWalker walker = new RandomWalker(vocab, ctxToWord, wordToCtx);
    WalkResult res = walker.walk(500, 6, 1);
    Map<String, WalkScore> byCandidate = res.allCandidates.get("yakin").stream()
        .collect(Collectors.toMap(s -> s.candidate, s -> s));

    Assert.assertTrue(byCandidate.containsKey("yakın"));
    Assert.assertTrue(byCandidate.containsKey("uzak"));
    Assert.assertTrue(
        byCandidate.get("yakın").contextualSimilarity > byCandidate.get("uzak").contextualSimilarity);
    Assert.assertEquals(
        1.0f,
        byCandidate.get("yakın").contextualSimilarity + byCandidate.get("uzak").contextualSimilarity,
        1e-4f);
  }

  @Test
  public void selectFromDistributionIsUnbiasedTest() throws Exception {
    NormalizationVocabulary vocab = createMiniVocabulary(
        Collections.singletonList("ev 1"),
        Collections.singletonList("ew 1"));
    RandomWalker walker = new RandomWalker(vocab, new UIntMap<>(), new UIntMap<>());
    RandomWalkNode node = new RandomWalkNode(new int[]{10, 1, 20, 1}, 2);

    int firstCount = 0;
    int draws = 10_000;
    for (int i = 0; i < draws; i++) {
      if (walker.selectFromDistribution(node) == 10) {
        firstCount++;
      }
    }
    double ratio = firstCount * 1.0 / draws;
    Assert.assertTrue("Expected ~0.5 ratio but got " + ratio, ratio > 0.47 && ratio < 0.53);
  }
}
