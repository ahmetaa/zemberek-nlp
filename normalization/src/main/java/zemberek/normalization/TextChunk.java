package zemberek.normalization;

import java.util.List;

public class TextChunk {

  String id;
  List<String> data;

  public TextChunk(String id, List<String> data) {
    this.id = id;
    this.data = data;
  }

  public String getId() {
    return id;
  }

  public List<String> getData() {
    return data;
  }
}
