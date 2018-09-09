package zemberek.normalization;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class BotExperiment {

  public static void main(String[] args) throws IOException {
    TurkishMorphology formal = TurkishMorphology.createWithDefaults();
    TurkishMorphology informal = TurkishMorphology
        .builder()
        .addDefaultBinaryDictionary()
        .morphotactics(new InformalTurkishMorphotactics(formal.getLexicon()))
        .build();
    Path root = Paths.get("/media/ahmetaa/depo/zemberek/data/normalization");
    Path splitList = root.resolve("split");
    Path rawLines = root.resolve("bot/raw");
    Path nodup = root.resolve("bot/sentences-nodup");
    Path sentencesNodup = root.resolve("bot/sentences-nodup");
    Path sentencesNodupTokenized = root.resolve("bot/sentences-nodup-tokenized");
    //Path sentencesNodupTokenized = root.resolve("bot/test");
    Path normalizationOutput = root.resolve("bot/normalized");

    Path lmPath = root.resolve("lm.slm");
    SmoothLm lm = SmoothLm.builder(lmPath).logBase(Math.E).build();

    TurkishSentenceNormalizer normalizer =
        new TurkishSentenceNormalizer(informal, splitList, lm);

    //preprocess(rawLines, nodup, sentencesNodup, sentencesNodupTokenized);

    normalize(normalizer, sentencesNodupTokenized, normalizationOutput);
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

  static void normalize(TurkishSentenceNormalizer normalizer, Path in, Path out)
      throws IOException {
    List<String> lines = Files.readAllLines(in, StandardCharsets.UTF_8);
    try (PrintWriter pw = new PrintWriter(out.toFile(), "utf-8")) {
      for (String line : lines) {
        String n = normalizer.normalize(line);
        if (!n.equals(line)) {
          pw.println(line);
          pw.println(n);
          pw.println();
        }
      }
    }

  }

}
