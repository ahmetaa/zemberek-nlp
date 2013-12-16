package zemberek.core;

import com.google.common.base.Stopwatch;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NgramCountMapTest {

    @Test
    public void getTest2() {
        int[][] data = {{5}, {1}, {7}, {9}, {10}, {123}};
        NgramCountMap map = new NgramCountMap(1);
        for (int[] ints : data) {
            map.put(ints, ints[0]);
        }
        for (int[] ints : data) {
            int read = map.getCount(ints);
            Assert.assertEquals(ints[0], read);
        }
    }

    @Test
    public void getTest3() {
        int[][] data = {{1}, {5}, {7}, {9}, {10}, {123}, {1}};
        NgramCountMap map = new NgramCountMap(1);
        for (int[] ints : data) {
            map.increment(ints);
        }
        Assert.assertEquals(6, map.size());
        Assert.assertEquals(2, map.getCount(new int[]{1}));
        Assert.assertEquals(1, map.getCount(new int[]{123}));
    }

    @Test
    public void sortTest() {
        int[][] data = {{5}, {1}, {10}, {7}};
        NgramCountMap map = new NgramCountMap(1);
        for (int[] ints : data) {
            map.increment(ints);
        }
        Assert.assertEquals(4, map.size());
        NgramCountMap.NgramCount[] allSorted = map.getAllSorted();
        Assert.assertEquals(4, allSorted.length);
        Assert.assertTrue(Arrays.equals(allSorted[0].ids, new int[]{1}));
        Assert.assertTrue(Arrays.equals(allSorted[1].ids, new int[]{5}));
    }

    @Test
    public void getTest() {
        int[] counts = {1, 5, 10, 100, 1000, 10000, 100000};
        int[] orders = {1, 2, 3, 4, 5, 6, 7};
        for (int order : orders) {
            for (int count : counts) {
                NgramCountMap map = new NgramCountMap(order);
                int[][] randomKeys = randomKeys(order, count);
                System.out.println();
                for (int[] randomKey : randomKeys) {
                    map.put(randomKey, randomKey[0]);
                }
                for (int[] randomKey : randomKeys) {
                    int read = map.getCount(randomKey);
                    Assert.assertEquals(randomKey[0], read);
                }
            }
        }
    }

    int[][] randomKeys(int order, int keyCount) {
        Random rnd = new Random(1);
        int[][] keys = new int[keyCount][order];
        for (int i = 0; i < keyCount; i++) {
            for (int j = 0; j < order; j++) {
                keys[i][j] = Math.abs(rnd.nextInt() & 0xffff);
            }
        }
        return keys;
    }

    @Test
    @Ignore(value = "Not a unit test")
    public void perf() {
        int[] counts = {1000000, 5000000, 10000000};
        int[] orders = {2, 3, 4, 5};
        for (int order : orders) {
            for (int count : counts) {
                NgramCountMap map = new NgramCountMap(order);
                System.out.println("Order:" + order + " Count=" + count);
                int[][] randomKeys = randomKeys(order, count);
                Stopwatch sw = Stopwatch.createStarted();
                System.out.println();
                for (int[] randomKey : randomKeys) {
                    map.put(randomKey, randomKey[0]);
                }
                System.out.println("put = " + sw.elapsed(TimeUnit.MILLISECONDS));

                sw.reset().start();
                for (int[] randomKey : randomKeys) {
                    map.getCount(randomKey);
                }
                System.out.println("get = " + sw.elapsed(TimeUnit.MILLISECONDS));

                sw.reset().start();
                NgramCountMap.NgramCount[] grams = map.getAllSorted();
                System.out.println("get sorted = " + sw.elapsed(TimeUnit.MILLISECONDS));
                System.out.println(grams.length);

            }
        }
    }


}
