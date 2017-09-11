package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class UIntMapTest {
    @Test
    public void getTest() {
        UIntMap<String> map = new UIntMap<>(1);
        map.put(1, "2");
        Assert.assertEquals("2", map.get(1));
        Assert.assertNull(map.get(2));
        map.put(1, "3");
        Assert.assertEquals("3", map.get(1));

        map = new UIntMap<>();
        for (int i = 0; i < 100000; i++) {
            map.put(i, String.valueOf(i + 1));
        }
        for (int i = 0; i < 100000; i++) {
            Assert.assertEquals(String.valueOf(i + 1), map.get(i));
        }
    }

    @Test
    public void testTroubleNumbers() {
        int[] troubleNumbers = {14, 1, 30, 31, 4, 21, 8, 37, 39};

        UIntMap<String> map = new UIntMap<>();
        for (int number : troubleNumbers) {
            map.put(number, String.valueOf(number));
        }
        map.put(15, "15");
        Assert.assertEquals("15", map.get(15));
    }

    @Test
    public void stressTest() {
        UIntMap<String> map = new UIntMap<>(1);
        int size = 100000;
        for (int i = 0; i < size; i++) {
            map.put(i, String.valueOf(i + 1));
        }
        Random rnd = new Random();
        int[] removed = new int[size];
        for (int i = 0; i < 50000; i++) {
            int key = rnd.nextInt(size);
            removed[key] = 1;
            map.remove(key);
        }

        for (int i = 0; i < size; i++) {
            if (removed[i] == 0)
                Assert.assertEquals(String.valueOf(i + 1), map.get(i));
            else
                Assert.assertFalse(map.containsKey(i));
        }

        for (int i = 0; i < 20000; i++) {
            int key = rnd.nextInt(size);
            removed[key] = 0;
            map.put(key, String.valueOf(key + 1));
        }

        for (int i = 0; i < size; i++) {
            if (removed[i] == 0)
                Assert.assertEquals(String.valueOf(i + 1), map.get(i));
            else
                Assert.assertFalse(map.containsKey(i));
        }
    }

    @Test
    public void getValuesTest() {
        UIntMap<String> map = new UIntMap<>();
        int size = 1000;
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String value = String.valueOf(i + 1);
            map.put(i, value);
            expected.add(value);
        }
        Assert.assertEquals(expected, map.getValuesSortedByKey());
    }

    @Test
    public void getValuesTest2() {
        UIntMap<String> map = new UIntMap<>();
        map.put(1, "a");
        map.put(5, "b");
        map.put(12345, "a");
        List<String> values = map.getValues();
        Collections.sort(values);
        List<String> expected = Lists.newArrayList("a", "a", "b");
        Assert.assertEquals(expected, values);
    }

    @Test
    public void removeTest() {
        UIntMap<String> map = new UIntMap<>();
        int count = 100000;
        for (int i = 0; i < count; i++) {
            map.put(i, String.valueOf(i + 1));
        }
        Assert.assertEquals(count, map.size());
        int removedCount = 0;
        for (int i = 0; i < count; i += 3) {
            map.remove(i);
            removedCount++;
        }
        Assert.assertEquals(count - removedCount, map.size());

        for (int i = 0; i < count; i += 3) {
            Assert.assertTrue(!map.containsKey(i));
        }

        for (int i = 0; i < count; i++) {
            map.put(i, String.valueOf(i + 1));
        }

        Assert.assertEquals(count, map.size());

        for (int i = 0; i < count; i += 3) {
            Assert.assertTrue(map.containsKey(i));
        }
    }

    @Test
    @Ignore("Not a unit test")
    public void performance() {
        Random r = new Random();
        int[] keys = new int[1000000];
        String[] values = new String[1000000];
        final int itCount = 10;
        for (int i = 0; i < keys.length; i++) {
            keys[i] = r.nextInt(500000);
            values[i] = String.valueOf(r.nextInt(5000) + 1);
        }
        Stopwatch sw = Stopwatch.createStarted();
        for (int j = 0; j < itCount; j++) {

            HashMap<Integer, String> map = new HashMap<>();

            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], values[i]);
            }

            for (int key : keys) {
                map.get(key);
            }

            for (int key : keys) {
                if (map.containsKey(key))
                    map.remove(key);
            }
        }
        System.out.println("Map Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));

        sw = Stopwatch.createStarted();

        for (int j = 0; j < itCount; j++) {

            UIntMap<String> map = new UIntMap<>();

            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], values[i]);
            }
            for (int key : keys) {
                map.get(key);
            }

            for (int key : keys) {
                if (map.containsKey(key))
                    map.remove(key);
            }
        }
        System.out.println("Uint Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
    }
}
