package zemberek.tokenization;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import opennlp.tools.cmdline.dictionary.DictionaryBuilderTool;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.sentdetect.*;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import zemberek.core.logging.Log;
import zemberek.core.text.Regexps;
import zemberek.core.text.TextUtil;
import zemberek.core.text.TokenSequence;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SentenceExtractionComparison {

    public static void main(String[] args) throws IOException {

        trainZemberekSentenceExtractor();

        Path test = Paths.get("tokenization/src/test/resources/tokenization/Sentence-Boundary-Test.txt");

        List<String> testSentences = TextUtil.loadLinesWithText(test);

        Log.info(" \n---------------- Zemberek ------------------\n");

        evaluate(testSentences, new TurkishSentenceExtractorAdapter());

        Log.info(" \n---------------- OpenNlp ------------------\n");
        SentenceExtractor openNlpAdapter = new OpenNlpAdapter(Paths.get("/home/ahmetaa/data/nlp/tr-sent.bin"));
        evaluate(testSentences, openNlpAdapter);
    }

    static void trainZemberekSentenceExtractor() throws IOException {
        Path train = Paths.get("tokenization/src/test/resources/tokenization/Sentence-Boundary-Train.txt");
        TurkishSentenceExtractor extractor = TurkishSentenceExtractor.Trainer
                .builder(train)
                .iterationCount(10)
                .learningRate(0.1f)
                .shuffleSentences()
                .build()
                .train();
        extractor.saveBinary(Paths.get("tokenization/src/main/resources/tokenization/sentence-boundary-model.bin"));
    }

    interface SentenceExtractor {
        List<String> extract(String paragraph);

        List<String> extract(List<String> paragraphs);
    }

    static class TurkishSentenceExtractorAdapter implements SentenceExtractor {

        TurkishSentenceExtractor sentenceDetector = TurkishSentenceExtractor.DEFAULT;

        @Override
        public List<String> extract(String paragraph) {
            return sentenceDetector.fromParagraph(paragraph);
        }

        @Override
        public List<String> extract(List<String> paragraphs) {
            return sentenceDetector.fromParagraphs(paragraphs);
        }
    }


    static class OpenNlpAdapter implements SentenceExtractor {

        SentenceDetectorME sentenceDetector;

        OpenNlpAdapter(Path modelPath) throws IOException {
            SentenceModel model = new SentenceModel(modelPath.toFile());
            this.sentenceDetector = new SentenceDetectorME(model);
        }

        @Override
        public List<String> extract(String paragraph) {
            return Arrays.asList(sentenceDetector.sentDetect(paragraph));
        }

        @Override
        public List<String> extract(List<String> paragraphs) {
            List<String> result = new ArrayList<>();
            for (String paragraph : paragraphs) {
                result.addAll(extract(paragraph));
            }
            return result;
        }
    }

    private static void evaluate(List<String> referenceSentences, SentenceExtractor detector) throws IOException {

        // Sanitize input
        referenceSentences = referenceSentences
                .stream()
                .map(s -> s.replaceAll("\\s+", " ").replaceAll("…", "...").trim())
                .collect(Collectors.toList());
        String joinedSentence = Joiner.on(" ").join(referenceSentences);

        // Extract sentences.
        Stopwatch sw = Stopwatch.createStarted();
        List<String> foundSentences = detector.extract(joinedSentence);
        long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
        Log.info("Extraction Elapsed: %d ms", elapsed);
        Log.info("Speed: %.2f sentences per second", referenceSentences.size() * 1000d / elapsed);

        // ---- Evaluation ------
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

        Log.info("Evaluation result for boundary matches.");
        Log.info("Actual sentence count = %d , Found sentence count = %d", referenceSentences.size(), foundSentences.size());
        Set<Integer> truePositives = new HashSet<>(refBoundaries);
        truePositives.retainAll(foundBoundaries);
        Set<Integer> falsePositives = new HashSet<>(foundBoundaries);
        falsePositives.removeAll(refBoundaries);
        double precision = truePositives.size() * 1d / (truePositives.size() + falsePositives.size());
        Log.info("Precision       = %.4f", precision);
        double recall = truePositives.size() * 1d / refBoundaries.size();
        Log.info("Recall          = %.4f", recall);
        double f = 2 * precision * recall / (precision + recall);
        Log.info("F               = %.4f", f);

        Set<Integer> insertions = new HashSet<>(foundBoundaries);
        insertions.removeAll(refBoundaries);
        Set<Integer> deletions = new HashSet<>(refBoundaries);
        deletions.removeAll(foundBoundaries);

        Log.info("NIST error rate = %.4f%%",
                (deletions.size() + insertions.size()) * 100d / refBoundaries.size());

        HashSet<String> matchingSentences = new HashSet<>(foundSentences);
        matchingSentences.retainAll(referenceSentences);
        Log.info("Amount of exactly matching sentences = %d in %d (%.4f%%)",
                matchingSentences.size(),
                referenceSentences.size(),
                matchingSentences.size() * 100d / referenceSentences.size());
    }

    public static void findCandidateTrainingSentences() throws IOException {
        Pattern p = Pattern.compile("[ ][a-zA-ZişüğıöçÜĞÖİŞÇ0-9,.'\\-%]+[.!?]+[ ]");
        List<String> input = Files.readAllLines(Paths.get("/media/depo/data/aaa/nlp/sentences/k/2017-01-04"));
        TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;
        List<String> extracted = extractor.fromParagraphs(input);
        List<String> foo = extracted
                .stream()
                .filter(s -> Regexps.matchesAny(p, s) && s.length() < 150)
                .collect(Collectors.toList());

        Files.write(Paths.get("/media/depo/data/aaa/nlp/sentences/candidate"), foo);
    }

    public static void trainOpenNlp() throws IOException {
        Charset charset = Charset.forName("UTF-8");
        ObjectStream<String> lineStream =
                new PlainTextByLineStream(() -> new FileInputStream(
                        Paths.get("src/test/resources/tokenization/Sentence-Boundary-Train.txt").toFile()),
                        charset);
        ObjectStream<SentenceSample> sampleStream = new SentenceSampleStream(lineStream);

        File abbrvFile = new File("/media/depo/data/aaa/nlp/abbreviations");
        File abbrvOut = new File("/media/depo/data/aaa/nlp/abbreviations.dict");

        SentenceModel model;
        new DictionaryBuilderTool().run(
                new String[]{
                        "-inputFile",
                        abbrvFile.getAbsolutePath(),
                        "-outputFile",
                        abbrvOut.getAbsolutePath(),
                        "-encoding",
                        "utf-8"});
        opennlp.tools.dictionary.Dictionary dictionary = new Dictionary(new FileInputStream(abbrvOut));
        SentenceDetectorFactory factory =
                new SentenceDetectorFactory("tr", true, dictionary, ".?!".toCharArray());

        model = SentenceDetectorME.train("tr", sampleStream, factory, TrainingParameters.defaultParams());

        OutputStream modelOut = new BufferedOutputStream(
                new FileOutputStream(new File("/media/depo/data/aaa/nlp/sentence.bin")));
        model.serialize(modelOut);
    }


}
