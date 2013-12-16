package zemberek.core.quantization;

import com.google.common.primitives.Ints;
import zemberek.core.math.DoubleArrays;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This applies a k-means like algorithm to the data points for quantization.
 * It randomly chooses cluster centers form actual data.
 * After that it re-assgins the mean value to the
 * center of mass of the cluster points. If a cluster has no data points, it assigns the mean value as (most crowded-data-point-mean+mean)/2
 * it iterates 15 times to find the final mean values and uses them as quatization points.
 */
public class KMeansQuantizer implements Quantizer {

    double data[];
    double sorted[];
    Map<Double, Integer> lookup = new HashMap<>();
    final int range;

    private KMeansQuantizer(double[] data, int range, Map<Double, Integer> lookup) {
        this.data = data;
        this.range = range;
        this.lookup = lookup;
        this.sorted = data.clone();
        Arrays.sort(sorted);
    }


    public static KMeansQuantizer generateFromRawData(float[] dataToQuantize, int bits) {
        return generateFromRawData(DoubleArrays.convert(dataToQuantize), bits);
    }

    /**
     * Creates a KMeansQuantizer with given histogram and bitsize.
     *
     * @param dataToQuantize input dataToQuantize to quantize.
     * @param bits           quantization level amount in bits. There will be 2^bits level.
     * @return A Quantizer.
     */
    public static KMeansQuantizer generateFromRawData(double[] dataToQuantize, int bits) {
        if (bits < 4 || bits > 24)
            throw new IllegalArgumentException("Bit count cannot be less than 4 or larger than 24" + bits);
        int range = 1 << bits;

        Map<Double, Integer> lookup = new HashMap<>();

        final int dataLength = dataToQuantize.length;
        if (range >= dataLength) {
            double[] means = new double[dataLength];
            int i = 0;
            for (double v : dataToQuantize) {
                lookup.put(v, i);
                means[i] = v;
                i++;
            }
            return new KMeansQuantizer(means, dataLength, lookup);
        }
        return kMeans(dataToQuantize, range, 10);
    }

    public int getQuantizationIndex(double value) {
        if (!lookup.containsKey(value)) {
            throw new IllegalArgumentException("cannot quantize value. Value does not exist in quantization lookup:" + value);
        }
        return lookup.get(value);
    }

    public double getQuantizedValue(double value) {
        return data[lookup.get(value)];
    }

    public DoubleLookup getDequantizer() {
        return new DoubleLookup(data);
    }

    private static KMeansQuantizer kMeans(double[] data, int clusterCount, int iterationCount) {
        double[] means = new double[clusterCount];

        Map<Double, Integer> lookup = new HashMap<>();

        int indexes[] = new int[data.length];

        //initialization. means are placed using random data.
        MeanCount[] meanCounts = new MeanCount[clusterCount];
        Random r = new Random();
        for (int i = 0; i < clusterCount; i++) {
            means[i] = data[r.nextInt(data.length)];
            meanCounts[i] = new MeanCount(i, 0);
        }

        for (int j = 0; j < iterationCount; j++) {
            // cluster points.
            for (int i = 0; i < data.length; i++) {
                int closestMeanIndex = -1;
                double m = Double.POSITIVE_INFINITY;
                for (int k = 0; k < means.length; k++) {
                    double dif = Math.abs(data[i] - means[k]);
                    if (dif < m) {
                        m = dif;
                        closestMeanIndex = k;
                    }
                }
                indexes[i] = closestMeanIndex;
                meanCounts[closestMeanIndex].count++;
            }

            Arrays.sort(meanCounts);

            // update means
            for (int k = 0; k < means.length; k++) {
                int pointCount = 0;
                double meanTotal = 0;
                for (int i = 0; i < data.length; i++) {
                    if (indexes[i] == k) {
                        pointCount++;
                        meanTotal += data[i];
                    }
                }
                // if there is no point in one cluster,reassign the mean value of the empty cluster to
                // (most crowded cluster mean + empty cluseter mean ) /2
                if (pointCount > 0) {
                    means[k] = meanTotal / pointCount;
                } else {
                    double m = (means[k] + means[meanCounts[0].index]) / 2;
                    means[k] = m;
                }
            }
        }
        int i = 0;
        // generate lookup for quantization.
        for (int index : indexes) {
            lookup.put(data[i++], index);
        }
        return new KMeansQuantizer(means, means.length, lookup);
    }

    private static class MeanCount implements Comparable<MeanCount> {
        int index;
        int count;

        MeanCount(int index, int count) {
            this.index = index;
            this.count = count;
        }

        public int compareTo(MeanCount o) {
            return -Ints.compare(count, o.count);
        }
    }
}
