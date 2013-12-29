package zemberek.core.math;

import com.google.common.base.Stopwatch;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LogMathTest {

    @Test
    public void logSumTest() throws IOException {
        double[] aLinear = new double[1000];
        double[] bLinear = new double[1000];

        for (int i = 0; i < bLinear.length; i++) {
            aLinear[i] = (double) i / 1000d;
            bLinear[i] = aLinear[i];
        }

        for (double a : aLinear) {
            for (double b : bLinear) {
                double logSumExpected = Math.log(a + b);
                Assert.assertEquals(logSumExpected, LogMath.logSum(Math.log(a), Math.log(b)), 0.001);
            }
        }
    }

    @Test
    public void logSumFloatTest() throws IOException {
        float[] aLinear = new float[1000];
        float[] bLinear = new float[1000];

        for (int i = 0; i < bLinear.length; i++) {
            aLinear[i] = (float) i / 1000f;
            bLinear[i] = aLinear[i];
        }

        for (float a : aLinear) {
            for (float b : bLinear) {
                float logSumExpected = (float) Math.log(a + b);
                Assert.assertEquals(logSumExpected, LogMath.logSum(Math.log(a), Math.log(b)), 0.001);
            }
        }
    }    

    @Test
    public void logSumExactTest() throws IOException {
        double[] aLinear = new double[1000];
        double[] bLinear = new double[1000];

        int j = 0;
        for (int i = 0; i < bLinear.length; i++) {
            aLinear[j] = (double) i / 1000d;
            bLinear[j] = aLinear[j];
            j++;
        }

        for (double a : aLinear) {
            for (double b : bLinear) {
                double logSumExact = LogMath.logSumExact(Math.log(a), Math.log(b));
                Assert.assertEquals("a=" + a + " b=" + b + " lin=" + Math.log(a + b),
                        logSumExact,
                        LogMath.logSum(Math.log(a), Math.log(b)), 0.001);
            }
        }
    }

    @Test
    @Ignore("Not a unit test")
    public void logSumPerf() throws IOException {
        double[] loga = new double[50000];
        double[] logb = new double[50000];

        int j = 0;
        for (int i = 0; i < logb.length; i++) {
            loga[j] = Math.log((double) i / 100000d);
            logb[j] = loga[j];
            j++;
        }

        Stopwatch sw = Stopwatch.createStarted();
        for (double a : loga) {
            for (double b : logb) {
                LogMath.logSumExact(a, b);
            }
        }
        System.out.println("Exact: " + sw.elapsed(TimeUnit.MILLISECONDS));
        sw.reset().start();
        for (double a : loga) {
            for (double b : logb) {
                LogMath.logSum(a, b);
            }
        }
        System.out.println("Lookup: " + sw.elapsed(TimeUnit.MILLISECONDS));
        sw.stop();
    }

    @Test
    public void logSumError() throws IOException {

        int VALS = 10000000;

        double[] logA = new double[VALS];

        for (int a = 0; a < VALS; a++) {
            if (a == 0)
                logA[a] = LogMath.LOG_ZERO;
            else
                logA[a] = Math.log((double) a / VALS);
        }

        Stopwatch sw = Stopwatch.createStarted();

        double maxError = 0;
        double a = 0;
        double b = 0;

        for (int i = 0; i < logA.length; i++) {
            double la = logA[i];
            double lb = logA[logA.length - i - 1];
            double exact = LogMath.logSumExact(la, lb);
            double approx = LogMath.logSum(la, lb);
            double error = Math.abs(exact - approx);
            if (error > maxError) {
                maxError = error;
                a = la;
                b = lb;
            }
        }

        System.out.println("Max error: " + maxError);
        System.out.println("Max error values: " + a + ":" + b);
        Assert.assertTrue(maxError < 0.0005);
        System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));
        sw.stop();
    }

    @Test
    public void logSumErrorFloat() throws IOException {

        int VALS = 10000000;

        float[] logA = new float[VALS];

        for (int a = 0; a < VALS; a++) {
            if (a == 0)
                logA[a] = LogMath.LOG_ZERO_FLOAT;
            else
                logA[a] = (float) Math.log((double) a / VALS);
        }

        Stopwatch sw = Stopwatch.createStarted();

        float maxError = 0;
        float a = 0;
        float b = 0;

        for (int i = 0; i < logA.length; i++) {
            float la = logA[i];
            float lb = logA[logA.length - i - 1];
            float exact = (float) LogMath.logSumExact(la, lb);
            float approx = LogMath.logSum(la, lb);
            float error = Math.abs(exact - approx);
            if (error > maxError) {
                maxError = error;
                a = la;
                b = lb;
            }
        }

        System.out.println("Max error: " + maxError);
        System.out.println("Max error values: " + a + ":" + b);
        Assert.assertTrue(maxError < 0.0005);
        System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));
        sw.stop();
    }    
    
    @Test
    public void testLog2() {
        Assert.assertEquals(2, (int) LogMath.log2(4));
        Assert.assertEquals(3, (int) LogMath.log2(8));
        Assert.assertEquals(-1, (int) LogMath.log2(0.5));
    }
}
