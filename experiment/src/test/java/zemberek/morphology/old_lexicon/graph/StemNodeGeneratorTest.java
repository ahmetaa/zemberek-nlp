package zemberek.morphology.old_lexicon.graph;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.old_lexicon.tr.StemNodeGenerator;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.old_lexicon.tr.TurkishSuffixes;
import zemberek.morphology.old_lexicon.graph.StemNode;

public class StemNodeGeneratorTest {

  static TurkishSuffixes suffixes = new TurkishSuffixes();

  @Test
  public void empty() {
    StemNodeGenerator generator = new StemNodeGenerator(suffixes);
    DictionaryItem kitap = getDictionaryItem("kitap");
    StemNode[] nodes = generator.generate(kitap);
    Assert.assertEquals(2, nodes.length);
    DictionaryItem odun = getDictionaryItem("odun");
    StemNode[] odunNodes = generator.generate(odun);
    Assert.assertEquals(1, odunNodes.length);
  }

  private DictionaryItem getDictionaryItem(String line) {
    TurkishDictionaryLoader loader = new TurkishDictionaryLoader();
    return loader.loadFromString(line);
  }

}
