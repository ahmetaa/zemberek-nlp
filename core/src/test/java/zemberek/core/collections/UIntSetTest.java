package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class UIntSetTest {

    @Test
    public void containsSet() {
        UIntSet set = new UIntSet();
        for (int i = 0; i < 1000000; i++) {
            set.add(i);
        }

        for (int i = 0; i < 2000000; i++) {
            if (i < 1000000)
                Assert.assertTrue(set.contains(i));
            else
                Assert.assertFalse(set.contains(i));
        }
    }

    @Test
    public void stressTest() {
        UIntSet set = new UIntSet();
        int size = 100000;
        for (int i = 0; i < size; i++) {
            set.add(i);
        }
        Random rnd = new Random();
        int[] removed = new int[size];
        for (int i = 0; i < 50000; i++) {
            int key = rnd.nextInt(size);
            removed[key] = 1;
            set.remove(key);
        }

        for (int i = 0; i < size; i++) {
            if (removed[i] == 0)
                Assert.assertTrue(set.contains(i));
            else
                Assert.assertFalse(set.contains(i));
        }

        for (int i = 0; i < 20000; i++) {
            int key = rnd.nextInt(size);
            removed[key] = 0;
            set.add(key);
        }

        for (int i = 0; i < size; i++) {
            if (removed[i] == 0)
                Assert.assertTrue(set.contains(i));
            else
                Assert.assertFalse(set.contains(i));
        }
    }

    @Test
    public void removeTest() {
        UIntSet set = new UIntSet();
        int count = 100000;
        for (int i = 0; i < count; i++) {
            set.add(i);
        }
        Assert.assertEquals(count, set.size());
        int removedCount = 0;
        for (int i = 0; i < count; i += 3) {
            set.remove(i);
            removedCount++;
        }
        Assert.assertEquals(count - removedCount, set.size());

        for (int i = 0; i < count; i += 3) {
            Assert.assertFalse(set.contains(i));
        }

        for (int i = 0; i < count; i++) {
            set.add(i);
        }

        Assert.assertEquals(count, set.size());

        for (int i = 0; i < count; i += 3) {
            Assert.assertTrue(set.contains(i));
        }
    }


    @Test
    @Ignore("Not a unit test")
    public void performance() {
        Random r = new Random();
        int[] keys = new int[1000000];
        final int itCount = 10;
        for (int i = 0; i < keys.length; i++) {
            keys[i] = r.nextInt(500000);
        }
        Stopwatch sw = Stopwatch.createStarted();
        for (int j = 0; j < itCount; j++) {

            Set<Integer> set = new HashSet<>();

            for (int key1 : keys) {
                set.add(key1);
            }

            for (int key : keys) {
                set.contains(key);
            }

            for (int key : keys) {
                if (set.contains(key))
                    set.remove(key);
            }
        }
        System.out.println("Set Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));

        sw = Stopwatch.createStarted();

        for (int j = 0; j < itCount; j++) {

            UIntSet set = new UIntSet();

            for (int key1 : keys) {
                set.add(key1);
            }
            for (int key : keys) {
                set.contains(key);
            }

            for (int key : keys) {
                if (set.contains(key))
                    set.remove(key);
            }
        }
        System.out.println("Uint Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
    }


}
