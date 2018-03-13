package zemberek.morphology._analyzer;

import com.google.common.base.Stopwatch;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class CoverageTest {

  @Test
  @Ignore(value = "Coverage Test.")
  public void testCoverage() throws IOException {
    String path = "../data/zemberek-oflazer/oflazer-zemberek-parsed.txt.gz";
    Log.info("Extracting coverage test file: %s" , path);
    Path oflazerAndZemberek = Paths.get(path);
    InputStream gzipStream = new GZIPInputStream(new FileInputStream(oflazerAndZemberek.toFile()));
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(gzipStream, StandardCharsets.UTF_8));

    LinkedHashSet<String> lines = reader.lines()
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Log.info("File read, analyzing.");
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
