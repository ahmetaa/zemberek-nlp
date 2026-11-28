package zemberek.morphology.analysis.format;

/**
 * Immutable representation of a Universal Dependencies v2 Multi-Word Token ({@code startId-endId})
 * range line in CoNLL-U format (for example, {@code 1-2\tİstanbul'daydı\t_\t...}).
 */
public record ConlluMwtSpan(
    int startId,
    int endId,
    String form,
    String misc,
    int sourceWordIndex) {

  public ConlluMwtSpan {
    if (startId < 1 || endId <= startId) {
      throw new IllegalArgumentException(
          "Invalid MWT span range: " + startId + "-" + endId);
    }
    form = ConlluToken.normalizeField(form);
    misc = ConlluToken.normalizeField(misc);
  }

  /** Formats this MWT span as a 10-column tab-separated CoNLL-U row (without trailing newline). */
  public String toConlluRow() {
    StringBuilder sb = new StringBuilder(48);
    appendConlluRow(sb);
    return sb.toString();
  }

  void appendConlluRow(StringBuilder sb) {
    sb.append(startId).append('-').append(endId).append('\t')
        .append(form).append('\t')
        .append("_\t_\t_\t_\t_\t_\t_\t")
        .append(misc);
  }
}
