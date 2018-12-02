package zemberek.morphology.lexicon.tr;

import static zemberek.core.turkish.PrimaryPos.Noun;
import static zemberek.core.turkish.PrimaryPos.Punctuation;
import static zemberek.core.turkish.PrimaryPos.Verb;
import static zemberek.core.turkish.RootAttribute.Doubling;
import static zemberek.core.turkish.RootAttribute.InverseHarmony;
import static zemberek.core.turkish.RootAttribute.LastVowelDrop;
import static zemberek.core.turkish.RootAttribute.NoVoicing;
import static zemberek.core.turkish.RootAttribute.Voicing;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.io.Strings;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

public class TurkishDictionaryLoaderTest {

  private static ItemAttrPair testPair(String s, RootAttribute... attrs) {
    return new ItemAttrPair(s, EnumSet.copyOf(Arrays.asList(attrs)));
  }

  @Test
  public void loadNounsFromFileTest() throws IOException {
    RootLexicon items = TurkishDictionaryLoader
        .load(new File(Resources.getResource("test-lexicon-nouns.txt").getFile()));

    Assert.assertFalse(items.isEmpty());
    for (DictionaryItem item : items) {
      Assert.assertSame(item.primaryPos, Noun);
    }
  }

  @Test
  public void nounInferenceTest() {
    DictionaryItem item = getItem("elma");
    Assert.assertEquals("elma", item.lemma);
    Assert.assertEquals("elma", item.root);
    Assert.assertEquals(Noun, item.primaryPos);

    item = getItem("elma [P:Noun]");
    Assert.assertEquals("elma", item.lemma);
    Assert.assertEquals("elma", item.root);
    Assert.assertEquals(Noun, item.primaryPos);
  }

  private DictionaryItem getItem(String itemStr) {
    return TurkishDictionaryLoader.loadFromString(itemStr);
  }

  private DictionaryItem getLastItem(String... itemStr) {
    String last = Strings.subStringUntilFirst(itemStr[itemStr.length - 1], " ");
    return TurkishDictionaryLoader.load(itemStr).getMatchingItems(last).get(0);
  }

  @Test
  public void verbInferenceTest() {
    DictionaryItem item = getItem("gelmek");
    Assert.assertEquals("gel", item.root);
    Assert.assertEquals("gelmek", item.lemma);
    Assert.assertEquals(Verb, item.primaryPos);

    String[] verbs = {"germek", "yarmak", "salmak", "yermek [P:Verb]", "etmek [P:Verb; A:Voicing]",
        "etmek [A:Voicing]",
        "yıkanmak [A:Reflexive]", "küfretmek [A:Voicing, Aorist_A]"};
    for (String verb : verbs) {
      item = getItem(verb);
      Assert.assertEquals(Verb, item.primaryPos);
    }
  }

  @Test
  public void compoundTest() {
    DictionaryItem item = getLastItem("kuyruk", "atkuyruğu [A:CompoundP3sg; Roots:at-kuyruk]");
    Assert.assertEquals("atkuyruğu", item.lemma);
    Assert.assertEquals(Noun, item.primaryPos);
  }

  @Test
  public void voicingInferenceTest() {
    DictionaryItem item = TurkishDictionaryLoader.loadFromString("aort [A:NoVoicing]");
    Assert.assertEquals("aort", item.root);
    Assert.assertEquals(Noun, item.primaryPos);
    Assert.assertTrue(item.hasAttribute(RootAttribute.NoVoicing));
    Assert.assertFalse(item.hasAttribute(RootAttribute.Voicing));

    item = TurkishDictionaryLoader.loadFromString("at");
    Assert.assertEquals("at", item.root);
    Assert.assertEquals(Noun, item.primaryPos);
    Assert.assertTrue(item.hasAttribute(RootAttribute.NoVoicing));
    Assert.assertFalse(item.hasAttribute(RootAttribute.Voicing));
  }

  @Test
  public void punctuationTest() {
    DictionaryItem item = TurkishDictionaryLoader.loadFromString("… [P:Punc]");
    Assert.assertEquals("…", item.root);
    Assert.assertEquals(Punctuation, item.primaryPos);
  }

  @Test
  public void properNounsShouldNotHaveVoicingAutomaticallyTest() {
    DictionaryItem item = TurkishDictionaryLoader.loadFromString("Tokat");
    Assert.assertEquals("tokat", item.root);
    Assert.assertEquals(Noun, item.primaryPos);
    Assert.assertEquals(SecondaryPos.ProperNoun, item.secondaryPos);
    Assert.assertFalse(item.hasAttribute(RootAttribute.Voicing));

    item = TurkishDictionaryLoader.loadFromString("Dink");
    Assert.assertEquals("dink", item.root);
    Assert.assertEquals(Noun, item.primaryPos);
    Assert.assertEquals(SecondaryPos.ProperNoun, item.secondaryPos);
    Assert.assertFalse(item.hasAttribute(RootAttribute.Voicing));
  }

