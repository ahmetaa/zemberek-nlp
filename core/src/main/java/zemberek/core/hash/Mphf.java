package zemberek.core.hash;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface Mphf {
    int get(int[] key);

    int get(int[] key, int hash);

    int get(String key);

    int get(int k0, int k1, int k2, int initialHash);

    int get(int k0, int k1, int initialHash);

    int get(String key, int hash);

    int get(int[] ngram, int begin, int end, int hash);

    void serialize(File file) throws IOException;

    void serialize(OutputStream os) throws IOException;

    double averageBitsPerKey();

    int size();
}
