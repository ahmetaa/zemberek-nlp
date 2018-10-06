package zemberek.core.text;

import java.util.List;

public class TextChunk {

  public final String id;
  public final int sourceIndex;
  public final int index;
  private List<String> data;

  public TextChunk(String id, List<String> data) {
    this.id = id;
    this.index = 0;
    this.sourceIndex = 0;
    this.data = data;
  }

  public TextChunk(String id, int sourceIndex, int index, List<String> data) {
    this.id = id;
    this.sourceIndex = sourceIndex;
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
    return id + "[" + sourceIndex + "-" + index + "]";
  }
}
