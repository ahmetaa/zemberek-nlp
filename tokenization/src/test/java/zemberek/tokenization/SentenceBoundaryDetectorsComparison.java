package zemberek.tokenization;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import zemberek.core.io.SimpleTextReader;
import zemberek.tokenizer.PerceptronSentenceBoundaryDetecor_;
import zemberek.tokenizer.SentenceBoundaryDetector;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SentenceBoundaryDetectorsComparison {

    public static void main(String[] args) throws IOException {
        List<String> testSentences = SimpleTextReader.trimmingUTF8Reader(
                new File("tokenization/src/test/resources/tokenizer/Sentence-Boundary-Test.txt")).asStringList();
        Stopwatch sw = Stopwatch.createStarted();
        PerceptronSentenceBoundaryDetecor_ perceptron = new PerceptronSentenceBoundaryDetecor_.Trainer(
                new File("tokenization/src/test/resources/tokenizer/Sentence-Boundary-Train.txt"),
                3).train();
        System.out.println("Train Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
//        test(testSentences, perceptron);
        evaluate(testSentences, perceptron);

        System.out.println(" \n---------------- Rule Based ------------------\n");
        //SimpleSentenceBoundaryDetector ruleBased = new SimpleSentenceBoundaryDetector();
//        test(testSentences, ruleBased);
        //evaluate(testSentences, ruleBased);

    }

    public static void test(List<String> sentences, SentenceBoundaryDetector detector) {
        Random rnd = new Random(1);
        StringBuilder sb = new StringBuilder();
        int sentenceCounter = 0;
        for (String sentence : sentences) {
            sb.append(sentence);
            // in approximately every 20 sentences we skip adding a space between sentences.
            if (rnd.nextInt(PerceptronSentenceBoundaryDetecor_.SKIP_SPACE_FREQUENCY) != 1 && sentenceCounter < sentences.size() - 1) {
                sb.append(" ");
            }
            sentenceCounter++;
        }
        String joinedSentence = sb.toString();
        Stopwatch sw = Stopwatch.createStarted();
        List<String> found = detector.getSentences(joinedSentence);
        System.out.println("Test Elapsed: " + sw.elapsed(TimeUnit.MILLISECONDS));
        evaluate(sentences, found);
    }

    private static void evaluate(List<String> sentences, List<String> found) {
        Set<String> reference = Sets.newLinkedHashSet(sentences);
        Set<String> foundSet = Sets.newLinkedHashSet(found);

        int hit = 0;
        for (String s : foundSet) {
            if (reference.contains(s)) {
                hit++;
                //System.out.println(s);
            } else
                System.out.println(s + " -");
        }
        System.out.println("Total=" + sentences.size() + " Hit=" + hit + " Precision:" + hit * 100d / sentences.size());
    }

    //todo (aaa): this does not work correctly.
    public static void evaluate(List<String> sentences, SentenceBoundaryDetector detector) throws IOException {

        sentences = sentences.stream().map(String::trim).collect(Collectors.toList());

        Set<Integer> refBoundaries = new LinkedHashSet<>();
        int j = 0;
        for (String sentence : sentences) {
            j = j + sentence.length() + 1;
            refBoundaries.add(j);
        }
        String joinedSentence = Joiner.on(" ").join(sentences);
        Stopwatch sw = Stopwatch.createStarted();
        List<String> found = detector.getSentences(joinedSentence);

        Set<Integer> foundBoundaries = new LinkedHashSet<>();
        int k = 0;
        for (String s : found) {
            k = k + s.length() + 1;
            foundBoundaries.add(k);
        }

        try (PrintWriter pw = new PrintWriter(new File("segments"))) {
            found.forEach(pw::println);
        }

        System.out.println("Test Elapsed: " + sw.elapsed(TimeUnit.MILLISECONDS));

        Set<Integer> truePositives = new HashSet<>(refBoundaries);
        truePositives.retainAll(foundBoundaries);
        Set<Integer> falsePositives = new HashSet<>(foundBoundaries);
        falsePositives.removeAll(refBoundaries);
        double precision = truePositives.size() * 1d / (truePositives.size() + falsePositives.size());
        System.out.println("Precision = " + precision);
        double recall = truePositives.size() * 1d / refBoundaries.size();
        System.out.println("Recall    = " + recall);
        double f = 2 * precision * recall / (precision + recall);
        System.out.println("F         = " + f);

        Set<Integer> insertions = new HashSet<>(foundBoundaries);
        insertions.removeAll(refBoundaries);
        Set<Integer> deletions = new HashSet<>(refBoundaries);
        deletions.removeAll(foundBoundaries);

        System.out.println("NIST error rate = " + (deletions.size() + insertions.size()) * 100d / refBoundaries.size());
    }
}
