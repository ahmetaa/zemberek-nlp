package zemberek.corpus;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.text.TextConsumer;

public class WebCorpus {

  String source;
  String id;

  private List<WebDocument> pages = new ArrayList<>();
  private Map<String, WebDocument> lookup = new HashMap<>();

  public WebCorpus(String source, String id, List<WebDocument> pages) {
    this.source = source;
    this.id = id;
    this.pages = pages;
    for (WebDocument page : pages) {
      lookup.put(page.getId(), page);
    }
  }

  public WebCorpus(String source, String id) {
    this.source = source;
    this.id = id;
  }

  public static List<WebDocument> loadDocuments(List<String> allLines) {
    List<WebDocument> pages = new ArrayList<>(allLines.size() / 10);

    TextConsumer textConsumer = new TextConsumer(allLines);
    textConsumer.moveUntil(s -> s.startsWith("<doc id="));
    while (!textConsumer.finished()) {
      String meta = textConsumer.current();
      textConsumer.advance();
      List<String> pageData = textConsumer.moveUntil(s -> s.startsWith("</doc>"));
      textConsumer.moveUntil(s -> s.startsWith("<doc"));
      WebDocument e = WebDocument.fromText(meta, pageData);
      if (e != null) {
        pages.add(e);
      }
    }
    return pages;
  }


  public static List<WebDocument> loadDocuments(Path corpusFile) throws IOException {
    List<String> allLines = Files.readAllLines(corpusFile, StandardCharsets.UTF_8);
    return loadDocuments(allLines);
  }

  //TODO: this may be lossy.
  public WebCorpus copyNoDuplicates() {
    Set<Long> hashes = new HashSet<>();
    WebCorpus noDup = new WebCorpus(this.source, this.id);
    for (WebDocument doc : pages) {
      if (hashes.contains(doc.getHash())) {
        continue;
      }
      if (doc.contentLength() < 50) {
        continue;
      }
      hashes.add(doc.getHash());
      noDup.addDocument(doc);
    }
    return noDup;
  }

  public static List<CorpusDocument> convertToCorpusDocument(Path path) throws IOException {
    return convertToCorpusDocument(loadDocuments(path));
  }

  public static List<CorpusDocument> convertToCorpusDocument(List<WebDocument> docs) {
    List<CorpusDocument> corpusDocuments = new ArrayList<>();
    for (WebDocument d : docs) {
      corpusDocuments.add(CorpusDocument.fromWebDocument(d));
    }
    return corpusDocuments;
  }

  static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static LocalDateTime parseDateTime(String str) {
    if (str == null || str.length() == 0 || str.equals("null")) {
      return null;
    }
    return LocalDate.parse(str, formatter).atTime(LocalTime.MIN);
  }


  public WebDocument getDocument(String id) {
    return lookup.get(id);
  }

  public String getSource() {
    return source;
  }

  public String getId() {
    return id;
  }

  public List<WebDocument> getDocuments() {
    return pages;
  }

  public void addDocuments(Collection<WebDocument> documents) {
    for (WebDocument document : documents) {
      addDocument(document);
    }
  }

  public void addDocument(WebDocument document) {
    pages.add(document);
    lookup.put(document.getId(), document);
  }

  public int documentCount() {
    return pages.size();
  }

  @Override
  public String toString() {
    return source + "-" + id;
  }

  public int totalPageLineCount() {
    int total = 0;
    for (WebDocument page : pages) {
      for (String line : page.lines) {
        if (line.length() == 0) {
          continue;
        }
        total++;
      }
    }
    return total;
  }

  public int uniquePageLineCount() {

    Set<Long> hashes = new HashSet<>(100000);
    for (WebDocument page : pages) {
      for (String line : page.lines) {
        hashes.add(Hashing.murmur3_128().hashUnencodedChars(line).asLong());
      }
    }
    return hashes.size();
  }

  public void saveToDir(Path outRoot, boolean onlyContent) throws IOException {

    Path subDir = outRoot.resolve(source);
    Files.createDirectories(subDir);
    save(subDir.resolve(id), onlyContent);
  }

  public void save(Path outFile, boolean onlyContent) throws IOException {

    try (PrintWriter p = new PrintWriter(outFile.toFile(), "utf-8")) {
      for (WebDocument page : pages) {
        if (!onlyContent) {
          p.println(page.getDocumentHeader());
        }
        p.println(page.getContentAsString());
        if (!onlyContent) {
          p.println("</doc>");
        }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    List<WebDocument> list = WebCorpus
        .loadDocuments(Paths.get("/home/ahmetaa/data/text/news-corpus/www.cnnturk.com.corpus"));
    WebCorpus c = new WebCorpus("foo", "bar");
    c.addDocuments(list);
    c = c.copyNoDuplicates();
    c.save(Paths.get("/home/ahmetaa/data/text/news-corpus/www.cnnturk.com.txt"), true);
  }

}

