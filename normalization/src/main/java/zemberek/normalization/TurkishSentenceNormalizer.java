package zemberek.normalization;

import static zemberek.normalization.NormalizationPreprocessor.isWord;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.core.logging.Log;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Tries to normalize a sentence using lookup tables and heuristics.
 */
public class TurkishSentenceNormalizer {

  TurkishMorphology morphology;
  SmoothLm lm;
  TurkishSpellChecker spellChecker;

  NormalizationPreprocessor preprocessor;

  public TurkishSentenceNormalizer(
      TurkishMorphology morphology,
      Path commonSplit,
      SmoothLm languageModel) throws IOException {
    Log.info("Language model = %s", languageModel.info());
    this.morphology = morphology;

    this.lm = languageModel;

    StemEndingGraph graph = new StemEndingGraph(morphology);
    CharacterGraphDecoder decoder = new CharacterGraphDecoder(graph.stemGraph);
    this.spellChecker = new TurkishSpellChecker(
        morphology,
        decoder,
        CharacterGraphDecoder.ASCII_TOLERANT_MATCHER);

    this.preprocessor = new NormalizationPreprocessor(morphology, commonSplit, lm);
  }

  public String normalize(List<Token> tokens) {
    String s = preprocessor.combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = preprocessor.splitNecessaryWords(tokens, false);
    if (NormalizationPreprocessor.probablyRequiresDeasciifier(s)) {
      Deasciifier deasciifier = new Deasciifier(s);
      s = deasciifier.convertToTurkish();
    }
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = preprocessor.combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = preprocessor.splitNecessaryWords(tokens, true);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);

    s = preprocessor.useInformalAnalysis(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = useSpellChecker(tokens);
    return s;
  }

  public String normalize(String input) {
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(input);
    return normalize(tokens);
  }

  String useSpellChecker(List<Token> tokens) {

    List<String> result = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      Token currentToken = tokens.get(i);
      String current = currentToken.getText();
      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).getText();
      String previous = i == 0 ? null : tokens.get(i - 1).getText();
      if (isWord(currentToken) && (!hasAnalysis(current))) {
        List<String> candidates = spellChecker.suggestForWord(current, previous, next, lm);
        if (candidates.size() > 0) {
          result.add(candidates.get(0));
        } else {
          result.add(current);
        }
      } else {
        result.add(current);
      }
    }
    return String.join(" ", result);
  }

  List<String> getSpellCandidates(Token currentToken, String previous, String next) {
    String current = currentToken.getText();
    if (isWord(currentToken) && (!hasAnalysis(current))) {
      List<String> candidates = spellChecker.suggestForWord(current, previous, next, lm);
      if (candidates.size() > 0) {
        return candidates;
      }
    }
    return Collections.emptyList();
  }


  boolean hasAnalysis(String s) {
    WordAnalysis a = morphology.analyze(s);
    return a.analysisCount() > 0;
  }


  private static class Hypothesis implements Scorable {

    // for a three gram model, holds the 2 history words.
    Candidate[] history;
    Candidate current;

    // required for back tracking.
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
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Hypothesis that = (Hypothesis) o;

      if (!Arrays.equals(history, that.history)) {
        return false;
      }
      return current.equals(that.current);
    }

    @Override
    public int hashCode() {
      int result = Arrays.hashCode(history);
      result = 31 * result + current.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Hypothesis{" +
          "history=" + Arrays.toString(history) +
          ", current=" + current +
          ", score=" + score +
          '}';
    }
  }

  /**
   * Represents a candidate word.
   */
  private static class Candidate {

    final String content;
    final float score;

    Candidate(String content) {
      this.content = content;
      score = 1f;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Candidate candidate = (Candidate) o;

      return content.equals(candidate.content);
    }

    @Override
    public int hashCode() {
      return content.hashCode();
    }

    @Override
    public String toString() {
      return "Candidate{" +
          "content='" + content + '\'' +
          ", score=" + score +
          '}';
    }
  }

  private static class Candidates {

    String word;
    List<Candidate> candidates;

    Candidates(String word,
        List<Candidate> candidates) {
      this.word = word;
      this.candidates = candidates;
    }

    @Override
    public String toString() {
      return "Candidates{" +
          "word='" + word + '\'' +
          ", candidates=" + candidates +
          '}';
    }
  }

  private ActiveList<Hypothesis> current = new ActiveList<>();
  private ActiveList<Hypothesis> next = new ActiveList<>();

  private static Candidate START = new Candidate("<s>");
  private static Candidate END = new Candidate("</s>");


  List<String> decode(String sentence) {

    sentence = (preprocessor.preProcess(sentence));
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);

    List<Candidates> candidatesList = new ArrayList<>();

    int lmOrder = lm.getOrder();

    for (int i = 0; i < tokens.size(); i++) {
      Token currentToken = tokens.get(i);
      String current = currentToken.getText();
      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).getText();
      String previous = i == 0 ? null : tokens.get(i - 1).getText();
      List<String> spellCandidates = getSpellCandidates(currentToken, previous, next);
      if (spellCandidates.size() > 5) {
        spellCandidates = new ArrayList<>(spellCandidates.subList(0, 5));
      }
      if (spellCandidates.isEmpty()) {
        spellCandidates = Lists.newArrayList(current);
      }
      Candidates candidates = new Candidates(
          currentToken.getText(),
          spellCandidates.stream().map(Candidate::new).collect(Collectors.toList()));
      candidatesList.add(candidates);
    }

    // Path with END tokens.
    candidatesList.add(new Candidates("</s>", Collections.singletonList(END)));

    Hypothesis initial = new Hypothesis();
    initial.history = new Candidate[lmOrder - 1];
    Arrays.fill(initial.history, START);
    initial.current = START;
    initial.score = 0f;
    current.add(initial);

    for (Candidates candidates : candidatesList) {
      for (Hypothesis h : current) {
        for (Candidate c : candidates.candidates) {
          Hypothesis newHyp = new Hypothesis();
          Candidate[] hist = new Candidate[lmOrder - 1];
          if (lmOrder > 2) {
            System.arraycopy(h.history, 1, hist, 0, lmOrder - 1);
          }
          hist[hist.length - 1] = h.current;
          newHyp.current = c;
          newHyp.history = hist;
          newHyp.previous = h;

          // score calculation.
          int[] indexes = new int[lmOrder];
          for (int j = 0; j < lmOrder - 1; j++) {
            indexes[j] = lm.getVocabulary().indexOf(hist[j].content);
          }
          indexes[lmOrder - 1] = lm.getVocabulary().indexOf(c.content);
          float score = lm.getProbability(indexes);

          newHyp.score = h.score + score;
          next.add(newHyp);
        }
      }
      current = next;
      next = new ActiveList<>();
    }

    Hypothesis best = current.getBest();
    List<String> seq = new ArrayList<>();
    Hypothesis h = best;
    while (h != null && h.current != START) {
      seq.add(h.current.content);
      h = h.previous;
    }
    Collections.reverse(seq);
    return seq;
  }

}
