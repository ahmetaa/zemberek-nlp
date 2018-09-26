package zemberek.normalization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import zemberek.core.collections.IntIntMap;
import zemberek.core.text.MultiPathBlockTextLoader;
import zemberek.core.text.TextIO;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.normalization.NoisyWordsLexiconGenerator.ContextualSimilarityGraph;
import zemberek.normalization.NoisyWordsLexiconGenerator.NormalizationVocabulary;
import zemberek.normalization.NoisyWordsLexiconGenerator.RandomWalker;
import zemberek.normalization.NoisyWordsLexiconGenerator.WalkResult;
import zemberek.normalization.NormalizationVocabularyGenerator.Vocabulary;

@FixMethodOrder(MethodSorters.JVM)
public class RandomWalkFunctionalTest {

  MultiPathBlockTextLoader corpora;

  @Before
  public void setUp() throws Exception {
    Path tmp = Files.createTempFile("foo", "bar");
    List<String> lines = TextIO.loadLinesFromResource("normalization/mini-noisy-corpus.txt", "#");
    Files.write(tmp, lines);
    corpora = MultiPathBlockTextLoader.fromPaths(Collections.singletonList(tmp));
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
        .addDefaultBinaryDictionary()
        .disableCache()
        .build();
  }

  TurkishMorphology getInformalMorphology() throws IOException {
    RootLexicon lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");
    return TurkishMorphology
        .builder()
        .useLexicon(lexicon)
        .disableUnidentifiedTokenAnalyzer()
        .morphotactics(new InformalTurkishMorphotactics(lexicon))
        .disableCache()
        .build();
  }


  @Test
  @Ignore(value = "Not working. Work in progress.")
  public void VocabularyGenerationTest() throws Exception {

    NormalizationVocabularyGenerator vocabularyGenerator =
        new NormalizationVocabularyGenerator(getDefaultMorphology());

    Vocabulary v = vocabularyGenerator.collectVocabularyHistogram(corpora, 1);
    Assert.assertEquals(5, v.incorrect.size());
    Assert.assertTrue(v.incorrect.contains("acıba"));
    Assert.assertTrue(v.incorrect.contains("ağşam"));

    NoisyWordsLexiconGenerator lexiconGenerator = new NoisyWordsLexiconGenerator();
    NormalizationVocabulary vocabulary = new NormalizationVocabulary(v, 1, 1);
    Assert.assertTrue(vocabulary.isValid("akşam"));
    Assert.assertTrue(vocabulary.isValid("kişi"));

    Assert.assertFalse(vocabulary.isValid("ağşam"));

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

  }
}
