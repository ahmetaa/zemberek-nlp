package zemberek.scratchpad;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
    return doc;
  }

  public static void main(String[] args) throws Exception {
    TdkLoader loader = new TdkLoader(Paths.get("tdk-out"));
    loader.loadFromHtmlAndSave("elma", true);
  }

}
