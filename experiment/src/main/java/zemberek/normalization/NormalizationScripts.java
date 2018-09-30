package zemberek.normalization;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class NormalizationScripts {

  public static void main(String[] args) throws Exception {

    Path root = Paths.get("/media/ahmetaa/depo/normalization/test");

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

    Path corporaRoot = Paths.get("/media/ahmetaa/depo/corpora");
    Path tweetRoot = Paths.get("/media/ahmetaa/depo/corpora");
/*
    cleanTwitterData(
        tweetRoot.resolve("tweets-20m.txt"),
        tweetRoot.resolve("tweets-20m-clean")
    );
*/

/*    splitSingleFileCorpus(
        corporaRoot.resolve("tweets-20m-clean.nodup"),
        corporaRoot.resolve("tweets-20m"));*/

    checkSuspiciousWords(
        TurkishMorphology.createWithDefaults(),
        Paths.get("/home/aaa/data/normalization/vocab-clean"),
        Paths.get("/home/aaa/data/normalization/vocab-noisy"),
        Paths.get("/home/aaa/data/normalization/test-large")
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

    AnalysisCache cache = AnalysisCache
        .builder()
        .dynamicCacheSize(300_000, 500_000).build();

    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .setCache(cache)
        .addDefaultBinaryDictionary()
        .disableUnidentifiedTokenAnalyzer()
        .build();

    int threadCount = Runtime.getRuntime().availableProcessors() / 2;
    if (threadCount > 20) {
      threadCount = 20;
    }
    ExecutorService executorService =
        new BlockingExecutor(threadCount);
    CompletionService<TwitterSaver> service =
        new ExecutorCompletionService<>(executorService);

    int blockSize = 20_000;
    BlockTextLoader loader = new BlockTextLoader(in, blockSize);

    Path foreign = Paths.get(out.toString() + ".foreign");
    TwitterSaver saver = new TwitterSaver(out, foreign, blockSize);

    int bc = 0;
    for (List<String> block : loader) {
      service.submit(new TwitterTask(morphology, saver, block, bc));
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

    TurkishMorphology morphology;
    TwitterSaver saver;
    List<String> block;
    int blockIndex;

    public TwitterTask(TurkishMorphology morphology,
        TwitterSaver saver, List<String> block, int blockIndex) {
      this.morphology = morphology;
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
        if (s.contains("... http://")) {
          continue;
        }
        // remove time information.
        if (s.contains("EEST")) {
          s = s.replaceAll("^[^-]+-[^-]+-", "").trim(); // replaces until after second -
        }
        s = s.replaceAll("[\\u0095\\u0085]", " ");
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
                    k.getText().indexOf('#') < 0 &&
                    k.getText().indexOf('@') < 0 &&
                    !k.getText().equals("RT"))
            .map(Token::getText)
            .collect(Collectors.toList());
        String join = String.join(" ", tokens);
        if (join.trim().length() == 0) {
          continue;
        }

        boolean turkish = false;
        String lang = lid.identify(join);
        //TODO: here is a hack for javanese-indonesian language confusion.
        // twitter data I use seems to have lots of data with those languages.
        // we try to deasciify those and sometimes erroneously identify them as turkish afterwards.
        // to prevent this, if language is identified as jv or id, we skip.
        // to remove this hack we need a better language identification model for Turkish
        // that is trained with also noisy text.
        if (!lang.equals("jv") && !lang.equals("id") && TurkishSentenceNormalizer
            .probablyRequiresDeasciifier(join)) {
          String k = new Deasciifier(join).convertToTurkish();
          // identify and check morphology to be sure.
          String l = lid.identify(join);
          if (l.equals("tr")) {
            List<String> words = Splitter.on(' ').splitToList(k);
            int accepted = 0;
            for (String word : words) {
              WordAnalysis a = morphology.analyze(word);
              if (a.isCorrect()) {
                accepted++;
              }
            }
            if (accepted * 1d / words.size() >= 0.3) {
              turkish = true;
            }
          }
        } else {
          turkish = lang.equals("tr");
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

  static void splitSingleFileCorpus(Path in, Path outRoot) throws IOException {
    int blockSize = 100_000;
    BlockTextLoader loader = new BlockTextLoader(in, blockSize);
    Files.createDirectories(outRoot);

    int bc = 0;
    for (List<String> block : loader) {
      String name = in.toFile().getName();
      Path blockPath = outRoot.resolve(name + "." + String.valueOf(bc));
      Files.write(blockPath, block, StandardCharsets.UTF_8);
      bc++;
    }
  }

  static void checkSuspiciousWords(
      TurkishMorphology morphology,
      Path cleanRoot,
      Path noisyRoot,
      Path outRoot) throws IOException {
    Histogram<String> correctFromNoisy =
        Histogram.loadFromUtf8File(noisyRoot.resolve("correct"), ' ');
    Log.info("Correct from noisy Loaded");
    Histogram<String> correctFromClean =
        Histogram.loadFromUtf8File(cleanRoot.resolve("correct"), ' ');
    Log.info("Correct from clean Loaded");

    Histogram<String> zero = new Histogram<>();
    Histogram<String> zeroWordZeroLemma = new Histogram<>();
    Histogram<String> zeroWordLowLemma = new Histogram<>();
    Histogram<String> lowFreq = new Histogram<>();
    Histogram<String> lowFreqLowLemmaFreq = new Histogram<>();

    double nTotal = correctFromNoisy.totalCount();
    double cTotal = correctFromClean.totalCount();

    for (String s : correctFromNoisy) {

      int nCount = correctFromNoisy.getCount(s);
      double nFreq = nCount / nTotal;

      if (!correctFromClean.contains(s)) {
        zero.add(s, nCount);
        WordAnalysis an = morphology.analyze(s);
        if (an.analysisCount() > 0) {
          Set<String> allLemmas = new HashSet<>();
          for (SingleAnalysis analysis : an) {
            allLemmas.addAll(analysis.getLemmas());
          }

          boolean none = true;
          boolean lowLemmaRatio = true;
          // TODO: this is not the best way. try extracting lemma frequencies from correct from clean
          for (String l : allLemmas) {
            if (correctFromClean.contains(l)) {
              none = false;
              double lnf = correctFromNoisy.getCount(l) / nTotal;
              double lcf = correctFromClean.getCount(l) / nTotal;
              if (lnf / lcf > 10) {
                lowLemmaRatio = false;
                break;
              }
            }
          }

          if (none) {
            zeroWordZeroLemma.add(s, nCount);
          }
          if (lowLemmaRatio) {
            zeroWordLowLemma.add(s, nCount);
          }
        }
        continue;
      }

      double cFreq = correctFromClean.getCount(s) / cTotal;
      if (nFreq / cFreq > 30) {
        lowFreq.add(s, nCount);
      }

    }
    Log.info("Saving suspicious");
    zero.saveSortedByCounts(noisyRoot.resolve("suspicious-zero"), " ");
    zeroWordZeroLemma.saveSortedByCounts(noisyRoot.resolve("suspicious-zero-no-lemma"), " ");
    zeroWordLowLemma.saveSortedByCounts(noisyRoot.resolve("suspicious-zero-low-lemma"), " ");
    lowFreq.saveSortedByCounts(noisyRoot.resolve("suspicious-lowfreq"), " ");

    Log.info("Creating vocabularies");

    // ----------- noisy ------------
    Histogram<String> noisy = new Histogram<>(1000_000);

    Histogram<String> noisyFromCleanCorpora =
        Histogram.loadFromUtf8File(cleanRoot.resolve("incorrect"), ' ');
    Histogram<String> noisyFromNoisyCorpora =
        Histogram.loadFromUtf8File(noisyRoot.resolve("incorrect"), ' ');
    Log.info("Incorrect words loaded.");

    noisy.add(noisyFromCleanCorpora);
    noisy.add(noisyFromNoisyCorpora);

    noisy.saveSortedByCounts(outRoot.resolve("incorrect")," ");
    Log.info("Noisy saved.");

    // ----------- maybe noisy -------------------
    Histogram<String> maybeNoisy = new Histogram<>(1000_000);
    maybeNoisy.add(zeroWordZeroLemma);
    for (String lf : lowFreq) {
      if (!maybeNoisy.contains(lf)) {
        maybeNoisy.add(lf, zeroWordZeroLemma.getCount(lf));
      }
    }

    int threshold = 2;

    for (String z : zero) {
      int c = zero.getCount(z);
      if (!maybeNoisy.contains(z) && c > threshold) {
        maybeNoisy.add(z, c);
      }
    }

    maybeNoisy.saveSortedByCounts(outRoot.resolve("maybe-incorrect")," ");
    Log.info("Maybe Noisy saved.");

    // ---------- clean ------------------

    Histogram<String> clean = new Histogram<>(1000_000);
    clean.add(correctFromClean);
    clean.add(correctFromNoisy);
    clean.removeAll(maybeNoisy);

    noisy.saveSortedByCounts(outRoot.resolve("correct")," ");
    Log.info("Clean saved.");

  }


}
