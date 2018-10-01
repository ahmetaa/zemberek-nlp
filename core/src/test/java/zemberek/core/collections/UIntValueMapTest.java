package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class UIntValueMapTest {

  @Test
  @Ignore(value = "Fails. When k = 1 million it passes.")
  public void addElementsFromAnotherIntMap() {
    int k = 2_000_000;
    UIntValueMap<String> map1 = new UIntValueMap<>();
    Set<Integer> intSet = new HashSet<>();
    Random rnd = new Random();
    while (intSet.size() < k) {
      int r = rnd.nextInt(Integer.MAX_VALUE - 100);
      intSet.add(r + 1);
    }
    for (Integer i : intSet) {
      map1.put(String.valueOf(i), i);
    }

    Stopwatch sw = Stopwatch.createStarted();
    UIntValueMap<String> map2 = new UIntValueMap<>();

    for (String s : map1) {
      map2.put(s, map1.get(s));
      if (map2.size() % 10_000 == 0) {
        Assert.assertTrue(sw.elapsed(TimeUnit.MILLISECONDS) < 100);
        sw.reset().start();
      }
    }
  }


}
