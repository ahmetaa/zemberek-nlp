package zemberek.core;

import com.google.common.base.Stopwatch;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CompactMultisetTest {

    @Test
    public void testCount() {
        CompactMultiSet set = new CompactMultiSet(2);
        addStrings(set, "elma", "armut", "ayva", "kiraz", "muz", "karpuz", "elma", "karpuz", "karpuz");
        Assert.assertEquals(2, set.get("elma".getBytes()));
        Assert.assertEquals(1, set.get("muz".getBytes()));
        Assert.assertEquals(3, set.get("karpuz".getBytes()));
        Assert.assertEquals(6, set.getSize());
    }

    @Test
    public void testCount3() {
        CompactMultiSet set = new CompactMultiSet(2);
        for (int i = 0; i < 128; i++) {
            addStrings(set, "tabak");
        }
        Assert.assertEquals(128, set.get("tabak".getBytes()));
    }

    @Test
    public void testCount2() {
        CompactMultiSet set = new CompactMultiSet(2);
        for (int i = 0; i < 1; i++) {
            addStrings(set, "armut");
        }
        for (int i = 0; i < 10; i++) {
            addStrings(set, "elma");
        }
        for (int i = 0; i < 126; i++) {
            addStrings(set, "kabak");
        }
        for (int i = 0; i < 128; i++) {
            addStrings(set, "tabak");
        }
        for (int i = 0; i < 10000; i++) {
            addStrings(set, "turşu");
        }
        for (int i = 0; i < 1000000; i++) {
            addStrings(set, "salak");
        }
        Assert.assertEquals(1, set.get("armut".getBytes()));
        Assert.assertEquals(10, set.get("elma".getBytes()));
        Assert.assertEquals(126, set.get("kabak".getBytes()));
        Assert.assertEquals(128, set.get("tabak".getBytes()));
        Assert.assertEquals(10000, set.get("turşu".getBytes()));
        Assert.assertEquals(1000000, set.get("salak".getBytes()));
    }


    @Test
    public void testIterator() {
        CompactMultiSet set = new CompactMultiSet(4);
        addStrings(set, "elma", "armut", "ayva", "kiraz", "muz", "karpuz", "elma", "karpuz", "karpuz",
                "ayva", "kiraz", "muz", "karpuz", "elma", "karpuz", "karpuz");
        Histogram<String> counts = new Histogram<String>();
        counts.add("elma", "armut", "ayva", "kiraz", "muz", "karpuz", "elma", "karpuz", "karpuz",
                "ayva", "kiraz", "muz", "karpuz", "elma", "karpuz", "karpuz");

        for (CompactMultiSet.Entry entry : set) {
            String s = new String(entry.arr);
            Assert.assertEquals(counts.getCount(s), entry.count);
        }
    }

    private void addStrings(CompactMultiSet set, String... strings) {
        for (String string : strings) {
            set.add(string.getBytes());
        }
    }

    @Test
    @Ignore("Not a unit test")
    public void testHistogram() throws IOException {
        WhiteSpaceTokenizer wst = new WhiteSpaceTokenizer();
        CompactMultiSet cms = new CompactMultiSet(1 << 24, 30000000, 4);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/kodlab/data/corpora/turkish-news-corpus/cnn-turk.txt"), "utf-8"), 1000000);
        String line = "";
        long l = 0;
        Stopwatch sw = new Stopwatch().start();
        while ((line = reader.readLine()) != null) {
            l++;
            String[] words = wst.split(line);

            for (String word : words) {
                if (word.length() > 100)
                    cms.add("<UNK>".getBytes());
                else
                    cms.add(word.getBytes());
            }

            printDot(l);
        }
        System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    @Ignore("Not a unit test")
    public void testCounts() throws IOException {
        Stopwatch sw = new Stopwatch().start();
        final int keyCount = 50000;
        final int divider = 500;
        CompactMultiSet cms = new CompactMultiSet(1 << 16, keyCount, 5);
        for (int i = 0; i < keyCount; i++) {
            for (int j = 0; j < i / divider + 1; j++)
                cms.add(String.valueOf(i).getBytes());
        }
        System.out.println("elapsed for add:" + sw.elapsed(TimeUnit.MILLISECONDS));
        System.out.println("Key Count:" + cms.getSize());
        sw.reset().start();

        for (int i = 0; i < keyCount; i++) {
            Assert.assertEquals(i / divider + 1, cms.get(String.valueOf(i).getBytes()));
        }
        System.out.println("elapsed for check:" + sw.elapsed(TimeUnit.MILLISECONDS));

        sw.reset().start();
        MapBasedCounter map = new MapBasedCounter(1 << 16);
        for (int i = 0; i < keyCount; i++) {
            for (int j = 0; j < i / divider + 1; j++) {
                map.add(String.valueOf(i));
            }
        }
        System.out.println("elapsed for add:" + sw.elapsed(TimeUnit.MILLISECONDS));
        sw.reset().start();
        for (int i = 0; i < keyCount; i++) {
            Assert.assertEquals(i / divider + 1, map.get(String.valueOf(i)));
        }
        System.out.println("elapsed for check:" + sw.elapsed(TimeUnit.MILLISECONDS));

        sw.reset().start();
        MapBasedByteArrayCounter mapa = new MapBasedByteArrayCounter(1 << 16);
        for (int i = 0; i < keyCount; i++) {
            for (int j = 0; j < i / divider + 1; j++) {
                mapa.add(new ByteArrayStuff(String.valueOf(i).getBytes()));
            }
        }
        System.out.println("elapsed for add:" + sw.elapsed(TimeUnit.MILLISECONDS));
        sw.reset().start();
        for (int i = 0; i < keyCount; i++) {
            try {
                Assert.assertEquals(i / divider + 1, mapa.get(new ByteArrayStuff(String.valueOf(i).getBytes())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("elapsed for check:" + sw.elapsed(TimeUnit.MILLISECONDS));
    }

    private class MapBasedCounter {
        private HashMap<String, Integer> map = new HashMap<>();

        private MapBasedCounter(int initial) {
            this.map = new HashMap<>(initial);
        }

        private void add(String key) {
            if (!map.containsKey(key))
                map.put(key, 1);
            else
                map.put(key, map.get(key) + 1);
        }

        private int get(String key) {
            return map.get(key);
        }
    }

    private class MapBasedByteArrayCounter {
        private HashMap<ByteArrayStuff, Integer> map = new HashMap<>();

        private MapBasedByteArrayCounter(int initial) {
            this.map = new HashMap<>(initial);
        }

        private void add(ByteArrayStuff key) {
            if (!map.containsKey(key))
                map.put(key, 1);
            else
                map.put(key, map.get(key) + 1);
        }

        private int get(ByteArrayStuff key) {
            return map.get(key);
        }
    }

    private class ByteArrayStuff {
        final byte[] bytes;

        private ByteArrayStuff(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (getClass() != o.getClass()) return false;
            ByteArrayStuff that = (ByteArrayStuff) o;
            return Arrays.equals(bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    private static final int DOT1 = 0x1fff;
    private static final int DOT2 = 0xfffff;

    private void printDot(long l) {
        if ((l & DOT1) == DOT1) {
            System.out.print(".");
        }
        if ((l & DOT2) == DOT2) {
            System.out.print(" " + l + "\n");
        }
    }
    
    @Test
    public void testSortedIterator() {
        for (int i = 0; i < 20; i++) {
            Random r = new Random(1);
            List<byte[]> keys = new ArrayList<>();
            
            for (int j = 0; j < 1000; j++) {
                byte[] b = new byte[r.nextInt(10) + 6];
                r.nextBytes(b);
                keys.add(b);
            }
            
            List<List<byte[]>> orders = new ArrayList<>();
            for (int j = 1; j < 10; j++) {
                CompactMultiSet cms = new CompactMultiSet(1 << i);
                Collections.shuffle(keys);
                for (byte[] key : keys) {
                    cms.add(key);
                }
                Iterator<CompactMultiSet.Entry> it = cms.sortedIterator();
                List<byte[]> order = new ArrayList<>();
                while (it.hasNext()) {
                    order.add(it.next().arr);
                }
                orders.add(order);
            }
            for (int j = 1; j < orders.size(); j++) {
                List<byte[]> l1 = orders.get(0);
                List<byte[]> l2 = orders.get(j);
                
                Assert.assertEquals(l1.size(), l2.size());
                for (int k = 0; k < l1.size(); k++) {
                    byte[] b1 = l1.get(k);
                    byte[] b2 = l2.get(k);
                    
                    Assert.assertEquals(b1.length, b2.length);
                    
                    for (int l = 0; l < b1.length; l++) {
                        Assert.assertEquals(b1[l], b2[l]);
                    }
                }
            }
        }
    }
}