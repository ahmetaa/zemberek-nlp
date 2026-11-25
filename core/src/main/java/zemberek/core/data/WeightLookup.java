package zemberek.core.data;

public interface WeightLookup {

  float get(String key);

  default float get(String prefix, String suffix) {
    return get(prefix + suffix);
  }

  default float get(String p1, String p2, String p3) {
    return get(p1 + p2 + p3);
  }

  int size();

}
