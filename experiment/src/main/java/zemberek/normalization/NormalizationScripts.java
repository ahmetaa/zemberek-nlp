package zemberek.normalization;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.core.ScoredItem;
import zemberek.core.collections.FixedBitVector;
import zemberek.core.collections.Histogram;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextUtil;
import zemberek.langid.LanguageIdentifier;
import zemberek.lm.compression.SmoothLm;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class NormalizationScripts {

  public static void main(String[] args) throws Exception {

    Path root = Paths.get("/media/ahmetaa/depo/zemberek/data/normalization/test");

    Path incorrect = root.resolve("incorrect");
    Path correct = root.resolve("correct");

    Path s = root.resolve("split");
    Path lm = root.resolve("lm.slm");
    //splitWords(p, s, lm, 2);

    Path quesOut = root.resolve("question-suffix");
    //getQuestionSuffixes(s, quesOut);
    //convertTweetData();

    Path repetitive = root.resolve("repetitions.hist.txt");
    //multipleLetterRepetitionWords(incorrect, repetitive);

    Path corporaRoot = Paths.get("/media/ahmetaa/depo/zemberek/data/corpora");
    cleanTwitterData(
        corporaRoot.resolve("20m-tweets-utf8"),
        corporaRoot.resolve("20m-tweets-utf8-clean")
    );

  }

  static void splitWords(Path wordFrequencyFile, Path splitFile, Path lmPath, int minWordCount)
      throws IOException {

    SmoothLm lm = SmoothLm.builder(lmPath).logBase(Math.E).build();
    Log.info("Language model = %s", lm.info());

    Histogram<String> wordFreq = Histogram.loadFromUtf8File(wordFrequencyFile, ' ');
    Log.info("%d words loaded.", wordFreq.size());

    wordFreq.removeSmaller(minWordCount);
    if (minWordCount > 1) {
      Log.info("%d words left after removing counts less than %d.",
          wordFreq.size(),
          minWordCount
      );
    }

    List<String> words = wordFreq.getSortedList();

    int unkIndex = lm.getVocabulary().getUnknownWordIndex();

    try (PrintWriter pw = new PrintWriter(splitFile.toFile(), "utf-8")) {
      for (String word : words) {

        if (word.length() < 5 || word.contains("-")) {
          continue;
        }

        List<ScoredItem<String>> k = new ArrayList<>();

        for (int i = 1; i < word.length() - 1; i++) {
          String head = word.substring(0, i);
          String tail = word.substring(i);
          int hi = lm.getVocabulary().indexOf(head);
          int ti = lm.getVocabulary().indexOf(tail);

          if (hi == unkIndex || ti == unkIndex) {
            continue;
          }

          if (lm.ngramExists(hi, ti)) {
            k.add(new ScoredItem<>(head + " " + tail, lm.getProbability(hi, ti)));
          }
        }

        if (k.size() > 1) {
          k.sort((a, b) -> Double.compare(b.score, a.score));
        }

        if (k.size() > 0) {
          ScoredItem<String> best = k.get(0);
          if (best.score > -6) {
            pw.println(word + " = " + best.item);
          }
        }
      }
    }
  }

  static void getQuestionSuffixes(Path in, Path out) throws IOException {
    List<String> splitLines = Files.readAllLines(in, Charsets.UTF_8);
    Histogram<String> endings = new Histogram<>();
    for (String splitLine : splitLines) {
      String[] tokens = splitLine.split("=");
      String s = tokens[1].trim();
      String[] t2 = s.split("[ ]");
      if (t2.length != 2) {
        System.out.println("Problem in " + splitLine);
        continue;
      }
      String suf = t2[1];
      if (suf.startsWith("mi") ||
          suf.startsWith("mu") ||
          suf.startsWith("mı") ||
          suf.startsWith("mü")
          ) {
        endings.add(t2[1]);
      }
    }
    for (String ending : endings.getSortedList()) {
      System.out.println(ending + " " + endings.getCount(ending));
    }
    for (String ending : endings.getSortedList()) {
      System.out.println(ending);
    }

  }

  static void convertTweetData() throws IOException {
    Path in = Paths.get("/home/aaa/Downloads/20milyontweet/all_tweets.txt");
    Path out = Paths.get("/home/aaa/Downloads/20milyontweet/all_tweets-utf8");
    List<String> lines = Files.readAllLines(in, Charset.forName("iso-8859-9"));
    Log.info("Writing.");
    Files.write(out, lines, StandardCharsets.UTF_8);
  }

  static void multipleLetterRepetitionWords(Path in, Path out) throws IOException {
    Histogram<String> noisyWords = Histogram.loadFromUtf8File(in, ' ');
    Histogram<String> repetitionWords = new Histogram<>();
    for (String w : noisyWords) {
      if (w.length() == 1) {
        continue;
      }
      int maxRepetitionCount = 1;
      int repetitionCount = 1;
      char lastChar = w.charAt(0);
      for (int i = 1; i < w.length(); i++) {
        char c = w.charAt(i);
        if (c == lastChar) {
          repetitionCount++;
        } else {
          if (repetitionCount > maxRepetitionCount) {
            maxRepetitionCount = repetitionCount;
          }
          repetitionCount = 0;
        }
        lastChar = c;
      }
      if (maxRepetitionCount > 1) {
        repetitionWords.set(w, noisyWords.getCount(w));
      }
    }
    repetitionWords.saveSortedByCounts(out, " ");
  }

  static LanguageIdentifier lid;

  static {
    try {
      lid = LanguageIdentifier.fromInternalModels();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  static void cleanTwitterData(Path in, Path out)
      throws Exception {

    int blockSize = 20_000;
    BlockTextLoader loader = new BlockTextLoader(in, blockSize);

    ExecutorService executorService =
        new BlockingExecutor(Runtime.getRuntime().availableProcessors() / 2);
    CompletionService<TwitterSaver> service =
        new ExecutorCompletionService<>(executorService);

    Path foreign = Paths.get(out.toString() + ".foreign");
    TwitterSaver saver = new TwitterSaver(out, foreign, blockSize);

    int bc = 0;
    for (List<String> block : loader) {
      service.submit(new TwitterTask(saver, block, bc));
      bc++;
    }
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.DAYS);
  }

  static class TwitterSaver {

    PrintWriter pwValid;
    PrintWriter pwForeign;
    int blockSize;
    int v = 0;
    int f = 0;
    int total = 0;

    TwitterSaver(Path valid, Path foreign, int blockSize)
        throws IOException {
      pwValid = new PrintWriter(valid.toFile(), "utf-8");
      pwForeign = new PrintWriter(foreign.toFile(), "utf-8");
      this.blockSize = blockSize;
    }

    void save(List<String> clean, List<String> foreign) {
      clean.forEach(pwValid::println);
      foreign.forEach(pwForeign::println);
      f += foreign.size();
      v += clean.size();
      total += blockSize;
      Log.info("Lines processed = %d valid = %d foreign = %d", total, v, f);
    }
  }

  static class TwitterTask implements Callable<TwitterSaver> {

    TwitterSaver saver;
    List<String> block;
    int blockIndex;

    public TwitterTask(TwitterSaver saver, List<String> block, int blockIndex) {
      this.saver = saver;
      this.block = block;
      this.blockIndex = blockIndex;
    }

    String set = "!.-><?(*)'\"_";
    FixedBitVector lookup = TextUtil.generateBitLookup(set);

    String removeMultipleSymbols(String s) {
      if (s.length() < 2) {
        return s;
      }
      StringBuilder sb = new StringBuilder(s.length());
      char previous = s.charAt(0);
      sb.append(previous);

      for (int i = 1; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c < lookup.length && lookup.get(c) && previous == c) {
          continue;
        }
        previous = c;
        sb.append(c);
      }
      return sb.toString();

    }

    @Override
    public TwitterSaver call() {
      Log.info("Processing block %d", blockIndex);
      List<String> clean = new ArrayList<>();
      List<String> foreign = new ArrayList<>();
      for (String s : block) {
        s = s.trim();
        if(s.contains("... http://")) {
          continue;
        }
        // remove time information.
        if (s.contains("EEST")) {
          s = s.replaceAll("^[^-]+-[^-]+-", "").trim(); // replaces until after second -
        }
        s = s.replaceAll("[\\u0095\\u0085]"," ");
        s = TextUtil.normalizeApostrophes(s);
        s = TextUtil.normalizeQuotesHyphens(s);
        s = TextUtil.normalizeSpacesAndSoftHyphens(s);
        s = removeMultipleSymbols(s);

        List<String> tokens = TurkishTokenizer.DEFAULT.tokenize(s)
            .stream().filter(
                k -> k.getType() != TurkishLexer.HashTag &&
                    k.getType() != TurkishLexer.Mention &&
                    k.getType() != TurkishLexer.URL &&
                    k.getText().indexOf('_') < 0 &&
                    !k.getText().equals("RT"))
            .map(Token::getText)
            .collect(Collectors.toList());
        String join = String.join(" ", tokens);
        if (join.trim().length() == 0) {
          continue;
        }

        // TODO: this mechanism fails in some cases as deasciification
        // makes sentences Turkish like
        boolean turkish;
        if (TurkishSentenceNormalizer.probablyRequiresDeasciifier(join)) {
          String k = new Deasciifier(join).convertToTurkish();
          turkish = lid.identify(k).equals("tr");
        } else {
          turkish = lid.identify(join).equals("tr");
        }
        if (turkish) {
          clean.add(join);
        } else {
          foreign.add(join);
        }
      }
      saver.save(clean, foreign);
      return saver;
    }
  }


}
