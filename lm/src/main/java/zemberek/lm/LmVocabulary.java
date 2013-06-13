package zemberek.lm;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class LmVocabulary {
    public static final String OUT_OF_VOCABULARY = "<OOV>";
    private String[] vocabulary;
    private Map<String, Integer> vocabularyIndexMap = new HashMap<>();

    /**
     * Generates a vocabulary with given String array.
     *
     * @param vocabulary word array.
     */
    public LmVocabulary(String... vocabulary) {
        this.vocabulary = vocabulary;
        generateMap();
    }

    /**
     * Generates a vocabulary with given String List.
     *
     * @param vocabulary word list.
     */
    public LmVocabulary(List<String> vocabulary) {
        this.vocabulary = vocabulary.toArray(new String[vocabulary.size()]);
        generateMap();
    }

    /**
     * Generates a vocabulary from a binary RandomAccessFile. first integer read from the file pointer
     * defines the vocabulary size. Rest is read in UTF. Constructor does not close the RandomAccessFile
     *
     * @param raf binary vocabulary RandomAccessFile
     * @throws IOException
     */
    public LmVocabulary(RandomAccessFile raf) throws IOException {
        vocabulary = new String[raf.readInt()];
        for (int i = 0; i < vocabulary.length; i++) {
            vocabulary[i] = raf.readUTF();
        }
        generateMap();
    }

    /**
     * Generates a vocabulary from a binary DataInputStream. first integer read from the file pointer
     * defines the vocabulary size. Rest is read in UTF. Constructor does not close the DataInputStream
     *
     * @param dis input stream to read the vocabulary data.
     * @throws IOException
     */
    public LmVocabulary(DataInputStream dis) throws IOException {
        loadVocabulary(dis);
    }

    private void loadVocabulary(DataInputStream dis) throws IOException {
        vocabulary = new String[dis.readInt()];
        for (int i = 0; i < vocabulary.length; i++) {
            vocabulary[i] = dis.readUTF();
        }
        generateMap();
    }

    /**
     * Generates a vocabulary from a binary vocabulary File. First integer in the file
     * defines the vocabulary size. Rest is read in UTF.
     *
     * @throws IOException
     */
    public LmVocabulary(File binaryVocabularyFile) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(binaryVocabularyFile)))) {
            loadVocabulary(dis);
        }
    }

    private void generateMap() {
        // construct vocabulary index lookup.
        for (int i = 0; i < vocabulary.length; i++) {
            if (vocabularyIndexMap.containsKey(vocabulary[i]))
                throw new IllegalStateException("Vocabulary has duplicate item: " + vocabulary[i]);
            else
                vocabularyIndexMap.put(vocabulary[i], i);
        }
    }

    public String[] getWordArray() throws IOException {
        return vocabulary;
    }

    public int size() {
        return vocabularyIndexMap.size();
    }

    public String getWord(int index) {
        return vocabulary[index];
    }

    public int indexOf(String word) {
        Integer k = vocabularyIndexMap.get(word);
        return k == null ? -1 : k;
    }

    /**
     * @param indexes word indexes
     * @return the Word representation of indexes. Such as for 2,3,4 it returns "foo bar zipf"
     *         For unknown id's it uses UNKNOWN word syntax.
     */
    public String getWordsString(int... indexes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indexes.length; i++) {
            int index = indexes[i];
            if (contains(index))
                sb.append(vocabulary[index]);
            else
                sb.append(OUT_OF_VOCABULARY);
            if (i < indexes.length - 1)
                sb.append(" ");
        }
        return sb.toString();
    }

    public boolean containsAll(int... indexes) {
        for (int id : indexes) {
            if (!contains(id))
                return false;
        }
        return true;
    }

    public boolean contains(int index) {
        return index >= 0 && index < vocabulary.length;
    }

    /**
     * @param g0 W0 Index
     * @param g1 W1 Index
     * @param g2 W2 Index
     * @return The encoded long representation of a trigram. Structure:
     *         [1 bit EMPTY][21 bit W2-ID][21 bit W1-ID][21 bit W0-ID]
     */
    public long encodeTrigram(int g0, int g1, int g2) {
        long encoded = g2;
        encoded = (encoded << 21) | g1;
        return (encoded << 21) | g0;
    }

    /**
     * @param triGram trigram Indexes.
     * @return the encoded long representation of a trigram. Structure:
     *         [1 bit EMPTY][21 bit W2-ID][21 bit W1-ID][21 bit W0-ID]
     */
    public long encodeTrigram(int... triGram) {
        if (triGram.length > 3)
            throw new IllegalArgumentException("Cannot generate long from order " + triGram.length + " grams ");
        long encoded = triGram[2];
        encoded = (encoded << 21) | triGram[1];
        return (encoded << 21) | triGram[0];
    }

    /**
     * @param words word array
     * @return the vocabulary index array for a word array.
     *         if a word is unknown, -1 is returned as its vocabulary index.
     */
    public int[] indexOf(String... words) {
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

    /**
     * @param indexes word indexes.
     * @return Words representations of the indexes. If an index is out of bounds, OUT_OF_VOCABULARY
     *         representation is used.
     */
    public String[] getWords(int... indexes) {
        String[] words = new String[indexes.length];
        int k = 0;
        for (int index : indexes) {
            if (contains(index))
                words[k++] = vocabulary[index];
            else
                words[k++] = OUT_OF_VOCABULARY;
        }
        return words;
    }

    /**
     * @param indexes indexes.
     * @return byte array representation of the word indexes. Each index is stored in 4 bytes big endian.
     *         [W0-MSByte, .., W0-LSByte, W1-MSByte,...]
     */
    public byte[] toByteArray(int... indexes) {
        byte[] bytes = new byte[indexes.length * 4];
        for (int k = 0; k < indexes.length; k++) {
            final int token = indexes[k];
            final int t = k * 4;
            bytes[t] = (byte) (token >>> 24);
            bytes[t + 1] = (byte) (token >>> 16 & 0xff);
            bytes[t + 2] = (byte) (token >>> 8 & 0xff);
            bytes[t + 3] = (byte) (token & 0xff);
        }
        return bytes;
    }


}
