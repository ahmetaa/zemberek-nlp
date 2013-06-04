package zemberek.core.quantization;

public class LinearQuantizer implements Quantizer {
    private final double min, max;
    private final int range;
    private final double quantizationStep;
    private double[] quantized;

    private LinearQuantizer(double min, double max, int range) {
        if (min > max)
            throw new IllegalArgumentException("Min value cannot be larger than max value. min:" + min + "  and max:" + max);
        this.min = min;
        this.max = max;
        this.range = range;
        this.quantizationStep = Math.abs(max - min) / (range - 1);
        quantized = new double[range];
        for (int i = 0; i < quantized.length; i++) {
            quantized[i] = min + (quantizationStep * i);
        }
    }

    /**
     * Creates a LinearQuantizer with given min max and bit count for quantizationStep count.
     *
     * @param min  min value
     * @param max  max value
     * @param bits quantizationStep count in bits. quantizer will have 2^bitCount steps.
     * @return LinearQuantizer with given paramters.
     */
    public static LinearQuantizer getByBitRange(double min, double max, int bits) {
        if (bits < 2 || bits > 24)
            throw new IllegalArgumentException("Bit count cannot be less than 1 or larger than 24" + bits);
        return new LinearQuantizer(min, max, 1 << bits);
    }

    /**
     * Creates a LinearQuantizer with given min max and range.
     *
     * @param min       min value
     * @param max       max value
     * @param stepCount defines how many steps Quantizer will have..
     * @return LinearQuantizer with given paramters.
     */
    public static LinearQuantizer getByStepCount(double min, double max, int stepCount) {
        if (stepCount < 3 || stepCount > 1 << 24)
            throw new IllegalArgumentException("Step count cannot be less than 3 or larger than 2^24 but it is" + stepCount);
        if (min >= max)
            throw new IllegalArgumentException("Min value cannot be larger than max value. min:" + min + "  and max:" + max);
        return new LinearQuantizer(min, max, stepCount);
    }


    /**
     * Applies linear quantization. Returns a value between 0..stepCount-1
     * it selects the closest integer approximate value by checking the distance between quantization values to minimize
     * quantization error.
     *
     * @param d input to quantize.
     * @return an integer in the range of  0..(2^bitCount-1)
     * @throws IllegalArgumentException if value is less than min or larger than max value.
     */
    public int getQuantizationIndex(double d) {
        if (d < min || d > max)
            throw new IllegalArgumentException(
                    "Value is out of quantization limits. min:" + min + " max:" + max + " but it is " + d);
        if (d == max)
            return range - 1;
        int low = (int) (Math.abs(d - min) / quantizationStep);
        double v1 = quantized[low];
        double dist = Math.abs(v1 - d);
        if (dist < quantizationStep - dist)
            return low;
        else
            return low + 1;
    }

    public double getQuantizedValue(double value) {
        throw new UnsupportedOperationException("This method is not supported in Linear Quantizer yet");
    }

    public DoubleLookup getDequantizer() {
        return new DoubleLookup(quantized);
    }

    /**
     * Minimum dequantization value.
     *
     * @return Minimum dequantization value.
     */
    public double getMin() {
        return min;
    }

    /**
     * Maximum dequantization value.
     *
     * @return Maximum dequantization value.
     */
    public double getMax() {
        return max;
    }

    public int getRange() {
        return range;
    }

    public double getQuantizationStep() {
        return quantizationStep;
    }
}
