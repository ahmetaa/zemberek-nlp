package zemberek.tokenization;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.logging.Log;
import zemberek.core.text.TokenSequence;
import zemberek.tokenizer.PerceptronSentenceBoundaryDetecor_;
import zemberek.tokenizer.SentenceBoundaryDetector;
import zemberek.tokenizer.SimpleSentenceBoundaryDetector;

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
                5).train();
        System.out.println("Train Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
//        test(testSentences, perceptron);
        evaluate2(testSentences, perceptron);

        System.out.println(" \n---------------- Rule Based ------------------\n");
        SimpleSentenceBoundaryDetector ruleBased = new SimpleSentenceBoundaryDetector();
//        test(testSentences, ruleBased);
        evaluate2(testSentences, ruleBased);

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
        //evaluate(sentences, found);
    }

    public static void evaluate2(List<String> referenceSentences, SentenceBoundaryDetector detector) throws IOException {

        // Sanitize
        referenceSentences = referenceSentences.stream().map(String::trim).collect(Collectors.toList());
        String joinedSentence = Joiner.on(" ").join(referenceSentences);
        Stopwatch sw = Stopwatch.createStarted();
        List<String> foundSentences = detector.getSentences(joinedSentence);
        Log.info("Segmentation Elapsed: %d ms", sw.elapsed(TimeUnit.MILLISECONDS));

        // separate each boundary token with space in all sentences.
        List<String> separatedSource = new ArrayList<>();
        for (String sentence : referenceSentences) {
            separatedSource.add(sentence
                    .replaceAll("[.]", " . ")
                    .replaceAll("[?]", " ? ")
                    .replaceAll("[!]", " ! ")
                    .replaceAll("\\s+", " ")
                    .trim());
        }

        List<String> separatedHypotheses = new ArrayList<>();
        for (String sentence : foundSentences) {
            separatedHypotheses.add(sentence
                    .replaceAll("[.]", " . ")
                    .replaceAll("[?]", " ? ")
                    .replaceAll("[!]", " ! ")
                    .replaceAll("\\s+", " ")
                    .trim());
        }

        Set<Integer> refBoundaries = new LinkedHashSet<>();
        int j = 0;
        for (String sentence : separatedSource) {
            TokenSequence seq = new TokenSequence(sentence);
            j = j + seq.size();
            refBoundaries.add(j);
        }

        Set<Integer> foundBoundaries = new LinkedHashSet<>();
        int k = 0;
        for (String s : separatedHypotheses) {
            TokenSequence seq = new TokenSequence(s);
            k = k + seq.size();
            foundBoundaries.add(k);
        }

        try (PrintWriter pw = new PrintWriter(new File("segments"))) {
            foundSentences.forEach(pw::println);
        }

        Log.info("Evaluation result for boundary matches.");
        Log.info("Actual sentence count = %d , Found sentence count = %d", referenceSentences.size(), foundSentences.size());
        Set<Integer> truePositives = new HashSet<>(refBoundaries);
        truePositives.retainAll(foundBoundaries);
        Set<Integer> falsePositives = new HashSet<>(foundBoundaries);
        falsePositives.removeAll(refBoundaries);
        double precision = truePositives.size() * 1d / (truePositives.size() + falsePositives.size());
        Log.info("Precision = %.4f", precision);
        double recall = truePositives.size() * 1d / refBoundaries.size();
        Log.info("Recall    = %.4f" , recall);
        double f = 2 * precision * recall / (precision + recall);
        Log.info("F         = %.4f", f);

        Set<Integer> insertions = new HashSet<>(foundBoundaries);
        insertions.removeAll(refBoundaries);
        Set<Integer> deletions = new HashSet<>(refBoundaries);
        deletions.removeAll(foundBoundaries);

        Log.info("NIST error rate = %.4f",
                (deletions.size() + insertions.size()) * 100d / refBoundaries.size());

        HashSet<String> matchingSentences = new HashSet<>(foundSentences);
        matchingSentences.retainAll(referenceSentences);
        Log.info("Amount of exactly matching sentences = %d in %d (%.4f)",
                matchingSentences.size(),
                referenceSentences.size(),
                matchingSentences.size() * 100d / referenceSentences.size());
    }

}
