package zemberek.dependency;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import zemberek.morphology.analysis.format.ConlluToken;

/**
 * Immutable representation of a single syntactic node in a Universal Dependencies v2
 * {@link DependencyTree}.
 *
 * <p>Node IDs are 1-based syntactic CoNLL-U indices; {@code headId == 0} indicates the syntactic
 * root of the sentence.
 */
public record DependencyNode(
    int id,
    String form,
    String lemma,
    String upos,
    String xpos,
    String feats,
    int headId,
    String deprel,
    String misc,
    int sourceWordIndex) {

  public static DependencyNode fromConlluToken(ConlluToken token) {
    return new DependencyNode(
        token.id(),
        token.form(),
        token.lemma(),
        token.upos(),
        token.xpos(),
        token.feats(),
        token.head(),
        token.deprel(),
        token.misc(),
        token.sourceWordIndex());
  }

  /** Returns {@code true} if this node attaches directly to the synthetic ROOT ({@code headId == 0}). */
  public boolean isRoot() {
    return headId == 0;
  }

  /** Returns the individual {@code Key=Value} UD morphological feature strings. */
  public List<String> featPairs() {
    if (feats == null || feats.isEmpty() || "_".equals(feats)) {
      return Collections.emptyList();
    }
    return Arrays.asList(feats.split("\\|"));
  }
}
