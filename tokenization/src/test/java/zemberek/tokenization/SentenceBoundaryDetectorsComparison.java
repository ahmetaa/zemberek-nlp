package zemberek.tokenization;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import zemberek.core.io.SimpleTextReader;
import zemberek.tokenizer.PerceptronSentenceBoundaryDetecor;
import zemberek.tokenizer.SentenceBoundaryDetector;
import zemberek.tokenizer.SimpleSentenceBoundaryDetector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SentenceBoundaryDetectorsComparison {

    public static void main(String[] args) throws IOException {
        List<String> testSentences = SimpleTextReader.trimmingUTF8Reader(
                new File("/home/kodlab/projects/zemberek-nlp/tokenization/src/test/resources/tokenizer/Test.txt")).asStringList();
        Stopwatch sw = new Stopwatch().start();
        PerceptronSentenceBoundaryDetecor perceptron = new PerceptronSentenceBoundaryDetecor.Trainer(
                new File("/home/kodlab/projects/zemberek-nlp/tokenization/src/test/resources/tokenizer/Total.txt"),
                2).train();
        System.out.println("Train Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
        test(testSentences, perceptron);
        SimpleSentenceBoundaryDetector ruleBased = new SimpleSentenceBoundaryDetector();
        test(testSentences, ruleBased);
    }

    public static void test(List<String> sentences, SentenceBoundaryDetector detector) {
        Random rnd = new Random(1);
        StringBuilder sb = new StringBuilder();
        int sentenceCounter = 0;
        for (String sentence : sentences) {
            sb.append(sentence);
            // in approximately every 20 sentences we skip adding a space between sentences.
            if (rnd.nextInt(PerceptronSentenceBoundaryDetecor.SKIP_SPACE_FREQUENCY) != 1 && sentenceCounter < sentences.size() - 1) {
                sb.append(" ");
            }
            sentenceCounter++;
        }
        String joinedSentence = sb.toString();
        Stopwatch sw = new Stopwatch().start();
        List<String> found = detector.getSentences(joinedSentence);
        System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));
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
        System.out.println("Total=" + sentences.size() + " Hit=" + hit + " Precision:" + hit*100d/sentences.size());
    }
}
