package zemberek.normalization;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class BotExperiment {

  public static void main(String[] args) throws IOException {

    RootLexicon lexicon = TurkishDictionaryLoader.loadFromResources(
        "tr/master-dictionary.dict",
        "tr/non-tdk.dict",
        "tr/proper.dict",
        "tr/proper-from-corpus.dict",
        "tr/abbreviations.dict",
        "tr/person-names.dict"
    );

    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .useLexicon(lexicon)
        .disableUnidentifiedTokenAnalyzer()
        .morphotactics(new InformalTurkishMorphotactics(lexicon))
        .build();

    Path root = Paths.get("/home/aaa/data/normalization");
    Path splitList = root.resolve("split");
    Path rawLines = root.resolve("bot/raw");
    Path nodup = root.resolve("bot/sentences-nodup");
    Path sentencesNodup = root.resolve("bot/sentences-nodup");
    Path sentencesNodupTokenized = root.resolve("bot/sentences-nodup-tokenized");
    //Path sentencesNodupTokenized = root.resolve("bot/test");
    Path output = root.resolve("bot/report.txt");

    Path lmPath = root.resolve("lm.slm");
    SmoothLm lm = SmoothLm.builder(lmPath).logBase(Math.E).build();

    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(morphology, splitList, lm);

    // preprocess(rawLines, nodup, sentencesNodup, sentencesNodupTokenized);

    //normalize(normalizer, sentencesNodupTokenized, output);

    //normalizer.decode("Acab yarn akram n ypsak");
    String input = "Amet ee gldi";
    List<String> seq = normalizer.decode(input);
    Log.info(input);
    Log.info(String.join(" ", seq));

    Log.info("Done.");

  }

  static void preprocess(
      Path in,
      Path linesNodupPath,
      Path sentencesNoDupPath,
      Path sentencesNoDupTokenizedPath
  ) throws IOException {
    List<String> lines = TextIO.loadLines(in);
    Log.info("There are %d lines in %s", lines.size(), in);

    LinkedHashSet<String> linesNodup = new LinkedHashSet<>();
    LinkedHashSet<String> sentencesNoDup = new LinkedHashSet<>();
    LinkedHashSet<String> sentencesNoDupTokenized = new LinkedHashSet<>();
    for (String line : lines) {
      linesNodup.add(line);
      List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraph(line);
      sentencesNoDup.addAll(sentences);
      for (String sentence : sentences) {
        List<String> tokenized = TurkishTokenizer.DEFAULT.tokenizeToStrings(sentence);
        sentencesNoDupTokenized.add(String.join(" ", tokenized));
      }
    }
    Log.info("%d lines  after removing duplicates in %s",
        linesNodup.size(), linesNodupPath);
    Log.info("%d sentences after removing duplicates in %s",
        sentencesNoDup.size(), sentencesNoDupPath);

    Files.write(linesNodupPath, linesNodup, StandardCharsets.UTF_8);
    Files.write(sentencesNoDupPath, sentencesNoDup, StandardCharsets.UTF_8);
    Files.write(sentencesNoDupTokenizedPath, sentencesNoDupTokenized, StandardCharsets.UTF_8);
  }

  static void normalize(
      TurkishSentenceNormalizer normalizer,
      Path in,
      Path normalized)
      throws IOException {
    List<String> lines = Files.readAllLines(in, StandardCharsets.UTF_8);
    Stopwatch sw = Stopwatch.createStarted();
    try (PrintWriter pw = new PrintWriter(normalized.toFile(), "utf-8")) {
      int tokenCount = 0, lineCount = 0;
      for (String line : lines) {
        tokenCount += TurkishTokenizer.DEFAULT.tokenize(line).size();
        lineCount++;
        String n = normalizer.normalize(line);
        if (!n.equals(line)) {
          pw.println(line);
          pw.println(n);
          pw.println();
        } else {
          pw.println(line);
          pw.println();
        }
      }
      pw.println("Line count = " + lineCount);
      pw.println("Token count = " + tokenCount);
      double elapsed = sw.elapsed(TimeUnit.MILLISECONDS) / 1000d;
      pw.println("Time to process = " + String.format("%.2f", elapsed) + " seconds.");
      pw.println("Speed = " + String.format("%.2f", tokenCount / elapsed) + " tokens/seconds.");
    }
  }

}
