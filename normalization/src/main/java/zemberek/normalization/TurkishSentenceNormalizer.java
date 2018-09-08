package zemberek.normalization;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;
import zemberek.morphology.TurkishMorphology;
import zemberek.tokenization.TurkishTokenizer;

public class TurkishSentenceNormalizer {

  TurkishMorphology morphology;

  public TurkishSentenceNormalizer(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  String normalize(String input) {
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(input);
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      result.add(separateCommon(token.getText()));
    }
    return String.join(" ", result);
  }

  /**
   * Tries to separate question words and conjunctions
   * Such as:
   * <pre>
   * gelecekmisin -> gelecek misin
   * tutupda -> tutup da
   * öyleki -> öyle ki
   * </pre>
   */
  String separateCommon(String input) {
    return input;
  }


}
