package zemberek.tokenizer;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.UIntSet;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.text.TextUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * This class is used for extracting sentences from paragraphs.
 * For making boundary decisions it uses a combination of rules and a binary averaged perceptron model.
 * It only breaks from [.!?] symbols.
 * It does not break from line break characters. Therefore input should not contain line breaks.
 */
public class TurkishSentenceExtractor implements SentenceExtractor {

    static final String BOUNDARY_CHARS = ".!?";
    private FloatValueMap<String> weights = new FloatValueMap<>();

    static Set<String> TurkishAbbreviationSet = new HashSet<>();
    private static Locale localeTr = new Locale("tr");

    static {
        try {
            for (String line : Resources.readLines(Resources.getResource("tokenizer/abbreviations.txt"), Charsets.UTF_8)) {
                if (line.trim().length() > 0) {
                    final String abbr = line.trim().replaceAll("\\s+",""); // erase spaces
                    TurkishAbbreviationSet.add(abbr.replaceAll("\\.$", "")); // erase last dot amd add.
                    TurkishAbbreviationSet.add(abbr.toLowerCase(localeTr).replaceAll("\\.$", "")); // lowercase and add.
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TurkishSentenceExtractor(FloatValueMap<String> weights) {
        this.weights = weights;
    }

    @Override
    public List<String> extract(List<String> paragraphs) {
        List<String> result = new ArrayList<>();
        for (String paragraph : paragraphs) {
            result.addAll(extract(paragraph));
        }
        return result;
    }

    @Override
    public List<String> extract(String paragraph) {
        List<String> sentences = new ArrayList<>();
        int begin = 0;
        for (int j = 0; j < paragraph.length(); j++) {
            // skip if char cannot be a boundary char.
            char chr = paragraph.charAt(j);
            if (BOUNDARY_CHARS.indexOf(chr) < 0)
                continue;
            BoundaryData boundaryData = new BoundaryData(paragraph, j);
            if (boundaryData.nonBoundaryCheck()) {
                continue;
            }
            List<String> features = boundaryData.extractFeatures();
            double score = 0;
            for (String feature : features) {
                score += weights.get(feature);
            }
            if (score > 0) {
                String sentence = paragraph.substring(begin, j + 1).trim();
                if (sentence.length() > 0) {
                    sentences.add(sentence);
                }
                begin = j + 1;
            }
        }

        if (begin < paragraph.length()) {
            String remaining = paragraph.substring(begin, paragraph.length()).trim();
            sentences.add(remaining);
        }
        return sentences;
    }

    public char[] getBoundaryCharacters() {
        return BOUNDARY_CHARS.toCharArray();
    }

    public void saveBinary(Path path) throws IOException {
        try (DataOutputStream dos = IOUtil.getDataOutputStream(path)) {
            dos.writeInt(weights.size());
            for (String feature : weights) {
                dos.writeUTF(feature);
                dos.writeFloat(weights.get(feature));
            }
        }
    }

    public static TurkishSentenceExtractor loadFromBinaryFile(Path file) throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(file)) {
            return load(dis);
        }
    }

    public static TurkishSentenceExtractor fromInternalModel() throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(
                Resources.getResource("tokenizer/sentence-boundary-model.bin").openStream())) {
            return load(dis);
        }
    }

    private static TurkishSentenceExtractor load(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        FloatValueMap<String> features = new FloatValueMap<>((int) (size * 1.5));
        for (int i = 0; i < size; i++) {
            features.set(dis.readUTF(), dis.readFloat());
        }
        return new TurkishSentenceExtractor(features);
    }

    public static class TrainerBuilder {
        Path trainFile;
        int iterationCount = 5;
        int skipSpaceFrequency = 20;
        int lowerCaseFirstLetterFrequency = 20;
        boolean shuffleInput = false;

        public TrainerBuilder(Path trainFile) {
            this.trainFile = trainFile;
        }

        public TrainerBuilder iterationCount(int count) {
            this.iterationCount = count;
            return this;
        }

        public TrainerBuilder shuffleSentences() {
            this.shuffleInput = true;
            return this;
        }

        public TrainerBuilder skipSpaceFrequencyonCount(int count) {
            this.skipSpaceFrequency = skipSpaceFrequency;
            return this;
        }

        public TrainerBuilder lowerCaseFirstLetterFrequency(int count) {
            this.lowerCaseFirstLetterFrequency = lowerCaseFirstLetterFrequency;
            return this;
        }

        public Trainer build() {
            return new Trainer(this);
        }
    }

    public static class Trainer {
        private TrainerBuilder builder;

        public static TrainerBuilder builder(Path trainFile) {
            return new TrainerBuilder(trainFile);
        }

        private Trainer(TrainerBuilder builder) {
            this.builder = builder;
        }

        private static Locale Turkish = new Locale("tr");

