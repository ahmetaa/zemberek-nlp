package zemberek.core.quantization;

import com.google.common.primitives.Doubles;
import zemberek.core.math.DoubleArrays;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A Quantizer that applies binning algorithm to input values to put them in a smaller range.
 * There are two binning implementation
 */
public class BinningQuantizer implements Quantizer {

    Map<Double, Integer> lookup = new HashMap<>();
    double[] means;

    public BinningQuantizer(Map<Double, Integer> lookup, double[] means) {
        this.lookup = lookup;
        this.means = means;
    }

    double calculateError(double[] data, int[] counts) {
        double totalError = 0;
        int i = 0;
        for (double v : data) {
            totalError += Math.abs(v - means[lookup.get(v)]) * counts[i];
            i++;
        }
        return totalError;
    }

    double calculateError(double[] data) {
        double totalError = 0;
        int i = 0;
        for (double v : data) {
            totalError += Math.abs(v - means[lookup.get(v)]);
            i++;
        }
        return totalError;
    }

    public static BinningQuantizer linearBinning(float[] dataToQuantize, int bits) {
        return linearBinning(DoubleArrays.convert(dataToQuantize), bits);
    }

    public static BinningQuantizer linearBinning(double[] dataToQuantize, int bits) {
        checkRange(bits);
        final int dataLength = dataToQuantize.length;
        int range = 1 << bits;
        Map<Double, Integer> lookup = new HashMap<>(dataToQuantize.length);
        double means[] = new double[range];
        if (range >= dataLength) {
            means = new double[dataLength];
            int i = 0;
            for (double v : dataToQuantize) {
                lookup.put(v, i);
                means[i] = v;
                i++;
            }
            return new BinningQuantizer(lookup, means);
        }
        RawData[] data = new RawData[dataLength];
        for (int i = 0; i < data.length; i++) {
            data[i] = new RawData(dataToQuantize[i], i);
        }
        Arrays.sort(data);
        double binStep = (double) dataLength / range;
        int i = 0;
        int start = 0;
        double cursor = 0;
        int end = 0;

        while (cursor < dataLength) {
            start = (int) cursor;
            cursor += binStep;
            end = (int) cursor;
            if (end >= dataLength)
                end = dataLength;
            double total = 0;
            for (int k = start; k < end; k++) {
                total += data[k].value;
                lookup.put(data[k].value, i);
            }
            double mean = total / (end - start);
            means[i] = mean;
            i++;
        }
        return new BinningQuantizer(lookup, means);
    }

    private static void checkRange(int bits) {
        if (bits < 2 || bits > 24)
            throw new IllegalArgumentException("Bit count cannot be less than 4 or larger than 24" + bits);
    }

    public static BinningQuantizer logCountBinning(float[] dataToQuantize, int counts[], int bits) {
        return logCountBinning(DoubleArrays.convert(dataToQuantize), counts, bits);
    }

    public static BinningQuantizer logCountBinning(double[] dataToQuantize, int counts[], int bits) {
        checkRange(bits);
        int range = 1 << bits;
        Map<Double, Integer> lookup = new HashMap<>(dataToQuantize.length);
        final int dataLength = dataToQuantize.length;
        double means[] = new double[range];

        if (range >= dataLength) {
            means = new double[dataLength];
            int i = 0;
            for (double v : dataToQuantize) {
                lookup.put(v, i);
                means[i] = v;
                i++;
            }
            return new BinningQuantizer(lookup, means);
        }

        RawDataWithCount[] data = new RawDataWithCount[dataLength];

        int totalLogCount = 0;

        for (int i = 0; i < data.length; i++) {
            data[i] = new RawDataWithCount(dataToQuantize[i], i, counts[i]);
            totalLogCount += data[i].logCount;
        }

        Arrays.sort(data);

        int i = 0;
        int k = 0;

        while (k < range) {
            double binStep = (double) totalLogCount / (range - k);
            int binLogCountTotal = 0;

            int start = i;
            while (binLogCountTotal < binStep) {
                binLogCountTotal += data[i].logCount;
                i++;
            }
            int end = i;

            double binWeightedValueTotal = 0;
            for (int j = start; j < end; j++) {
                binWeightedValueTotal += (data[j].value * data[j].logCount);
                lookup.put(data[j].value, k);
            }
            double weightedAverage = binWeightedValueTotal / binLogCountTotal;
            means[k] = weightedAverage;

            totalLogCount -= binLogCountTotal;
            k++;
        }
        return new BinningQuantizer(lookup, means);
    }

    private static class RawData implements Comparable<RawData> {
        double value;
        int index;

        private RawData(double value, int index) {
            this.value = value;
            this.index = index;
        }

        public int compareTo(RawData o) {
            return Doubles.compare(value, o.value);
        }
    }

    private static class RawDataWithCount implements Comparable<RawDataWithCount> {
        double value;
        int index;
        int logCount;

        private RawDataWithCount(double value, int index, int count) {
            this.value = value;
            this.index = index;
            this.logCount = (int) (Math.log(count) + 1);
        }

        public int compareTo(RawDataWithCount o) {
            return Doubles.compare(value, o.value);
        }
    }

    public int getQuantizationIndex(double value) {
        return lookup.get(value);
    }

    public double getQuantizedValue(double value) {
        return means[lookup.get(value)];
    }

    public DoubleLookup getDequantizer() {
        return new DoubleLookup(means);
    }
}
