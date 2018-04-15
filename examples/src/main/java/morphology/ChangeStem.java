package morphology;

import java.io.IOException;
import java.util.List;
import zemberek.morphology._analyzer._Generator;
import zemberek.morphology._analyzer._Generator.GenerationResult;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._TurkishMorphology;
import zemberek.morphology._analyzer._WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;

public class ChangeStem {

  _TurkishMorphology morphology;

  public ChangeStem(_TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    _TurkishMorphology morphology = _TurkishMorphology.createWithDefaults();
    DictionaryItem newStem = morphology.getLexicon().getMatchingItems("poğaça").get(0);
    new ChangeStem(morphology).regenerate("simidime", newStem);
  }

  private void regenerate(String word, DictionaryItem dictionaryItem) {
    System.out.println("Input Word = " + word);
    _WordAnalysis results = morphology.analyze(word);
    for (_SingleAnalysis result : results) {
      List<_Generator.GenerationResult> generated =
          morphology.getGenerator().generate(dictionaryItem.lemma, result.getMorphemes());
      for (GenerationResult s : generated) {
        System.out.println("Input analysis: " + result.formatLong());
        System.out.println("After stem change, word = " + s.surface);
        System.out.println("After stem change, Analysis = " + s.analysis.formatLong());
      }
    }
  }

}