        public TurkishSentenceExtractor train() throws IOException {
            FloatValueMap<String> weights = new FloatValueMap<>();
            List<String> sentences = TextUtil.loadLinesWithText(builder.trainFile);
            FloatValueMap<String> averages = new FloatValueMap<>();

            Random rnd = new Random(1);

            int updateCount = 0;

            for (int i = 0; i < builder.iterationCount; i++) {

                Log.info("Iteration = %d", i + 1);

                UIntSet indexSet = new UIntSet();
                StringBuilder sb = new StringBuilder();
                int boundaryIndexCounter;
                int sentenceCounter = 0;
                if (builder.shuffleInput) {
                    Collections.shuffle(sentences);
                }
                for (String sentence : sentences) {
                    if (sentence.trim().length() == 0) {
                        continue;
                    }
                    // sometimes make first letter of the sentence lower case.
                    if (rnd.nextInt(builder.lowerCaseFirstLetterFrequency) == 0) {
                        sentence = sentence.substring(0, 1).toLowerCase(Turkish) + sentence.substring(1);
                    }
                    sb.append(sentence);
                    boundaryIndexCounter = sb.length() - 1;
                    indexSet.add(boundaryIndexCounter);
                    // in some sentences we skip adding a space between sentences.
                    if (rnd.nextInt(builder.skipSpaceFrequency) != 1 && sentenceCounter < sentences.size() - 1) {
                        sb.append(" ");
                    }
                    sentenceCounter++;
                }
                String joinedSentence = sb.toString();

                for (int j = 0; j < joinedSentence.length(); j++) {
                    // skip if char cannot be a boundary char.
                    char chr = joinedSentence.charAt(j);
                    if (BOUNDARY_CHARS.indexOf(chr) < 0) {
                        continue;
                    }
                    BoundaryData boundaryData = new BoundaryData(joinedSentence, j);
                    if (boundaryData.nonBoundaryCheck()) {
                        continue;
                    }
                    List<String> features = boundaryData.extractFeatures();
                    float score = 0;
                    for (String feature : features) {
                        score += weights.get(feature);
                    }
                    int update = 0;
                    // if we found no-boundary but it is a boundary
                    if (score <= 0 && indexSet.contains(j)) {
                        update = 1;
                    }
                    // if we found boundary but it is not a boundary
                    else if (score > 0 && !indexSet.contains(j)) {
                        update = -1;
                    }
                    updateCount++;
                    if (update != 0) {
                        for (String feature : features) {
                            double d = weights.incrementByAmount(feature, update);
                            if (d == 0.0) {
                                weights.remove(feature);
                            }
                            d = averages.incrementByAmount(feature, updateCount * update);
                            if (d == 0.0) {
                                averages.remove(feature);
                            }
                        }
                    }
                }
            }
            for (String key : weights) {
                weights.set(key, weights.get(key) - averages.get(key) * 1f / updateCount);
            }

            return new TurkishSentenceExtractor(weights);
        }
    }

    static class BoundaryData {
        char currentChar;
        char previousLetter;
        char nextLetter;
        String previousTwoLetters;
        String nextTwoLetters;
        String currentWord;
        String nextWord;
        String currentWordNoPunctuation;
        String rightChunk;
        String rightChunkUntilBoundary;
        String leftChunk;
        String leftChunkUntilBoundary;

        BoundaryData(String input, int pointer) {

            previousLetter = pointer > 0 ? input.charAt(pointer - 1) : '_';
            nextLetter = pointer < input.length() - 1 ? nextLetter = input.charAt(pointer + 1) : '_';
            previousTwoLetters = pointer > 2 ? input.substring(pointer - 2, pointer) : "__";
            nextTwoLetters = pointer < input.length() - 3 ? input.substring(pointer + 1, pointer + 3) : "__";

            int previousSpace = findBackwardsSpace(input, pointer);
            if (previousSpace < 0) {
                previousSpace = 0;
            }
            leftChunk = "";
            if (previousSpace >= 0) {
                leftChunk = input.substring(previousSpace, pointer);
            }
            int previousBoundaryOrSpace = findBackwardsSpaceOrChar(input, pointer, '.');
            if (previousBoundaryOrSpace < 0) {
                previousBoundaryOrSpace = 0;
            }
            leftChunkUntilBoundary = previousBoundaryOrSpace == previousSpace ?
                    leftChunk : input.substring(previousBoundaryOrSpace, pointer);

            int nextSpace = findForwardsSpace(input, pointer);
            rightChunk = "";
            if (pointer < input.length() - 1) {
                rightChunk = input.substring(pointer + 1, nextSpace);
            }
            int nextBoundaryOrSpace = findForwardsSpaceOrChar(input, pointer, '.');
            rightChunkUntilBoundary = nextSpace == nextBoundaryOrSpace ?
                    rightChunk : input.substring(pointer + 1, nextBoundaryOrSpace);

            currentChar = input.charAt(pointer);
            currentWord = leftChunk + String.valueOf(currentChar) + rightChunk;
            currentWordNoPunctuation = currentWord.replaceAll("[.!?]", "");

            StringBuilder sb = new StringBuilder();
            int j = nextSpace;
            while (j < input.length()) {
                char c = input.charAt(j);
                if (c == ' ')
                    break;
                sb.append(c);
                j++;
            }
            nextWord = sb.toString();
        }

