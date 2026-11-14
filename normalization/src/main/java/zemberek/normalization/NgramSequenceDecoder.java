package zemberek.normalization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.core.turkish.Turkish;
import zemberek.lm.LmVocabulary;
import zemberek.lm.compression.SmoothLm;

/**
 * N-gram Viterbi beam decoder over scored token candidate lists backed by {@link SmoothLm}.
 */
final class NgramSequenceDecoder {

  private static final Candidate START = new Candidate("<s>", 0f);
  private static final Candidate END = new Candidate("</s>", 0f);
  private static final Candidates END_CANDIDATES = new Candidates("</s>", List.of(END));

  private final SmoothLm lm;

  NgramSequenceDecoder(SmoothLm lm) {
    this.lm = lm;
  }

  record Candidate(String content, float score) {
    @Override
    public boolean equals(Object o) {
      return this == o || (o instanceof Candidate other && content.equals(other.content));
    }

    @Override
    public int hashCode() {
      return content.hashCode();
    }
  }

  record Candidates(String word, List<Candidate> candidates) {}

  private static final class Hypothesis implements Scorable {
    Candidate[] history;
    Candidate current;
    Hypothesis previous;
    float score;

    @Override
    public float getScore() {
      return score;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Hypothesis that)) {
        return false;
      }
      return Arrays.equals(history, that.history) && current.equals(that.current);
    }

    @Override
    public int hashCode() {
      return 31 * Arrays.hashCode(history) + current.hashCode();
    }
  }

  List<String> decode(List<Candidates> candidatesList) {
    ActiveList<Hypothesis> current = new ActiveList<>();
    ActiveList<Hypothesis> next = new ActiveList<>();

    List<Candidates> sequence = new ArrayList<>(candidatesList.size() + 1);
    sequence.addAll(candidatesList);
    sequence.add(END_CANDIDATES);

    int lmOrder = lm.getOrder();
    Hypothesis initial = new Hypothesis();
    initial.history = new Candidate[lmOrder - 1];
    Arrays.fill(initial.history, START);
    initial.current = START;
    initial.score = 0f;
    current.add(initial);

    for (Candidates candidates : sequence) {
      for (Hypothesis h : current) {
        for (Candidate c : candidates.candidates()) {
          Hypothesis newHyp = new Hypothesis();
          Candidate[] hist = new Candidate[lmOrder - 1];
          if (lmOrder > 2) {
            System.arraycopy(h.history, 1, hist, 0, lmOrder - 2);
          }
          if (hist.length > 0) {
            hist[hist.length - 1] = h.current;
          }
          newHyp.current = c;
          newHyp.history = hist;
          newHyp.previous = h;

          int[] indexes = new int[lmOrder];
          for (int j = 0; j < lmOrder - 1; j++) {
            indexes[j] = lmIndex(hist[j].content);
          }
          indexes[lmOrder - 1] = lmIndex(c.content);
          newHyp.score = h.score + lm.getProbability(indexes) + c.score;
          next.add(newHyp);
        }
      }
      current = next;
      next = new ActiveList<>();
    }

    List<String> result = new ArrayList<>();
    Hypothesis h = current.getBest().previous; // skip </s>
    while (h != null && h.current != START) {
      result.add(h.current.content);
      h = h.previous;
    }
    Collections.reverse(result);
    return result;
  }

  private int lmIndex(String word) {
    LmVocabulary vocabulary = lm.getVocabulary();
    String normalized = word.indexOf('’') >= 0 ? word.replace('’', '\'') : word;
    int idx = vocabulary.indexOf(normalized);
    if (idx == vocabulary.getUnknownWordIndex() && normalized.indexOf('\'') > 0) {
      int capIdx = vocabulary.indexOf(Turkish.capitalize(normalized));
      if (capIdx != vocabulary.getUnknownWordIndex()) {
        return capIdx;
      }
    }
    return idx;
  }
}
