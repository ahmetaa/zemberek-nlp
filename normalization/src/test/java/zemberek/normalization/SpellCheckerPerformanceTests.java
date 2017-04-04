package zemberek.normalization;

import com.google.common.base.Stopwatch;
import com.google.common.io.Resources;
import org.antlr.v4.runtime.Token;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SpellCheckerPerformanceTests {

    @Test
    @Ignore(value = "Not a test.")
    public void correctWordFindingTest() throws Exception {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);
        TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;
        TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

        Path path = new File(Resources.getResource("spell-checker-test.txt").getFile()).toPath();
        List<String> lines = Files.readAllLines(path);
        List<String> sentences = extractor.fromParagraphs(lines);

        Stopwatch sw = Stopwatch.createStarted();

        Histogram<String> incorrectFound = new Histogram<>();
        Histogram<String> correctFound = new Histogram<>();

        for (String sentence : sentences) {
            List<Token> tokens = tokenizer.tokenize(sentence);
            for (Token token : tokens) {
                String text = token.getText();
                if (!spellChecker.check(text)) {
                    incorrectFound.add(text);
                } else {
                    correctFound.add(text);
                }
            }
        }
        Log.info("Elapsed = %d", sw.elapsed(TimeUnit.MILLISECONDS));
        Log.info("Incorrect (total/unique) = %d / %d", incorrectFound.totalCount(), incorrectFound.size());
        Log.info("Correct (total/unique) = %d / %d", correctFound.totalCount(), correctFound.size());
        incorrectFound.saveSortedByCounts(Paths.get("incorrect.txt"), " : ");
        correctFound.saveSortedByCounts(Paths.get("correct.txt"), " : ");

/*
        Path lmPath = Paths.get(ClassLoader.getSystemResource("lm-bigram.slm").toURI());
        SmoothLm model = SmoothLm.builder(lmPath.toFile()).build();
*/
    }


}
