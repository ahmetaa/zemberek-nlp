package zemberek.morphology.analysis.format;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable structured representation of a sentence in Universal Dependencies v2 {@code CoNLL-U}
 * format, supporting both serialization ({@link #toConllu()}) and parsing
 * ({@link #parseAll(String)}, {@link #parseFile(Path)}).
 *
 * <p>Bridges Zemberek's morphological projection ({@link ConlluFormatter#projectSentence}) directly
 * to downstream syntactic dependency parsers without intermediate string round-trips.
 */
public record ConlluSentence(
    String sentId,
    String text,
    List<ConlluToken> tokens,
    List<ConlluMwtSpan> mwtSpans) {

  public ConlluSentence {
    tokens = tokens == null ? Collections.emptyList() : List.copyOf(tokens);
    mwtSpans = mwtSpans == null ? Collections.emptyList() : List.copyOf(mwtSpans);
  }

  /** Number of syntactic tokens in this sentence. */
  public int size() {
    return tokens.size();
  }

  /** Returns the 1-indexed syntactic token with the given CoNLL-U {@code ID}. */
  public ConlluToken tokenById(int id) {
    if (id >= 1 && id <= tokens.size()) {
      ConlluToken candidate = tokens.get(id - 1);
      if (candidate.id() == id) {
        return candidate;
      }
    }
    for (ConlluToken token : tokens) {
      if (token.id() == id) {
        return token;
      }
    }
    throw new IllegalArgumentException("No CoNLL-U token with id=" + id);
  }

  /**
   * Returns a new {@code ConlluSentence} with syntactic {@code HEAD} and {@code DEPREL} columns
   * populated from 0-indexed arrays matching {@link #tokens()}.
   */
  public ConlluSentence withDependencies(int[] heads, String[] deprels) {
    if (heads == null || deprels == null
        || heads.length != tokens.size()
        || deprels.length != tokens.size()) {
      throw new IllegalArgumentException(
          "heads and deprels arrays must match token count (" + tokens.size() + ")");
    }
    List<ConlluToken> updated = new ArrayList<>(tokens.size());
    for (int i = 0; i < tokens.size(); i++) {
      updated.add(tokens.get(i).withDependency(heads[i], deprels[i]));
    }
    return new ConlluSentence(sentId, text, updated, mwtSpans);
  }

  /**
   * Reconstructs the sequence of surface word forms in this sentence by collapsing any
   * {@link ConlluMwtSpan} ranges back into their original orthographic surface tokens.
   */
  public List<String> surfaceForms() {
    if (mwtSpans.isEmpty()) {
      List<String> forms = new ArrayList<>(tokens.size());
      for (ConlluToken token : tokens) {
        forms.add(token.form());
      }
      return forms;
    }
    Map<Integer, ConlluMwtSpan> spanByStart = new HashMap<>(mwtSpans.size() * 2);
    for (ConlluMwtSpan span : mwtSpans) {
      spanByStart.put(span.startId(), span);
    }
    List<String> forms = new ArrayList<>(tokens.size());
    int skipUntilId = -1;
    for (ConlluToken token : tokens) {
      if (token.id() <= skipUntilId) {
        continue;
      }
      ConlluMwtSpan span = spanByStart.get(token.id());
      if (span != null) {
        forms.add(span.form());
        skipUntilId = span.endId();
      } else {
        forms.add(token.form());
      }
    }
    return forms;
  }

  /** Serializes this sentence in standard 10-column CoNLL-U v2 format (trailing newline). */
  public String toConllu() {
    StringBuilder sb = new StringBuilder(tokens.size() * 64 + 64);
    if (sentId != null && !sentId.isEmpty()) {
      sb.append("# sent_id = ").append(sentId).append('\n');
    }
    if (text != null && !text.isEmpty()) {
      sb.append("# text = ").append(text).append('\n');
    }
    Map<Integer, ConlluMwtSpan> spanByStart = null;
    if (!mwtSpans.isEmpty()) {
      spanByStart = new HashMap<>(mwtSpans.size() * 2);
      for (ConlluMwtSpan span : mwtSpans) {
        spanByStart.put(span.startId(), span);
      }
    }
    for (ConlluToken token : tokens) {
      if (spanByStart != null) {
        ConlluMwtSpan span = spanByStart.get(token.id());
        if (span != null) {
          span.appendConlluRow(sb);
          sb.append('\n');
        }
      }
      token.appendConlluRow(sb);
      sb.append('\n');
    }
    return sb.toString();
  }

  /** Formats a list of sentences separated by blank lines per the CoNLL-U specification. */
  public static String formatAll(List<ConlluSentence> sentences) {
    StringBuilder sb = new StringBuilder(sentences.size() * 256);
    for (int i = 0; i < sentences.size(); i++) {
      if (i > 0) {
        sb.append('\n');
      }
      sb.append(sentences.get(i).toConllu());
    }
    return sb.toString();
  }

  /** Parses all sentences from a UTF-8 CoNLL-U file by streaming line-by-line. */
  public static List<ConlluSentence> parseFile(Path path) throws IOException {
    LineParser parser = new LineParser();
    try (java.io.BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        parser.accept(line);
      }
    }
    return parser.finish();
  }

  /** Parses all sentences from a CoNLL-U formatted string. */
  public static List<ConlluSentence> parseAll(String conlluText) {
    if (conlluText == null || conlluText.isEmpty()) {
      return Collections.emptyList();
    }
    return parseAll(Arrays.asList(conlluText.split("\\r?\\n", -1)));
  }

  /**
   * Parses CoNLL-U v2 sentences from raw lines.
   *
   * <p>Preserves {@code # sent_id}, {@code # text}, syntactic tokens ({@code 1..N}), and MWT range
   * lines ({@code i-j}). Enhanced-dependency empty nodes ({@code i.1}) are ignored.
   */
  public static List<ConlluSentence> parseAll(List<String> lines) {
    LineParser parser = new LineParser();
    for (String rawLine : lines) {
      parser.accept(rawLine);
    }
    return parser.finish();
  }

  private static final class LineParser {
    private final List<ConlluSentence> result = new ArrayList<>();
    private String sentId;
    private String text;
    private List<ConlluToken> tokens = new ArrayList<>();
    private List<ConlluMwtSpan> mwtSpans = new ArrayList<>();
    private int sourceWordIdx;
    private int activeMwtEnd = -1;
    private int activeMwtWordIdx = -1;

    void accept(String rawLine) {
      if (rawLine.isBlank()) {
        flushSentence();
        return;
      }
      String line = stripLineEnding(rawLine);
      if (line.startsWith("#")) {
        if (line.startsWith("# sent_id =")) {
          sentId = line.substring("# sent_id =".length()).trim();
        } else if (line.startsWith("# text =")) {
          text = line.substring("# text =".length()).trim();
        }
        return;
      }

      String[] cols = line.split("\t", -1);
      if (cols.length != 10) {
        throw new IllegalArgumentException(
            "Expected 10 tab-separated CoNLL-U columns, got " + cols.length + ": " + line);
      }
      String idField = cols[0];
      if (idField.indexOf('.') >= 0) {
        // Skip empty nodes (e.g. "5.1") used only in enhanced UD graphs.
        return;
      }
      int dashIdx = idField.indexOf('-');
      if (dashIdx > 0) {
        int start = Integer.parseInt(idField.substring(0, dashIdx));
        int end = Integer.parseInt(idField.substring(dashIdx + 1));
        int wordIdx = sourceWordIdx++;
        mwtSpans.add(new ConlluMwtSpan(start, end, cols[1], cols[9], wordIdx));
        activeMwtEnd = end;
        activeMwtWordIdx = wordIdx;
        return;
      }

      int id = Integer.parseInt(idField);
      int head = "_".equals(cols[6]) ? ConlluToken.UNASSIGNED_HEAD : Integer.parseInt(cols[6]);
      int wordIdx = (id <= activeMwtEnd) ? activeMwtWordIdx : sourceWordIdx++;
      tokens.add(new ConlluToken(
          id, cols[1], cols[2], cols[3], cols[4], cols[5], head, cols[7], cols[8], cols[9],
          wordIdx));
    }

    List<ConlluSentence> finish() {
      flushSentence();
      return result;
    }

    private void flushSentence() {
      if (!tokens.isEmpty()) {
        result.add(new ConlluSentence(sentId, text, tokens, mwtSpans));
        sentId = null;
        text = null;
        tokens = new ArrayList<>();
        mwtSpans = new ArrayList<>();
        sourceWordIdx = 0;
        activeMwtEnd = -1;
        activeMwtWordIdx = -1;
      }
    }

    private static String stripLineEnding(String s) {
      int end = s.length();
      while (end > 0 && (s.charAt(end - 1) == '\r' || s.charAt(end - 1) == '\n')) {
        end--;
      }
      return end == s.length() ? s : s.substring(0, end);
    }
  }
}
