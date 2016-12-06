package zemberek.lm;

import com.google.common.collect.Lists;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.core.text.TextConverter;

import java.io.*;
import java.nio.file.Path;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

public class LmVocabulary {
    public static final String DEFAULT_SENTENCE_BEGIN_MARKER = "<s>";
    public static final String DEFAULT_SENTENCE_END_MARKER = "</s>";
    public static final String DEFAULT_UNKNOWN_WORD = "<unk>";
    private String unknownWord;
    private String sentenceStart;
    private String sentenceEnd;
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
        Lists.newArrayList(vocabulary);
        generateMap(Lists.newArrayList(vocabulary));
    }

    /**
     * Generates a vocabulary with given String List.
     *
     * @param vocabulary word list.
     */
    public LmVocabulary(List<String> vocabulary) {
        generateMap(vocabulary);
    }

    /**
     * Generates a vocabulary from a binary RandomAccessFile. first integer read from the file pointer
     * defines the vocabulary size. Rest is read in UTF. Constructor does not close the RandomAccessFile
     *
     * @param raf binary vocabulary RandomAccessFile
     * @throws IOException
     */
    private LmVocabulary(RandomAccessFile raf) throws IOException {
        int vocabLength = raf.readInt();
        List<String> vocabulary = new ArrayList<>(vocabLength);
        for (int i = 0; i < vocabLength; i++) {
            vocabulary.add(raf.readUTF());
        }
        generateMap(vocabulary);
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
        int vocabularyLength = dis.readInt();
        List<String> vocabulary = new ArrayList<>(vocabularyLength);
        for (int i = 0; i < vocabularyLength; i++) {
            vocabulary.add(dis.readUTF());
        }
        generateMap(vocabulary);
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
     * Generates a vocabulary from a binary vocabulary File. First integer in the file
     * defines the vocabulary size. Rest is read in UTF.
     *
     * @throws IOException
     */
    public static LmVocabulary loadFromBinary(Path binaryVocabularyFilePath) throws IOException {
        return loadFromBinary(binaryVocabularyFilePath.toFile());
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
    public static LmVocabulary loadFromRandomAccessFile(RandomAccessFile raf) throws IOException {
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

    private void generateMap(List<String> inputVocabulary) {
        // construct vocabulary index lookup.
        int indexCounter = 0;
        List<String> cleanVocab = new ArrayList<>();
        for (String word : inputVocabulary) {
            if (vocabularyIndexMap.containsKey(word)) {
                Log.warn("Language model vocabulary has duplicate item: " + word);
                continue;
            }
            if (word.equalsIgnoreCase(DEFAULT_UNKNOWN_WORD)) {
                if (unknownWordIndex != -1)
                    Log.warn("Unknown word was already defined as %s but another matching token exist in the input vocabulary: %s", unknownWord, word);
                else {
                    unknownWord = word;
                    unknownWordIndex = indexCounter;
                }
            } else if (word.equalsIgnoreCase(DEFAULT_SENTENCE_BEGIN_MARKER)) {
                if (sentenceStartIndex != -1)
                    Log.warn("Sentence start index was already defined as %s but another matching token exist in the input vocabulary: %s", sentenceStart, word);
                else {
                    sentenceStart = word;
                    sentenceStartIndex = indexCounter;
                }
            } else if (word.equalsIgnoreCase(DEFAULT_SENTENCE_END_MARKER)) {
                if (sentenceEndIndex != -1)
                    Log.warn("Sentence end index was already defined as %s but another matching token exist in the input vocabulary: %s", sentenceEnd, word);
                else {
                    sentenceEnd = word;
                    sentenceEndIndex = indexCounter;
                }
            }
            vocabularyIndexMap.put(word, indexCounter);
            cleanVocab.add(word);
            indexCounter++;
        }
        if (unknownWordIndex == -1) {
            unknownWord = DEFAULT_UNKNOWN_WORD;
            cleanVocab.add(unknownWord);
            vocabularyIndexMap.put(unknownWord, indexCounter++);
            Log.debug("Necessary special token " + unknownWord + " was not found in the vocabulary, it is added explicitly");
        }
        unknownWordIndex = vocabularyIndexMap.get(unknownWord);
        if (sentenceStartIndex == -1) {
            sentenceStart = DEFAULT_SENTENCE_BEGIN_MARKER;
            cleanVocab.add(sentenceStart);
            vocabularyIndexMap.put(sentenceStart, indexCounter++);
            Log.debug("Vocabulary does not contain sentence start token, it is added explicitly.");
        }
        sentenceStartIndex = vocabularyIndexMap.get(sentenceStart);
        if (sentenceEndIndex == -1) {
            sentenceEnd = DEFAULT_SENTENCE_END_MARKER;
            cleanVocab.add(sentenceEnd);
            vocabularyIndexMap.put(sentenceEnd, indexCounter);
            Log.debug("Vocabulary does not contain sentence end token, it is added explicitly.");
        }
        sentenceEndIndex = vocabularyIndexMap.get(sentenceEnd);
        vocabulary = Collections.unmodifiableList(cleanVocab);
    }

    public int size() {
        // Because we may have duplicate items in word list but not in map, we return the size of the list.
        return vocabulary.size();
    }

    public boolean containsUnknown(int... gramIds) {
        for (int gramId : gramIds) {
            if (gramId < 0 || gramId >= vocabulary.size() || gramId == unknownWordIndex)
                return true;
        }
        return false;
    }

    /**
     * returns true if any word in vocabulary starts with `_` or `-`
     */
    public boolean containsSuffix() {
        for (String s : vocabulary) {
            if (s.startsWith("_") || s.startsWith("-"))
                return true;
        }
        return false;
    }

    /**
     * returns true if any word in vocabulary ends with `_` or `-`
     */
    public boolean containsPrefix() {
        for (String s : vocabulary) {
            if (s.endsWith("_") || s.endsWith("-"))
                return true;
        }
        return false;
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

        public Builder addAll(String... words) {
            for (String word : words) {
                add(word);
            }
            return this;
        }

        public Builder addAll(Iterable<String> words) {
            for (String word : words) {
                add(word);
            }
            return this;
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

        public LmVocabulary generateDecoded(TextConverter converter) {
            List<String> newTokens = new ArrayList<>();
            for (String token : tokens) {
                if (Strings.containsNone(token, "</>")) {
                    newTokens.add(converter.decode(token));
                } else {
                    newTokens.add(token);
                }
            }
            if (this.tokens.size() != newTokens.size()) {
                throw new IllegalStateException("After vocabulary text conversion, token sizes does not match!");
            }
            this.tokens = newTokens;
            return generate();
        }
    }

    /**
     * @param index Word for the index. if index is out of bounds, <UNK> is returned with warning.
     *              Note that Vocabulary may contain <UNK> token as well.
     */
    public String getWord(int index) {
        if (index < 0 || index >= vocabulary.size()) {
            Log.warn("Out of bounds word index is used:" + index);
            return unknownWord;
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

    public String getUnknownWord() {
        return unknownWord;
    }

    public String getSentenceStart() {
        return sentenceStart;
    }

    public String getSentenceEnd() {
        return sentenceEnd;
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

    public Iterable<String> words() {
        return this.vocabulary;
    }

    public Iterable<String> wordsSorted() {
        List<String> sorted = new ArrayList<>(vocabulary);
        Collections.sort(sorted);
        return sorted;
    }

    public Iterable<String> wordsSorted(Locale locale) {
        List<String> sorted = new ArrayList<>(vocabulary);
        Collections.sort(sorted, Collator.getInstance(locale));
        return sorted;
    }

    /**
     * Generates a new LmVocabulary instance that contains words that exist in both `v1` and `v2`
     * There is no guarantee that new vocabulary indexes will match with v1 or v2.
     */
    public static LmVocabulary intersect(LmVocabulary v1, LmVocabulary v2) {
        HashSet<String> ls = new HashSet<>(v1.vocabulary);
        List<String> intersection = new ArrayList<>(Math.min(v1.size(), v2.size()));
        intersection.addAll(new HashSet<>(v2.vocabulary)
                .stream()
                .filter(ls::contains)
                .collect(Collectors.toList()));
        return new LmVocabulary(intersection);
    }


    /**
     * @param indexes word indexes
     * @return the Word representation of indexes. Such as for 2,3,4 it returns "foo bar zipf"
     * For unknown indexes it uses <unk>.
     */
    public String getWordsString(int... indexes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indexes.length; i++) {
            int index = indexes[i];
            if (contains(index))
                sb.append(vocabulary.get(index));
            else {
                Log.warn("Out of bounds word index is used:" + index);
                sb.append(unknownWord);
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
     * [1 bit EMPTY][21 bit W2-IND][21 bit W1-IND][21 bit W0-IND]
     */
    public long encodeTrigram(int g0, int g1, int g2) {
        long encoded = g2;
        encoded = (encoded << 21) | g1;
        return (encoded << 21) | g0;
    }

    /**
     * @param triGram trigram Indexes.
     * @return the encoded long representation of a trigram. Structure:
     * [1 bit EMPTY][21 bit W2-IND][21 bit W1-IND][21 bit W0-IND]
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
     * if a word is unknown, index of <UNK> is used is returned as its vocabulary index.
     * This value can be -1.
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
     * @param history and current word.
     * @return the vocabulary index array for a word array.
     * if a word is unknown, index of <UNK> is used is returned as its vocabulary index.
     * This value can be -1.
     */
    public int[] toIndexes(String[] history, String word) {
        int[] indexes = new int[history.length + 1];
        for (int j = 0; j <= history.length; j++) {
            String s = j < history.length ? history[j] : word;
            if (!vocabularyIndexMap.containsKey(s)) {
                indexes[j] = unknownWordIndex;
            } else
                indexes[j] = vocabularyIndexMap.get(s);
        }
        return indexes;
    }

    /**
     * word index history and current word.
     *
     * @return the vocabulary index array for a word array.
     * if a word is unknown, index of <UNK> is used is returned as its vocabulary index.
     * This value can be -1.
     */
    public int[] toIndexes(int[] history, String word) {
        int[] indexes = new int[history.length + 1];
        for (int j = 0; j <= history.length; j++) {
            int index = j < history.length ? history[j] : indexOf(word);
            if (!contains(index)) {
                indexes[j] = unknownWordIndex;
            } else
                indexes[j] = index;
        }
        return indexes;
    }

    /**
     * @param indexes word indexes.
     * @return Words representations of the indexes. If an index is out of bounds, <UNK>
     * representation is used. Note that Vocabulary may contain an <UNK> word in it.
     */
    public String[] toWords(int... indexes) {
        String[] words = new String[indexes.length];
        int k = 0;
        for (int index : indexes) {
            if (contains(index))
                words[k++] = vocabulary.get(index);
            else {
                Log.warn("Out of bounds word index is used:" + index);
                words[k++] = unknownWord;
            }
        }
        return words;
    }
}