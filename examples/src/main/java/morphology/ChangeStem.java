package morphology;

import java.io.IOException;
import java.util.List;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;

public class ChangeStem {

  TurkishMorphology morphology;

  public ChangeStem(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    DictionaryItem newStem = morphology.getLexicon().getMatchingItems("poğaça").get(0);
    new ChangeStem(morphology).regenerate("simidime", newStem);
  }

  private void regenerate(String word, DictionaryItem dictionaryItem) {
    System.out.println("Input Word = " + word);
    WordAnalysis results = morphology.analyze(word);
    for (SingleAnalysis result : results) {
      List<Result> generated =
          morphology.getWordGenerator().generate(dictionaryItem, result.getMorphemes());
      for (Result s : generated) {
        System.out.println("Input analysis: " + result.formatLong());
        System.out.println("After stem change, word = " + s.surface);
        System.out.println("After stem change, Analysis = " + s.analysis.formatLong());
      }
    }
  }

}
