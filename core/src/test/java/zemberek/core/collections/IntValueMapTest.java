package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class IntValueMapTest {

    @Test
    public void constructorTest() {
        IntValueMap set = new IntValueMap();
        Assert.assertEquals(0, set.size());
        set = new IntValueMap(1);
        Assert.assertEquals(0, set.size());
    }

    @Test
    public void constructorTest2() {
        IntValueMap<String> set = new IntValueMap<>(1);
        set.addOrIncrement("foo");
        Assert.assertEquals(1, set.size());
        set.remove("foo");
        Assert.assertEquals(0, set.size());
        Assert.assertFalse(set.contains("foo"));
    }


    @Test
    public void putTest() {
        IntValueMap<String> table = new IntValueMap<>();
        table.put("foo", 1);
        Assert.assertEquals(1, table.size());
        table.put("foo", 2);
        Assert.assertEquals(1, table.size());

        table = new IntValueMap<>();
        for (int i = 0; i < 1000; i++) {
            table.put(String.valueOf(i), i + 1);
            Assert.assertEquals(i + 1, table.size());
        }

        table = new IntValueMap<>();
        for (int i = 0; i < 1000; i++) {
            table.put(String.valueOf(i), i + 1);
            table.put(String.valueOf(i), i + 1);
            Assert.assertEquals(i + 1, table.size());
        }
    }

    @Test
    public void expandTest() {
        IntValueMap<String> table = new IntValueMap<>();

        // we put 0..9999 keys with 1..10000 values
        for (int i = 0; i < 10000; i++) {
            table.put(String.valueOf(i), i + 1);
            Assert.assertEquals(i + 1, table.size());
        }
        // we remove the first half
        for (int i = 0; i < 5000; i++) {
            table.remove(String.valueOf(i));
            Assert.assertEquals(10000 - i - 1, table.size());
        }
        // now we check if remaining values are intact
        for (int i = 5000; i < 10000; i++) {
            Assert.assertEquals(i + 1, table.get(String.valueOf(i)));
        }
    }

    @Test
    public void collisionTest() {
        IntValueMap<Integer> v = new IntValueMap<>(16);
        v.put(3, 5);
        v.put(19, 9);
        v.put(35, 13);
        Assert.assertEquals(3, v.keyCount);

        Assert.assertEquals(5, v.get(3));
        Assert.assertEquals(9, v.get(19));
        Assert.assertEquals(13, v.get(35));

        v.remove(19);
        Assert.assertEquals(2, v.keyCount);

        Assert.assertEquals(5, v.get(3));
        Assert.assertEquals(0, v.get(19));
        Assert.assertEquals(13, v.get(35));

        v.addOrIncrement(35);
        Assert.assertEquals(2, v.keyCount);
        Assert.assertEquals(14, v.get(35));

        v.remove(35);
        Assert.assertEquals(1, v.keyCount);
        v.put(19, 5);
        Assert.assertEquals(2, v.keyCount);
        v.addOrIncrement(35);
        Assert.assertEquals(3, v.keyCount);

        Assert.assertEquals(1, v.get(35));
        Assert.assertEquals(5, v.get(19));
    }

    @Test
    public void removeTest() {
        IntValueMap<String> table = new IntValueMap<>();
        table.put(String.valueOf(1), 1);
        Assert.assertEquals(1, table.size());
        table.remove(String.valueOf(1));
        Assert.assertEquals(0, table.size());

        table = new IntValueMap<>();
        for (int i = 0; i < 1000; i++) {
            table.put(String.valueOf(i), i + 1);
        }
        for (int i = 0; i < 1000; i++) {
            table.remove(String.valueOf(i));
            Assert.assertEquals(0, table.get(String.valueOf(i)));
            Assert.assertEquals(1000 - i - 1, table.size());
        }

        table = new IntValueMap<>(8);
        table.put(String.valueOf(1), 1);
        table.put(String.valueOf(9), 1);
        Assert.assertEquals(2, table.size());
        table.remove(String.valueOf(9));
        Assert.assertEquals(1, table.size());
        Assert.assertEquals(0, table.get(String.valueOf(9)));
    }

    @Test
    public void incremenTest() {
        IntValueMap<Integer> table = new IntValueMap<>();

        int res = table.addOrIncrement(1);
        Assert.assertEquals(1, res);
        Assert.assertEquals(1, table.get(1));

        table.put(1, 2);
        res = table.addOrIncrement(1);
        Assert.assertEquals(3, res);
        Assert.assertEquals(3, table.get(1));

        table = new IntValueMap<>();
        for (int i = 0; i < 1000; i++) {
            res = table.addOrIncrement(1);
            Assert.assertEquals(i + 1, res);
            Assert.assertEquals(i + 1, table.get(1));
            Assert.assertEquals(1, table.size());
        }
    }

    @Test
    public void decremenTest() {
        IntValueMap<Integer> set = new IntValueMap<>();

        int res = set.decrement(1);
        Assert.assertEquals(-1, res);
        final int val = 5;
        set.put(1, val);
        set.put(9, val);
        for (int i = 0; i < val; i++) {
            res = set.decrement(1);
            int expected = val - i - 1;
            Assert.assertEquals(expected, res);
            Assert.assertEquals(expected, set.get(1));
        }
        Assert.assertEquals(2, set.size());
        res = set.decrement(1);
        Assert.assertEquals(-1, res);

        set = new IntValueMap<>();
        for (int i = 0; i < 1000; i++) {
            set.put(i, 1);
        }

        for (int i = 0; i < 1000; i++) {
            res = set.decrement(i);
            Assert.assertEquals(0, res);
            Assert.assertEquals(0, set.get(i));
        }

        set = new IntValueMap<>(8);
        set.put(1, 1);
        set.put(9, 1);
        Assert.assertEquals(2, set.size());
        set.decrement(9);
        Assert.assertEquals(0, set.get(9));
        Assert.assertEquals(2, set.size());
    }

    @Test
    public void getTest() {
        IntValueMap<Integer> table = new IntValueMap<>();
        table.put(1, 2);
        Assert.assertEquals(2, table.get(1));
        Assert.assertEquals(0, table.get(2));
        table.put(1, 3);
        Assert.assertEquals(3, table.get(1));

        table = new IntValueMap<>();
        for (int i = 0; i < 1000; i++) {
            table.put(i, i + 1);
        }
        for (int i = 0; i < 1000; i++) {
            Assert.assertEquals(i + 1, table.get(i));
        }
    }

    @Test
    public void copyOfValuesTest() {
        IntValueMap<String> set = new IntValueMap<>();
        for (int i = 0; i < 1000; i++) {
            set.put(String.valueOf(i), i + 1);
        }
        int[] values = set.copyOfValues();
        Assert.assertEquals(1000, values.length);
        Arrays.sort(values);
        for (int i = 0; i < 1000; i++) {
            Assert.assertEquals(i + 1, values[i]);
        }

        set.remove("768");
        set.remove("0");
        set.remove("999");

        values = set.copyOfValues();
        Assert.assertEquals(997, values.length);
        Arrays.sort(values);

        Assert.assertTrue(Arrays.binarySearch(values, 769) < 0);
        Assert.assertTrue(Arrays.binarySearch(values, 1) < 0);
        Assert.assertTrue(Arrays.binarySearch(values, 1000) < 0);
    }

    @Test
    public void stressTest() {
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < 20; i++) {
            IntValueMap<Integer> siv = new IntValueMap<>();
            int kc = 0;
            for (int j = 0; j < 100000; j++) {
                int key = rand.nextInt(1000);
                boolean exist = siv.contains(key);
                int operation = rand.nextInt(8);
                switch (operation) {
                    case 0: // insert
                        int value = rand.nextInt(10) + 1;
                        if (!exist) {
                            siv.put(key, value);
                            kc++;
                        }
                        break;
                    case 1:
                        if (exist) {
                            siv.remove(key);
                            kc--;
                        }
                        break;
                    case 2:
                        siv.addOrIncrement(key);
                        if (!exist)
                            kc++;
                        break;
                    case 3:
                        siv.get(key);
                        break;
                    case 4:
                        if (!exist)
                            kc++;
                        siv.decrement(key);
                        break;
                    case 6:
                        value = rand.nextInt(10) + 1;
                        siv.incrementByAmount(key, value);
                        if (!exist)
                            kc++;
                        break;
                    case 7:
                        value = rand.nextInt(10) + 1;
                        siv.incrementByAmount(key, -value);
                        if (!exist)
                            kc++;
                        break;
                }
            }
            System.out.println(i + " Calculated=" + kc + " Actual=" + siv.keyCount);
        }
    }

    @Test
    @Ignore("Not a unit test")
    public void performanceAgainstMap() {
        Random r = new Random();
        int[][] keyVals = new int[100000][2];
        final int itCount = 100;
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
                if (map.containsKey(keyVal[0])) {
                    map.put(keyVal[0], map.get(keyVal[0]) + 1);
                }
            }

            for (int[] keyVal : keyVals) {
                if (map.containsKey(keyVal[0])) {
                    int count = map.get(keyVal[0]);
                    if (count == 1)
                        map.remove(keyVal[0]);
                    else
                        map.put(keyVal[0], count - 1);
                }
            }
        }
        System.out.println("Map Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));


        IntValueMap<Integer> countTable = new IntValueMap<>();
        sw = Stopwatch.createStarted();

        for (int j = 0; j < itCount; j++) {

            for (int[] keyVal : keyVals) {
                countTable.put(keyVal[0], keyVal[1]);
            }
            for (int[] keyVal : keyVals) {
                countTable.get(keyVal[0]);
            }

            for (int[] keyVal : keyVals) {
                countTable.addOrIncrement(keyVal[0]);
            }

            for (int[] keyVal : keyVals) {
                countTable.decrement(keyVal[0]);
            }
        }
        System.out.println("Count Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    @Ignore("Not a unit test")
    public void perfStrings() {
        for (int i = 0; i < 5; i++) {
            Set<String> strings = uniqueStrings(1000000, 7);
            Stopwatch sw = Stopwatch.createStarted();
            Set<String> newSet = new HashSet<>(strings);
            System.out.println("Java Set : " + sw.elapsed(TimeUnit.MILLISECONDS));
            System.out.println("Size  = " + newSet.size());
            sw.reset().start();
            IntValueMap<String> cs = new IntValueMap<>(strings.size()*2);
            cs.addOrIncrementAll(strings);
            System.out.println("Count Add : " + sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private Set<String> uniqueStrings(int amount, int stringLength) {
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
    

