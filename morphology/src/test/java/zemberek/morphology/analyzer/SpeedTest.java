package zemberek.morphology.analyzer;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.antlr.v4.runtime.Token;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class SpeedTest {

  @Test
  @Ignore(value = "Speed Test.")
  public void testNewsCorpus() throws IOException {
    //Path p = Paths.get("/media/aaa/Data/corpora/me-sentences/www.aljazeera.com.tr/2018-02-22");
    Path p = Paths.get("src/main/resources/corpora/cnn-turk-10k");
    List<String> sentences = getSentences(p);
    TurkishMorphology analyzer = TurkishMorphology.createWithDefaults();

    Stopwatch sw = Stopwatch.createStarted();

    int tokenCount = 0;
    int noAnalysis = 0;
    int sentenceCount = 0;
    Histogram<String> failedWords = new Histogram<>(100000);
    for (String sentence : sentences) {
      List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
      for (Token token : tokens) {
        if (token.getType() == TurkishLexer.Punctuation) {
          continue;
        }
        tokenCount++;
        WordAnalysis results = analyzer.analyze(token.getText());
        if (!results.isCorrect()) {
          noAnalysis++;
          failedWords.add(token.getText());
        }
      }
      sentenceCount++;
      if (sentenceCount % 2000 == 0) {
        Log.info("%d tokens analyzed.", tokenCount);
      }
    }
    double seconds = sw.stop().elapsed(TimeUnit.MILLISECONDS) / 1000d;
    double speed = tokenCount / seconds;
    double parseRatio = 100 - (noAnalysis * 100d / tokenCount);
    System.out.println(analyzer.getCache());
    Log.info("%nElapsed = %.2f seconds", seconds);
    Log.info("%nToken Count (No Punc) = %d %nParse Ratio = %.4f%nSpeed = %.2f tokens/sec%n",
        tokenCount, parseRatio, speed);
    Log.info("Saving Unknown Tokens");
    failedWords.saveSortedByCounts(Paths.get("unk.freq"), " ");
    failedWords.saveSortedByKeys(Paths.get("unk"), " ", Turkish.STRING_COMPARATOR_ASC);
  }

  private List<String> getSentences(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
    return TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines);
  }

}
