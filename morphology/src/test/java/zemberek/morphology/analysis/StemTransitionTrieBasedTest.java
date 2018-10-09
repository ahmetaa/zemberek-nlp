package zemberek.morphology.analysis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.TestUtil;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class StemTransitionTrieBasedTest {

  @Test
  public void testPrefix() {
    RootLexicon lexicon = getLexicon();
    StemTransitionsTrieBased t = new StemTransitionsTrieBased(
        lexicon,
        new TurkishMorphotactics(lexicon));

    List<StemTransition> matches = t.getPrefixMatches("kabağa", false);
    Assert.assertEquals(3, matches.size());
    Set<String> lemmas = matches.stream().map(s -> s.item.lemma).collect(Collectors.toSet());
    Assert.assertTrue(TestUtil.containsAll(lemmas, "kaba", "kabağ", "kabak"));

    matches = t.getPrefixMatches("kabak", false);
    Assert.assertEquals(2, matches.size());
    lemmas = matches.stream().map(s -> s.item.lemma).collect(Collectors.toSet());
    Assert.assertTrue(TestUtil.containsAll(lemmas, "kaba", "kabak"));

    matches = t.getPrefixMatches("kapak", false);
    Assert.assertEquals(3, matches.size());
    lemmas = matches.stream().map(s -> s.item.lemma).collect(Collectors.toSet());
    Assert.assertTrue(TestUtil.containsAll(lemmas, "kapak"));
  }

  @Test
  public void testItem() {
    RootLexicon lexicon = getLexicon();
    StemTransitionsTrieBased t = new StemTransitionsTrieBased(
        lexicon,
        new TurkishMorphotactics(lexicon));

    DictionaryItem item = lexicon.getItemById("kapak_Noun");
    List<StemTransition> transitions = t.getTransitions(item);
    Assert.assertEquals(2, transitions.size());
    Set<String> surfaces = transitions.stream().map(s -> s.surface).collect(Collectors.toSet());
    Assert.assertTrue(TestUtil.containsAll(surfaces, "kapak", "kapağ"));
  }

  private RootLexicon getLexicon() {
    return TurkishDictionaryLoader.load(
        "kapak",
        "kapak [P:Adj]",
        "kapak [A:InverseHarmony]",
        "kabak",
        "kapaklı",
        "kabağ", // <-- only for testing.
        "kaba",
        "aba",
        "aba [P:Adj]"
    );
  }

}
