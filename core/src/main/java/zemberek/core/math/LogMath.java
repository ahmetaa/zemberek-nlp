package zemberek.core.math;

public class LogMath {

    private static final double[] logSumLookup = new double[30000];
    private static final double SCALE = 1000d;

    public static final double LOG_ZERO = -Math.log(Double.MAX_VALUE);
    public static final double LOG_ONE = 0;
    public static final double LOG_TEN = Math.log(10);
    public static final double LOG_TWO = Math.log(2);

    // do not allow instantiation
    private LogMath() {
    }

    // initialization of the log-sum lookup. it populates an array with arr[i] = log(1+exp(-i/SCALE))
    static {
        for (int i = 0; i < logSumLookup.length; i++) {
            logSumLookup[i] = Math.log(1.0 + Math.exp((double) -i / SCALE));
        }
    }

    /**
     * Calculates an approximation of log(a+b) when log(a) and log(b) are given using the formula
     * <p><b>log(a+b) = log(b) + log(1 + exp(log(a)-log(b)))</b> where log(b)>log(a)
     * <p>This method is an approximation because it uses a lookup table for <b>log(1 + exp(log(b)-log(a)))</b> part
     * <p>This is useful for log-probabilities where values vary between -30 < log(p) <= 0
     * <p>if difference between values is larger than 20 (which means sum of the numbers will be very close to the larger
     * value in linear domain) large value is returned instead of the logSum calculation because effect of the other
     * value is negligible
     *
     * @param logA logarithm of A
     * @param logB logarithm of B
     * @return approximation of log(A+B)
     */
    public static double logSum(double logA, double logB) {
        if (logA > logB) {
            final double dif = logA - logB; // logA-logB because during lookup calculation dif is multiplied with -1
            return dif >= 30d ? logA : logA + logSumLookup[(int) (dif * SCALE)];
        } else {
            final double dif = logB - logA;
            return dif >= 30d ? logB : logB + logSumLookup[(int) (dif * SCALE)];
        }
    }

    /**
     * Calculates approximate logSum of log values using the <code> logSum(logA,logB) </code>
     *
     * @param logValues log values to use in logSum calculation.
     * @return <p>log(a+b) value approximation
     */
    public static double logSum(double... logValues) {
        double result = LOG_ZERO;
        for (double logValue : logValues) {
            result = logSum(result, logValue);
        }
        return result;
    }

    /**
     * Exact calculation of log(a+b) using log(a) and log(b) with formula
     * <p><b>log(a+b) = log(b) + log(1 + exp(log(b)-log(a)))</b> where log(b)>log(a)
     *
     * @param logA logarithm of A
     * @param logB logarithm of B
     * @return approximation of log(A+B)
     */
    public static double logSumExact(double logA, double logB) {
        if (Double.isInfinite(logA))
            return logB;
        if (Double.isInfinite(logB))
            return logA;
        if (logA > logB) {
            double dif = logA - logB;
            return dif >= 30d ? logA : logA + Math.log(1 + Math.exp(-dif));
        } else {
            double dif = logB - logA;
            return dif >= 30d ? logB : logB + Math.log(1 + Math.exp(-dif));
        }
    }

    /**
     * Calculates approximate logSum of log values using the <code> logSumExact(logA,logB) </code>
     *
     * @param logValues log values to use in logSum calculation.
     * @return </p>log(a+b) value approximation
     */
    public static double logSumExact(double... logValues) {
        double result = LOG_ZERO;
        for (double logValue : logValues) {
            result = logSumExact(result, logValue);
        }
        return result;
    }

    public static double linearToLog(double linear) {
        if (linear == 0)
            return LOG_ZERO;
        return Math.log(linear);
    }

    public static double[] linearToLog(double... linear) {
        double[] result = new double[linear.length];
        for (int i = 0; i < linear.length; i++) {
            result[i] = linearToLog(linear[i]);
        }
        return result;
    }

    public static void linearToLogInPlace(double... linear) {
        for (int i = 0; i < linear.length; i++) {
            linear[i] = linearToLog(linear[i]);
        }
    }

    /**
     * Calculates 2 base logarithm
     *
     * @param input value to calculate log
     * @return 2 base logarithm of the input
     */
    public static double log2(double input) {
        return Math.log(input) / LOG_TWO;
    }

    /**
     * convert a value which is in log10 base to Log base.
     *
     * @param log10Value loog10 value.
     * @return loge values
     */
    public static double log10ToLog(double log10Value) {
        return log10Value * LOG_TEN;
    }
}