  @Test
  public void nounVoicingTest() {
    String[] voicing = {"kabak", "kabak [A:Voicing]", "psikolog", "havuç", "turp [A:Voicing]",
        "galip", "nohut", "cenk", "kükürt"};
    for (String s : voicing) {
      DictionaryItem item = TurkishDictionaryLoader.loadFromString(s);
      Assert.assertEquals(Noun, item.primaryPos);
      Assert.assertTrue("error in:" + s, item.hasAttribute(RootAttribute.Voicing));
    }

    String[] novoicing = {"kek", "link [A:NoVoicing]", "top", "kulp", "takat [A:NoVoicing]"};
    for (String s : novoicing) {
      DictionaryItem item = TurkishDictionaryLoader.loadFromString(s);
      Assert.assertEquals(Noun, item.primaryPos);
      Assert.assertTrue("error in:" + s, item.hasAttribute(NoVoicing));
    }
  }

  @Test
  public void referenceTest1() {
    String[] ref = {"ad", "ad [A:Doubling,InverseHarmony]", "soy",
        "soyadı [A:CompoundP3sg; Roots:soy-ad]"};
    RootLexicon lexicon = TurkishDictionaryLoader.load(ref);
    DictionaryItem item = lexicon.getItemById("soyadı_Noun");
    Assert.assertNotNull(item);
    Assert.assertFalse(item.attributes.contains(RootAttribute.Doubling));
  }

  @Test
  public void referenceTest2() {
    String[] ref = {"ad", "ad [A:Doubling,InverseHarmony;Index:1]", "soy",
        "soyadı [A:CompoundP3sg; Roots:soy-ad]"};
    RootLexicon lexicon = TurkishDictionaryLoader.load(ref);
    DictionaryItem item = lexicon.getItemById("soyadı_Noun");
    Assert.assertNotNull(item);
    Assert.assertFalse(item.attributes.contains(RootAttribute.Doubling));
  }

  @Test
  public void pronunciation1() {
    String[] ref = {
        "VST [P:Noun, Abbrv; Pr:viesti]",
        "VST [P:Noun, Abbrv; Pr:vesete; Ref:VST_Noun_Abbrv; Index:2]"};
    RootLexicon lexicon = TurkishDictionaryLoader.load(ref);
    DictionaryItem item = lexicon.getItemById("VST_Noun_Abbrv");
    Assert.assertNotNull(item);
    DictionaryItem item2 = lexicon.getItemById("VST_Noun_Abbrv_2");
    Assert.assertNotNull(item2);
    Assert.assertEquals(item, item2.getReferenceItem());
  }

  @Test
  public void implicitP3sgTest() {
    String[] lines = {
        "üzeri [A:CompoundP3sg;Roots:üzer]"};
    RootLexicon lexicon = TurkishDictionaryLoader.load(lines);
    Assert.assertEquals(2, lexicon.size());
  }

  @Test
  public void nounAttributesTest() {

    List<ItemAttrPair> testList = Lists.newArrayList(
        testPair("takat [A:NoVoicing, InverseHarmony]", NoVoicing, InverseHarmony),
        testPair("nakit [A: LastVowelDrop]", Voicing, LastVowelDrop),
        testPair("ret [A:Voicing, Doubling]", Voicing, Doubling)
    );
    for (ItemAttrPair pair : testList) {
      DictionaryItem item = TurkishDictionaryLoader.loadFromString(pair.str);
      Assert.assertEquals(Noun, item.primaryPos);
      Assert.assertEquals("error in:" + pair.str, pair.attrs, item.attributes);
    }
  }

  @Test
  @Ignore("Not a unit Test. Converts word histogram to word list")
  public void prepareWordListFromHistogram() throws IOException {
    List<String> hist = SimpleTextReader
        .trimmingUTF8Reader(new File("test/data/all-turkish-noproper.txt.tr")).asStringList();
    List<String> all = Lists.newArrayList();
    for (String s : hist) {
      all.add(Strings.subStringUntilFirst(s, " ").trim());
    }
    SimpleTextWriter.oneShotUTF8Writer(new File(Resources.getResource("z2-vocab.tr").getFile()))
        .writeLines(all);
  }

  @Test
  @Ignore("Not a unit test")
  public void shouldPrintItemsInDevlDictionary() throws IOException {
    RootLexicon items = TurkishDictionaryLoader
        .load(new File(Resources.getResource("dev-lexicon.txt").getFile()));
    for (DictionaryItem item : items) {
      System.out.println(item);
    }
  }

  @Test
  @Ignore("Not a unit test")
  public void saveFullAttributes() throws IOException {
    RootLexicon items = TurkishDictionaryLoader.loadDefaultDictionaries();
    PrintWriter p = new PrintWriter(new File("dictionary-all-attributes.txt"), "utf-8");
    for (DictionaryItem item : items) {
      p.println(item.toString());
    }
  }

  private static class ItemAttrPair {

    String str;
    EnumSet<RootAttribute> attrs;

    private ItemAttrPair(String str, EnumSet<RootAttribute> attrs) {
      this.str = str;
      this.attrs = attrs;
    }
  }


}
