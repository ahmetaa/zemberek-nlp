package zemberek.core.math;

public class LogMath {

    public static final double LOG_ZERO = -Math.log(Double.MAX_VALUE);
    public static final double LOG_ONE = 0;
    public static final double LOG_TEN = Math.log(10);
    public static final double LOG_TWO = Math.log(2);
    public static final double INVERSE_LOG_TWO = 1 / Math.log(2);

    public static final float LOG_ZERO_FLOAT = (float) -Math.log(Float.MAX_VALUE);
    public static final float LOG_ONE_FLOAT = 0;
    public static final float LOG_TEN_FLOAT = (float) Math.log(10);
    public static final float LOG_TWO_FLOAT = (float) Math.log(2);

    // Double value log sum lookup base Math.E
    public static final LogSumLookup LOG_SUM = new LogSumLookup(Math.E);
    // Float value log sum lookup base Math.E
    public static final LogSumLookupFloat LOG_SUM_FLOAT = new LogSumLookupFloat(Math.E);

    // Double value linear to Log value converter for base Math.E
    public static final LinearToLogConverter LINEAR_TO_LOG = new LinearToLogConverter(Math.E);
    // Float value linear to Log value converter for base Math.E
    public static final LinearToLogConverterFloat LINEAR_TO_LOG_FLOAT = new LinearToLogConverterFloat(Math.E);

    // do not allow instantiation
    private LogMath() {
    }

