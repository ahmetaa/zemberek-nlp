package zemberek.langid.model;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

import java.io.*;
import java.util.Map;

public class MapBasedCharNgramLanguageModel extends BaseCharNgramModel implements Serializable, CharNgramLanguageModel {

    private transient CharNgramCountModel gramCounts;
    GramLogProbData[] gramLogProbs;
    public static double BACK_OFF = -2;

    private static final long serialVersionUID = 0xDEADBEEFCAFEL;

    public MapBasedCharNgramLanguageModel(int order, String modelId, GramLogProbData[] gramLogProbs) {
        super(modelId, order);
        this.gramLogProbs = gramLogProbs;
    }

    public MapBasedCharNgramLanguageModel(CharNgramCountModel gramCounts) {
        super(gramCounts.id, gramCounts.order);
        this.gramCounts = gramCounts;
        gramLogProbs = new GramLogProbData[order + 1];
        for (int i = 1; i < order + 1; i++) {
            gramLogProbs[i] = new GramLogProbData();
        }
    }

    public MapBasedCharNgramLanguageModel(String modelId, int order) {
        super(modelId, order);
        gramCounts = new CharNgramCountModel(modelId, order);
        gramLogProbs = new GramLogProbData[order + 1];
        for (int i = 1; i < order + 1; i++) {
            gramLogProbs[i] = new GramLogProbData();
        }
    }

    public static MapBasedCharNgramLanguageModel train(CharNgramCountModel countModel) {
        MapBasedCharNgramLanguageModel m = new MapBasedCharNgramLanguageModel(countModel);
        for (int i = 1; i <= m.order; ++i) {
            m.calculateProbabilities(i);
        }
        return m;
    }

    public void calculateProbabilities(int o) {
        Map<String, Double> frequencyMap = gramLogProbs[o].values;
        if (o == 1) {
            int total = gramCounts.totalCount(1);
            for (String s : gramCounts.getKeyIterator(o)) {
                double prob = Math.log((double) gramCounts.getCount(o, s) / (double) total);
                frequencyMap.put(s, prob);
            }
        } else {
            for (String s : gramCounts.getKeyIterator(o)) {
                final String parentGram = s.substring(0, o - 1);
                if (!gramCounts.containsKey(o - 1, parentGram))
                    continue;
                int cnt = gramCounts.getCount(o - 1, parentGram);
                double prob = Math.log((double) gramCounts.getCount(o, s) / (double) cnt);
                frequencyMap.put(s, prob);
            }
        }
    }

    public double gramProbability(String gram) {
        if (gram.length() == 0)
            return -10;
        if (gram.length() > order)
            throw new IllegalArgumentException("Gram size is larger than order! gramSize=" + gram.length() + " but order is:" + order);
        int o = gram.length();
        if (gramLogProbs[o].values.containsKey(gram))
            return gramLogProbs[o].values.get(gram);
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

    public void saveCustom(File f) throws IOException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            dos.writeInt(order);
            dos.writeUTF(id);
            for (int i = 1; i < gramLogProbs.length; i++) {
                Map<String, Double> probs = gramLogProbs[i].values;
                dos.writeInt(probs.size());
                for (String key : probs.keySet()) {
                    dos.writeUTF(key);
                    dos.writeFloat(probs.get(key).floatValue());
                }
            }
        } finally {
            Closeables.close(dos, true);
        }
    }

    public static MapBasedCharNgramLanguageModel loadCustom(InputStream f) throws IOException {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(f));
            int order = dis.readInt();
            String modelId = dis.readUTF();
            GramLogProbData[] logProbs = new GramLogProbData[order + 1];
            for (int j = 1; j <= order; j++) {
                int size = dis.readInt();
                Map<String, Double> probs = Maps.newHashMap();
                for (int i = 0; i < size; i++) {
                    String key = dis.readUTF();
                    probs.put(key, (double) dis.readFloat());
                }
                logProbs[j] = new GramLogProbData(probs);

            }
            return new MapBasedCharNgramLanguageModel(order, modelId, logProbs);
        } finally {
            Closeables.close(dis, true);
        }
    }

    public static MapBasedCharNgramLanguageModel loadCustom(File f) throws IOException {
        return loadCustom(new FileInputStream(f));
    }

    public void dump() {
        System.out.println("Model ID=" + id + " Order=" + order);
        for (int i = 1; i < gramLogProbs.length; i++) {
            System.out.println(gramLogProbs[i].values.size());
        }
    }

    static class GramLogProbData implements Serializable {
        Map<String, Double> values = Maps.newHashMap();

        GramLogProbData() {
        }

        GramLogProbData(Map<String, Double> values) {
            this.values = values;
        }
    }
}