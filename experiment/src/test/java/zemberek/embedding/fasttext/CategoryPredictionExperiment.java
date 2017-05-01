package zemberek.embedding.fasttext;

import org.antlr.v4.runtime.Token;
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
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static zemberek.embedding.fasttext.AutomaticLabelingExperiment.saveSets;

public class CategoryPredictionExperiment {

    private Path experimentRoot;
    private Path rawCorpusRoot;

    private CategoryPredictionExperiment(Path experimentRoot, Path rawCorpusRoot) {
        this.experimentRoot = experimentRoot;
        this.rawCorpusRoot = rawCorpusRoot;
    }

    /**
     * run with -Xmx8G or more.
     */
    public static void main(String[] args) throws Exception {

        Path expRoot = Paths.get("/media/data/corpora/cat-exp");

        new CategoryPredictionExperiment(
                expRoot,
                expRoot.resolve("www.cnnturk.com")).runExperiment();
    }

    private void runExperiment() throws Exception {
        Path corpusPath = experimentRoot.resolve("category.corpus");
        Path train = experimentRoot.resolve("category.train");
        Path test = experimentRoot.resolve("category.test");
        Path modelPath = experimentRoot.resolve("category.model");
        Path predictionPath = experimentRoot.resolve("category.predictions");
        extractCategoryDocuments(rawCorpusRoot, corpusPath);
        boolean useOnlyTitles = true;
        boolean useLemmas = true;
        generateSets(corpusPath, train, test, useOnlyTitles, useLemmas);

        FastText fastText;

        if (modelPath.toFile().exists()) {
            Log.info("Reusing existing model %s", modelPath);
            fastText = FastText.load(modelPath);
        } else {
            Args argz = Args.forSupervised();
            argz.thread = 4;
            argz.model = Args.model_name.sup;
            argz.loss = Args.loss_name.softmax;
            argz.threadSafe = false;
            argz.epoch = 50;
            argz.wordNgrams = 2;
            argz.minCount = 0;
            argz.lr = 0.2;
            argz.dim = 100;
            argz.bucket = 5_000_000;

            fastText = FastText.train(train, argz);
            fastText.saveModel(modelPath);
        }

        fastText.test(test, 1);

        WebCorpus corpus = new WebCorpus("corpus", "labeled");
        corpus.addDocuments(WebCorpus.loadDocuments(corpusPath));
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

    private void generateSets(
            Path input,
            Path train,
            Path test,
            boolean useOnlyTitle,
            boolean useRoots) throws IOException {

        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

        TurkishSentenceAnalyzer analyzer = new TurkishSentenceAnalyzer(morphology, new Z3MarkovModelDisambiguator());

        WebCorpus corpus = new WebCorpus("category", "category");
        Log.info("Loading corpus from %s", input);
        corpus.addDocuments(WebCorpus.loadDocuments(input));
        List<String> set = new ArrayList<>(corpus.documentCount());

        TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;

        Histogram<String> categoryCounts = new Histogram<>();
        for (WebDocument document : corpus.getDocuments()) {
            String category = document.getCategory();
            if (category.length() > 0) {
                categoryCounts.add(category);
            }
        }

        Log.info("All category count = %d", categoryCounts.size());
        categoryCounts.removeSmaller(20);
        Log.info("Reduced label count = %d", categoryCounts.size());

        Log.info("Extracting data from %d documents ", corpus.documentCount());
        int c = 0;

        for (WebDocument document : corpus.getDocuments()) {
            if (document.getCategory().length() == 0) {
                continue;
            }
            if (useOnlyTitle && document.getTitle().length() == 0) {
                continue;
            }

            String content = document.getContentAsString();
            String title = document.getTitle();

            List<Token> docTokens = useOnlyTitle ? lexer.tokenize(title) : lexer.tokenize(content);
            List<String> reduced = new ArrayList<>(docTokens.size());

            String category = document.getCategory();
            if (categoryCounts.contains(category)) {
                category = "__label__" + document.getCategory().replaceAll("[ ]+", "_").toLowerCase(Turkish.LOCALE);
            } else {
                continue;
            }

            for (Token token : docTokens) {
                if (
                        token.getType() == TurkishLexer.PercentNumeral ||
                                token.getType() == TurkishLexer.Number ||
                                token.getType() == TurkishLexer.Punctuation ||
                                token.getType() == TurkishLexer.RomanNumeral ||
                                token.getType() == TurkishLexer.Time ||
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

            set.add("#" + document.getId() + " " + category + " " + join.replaceAll("[']", "").toLowerCase(Turkish.LOCALE));

            if (c++ % 1000 == 0) {
                Log.info("%d of %d processed.", c, corpus.documentCount());
            }
        }

        Log.info("Generate train and test set.");

        saveSets(train, test, new LinkedHashSet<>(set));
    }

    private void extractCategoryDocuments(Path root, Path categoryFile) throws IOException {

        List<Path> files = Files.walk(root).filter(s -> s.toFile().isFile()).collect(Collectors.toList());
        files.sort(Comparator.comparing(Path::toString));
        WebCorpus corpus = new WebCorpus("category", "category");
        for (Path file : files) {
            if (file.toFile().isDirectory()) {
                continue;
            }
            Log.info("Adding %s", file);
            List<WebDocument> doc = WebCorpus.loadDocuments(file);
            List<WebDocument> labeled = doc.stream()
                    .filter(s -> s.getCategory().length() > 0 && s.getContentAsString().length() > 200)
                    .collect(Collectors.toList());
            corpus.addDocuments(labeled);
        }
        Log.info("Total amount of files = %d", corpus.getDocuments().size());
        WebCorpus noDuplicates = corpus.copyNoDuplicates();
        Log.info("Corpus size = %d, After removing duplicates = %d",
                corpus.documentCount(),
                noDuplicates.documentCount());
        Log.info("Saving corpus to %s", categoryFile);
        noDuplicates.save(categoryFile, false);
    }

}
