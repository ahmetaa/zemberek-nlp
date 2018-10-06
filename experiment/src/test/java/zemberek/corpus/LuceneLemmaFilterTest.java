package zemberek.corpus;

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LuceneLemmaFilterTest {

  private IndexSearcher searcher;
  QueryParser parser;

  @Before
  public void setUp() throws Exception {

    Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer("standard")
        .addTokenFilter(LuceneLemmaFilter.Factory.class)
        .build();

    Analyzer searchAnalyzer = new StandardAnalyzer();

    RAMDirectory directory = new RAMDirectory();
    IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
    Document doc = new Document();
    doc.add(new TextField("content",
        "çoğu günler limonlu kekten aldırdım",
        Field.Store.YES));
    writer.addDocument(doc);

    writer.close();

    IndexReader reader = DirectoryReader.open(directory);

    searcher = new IndexSearcher(reader);
    parser = new QueryParser("content", searchAnalyzer);
  }

  @Test
  public void testWordSearch() throws IOException {
    TermQuery tq = new TermQuery(new Term("content", "çok"));
    TopDocs hits = searcher.search(tq, 10);
    Assert.assertEquals(1, hits.totalHits);
  }

  @Test
  public void testMultiWordSearch() throws IOException, ParseException {

    Query query = parser.parse("\"limon kek\"");
    TopDocs hits = searcher.search(query, 10);
    Assert.assertEquals(1, hits.totalHits);
  }

}

