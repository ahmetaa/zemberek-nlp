package zemberek.dependency;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import zemberek.morphology.analysis.format.ConlluSentence;
import zemberek.morphology.analysis.format.ConlluToken;

/**
 * Immutable Universal Dependencies v2 dependency tree for a single sentence, backed by a
 * {@link ConlluSentence} with populated {@code HEAD} and {@code DEPREL} columns.
 */
public final class DependencyTree {

  public record EvaluationMetrics(
      int sentences,
      int tokens,
      int uasCorrect,
      int lasCorrect,
      int exactSentences,
      int rootCorrect) {

    public double uas() {
      return tokens == 0 ? 0.0 : (100.0 * uasCorrect) / tokens;
    }

    public double las() {
      return tokens == 0 ? 0.0 : (100.0 * lasCorrect) / tokens;
    }

    public double exactMatch() {
      return sentences == 0 ? 0.0 : (100.0 * exactSentences) / sentences;
    }

    public double rootAccuracy() {
      return sentences == 0 ? 0.0 : (100.0 * rootCorrect) / sentences;
    }

    @Override
    public String toString() {
      return String.format(Locale.ROOT,
          "UAS: %.2f%% (%d/%d) | LAS: %.2f%% (%d/%d) | Root: %.2f%% | Exact: %.2f%% (%d sents)",
          uas(), uasCorrect, tokens, las(), lasCorrect, tokens,
          rootAccuracy(), exactMatch(), sentences);
    }
  }

  private final ConlluSentence conlluSentence;
  private final List<DependencyNode> nodes;

  public DependencyTree(ConlluSentence conlluSentence) {
    this.conlluSentence = conlluSentence;
    List<DependencyNode> list = new ArrayList<>(conlluSentence.size());
    for (ConlluToken token : conlluSentence.tokens()) {
      list.add(DependencyNode.fromConlluToken(token));
    }
    this.nodes = Collections.unmodifiableList(list);
  }

  public String getText() {
    return conlluSentence.text();
  }

  public String getSentId() {
    return conlluSentence.sentId();
  }

  public int size() {
    return nodes.size();
  }

  public List<DependencyNode> getNodes() {
    return nodes;
  }

  public DependencyNode nodeById(int id) {
    if (id < 1 || id > nodes.size()) {
      throw new IllegalArgumentException("Node id out of range [1.." + nodes.size() + "]: " + id);
    }
    return nodes.get(id - 1);
  }

  /** Returns the syntactic root node (first node with {@code headId == 0}), or {@code null} if empty. */
  public DependencyNode getRoot() {
    for (DependencyNode node : nodes) {
      if (node.isRoot()) {
        return node;
      }
    }
    return null;
  }

  /** Returns all direct syntactic dependents of {@code headId} in linear sentence order. */
  public List<DependencyNode> getChildren(int headId) {
    List<DependencyNode> children = new ArrayList<>();
    for (DependencyNode node : nodes) {
      if (node.headId() == headId) {
        children.add(node);
      }
    }
    return children;
  }

  /** Returns the full syntactic subtree rooted at {@code rootId} (inclusive), ordered by token id. */
  public List<DependencyNode> getSubtree(int rootId) {
    List<DependencyNode> subtree = new ArrayList<>();
    boolean[] visited = new boolean[nodes.size() + 1];
    Deque<Integer> queue = new ArrayDeque<>();
    queue.add(rootId);
    visited[rootId] = true;
    while (!queue.isEmpty()) {
      int cur = queue.poll();
      subtree.add(nodeById(cur));
      for (DependencyNode node : nodes) {
        if (node.headId() == cur && !visited[node.id()]) {
          visited[node.id()] = true;
          queue.add(node.id());
        }
      }
    }
    subtree.sort(Comparator.comparingInt(DependencyNode::id));
    return subtree;
  }

  public ConlluSentence getConlluSentence() {
    return conlluSentence;
  }

  /** Serializes this dependency tree in standard 10-column CoNLL-U v2 format. */
  public String toConllu() {
    return conlluSentence.toConllu();
  }

  @Override
  public String toString() {
    return toConllu();
  }

  /**
   * Computes standard CoNLL-U UAS, LAS, Root Accuracy, and Exact Sentence Match over aligned
   * gold and predicted trees (including punctuation, matching standard UD CoNLL evaluation).
   */
  public static EvaluationMetrics evaluate(
      List<ConlluSentence> goldSentences, List<DependencyTree> predictedTrees) {
    if (goldSentences.size() != predictedTrees.size()) {
      throw new IllegalArgumentException(
          "Gold (" + goldSentences.size() + ") and predicted (" + predictedTrees.size()
              + ") sentence counts must match.");
    }
    int tokens = 0;
    int uas = 0;
    int las = 0;
    int exact = 0;
    int rootCorrect = 0;

    for (int s = 0; s < goldSentences.size(); s++) {
      ConlluSentence gold = goldSentences.get(s);
      DependencyTree pred = predictedTrees.get(s);
      if (gold.size() != pred.size()) {
        throw new IllegalArgumentException(
            "Token count mismatch at sentence " + s + ": gold=" + gold.size()
                + ", pred=" + pred.size());
      }
      boolean sentExact = true;
      boolean sentRootCorrect = false;
      for (int i = 0; i < gold.size(); i++) {
        ConlluToken g = gold.tokens().get(i);
        DependencyNode p = pred.getNodes().get(i);
        tokens++;
        boolean headMatch = (g.head() == p.headId());
        boolean relMatch = headMatch && g.deprel().equals(p.deprel());
        if (headMatch) {
          uas++;
        }
        if (relMatch) {
          las++;
        } else {
          sentExact = false;
        }
        if (g.head() == 0 && p.headId() == 0) {
          sentRootCorrect = true;
        }
      }
      if (sentExact) {
        exact++;
      }
      if (sentRootCorrect) {
        rootCorrect++;
      }
    }
    return new EvaluationMetrics(goldSentences.size(), tokens, uas, las, exact, rootCorrect);
  }
}
