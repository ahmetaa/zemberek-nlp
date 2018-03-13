package zemberek.morphology._analyzer;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.structure.Turkish;

public class CoverageTest {

  @Test
  @Ignore(value = "Coverage Test.")
  public void testCoverage() throws IOException {
    Path oflazerAndZemberek = Paths.get("../data/zemberek-oflazer/oflazer-zemberek-parsed.txt");
    LinkedHashSet<String> lines = new LinkedHashSet<>(
        Files.readAllLines(oflazerAndZemberek, StandardCharsets.UTF_8));

    RootLexicon lexicon = TurkishDictionaryLoader.loadDefaultDictionaries();
    InterpretingAnalyzer analyzer = new InterpretingAnalyzer(lexicon);

    List<String> failedWords = new ArrayList<>();

    int i = 0;
    Stopwatch sw = Stopwatch.createStarted();
    for (String line : lines) {
      List<AnalysisResult> results = analyzer.analyze(line);
      if (results.size() == 0) {
        failedWords.add(line);
      }
      i++;
      if (i % 100_000 == 0) {
        logResult(failedWords, i, sw);
      }
    }
    logResult(failedWords, lines.size(), sw);
    Files.write(
        Paths.get("../data/zemberek-oflazer/new-analyzer-failed.txt"),
        failedWords, StandardCharsets.UTF_8);
  }

  private void logResult(List<String> failedWords, int i, Stopwatch sw) {
    double coverage = 100 - (failedWords.size() * 100d / i);
    double seconds = sw.elapsed(TimeUnit.MILLISECONDS) / 1000d;
    double speed = i / seconds;
    Log.info("%d analysed. Coverage = %.3f . Speed = %.3f tokens/sec", i, coverage, speed);
  }


}