    /**
     * Exact calculation of log(a+b) using log(a) and log(b) with formula
     * <p><b>log(a+b) = log(b) + log(1 + exp(log(b)-log(a)))</b> where log(b)>log(a)
     *
     * @param logA logarithm of A
     * @param logB logarithm of B
     * @return approximation of log(A+B)
     */
    public static double logSum(double logA, double logB) {
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
     * Exact calculation of log10(a+b) using log10(a) and log10(b) with formula
     * <p><b>log10(a+b) = log10(b) + log10(1 + 10^(log(b)-log(a)))</b> where log(b)>log(a)
     *
     * @param log10A 10 base logarithm of A
     * @param log10B 10 base logarithm of B
     * @return approximation of log(A+B)
     */
    public static double logSum10(double log10A, double log10B) {
        if (Double.isInfinite(log10A))
            return log10B;
        if (Double.isInfinite(log10B))
            return log10A;
        if (log10A > log10B) {
            double dif = log10A - log10B;
            return dif >= 30d ? log10A : log10A + Math.log10(1 + Math.pow(10, -dif));
        } else {
            double dif = log10B - log10A;
            return dif >= 30d ? log10B : log10B + Math.log10(1 + Math.pow(10, -dif));
        }
    }

    /**
     * Calculates exact logSum of log values using the <code> logSum(logA,logB) </code>
     *
     * @param logValues log values to use in logSum calculation.
     * @return </p>log(a+b) value approximation
     */
    public static double logSum(double... logValues) {
        double result = LOG_ZERO;
        for (double logValue : logValues) {
            result = logSum(result, logValue);
        }
        return result;
    }

    /**
     * A lookup structure for approximate logSum calculation.
     */
    public static class LogSumLookupFloat {
        public static final float DEFAULT_SCALE = 1000f;
        public static final int DEFAULT_LOOKUP_SIZE = 5000;

        private final float[] lookup;
        public final float scale;

        public LogSumLookupFloat(double base) {
            this(base, DEFAULT_LOOKUP_SIZE, DEFAULT_SCALE);
        }

        public LogSumLookupFloat(double base, int lookupSize, float scale) {
            this.scale = scale;
            this.lookup = new float[lookupSize];
            for (int i = 0; i < lookup.length; i++) {
                lookup[i] = (float) log(base, 1.0 + Math.pow(base, (double) -i / scale));
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
        public float lookup(float logA, float logB) {
            if (logA > logB) {
                final float dif = logA - logB; // logA-logB because during lookup calculation dif is multiplied with -1
                return dif >= 5f ? logA : logA + lookup[(int) (dif * scale)];
            } else {
                final float dif = logB - logA;
                return dif >= 5f ? logB : logB + lookup[(int) (dif * scale)];
            }
        }

        /**
         * Calculates approximate logSum of log values using the <code> logSum(logA,logB) </code>
         *
         * @param logValues log values to use in logSum calculation.
         * @return <p>log(a+b) value approximation
         */
        public float lookup(float... logValues) {
            float result = LOG_ZERO_FLOAT;
            for (float logValue : logValues) {
                result = lookup(result, logValue);
            }
            return result;
        }
    }


    /**
     * A lookup structure for approximate logSum calculation.
     */
    public static class LogSumLookup {
        public static final double DEFAULT_SCALE = 1000d;
        public static final int DEFAULT_LOOKUP_SIZE = 20000;

        private final double[] lookup;
        public final double scale;

        public LogSumLookup(double base) {
            this(base, DEFAULT_LOOKUP_SIZE, DEFAULT_SCALE);
        }

        public LogSumLookup(double base, int lookupSize, double scale) {
            this.scale = scale;
            this.lookup = new double[lookupSize];
            for (int i = 0; i < lookup.length; i++) {
                lookup[i] = log(base, 1.0 + Math.pow(base, (double) -i / scale));
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
        public double lookup(double logA, double logB) {
            if (logA > logB) {
                final double dif = logA - logB; // logA-logB because during lookup calculation dif is multiplied with -1
                return dif >= 20d ? logA : logA + lookup[(int) (dif * scale)];
            } else {
                final double dif = logB - logA;
                return dif >= 20d ? logB : logB + lookup[(int) (dif * scale)];
            }
        }

        /**
         * Calculates approximate logSum of log values using the <code> logSum(logA,logB) </code>
         *
         * @param logValues log values to use in logSum calculation.
         * @return <p>log(a+b) value approximation
         */
        public double lookup(double... logValues) {
            double result = LOG_ZERO;
            for (double logValue : logValues) {
                result = lookup(result, logValue);
            }
            return result;
        }
    }

    /**
     * A converter class for converting linear values log values.
     */
    public static class LinearToLogConverter {
        public final double inverseLogBase;

        public LinearToLogConverter(double base) {
            if (base == 0)
                throw new IllegalArgumentException("Base of the logarithm cannot be zero.");
            this.inverseLogBase = (float) (1 / Math.log(base));
        }

        public double convert(double linear) {
            return Math.log(linear) * inverseLogBase;
        }

        public double[] convert(double... linear) {
            double[] result = new double[linear.length];
            for (int i = 0; i < linear.length; i++) {
                result[i] = convert(linear[i]);
            }
            return result;
        }

        public void convertInPlace(double... linear) {
            for (int i = 0; i < linear.length; i++) {
                linear[i] = convert(linear[i]);
            }
        }
    }


    /**
     * A converter class for converting linear values log values.
     */
    public static class LinearToLogConverterFloat {

        public final float inverseLogOfBase;

        public LinearToLogConverterFloat(double base) {
            if (base == 0)
                throw new IllegalArgumentException("Base of the logarithm cannot be zero.");
            this.inverseLogOfBase = (float) (1 / Math.log(base));
        }

        public float convert(float linear) {
            return (float) Math.log(linear) * inverseLogOfBase;
        }

        public float[] convert(float... linear) {
            float[] result = new float[linear.length];
            for (int i = 0; i < linear.length; i++) {
                result[i] = convert(linear[i]);
            }
            return result;
        }

        public void convertInPlace(float... linear) {
            for (int i = 0; i < linear.length; i++) {
                linear[i] = convert(linear[i]);
            }
        }
    }

    /**
     * Calculates logarithm in any base.
     */
    public static double log(double base, double val) {
        return Math.log(val) / Math.log(base);
    }

    /**
     * Calculates 2 base logarithm
     *
     * @param input value to calculate log
     * @return 2 base logarithm of the input
     */
    public static double log2(double input) {
        return Math.log(input) * INVERSE_LOG_TWO;
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

    private static final double SPHINX_4_LOG_BASE = 1.0001;
    private static final double INVERSE_LOG_SPHINX_BASE = 1 / Math.log(SPHINX_4_LOG_BASE);

    /**
     * Converts a log value to Sphinx4 log base. Can be used for comparison.
     *
     * @param logValue value in natural logarithm
     * @return value in Sphinx4 log base.
     */
    public static double toLogSphinx(double logValue) {
        return logValue * INVERSE_LOG_SPHINX_BASE;
    }
}
