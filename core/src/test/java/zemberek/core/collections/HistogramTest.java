package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HistogramTest {

    @Test
    public void testGenerate() {
        Histogram<String> histogram = new Histogram<>();
        histogram.add("Apple", "Pear", "Plum", "Apple", "Apple", "Grape", "Pear");
        Assert.assertEquals(3, histogram.getCount("Apple"));
        Assert.assertEquals(2, histogram.getCount("Pear"));
        Assert.assertEquals(1, histogram.getCount("Plum"));
    }

    @Test
    public void testRemove() {
        Histogram<String> histogram = new Histogram<>();
        histogram.add("Apple", "Pear", "Plum", "Apple", "Apple", "Grape", "Pear");
        histogram.remove("Apple");
        Assert.assertEquals(0, histogram.getCount("Apple"));
        Assert.assertEquals(2, histogram.getCount("Pear"));
        Assert.assertEquals(1, histogram.getCount("Plum"));
        Assert.assertFalse(histogram.contains("Apple"));
    }

    @Test
    public void testSortedByCount() {
        Histogram<String> histogram = new Histogram<>();
        histogram.add("Apple", "Pear", "Apple", "Apple", "Grape", "Pear");
        List<String> sortedByCount = histogram.getSortedList();
        Assert.assertEquals(Lists.newArrayList("Apple", "Pear", "Grape"), sortedByCount);

        sortedByCount = histogram.getTop(1); // top 1
        Assert.assertEquals(Lists.newArrayList("Apple"), sortedByCount);
        sortedByCount = histogram.getTop(2); // top 2
        Assert.assertEquals(Lists.newArrayList("Apple", "Pear"), sortedByCount);
        sortedByCount = histogram.getTop(5); // top 5 should return all list
        Assert.assertEquals(Lists.newArrayList("Apple", "Pear", "Grape"), sortedByCount);
    }

    @Test
    public void testAddHistogram() {
        Histogram<String> histogram = new Histogram<>();
        histogram.add("Apple", "Pear", "Apple", "Apple", "Grape", "Pear");
        Histogram<String> histogram2 = new Histogram<>();
        histogram2.add("Apple", "Mango", "Apple", "Grape");
        histogram.add(histogram2);
        Assert.assertEquals(5, histogram.getCount("Apple"));
        Assert.assertEquals(1, histogram.getCount("Mango"));
        Assert.assertEquals(2, histogram.getCount("Grape"));
    }

    @Test
    @Ignore("Not a test.")
    public void testMergePerformance() throws IOException {
        Histogram<String> first = new Histogram<>();
        Histogram<String> second = new Histogram<>();
        Set<String> c1 = uniqueStrings(1000000, 5);
        Set<String> c2 = uniqueStrings(1000000, 5);
        Stopwatch sw = Stopwatch.createStarted();
        first.add(c1);
        second.add(c2);
        System.out.println("Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
        sw.reset().start();
        first.add(second);
        System.out.println("Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
    }

    public Set<String> uniqueStrings(int amount, int stringLength) {
        Set<String> set = new HashSet<>(amount);
        Random r = new Random();
        while (set.size() < amount) {
            StringBuilder sb = new StringBuilder(stringLength);
            for (int i = 0; i < stringLength; i++) {
                sb.append((char) (r.nextInt(26) + 'a'));
            }
            set.add(sb.toString());
        }
        return set;
    }


}