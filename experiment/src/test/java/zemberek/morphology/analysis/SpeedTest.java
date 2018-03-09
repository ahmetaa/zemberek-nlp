package zemberek.morphology.analysis;

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
import zemberek.core.logging.Log;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class SpeedTest {

  @Test
  @Ignore(value = "Speed Test.")
  public void testNewsCorpusNoCache() throws IOException {
    Path p = Paths.get("src/main/resources/corpora/cnn-turk-10k");
    List<String> sentences = getSentences(p);
    RootLexicon lexicon = TurkishDictionaryLoader.loadDefaultDictionaries();
    InterpretingAnalyzer analyzer = new InterpretingAnalyzer(lexicon);

    Stopwatch sw = Stopwatch.createStarted();

    int tokenCount = 0;
    int noAnalysis = 0;
    int sentenceCount = 0;
    for (String sentence : sentences) {
      List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
      for (Token token : tokens) {
        if(token.getType()== TurkishLexer.Punctuation) {
          continue;
        }
        tokenCount ++;
        List<AnalysisResult> results = analyzer.analyze(token.getText());
        if (results.size() == 0) {
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
    Log.info("%nElapsed = %.2f seconds", seconds);
    Log.info("%nToken Count (No Punc) = %d %nParse Ratio = %.4f%nSpeed = %.2f tokens/sec",
        tokenCount, parseRatio, speed);
  }

  private List<String> getSentences(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
    return TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines);
  }

}
