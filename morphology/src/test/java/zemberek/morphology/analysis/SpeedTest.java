package zemberek.morphology.analysis;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class SpeedTest {

  @Test
  @Ignore(value = "Speed Test.")
  public void testNewsCorpus() throws IOException {
    //Path p = Paths.get("/media/aaa/Data/corpora/me-sentences/www.aljazeera.com.tr/2018-02-22");
    Path p = Paths.get("src/test/resources/corpora/cnn-turk-10k");
    List<String> sentences = getSentences(p);
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    Stopwatch sw = Stopwatch.createStarted();

    int tokenCount = 0;
    int noAnalysis = 0;
    int sentenceCount = 0;
    Histogram<String> failedWords = new Histogram<>(100000);
    for (String sentence : sentences) {
      List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
      for (Token token : tokens) {
        if (token.getType() == Token.Type.Punctuation) {
          continue;
        }
        tokenCount++;
        WordAnalysis results = morphology.analyze(token.getText());
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
    Log.info("%nElapsed = %.2f seconds", seconds);
    Log.info("%nToken Count (No Punc) = %d %nParse Ratio = %.4f%nSpeed = %.2f tokens/sec%n",
        tokenCount, parseRatio, speed);
    Log.info("Saving Unknown Tokens");
    failedWords.saveSortedByCounts(Paths.get("unk.freq"), " ");
    failedWords.saveSortedByKeys(Paths.get("unk"), " ", Turkish.STRING_COMPARATOR_ASC);
  }


  private static void testForVisualVm(Path p, TurkishMorphology analyzer) throws IOException {
    //Path p = Paths.get("/media/aaa/Data/corpora/me-sentences/www.aljazeera.com.tr/2018-02-22");
    List<String> sentences = getSentences(p);

    Stopwatch sw = Stopwatch.createStarted();

    int tokenCount = 0;
    int noAnalysis = 0;
    int sentenceCount = 0;
    for (String sentence : sentences) {

      List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
      for (Token token : tokens) {
        tokenCount++;
        WordAnalysis results = analyzer.analyze(token.getText());
        if (!results.isCorrect()) {
          noAnalysis++;
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
  }

  public static void main(String[] args) throws IOException {
    Path p = Paths.get("morphology/src/test/resources/corpora/cnn-turk-10k");

    TurkishMorphology analyzer = TurkishMorphology.createWithDefaults();
    for (int i = 0; i < 10; i++) {
      testForVisualVm(p, analyzer);
      analyzer.invalidateCache();
      System.in.read();
    }
  }

  private static List<String> getSentences(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
    lines = lines.stream().map(s ->
        s.replaceAll("\\s+|\\u00a0", " ")
            .replaceAll("[\\u00ad]", "").
            replaceAll("[…]", "...")
    ).collect(Collectors.toList());
    return TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines);
  }

}
