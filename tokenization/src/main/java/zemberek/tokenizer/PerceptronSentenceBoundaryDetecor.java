package zemberek.tokenizer;

import com.google.common.collect.Sets;
import zemberek.core.DoubleValueSet;
import zemberek.core.io.SimpleTextReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

//TODO: experimental work.
public class PerceptronSentenceBoundaryDetecor {


    public static final int SKIP_SPACE_FREQUENCY = 20;
    public static final String BOUNDARY_CHARS = ".!?";
    DoubleValueSet<String> weights = new DoubleValueSet<>();

    void train(File trainFile) throws IOException {

        List<String> sentences = SimpleTextReader.trimmingUTF8Reader(trainFile).asStringList();

        Set<Integer> indexSet = new LinkedHashSet<>();

        Random rnd = new Random(1);
        StringBuilder sb = new StringBuilder();
        int boundaryIndexCounter = 0;
        int sentenceCounter = 0;
        for (String sentence : sentences) {
            sb.append(sentence);
            boundaryIndexCounter += sb.length() - 1;
            indexSet.add(boundaryIndexCounter);
            // in approximately every 20 sentences we skip adding a space between sentences.
            if (rnd.nextInt(SKIP_SPACE_FREQUENCY) != 1 && sentenceCounter < sentences.size() - 1) {
                sb.append(" ");
            }
            sentenceCounter++;
        }

        String joinedSentence = sb.toString();
        for (int i = 0; i < 5; i++) {
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
                if (score < 0 && indexSet.contains(j)) {
                    update = 1;
                }
                // if we found boundary but it is not a boundary
                if (score >= 0 && !indexSet.contains(j)) {
                    update = -1;
                }
                if (update != 0) {
                    for (String feature : features) {
                        weights.incrementByAmount(feature, update);
                    }
                }
            }

            for (String key : weights) {
                System.out.println(key + ":" + weights.get(key));
            }
            System.out.println("-----------------------");
        }
    }

    List<String> findBoundaries(String doc) {
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
            if (score >= 0) {
                sentences.add(doc.substring(begin, j).trim());
                begin = j;
            }
        }
        return sentences;
    }

    public void test(List<String> sentences) {
        Random rnd = new Random(1);
        StringBuilder sb = new StringBuilder();
        int sentenceCounter = 0;
        for (String sentence : sentences) {
            sb.append(sentence);
            // in approximately every 20 sentences we skip adding a space between sentences.
            if (rnd.nextInt(SKIP_SPACE_FREQUENCY) != 1 && sentenceCounter < sentences.size() - 1) {
                sb.append(" ");
            }
            sentenceCounter++;
        }
        String joinedSentence = sb.toString();
        List<String> found = findBoundaries(joinedSentence);
        Set<String> reference = Sets.newHashSet(sentences);
        Set<String> foundSet = Sets.newHashSet(found);

        int hit = 0;
        for (String s : foundSet) {
            if (reference.contains(s))
                hit++;
        }
        System.out.println("Total=" + sentences.size() + " Hit=" + hit);
    }

    List<String> extractFeatures(String input, int pointer) {

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

        features.add("1:" + firstLetter + secondLetter);

        char firstMeta = getMetaChar(firstLetter);
        char secondMeta = getMetaChar(secondLetter);

        features.add("2:" + firstMeta + secondMeta);

        return features;

    }

    private char getMetaChar(char firstLetter) {
        char c;
        if (Character.isUpperCase(firstLetter))
            c = 'C';
        else if (Character.isLowerCase(firstLetter))
            c = 'c';
        else if (Character.isDigit(firstLetter))
            c = 'd';
        else if (Character.isWhitespace(firstLetter))
            c = ' ';
        else c = '-';
        return c;
    }

    public static void main(String[] args) throws IOException {
        PerceptronSentenceBoundaryDetecor detecor = new PerceptronSentenceBoundaryDetecor();
        detecor.train(
                new File("/home/kodlab/projects/zemberek-nlp/tokenization/src/main/resources/tokenizer/Total.txt"));
        detecor.test(SimpleTextReader.trimmingUTF8Reader(
                new File("/home/kodlab/projects/zemberek-nlp/tokenization/src/main/resources/tokenizer/Test.txt")).asStringList());
    }

}
