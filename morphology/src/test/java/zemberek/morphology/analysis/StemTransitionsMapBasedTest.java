package zemberek.morphology.analysis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.TestUtil;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class StemTransitionsMapBasedTest {

  @Test
  public void testPrefix() {
    RootLexicon lexicon = getLexicon();
    StemTransitionsMapBased t = new StemTransitionsMapBased(
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
    StemTransitionsMapBased t = new StemTransitionsMapBased(
        lexicon,
        new TurkishMorphotactics(lexicon));

    DictionaryItem item = lexicon.getItemById("kapak_Noun");
    List<StemTransition> transitions = t.getTransitions(item);
    Assert.assertEquals(2, transitions.size());
    Set<String> surfaces = transitions.stream().map(s -> s.surface).collect(Collectors.toSet());
    Assert.assertTrue(TestUtil.containsAll(surfaces, "kapak", "kapağ"));
  }

  @Test
  public void testAsciiTolerantAndRuntimeAddRemove() {
    RootLexicon lexicon = TurkishDictionaryLoader.load("sıra", "şıra", "kapak");
    StemTransitionsMapBased t = new StemTransitionsMapBased(
        lexicon,
        new TurkishMorphotactics(lexicon));

    // Trigger lazy asciiKeys initialization
    List<StemTransition> asciiMatches = t.getPrefixMatches("sira", true);
    Set<String> lemmas = asciiMatches.stream().map(s -> s.item.lemma).collect(Collectors.toSet());
    Assert.assertEquals(Set.of("sıra", "şıra"), lemmas);

    // Add a runtime item with Turkish characters AFTER asciiKeys was initialized
    DictionaryItem runtimeItem = new DictionaryItem(
        "Şırnak", "şırnak", "şırnak", PrimaryPos.Noun, SecondaryPos.ProperNoun);
    t.addDictionaryItem(runtimeItem);

    List<StemTransition> afterAdd = t.getPrefixMatches("sirnak", true);
    Assert.assertTrue(afterAdd.stream().anyMatch(s -> s.item.equals(runtimeItem)));

    // Add a colliding runtime item on existing single stem "kapak" and then remove it
    DictionaryItem colliding = new DictionaryItem(
        "Kapak", "kapak", "kapak", PrimaryPos.Noun, SecondaryPos.ProperNoun);
    t.addDictionaryItem(colliding);
    Assert.assertEquals(2, t.getPrefixMatches("kapak", false).size());
    t.removeDictionaryItem(colliding);
    Assert.assertEquals(1, t.getPrefixMatches("kapak", false).size());

    // Remove runtime Turkish item and verify asciiKeys stays in sync
    t.removeDictionaryItem(runtimeItem);
    List<StemTransition> afterRemove = t.getPrefixMatches("sirnak", true);
    Assert.assertTrue(afterRemove.stream().noneMatch(s -> s.item.equals(runtimeItem)));
  }

  @Test
  public void testLongInputAndEmptyInput() {
    RootLexicon lexicon = getLexicon();
    StemTransitionsMapBased t = new StemTransitionsMapBased(
        lexicon,
        new TurkishMorphotactics(lexicon));

    Assert.assertTrue(t.getPrefixMatches("", false).isEmpty());
    String longInput = "kabak" + "x".repeat(500);
    List<StemTransition> matches = t.getPrefixMatches(longInput, false);
    Assert.assertEquals(2, matches.size());
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
