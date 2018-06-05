package zemberek.corpus;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.IOUtil;
import zemberek.core.text.TextIO;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class CorpusDbTest {

  @Test
  public void saveLoadDocument() throws IOException, SQLException {
    Path tempDir = Files.createTempDirectory("foo");
    CorpusDb storage = new CorpusDb(tempDir);

    Map<Integer, CorpusDocument> docMap = saveDocuments(storage);

    for (Integer key : docMap.keySet()) {
      CorpusDocument expected = docMap.get(key);
      CorpusDocument actual = storage.loadDocumentByKey(key);
      Assert.assertEquals(expected.id, actual.id);
      Assert.assertEquals(expected.content, actual.content);
    }
    IOUtil.deleteTempDir(tempDir);
  }

  private Map<Integer, CorpusDocument> saveDocuments(CorpusDb storage) throws IOException {
    List<WebDocument> corpus = WebCorpus.loadDocuments(
        TextIO.loadLinesFromResource("/corpus/test-corpus.txt"));

    List<CorpusDocument> docs = WebCorpus.convertToCorpusDocument(corpus);
    Map<Integer, CorpusDocument> docMap = new HashMap<>();

    for (CorpusDocument doc : docs) {
      int key = storage.saveDocument(doc);
      docMap.put(key, doc);
    }
    return docMap;
  }

  @Test
  public void search() throws IOException, SQLException {
    Path tempDir = Files.createTempDirectory("foo");
    CorpusDb storage = new CorpusDb(tempDir);

    Map<Integer, CorpusDocument> docMap = saveDocuments(storage);

    for (Integer key : docMap.keySet()) {
      CorpusDocument doc = docMap.get(key);
      List<String> paragraphs = Splitter.on("\n").splitToList(doc.content);
      List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraphs(paragraphs);
      storage.saveSentences(key, sentences);
    }
    List<SentenceSearchResult> searchResults = storage.search("milyar");
    for (SentenceSearchResult searchResult : searchResults) {
      System.out.println(searchResult);
    }

    IOUtil.deleteTempDir(tempDir);
  }

}
