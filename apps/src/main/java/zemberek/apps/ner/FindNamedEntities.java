package zemberek.apps.ner;

import com.beust.jcommander.Parameter;
import com.google.common.base.Stopwatch;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.text.TextUtil;
import zemberek.morphology.TurkishMorphology;
import zemberek.ner.NerSentence;
import zemberek.ner.PerceptronNer;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class FindNamedEntities extends NerAppBase {

  @Parameter(
      names = {"--modelRoot", "-m"},
      required = true,
      description = "Model files root. ")
  public Path modelRoot;

  @Parameter(
      names = {"--input", "-i"},
      required = true,
      description = "Input to be processed.")
  public Path inputPath;

  @Override
  public String description() {
    return "Finds named entities from a Turkish text file.";
  }

  @Override
  public void run() throws Exception {

    initializeOutputDir();
    IOUtil.checkDirectoryArgument(modelRoot, "Model Root");
    IOUtil.checkFileArgument(inputPath, "Input File");

    Path out = outDir.resolve(inputPath.toFile().getName() + ".ne");

    List<String> lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
    List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines);

    Log.info("There are %d lines and about %d sentences", lines.size(), sentences.size());

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    PerceptronNer ner = PerceptronNer.loadModel(modelRoot, morphology);

    Stopwatch sw = Stopwatch.createStarted();

    int tokenCount = 0;
    try (PrintWriter pw = new PrintWriter(out.toFile(), "UTF-8")) {
      for (String sentence : sentences) {
        sentence = TextUtil.normalizeApostrophes(sentence);
        sentence = TextUtil.normalizeQuotesHyphens(sentence);
        sentence = TextUtil.normalizeSpacesAndSoftHyphens(sentence);
        List<String> words = TurkishTokenizer.DEFAULT.tokenizeToStrings(sentence);
        tokenCount += words.size();
        NerSentence result = ner.findNamedEntities(sentence, words);
        pw.println(result.getAsTrainingSentence(annotationStyle));
      }
    }

    double secs = sw.elapsed(TimeUnit.MILLISECONDS) / 1000d;
    Log.info("Token count = %s", tokenCount);
    Log.info("File processed in %.4f seconds.", secs);
    Log.info("Speed = %.2f tokens/sec", tokenCount / secs);

    Log.info("Result is written in %s", out);
  }

  public static void main(String[] args) {
    new FindNamedEntities().execute(args);
  }
}
