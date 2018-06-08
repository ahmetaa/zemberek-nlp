package zemberek.corpus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import zemberek.core.logging.Log;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class LuceneIndexer {

  Path indexPath;

  public LuceneIndexer(Path indexPath) {
    this.indexPath = indexPath;
  }

  public void addDocs(IndexWriter writer, Path corpusFile) throws IOException {
    List<WebDocument> corpus = WebCorpus.loadDocuments(corpusFile);
    for (WebDocument d : corpus) {
      List<String> paragraphs = d.lines.stream()
          .map(s -> s.replace('\u00a0', ' ')
              .replaceAll("[\\u00ad]", "").trim())
          .collect(Collectors.toList());

      List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraphs(paragraphs);

      for (String sentence : sentences) {
        sentence = sentence.replaceAll("\\s+", " ");
        String tokenized = String.join(" ", TurkishTokenizer.DEFAULT.tokenizeToStrings(sentence));
        Document doc = new Document();
        doc.add(new TextField("content", tokenized, Store.YES));
        writer.addDocument(doc);
      }
    }
  }

  public void addCorpora(List<Path> corpora, double ramBufferInMb) throws IOException {
    Directory dir = FSDirectory.open(indexPath);
    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

    iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

    iwc.setRAMBufferSizeMB(ramBufferInMb);
    IndexWriter writer = new IndexWriter(dir, iwc);

    for (Path path : corpora) {
      Log.info("Adding %s", path);
      addDocs(writer, path);
    }
    writer.close();
  }

  public void addCorpora(List<Path> corpora) throws IOException {
    addCorpora(corpora, 1024);
  }

  public static void main(String[] args) throws IOException {
    Path indexRoot = Paths.get("/home/aaa/data/zemberek/corpus-index");
    LuceneIndexer index = new LuceneIndexer(indexRoot);

    Path corporaRoot = Paths.get("/media/aaa/Data/corpora/final/www.haberturk.com");
    List<Path> corpora = Files.walk(corporaRoot, 1)
        .filter(s -> s.toFile().isFile())
        .collect(Collectors.toList());
    index.addCorpora(corpora, 1024 * 4);
  }


}
