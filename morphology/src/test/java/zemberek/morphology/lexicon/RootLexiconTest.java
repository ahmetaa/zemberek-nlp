package zemberek.morphology.lexicon;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.PrimaryPos;

public class RootLexiconTest {

  @Test
  public void singleAndMultiSenseLemmaLookupAndRemoval() {
    RootLexicon lexicon =
        RootLexicon.fromLines(
            "elma",
            "yüz [P:Noun]",
            "yüz [P:Num]",
            "yüzmek",
            "yüz [P:Adj; Index:1]");

    Assert.assertEquals(5, lexicon.size());
    Assert.assertFalse(lexicon.isEmpty());

    List<DictionaryItem> elma = lexicon.getMatchingItems("elma");
    Assert.assertEquals(1, elma.size());
    Assert.assertTrue(lexicon.containsItem(elma.get(0)));
    Assert.assertSame(elma.get(0), lexicon.getItemById("elma_Noun"));

    List<DictionaryItem> yuzAll = lexicon.getMatchingItems("yüz");
    Assert.assertEquals(3, yuzAll.size());
    // Callers such as TurkishDictionaryLoader sort the returned list in place
    yuzAll.sort((a, b) -> Integer.compare(b.index, a.index));
    Assert.assertEquals(1, yuzAll.get(0).index);
    Assert.assertEquals(1, lexicon.getMatchingItems("yüz", PrimaryPos.Numeral).size());
    Assert.assertEquals(1, lexicon.getMatchingItems("yüz", PrimaryPos.Adjective).size());
    Assert.assertTrue(lexicon.getMatchingItems("yüz", PrimaryPos.Adverb).isEmpty());
    Assert.assertTrue(lexicon.getMatchingItems("olmayan").isEmpty());

    // Duplicate addition should be ignored
    lexicon.add(elma.get(0));
    Assert.assertEquals(5, lexicon.size());

    // Removing one sense of multi-sense lemma preserves remaining senses
    DictionaryItem yuzNum = lexicon.getItemById("yüz_Num");
    lexicon.remove(yuzNum);
    Assert.assertFalse(lexicon.containsItem(yuzNum));
    Assert.assertEquals(2, lexicon.getMatchingItems("yüz").size());
    Assert.assertEquals(4, lexicon.size());

    // Removing second sense transitions back to single-item storage cleanly
    DictionaryItem yuzAdj = lexicon.getItemById("yüz_Adj_1");
    lexicon.remove(yuzAdj);
    Assert.assertEquals(1, lexicon.getMatchingItems("yüz").size());
    Assert.assertEquals(PrimaryPos.Noun, lexicon.getMatchingItems("yüz").get(0).primaryPos);

    // removeAllLemmas clears remaining sense
    lexicon.removeAllLemmas("yüz");
    Assert.assertTrue(lexicon.getMatchingItems("yüz").isEmpty());
    Assert.assertEquals(2, lexicon.size());
  }

  @Test
  public void preservesInsertionOrder() {
    RootLexicon ordered = RootLexicon.fromLines("zebra", "armut", "kalem");
    List<String> lemmas = new ArrayList<>();
    for (DictionaryItem item : ordered) {
      lemmas.add(item.lemma);
    }
    Assert.assertEquals(List.of("zebra", "armut", "kalem"), lemmas);
  }
}
