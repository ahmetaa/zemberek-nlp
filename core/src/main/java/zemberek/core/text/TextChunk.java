package zemberek.core.text;

import java.util.List;

public class TextChunk {

  public final String id;
  private List<String> data;

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
