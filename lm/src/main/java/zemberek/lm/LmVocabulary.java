package zemberek.lm;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.logging.Log;

import java.io.*;
import java.text.Collator;
import java.util.*;

public class LmVocabulary {
    public static final String UNKNOWN_WORD = "<unk>";
    public static final String SENTENCE_START = "<s>";
    public static final String SENTENCE_END = "</s>";
    private List<String> vocabulary;
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
        generateMap(Lists.newArrayList(vocabulary));
    }

    /**
     * Generates a vocabulary with given String List.
     *
     * @param vocabulary word list.
     */
    public LmVocabulary(List<String> vocabulary) {
        generateMap(Lists.newArrayList(vocabulary));
    }

    /**
     * Generates a vocabulary from a binary RandomAccessFile. first integer read from the file pointer
     * defines the vocabulary size. Rest is read in UTF. Constructor does not close the RandomAccessFile
     *
     * @param raf binary vocabulary RandomAccessFile
     * @throws IOException
     */
    private LmVocabulary(RandomAccessFile raf) throws IOException {
        List<String> words = new ArrayList<>();
        int size = raf.readInt();
        for (int i = 0; i < size; i++) {
            words.add(raf.readUTF());
        }
        generateMap(words);
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
        List<String> words = new ArrayList<>();
        int size = dis.readInt();
        for (int i = 0; i < size; i++) {
            words.add(dis.readUTF());
        }
        generateMap(words);
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
     * Binary serialization of the vocabulary.
     *
     * @param file to serialize.
     * @throws IOException
     */
    public void saveBinary(File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            saveBinary(dos);
        }
    }

    /**
     * Binary serialization of the vocabulary.
     *
     * @param dos output stream to serialize.
     * @throws IOException
     */
    public void saveBinary(DataOutputStream dos) throws IOException {
        dos.writeInt(vocabulary.size());
        for (String s : vocabulary) {
            dos.writeUTF(s);
        }
    }

    /**
     * Generates a vocabulary from a binary RandomAccessFile. first integer read from the file pointer
     * defines the vocabulary size. Rest is read in UTF. The RandomAccessFile will not be closed by this method.
     *
     * @param raf binary vocabulary RandomAccessFile
     * @throws IOException
     */
    public static LmVocabulary loadFromRandomAcessFile(RandomAccessFile raf) throws IOException {
        return new LmVocabulary(raf);
    }

    /**
     * Generates a vocabulary from a binary DataInputStream. first integer read from the file pointer
     * defines the vocabulary size. Rest is read in UTF. This method does not close the DataInputStream
     *
     * @param dis input stream to read the vocabulary data.
     * @throws IOException
     */
    public static LmVocabulary loadFromDataInputStream(DataInputStream dis) throws IOException {
        return new LmVocabulary(dis);
    }

    /**
     * Generates a vocabulary from a UTF8-encoded text file.
     *
     * @param utfVocabularyFile input utf8 file to read vocabulary data.
     * @return LMVocabulary instance.
     * @throws IOException
     */
    public static LmVocabulary loadFromUtf8File(File utfVocabularyFile) throws IOException {
        return new LmVocabulary(SimpleTextReader.trimmingUTF8Reader(utfVocabularyFile).asStringList());
    }

    private static Set<String> SPECIAL_WORDS = Sets.newHashSet(SENTENCE_START, SENTENCE_END, UNKNOWN_WORD);

    private void generateMap(List<String> words) {

        List<String> finalVocabulary = Lists.newArrayList();

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            String lowerCase = word.toLowerCase(Locale.ENGLISH);
            if (SPECIAL_WORDS.contains(lowerCase) && !word.equals(lowerCase)) {
                if (!lowerCase.equals(word)) {
                    Log.warn("Input contains special word %s but case does not match:%s. " +
                            "%s form will be used.", lowerCase, word, lowerCase);
                }
                words.set(i, lowerCase);
                continue;
            }
            if (vocabularyIndexMap.containsKey(word)) {
                Log.warn("Language model vocabulary has duplicate item: " + word);
            } else {
                vocabularyIndexMap.put(word, finalVocabulary.size());
                finalVocabulary.add(word);
            }
        }
        if (!words.contains(SENTENCE_START)) {
            int index = finalVocabulary.size();
            vocabularyIndexMap.put(SENTENCE_START, index);
            finalVocabulary.add(SENTENCE_START);
            sentenceStartIndex = index;
            Log.warn("Input vocabulary does not contain sentence start word <s>. It is added automatically.");
        }
        if (!words.contains(SENTENCE_END)) {
            int index = finalVocabulary.size();
            vocabularyIndexMap.put(SENTENCE_END, index);
            finalVocabulary.add(SENTENCE_END);
            sentenceEndIndex = index;
            Log.warn("Input Vocabulary does not contain sentence end word </s>. It is added automatically.");
        }
        if (!words.contains(UNKNOWN_WORD)) {
            int index = finalVocabulary.size();
            vocabularyIndexMap.put(UNKNOWN_WORD, index);
            finalVocabulary.add(UNKNOWN_WORD);
            unknownWordIndex = index;
            Log.info("Input Vocabulary does not contain unknown word token </unk>. It is added automatically.");
        }
        vocabulary = Collections.unmodifiableList(finalVocabulary);
    }

    public int size() {
        // Because we may have duplicate items in word list but not in map, we return the size of the list.
        return vocabulary.size();
    }

    /**
     * Returns a mutable Builder class which can be used for generating an LmVocabulary object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * This class acts like a mutable vocabulary. It can be used for dynamically generating an LmVocabulary object.
     */
    public static class Builder {
        private Map<String, Integer> map = new HashMap<>();
        private List<String> tokens = Lists.newArrayList();

        public int add(String word) {
            int index = indexOf(word);
            if (index != -1) {
                return index;
            } else {
                index = tokens.size();
                map.put(word, index);
                tokens.add(word);
                return index;
            }
        }

        int size() {
            return tokens.size();
        }

        public void addAll(String... words) {
            for (String word : words) {
                add(word);
            }
        }

        public void addAll(Iterable<String> words) {
            for (String word : words) {
                add(word);
            }
        }

        /**
         * @return index of input. -1 if input does not exist.
         */
        public int indexOf(String key) {
            if (map.containsKey(key))
                return map.get(key);
            else return -1;
        }

        /**
         * @return an unmodifiable copy of the words so far added.
         */
        public List<String> words() {
            return Collections.unmodifiableList(tokens);
        }

        /**
         * @return indexes of words when the words are alphabetically sorted according to the input locale.
         */
        public Iterable<Integer> alphabeticallySortedWordsIds(Locale locale) {
            TreeMap<String, Integer> treeMap = new TreeMap<>(Collator.getInstance(locale));
            treeMap.putAll(map);
            return treeMap.values();
        }

        /**
         * @return indexes of words when the words are alphabetically sorted according to EN locale.
         */
        public Iterable<Integer> alphabeticallySortedWordsIds() {
            return new TreeMap<>(map).values();
        }

        /**
         * @return Generated unmodifiable LmVocabulary
         */
        public LmVocabulary generate() {
            return new LmVocabulary(tokens);
        }
    }

    /**
     * @param index Word for the index. if index is out of bounds, <UNK> is returned with warning.
     */
    public String getWord(int index) {
        if (index < 0 || index >= vocabulary.size()) {
            Log.warn("Out of bounds word index is used:" + index);
            return UNKNOWN_WORD;
        }
        return vocabulary.get(index);
    }

    public int indexOf(String word) {
        Integer k = vocabularyIndexMap.get(word);
        return k == null ? unknownWordIndex : k;
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
     * @return indexes of words when the words are alphabetically sorted according to the input locale.
     */
    public Iterable<Integer> alphabeticallySortedWordsIds(Locale locale) {
        return new TreeMap<String, Integer>(Collator.getInstance(locale)).values();
    }

    /**
     * @return indexes of words when the words are alphabetically sorted according to the default locale.
     */
    public Iterable<Integer> alphabeticallySortedWordsIds() {
        return new TreeMap<>(vocabularyIndexMap).values();
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
                sb.append(vocabulary.get(index));
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
        return index >= 0 && index < vocabulary.size();
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
     * @return if vocabulary contains the word. For special tokens, it always return true.
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
                words[k++] = vocabulary.get(index);
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