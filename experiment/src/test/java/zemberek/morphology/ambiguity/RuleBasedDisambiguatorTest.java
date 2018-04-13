package zemberek.morphology.ambiguity;

import java.io.IOException;
import org.junit.Test;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.AmbiguityAnalysis;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.ResultSentence;

public class RuleBasedDisambiguatorTest {

  @Test
  public void test() throws IOException {
    //String input = "ABD Açık Serena Williams'ın";
    String input = "Çünkü birbirine tezat oluşturuyor.";
    _TurkishMorphologicalAnalyzer analyzer = _TurkishMorphologicalAnalyzer.createDefault();
    RuleBasedDisambiguator disambiguator = new RuleBasedDisambiguator(analyzer);

    ResultSentence resultSentence = disambiguator.disambiguate(input);
    for(AmbiguityAnalysis a : resultSentence.results) {
      System.out.println(a.getForTrainingOutput());
    }

  }

}
