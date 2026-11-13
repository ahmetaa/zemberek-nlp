package zemberek.normalization;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.CompactStemEndingSpellChecker.ScoredCandidate;

public class CompactStemEndingSpellCheckerTest {

  @Test
  public void memoryFootprintAndBasicSuggestionsDefaultMorphologyTest() throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishSpellChecker exact = new TurkishSpellChecker(morphology);
    CompactStemEndingSpellChecker compact = exact.getCompactSpellChecker();
    Assert.assertNotNull(compact);
    Assert.assertTrue(
        "Expected compact CSR graph under 15 MB, got " + compact.estimatedGraphBytes(),
        compact.estimatedGraphBytes() < 15L * 1024L * 1024L);

    TurkishSpellChecker diacIgnoring = exact.withDiacriticsIgnoring();
    Assert.assertNotNull(diacIgnoring.getCompactSpellChecker());
    Assert.assertEquals(
        compact.totalNodeCount(), diacIgnoring.getCompactSpellChecker().totalNodeCount());

    Assert.assertTrue(exact.suggestForWord("okudm").contains("okudum"));
    Assert.assertTrue(diacIgnoring.suggestForWord("guldum").contains("güldüm"));
    Assert.assertTrue(diacIgnoring.suggestForWord("şarz").contains("şarj"));
  }

  @Test
  public void symmetricDiacriticsAndProperNounsTest() throws IOException {
    TurkishMorphology morphology =
        TurkishMorphology.builder()
            .disableCache()
            .setLexicon("kâr", "kar", "gül", "Ankara", "Türkiye")
            .build();
    TurkishSpellChecker checker = new TurkishSpellChecker(morphology, true);

    List<ScoredCandidate> overDiacritized =
        checker.getCompactSpellChecker().suggestScored("kârı");
    boolean foundKar =
        overDiacritized.stream().anyMatch(c -> c.surface.equals("kar") || c.surface.equals("kâr"));
    Assert.assertTrue(foundKar);

    List<String> proper = checker.suggestForWord("Ankarada");
    Assert.assertTrue(proper.contains("Ankara'da"));
  }

  @Test
  public void analysisPredicateFilteringTest() throws IOException {
    TurkishMorphology morphology =
        TurkishMorphology.builder()
            .disableCache()
            .setLexicon("Ankara", "ankara", "elma")
            .build();
    TurkishSpellChecker checker = new TurkishSpellChecker(morphology, false);
    Assert.assertTrue(checker.suggestForWord("elmx").contains("elma"));

    checker.setAnalysisPredicate(
        a -> a.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun);
    Assert.assertFalse(checker.suggestForWord("elmx").contains("elma"));
    Assert.assertTrue(checker.suggestForWord("Ankarx").contains("Ankara"));
  }

  @Test
  public void concurrentCacheAccessTest() throws Exception {
    TurkishMorphology morphology =
        TurkishMorphology.builder()
            .disableCache()
            .setLexicon("okumak", "yazmak", "gelmek", "görmek")
            .build();
    TurkishSpellChecker checker = new TurkishSpellChecker(morphology, true);

    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      Callable<Boolean> task =
          () -> {
            for (int i = 0; i < 50; i++) {
              if (!checker.suggestForWord("okumk").contains("okumak")) {
                return false;
              }
              if (!checker.suggestForWord("gormek").contains("görmek")) {
                return false;
              }
            }
            return true;
          };
      List<Future<Boolean>> futures =
          pool.invokeAll(java.util.Arrays.asList(task, task, task, task));
      for (Future<Boolean> f : futures) {
        Assert.assertTrue(f.get());
      }
    } finally {
      pool.shutdown();
    }
  }
}
