package zemberek.langid.model;

import com.google.common.collect.Lists;
import zemberek.core.collections.Histogram;
import zemberek.core.hash.IntHashKeyProvider;
import zemberek.core.hash.Mphf;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.core.quantization.BinningQuantizer;
import zemberek.core.quantization.DoubleLookup;
import zemberek.core.quantization.Quantizer;

import java.io.*;
import java.util.List;

/**
 * A compressed character N-gram model that uses Minimal Perfect Hash functions and Quantization.
 * System uses around 20 bits per n-gram. 8 Bit quantized probability value, 16 bit fingerprint for OOV detection and 3,5 bit for hash.
 */
public class CompressedCharNgramModel extends BaseCharNgramModel implements CharNgramLanguageModel {

    public static final int UNK_CHAR_PENALTY = -10;
    static final double BACK_OFF = -2;
    // all arrays below are 1 based
    Mphf[] mphfs;
    ProbData[] gramData;

    DoubleLookup[] lookups;
    static final int FINGER_PRINT_MASK = (1 << 16) - 1;


    private CompressedCharNgramModel(int order, String modelId, Mphf[] mphfs, ProbData[] gramData, DoubleLookup[] lookups) {
        super(modelId, order);
        this.mphfs = mphfs;
        this.gramData = gramData;
        this.lookups = lookups;
    }

    /**
     * Applies compression to a char language model using Minimal Perfect hash functions and quantization
     *
     * @param input  raw model file
     * @param output compressed file
     * @throws IOException
     */
    public static void compress(File input, File output) throws IOException {
        compress(MapBasedCharNgramLanguageModel.loadCustom(input), output);
    }

    public static void compress(MapBasedCharNgramLanguageModel model, File output) throws IOException {
        Mphf[] mphfs = new MultiLevelMphf[model.getOrder() + 1];
        DoubleLookup[] lookups = new DoubleLookup[model.getOrder() + 1];
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            dos.writeInt(model.getOrder());
            dos.writeUTF(model.getId());

            for (int i = 1; i <= model.getOrder(); i++) {
                Histogram<Double> histogram = new Histogram<>();
                histogram.add(model.gramLogProbs[i].values.values());
                double[] lookup = new double[histogram.size()];
                int j = 0;
                for (Double key : histogram) {
                    lookup[j] = key;
                    j++;
                }
                Quantizer quantizer = BinningQuantizer.linearBinning(lookup, 8);
                lookups[i] = quantizer.getDequantizer();
                List<String> keys = Lists.newArrayList(model.gramLogProbs[i].values.keySet());

                int[] fingerprints = new int[keys.size()];
                int[] probabilityIndexes = new int[keys.size()];

                mphfs[i] = MultiLevelMphf.generate(new StringListKeyProvider(keys));

                for (final String key : keys) {
                    final int index = mphfs[i].get(key);
                    fingerprints[index] = MultiLevelMphf.hash(key, -1) & FINGER_PRINT_MASK;
                    probabilityIndexes[index] = quantizer.getQuantizationIndex(
                            model.gramLogProbs[i].values.get(key));
                }

                lookups[i].save(dos);
                dos.writeInt(keys.size());
                for (int k = 0; k < keys.size(); k++) {
                    dos.writeShort(fingerprints[k] & 0xffff);
                    dos.writeByte(probabilityIndexes[k]);
                }
                mphfs[i].serialize(dos);
            }
        }
    }

    /**
     * simple stupid back-off probability calculation
     *
     * @param gram gram String
     * @return log Probability
     */
    public double gramProbability(String gram) {
        if (gram.length() == 0)
            return UNK_CHAR_PENALTY;
        if (gram.length() > order)
            throw new IllegalArgumentException("Gram size is larger than order! gramSize="
                    + gram.length() + " but order is:" + order);
        int o = gram.length();
        int fingerPrint = MultiLevelMphf.hash(gram, -1);
        int hash = mphfs[o].get(gram, fingerPrint);
        if ((fingerPrint & FINGER_PRINT_MASK) == gramData[o].getFP(hash)) {
            return lookups[o].get(gramData[o].getProbLookupIndex(hash));
        } else
            return BACK_OFF + gramProbability(gram.substring(0, o - 1));
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String getId() {
        return id;
    }

    public static CompressedCharNgramModel load(InputStream is) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(is))) {
            int order = dis.readInt();
            String modelId = dis.readUTF();
            MultiLevelMphf[] mphfs = new MultiLevelMphf[order + 1];
            ProbData[] probDatas = new ProbData[order + 1];
            DoubleLookup[] lookups = new DoubleLookup[order + 1];
            for (int i = 1; i <= order; i++) {
                lookups[i] = DoubleLookup.getLookup(dis);
                probDatas[i] = new ProbData(dis);
                mphfs[i] = MultiLevelMphf.deserialize(dis);
            }
            return new CompressedCharNgramModel(order, modelId, mphfs, probDatas, lookups);
        }
    }

    public static CompressedCharNgramModel load(File file) throws IOException {
        return load(new FileInputStream(file));
    }

    private static class ProbData {
        byte[] data;

        ProbData(DataInputStream dis) throws IOException {
            int count = dis.readInt();
            data = new byte[count * 3];
            dis.readFully(data);
        }

        int getFP(int index) {
            return ((data[index * 3] & 0xff) << 8) | (data[index * 3 + 1] & 0xff);
        }

        int getProbLookupIndex(int index) {
            return data[index * 3 + 2] & 0xff;
        }
    }

    private static class StringListKeyProvider implements IntHashKeyProvider {
        List<String> keys;

        public StringListKeyProvider(List<String> keys) {
            this.keys = keys;
        }

        @Override
        public int[] getKey(int index) {
            String key = keys.get(index);
            int[] arr = new int[key.length()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = key.charAt(i);
            }
            return arr;
        }

        @Override
        public int keyAmount() {
            return keys.size();
        }
    }


}
