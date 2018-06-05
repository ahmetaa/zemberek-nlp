package zemberek.corpus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.IOUtil;
import zemberek.core.text.TextIO;

public class CorpusDbTest {

  @Test
  public void saveLoadIndexData() throws IOException, SQLException {
    Path tempDir = Files.createTempDirectory("foo");
    CorpusDb storage = new CorpusDb(tempDir);
    List<CorpusDocument> docs = loadDocuments();
    Map<Integer, CorpusDocument> docMap = new HashMap<>();

    for (CorpusDocument doc : docs) {
      int key = storage.saveDocument(doc);
      docMap.put(key, doc);
    }

    for (Integer key : docMap.keySet()) {
      CorpusDocument expected = docMap.get(key);
      CorpusDocument actual = storage.loadDocumentByKey(key);
      Assert.assertEquals(expected.id, actual.id);
      Assert.assertEquals(expected.content, actual.content);
    }
    IOUtil.deleteTempDir(tempDir);
  }

  private List<CorpusDocument> loadDocuments() throws IOException {
    List<WebDocument> corpus = WebCorpus.loadDocuments(
        TextIO.loadLinesFromResource("/corpus/test-corpus.txt"));
    List<CorpusDocument> corpusDocuments = new ArrayList<>();
    for (WebDocument d : corpus) {
      String content = String.join("\n", d.lines);
      corpusDocuments.add(
          new CorpusDocument(
              d.id,
              d.source,
              content,
              parseDateTime(d.crawlDate),
              parseDateTime(d.crawlDate)));
    }
    return corpusDocuments;
  }

  private LocalDateTime parseDateTime(String str) {
    if (str == null || str.length() == 0) {
      return null;
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return LocalDate.parse(str, formatter).atTime(LocalTime.MIN);
  }

}
