package zemberek.core.compression;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.data.CompressedWeights;
import zemberek.core.hash.MultiLevelMphf;

public class LossyIntLookupTest {

  @Test
  public void multiPartHashMatchesConcatenatedHash() {
    String[] parts = {"", "15:", "Noun+A3sg-", "Verb+Past+A1sg", "çğıöşüİ"};
    int[] seeds = {-1, 0, 1, 7, 42, 255};
    for (int seed : seeds) {
      for (String p1 : parts) {
        for (String p2 : parts) {
          Assert.assertEquals(
              MultiLevelMphf.hash(p1 + p2, seed),
              MultiLevelMphf.hash(p1, p2, seed));
          for (String p3 : parts) {
            Assert.assertEquals(
                MultiLevelMphf.hash(p1 + p2 + p3, seed),
                MultiLevelMphf.hash(p1, p2, p3, seed));
            for (String p4 : parts) {
              Assert.assertEquals(
                  MultiLevelMphf.hash(p1 + p2 + p3 + p4, seed),
                  MultiLevelMphf.hash(p1, p2, p3, p4, seed));
            }
          }
        }
      }
    }
  }

  @Test
  public void multiPartFloatLookupMatchesSingleKeyLookup() {
    FloatValueMap<String> map = new FloatValueMap<>();
    String longSuffix = "x".repeat(300);
    String[][] twoPart = {
        {"4:", "kitap+Noun+A3sg"},
        {"10:", "kitap"},
        {"17:Noun+A3sg-", "Verb+Pres"},
        {"20:0-", "Adj+çğıöşü"},
        {"long:", longSuffix}
    };
    String[][] threePart = {
        {"15:Noun+A3sg-", "Adj-", "Verb+Past"},
        {"15:<s>-", "Noun+A3pl+Loc-", "Verb+Narr+A3sg"},
        {"15:Verb-", "", "</s>"},
        {"15:long-", longSuffix, "-end"}
    };
    String[][] fourPart = {
        {"15:", "Noun+A3sg-", "Adj-", "Verb+Past"},
        {"2:", "kitap+Noun+A3sg+Pnon+Acc", "-", "okumak+Verb+Past+A3sg"},
        {"9:", "kitap", "-", "okumak"},
        {"3:", "", "-", ""},
        {"15:", "Noun-", longSuffix, "-end"}
    };

    float v = 1.25f;
    for (String[] kv : twoPart) {
      map.set(kv[0] + kv[1], v);
      v += 0.5f;
    }
    for (String[] kv : threePart) {
      map.set(kv[0] + kv[1] + kv[2], v);
      v += 0.5f;
    }
    for (String[] kv : fourPart) {
      map.set(kv[0] + kv[1] + kv[2] + kv[3], v);
      v += 0.5f;
    }

    LossyIntLookup lookup = LossyIntLookup.generate(map);
    CompressedWeights weights = new CompressedWeights(lookup);

    for (String[] kv : twoPart) {
      float expected = lookup.getAsFloat(kv[0] + kv[1]);
      Assert.assertEquals(expected, lookup.getAsFloat(kv[0], kv[1]), 0f);
      Assert.assertEquals(expected, weights.get(kv[0], kv[1]), 0f);
    }
    for (String[] kv : threePart) {
      float expected = lookup.getAsFloat(kv[0] + kv[1] + kv[2]);
      Assert.assertEquals(expected, lookup.getAsFloat(kv[0], kv[1], kv[2]), 0f);
      Assert.assertEquals(expected, weights.get(kv[0], kv[1], kv[2]), 0f);
    }
    for (String[] kv : fourPart) {
      float expected = lookup.getAsFloat(kv[0] + kv[1] + kv[2] + kv[3]);
      Assert.assertEquals(expected, lookup.getAsFloat(kv[0], kv[1], kv[2], kv[3]), 0f);
      Assert.assertEquals(expected, weights.get(kv[0], kv[1], kv[2], kv[3]), 0f);
    }

    // Absent keys should match single-string lookup (0f)
    Assert.assertEquals(0f, weights.get("4:", "nonexistent_key_12345"), 0f);
    Assert.assertEquals(0f, weights.get("15:absent1-", "absent2-", "absent3"), 0f);
    Assert.assertEquals(0f, weights.get("15:", "absent1-", "absent2-", "absent3"), 0f);
  }
}
