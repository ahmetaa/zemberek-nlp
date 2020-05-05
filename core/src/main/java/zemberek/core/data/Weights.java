package zemberek.core.data;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.compression.LossyIntLookup;
import zemberek.core.io.Strings;
import zemberek.core.text.TextIO;

public class Weights implements WeightLookup, Iterable<String> {

  static float epsilon = 0.0001f;

  FloatValueMap<String> data;

  public Weights(FloatValueMap<String> data) {
    this.data = data;
  }

  public Weights() {
    data = new FloatValueMap<>(10000);
  }

  public FloatValueMap<String> getData() {
    return data;
  }

  public int size() {
    return data.size();
  }

  public static Weights loadFromResource(String resource) throws IOException {
    List<String> lines = TextIO.loadLinesFromResource(resource);
    return loadFromLines(lines);
  }

  public static Weights loadFromFile(Path file) throws IOException {
    List<String> all = TextIO.loadLines(file);
    return loadFromLines(all);
  }

  public static Weights loadFromLines(List<String> lines) {
    FloatValueMap<String> data = new FloatValueMap<>(10000);
    for (String s : lines) {
      float weight = Float.parseFloat(Strings.subStringUntilFirst(s, " "));
      String key = Strings.subStringAfterFirst(s, " ");
      data.set(key, weight);
    }
    return new Weights(data);
  }

  public void saveAsText(Path file) throws IOException {
    try (PrintWriter pw = new PrintWriter(file.toFile(), "utf-8")) {
      for (String s : data.getKeyList()) {
        pw.println(String.format(Locale.ENGLISH, "%.3f %s", data.get(s), s));
      }
    }
  }

  public Weights copy() {
    return new Weights(data.copy());
  }

  public void pruneNearZeroWeights() {
    FloatValueMap<String> pruned = new FloatValueMap<>();

    for (String key : data) {
      float w = data.get(key);
      if (Math.abs(w) > epsilon) {
        pruned.set(key, w);
      }
    }
    this.data = pruned;
  }

  public CompressedWeights compress() {
    return new CompressedWeights(LossyIntLookup.generate(data));
  }

  public float get(String key) {
    return data.get(key);
  }

  public void put(String key, float value) {
    this.data.set(key, value);
  }

  public void increment(String key, float value) {
    data.incrementByAmount(key, value);
  }

  @Override
  public Iterator<String> iterator() {
    return data.iterator();
  }
}
