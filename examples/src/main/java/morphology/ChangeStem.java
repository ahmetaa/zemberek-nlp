package morphology;

import java.io.IOException;
import java.util.List;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
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

  public void regenerate(String word, DictionaryItem lemma) {
    System.out.println("Word = " + word);
    List<WordAnalysis> results = morphology.analyze(word);
    for (WordAnalysis result : results) {
      String[] generated = morphology.getGenerator().generate(lemma, result.getSuffixes());
      for (String s : generated) {
        System.out
            .println("Generated for " + result.formatLong() + " with item " + lemma + " = " + s);
      }
    }
  }

}
