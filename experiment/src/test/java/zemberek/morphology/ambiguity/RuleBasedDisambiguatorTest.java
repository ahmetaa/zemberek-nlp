package zemberek.morphology.ambiguity;

import java.io.IOException;
import org.junit.Test;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.AmbiguityAnalysis;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.PairRule;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.ResultSentence;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.Rules;

public class RuleBasedDisambiguatorTest {

  @Test
  public void test() throws IOException {
    //String input = "ABD Açık Serena Williams'ın";
    //String input = "Çünkü birbirine tezat oluşturuyor.";
    //String input = "O anda gördüm.";
    String input = "Aklımıza ilk gelen emeği öncelemek.";
    _TurkishMorphologicalAnalyzer analyzer = _TurkishMorphologicalAnalyzer.createDefault();
    Rules rules = new Rules();
    rules.pairLexRules.add(PairRule.fromLine("Aklı*|aklı* [akıl:Noun] *"));
    RuleBasedDisambiguator disambiguator = new RuleBasedDisambiguator(analyzer, rules);

    ResultSentence resultSentence = disambiguator.disambiguate(input);
    for (AmbiguityAnalysis a : resultSentence.results) {
      a.getForTrainingOutput().forEach(System.out::println);
    }

  }

}
