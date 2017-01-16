package zemberek.tokenizer;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import zemberek.core.collections.DoubleValueMap;
import zemberek.core.io.SimpleTextReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PerceptronSentenceBoundaryDetecor_ implements SentenceBoundaryDetector {

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
                    if (abbr.endsWith(".")) {
                        TurkishAbbreviationSet.add(abbr);
                        TurkishAbbreviationSet.add(abbr.toLowerCase(Locale.ENGLISH));
                        TurkishAbbreviationSet.add(abbr.toLowerCase(localeTr));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PerceptronSentenceBoundaryDetecor_(DoubleValueMap<String> weights) {
        this.weights = weights;
    }

    public static class Trainer {
        File trainFile;
        int iterationCount;

        public Trainer(File trainFile, int iterationCount) {
            this.trainFile = trainFile;
            this.iterationCount = iterationCount;
        }

        public PerceptronSentenceBoundaryDetecor_ train() throws IOException {
            DoubleValueMap<String> weights = new DoubleValueMap<>();
            List<String> sentences = SimpleTextReader.trimmingUTF8Reader(trainFile).asStringList();

/*
                if (i > 0)
                    Collections.shuffle(sentences);
*/
            Set<Integer> indexSet = new LinkedHashSet<>();
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

            String joinedSentence = sb.toString();
            for (int i = 0; i < iterationCount; i++) {

                for (int j = 0; j < joinedSentence.length(); j++) {
                    // skip if char cannot be a boundary char.
                    char chr = joinedSentence.charAt(j);
                    if (BOUNDARY_CHARS.indexOf(chr) < 0)
                        continue;
                    List<String> features = extractFeatures(joinedSentence, j);
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
                    if (update != 0) {
                        for (String feature : features) {
                            double d = weights.incrementByAmount(feature, update);
                            if (d == 0.0)
                                weights.remove(feature);
                        }
                    }
                }
            }
            return new PerceptronSentenceBoundaryDetecor_(weights);
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
            List<String> features = extractFeatures(doc, j);
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

    private static List<String> extractFeatures(String input, int pointer) {

        List<String> features = new ArrayList<>();

        // 1 letter before and after
        char previousLetter;
        if (pointer > 0)
            previousLetter = input.charAt(pointer - 1);
        else
            previousLetter = '_';
        char nextLetter;
        if (pointer < input.length() - 1)
            nextLetter = input.charAt(pointer + 1);
        else
            nextLetter = '_';


/*        features.add("1:" + Character.isUpperCase(previousLetter));
        features.add("1b:" + Character.isWhitespace(nextLetter));
        features.add("1a:" + previousLetter);
        features.add("1b:" + nextLetter);*/



        features.add("2:" + getMetaChar(previousLetter) + getMetaChar(nextLetter));

        String prev2 = "__";
        if (pointer > 2)
            prev2 = input.substring(pointer - 2, pointer);
        String next2 = "__";
        if (pointer < input.length() - 3)
            next2 = input.substring(pointer + 1, pointer + 3);

        //features.add("3:" + prev2 + next2);
        features.add("4:" + getMetaChars(prev2) + getMetaChars(next2));

        int i = pointer - 1;
        while (i >= 0) {
            char c = input.charAt(i);
            if (c == ' ') {
                i++;
                break;
            }
            i--;
        }
        String leftChunk = input.substring(i, pointer);

        int j = pointer + 1;
        while (j < input.length()) {
            char c = input.charAt(j);
            if (c == ' ') {
                break;
            }
            j++;
        }

        String rightChunk = "";
        if (pointer < input.length() - 1)
            rightChunk = input.substring(pointer + 1, j);

        String currentChar = String.valueOf(input.charAt(pointer));
        String currentWord = leftChunk + currentChar + rightChunk;

/*        while (j < input.length()) {
            if (input.charAt(j) != ' ')
                break;
            j++;
        }*/
        StringBuilder sb = new StringBuilder();
        while (j < input.length()) {
            char c = input.charAt(j);
            if (c == ' ')
                break;
            sb.append(c);
            j++;
        }
        String nextWord = sb.toString();



        if (currentWord.length() > 0) {
            features.add("7c:" + Character.isUpperCase(currentWord.charAt(0)));
            features.add("8c:" + currentWord);
            features.add("9c:" + getMetaChars(currentWord));

        }
        if (rightChunk.length() > 0) {
            features.add("7r:" + Character.isUpperCase(rightChunk.charAt(0)));
            features.add("8r:" + rightChunk);
            features.add("9r:" + getMetaChars(rightChunk));

        }
        if (nextWord.length() > 0) {
            features.add("7n:" + Character.isUpperCase(nextWord.charAt(0)));
            features.add("8n:" + nextWord);
            features.add("9n:" + getMetaChars(nextWord));
        }

        String currentNoPunct = currentWord.replaceAll("[.]", "");
        if (currentNoPunct.length() > 0 && currentNoPunct.length() != currentWord.length()) {
            features.add("10:" + currentNoPunct);
        }

        if (currentNoPunct.length() > 0) {
            boolean allUp = true;
            boolean allDigit = true;
            for (char c : currentNoPunct.toCharArray()) {
                if (!Character.isUpperCase(c))
                    allUp = false;
                if (!Character.isDigit(c))
                    allDigit = false;
            }

            if (allUp)
                features.add("11u:" + allUp);
            if (allDigit)
                features.add("11d:" + allDigit);
        }


        int numberOfPoints = numberOfChars(currentWord, '.');
        if (numberOfPoints > 0)
            features.add("12:" + String.valueOf(numberOfPoints));
        features.add("13:" + String.valueOf(TurkishAbbreviationSet.contains(currentWord + ".")));
        features.add("14:" + String.valueOf(TurkishAbbreviationSet.contains(rightChunk)));
        if (potentialWebSite(currentWord)) {
            features.add("15:web");
        }

        int numberOfCapitalLetters = numberOfCapitalLetters(currentWord);
        if (numberOfCapitalLetters > 0)
            features.add("16:" + String.valueOf(numberOfCapitalLetters));

        return features;
    }

    class Features {

        Features(String input, int pointer) {

            // 1 letter before and after
            char previousLetter;
            if (pointer > 0)
                previousLetter = input.charAt(pointer - 1);
            else
                previousLetter = '_';
            char nextLetter;
            if (pointer < input.length() - 1)
                nextLetter = input.charAt(pointer + 1);
            else
                nextLetter = '_';

            String prev2 = "__";
            if (pointer > 2)
                prev2 = input.substring(pointer - 2, pointer);
            String next2 = "__";
            if (pointer < input.length() - 3)
                next2 = input.substring(pointer + 1, pointer + 3);

            int i = pointer - 1;
            while (i >= 0) {
                char c = input.charAt(i);
                if (c == ' ') {
                    i++;
                    break;
                }
                i--;
            }
            String leftChunk = input.substring(i, pointer);

            int j = pointer + 1;
            while (j < input.length()) {
                char c = input.charAt(j);
                if (c == ' ') {
                    break;
                }
                j++;
            }

            String rightChunk = "";
            if (pointer < input.length() - 1)
                rightChunk = input.substring(pointer + 1, j);

            String currentChar = String.valueOf(input.charAt(pointer));
            String currentWord = leftChunk + currentChar + rightChunk;

            StringBuilder sb = new StringBuilder();
            while (j < input.length()) {
                char c = input.charAt(j);
                if (c == ' ')
                    break;
                sb.append(c);
                j++;
            }
            String nextWord = sb.toString();

            String currentNoPunct = currentWord.replaceAll("[.]", "");

            if (currentNoPunct.length() > 0) {
                boolean allUp = true;
                boolean allDigit = true;
                for (char c : currentNoPunct.toCharArray()) {
                    if (!Character.isUpperCase(c))
                        allUp = false;
                    if (!Character.isDigit(c))
                        allDigit = false;
                }
            }

            int numberOfPoints = numberOfChars(currentWord, '.');

            int numberOfCapitalLetters = numberOfCapitalLetters(currentWord);
        }
    }


    public static final Set<String> urlWords =
            Sets.newHashSet("http", "www", ".tr", ".edu", ".com", ".net", ".gov", ".org");

    private static boolean potentialWebSite(String s) {
        for (String urlWord : urlWords) {
            if (s.contains(urlWord))
                return true;
        }
        return false;
    }

    private static int numberOfChars(String s, char c) {
        int result = 0;
        for (char chr : s.toCharArray()) {
            if (chr == c)
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

    private static char getMetaChar(char letter) {
        char c;
        if (Character.isUpperCase(letter))
            c = 'C';
        else if (Character.isLowerCase(letter))
            c = 'c';
        else if (Character.isDigit(letter))
            c = 'd';
        else if (Character.isWhitespace(letter))
            c = ' ';
        else if (letter =='.' || letter=='!' || letter=='?')
           return letter;
        else c = '-';
        return c;
    }

    private static String getMetaChars(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            sb.append(getMetaChar(str.charAt(i)));
        }
        return sb.toString();
    }
}