package zemberek.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

/**
 * Normalizes expressive letter elongations in noisy Turkish text (e.g. {@code çooook -> çok},
 * {@code saatttt -> saat}, {@code evett -> evet}) while preserving valid Turkish geminate
 * consonants/vowels and scoring competing reductions by language model unigram probability.
 */
public final class ElongationNormalizer {

  private static final Pattern ROMAN_NUMERAL_PATTERN = Pattern.compile("^[ivxlcdm]+$");
  private static final int MAX_VARIABLE_RUNS = 4;

  private final Predicate<String> isValidWord;
  private final ToDoubleFunction<String> unigramScorer;

  public ElongationNormalizer(
      Predicate<String> isValidWord,
      ToDoubleFunction<String> unigramScorer) {
    this.isValidWord = Objects.requireNonNull(isValidWord, "isValidWord");
    this.unigramScorer = Objects.requireNonNull(unigramScorer, "unigramScorer");
  }

  private static final class CharRun {

    final char ch;
    final int count;
    final boolean letter;

    CharRun(char ch, int count, boolean letter) {
      this.ch = ch;
      this.count = count;
      this.letter = letter;
    }
  }

  /**
   * Returns an elongation-normalized version of {@code word}, or {@code word} unchanged if it is
   * already valid or has no reducible elongation.
   */
  public String normalizeWord(String word) {
    if (word == null || word.length() < 3 || isValidWord.test(word)
        || ROMAN_NUMERAL_PATTERN.matcher(word).matches()) {
      return word;
    }

    List<CharRun> runs = splitRuns(word);
    List<Integer> threePlusRuns = new ArrayList<>();
    for (int i = 0; i < runs.size(); i++) {
      CharRun run = runs.get(i);
      if (run.letter && run.count >= 3) {
        threePlusRuns.add(i);
      }
    }

    if (!threePlusRuns.isEmpty()) {
      return normalizeThreePlusRuns(runs, threePlusRuns);
    }

    CharRun lastRun = runs.get(runs.size() - 1);
    if (lastRun.letter && lastRun.count == 2) {
      String collapsed = word.substring(0, word.length() - 1);
      if (isValidWord.test(collapsed)) {
        return collapsed;
      }
    }
    return word;
  }

  private String normalizeThreePlusRuns(List<CharRun> runs, List<Integer> threePlusRuns) {
    boolean[] searchRun = new boolean[runs.size()];
    boolean[] forceOne = new boolean[runs.size()];
    int variableCount = 0;

    for (int idx : threePlusRuns) {
      if (variableCount < MAX_VARIABLE_RUNS) {
        searchRun[idx] = true;
        variableCount++;
      } else {
        forceOne[idx] = true;
      }
    }

    int lastIdx = runs.size() - 1;
    CharRun lastRun = runs.get(lastIdx);
    if (lastRun.letter && lastRun.count == 2 && variableCount < MAX_VARIABLE_RUNS) {
      searchRun[lastIdx] = true;
    }

    List<String> validCandidates = new ArrayList<>();
    StringBuilder prefix = new StringBuilder();
    collectCandidates(runs, 0, searchRun, forceOne, prefix, validCandidates);

    if (!validCandidates.isEmpty()) {
      String best = validCandidates.get(0);
      double bestScore = unigramScorer.applyAsDouble(best);
      for (int i = 1; i < validCandidates.size(); i++) {
        String cand = validCandidates.get(i);
        double score = unigramScorer.applyAsDouble(cand);
        if (score > bestScore) {
          best = cand;
          bestScore = score;
        }
      }
      return best;
    }

    // Fallback when no candidate is morphologically valid (e.g. ASCII text prior to deasciifier or
    // an unsplit clitic like "gelecekmiiii"): collapse runs >= 3 to length 1 so downstream passes
    // can resolve it cleanly.
    StringBuilder fallback = new StringBuilder();
    for (CharRun run : runs) {
      int emit = (run.letter && run.count >= 3) ? 1 : run.count;
      appendRepeated(fallback, run.ch, emit);
    }
    return fallback.toString();
  }

  private void collectCandidates(
      List<CharRun> runs,
      int index,
      boolean[] searchRun,
      boolean[] forceOne,
      StringBuilder prefix,
      List<String> out) {
    if (index == runs.size()) {
      String candidate = prefix.toString();
      if (isValidWord.test(candidate)) {
        out.add(candidate);
      }
      return;
    }

    CharRun run = runs.get(index);
    int savedLen = prefix.length();
    if (searchRun[index]) {
      // Search {2, 1} so valid geminates precede single-letter collapses on equal scores.
      appendRepeated(prefix, run.ch, 2);
      collectCandidates(runs, index + 1, searchRun, forceOne, prefix, out);
      prefix.setLength(savedLen);

      appendRepeated(prefix, run.ch, 1);
      collectCandidates(runs, index + 1, searchRun, forceOne, prefix, out);
      prefix.setLength(savedLen);
    } else if (forceOne[index]) {
      appendRepeated(prefix, run.ch, 1);
      collectCandidates(runs, index + 1, searchRun, forceOne, prefix, out);
      prefix.setLength(savedLen);
    } else {
      appendRepeated(prefix, run.ch, run.count);
      collectCandidates(runs, index + 1, searchRun, forceOne, prefix, out);
      prefix.setLength(savedLen);
    }
  }

  private static List<CharRun> splitRuns(String word) {
    List<CharRun> runs = new ArrayList<>();
    char current = word.charAt(0);
    int count = 1;
    for (int i = 1; i < word.length(); i++) {
      char ch = word.charAt(i);
      if (ch == current) {
        count++;
      } else {
        runs.add(new CharRun(current, count, Character.isLetter(current)));
        current = ch;
        count = 1;
      }
    }
    runs.add(new CharRun(current, count, Character.isLetter(current)));
    return runs;
  }

  private static void appendRepeated(StringBuilder sb, char ch, int count) {
    for (int i = 0; i < count; i++) {
      sb.append(ch);
    }
  }
}
