package zemberek.apps.corpus;

import com.beust.jcommander.Parameter;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import zemberek.apps.ConsoleApp;
import zemberek.core.collections.LongUIntMap;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;

public class RemoveDuplicateLines extends ConsoleApp {

  @Parameter(names = {"--input", "-i"},
      required = true,
      description = "Input corpus file. Must be in UTF-8 Format.")
  Path corpus;

  @Parameter(names = {"--output", "-o"},
      description = "Output corpus file. If not defined, [input].nodup file is generated.")
  Path output;

  @Parameter(names = {"--locale", "-l"},
          description = "Locale")
  String localeStr = "tr";

  @Parameter(names = {"--lineCount", "-cnt"},
          description = "Amount of lines to process.")
  long count = -1;

  @Parameter(names = {"--normalizeLines", "-norm"},
      description = "Applies normalization before processing a line.")
  boolean normalizeLines = false;

  @Parameter(names = {"--writeCounts", "-wc"},
            description = "Write count information for each line.")
    boolean writeCounts = false;


  @Override
  public String description() {
    return "Eliminates duplicate lines from a corpus. This is a lossy approach but works reasonably"
        + "fast for large corpus files. If corpus is too large (>10G), you may need to "
        + "adjust VM parameters for more heap memory.";
  }

  private int duplicateCount = 0, totalCount = 0;
  private int PROGRESS = 500_000;

  private LongUIntMap index = new LongUIntMap(20_000_000);
  private LongUIntMap histogram = new LongUIntMap(5_000_000);
  private Locale locale;

  @Override
  public void run() throws Exception {

    if(locale!=null) {
      locale = new Locale(localeStr);
    } else {
      locale = Turkish.LOCALE;
    }

    if (!corpus.toFile().exists()) {
      throw new IllegalArgumentException("Can not find the corpus file: " + corpus);
    }
    if (output == null) {
      output = Paths.get(corpus.toFile().getAbsoluteFile() + ".nodup");
    }

    Log.info("Input  : %s", corpus);
    Log.info("Output : %s", output);
    Log.info("Finding duplicates.");
    findDuplicates();
    Log.info("Regenerating corpus.");
    recreateCorpus();
  }

  private void findDuplicates() {

    BlockTextLoader loader = BlockTextLoader.fromPath(corpus, 10_000);
    int lineCounter=0;

    for (TextChunk block : loader) {
      for (String line : block.getData()) {
        String l = line;

        if (normalizeLines) {
          l = process(line);
        }
        totalCount++;
        lineCounter++;
        if (totalCount % PROGRESS == 0) {
          Log.info("Total lines read: %d. Duplicates: %d", totalCount, duplicateCount);
        }
        long hash = longHash(l);
        if (index.containsKey(hash)) {
          duplicateCount++;
        } else {
          index.put(hash, totalCount);
        }
        histogram.increment(hash);
      }
        if(count!=-1 && lineCounter>count) {
            break;
        }
    }
    Log.info("Total lines read: %d. Duplicates: %d", totalCount, duplicateCount);
    Log.info("Duplicate Ratio: %.3f", duplicateCount * 100.0d / totalCount);
  }

  private String process(String line) {
    String l = line.replaceAll("[^" + TurkishAlphabet.INSTANCE.getAllLetters() + "]", "");
    l = l.toLowerCase(locale);
    return l;
  }

  private long longHash(String line) {
    return Hashing.murmur3_128().hashString(line, Charsets.UTF_8).asLong();
  }

  private void recreateCorpus() throws IOException {
    int lineCounter = 0;
    int writtenLines = 0;
    try (PrintWriter writer = new PrintWriter(
        new OutputStreamWriter(IOUtil.geBufferedOutputStream(output), "UTF-8"))) {
      BlockTextLoader loader = BlockTextLoader.fromPath(corpus, 10_000);
      for (TextChunk block : loader) {
        for (String line : block.getData()) {
          String l = line;
          if (normalizeLines) {
            l = process(line);
          }
          lineCounter++;
          if (lineCounter % PROGRESS == 0) {
            Log.info("Total lines read: %d. Lines Written: %d", lineCounter, writtenLines);
          }
          long hash = longHash(l);
          if (index.get(hash) == lineCounter) {
            if(writeCounts) {
                writer.println(histogram.get(hash)  + " " + line);
            } else {
              writer.println(line);
            }
            writtenLines++;
          }
        }
          if(count!=-1 && lineCounter>count) {
              break;
          }
      }

      Log.info("Total lines read: %d. Lines Written: %d", lineCounter, writtenLines);
    }
  }

  public static void main(String[] args) {
    new RemoveDuplicateLines().execute(args);
  }
}

