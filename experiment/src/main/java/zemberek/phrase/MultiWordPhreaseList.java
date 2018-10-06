package zemberek.phrase;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import zemberek.core.logging.Log;
import zemberek.core.turkish.Turkish;

public class MultiWordPhreaseList {

  static void processRaw(Path input, Path output) throws IOException {
    List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
    Set<String> rawPhrases = new HashSet<>();
    int totalRaw = 0;
    for (String line : lines) {
      String relevant = line.substring(line.indexOf('=') + 1).trim();
      List<String> items = Splitter.on("|").trimResults().omitEmptyStrings().splitToList(relevant);
      totalRaw += items.size();
      rawPhrases.addAll(items);
    }
    Log.info("Total Raw    = " + totalRaw);
    Log.info("No Duplicate = " + rawPhrases.size());

    List<String> sorted = new ArrayList<>(rawPhrases);
    sorted.sort(Turkish.COLLATOR);

    Files.write(output, sorted, StandardCharsets.UTF_8);


  }

  public static void main(String[] args) throws IOException {
    processRaw(
        Paths.get("data/phrase/raw-phrases.txt"),
        Paths.get("data/phrase/raw-phrases2.txt")
        );
  }

}
