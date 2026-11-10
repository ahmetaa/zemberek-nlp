package zemberek.core.data;

public interface WeightLookup {

  float get(String key);

  default float get(String prefix, String suffix) {
    return get(prefix + suffix);
  }

  int size();

}
