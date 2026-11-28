package zemberek.dependency;

import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class ChuLiuEdmondsTest {

  @Test
  public void matchesBruteForceOptimumOnRandomDenseGraphs() {
    Random rnd = new Random(42);
    for (int n = 2; n <= 5; n++) {
      int trials = (n <= 4) ? 60 : 25;
      for (int t = 0; t < trials; t++) {
        double[][] scores = new double[n + 1][n + 1];
        for (int dep = 1; dep <= n; dep++) {
          for (int head = 0; head <= n; head++) {
            scores[dep][head] = (head == dep) ? -1e9 : (rnd.nextDouble() * 20.0 - 10.0);
          }
        }
        int[] cleHeads = ChuLiuEdmonds.decodeSingleRoot(scores);
        assertValidSingleRootTree(cleHeads);

        double cleScore = treeScore(scores, cleHeads);
        double bruteBest = bruteForceBestSingleRoot(scores, n);
        Assert.assertEquals(
            "MST score mismatch for n=" + n + ", trial=" + t,
            bruteBest, cleScore, 1e-9);
      }
    }
  }

  @Test
  public void recoversNonProjectiveCrossingDependencies() {
    // 1 -> 3 and 2 -> 4 cross, with 0 -> 1 as root (valid non-projective Turkish scrambling pattern).
    double[][] scores = new double[5][5];
    for (int d = 1; d <= 4; d++) {
      for (int h = 0; h <= 4; h++) {
        scores[d][h] = (d == h) ? -1e9 : -10.0;
      }
    }
    scores[1][0] = 10.0; // 0 -> 1 (root)
    scores[3][1] = 10.0; // 1 -> 3
    scores[2][3] = 10.0; // 3 -> 2
    scores[4][2] = 10.0; // 2 -> 4 (crosses 1 -> 3!)

    int[] heads = ChuLiuEdmonds.decodeSingleRoot(scores);
    assertValidSingleRootTree(heads);
    Assert.assertEquals(0, heads[1]);
    Assert.assertEquals(3, heads[2]);
    Assert.assertEquals(1, heads[3]);
    Assert.assertEquals(2, heads[4]);
  }

  @Test
  public void resolvesMultipleStrongRootsAndNestedCycles() {
    // Two strong candidates for root (1 and 3) plus a 2-cycle between 2 and 4.
    double[][] scores = new double[5][5];
    for (int d = 1; d <= 4; d++) {
      for (int h = 0; h <= 4; h++) {
        scores[d][h] = (d == h) ? -1e9 : -5.0;
      }
    }
    scores[1][0] = 8.0;
    scores[3][0] = 7.0;
    scores[3][1] = 6.0;
    scores[2][4] = 9.0;
    scores[4][2] = 9.0;
    scores[2][3] = 4.0;

    int[] heads = ChuLiuEdmonds.decodeSingleRoot(scores);
    assertValidSingleRootTree(heads);
    double expected = bruteForceBestSingleRoot(scores, 4);
    Assert.assertEquals(expected, treeScore(scores, heads), 1e-9);
  }

  @Test
  public void handlesTrivialAndLongSentences() {
    Assert.assertArrayEquals(new int[] {-1}, ChuLiuEdmonds.decodeSingleRoot(new double[1][1]));
    Assert.assertArrayEquals(new int[] {-1, 0}, ChuLiuEdmonds.decodeSingleRoot(new double[2][2]));

    Random rnd = new Random(123);
    int n = 50;
    double[][] scores = new double[n + 1][n + 1];
    for (int d = 1; d <= n; d++) {
      for (int h = 0; h <= n; h++) {
        scores[d][h] = (d == h) ? -1e9 : rnd.nextGaussian();
      }
    }
    int[] heads = ChuLiuEdmonds.decodeSingleRoot(scores);
    assertValidSingleRootTree(heads);
  }

  @Test
  public void handlesSentinelAndInfiniteMaskedEdgesWithoutCycles() {
    // Regression case: cycle (1 <-> 2) with -1e18 masked incoming arcs from node 3.
    double[][] scores = {
        {0.0, 0.0, 0.0, 0.0},
        {10.0, -1e9, 5.0, -1e18},
        {10.0, 5.0, -1e9, -1e18},
        {100.0, 2.0, 2.0, -1e9}
    };
    int[] heads = ChuLiuEdmonds.decodeSingleRoot(scores);
    assertValidSingleRootTree(heads);
    Assert.assertEquals(bruteForceBestSingleRoot(scores, 3), treeScore(scores, heads), 1e-6);

    // Same graph with Double.NEGATIVE_INFINITY masked incoming arcs must not produce NaN.
    double[][] infScores = {
        {0.0, 0.0, 0.0, 0.0},
        {10.0, Double.NEGATIVE_INFINITY, 5.0, Double.NEGATIVE_INFINITY},
        {10.0, 5.0, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY},
        {100.0, 2.0, 2.0, Double.NEGATIVE_INFINITY}
    };
    int[] infHeads = ChuLiuEdmonds.decodeSingleRoot(infScores);
    assertValidSingleRootTree(infHeads);
  }

  private static void assertValidSingleRootTree(int[] heads) {
    int n = heads.length - 1;
    Assert.assertEquals(-1, heads[0]);
    int rootCount = 0;
    for (int i = 1; i <= n; i++) {
      Assert.assertTrue("Head out of range", heads[i] >= 0 && heads[i] <= n);
      Assert.assertNotEquals("Self loop at " + i, i, heads[i]);
      if (heads[i] == 0) {
        rootCount++;
      }
      // Verify path reaches 0 within n steps (acyclic)
      int cur = i;
      for (int steps = 0; steps <= n && cur != 0; steps++) {
        cur = heads[cur];
      }
      Assert.assertEquals("Cycle detected from node " + i, 0, cur);
    }
    Assert.assertEquals("Must have exactly one root child", 1, rootCount);
  }

  private static double treeScore(double[][] scores, int[] heads) {
    double sum = 0.0;
    for (int i = 1; i < heads.length; i++) {
      sum += scores[i][heads[i]];
    }
    return sum;
  }

  private static double bruteForceBestSingleRoot(double[][] scores, int n) {
    int[] heads = new int[n + 1];
    heads[0] = -1;
    double[] best = new double[] {Double.NEGATIVE_INFINITY};
    enumerate(scores, n, 1, heads, 0, best);
    return best[0];
  }

  private static void enumerate(
      double[][] scores, int n, int dep, int[] heads, int rootCount, double[] best) {
    if (dep > n) {
      if (rootCount != 1) {
        return;
      }
      for (int i = 1; i <= n; i++) {
        int cur = i;
        for (int steps = 0; steps <= n && cur != 0; steps++) {
          cur = heads[cur];
        }
        if (cur != 0) {
          return;
        }
      }
      double s = treeScore(scores, heads);
      if (s > best[0]) {
        best[0] = s;
      }
      return;
    }
    for (int h = 0; h <= n; h++) {
      if (h == dep) {
        continue;
      }
      if (h == 0 && rootCount >= 1) {
        continue;
      }
      heads[dep] = h;
      enumerate(scores, n, dep + 1, heads, rootCount + (h == 0 ? 1 : 0), best);
    }
  }
}
