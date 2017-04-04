package zemberek.core.hash;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.TestUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MultiLevelMphfTest {
    @Test
    public void intKeys() throws IOException {
        int[] limits = {1, 2, 10, 100, 1000, 50000, 100000};
        int arraySize = 5;
        for (int limit : limits) {
            System.out.println("Key amount: " + limit);
            final int[][] arr = new int[limit][arraySize];
            for (int i = 0; i < limit; i++) {
                for (int j = 0; j < arraySize; j++)
                    arr[i][j] = i;
            }
            generateAndTest(new IntArrayKeyProvider(arr));
        }
    }

    @Test
    public void stringKeys() throws IOException {
        int[] limits = {1, 2, 10, 100, 1000, 50000, 100000};
        int strSize = 5;
        for (int limit : limits) {
            System.out.println("Key amount: " + limit);
            Stopwatch sw = Stopwatch.createStarted();
            StringHashKeyProvider provider = new StringHashKeyProvider(TestUtil.uniqueStrings(limit, strSize));
            System.out.println("Generation:" + sw.elapsed(TimeUnit.MILLISECONDS));
            generateAndTest(provider);
        }
    }

    private void generateAndTest(IntHashKeyProvider provider) {

        long start = System.currentTimeMillis();
        MultiLevelMphf fmph = MultiLevelMphf.generate(provider);
        System.out.println("Time to generate:" + (System.currentTimeMillis() - start));

        System.out.println("Bits per key:" + fmph.averageBitsPerKey());
        System.out.println("Hash levels:" + fmph.getLevelCount());
        start = System.currentTimeMillis();

        final int keyAmount = provider.keyAmount();
        int[] values = new int[keyAmount];
        for (int i = 0; i < keyAmount; i++) {
            values[i] = fmph.get(provider.getKey(i));
        }

        System.out.println("Time to query:" + (System.currentTimeMillis() - start));

        Set<Integer> results = new HashSet<>(keyAmount);
        for (int i = 0; i < keyAmount; i++) {
            Assert.assertTrue(i + ":" + values[i], results.add(values[i]));
        }
    }

}
