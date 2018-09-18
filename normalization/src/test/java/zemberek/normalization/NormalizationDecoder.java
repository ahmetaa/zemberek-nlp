package zemberek.normalization;

import java.util.List;
import org.antlr.v4.runtime.Token;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;

public class NormalizationDecoder {

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

  List<String> decode(List<Token> tokens) {
    return null;
  }

}

