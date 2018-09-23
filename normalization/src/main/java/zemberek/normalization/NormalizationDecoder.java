package zemberek.normalization;

import static zemberek.normalization.TurkishSentenceNormalizer.probablyRequiresDeasciifier;

import java.util.List;
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

  static class NormalizationHypothesis implements Scorable {

    // for a three gram model, holds the 2 history words.
    int[] history;

    // required for back tracking.
    NormalizationHypothesis previous;

    float score;

    @Override
    public float getScore() {
      return score;
    }

  }

  ActiveList<NormalizationHypothesis> current;
  ActiveList<NormalizationHypothesis> next;

  List<String> decode(String sentence) {

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
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);

    return null;
  }

}

