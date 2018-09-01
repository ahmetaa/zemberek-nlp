package zemberek.corpus;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class CorpusSearcher {

  Path index;

  public CorpusSearcher(Path index) {
    this.index = index;
  }

  List<String> search(String queryStr, int hitCount) throws Exception {
    IndexReader reader = DirectoryReader.open(FSDirectory.open(index));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer("standard")
        .build();
    QueryParser parser = new QueryParser("content", analyzer);
    Query query = parser.parse(queryStr);
    TopDocs results = searcher.search(query, hitCount);
    ScoreDoc[] hits = results.scoreDocs;
    List<String> result = new ArrayList<>();
    for (ScoreDoc hit : hits) {
      Document doc = searcher.doc(hit.doc);
      result.add(doc.get("content"));
    }
    return result;
  }

  public static void main(String[] args) throws Exception {
    Path indexRoot = Paths.get("/media/ahmetaa/depo/zemberek/data/corpus-index-lemma");
    CorpusSearcher searcher = new CorpusSearcher(indexRoot);

    List<String> hits = searcher.search("\"şart koş\"", 1000);

    hits.forEach(System.out::println);

  }


}
