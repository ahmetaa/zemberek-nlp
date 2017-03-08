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
}
