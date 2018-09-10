package zemberek.normalization;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import zemberek.core.ScoredItem;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.lm.compression.SmoothLm;

public class NormalizationScripts {

  public static void main(String[] args) throws IOException {
    Path root = Paths.get("/home/aaa/data/normalization");
    Path p = root.resolve("incorrect");
    Path s = root.resolve("split");
    Path lm = root.resolve("lm.slm");
    splitWords(p, s, lm, 2);

    Path quesOut = root.resolve("question-suffix");
    getQuestionSuffixes(s, quesOut);
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

}
