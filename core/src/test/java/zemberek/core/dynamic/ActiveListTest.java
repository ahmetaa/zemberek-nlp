package zemberek.core.dynamic;

import org.junit.Assert;
import org.junit.Test;

public class ActiveListTest {

  static class Item implements Scorable {
    final int id;
    float score;

    Item(int id, float score) {
      this.id = id;
      this.score = score;
    }

    @Override
    public float getScore() {
      return score;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Item item = (Item) o;
      return id == item.id;
    }

    @Override
    public int hashCode() {
      return id;
    }
  }

  @Test
  public void testActiveListDeduplicationAcrossExpansion() {
    ActiveList<Item> list = new ActiveList<>(8);

    // Add items 0..9 (forces multiple expansions from capacity 8 to 16 to 32)
    for (int i = 0; i < 10; i++) {
      list.add(new Item(i, i * 1.0f));
    }

    // Now update existing item 2 with a higher score (99.0f)
    list.add(new Item(2, 99.0f));

    // Also try updating item 2 with a lower score (1.0f) - should NOT overwrite 99.0f
    list.add(new Item(2, 1.0f));

    // Verify there are exactly 10 items, no duplicates, and item 2 has score 99.0f
    int count = 0;
    float item2Score = -1;
    for (Item item : list) {
      count++;
      if (item.id == 2) {
        item2Score = item.score;
      }
    }

    Assert.assertEquals(10, count);
    Assert.assertEquals(99.0f, item2Score, 0.001f);
    Assert.assertEquals(99.0f, list.getBest().score, 0.001f);
    Assert.assertEquals(2, list.getBest().id);
  }

  @Test
  public void testCapacityOneExpansion() {
    ActiveList<Item> list = new ActiveList<>(1);
    list.add(new Item(1, 10.0f));
    list.add(new Item(2, 20.0f));
    list.add(new Item(3, 30.0f));

    Assert.assertEquals(30.0f, list.getBest().score, 0.001f);
    Assert.assertEquals(3, list.getBest().id);
  }

  @Test
  public void testIteratorContract() {
    ActiveList<Item> list = new ActiveList<>(8);
    list.add(new Item(1, 10.0f));
    list.add(new Item(2, 20.0f));

    java.util.Iterator<Item> it = list.iterator();
    // Multiple hasNext() calls should not advance pointer or skip items
    Assert.assertTrue(it.hasNext());
    Assert.assertTrue(it.hasNext());
    Assert.assertTrue(it.hasNext());

    Item first = it.next();
    Assert.assertNotNull(first);

    Assert.assertTrue(it.hasNext());
    Item second = it.next();
    Assert.assertNotNull(second);
    Assert.assertNotEquals(first.id, second.id);

    Assert.assertFalse(it.hasNext());
    try {
      it.next();
      Assert.fail("Should have thrown NoSuchElementException");
    } catch (java.util.NoSuchElementException expected) {
      // Expected
    }
  }
}

