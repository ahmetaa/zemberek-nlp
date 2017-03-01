package zemberek.embedding.fasttext;

import org.antlr.v4.runtime.Token;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.ScoredItem;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.corpus.WebCorpus;
import zemberek.corpus.WebDocument;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.analysis.tr.TurkishSentenceAnalyzer;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenizer.ZemberekLexer;
import zemberek.tokenizer.antlr.TurkishLexer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FastTextTest {

    /**
     * Runs the dbpedia classification task.
     * run with -Xms8G or more.
     */
    @Test
    @Ignore("Not an actual Test.")
    public void dbpediaClassificationTest() throws Exception {

        Path inputRoot = Paths.get("/home/ahmetaa/projects/fastText/data");
        Path trainFile = inputRoot.resolve("dbpedia.train");
        Path modelPath = Paths.get("/home/ahmetaa/data/vector/fasttext/dbpedia.bin");

        FastText fastText;

        if (modelPath.toFile().exists()) {
            fastText = FastText.load(modelPath);
        } else {
            Args argz = Args.forSupervised();
            argz.thread = 4;
            argz.model = Args.model_name.sup;
            argz.loss = Args.loss_name.softmax;
            argz.threadSafe = false;
            argz.epoch = 5;
            argz.wordNgrams = 2;
            argz.minCount = 1;
            argz.lr = 0.1;
            argz.dim = 10;
            argz.bucket = 10_000_000;

            fastText = FastText.train(trainFile, argz);
            fastText.saveModel(modelPath);
        }

        Path testFile = inputRoot.resolve("dbpedia.test");
        Log.info("Testing started.");
        fastText.test(testFile, 1);
    }

    /**
     * Generates word vectors using skip-gram model.
     * run with -Xms8G or more.
     */
    @Test
    @Ignore("Not an actual Test.")
    public void skipgram() throws Exception {
        Args argz = Args.forWordVectors(Args.model_name.sg);
        argz.thread = 8;
        argz.epoch = 10;
        argz.dim = 150;
        argz.bucket = 2_000_000;
        argz.minn = 3;
        argz.maxn = 6;
        argz.subWordHashProvider = new Dictionary.CharacterNgramHashProvider(argz.minn, argz.maxn);

        Path input = Paths.get("/home/ahmetaa/data/nlp/corpora/corpus-1M.txt");

        Path outRoot = Paths.get("/home/ahmetaa/data/vector/fasttext");

        FastText fastText = FastText.train(input, argz);
        Path vectorFile = outRoot.resolve("1M-skipgram.vec");
        Log.info("Saving vectors to %s", vectorFile);
        fastText.saveVectors(vectorFile);
    }

    /**
     * run with -Xms8G or more.
     */
    @Test
    @Ignore("Not an actual Test.")
    public void labelExperiment() throws Exception {
        Path root = Paths.get("/media/data/corpora/raw2/www.cnnturk.com");
        Path labeledNewsDocuments = Paths.get("/media/data/corpora/raw2/cnnturk.labeled");
        Path out = Paths.get("/media/data/corpora/raw2");
        Path train = out.resolve("cnnturk.train");
        Path test = out.resolve("cnnturk.test");
        Path modelPath = out.resolve("cnnturk.model");
        Path predictionPath = out.resolve("cnnturk.prediction");

        TurkishMorphology morphology = TurkishMorphology.builder()
                .addDefaultDictionaries()
                .cacheParameters(50_000, 100_000)
                .build();

        TurkishSentenceAnalyzer analyzer = new TurkishSentenceAnalyzer(morphology, new Z3MarkovModelDisambiguator());
        //extractLabeledDocuments(root, labeledNewsDocuments);
        //generateSetsForLabelExperiment(labeledNewsDocuments, train, test, analyzer, true);

        FastText fastText;


        if (modelPath.toFile().exists()) {
            fastText = FastText.load(modelPath);
        } else {
            Args argz = Args.forSupervised();
            argz.thread = 16;
            argz.loss = Args.loss_name.hs;
            argz.threadSafe = false;
            argz.epoch = 150;
            argz.wordNgrams = 2;
            argz.minCount = 5;
            argz.lr = 0.25;
            argz.dim = 200;
            argz.bucket = 10_000_000;

            fastText = FastText.train(train, argz);
            fastText.saveModel(modelPath);
        }

        WebCorpus corpus = new WebCorpus("www.cnnturk.com", "labeled");
        corpus.addDocuments(WebCorpus.loadDocuments(labeledNewsDocuments));

        Log.info("Testing started.");
        List<String> testLines = Files.readAllLines(test, StandardCharsets.UTF_8);
        try (PrintWriter pw = new PrintWriter(predictionPath.toFile(), "utf-8")) {
            for (String testLine : testLines) {
                String id = testLine.substring(0, testLine.indexOf(' ')).substring(1);
                WebDocument doc = corpus.getDocument(id);
                List<ScoredItem<String>> res = fastText.predict(testLine, 7);
                List<String> predictedLabels = new ArrayList<>();
                for (ScoredItem<String> re : res) {
                    predictedLabels.add(String.format("%s (%.2f)",
                            re.item.replaceAll("__label__", "").replaceAll("_", " "), re.score));
                }

                pw.println("id = " + id);
                pw.println();
                pw.println(doc.getContent().replaceAll("[\n\r]+", "\n"));
                pw.println();
                pw.println("Actual Labels = " + String.join(", ", doc.getLabels()));
                pw.println("Predictions   = " + String.join(", ", predictedLabels));
                pw.println();
                pw.println("------------------------------------------------------");
                pw.println();
            }
        }
        Log.info("Done.");
    }

    private void generateSetsForLabelExperiment(
            Path input,
            Path train,
            Path test,
            TurkishSentenceAnalyzer analyzer,
            boolean useRoots) throws IOException {
        WebCorpus corpus = new WebCorpus("www.cnnturk.com", "labeled");
        corpus.addDocuments(WebCorpus.loadDocuments(input));
        List<String> set = new ArrayList<>(corpus.count());

        ZemberekLexer lexer = new ZemberekLexer(true);

        Log.info("Extracting data.");


        Histogram<String> labelCounts = new Histogram<>();
        for (WebDocument document : corpus.getPages()) {
            List<String> labels = document.getLabels();
            labelCounts.add(labels);
        }

        Log.info("All label count = %d", labelCounts.size());
        labelCounts.removeSmaller(5);
        Log.info("Reduced label count = %d", labelCounts.size());
        Log.info("Extracting data from %d documents ", corpus.count());
        int c = 0;

        Set<Long> contentHash = new HashSet<>();

        for (WebDocument document : corpus.getPages()) {
            Long hash = document.contentHash();
            if (contentHash.contains(hash)) {
                continue;
            }
            contentHash.add(hash);
            String content = document.getContent();
            List<Token> docTokens = lexer.tokenizeAll(content);

            List<String> labelTags = new ArrayList<>();

            boolean labelFound = false;
            for (String label : document.getLabels()) {
                if (labelCounts.contains(label)) {
                    labelTags.add("__label__" + label.replaceAll("[ ]+", "_").toLowerCase(Turkish.LOCALE));
                    labelFound = true;
                }
            }
            if (!labelFound) {
                continue;
            }

            List<String> reduced = new ArrayList<>(docTokens.size());
            for (Token token : docTokens) {
                if (token.getType() == TurkishLexer.PercentNumeral ||
                        token.getType() == TurkishLexer.Number ||
                        token.getType() == TurkishLexer.Punctuation ||
                        token.getType() == TurkishLexer.RomanNumeral ||
                        token.getType() == TurkishLexer.TimeHours ||
                        token.getType() == TurkishLexer.UnknownWord ||
                        token.getType() == TurkishLexer.Unknown) {
                    continue;
                }
                String tokenStr = token.getText();
                reduced.add(tokenStr);
            }
            String join = String.join(" ", reduced);

            if (useRoots) {
                SentenceAnalysis analysis = analyzer.analyze(join);
                analyzer.disambiguate(analysis);
                List<String> res = new ArrayList<>();
                for (SentenceAnalysis.Entry e : analysis) {
                    WordAnalysis best = e.parses.get(0);
                    if (best.isUnknown()) {
                        res.add(e.input);
                        continue;
                    }
                    List<String> lemmas = best.getLemmas();
                    if (lemmas.size() == 0) {
                        continue;
                    }
                    res.add(lemmas.get(lemmas.size() - 1));
                }
                join = String.join(" ", res);
            }

            String labelStr = String.join(" ", labelTags);

            set.add("#" + document.getId() + " " + labelStr + " " + join.replaceAll("[']", "").toLowerCase(Turkish.LOCALE));

            if (c++ % 1000 == 0) {
                Log.info("%d of %d processed.", c, corpus.count());
            }
        }

        Log.info("Generate train and test set.");

        saveSets(train, test, set);
    }

    private void saveSets(Path train, Path test, List<String> set) throws IOException {
        try (PrintWriter pwTrain = new PrintWriter(train.toFile(), "utf-8");
             PrintWriter pwTest = new PrintWriter(test.toFile(), "utf-8")) {

            int t = 0;
            int split = (int) (set.size() * 0.95);

            for (String s : set) {
                if (t < split) {
                    pwTrain.println(s);
                } else {
                    pwTest.println(s);
                }
                t++;
            }
        }
    }

    /**
     * run with -Xms8G or more.
     */
    @Test
    @Ignore("Not an actual Test.")
    public void categoryExperiment() throws Exception {
        Path root = Paths.get("/media/data/corpora/raw3/www.cnnturk.com");
        Path labeledNewsDocuments = Paths.get("/media/data/corpora/raw3/cnnturk.labeled");
        Path out = Paths.get("/media/data/corpora/raw3");
        Path train = out.resolve("cnn-cat.train");
        Path test = out.resolve("cnn-cat.test");
        Path modelPath = out.resolve("cnn-cat.model");
        Path predictionPath = out.resolve("cnn-cat.predictions");
        extractCategoryDocuments(root, labeledNewsDocuments);
        boolean useOnlyTitles = true;
        generateCatSets(labeledNewsDocuments, train, test, useOnlyTitles);

        FastText fastText;

        if (modelPath.toFile().exists()) {
            fastText = FastText.load(modelPath);
        } else {
            Args argz = Args.forSupervised();
            argz.thread = 4;
            argz.model = Args.model_name.sup;
            //argz.loss = Args.loss_name.hs;
            argz.threadSafe = false;
            argz.epoch = 30;
            argz.wordNgrams = 1;
            argz.minCount = 0;
            argz.lr = 0.2;
            argz.dim = 30;
            argz.bucket = 10_000_000;

            fastText = FastText.train(train, argz);
            fastText.saveModel(modelPath);
        }

        fastText.test(test, 1);

        WebCorpus corpus = new WebCorpus("www.cnnturk.com", "labeled");
        corpus.addDocuments(WebCorpus.loadDocuments(labeledNewsDocuments));
        Log.info("Testing started.");
        List<String> testLines = Files.readAllLines(test, StandardCharsets.UTF_8);
        try (PrintWriter pw = new PrintWriter(predictionPath.toFile(), "utf-8")) {
            for (String testLine : testLines) {
                String id = testLine.substring(0, testLine.indexOf(' ')).substring(1);
                WebDocument doc = corpus.getDocument(id);
                List<ScoredItem<String>> res = fastText.predict(testLine, 3);
                List<String> predictedCategories = new ArrayList<>();
                for (ScoredItem<String> re : res) {
                    if (re.score < -10) {
                        continue;
                    }
                    predictedCategories.add(String.format("%s (%.2f)",
                            re.item.replaceAll("__label__", "").replaceAll("_", " "), re.score));
                }
                pw.println("id = " + id);
                pw.println();
                pw.println(doc.getTitle());
                pw.println();
                pw.println("Actual Category = " + doc.getCategory());
                pw.println("Predictions   = " + String.join(", ", predictedCategories));
                pw.println();
                pw.println("------------------------------------------------------");
                pw.println();
            }
        }
        Log.info("Done.");
    }


    private void generateCatSets(Path input, Path train, Path test, boolean useOnlyTitle) throws IOException {
        WebCorpus corpus = new WebCorpus("www.cnnturk.com", "category");
        corpus.addDocuments(WebCorpus.loadDocuments(input));
        List<String> set = new ArrayList<>(corpus.count());

        ZemberekLexer lexer = new ZemberekLexer(true);


        Histogram<String> categoryCounts = new Histogram<>();
        for (WebDocument document : corpus.getPages()) {
            String category = document.getCategory();
            if (category.length() > 0) {
                categoryCounts.add(category);
            }
        }

        Log.info("All category count = %d", categoryCounts.size());
        categoryCounts.removeSmaller(50);
        Log.info("Reduced label count = %d", categoryCounts.size());

        Log.info("Extracting data from %d documents ", corpus.count());
        int c = 0;
        Set<Long> contentHash = new HashSet<>();

        for (WebDocument document : corpus.getPages()) {
            Long hash = document.contentHash();
            if (contentHash.contains(hash)) {
                continue;
            }
            contentHash.add(hash);
            if (document.getCategory().length() == 0) {
                continue;
            }
            if (useOnlyTitle && document.getTitle().length() == 0) {
                continue;
            }

            String content = document.getContent();
            String title = document.getTitle();

            List<Token> docTokens = useOnlyTitle ? lexer.tokenizeAll(title) : lexer.tokenizeAll(content);
            List<String> reduced = new ArrayList<>(docTokens.size());

            String category = document.getCategory();
            if (categoryCounts.contains(category)) {
                reduced.add("__label__" + document.getCategory().replaceAll("[ ]+", "_").toLowerCase(Turkish.LOCALE));
            } else {
                continue;
            }

            for (Token token : docTokens) {
                if (token.getType() == TurkishLexer.PercentNumeral ||
                        token.getType() == TurkishLexer.Number ||
                        token.getType() == TurkishLexer.Punctuation ||
                        token.getType() == TurkishLexer.RomanNumeral ||
                        token.getType() == TurkishLexer.TimeHours ||
                        token.getType() == TurkishLexer.UnknownWord ||
                        token.getType() == TurkishLexer.Unknown) {
                    continue;
                }
                String tokenStr = token.getText();
                reduced.add(tokenStr.replaceAll("[']", "").toLowerCase(Turkish.LOCALE));
            }
            set.add("#" + document.getId() + " " + String.join(" ", reduced));
            if (c++ % 1000 == 0) {
                Log.info("%d of %d processed.", c, corpus.count());
            }
        }

        Log.info("Generate train and test set.");

        saveSets(train, test, set);
    }

    private void extractLabeledDocuments(Path root, Path labeledFile) throws IOException {
        List<Path> files = Files.walk(root).filter(s -> s.toFile().isFile()).collect(Collectors.toList());
        WebCorpus corpus = new WebCorpus("www.cnnturk.com", "labeled");
        files.sort(Comparator.comparing(Path::toString));
        for (Path file : files) {
            Log.info("Adding %s", file);
            List<WebDocument> doc = WebCorpus.loadDocuments(file);
            List<WebDocument> labeled = doc.stream()
                    .filter(s -> s.getLabelString().length() > 0 && s.getContent().length() > 200)
                    .collect(Collectors.toList());
            corpus.addDocuments(labeled);
        }
        Log.info("Total amount of files = %d", corpus.getPages().size());
        corpus.save(labeledFile, false);
    }

    private void extractCategoryDocuments(Path root, Path categoryFile) throws IOException {

        List<Path> files = Files.walk(root).filter(s -> s.toFile().isFile()).collect(Collectors.toList());
        files.sort(Comparator.comparing(Path::toString));
        WebCorpus corpus = new WebCorpus("www.cnnturk.com", "category");
        for (Path file : files) {
            Log.info("Adding %s", file);
            List<WebDocument> doc = WebCorpus.loadDocuments(file);
            List<WebDocument> labeled = doc.stream()
                    .filter(s -> s.getCategory().length() > 0 && s.getContent().length() > 200)
                    .collect(Collectors.toList());
            corpus.addDocuments(labeled);
        }
        Log.info("Total amount of files = %d", corpus.getPages().size());
        corpus.save(categoryFile, false);
    }

}
