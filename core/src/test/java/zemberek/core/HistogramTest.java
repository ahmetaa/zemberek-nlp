package zemberek.core;

import com.google.common.base.Stopwatch;
import com.google.common.io.Resources;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
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
    @Ignore("Requires external file.")
    public void testPerf() throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(
                Resources.getResource("io/tr.count").openStream()))) {
            int order = dis.readInt();
            String modelId = dis.readUTF();
            System.out.println(modelId);
            Histogram<String>[] gramCounts = new Histogram[order + 1];
            for (int j = 1; j <= order; j++) {
                System.out.println("Order = " + j);
                Stopwatch sw = new Stopwatch().start();
                int size = dis.readInt();
                Histogram<String> countSet = new Histogram<>(size * 2);
                for (int i = 0; i < size; i++) {
                    String key = dis.readUTF();
                    countSet.add(key, dis.readInt());
                }

                System.out.println("Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
                gramCounts[j] = countSet;
            }

            Map<String, Double> frequencyMap = new HashMap<>();
            Stopwatch sw = new Stopwatch().start();
            for (String s : gramCounts[3]) {
                final String parentGram = s.substring(0, 2);
                if (!gramCounts[2].contains(parentGram))
                    continue;
                int cnt = gramCounts[2].getCount(parentGram)+1;
                double prob = Math.log((double) gramCounts[3].getCount(s) / (double) cnt);
                frequencyMap.put(s, prob);
            }
            System.out.println("Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }


    @Test
    @Ignore("Requires external file.")
    public void testPerf22CS() throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(
                Resources.getResource("io/ja.count").openStream()))) {
            int order = dis.readInt();
            String modelId = dis.readUTF();
            System.out.println(modelId);
            CountSet<String>[] gramCounts = new CountSet[order + 1];
            for (int j = 1; j <= order; j++) {
                System.out.println("Order = " + j);
                Stopwatch sw = new Stopwatch().start();
                int size = dis.readInt();
                CountSet<String> countSet = new CountSet<>(size * 2);
                for (int i = 0; i < size; i++) {
                    String key = dis.readUTF();
                    countSet.incrementByAmount(key, dis.readInt());
                }
                System.out.println("Size:" + countSet.size());

                System.out.println("Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
                gramCounts[j] = countSet;
            }

            Map<String, Double> frequencyMap = new HashMap<>();
            Stopwatch sw = new Stopwatch().start();
            for (String s : gramCounts[3]) {
                final String parentGram = s.substring(0, 2);
                if (!gramCounts[2].contains(parentGram))
                    continue;
                int cnt = gramCounts[2].get(parentGram)+1;
                double prob = Math.log((double) gramCounts[3].get(s) / (double) cnt);
                frequencyMap.put(s, prob);
            }
            System.out.println("Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));

        }

    }

    @Test
    public void testPerfMerge() throws IOException {
        Histogram<String> first  = new Histogram<>();
        Histogram<String> second  = new Histogram<>();
        Set<String> c1 = uniqueStrings(1000000, 5);
        Set<String> c2 = uniqueStrings(1000000, 5);
        Stopwatch sw = new Stopwatch().start();
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