package zemberek.lm;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LmVocabulary {
    private final String[] vocabulary;
    private Map<String, Integer> vocabularyIndexMap = new HashMap<>();

    public LmVocabulary(String[] vocabulary) {
        this.vocabulary = vocabulary;
        generateMap(vocabulary);
    }

    public LmVocabulary(RandomAccessFile raf, int vocabularySize) throws IOException {
        vocabulary = new String[vocabularySize];
        for (int i = 0; i < vocabulary.length; i++) {
            vocabulary[i] = raf.readUTF();
        }
        generateMap(vocabulary);
    }

    public LmVocabulary(DataInputStream dis, int vocabularySize) throws IOException {
        vocabulary = new String[vocabularySize];
        for (int i = 0; i < vocabulary.length; i++) {
            vocabulary[i] = dis.readUTF();
        }
        generateMap(vocabulary);
    }

    public LmVocabulary(File binaryVocabularyFile) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(binaryVocabularyFile), 1000000));
        int count = dis.readInt();
        vocabulary = new String[count];
        for (int i = 0; i < vocabulary.length; i++) {
            vocabulary[i] = dis.readUTF();
        }
        generateMap(vocabulary);
        dis.close();
    }

    private void generateMap(String[] vocabulary) {
        // construct vocabulary index lookup.
        for (int i = 0; i < vocabulary.length; i++) {
            if (vocabularyIndexMap.containsKey(vocabulary[i]))
                System.out.println("Warning: Vocabulary has duplicate item:" + vocabulary[i]);
            else
                vocabularyIndexMap.put(vocabulary[i], i);
        }
    }

    public String[] getWordArray() throws IOException {
        return vocabulary;
    }

    public int size() {
        return vocabulary.length;
    }


    public String getWord(int index) {
        return vocabulary[index];
    }

    public int getId(String word) {
        Integer k = vocabularyIndexMap.get(word);
        return k == null ? -1 : k;
    }

    public String getAsSingleString(int... index) {
        StringBuilder sb = new StringBuilder();
        for (int i : index) {
            if (isValid(i))
                sb.append(vocabulary[i]).append(" ");
            else
                sb.append("???").append(" ");
        }
        return sb.toString() + Arrays.toString(index);
    }

    public String getWordsString(int... index) {
        StringBuilder sb = new StringBuilder();
        for (int i : index) {
            if (isValid(i))
                sb.append(vocabulary[i]).append(" ");
            else
                sb.append("???").append(" ");
        }
        return sb.toString();
    }

    public boolean checkIfAllValidId(int... ids) {
        for (int id : ids) {
            if (isValid(id))
                return false;
        }
        return true;
    }

    public boolean isValid(int id) {
        return id >= 0 && id < vocabulary.length;
    }

    public long getTrigramAsLong(int g0, int g1, int g2) {
        long encoded = g2;
        encoded = (encoded << 21) | g1;
        return (encoded << 21) | g0;
    }

    public long getTrigramAsLong(int... grams) {
        long encoded = grams[2];
        encoded = (encoded << 21) | grams[1];
        return (encoded << 21) | grams[0];
    }

    public long getAsLong(int... is3) {
        if (is3.length > 3)
            throw new IllegalArgumentException("Cannot generate long from order " + is3.length + " grams ");
        long encoded = 0;
        for (int i = 2; i >= 0; --i) {
            encoded |= is3[i];
            if (i > 0)
                encoded = encoded << 21;
        }
        return encoded;
    }

    /**
     * returns the vicabulary id array for a word array. if a word is unknown, -1 is returned as its vocabulary id.
     *
     * @param words word array
     * @return id array.
     */
    public int[] getIds(String... words) {
        int[] ids = new int[words.length];
        int i = 0;
        for (String word : words) {
            if (!vocabularyIndexMap.containsKey(word))
                ids[i] = -1;
            else
                ids[i] = vocabularyIndexMap.get(word);
            i++;
        }
        return ids;
    }

    public byte[] toByteArray(int[] wordIds) {
        byte[] bytes = new byte[wordIds.length * 4];
        for (int k = 0; k < wordIds.length; k++) {
            final int token = wordIds[k];
            final int t = k * 4;
            bytes[t] = (byte) (token >>> 24);
            bytes[t + 1] = (byte) (token >>> 16 & 0xff);
            bytes[t + 2] = (byte) (token >>> 8 & 0xff);
            bytes[t + 3] = (byte) (token & 0xff);
        }
        return bytes;
    }


}
