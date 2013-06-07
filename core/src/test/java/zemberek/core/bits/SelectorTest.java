package zemberek.core.bits;

import com.google.common.base.Stopwatch;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SelectorTest {

    @Test
    public void select1Small() {
        LongBitVector vector = LongBitVector.fromBinaryString("101100100011101 1011001010111 11001010011101 01110110111 10101101");
        Selector selector = new Selector(vector);
        int j = 1;
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i)) {
                Assert.assertEquals(i, selector.select1(j));
                j++;
            }
        }
    }

    @Test
    public void select1Big() {
        Random rnd = new Random();
        final int size = 1000000;
        LongBitVector vector = new LongBitVector(size);
        for (int i = 0; i < size / 3; i++) {
            vector.set(rnd.nextInt(size));
        }
        Selector selector = new Selector(vector);
        int j = 1;
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i)) {
                Assert.assertEquals(i, selector.select1(j));
                j++;
            }
        }
    }

    @Test
    public void performance() {
        Random rnd = new Random(0xbeefcafe);
        final int size = 10000000;
        LongBitVector vector = new LongBitVector(size);
        for (int i = 0; i < size / 1.5d; i++) {
            vector.set(rnd.nextInt(size));
        }

        for (int it = 0; it < 5; it++) {
            Stopwatch sw = new Stopwatch().start();
            Selector selector = new Selector(vector);
            System.out.println("Selector generation time:" + sw.elapsed(TimeUnit.MILLISECONDS));
            System.out.println("One count:" + selector.getOneCount());
            sw.reset().start();
            for (int i = 1; i < selector.getOneCount(); i++) {
                selector.select1(i);
            }
            System.out.println("Selector select time:" + sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}
