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
}