        int findBackwardsSpace(String input, int pos) {
            return findBackwardsSpaceOrChar(input, pos, ' ');
        }

        int findBackwardsSpaceOrChar(String input, int pos, char chr) {
            int i = pos - 1;
            while (i >= 0) {
                char c = input.charAt(i);
                if (c == ' ' || c == chr) {
                    i++;
                    break;
                }
                i--;
            }
            return i;
        }

        int findForwardsSpace(String input, int pos) {
            return findForwardsSpaceOrChar(input, pos, ' ');
        }

        int findForwardsSpaceOrChar(String input, int pos, char chr) {
            int j = pos + 1;
            while (j < input.length()) {
                char c = input.charAt(j);
                if (c == ' ' || c == chr) {
                    break;
                }
                j++;
            }
            return j;
        }

        boolean nonBoundaryCheck() {
            return TurkishAbbreviationSet.contains(currentWord)
                    || TurkishAbbreviationSet.contains(leftChunkUntilBoundary)
                    || (leftChunkUntilBoundary.length() == 1)
                    || BOUNDARY_CHARS.indexOf(nextLetter) >= 0
                    || nextLetter == '\''
                    || potentialWebSite(currentWord);
        }

        List<String> extractFeatures() {
            List<String> features = new ArrayList<>();
            features.add("1:" + Character.isUpperCase(previousLetter));
            features.add("1b:" + Character.isWhitespace(nextLetter));
            features.add("1a:" + previousLetter);
            features.add("1b:" + nextLetter);
            features.add("2p:" + previousTwoLetters);
            features.add("2n:" + nextTwoLetters);

            if (currentWord.length() > 0) {
                features.add("7c:" + Character.isUpperCase(currentWord.charAt(0)));
                features.add("9c:" + getMetaChars(currentWord));
            }

            if (rightChunk.length() > 0) {
                features.add("7r:" + Character.isUpperCase(rightChunk.charAt(0)));
                features.add("9r:" + getMetaChars(rightChunk));
                if (!containsVowel(rightChunk)) {
                    features.add("rcc:true");
                }
            }
            if (nextWord.length() > 0) {
                features.add("7n:" + Character.isUpperCase(nextWord.charAt(0)));
                features.add("9n:" + getMetaChars(nextWord));
            }

            if (leftChunk.length() > 0) {
                if (!containsVowel(leftChunk)) {
                    features.add("lcc:true");
                }
            }

            if (currentWordNoPunctuation.length() > 0) {
                boolean allUp = true;
                boolean allDigit = true;
                for (int j = 0; j < currentWordNoPunctuation.length(); j++) {
                    char c = currentWordNoPunctuation.charAt(j);
                    if (!Character.isUpperCase(c))
                        allUp = false;
                    if (!Character.isDigit(c))
                        allDigit = false;
                }

                if (allUp) {
                    features.add("11u:true");
                }
                if (allDigit) {
                    features.add("11d:true");
                }
            }
            return features;
        }
    }

    private static final Set<String> webWords =
            Sets.newHashSet("http:", ".html", "www", ".tr", ".edu", ".com", ".net", ".gov", ".org", "@");

    private static boolean potentialWebSite(String s) {
        for (String urlWord : webWords) {
            if (s.contains(urlWord))
                return true;
        }
        return false;
    }

    private static String lowerCaseVowels = "aeıioöuüâîû";
    private static String upperCaseVowels = "AEIİOÖUÜÂÎÛ";

    private static char getMetaChar(char letter) {
        char c;
        if (Character.isUpperCase(letter)) {
            c = upperCaseVowels.indexOf(letter) > 0 ? 'V' : 'C';
        } else if (Character.isLowerCase(letter)) {
            c = lowerCaseVowels.indexOf(letter) > 0 ? 'v' : 'c';
        } else if (Character.isDigit(letter))
            c = 'd';
        else if (Character.isWhitespace(letter))
            c = ' ';
        else if (letter == '.' || letter == '!' || letter == '?')
            return letter;
        else c = '-';
        return c;
    }

    private static boolean containsVowel(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (lowerCaseVowels.indexOf(c) > 0 || upperCaseVowels.indexOf(c) > 0) {
                return true;
            }
        }
        return false;
    }

    private static String getMetaChars(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            sb.append(getMetaChar(str.charAt(i)));
        }
        return sb.toString();
    }
}