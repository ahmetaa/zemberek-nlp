package zemberek.apps.corpus;

import com.beust.jcommander.Parameter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import zemberek.apps.ConsoleApp;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextIO;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class PreprocessTurkishCorpus extends ConsoleApp {

  @Parameter(names = {"--input", "-i"},
      required = true,
      description = "Input corpus file or directory. "
          + "If this is a directory, all files in it will be processed. Files must be in UTF-8 Format.")
  public Path input;

  @Parameter(names = {"--output", "-o"},
      required = true,
      description = "Output corpus file. One sentence per line and tokenized.")
  public Path output;

  @Parameter(names = {"--extension", "-e"},
      description = "If used, only file(s) that ends with `[extension]` will be processed.")
  public String extension;

  @Parameter(names = {"--toLowercase", "-lc"},
      description = "If used, applies Turkish lower casing to resulting sentences.")
  public boolean toLowercase = false;

  @Parameter(names = {"--operation", "-op"},
      description = "Applies operation to words. If LEMMA is selected, words are replaced with "
          + "longest lemmas.")
  public Operation operation = Operation.NONE;

  @Override
  public String description() {
    return "Applies Turkish Sentence boundary detection and tokenization to a corpus file or a "
        + "directory of corpus files. "
        + "Lines start with `<` character are ignored. It applies white space normalization and "
        + " removes soft hyphens. Sentences that contain `combining diacritic` symbols are "
        + "ignored.";
  }

  enum Operation {
    NONE,
    LEMMA
  }

  TurkishMorphology morphology;

  @Override
  public void run() throws IOException {
    System.setProperty("org.jline.terminal.dumb", "true");
    List<Path> paths = new ArrayList<>();
    if (input.toFile().isFile()) {
      paths.add(input);
    } else {
      paths.addAll(Files.walk(input, 1)
          .filter(s -> s.toFile().isFile() && (extension == null || s.endsWith(extension)))
          .collect(Collectors.toList()));
    }
    Log.info("There are %d files to process.", paths.size());
    long totalLines = 0;
    for (Path path : paths) {
      totalLines += TextIO.lineCount(path);
    }

    if (paths.size() == 0) {
      Log.info("No corpus files found for input : %s", input);
      System.exit(0);
    }
    int i = 1;
    long sentenceCount = 0;

    if (operation == Operation.LEMMA) {
      morphology = TurkishMorphology.createWithDefaults();
    }

    try (PrintWriter pw = new PrintWriter(output.toFile(), "UTF-8")) {
      ProgressBar pb = new ProgressBar("Lines", totalLines, ProgressBarStyle.ASCII);

      for (Path path : paths) {
        // process with chunks of 10.000 lines.
        BlockTextLoader loader = new BlockTextLoader(path, StandardCharsets.UTF_8, 10_000);
        for (List<String> block : loader) {

          List<String> chunk = block.stream()
              .filter(s -> !s.startsWith("<"))
              .map(TextUtil::normalizeSpacesAndSoftHyphens)
              .collect(Collectors.toList());

          List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraphs(chunk);
          sentences = sentences.stream()
              .filter(s -> !TextUtil.containsCombiningDiacritics(s))
              .map(s -> {
                if (operation == Operation.LEMMA) {
                  return replaceWordsWithLemma(s);
                } else {
                  return String.join(" ", TurkishTokenizer.DEFAULT.tokenizeToStrings(s));
                }
              })
              .map(s -> toLowercase ? s.toLowerCase(Turkish.LOCALE) : s)
              .collect(Collectors.toList());

          sentences.forEach(pw::println);

          sentenceCount += sentences.size();
          pb.stepBy(block.size());
          pb.setExtraMessage(String.format("(%d/%d)", i, paths.size()));
        }
        i++;
      }
      pb.close();
    }
    Log.info("%d sentences are written in %s", sentenceCount, output);
  }

  private String replaceWordsWithLemma(String sentence) {
    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);
    List<String> res = new ArrayList<>();
    for (SentenceWordAnalysis e : analysis) {
      SingleAnalysis best = e.getBestAnalysis();
      if (best.isUnknown()) {
        res.add(e.getWordAnalysis().getInput());
        continue;
      }
      List<String> lemmas = best.getLemmas();
      res.add(lemmas.get(lemmas.size() - 1));
    }
    return String.join(" ", res);
  }

  public static void main(String[] args) {
    new PreprocessTurkishCorpus().execute(args);
  }
}
