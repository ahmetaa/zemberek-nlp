package zemberek.normalization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextIO;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;

@FixMethodOrder(MethodSorters.JVM)
public class RandomWalkFunctionalTest {

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
}
