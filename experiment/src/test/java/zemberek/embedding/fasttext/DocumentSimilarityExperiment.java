package zemberek.embedding.fasttext;

import org.antlr.v4.runtime.Token;
import zemberek.core.ScoredItem;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.corpus.WebCorpus;
import zemberek.corpus.WebDocument;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DocumentSimilarityExperiment {

    public static void main(String[] args) throws Exception {
        DocumentSimilarityExperiment experiment = new DocumentSimilarityExperiment();
        Path root = Paths.get("/home/ahmetaa/data/nlp/news-corpora");
        Path expRoot = Paths.get("/home/ahmetaa/data/nlp/experiments/similarity");
        Path corpusFile = expRoot.resolve("corpus");
        Path testCorpus = expRoot.resolve("test.corpus");
        Path sentenceFile = expRoot.resolve("sentences");
        Path sentenceReducedFile = expRoot.resolve("sentences.reduced");
        Path modelFile = expRoot.resolve("vector.model");
        Path outPath = expRoot.resolve("similarity.predicitons");
        //experiment.prepareCorpus(root, corpusFile);
        //experiment.onlySentences(corpusFile, sentenceFile);
        //experiment.removeDuplicates(sentenceFile, sentenceReducedFile, 10);
        //experiment.generateVectorModel(sentenceReducedFile, modelFile);
        //experiment.prepareCorpus(root.resolve("www.cnnturk.com"), expRoot.resolve("test.corpus"));
        experiment.checkSimilarity(modelFile, testCorpus, outPath);
    }

    public void checkSimilarity(Path model, Path corpusFile, Path outPath) throws IOException {

        FastText fastText = FastText.load(model);

        List<WebDocument> docs = WebCorpus.loadDocuments(corpusFile);
        List<DocumentSimilarity> sims = new ArrayList<>();
        Log.info("Calculating document vectors.");
        for (WebDocument doc : docs) {
            doc.setContent(hack(doc.getLines()));
            if (doc.contentLength() < 500) {
                continue;
            }
            String str = doc.getContentAsString();
            str = str.length() > 200 ? str.substring(0, 200) : str;
            float[] vec = fastText.textVector(str).data_.clone();
            //float[] vec = fastText.textVectors(doc.getLines()).data_.clone();
            sims.add(new DocumentSimilarity(doc, vec));
        }

        try (PrintWriter pw = new PrintWriter(outPath.toFile(), "utf-8")) {
            int i = 0;
            for (DocumentSimilarity sim : sims) {
                List<ScoredItem<WebDocument>> nearest = nearestK(sim, sims, 5);
                pw.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                pw.println(String.join("\n", sim.document.getLines()));
                for (ScoredItem<WebDocument> w : nearest) {
                    pw.println("----------------------------------");
                    pw.println(String.join("\n", w.item.getLines()));
                }
                i++;
                if (i == 100) {
                    break;
                }
            }
        }
    }

    public List<String> hack(List<String> content) {
        List<String> res = new ArrayList<>();
        for (String s : content) {
            if (s.startsWith("yasal uyarı") ||
                    s.startsWith("yazara gönder") ||
                    s.startsWith("yükleniyor ...") ||
                    s.startsWith("öne çıkanlar") ||
                    s.startsWith("metni yazdır") ||
                    s.startsWith("metin boyutu") ||
                    s.startsWith("güncellendi :") ||
                    s.startsWith("ayrıntılar için lütfen tıklayın") ||
                    s.startsWith("kaynak gösterilse dahi") ||
                    s.startsWith("ancak alıntılanan köşe bir bölümü")) {
                continue;
            }
            res.add(s);
        }
        return res;
    }


    public List<ScoredItem<WebDocument>> nearestK(DocumentSimilarity source, List<DocumentSimilarity> sims, int k) {
        PriorityQueue<ScoredItem<WebDocument>> queue = new PriorityQueue<>(k, (a, b) -> Double.compare(a.score, b.score));

        for (DocumentSimilarity sim : sims) {
            // skip self.
            if (source.document.getId().equals(sim.document.getId())) {
                continue;
            }
            float distance = source.cosDistance(sim);
            if (queue.size() < k) {
                queue.add(new ScoredItem<>(sim.document, distance));
            } else {
                ScoredItem<WebDocument> weakest = queue.peek();
                if (weakest.score < distance) {
                    queue.remove();
                    queue.add(new ScoredItem<>(sim.document, distance));
                }
            }
        }
        ArrayList<ScoredItem<WebDocument>> result = new ArrayList<>(queue);
        Collections.sort(result);
        return result;
    }


    static class DocumentSimilarity {
        WebDocument document;
        float[] vector;
        float c;

        public DocumentSimilarity(WebDocument document, float[] vector) {
            this.document = document;
            this.vector = vector;
            float sum = 0;
            for (float v : vector) {
                sum += v * v;
            }
            c = (float) Math.sqrt(sum);
        }

        public float cosDistance(DocumentSimilarity v) {
            float tmp = 0;
            for (int i = 0; i < vector.length; i++) {
                tmp += (vector[i] * v.vector[i]);
            }
            return tmp / (c * v.c);
        }
    }

    public void generateVectorModel(Path input, Path modelFile) throws Exception {
        Args argz = Args.forWordVectors(Args.model_name.sg);
        argz.thread = 16;
        argz.epoch = 10;
        argz.dim = 250;
        argz.bucket = 10;
        argz.minCount = 10;
        argz.minn = 0;
        argz.maxn = 0;
        //argz.wordNgrams = 2;
        argz.subWordHashProvider = new Dictionary.EmptySubwordHashProvider();
        //argz.subWordHashProvider = new Dictionary.CharacterNgramHashProvider(argz.minn, argz.maxn);

        FastText fastText = FastText.train(input, argz);
        Log.info("Saving vmodel to %s", modelFile);
        fastText.saveModel(modelFile);
    }

    public void onlySentences(Path input, Path output) throws IOException {
        WebCorpus corpus = new WebCorpus("web-news", "all");
        corpus.addDocuments(WebCorpus.loadDocuments(input));
        Log.info("Corpus loaded. There are %d documents.", corpus.documentCount());
        corpus.save(output, true);
    }

    public void removeDuplicates(Path input, Path output, int k) throws IOException {
        List<String> all = Files.readAllLines(input);
        Log.info("Sentence count = %d", all.size());
        Histogram<String> h = new Histogram<>(10_000_000);
        h.add(all);
        for (String s : h.getSortedList()) {
            int count = h.getCount(s);
            if (count > k) {
                h.set(s, k);
            }
        }
        int newCount = 0;
        try (PrintWriter pw = new PrintWriter(output.toFile(), "utf-8")) {
            for (String s : all) {
                if (h.getCount(s) > 0) {
                    pw.println(s);
                    h.decrementIfPositive(s);
                    newCount++;
                }
            }
        }
        Log.info("New count = %d", newCount);

    }

    public void prepareCorpus(Path root, Path target) throws IOException {

        Set<Long> hashes = new HashSet<>();

        List<Path> files = new ArrayList<>();
        if (root.toFile().isFile()) {
            files.add(root);
        } else {
            files.addAll(Files.walk(root).filter(s -> s.toFile().isFile()).collect(Collectors.toList()));
        }
        files.sort(Comparator.comparing(Path::toString));
        WebCorpus corpus = new WebCorpus("web-news", "all");
        int duplicateCount = 0;

        TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;

        for (Path file : files) {
            Log.info("Adding %s", file);
            List<WebDocument> docs = WebCorpus.loadDocuments(file);

            for (WebDocument doc : docs) {

                doc.setContent(extractor.fromParagraphs(doc.getLines()));

                doc.setContent(normalizeLines(doc.getLines()));
                if (hashes.contains(doc.getHash())) {
                    duplicateCount++;
                    continue;
                }
                if (doc.contentLength() < 50) {
                    continue;
                }
                hashes.add(doc.getHash());
                corpus.addDocument(doc);
            }
            Log.info("Total doc count = %d Duplicate count= %d", corpus.documentCount(), duplicateCount);
        }
        Log.info("Total amount of files = %d", corpus.getDocuments().size());
        corpus.save(target, false);
    }

    public List<String> normalizeLines(List<String> lines) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String line : lines) {
            if (line.length() < 10) {
                continue;
            }
            if (line.contains("http") || line.contains("@")) {
                continue;
            }
            String e = normalizeLine(line);
            if (e.length() < 10) {
                continue;
            }
            result.add(e);
        }
        return new ArrayList<>(result);
    }


    public String normalizeLine(String input) {
        TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;
        List<Token> tokens = lexer.tokenize(input);
        List<String> reduced = new ArrayList<>();
        for (Token token : tokens) {
            if (token.getType() == TurkishLexer.PercentNumeral ||
                    token.getType() == TurkishLexer.Number ||
                    //token.getType() == TurkishLexer.Punctuation ||
                    token.getType() == TurkishLexer.RomanNumeral ||
                    token.getType() == TurkishLexer.Time ||
                    token.getType() == TurkishLexer.UnknownWord ||
                    token.getType() == TurkishLexer.Unknown) {
                continue;
            }
            String tokenStr = token.getText();
            reduced.add(tokenStr.replaceAll("'’", "").toLowerCase(Turkish.LOCALE));
        }
        return String.join(" ", reduced);
    }

}
