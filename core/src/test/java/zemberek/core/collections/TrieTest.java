package zemberek.core.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import zemberek.core.turkish.TurkishAlphabet;

public class TrieTest {

  private static Random r = new Random(0xCAFEDEADBEEFL);
  private static TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
  private Trie<Item> lt;

  private static class Item {

    String surfaceForm;
    String payload;

    public Item(String surfaceForm, String payload) {
      this.surfaceForm = surfaceForm;
      this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Item item = (Item) o;
      return Objects.equal(surfaceForm, item.surfaceForm) &&
          Objects.equal(payload, item.payload);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(surfaceForm, payload);
    }

    @Override
    public String toString() {
      return surfaceForm;
    }
  }

  @Before
  public void setUp() {
    lt = new Trie();
  }

  private void additems(List<Item> items) {
    for (Item item : items) {
      lt.add(item.surfaceForm, item);
    }
  }

  private List<Item> createitems(String... stems) {
    List<Item> items = Lists.newArrayList();
    for (String s : stems) {
      Item Item = new Item(s, "surface form:" + s);
      items.add(Item);
    }
    return items;
  }

  private void checkitemsExist(List<Item> items) {
    for (Item item : items) {
      List<Item> stems = lt.getMatchingItems(item.surfaceForm);
      assertTrue(" Should have contained: " + item, stems.contains(item));
    }
  }

  private void checkitemsMatches(String prefix, List<Item> items) {
    List<Item> stems = lt.getMatchingItems(prefix);
    for (Item Item : items) {
      assertTrue("Should have contained: " + Item, stems.contains(Item));
    }
  }

  @Test
  public void empty() {
    List<Item> stems = lt.getMatchingItems("foo");
    assertEquals(stems.size(), 0);
  }

  @Test
  public void singleItem() {
    List<Item> items = createitems("elma");
    additems(items);
    checkitemsExist(items);
  }

  @Test
  public void distinctStems() {
    List<Item> items = createitems("elma", "armut");
    additems(items);
    checkitemsExist(items);
  }

  @Test
  public void stemsSharingSamePrefixOrder1() {
    List<Item> items = createitems("elmas", "elma");
    additems(items);
    checkitemsExist(items);
    checkitemsMatches("elma", createitems("elma"));
    checkitemsMatches("elmas", createitems("elma", "elmas"));
  }

  @Test
  public void stemsSharingSamePrefixOrder2() {
    List<Item> items = createitems("elma", "elmas");
    additems(items);
    checkitemsExist(items);
    checkitemsMatches("elma", createitems("elma"));
    checkitemsMatches("elmas", createitems("elma", "elmas"));
  }

  @Test
  public void stemsSharingSamePrefix3Stems() {
    List<Item> items = createitems("el", "elmas", "elma");
    additems(items);
    checkitemsExist(items);
    checkitemsMatches("elma", createitems("el", "elma"));
    checkitemsMatches("el", createitems("el"));
    checkitemsMatches("elmas", createitems("el", "elma", "elmas"));
    checkitemsMatches("elmaslar", createitems("el", "elma", "elmas"));
  }

  @Test
  public void stemsForLongerInputs() {
    List<Item> items = createitems("el", "elmas", "elma", "ela");
    additems(items);
    checkitemsExist(items);
    checkitemsMatches("e", createitems());
    checkitemsMatches("el", createitems("el"));
    checkitemsMatches("elif", createitems("el"));
    checkitemsMatches("ela", createitems("el", "ela"));
    checkitemsMatches("elastik", createitems("el", "ela"));
    checkitemsMatches("elmas", createitems("el", "elma", "elmas"));
    checkitemsMatches("elmaslar", createitems("el", "elma", "elmas"));
  }

  @Test
  public void removeStems() {
    List<Item> items = createitems("el", "elmas", "elma", "ela");
    additems(items);
    checkitemsExist(items);
    checkitemsMatches("el", createitems("el"));
    // Remove el
    checkitemsMatches("el", createitems());
    // Remove elmas
    lt.remove(items.get(1).surfaceForm, items.get(1));
    checkitemsMatches("elmas", createitems());
    checkitemsMatches("e", createitems());
    checkitemsMatches("ela", createitems("ela"));
    checkitemsMatches("elastik", createitems("ela"));
    checkitemsMatches("elmas", createitems("el", "elma"));
    checkitemsMatches("elmaslar", createitems("el", "elma"));
  }

  @Test
  public void stemsSharingPartialPrefix1() {
    List<Item> items = createitems("fix", "foobar", "foxes");
    additems(items);
    checkitemsExist(items);
  }

  private List<String> generateRandomWords(int number) {
    List<String> randomWords = Lists.newArrayList();
    String letters = alphabet.getLowercaseLetters();
    for (int i = 0; i < number; i++) {
      int len = r.nextInt(20) + 1;
      char[] chars = new char[len];
      for (int j = 0; j < len; j++) {
        chars[j] = letters.charAt(r.nextInt(29) + 1);
      }
      randomWords.add(new String(chars));
    }
    return randomWords;
  }

  @Test
  public void testBigNumberOfBigWords() {
    List<String> words = generateRandomWords(10000);
    Trie<Item> testTrie = new Trie<>();
    List<Item> items = new ArrayList<>();
    for (String s : words) {
      Item item = new Item(s, "s: " + s);
      testTrie.add(item.surfaceForm, item);
      items.add(item);
    }
    for (Item item : items) {
      List<Item> res = testTrie.getMatchingItems(item.surfaceForm);
      assertTrue(res.contains(item));
      assertTrue(res.get(res.size() - 1).surfaceForm.equals(item.surfaceForm));
      for (Item n : res) {
        // Check if all stems are a prefix of last one on the tree.
        assertTrue(res.get(res.size() - 1).surfaceForm.startsWith(n.surfaceForm));
      }
    }
  }

}