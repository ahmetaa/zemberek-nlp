package zemberek.corpus;

import zemberek.core.logging.Log;
import zemberek.normalization.TurkishSpellChecker;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpellingCorpusProducer {

    public static void main(String[] args) throws IOException {
        Path rawRoot = Paths.get("/home/ahmetaa/data/text/news-corpus");
        Path sentenceRoot = Paths.get("/home/ahmetaa/data/text/news-sentences");
        Path corpus = Paths.get("/home/ahmetaa/data/text/spelling-corpus");
        extractSentencesFromDir(rawRoot, sentenceRoot);
        extractTokenCorpusFromDir(sentenceRoot, corpus);
    }

    void extractSentences(Path input, Path output) throws IOException {

        TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;

        WebCorpus wc = getWebCorpus(input);

        Log.info("Processing documents.");
        for (WebDocument doc : wc.getDocuments()) {
            List<String> paragraphs = doc.getLines();
            List<String> sentences = new ArrayList<>(paragraphs.size() * 5);
            for (String paragraph : paragraphs) {
                sentences.addAll(extractor.fromParagraph(paragraph));
            }
            // set new content.
            doc.setContent(sentences);
        }

        Log.info("Saving corpus to %s", output);
        wc.save(output, false);
    }

    private WebCorpus getWebCorpus(Path input) throws IOException {
        Log.info("Loading %s", input);
        WebCorpus wc = new WebCorpus(input.toFile().getName(), input.toFile().getName());
        wc.addDocuments(WebCorpus.loadDocuments(input));
        return wc;
    }

    void generateCorpus(List<Path> sentenceCorpusPaths, Path output) throws IOException {
        TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;
        try (PrintWriter pw = new PrintWriter(output.toFile(), "utf-8")) {
            for (Path input : sentenceCorpusPaths) {
                WebCorpus wc = getWebCorpus(input);
                Log.info("Processing documents.");
                for (WebDocument doc : wc.getDocuments()) {
                    List<String> sentences = doc.getLines();
                    for (String sentence : sentences) {
                        List<String> tokenized = TurkishSpellChecker.tokenizeForSpelling(sentence);
                        String tokenSentence = String.join(" ", tokenized);
                        pw.println(tokenSentence);
                    }
                }
            }
        }
    }

    private static void extractSentencesFromDir(Path rawRoot, Path sentenceRoot) throws IOException {
        List<Path> paths = Files.walk(rawRoot)
                .filter(s -> s.toFile().getName().endsWith(".corpus")).collect(Collectors.toList());
        SpellingCorpusProducer producer = new SpellingCorpusProducer();
        for (Path in : paths) {
            Path out = sentenceRoot.resolve(in.toFile().getName());
            producer.extractSentences(in, out);
        }
    }

    private static void extractTokenCorpusFromDir(Path root, Path out) throws IOException {
        List<Path> paths = Files.walk(root)
                .filter(s -> s.toFile().getName().endsWith(".corpus")).collect(Collectors.toList());
        SpellingCorpusProducer producer = new SpellingCorpusProducer();
        producer.generateCorpus(paths, out);
    }


}
