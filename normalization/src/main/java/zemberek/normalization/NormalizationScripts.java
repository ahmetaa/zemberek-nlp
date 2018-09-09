package zemberek.normalization;

import java.io.IOException;
import java.io.PrintWriter;
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
    Path root = Paths.get("/media/ahmetaa/depo/zemberek/data/normalization");
    Path p = root.resolve("incorrect");
    Path s = root.resolve("split");
    splitWords(p, s, 2);
  }

  static void splitWords(Path wordFrequencyFile, Path splitFile, int minWordCount)
      throws IOException {

    Path bigramLm = Paths.get("bin/lm/lm-bigram.slm");
    SmoothLm lm = SmoothLm.builder(bigramLm).logBase(Math.E).build();
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

        for (int i = 2; i < word.length() - 1; i++) {
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
          if (best.score > -8) {
            pw.println(word + " = " + best.item);
          }
        }
      }
    }
  }

}
