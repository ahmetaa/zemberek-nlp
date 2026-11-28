package zemberek.dependency;

import java.util.Arrays;

/**
 * Exact non-projective Maximum Spanning Arborescence decoder using the Chu-Liu / Edmonds algorithm,
 * with exact single-root enforcement for Universal Dependencies trees.
 *
 * <p>Convention: node {@code 0} is the synthetic {@code ROOT}, and nodes {@code 1..N} are syntactic
 * tokens. {@code scores[dep][head]} is the score of directed arc {@code head -> dep}.
 */
public final class ChuLiuEdmonds {

  private static final double NEG_INF = -1e18;

  private ChuLiuEdmonds() {}

  /**
   * Decodes the exact maximum-scoring valid single-root spanning tree rooted at {@code 0}.
   *
   * @param scores {@code (N + 1) x (N + 1)} matrix where {@code scores[dep][head]} is the arc score
   *     for {@code head -> dep}.
   * @return array {@code heads} of length {@code N + 1} with {@code heads[0] = -1} and
   *     {@code heads[i] in [0..N]} for {@code i = 1..N}, guaranteed cycle-free with exactly one
   *     child of {@code 0}.
   */
  public static int[] decodeSingleRoot(double[][] scores) {
    int size = scores.length;
    if (size <= 1) {
      return new int[] {-1};
    }
    if (size == 2) {
      return new int[] {-1, 0};
    }

    // Fast path: unconstrained MST rooted at 0. In practice >98% of neural arc matrices already
    // produce a unique root child on the first pass.
    int[] unconstrained = decodeArborescence(scores);
    int rootChildren = 0;
    for (int i = 1; i < size; i++) {
      if (unconstrained[i] == 0) {
        rootChildren++;
      }
    }
    if (rootChildren == 1) {
      return unconstrained;
    }

    // Exact single-root resolution: try each candidate root r in 1..N, forcing 0 -> r to be the
    // sole outgoing arc from 0 and finding the exact max arborescence on the remaining nodes.
    double bestTotal = Double.NEGATIVE_INFINITY;
    int[] bestHeads = null;
    double[][] constrained = new double[size][size];

    for (int r = 1; r < size; r++) {
      for (int dep = 0; dep < size; dep++) {
        System.arraycopy(scores[dep], 0, constrained[dep], 0, size);
      }
      for (int dep = 1; dep < size; dep++) {
        if (dep == r) {
          Arrays.fill(constrained[r], NEG_INF);
          constrained[r][0] = scores[r][0];
        } else {
          constrained[dep][0] = NEG_INF;
        }
      }
      int[] candidate = decodeArborescence(constrained);
      double total = 0.0;
      for (int dep = 1; dep < size; dep++) {
        total += constrained[dep][candidate[dep]];
      }
      if (bestHeads == null || total > bestTotal) {
        bestTotal = total;
        bestHeads = candidate;
      }
    }
    return bestHeads;
  }

  /**
   * Standard recursive Chu-Liu / Edmonds maximum spanning arborescence rooted at node {@code 0}
   * (allows one or more children of {@code 0}, guaranteed cycle-free).
   */
  public static int[] decodeArborescence(double[][] scores) {
    int n = scores.length;
    int[] bestHead = new int[n];
    bestHead[0] = -1;

    // 1. Greedy highest-scoring incoming edge for every non-root node.
    for (int dep = 1; dep < n; dep++) {
      int argmax = 0;
      double maxScore = Double.NEGATIVE_INFINITY;
      double[] row = scores[dep];
      for (int head = 0; head < n; head++) {
        if (head == dep) {
          continue;
        }
        if (row[head] > maxScore) {
          maxScore = row[head];
          argmax = head;
        }
      }
      bestHead[dep] = argmax;
    }

    // 2. Detect any directed cycle.
    // state: 0 = unvisited, >0 = visit stamp of current path, -1 = verified acyclic
    int[] visited = new int[n];
    visited[0] = -1;
    boolean[] inCycle = new boolean[n];
    int cycleStart = -1;

    for (int start = 1; start < n; start++) {
      if (visited[start] != 0) {
        continue;
      }
      int cur = start;
      while (cur != 0 && visited[cur] == 0) {
        visited[cur] = start;
        cur = bestHead[cur];
      }
      if (cur != 0 && visited[cur] == start) {
        cycleStart = cur;
        int node = cur;
        do {
          inCycle[node] = true;
          node = bestHead[node];
        } while (node != cur);
        break;
      }
      int walk = start;
      while (walk != 0 && visited[walk] == start) {
        visited[walk] = -1;
        walk = bestHead[walk];
      }
    }

    // Acyclic -> optimal arborescence found!
    if (cycleStart == -1) {
      return bestHead;
    }

    // 3. Contract the cycle into a single supernode.
    int[] comp = new int[n];
    int[] origOfNonCycle = new int[n];
    comp[0] = 0;
    origOfNonCycle[0] = 0;
    int nextId = 1;
    for (int v = 1; v < n; v++) {
      if (!inCycle[v]) {
        comp[v] = nextId;
        origOfNonCycle[nextId] = v;
        nextId++;
      }
    }
    int cycleId = nextId++;
    for (int v = 1; v < n; v++) {
      if (inCycle[v]) {
        comp[v] = cycleId;
      }
    }

    int m = nextId;
    double[][] cScores = new double[m][m];
    int[][] bestOrigHead = new int[m][m];
    int[][] bestOrigDep = new int[m][m];
    for (int i = 0; i < m; i++) {
      Arrays.fill(cScores[i], Double.NEGATIVE_INFINITY);
      Arrays.fill(bestOrigHead[i], -1);
      Arrays.fill(bestOrigDep[i], -1);
    }

    for (int dep = 1; dep < n; dep++) {
      int cd = comp[dep];
      for (int head = 0; head < n; head++) {
        if (head == dep) {
          continue;
        }
        int ch = comp[head];
        if (ch == cd) {
          continue;
        }
        double w = scores[dep][head];
        if (inCycle[dep] && Double.isFinite(w) && Double.isFinite(scores[dep][bestHead[dep]])) {
          // Replacing dep's cycle-internal parent bestHead[dep] with external head.
          w -= scores[dep][bestHead[dep]];
        }
        if (w > cScores[cd][ch] || bestOrigHead[cd][ch] == -1) {
          cScores[cd][ch] = w;
          bestOrigHead[cd][ch] = head;
          bestOrigDep[cd][ch] = dep;
        }
      }
    }

    // 4. Recurse on contracted graph.
    int[] cHeads = decodeArborescence(cScores);

    // 5. Expand contracted arborescence back to original nodes.
    int[] result = new int[n];
    result[0] = -1;
    // Non-cycle nodes take their contracted parent's best original source.
    for (int cNode = 1; cNode < m; cNode++) {
      if (cNode == cycleId) {
        continue;
      }
      int origDep = origOfNonCycle[cNode];
      int cHead = cHeads[cNode];
      result[origDep] = bestOrigHead[cNode][cHead];
    }

    // Cycle nodes keep their cycle-internal parents except the single node entered from outside.
    int cHeadOfCycle = cHeads[cycleId];
    int kickDep = bestOrigDep[cycleId][cHeadOfCycle];
    int kickHead = bestOrigHead[cycleId][cHeadOfCycle];
    for (int v = 1; v < n; v++) {
      if (inCycle[v]) {
        result[v] = (v == kickDep) ? kickHead : bestHead[v];
      }
    }
    return result;
  }
}
