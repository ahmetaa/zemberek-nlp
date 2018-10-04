package zemberek.core.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.logging.Log;

public class UIntValueMapTest {

  @Test
  public void stressTest() {

    List<String> stringSet = randomNumberStrings(100_000);
    UIntValueMap<String> a = new UIntValueMap<>();
    UIntValueMap<String> b = new UIntValueMap<>();

    for (int i = 0; i < 20; i++) {
      for (String s : stringSet) {
        char c = s.charAt(s.length() - 1);
        if (a.contains(s)) {
          a.incrementByAmount(s, 1);
          continue;
        }
        if (b.contains(s)) {
          b.incrementByAmount(s, 1);
          continue;
        }
        if (c % 2 == 0) {
          a.incrementByAmount(s, 1);
        } else {
          b.incrementByAmount(s, 1);
        }
      }
    }
    Log.info("[a] key count = %d ", a.size());
    Log.info("[b] key count = %d ", b.size());

    // a and b cannot have shared keys.
    for (String k : a) {
      Assert.assertFalse(b.contains(k));
    }

  }

  private List<String> randomNumberStrings(int k) {

    List<String> intSet = new ArrayList<>();
    Random rnd = new Random(1);
    while (intSet.size() < k) {
      int r = rnd.nextInt(100_000);
      intSet.add(String.valueOf(r + 1));
    }
    return intSet;
  }

}
