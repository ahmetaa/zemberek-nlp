package zemberek.core.bits;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class DenseIntegerSequenceTest {

    @Test
    public void extremumValues() {
        int[] testArray = {0,1,7,232,655,-2323,Integer.MAX_VALUE, Integer.MIN_VALUE, 0,2,5};
        generateAndCheck(testArray);
    }

    @Test
    public void smallArryas() {
        generateAndCheck(0,1);
        generateAndCheck(1);
        generateAndCheck(-1);
    }

    private void generateAndCheck(int... testArray) {
        DenseIntegerSequence sequence = new DenseIntegerSequence(testArray);
        for (int i = 0; i < testArray.length; i++) {
            Assert.assertEquals(testArray[i],sequence.get(i));
        }
    }

    @Test
    public void randomValues() {
        Random rnd = new Random();
        int[] testArray = new int[100000];
        for (int i = 0; i < testArray.length; i++) {
            testArray[i] = rnd.nextInt();
        }
        long start = System.currentTimeMillis();
        DenseIntegerSequence sequence = new DenseIntegerSequence(testArray);
        System.out.println("Dense sequence generation time:" + (System.currentTimeMillis()-start));
        for (int i = 0; i < testArray.length; i++) {
            Assert.assertEquals(testArray[i],sequence.get(i));
        }
        start = System.currentTimeMillis();
        for (int i = 0; i < testArray.length; i++) {
            sequence.get(i);
        }
        System.out.println("Access time for 100.000 get:" + (System.currentTimeMillis()-start));
    }

}
