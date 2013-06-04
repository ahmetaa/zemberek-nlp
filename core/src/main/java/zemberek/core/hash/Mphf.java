package zemberek.core.hash;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface Mphf {
    int get(int[] key);

    int get(int[] key, int hash);

    int get(long encodedKey, int order, int hash);

    int get(long encodedKey, int order);

    int get(String key);

    int get(String key, int hash);

    int get(int[] ngram, int begin, int end, int hash);

    void serialize(File file) throws IOException;

    void serialize(OutputStream os) throws IOException;
}
