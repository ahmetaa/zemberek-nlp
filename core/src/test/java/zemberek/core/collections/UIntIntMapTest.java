package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class UIntIntMapTest {

    @Test
    public void getTest() {
        UIntIntMap map = new UIntIntMap();
        map.put(1, 2);
        Assert.assertEquals(2, map.get(1));
        Assert.assertEquals(0, map.get(2));
        map.put(1, 3);
        Assert.assertEquals(3, map.get(1));

        map = new UIntIntMap();
        for (int i = 0; i < 100000; i++) {
            map.put(i, i + 1);
        }
        for (int i = 0; i < 100000; i++) {
            Assert.assertEquals(i + 1, map.get(i));
        }
    }

    @Test
    public void removeTest() {
        UIntIntMap map = new UIntIntMap();
        for (int i = 0; i < 100000; i++) {
            map.put(i, i + 1);
        }
        for (int i = 0; i < 100000; i += 3) {
            map.remove(i);
        }
        for (int i = 0; i < 100000; i += 3) {
            Assert.assertTrue(!map.containsKey(i));
        }
        for (int i = 0; i < 100000; i++) {
            map.put(i, i + 1);
        }
        for (int i = 0; i < 100000; i += 3) {
            Assert.assertTrue(map.containsKey(i));
        }
    }

    @Test
    @Ignore("Not a unit test")
    public void perf() {
        Random r = new Random();
        int[][] keyVals = new int[1000000][2];
        final int itCount = 10;
        for (int i = 0; i < keyVals.length; i++) {
            keyVals[i][0] = r.nextInt(500000);
            keyVals[i][1] = r.nextInt(5000) + 1;
        }
        Stopwatch sw = Stopwatch.createStarted();
        for (int j = 0; j < itCount; j++) {

            HashMap<Integer, Integer> map = new HashMap<>();

            for (int[] keyVal : keyVals) {
                map.put(keyVal[0], keyVal[1]);
            }

            for (int[] keyVal : keyVals) {
                map.get(keyVal[0]);
            }

            for (int[] keyVal : keyVals) {
                if (map.containsKey(keyVal[0]))
                    map.remove(keyVal[0]);
            }
        }
        System.out.println("Map Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));


        UIntIntMap countTable = new UIntIntMap();
        sw = Stopwatch.createStarted();

        for (int j = 0; j < itCount; j++) {

            for (int[] keyVal : keyVals) {
                countTable.put(keyVal[0], keyVal[1]);
            }
            for (int[] keyVal : keyVals) {
                countTable.get(keyVal[0]);
            }
            for (int[] keyVal : keyVals) {
                if (countTable.containsKey(keyVal[0]))
                    countTable.remove(keyVal[0]);
            }
        }
        System.out.println("Uint Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
    }

}
