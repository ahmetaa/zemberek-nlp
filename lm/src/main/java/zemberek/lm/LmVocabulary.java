package zemberek.lm;

import zemberek.core.io.SimpleTextReader;
import zemberek.core.logging.Log;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LmVocabulary {
    public static final String UNKNOWN_WORD = "<unk>";
    public static final String SENTENCE_START = "<s>";
    public static final String SENTENCE_END = "</s>";
    private String[] vocabulary;
    private Map<String, Integer> vocabularyIndexMap = new HashMap<>();

    private int unknownWordIndex = -1;
    private int sentenceStartIndex = -1;
    private int sentenceEndIndex = -1;

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
    private LmVocabulary(RandomAccessFile raf) throws IOException {
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
    private LmVocabulary(DataInputStream dis) throws IOException {
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
    public static LmVocabulary loadFromBinary(File binaryVocabularyFile) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(binaryVocabularyFile)))) {
            return new LmVocabulary(dis);
        }
    }

    /**
     * Generates a vocabulary from a binary RandomAccessFile. first integer read from the file pointer
     * defines the vocabulary size. Rest is read in UTF. The RandomAccessFile will not be closed by this method.
     *
     * @param raf binary vocabulary RandomAccessFile
     * @throws IOException
     */
    public static LmVocabulary loadFromRandomAcessFile(RandomAccessFile raf) throws IOException{
        return new LmVocabulary(raf);
    }

    /**
     * Generates a vocabulary from a binary DataInputStream. first integer read from the file pointer
     * defines the vocabulary size. Rest is read in UTF. This method does not close the DataInputStream
     *
     * @param dis input stream to read the vocabulary data.
     * @throws IOException
     */
    public static LmVocabulary loadFromDataInputStream(DataInputStream dis) throws IOException{
        return new LmVocabulary(dis);
    }

    /**
     * Generates a vocabulary from a UTF8-encoded text file.
     * @param utfVocabularyFile input utf8 file to read vocabulary data.
     * @return
     * @throws IOException
     */
    public static LmVocabulary loadFromUtf8File(File utfVocabularyFile) throws IOException{
        return new LmVocabulary(SimpleTextReader.trimmingUTF8Reader(utfVocabularyFile).asStringList());
    }

    private void generateMap() {
        // construct vocabulary index lookup.
        for (int i = 0; i < vocabulary.length; i++) {
            String word = vocabulary[i];
            if (vocabularyIndexMap.containsKey(word)) {
                Log.warn("Language model vocabulary has duplicate item: " + word);
            } else {
                if (word.equalsIgnoreCase(UNKNOWN_WORD)) {
                    unknownWordIndex = i;
                    vocabularyIndexMap.put(UNKNOWN_WORD, i);
                    vocabularyIndexMap.put(UNKNOWN_WORD.toUpperCase(), i);
                } else if (word.equalsIgnoreCase(SENTENCE_START)) {
                    sentenceStartIndex = i;
                    vocabularyIndexMap.put(SENTENCE_START, i);
                    vocabularyIndexMap.put(SENTENCE_START.toUpperCase(), i);
                } else if (word.equalsIgnoreCase(SENTENCE_END)) {
                    sentenceEndIndex = i;
                    vocabularyIndexMap.put(SENTENCE_END, i);
                    vocabularyIndexMap.put(SENTENCE_END.toUpperCase(), i);
                } else
                    vocabularyIndexMap.put(word, i);
            }
        }
        if (sentenceEndIndex == -1) {
            Log.warn("Vocabulary does not contain sentence start word.");
        }
        if (sentenceStartIndex == -1) {
            Log.warn("Vocabulary does not contain sentence end word.");
        }
    }

    public int size() {
        return vocabularyIndexMap.size();
    }

    /**
     * @param index Word for the index. if index is out of bounds, <UNK> is returned with warning.
     *              Note that Vocabulary may contain <UNK> token as well.
     */
    public String getWord(int index) {
        if (index < 0 || index >= vocabulary.length) {
            Log.warn("Out of bounds word index is used:" + index);
            return UNKNOWN_WORD;
        }
        return vocabulary[index];
    }

    public int indexOf(String word) {
        Integer k = vocabularyIndexMap.get(word);
        return k == null ? -1 : k;
    }

    public int getSentenceStartIndex() {
        return sentenceStartIndex;
    }

    public int getSentenceEndIndex() {
        return sentenceEndIndex;
    }

    public int getUnknownWordIndex() {
        return unknownWordIndex;
    }

    /**
     * @param indexes word indexes
     * @return the Word representation of indexes. Such as for 2,3,4 it returns "foo bar zipf"
     *         For unknown indexes it uses <UNK>.
     */
    public String getWordsString(int... indexes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indexes.length; i++) {
            int index = indexes[i];
            if (contains(index))
                sb.append(vocabulary[index]);
            else {
                Log.warn("Out of bounds word index is used:" + index);
                sb.append(UNKNOWN_WORD);
            }
            if (i < indexes.length - 1)
                sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * @param indexes index array
     * @return true if all indexes are within the Vocabulary boundaries.
     */
    public boolean containsAll(int... indexes) {
        for (int index : indexes) {
            if (!contains(index))
                return false;
        }
        return true;
    }

    /**
     * @param index word index
     * @return true if index is within the Vocabulary boundaries.
     */
    public boolean contains(int index) {
        return index >= 0 && index < vocabulary.length;
    }

    /**
     * @param words Words
     * @return true if vocabulary contains all words.
     */
    public boolean containsAll(String... words) {
        for (String word : words) {
            if (!contains(word)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param word Word
     * @return if vocabulary contains the word.
     */
    public boolean contains(String word) {
        return vocabularyIndexMap.containsKey(word);
    }

    /**
     * @param g0 W0 Index
     * @param g1 W1 Index
     * @param g2 W2 Index
     * @return The encoded long representation of a trigram. Structure:
     *         [1 bit EMPTY][21 bit W2-IND][21 bit W1-IND][21 bit W0-IND]
     */
    public long encodeTrigram(int g0, int g1, int g2) {
        long encoded = g2;
        encoded = (encoded << 21) | g1;
        return (encoded << 21) | g0;
    }

    /**
     * @param triGram trigram Indexes.
     * @return the encoded long representation of a trigram. Structure:
     *         [1 bit EMPTY][21 bit W2-IND][21 bit W1-IND][21 bit W0-IND]
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
     *         if a word is unknown, index of <UNK> is used is returned as its vocabulary index.
     *         This value can be -1.
     */
    public int[] toIndexes(String... words) {
        int[] indexes = new int[words.length];
        int i = 0;
        for (String word : words) {
            if (!vocabularyIndexMap.containsKey(word)) {
                indexes[i] = unknownWordIndex;
            } else
                indexes[i] = vocabularyIndexMap.get(word);
            i++;
        }
        return indexes;
    }

    /**
     * @param indexes word indexes.
     * @return Words representations of the indexes. If an index is out of bounds, <UNK>
     *         representation is used. Note that Vocabulary may contain an <UNK> word in it.
     */
    public String[] toWords(int... indexes) {
        String[] words = new String[indexes.length];
        int k = 0;
        for (int index : indexes) {
            if (contains(index))
                words[k++] = vocabulary[index];
            else {
                Log.warn("Out of bounds word index is used:" + index);
                words[k++] = UNKNOWN_WORD;
            }
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