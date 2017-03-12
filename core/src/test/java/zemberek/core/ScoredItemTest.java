package zemberek.core;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class ScoredItemTest {

    @Test
    public void testConstructor() {
        ScoredItem<String> k = new ScoredItem<>("foo", 0.1f);
        Assert.assertEquals("foo",k.item);
        Assert.assertEquals(0.1f,k.score,0.0001f);
    }

    @Test
    public void testDefaultComparable() {
        ScoredItem<String> i1 = new ScoredItem<>("foo", 0.3f);
        ScoredItem<String> i2 = new ScoredItem<>("bar", 0.1f);
        ScoredItem<String> i3 = new ScoredItem<>("baz", 0.5f);
        List<ScoredItem<String>> l = Lists.newArrayList(i1, i2, i3);
        Collections.sort(l);
        Assert.assertEquals("baz",l.get(0).item);
        Assert.assertEquals("foo",l.get(1).item);
        Assert.assertEquals("bar",l.get(2).item);
    }

    @Test
    public void testStringComparators() {
        ScoredItem<String> i1 = new ScoredItem<>("foo", 0.3f);
        ScoredItem<String> i2 = new ScoredItem<>("bar", 0.1f);
        ScoredItem<String> i3 = new ScoredItem<>("baz", 0.5f);
        List<ScoredItem<String>> l = Lists.newArrayList(i1, i2, i3);
        l.sort(ScoredItem.STRING_COMP_DESCENDING);
        Assert.assertEquals("baz",l.get(0).item);
        Assert.assertEquals("foo",l.get(1).item);
        Assert.assertEquals("bar",l.get(2).item);
        l.sort(ScoredItem.STRING_COMP_ASCENDING);
        Assert.assertEquals("bar",l.get(0).item);
        Assert.assertEquals("foo",l.get(1).item);
        Assert.assertEquals("baz",l.get(2).item);
    }
}
