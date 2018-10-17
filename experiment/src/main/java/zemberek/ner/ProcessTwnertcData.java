package zemberek.ner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.text.TextUtil;

public class ProcessTwnertcData {

  public static void main(String[] args) throws IOException {
    Path corpus = Paths.get(
        "/media/ahmetaa/depo/ner/TWNERTC_All_Versions/TWNERTC_TC_Coarse_Grained_NER_DomainDependent_NoiseReduction.DUMP");
    Path nerOut = Paths.get("/media/ahmetaa/depo/ner/ner-coarse");
    Path categoryOut = Paths.get("/media/ahmetaa/depo/classification/twnertc-data");
    BlockTextLoader loader = BlockTextLoader.fromPath(corpus, 10_000);

    List<String> nerLines = new ArrayList<>();
    List<String> categoryLines = new ArrayList<>();
    Histogram<String> categories = new Histogram<>();
    for (TextChunk chunk : loader) {
      for (String line : chunk) {
        List<String> parts = TextUtil.TAB_SPLITTER.splitToList(line);
        categoryLines.add("__label__" + parts.get(0) + " " + parts.get(2));
        categories.add(parts.get(0));

        List<String> nerLabels = TextUtil.SPACE_SPLITTER.splitToList(parts.get(1));
        List<String> nerWords = TextUtil.SPACE_SPLITTER.splitToList(parts.get(2));
        if (nerLabels.size() != nerWords.size()) {
          continue;
        }
        List<NerRange> ranges = new ArrayList<>();

        NerRange range = new NerRange();
        for (int i = 0; i < nerLabels.size(); i++) {
          String lbl = nerLabels.get(i);
          String word = nerWords.get(i);

          if (lbl.equals("O")) {
            if (range.type == null) {
              range.type = "O";
            } else {
              if (range.type.equals("O")) {
                range.seq.add(word);
              } else {
                ranges.add(range);
                range = new NerRange();
                range.type = "O";
                range.seq.add(word);
              }
            }
          }

        }


      }
      Log.info(chunk.index * loader.getBlockSize());

    }

    Files.write(categoryOut, categoryLines);
    categories.saveSortedByCounts(Paths.get("/media/ahmetaa/depo/classification/categories"), " ");

  }

  static class NerRange {

    int start;
    int end;
    String type;
    List<String> seq = new ArrayList<>();


  }

}
