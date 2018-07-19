package zemberek.scratchpad;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import zemberek.core.logging.Log;

public class TdkLoader {

  private static String siteString =
      "http://www.tdk.gov.tr/index.php?option=com_gts&arama=gts&kelime=";

  Path outputRoot;

  public TdkLoader(Path outputRoot) throws IOException {
    Files.createDirectories(outputRoot);
    this.outputRoot = outputRoot;

  }

  Document loadFromHtmlAndSave(String searchPhrase, boolean overwrite)
      throws Exception {

    if (searchPhrase == null || searchPhrase.trim().isEmpty()) {
      throw new IllegalArgumentException("Search phrase cannot be empty.");
    }
    searchPhrase = searchPhrase.trim();
    String encode = URLEncoder.encode(searchPhrase, "utf-8");

    String prefix = searchPhrase.length() > 1 ? searchPhrase.substring(0, 2) : searchPhrase;
    Path dir = outputRoot.resolve(prefix);
    Files.createDirectories(dir);
    Path out = dir.resolve(encode + ".html");
    if (out.toFile().exists()) {
      Log.info("Skip %s", searchPhrase);
      return null;
    }
    if (!overwrite && out.toFile().exists()) {
      return null;
    }
    Document doc = Jsoup.connect(siteString + encode)
        .ignoreHttpErrors(true)
        .userAgent(
            "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
        .referrer("http://www.google.com")
        .get();
    doc = doc.normalise();
    String htmlContent = doc.toString();
    Files.write(
        out,
        Collections.singletonList(htmlContent),
        StandardCharsets.UTF_8);
    Thread.sleep(1500);
    return doc;
  }

  public static void main(String[] args) throws Exception {
    Path outputRoot = Paths.get("/home/ahmetaa/data/tdk-out");
    TdkLoader loader = new TdkLoader(outputRoot);
    Path vocab = Paths.get("/home/ahmetaa/data/zemberek.vocab.filtered");
    List<String> words = Files.readAllLines(vocab);
    Collections.shuffle(words);
    int trial = 0;
    while (trial < 10) {
      try {
        for (String s : words) {
          Log.info(s);
          loader.loadFromHtmlAndSave(s, true);
        }
        System.exit(0);
      } catch (Exception e) {
        Thread.sleep(10000);
        trial++;
        Log.info("Retrying %d", trial);
      }
    }
  }

}
