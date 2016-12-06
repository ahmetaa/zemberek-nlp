package zemberek.lm.compression;

import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.quantization.BinningQuantizer;
import zemberek.core.quantization.KMeansQuantizer;
import zemberek.core.quantization.Quantizer;
import zemberek.core.quantization.QuantizerType;

import java.io.*;

public class BinaryFloatFileReader {

    File file;
    int count;
    RandomAccessFile raf;

    public BinaryFloatFileReader(File file) throws IOException {
        this.file = file;
        raf = new RandomAccessFile(file, "r");
        count = raf.readInt();
    }

    public DataInputStream getStream() throws FileNotFoundException {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(file), 1000000));
    }

    public void getFloat(int index, float[] data) throws IOException {
        raf.seek(index * 4);
        for (int i = 0; i < data.length; i++) {
            data[i] = raf.readFloat();
        }
    }

    public static Quantizer getQuantizer(File file, int bitCount, QuantizerType quantizerType) throws IOException {
        BinaryFloatFileReader reader = new BinaryFloatFileReader(file);
        try (DataInputStream dis = reader.getStream()) {
            dis.skipBytes(4); // skip the count.
            LookupCalculator lookupCalc = new LookupCalculator(bitCount);
            for (int i = 0; i < reader.count; i++) {
                double d = dis.readFloat();
                lookupCalc.add(d);
                if (Log.isDebug() && i % 500000 == 0) {
                    Log.debug("Values added to value histogram = %d", i);
                }
            }
            return lookupCalc.getQuantizer(quantizerType);
        }
    }

    private static class LookupCalculator {
        int bitCount;
        int n;
        Histogram<Double> histogram;

        LookupCalculator(int bitCount) throws IOException {
            this.bitCount = bitCount;
            this.n = 1 << bitCount;
            histogram = new Histogram<>(n / 2);
        }

        void add(double d) {
            histogram.add(d);
        }

        public Quantizer getQuantizer(QuantizerType type) {
            Log.info("Unique value count:" + histogram.size());
            double[] lookup = new double[histogram.size()];
            int[] counts = new int[histogram.size()];
            int j = 0;
            for (double key : histogram) {
                lookup[j] = key;
                counts[j] = histogram.getCount(key);
                j++;
            }
            Log.info("Quantizing to " + bitCount + " bits");

            switch (type) {
                case BINNING:
                    return BinningQuantizer.linearBinning(lookup, bitCount);
                case BINNING_WEIGHTED:
                    return BinningQuantizer.logCountBinning(lookup, counts, bitCount);
                case KMEANS:
                    return KMeansQuantizer.generateFromRawData(lookup, bitCount);
                default:
                    throw new UnsupportedOperationException("Linear cannot be used in this operation");
            }
        }

        public int getSize() {
            return histogram.size();
        }
    }
}
