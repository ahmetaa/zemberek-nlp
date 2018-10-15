package zemberek.core.text;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class TextChunk implements Iterable<String> {

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

  @Override
  public Iterator<String> iterator() {
    return data.iterator();
  }

  @Override
  public void forEach(Consumer<? super String> action) {
    data.forEach(action);
  }

}
