package zemberek.core;

import com.google.common.base.Stopwatch;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SparseIntVectorTest {

    @Test
    public void constructorTest() {
        SparseIntVector table = new SparseIntVector();
        Assert.assertEquals(0, table.size());
        Assert.assertEquals(SparseIntVector.INITIAL_SIZE, table.capacity());
        table = new SparseIntVector(10);
        Assert.assertEquals(16, table.capacity());
        table = new SparseIntVector(16);
        Assert.assertEquals(16, table.capacity());
        table = new SparseIntVector(17);
        Assert.assertEquals(32, table.capacity());
    }

    @Test
    public void putTest() {
        SparseIntVector table = new SparseIntVector();
        table.set(1, 1);
        Assert.assertEquals(1, table.size());
        table.set(1, 2);
        Assert.assertEquals(1, table.size());

        table = new SparseIntVector();
        for (int i = 0; i < 1000; i++) {
            table.set(i, i + 1);
            Assert.assertEquals(i + 1, table.size());
        }
        Assert.assertEquals(2048, table.capacity());

        table = new SparseIntVector();
        for (int i = 0; i < 1000; i++) {
            table.set(i, i + 1);
            table.set(i, i + 1);
            Assert.assertEquals(i + 1, table.size());
        }
    }

    @Test
    public void expandTest() {
        SparseIntVector table = new SparseIntVector();

        // we put 0..999 keys with 1..1000 values
        for (int i = 0; i < 10000; i++) {
            table.set(i, i + 1);
            Assert.assertEquals(i + 1, table.size());
        }
        // we remove the first half
        for (int i = 0; i < 5000; i++) {
            table.remove(i);
            Assert.assertEquals(10000 - i - 1, table.size());
        }
        // now we check if remaining values are intact
        for (int i = 5000; i < 10000; i++) {
            Assert.assertEquals(i + 1, table.get(i));
        }
    }

    @Test
    public void removeTest() {
        SparseIntVector table = new SparseIntVector();
        table.set(1, 1);
        Assert.assertEquals(1, table.size());
        table.remove(1);
        Assert.assertEquals(0, table.size());

        table = new SparseIntVector();
        for (int i = 0; i < 1000; i++) {
            table.set(i, i + 1);
        }
        for (int i = 0; i < 1000; i++) {
            table.remove(i);
            Assert.assertEquals(0, table.get(i));
            Assert.assertEquals(1000 - i - 1, table.size());
        }

        table = new SparseIntVector(8);
        table.set(1, 1);
        table.set(9, 1);
        Assert.assertEquals(2, table.size());
        table.remove(9);
        Assert.assertEquals(1, table.size());
        Assert.assertEquals(0, table.get(9));
    }

    @Test
    public void zeroValueTest() {
        Random rnd = new Random();
        SparseIntVector table = new SparseIntVector();
        int k = 0;
        while (k < 1000) {
            int val = rnd.nextInt(10);
            if (val == 0)
                continue;
            if (rnd.nextBoolean())
                val = -val;
            table.set(k++, val);
        }
        Assert.assertEquals(1000, table.size());
        for (int i = 0; i < 1000000; i++) {
            int key = rnd.nextInt(1000);
            if (rnd.nextBoolean()) {
                table.increment(key);
            } else
                table.decrement(key);
        }

        for (SparseIntVector.TableEntry tableEntry : table) {
            Assert.assertTrue(tableEntry.value != 0);
        }
    }

    @Test
    public void collisionTest() {
        SparseIntVector v = new SparseIntVector(16);
        v.set(3, 5);
        v.set(19, 9);
        v.set(35, 13);
        Assert.assertEquals(3, v.keyCount);

        Assert.assertEquals(5, v.get(3));
        Assert.assertEquals(9, v.get(19));
        Assert.assertEquals(13, v.get(35));

        v.remove(19);
        Assert.assertEquals(2, v.keyCount);

        Assert.assertEquals(5, v.get(3));
        Assert.assertEquals(0, v.get(19));
        Assert.assertEquals(13, v.get(35));

        v.increment(35);
        Assert.assertEquals(2, v.keyCount);
        Assert.assertEquals(14, v.get(35));

        v.remove(35);
        Assert.assertEquals(1, v.keyCount);
        v.set(19, 5);
        Assert.assertEquals(2, v.keyCount);
        v.increment(35);
        Assert.assertEquals(3, v.keyCount);

        Assert.assertEquals(1, v.get(35));
        Assert.assertEquals(5, v.get(19));
    }

    @Test
    public void incremenTest() {
        SparseIntVector table = new SparseIntVector();

        int res = table.increment(1);
        Assert.assertEquals(1, res);
        Assert.assertEquals(1, table.get(1));

        table.set(1, 2);
        res = table.increment(1);
        Assert.assertEquals(3, res);
        Assert.assertEquals(3, table.get(1));

        table = new SparseIntVector();
        for (int i = 0; i < 1000; i++) {
            res = table.increment(1);
            Assert.assertEquals(i + 1, res);
            Assert.assertEquals(i + 1, table.get(1));
            Assert.assertEquals(1, table.size());
        }
    }

    @Test
    public void decremenTest() {
        SparseIntVector table = new SparseIntVector();

        int res = table.decrement(1);
        Assert.assertEquals(-1, res);
        final int val = 5;
        table.set(1, val);
        table.set(9, val);
        for (int i = 0; i < val; i++) {
            res = table.decrement(1);
            int expected = val - i - 1;
            if (expected == 0)
                expected = 0;
            Assert.assertEquals(expected, res);
            Assert.assertEquals(expected, table.get(1));
        }
        Assert.assertEquals(1, table.size());
        res = table.decrement(1);
        Assert.assertEquals(-1, res);

        table = new SparseIntVector();
        for (int i = 0; i < 1000; i++) {
            table.set(i, 1);
        }

        for (int i = 0; i < 1000; i++) {
            res = table.decrement(i);
            Assert.assertEquals(0, res);
            Assert.assertEquals(0, table.get(i));
            Assert.assertEquals(1000 - i - 1, table.size());
        }

        table = new SparseIntVector(8);
        table.set(1, 1);
        table.set(9, 1);
        Assert.assertEquals(2, table.size());
        table.decrement(9);
        Assert.assertEquals(1, table.size());
        Assert.assertEquals(0, table.get(9));

    }

    @Test
    public void getTest() {
        SparseIntVector table = new SparseIntVector();
        table.set(1, 2);
        Assert.assertEquals(2, table.get(1));
        Assert.assertEquals(0, table.get(2));
        table.set(1, 3);
        Assert.assertEquals(3, table.get(1));

        table = new SparseIntVector();
        for (int i = 0; i < 1000; i++) {
            table.set(i, i + 1);
        }
        for (int i = 0; i < 1000; i++) {
            Assert.assertEquals(i + 1, table.get(i));
        }
    }

    private int countIteration(SparseIntVector table) {
        int i = 0;
        for (SparseIntVector.TableEntry tableEntry : table) {
            i++;
        }
        return i;
    }

    @Test
    public void collisionTest2() {
        SparseIntVector table = new SparseIntVector(1024);
        for (int i = 1; i <= 100; i++) {
            table.set(i, i);
            Assert.assertEquals(i, table.get(i));
        }
        Assert.assertEquals(100, table.keyCount);
        Assert.assertEquals(100, countIteration(table));

        for (int i = 1024; i < 1024 + 100; i++) {
            table.set(i, i);
            Assert.assertEquals(i, table.get(i));
        }
        Assert.assertEquals(200, table.keyCount);
        Assert.assertEquals(200, countIteration(table));


        for (int i = 2048; i < 2048 + 100; i++) {
            table.set(i, i);
            Assert.assertEquals(i, table.get(i));
        }
        Assert.assertEquals(300, table.keyCount);
        Assert.assertEquals(300, countIteration(table));


        for (int i = 1024; i < 1024 + 100; i++) {
            table.remove(i);
            Assert.assertEquals(0, table.get(i));
        }
        Assert.assertEquals(200, table.keyCount);
        Assert.assertEquals(200, countIteration(table));


        for (int i = 2048; i < 2048 + 100; i++) {
            table.increment(i);
            Assert.assertEquals(i + 1, table.get(i));
        }
        Assert.assertEquals(200, table.keyCount);
        Assert.assertEquals(200, countIteration(table));


        for (int i = 1; i <= 100; i++) {
            table.incrementByAmount(i, -i);
            Assert.assertEquals(0, table.get(i));
        }
        Assert.assertEquals(100, table.keyCount);
        Assert.assertEquals(100, countIteration(table));


        for (int i = 1; i <= 100; i++) {
            table.set(i, i);
            Assert.assertEquals(i, table.get(i));
        }
        Assert.assertEquals(200, table.keyCount);
        Assert.assertEquals(200, countIteration(table));


        for (int i = 1024; i < 1024 + 100; i++) {
            table.set(i, i);
            Assert.assertEquals(i, table.get(i));
        }
        Assert.assertEquals(300, table.keyCount);

        for (int i = 2048; i < 2048 + 100; i++) {
            table.set(i, i);
            Assert.assertEquals(i, table.get(i));
        }
        Assert.assertEquals(300, table.keyCount);

        for (int i = 3096; i < 3096 + 100; i++) {
            table.set(i, i);
            Assert.assertEquals(i, table.get(i));
        }
        Assert.assertEquals(400, table.keyCount);

        for (int i = 1024; i < 1024 + 100; i++) {
            table.remove(i);
            Assert.assertEquals(0, table.get(i));
        }
        Assert.assertEquals(300, table.keyCount);

        for (int i = 1024; i < 1024 + 100; i++) {
            table.remove(i);
            Assert.assertEquals(0, table.get(i));
        }
        Assert.assertEquals(300, table.keyCount);

        for (int i = 2048; i < 2048 + 100; i++) {
            table.remove(i);
            Assert.assertEquals(0, table.get(i));
        }
        Assert.assertEquals(200, table.keyCount);

        for (int i = 3096; i < 3096 + 100; i++) {
            Assert.assertEquals(i, table.get(i));
        }
        Assert.assertEquals(200, table.keyCount);
    }

    @Test
    public void stressTest() {
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < 20; i++) {
            SparseIntVector siv = new SparseIntVector();
            int kc = 0;
            for (int j = 0; j < 100000; j++) {
                int key = rand.nextInt(1000);
                boolean exist = siv.get(key) != 0;
                int operation = rand.nextInt(8);
                switch (operation) {
                    case 0: // insert
                        int value = rand.nextInt(10) + 1;
                        if (!exist) {
                            siv.set(key, value);
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
                        siv.increment(key);
                        if (siv.get(key) == 1)
                            kc++;
                        if (siv.get(key) == 0)
                            kc--;
                        break;
                    case 3:
                        siv.get(key);
                        break;
                    case 4:
                        if (siv.get(key) == 0)
                            kc++;
                        if (siv.get(key) == 1)
                            kc--;
                        siv.decrement(key);
                        break;
                    case 6:
                        value = rand.nextInt(10) + 1;
                        siv.incrementByAmount(key, value);
                        if (!exist && siv.get(key) != 0)
                            kc++;
                        if (siv.get(key) == 0)
                            kc--;
                        break;
                    case 7:
                        value = rand.nextInt(10) + 1;
                        siv.incrementByAmount(key, -value);
                        if (!exist && siv.get(key) != 0)
                            kc++;
                        if (siv.get(key) == 0)
                            kc--;
                        break;
                }
            }
            System.out.println(i+" Calculated=" + kc + " Actual=" + siv.keyCount);
        }
    }

    @Test
    public void perf() {
        Random r = new Random();
        int[][] keyVals = new int[10000][2];
        final int itCount = 1000;
        for (int i = 0; i < keyVals.length; i++) {
            keyVals[i][0] = r.nextInt(500000);
            keyVals[i][1] = r.nextInt(5000) + 1;
        }
        Stopwatch sw = Stopwatch.createStarted();
        for (int j = 0; j < itCount; j++) {

            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

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


        SparseIntVector countTable = new SparseIntVector();
        sw = Stopwatch.createStarted();

        for (int j = 0; j < itCount; j++) {

            for (int[] keyVal : keyVals) {
                countTable.set(keyVal[0], keyVal[1]);
            }
            for (int[] keyVal : keyVals) {
                countTable.get(keyVal[0]);
            }

            for (int[] keyVal : keyVals) {
                countTable.increment(keyVal[0]);
            }

            for (int[] keyVal : keyVals) {
                countTable.decrement(keyVal[0]);
            }
        }
        System.out.println("Count Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
    }
}
