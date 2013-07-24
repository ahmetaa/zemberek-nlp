package zemberek.tokenizer;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.antlr.v4.runtime.Token;
import zemberek.core.DoubleValueSet;
import zemberek.core.io.SimpleTextReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PerceptronSentenceBoundaryDetecor implements SentenceBoundaryDetector {

    public static final int SKIP_SPACE_FREQUENCY = 20;
    public static final String BOUNDARY_CHARS = ".!?";
    DoubleValueSet<String> weights = new DoubleValueSet<>();

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

    public PerceptronSentenceBoundaryDetecor(DoubleValueSet<String> weights) {
        this.weights = weights;
    }

    public static class Trainer {
        File trainFile;
        int iterationCount;

        public Trainer(File trainFile, int iterationCount) {
            this.trainFile = trainFile;
            this.iterationCount = iterationCount;
        }

        public PerceptronSentenceBoundaryDetecor train() throws IOException {
            DoubleValueSet<String> weights = new DoubleValueSet<>();
            List<String> sentences = SimpleTextReader.trimmingUTF8Reader(trainFile).asStringList();

            Set<Integer> indexSet = new LinkedHashSet<>();

            Random rnd = new Random(1);
            StringBuilder sb = new StringBuilder();
            int boundaryIndexCounter = 0;
            int sentenceCounter = 0;
            for (String sentence : sentences) {
                sb.append(sentence);
                boundaryIndexCounter = sb.length() - 1;
                indexSet.add(boundaryIndexCounter);
                // in approximately every 20 sentences we skip adding a space between sentences.
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
            return new PerceptronSentenceBoundaryDetecor(weights);
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
        char firstLetter;
        if (pointer > 0)
            firstLetter = input.charAt(pointer - 1);
        else
            firstLetter = '_';
        char secondLetter;
        if (pointer < input.length() - 1)
            secondLetter = input.charAt(pointer + 1);
        else
            secondLetter = '_';

        //features.add("1:" + firstLetter + secondLetter);
        //features.add("1a:" + firstLetter);
        //features.add("1b:" + secondLetter);


        //features.add("2:" + getMetaChar(firstLetter) + getMetaChar(secondLetter));

        String prev2 = "__";
        if (pointer > 2)
            prev2 = input.substring(pointer - 2, pointer);
        String next2 = "__";
        if (pointer < input.length() - 3)
            next2 = input.substring(pointer + 1, pointer + 3);

        //features.add("3:" + prev2 + next2);

        //features.add("4:" + getMetaChars(prev2) + getMetaChars(next2));
        //features.add("5:" + getMetaChars(prev2));

        String currentWord;
        int i = pointer - 1;
        StringBuilder sb = new StringBuilder();
        while (i > 0) {
            char c = input.charAt(i);
            if (c == ' ') {
                break;
            }
            sb.append(c);
            i--;
        }
        currentWord = sb.reverse().toString();

/*        if (currentWord.length() > 0) {
            int trimLength = 3;
            if (sb.length() < trimLength) {
                trimLength = sb.length();
            }
            features.add("5:" + currentWord.substring(0, trimLength));
        }*/


        String nextWord;
        i = pointer + 1;
        sb = new StringBuilder();
        while (i < input.length()) {
            if (input.charAt(i) != ' ')
                break;
            i++;
        }
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == ' ') {
                break;
            }
            sb.append(c);
            i++;
        }
        nextWord = sb.toString();

/*        if (nextWord.length() > 0) {
            int trimLength = 3;
            if (sb.length() < trimLength) {
                trimLength = sb.length();
            }
            features.add("6:" + nextWord.substring(0, trimLength));
        }*/

        features.add("7:" + Character.isUpperCase(firstLetter));
        if (currentWord.length() > 0) {
            features.add("8:" + currentWord);
            features.add("9:" + getMetaChars(currentWord));
        }
        String previousNoPunct = currentWord.replaceAll("[.]", "");
        if (previousNoPunct.length() > 0)
            features.add("10:" + getMetaChars(previousNoPunct));

        if (previousNoPunct.length() > 0) {
            boolean allUp = true;
            for (char c : previousNoPunct.toCharArray()) {
                if (!Character.isUpperCase(c))
                    allUp = false;
            }
            features.add("11:" + allUp);
        }
        features.add("12:" + String.valueOf(numberOfChars(currentWord, '.')));
        features.add("13:" + String.valueOf(TurkishAbbreviationSet.contains(currentWord + ".")));
        features.add("14:" + String.valueOf(TurkishAbbreviationSet.contains(nextWord)));
        features.add("15:" + String.valueOf(potentialWebSite(currentWord)));
        return features;
    }


    public static final Set<String> urlWords = Sets.newHashSet("http", "www", ".tr", ".edu", ".com", ".net", ".gov", ".org");

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
