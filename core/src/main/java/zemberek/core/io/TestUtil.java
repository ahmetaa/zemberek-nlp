package zemberek.core.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import zemberek.core.ScoredItem;
import zemberek.core.collections.FloatValueMap;

public class TestUtil {

  @SafeVarargs
  public static <T> boolean containsAllKeys(Map<T, ?> map, T... keys) {
    for (T key : keys) {
      if (!map.containsKey(key)) {
        return false;
      }
    }
    return true;
  }

  @SafeVarargs
  public static <V> boolean containsAllValues(Map<?, V> map, V... values) {
    for (V value : values) {
      if (!map.containsValue(value)) {
        return false;
      }
    }
    return true;
  }

  @SafeVarargs
  public static <V> boolean containsAll(Set<V> set, V... values) {
    for (V value : values) {
      if (!set.contains(value)) {
        return false;
      }
    }
    return true;
  }

  public static Path tempFileWithData(Collection<String> collection) {
    try {
      Path temp = java.nio.file.Files.createTempFile("zemberek", "");
      temp.toFile().deleteOnExit();
      java.nio.file.Files.write(temp, collection, StandardCharsets.UTF_8);
      return temp;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public static Path tempFileWithData(String... lines) {
    try {
      Path temp = java.nio.file.Files.createTempFile("zemberek", "");
      temp.toFile().deleteOnExit();
      java.nio.file.Files.write(temp, Arrays.asList(lines), StandardCharsets.UTF_8);
      return temp;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SafeVarargs
  public static <T> boolean containsAll(FloatValueMap<T> set, T... items) {
    for (T item : items) {
      if (!set.contains(item)) {
        return false;
      }
    }
    return true;
  }

  @SafeVarargs
  public static <T> boolean containsAll(List<ScoredItem<T>> list, T... items) {
    Set<T> set = list.stream()
        .map(s1 -> s1.item)
        .collect(Collectors.toSet());
    for (T item : items) {
      if (!set.contains(item)) {
        return false;
      }
    }
    return true;
  }


  public static Set<String> uniqueStrings(int amount, int stringLength) {
    return uniqueStrings(amount, stringLength, -1);
  }

  public static Set<String> uniqueStrings(int amount, int stringLength, int randomSeed) {
    Set<String> set = new HashSet<>(amount);

    Random r = randomSeed == -1 ? new Random() : new Random(randomSeed);
    while (set.size() < amount) {
      StringBuilder sb = new StringBuilder(stringLength);
      for (int i = 0; i < stringLength; i++) {
        sb.append((char) (r.nextInt(32) + 'a'));
      }
      set.add(sb.toString());
    }
    return set;
  }

}
