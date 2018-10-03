package zemberek.core.text;

import java.util.List;

public class TextChunk {

  public final String id;
  public final int index;
  private List<String> data;

  public TextChunk(String id, List<String> data) {
    this.id = id;
    this.index = 0;
    this.data = data;
  }

  public TextChunk(String id, int index, List<String> data) {
    this.id = id;
    this.index = index;
    this.data = data;
  }

  public String getId() {
    return id;
  }

  public List<String> getData() {
    return data;
  }

  public int size() {
    return data.size();
  }

  @Override
  public String toString() {
    return id + "-" + index;
  }
}
