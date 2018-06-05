package zemberek.corpus;

import java.time.LocalDateTime;

public class CorpusDocument {
  int key;
  String id;
  String source;
  String content;
  LocalDateTime sourceDate;
  LocalDateTime processDate;

  public CorpusDocument(String id, String source, String content) {
    this.id = id;
    this.source = source;
    this.content = content;
  }

  public CorpusDocument(String id, String source, String content, LocalDateTime sourceDate,
      LocalDateTime processDate) {
    this.id = id;
    this.source = source;
    this.content = content;
    this.sourceDate = sourceDate;
    this.processDate = processDate;
  }

  public void setKey(int key) {
    this.key = key;
  }
}
