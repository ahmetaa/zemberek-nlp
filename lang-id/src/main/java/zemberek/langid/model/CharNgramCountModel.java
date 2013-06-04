package zemberek.langid.model;

import com.google.common.io.Closeables;
import zemberek.core.Histogram;

import java.io.*;
import java.util.List;

/**
 * This model holds the counts of char ngrams.
 */
public class CharNgramCountModel extends BaseCharNgramModel implements Serializable {

    private Histogram<String>[] gramCounts;

    private static final long serialVersionUID = 0xBEEFCAFEABCDL;

    public CharNgramCountModel(String modelId, int order) {
        super(modelId, order);
        gramCounts = new Histogram[order + 1];
        for (int i = 0; i < gramCounts.length; i++) {
            gramCounts[i] = new Histogram<String>();
        }
    }

    private CharNgramCountModel(String id, int order, Histogram<String>[] gramCounts) {
        super(id, order);
        this.gramCounts = gramCounts;
    }

    /**
     * Loads data from the custom serialized file and generates a CharNgramCountModel from it.
     *
     * @param f file to load data.
     * @return a CharNgramCountModel generated from file.
     * @throws java.io.IOException
     */
    public static CharNgramCountModel load(File f) throws IOException {
        return load(new FileInputStream(f));
    }

    /**
     * Loads data from the custom serialized file and generates a CharNgramCountModel from it.
     *
     * @param is InputStream to load data.
     * @return a CharNgramCountModel generated from file.
     * @throws java.io.IOException
     */
    public static CharNgramCountModel load(InputStream is) throws IOException {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(is));
            int order = dis.readInt();
            String modelId = dis.readUTF();
            Histogram<String>[] gramCounts = new Histogram[order + 1];
            for (int j = 1; j <= order; j++) {
                int size = dis.readInt();
                Histogram<String> countSet = new Histogram<String>(size);
                for (int i = 0; i < size; i++) {
                    String key = dis.readUTF();
                    countSet.add(key, dis.readInt());
                }
                gramCounts[j] = countSet;

            }
            return new CharNgramCountModel(modelId, order, gramCounts);
        } finally {
            Closeables.close(dis, true);
        }
    }

    /**
     * A custom serializer. Big-endian format is like this:
     * int32 order
     * Utf id
     * int32 keyCount
     * utf key
     * int32 count
     * ...
     * int32 keyCount
     * utf key
     * int32 count
     * ...
     *
     * @param f file to serialize.
     * @throws java.io.IOException
     */
    public void save(File f) throws IOException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            dos.writeInt(order);
            dos.writeUTF(id);
            for (int i = 1; i < gramCounts.length; i++) {
                dos.writeInt(gramCounts[i].size());
                for (String key : gramCounts[i]) {
                    dos.writeUTF(key);
                    dos.writeInt(gramCounts[i].getCount(key));
                }
            }
        } finally {
            Closeables.close(dos, true);
        }
    }


    public void addGrams(String seq) {
        for (int i = 1; i <= order; ++i) {
            List<String> grams = this.getGram(seq, i);
            for (String gram : grams) {
                gramCounts[i].add(gram);
            }
        }
    }

    public void applyCutOffs(int[] cutOffs) {
        if (cutOffs == null)
            return;
        if (cutOffs.length > order)
            throw new IllegalArgumentException("Cannot apply cutoff values. Cutoff array length " + cutOffs.length
                    + " is larger than the order of the model " + order);
        if (cutOffs.length > 0) {
            for (int i = 0; i < cutOffs.length; i++) {
                int o = i + 1;
                System.out.println(o + " gram Count Before cut off: " + keyCount(o));
                removeSmaller(o, cutOffs[i] + 1);
                System.out.println(o + " gram Count After cut off: " + keyCount(o));
            }
        }
    }

    public void merge(CharNgramCountModel otherModel) {
        if (otherModel.order != order) {
            throw new IllegalArgumentException("Model orders does not match. Order of this model is" + order +
                    " but merged model order is " + otherModel.order);
        }
        for (int i = 1; i < gramCounts.length; i++) {
            gramCounts[i].add(otherModel.gramCounts[i]);
        }
    }

    public int getCount(int order, String key) {
        return gramCounts[order].getCount(key);
    }

    public void add(int order, String key) {
        gramCounts[order].add(key);
    }

    public int keyCount(int order) {
        return gramCounts[order].size();
    }

    public boolean containsKey(int order, String key) {
        return gramCounts[order].contains(key);
    }

    public int removeSmaller(int order, int size) {
        return gramCounts[order].removeSmaller(size);
    }

    public int totalCount(int order) {
        return gramCounts[order].countAll();
    }

    public Iterable<String> getKeyIterator(int order) {
        return gramCounts[order];
    }
}
