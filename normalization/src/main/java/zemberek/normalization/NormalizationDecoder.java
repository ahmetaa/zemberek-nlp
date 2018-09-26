package zemberek.normalization;

import static zemberek.normalization.TurkishSentenceNormalizer.probablyRequiresDeasciifier;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.tokenization.TurkishTokenizer;

public class NormalizationDecoder {

  TurkishSentenceNormalizer normalizer;

  public NormalizationDecoder(TurkishSentenceNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  private static class Hypothesis implements Scorable {

    // for a three gram model, holds the 2 history words.
    Candidate[] history;

    // required for back tracking.
    Hypothesis previous;

    float score;

    @Override
    public float getScore() {
      return score;
    }
  }

  /**
   * Represents a candidate word.
   */
  private static class Candidate {

    final String content;
    final float score;

    public Candidate(String content, float score) {
      this.content = content;
      this.score = score;
    }

    public Candidate(String content) {
      this.content = content;
      score = 1f;
    }
  }

  private static class Candidates {

    String word;
    List<Candidate> candidates;

    public Candidates(String word,
        List<Candidate> candidates) {
      this.word = word;
      this.candidates = candidates;
    }
  }

  ActiveList<Hypothesis> current;
  ActiveList<Hypothesis> next;

  Candidate START = new Candidate("<s>");
  Candidate END = new Candidate("</s>");


  List<String> decode(String sentence) {

    List<Token> tokens = preprocess(sentence);

    List<Candidates> candidatesList = new ArrayList<>();

    int lmOrder = normalizer.lm.getOrder();

    // pad beginning with START tokens.
    for (int i = 0; i < lmOrder - 1; i++) {
      candidatesList.add(new Candidates("<s>", Collections.singletonList(START)));
    }

    for (int i = 0; i < tokens.size(); i++) {
      Token currentToken = tokens.get(i);
      String current = currentToken.getText();
      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).getText();
      String previous = i == 0 ? null : tokens.get(i - 1).getText();
      List<String> spellCandidates = normalizer.getSpellCandidates(currentToken, previous, next);
      if (spellCandidates.isEmpty()) {
        spellCandidates = Lists.newArrayList(current);
      }
      Candidates candidates = new Candidates(
          currentToken.getText(),
          spellCandidates.stream().map(Candidate::new).collect(Collectors.toList()));
      candidatesList.add(candidates);
    }

    // pad end with END tokens.
    for (int i = 0; i < lmOrder - 1; i++) {
      candidatesList.add(new Candidates("</s>", Collections.singletonList(END)));
    }

    for (int i = lmOrder - 1; i < candidatesList.size() - lmOrder + 1; i++) {
      Candidates candidates = candidatesList.get(i);

    }
    return null;
  }


  List<Token> preprocess(String sentence) {
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    String s = normalizer.combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = normalizer.splitNecessaryWords(tokens, false);
    if (probablyRequiresDeasciifier(s)) {
      Deasciifier deasciifier = new Deasciifier(s);
      s = deasciifier.convertToTurkish();
    }
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = normalizer.combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = normalizer.splitNecessaryWords(tokens, true);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);

    s = normalizer.useInformalAnalysis(tokens);
    return TurkishTokenizer.DEFAULT.tokenize(s);
  }

}

