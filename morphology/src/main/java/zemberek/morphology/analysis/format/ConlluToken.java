package zemberek.morphology.analysis.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable structured representation of a single syntactic word row in Universal Dependencies v2
 * 10-column {@code CoNLL-U} format.
 *
 * <p>Column values follow CoNLL-U v2 conventions: missing/unspecified string fields are normalized
 * to {@code "_"}, and an unassigned syntactic {@code HEAD} is represented by
 * {@link #UNASSIGNED_HEAD} ({@code -1}).
 */
public record ConlluToken(
    int id,
    String form,
    String lemma,
    String upos,
    String xpos,
    String feats,
    int head,
    String deprel,
    String deps,
    String misc,
    int sourceWordIndex) {

  /** Sentinel value for an unassigned CoNLL-U {@code HEAD} column ({@code "_"}). */
  public static final int UNASSIGNED_HEAD = -1;

  public ConlluToken {
    if (id < 1) {
      throw new IllegalArgumentException("CoNLL-U token id must be >= 1, got: " + id);
    }
    if (head < UNASSIGNED_HEAD) {
      throw new IllegalArgumentException("CoNLL-U head must be >= -1, got: " + head);
    }
    form = normalizeField(form);
    lemma = normalizeField(lemma);
    upos = normalizeField(upos);
    xpos = normalizeField(xpos);
    feats = normalizeField(feats);
    deprel = normalizeField(deprel);
    deps = normalizeField(deps);
    misc = normalizeField(misc);
  }

  /**
   * Creates a morphology-only token with unassigned syntactic columns ({@code HEAD = _},
   * {@code DEPREL = _}, {@code DEPS = _}).
   */
  public static ConlluToken ofMorphology(
      int id,
      String form,
      String lemma,
      String upos,
      String xpos,
      String feats,
      String misc,
      int sourceWordIndex) {
    return new ConlluToken(
        id, form, lemma, upos, xpos, feats, UNASSIGNED_HEAD, "_", "_", misc, sourceWordIndex);
  }

  /** Returns {@code true} if this token has an assigned syntactic head ({@code head >= 0}). */
  public boolean hasHead() {
    return head >= 0;
  }

  /** Returns a copy of this token with the given syntactic {@code head} and {@code deprel}. */
  public ConlluToken withDependency(int head, String deprel) {
    return new ConlluToken(
        id, form, lemma, upos, xpos, feats, head, deprel, deps, misc, sourceWordIndex);
  }

  /**
   * Returns the individual {@code Key=Value} morphological feature strings from {@code FEATS},
   * or an empty list if {@code FEATS} is {@code "_"}.
   */
  public List<String> featPairs() {
    if ("_".equals(feats)) {
      return Collections.emptyList();
    }
    return Arrays.asList(feats.split("\\|"));
  }

  /** Formats this token as a 10-column tab-separated CoNLL-U row (without trailing newline). */
  public String toConlluRow() {
    StringBuilder sb = new StringBuilder(64);
    appendConlluRow(sb);
    return sb.toString();
  }

  void appendConlluRow(StringBuilder sb) {
    sb.append(id).append('\t')
        .append(form).append('\t')
        .append(lemma).append('\t')
        .append(upos).append('\t')
        .append(xpos).append('\t')
        .append(feats).append('\t')
        .append(head >= 0 ? Integer.toString(head) : "_").append('\t')
        .append(deprel).append('\t')
        .append(deps).append('\t')
        .append(misc);
  }

  static String normalizeField(String value) {
    return (value == null || value.isEmpty()) ? "_" : value;
  }
}
