package zemberek.corpus;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.h2.jdbcx.JdbcConnectionPool;
import zemberek.core.logging.Log;
import zemberek.tokenization.TurkishSentenceExtractor;

public class CorpusDb {

  private Path dbPath;
  private JdbcConnectionPool connectionPool;

  public CorpusDb(Path dbRoot) throws IOException, SQLException {
    if (dbRoot.toFile().exists() && !dbRoot.toFile().isDirectory()) {
      throw new IllegalArgumentException("Database root path is not a directory :" + dbRoot);
    }
    if (!dbRoot.toFile().exists()) {
      Log.info("Creating database root folder %s", dbRoot);
      Files.createDirectories(dbRoot);
    }
    this.dbPath = dbRoot.resolve("data");
    connectionPool = JdbcConnectionPool.create(getJdbcConnectionString(), "sa", "sa");
    createTablesIfNotExist();
  }

  private CorpusDb(JdbcConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  private void createTablesIfNotExist() throws IOException, SQLException {
    try {
      String connectionStr = getJdbcConnectionString() + ";IFEXISTS=TRUE";
      DriverManager.getConnection(connectionStr, "sa", "sa");
    } catch (SQLException e) {
      if (e.getErrorCode() == 90013) {
        Log.info("Creating tables for the first time.");
        generateTables();
      } else {
        throw e;
      }
    }
  }

  public void addAll(List<CorpusDocument> docs) {
    for (CorpusDocument doc : docs) {
      int key = saveDocument(doc);
      doc.setKey(key);
    }
  }

  public void generateTables() throws SQLException, IOException {
    String query = Resources.toString(
        Resources.getResource("corpus/corpus-schema.sql"), StandardCharsets.UTF_8);
    try (Connection connection = connectionPool.getConnection()) {
      Statement s = connection.createStatement();
      s.execute(query);
    }
  }

  private String getJdbcConnectionString() {
    return "jdbc:h2:" + dbPath.toFile().getAbsolutePath() + ";COMPRESS=TRUE";
  }

  public void saveSentences(int docKey, List<String> sentences) {
    try (Connection connection = connectionPool.getConnection()) {
      String sql = "INSERT INTO SENTENCE_TABLE " +
          "(DOC_KEY, CONTENT)" +
          " VALUES (?,?)";
      PreparedStatement employeeStmt = connection.prepareStatement(sql);
      for (String sentence : sentences) {
        if (sentence.length() >= 512) {
          continue;
        }
        employeeStmt.setInt(1, docKey);
        employeeStmt.setString(2, sentence);
        employeeStmt.addBatch();
      }
      employeeStmt.executeBatch();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public List<SentenceSearchResult> search(String text) {
    try (Connection connection = connectionPool.getConnection()) {
      String sql = "SELECT T.* FROM FT_SEARCH_DATA('" + text + "', 0, 0) FT, SENTENCE_TABLE T "
          + "WHERE FT.TABLE='SENTENCE_TABLE' AND T.ID=FT.KEYS[0];";
      Statement stat = connection.createStatement();
      ResultSet set = stat.executeQuery(sql);
      List<SentenceSearchResult> result = new ArrayList<>();
      while (set.next()) {
        result.add(new SentenceSearchResult(set.getInt(1), set.getInt(2), set.getString(3)));
      }
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  int saveDocument(CorpusDocument doc) {
    try (Connection connection = connectionPool.getConnection()) {
/*
      Clob clob = connection.createNClob();
      clob.setString(1, doc.content);
*/

      String sql = "INSERT INTO DOCUMENT_TABLE " +
          "(DOC_ID,SOURCE_ID,SOURCE_DATE,PROCESS_DATE)" +
          " VALUES (?,?,?,?)";
      try (PreparedStatement ps = connection
          .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, doc.id);
        ps.setString(2, doc.source);
        ps.setTimestamp(3, toTimeStamp(doc.sourceDate));
        ps.setTimestamp(4, toTimeStamp(doc.processDate));
        //ps.setClob(5, clob);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
          int key = rs.getInt(1);
          doc.setKey(key);
          return key;
        } else {
          return -1;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  Timestamp toTimeStamp(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    Timestamp timestamp = Timestamp.valueOf(dateTime);
    return new Timestamp(timestamp.getTime());
  }

  public CorpusDocument loadDocumentByKey(int key) {
    String sql =
        "SELECT ID, DOC_ID, SOURCE_ID, SOURCE_DATE, PROCESS_DATE, CONTENT FROM DOCUMENT_TABLE " +
            "WHERE ID = " + key;
    return getDocument(sql);
  }

  private CorpusDocument getDocument(String sql) {

    CorpusDocument document = null;
    try (Connection connection = connectionPool.getConnection()) {
      Statement s = connection.createStatement();
      ResultSet rs = s.executeQuery(sql);
      while (rs.next()) {
        document = getDocumentFromResultSet(rs);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return document;
  }

  private CorpusDocument getDocumentFromResultSet(ResultSet rs) throws Exception {
    CorpusDocument doc;
    int key = rs.getInt(1);
    String dId = rs.getString(2);
    String sId = rs.getString(3);
    Timestamp sourceDate = rs.getTimestamp(4);
    Timestamp processDate = rs.getTimestamp(5);

    Clob clob = rs.getClob(6);
    String content = readClob(clob);
    clob.free();
    doc = new CorpusDocument(
        dId,
        sId,
        content,
        sourceDate == null ? null : sourceDate.toLocalDateTime(),
        processDate == null ? null : processDate.toLocalDateTime()
    );
    doc.setKey(key);
    return doc;
  }

  private static String readClob(Clob clob) throws SQLException, IOException {
    StringBuilder sb = new StringBuilder((int) clob.length());
    Reader r = clob.getCharacterStream();
    char[] cbuf = new char[2048];
    int n;
    while ((n = r.read(cbuf, 0, cbuf.length)) != -1) {
      sb.append(cbuf, 0, n);
    }
    return sb.toString();
  }

  public void addDocs(Path corpusFile) throws IOException {
    List<WebDocument> corpus = WebCorpus.loadDocuments(corpusFile);
    for (WebDocument doc : corpus) {
      CorpusDocument cd = CorpusDocument.fromWebDocument(doc);
      int key = saveDocument(cd);
      List<String> paragraphs = doc.lines;
      List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraphs(paragraphs);
      saveSentences(key, sentences);
    }
  }

}
