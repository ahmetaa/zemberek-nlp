package zemberek.tokenizer;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import zemberek.core.collections.DoubleValueMap;
import zemberek.core.collections.UIntSet;
import zemberek.core.io.IOUtil;
import zemberek.core.text.TextUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class PerceptronSentenceBoundaryDetector implements SentenceBoundaryDetector {

    public static final int SKIP_SPACE_FREQUENCY = 50;
    public static final String BOUNDARY_CHARS = ".!?";
    DoubleValueMap<String> weights = new DoubleValueMap<>();


    static Set<String> TurkishAbbreviationSet = new HashSet<>();
    private static Locale localeTr = new Locale("tr");

    static {
        try {
            for (String line : Resources.readLines(Resources.getResource("tokenizer/abbreviations.txt"), Charsets.UTF_8)) {
                final int abbrEndIndex = line.indexOf(":");
                if (abbrEndIndex > 0) {
                    final String abbr = line.substring(0, abbrEndIndex);
                    TurkishAbbreviationSet.add(abbr.replaceAll("\\.$", ""));
                    TurkishAbbreviationSet.add(abbr.toLowerCase(localeTr).replaceAll("\\.$", ""));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PerceptronSentenceBoundaryDetector(DoubleValueMap<String> weights) {
        this.weights = weights;
    }

    public void saveBinary(Path path) throws IOException {
        try (DataOutputStream dos = IOUtil.getDataOutputStream(path)) {
            dos.writeInt(weights.size());
            for (String feature : weights) {
                dos.writeUTF(feature);
                dos.writeDouble(weights.get(feature));
            }
        }
    }

    public static PerceptronSentenceBoundaryDetector loadFromBinary(Path file) throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(file)) {
            int size = dis.readInt();
            DoubleValueMap<String> features = new DoubleValueMap<>((int) (size * 1.5));
            for (int i = 0; i < size; i++) {
                features.set(dis.readUTF(), dis.readDouble());
            }
            return new PerceptronSentenceBoundaryDetector(features);
        }
    }


    public static class Trainer {
        Path trainFile;
        int iterationCount;

        public Trainer(Path trainFile, int iterationCount) {
            this.trainFile = trainFile;
            this.iterationCount = iterationCount;
        }

        public PerceptronSentenceBoundaryDetector train() throws IOException {
            DoubleValueMap<String> weights = new DoubleValueMap<>();
            List<String> sentences = TextUtil.loadLinesWithText(trainFile);
            DoubleValueMap<String> averages = new DoubleValueMap<>();
            UIntSet indexSet = new UIntSet();
            Random rnd = new Random(1);
            StringBuilder sb = new StringBuilder();
            int boundaryIndexCounter = 0;
            int sentenceCounter = 0;
            for (String sentence : sentences) {
                sb.append(sentence);
                boundaryIndexCounter = sb.length() - 1;
                indexSet.add(boundaryIndexCounter);
                // in some sentences we skip adding a space between sentences.
                if (rnd.nextInt(SKIP_SPACE_FREQUENCY) != 1 && sentenceCounter < sentences.size() - 1) {
                    sb.append(" ");
                }
                sentenceCounter++;
            }

            int updateCount = 0;

            String joinedSentence = sb.toString();
            for (int i = 0; i < iterationCount; i++) {

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
                    double score = 0;
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
                weights.set(key, weights.get(key) - averages.get(key) * 1d / updateCount);
            }

            return new PerceptronSentenceBoundaryDetector(weights);
        }
    }

    private static class BoundaryData {
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
            leftChunk = "";
            if (previousSpace >= 0) {
                leftChunk = input.substring(previousSpace, pointer);
            }
            int previousBoundaryOrSpace = findBackwardsSpaceOrChar(input, pointer, '.');
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
                for (char c : currentWordNoPunctuation.toCharArray()) {
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

    @Override
    public List<String> getSentences(String doc) {
        List<String> sentences = new ArrayList<>();
        int begin = 0;
        for (int j = 0; j < doc.length(); j++) {
            // skip if char cannot be a boundary char.
            char chr = doc.charAt(j);
            if (BOUNDARY_CHARS.indexOf(chr) < 0)
                continue;
            BoundaryData boundaryData = new BoundaryData(doc, j);
            if (boundaryData.nonBoundaryCheck()) {
                continue;
            }
            List<String> features = boundaryData.extractFeatures();
            double score = 0;
            for (String feature : features) {
                score += weights.get(feature);
            }
            if (score > 0) {
                sentences.add(doc.substring(begin, j + 1).trim());
                begin = j + 1;
            }
        }
        return sentences;
    }

    private static final Set<String> urlWords =
            Sets.newHashSet("http", "html", "www", ".tr", ".edu", ".com", ".net", ".gov", ".org", "@");

    private static boolean potentialWebSite(String s) {
        for (String urlWord : urlWords) {
            if (s.contains(urlWord))
                return true;
        }
        return false;
    }

    private static int numberOfLetters(String s) {
        int result = 0;
        for (char chr : s.toCharArray()) {
            if (Character.isAlphabetic(chr))
                result++;
        }
        return result;
    }

    private static int numberOfCapitalLetters(String s) {
        int result = 0;
        for (char chr : s.toCharArray()) {
            if (Character.isUpperCase(chr))
                result++;
        }
        return result;
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